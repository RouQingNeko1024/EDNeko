package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Flux(inst: Target2) : TargetStyle("Flux", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val hp = decimalFormat.format(easingHealth)
        val additionalWidth = Fonts.minecraftFont.getStringWidth("${entity.name}  $hp hp").coerceAtLeast(75)
        RenderUtils.drawRoundedRect(
            0f,
            0f,
            45f + additionalWidth + 6f,
            34f,
            Color(0, 0, 0, targetInstance.bgColorValue.alpha).rgb,
            5f
        )
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 5, 3, 29, 28)
        }
        RenderUtils.drawBorderedRect(5f, 2f, 35f, 32f, 1f, targetInstance.bordercolor.rgb, Color(0, 0, 0, 0).rgb)
        Fonts.minecraftFont.drawStringWithShadow(entity.name, 40f, 4f, Color.WHITE.rgb)
        Fonts.minecraftFont.drawStringWithShadow("$hp hp", 40f, 15f, Color.WHITE.rgb)
        val barWidth = (easingHealth / entity.maxHealth) * (additionalWidth + 6f)
        RenderUtils.drawRoundedRect(40f, 26f, 45f + additionalWidth + 1f, 31f, Color(30, 30, 30).rgb, 2f)
        RenderUtils.drawRoundedRect(40f, 26f, 40f + barWidth, 31f, targetInstance.barColor.rgb, 2f)
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
    }

    override fun handleBlur(entity: EntityPlayer) {
        val hp = decimalFormat.format(entity.health)
        val additionalWidth = Fonts.minecraftFont.getStringWidth("${entity.name}  $hp hp").coerceAtLeast(75)
        RenderUtils.quickDrawRect(0f, 0f, 45f + additionalWidth + 6f, 34f)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val hp = decimalFormat.format(entity.health)
        val additionalWidth = Fonts.minecraftFont.getStringWidth("${entity.name}  $hp hp").coerceAtLeast(75)
        RenderUtils.quickDrawRect(0f, 0f, 45f + additionalWidth + 6f, 34f, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        entity ?: return Border(0F, 0F, 126F, 34F)
        val hp = decimalFormat.format(entity.health)
        val additionalWidth = Fonts.minecraftFont.getStringWidth("${entity.name}  $hp hp").coerceAtLeast(75)
        return Border(0F, 0F, 45F + additionalWidth + 6F, 34F)
    }
}
