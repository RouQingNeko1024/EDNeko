package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color

class Novoline(inst: Target2) : TargetStyle("Novoline", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.drawRect(0f, 0f, width + 14f, 44f, Color(0, 0, 0, targetInstance.bgColorValue.alpha).rgb)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 3, 3, 30, 30)
        }
        Fonts.fontSF35.drawString(entity.name, 34.5f, 4f, Color.WHITE.rgb)
        Fonts.fontSF35.drawString("Health: ${decimalFormat.format(entity.health)}", 34.5f, 14f, Color.WHITE.rgb)
        Fonts.fontSF35.drawString(
            "Distance: ${decimalFormat.format(mc.thePlayer.getDistanceToEntity(entity))}",
            34.5f, 24f, Color.WHITE.rgb
        )
        RenderUtils.drawRect(34f, 34f, width + 14f, 42f, Color(60, 60, 60).rgb)
        RenderUtils.drawRect(34f, 34f, 34f + (easingHealth / entity.maxHealth) * (width - 20f), 42f, targetInstance.barColor.rgb)
    }

    override fun handleBlur(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.quickDrawRect(0f, 0f, width + 14f, 44f)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        RenderUtils.quickDrawRect(0f, 0f, width + 14f, 44f, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity?.name ?: "")).coerceAtLeast(118).toFloat()
        return Border(0f, 0f, width + 14f, 44f)
    }
}
