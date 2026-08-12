package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Slowly(inst: Target2) : TargetStyle("Slowly", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val font = Fonts.fontSF35
        val length = (font.getStringWidth(entity.name).toFloat() + 40F).coerceAtLeast(75F)
        RenderUtils.drawRoundedRect(0F, 0F, 32F + length, 36F, targetInstance.bgColor.rgb, 8F)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 2, 2, 30, 30)
        }
        font.drawString(entity.name, 36F, 4F, Color.WHITE.rgb)
        font.drawString("HP: ${decimalFormat.format(entity.health)}", 36F, 16F, Color.WHITE.rgb)
        val barWidth = (easingHealth / entity.maxHealth) * length
        RenderUtils.drawRoundedRect(36F, 28F, 32F + length - 3F, 34F, Color(30, 30, 30).rgb, 3F)
        RenderUtils.drawRoundedRect(36F, 28F, 36F + barWidth, 34F, targetInstance.barColor.rgb, 3F)
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
    }

    override fun handleBlur(entity: EntityPlayer) {
        val font = Fonts.fontSF35
        val length = (font.getStringWidth(entity.name).toFloat() + 40F).coerceAtLeast(75F)
        RenderUtils.quickDrawRect(0F, 0F, 32F + length, 36F)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val font = Fonts.fontSF35
        val length = (font.getStringWidth(entity.name).toFloat() + 40F).coerceAtLeast(75F)
        RenderUtils.quickDrawRect(0F, 0F, 32F + length, 36F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        val font = Fonts.fontSF35
        val length = (font.getStringWidth(entity?.name ?: "").toFloat() + 40F).coerceAtLeast(75F)
        return Border(0F, 0F, 32F + length, 36F)
    }
}
