/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element

import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.ColorManager
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.IconManager
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element.module.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.extensions.animSmooth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11

import java.awt.Color
import java.util.List

class SearchElement {

    private var scrollHeight = 0F
    private var animScrollHeight = 0F
    private var lastHeight = 0F

    private var searchBox: SearchBox? = null
    private var currentX = 0F
    private var currentY = 0F
    private var currentWidth = 0F
    private var currentHeight = 0F

    private fun updatePosition(x: Float, y: Float, width: Float, height: Float) {
        if (x != currentX || y != currentY || width != currentWidth || height != currentHeight) {
            currentX = x
            currentY = y
            currentWidth = width
            currentHeight = height
            if (width > 0 && height > 0) {
                searchBox = SearchBox(0, x.toInt() + 2, y.toInt() + 2, width.toInt() - 4, height.toInt() - 2)
            }
        }
    }

    fun drawBox(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, height: Float, accentColor: Color): Boolean {
        updatePosition(x, y, width, height)
        
        if (searchBox == null) return false
        
        RenderUtils.originalRoundedRect(x - 0.5F, y - 0.5F, x + width + 0.5F, y + height + 0.5F, 4F, ColorManager.buttonOutline.rgb)
        Stencil.write(true)
        RenderUtils.originalRoundedRect(x, y, x + width, y + height, 4F, ColorManager.textBox.rgb)
        Stencil.erase(true)
        
        val sb = searchBox!!
        if (sb.isFocused) {
            RenderUtils.drawRoundedRect(x, y + height - 1F, x + width, y + height, accentColor.rgb, 0.0F)
            sb.drawTextBox()
        } else if (sb.text.length <= 0) {
            sb.text = "Search"
            sb.drawTextBox()
            sb.text = ""
        } else {
            sb.drawTextBox()
        }

        Stencil.dispose()
        GlStateManager.disableAlpha()
        RenderUtils.drawImage(IconManager.search, (x + width - 15).toInt(), (y + 5).toInt(), 10, 10)
        GlStateManager.enableAlpha()
        return sb.text.length > 0
    }

