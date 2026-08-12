/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.AuroraGUI
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values.*
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import java.awt.Color

class AuroraCategoryElement(val category: Category) {
    private val moduleElements = mutableListOf<AuroraModuleElement>()
    private var targetScroll = 0F
    private var animScroll = 0F
    private var maxScroll = 0F

    init {
        LiquidBounce.moduleManager.filter { it.category == category }.forEach {
            moduleElements.add(AuroraModuleElement(it))
        }
    }

    fun drawPanel(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, height: Float, wheel: Int) {
        handleScrolling(wheel, height)

        val clipTop = y
        val clipBottom = y + height

        var currentY = y - animScroll

        for (moduleElement in moduleElements) {
            val elementHeight = moduleElement.getHeight()
            if (currentY + elementHeight >= clipTop && currentY <= clipBottom) {
                moduleElement.drawElement(mouseX, mouseY, x, currentY, width, AuroraGUI.getAuroraColor(), clipTop, clipBottom)
            }
            currentY += elementHeight + 5F
        }

        maxScroll = (moduleElements.sumOf { it.getHeight().toDouble() } + (moduleElements.size - 1) * 5F - height).toFloat().coerceAtLeast(0F)
    }

    private fun handleScrolling(wheel: Int, height: Float) {
        if (wheel != 0) {
            targetScroll -= wheel * 0.5F
            targetScroll = targetScroll.coerceIn(0F, maxScroll)
        }
        animScroll += (targetScroll - animScroll) * 0.15F
    }

    fun handleMouseClick(mouseX: Int, mouseY: Int, mouseButton: Int, x: Float, y: Float, width: Float) {
        var currentY = y - animScroll

        for (moduleElement in moduleElements) {
            val elementHeight = moduleElement.getHeight()
            if (mouseX >= x && mouseX <= x + width &&
                mouseY >= currentY && mouseY <= currentY + elementHeight) {
                moduleElement.handleClick(mouseX, mouseY, mouseButton, x, currentY, width)
                return
            }
            currentY += elementHeight + 5F
        }
    }

    fun handleMouseDrag(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        var currentY = y - animScroll

        for (moduleElement in moduleElements) {
            val elementHeight = moduleElement.getHeight()
            if (currentY + elementHeight >= y) {
                moduleElement.handleDrag(mouseX, mouseY, x, currentY, width)
            }
            currentY += elementHeight + 5F
        }
    }

    fun handleMouseRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        for (moduleElement in moduleElements) {
            moduleElement.handleRelease()
        }
    }

    fun handleKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        for (moduleElement in moduleElements) {
            if (moduleElement.handleKeyTyped(typedChar, keyCode)) {
                return true
            }
        }
        return false
    }

    fun isAnyValueTyping(): Boolean {
        for (moduleElement in moduleElements) {
            if (moduleElement.isAnyValueTyping()) {
                return true
            }
        }
        return false
    }

    fun expandModule(module: Module) {
        var currentY = 0F
        var targetModuleY = 0F
        var foundModule = false

        for (moduleElement in moduleElements) {
            if (moduleElement.module == module) {
                moduleElement.expanded = true
                val expandedHeight = moduleElement.getExpandedHeight()
                moduleElement.animExpanded = expandedHeight - 45F
                targetModuleY = currentY
                foundModule = true
            } else {
                moduleElement.expanded = false
                moduleElement.animExpanded = 0F
            }
            currentY += moduleElement.getHeight() + 5F
        }

        if (foundModule) {
            maxScroll = (currentY - 5F - AuroraGUI.getInstance().panelHeight + 60F).coerceAtLeast(0F)
            targetScroll = targetModuleY.coerceIn(0F, maxScroll)
            animScroll = targetScroll
        }
    }
}

class AuroraModuleElement(val module: Module) {
    private val valueElements = mutableListOf<AuroraValueElement<*>>()
    internal var expanded = false
    internal var animExpanded = 0F
    private var listeningToKey = false

    init {
        for (value in module.values) {
            when (value) {
                is net.ccbluex.liquidbounce.config.BoolValue -> valueElements.add(AuroraBooleanElement(value))
                is net.ccbluex.liquidbounce.config.IntValue -> valueElements.add(AuroraIntElement(value))
                is net.ccbluex.liquidbounce.config.FloatValue -> valueElements.add(AuroraFloatElement(value))
                is net.ccbluex.liquidbounce.config.IntRangeValue -> valueElements.add(AuroraIntRangeElement(value))
                is net.ccbluex.liquidbounce.config.FloatRangeValue -> valueElements.add(AuroraFloatRangeElement(value))
                is net.ccbluex.liquidbounce.config.TextValue -> valueElements.add(AuroraTextElement(value))
                is net.ccbluex.liquidbounce.config.ListValue -> valueElements.add(AuroraListElement(value))
                is net.ccbluex.liquidbounce.config.ColorValue -> valueElements.add(AuroraColorElement(value))
                is net.ccbluex.liquidbounce.config.FontValue -> valueElements.add(AuroraFontElement(value))
                is net.ccbluex.liquidbounce.config.BlockValue -> valueElements.add(AuroraBlockElement(value))
                else -> {}
            }
        }
    }

