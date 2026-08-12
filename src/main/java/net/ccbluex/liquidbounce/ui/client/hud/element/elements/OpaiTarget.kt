package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import java.awt.Color

@ElementInfo(name = "OpaiTarget")
class OpaiTarget : Element("OpaiTarget"), Listenable {
    private val onlyOnKillaura by boolean("OnlyOnKillaura", true)
    private val livingTime by int("LivingTime", 3, 0..20) { !onlyOnKillaura }

    private val themeColorRGB by color("themeColor",Color(242,172,244))
    private val backGroundAlpha by int("BackGroundAlpha",120,0..255)
    private val ShadowCheck by boolean("ShadowCheck",false)
    private val shadowStrengh by float("shadowStrengh",0.5f,0.0f..1.0f) {ShadowCheck}
    private val vanishDelay by int("VanishDelay", 300, 0..500)

    val roundedRectRadius = 5f

    val headWidth = 30f
    val borderWidth = 3.5f
    val progressWidth = 135f
    val progressHeight = 5f
    var AnimX = progressWidth

    private var delayCounter = 0
    private var lastTarget: EntityLivingBase? = null

    private var attackTarget: EntityLivingBase? = null
    private var targetDisappearTime = 0L

    private val attackEventHandler = handler<AttackEvent> { event ->
        if (!onlyOnKillaura) {
            val entity = event.targetEntity
            if (entity is EntityLivingBase && entity is EntityPlayer && !AntiBot.isBot(entity)) {
                attackTarget = entity
                targetDisappearTime = System.currentTimeMillis()
            }
        }
    }

    override fun drawElement(): Border {
        val killAuraTarget = KillAura.target.takeIf { it is EntityPlayer }
        val isEditing = mc.currentScreen is net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner

        val shouldRender: Boolean
        val target: EntityLivingBase

        if (isEditing) {
            shouldRender = true
            target = mc.thePlayer
        } else if (onlyOnKillaura) {
            shouldRender = KillAura.handleEvents() && killAuraTarget != null || mc.currentScreen is GuiChat
            target = killAuraTarget ?: if (delayCounter >= vanishDelay) {
                mc.thePlayer
            } else {
                lastTarget ?: mc.thePlayer
            }
        } else {
            val currentTime = System.currentTimeMillis()
            val elapsedSeconds = (currentTime - targetDisappearTime) / 1000.0

            if (killAuraTarget != null) {
                shouldRender = true
                target = killAuraTarget
                targetDisappearTime = currentTime
            } else if (mc.currentScreen is GuiChat) {
                shouldRender = true
                target = mc.thePlayer
            } else if (attackTarget != null && attackTarget!!.isEntityAlive && !AntiBot.isBot(attackTarget!!)) {
                if (elapsedSeconds <= livingTime) {
                    shouldRender = true
                    target = attackTarget!!
                } else {
                    shouldRender = delayCounter < vanishDelay
                    target = lastTarget ?: mc.thePlayer
                }
            } else {
                if (elapsedSeconds <= livingTime && lastTarget != null && lastTarget!!.isEntityAlive) {
                    shouldRender = delayCounter < vanishDelay
                    target = lastTarget!!
                } else {
                    shouldRender = false
                    target = mc.thePlayer
                    attackTarget = null
                }
            }
        }

        if (shouldRender) {
            delayCounter = 0
        } else {
            delayCounter++
        }

        if (shouldRender || delayCounter < vanishDelay) {
            val targetName = target.name+"  "
            val targetNameWidth = Fonts.fontSemibold35.getStringWidth(targetName)
            val targetHealth = target.health.toInt()
            val targetHealthWidth = Fonts.fontSemibold35.getStringWidth(targetHealth.toString())
            val textsDrawBegin = borderWidth+headWidth+borderWidth
            val allTextLen = targetNameWidth+targetHealthWidth
            val resultProgressWidth = maxOf(progressWidth,textsDrawBegin+allTextLen+8f)
            val publicXY:Pair<Float,Float> = Pair(borderWidth*2+resultProgressWidth,borderWidth+headWidth+borderWidth+progressHeight+borderWidth)

            ShowShadow(0f,0f,publicXY.first,publicXY.second)
            drawRoundedBorderRect(0f,0f,publicXY.first-borderWidth,publicXY.second,0.2f,Color(0,0,0,backGroundAlpha).rgb,Color(0,0,0,backGroundAlpha).rgb, roundedRectRadius)
            if (target is EntityLivingBase){
                drawHead(target,borderWidth,borderWidth)
            }
            val progressBarLength = resultProgressWidth/target.maxHealth*targetHealth
            AnimX = AnimationUtil.base(AnimX.toDouble(),progressBarLength.toDouble(),0.2).toFloat()
            drawRoundedBorderRect(borderWidth,borderWidth+headWidth+borderWidth,resultProgressWidth,borderWidth+headWidth+borderWidth+progressHeight,0.3f,Color(0,0,0,200).rgb,Color(0,0,0,200).rgb,roundedRectRadius)
            drawRoundedBorderRect(borderWidth,borderWidth+headWidth+borderWidth,AnimX,borderWidth+headWidth+borderWidth+progressHeight,0.3f,Color(themeColorRGB.red,themeColorRGB.green,themeColorRGB.blue,150).rgb,Color(themeColorRGB.red,themeColorRGB.green,themeColorRGB.blue,150).rgb,4F)
            drawRoundedBorderRect(borderWidth,borderWidth+headWidth+borderWidth,progressBarLength,borderWidth+headWidth+borderWidth+progressHeight,0.3f,themeColorRGB.rgb,themeColorRGB.rgb,4F)
            Fonts.fontSemibold35.drawString(targetName,textsDrawBegin+borderWidth,borderWidth*2,Color.WHITE.rgb)
            Fonts.fontSemibold35.drawString((targetHealth).toString(),textsDrawBegin+targetNameWidth+borderWidth,borderWidth*2-1F,themeColorRGB.rgb)
            val armorX = textsDrawBegin
            val armorY = borderWidth + headWidth - 18
            drawArmor(armorX, armorY, target)
        }

        lastTarget = target
        return Border(0f, 0f, 200f, 41f)
    }

