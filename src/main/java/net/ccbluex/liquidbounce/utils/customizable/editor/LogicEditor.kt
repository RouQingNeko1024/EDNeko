package net.ccbluex.liquidbounce.utils.customizable.editor

import net.ccbluex.liquidbounce.utils.customizable.CustomizableController
import net.ccbluex.liquidbounce.utils.customizable.logic.*
import java.awt.*
import java.awt.event.*
import java.io.File
import java.util.UUID
import javax.swing.*
import javax.swing.border.BevelBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder

/**
 * Win95 风格逻辑编辑器主窗口
 */
class LogicEditor(private val graph: LogicGraph) : JFrame("EDNeko Customizable Logic Editor") {

    private var currentFile: File? = null
    private var hasUnsavedChanges = false

    // 主面板
    private val canvas = EditorCanvas(graph)
    private val modulePanel = ModulePanel()
    private val simulationPanel = SimulationPanel()

    // 状态栏
    private val statusLabel = JLabel("Ready")

    // 撤消/重做栈
    private val undoStack = mutableListOf<LogicGraph>()
    private val redoStack = mutableListOf<LogicGraph>()
    private val MAX_UNDO = 50

    init {
        setupWindow()
        setupMenuBar()
        setupToolbar()
        setupMainLayout()
        setupStatusBar()
        setupEventHandlers()

        saveUndoState()
        updateTitle()

        // Win95 图标
        try {
            iconImage = Toolkit.getDefaultToolkit().getImage(javaClass.classLoader.getResource("assets/minecraft/edneko/icon.png"))
        } catch (e: Exception) { }

        pack()
        setSize(1200, 800)
        setLocationRelativeTo(null)
    }

    fun start() {
        isVisible = true
    }

    fun close() {
        if (hasUnsavedChanges) {
            val result = JOptionPane.showConfirmDialog(
                this,
                "Save changes before closing?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
            )
            if (result == JOptionPane.CANCEL_OPTION) return
            if (result == JOptionPane.YES_OPTION) saveFile()
        }
        dispose()
    }

    override fun toFront() {
        state = Frame.NORMAL
        super.toFront()
    }

    // ===== 窗口设置 =====
    private fun setupWindow() {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        background = EditorTheme.BACKGROUND
        // Win95 风格边框
        rootPane.border = BevelBorder(BevelBorder.RAISED)
    }

    // ===== 菜单栏 =====
    private fun setupMenuBar() {
        val menuBar = JMenuBar()
        menuBar.background = EditorTheme.BACKGROUND

        // File 菜单
        val fileMenu = JMenu("File")
        fileMenu.font = EditorTheme.UI_FONT
        addMenuItem(fileMenu, "New", KeyEvent.VK_N, { newFile() })
        addMenuItem(fileMenu, "Open...", KeyEvent.VK_O, { openFile() })
        fileMenu.addSeparator()
        addMenuItem(fileMenu, "Save", KeyEvent.VK_S, { saveFile() })
        addMenuItem(fileMenu, "Save As...", KeyEvent.VK_A, { saveAsFile() })
        fileMenu.addSeparator()
        addMenuItem(fileMenu, "Exit", KeyEvent.VK_X, { close() })

        // Edit 菜单
        val editMenu = JMenu("Edit")
        editMenu.font = EditorTheme.UI_FONT
        addMenuItem(editMenu, "Undo", KeyEvent.VK_Z, { undo() })
        addMenuItem(editMenu, "Redo", KeyEvent.VK_Y, { redo() })
        editMenu.addSeparator()
        addMenuItem(editMenu, "Delete Selected", KeyEvent.VK_D, { canvas.deleteSelected() })
        addMenuItem(editMenu, "Duplicate Selected", KeyEvent.VK_P, { canvas.duplicateSelected() })

        // View 菜单
        val viewMenu = JMenu("View")
        viewMenu.font = EditorTheme.UI_FONT
        addMenuItem(viewMenu, "Zoom In", KeyEvent.VK_PLUS, { canvas.zoomIn() })
        addMenuItem(viewMenu, "Zoom Out", KeyEvent.VK_MINUS, { canvas.zoomOut() })
        addMenuItem(viewMenu, "Reset View", KeyEvent.VK_R, { canvas.resetView() })

        // Logic 菜单
        val logicMenu = JMenu("Logic")
        logicMenu.font = EditorTheme.UI_FONT
        addMenuItem(logicMenu, "Validate", KeyEvent.VK_V, { validateLogic() })
        addMenuItem(logicMenu, "Reset Runtime", KeyEvent.VK_T, { resetRuntime() })
        logicMenu.addSeparator()
        addMenuItem(logicMenu, "Reload from KillAura", KeyEvent.VK_R, { reloadFromKA() })

        // Tools 菜单
        val toolsMenu = JMenu("Tools")
        toolsMenu.font = EditorTheme.UI_FONT
        addMenuItem(toolsMenu, "Settings...", KeyEvent.VK_S, { showSettings() })
        addMenuItem(toolsMenu, "Debug Panel", KeyEvent.VK_D, { toggleDebugPanel() })

        menuBar.add(fileMenu)
        menuBar.add(editMenu)
        menuBar.add(viewMenu)
        menuBar.add(logicMenu)
        menuBar.add(toolsMenu)

        jMenuBar = menuBar
    }

