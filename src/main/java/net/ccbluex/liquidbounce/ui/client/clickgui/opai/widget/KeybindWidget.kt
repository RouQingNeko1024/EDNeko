package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.Animation
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.Easing
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.KeybindSetting
import org.lwjgl.input.Keyboard

class KeybindWidget(setting: KeybindSetting) : SettingWidget<KeybindSetting>(setting) {

    private val hoverAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER)
    var listening = false

    override fun getHeight(): Float = OpaiTheme.SETTING_HEIGHT

    override fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        renderer.text().addText(setting.getDisplayName(), x + OpaiTheme.SETTING_PADDING_X, y + (getHeight() - renderer.text().getHeight(OpaiTheme.SETTING_TEXT_SCALE)) * 0.5f, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabel())

        val keyText = if (listening) "..." else Keyboard.getKeyName(setting.getValue())
        val textW = renderer.text().getWidth(keyText, OpaiTheme.SETTING_TEXT_SCALE)
        val btnW = OpaiTheme.KEYBIND_WIDTH.coerceAtLeast(textW + 8.0f)
        val btnH = OpaiTheme.KEYBIND_HEIGHT
        val btnX = x + width - OpaiTheme.SETTING_PADDING_X - btnW
        val btnY = y + (getHeight() - btnH) * 0.5f

        renderer.roundRect().addRoundRect(btnX, btnY, btnW, btnH, OpaiTheme.KEYBIND_RADIUS, OpaiTheme.keybindSurface(listening))
        renderer.text().addText(keyText, btnX + (btnW - textW) * 0.5f, btnY + (btnH - renderer.text().getHeight(OpaiTheme.SETTING_TEXT_SCALE)) * 0.5f, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.keybindText(listening))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // We need a dummy renderer for width calc or hardcode
        val btnW = OpaiTheme.KEYBIND_WIDTH // Simplification
        val btnH = OpaiTheme.KEYBIND_HEIGHT
        val btnX = x + width - OpaiTheme.SETTING_PADDING_X - btnW
        val btnY = y + (getHeight() - btnH) * 0.5f

        if (button == 0 && isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
            listening = !listening
            return true
        }

        if (listening && button != 0) {
            setting.setValue(button + 1000) // encoded
            listening = false
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!listening) return false

        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK) {
            setting.setValue(0)
        } else {
            setting.setValue(keyCode)
        }
        listening = false
        return true
    }
}
