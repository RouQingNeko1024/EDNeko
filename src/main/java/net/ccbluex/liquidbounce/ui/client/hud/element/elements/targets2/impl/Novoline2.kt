package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Novoline2(inst: Target2) : TargetStyle("Novoline2", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val width = (38 + Fonts.minecraftFont.getStringWidth(entity.name))
            .coerceAtLeast(118)
            .toFloat()
        targetInstance.counter1[0] += 1
        targetInstance.counter2[0] += 1
        targetInstance.counter1[0] = targetInstance.counter1[0].coerceIn(0, 50)
        targetInstance.counter2[0] = targetInstance.counter2[0].coerceIn(0, 80)
        RenderUtils.drawRect(0F, 0F, width, 34.5F, Color(0, 0, 0, targetInstance.bgColorValue.alpha))
        val customColor = Color(targetInstance.redValue, targetInstance.greenValue, targetInstance.blueValue, 255)
        RenderUtils.drawGradientSideways(
            34.0, 16.0, width.toDouble() - 2,
            24.0, Color(40, 40, 40, 220).rgb, Color(60, 60, 60, 255).rgb
        )
        RenderUtils.drawGradientSideways(
            34.0, 16.0, (36.0F + (easingHealth / entity.maxHealth) * (width - 36.0F)).toDouble() - 2,
            24.0, customColor.rgb, customColor.darker().rgb
        )
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
        Fonts.minecraftFont.drawString(entity.name, 34, 4, Color(255, 255, 255, 255).rgb)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 2, 2, 30, 30)
        }
        Fonts.minecraftFont.drawStringWithShadow(
            java.math.BigDecimal((entity.health / entity.maxHealth * 100).toDouble()).setScale(
                1,
                java.math.BigDecimal.ROUND_HALF_UP
            ).toString() + "%", width / 2F + 5.5F, 16F, Color.white.rgb
        )
    }

    override fun handleBlur(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name))
            .coerceAtLeast(118)
            .toFloat()
        RenderUtils.quickDrawRect(0F, 0F, 118F, 34F)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        RenderUtils.quickDrawRect(0F, 0F, 118F, 34F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        return Border(0F, 0F, 118F, 34F)
    }
}
