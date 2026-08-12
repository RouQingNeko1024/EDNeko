package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class RavenB4(inst: Target2) : TargetStyle("RavenB4", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val font = Fonts.minecraftFont
        val width = (38 + font.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.drawBorderedRect(0F, 0F, width, 36F, 2F, Color(0, 0, 0).rgb, Color(28, 28, 28).rgb)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 2, 2, 30, 30)
        }
        font.drawString(entity.name, 36, 4, Color.WHITE.rgb)
        font.drawString("HP: ${decimalFormat.format(entity.health)}", 36, 14, Color.WHITE.rgb)
        font.drawString("Dist: ${decimalFormat.format(mc.thePlayer.getDistanceToEntity(entity))}", 36, 24, Color.WHITE.rgb)
        RenderUtils.drawRect(0F, 34F, (easingHealth / entity.maxHealth) * width, 36F, targetInstance.barColor.rgb)
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
    }

    override fun handleBlur(entity: EntityPlayer) {
        val width = (38 + Fonts.minecraftFont.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderUtils.quickDrawRect(0F, 0F, width, 36F)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val width = (38 + Fonts.minecraftFont.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.quickDrawRect(0F, 0F, width, 36F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        val width = (38 + Fonts.minecraftFont.getStringWidth(entity?.name ?: "")).coerceAtLeast(118).toFloat()
        return Border(0F, 0F, width, 36F)
    }
}