    fun drawPanel(mX: Int, mY: Int, x: Float, y: Float, w: Float, h: Float, wheel: Int, ces: List<CategoryElement>, accentColor: Color) {
        var mouseX = mX
        var mouseY = mY
        lastHeight = 0F
        for (ce in ces) {
            for (me in ce.moduleElements) {
                if (me.module.name.startsWith(searchBox?.text ?: "", true))
                    lastHeight += me.animHeight + 40F
            }
        }
        if (lastHeight >= 10F) lastHeight -= 10F
        handleScrolling(wheel, h)
        drawScroll(x, y + 50F, w, h)
        Fonts.font52.drawString("Search", x + 10F, y + 10F, -1)
        Fonts.font30.drawString("Search", x - 170F, y - 12F, -1)
        RenderUtils.drawImage(IconManager.back, x.toInt() - 190, y.toInt() - 15, 10, 10)
        var startY = y + 50F
        if (mouseY < y + 50F || mouseY >= y + h)
            mouseY = -1
        RenderUtils.makeScissorBox(x, y + 50F, x + w, y + h)
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        for (ce in ces) {
            for (me in ce.moduleElements) {
                if (me.module.name.startsWith(searchBox?.text ?: "", true)) {
                    if (startY + animScrollHeight > y + h || startY + animScrollHeight + 40F + me.animHeight < y + 50F)
                        startY += 40F + me.animHeight
                    else
                        startY += me.drawElement(mouseX, mouseY, x, startY + animScrollHeight, w, 40F, accentColor)
                }
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    private fun handleScrolling(wheel: Int, height: Float) {
        if (wheel != 0) {
            if (wheel > 0)
                scrollHeight += 50F
            else
                scrollHeight -= 50F
        }
        if (lastHeight > height - 60F)
            scrollHeight = scrollHeight.coerceIn(-lastHeight + height - 60F, 0F)
        else
            scrollHeight = 0F
        animScrollHeight = animScrollHeight.animSmooth(scrollHeight, 0.5F)
    }

    private fun drawScroll(x: Float, y: Float, w: Float, h: Float) {
        if (lastHeight <= h - 60F) return
        val scrollBarHeight = (h - 60F) * (h - 60F) / lastHeight
        val scrollY = y + (-animScrollHeight / lastHeight) * (h - 60F)
        RenderUtils.drawRect(x + w - 5F, scrollY, x + w - 2F, scrollY + scrollBarHeight, Color(200, 200, 200, 150).rgb)
    }

    fun handleMouseClick(mouseX: Int, mouseY: Int, mouseButton: Int, x: Float, y: Float, w: Float, h: Float, ces: List<CategoryElement>) {
        searchBox?.mouseClicked(mouseX, mouseY, mouseButton)
        
        if (mouseButton == 0 && mouseX >= x.toInt() - 190 && mouseX <= x.toInt() - 180 && mouseY >= y.toInt() - 15 && mouseY <= y.toInt() - 5) {
            searchBox?.text = ""
        }
        
        if (mouseY < y + 50F) return
        
        var startY = y + 50F
        for (ce in ces) {
            for (me in ce.moduleElements) {
                if (me.module.name.startsWith(searchBox?.text ?: "", true)) {
                    val moduleHeight = 40F + me.animHeight
                    if (startY + animScrollHeight > y + h || startY + animScrollHeight + moduleHeight < y + 50F) {
                        startY += moduleHeight
                    } else {
                        me.handleClick(mouseX, mouseY, x, startY + animScrollHeight, w, 40F)
                        startY += moduleHeight
                    }
                }
            }
        }
    }

    fun handleMouseRelease(mouseX: Int, mouseY: Int, state: Int, x: Float, y: Float, w: Float, h: Float, ces: List<CategoryElement>) {
        var startY = y + 50F
        for (ce in ces) {
            for (me in ce.moduleElements) {
                if (me.module.name.startsWith(searchBox?.text ?: "", true)) {
                    val moduleHeight = 40F + me.animHeight
                    if (startY + animScrollHeight > y + h || startY + animScrollHeight + moduleHeight < y + 50F) {
                        startY += moduleHeight
                    } else {
                        me.handleRelease(mouseX, mouseY, x, startY + animScrollHeight, w, 40F)
                        startY += moduleHeight
                    }
                }
            }
        }
    }

    fun handleMouseDrag(mouseX: Int, mouseY: Int, state: Int, x: Float, y: Float, w: Float, h: Float, ces: List<CategoryElement>) {
        var startY = y + 50F
        for (ce in ces) {
            for (me in ce.moduleElements) {
                if (me.module.name.startsWith(searchBox?.text ?: "", true)) {
                    val moduleHeight = 40F + me.animHeight
                    if (startY + animScrollHeight > y + h || startY + animScrollHeight + moduleHeight < y + 50F) {
                        startY += moduleHeight
                    } else {
                        me.handleDrag(mouseX, mouseY, x, startY + animScrollHeight, w, 40F)
                        startY += moduleHeight
                    }
                }
            }
        }
    }

    fun handleTyping(typedChar: Char, keyCode: Int, x: Float, y: Float, w: Float, h: Float, ces: List<CategoryElement>): Boolean {
        var startY = y + 50F
        for (ce in ces) {
            for (me in ce.moduleElements) {
                if (me.module.name.startsWith(searchBox?.text ?: "", true)) {
                    val moduleHeight = 40F + me.animHeight
                    if (startY + animScrollHeight <= y + h && startY + animScrollHeight + moduleHeight >= y + 50F) {
                        if (me.handleKeyTyped(typedChar, keyCode)) {
                            return true
                        }
                    }
                    startY += moduleHeight
                }
            }
        }
        
        if (searchBox?.isFocused == true) {
            val prevText = searchBox?.text
            searchBox?.textboxKeyTyped(typedChar, keyCode)
            return searchBox?.text != prevText
        }
        
        return false
    }

    fun isTyping(): Boolean {
        return searchBox?.isFocused ?: false
    }

    fun hasSearchContent(): Boolean {
        return !searchBox?.text.isNullOrEmpty()
    }
    
    fun clearFocus() {
        searchBox?.isFocused = false
    }
    
    fun isAnyModuleTyping(ces: List<CategoryElement>): Boolean {
        for (ce in ces) {
            for (me in ce.moduleElements) {
                if (me.module.name.startsWith(searchBox?.text ?: "", true)) {
                    if (me.isAnyValueTyping()) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    fun isAnyKeybindListening(ces: List<CategoryElement>): Boolean {
        for (ce in ces) {
            for (me in ce.moduleElements) {
                if (me.module.name.startsWith(searchBox?.text ?: "", true)) {
                    if (me.listeningKeybind()) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
