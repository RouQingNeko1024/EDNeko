/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.client.NewGUI
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.AuroraCategoryElement
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.AuroraSearchBox
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Mouse
import java.awt.Color

class AuroraGUI : GuiScreen() {
    companion object {
        private var instance: AuroraGUI? = null
        fun getInstance(): AuroraGUI = instance ?: AuroraGUI().also { instance = it }

        var hueOffset = 0F

        fun getAuroraColor(offset: Float = 0F): Color {
            val baseColor = NewGUI.accentColor
            if (baseColor != null) {
                when (NewGUI.colorModeValue) {
                    "Custom" -> return baseColor
                    "Fade" -> return ColorUtils.fade(baseColor, (offset * 100).toInt(), 100)
                }
            }
            val hue = (System.currentTimeMillis() % 10000L) / 10000F + offset
            return Color.getHSBColor(hue, 0.6F, 1F)
        }

        fun getGradientColor(x: Float, width: Float): Color {
            val progress = x / width
            val hue = (System.currentTimeMillis() % 5000L) / 5000F + progress * 0.3F
            return Color.getHSBColor(hue % 1F, 0.7F, 0.95F)
        }
    }

    private val categoryElements = mutableListOf<AuroraCategoryElement>()
    private var searchBox: AuroraSearchBox? = null
    private var selectedCategoryIndex = 0
    private var categoryScroll = 0F
    private var categoryTargetScroll = 0F
    private var maxCategoryScroll = 0F

    private var isDragging = false
    private var dragOffsetX = 0F
    private var dragOffsetY = 0F
    private var customPanelX = 0F
    private var customPanelY = 30F
    private var hasCustomPosition = false

    private var lastScaledWidth = 0
    private var lastScaledHeight = 0
    private var lastScaleWidth = 0F
    private var lastScaleHeight = 0F
    private var cachedPanelX = 0F
    private var cachedPanelY = 30F
    private var cachedPanelWidth = 0F
    private var cachedPanelHeight = 0F

    val panelX: Float 
        get() = if (hasCustomPosition) customPanelX else cachedPanelX
    val panelY: Float 
        get() = if (hasCustomPosition) customPanelY else 30F
    val panelWidth: Float get() = cachedPanelWidth
    val panelHeight: Float get() = cachedPanelHeight
    val categoryWidth = 100F
    val contentX: Float get() = panelX + categoryWidth + 20F
    val contentWidth: Float get() = panelWidth - categoryWidth - 40F
    val contentY: Float get() = panelY + 40F

