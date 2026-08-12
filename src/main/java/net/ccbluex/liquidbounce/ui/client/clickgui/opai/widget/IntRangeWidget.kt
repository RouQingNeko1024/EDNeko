package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.config.RangeSlider
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntRangeSetting
import net.minecraft.util.MathHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class IntRangeWidget(setting: IntRangeSetting) : SettingWidget<IntRangeSetting>(setting) {

    override fun getHeight(): Float {
        return OpaiTheme.SETTING_HEIGHT + 26.0f
    }

    override fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        val offsetY = 0.0f
        val labelY = y + 1.0f

        renderer.text().addText(setting.getDisplayName(), x + OpaiTheme.SETTING_PADDING_X, labelY, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabel())

        val firstDisplay = formatValue(setting.getMinimum())
        val lastDisplay = formatValue(setting.getMaximum())
        val firstWidth = renderer.text().getWidth(firstDisplay, OpaiTheme.SETTING_TEXT_SCALE)
        val lastWidth = renderer.text().getWidth(lastDisplay, OpaiTheme.SETTING_TEXT_SCALE)
        val dashWidth = renderer.text().getWidth(" - ", OpaiTheme.SETTING_TEXT_SCALE)
        val firstBoxWidth = firstWidth + 10.0f
        val lastBoxWidth = lastWidth + 10.0f
        val totalWidth = firstBoxWidth + 4.0f + dashWidth + 4.0f + lastBoxWidth
        val firstBoxX = x + width - totalWidth - 8.0f
        val dashX = firstBoxX + firstBoxWidth + 4.0f
        val lastBoxX = dashX + dashWidth + 4.0f

        renderer.roundRect().addRoundRect(firstBoxX, y + 1.0f + offsetY, firstBoxWidth, 14.0f, OpaiTheme.INPUT_RADIUS,
                OpaiTheme.settingSurface())
        renderer.roundRect().addRoundRect(lastBoxX, y + 1.0f + offsetY, lastBoxWidth, 14.0f, OpaiTheme.INPUT_RADIUS,
                OpaiTheme.settingSurface())

        renderer.text().addText(firstDisplay, firstBoxX + 5.0f, y + 4.0f + offsetY, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabel())
        renderer.text().addText("-", dashX, y + 4.0f + offsetY, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabelMuted())
        renderer.text().addText(lastDisplay, lastBoxX + 5.0f, y + 4.0f + offsetY, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabel())

        val trackX = x + OpaiTheme.SETTING_PADDING_X
        val trackY = y + OpaiTheme.SETTING_HEIGHT + 8.0f + offsetY
        val trackW = width - OpaiTheme.SETTING_PADDING_X * 2.0f
        val trackH = OpaiTheme.SLIDER_HEIGHT
        val range = (setting.getMaximum() - setting.getMinimum()).coerceAtLeast(1)
        val leftRatio = (setting.getValue().first - setting.getMinimum()).toFloat() / range.toFloat()
        val rightRatio = (setting.getValue().last - setting.getMinimum()).toFloat() / range.toFloat()
        val leftX = trackX + trackW * MathHelper.clamp_float(leftRatio, 0.0f, 1.0f)
        val rightX = trackX + trackW * MathHelper.clamp_float(rightRatio, 0.0f, 1.0f)
        val startX = min(leftX, rightX)
        val endX = max(leftX, rightX)

        renderer.roundRect().addRoundRect(trackX, trackY, trackW, trackH, OpaiTheme.SLIDER_RADIUS, OpaiTheme.sliderTrack())
        renderer.roundRect().addRoundRect(startX, trackY, endX - startX, trackH, OpaiTheme.SLIDER_RADIUS, OpaiTheme.sliderActive())

        drawHandle(renderer, leftX, trackY, setting.getSelectedRangeSlider() == RangeSlider.LEFT)
        drawHandle(renderer, rightX, trackY, setting.getSelectedRangeSlider() == RangeSlider.RIGHT)

        renderer.text().addText("${setting.getMinimum()} - ${setting.getMaximum()}", x + OpaiTheme.SETTING_PADDING_X, y + 36.0f + offsetY,
                OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabelMuted())

        val selected = setting.getSelectedRangeSlider()
        if (selected != null) {
            updateFromMouse(mouseX, selected)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false

        val offsetY = 0.0f
        val trackX = x + OpaiTheme.SETTING_PADDING_X
        val trackY = y + OpaiTheme.SETTING_HEIGHT + 8.0f + offsetY
        val trackW = width - OpaiTheme.SETTING_PADDING_X * 2.0f
        val trackH = OpaiTheme.SLIDER_HEIGHT + 8.0f

        if (!isHovered(mouseX, mouseY, x, y, width, getHeight())) return false

        val leftRatio = (setting.getValue().first - setting.getMinimum()).toFloat() / (setting.getMaximum() - setting.getMinimum()).coerceAtLeast(1).toFloat()
        val rightRatio = (setting.getValue().last - setting.getMinimum()).toFloat() / (setting.getMaximum() - setting.getMinimum()).coerceAtLeast(1).toFloat()
        val leftX = trackX + trackW * MathHelper.clamp_float(leftRatio, 0.0f, 1.0f)
        val rightX = trackX + trackW * MathHelper.clamp_float(rightRatio, 0.0f, 1.0f)

        val nearest = if (abs(mouseX.toFloat() - leftX) <= abs(mouseX.toFloat() - rightX)) RangeSlider.LEFT else RangeSlider.RIGHT
        setting.intRangeValue.lastChosenSlider = nearest
        updateFromMouse(mouseX.toInt(), nearest)
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && setting.intRangeValue.lastChosenSlider != null) {
            setting.intRangeValue.lastChosenSlider = null
            return true
        }
        return false
    }

    private fun updateFromMouse(mouseX: Int, slider: RangeSlider) {
        val trackX = x + OpaiTheme.SETTING_PADDING_X
        val trackW = width - OpaiTheme.SETTING_PADDING_X * 2.0f
        val percent = ((mouseX - trackX) / trackW).coerceIn(0.0f, 1.0f)
        val minValue = setting.getMinimum()
        val maxValue = setting.getMaximum()
        val value = minValue + ((maxValue - minValue) * percent).roundToInt()
        when (slider) {
            RangeSlider.LEFT -> setting.setFirst(value.coerceAtMost(setting.getValue().last))
            RangeSlider.RIGHT -> setting.setLast(value.coerceAtLeast(setting.getValue().first))
        }
    }

    private fun drawHandle(renderer: OpaiRenderer, centerX: Float, trackY: Float, selected: Boolean) {
        val radius = OpaiTheme.SLIDER_KNOB_RADIUS + if (selected) 0.5f else 0.0f
        renderer.roundRect().addRoundRect(centerX - radius, trackY - 2.0f, radius * 2.0f, radius * 2.0f, radius, OpaiTheme.sliderKnob())
    }

    private fun formatValue(value: Int): String = value.toString()

}
