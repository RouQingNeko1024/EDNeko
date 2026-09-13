package net.ccbluex.liquidbounce.utils.customizable.editor

import net.ccbluex.liquidbounce.utils.customizable.CustomizableController
import net.ccbluex.liquidbounce.utils.customizable.logic.*
import java.awt.*
import java.awt.event.*
import java.util.UUID
import javax.swing.*
import javax.swing.border.BevelBorder

/**
 * 逻辑编辑画布
 *
 * 中间区域 - 拖动模块拼接逻辑的主要编辑区
 * 支持：
 * - 拖动模块
 * - 连接节点
 * - 选择/删除/复制
 * - 缩放/滚动
 * - 网格背景
 */
class EditorCanvas(private var graph: LogicGraph) : JComponent() {

    // ===== 交互状态 =====
    private var selectedNodeId: String? = null
    private var draggingNodeId: String? = null
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0

    // 连接拖拽
    private var isDraggingConnection = false
    private var connectionDragStart: NodePort? = null
    private var connectionDragStartNodeId: String? = null
    private var connectionDragEndX = 0.0
    private var connectionDragEndY = 0.0

    // 滚动/缩放
    private var offsetX = 0.0
    private var offsetY = 0.0
    private var zoom = 1.0
    private var panning = false
    private var panStartX = 0
    private var panStartY = 0

    // 悬停
    private var hoveredNodeId: String? = null

    // 模块拖入（从左侧模块面板）
    private var isDraggingNewNode = false
    private var newNodeType: String? = null
    private var newNodeDef: NodeDefinition? = null

    init {
        setupCanvas()
    }

    fun loadGraph(newGraph: LogicGraph) {
        graph = newGraph
        repaint()
    }