    private fun addMenuItem(menu: JMenu, text: String, keyEvent: Int, action: () -> Unit) {
        val item = JMenuItem(text)
        item.font = EditorTheme.UI_FONT
        item.background = EditorTheme.BACKGROUND
        item.accelerator = KeyStroke.getKeyStroke(keyEvent, InputEvent.CTRL_MASK)
        item.addActionListener { action() }
        menu.add(item)
    }

    // ===== 工具栏 =====
    private fun setupToolbar() {
        val toolbar = JPanel()
        toolbar.layout = FlowLayout(FlowLayout.LEFT, 2, 2)
        toolbar.background = EditorTheme.BACKGROUND
        toolbar.border = BevelBorder(BevelBorder.RAISED)

        // 使用最简单的按钮
        toolbar.add(createToolButton("New", { newFile() }))
        toolbar.add(createToolButton("Save", { saveFile() }))
        toolbar.add(createToolButton("Load", { openFile() }))
        toolbar.add(createToolSeparator())
        toolbar.add(createToolButton("Undo", { undo() }))
        toolbar.add(createToolButton("Redo", { redo() }))
        toolbar.add(createToolSeparator())
        toolbar.add(createToolButton("Delete", { canvas.deleteSelected() }))
        toolbar.add(createToolSeparator())
        toolbar.add(createToolButton("Validate", { validateLogic() }))
        toolbar.add(createToolButton("Reset", { resetRuntime() }))

        add(toolbar, BorderLayout.NORTH)
    }

    private fun createToolButton(text: String, action: () -> Unit): JButton {
        val btn = JButton(text)
        btn.font = EditorTheme.UI_FONT
        btn.background = EditorTheme.BACKGROUND
        btn.margin = Insets(1, 6, 1, 6)
        btn.isFocusPainted = false
        btn.addActionListener { action() }
        return btn
    }

    private fun createToolSeparator(): JComponent {
        val sep = JSeparator(SwingConstants.VERTICAL)
        sep.preferredSize = Dimension(4, 22)
        sep.maximumSize = Dimension(4, 22)
        return sep
    }

    // ===== 主布局 =====
    private fun setupMainLayout() {
        // 拆分面板
        val leftSplit = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            createLeftPanel(),
            createCenterPanel()
        )
        leftSplit.dividerLocation = 200
        leftSplit.dividerSize = 3
        leftSplit.border = null
        leftSplit.background = EditorTheme.BACKGROUND

