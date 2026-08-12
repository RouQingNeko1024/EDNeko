package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.StringSetting
import org.lwjgl.input.Keyboard

class StringWidget(setting: StringSetting) : SettingWidget<StringSetting>(setting) {

    private val inputField = OpaiTextField(100)

    override fun getHeight(): Float = OpaiTheme.SETTING_HEIGHT + OpaiTheme.INPUT_HEIGHT + 2.0f

    override fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        renderer.text().addText(setting.getDisplayName(), x + OpaiTheme.SETTING_PADDING_X, y + 1.0f, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabel())

        val fieldX = x + OpaiTheme.SETTING_PADDING_X
        val fieldY = y + OpaiTheme.SETTING_HEIGHT
        val fieldW = width - OpaiTheme.SETTING_PADDING_X * 2.0f
        val fieldH = OpaiTheme.INPUT_HEIGHT

        if (!inputField.focused && inputField.text != setting.getValue()) {
            inputField.text = setting.getValue()
        }
        inputField.draw(renderer, fieldX, fieldY, fieldW, fieldH, mouseX, mouseY, "...", OpaiTheme.SETTING_TEXT_SCALE)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val fieldX = x + OpaiTheme.SETTING_PADDING_X
        val fieldY = y + OpaiTheme.SETTING_HEIGHT
        val fieldW = width - OpaiTheme.SETTING_PADDING_X * 2.0f
        val fieldH = OpaiTheme.INPUT_HEIGHT

        if (button == 0 && inputField.focusIfContains(mouseX, mouseY, fieldX, fieldY, fieldW, fieldH)) {
            inputField.text = setting.getValue()
            inputField.cursor = inputField.text.length
            return true
        }
        if (inputField.focused) {
            syncSetting()
            inputField.blur()
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!inputField.focused) return false

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            syncSetting()
            inputField.blur()
            return true
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            inputField.text = setting.getValue()
            inputField.blur()
            return true
        }
        if (inputField.keyPressed(keyCode)) {
            syncSetting()
            return true
        }
        return false
    }

    override fun charTyped(typedText: String): Boolean {
        if (inputField.charTyped(typedText[0])) {
            syncSetting()
            return true
        }
        return false
    }

    fun isFocused(): Boolean = inputField.focused

    private fun syncSetting() {
        setting.setValue(inputField.text)
    }

}
