package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11
import java.awt.Color

class ShadowRenderer {
    companion object { fun create() = ShadowRenderer() }
    fun addShadow(x: Float, y: Float, w: Float, h: Float, radius: Float, blur: Float, color: Color) {}
    fun setScissor(x: Int, y: Int, w: Int, h: Int) { ScissorHelper.push(x, y, w, h) }
    fun clearScissor() { ScissorHelper.pop() }
    fun drawAndClear() {}
    fun close() {}
}

class RoundRectRenderer {
    companion object { fun create() = RoundRectRenderer() }
    fun addRoundRect(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Color) {
        RenderUtils.drawRoundedRect(x, y, x + w, y + h, color.rgb, radius, RenderUtils.RoundedCorners.ALL)
    }
    fun addRoundRectGradient(x: Float, y: Float, w: Float, h: Float, r1: Float, r2: Float, r3: Float, r4: Float, c1: Color, c2: Color, c3: Color, c4: Color) {
        RenderUtils.drawRoundedRect(x, y, x + w, y + h, c1.rgb, r1, RenderUtils.RoundedCorners.ALL)
    }
    fun setScissor(x: Int, y: Int, w: Int, h: Int) { ScissorHelper.push(x, y, w, h) }
    fun clearScissor() { ScissorHelper.pop() }
    fun drawAndClear() {}
    fun close() {}
}

class RoundRectOutlineRenderer {
    companion object { fun create() = RoundRectOutlineRenderer() }
    fun addOutline(x: Float, y: Float, w: Float, h: Float, radius: Float, thickness: Float, color: Color) {
        RenderUtils.drawRoundedBorder(x, y, x + w, y + h, thickness, color.rgb, radius)
    }
    fun setScissor(x: Int, y: Int, w: Int, h: Int) { ScissorHelper.push(x, y, w, h) }
    fun clearScissor() { ScissorHelper.pop() }
    fun drawAndClear() {}
    fun close() {}
}

class RectRenderer {
    companion object { fun create() = RectRenderer() }
    fun addRect(x: Float, y: Float, w: Float, h: Float, color: Color) {
        RenderUtils.drawRect(x, y, x + w, y + h, color.rgb)
    }
    fun addRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        RenderUtils.drawRect(x, y, x + w, y + h, color)
    }
    fun setScissor(x: Int, y: Int, w: Int, h: Int) { ScissorHelper.push(x, y, w, h) }
    fun clearScissor() { ScissorHelper.pop() }
    fun drawAndClear() {}
    fun close() {}
}

class TriangleRenderer {
    companion object { fun create() = TriangleRenderer() }
    fun addChevronTriangle(cx: Float, cy: Float, size: Float, progress: Float, color: Color) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(cx, cy, 0f)
        GlStateManager.rotate(progress * 180f, 0f, 0f, 1f)
        drawTriangle(0f, -size / 2f, -size, size / 2f, size, size / 2f, color.rgb)
        GlStateManager.popMatrix()
    }
    
    private fun drawTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int) {
        val alpha = (color shr 24 and 255).toFloat() / 255.0f
        val red = (color shr 16 and 255).toFloat() / 255.0f
        val green = (color shr 8 and 255).toFloat() / 255.0f
        val blue = (color and 255).toFloat() / 255.0f
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(red, green, blue, alpha)
        worldrenderer.begin(7, DefaultVertexFormats.POSITION)
        worldrenderer.pos(x1.toDouble(), y1.toDouble(), 0.0).endVertex()
        worldrenderer.pos(x2.toDouble(), y2.toDouble(), 0.0).endVertex()
        worldrenderer.pos(x3.toDouble(), y3.toDouble(), 0.0).endVertex()
        worldrenderer.pos(x1.toDouble(), y1.toDouble(), 0.0).endVertex()
        tessellator.draw()
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    fun setScissor(x: Int, y: Int, w: Int, h: Int) { ScissorHelper.push(x, y, w, h) }
    fun clearScissor() { ScissorHelper.pop() }
    fun drawAndClear() {}
    fun close() {}
}

class TextRenderer {
    companion object { fun create() = TextRenderer() }
    fun addText(text: String, x: Float, y: Float, scale: Float, color: Color, font: Any? = null) {
        val fr = if (font is GameFontRenderer) font else Fonts.fontSF35
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)
        GlStateManager.scale(scale, scale, 1f)
        fr.drawString(text, 0f, 0f, color.rgb)
        GlStateManager.popMatrix()
    }
    fun getWidth(text: String, scale: Float, font: Any? = null): Float {
        val fr = if (font is GameFontRenderer) font else Fonts.fontSF35
        return fr.getStringWidth(text) * scale
    }
    fun getHeight(scale: Float, font: Any? = null): Float {
        val fr = if (font is GameFontRenderer) font else Fonts.fontSF35
        return fr.FONT_HEIGHT * scale
    }
    fun getLineHeight(scale: Float): Float = getHeight(scale)
    fun setScissor(x: Int, y: Int, w: Int, h: Int) { ScissorHelper.push(x, y, w, h) }
    fun clearScissor() { ScissorHelper.pop() }
    fun drawAndClear() {}
    fun close() {}
}

object ScissorHelper {
    fun push(x: Int, y: Int, w: Int, h: Int) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(x, y, w, h)
    }
    fun pop() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }
}
