package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Exhibition(inst: Target2) : TargetStyle("Exhibition", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val font = Fonts.font40
        val minWidth = 143F.coerceAtLeast(47F + font.getStringWidth(entity.name))
        RenderUtils.drawRoundedRect(0F, 0F, minWidth, 48F, targetInstance.bgColor.rgb, 8F)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 4, 4, 28, 28)
        }
        font.drawStringWithShadow(entity.name, 38F, 6F, getColor(-1).rgb)
        Fonts.font35.drawString("HP:${entity.health.toInt()} | Dist:${mc.thePlayer.getDistanceToEntity(entity).toInt()}", 38F, 21F, getColor(-1).rgb)
        val barWidth = (easingHealth / entity.maxHealth) * (minWidth - 8F)
        RenderUtils.drawRoundedRect(4F, 36F, minWidth - 4F, 44F, Color(30, 30, 30).rgb, 3F)
        RenderUtils.drawRoundedRect(4F, 36F, 4F + barWidth, 44F, targetInstance.barColor.rgb, 3F)
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
    }

    override fun handleBlur(entity: EntityPlayer) {
        val font = Fonts.font40
        val minWidth = 143F.coerceAtLeast(47F + font.getStringWidth(entity.name))
        RenderUtils.quickDrawRect(0F, 0F, minWidth, 48F)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val font = Fonts.font40
        val minWidth = 143F.coerceAtLeast(47F + font.getStringWidth(entity.name))
        RenderUtils.quickDrawRect(0F, 0F, minWidth, 48F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        val font = Fonts.font40
        val minWidth = 143F.coerceAtLeast(47F + font.getStringWidth(entity?.name ?: ""))
        return Border(0F, 0F, minWidth, 48F)
    }
}
