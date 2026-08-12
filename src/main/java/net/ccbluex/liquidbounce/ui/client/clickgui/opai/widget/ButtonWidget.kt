package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.Animation
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.Easing
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.ButtonSetting

class ButtonWidget(setting: ButtonSetting) : SettingWidget<ButtonSetting>(setting) {

    private val hoverAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER)

    override fun getHeight(): Float = OpaiTheme.SETTING_HEIGHT + 2.0f

    override fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        val btnX = x + OpaiTheme.SETTING_PADDING_X
        val btnY = y + 1.0f
        val btnW = width - OpaiTheme.SETTING_PADDING_X * 2.0f
        val btnH = OpaiTheme.BUTTON_HEIGHT

        val hovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), btnX, btnY, btnW, btnH)
        hoverAnim.run(if (hovered) 1.0f else 0.0f)

        renderer.roundRect().addRoundRect(btnX, btnY, btnW, btnH, OpaiTheme.BUTTON_RADIUS, OpaiTheme.buttonSurface(hoverAnim.getValue()))

        val label = setting.getDisplayName()
        val textW = renderer.text().getWidth(label, OpaiTheme.SETTING_TEXT_SCALE)
        val textY = btnY + (btnH - renderer.text().getHeight(OpaiTheme.SETTING_TEXT_SCALE)) * 0.5f
        renderer.text().addText(label, btnX + (btnW - textW) * 0.5f, textY, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.buttonText())
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false

        val btnX = x + OpaiTheme.SETTING_PADDING_X
        val btnY = y + 1.0f
        val btnW = width - OpaiTheme.SETTING_PADDING_X * 2.0f
        val btnH = OpaiTheme.BUTTON_HEIGHT

        if (isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
            setting.action?.run()
            return true
        }
        return false
    }

}