        val mainSplit = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            leftSplit,
            createRightPanel()
        )
        mainSplit.dividerLocation = 850
        mainSplit.dividerSize = 3
        mainSplit.border = null
        mainSplit.background = EditorTheme.BACKGROUND

        add(mainSplit, BorderLayout.CENTER)
    }

    private fun createLeftPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.background = EditorTheme.BACKGROUND
        panel.preferredSize = Dimension(200, 0)

        // 标题
        val title = JLabel("  Modules", SwingConstants.LEFT)
        title.font = EditorTheme.UI_FONT_BOLD
        title.background = EditorTheme.TITLE_BAR_ACTIVE
        title.foreground = EditorTheme.TITLE_TEXT
        title.border = BevelBorder(BevelBorder.RAISED)
        panel.add(title, BorderLayout.NORTH)

        panel.add(JScrollPane(modulePanel), BorderLayout.CENTER)

        return panel
    }

    private fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.background = EditorTheme.BACKGROUND

        val canvasScroll = JScrollPane(
            canvas,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        canvasScroll.border = BevelBorder(BevelBorder.LOWERED)
        canvasScroll.background = EditorTheme.CANVAS_BACKGROUND

        // 最小尺寸
        canvasScroll.viewport.view = canvas
        canvas.setSize(2000, 2000)

        panel.add(canvasScroll, BorderLayout.CENTER)
        return panel
    }

    private fun createRightPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.background = EditorTheme.BACKGROUND
        panel.preferredSize = Dimension(300, 0)

        // 仿真面板
        val simTitle = JLabel("  Runtime Preview", SwingConstants.LEFT)
        simTitle.font = EditorTheme.UI_FONT_BOLD
        simTitle.background = EditorTheme.TITLE_BAR_ACTIVE
        simTitle.foreground = EditorTheme.TITLE_TEXT
        simTitle.border = BevelBorder(BevelBorder.RAISED)
        panel.add(simTitle, BorderLayout.NORTH)

        panel.add(JScrollPane(simulationPanel), BorderLayout.CENTER)

        return panel
    }

    // ===== 状态栏 =====
    private fun setupStatusBar() {
        val statusBar = JPanel(BorderLayout())
        statusBar.background = EditorTheme.BACKGROUND
        statusBar.border = BevelBorder(BevelBorder.LOWERED)

        statusLabel.font = EditorTheme.UI_FONT
        statusLabel.background = EditorTheme.BACKGROUND
        statusLabel.border = EmptyBorder(1, 4, 1, 4)

        statusBar.add(statusLabel, BorderLayout.WEST)
        add(statusBar, BorderLayout.SOUTH)
    }

    // ===== 事件处理 =====
    private fun setupEventHandlers() {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                close()
            }
        })

        // 定时更新仿真面板
        Timer(100) {
            if (isVisible) {
                simulationPanel.update(CustomizableController.getDebugInfo())
                canvas.repaint()
            }
        }.start()
    }

    // ===== 文件操作 =====
    private fun newFile() {
        if (hasUnsavedChanges) {
            val result = JOptionPane.showConfirmDialog(
                this, "Save changes?", "New", JOptionPane.YES_NO_CANCEL_OPTION
            )
            if (result == JOptionPane.CANCEL_OPTION) return
            if (result == JOptionPane.YES_OPTION) saveFile()
        }

        val newGraph = LogicGraph(id = UUID.randomUUID().toString(), name = "Untitled")
        CustomizableController.setGraph(newGraph)
        canvas.loadGraph(newGraph)
        currentFile = null
        hasUnsavedChanges = false
        saveUndoState()
        updateTitle()
        statusLabel.text = "New file created"
    }

    private fun openFile() {
        val fileChooser = JFileChooser(CustomizableController.saveDir)
        fileChooser.font = EditorTheme.UI_FONT
        fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
            "Logic Files (*.json)", "json"
        )

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            val loadedGraph = LogicSerializer.load(file)
            if (loadedGraph != null) {
                CustomizableController.setGraph(loadedGraph)
                canvas.loadGraph(loadedGraph)
                currentFile = file
                hasUnsavedChanges = false
                saveUndoState()
                updateTitle()
                statusLabel.text = "Loaded: ${file.name}"
            } else {
                JOptionPane.showMessageDialog(
                    this, "Failed to load file: ${file.name}",
                    "Error", JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun saveFile() {
        if (currentFile != null) {
            saveToFile(currentFile!!)
        } else {
            saveAsFile()
        }
    }

    private fun saveAsFile() {
        val fileChooser = JFileChooser(CustomizableController.saveDir)
        fileChooser.font = EditorTheme.UI_FONT
        fileChooser.selectedFile = File(CustomizableController.saveDir, "${graph.name}.json")
        fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
            "Logic Files (*.json)", "json"
        )

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile
            if (!file.name.endsWith(".json")) {
                file = File(file.absolutePath + ".json")
            }
            saveToFile(file)
        }
    }

    private fun saveToFile(file: File) {
        if (LogicSerializer.save(graph, file)) {
            currentFile = file
            hasUnsavedChanges = false
            updateTitle()
            statusLabel.text = "Saved: ${file.name}"
        } else {
            JOptionPane.showMessageDialog(
                this, "Failed to save file!",
                "Error", JOptionPane.ERROR_MESSAGE
            )
        }
    }

    // ===== 编辑操作 =====
    private fun undo() {
        if (undoStack.size > 1) {
            redoStack.add(0, undoStack.removeAt(undoStack.size - 1))
            val state = undoStack.last().copy()
            CustomizableController.setGraph(state)
            canvas.loadGraph(state)
            hasUnsavedChanges = true
            updateTitle()
            statusLabel.text = "Undo"
        }
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) {
            val state = redoStack.removeAt(0)
            undoStack.add(state)
            CustomizableController.setGraph(state)
            canvas.loadGraph(state)
            hasUnsavedChanges = true
            updateTitle()
            statusLabel.text = "Redo"
        }
    }

    fun saveUndoState() {
        undoStack.add(graph.copy())
        if (undoStack.size > MAX_UNDO) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    // ===== 逻辑操作 =====
    private fun validateLogic() {
        val errors = CustomizableController.validate()
        if (errors.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Logic is valid!",
                "Validation", JOptionPane.INFORMATION_MESSAGE
            )
            statusLabel.text = "Validation: OK"
        } else {
            val msg = errors.joinToString("\n")
            JOptionPane.showMessageDialog(
                this, msg,
                "Validation Errors", JOptionPane.WARNING_MESSAGE
            )
            statusLabel.text = "Validation: ${errors.size} error(s)"
        }
    }

    private fun resetRuntime() {
        CustomizableController.resetRuntime()
        statusLabel.text = "Runtime reset"
    }

    private fun reloadFromKA() {
        val g = CustomizableController.getGraph()
        if (g != null) {
            canvas.loadGraph(g)
            statusLabel.text = "Reloaded from KillAura"
        }
    }

    private fun showSettings() {
        JOptionPane.showMessageDialog(
            this,
            "Customizable Logic Editor v1.0\nSave location: ${CustomizableController.saveDir.absolutePath}",
            "Settings",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun toggleDebugPanel() {
        // 简单实现：显示 debug 日志对话框
        val runtime = CustomizableController.getRuntime()
        if (runtime != null) {
            val log = runtime.debugLog.joinToString("\n")
            JOptionPane.showMessageDialog(
                this,
                if (log.isEmpty()) "No debug output" else log,
                "Debug Log",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }

    private fun updateTitle() {
        val fileInfo = currentFile?.name ?: "Untitled"
        val modified = if (hasUnsavedChanges) " *" else ""
        title = "EDNeko Customizable Logic Editor - $fileInfo$modified"
    }
}