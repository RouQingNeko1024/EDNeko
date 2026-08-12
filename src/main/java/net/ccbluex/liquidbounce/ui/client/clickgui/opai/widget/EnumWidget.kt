package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.EnumSetting
import java.awt.Color

class EnumWidget(setting: EnumSetting) : SettingWidget<EnumSetting>(setting) {

    private val chipHeight = 14.0f
    private val chipRadius = 7.0f
    private val chipPaddingX = 6.0f
    private val chipGap = 4.0f
    private val chipTextScale = 0.56f
    private val chipHoverAnims = HashMap<Int, Animation>()

    private var computedHeight = OpaiTheme.SETTING_HEIGHT - 1.0f + chipHeight + 1.0f
    private var chipBoundsX = FloatArray(0)
    private var chipBoundsY = FloatArray(0)
    private var chipBoundsW = FloatArray(0)

    init {
        val count = setting.modes.size
        chipBoundsX = FloatArray(count)
        chipBoundsY = FloatArray(count)
        chipBoundsW = FloatArray(count)
    }

    override fun getHeight(): Float = computedHeight

    override fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        renderer.text().addText(setting.getDisplayName(), x + OpaiTheme.SETTING_PADDING_X, y + 1.0f, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabel())

        val modes = setting.modes
        if (chipBoundsX.size != modes.size) {
            chipBoundsX = FloatArray(modes.size)
            chipBoundsY = FloatArray(modes.size)
            chipBoundsW = FloatArray(modes.size)
        }
        
        var currentChipX = x + OpaiTheme.SETTING_PADDING_X
        var currentChipY = y + OpaiTheme.SETTING_HEIGHT - 1.0f
        val maxX = x + width - OpaiTheme.SETTING_PADDING_X

        for (i in modes.indices) {
            val label = setting.getTranslatedValueByIndex(i)
            val textW = renderer.text().getWidth(label, chipTextScale)
            val chipW = textW + chipPaddingX * 2.0f

            if (currentChipX + chipW > maxX && currentChipX > x + OpaiTheme.SETTING_PADDING_X) {
                currentChipX = x + OpaiTheme.SETTING_PADDING_X
                currentChipY += chipHeight + chipGap
            }

            chipBoundsX[i] = currentChipX
            chipBoundsY[i] = currentChipY
            chipBoundsW[i] = chipW

            val selected = setting.getValue() == modes[i]
            val fg = if (selected) OpaiTheme.chipSelectedText() else OpaiTheme.chipUnselectedText()

            val chipHovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), currentChipX, currentChipY, chipW, chipHeight)
            val chipAnim = chipHoverAnims.getOrPut(i) { Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER) }
            chipAnim.run(if (chipHovered) 1.0f else 0.0f)
            val chipHover = chipAnim.getValue()

            if (selected) {
                val c = MD3Theme.lerp(OpaiTheme.chipSelected(), OpaiTheme.accent(MD3Theme.SECONDARY_CONTAINER), chipHover * 0.3f)
                renderer.roundRect().addRoundRect(currentChipX, currentChipY, chipW, chipHeight, chipRadius, c)
            } else {
                val outlineColor = MD3Theme.lerp(OpaiTheme.chipOutline(), MD3Theme.withAlpha(MD3Theme.OUTLINE, 180), chipHover)
                renderer.outline().addOutline(currentChipX, currentChipY, chipW, chipHeight, chipRadius, 1.0f, outlineColor)
                if (chipHover > 0.02f) {
                    val hoverFill = MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER_HIGH, (60 * chipHover).toInt())
                    renderer.roundRect().addRoundRect(currentChipX, currentChipY, chipW, chipHeight, chipRadius, hoverFill)
                }
            }
            renderer.text().addText(label, currentChipX + chipPaddingX, currentChipY + (chipHeight - renderer.text().getHeight(chipTextScale)) * 0.5f, chipTextScale, fg)

            currentChipX += chipW + chipGap
        }

        computedHeight = currentChipY - y + chipHeight + 1.0f
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false

        val modes = setting.modes
        for (i in modes.indices) {
            if (isHovered(mouseX, mouseY, chipBoundsX[i], chipBoundsY[i], chipBoundsW[i], chipHeight)) {
                setting.setMode(modes[i])
                return true
            }
        }
        return false
    }

}
