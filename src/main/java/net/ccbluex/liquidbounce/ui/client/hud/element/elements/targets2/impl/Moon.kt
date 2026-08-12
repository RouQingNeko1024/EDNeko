package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Moon(inst: Target2) : TargetStyle("Moon", inst, true) {
    private val moonHealthColor = ListValue("Moon-HealthColor", arrayOf("White", "Health", "Custom"), "Health")

    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val font = Fonts.fontSF35
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name))
            .coerceAtLeast(118)
            .toFloat()
        RenderUtils.drawRoundedRect(0F, 0F, width, 32F, targetInstance.bgColor.rgb, 8F)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 2, 2, 30, 30)
        }
        Fonts.fontSF40.drawString(entity.name, 37, 3, getColor(-1).rgb)
        val barWidth = (easingHealth / entity.maxHealth) * (width - 39F)
        RenderUtils.drawRoundedRect(36F, 22F, width - 3F, 28F, Color(30, 30, 30).rgb, 3F)
        val healthColor = when (moonHealthColor.get()) {
            "White" -> Color.WHITE.rgb
            "Health" -> targetInstance.barColor.rgb
            "Custom" -> targetInstance.bordercolor.rgb
            else -> targetInstance.barColor.rgb
        }
        RenderUtils.drawRoundedRect(36F, 22F, 36F + barWidth, 28F, healthColor, 3F)
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
    }

    override fun handleBlur(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.quickDrawRect(0F, 0F, width, 32F)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.quickDrawRect(0F, 0F, width, 32F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity?.name ?: "")).coerceAtLeast(118).toFloat()
        return Border(0F, 0F, width, 32F)
    }
}