    private fun drawArmor(x: Float, y: Float, target: EntityLivingBase){
        if (target !is EntityPlayer) return
        glPushMatrix()
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        enableGUIStandardItemLighting()
        var offsetX = x
        val renderItem = mc.renderItem
        for (index in 3 downTo 0) {
            val stack = target.inventory.armorInventory[index] ?: continue
            renderItem.renderItemIntoGUI(stack, offsetX.toInt(), y.toInt())
            renderItem.renderItemOverlays(mc.fontRendererObj, stack, offsetX.toInt(), y.toInt())
            offsetX += 18f
        }
        disableStandardItemLighting()
        glDisable(GL_BLEND)
        glPopMatrix()
    }

    private fun drawHead(target: EntityLivingBase, x: Float, y: Float) {
        val texture = mc.renderManager.getEntityRenderObject<Entity>(target)
            ?.getEntityTexture(target) ?: return

        withClipping(main = {
            drawRoundedRect(x, y, x + headWidth, y + headWidth, 0, roundedRectRadius)
        }, toClip = {
            RenderUtils.drawHead(
                texture, x.toInt(), y.toInt(),
                8f, 8f, 8, 8, headWidth.toInt(), headWidth.toInt(), 64f, 64f,
                Color.WHITE
            )
        })
    }

    private fun ShowShadow(startX: Float,startY: Float,width: Float,height:Float){
        if (ShadowCheck) {
            GlowUtils.drawGlow(
                startX, startY,
                width, height,
                (shadowStrengh * 13F).toInt(),
                Color(0, 0, 0, 120)
            )
        }
    }
}
