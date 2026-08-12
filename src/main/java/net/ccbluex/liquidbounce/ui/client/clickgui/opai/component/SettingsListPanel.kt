package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.Setting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.SettingGroup
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.SettingWidget
import java.util.*

class SettingsListPanel : AbstractOpaiPanel {

    private val settingsContent: SettingsContent

    constructor(id: String, title: String, icon: String?, panelIndex: Int, settings: List<Setting<*>>) : this(id, title, icon, panelIndex, settings, ArrayList())

    constructor(id: String, title: String, icon: String?, panelIndex: Int, settings: List<Setting<*>>, orderedGroups: List<SettingGroup>) : super(id, title, icon, panelIndex) {
        this.settingsContent = SettingsContent(settings, orderedGroups)
    }

    override fun computeContentHeight(): Float {
        return settingsContent.computeContentHeight()
    }

    override fun drawPanelContent(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, visibleHeight: Float) {
        settingsContent.draw(renderer, mouseX, mouseY, panelX, panelY + OpaiTheme.PANEL_HEADER_HEIGHT - scroll, panelWidth)
    }

    override fun mouseClickedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return settingsContent.mouseClicked(mouseX, mouseY, button, panelX, panelY + OpaiTheme.PANEL_HEADER_HEIGHT - scroll, panelWidth)
    }

    override fun mouseReleasedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return settingsContent.mouseReleased(mouseX, mouseY, button, panelX, panelY + OpaiTheme.PANEL_HEADER_HEIGHT - scroll, panelWidth)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return settingsContent.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(typedText: String): Boolean {
        return settingsContent.charTyped(typedText)
    }

    override fun hasActiveInput(): Boolean {
        return settingsContent.hasActiveInput()
    }
}
