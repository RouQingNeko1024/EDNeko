/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.font

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.kotlin.LruCache
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.GlStateManager.bindTexture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SideOnly(Side.CLIENT)
class AWTFontRenderer(
    val font: Font,
    startChar: Int = 0,
    stopChar: Int = 65535,
    private val loadingScreen: Boolean = false
) : MinecraftInstance {

    companion object : Listenable {
        var assumeNonVolatile: Boolean = false

        private val activeFontRenderers = mutableListOf<AWTFontRenderer>()
        
        private val chineseFallbackFonts = mutableMapOf<Int, Font>()
        
        private fun getChineseFallbackFont(size: Int): Font? {
            chineseFallbackFonts[size]?.let { return it }
            
            val fallbackFonts = listOf(
                "Microsoft YaHei",
                "SimHei",
                "SimSun",
                "PingFang SC",
                "Noto Sans CJK SC",
                "WenQuanYi Micro Hei",
                "SansSerif"
            )
            
            for (fallbackName in fallbackFonts) {
                try {
                    val fallbackFont = Font(fallbackName, Font.PLAIN, size)
                    if (fallbackFont.canDisplay('\u4e2d') || fallbackFont.canDisplay('\u6587')) {
                        chineseFallbackFonts[size] = fallbackFont
                        return fallbackFont
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
            return null
        }

        inline fun assumeNonVolatile(block: () -> Unit) {
            assumeNonVolatile = true
            try {
                block()
            } finally {
                assumeNonVolatile = false
            }
        }

        private const val GC_TICKS = 600
        private const val CACHED_FONT_REMOVAL_TIME = 30000L
        private const val MAX_CACHED_STRINGS = 255

        private var gcTicks = 0

        private val onRender2D = handler<Render2DEvent>(priority = Byte.MIN_VALUE) {
            if (++gcTicks > GC_TICKS) {
                activeFontRenderers.forEach { it.collectGarbage() }
                gcTicks = 0
            }
        }
    }

    private data class CharLocation(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    private data class CachedFont(
        val displayList: Int,
        var lastUsage: Long,
        var deleted: Boolean = false
    )

    private val charLocations = mutableMapOf<Int, CharLocation>()
    private val dynamicCharTextures = mutableMapOf<Int, Int>()

    private val cachedStringFonts = LruCache<String, CachedFont>(MAX_CACHED_STRINGS)
    private val cachedStringWidths = LruCache<String, Int>(MAX_CACHED_STRINGS)

    private var textureID: Int = -1
    private var textureWidth: Int = 0
    private var textureHeight: Int = 0
    private var fontHeight: Int = -1

    val height: Int
        get() = (fontHeight - 8) / 2

    private val isChineseFont: Boolean

    init {
        isChineseFont = try {
            font.canDisplay('\u4e2d') || font.canDisplay('\u6587') || font.canDisplay('\u4f60') || font.canDisplay('\u597d') || font.canDisplay('\u4e16')
        } catch (e: Exception) {
            false
        }
        renderBitmap(startChar, minOf(stopChar, 256))
        activeFontRenderers += this
    }

    fun drawString(text: String, x: Double, y: Double, color: Int) {
        glPushMatrix()
        glScaled(0.25, 0.25, 0.25)
        glTranslated(x * 2.0, y * 2.0 - 2.0, 0.0)

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_TEXTURE_2D)

        if (loadingScreen) {
            glBindTexture(GL_TEXTURE_2D, textureID)
        } else {
            bindTexture(textureID)
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        val (alpha, red, green, blue) = ColorUtils.unpackARGBFloatValue(color)
        glColor4f(red, green, blue, alpha)

        val cached = cachedStringFonts[text]
        if (cached != null) {
            glCallList(cached.displayList)
            cached.lastUsage = System.currentTimeMillis()
            glPopMatrix()
            return
        }

        var listID = -1
        if (assumeNonVolatile) {
            listID = glGenLists(1)
            glNewList(listID, GL_COMPILE_AND_EXECUTE)
        }

        glBegin(GL_QUADS)

        var currX = 0f
        var fallbackWidth = 0f

        for (char in text) {
            val charCode = char.code
            var loc = charLocations[charCode]
            
            if (loc == null && charCode > 255) {
                glEnd()
                loc = renderDynamicChar(char)
                if (loadingScreen) {
                    glBindTexture(GL_TEXTURE_2D, textureID)
                } else {
                    bindTexture(textureID)
                }
                glBegin(GL_QUADS)
            }

            if (loc == null) {
                glEnd()
                GlStateManager.resetColor()

                glPushMatrix()
                val rev = 4.0f
                glScalef(rev, rev, rev)
                val scale = font.size / 32.0f
                glScalef(scale, scale, 1.0f)

                mc.fontRendererObj.posY = 1.0f
                mc.fontRendererObj.posX = currX / rev

                val fallbackW = mc.fontRendererObj.renderUnicodeChar(char, false).coerceAtLeast(0f)
                fallbackWidth += fallbackW

                if (loadingScreen) {
                    glBindTexture(GL_TEXTURE_2D, textureID)
                } else {
                    bindTexture(textureID)
                }
                glPopMatrix()
                glBegin(GL_QUADS)
                
                currX += fallbackW * rev
                mc.fontRendererObj.posX = currX / rev
            } else {
                drawChar(loc, currX, 0f, charCode)
                currX += (loc.width - 8f)
            }
        }

        glEnd()

        if (assumeNonVolatile && listID >= 0) {
            cachedStringFonts[text] = CachedFont(listID, System.currentTimeMillis())
            glEndList()
        }

        glPopMatrix()
    }

    private fun renderDynamicChar(c: Char): CharLocation? {
        val charCode = c.code
        
        var useFont = font
        if (!font.canDisplay(c)) {
            val fallback = getChineseFallbackFont(font.size)
            if (fallback != null && fallback.canDisplay(c)) {
                useFont = fallback
            } else {
                return null
            }
        }

        try {
            val measureImg = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            val measureG = measureImg.createGraphics()
            measureG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            measureG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            measureG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            measureG.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            measureG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            measureG.font = useFont

            val fm = measureG.fontMetrics
            var w = fm.charWidth(c) + 8
            if (w <= 0) w = 7
            var h = fm.height + 3
            if (h <= 0) h = useFont.size
            measureG.dispose()

            val charImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val g2d = charImg.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g2d.font = useFont
            g2d.color = Color.WHITE
            g2d.drawString(c.toString(), 3, 1 + fm.ascent)
            g2d.dispose()

            val texId = TextureUtil.glGenTextures()
            glBindTexture(GL_TEXTURE_2D, texId)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            TextureUtil.uploadTextureImageAllocate(texId, charImg, true, true)
            dynamicCharTextures[charCode] = texId

            val loc = CharLocation(0, 0, w, h)
            charLocations[charCode] = loc

            if (h > fontHeight) {
                fontHeight = h
            }

            return loc
        } catch (e: Exception) {
            return null
        }
    }

    fun getStringWidth(text: String): Int = cachedStringWidths.getOrPut(text) {
        var myWidth = 0
        var fallbackWidth = 0f
        val fallbackScale = font.size / 32f

        for (char in text) {
            val charCode = char.code
            var loc = charLocations[charCode]
            
            if (loc == null && charCode > 255) {
                loc = renderDynamicChar(char)
            }
            
            if (loc == null) {
                val w = mc.fontRendererObj.getCharWidth(char)
                fallbackWidth += ((w + 8) * fallbackScale).coerceAtLeast(0f)
            } else {
                myWidth += (loc.width - 8)
            }
        }

        (myWidth / 2) + fallbackWidth.roundToInt()
    }

    fun dispose() {
        if (textureID != -1) {
            glDeleteTextures(textureID)
            textureID = -1
        }
        dynamicCharTextures.values.forEach { glDeleteTextures(it) }
        dynamicCharTextures.clear()
        activeFontRenderers.remove(this)
    }

    protected fun finalize() {
        dispose()
    }

    private fun drawChar(loc: CharLocation, x: Float, y: Float, charCode: Int = -1) {
        val dynamicTexId = if (charCode >= 0) dynamicCharTextures[charCode] else null
        if (dynamicTexId != null) {
            glEnd()
            bindTexture(dynamicTexId)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glBegin(GL_QUADS)
        }

        val w = loc.width.toDouble()
        val h = loc.height.toDouble()
        val xd = x.toDouble()
        val yd = y.toDouble()

        if (dynamicTexId != null) {
            glTexCoord2d(0.0, 0.0)
            glVertex2d(xd, yd)

            glTexCoord2d(0.0, 1.0)
            glVertex2d(xd, yd + h)

            glTexCoord2d(1.0, 1.0)
            glVertex2d(xd + w, yd + h)

            glTexCoord2d(1.0, 0.0)
            glVertex2d(xd + w, yd)
        } else {
            val u = loc.x.toDouble() / textureWidth
            val v = loc.y.toDouble() / textureHeight
            val uw = w / textureWidth
            val vh = h / textureHeight

            glTexCoord2d(u, v)
            glVertex2d(xd, yd)

            glTexCoord2d(u, v + vh)
            glVertex2d(xd, yd + h)

            glTexCoord2d(u + uw, v + vh)
            glVertex2d(xd + w, yd + h)

            glTexCoord2d(u + uw, v)
            glVertex2d(xd + w, yd)
        }

        if (dynamicTexId != null) {
            glEnd()
            if (loadingScreen) {
                glBindTexture(GL_TEXTURE_2D, textureID)
            } else {
                bindTexture(textureID)
            }
            glBegin(GL_QUADS)
        }
    }

    private fun renderBitmap(startChar: Int, stopChar: Int) {
        val fontImages = mutableMapOf<Int, BufferedImage>()

        var rowHeight = 0
        var charX = 0
        var charY = 0

        for (charCode in startChar until stopChar) {
            val charImg = drawCharToImage(charCode.toChar())
            val cw = charImg.width
            val ch = charImg.height

            if (ch > fontHeight) {
                fontHeight = ch
            }

            val loc = CharLocation(charX, charY, cw, ch)
            charLocations[charCode] = loc
            fontImages[charCode] = charImg

            charX += cw
            if (cw > 0 && ch > rowHeight) {
                rowHeight = ch
            }
            if (charX > 2048) {
                if (charX > textureWidth)
                    textureWidth = charX
                charX = 0
                charY += rowHeight
                rowHeight = 0
            }
        }
        textureWidth = max(textureWidth, charX)
        textureHeight = charY + rowHeight

        val bigImage = BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB)
        val g = bigImage.createGraphics()
        g.font = font
        g.color = Color(255, 255, 255, 0)
        g.fillRect(0, 0, textureWidth, textureHeight)
        g.color = Color.WHITE
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        for (charCode in startChar until stopChar) {
            val subImg = fontImages[charCode] ?: continue
            val loc = charLocations[charCode] ?: continue
            g.drawImage(subImg, loc.x, loc.y, null)
        }

        textureID = TextureUtil.glGenTextures()
        glBindTexture(GL_TEXTURE_2D, textureID)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        TextureUtil.uploadTextureImageAllocate(textureID, bigImage, true, true)
    }

    private fun drawCharToImage(c: Char): BufferedImage {
        val measureImg = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val measureG = measureImg.createGraphics()
        measureG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        measureG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        measureG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        measureG.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        measureG.font = font

        val fm = measureG.fontMetrics
        var w = fm.charWidth(c) + 8
        if (w <= 0) w = 7
        var h = fm.height + 3
        if (h <= 0) h = font.size

        val charImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g2d = charImg.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g2d.font = font
        g2d.color = Color.WHITE
        g2d.drawString(c.toString(), 3, 1 + fm.ascent)
        return charImg
    }

    private fun collectGarbage() {
        val now = System.currentTimeMillis()

        with(cachedStringFonts.entries.iterator()) {
            while (hasNext()) {
                val cached = next().value
                if (!cached.deleted && (now - cached.lastUsage) > CACHED_FONT_REMOVAL_TIME) {
                    glDeleteLists(cached.displayList, 1)
                    cached.deleted = true
                    remove()
                }
            }
        }
    }
}
