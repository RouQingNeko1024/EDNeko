package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.pow

class Novoline3(inst: Target2) : TargetStyle("Novoline3", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)

        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name))
            .coerceAtLeast(118)
            .toFloat()

        RenderUtils.drawRoundedRect(-3F, -4F, width + 27F, 47F, targetInstance.bgColor.rgb, 1F)

        Fonts.fontSF40.drawStringWithShadow(entity.name, 40f, 3f, Color(255, 255, 255, 255).rgb)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, width = 33, height = 33, alpha = 1F - targetInstance.getFadeProgress())
        }

        RenderUtils.drawRect(
            1F,
            39f,
            easingHealth / entity.maxHealth * (width + 2.5f) + 7F,
            43f,
            targetInstance.barColor.rgb
        )

        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
        
        RenderHelper.enableGUIStandardItemLighting()
        var armorX = 40
        val armorY = 22
        for (index in 3 downTo 0) {
            val stack = entity.inventory.armorInventory[index]
            if (stack != null && stack.item != null) {
                mc.renderItem.renderItemAndEffectIntoGUI(stack, armorX, armorY)
            }
            armorX += 18
        }
        RenderHelper.disableStandardItemLighting()
        
        val healthName = decimalFormat2.format(easingHealth)
        Fonts.fontSF35.drawString(healthName, easingHealth / entity.maxHealth * (width + 2.5f) + 5, 37F, Color(255, 255, 255, 255).rgb)
    }

    override fun handleBlur(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name))
            .coerceAtLeast(118)
            .toFloat()
        RenderUtils.quickDrawRect(-3F, -4F, width + 26F, 47F)
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val width = (38 + Fonts.fontSF40.getStringWidth(entity.name))
            .coerceAtLeast(118)
            .toFloat()
        RenderUtils.quickDrawRect(-3F, -4F, width + 26F, 47F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        return Border(0F, 0F, 124F, 44F)
    }
}
