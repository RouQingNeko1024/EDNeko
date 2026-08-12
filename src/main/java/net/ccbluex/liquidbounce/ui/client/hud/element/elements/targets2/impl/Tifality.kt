package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.BlendUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.awt.Color

class Tifality(inst: Target2) : TargetStyle("Tifality", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        
        val nameWidth = Fonts.font40.getStringWidth(entity.name)
        val bgWidth = if (nameWidth > 70) 124f + nameWidth - 70f else 124f
        
        RenderUtils.drawRect(0f, 0f, bgWidth, 40f, Color(0, 0, 0, 180).rgb)
        RenderUtils.drawBorderedRect(0f, 0f, bgWidth, 40f, 1f, Color(60, 60, 60).rgb, Color(0, 0, 0, 0).rgb)
        
        Fonts.font40.drawString(entity.name, 42f, 4f, Color.WHITE.rgb)
        
        val health = entity.health
        val totalHealth = entity.health + entity.absorptionAmount
        val fractions = floatArrayOf(0.0f, 0.5f, 1.0f)
        val colors: Array<Color> = arrayOf(Color.RED, Color.YELLOW, Color.GREEN)
        val progress = health / entity.maxHealth
        val customColor: Color = if (health >= 0.0f) 
            ColorUtils.blendColors(fractions, colors, progress).brighter() 
        else 
            Color.RED
        
        val barWidth = 50.0 * progress
        RenderUtils.drawRect(42.5f, 20f, 53f + barWidth.toFloat(), 24f, customColor.rgb)
        
        if (entity.absorptionAmount > 0.0f) {
            RenderUtils.drawRect(97.5f - entity.absorptionAmount, 20f, 103.5f, 24f, Color(137, 112, 9).rgb)
        }
        
        RenderUtils.drawBorderedRect(42f, 19.5f, 104f, 24.5f, 0.5f, Color(0, 0, 0).rgb, Color(0, 0, 0, 0).rgb)
        
        for (dist in 1..9) {
            val dThing = 50.0 / 8.5 * dist
            RenderUtils.drawRect(43.5f + dThing.toFloat(), 19.5f, 44f + dThing.toFloat(), 24.5f, Color(0, 0, 0).rgb)
        }
        
        val distance = mc.thePlayer.getDistanceToEntity(entity).toInt()
        val str = "HP: ${totalHealth.toInt()} | Dist: $distance"
        Fonts.font35.drawString(str, 42.6f, 28f, Color.WHITE.rgb)
        
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        
        drawArmor(entity)
        
        GlStateManager.resetColor()
        RenderUtils.drawEntityOnScreen(20.0, 35.0, 15f, entity)
    }

    override fun handleBlur(entity: EntityPlayer) {
        val nameWidth = Fonts.font40.getStringWidth(entity.name)
        val bgWidth = if (nameWidth > 70) 124f + nameWidth - 70f else 124f
        
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderUtils.quickDrawRect(0F, 0F, bgWidth, 40F)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val nameWidth = Fonts.font40.getStringWidth(entity.name)
        val bgWidth = if (nameWidth > 70) 124f + nameWidth - 70f else 124f
        
        RenderUtils.quickDrawRect(0F, 0F, bgWidth, 40F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        entity ?: return Border(0f, 0f, 124F, 40F)
        
        val nameWidth = Fonts.font40.getStringWidth(entity.name)
        val bgWidth = if (nameWidth > 70) 124f + nameWidth - 70f else 124f
        
        return Border(0F, 0F, bgWidth, 40F)
    }
    
    private fun drawArmor(entity: EntityPlayer) {
        val x = 42
        var y = 32
        
        GlStateManager.pushMatrix()
        RenderHelper.enableGUIStandardItemLighting()
        
        for (index in 3 downTo 0) {
            val stack = entity.inventory.armorInventory[index] ?: continue
            if (stack.item == null) continue
            
            mc.renderItem.renderItemIntoGUI(stack, x + index * 16, y)
            mc.renderItem.renderItemOverlays(mc.fontRendererObj, stack, x + index * 16, y)
        }
        
        val mainStack = entity.heldItem
        if (mainStack != null && mainStack.item != null) {
            mc.renderItem.renderItemIntoGUI(mainStack, x + 4 * 16, y)
            mc.renderItem.renderItemOverlays(mc.fontRendererObj, mainStack, x + 4 * 16, y)
        }
        
        RenderHelper.disableStandardItemLighting()
        GlStateManager.popMatrix()
    }
}
