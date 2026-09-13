package net.ccbluex.liquidbounce.utils.customizable.editor

import net.ccbluex.liquidbounce.utils.customizable.CustomizableController
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.BevelBorder
import javax.swing.border.EmptyBorder
import kotlin.math.*

/**
 * 右侧仿真面板
 *
 * 实时显示逻辑图形运行状态
 * 包含模拟视角和运行时状态
 */
class SimulationPanel : JPanel() {

    private var debugInfo: Map<String, String> = emptyMap()

    // 模拟滑块
    private val playerYawSlider = JSlider(JSlider.HORIZONTAL, -180, 180, 0)
    private val playerPitchSlider = JSlider(JSlider.HORIZONTAL, -90, 90, 0)
    private val targetYawSlider = JSlider(JSlider.HORIZONTAL, -180, 180, 45)
    private val targetPitchSlider = JSlider(JSlider.HORIZONTAL, -90, 90, 10)
    private val distanceSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 30)

    // 模拟视图
    private val simulationView = SimulationView()

    // 运行时状态标签
    private val stateLabels = mutableListOf<JLabel>()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = EditorTheme.BACKGROUND
        border = EmptyBorder(4, 4, 4, 4)

        buildSimulationView()
        buildSliders()
        buildRuntimeState()
    }

    private fun buildSimulationView() {
        // 模拟视图标题
        val viewTitle = createSectionHeader("Simulation View")
        add(viewTitle)

        simulationView.preferredSize = Dimension(280, 200)
        simulationView.minimumSize = Dimension(200, 150)
        simulationView.maximumSize = Dimension(Int.MAX_VALUE, 250)
        simulationView.background = Color(240, 240, 240)
        simulationView.border = BevelBorder(BevelBorder.LOWERED)
        simulationView.alignmentX = Component.LEFT_ALIGNMENT
        add(simulationView)
        add(Box.createVerticalStrut(8))
    }

    private fun buildSliders() {
        val sliderPanel = JPanel()
        sliderPanel.layout = BoxLayout(sliderPanel, BoxLayout.Y_AXIS)
        sliderPanel.background = EditorTheme.BACKGROUND
        sliderPanel.border = BevelBorder(BevelBorder.RAISED)
        sliderPanel.alignmentX = Component.LEFT_ALIGNMENT

        sliderPanel.add(createSliderRow("Player Yaw:", playerYawSlider))
        sliderPanel.add(createSliderRow("Player Pitch:", playerPitchSlider))
        sliderPanel.add(createSliderRow("Target Yaw:", targetYawSlider))
        sliderPanel.add(createSliderRow("Target Pitch:", targetPitchSlider))
        sliderPanel.add(createSliderRow("Distance:", distanceSlider))

        // 添加值标签
        val valuePanel = JPanel(GridLayout(1, 5, 2, 0))
        valuePanel.background = EditorTheme.BACKGROUND
        val labels = listOf(
            JLabel("Yaw: ${playerYawSlider.value}°"),
            JLabel("Pitch: ${playerPitchSlider.value}°"),
            JLabel("TY: ${targetYawSlider.value}°"),
            JLabel("TP: ${targetPitchSlider.value}°"),
            JLabel("Dist: ${distanceSlider.value / 10.0}")
        )
        labels.forEach { it.font = EditorTheme.UI_FONT; it.foreground = EditorTheme.DARKEST }
        labels.forEach { valuePanel.add(it) }

        // 更新值和视图
        val updateAction = ActionListener {
            labels[0].text = "Yaw: ${playerYawSlider.value}°"
            labels[1].text = "Pitch: ${playerPitchSlider.value}°"
            labels[2].text = "TY: ${targetYawSlider.value}°"
            labels[3].text = "TP: ${targetPitchSlider.value}°"
            labels[4].text = "Dist: ${String.format("%.1f", distanceSlider.value / 10.0)}"
            simulationView.updateValues(
                playerYawSlider.value.toFloat(),
                playerPitchSlider.value.toFloat(),
                targetYawSlider.value.toFloat(),
                targetPitchSlider.value.toFloat(),
                distanceSlider.value.toDouble() / 10.0
            )
        }

        playerYawSlider.addChangeListener { updateAction.actionPerformed(null) }
        playerPitchSlider.addChangeListener { updateAction.actionPerformed(null) }
        targetYawSlider.addChangeListener { updateAction.actionPerformed(null) }
        targetPitchSlider.addChangeListener { updateAction.actionPerformed(null) }
        distanceSlider.addChangeListener { updateAction.actionPerformed(null) }

        sliderPanel.add(valuePanel)
        add(sliderPanel)
        add(Box.createVerticalStrut(8))
    }

    private fun createSliderRow(label: String, slider: JSlider): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = EditorTheme.BACKGROUND

        val lbl = JLabel(label)
        lbl.font = EditorTheme.UI_FONT
        lbl.preferredSize = Dimension(100, 20)
        panel.add(lbl, BorderLayout.WEST)

        slider.font = EditorTheme.UI_FONT
        slider.background = EditorTheme.BACKGROUND
        slider.preferredSize = Dimension(160, 20)
        slider.majorTickSpacing = 90
        slider.minorTickSpacing = 10
        slider.paintTicks = true
        slider.paintLabels = false
        panel.add(slider, BorderLayout.CENTER)

        return panel
    }

    private fun buildRuntimeState() {
        val stateTitle = createSectionHeader("Runtime State")
        add(stateTitle)

        val statePanel = JPanel()
        statePanel.layout = BoxLayout(statePanel, BoxLayout.Y_AXIS)
        statePanel.background = EditorTheme.WHITE
        statePanel.border = BevelBorder(BevelBorder.LOWERED)
        statePanel.alignmentX = Component.LEFT_ALIGNMENT

        // 预定义状态标签
        val stateItems = listOf(
            "Target", "Distance", "Health",
            "Player Yaw", "Player Pitch",
            "Target Yaw", "Target Pitch",
            "Yaw Diff", "Pitch Diff",
            "Noise", "Inertia",
            "Rotation Speed",
            "Target Align",
            "Attack", "Block",
            "CPS Left", "CPS Right",
            "Execution Time",
            "Current Node",
            "Debug"
        )

        for (item in stateItems) {
            val row = JPanel(BorderLayout())
            row.background = EditorTheme.WHITE

            val nameLabel = JLabel(" $item:")
            nameLabel.font = EditorTheme.UI_FONT_BOLD
            nameLabel.preferredSize = Dimension(110, 18)
            row.add(nameLabel, BorderLayout.WEST)

            val valueLabel = JLabel(" -")
            valueLabel.font = EditorTheme.MONO_FONT
            valueLabel.foreground = Color(0, 0, 128)
            row.add(valueLabel, BorderLayout.CENTER)

            statePanel.add(row)
            stateLabels.add(valueLabel)
        }

        val scrollPane = JScrollPane(statePanel)
        scrollPane.border = null
        scrollPane.alignmentX = Component.LEFT_ALIGNMENT
        scrollPane.preferredSize = Dimension(280, 300)
        add(scrollPane)
    }

    private fun createSectionHeader(text: String): JLabel {
        val label = JLabel(" $text")
        label.font = EditorTheme.UI_FONT_BOLD
        label.foreground = EditorTheme.TITLE_TEXT
        label.background = EditorTheme.TITLE_BAR_ACTIVE
        label.border = BevelBorder(BevelBorder.RAISED)
        label.alignmentX = Component.LEFT_ALIGNMENT
        label.maximumSize = Dimension(Int.MAX_VALUE, 22)
        label.minimumSize = Dimension(100, 22)
        return label
    }

    /**
     * 更新调试信息
     */
    fun update(info: Map<String, String>) {
        debugInfo = info

        // 更新状态标签
        val stateIndices = mapOf(
            "Target" to 0, "Distance" to 1, "Health" to 2,
            "Player Yaw" to 3, "Player Pitch" to 4,
            "Target Yaw" to 5, "Target Pitch" to 6,
            "Yaw Diff" to 7, "Pitch Diff" to 8,
            "Noise" to 9, "Inertia" to 10,
            "Rotation Speed" to 11,
            "Target Align" to 12,
            "Attack" to 13, "Block" to 14,
            "CPS Left" to 15, "CPS Right" to 16,
            "Execution Time" to 17,
            "Current Node" to 18,
            "Debug" to 19
        )

        // 从 info 和运行时提取数据
        val runtime = CustomizableController.getRuntime()
        val output = CustomizableController.getLastOutput()

        val values = arrayOf(
            info["target"] ?: "None",
            info["targetDistance"] ?: "N/A",
            output?.let { String.format("%.1f", runtime?.state?.target?.health ?: 0.0) } ?: "0.0",
            info["playerYaw"] ?: "0.0",
            info["playerPitch"] ?: "0.0",
            info["targetYaw"] ?: "0.0",
            info["targetPitch"] ?: "0.0",
            calcDiff(info),
            calcPitchDiff(info),
            info["noiseValue"] ?: "0.0000",
            info["inertiaValue"] ?: "0.0000",
            String.format("%.1f", runtime?.state?.rotationSpeed ?: 90f),
            output?.let { if (it.hasRotation) "TRUE" else "FALSE" } ?: "FALSE",
            output?.let { if (it.shouldAttack) "TRUE" else "FALSE" } ?: "FALSE",
            output?.let { if (it.shouldBlock) "TRUE" else "FALSE" } ?: "FALSE",
            String.format("%.1f", output?.cpsValue ?: 0f),
            String.format("%.1f", output?.cpsValue?.div(1.5f) ?: 0f),
            info["executionTime"] ?: "0 ms",
            info["currentNode"] ?: "None",
            info["debugLog"]?.take(40) ?: "-"
        )

        for ((index, label) in stateLabels.withIndex()) {
            if (index < values.size) {
                label.text = " ${values[index]}"
            }
        }

        simulationView.repaint()
    }

    private fun calcDiff(info: Map<String, String>): String {
        val py = info["playerYaw"]?.toFloatOrNull() ?: return "0.0"
        val ty = info["targetYaw"]?.toFloatOrNull() ?: return "0.0"
        var diff = ty - py
        diff = ((diff + 180) % 360) - 180
        return String.format("%.2f", diff)
    }

    private fun calcPitchDiff(info: Map<String, String>): String {
        val pp = info["playerPitch"]?.toFloatOrNull() ?: return "0.0"
        val tp = info["targetPitch"]?.toFloatOrNull() ?: return "0.0"
        return String.format("%.2f", tp - pp)
    }

    /**
     * 模拟视角视图
     */
    inner class SimulationView : JComponent() {
        private var playerYaw = 0f
        private var playerPitch = 0f
        private var targetYaw = 45f
        private var targetPitch = 10f
        private var distance = 3.0

        fun updateValues(pYaw: Float, pPitch: Float, tYaw: Float, tPitch: Float, dist: Double) {
            playerYaw = pYaw
            playerPitch = pPitch
            targetYaw = tYaw
            targetPitch = tPitch
            distance = dist
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            val w = width.toDouble()
            val h = height.toDouble()
            val cx = w / 2
            val cy = h / 2

            // 背景
            g2d.color = Color(200, 200, 200)
            g2d.fillRect(0, 0, width, height)

            // 网格
            g2d.color = Color(180, 180, 180)
            for (i in 0..10) {
                val x = (i * w / 10).toInt()
                g2d.drawLine(x, 0, x, height)
                val y = (i * h / 10).toInt()
                g2d.drawLine(0, y, width, y)
            }

            // 十字准心
            g2d.color = Color(100, 100, 100)
            g2d.stroke = BasicStroke(1f)
            g2d.drawLine((cx - 20).toInt(), cy.toInt(), (cx + 20).toInt(), cy.toInt())
            g2d.drawLine(cx.toInt(), (cy - 20).toInt(), cx.toInt(), (cy + 20).toInt())

            // 玩家位置（中心）
            val playerSize = 12.0
            g2d.color = Color(0, 100, 200)
            g2d.fillOval((cx - playerSize / 2).toInt(), (cy - playerSize / 2).toInt(),
                playerSize.toInt(), playerSize.toInt())
            g2d.color = Color.BLACK
            g2d.drawOval((cx - playerSize / 2).toInt(), (cy - playerSize / 2).toInt(),
                playerSize.toInt(), playerSize.toInt())

            // 玩家朝向
            val playerAngle = Math.toRadians(-playerYaw.toDouble())
            val playerDirLen = 30.0
            g2d.color = Color(0, 150, 0)
            g2d.stroke = BasicStroke(2f)
            g2d.drawLine(
                cx.toInt(), cy.toInt(),
                (cx + cos(playerAngle) * playerDirLen).toInt(),
                (cy + sin(playerAngle) * playerDirLen).toInt()
            )

            // 目标位置
            val targetAngle = Math.toRadians(-targetYaw.toDouble())
            val viewScale = minOf(w, h) / 2.5 * 0.8
            val targetDist = distance.coerceAtMost(10.0) / 10.0
            val targetX = cx + cos(targetAngle) * viewScale * targetDist
            val targetY = cy + sin(targetAngle) * viewScale * targetDist

            val targetSize = 10.0
            g2d.color = Color(200, 0, 0)
            g2d.fillOval((targetX - targetSize / 2).toInt(), (targetY - targetSize / 2).toInt(),
                targetSize.toInt(), targetSize.toInt())
            g2d.color = Color.BLACK
            g2d.drawOval((targetX - targetSize / 2).toInt(), (targetY - targetSize / 2).toInt(),
                targetSize.toInt(), targetSize.toInt())

            // 目标朝向
            val targetAngle2 = Math.toRadians(-targetPitch.toDouble())
            val tDirLen = 20.0
            g2d.color = Color(200, 100, 0)
            g2d.stroke = BasicStroke(1.5f)
            g2d.drawLine(
                targetX.toInt(), targetY.toInt(),
                (targetX + cos(targetAngle2) * tDirLen).toInt(),
                (targetY + sin(targetAngle2) * tDirLen).toInt()
            )

            // 连线
            g2d.color = Color(100, 100, 100, 100)
            g2d.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(4f), 0f)
            g2d.drawLine(cx.toInt(), cy.toInt(), targetX.toInt(), targetY.toInt())

            // 标签
            g2d.font = EditorTheme.UI_FONT
            g2d.color = Color(0, 100, 200)
            g2d.drawString("Player", 5, 15)
            g2d.color = Color(200, 0, 0)
            g2d.drawString("Target", 5, 30)
            g2d.color = Color(50, 50, 50)
            g2d.drawString("Dist: ${String.format("%.1f", distance)}m", 5, height - 10)
            g2d.drawString("P Yaw: ${String.format("%.1f", playerYaw)}°", w.toInt() - 120, 15)
            g2d.drawString("T Yaw: ${String.format("%.1f", targetYaw)}°", w.toInt() - 120, 30)
        }
    }
}