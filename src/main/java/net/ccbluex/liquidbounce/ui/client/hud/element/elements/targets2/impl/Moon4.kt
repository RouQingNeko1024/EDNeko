package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Moon4(inst: Target2) : TargetStyle("Moon4", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val nameLength = (Fonts.fontSF40.getStringWidth("${entity.name}")).coerceAtLeast(
            Fonts.fontSF35.getStringWidth(
                decimalFormat2.format(entity.health)
            )
        )
        val width = 36 + nameLength + 5
        RenderUtils.drawRoundedRect(0F, 0F, width.toFloat(), 36F, targetInstance.bgColor.rgb, 8F)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 2, 2, 30, 30)
        }
        Fonts.fontSF40.drawStringWithShadow(entity.name, 36F, 2F, -1)
        val percent = easingHealth / entity.maxHealth * 100.0F
        Fonts.fontSF35.drawStringWithShadow(decimalFormat2.format(percent) + "HP", 38F, 15F, Color.WHITE.rgb)
        val barWidth = (easingHealth / entity.maxHealth) * (width - 39F)
        RenderUtils.drawRoundedRect(36F, 28F, width.toFloat() - 3F, 34F, Color(30, 30, 30).rgb, 3F)
        RenderUtils.drawRoundedRect(36F, 28F, 36F + barWidth, 34F, targetInstance.barColor.rgb, 3F)
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
    }

    override fun handleBlur(entity: EntityPlayer) {
        val nameLength = (Fonts.fontSF40.getStringWidth("${entity.name}")).coerceAtLeast(
            Fonts.fontSF35.getStringWidth(
                decimalFormat2.format(entity.health)
            )
        )
        val width = 36 + nameLength + 5
        RenderUtils.quickDrawRect(0F, 0F, width.toFloat(), 36F)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val nameLength = (Fonts.fontSF40.getStringWidth("${entity.name}")).coerceAtLeast(
            Fonts.fontSF35.getStringWidth(
                decimalFormat2.format(entity.health)
            )
        )
        val width = 36 + nameLength + 5
        RenderUtils.quickDrawRect(0F, 0F, width.toFloat(), 36F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        entity ?: return Border(0F, 0F, 120F, 36F)
        val nameLength = (Fonts.fontSF40.getStringWidth("${entity.name}")).coerceAtLeast(
            Fonts.fontSF35.getStringWidth(
                decimalFormat2.format(entity.health)
            )
        )
        val width = 36 + nameLength + 5
        return Border(0F, 0F, width.toFloat(), 36F)
    }
}
