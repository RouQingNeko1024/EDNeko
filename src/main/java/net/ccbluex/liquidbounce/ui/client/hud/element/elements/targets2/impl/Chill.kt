package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl

import net.ccbluex.liquidbounce.config.BoolValue
import net.ccbluex.liquidbounce.config.FloatValue
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target2
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.utils.CharRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.darker
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.ShaderUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11

class Chill(inst: Target2) : TargetStyle("Chill", inst, true) {

    private val chillFontSpeed = FloatValue("Chill-FontSpeed", 0.5F, 0.01F..1F).apply { 
        setSupport { targetInstance.styleValueName.equals("chill", ignoreCase = true) }
    }
    private val chillRoundValue = BoolValue("Chill-RoundedBar", true).apply {
        setSupport { targetInstance.styleValueName.equals("chill", ignoreCase = true) }
    }

    private val numberRenderer = CharRenderer(false)

    private var calcScaleX = 0F
    private var calcScaleY = 0F
    private var calcTranslateX = 0F
    private var calcTranslateY = 0F

    fun updateData(ctx: Float, cty: Float, csx: Float, csy: Float) {
        calcTranslateX = ctx
        calcTranslateY = cty
        calcScaleX = csx
        calcScaleY = csy
    }

    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHp = entity.maxHealth.coerceAtLeast(1F)
        val tWidth = (45F + Fonts.font40.getStringWidth(name).coerceAtLeast(Fonts.font40.getStringWidth(decimalFormat.format(health)))).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        RenderUtils.drawRect(0F, 0F, tWidth, 48F, targetInstance.bgColor.rgb)

        if (playerInfo != null) {
            try {
                Stencil.write(false)
                GL11.glDisable(GL11.GL_TEXTURE_2D)
                GL11.glEnable(GL11.GL_BLEND)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                RenderUtils.fastRoundedRect(4F, 4F, 34F, 34F, 7F)
                GL11.glDisable(GL11.GL_BLEND)
                GL11.glEnable(GL11.GL_TEXTURE_2D)
                Stencil.erase(true)
                drawHead(playerInfo.locationSkin, 4, 4, 30, 30, 1F - targetInstance.getFadeProgress())
                Stencil.dispose()
            } catch (e: Exception) {
                Stencil.dispose()
            }
        }

        GlStateManager.resetColor()
        GL11.glColor4f(1F, 1F, 1F, 1F)

        Fonts.font40.drawString(name, 38F, 6F, getColor(-1).rgb)
        numberRenderer.renderChar(health, calcTranslateX, calcTranslateY, 38F, 17F, calcScaleX, calcScaleY, false, chillFontSpeed.get(), getColor(-1).rgb)

        RenderUtils.drawRect(4F, 38F, tWidth - 4F, 44F, targetInstance.barColor.darker(0.5F).rgb)

        try {
            Stencil.write(false)
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(4F, 38F, tWidth - 4F, 44F, 3F)
            GL11.glDisable(GL11.GL_BLEND)
            Stencil.erase(true)
            if (chillRoundValue.get())
                RenderUtils.drawRect(4F, 38F, 4F + (easingHealth / maxHp) * (tWidth - 8F), 44F, targetInstance.barColor.rgb)
            else
                RenderUtils.drawRect(4F, 38F, 4F + (easingHealth / maxHp) * (tWidth - 8F), 44F, targetInstance.barColor.rgb)
            Stencil.dispose()
        } catch (e: Exception) {
            Stencil.dispose()
        }
    }

    override fun handleBlur(entity: EntityPlayer) {
        val tWidth = (45F + Fonts.font40.getStringWidth(entity.name).coerceAtLeast(Fonts.font40.getStringWidth(decimalFormat.format(entity.health)))).coerceAtLeast(120F)
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        RenderUtils.fastRoundedRect(0F, 0F, tWidth, 48F, 7F)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    override fun handleShadowCut(entity: EntityPlayer) = handleBlur(entity)

    override fun handleShadow(entity: EntityPlayer) {
        val tWidth = (45F + Fonts.font40.getStringWidth(entity.name).coerceAtLeast(Fonts.font40.getStringWidth(decimalFormat.format(entity.health)))).coerceAtLeast(120F)
        RenderUtils.originalRoundedRect(0F, 0F, tWidth, 48F, 7F, shadowOpaque.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border {
        entity ?: return Border(0F, 0F, 120F, 48F)
        val tWidth = (45F + Fonts.font40.getStringWidth(entity.name).coerceAtLeast(Fonts.font40.getStringWidth(decimalFormat.format(entity.health)))).coerceAtLeast(120F)
        return Border(0F, 0F, tWidth, 48F)
    }
}
