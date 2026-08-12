package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.darker
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Astolfo(inst: Target2) : TargetStyle("Astolfo", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val colors = targetInstance.barColor.rgb
        val colors1 = targetInstance.barColor.darker(1.0f).rgb
        val colors2 = targetInstance.barColor.darker(0.5f).rgb
        val additionalWidth = Fonts.minecraftFont.getStringWidth(entity.name).coerceAtLeast(125)
        GlStateManager.pushMatrix()
        GlStateManager.translate(15F, 55F, 0.0f)
        GlStateManager.color(1f, 1f, 1f)
        GuiInventory.drawEntityOnScreen(-18, 47, 30, -180f, 0f, entity)
        RenderUtils.drawRect(
            -38f,
            -14f,
            133f,
            52f,
            Color(0, 0, 0, 180).rgb
        )
        mc.fontRendererObj.drawStringWithShadow(entity.name, 0.0f, -8.0f, Color(255, 255, 255).rgb)
        RenderUtils.drawRect(0f, 8f + Math.round(40.0f), 130f, 40f, colors2)
        if (entity.health / 2.0f + entity.absorptionAmount / 2.0f > 1.0) {
            RenderUtils.drawRect(
                0f,
                8f + Math.round(40.0f),
                (entity.health / entity.maxHealth) * additionalWidth + 5f,
                40f,
                colors1
            )
        }
        RenderUtils.drawRect(
            0f,
            8f + Math.round(40.0f),
            (easingHealth / entity.maxHealth) * additionalWidth,
            40f,
            colors
        )
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
        GlStateManager.scale(3f, 3f, 3f)
        mc.fontRendererObj.drawStringWithShadow(
            "${entity.health.toInt()}\u2764",
            0.0f,
            2.5f,
            colors
        )
        GlStateManager.popMatrix()
    }

    override fun handleBlur(entity: EntityPlayer) {
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderUtils.quickDrawRect(-23F, 41F, 148F, 107F)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        RenderUtils.quickDrawRect(-23F, 41F, 148F, 107F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        return Border(-23F, 41F, 148F, 107F)
    }
}
