package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.OpaiModuleManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import java.util.ArrayList

class CategoryOpaiPanel(val category: Category, panelIndex: Int) : AbstractOpaiPanel("category:" + category.name, category.name, "", panelIndex) {

    private val moduleButtons = ArrayList<ModuleButton>()
    private var searchQuery = ""

    init {
        val modules = OpaiModuleManager.INSTANCE.getModules().filter { it.category == category }
        for (module in modules) {
            moduleButtons.add(ModuleButton(module))
        }
    }

    override fun drawPanelContent(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, visibleHeight: Float) {
        val expand = openAnim.getValue()
        var currentY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT - scroll
        for (button in moduleButtons) {
            if (!matchesSearch(button)) continue
            button.setPosition(panelX, currentY, panelWidth)
            val btnH = button.getHeight()

            val visibleTop = panelY + OpaiTheme.PANEL_HEADER_HEIGHT
            val visibleBottom = visibleTop + visibleHeight * expand
            if (currentY + btnH > visibleTop && currentY < visibleBottom) {
                button.draw(renderer, mouseX, mouseY)
            }

            currentY += btnH
        }
    }

    override fun computeContentHeight(): Float {
        var total = 0.0f
        for (button in moduleButtons) {
            if (!matchesSearch(button)) continue
            total += button.getHeight()
        }
        return total
    }

    override fun mouseClickedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (mb in moduleButtons) {
            if (!matchesSearch(mb)) continue
            if (mb.mouseClicked(mouseX, mouseY, button)) {
                return true
            }
        }
        return false
    }

    override fun mouseReleasedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (mb in moduleButtons) {
            if (!matchesSearch(mb)) continue
            if (mb.mouseReleased(mouseX, mouseY, button)) {
                return true
            }
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        for (mb in moduleButtons) {
            if (!matchesSearch(mb)) continue
            if (mb.keyPressed(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun charTyped(typedText: String): Boolean {
        for (mb in moduleButtons) {
            if (!matchesSearch(mb)) continue
            if (mb.charTyped(typedText)) {
                return true
            }
        }
        return false
    }

    fun setSearchQuery(searchQuery: String?) {
        this.searchQuery = searchQuery?.trim()?.lowercase() ?: ""
        scroll = 0.0f
    }

    override fun hasActiveInput(): Boolean {
        for (mb in moduleButtons) {
            if (!matchesSearch(mb)) continue
            if (mb.hasListeningKeybind() || mb.hasFocusedInput()) return true
        }
        return false
    }

    private fun matchesSearch(button: ModuleButton): Boolean {
        if (searchQuery.isEmpty()) return true
        val module = button.module
        val name = module.name ?: ""
        val categoryName = module.category.name.lowercase()
        return name.lowercase().contains(searchQuery) || categoryName.contains(searchQuery)
    }

}
