package net.ccbluex.liquidbounce.utils.extras

import com.jhlabs.image.GaussianFilter
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.HashMap

object GlowUtils2 {
    private val blurCache = HashMap<BlurCacheKey, Int>()
    private var framebuffer: Framebuffer? = null

    /**
     * 绘制亚克力模糊效果
     * @param x 位置X
     * @param y 位置Y
     * @param width 宽度
     * @param height 高度
     * @param blurRadius 模糊半径
     * @param tintColor 色调颜色
     * @param opacity 不透明度 (0.0-1.0)
     */
    fun drawAcrylicBlur(x: Float, y: Float, width: Float, height: Float, blurRadius: Int, tintColor: Color, opacity: Float) {
        if (width <= 0 || height <= 0) return

        val mc = Minecraft.getMinecraft()

        // 确保帧缓冲区存在
        if (framebuffer == null || framebuffer!!.framebufferWidth != mc.displayWidth || framebuffer!!.framebufferHeight != mc.displayHeight) {
            framebuffer?.deleteFramebuffer()
            framebuffer = Framebuffer(mc.displayWidth, mc.displayHeight, true)
        }

        // 捕获当前屏幕内容到帧缓冲区
        captureScreenContent()

        // 应用模糊效果
        applyBlurEffect(x, y, width, height, blurRadius, tintColor, opacity)
    }

    private fun captureScreenContent() {
        val mc = Minecraft.getMinecraft()

        // 绑定帧缓冲区并复制当前屏幕内容
        framebuffer?.bindFramebuffer(true)
        // 使用glCopyTexSubImage2D将当前屏幕内容复制到帧缓冲区纹理
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer?.framebufferTexture ?: return)
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0,
            mc.displayWidth, mc.displayHeight)

        // 恢复主帧缓冲区
        mc.framebuffer.bindFramebuffer(false)
    }

    private fun applyBlurEffect(x: Float, y: Float, width: Float, height: Float, blurRadius: Int, tintColor: Color, opacity: Float) {
        val safeRadius = blurRadius.coerceIn(1, 20)
        val areaWidth = width.toInt()
        val areaHeight = height.toInt()

        val key = BlurCacheKey(areaWidth, areaHeight, safeRadius)

        GL11.glPushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)

        val texId = if (blurCache.containsKey(key)) {
            blurCache[key]!!
        } else {
            // 创建模糊纹理
            val original = BufferedImage(areaWidth, areaHeight, BufferedImage.TYPE_INT_ARGB)

            // 这里应该从帧缓冲区提取对应区域的像素数据
            // 简化实现：创建一个半透明的模糊纹理
            val g = original.graphics
            g.color = Color(tintColor.red, tintColor.green, tintColor.blue, (opacity * 255).toInt())
            g.fillRect(0, 0, areaWidth, areaHeight)
            g.dispose()

            val op = GaussianFilter(safeRadius.toFloat())
            val blurred = op.filter(original, null)

            val newTexId = TextureUtil.glGenTextures()
            TextureUtil.uploadTextureImageAllocate(newTexId, blurred, true, false)
            blurCache[key] = newTexId
            newTexId
        }

        GlStateManager.bindTexture(texId)

        // 设置颜色和透明度
        GlStateManager.color(1.0f, 1.0f, 1.0f, opacity)

        // 绘制模糊区域
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(x, y)
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(x, y + height)
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f(x + width, y + height)
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f(x + width, y)
        GL11.glEnd()

        GlStateManager.disableBlend()
        GlStateManager.resetColor()
        GL11.glPopMatrix()
    }

    fun clearCache() {
        blurCache.values.forEach {
            GlStateManager.deleteTexture(it)
        }
        blurCache.clear()
        framebuffer?.deleteFramebuffer()
        framebuffer = null
    }
}

data class BlurCacheKey(val width: Int, val height: Int, val radius: Int)
