package net.ccbluex.liquidbounce.utils.customizable.editor

import net.ccbluex.liquidbounce.utils.customizable.logic.*
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.dnd.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.BevelBorder
import javax.swing.border.EmptyBorder

/**
 * 左侧模块面板
 *
 * 显示分类模块列表，可以拖拽到中间画布
 * Win95 风格
 */
class ModulePanel : JPanel() {

    private var selectedCategory: NodeCategory? = null
    private var expandedCategories = mutableSetOf<NodeCategory>()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = EditorTheme.BACKGROUND
        border = EmptyBorder(2, 2, 2, 2)

        // 默认展开几个分类
        expandedCategories.addAll(listOf(
            NodeCategory.EVENT,
            NodeCategory.TARGET,
            NodeCategory.ROTATION,
            NodeCategory.BEHAVIOR
        ))

        buildPanel()
    }

    private fun buildPanel() {
        removeAll()

        // 按分类分组
        val grouped = NodeRegistry.getDefinitions().groupBy { it.category }

        for (category in NodeCategory.values()) {
            val nodes = grouped[category] ?: continue
            if (nodes.isEmpty()) continue

            val isExpanded = category in expandedCategories

            // 分类标题按钮
            val categoryBtn = createCategoryButton(category, isExpanded)
            add(categoryBtn)

            // 分类内容
            if (isExpanded) {
                val contentPanel = JPanel()
                contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
                contentPanel.background = EditorTheme.WHITE
                contentPanel.border = BevelBorder(BevelBorder.LOWERED)

                for (def in nodes) {
                    val nodeBtn = createNodeButton(def)
                    contentPanel.add(nodeBtn)
                }

                add(contentPanel)
            }

            add(Box.createVerticalStrut(2))
        }

        revalidate()
        repaint()
    }

    private fun createCategoryButton(category: NodeCategory, isExpanded: Boolean): JButton {
        val text = "${if (isExpanded) "▼" else "►"} ${category.displayName} (${NodeRegistry.getDefinitions().count { it.category == category }})"
        val btn = JButton(text)
        btn.font = EditorTheme.UI_FONT_BOLD
        btn.background = EditorTheme.TITLE_BAR_ACTIVE
        btn.foreground = EditorTheme.TITLE_TEXT
        btn.border = BevelBorder(BevelBorder.RAISED)
        btn.isFocusPainted = false
        btn.horizontalAlignment = SwingConstants.LEFT
        btn.maximumSize = Dimension(Int.MAX_VALUE, 24)
        btn.minimumSize = Dimension(100, 24)
        btn.alignmentX = Component.LEFT_ALIGNMENT

        btn.addActionListener {
            if (category in expandedCategories) {
                expandedCategories.remove(category)
            } else {
                expandedCategories.add(category)
            }
            buildPanel()
        }

        return btn
    }

    private fun createNodeButton(def: NodeDefinition): JComponent {
        val panel = JPanel(BorderLayout())
        panel.background = EditorTheme.getNodeCategoryColor(def.category.name)
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, EditorTheme.SHADOW),
            EmptyBorder(1, 4, 1, 4)
        )
        panel.maximumSize = Dimension(Int.MAX_VALUE, 22)
        panel.minimumSize = Dimension(100, 22)
        panel.alignmentX = Component.LEFT_ALIGNMENT
        panel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        val label = JLabel(def.displayName)
        label.font = EditorTheme.UI_FONT
        label.background = panel.background
        panel.add(label, BorderLayout.WEST)

        // 点击模块直接添加到画布
        panel.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                panel.background = panel.background.darker()
            }

            override fun mouseExited(e: MouseEvent) {
                panel.background = EditorTheme.getNodeCategoryColor(def.category.name)
            }

            override fun mousePressed(e: MouseEvent) {
                addNodeToCanvas(def)
            }
        })

        return panel
    }

    private fun addNodeToCanvas(def: NodeDefinition) {
        // 找到 EditorCanvas 并添加新节点
        val canvas = findCanvas()
        if (canvas != null) {
            canvas.addNodeAtCenter(def)
        }
    }

    private fun findCanvas(): EditorCanvas? {
        var parent = parent
        while (parent != null) {
            if (parent is JViewport) {
                parent = parent.parent
                continue
            }
            if (parent is JScrollPane) {
                parent = parent.parent
                continue
            }
            if (parent is JSplitPane) {
                parent = parent.parent
                continue
            }
            break
        }

        // 从顶层窗口查找
        val root = SwingUtilities.getWindowAncestor(this)
        if (root is Frame) {
            return findCanvasRecursive(root)
        }
        return null
    }

    private fun findCanvasRecursive(container: Container): EditorCanvas? {
        for (comp in container.components) {
            if (comp is EditorCanvas) return comp
            if (comp is Container) {
                val result = findCanvasRecursive(comp)
                if (result != null) return result
            }
        }
        return null
    }
}