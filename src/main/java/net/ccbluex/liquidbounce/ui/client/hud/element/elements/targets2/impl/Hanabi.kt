package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Hanabi(inst: Target2) : TargetStyle("Hanabi", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.drawRoundedRect(0F, 0F, width + 10F, 38F, targetInstance.bgColor.rgb, 8F)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 3, 3, 30, 30)
        }
        Fonts.fontSF40.drawString(entity.name, 38.0f, 4.0f, Color.WHITE.rgb)
        val healthStr = decimalFormat.format(entity.health)
        val hurt = Color(255, 0, 0, 255)
        val heartX = 38F
        Fonts.fontSF35.drawStringWithShadow("❤", heartX, 28F, hurt.rgb)
        Fonts.fontSF35.drawStringWithShadow(healthStr, heartX + 8F, 28F, Color.WHITE.rgb)
        RenderUtils.drawRoundedRect(38F, 18F, width + 7F, 24F, Color(30, 30, 30).rgb, 3F)
        RenderUtils.drawRoundedRect(38F, 18F, 38F + (easingHealth / entity.maxHealth) * (width - 31F), 24F, targetInstance.barColor.rgb, 3F)
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
    }

    override fun handleBlur(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.quickDrawRect(0F, 0F, width + 10F, 38F)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.quickDrawRect(0F, 0F, width + 10F, 38F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity?.name ?: "")).coerceAtLeast(118).toFloat()
        return Border(0F, 0F, width + 10F, 38F)
    }
}
