package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.OpaiTextField
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager
import org.lwjgl.input.Keyboard
import java.util.Objects

class ConfigOpaiPanel(panelIndex: Int) : AbstractOpaiPanel("config", EpsilonTranslateComponent.create("gui", "tab.config"), "", panelIndex) {

    private val emptyComponent = EpsilonTranslateComponent.create("gui", "config.empty")
    private val saveAsComponent = EpsilonTranslateComponent.create("gui", "config.action.saveas")
    private val reloadComponent = EpsilonTranslateComponent.create("gui", "config.action.reload")
    private val exportComponent = EpsilonTranslateComponent.create("gui", "config.action.export")
    private val importComponent = EpsilonTranslateComponent.create("gui", "config.action.import")
    private val openFolderComponent = EpsilonTranslateComponent.create("gui", "config.action.open_folder")
    private val savedComponent = EpsilonTranslateComponent.create("gui", "dropdown.status.saved")
    private val reloadedComponent = EpsilonTranslateComponent.create("gui", "dropdown.status.reloaded")
    private val exportedComponent = EpsilonTranslateComponent.create("gui", "dropdown.status.exported")
    private val importedComponent = EpsilonTranslateComponent.create("gui", "dropdown.status.imported")
    private val deletedComponent = EpsilonTranslateComponent.create("gui", "dropdown.status.deleted")
    private val switchedComponent = EpsilonTranslateComponent.create("gui", "dropdown.status.switched")
    private val errorComponent = EpsilonTranslateComponent.create("gui", "config.error.title")

    private val inputField = OpaiTextField(160)
    private var status = ""

    override fun computeContentHeight(): Float {
        val configCount = ConfigManager.listConfigs().size
        val configRowsHeight = 24.0f.coerceAtLeast(configCount.toFloat() * (24.0f + 4.0f))
        return 6.0f * 2.0f + 18.0f + 4.0f + 17.0f * 3.0f + 4.0f * 3.0f + configRowsHeight + (if (status.isEmpty()) 0.0f else 24.0f)
    }

