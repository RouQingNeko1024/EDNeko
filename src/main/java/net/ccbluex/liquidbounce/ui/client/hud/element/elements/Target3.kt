/*
 * Lizz Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/L1zzLLC/Lizz/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.extensions.safeDiv
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawGradientRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHead
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawScaledCustomSizeModalRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

@ElementInfo(name = "Target3")
class Target3 : Element("Target3"), Listenable {

    private val onlyOnKillaura by boolean("OnlyOnKillaura", true)
    private val livingTime by int("LivingTime", 3, 0..20) { !onlyOnKillaura }

    private val roundedRectRadius by float("Rounded-Radius", 3F, 0F..5F)

    private val borderStrength by float("Border-Strength", 3F, 1F..5F)

    private val backgroundMode by choices("Background-ColorMode", arrayOf("Custom", "Rainbow"), "Custom")
    private val backgroundColor by color("Background-Color", Color.BLACK.withAlpha(150)) { backgroundMode == "Custom" }
    private val ShadowCheck by boolean("ShadowCheck", true)
    private val ShadowValue by float("ShadowValue", 1F, 1F..2F) { ShadowCheck }

    private val blur by boolean("Blur", true)
    private val blurStrength by float("BlurStrength", 5F, 1F..10F) { blur }

    private val enableGlass by boolean("EnableGlass", false)
    private val enableNeon by boolean("EnableNeon", false)
    
    private val neonColor by color("NeonColor", Color(0, 255, 255)) { enableNeon }
    private val neonOuterGlowAlpha by int("Neon-OuterGlowAlpha", 50, 10..255) { enableNeon }
    private val neonInnerBorderAlpha by int("Neon-InnerBorderAlpha", 180, 50..255) { enableNeon }
    private val neonBackgroundAlpha by int("Neon-BackgroundAlpha", 80, 0..255) { enableNeon }
    
    private val glassBaseColor by color("Glass-BaseColor", Color(200, 220, 255, 15)) { enableGlass }
    private val glassHighlightAlpha by int("Glass-HighlightAlpha", 20, 0..100) { enableGlass }
    private val glassBorderColor by color("Glass-BorderColor", Color(255, 255, 255, 40)) { enableGlass }

    private val textGlow by boolean("TextGlow", false)
    private val textGlowStrength by float("TextGlowStrength", 0.5F, 0.1F..1.0F) { textGlow }
    private val textGlowColor by color("TextGlowColor", Color(0, 150, 255)) { textGlow }
    
    private val backgroundGlow by boolean("BackgroundGlow", true)
    private val backgroundGlowStrength by float("BackgroundGlowStrength", 0.5F, 0.1F..1.0F) { backgroundGlow }
    private val backgroundGlowColor by color("BackgroundGlowColor", Color(0, 150, 255)) { backgroundGlow }

    private val healthBarColor1 by color("HealthBar-Gradient1", Color(3, 65, 252))
    private val healthBarColor2 by color("HealthBar-Gradient2", Color(3, 252, 236))

    private val roundHealthBarShape by boolean("RoundHealthBarShape", true)

    private val borderColor by color("Border-Color", Color.BLACK)

    private val textColor by color("TextColor", Color.WHITE)

    private val rainbowX by float("Rainbow-X", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }
    private val rainbowY by float("Rainbow-Y", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }

    private val titleFont by font("TitleFont", Fonts.fontSemibold40)
    private val healthFont by font("HealthFont", Fonts.fontRegular30)
    private val textShadow by boolean("TextShadow", false)

    private val fadeSpeed by float("FadeSpeed", 2F, 1F..9F)
    private val absorption by boolean("Absorption", true)
    private val healthFromScoreboard by boolean("HealthFromScoreboard", true)

    private val animation by choices("Animation", arrayOf("Smooth", "Fade"), "Fade")
    private val animationSpeed by float("AnimationSpeed", 0.2F, 0.05F..1F)
    private val vanishDelay by int("VanishDelay", 300, 0..500)

    private var easingHealth = 0F
    private var lastTarget: EntityLivingBase? = null

    private var width = 0f
    private var height = 0f

    private val isRendered
        get() = width > 0f || height > 0f

    private var alphaText = 0
    private var alphaBackground = 0
    private var alphaBorder = 0

    private val isAlpha
        get() = alphaBorder > 0 || alphaBackground > 0 || alphaText > 0

    private var delayCounter = 0
    private var easingHurtTime = 0F

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
        val smoothMode = animation == "Smooth"
        val fadeMode = animation == "Fade"

        val killAuraTarget = KillAura.target.takeIf { it is EntityPlayer }
        val isEditing = mc.currentScreen is net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner

        val shouldRender: Boolean
        val target: EntityLivingBase

        if (isEditing) {
            shouldRender = true
            target = mc.thePlayer
        } else if (onlyOnKillaura) {
            shouldRender = KillAura.handleEvents() && killAuraTarget != null || mc.currentScreen is GuiChat
            target = killAuraTarget ?: if (delayCounter >= vanishDelay && !isRendered) {
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
                    shouldRender = isRendered || isAlpha
                    target = lastTarget ?: mc.thePlayer
                }
            } else {
                if (elapsedSeconds <= livingTime && lastTarget != null && lastTarget!!.isEntityAlive) {
                    shouldRender = isRendered || isAlpha
                    target = lastTarget!!
                } else {
                    shouldRender = false
                    target = mc.thePlayer
                    attackTarget = null
                }
            }
        }

        val stringWidth = (40f + (target.name?.let(titleFont::getStringWidth) ?: 0)).coerceAtLeast(118F)

        assumeNonVolatile {
            if (shouldRender) {
                delayCounter = 0
            } else if (isRendered || isAlpha) {
                delayCounter++
            }

            if (shouldRender || isRendered || isAlpha) {
                val targetHealth = getHealth(target, healthFromScoreboard, absorption)
                val maxHealth = target.maxHealth + if (absorption) target.absorptionAmount else 0F

                easingHealth += (targetHealth - easingHealth) / 2f.pow(10f - fadeSpeed) * deltaTime
                easingHealth = easingHealth.coerceIn(0f, maxHealth)
                val targetHurtTime = if (target.isEntityAlive()) target.hurtTime.toFloat() else 0F
                easingHurtTime = (easingHurtTime..targetHurtTime).lerpWith(RenderUtils.deltaTimeNormalized())

                if (target != lastTarget || abs(easingHealth - targetHealth) < 0.01) {
                    easingHealth = targetHealth
                }

                if (smoothMode) {
                    val targetWidth = if (shouldRender) stringWidth else if (delayCounter >= vanishDelay) 0f else width
                    width = AnimationUtil.base(width.toDouble(), targetWidth.toDouble(), animationSpeed.toDouble())
                        .toFloat().coerceAtLeast(0f)

                    val targetHeight = if (shouldRender) 36f else if (delayCounter >= vanishDelay) 0f else height
                    height = AnimationUtil.base(height.toDouble(), targetHeight.toDouble(), animationSpeed.toDouble())
                        .toFloat().coerceAtLeast(0f)
                } else {
                    width = stringWidth
                    height = 36f

                    val targetText =
                        if (shouldRender) textColor.alpha else if (delayCounter >= vanishDelay) 0f else alphaText
                    alphaText =
                        AnimationUtil.base(alphaText.toDouble(), targetText.toDouble(), animationSpeed.toDouble())
                            .toInt()

                    val targetBackground = if (shouldRender) {
                        backgroundColor.alpha
                    } else if (delayCounter >= vanishDelay) {
                        0f
                    } else alphaBackground

                    alphaBackground = AnimationUtil.base(
                        alphaBackground.toDouble(), targetBackground.toDouble(), animationSpeed.toDouble()
                    ).toInt()

                    val targetBorder = if (shouldRender) {
                        borderColor.alpha
                    } else if (delayCounter >= vanishDelay) {
                        0f
                    } else alphaBorder

                    alphaBorder =
                        AnimationUtil.base(alphaBorder.toDouble(), targetBorder.toDouble(), animationSpeed.toDouble())
                            .toInt()
                }

                val backgroundCustomColor = backgroundColor.withAlpha(
                    if (fadeMode) alphaBackground else backgroundColor.alpha
                ).rgb
                val borderCustomColor = borderColor.withAlpha(
                    if (fadeMode) alphaBorder else borderColor.alpha
                ).rgb
                val textCustomColor = textColor.withAlpha(
                    if (fadeMode) alphaText else textColor.alpha
                ).rgb

                val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
                val actualRainbowX = 1f safeDiv rainbowX
                val actualRainbowY = 1f safeDiv rainbowY

                glPushMatrix()

                glEnable(GL_BLEND)
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

                if (fadeMode && isAlpha || smoothMode && isRendered || delayCounter < vanishDelay) {
                    val width = width.coerceAtLeast(0F)
                    val height = height.coerceAtLeast(0F)

                    if (backgroundGlow) {
                        GlowUtils.drawGlow(0f, 0f, width, height, (backgroundGlowStrength * 15F).toInt(), backgroundGlowColor)
                    }

                    if (enableNeon) {
                        glPushMatrix()
                        glTranslated(-renderX, -renderY, 0.0)
                        glScalef(1F / scale, 1F / scale, 1F)
                        RenderUtils.drawNeonBorder(
                            renderX.toFloat(), renderY.toFloat(),
                            width, height,
                            roundedRectRadius, neonColor, blurStrength,
                            neonOuterGlowAlpha, neonInnerBorderAlpha, neonBackgroundAlpha
                        )
                        glPopMatrix()
                    } else if (enableGlass) {
                        glPushMatrix()
                        glTranslated(-renderX, -renderY, 0.0)
                        glScalef(1F / scale, 1F / scale, 1F)
                        RenderUtils.drawGlassmorphism(
                            renderX.toFloat(), renderY.toFloat(),
                            width, height,
                            roundedRectRadius, blurStrength,
                            glassBaseColor, glassHighlightAlpha, glassBorderColor
                        )
                        glPopMatrix()
                    } else if (blur) {
                        glPushMatrix()
                        glTranslated(-renderX, -renderY, 0.0)
                        glScalef(1F / scale, 1F / scale, 1F)
                        BlurUtils.blurAreaRounded(
                            renderX.toFloat(), renderY.toFloat(),
                            renderX.toFloat() + width, renderY.toFloat() + height,
                            roundedRectRadius, blurStrength
                        )
                        glPopMatrix()
                    }

                    RainbowShader.begin(backgroundMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset).use {
                        drawRoundedBorderRect(
                            0F,
                            0F,
                            width,
                            height,
                            borderStrength,
                            if (backgroundMode == "Rainbow") 0 else backgroundCustomColor,
                            borderCustomColor,
                            roundedRectRadius
                        )
                    }

                    ShowShadow(0F,0F,width,height)

                    val healthBarTop = 24F
                    val healthBarHeight = 8F
                    val healthBarStart = 36F
                    val healthBarTotal = (width - 39F).coerceAtLeast(0F)
                    val currentWidth = (easingHealth / maxHealth).coerceIn(0F, 1F) * healthBarTotal

                    val backgroundBar = {
                        drawRoundedRect(
                            healthBarStart,
                            healthBarTop,
                            healthBarStart + healthBarTotal,
                            healthBarTop + healthBarHeight,
                            Color.BLACK.rgb,
                            6F,
                        )
                    }

                    if (roundHealthBarShape) {
                        backgroundBar()
                    }

                    withClipping(main = {
                        if (roundHealthBarShape) {
                            drawRoundedRect(
                                healthBarStart,
                                healthBarTop,
                                healthBarStart + currentWidth,
                                healthBarTop + healthBarHeight,
                                0,
                                6F
                            )
                        } else {
                            backgroundBar()
                        }
                    }, toClip = {
                        drawGradientRect(
                            healthBarStart.toInt(),
                            healthBarTop.toInt(),
                            healthBarStart.toInt() + currentWidth.toInt(),
                            healthBarTop.toInt() + healthBarHeight.toInt(),
                            healthBarColor1.rgb,
                            healthBarColor2.rgb,
                            0f
                        )
                    })

                    val DECIMAL_FORMAT = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
                    val healthPercentage = easingHealth
                    val percentageText = "${DECIMAL_FORMAT.format(healthPercentage)}HP"
                    val distanceToTarget = "${DECIMAL_FORMAT.format(mc.thePlayer.getDistanceToEntity(target))}m"
                    val textWidth1 = healthFont.getStringWidth(percentageText)
                    val calcX = healthBarStart+healthBarTotal-textWidth1
                    val textX = healthBarStart
                    val textY = healthBarTop - Fonts.fontRegular30.fontHeight / 2 - 3F
                    healthFont.drawString(percentageText, calcX, textY, textCustomColor, textShadow)
                    healthFont.drawString(distanceToTarget, textX, textY, textCustomColor, textShadow)


                    val shouldRenderBody =
                        (fadeMode && alphaText + alphaBackground + alphaBorder > 100) || (smoothMode && width + height > 100)

                    if (shouldRenderBody) {
                        val renderer = mc.renderManager.getEntityRenderObject<Entity>(target)

                        if (renderer != null) {
                            val entityTexture = renderer.getEntityTexture(target)

                            glPushMatrix()
                            val scale = 1 - easingHurtTime / 10f
                            val f1 = (0.7F..1F).lerpWith(scale) * this.scale
                            val color = ColorUtils.interpolateColor(Color.RED, Color.WHITE, scale)
                            val centerX1 = (4..32).lerpWith(0.5F)
                            val midY = (4f..28f).lerpWith(0.5F)

                            glTranslatef(centerX1, midY, 0f)
                            glScalef(f1, f1, f1)
                            glTranslatef(-centerX1, -midY, 0f)

                            if (entityTexture != null) {
                                withClipping(main = {
                                    drawRoundedRect(4f, 4f, 32f, 32f, 0, roundedRectRadius)
                                }, toClip = {
                                    drawHead(
                                        entityTexture, 4, 4, 8f, 8f, 8, 8, 28, 28, 64F, 64F, color
                                    )
                                })
                            }
                            glPopMatrix()
                        }

                        target.name?.let {
                            if (textGlow) {
                                GlowUtils.drawGlow(
                                    healthBarStart, 6F,
                                    healthBarStart + titleFont.getStringWidth(it), 6F + titleFont.FONT_HEIGHT,
                                    (textGlowStrength * 10F).toInt(), textGlowColor
                                )
                            }
                            titleFont.drawString(it, healthBarStart, 6F, textCustomColor, textShadow)
                        }
                    }
                }

                glPopMatrix()
            }
        }


        lastTarget = target
        return Border(0F, 0F, stringWidth, 36F)
    }
    
    private fun ShowShadow(startX: Float,startY: Float,width: Float,height:Float){
        if (ShadowCheck) {
            GlowUtils.drawGlow(
                startX, startY,
                width, height,
                (ShadowValue * 13F).toInt(),
                Color(0, 0, 0, 120)
            )
        }
    }

    internal fun drawHead(skin: ResourceLocation, x: Int, y: Int, width: Int, height: Int, color: Color) {
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        mc.textureManager.bindTexture(skin)
        drawScaledCustomSizeModalRect(x, y, 8f, 8f, 8, 8, width, height, 64f, 64f)
        GlStateManager.color(1f, 1f, 1f, 1f)
    }
}