    // ===== 设置 =====
    private fun setupCanvas() {
        background = EditorTheme.CANVAS_BACKGROUND
        preferredSize = Dimension(3000, 3000)
        isOpaque = true
        isFocusable = true

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                requestFocusInWindow()
                val (mx, my) = screenToCanvas(e.x, e.y)

                when {
                    // 检查连接端口点击
                    e.button == MouseEvent.BUTTON1 && tryStartConnectionDrag(mx, my) -> {}

                    // 检查节点点击
                    e.button == MouseEvent.BUTTON1 -> {
                        val clicked = findNodeAt(mx, my)
                        if (clicked != null) {
                            selectedNodeId = clicked.id
                            draggingNodeId = clicked.id
                            dragOffsetX = mx - clicked.x
                            dragOffsetY = my - clicked.y
                            repaint()
                        } else {
                            selectedNodeId = null
                            repaint()
                        }
                    }

                    // 中键平移
                    e.button == MouseEvent.BUTTON2 -> {
                        panning = true
                        panStartX = e.x
                        panStartY = e.y
                        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    }
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                when {
                    isDraggingConnection -> {
                        val (mx, my) = screenToCanvas(e.x, e.y)
                        finishConnectionDrag(mx, my)
                    }
                    draggingNodeId != null -> {
                        draggingNodeId = null
                    }
                    panning -> {
                        panning = false
                        cursor = Cursor.getDefaultCursor()
                    }
                    isDraggingNewNode -> {
                        val (mx, my) = screenToCanvas(e.x, e.y)
                        finishNewNodeDrag(mx, my)
                    }
                }
            }

            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && e.button == MouseEvent.BUTTON1) {
                    val (mx, my) = screenToCanvas(e.x, e.y)
                    val node = findNodeAt(mx, my)
                    if (node != null) {
                        showNodeEditor(node)
                    }
                }
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                when {
                    draggingNodeId != null -> {
                        val (mx, my) = screenToCanvas(e.x, e.y)
                        val node = graph.getNode(draggingNodeId!!) ?: return
                        node.x = mx - dragOffsetX
                        node.y = my - dragOffsetY
                        repaint()
                    }
                    isDraggingConnection -> {
                        connectionDragEndX = e.x.toDouble()
                        connectionDragEndY = e.y.toDouble()
                        repaint()
                    }
                    panning -> {
                        offsetX += (e.x - panStartX).toDouble() / zoom
                        offsetY += (e.y - panStartY).toDouble() / zoom
                        panStartX = e.x
                        panStartY = e.y
                        repaint()
                    }
                }
            }

            override fun mouseMoved(e: MouseEvent) {
                val (mx, my) = screenToCanvas(e.x, e.y)
                val node = findNodeAt(mx, my)
                hoveredNodeId = node?.id
                updateCursor(mx, my, e.x, e.y)
                repaint()
            }
        })

        // 滚轮缩放
        addMouseWheelListener { e ->
            if (e.isControlDown) {
                val factor = if (e.wheelRotation < 0) 1.1 else 0.9
                zoom = (zoom * factor).coerceIn(0.25, 4.0)
                repaint()
            } else {
                // 滚动画布
                val parent = parent
                if (parent is JViewport) {
                    val p = parent.viewPosition
                    p.translate(0, e.unitsToScroll * EditorTheme.SCROLL_UNIT)
                    parent.viewPosition = p
                }
            }
        }

        // 键盘快捷键
        registerKeyboardAction({ deleteSelected() },
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_FOCUSED)
        registerKeyboardAction({ deleteSelected() },
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), JComponent.WHEN_FOCUSED)
    }

    // ===== 新增节点拖入 =====

    /**
     * 开始拖入新节点（从左侧模块面板调用）
     */
    fun startNewNodeDrag(nodeType: String, definition: NodeDefinition) {
        isDraggingNewNode = true
        newNodeType = nodeType
        newNodeDef = definition
    }

    private fun finishNewNodeDrag(canvasX: Double, canvasY: Double) {
        if (!isDraggingNewNode) return

        val nodeType = newNodeType ?: return
        val def = newNodeDef ?: return

        val node = LogicNode(
            id = UUID.randomUUID().toString(),
            type = nodeType,
            definition = def,
            x = canvasX,
            y = canvasY
        )
        graph.addNode(node)
        CustomizableController.setGraph(graph)
        saveUndoState()
        repaint()

        isDraggingNewNode = false
        newNodeType = null
        newNodeDef = null
    }

    // ===== 新增节点（从左侧模块面板点击） =====

    /**
     * 在画布中央添加新节点
     */
    fun addNodeAtCenter(def: NodeDefinition) {
        // 计算可见区域的中心
        val viewport = parent
        var centerX = 200.0
        var centerY = 200.0
        if (viewport is JViewport) {
            val viewPos = viewport.viewPosition
            val viewSize = viewport.extentSize
            centerX = (viewPos.x + viewSize.width / 2.0 - offsetX) / zoom
            centerY = (viewPos.y + viewSize.height / 2.0 - offsetY) / zoom
        }

        val node = LogicNode(
            id = UUID.randomUUID().toString(),
            type = def.type,
            definition = def,
            x = centerX,
            y = centerY
        )
        graph.addNode(node)
        CustomizableController.setGraph(graph)
        selectedNodeId = node.id
        saveUndoState()
        repaint()
    }

    // ===== 连接操作 =====

    private fun tryStartConnectionDrag(canvasX: Double, canvasY: Double): Boolean {
        for (node in graph.nodes.values) {
            val nodeRect = getNodeRect(node)

            // 检查输入端口
            for (port in node.definition.inputs) {
                val portPos = getInputPortPos(node, port, nodeRect)
                if (Point(canvasX.toInt(), canvasY.toInt())
                        .distance(portPos.x.toDouble(), portPos.y.toDouble()) <= EditorTheme.NODE_PORT_RADIUS + 4
                ) {
                    // 开始从输入端口拖出（反向连接）
                    isDraggingConnection = true
                    connectionDragStart = port
                    connectionDragStartNodeId = node.id
                    connectionDragEndX = canvasX
                    connectionDragEndY = canvasY
                    return true
                }
            }

            // 检查输出端口
            for (port in node.definition.outputs) {
                val portPos = getOutputPortPos(node, port, nodeRect)
                if (Point(canvasX.toInt(), canvasY.toInt())
                        .distance(portPos.x.toDouble(), portPos.y.toDouble()) <= EditorTheme.NODE_PORT_RADIUS + 4
                ) {
                    isDraggingConnection = true
                    connectionDragStart = port
                    connectionDragStartNodeId = node.id
                    connectionDragEndX = canvasX
                    connectionDragEndY = canvasY
                    return true
                }
            }
        }
        return false
    }

    private fun finishConnectionDrag(canvasX: Double, canvasY: Double) {
        if (!isDraggingConnection) return

        val sourceNodeId = connectionDragStartNodeId ?: return
        val sourcePort = connectionDragStart ?: return

        // 寻找目标端口
        for (node in graph.nodes.values) {
            if (node.id == sourceNodeId) continue
            val nodeRect = getNodeRect(node)

            // 检查输入端口（如果拖出的是输出端口）
            if (!sourcePort.isInput) {
                for (port in node.definition.inputs) {
                    val portPos = getInputPortPos(node, port, nodeRect)
                    if (Point(canvasX.toInt(), canvasY.toInt())
                            .distance(portPos.x.toDouble(), portPos.y.toDouble()) <= EditorTheme.NODE_PORT_RADIUS + 6
                    ) {
                        createConnection(sourceNodeId, sourcePort.id, node.id, port.id)
                        isDraggingConnection = false
                        repaint()
                        return
                    }
                }
            }

            // 检查输出端口（如果拖出的是输入端口）
            if (sourcePort.isInput) {
                for (port in node.definition.outputs) {
                    val portPos = getOutputPortPos(node, port, nodeRect)
                    if (Point(canvasX.toInt(), canvasY.toInt())
                            .distance(portPos.x.toDouble(), portPos.y.toDouble()) <= EditorTheme.NODE_PORT_RADIUS + 6
                    ) {
                        createConnection(node.id, port.id, sourceNodeId, sourcePort.id)
                        isDraggingConnection = false
                        repaint()
                        return
                    }
                }
            }
        }

        isDraggingConnection = false
        repaint()
    }

    private fun createConnection(sourceNodeId: String, sourcePortId: String, targetNodeId: String, targetPortId: String) {
        val conn = LogicConnection(
            id = UUID.randomUUID().toString(),
            sourceNodeId = sourceNodeId,
            sourcePortId = sourcePortId,
            targetNodeId = targetNodeId,
            targetPortId = targetPortId
        )

        if (!conn.validate(graph)) {
            // 类型不兼容
            return
        }

        // 检查是否已存在相同连接
        val exists = graph.connections.values.any {
            it.sourceNodeId == sourceNodeId && it.sourcePortId == sourcePortId &&
                    it.targetNodeId == targetNodeId && it.targetPortId == targetPortId
        }
        if (exists) return

        graph.addConnection(conn)
        CustomizableController.setGraph(graph)
        saveUndoState()
    }

    // ===== 节点操作 =====

    fun deleteSelected() {
        val id = selectedNodeId ?: return
        graph.removeNode(id)
        selectedNodeId = null
        CustomizableController.setGraph(graph)
        saveUndoState()
        repaint()
    }

    fun duplicateSelected() {
        val id = selectedNodeId ?: return
        val original = graph.getNode(id) ?: return

        val newNode = original.copy(
            id = UUID.randomUUID().toString(),
            x = original.x + 30,
            y = original.y + 30
        )
        graph.addNode(newNode)
        selectedNodeId = newNode.id
        CustomizableController.setGraph(graph)
        saveUndoState()
        repaint()
    }

    fun zoomIn() { zoom = (zoom * 1.25).coerceAtMost(4.0); repaint() }
    fun zoomOut() { zoom = (zoom / 1.25).coerceAtLeast(0.25); repaint() }
    fun resetView() { zoom = 1.0; offsetX = 0.0; offsetY = 0.0; repaint() }

    // ===== 节点编辑器 =====

    private fun showNodeEditor(node: LogicNode) {
        if (node.definition.parameters.isEmpty()) return

        val dialog = JDialog(this@EditorCanvas.topLevelAncestor as? Frame ?: JFrame(), "Edit: ${node.definition.displayName}", true)
        dialog.background = EditorTheme.BACKGROUND

        val panel = JPanel(GridBagLayout())
        panel.background = EditorTheme.BACKGROUND
        panel.border = BevelBorder(BevelBorder.RAISED)
        val gbc = GridBagConstraints()
        gbc.insets = Insets(4, 8, 4, 8)
        gbc.fill = GridBagConstraints.HORIZONTAL

        val fields = mutableListOf<Pair<NodeParameter, JComponent>>()

        gbc.gridy = 0
        for (param in node.definition.parameters) {
            gbc.gridx = 0
            val label = JLabel(" ${param.name}:")
            label.font = EditorTheme.UI_FONT
            panel.add(label, gbc)

            gbc.gridx = 1
            val value = node.getParameter(param.id) ?: param.defaultValue
            val comp: JComponent

            when (param.type) {
                ValueType.BOOLEAN -> {
                    val cb = JCheckBox("", value as? Boolean ?: false)
                    cb.background = EditorTheme.BACKGROUND
                    comp = cb
                }
                ValueType.INTEGER -> {
                    val tf = JTextField(if (value != null) value.toString() else param.defaultValue?.toString() ?: "0", 10)
                    tf.font = EditorTheme.UI_FONT
                    comp = tf
                }
                ValueType.FLOAT, ValueType.DOUBLE -> {
                    val tf = JTextField(if (value != null) value.toString() else param.defaultValue?.toString() ?: "0.0", 10)
                    tf.font = EditorTheme.UI_FONT
                    comp = tf
                }
                else -> {
                    val tf = JTextField(value?.toString() ?: "", 15)
                    tf.font = EditorTheme.UI_FONT
                    comp = tf
                }
            }
            panel.add(comp, gbc)
            fields.add(param to comp)
            gbc.gridy++
        }

        // 按钮
        gbc.gridx = 0
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.CENTER

        val btnPanel = JPanel(FlowLayout(FlowLayout.CENTER, 8, 4))
        btnPanel.background = EditorTheme.BACKGROUND

        val okBtn = JButton("OK")
        okBtn.font = EditorTheme.UI_FONT
        okBtn.background = EditorTheme.BACKGROUND
        okBtn.addActionListener {
            for ((param, comp) in fields) {
                val newValue: Any? = when (comp) {
                    is JCheckBox -> comp.isSelected
                    is JTextField -> {
                        when (param.type) {
                            ValueType.INTEGER -> comp.text.toIntOrNull() ?: param.defaultValue
                            ValueType.FLOAT -> comp.text.toFloatOrNull() ?: param.defaultValue
                            ValueType.DOUBLE -> comp.text.toDoubleOrNull() ?: param.defaultValue
                            else -> comp.text
                        }
                    }
                    else -> (comp as? JTextField)?.text
                }
                node.setParameter(param.id, newValue)
            }
            CustomizableController.setGraph(graph)
            saveUndoState()
            dialog.dispose()
        }

        val cancelBtn = JButton("Cancel")
        cancelBtn.font = EditorTheme.UI_FONT
        cancelBtn.background = EditorTheme.BACKGROUND
        cancelBtn.addActionListener { dialog.dispose() }

        btnPanel.add(okBtn)
        btnPanel.add(cancelBtn)
        panel.add(btnPanel, gbc)

        dialog.add(panel)
        dialog.pack()
        dialog.setLocationRelativeTo(this)
        dialog.isVisible = true
    }

    // ===== 绘画 =====

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // 背景
        g2d.color = EditorTheme.CANVAS_BACKGROUND
        g2d.fillRect(0, 0, width, height)

        g2d.translate(offsetX, offsetY)
        g2d.scale(zoom, zoom)

        // 网格
        drawGrid(g2d)

        // 连接线
        drawConnections(g2d)

        // 拖拽中的连接线
        if (isDraggingConnection) {
            drawDraggingConnection(g2d)
        }

        // 节点
        drawNodes(g2d)

        // 运行时高亮
        drawRuntimeHighlight(g2d)
    }

    private fun drawGrid(g2d: Graphics2D) {
        g2d.color = EditorTheme.CANVAS_GRID
        val grid = (EditorTheme.GRID_SIZE * zoom).toInt()

        val startX = -(offsetX % grid).toInt()
        val startY = -(offsetY % grid).toInt()

        var x = startX
        while (x < width) {
            g2d.drawLine(x, 0, x, height)
            x += grid
        }
        var y = startY
        while (y < height) {
            g2d.drawLine(0, y, width, y)
            y += grid
        }
    }

    private fun drawConnections(g2d: Graphics2D) {
        g2d.stroke = BasicStroke(2f)
        g2d.color = EditorTheme.CONNECTION_LINE

        for (conn in graph.connections.values) {
            val sourceNode = graph.getNode(conn.sourceNodeId) ?: continue
            val targetNode = graph.getNode(conn.targetNodeId) ?: continue

            val sourceRect = getNodeRect(sourceNode)
            val targetRect = getNodeRect(targetNode)

            val sourcePort = sourceNode.definition.outputs.find { it.id == conn.sourcePortId }
                ?: sourceNode.definition.inputs.find { it.id == conn.sourcePortId }
                ?: continue
            val targetPort = targetNode.definition.inputs.find { it.id == conn.targetPortId }
                ?: targetNode.definition.outputs.find { it.id == conn.targetPortId }
                ?: continue

            val sourcePos = if (!sourcePort.isInput || sourcePort.isExecution)
                getOutputPortPos(sourceNode, sourcePort, sourceRect)
            else
                getInputPortPos(sourceNode, sourcePort, sourceRect)

            val targetPos = if (targetPort.isInput || targetPort.isExecution)
                getInputPortPos(targetNode, targetPort, targetRect)
            else
                getOutputPortPos(targetNode, targetPort, targetRect)

            // 贝塞尔曲线
            val controlOffset = Math.max(
                Math.abs(sourcePos.x - targetPos.x).toFloat() * 0.4f,
                30f
            )

            val path = java.awt.geom.CubicCurve2D.Float(
                sourcePos.x.toFloat(), sourcePos.y.toFloat(),
                sourcePos.x.toFloat() + controlOffset, sourcePos.y.toFloat(),
                targetPos.x.toFloat() - controlOffset, targetPos.y.toFloat(),
                targetPos.x.toFloat(), targetPos.y.toFloat()
            )
            g2d.draw(path)
        }
    }

    private fun drawDraggingConnection(g2d: Graphics2D) {
        val sourceNode = graph.getNode(connectionDragStartNodeId ?: return) ?: return
        val sourceRect = getNodeRect(sourceNode)
        val sourcePort = connectionDragStart ?: return

        val sourcePos = if (!sourcePort.isInput || sourcePort.isExecution)
            getOutputPortPos(sourceNode, sourcePort, sourceRect)
        else
            getInputPortPos(sourceNode, sourcePort, sourceRect)

        // 终点在屏幕坐标，需转换
        val endX = connectionDragEndX / zoom - offsetX / zoom
        val endY = connectionDragEndY / zoom - offsetY / zoom

        g2d.stroke = BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(8f), 0f)
        g2d.color = EditorTheme.CONNECTION_HIGHLIGHT
        g2d.drawLine(sourcePos.x.toInt(), sourcePos.y.toInt(), endX.toInt(), endY.toInt())
    }

    private fun drawNodes(g2d: Graphics2D) {
        for (node in graph.nodes.values) {
            drawNode(g2d, node)
        }
    }

    private fun drawNode(g2d: Graphics2D, node: LogicNode) {
        val rect = getNodeRect(node)
        val isSelected = node.id == selectedNodeId
        val isHovered = node.id == hoveredNodeId
        val categoryColor = EditorTheme.getNodeCategoryColor(node.definition.category.name)

        // 节点阴影（Win95 效果）
        g2d.color = EditorTheme.SHADOW
        g2d.fillRect(rect.x + 3, rect.y + 3, rect.width, rect.height)

        // 节点背景
        g2d.color = categoryColor
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height)

        // 节点边框
        g2d.color = if (isSelected) EditorTheme.NODE_SELECTED_BORDER else EditorTheme.NODE_BORDER
        g2d.stroke = BasicStroke(if (isSelected) 2f else 1f)
        g2d.drawRect(rect.x, rect.y, rect.width, rect.height)

        // Win95 凸起装饰
        g2d.color = EditorTheme.HIGHLIGHT
        g2d.drawLine(rect.x + 1, rect.y + 1, rect.x + rect.width - 2, rect.y + 1)
        g2d.drawLine(rect.x + 1, rect.y + 1, rect.x + 1, rect.y + rect.height - 2)
        g2d.color = EditorTheme.DARKEST
        g2d.drawLine(rect.x + rect.width - 1, rect.y, rect.x + rect.width - 1, rect.y + rect.height - 1)
        g2d.drawLine(rect.x, rect.y + rect.height - 1, rect.x + rect.width - 1, rect.y + rect.height - 1)

        // 标题背景
        g2d.color = EditorTheme.TITLE_BAR_ACTIVE
        g2d.fillRect(rect.x + 2, rect.y + 2, rect.width - 4, EditorTheme.NODE_HEADER_HEIGHT - 2)
        g2d.color = EditorTheme.TITLE_TEXT

        val titleFont = EditorTheme.UI_FONT_BOLD.deriveFont((9 * zoom).toFloat().coerceAtMost(11f))
        g2d.font = titleFont

        val titleText = "${node.definition.category.displayName}: ${node.definition.displayName}"
        val fm = g2d.fontMetrics
        g2d.drawString(
            titleText,
            rect.x + 6,
            rect.y + EditorTheme.NODE_HEADER_HEIGHT - 7
        )

        // 端口
        g2d.font = EditorTheme.UI_FONT.deriveFont((9 * zoom).toFloat().coerceAtMost(11f))

        // 输入端口
        for (port in node.definition.inputs) {
            val portPos = getInputPortPos(node, port, rect)
            drawPort(g2d, portPos.x.toInt(), portPos.y.toInt(), port)
            // 端口名
            g2d.color = EditorTheme.BLACK
            g2d.drawString(port.name, portPos.x.toInt() + 10, portPos.y.toInt() + 4)
        }

        // 输出端口
        for (port in node.definition.outputs) {
            val portPos = getOutputPortPos(node, port, rect)
            drawPort(g2d, portPos.x.toInt(), portPos.y.toInt(), port)
            // 端口名（右侧）
            g2d.color = EditorTheme.BLACK
            val textWidth = g2d.fontMetrics.stringWidth(port.name)
            g2d.drawString(port.name, portPos.x.toInt() - textWidth - 8, portPos.y.toInt() + 4)
        }

        // 参数值显示
        g2d.font = EditorTheme.UI_FONT.deriveFont((8 * zoom).toFloat().coerceAtMost(9f))
        g2d.color = EditorTheme.DARKEST
        var paramY = rect.y + EditorTheme.NODE_HEADER_HEIGHT + 18
        val totalInputs = node.definition.inputs.size
        val totalOutputs = node.definition.outputs.size
        val maxPorts = Math.max(totalInputs, totalOutputs)
        val contentStart = rect.y + EditorTheme.NODE_HEADER_HEIGHT + Math.max(maxPorts * 18, 10)

        if (node.definition.parameters.isNotEmpty()) {
            for (param in node.definition.parameters) {
                val value = node.getParameter(param.id) ?: param.defaultValue
                if (value != null) {
                    val displayText = "${param.name}: ${formatParamValue(value)}"
                    val displayY = contentStart + (node.definition.parameters.indexOf(param) * 15) + 15
                    g2d.drawString(displayText, rect.x + 8, displayY)
                }
            }
        }
    }

    private fun drawPort(g2d: Graphics2D, x: Int, y: Int, port: NodePort) {
        val r = EditorTheme.NODE_PORT_RADIUS
        g2d.color = if (port.isExecution) EditorTheme.NODE_EXEC_PORT else EditorTheme.NODE_PORT

        if (port.isInput) {
            // 输入端口：凹陷方形
            g2d.fillRect(x - r, y - r, r * 2, r * 2)
            g2d.color = EditorTheme.HIGHLIGHT
            g2d.drawRect(x - r, y - r, r * 2, r * 2)
        } else {
            // 输出端口：凸起圆形
            g2d.fillOval(x - r, y - r, r * 2, r * 2)
            g2d.color = EditorTheme.HIGHLIGHT
            g2d.drawOval(x - r, y - r, r * 2, r * 2)
        }
    }

    private fun drawRuntimeHighlight(g2d: Graphics2D) {
        val runtime = CustomizableController.getRuntime() ?: return
        val currentNodeId = runtime.currentNodeId ?: return
        val node = graph.getNode(currentNodeId) ?: return
        val rect = getNodeRect(node)

        g2d.color = Color(255, 255, 0, 80)
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height)
    }

    // ===== 几何计算 =====

    private fun getNodeRect(node: LogicNode): Rectangle {
        val maxPorts = Math.max(node.definition.inputs.size, node.definition.outputs.size)
        val contentHeight = Math.max(maxPorts * 18, 20) + node.definition.parameters.size * 15 + 20
        val height = Math.max(EditorTheme.NODE_MIN_HEIGHT, EditorTheme.NODE_HEADER_HEIGHT + contentHeight)
        return Rectangle(
            node.x.toInt(),
            node.y.toInt(),
            EditorTheme.NODE_WIDTH,
            height
        )
    }

    private fun getInputPortPos(node: LogicNode, port: NodePort, rect: Rectangle): Point {
        val index = node.definition.inputs.indexOf(port)
        val y = rect.y + EditorTheme.NODE_HEADER_HEIGHT + 10 + index * 18
        return Point(rect.x, y)
    }

    private fun getOutputPortPos(node: LogicNode, port: NodePort, rect: Rectangle): Point {
        val index = node.definition.outputs.indexOf(port)
        val y = rect.y + EditorTheme.NODE_HEADER_HEIGHT + 10 + index * 18
        return Point(rect.x + rect.width, y)
    }

    private fun findNodeAt(canvasX: Double, canvasY: Double): LogicNode? {
        for (node in graph.nodes.values.reversed()) {
            val rect = getNodeRect(node)
            if (canvasX >= rect.x && canvasX <= rect.x + rect.width &&
                canvasY >= rect.y && canvasY <= rect.y + rect.height
            ) {
                return node
            }
        }
        return null
    }

    private fun screenToCanvas(screenX: Int, screenY: Int): Pair<Double, Double> {
        val cx = (screenX.toDouble() - offsetX) / zoom
        val cy = (screenY.toDouble() - offsetY) / zoom
        return Pair(cx, cy)
    }

    private fun updateCursor(mx: Double, my: Double, sx: Int, sy: Int) {
        val onPort = isOnPort(mx, my)
        cursor = if (onPort) Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
        else Cursor.getDefaultCursor()
    }

    private fun isOnPort(canvasX: Double, canvasY: Double): Boolean {
        for (node in graph.nodes.values) {
            val rect = getNodeRect(node)
            for (port in node.definition.inputs) {
                val pos = getInputPortPos(node, port, rect)
                if (Point(canvasX.toInt(), canvasY.toInt()).distance(pos.x.toDouble(), pos.y.toDouble()) <= EditorTheme.NODE_PORT_RADIUS + 4)
                    return true
            }
            for (port in node.definition.outputs) {
                val pos = getOutputPortPos(node, port, rect)
                if (Point(canvasX.toInt(), canvasY.toInt()).distance(pos.x.toDouble(), pos.y.toDouble()) <= EditorTheme.NODE_PORT_RADIUS + 4)
                    return true
            }
        }
        return false
    }

    // ===== 辅助 =====

    private fun formatParamValue(value: Any?): String {
        return when (value) {
            is Float -> String.format("%.2f", value)
            is Double -> String.format("%.2f", value)
            is Boolean -> if (value) "ON" else "OFF"
            else -> value.toString()
        }
    }

    private fun saveUndoState() {
        // 通知编辑器保存撤销状态
        val topLevel = topLevelAncestor
        if (topLevel is LogicEditor) {
            topLevel.saveUndoState()
        }
    }
}