    fun getHeight(): Float {
        var height = 45F
        if (expanded && valueElements.isNotEmpty()) {
            val targetExpanded = valueElements.filter { it.isDisplayable() }.sumOf { it.getHeight().toDouble() }.toFloat() + 10F
            height += targetExpanded
        }
        return height
    }

    fun getExpandedHeight(): Float {
        var height = 45F
        if (valueElements.isNotEmpty()) {
            val targetExpanded = valueElements.filter { it.isDisplayable() }.sumOf { it.getHeight().toDouble() }.toFloat() + 10F
            height += targetExpanded
        }
        return height
    }

    fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: Color, clipTop: Float, clipBottom: Float) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 45F

        RenderUtils.originalRoundedRect(x, y, x + width, y + 45F, 4F,
            (if (isHovered) Color(40, 40, 45, 220) else Color(35, 35, 40, 200)).rgb)

        if (module.state) {
            RenderUtils.originalRoundedRect(x, y, x + 3F, y + 45F, 2F, accentColor.rgb)
        }

        Fonts.font40.drawString(module.name, x + 15F, y + 8F,
            if (module.state) Color.WHITE.rgb else Color(150, 150, 155).rgb)

        Fonts.font35.drawString(module.description, x + 15F, y + 24F, Color(120, 120, 125).rgb)

        if (module.state) {
            Fonts.font35.drawString("ON", x + width - 70F, y + 15F, accentColor.rgb)
        }

        if (valueElements.isNotEmpty()) {
            val expandChar = if (expanded) "\u25BC" else "\u25B6"
            Fonts.font35.drawString(expandChar, x + width - 20F, y + 15F, Color(120, 120, 125).rgb)
        }

        if (listeningToKey) {
            Fonts.font35.drawString("...", x + width - 100F, y + 15F, Color.YELLOW.rgb)
        }

        if (expanded && valueElements.isNotEmpty()) {
            val displayableValues = valueElements.filter { it.isDisplayable() }
            val targetExpanded = displayableValues.sumOf { it.getHeight().toDouble() }.toFloat() + 10F
            animExpanded += (targetExpanded - animExpanded) * 0.2F

            val contentY = y + 50F
            val contentTop = contentY.coerceAtLeast(clipTop)
            val contentBottom = (contentY + animExpanded).coerceAtMost(clipBottom)

            if (contentTop < contentBottom && contentY < clipBottom) {
                RenderUtils.originalRoundedRect(x + 5F, contentTop, x + width - 5F, contentBottom, 4F, Color(28, 28, 32, 240).rgb)

                var valueY = contentY + 5F
                for (valueElement in displayableValues) {
                    val elementHeight = valueElement.getHeight()
                    if (valueY + elementHeight > clipTop && valueY < clipBottom) {
                        valueElement.drawElement(mouseX, mouseY, x + 10F, valueY, width - 20F, accentColor)
                    }
                    valueY += elementHeight
                }
            }
        } else {
            animExpanded *= 0.8F
        }
    }

    fun handleClick(mouseX: Int, mouseY: Int, mouseButton: Int, x: Float, y: Float, width: Float) {
        if (mouseY >= y && mouseY <= y + 45F) {
            if (mouseButton == 0) {
                if (listeningToKey) {
                    listeningToKey = false
                } else {
                    module.toggle()
                }
            } else if (mouseButton == 1) {
                if (valueElements.isNotEmpty()) {
                    expanded = !expanded
                    if (!expanded) {
                        animExpanded = 0F
                    }
                }
            } else if (mouseButton == 2) {
                listeningToKey = !listeningToKey
            }
            return
        }

        if (expanded) {
            var valueY = y + 50F
            for (valueElement in valueElements) {
                if (valueElement.isDisplayable()) {
                    if (mouseY >= valueY && mouseY <= valueY + valueElement.getHeight()) {
                        valueElement.onClick(mouseX, mouseY, x + 10F, valueY, width - 20F)
                        return
                    }
                    valueY += valueElement.getHeight()
                }
            }
        }
    }

    fun handleDrag(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (expanded) {
            var valueY = y + 50F
            for (valueElement in valueElements) {
                if (valueElement.isDisplayable()) {
                    valueElement.onDrag(mouseX, mouseY, x + 10F, valueY, width - 20F)
                    valueY += valueElement.getHeight()
                }
            }
        }
    }

    fun handleRelease() {
        for (valueElement in valueElements) {
            if (valueElement.isDisplayable()) {
                valueElement.onRelease(0, 0, 0F, 0F, 0F)
            }
        }
    }

    fun handleKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listeningToKey) {
            if (keyCode != Keyboard.KEY_ESCAPE) {
                module.keyBind = keyCode
            }
            listeningToKey = false
            return true
        }

        if (expanded) {
            for (valueElement in valueElements) {
                if (valueElement.isDisplayable() && valueElement.onKeyPress(typedChar, keyCode)) {
                    return true
                }
            }
        }

        return false
    }

    fun isAnyValueTyping(): Boolean {
        if (!expanded) return false
        for (valueElement in valueElements) {
            if (valueElement.isDisplayable() && valueElement.isTyping()) {
                return true
            }
        }
        return false
    }
}
