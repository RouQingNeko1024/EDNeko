package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.Animation
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.Easing
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.ColorSetting
import net.minecraft.util.MathHelper
import java.awt.Color

class ColorWidget(setting: ColorSetting) : SettingWidget<ColorSetting>(setting) {

    private val openAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_EXPAND)
    private var opened = false
    private var pickingSB = false
    private var pickingHue = false
    private var pickingAlpha = false

    override fun getHeight(): Float {
        openAnim.run(if (opened) 1.0f else 0.0f)
        val expandedHeight = OpaiTheme.COLOR_PICKER_HEIGHT + OpaiTheme.COLOR_HUE_HEIGHT + (if (setting.isAllowAlpha()) OpaiTheme.COLOR_ALPHA_HEIGHT + 4.0f else 0.0f) + 10.0f
        return OpaiTheme.SETTING_HEIGHT + expandedHeight * openAnim.getValue()
    }

    override fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        openAnim.run(if (opened) 1.0f else 0.0f)
        val t = openAnim.getValue()

        renderer.text().addText(setting.getDisplayName(), x + OpaiTheme.SETTING_PADDING_X, y + (OpaiTheme.SETTING_HEIGHT - renderer.text().getHeight(OpaiTheme.SETTING_TEXT_SCALE)) * 0.5f, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabel())

        val previewX = x + width - OpaiTheme.SETTING_PADDING_X - OpaiTheme.COLOR_PREVIEW_SIZE
        val previewY = y + (OpaiTheme.SETTING_HEIGHT - OpaiTheme.COLOR_PREVIEW_SIZE) * 0.5f
        renderer.roundRect().addRoundRect(previewX, previewY, OpaiTheme.COLOR_PREVIEW_SIZE, OpaiTheme.COLOR_PREVIEW_SIZE, 2.0f, setting.getValue())

        if (t < 0.01f) return

        val color = setting.getValue()
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)

        val padX = OpaiTheme.SETTING_PADDING_X
        val gradX = x + padX
        val gradY = y + OpaiTheme.SETTING_HEIGHT + 2.0f
        val gradW = width - padX * 2.0f
        val gradH = OpaiTheme.COLOR_PICKER_HEIGHT * t

        val hueColor = Color.getHSBColor(hsb[0], 1.0f, 1.0f)
        renderer.roundRect().addRoundRectGradient(gradX, gradY, gradW, gradH, OpaiTheme.COLOR_RADIUS, OpaiTheme.COLOR_RADIUS, OpaiTheme.COLOR_RADIUS, OpaiTheme.COLOR_RADIUS, Color.WHITE, Color.BLACK, Color.BLACK, hueColor)

        val hueY = gradY + gradH + 3.0f
        val hueH = OpaiTheme.COLOR_HUE_HEIGHT * t
        for (i in 0 until gradW.toInt()) {
            val c = Color.getHSBColor(i / gradW, 1.0f, 1.0f)
            renderer.rect().addRect(gradX + i, hueY, 1.0f, hueH, c)
        }

        if (setting.isAllowAlpha()) {
            val alphaY = hueY + hueH + 4.0f
            val alphaH = OpaiTheme.COLOR_ALPHA_HEIGHT * t
            for (i in 0 until gradW.toInt()) {
                val a = i / gradW
                val c = Color(color.red, color.green, color.blue, (a * 255).toInt())
                renderer.rect().addRect(gradX + i, alphaY, 1.0f, alphaH, c)
            }

            if (pickingAlpha) {
                val newAlpha = MathHelper.clamp_float((mouseX - gradX) / gradW, 0.0f, 1.0f)
                val current = setting.getValue()
                setting.setValue(Color(current.red, current.green, current.blue, (newAlpha * 255).toInt()))
            }
        }

        if (pickingSB) {
            val newSat = MathHelper.clamp_float((mouseX - gradX) / gradW, 0.0f, 1.0f)
            val newBri = 1.0f - MathHelper.clamp_float((mouseY - gradY) / (OpaiTheme.COLOR_PICKER_HEIGHT * t), 0.0f, 1.0f)
            var newColor = Color.getHSBColor(hsb[0], newSat, newBri)
            if (setting.isAllowAlpha()) {
                newColor = Color(newColor.red, newColor.green, newColor.blue, color.alpha)
            }
            setting.setValue(newColor)
        }

        if (pickingHue) {
            val newHue = MathHelper.clamp_float((mouseX - gradX) / gradW, 0.0f, 1.0f)
            var newColor = Color.getHSBColor(newHue, hsb[1], hsb[2])
            if (setting.isAllowAlpha()) {
                newColor = Color(newColor.red, newColor.green, newColor.blue, color.alpha)
            }
            setting.setValue(newColor)
        }

        val pickerCx = gradX + gradW * hsb[1]
        val pickerCy = gradY + gradH * (1.0f - hsb[2])
        renderer.roundRect().addRoundRect(pickerCx - 2.0f, pickerCy - 2.0f, 4.0f, 4.0f, 2.0f, Color.WHITE)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false

        val previewX = x + width - OpaiTheme.SETTING_PADDING_X - OpaiTheme.COLOR_PREVIEW_SIZE
        val previewY = y + (OpaiTheme.SETTING_HEIGHT - OpaiTheme.COLOR_PREVIEW_SIZE) * 0.5f
        if (isHovered(mouseX, mouseY, previewX - 2f, previewY - 2f, OpaiTheme.COLOR_PREVIEW_SIZE + 4f, OpaiTheme.COLOR_PREVIEW_SIZE + 4f)) {
            opened = !opened
            return true
        }

        if (!opened || openAnim.getValue() < 0.5f) return false

        val padX = OpaiTheme.SETTING_PADDING_X
        val gradX = x + padX
        val gradY = y + OpaiTheme.SETTING_HEIGHT + 2.0f
        val gradW = width - padX * 2.0f
        val gradH = OpaiTheme.COLOR_PICKER_HEIGHT * openAnim.getValue()
        val hueY = gradY + gradH + 3.0f
        val hueH = OpaiTheme.COLOR_HUE_HEIGHT * openAnim.getValue()

        if (isHovered(mouseX, mouseY, gradX, gradY, gradW, gradH)) {
            pickingSB = true
            return true
        }
        if (isHovered(mouseX, mouseY, gradX, hueY, gradW, hueH)) {
            pickingHue = true
            return true
        }
        if (setting.isAllowAlpha()) {
            val alphaY = hueY + hueH + 4.0f
            val alphaH = OpaiTheme.COLOR_ALPHA_HEIGHT * openAnim.getValue()
            if (isHovered(mouseX, mouseY, gradX, alphaY, gradW, alphaH)) {
                pickingAlpha = true
                return true
            }
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && (pickingSB || pickingHue || pickingAlpha)) {
            pickingSB = false
            pickingHue = false
            pickingAlpha = false
            return true
        }
        return false
    }

}