    init {
        Category.values().filter { it.shouldShow() }.forEach { category ->
            categoryElements.add(AuroraCategoryElement(category))
        }
        searchBox = AuroraSearchBox(LiquidBounce.moduleManager)
        searchBox?.onExpandModule = { module ->
            val categoryIndex = categoryElements.indexOfFirst { it.category == module.category }
            if (categoryIndex >= 0) {
                selectedCategoryIndex = categoryIndex
                searchBox?.clearSearch()
                categoryElements[categoryIndex].expandModule(module)
            }
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val sr = ScaledResolution(mc)
        val width = sr.scaledWidth.toFloat()
        val height = sr.scaledHeight.toFloat()

        if (sr.scaledWidth != lastScaledWidth || sr.scaledHeight != lastScaledHeight ||
            NewGUI.scaleWidth != lastScaleWidth || NewGUI.scaleHeight != lastScaleHeight) {
            lastScaledWidth = sr.scaledWidth
            lastScaledHeight = sr.scaledHeight
            lastScaleWidth = NewGUI.scaleWidth
            lastScaleHeight = NewGUI.scaleHeight
            cachedPanelX = width * ((1F - NewGUI.scaleWidth) / 2F)
            cachedPanelWidth = width * NewGUI.scaleWidth
            cachedPanelHeight = height * NewGUI.scaleHeight
        }

        hueOffset += 0.001F
        if (hueOffset > 1F) hueOffset = 0F

        drawAuroraBackground(width, height)

        drawMainPanel(panelX, panelY, panelWidth, panelHeight)

        val wheel = Mouse.getDWheel()
        val catBarX = panelX + 10F
        val catBarY = panelY + 10F
        val catBarHeight = panelHeight - 20F

        if (wheel != 0 && mouseX >= catBarX && mouseX <= catBarX + categoryWidth &&
            mouseY >= catBarY && mouseY <= catBarY + catBarHeight) {
            categoryTargetScroll -= wheel * 0.5F
            categoryTargetScroll = categoryTargetScroll.coerceIn(0F, maxCategoryScroll)
        }

        drawCategoryBar(catBarX, catBarY, categoryWidth, catBarHeight, mouseX, mouseY)

        val moduleWheel = if (mouseX >= catBarX && mouseX <= catBarX + categoryWidth &&
            mouseY >= catBarY && mouseY <= catBarY + catBarHeight) 0 else wheel

        if (searchBox?.isSearching == true) {
            searchBox?.drawSearchResults(mouseX, mouseY, contentX, contentY, contentWidth, panelHeight - 60F, getAuroraColor())
        } else if (selectedCategoryIndex < categoryElements.size) {
            categoryElements[selectedCategoryIndex].drawPanel(
                mouseX, mouseY, contentX, contentY, contentWidth, panelHeight - 60F, moduleWheel
            )
        }

        searchBox?.draw(mouseX, mouseY, contentX, panelY + 10F, contentWidth)

        drawAuroraTitle(panelX, panelY - 25F, panelWidth)

        handleDrag(mouseX, mouseY)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun handleDrag(mouseX: Int, mouseY: Int) {
        if (isDragging && Mouse.isButtonDown(0)) {
            hasCustomPosition = true
            customPanelX = mouseX - dragOffsetX
            customPanelY = mouseY - dragOffsetY
        }
        
        if (!Mouse.isButtonDown(0)) {
            isDragging = false
            if (searchBox?.isSearching != true && selectedCategoryIndex < categoryElements.size) {
                categoryElements[selectedCategoryIndex].handleMouseRelease(
                    mouseX, mouseY, contentX, contentY, contentWidth
                )
            }
        } else {
            if (searchBox?.isSearching != true && selectedCategoryIndex < categoryElements.size) {
                categoryElements[selectedCategoryIndex].handleMouseDrag(
                    mouseX, mouseY, contentX, contentY, contentWidth
                )
            }
        }
    }

    private fun drawAuroraBackground(width: Float, height: Float) {
        drawDefaultBackground()
    }

    private fun drawMainPanel(x: Float, y: Float, width: Float, height: Float) {
        for (i in 10 downTo 1) {
            val alpha = (40 * (1F - i / 10F)).toInt()
            RenderUtils.originalRoundedRect(x - i, y - i, x + width + i, y + height + i, 10F, Color(0, 0, 0, alpha).rgb)
        }

        RenderUtils.originalRoundedRect(x, y, x + width, y + height, 8F, Color(20, 20, 25, 240).rgb)

        val gradientHeight = 3F
        for (i in 0 until width.toInt() step 2) {
            val color = getGradientColor(x + i, width)
            RenderUtils.drawRect(x + i, y, x + i + 2, y + gradientHeight, color.rgb)
        }
    }

    private fun drawCategoryBar(x: Float, y: Float, width: Float, height: Float, mouseX: Int, mouseY: Int) {
        RenderUtils.originalRoundedRect(x, y, x + width, y + height, 6F, Color(30, 30, 35, 200).rgb)

        val totalHeight = categoryElements.size * 35F
        maxCategoryScroll = (totalHeight - height + 20F).coerceAtLeast(0F)

        categoryScroll += (categoryTargetScroll - categoryScroll) * 0.15F

        var categoryY = y + 10F - categoryScroll
        categoryElements.forEachIndexed { index, element ->
            if (categoryY + 28F >= y && categoryY <= y + height) {
                val isSelected = index == selectedCategoryIndex
                val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= categoryY && mouseY <= categoryY + 30F

                if (isSelected) {
                    val color = getAuroraColor(index * 0.1F)
                    RenderUtils.originalRoundedRect(x + 5F, categoryY, x + width - 5F, categoryY + 28F, 4F, Color(color.red, color.green, color.blue, 60).rgb)
                } else if (isHovered) {
                    RenderUtils.originalRoundedRect(x + 5F, categoryY, x + width - 5F, categoryY + 28F, 4F, Color(50, 50, 55, 150).rgb)
                }

                val text = element.category.displayName
                val textColor = if (isSelected) getAuroraColor(index * 0.1F) else Color(180, 180, 185)
                Fonts.font40.drawString(text, x + 10F, categoryY + 10F, textColor.rgb)
            }

            categoryY += 35F
        }
    }

    private fun drawAuroraTitle(x: Float, y: Float, width: Float) {
        val title = "Air Client"
        val titleWidth = Fonts.font52.getStringWidth(title)
        val titleX = x + (width - titleWidth) / 2F

        for (i in title.indices) {
            val char = title[i].toString()
            val charX = titleX + Fonts.font52.getStringWidth(title.substring(0, i))
            val color = getAuroraColor(i * 0.08F)
            Fonts.font52.drawString(char, charX, y, color.rgb)
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton == 0) {
            if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
                mouseY >= panelY - 25F && mouseY <= panelY) {
                isDragging = true
                dragOffsetX = mouseX - panelX
                dragOffsetY = mouseY - panelY
                return
            }
        }

        val catBarX = panelX + 10F
        val catBarY = panelY + 10F
        val catBarHeight = panelHeight - 20F

        if (mouseButton == 0) {
            var categoryY = catBarY - categoryScroll
            categoryElements.forEachIndexed { index, _ ->
                if (categoryY + 28F >= catBarY && categoryY <= catBarY + catBarHeight) {
                    if (mouseX >= catBarX && mouseX <= catBarX + categoryWidth &&
                        mouseY >= categoryY && mouseY <= categoryY + 30F) {
                        selectedCategoryIndex = index
                        searchBox?.clearSearch()
                        return
                    }
                }
                categoryY += 35F
            }

            if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
                mouseY >= panelY + 10F && mouseY <= panelY + 35F) {
                searchBox?.handleClick(mouseX, mouseY, mouseButton, contentX, panelY + 10F)
                return
            }
        }

        if (searchBox?.isSearching == true) {
            if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
                mouseY >= contentY && mouseY <= panelY + panelHeight) {
                searchBox?.handleModuleClick(mouseX, mouseY, mouseButton, contentX, contentY, contentWidth)
            }
        } else if (selectedCategoryIndex < categoryElements.size) {
            if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
                mouseY >= contentY && mouseY <= panelY + panelHeight) {
                categoryElements[selectedCategoryIndex].handleMouseClick(
                    mouseX, mouseY, mouseButton, contentX, contentY, contentWidth
                )
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (isDragging && clickedMouseButton == 0) {
            hasCustomPosition = true
            customPanelX = mouseX - dragOffsetX
            customPanelY = mouseY - dragOffsetY
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        isDragging = false
        super.mouseReleased(mouseX, mouseY, state)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (searchBox?.isTyping == true) {
            if (searchBox?.handleKeyTyped(typedChar, keyCode) == true) {
                return
            }
        }

        if (selectedCategoryIndex < categoryElements.size) {
            if (categoryElements[selectedCategoryIndex].isAnyValueTyping()) {
                if (categoryElements[selectedCategoryIndex].handleKeyTyped(typedChar, keyCode)) {
                    return
                }
            }
        }

        super.keyTyped(typedChar, keyCode)
    }

    override fun doesGuiPauseGame(): Boolean = false
}
