package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Lnk(inst: Target2) : TargetStyle("Lnk", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.drawRoundedRect(0F, 0F, width, 36F, targetInstance.bgColor.rgb, 8F)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 3, 3, 30, 30)
        }
        Fonts.fontSF40.drawString(entity.name, 36F, 3F, Color.WHITE.rgb)
        Fonts.fontSF35.drawString("HP: ${decimalFormat.format(entity.health)}", 36F, 14F, Color.WHITE.rgb)
        val barWidth = (easingHealth / entity.maxHealth) * (width - 39F)
        RenderUtils.drawRoundedRect(36F, 26F, width - 3F, 32F, Color(30, 30, 30).rgb, 3F)
        RenderUtils.drawRoundedRect(36F, 26F, 36F + barWidth, 32F, targetInstance.barColor.rgb, 3F)
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