    override fun drawPanelContent(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, visibleHeight: Float) {
        var currentY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT + 6.0f - scroll
        val contentX = panelX + 6.0f
        val contentW = panelWidth - 6.0f * 2.0f

        val placeholder = ConfigManager.getActiveConfigName()
        inputField.draw(renderer, contentX, currentY, contentW, 18.0f, mouseX, mouseY, placeholder, OpaiTheme.SETTING_TEXT_SCALE)
        currentY += 18.0f + 4.0f

        val actions = arrayOf(
                saveAsComponent.translatedName,
                reloadComponent.translatedName,
                exportComponent.translatedName,
                importComponent.translatedName,
                openFolderComponent.translatedName
        )
        for (row in 0 until 3) {
            val columns = if (row == 2) 1 else 2
            for (col in 0 until columns) {
                val index = row * 2 + col
                if (index >= actions.size) continue
                var btnW = (contentW - 4.0f) * 0.5f
                var btnX = contentX + col * (btnW + 4.0f)
                if (row == 2) {
                    btnW = contentW
                    btnX = contentX
                }
                val btnY = currentY + row * (17.0f + 4.0f)
                val hovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), btnX, btnY, btnW, 17.0f)
                renderer.roundRect().addRoundRect(btnX, btnY, btnW, 17.0f, OpaiTheme.BUTTON_RADIUS,
                        if (hovered) MD3Theme.PRIMARY_CONTAINER else MD3Theme.SURFACE_CONTAINER_HIGH)
                val labelScale = 0.58f
                val labelW = renderer.text().getWidth(actions[index], labelScale)
                renderer.text().addText(actions[index], btnX + (btnW - labelW) * 0.5f, getCenteredTextY(renderer, btnY, 17.0f, labelScale), labelScale, MD3Theme.TEXT_PRIMARY)
            }
        }
        currentY += 17.0f * 3.0f + 4.0f * 3.0f

        if (status.isNotEmpty()) {
            val statusScale = 0.58f
            renderer.text().addText(trimToWidth(status, statusScale, contentW, renderer), contentX, getCenteredTextY(renderer, currentY, 24.0f, statusScale), statusScale, MD3Theme.TEXT_MUTED)
            currentY += 24.0f
        }

        val active = ConfigManager.getActiveConfigName()
        val configs = ConfigManager.listConfigs()
        if (configs.isEmpty()) {
            val emptyScale = 0.62f
            renderer.text().addText(emptyComponent.translatedName, contentX, getCenteredTextY(renderer, currentY, 24.0f, emptyScale), emptyScale, MD3Theme.TEXT_MUTED)
            return
        }
        for (name in configs) {
            val activeRow = name == active
            val hovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), contentX, currentY, contentW, 24.0f)
            renderer.roundRect().addRoundRect(contentX, currentY, contentW, 24.0f, OpaiTheme.BUTTON_RADIUS,
                    if (activeRow) MD3Theme.PRIMARY_CONTAINER else (if (hovered) MD3Theme.SURFACE_CONTAINER_HIGH else MD3Theme.SURFACE_CONTAINER_LOW))
            val nameScale = 0.64f
            renderer.text().addText(trimToWidth(name, nameScale, contentW - 28.0f, renderer), contentX + 6.0f, getCenteredTextY(renderer, currentY, 24.0f, nameScale), nameScale,
                    if (activeRow) MD3Theme.ON_PRIMARY_CONTAINER else MD3Theme.TEXT_PRIMARY)
            val deleteX = contentX + contentW - 18.0f
            val deleteScale = 0.60f
            renderer.text().addText("x", deleteX + 5.0f, getCenteredTextY(renderer, currentY + 3.0f, 16.0f, deleteScale), deleteScale,
                    if (isHovered(mouseX.toDouble(), mouseY.toDouble(), deleteX, currentY + 3.0f, 16.0f, 16.0f)) MD3Theme.ERROR else MD3Theme.TEXT_MUTED)
            currentY += 24.0f + 4.0f
        }
    }

    override fun mouseClickedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false
        var currentY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT + 6.0f - scroll
        val contentX = panelX + 6.0f
        val contentW = panelWidth - 6.0f * 2.0f
        if (inputField.focusIfContains(mouseX, mouseY, contentX, currentY, contentW, 18.0f)) {
            if (inputField.text.isEmpty()) {
                inputField.text = ConfigManager.getActiveConfigName()
                inputField.cursor = inputField.text.length
            }
            return true
        }
        inputField.blur()
        currentY += 18.0f + 4.0f

        for (row in 0 until 3) {
            val columns = if (row == 2) 1 else 2
            for (col in 0 until columns) {
                val index = row * 2 + col
                if (index >= 5) continue
                var btnW = (contentW - 4.0f) * 0.5f
                var btnX = contentX + col * (btnW + 4.0f)
                if (row == 2) {
                    btnW = contentW
                    btnX = contentX
                }
                val btnY = currentY + row * (17.0f + 4.0f)
                if (isHovered(mouseX, mouseY, btnX, btnY, btnW, 17.0f)) {
                    runAction(index)
                    return true
                }
            }
        }
        currentY += 17.0f * 3.0f + 4.0f * 3.0f
        if (status.isNotEmpty()) currentY += 24.0f

        for (name in ConfigManager.listConfigs()) {
            val deleteX = contentX + contentW - 18.0f
            if (isHovered(mouseX, mouseY, deleteX, currentY + 3.0f, 16.0f, 16.0f)) {
                try {
                    ConfigManager.deleteConfig(name)
                    status = deletedComponent.translatedName + " " + name
                } catch (e: Exception) {
                    status = errorText(e)
                }
                return true
            }
            if (isHovered(mouseX, mouseY, contentX, currentY, contentW, 24.0f)) {
                try {
                    ConfigManager.switchConfig(name)
                    inputField.text = name
                    inputField.cursor = inputField.text.length
                    status = switchedComponent.translatedName + " " + name
                } catch (e: Exception) {
                    status = errorText(e)
                }
                return true
            }
            currentY += 24.0f + 4.0f
        }
        return false
    }

    private fun runAction(index: Int) {
        val value = inputField.text.trim()
        try {
            when (index) {
                0 -> if (value.isNotEmpty()) {
                    val saved = ConfigManager.saveAsConfig(value)
                    inputField.text = saved
                    inputField.cursor = inputField.text.length
                    status = savedComponent.translatedName + " " + saved
                }
                1 -> {
                    ConfigManager.reloadOrThrow()
                    status = reloadedComponent.translatedName
                }
                2 -> status = exportedComponent.translatedName + " " + ConfigManager.exportActiveConfigToZip(value)?.fileName
                3 -> if (value.isNotEmpty()) {
                    val imported = ConfigManager.importConfigFromZip(value)
                    inputField.text = imported
                    inputField.cursor = inputField.text.length
                    status = importedComponent.translatedName + " " + imported
                }
                4 -> status = openFolderComponent.translatedName + " " + ConfigManager.openConfigFolder()
            }
        } catch (e: Exception) {
            status = errorText(e)
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!inputField.focused) return false
        if (keyCode == 1) { // ESC
            inputField.blur()
            return true
        }
        return inputField.keyPressed(keyCode)
    }

    override fun charTyped(typedText: String): Boolean {
        return if (typedText.isNotEmpty()) inputField.charTyped(typedText[0]) else false
    }

    override fun hasActiveInput(): Boolean = inputField.focused

    private fun errorText(e: Exception): String {
        val message = e.message
        return errorComponent.translatedName + ": " + if (message == null || message.isEmpty()) e.javaClass.simpleName else message
    }

    private fun getCenteredTextY(renderer: OpaiRenderer, boxY: Float, boxH: Float, scale: Float): Float {
        return boxY + (boxH - renderer.text().getHeight(scale)) / 2.0f
    }

}
