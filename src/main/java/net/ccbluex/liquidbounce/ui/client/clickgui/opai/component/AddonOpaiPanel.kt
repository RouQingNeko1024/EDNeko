package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.StaticFontLoader
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.AddonManager
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager
import java.util.*

class AddonOpaiPanel(panelIndex: Int) : AbstractOpaiPanel("addon", EpsilonTranslateComponent.create("gui", "tab.addon"), "", panelIndex) {

    private val emptyComponent = EpsilonTranslateComponent.create("gui", "addon.empty")
    private val noSettingsComponent = EpsilonTranslateComponent.create("gui", "addon.no_settings")
    private val modulesComponent = EpsilonTranslateComponent.create("gui", "addon.info.modules")
    private val versionComponent = EpsilonTranslateComponent.create("gui", "addon.info.version")

    private var selectedAddonId = ""
    private val widgets = ArrayList<SettingWidget<*>>()
    private var lastAddon: Any? = null

    override fun computeContentHeight(): Float {
        val addon = resolveSelectedAddon()
        if (addon == null) {
            return 6.0f * 2.0f + 28.0f
        }
        ensureWidgets(addon)
        var height = 6.0f + AddonManager.getAddons().size * (28.0f + 4.0f) + 38.0f + 4.0f
        if (widgets.isEmpty()) {
            height += 28.0f
        } else {
            for (widget in widgets) {
                if (widget.isVisible()) height += widget.getHeight() + OpaiTheme.SETTING_GAP
            }
        }
        return height + 6.0f
    }

    override fun drawPanelContent(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, visibleHeight: Float) {
        var currentY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT + 6.0f - scroll
        val contentX = panelX + 6.0f
        val contentW = panelWidth - 6.0f * 2.0f
        val addons = AddonManager.getAddons()
        val selected = resolveSelectedAddon()
        if (addons.isEmpty()) {
            renderer.text().addText(emptyComponent.translatedName, contentX, currentY + 4.0f, 0.62f, MD3Theme.TEXT_MUTED)
            return
        }

        for (addon in addons) {
            val active = selected != null && addon == selected
            val hovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), contentX, currentY, contentW, 28.0f)
            renderer.roundRect().addRoundRect(contentX, currentY, contentW, 28.0f, OpaiTheme.BUTTON_RADIUS,
                    if (active) MD3Theme.PRIMARY_CONTAINER else (if (hovered) MD3Theme.SURFACE_CONTAINER_HIGH else MD3Theme.SURFACE_CONTAINER_LOW))
            currentY += 28.0f + 4.0f
        }

        if (selected == null) return
        ensureWidgets(selected)
        renderer.roundRect().addRoundRect(contentX, currentY, contentW, 38.0f, OpaiTheme.BUTTON_RADIUS, MD3Theme.SURFACE_CONTAINER_HIGH)
        currentY += 38.0f + 4.0f

        if (widgets.isEmpty()) {
            renderer.text().addText(noSettingsComponent.translatedName, contentX, currentY + 4.0f, 0.62f, MD3Theme.TEXT_MUTED)
            return
        }
        for (widget in widgets) {
            if (!widget.isVisible()) continue
            widget.setPosition(contentX, currentY, contentW)
            widget.draw(renderer, mouseX, mouseY)
            currentY += widget.getHeight() + OpaiTheme.SETTING_GAP
        }
    }

    override fun mouseClickedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 && button != 1 && button != 2) return false
        var currentY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT + 6.0f - scroll
        val contentX = panelX + 6.0f
        val contentW = panelWidth - 6.0f * 2.0f
        for (addon in AddonManager.getAddons()) {
            if (isHovered(mouseX, mouseY, contentX, currentY, contentW, 28.0f)) {
                scroll = 0.0f.coerceAtLeast((currentY - panelY).coerceAtMost(maxScroll))
                ensureWidgets(addon)
                return true
            }
            currentY += 28.0f + 4.0f
        }
        currentY += 38.0f + 4.0f
        for (widget in widgets) {
            if (!widget.isVisible()) continue
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                ConfigManager.saveNow()
                return true
            }
            currentY += widget.getHeight() + OpaiTheme.SETTING_GAP
        }
        return false
    }

    override fun mouseReleasedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (widget in widgets) {
            if (!widget.isVisible()) continue
            if (widget.mouseReleased(mouseX, mouseY, button)) {
                ConfigManager.saveNow()
                return true
            }
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        for (widget in widgets) {
            if (!widget.isVisible()) continue
            if (widget.keyPressed(keyCode, scanCode, modifiers)) {
                ConfigManager.saveNow()
                return true
            }
        }
        return false
    }

    override fun charTyped(typedText: String): Boolean {
        for (widget in widgets) {
            if (!widget.isVisible()) continue
            if (widget.charTyped(typedText)) {
                ConfigManager.saveNow()
                return true
            }
        }
        return false
    }

    override fun hasActiveInput(): Boolean {
        for (widget in widgets) {
            if (widget is KeybindWidget && widget.listening) return true
            if (widget is StringWidget && widget.isFocused()) return true
        }
        return false
    }

    private fun resolveSelectedAddon(): Any? {
        val addons = AddonManager.getAddons()
        if (addons.isEmpty()) {
            selectedAddonId = ""
            lastAddon = null
            widgets.clear()
            return null
        }
        return addons[0]
    }

    private fun ensureWidgets(addon: Any) {
        if (addon == lastAddon) return
        widgets.clear()
        lastAddon = addon
    }

}
