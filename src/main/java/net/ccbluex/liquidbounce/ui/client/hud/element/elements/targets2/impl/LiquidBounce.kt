package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class LiquidBounce(inst: Target2) : TargetStyle("LiquidBounce", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name))
            .coerceAtLeast(118)
            .toFloat()
        RenderUtils.drawRoundedRect(0F, 0F, width, 36F, targetInstance.bgColor.rgb, 8F)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 2, 2, 30, 30)
        }
        Fonts.fontSF40.drawString(entity.name, 36, 3, getColor(-1).rgb)
        Fonts.fontSF35.drawString("Distance: ${decimalFormat.format(mc.thePlayer.getDistanceToEntity(entity))}", 36, 15, getColor(-1).rgb)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)
        if (playerInfo != null) {
            Fonts.fontSF35.drawString("Ping: ${playerInfo.responseTime.coerceAtLeast(0)}", 36, 25, getColor(-1).rgb)
        }
        RenderUtils.drawRoundedRect(36F, 33F, width - 3F, 35F, targetInstance.barColor.rgb, 1F)
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
    }

    override fun handleBlur(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.quickDrawRect(0F, 0F, width, 36F)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.quickDrawRect(0F, 0F, width, 36F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity?.name ?: "")).coerceAtLeast(118).toFloat()
        return Border(0F, 0F, width, 36F)
    }
}
