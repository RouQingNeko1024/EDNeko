package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.TargetStyle
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets2.impl.*
import net.ccbluex.liquidbounce.utils.render.*
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.awt.Color

@ElementInfo(name = "Target2", retrieveDamage = true)
class Target2 : Element("Target2") {

    private val styleList = mutableListOf<TargetStyle>()

    private val styleValue = ListValue("Style", arrayOf("Chill"), "Chill")

    val blurValue by boolean("Blur", false)
    val blurStrength by float("Blur-Strength", 1F, 0.01F..40F) { blurValue }

    val shadowValue by boolean("Shadow", false)
    val shadowStrength by float("Shadow-Strength", 1F, 0.01F..40F) { shadowValue }
    val shadowColorMode by choices("Shadow-Color", arrayOf("Background", "Custom", "Bar"), "Background") { shadowValue }

    val shadowColorRedValue by int("Shadow-Red", 0, 0..255) { shadowValue && shadowColorMode.equals("custom", ignoreCase = true) }
    val shadowColorGreenValue by int("Shadow-Green", 111, 0..255) { shadowValue && shadowColorMode.equals("custom", ignoreCase = true) }
    val shadowColorBlueValue by int("Shadow-Blue", 255, 0..255) { shadowValue && shadowColorMode.equals("custom", ignoreCase = true) }

    val noAnimValue by boolean("No-Animation", false)
    val globalAnimSpeed by float("Global-AnimSpeed", 3F, 1F..6.30F) { !noAnimValue }

    val showWithChatOpen by boolean("Show-ChatOpen", true)

    val animationType by choices("AnimationType", arrayOf("Scale", "Fade", "Slide", "Bounce", "Zoom", "Elastic", "SlideUp", "SlideDown", "SlideLeft", "SlideRight", "Rotate", "Pulse", "None"), "Scale")
    val animSpeed by float("AnimationSpeed", 0.1F, 0.01F..0.5F)
    val bounceTension by float("BounceTension", 0.08f, 0.01f..0.5f) { animationType == "Bounce" }
    val bounceFriction by float("BounceFriction", 0.2f, 0.01f..0.5f) { animationType == "Bounce" }
    val slideDistance by float("SlideDistance", 50F, 10F..200F) { animationType in listOf("Slide", "SlideUp", "SlideDown", "SlideLeft", "SlideRight") }

    val colorModeValue by choices("Color", arrayOf("Custom", "Rainbow", "Sky", "Slowly", "Fade", "Mixer", "Health"), "Custom")
    val redValue by int("Red", 252, 0..255)
    val greenValue by int("Green", 96, 0..255)
    val blueValue by int("Blue", 66, 0..255)
    val saturationValue by float("Saturation", 1F, 0F..1F)
    val brightnessValue by float("Brightness", 1F, 0F..1F)
    val waveSecondValue by int("Seconds", 2, 1..10)
    val bgColorValue by color("Background", Color(0, 0, 0, 160))

    val bordercolor: Color
        get() = Color(redValue, greenValue, blueValue)

    val styleValueName: String
        get() = styleValue.get()

    var barColor = Color(-1)
    var bgColor = Color(-1)
    var animProgress = 0F

    private var animAlpha = 0F
    private var animScale = 0F
    private var animSlideX = 0F
    private var animSlideY = 0F
    private var animRotation = 0F
    
    private var velAlpha = 0f
    private var velScale = 0f
    private var velSlideX = 0f
    private var velSlideY = 0f
    private var velRotation = 0f
    
    private var lastHasTarget = false

    val counter1 = intArrayOf(50)
    val counter2 = intArrayOf(80)

    private var mainTarget: EntityPlayer? = null

    private fun spring(current: Float, target: Float, velocity: Float, tension: Float = bounceTension, friction: Float = bounceFriction): Pair<Float, Float> {
        val displacement = target - current
        val force = displacement * tension
        val drag = velocity * friction
        val acceleration = force - drag
        val newVelocity = velocity + acceleration
        val newPosition = current + newVelocity
        return newPosition to newVelocity
    }

    init {
        val styles = arrayOf(
            Astolfo(this),
            Astolfo2(this),
            AsuidBounce(this),
            Chill(this),
            Exhibition(this),
            Flux(this),
            Hanabi(this),
            LiquidBounce(this),
            Lnk(this),
            Moon(this),
            Moon4(this),
            Novoline(this),
            Novoline2(this),
            Novoline3(this),
            Raven(this),
            RavenB4(this),
            Remix(this),
            Rice(this),
            Slowly(this),
            Tifality(this)
        )
        styles.forEach { styleList.add(it) }
        styleValue.values = styleList.map { it.name }.toTypedArray()

        addValue(styleValue)
        styleList.forEach { addValues(it.values) }
    }

