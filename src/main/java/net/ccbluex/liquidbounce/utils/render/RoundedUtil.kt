/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * skid FDP Client
 * https://github.com/SkidderMC/FDPClient
 */
package net.ccbluex.liquidbounce.utils.render

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11
import java.awt.Color

object RoundedUtil {
    
    @JvmStatic
    fun drawRound(x: Float, y: Float, width: Float, height: Float, radius: Float, color: Color) {
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.shadeModel(GL11.GL_SMOOTH)
        
        val alpha = color.alpha / 255f
        val red = color.red / 255f
        val green = color.green / 255f
        val blue = color.blue / 255f
        
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        
        worldRenderer.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION_TEX_COLOR)
        
        val degree = Math.PI / 180
        
        var i = 0.0
        while (i <= 90) {
            val angle = i * degree
            val x1 = x + width - radius + Math.sin(angle) * radius
            val y1 = y + height - radius + Math.cos(angle) * radius
            worldRenderer.pos(x1, y1, 0.0).tex(0.0, 0.0).color(red, green, blue, alpha).endVertex()
            i += 1.0
        }
        
        i = 90.0
        while (i <= 180) {
            val angle = i * degree
            val x1 = x + width - radius + Math.sin(angle) * radius
            val y1 = y + radius + Math.cos(angle) * radius
            worldRenderer.pos(x1, y1, 0.0).tex(0.0, 0.0).color(red, green, blue, alpha).endVertex()
            i += 1.0
        }
        
        i = 180.0
        while (i <= 270) {
            val angle = i * degree
            val x1 = x + radius + Math.sin(angle) * radius
            val y1 = y + radius + Math.cos(angle) * radius
            worldRenderer.pos(x1, y1, 0.0).tex(0.0, 0.0).color(red, green, blue, alpha).endVertex()
            i += 1.0
        }
        
        i = 270.0
        while (i <= 360) {
            val angle = i * degree
            val x1 = x + radius + Math.sin(angle) * radius
            val y1 = y + height - radius + Math.cos(angle) * radius
            worldRenderer.pos(x1, y1, 0.0).tex(0.0, 0.0).color(red, green, blue, alpha).endVertex()
            i += 1.0
        }
        
        tessellator.draw()
        
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.shadeModel(GL11.GL_FLAT)
    }

    @JvmStatic
    fun applyGradientHorizontal(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        c1: Color,
        c2: Color,
        callback: Runnable
    ) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.shadeModel(GL11.GL_SMOOTH)
        
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        
        worldRenderer.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION_TEX_COLOR)
        
        val degree = Math.PI / 180
        val alpha1 = c1.alpha / 255f
        val red1 = c1.red / 255f
        val green1 = c1.green / 255f
        val blue1 = c1.blue / 255f
        val alpha2 = c2.alpha / 255f
        val red2 = c2.red / 255f
        val green2 = c2.green / 255f
        val blue2 = c2.blue / 255f
        
        var i = 0.0
        while (i <= 90) {
            val angle = i * degree
            val x1 = x + width - radius + Math.sin(angle) * radius
            val y1 = y + height - radius + Math.cos(angle) * radius
            worldRenderer.pos(x1, y1, 0.0).tex(0.0, 0.0).color(red2, green2, blue2, alpha2).endVertex()
            i += 1.0
        }
        
        i = 90.0
        while (i <= 180) {
            val angle = i * degree
            val x1 = x + width - radius + Math.sin(angle) * radius
            val y1 = y + radius + Math.cos(angle) * radius
            worldRenderer.pos(x1, y1, 0.0).tex(0.0, 0.0).color(red2, green2, blue2, alpha2).endVertex()
            i += 1.0
        }
        
        i = 180.0
        while (i <= 270) {
            val angle = i * degree
            val x1 = x + radius + Math.sin(angle) * radius
            val y1 = y + radius + Math.cos(angle) * radius
            worldRenderer.pos(x1, y1, 0.0).tex(0.0, 0.0).color(red1, green1, blue1, alpha1).endVertex()
            i += 1.0
        }
        
        i = 270.0
        while (i <= 360) {
            val angle = i * degree
            val x1 = x + radius + Math.sin(angle) * radius
            val y1 = y + height - radius + Math.cos(angle) * radius
            worldRenderer.pos(x1, y1, 0.0).tex(0.0, 0.0).color(red1, green1, blue1, alpha1).endVertex()
            i += 1.0
        }
        
        tessellator.draw()
        
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.shadeModel(GL11.GL_FLAT)
        GlStateManager.popMatrix()
        
        callback.run()
    }
}
