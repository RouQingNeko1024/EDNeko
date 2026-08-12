package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils.drawGlow
import net.ccbluex.liquidbounce.utils.render.LBPPAnimationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object PotionEffect : Module("PotionEffect", Category.RENDER) {

    private val backgroundAlpha by int("BackgroundAlpha", 120, 0..255)
    private val leftRectRadius by float("LeftRectValue", 0f, 0f..10f)
    private val rightRectRadius by float("RightRectValue", 0f, 0f..10f)
    private val animationSpeed by float("AnimationSpeed", 0.15f, 0.01f..0.5f)
    private val barWidth by float("BarWidth", 5f, 1f..10f)
    private val xOffset by float("X-Offset", 5f, 0f..50f)
    private val yOffset by float("Y-Offset", 0f, -50f..50f)
    private val spacing by float("Spacing", 5f, 0f..10f)
    private val fontMode by choices("Font", arrayOf("Minecraft", "HarmonyOS"), "Minecraft")

    private val activePotions = CopyOnWriteArrayList<AnimatedPotion>()
    private val inventoryTexture = ResourceLocation("textures/gui/container/inventory.png")

    val onRender2D = handler<Render2DEvent> {
        if (mc.thePlayer == null) return@handler

        val sr = ScaledResolution(mc)

        updatePotionList()

        if (activePotions.isEmpty()) return@handler

        activePotions.forEach { it.update(animationSpeed) }
        activePotions.removeIf { it.isReadyToRemove() }

        activePotions.sortBy { it.effect.duration }

        val totalHeight = activePotions.sumByDouble { (it.boxHeight + spacing) * it.animationY.toDouble() }.toFloat() - spacing
        var currentY = (sr.scaledHeight / 2f) - (totalHeight / 2f) + yOffset

        for (potion in activePotions) {
            potion.draw(currentY, xOffset, fontMode)
            currentY += (potion.boxHeight + spacing) * potion.animationY
        }
    }

    private fun updatePotionList() {
        val playerEffects = mc.thePlayer.activePotionEffects.filter { Potion.potionTypes[it.potionID] != null }

        activePotions.forEach { animatedPotion ->
            if (playerEffects.none { it.potionID == animatedPotion.effect.potionID }) {
                animatedPotion.isMarkedForRemoval = true
            }
        }

        playerEffects.forEach { playerEffect ->
            if (activePotions.none { it.effect.potionID == playerEffect.potionID }) {
                activePotions.add(AnimatedPotion(playerEffect))
            } else {
                activePotions.find { it.effect.potionID == playerEffect.potionID }?.effect = playerEffect
            }
        }
    }

    private class AnimatedPotion(var effect: PotionEffect) {
        val boxWidth = 120f
        val boxHeight = 32f

        var isMarkedForRemoval = false
        private var animationX = -boxWidth - 10f
        var animationY = 0f

        private val potion: Potion = Potion.potionTypes[effect.potionID]
        private val dataColor = potionColorMap[effect.potionID] ?: Color.GRAY

        fun update(speed: Float) {
            val targetX = if (isMarkedForRemoval) -boxWidth - 10f else 0f
            val targetY = if (isMarkedForRemoval) 0f else 1f
            animationX = LBPPAnimationUtils.animate(targetX, animationX, speed)
            animationY = LBPPAnimationUtils.animate(targetY, animationY, speed)
        }

        fun isReadyToRemove(): Boolean {
            return isMarkedForRemoval && animationX <= -boxWidth
        }

        fun draw(y: Float, xOffset: Float, fontMode: String) {
            val startX = xOffset + animationX
            val startY = y
            val endX = startX + boxWidth
            val endY = startY + boxHeight

            if (startX > boxWidth) return

            val animatedHeight = boxHeight * animationY
            if (animatedHeight < 1) return

            val bgColor = Color(40, 40, 40, (backgroundAlpha * animationY).toInt())

            drawRoundedRect(startX, startY, endX, startY + animatedHeight, bgColor.rgb, leftRectRadius)
            drawGlow(startX, startY, endX - startX, endY - startY, 8, bgColor)
            drawRoundedRect(startX, startY, startX + barWidth, startY + animatedHeight, dataColor.rgb, rightRectRadius)

            glPushMatrix()
            RenderUtils.makeScissorBox(startX, startY, endX, startY + animatedHeight)
            glEnable(GL_SCISSOR_TEST)

            if (potion.hasStatusIcon()) {
                mc.textureManager.bindTexture(inventoryTexture)
                GlStateManager.color(1f, 1f, 1f, animationY)
                val iconX = potion.statusIconIndex % 8 * 18
                val iconY = 198 + potion.statusIconIndex / 8 * 18
                RenderUtils.drawTexturedModalRect((startX + 8f).toInt(), (startY + (animatedHeight - 18) / 2).toInt(), iconX, iconY, 18, 18, 0.0F)
            }

            val textX = startX + 30
            val textY = startY + (animatedHeight / 2) - 8
            val nameColor = Color(dataColor.red, dataColor.green, dataColor.blue, (255 * animationY).toInt()).rgb
            val potionName = I18n.format(potion.name)
            val displayName = potionName + if (effect.amplifier > 0) " ${effect.amplifier + 1}" else ""

            val duration = effect.duration / 20
            val durationText = String.format("%02d:%02d", duration / 60, duration % 60)
            val durationColor = if (duration <= 10) {
                Color(255, 80, 80, (255 * animationY).toInt()).rgb
            } else {
                Color(255, 255, 255, (200 * animationY).toInt()).rgb
            }

            when (fontMode) {
                "Minecraft" -> {
                    mc.fontRendererObj.drawString(displayName, textX.toInt(), textY.toInt(), nameColor)
                    mc.fontRendererObj.drawString(durationText, textX.toInt(), (textY + 11).toInt(), durationColor)
                }
                "HarmonyOS" -> {
                    Fonts.fontSemibold35.drawString(displayName, textX, textY, nameColor)
                    Fonts.fontSemibold35.drawString(durationText, textX, textY + 11, durationColor)
                }
            }

            glDisable(GL_SCISSOR_TEST)
            glPopMatrix()
        }
    }

    private val potionColorMap = mapOf(
        Potion.moveSpeed.id to Color(124, 175, 198),
        Potion.digSpeed.id to Color(217, 192, 67),
        Potion.damageBoost.id to Color(204, 91, 89),
        Potion.jump.id to Color(34, 255, 76),
        Potion.regeneration.id to Color(221, 122, 146),
        Potion.resistance.id to Color(153, 69, 59),
        Potion.fireResistance.id to Color(228, 154, 58),
        Potion.waterBreathing.id to Color(46, 82, 153),
        Potion.invisibility.id to Color(127, 131, 146),
        Potion.nightVision.id to Color(31, 31, 165),
        Potion.healthBoost.id to Color(248, 125, 35),
        Potion.absorption.id to Color(36, 147, 147),
        Potion.saturation.id to Color(248, 36, 35),
        Potion.moveSlowdown.id to Color(90, 108, 127),
        Potion.digSlowdown.id to Color(74, 66, 23),
        Potion.weakness.id to Color(72, 77, 77),
        Potion.poison.id to Color(78, 157, 48),
        Potion.wither.id to Color(53, 42, 39),
        Potion.hunger.id to Color(88, 83, 22),
        Potion.confusion.id to Color(85, 29, 74),
        Potion.blindness.id to Color(31, 31, 36)
    )
}
