package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntSetting
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard
import kotlin.math.roundToInt

class IntSliderWidget(setting: IntSetting) : SettingWidget<IntSetting>(setting) {

    private val valueFieldWidth = 40.0f
    private val valueFieldHeight = 12.0f

    private val inputField = OpaiTextField(12) { it.matches("[0-9-]".toRegex()) }
    private var dragging = false
    private val hoverAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER)

    override fun getHeight(): Float = OpaiTheme.SETTING_HEIGHT + 8.0f

    override fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        val ratio = (setting.getValue() - setting.min).toFloat() / (setting.max - setting.min).toFloat()
        val sliderRatio = MathHelper.clamp_float(ratio, 0.0f, 1.0f)

        renderer.text().addText(setting.getDisplayName(), x + OpaiTheme.SETTING_PADDING_X, y + 1.0f, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabel())

        var valueStr = if (inputField.focused)
                inputField.text
                else (if (setting.isPercentageMode()) setting.getValue().toString() + "%" else setting.getValue().toString())
        if (!inputField.focused && inputField.text != valueStr) {
            inputField.text = valueStr
        }
        inputField.draw(renderer, getFieldX(), getFieldY(), valueFieldWidth, valueFieldHeight, mouseX, mouseY, valueStr, OpaiTheme.SETTING_TEXT_SCALE)

        val trackX = x + OpaiTheme.SETTING_PADDING_X
        val trackY = y + OpaiTheme.SETTING_HEIGHT
        val trackW = width - OpaiTheme.SETTING_PADDING_X * 2.0f
        val trackH = OpaiTheme.SLIDER_HEIGHT

        renderer.roundRect().addRoundRect(trackX, trackY, trackW, trackH, OpaiTheme.SLIDER_RADIUS, OpaiTheme.sliderTrack())

        val activeW = trackW * sliderRatio
        if (activeW > 0.5f) {
            renderer.roundRect().addRoundRect(trackX, trackY, activeW, trackH, OpaiTheme.SLIDER_RADIUS, OpaiTheme.sliderActive())
        }

        val knobX = trackX + trackW * sliderRatio
        val knobY = trackY + trackH * 0.5f
        val kr = OpaiTheme.SLIDER_KNOB_RADIUS

        val knobHovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), knobX - kr - 5.0f, knobY - kr - 5.0f, kr * 2.0f + 10.0f, kr * 2.0f + 10.0f)
        hoverAnim.run(if (knobHovered) 1.0f else 0.0f)
        val knobHoverProgress = hoverAnim.getValue()
        if (knobHoverProgress > 0.02f) {
            val haloSize = kr * 3.0f
            val haloX = knobX - haloSize * 0.5f
            val haloY = knobY - haloSize * 0.5f
            renderer.roundRect().addRoundRect(haloX, haloY, haloSize, haloSize, haloSize * 0.5f, OpaiTheme.stateLayer(OpaiTheme.settingLabel(), knobHoverProgress, 16))
        }

        renderer.roundRect().addRoundRect(knobX - kr, knobY - kr, kr * 2.0f, kr * 2.0f, kr, OpaiTheme.sliderKnob())

        if (dragging) {
            val rawRatio = MathHelper.clamp_float((mouseX - trackX) / trackW, 0.0f, 1.0f)
            val range = setting.max - setting.min
            val step = setting.step
            val value = setting.min + (rawRatio * range / step).roundToInt() * step
            setting.setValue(MathHelper.clamp_int(value, setting.min, setting.max))
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val plainValue = setting.getValue().toString()
            if (inputField.focusIfContains(mouseX, mouseY, getFieldX(), getFieldY(), valueFieldWidth, valueFieldHeight)) {
                inputField.text = plainValue
                inputField.cursor = plainValue.length
                dragging = false
                return true
            }
            if (inputField.focused) {
                commitInput()
                inputField.blur()
            }
            val trackX = x + OpaiTheme.SETTING_PADDING_X
            val trackY = y + OpaiTheme.SETTING_HEIGHT - 3.0f
            val trackW = width - OpaiTheme.SETTING_PADDING_X * 2.0f
            if (isHovered(mouseX, mouseY, trackX, trackY, trackW, OpaiTheme.SLIDER_HEIGHT + 6.0f)) {
                dragging = true
                return true
            }
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && dragging) {
            dragging = false
            return true
        }
        if (button == 0 && inputField.focused) {
            if (isHovered(mouseX, mouseY, getFieldX(), getFieldY(), valueFieldWidth, valueFieldHeight)) {
                return true
            }
            commitInput()
            inputField.blur()
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!inputField.focused) return false
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            commitInput()
            inputField.blur()
            return true
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            inputField.text = setting.getValue().toString()
            inputField.blur()
            return true
        }
        if (inputField.keyPressed(keyCode)) {
            syncInputValue()
            return true
        }
        return false
    }

    override fun charTyped(typedText: String): Boolean {
        if (inputField.charTyped(typedText[0])) {
            syncInputValue()
            return true
        }
        return false
    }

    fun isFocused(): Boolean = inputField.focused

    private fun commitInput() {
        val text = inputField.text
        if (text.isEmpty() || text == "-") {
            inputField.text = setting.getValue().toString()
            return
        }
        try {
            val value = text.toInt()
            setting.setValue(MathHelper.clamp_int(value, setting.min, setting.max))
        } catch (ignored: NumberFormatException) {
        }
        inputField.text = setting.getValue().toString()
        inputField.cursor = inputField.text.length
    }

    private fun syncInputValue() {
        val text = inputField.text
        if (text.isEmpty() || text == "-") return
        try {
            val value = text.toInt()
            setting.setValue(MathHelper.clamp_int(value, setting.min, setting.max))
        } catch (ignored: NumberFormatException) {
        }
    }

    private fun getFieldX(): Float = x + width - OpaiTheme.SETTING_PADDING_X - valueFieldWidth

    private fun getFieldY(): Float = y + 2.0f

}
