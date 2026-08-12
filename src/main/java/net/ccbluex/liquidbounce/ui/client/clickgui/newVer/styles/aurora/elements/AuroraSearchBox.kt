/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.AuroraGUI
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import java.awt.Color

class AuroraSearchBox(private val moduleManager: net.ccbluex.liquidbounce.features.module.ModuleManager) {
    private var text = ""
    private var focused = false
    private var cursorPos = 0
    private var cursorVisible = true
    private var cursorTimer = 0L

    private val searchResults = mutableListOf<Module>()
    private val expandedModules = mutableSetOf<Module>()

    val isSearching: Boolean
        get() = text.isNotEmpty()

    val isTyping: Boolean
        get() = focused

    var onExpandModule: ((Module) -> Unit)? = null

    fun draw(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        val height = 25F

        RenderUtils.originalRoundedRect(x, y, x + width, y + height, 6F,
            (if (focused) Color(40, 40, 45) else Color(30, 30, 35)).rgb)

        Fonts.font35.drawString("\uD83D\uDD0D", x + 8F, y + 8F, Color(150, 150, 155).rgb)

        val displayText = if (text.isEmpty()) "Search modules..." else text
        val textColor = if (text.isEmpty()) Color(100, 100, 105) else Color.WHITE
        Fonts.font35.drawString(displayText, x + 25F, y + 8F, textColor.rgb)

        if (focused && text.isNotEmpty()) {
            if (System.currentTimeMillis() - cursorTimer > 500) {
                cursorVisible = !cursorVisible
                cursorTimer = System.currentTimeMillis()
            }

            if (cursorVisible) {
                val cursorX = x + 25F + Fonts.font35.getStringWidth(text.take(cursorPos))
                RenderUtils.drawRect(cursorX, y + 7F, cursorX + 1F, y + 18F, Color.WHITE.rgb)
            }
        }
    }

    fun drawSearchResults(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, height: Float, accentColor: Color) {
        if (searchResults.isEmpty()) {
            Fonts.font40.drawString("No results found", x + 10F, y + 10F, Color(150, 150, 155).rgb)
            return
        }

        var resultY = y
        for (module in searchResults) {
            if (resultY + 30F > y + height) break

            val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= resultY && mouseY <= resultY + 30F

            RenderUtils.originalRoundedRect(x, resultY, x + width, resultY + 30F, 4F,
                (if (isHovered) Color(45, 45, 50) else Color(35, 35, 40)).rgb)

            if (module.state) {
                RenderUtils.originalRoundedRect(x, resultY, x + 3F, resultY + 30F, 2F, accentColor.rgb)
            }

            Fonts.font40.drawString(module.name, x + 10F, resultY + 8F,
                if (module.state) Color.WHITE.rgb else Color(150, 150, 155).rgb)

            Fonts.font35.drawString(module.category.displayName, x + width - 80F, resultY + 10F, Color(120, 120, 125).rgb)

            if (module.values.isNotEmpty()) {
                Fonts.font35.drawString("\u25B6", x + width - 20F, resultY + 10F, Color(120, 120, 125).rgb)
            }

            resultY += 35F
        }
    }

    fun handleClick(mouseX: Int, mouseY: Int, mouseButton: Int, x: Float, y: Float): Boolean {
        val height = 25F

        if (mouseX >= x && mouseX <= x + 300F && mouseY >= y && mouseY <= y + height) {
            focused = true
            cursorTimer = System.currentTimeMillis()
            cursorVisible = true
            return true
        } else {
            focused = false
        }
        return false
    }

    fun handleModuleClick(mouseX: Int, mouseY: Int, mouseButton: Int, x: Float, y: Float, width: Float): Boolean {
        if (!isSearching) return false

        var resultY = y
        for (module in searchResults) {
            if (resultY + 30F > y + (AuroraGUI.getInstance().panelHeight - 60F)) break

            if (mouseX >= x && mouseX <= x + width && mouseY >= resultY && mouseY <= resultY + 30F) {
                if (mouseButton == 0) {
                    module.toggle()
                    return true
                } else if (mouseButton == 1) {
                    if (module.values.isNotEmpty()) {
                        onExpandModule?.invoke(module)
                    }
                    return true
                }
            }
            resultY += 35F
        }
        return false
    }

    fun handleKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!focused) return false

        when (keyCode) {
            Keyboard.KEY_BACK -> {
                if (cursorPos > 0) {
                    text = text.removeRange(cursorPos - 1, cursorPos)
                    cursorPos--
                    updateSearch()
                }
            }
            Keyboard.KEY_DELETE -> {
                if (cursorPos < text.length) {
                    text = text.removeRange(cursorPos, cursorPos + 1)
                    updateSearch()
                }
            }
            Keyboard.KEY_LEFT -> {
                if (cursorPos > 0) cursorPos--
            }
            Keyboard.KEY_RIGHT -> {
                if (cursorPos < text.length) cursorPos++
            }
            Keyboard.KEY_ESCAPE -> {
                if (text.isNotEmpty()) {
                    text = ""
                    cursorPos = 0
                    updateSearch()
                } else {
                    focused = false
                }
            }
            Keyboard.KEY_RETURN -> {
                if (searchResults.isNotEmpty()) {
                    searchResults.first().toggle()
                }
            }
            else -> {
                if (typedChar.isLetterOrDigit() || typedChar == ' ') {
                    text = text.substring(0, cursorPos) + typedChar.lowercaseChar() + text.substring(cursorPos)
                    cursorPos++
                    updateSearch()
                }
            }
        }

        return true
    }

    private fun updateSearch() {
        searchResults.clear()
        if (text.isNotEmpty()) {
            moduleManager.filter {
                it.name.lowercase().contains(text.lowercase())
            }.forEach { searchResults.add(it) }
        }
    }

    fun clearSearch() {
        text = ""
        cursorPos = 0
        searchResults.clear()
    }
}