    override fun drawElement(): Border? {
        val isEditing = mc.currentScreen is GuiHudDesigner
        
        val kaTarget = KillAura.target

        if (isEditing) {
            mainTarget = mc.thePlayer
        } else if (kaTarget != null && kaTarget is EntityPlayer) {
            mainTarget = kaTarget as EntityPlayer
        } else if (mc.currentScreen is GuiChat && showWithChatOpen) {
            mainTarget = mc.thePlayer
        } else {
            mainTarget = null
        }

        val currentStyleName = styleValue.get()
        val mainStyle = styleList.find { it.name.equals(currentStyleName, ignoreCase = true) } ?: return Border(0F, 0F, 120F, 48F)

        val hasTarget = mainTarget != null
        
        if (!hasTarget) {
            lastHasTarget = false
            return Border(0F, 0F, 120F, 48F)
        }

        val convertTarget = mainTarget!!

        val preBarColor = when (colorModeValue) {
            "Rainbow" -> Color(ColorUtils.getRainbowOpaque(waveSecondValue, saturationValue, brightnessValue, 0))
            "Custom" -> Color(redValue, greenValue, blueValue)
            "Sky" -> ColorUtils.skyRainbow(0, saturationValue, brightnessValue, 1f)
            "Fade" -> ColorUtils.fade(Color(redValue, greenValue, blueValue), 0, 100)
            "Health" -> BlendUtils.getHealthColor(convertTarget.health, convertTarget.maxHealth)
            "Mixer" -> getMixedColor(0, waveSecondValue)
            else -> ColorUtils.LiquidSlowly(System.nanoTime(), 0, saturationValue, brightnessValue)
        }

        barColor = ColorUtils.reAlpha(preBarColor, preBarColor.alpha / 255F * (1F - animProgress))
        bgColor = Color(bgColorValue.red, bgColorValue.green, bgColorValue.blue, (bgColorValue.alpha * (1F - animProgress)).toInt())

        val returnBorder = mainStyle.getBorder(convertTarget) ?: return Border(0F, 0F, 120F, 48F)

        val targetScale = 1F
        val targetAlpha = 1F
        
        if (hasTarget && !lastHasTarget) {
            animScale = 0F
            animAlpha = 0F
            animSlideX = if (animationType == "Slide" || animationType == "SlideRight") -slideDistance else if (animationType == "SlideLeft") slideDistance else 0F
            animSlideY = if (animationType == "SlideUp") slideDistance else if (animationType == "SlideDown") -slideDistance else 0F
            animRotation = if (animationType == "Rotate") 360F else 0F
            velAlpha = 0f
            velScale = 0f
        }
        lastHasTarget = hasTarget

        when (animationType) {
            "Scale" -> {
                animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animScale.toDouble(), targetScale.toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = 1F
            }
            "Fade" -> {
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
            }
            "Slide" -> {
                animSlideX = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideX.toDouble(), (if (hasTarget) 0F else -slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
            }
            "SlideUp" -> {
                animSlideY = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideY.toDouble(), (if (hasTarget) 0F else slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
            }
            "SlideDown" -> {
                animSlideY = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideY.toDouble(), (if (hasTarget) 0F else -slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
            }
            "SlideLeft" -> {
                animSlideX = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideX.toDouble(), (if (hasTarget) 0F else slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
            }
            "SlideRight" -> {
                animSlideX = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideX.toDouble(), (if (hasTarget) 0F else -slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
            }
            "Bounce" -> {
                val (nextAlpha, vA) = spring(animAlpha, targetAlpha, velAlpha)
                animAlpha = nextAlpha.coerceIn(0F, 1F)
                velAlpha = vA
                
                val (nextScale, vS) = spring(animScale, targetScale, velScale)
                animScale = nextScale.coerceIn(0F, 1.5F)
                velScale = vS
            }
            "Zoom" -> {
                val zoomTarget = if (hasTarget) 1F else 0F
                animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animScale.toDouble(), zoomTarget.toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = animScale
            }
            "Elastic" -> {
                val elasticTarget = if (hasTarget) 1F else 0F
                animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animScale.toDouble(), elasticTarget.toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = animScale
            }
            "Rotate" -> {
                animRotation = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animRotation.toDouble(), (if (hasTarget) 0F else 360F).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
            }
            "Pulse" -> {
                val pulseTarget = if (hasTarget) 1F else 0F
                animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animScale.toDouble(), pulseTarget.toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = animScale
            }
            "None" -> {
                animScale = 1F
                animAlpha = 1F
                animSlideX = 0F
                animSlideY = 0F
                animRotation = 0F
            }
        }

        GL11.glPushMatrix()
        
        try {
            if (shadowValue && mainStyle.shaderSupport) {
                GL11.glTranslated(-renderX, -renderY, 0.0)
                GL11.glPushMatrix()
                ShadowUtils.shadow(shadowStrength, {
                    GL11.glPushMatrix()
                    GL11.glTranslated(renderX, renderY, 0.0)
                    mainStyle.handleShadow(convertTarget)
                    GL11.glPopMatrix()
                }, {
                    GL11.glPushMatrix()
                    GL11.glTranslated(renderX, renderY, 0.0)
                    mainStyle.handleShadowCut(convertTarget)
                    GL11.glPopMatrix()
                })
                GL11.glPopMatrix()
                GL11.glTranslated(renderX, renderY, 0.0)
            }

            if (blurValue && mainStyle.shaderSupport) {
                val floatX = renderX.toFloat()
                val floatY = renderY.toFloat()
                GL11.glTranslated(-renderX, -renderY, 0.0)
                GL11.glPushMatrix()
                BlurUtils.blur(floatX + returnBorder.x, floatY + returnBorder.y, floatX + returnBorder.x2, floatY + returnBorder.y2, blurStrength * (1F - animProgress), false) {
                    GL11.glPushMatrix()
                    GL11.glTranslated(renderX, renderY, 0.0)
                    mainStyle.handleBlur(convertTarget)
                    GL11.glPopMatrix()
                }
                GL11.glPopMatrix()
                GL11.glTranslated(renderX, renderY, 0.0)
            }

            if (mainStyle is Chill) {
                val borderWidth = returnBorder.x2 - returnBorder.x
                val borderHeight = returnBorder.y2 - returnBorder.y
                val calcScaleX = animProgress * (4F / (borderWidth / 2F))
                val calcScaleY = animProgress * (4F / (borderHeight / 2F))
                val calcTranslateX = borderWidth / 2F * calcScaleX
                val calcTranslateY = borderHeight / 2F * calcScaleY
                mainStyle.updateData(renderX.toFloat() + calcTranslateX, renderY.toFloat() + calcTranslateY, calcScaleX, calcScaleY)
            }

            val styleWidth = returnBorder.x2 - returnBorder.x
            val styleHeight = returnBorder.y2 - returnBorder.y
            val centerX = returnBorder.x + styleWidth / 2F
            val centerY = returnBorder.y + styleHeight / 2F

            GlStateManager.pushMatrix()
            
            when (animationType) {
                "Scale" -> {
                    GlStateManager.translate(centerX, centerY, 0f)
                    GlStateManager.scale(animScale, animScale, 1f)
                    GlStateManager.translate(-centerX, -centerY, 0f)
                }
                "Slide" -> {
                    GlStateManager.translate(animSlideX, 0f, 0f)
                }
                "SlideUp" -> {
                    GlStateManager.translate(0f, animSlideY, 0f)
                }
                "SlideDown" -> {
                    GlStateManager.translate(0f, animSlideY, 0f)
                }
                "SlideLeft" -> {
                    GlStateManager.translate(animSlideX, 0f, 0f)
                }
                "SlideRight" -> {
                    GlStateManager.translate(animSlideX, 0f, 0f)
                }
                "Bounce" -> {
                    GlStateManager.translate(centerX, centerY, 0f)
                    GlStateManager.scale(animScale, animScale, 1f)
                    GlStateManager.translate(-centerX, -centerY, 0f)
                }
                "Zoom" -> {
                    GlStateManager.translate(centerX, centerY, 0f)
                    GlStateManager.scale(animScale, animScale, 1f)
                    GlStateManager.translate(-centerX, -centerY, 0f)
                }
                "Elastic" -> {
                    GlStateManager.translate(centerX, centerY, 0f)
                    GlStateManager.scale(animScale, animScale, 1f)
                    GlStateManager.translate(-centerX, -centerY, 0f)
                }
                "Rotate" -> {
                    GlStateManager.translate(centerX, centerY, 0f)
                    GlStateManager.rotate(animRotation, 0f, 0f, 1f)
                    GlStateManager.translate(-centerX, -centerY, 0f)
                }
                "Pulse" -> {
                    val pulseScale = animScale + Math.sin(System.currentTimeMillis() / 100.0) * 0.05f
                    GlStateManager.translate(centerX, centerY, 0f)
                    GlStateManager.scale(pulseScale.toFloat(), pulseScale.toFloat(), 1f)
                    GlStateManager.translate(-centerX, -centerY, 0f)
                }
            }

            mainStyle.drawTarget(convertTarget)
            
            GlStateManager.popMatrix()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            GlStateManager.resetColor()
            GL11.glPopMatrix()
        }

        return returnBorder
    }

    override fun handleDamage(ent: EntityPlayer) {
        if (mainTarget != null && ent == mainTarget) {
            val currentStyleName = styleValue.get()
            val mainStyle = styleList.find { it.name.equals(currentStyleName, ignoreCase = true) }
            mainStyle?.handleDamage(ent)
        }
    }

    fun getFadeProgress() = animProgress
}
