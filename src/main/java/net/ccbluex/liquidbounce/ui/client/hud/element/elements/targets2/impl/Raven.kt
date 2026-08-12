package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color

class Raven(inst: Target2) : TargetStyle("Raven", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val mainStr = "${entity.name} | ${decimalFormat.format(entity.health)}"
        val healthString = decimalFormat.format(entity.health)
        RenderUtils.drawRect(0f, 0f, Fonts.minecraftFont.getStringWidth(mainStr) + 24f, 30f, Color(0, 0, 0, 100).rgb)
        Fonts.minecraftFont.drawStringWithShadow(mainStr, 6F, 5F, Color(255, 255, 255).rgb)
        Fonts.minecraftFont.drawStringWithShadow("Health: ", 6F, 9 + Fonts.minecraftFont.FONT_HEIGHT.toFloat(), Color(255, 255, 255).rgb)
        Fonts.minecraftFont.drawStringWithShadow(healthString, 6 + Fonts.minecraftFont.getStringWidth("Health: ").toFloat(), 9 + Fonts.minecraftFont.FONT_HEIGHT.toFloat(), targetInstance.barColor.rgb)
    }

    override fun handleBlur(entity: EntityPlayer) {
        val mainStr = "${entity.name} | ${decimalFormat.format(entity.health)}"
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderUtils.quickDrawRect(0F, 0F, Fonts.minecraftFont.getStringWidth(mainStr) + 24f, 30f)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val mainStr = "${entity.name} | ${decimalFormat.format(entity.health)}"
        RenderUtils.quickDrawRect(0F, 0F, Fonts.minecraftFont.getStringWidth(mainStr) + 24f, 30f, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        val mainStr = "${entity?.name} | ${decimalFormat.format(entity?.health ?: 0f)}"
        val width = (Fonts.minecraftFont.getStringWidth(mainStr)).toFloat()
        return Border(0F, 0F, width + 24f, 30f)
    }
}
