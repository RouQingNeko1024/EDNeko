package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.pow

class Remix(inst: Target2) : TargetStyle("Remix", inst, true) {
    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        val width = 142F
        RenderUtils.drawRect(0F, 0F, width, 44F, Color(0, 0, 0, targetInstance.bgColorValue.alpha).rgb)
        val skinLocation = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skinLocation != null) {
            drawHead(skinLocation, 2, 2, 30, 30)
        }
        Fonts.minecraftFont.drawStringWithShadow(entity.name, 41F, 5F, getColor(-1).rgb)
        val barWidth = (easingHealth / entity.maxHealth) * 95F
        RenderUtils.drawRect(41F, 20F, 136F, 26F, Color(50, 50, 50).rgb)
        RenderUtils.drawRect(41F, 20F, 41F + barWidth, 26F, targetInstance.barColor.rgb)
        val stringTime = decimalFormat.format(entity.health)
        GL11.glPushMatrix()
        GL11.glTranslatef(142F - Fonts.minecraftFont.getStringWidth(stringTime) / 2F, 28F, 0F)
        Fonts.minecraftFont.drawStringWithShadow(stringTime, 0F, 0F, getColor(-1).rgb)
        GL11.glPopMatrix()
        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.globalAnimSpeed)) * RenderUtils.deltaTime
    }

    override fun handleBlur(entity: EntityPlayer) {
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderUtils.quickDrawRect(0F, 0F, 142F, 44F)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        RenderUtils.quickDrawRect(0F, 0F, 142F, 44F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        return Border(0F, 0F, 142F, 44F)
    }
}
