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
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.EmbeddedStencil
import net.ccbluex.liquidbounce.utils.render.InternalBlurShader
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHead
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumChatFormatting.BOLD
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.*

@ElementInfo(name = "Target")
class Target : Element("Target"), Listenable {
    private val onlyOnKillaura by boolean("OnlyOnKillaura", true)
    private val livingTime by int("LivingTime", 3, 0..20) { !onlyOnKillaura }

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

    private val hudStyle by choices(
        "Style",
        arrayOf(
            "Default",
            "Rise",
            "Flux",
            "Arc",
            "Compact",
            "Moon4",
            "Novoline",
            "Chill",
            "Astolfo",
            "Myau",
            "RavenB4",
            "Naven",
            "Southside",
            "Animated",
            "Styles",
            "Exhibition",
            "Opai",
            "Augustus",
            "Liquid",
            "Radar",
            "Digital",
            "Crystal",
            "Matrix",
            "Pixel",
            "Neon",
            "Glitch",
            "Aqua",
            "Outline",
            "Elegant"
        ),
        "Flux"
    )
    private val animSpeed by float("AnimationSpeed", 0.1F, 0.01F..0.5F)
    private val animationType by choices("AnimationType", arrayOf("Scale", "Fade", "Slide", "Bounce", "Zoom", "Elastic", "SlideUp", "SlideDown", "SlideLeft", "SlideRight", "Flip", "Rotate", "Pulse", "None"), "Scale")
    private val bounceTension by float("BounceTension", 0.08f, 0.01f..0.5f) { animationType == "Bounce" }
    private val bounceFriction by float("BounceFriction", 0.2f, 0.01f..0.5f) { animationType == "Bounce" }
    private val slideDistance by float("SlideDistance", 50F, 10F..200F) { animationType in listOf("Slide", "SlideUp", "SlideDown", "SlideLeft", "SlideRight") }

    private val fluxColorMode by choices("Flux-Color", arrayOf("Custom", "Health", "Rainbow"), "Health") { hudStyle == "Flux" }
    private val fluxColorRed by int("Flux-Red", 0, 0..255) { hudStyle == "Flux" && fluxColorMode == "Custom" }
    private val fluxColorGreen by int("Flux-Green", 120, 0..255) { hudStyle == "Flux" && fluxColorMode == "Custom" }
    private val fluxColorBlue by int("Flux-Blue", 255, 0..255) { hudStyle == "Flux" && fluxColorMode == "Custom" }

    private val arcRainbow by boolean("Arc-Rainbow", true) { hudStyle == "Arc" }
    private val arcColorRed by int("Arc-Red", 255, 0..255) { hudStyle == "Arc" && !arcRainbow }
    private val arcColorGreen by int("Arc-Green", 255, 0..255) { hudStyle == "Arc" && !arcRainbow }
    private val arcColorBlue by int("Arc-Blue", 255, 0..255) { hudStyle == "Arc" && !arcRainbow }

    private val moon4BarColorR by int("Moon4-BarR", 70, 0..255) { hudStyle == "Moon4" }
    private val moon4BarColorG by int("Moon4-BarG", 130, 0..255) { hudStyle == "Moon4" }
    private val moon4BarColorB by int("Moon4-BarB", 255, 0..255) { hudStyle == "Moon4" }
    private val moon4BGColorR by int("Moon4-BGR", 30, 0..255) { hudStyle == "Moon4" }
    private val moon4BGColorG by int("Moon4-BGG", 30, 0..255) { hudStyle == "Moon4" }
    private val moon4BGColorB by int("Moon4-BGB", 30, 0..255) { hudStyle == "Moon4" }
    private val moon4BGColorA by int("Moon4-BGA", 180, 0..255) { hudStyle == "Moon4" }
    private val moon4AnimSpeed by int("Moon4-AnimSpeed", 4, 1..10) { hudStyle == "Moon4" }

    private val newStyleAvatarSize by int("NewStyle-AvatarSize", 30, 20..50) { hudStyle in listOf("Liquid", "Radar", "Digital", "Crystal", "Matrix", "Pixel", "Neon", "Glitch", "Aqua", "Outline", "Elegant") }

    private val rainbow by boolean("Myau-Rainbow", true) { hudStyle == "Myau" }
    private val borderRed by int("Myau-Border-Red", 255, 0..255) { hudStyle == "Myau" }
    private val borderGreen by int("Myau-Border-Green", 255, 0..255) { hudStyle == "Myau" }
    private val borderBlue by int("Myau-Border-Blue", 255, 0..255) { hudStyle == "Myau" }
    private val showAvatar by boolean("Myau-Show-Avatar", true) { hudStyle == "Myau" }

    private val barColorR by int("RavenB4-BarColorR", 255, 0..255) { hudStyle == "RavenB4" }
    private val barColorG by int("RavenB4-BarColorG", 255, 0..255) { hudStyle == "RavenB4" }
    private val barColorB by int("RavenB4-BarColorB", 255, 0..255) { hudStyle == "RavenB4" }
    private val animSpeedRB4 by int("RavenB4-AnimSpeed", 3, 1..10) { hudStyle == "RavenB4" }

    private val fontMode by choices("Font", arrayOf("Minecraft", "HarmonyOS"), "Minecraft")

    private fun drawFontString(text: String, x: Float, y: Float, color: Int) {
        when (fontMode) {
            "Minecraft" -> mc.fontRendererObj.drawString(text, x.toInt(), y.toInt(), color)
            "HarmonyOS" -> Fonts.font40.drawString(text, x, y, color)
        }
    }

    private fun getFontWidth(text: String): Int {
        return when (fontMode) {
            "Minecraft" -> mc.fontRendererObj.getStringWidth(text)
            "HarmonyOS" -> Fonts.font40.getStringWidth(text)
            else -> mc.fontRendererObj.getStringWidth(text)
        }
    }

    private val novolineColorMode by choices("Novoline-Color", arrayOf("Custom", "Health", "Rainbow"), "Health") { hudStyle == "Novoline" }
    private val novolineColorRed by int("Novoline-Red", 0, 0..255) { hudStyle == "Novoline" && novolineColorMode == "Custom" }
    private val novolineColorGreen by int("Novoline-Green", 120, 0..255) { hudStyle == "Novoline" && novolineColorMode == "Custom" }
    private val novolineColorBlue by int("Novoline-Blue", 255, 0..255) { hudStyle == "Novoline" && novolineColorMode == "Custom" }
    private val novolineColorSpec by boolean("Novoline-Gradient",true) {hudStyle == "Novoline"}
    private val novolineLeftColor by color("Novoline-left-Color", Color(0, 255, 150)) { novolineColorSpec }
    private val novolineRightColor by color("Novoline-right-Color", Color(10, 80, 120)) { novolineColorSpec }

    private val chillFontSpeed by float("Chill-FontSpeed", 0.5F, 0.01F..1F) { hudStyle == "Chill" }
    private val chillRoundedBar by boolean("Chill-RoundedBar", true) { hudStyle == "Chill" }
    private val chillFont by font("Chill-Font", Fonts.font40) { hudStyle == "Chill" }

    private val astolfoBarColor by color("Astolfo-BarColor", Color(0, 120, 255)) { hudStyle == "Astolfo" }
    private val astolfoBgColor by color("Astolfo-BgColor", Color(0, 0, 0, 120)) { hudStyle == "Astolfo" }

    private val stylesShadow by boolean("Styles-Shadow", true) { hudStyle == "Styles" }

    private val opaiThemeColor by color("Opai-themeColor", Color(242,172,244)) { hudStyle == "Opai" }
    private val opaiBackGroundAlpha by int("Opai-BackGroundAlpha",120,0..255) { hudStyle == "Opai" }
    private val opaiShadowCheck by boolean("Opai-ShadowCheck",false) { hudStyle == "Opai" }
    private val opaiShadowStrengh by float("Opai-shadowStrengh",0.5f,0.0f..1.0f) { hudStyle == "Opai" && opaiShadowCheck }
    private val opaiBlurCheck by boolean("Opai-BlurCheck", true) { hudStyle == "Opai" }
    private val opaiBlurRadius by float("Opai-BlurStrength", 10f, 1f..50f) { hudStyle == "Opai" && opaiBlurCheck }
    private val opaiVanishDelay by int("Opai-VanishDelay", 300, 0..500) { hudStyle == "Opai" }

    private val augustusBackgroundAlpha by int("Augustus-BackgroundAlpha", 60, 0..255) { hudStyle == "Augustus" }
    private val augustusGlowColor by color("Augustus-GlowColor", Color(0, 150, 255)) { hudStyle == "Augustus" }
    private val augustusGlowStrength by float("Augustus-GlowStrength", 0.3f, 0.0f..1.0f) { hudStyle == "Augustus" }
    private val augustusCornerRadius by float("Augustus-CornerRadius", 6f, 0f..20f) { hudStyle == "Augustus" }

    private val defaultRoundedRectRadius by float("Default-Rounded-Radius", 3F, 0F..5F) { hudStyle == "Default" }
    private val defaultBorderStrength by float("Default-Border-Strength", 3F, 1F..5F) { hudStyle == "Default" }
    private val defaultBackgroundMode by choices("Default-Background-ColorMode", arrayOf("Custom", "Rainbow"), "Custom") { hudStyle == "Default" }
    private val defaultBackgroundColor by color("Default-Background-Color", Color(0, 0, 0, 150)) { hudStyle == "Default" && defaultBackgroundMode == "Custom" }
    private val defaultBorderColor by color("Default-Border-Color", Color.BLACK) { hudStyle == "Default" }
    private val defaultTextColor by color("Default-TextColor", Color.WHITE) { hudStyle == "Default" }
    private val defaultHealthBarColor1 by color("Default-HealthBar-Gradient1", Color(3, 65, 252)) { hudStyle == "Default" }
    private val defaultHealthBarColor2 by color("Default-HealthBar-Gradient2", Color(3, 252, 236)) { hudStyle == "Default" }
    private val defaultTitleFont by font("Default-TitleFont", Fonts.fontSemibold40) { hudStyle == "Default" }
    private val defaultHealthFont by font("Default-HealthFont", Fonts.fontRegular30) { hudStyle == "Default" }
    private val defaultTextShadow by boolean("Default-TextShadow", false) { hudStyle == "Default" }
    private val defaultFadeSpeed by float("Default-FadeSpeed", 2F, 1F..9F) { hudStyle == "Default" }
    private val defaultAnimation by choices("Default-Animation", arrayOf("Smooth", "Fade"), "Fade") { hudStyle == "Default" }
    private val defaultAnimationSpeed by float("Default-AnimationSpeed", 0.2F, 0.05F..1F) { hudStyle == "Default" }
    private val defaultVanishDelay by int("Default-VanishDelay", 300, 0..500) { hudStyle == "Default" }
    private val defaultRoundHealthBarShape by boolean("Default-RoundHealthBarShape", true) { hudStyle == "Default" }

    private val riseHealthBarColor by color("Rise-HealthBar-Color", Color(0, 120, 255)) { hudStyle == "Rise" }

    private val followTarget by boolean("FollowTarget", false)

    private val decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
    private var target: EntityLivingBase? = null
    private var lastTarget: EntityLivingBase? = null
    private var hue = 0.0f

    private var easingHealth = 0F
    private var moon4EasingHealth = 0F
    private var southsideEasingHealth = 0F
    private var slideIn = 0F
    private var damageHealth = 0F
    private var stylesEasingHealth = 0F
    private var NavenEasingHealth = 0F
    private var prevHealth = -1f
    private var avatarScale = 1f
    private var avatarTargetScale = 1f
    private var avatarTintAlpha = 0f
    private var isHitAnimating = false
    private var hitAnimTimer = 0f
    private val hitAnimDuration = 500f

    private var animAlpha = 1F
    private var animScale = 1F
    private var animSlideX = 0F
    private var animSlideY = 0F
    private var animRotation = 0F
    
    private var velAlpha = 0f
    private var velScale = 0f
    private var velSlideX = 0f
    private var velSlideY = 0f
    private var velRotation = 0f

    private fun spring(current: Float, target: Float, velocity: Float, tension: Float = bounceTension, friction: Float = bounceFriction): Pair<Float, Float> {
        val displacement = target - current
        val force = displacement * tension
        val drag = velocity * friction
        val acceleration = force - drag
        val newVelocity = velocity + acceleration
        val newPosition = current + newVelocity
        return newPosition to newVelocity
    }

    private var opaiDelayCounter = 0
    private var opaiAnimX = 135F
    private var opaiAvatarScale = 1f
    private var opaiAvatarTargetScale = 1f
    private var opaiAvatarTintAlpha = 0f
    private var opaiIsHitAnimating = false
    private var opaiHitAnimTimer = 0f
    private var opaiPrevHealth = -1f
    private val opaiHitAnimDuration = 500f
    private var augustusEasingHealth = 0F

    private var defaultWidth = 0f
    private var defaultHeight = 0f
    private var defaultAlphaText = 0
    private var defaultAlphaBackground = 0
    private var defaultAlphaBorder = 0
    private var defaultDelayCounter = 0
    private var defaultEasingHurtTime = 0F
    private var easingHurtTime = 0F

    private val chillCharRenderer = net.ccbluex.liquidbounce.utils.render.CharRenderer(false)
    private var chillCalcScaleX = 0F
    private var chillCalcScaleY = 0F
    private var chillCalcTranslateX = 0F
    private var chillCalcTranslateY = 0F

    private var astolfoLastTarget: EntityPlayer? = null

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

    private fun updateSouthsideEasingHealth(targetHealth: Float, maxHealth: Float) {
        if (targetHealth.isNaN() || targetHealth.isInfinite() || maxHealth.isNaN() || maxHealth.isInfinite()) {
            return
        }
        
        val changeAmount = abs(southsideEasingHealth - targetHealth)
        var speed = 0.02f * deltaTime

        if (changeAmount > 5) {
            speed *= 2.0f
        } else if (changeAmount > 2) {
            speed *= 1.5f
        }

        if (abs(southsideEasingHealth - targetHealth) < 0.1f) {
            southsideEasingHealth = targetHealth
        } else if (southsideEasingHealth > targetHealth) {
            southsideEasingHealth -= min(speed * 1.2f, southsideEasingHealth - targetHealth)
        } else {
            southsideEasingHealth += min(speed, targetHealth - southsideEasingHealth)
        }
        southsideEasingHealth = southsideEasingHealth.coerceAtMost(maxHealth)
    }

    private fun renderSouthsideHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)

        val health = entity.health
        val maxHealth = entity.maxHealth
        val healthPercent = (health / maxHealth).coerceIn(0f, 1f)

        updateSouthsideEasingHealth(health, maxHealth)
        val easingHealthPercent = (southsideEasingHealth / maxHealth).coerceIn(0f, 1f)

        val name = entity.name
        val width = Fonts.fontSemibold40.getStringWidth(name) + 75f
        val presentWidth = easingHealthPercent * width
        val height = 40f

        GlStateManager.pushMatrix()

        val animOutput = slideIn
        GlStateManager.translate((x + width / 2) * (1 - animOutput).toDouble(), (y + 20) * (1 - animOutput).toDouble(), 0.0)
        GlStateManager.scale(animOutput, animOutput, animOutput)

        RenderUtils.drawRect(x, y, x + width, y + height, Color(0, 0, 0, 100).rgb)
        RenderUtils.drawRect(x, y, x + presentWidth, y + height, Color(230, 230, 230, 100).rgb)

        val healthColor = when {
            healthPercent > 0.5 -> Color(63, 157, 4, 150)
            healthPercent > 0.25 -> Color(255, 144, 2, 150)
            else -> Color(168, 1, 1, 150)
        }
        RenderUtils.drawRect(x, y + 12.5f, x + 3, y + 27.5f, healthColor.rgb)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            drawHead(it.locationSkin, x.toInt() + 7, y.toInt() + 7, 26, 26, getHurtColor(Color.WHITE))
        } ?: RenderUtils.drawRect(x + 6, y + 6, x + 34, y + 34, Color.BLACK.rgb)

        Fonts.fontSemibold40.drawString(name, x + 40, y + 7, Color(200, 200, 200, 255).rgb)
        Fonts.fontSemibold40.drawString("${health.toInt()} HP", x + 40, y + 22, Color(200, 200, 200, 255).rgb)

        val itemStack = entity.heldItem
        val itemX = x + Fonts.fontSemibold40.getStringWidth(name) + 50
        if (itemStack != null) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(itemX, y + 12, 0f)
            GlStateManager.scale(1.5f, 1.5f, 1.5f)
            enableGUIStandardItemLighting()
            mc.renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0)
            disableStandardItemLighting()
            GlStateManager.popMatrix()
        } else {
            Fonts.fontSemibold40.drawString("?", x + Fonts.fontSemibold40.getStringWidth(name) + 55, y + 11, Color(200, 200, 200, 255).rgb)
        }

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    override fun drawElement(): Border? {
        val kaTarget = KillAura.target

        val isEditing = mc.currentScreen is net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner

        if (isEditing) {
            target = mc.thePlayer
        } else if (onlyOnKillaura) {
            if (kaTarget != null && kaTarget is EntityPlayer && !AntiBot.isBot(kaTarget)) {
                target = kaTarget
            } else if (mc.currentScreen is GuiChat) {
                target = mc.thePlayer
            } else if (target != null && (KillAura.target == null || !target!!.isEntityAlive || AntiBot.isBot(target!!))) {
                target = null
            }
        } else {
            val currentTime = System.currentTimeMillis()
            val elapsedSeconds = (currentTime - targetDisappearTime) / 1000.0

            if (kaTarget != null && kaTarget is EntityPlayer && !AntiBot.isBot(kaTarget)) {
                target = kaTarget
                targetDisappearTime = currentTime
            } else if (mc.currentScreen is GuiChat) {
                target = mc.thePlayer
            } else if (attackTarget != null && attackTarget!!.isEntityAlive && !AntiBot.isBot(attackTarget!!)) {
                if (elapsedSeconds <= livingTime) {
                    target = attackTarget
                } else {
                    target = null
                    attackTarget = null
                }
            } else {
                if (elapsedSeconds <= livingTime && lastTarget != null && lastTarget!!.isEntityAlive) {
                    target = lastTarget
                } else {
                    target = null
                    attackTarget = null
                }
            }
        }

        if (target != lastTarget) {
            if (lastTarget != null) {
                val lastTargetHealth = if (lastTarget!!.health.isNaN() || lastTarget!!.health.isInfinite()) 0f else lastTarget!!.health
                easingHealth = lastTargetHealth
                damageHealth = lastTargetHealth
                augustusEasingHealth = lastTargetHealth
            } else if (target != null) {
                val targetHealth = if (target!!.health.isNaN() || target!!.health.isInfinite()) 0f else target!!.health
                easingHealth = targetHealth
                damageHealth = targetHealth
                augustusEasingHealth = targetHealth
            }
            if (target != null) {
                val targetHealth = if (target!!.health.isNaN() || target!!.health.isInfinite()) 0f else target!!.health
                southsideEasingHealth = targetHealth
                NavenEasingHealth = targetHealth
                moon4EasingHealth = targetHealth
            }
        }

        lastTarget = target
        
        if (target != null) {
            updateHurtTime(target!!)
        }

        hue += 0.05f * deltaTime * 0.1f
        if (hue > 1F) hue = 0F

        val hasTarget = target != null
        val targetAlpha = if (hasTarget) 1F else 0F
        val targetScale = if (hasTarget) 1F else 0F

        when (animationType) {
            "Scale" -> {
                animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animScale.toDouble(), targetScale.toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = 1F
                slideIn = animScale
            }
            "Fade" -> {
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
                slideIn = animAlpha
            }
            "Slide" -> {
                animSlideX = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideX.toDouble(), (if (hasTarget) 0F else -slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
                slideIn = animAlpha
            }
            "SlideUp" -> {
                animSlideY = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideY.toDouble(), (if (hasTarget) 0F else slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
                slideIn = animAlpha
            }
            "SlideDown" -> {
                animSlideY = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideY.toDouble(), (if (hasTarget) 0F else -slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
                slideIn = animAlpha
            }
            "SlideLeft" -> {
                animSlideX = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideX.toDouble(), (if (hasTarget) 0F else slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
                slideIn = animAlpha
            }
            "SlideRight" -> {
                animSlideX = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideX.toDouble(), (if (hasTarget) 0F else -slideDistance).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = 1F
                slideIn = animAlpha
            }
            "Bounce" -> {
                val (nextAlpha, vA) = spring(animAlpha, targetAlpha, velAlpha)
                animAlpha = nextAlpha.coerceIn(0F, 1F)
                velAlpha = vA
                
                val (nextScale, vS) = spring(animScale, targetScale, velScale)
                animScale = nextScale.coerceIn(0F, 1.5F)
                velScale = vS
                slideIn = animScale
            }
            "Elastic" -> {
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.easeOutElasticX(animAlpha.toDouble()).toFloat().coerceIn(0F, 1.5F)
                slideIn = animScale
            }
            "Zoom" -> {
                animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animScale.toDouble(), targetScale.toDouble(), animSpeed.toDouble() * 1.5).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                slideIn = animScale
            }
            "Flip" -> {
                animRotation = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animRotation.toDouble(), (if (hasTarget) 0F else 90F).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                slideIn = animAlpha
            }
            "Rotate" -> {
                animRotation = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animRotation.toDouble(), (if (hasTarget) 0F else 180F).toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                slideIn = animAlpha
            }
            "Pulse" -> {
                val pulseScale = if (hasTarget) {
                    1F + 0.05F * kotlin.math.sin(System.currentTimeMillis() * 0.005).toFloat()
                } else {
                    0F
                }
                animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animScale.toDouble(), pulseScale.toDouble(), animSpeed.toDouble()).toFloat()
                animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animSpeed.toDouble()).toFloat()
                slideIn = animScale
            }
            "None" -> {
                animAlpha = 1F
                animScale = 1F
                slideIn = 1F
            }
            else -> {
                slideIn = lerp(slideIn, if (target != null) 1F else 0F, animSpeed)
            }
        }
        
        if (target != null) {
            val targetHealth = if (target!!.health.isNaN() || target!!.health.isInfinite()) 0f else target!!.health
            augustusEasingHealth = lerp(augustusEasingHealth, targetHealth, animSpeed)
        } else {
            augustusEasingHealth = lerp(augustusEasingHealth, 0F, animSpeed)
        }

        val x = 0f
        val y = 0f

        val offsetX = if (followTarget && target != null && target != mc.thePlayer) {
            val screenPos = projectEntity(target ?: return null)
            if (screenPos != null) {
                screenPos[0] - x
            } else {
                0f
            }
        } else {
            0f
        }

        val offsetY = if (followTarget && target != null && target != mc.thePlayer) {
            val screenPos = projectEntity(target ?: return null)
            if (screenPos != null) {
                screenPos[1] - y
            } else {
                0f
            }
        } else {
            0f
        }

        if (slideIn < 0.01F && target == null) {
            val (defaultWidth, defaultHeight) = when (hudStyle.lowercase(Locale.getDefault())) {
                "default" -> 140f to 36f
                "rise" -> 120f to 50f
                "flux" -> 140f to 46f
                "arc" -> 80f to 50f
                "compact" -> 120f to 18f
                "moon4" -> 150f to 40f
                "novoline" -> 120f to 46f
                "chill" -> 150f to 48f
                "astolfo" -> 118f to 42f
                "myau" -> 120f to 25f
                "ravenb4" -> 150f to 35f
                "naven" -> 130f to 50f
                "southside" -> 150f to 40f
                "animated" -> 190f to 52f
                "styles" -> 132f to 44f
                "exhibition" -> 120f to 45f
                "opai" -> 150f to 50f
                "augustus" -> 150f to 40f
            "liquid" -> 150f to 50f
            "radar" -> 150f to 50f
            "digital" -> 150f to 50f
            "crystal" -> 150f to 50f
            "fire" -> 150f to 50f
            "matrix" -> 150f to 50f
            else -> 135f to 35f
            }
            return Border(x, y, x + defaultWidth, y + defaultHeight)
        }

        val renderX = x + offsetX
        val renderY = y + offsetY
        
        val (styleWidth, styleHeight) = when (hudStyle.lowercase(Locale.getDefault())) {
            "default" -> 140f to 36f
            "rise" -> 120f to 50f
            "flux" -> 140f to 46f
            "arc" -> 80f to 50f
            "compact" -> 120f to 18f
            "moon4" -> 150f to 40f
            "novoline" -> 120f to 46f
            "chill" -> 150f to 48f
            "astolfo" -> 118f to 42f
            "myau" -> 120f to 25f
            "ravenb4" -> 150f to 35f
            "naven" -> 130f to 50f
            "southside" -> 150f to 40f
            "animated" -> 190f to 52f
            "styles" -> 132f to 44f
            "exhibition" -> 120f to 45f
            "opai" -> 150f to 50f
            "augustus" -> 150f to 40f
            "liquid" -> 150f to 50f
            "radar" -> 150f to 50f
            "digital" -> 150f to 50f
            "crystal" -> 150f to 50f
            "matrix" -> 150f to 50f
            "pixel" -> 150f to 50f
            "neon" -> 150f to 50f
            "glitch" -> 150f to 50f
            "aqua" -> 150f to 50f
            "outline" -> 150f to 50f
            "elegant" -> 150f to 50f
            else -> 135f to 35f
        }
        
        if (animationType != "None" && slideIn < 0.99F) {
            val progress = slideIn
            val centerX = renderX + styleWidth / 2
            val centerY = renderY + styleHeight / 2
            
            GlStateManager.pushMatrix()
            
            when (animationType) {
                "Scale" -> {
                    GlStateManager.translate(centerX, centerY, 0f)
                    GlStateManager.scale(progress, progress, 1f)
                    GlStateManager.translate(-centerX, -centerY, 0f)
                }
                "Slide" -> {
                    GlStateManager.translate((1 - progress) * styleWidth * 0.3f, 0f, 0f)
                }
                "Bounce" -> {
                    val bounceProgress = if (progress < 0.5f) {
                        4 * progress * progress * progress
                    } else {
                        (1 - Math.pow((-2 * progress + 2).toDouble(), 3.0) / 2).toFloat()
                    }
                    GlStateManager.translate(centerX, centerY, 0f)
                    GlStateManager.scale(bounceProgress, bounceProgress, 1f)
                    GlStateManager.translate(-centerX, -centerY, 0f)
                }
                "Zoom" -> {
                    val zoomProgress = progress * progress
                    GlStateManager.translate(centerX, centerY, 0f)
                    GlStateManager.scale(zoomProgress, zoomProgress, 1f)
                    GlStateManager.translate(-centerX, -centerY, 0f)
                }
            }
            
            val result = when (hudStyle.lowercase(Locale.getDefault())) {
                "default" -> renderDefaultHUD(renderX, renderY)
                "rise" -> renderRiseHUD(renderX, renderY)
                "flux" -> renderFluxHUD(renderX, renderY)
                "arc" -> renderArcHUD(renderX, renderY)
                "compact" -> renderCompactHUD(renderX, renderY)
                "moon4" -> renderMoon4HUD(renderX, renderY)
                "novoline" -> renderNovolineHUD(renderX, renderY)
                "chill" -> renderChillHUD(renderX, renderY)
                "astolfo" -> renderAstolfoHUD(renderX, renderY)
                "myau" -> renderMyauHUD(renderX, renderY)
                "ravenb4" -> renderRavenB4HUD(renderX, renderY)
                "naven" -> renderNavenHUD(renderX, renderY)
                "southside" -> renderSouthsideHUD(renderX, renderY)
                "animated" -> renderAnimatedHUD(renderX, renderY)
                "styles" -> renderStylesHUD(renderX, renderY)
                "exhibition" -> renderExhibitionHUD(renderX, renderY)
                "opai" -> renderOpaiHUD(renderX, renderY)
                "augustus" -> renderAugustusHUD(renderX, renderY)
                "liquid" -> renderLiquidHUD(renderX, renderY)
                "radar" -> renderRadarHUD(renderX, renderY)
                "digital" -> renderDigitalHUD(renderX, renderY)
                "crystal" -> renderCrystalHUD(renderX, renderY)
                "matrix" -> renderMatrixHUD(renderX, renderY)
                "pixel" -> renderPixelHUD(renderX, renderY)
                "neon" -> renderNeonHUD(renderX, renderY)
                "glitch" -> renderGlitchHUD(renderX, renderY)
                "aqua" -> renderAquaHUD(renderX, renderY)
                "outline" -> renderOutlineHUD(renderX, renderY)
                "elegant" -> renderElegantHUD(renderX, renderY)
                else -> renderFluxHUD(renderX, renderY)
            }
            
            GlStateManager.popMatrix()
            return result
        }

        return when (hudStyle.lowercase(Locale.getDefault())) {
            "default" -> renderDefaultHUD(renderX, renderY)
            "rise" -> renderRiseHUD(renderX, renderY)
            "flux" -> renderFluxHUD(renderX, renderY)
            "arc" -> renderArcHUD(renderX, renderY)
            "compact" -> renderCompactHUD(renderX, renderY)
            "moon4" -> renderMoon4HUD(renderX, renderY)
            "novoline" -> renderNovolineHUD(renderX, renderY)
            "chill" -> renderChillHUD(renderX, renderY)
            "astolfo" -> renderAstolfoHUD(renderX, renderY)
            "myau" -> renderMyauHUD(renderX, renderY)
            "ravenb4" -> renderRavenB4HUD(renderX, renderY)
            "naven" -> renderNavenHUD(renderX, renderY)
            "southside" -> renderSouthsideHUD(renderX, renderY)
            "animated" -> renderAnimatedHUD(renderX, renderY)
            "styles" -> renderStylesHUD(renderX, renderY)
            "exhibition" -> renderExhibitionHUD(renderX, renderY)
            "opai" -> renderOpaiHUD(renderX, renderY)
            "augustus" -> renderAugustusHUD(renderX, renderY)
            "liquid" -> renderLiquidHUD(renderX, renderY)
            "radar" -> renderRadarHUD(renderX, renderY)
            "digital" -> renderDigitalHUD(renderX, renderY)
            "crystal" -> renderCrystalHUD(renderX, renderY)
            "matrix" -> renderMatrixHUD(renderX, renderY)
            "pixel" -> renderPixelHUD(renderX, renderY)
            "neon" -> renderNeonHUD(renderX, renderY)
            "glitch" -> renderGlitchHUD(renderX, renderY)
            "aqua" -> renderAquaHUD(renderX, renderY)
            "outline" -> renderOutlineHUD(renderX, renderY)
            "elegant" -> renderElegantHUD(renderX, renderY)
            else -> renderFluxHUD(renderX, renderY)
        }
    }

    private fun renderDefaultHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        
        val smoothMode = defaultAnimation == "Smooth"
        val fadeMode = defaultAnimation == "Fade"
        
        val shouldRender = target != null || mc.currentScreen is GuiChat
        
        if (shouldRender) {
            defaultDelayCounter = 0
        } else if (defaultWidth > 0f || defaultHeight > 0f) {
            defaultDelayCounter++
        }
        
        val stringWidth = (40f + (entity.name?.let(defaultTitleFont::getStringWidth) ?: 0)).coerceAtLeast(118F)
        
        val targetHealth = entity.health
        val maxHealth = entity.maxHealth
        
        easingHealth += (targetHealth - easingHealth) / 2.0f.pow(10f - defaultFadeSpeed) * deltaTime
        easingHealth = easingHealth.coerceIn(0f, maxHealth)
        
        if (smoothMode) {
            val targetWidth = if (shouldRender) stringWidth else if (defaultDelayCounter >= defaultVanishDelay) 0f else defaultWidth
            defaultWidth = AnimationUtil.base(defaultWidth.toDouble(), targetWidth.toDouble(), defaultAnimationSpeed.toDouble())
                .toFloat().coerceAtLeast(0f)

            val targetHeight = if (shouldRender) 36f else if (defaultDelayCounter >= defaultVanishDelay) 0f else defaultHeight
            defaultHeight = AnimationUtil.base(defaultHeight.toDouble(), targetHeight.toDouble(), defaultAnimationSpeed.toDouble())
                .toFloat().coerceAtLeast(0f)
        } else {
            defaultWidth = stringWidth
            defaultHeight = 36f

            val targetText = if (shouldRender) defaultTextColor.alpha else if (defaultDelayCounter >= defaultVanishDelay) 0f else defaultAlphaText
            defaultAlphaText = AnimationUtil.base(defaultAlphaText.toDouble(), targetText.toDouble(), defaultAnimationSpeed.toDouble()).toInt()

            val targetBackground = if (shouldRender) defaultBackgroundColor.alpha else if (defaultDelayCounter >= defaultVanishDelay) 0f else defaultAlphaBackground
            defaultAlphaBackground = AnimationUtil.base(defaultAlphaBackground.toDouble(), targetBackground.toDouble(), defaultAnimationSpeed.toDouble()).toInt()

            val targetBorder = if (shouldRender) defaultBorderColor.alpha else if (defaultDelayCounter >= defaultVanishDelay) 0f else defaultAlphaBorder
            defaultAlphaBorder = AnimationUtil.base(defaultAlphaBorder.toDouble(), targetBorder.toDouble(), defaultAnimationSpeed.toDouble()).toInt()
        }
        
        val backgroundCustomColor = Color(defaultBackgroundColor.red, defaultBackgroundColor.green, defaultBackgroundColor.blue,
            if (fadeMode) defaultAlphaBackground else defaultBackgroundColor.alpha)
        val borderCustomColor = Color(defaultBorderColor.red, defaultBorderColor.green, defaultBorderColor.blue,
            if (fadeMode) defaultAlphaBorder else defaultBorderColor.alpha)
        val textCustomColor = Color(defaultTextColor.red, defaultTextColor.green, defaultTextColor.blue,
            if (fadeMode) defaultAlphaText else defaultTextColor.alpha)
        
        val width = defaultWidth.coerceAtLeast(0F)
        val height = defaultHeight.coerceAtLeast(0F)
        
        if (width < 1f || height < 1f) return Border(x, y, x + stringWidth, y + 36f)
        
        glPushMatrix()
        glTranslatef(x, y, 0f)
        
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
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
                defaultRoundedRectRadius, neonColor, blurStrength,
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
                defaultRoundedRectRadius, blurStrength,
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
                defaultRoundedRectRadius, blurStrength
            )
            glPopMatrix()
        }
        
        RenderUtils.drawRoundedBorderRect(
            0F, 0F, width, height,
            defaultBorderStrength,
            backgroundCustomColor.rgb,
            borderCustomColor.rgb,
            defaultRoundedRectRadius
        )
        
        val healthBarTop = 24F
        val healthBarHeight = 8F
        val healthBarStart = 36F
        val healthBarTotal = (width - 39F).coerceAtLeast(0F)
        val currentWidth = (easingHealth / maxHealth).coerceIn(0F, 1F) * healthBarTotal
        
        if (defaultRoundHealthBarShape) {
            drawRoundedRect(healthBarStart, healthBarTop, healthBarStart + healthBarTotal, healthBarTop + healthBarHeight, Color.BLACK.rgb, 6F)
        }
        
        withClipping(main = {
            if (defaultRoundHealthBarShape) {
                drawRoundedRect(healthBarStart, healthBarTop, healthBarStart + currentWidth, healthBarTop + healthBarHeight, 0, 6F)
            } else {
                drawRoundedRect(healthBarStart, healthBarTop, healthBarStart + healthBarTotal, healthBarTop + healthBarHeight, Color.BLACK.rgb, 6F)
            }
        }, toClip = {
            RenderUtils.drawGradientRect(
                healthBarStart.toInt(), healthBarTop.toInt(),
                healthBarStart.toInt() + currentWidth.toInt(), healthBarTop.toInt() + healthBarHeight.toInt(),
                defaultHealthBarColor1.rgb, defaultHealthBarColor2.rgb, 0F
            )
        })
        
        val healthPercentage = (easingHealth / maxHealth * 100).toInt()
        val percentageText = "$healthPercentage%"
        val textWidth = defaultHealthFont.getStringWidth(percentageText)
        val calcX = healthBarStart + currentWidth - textWidth
        val textX = max(healthBarStart, calcX)
        val textY = healthBarTop - Fonts.fontRegular30.fontHeight / 2 - 2F
        defaultHealthFont.drawString(percentageText, textX, textY, textCustomColor.rgb, defaultTextShadow)
        
        val shouldRenderBody = (fadeMode && defaultAlphaText + defaultAlphaBackground + defaultAlphaBorder > 100) || (smoothMode && width + height > 100)
        
        if (shouldRenderBody) {
            val renderer = mc.renderManager.getEntityRenderObject<Entity>(entity)
            if (renderer != null) {
                val entityTexture = renderer.getEntityTexture(entity)
                if (entityTexture != null) {
                    withClipping(main = {
                        drawRoundedRect(4f, 4f, 32f, 32f, 0, defaultRoundedRectRadius)
                    }, toClip = {
                        RenderUtils.drawHead(entityTexture, 4, 4, 8f, 8f, 8, 8, 28, 28, 64F, 64F, getHurtColor(Color.WHITE))
                    })
                }
            }
            
            entity.name?.let {
                if (textGlow) {
                    GlowUtils.drawGlow(healthBarStart, 6F, healthBarStart + defaultTitleFont.getStringWidth(it), 6F + defaultTitleFont.FONT_HEIGHT, (textGlowStrength * 10F).toInt(), textGlowColor)
                }
                defaultTitleFont.drawString(it, healthBarStart, 6F, textCustomColor.rgb, defaultTextShadow)
            }
        }
        
        glPopMatrix()
        return Border(x, y, x + stringWidth, y + 36F)
    }

    private fun renderRiseHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        val font = Fonts.font35
        val name = entity.name ?: "Unknown"
        
        easingHealth = lerp(easingHealth, entity.health, animSpeed)
        
        val additionalWidth = ((font.getStringWidth(name) * 1.1).toInt().coerceAtLeast(70) + font.getStringWidth("Name: ") * 1.1 + 7.0).toInt()
        val healthBarWidth = additionalWidth - (font.getStringWidth("20") * 1.15).toInt() - 16
        
        val bgAlpha = (130 * slideIn).toInt()
        val textAlpha = (255 * slideIn).toInt()

        glPushMatrix()
        glTranslatef(x, y, 0f)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        RenderUtils.drawRoundedCornerRect(0f, 0f, 50f + additionalWidth, 50f, 7f, Color(0, 0, 0, bgAlpha).rgb)

        val hurtPercent = entity.hurtTime.toFloat() / entity.maxHurtTime.coerceAtLeast(1)
        val scale = if (hurtPercent == 0f) 1f else if (hurtPercent < 0.5f) {
            1 - (0.2f * hurtPercent * 2)
        } else {
            0.8f + (0.2f * (hurtPercent - 0.5f) * 2)
        }

        glPushMatrix()
        glTranslatef(9f, 10f, 0f)
        glColor4f(1f, 1 - hurtPercent, 1 - hurtPercent, 1f)
        Stencil.write(false)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(-2F, -3F, 33F, 33F, 8F)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        Stencil.erase(true)
        
        val skin = mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin
        if (skin != null) {
            RenderUtils.drawHead(skin, -2, -3, 35, 35, getHurtColor(Color(255, 255, 255, textAlpha)))
        }
        
        Stencil.dispose()
        glPopMatrix()
        
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        GlStateManager.resetColor()

        glPushMatrix()
        glScalef(1.1f, 1.1f, 1.1f)
        font.drawString("Name: $name", 45, 14, Color(riseHealthBarColor.red, riseHealthBarColor.green, riseHealthBarColor.blue, textAlpha).rgb)
        font.drawString("Name:", 45, 14, Color(255, 255, 255, textAlpha).rgb)
        glPopMatrix()

        RenderUtils.drawRoundedCornerRect(50f, 31f, 50f + healthBarWidth, 39f, 3f, Color(20, 20, 20, textAlpha).rgb)
        RenderUtils.drawRoundedCornerRect(50f, 31f, 50f + (healthBarWidth * (easingHealth / entity.maxHealth)), 39f, 4f, Color(riseHealthBarColor.red, riseHealthBarColor.green, riseHealthBarColor.blue, textAlpha).rgb)
        RenderUtils.drawRoundedCornerRect(52f, 31f, 48f + (healthBarWidth * (easingHealth / entity.maxHealth)), 34f, 2f, Color(255, 255, 255, 30).rgb)
        RenderUtils.drawRoundedCornerRect(52f, 36f, 48f + (healthBarWidth * (easingHealth / entity.maxHealth)), 39f, 2f, Color(0, 0, 0, 30).rgb)
        
        glPushMatrix()
        glScalef(1.15f, 1.15f, 1.15f)
        font.drawString(entity.health.toInt().toString(), ((38 + additionalWidth - font.getStringWidth(entity.health.toInt().toString())) / 1.15).toInt(), 29 - (font.FONT_HEIGHT / 2), Color(riseHealthBarColor.red, riseHealthBarColor.green, riseHealthBarColor.blue, textAlpha).rgb)
        glPopMatrix()

        glPopMatrix()
        return Border(x, y, x + 50F + additionalWidth, y + 50F)
    }

    private fun renderAugustusHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)

        val avatarSize = 32f
        val avatarPadding = 4f
        val healthBarWidth = 100f
        val healthBarHeight = 20f
        val nameHeight = Fonts.fontSemibold35.FONT_HEIGHT
        val verticalSpacing = 4f

        val totalWidth = avatarSize + avatarPadding * 2 + healthBarWidth + 8f
        val totalHeight = avatarSize + avatarPadding * 2

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)
        GlStateManager.translate(-x, -y, 0F)

        if (backgroundGlow) {
            GlowUtils.drawGlow(x, y, x + totalWidth, y + totalHeight, (backgroundGlowStrength * 15F).toInt(), backgroundGlowColor)
        }

        if (enableNeon) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            RenderUtils.drawNeonBorder(
                renderX.toFloat() + x, renderY.toFloat() + y,
                totalWidth, totalHeight,
                augustusCornerRadius, neonColor, blurStrength,
                neonOuterGlowAlpha, neonInnerBorderAlpha, neonBackgroundAlpha
            )
            glPopMatrix()
        } else if (enableGlass) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            RenderUtils.drawGlassmorphism(
                renderX.toFloat() + x, renderY.toFloat() + y,
                totalWidth, totalHeight,
                augustusCornerRadius, blurStrength,
                glassBaseColor, glassHighlightAlpha, glassBorderColor
            )
            glPopMatrix()
        } else if (blur) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            BlurUtils.blurAreaRounded(
                renderX.toFloat() + x, renderY.toFloat() + y,
                renderX.toFloat() + x + totalWidth, renderY.toFloat() + y + totalHeight,
                augustusCornerRadius, blurStrength
            )
            glPopMatrix()
        }

        GlowUtils.drawGlow(
            x, y,
            x + totalWidth, y + totalHeight,
            (augustusGlowStrength * 15F).toInt(),
            augustusGlowColor
        )

        if (augustusBackgroundAlpha > 0) {
            drawRoundedRect(x, y, x + totalWidth, y + totalHeight, Color(30, 30, 30, augustusBackgroundAlpha).rgb, augustusCornerRadius)

            RenderUtils.drawRoundedBorderRect(
                x, y, x + totalWidth, y + totalHeight, 1f,
                Color(augustusGlowColor.red, augustusGlowColor.green, augustusGlowColor.blue, 150).rgb,
                Color(0, 0, 0, 0).rgb,
                augustusCornerRadius
            )
        }

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            drawRoundedRect(x + avatarPadding, y + avatarPadding, x + avatarPadding + avatarSize, y + avatarPadding + avatarSize, Color.WHITE.rgb, augustusCornerRadius)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(it.locationSkin, (x + avatarPadding).toInt(), (y + avatarPadding).toInt(), avatarSize.toInt(), avatarSize.toInt(), getHurtColor(Color.WHITE))
            Stencil.dispose()
        }

        val name = entity.name
        val nameX = x + avatarSize + avatarPadding * 2
        val availableHeightForText = avatarSize - healthBarHeight - verticalSpacing
        val nameY = y + avatarPadding + (availableHeightForText - nameHeight) / 2
        if (textGlow) {
            GlowUtils.drawGlow(nameX, nameY, nameX + Fonts.fontSemibold35.getStringWidth(name), nameY + nameHeight, (textGlowStrength * 10F).toInt(), textGlowColor)
        }
        Fonts.fontSemibold35.drawString(name, nameX, nameY, Color.WHITE.rgb)

        val healthBarX = x + avatarSize + avatarPadding * 2
        val healthBarY = y + avatarPadding + avatarSize - healthBarHeight

        val safeAugustusEasingHealth = if (augustusEasingHealth.isNaN() || augustusEasingHealth.isInfinite()) 0f else augustusEasingHealth
        val safeMaxHealth = if (entity.maxHealth.isNaN() || entity.maxHealth.isInfinite()) 1f else entity.maxHealth
        
        val healthPercent = (safeAugustusEasingHealth / safeMaxHealth).coerceIn(0f, 1f)
        val healthBarFillWidth = healthBarWidth * healthPercent

        drawRoundedRect(healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + healthBarHeight, Color(40, 40, 40, 200).rgb, 4f)

        val healthBarColor = ColorUtils.getHealthColor(safeAugustusEasingHealth, safeMaxHealth)
        drawRoundedRect(healthBarX, healthBarY, healthBarX + healthBarFillWidth, healthBarY + healthBarHeight, healthBarColor.rgb, 4f)

        GlStateManager.popMatrix()
        return Border(x, y, x + totalWidth, y + totalHeight)
    }

    private fun renderExhibitionHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)

        val width = 120F
        val height = 45F
        val modelSize = 40F
        val borderRadius = 5f

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        drawRoundedRect(0F, 0F, width, height, Color(10, 10, 10, 255).rgb, borderRadius)

        RenderUtils.drawRoundedBorderRect(0F, 0F, width, height, 1F, Color(40, 40, 40, 255).rgb, Color(0, 0, 0, 0).rgb, borderRadius)

        val modelX = 2.5F
        val modelY = (height - modelSize) / 2

        renderPlayerModel(entity, modelX, modelY, modelSize)

        val infoX = modelX + modelSize + 2F
        val infoWidth = width - infoX - 5F

        val name = entity.name
        val nameY = 5F

        mc.fontRendererObj.drawStringWithShadow(
            name,
            infoX,
            nameY,
            Color.WHITE.rgb
        )

        val healthText = "HP: ${decimalFormat.format(entity.health)}"
        val distance = mc.thePlayer.getDistanceToEntity(entity)
        val distanceText = "Dist: ${decimalFormat.format(distance)}"
        val infoText = "$healthText | $distanceText"

        val infoY = nameY + mc.fontRendererObj.FONT_HEIGHT + 2F

        GlStateManager.pushMatrix()
        GlStateManager.translate(infoX, infoY, 0F)
        GlStateManager.scale(0.8F, 0.8F, 0.8F)
        mc.fontRendererObj.drawStringWithShadow(
            infoText,
            0F,
            0F,
            Color.WHITE.rgb
        )
        GlStateManager.popMatrix()

        val barY = infoY + 8F
        val barHeight = 5F
        val barWidth = infoWidth
        val barBorderRadius = 2f

        val healthPercent = entity.health / entity.maxHealth

        val barColor = when {
            healthPercent > 2.0/3.0 -> Color(0, 180, 0)
            healthPercent > 1.0/3.0 -> Color(255, 255, 0)
            else -> Color(255, 0, 0)
        }

        drawRoundedRect(infoX, barY, infoX + barWidth, barY + barHeight, Color(40, 40, 40).rgb, barBorderRadius)

        val barFillWidth = barWidth * healthPercent
        drawRoundedRect(infoX, barY, infoX + barFillWidth, barY + barHeight, barColor.rgb, barBorderRadius)

        val segmentWidth = 4F
        val dividerWidth = 1F
        var currentX = infoX + segmentWidth

        while (currentX < infoX + barWidth) {
            RenderUtils.drawRect(
                currentX, barY,
                currentX + dividerWidth, barY + barHeight,
                Color(10, 10, 10).rgb
            )
            currentX += segmentWidth + dividerWidth
        }

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    private fun renderPlayerModel(entity: EntityLivingBase, x: Float, y: Float, size: Float) {
        GlStateManager.pushMatrix()

        GlStateManager.translate(x + size / 2, y + size, 0F)
        GlStateManager.scale(size / 2, size / 2, size / 2)

        GlStateManager.rotate(180F, 0F, 0F, 1F)
        GlStateManager.rotate(30F, 0F, 1F, 0F)

        RenderHelper.enableStandardItemLighting()
        GlStateManager.enableDepth()
        GlStateManager.disableCull()

        val renderManager = mc.renderManager
        renderManager.isRenderShadow = false

        try {
            renderManager.renderEntityWithPosYaw(
                entity,
                0.0, 0.0, 0.0,
                0F,
                1.0F
            )
        } catch (_: Exception) {
            RenderUtils.drawRect(x, y, x + size, y + size, Color.RED.rgb)
        }

        renderManager.isRenderShadow = true
        GlStateManager.enableCull()
        GlStateManager.disableDepth()
        disableStandardItemLighting()

        GlStateManager.popMatrix()
    }

    private fun renderNovolineHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        val width = 120F
        val height = 46F

        easingHealth = lerp(easingHealth, entity.health, animSpeed)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)
        GlStateManager.translate(-x, -y, 0F)

        if (backgroundGlow) {
            GlowUtils.drawGlow(x, y, x + width, y + height, (backgroundGlowStrength * 15F).toInt(), backgroundGlowColor)
        }

        if (enableNeon) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            RenderUtils.drawNeonBorder(
                renderX.toFloat() + x, renderY.toFloat() + y,
                width, height,
                3f, neonColor, blurStrength,
                neonOuterGlowAlpha, neonInnerBorderAlpha, neonBackgroundAlpha
            )
            glPopMatrix()
        } else if (enableGlass) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            RenderUtils.drawGlassmorphism(
                renderX.toFloat() + x, renderY.toFloat() + y,
                width, height,
                3f, blurStrength,
                glassBaseColor, glassHighlightAlpha, glassBorderColor
            )
            glPopMatrix()
        } else if (blur) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            BlurUtils.blurAreaRounded(
                renderX.toFloat() + x, renderY.toFloat() + y,
                renderX.toFloat() + x + width, renderY.toFloat() + y + height,
                3f, blurStrength
            )
            glPopMatrix()
        }

        RenderUtils.drawRoundedBorderRect(x+1, y+1, x + width - 1, y + height - 1, 1f, Color(40, 40, 40,200).rgb, Color.BLACK.rgb, 0F)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            drawHead(it.locationSkin, (x + 6).toInt(), (y + 6).toInt(), 34, 34, Color.WHITE)
        }

        if (textGlow) {
            GlowUtils.drawGlow(x + 46, y + 8F, x + 46 + Fonts.minecraftFont.getStringWidth(entity.name), y + 8F + Fonts.minecraftFont.FONT_HEIGHT, (textGlowStrength * 10F).toInt(), textGlowColor)
        }
        Fonts.minecraftFont.drawString(entity.name, (x + 46).toInt(), (y + 8).toInt(), Color.WHITE.rgb)

        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val healthBarWidth = (width - 52) * healthPercent
        val barColor = when(novolineColorMode) {
            "Custom" -> Color(novolineColorRed, novolineColorGreen, novolineColorBlue)
            "Rainbow" -> Color.getHSBColor(hue, 0.7f, 0.9f)
            else -> ColorUtils.getHealthColor(easingHealth, entity.maxHealth)
        }

        RenderUtils.drawRect(x + 46, y + 24, x + 46 + healthBarWidth, y + 30, barColor.rgb)
        RenderUtils.drawRect(x + 46, y + 24, x + width - 6, y + 30, Color(0, 0, 0, 100).rgb)

        val healthPercentage = (easingHealth / entity.maxHealth) * 100
        val healthText = "${decimalFormat.format(healthPercentage)}%"
        Fonts.minecraftFont.drawString(healthText, (x + 66).toInt(), (y + 24).toInt(), Color.WHITE.rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    private fun renderStylesHUD(x: Float, y: Float): Border {
        val entity = target ?: return Border(0f, 0f, 0f, 0f)
        val width = (38 + Fonts.fontRegular35.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()
        val height = 44f

        stylesEasingHealth = lerp(stylesEasingHealth, entity.health, animSpeed)

        stylesEasingHealth = lerp(stylesEasingHealth, entity.health, animSpeed * 1.4F)

        if (backgroundGlow) {
            GlowUtils.drawGlow(x, y, x + width + 14f, y + height, (backgroundGlowStrength * 15F).toInt(), backgroundGlowColor)
        }

        if (enableNeon) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            RenderUtils.drawNeonBorder(
                renderX.toFloat() + x, renderY.toFloat() + y,
                width + 14f, height,
                3f, neonColor, blurStrength,
                neonOuterGlowAlpha, neonInnerBorderAlpha, neonBackgroundAlpha
            )
            glPopMatrix()
        } else if (enableGlass) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            RenderUtils.drawGlassmorphism(
                renderX.toFloat() + x, renderY.toFloat() + y,
                width + 14f, height,
                3f, blurStrength,
                glassBaseColor, glassHighlightAlpha, glassBorderColor
            )
            glPopMatrix()
        } else if (blur) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            BlurUtils.blurAreaRounded(
                renderX.toFloat() + x, renderY.toFloat() + y,
                renderX.toFloat() + x + width + 14f, renderY.toFloat() + y + height,
                3f, blurStrength
            )
            glPopMatrix()
        }

        if (stylesShadow) {
            ShowShadow(x, y, width + 14f, height, 0.3F)
        }

        RenderUtils.drawRect(x, y, x + width + 14f, y + height, Color(0, 0, 0, 120).rgb)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            drawHead(it.locationSkin, x.toInt() + 3, y.toInt() + 3, 30, 30, Color.WHITE)
        }

        if (textGlow) {
            GlowUtils.drawGlow(x + 34.5f, y + 4f, x + 34.5f + Fonts.fontRegular35.getStringWidth(entity.name), y + 4f + Fonts.fontRegular35.FONT_HEIGHT, (textGlowStrength * 10F).toInt(), textGlowColor)
        }
        Fonts.fontRegular35.drawString(entity.name, x + 34.5f, y + 4f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Health: ${"%.1f".format(stylesEasingHealth)}", x + 34.5f, y + 14f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Distance: ${"%.1f".format(mc.thePlayer.getDistanceToEntity(entity))}m", x + 34.5f, y + 24f, Color.WHITE.rgb)

        RenderUtils.drawRect(x + 2.5f, y + 35.5f, x + width + 11.5f, y + 37.5f, Color(0, 0, 0, 200).rgb)
        RenderUtils.drawRect(x + 3f, y + 36f, x + 3f + (stylesEasingHealth / entity.maxHealth) * (width + 8.5f), y + 37f, Color(0,255,150))

        RenderUtils.drawRect(x + 2.5f, y + 39.5f, x + width + 11.5f, y + 41.5f, Color(0, 0, 0, 200).rgb)
        RenderUtils.drawRect(x + 3f, y + 40f, x + 3f + (entity.totalArmorValue / 20f) * (width + 8.5f), y + 41f, Color(77, 128, 255).rgb)

        return Border(x, y, x + width + 14f, y + height)
    }

    private fun renderOpaiHUD(x: Float, y: Float): Border {
        val killAuraTarget = KillAura.target.takeIf { it is EntityPlayer }
        val shouldRender = KillAura.handleEvents() && killAuraTarget != null || mc.currentScreen is GuiChat
        val target = killAuraTarget ?: if (opaiDelayCounter >= opaiVanishDelay) {
            mc.thePlayer
        } else {
            lastTarget ?: mc.thePlayer
        }

        if (shouldRender) {
            opaiDelayCounter = 0
        } else {
            opaiDelayCounter++
        }

        if (!shouldRender && opaiDelayCounter >= opaiVanishDelay) return Border(0f, 0f, 0f, 0f)

        val entity = target as? EntityLivingBase ?: return Border(0f, 0f, 0f, 0f)
        
        val targetName = entity.name + "  "
        val targetNameWidth = Fonts.fontRegular35.getStringWidth(targetName)
        val targetHealth = entity.health.toInt()
        val targetHealthWidth = Fonts.fontRegular35.getStringWidth(targetHealth.toString())
        val textsDrawBegin = 3.5f + 30f + 3.5f
        val allTextLen = targetNameWidth + targetHealthWidth
        val resultProgressWidth = maxOf(135f, textsDrawBegin + allTextLen + 8f)
        val panelWidth = 3.5f * 2 + resultProgressWidth
        val panelHeight = 3.5f + 30f + 3.5f + 5f + 3.5f
        val currentRadius = 5f

        val progressBarLength = resultProgressWidth / entity.maxHealth * entity.health
        opaiAnimX = AnimationUtil.base(opaiAnimX.toDouble(), progressBarLength.toDouble(), 0.2).toFloat()

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        try {
            EmbeddedStencil.checkSetupFBO(mc.framebuffer)
            EmbeddedStencil.write(false)
            RenderUtils.drawRoundedRect(0f, 0f, panelWidth, panelHeight, Color.WHITE.rgb, currentRadius)

            EmbeddedStencil.erase(false)
            if (opaiShadowCheck) {
                val maxDist = (opaiShadowStrengh * 13f).toInt()
                for (i in maxDist downTo 1) {
                    val alpha = (100 * (1f - i.toFloat() / (maxDist + 1))).toInt().coerceIn(1, 100)
                    RenderUtils.drawRoundedRect(
                        -i.toFloat(), -i.toFloat(),
                        panelWidth + i.toFloat(), panelHeight + i.toFloat(),
                        Color(opaiThemeColor.red, opaiThemeColor.green, opaiThemeColor.blue, alpha).rgb,
                        currentRadius + i.toFloat()
                    )
                }
            }

            EmbeddedStencil.erase(true)
            if (opaiBlurCheck) {
                GlStateManager.popMatrix()
                glPushMatrix()
                glTranslated(-renderX, -renderY, 0.0)
                glScalef(1F / scale, 1F / scale, 1F)
                val absX = renderX.toFloat() + x
                val absY = renderY.toFloat() + y
                InternalBlurShader.blurArea(absX, absY, panelWidth * slideIn, panelHeight * slideIn, opaiBlurRadius)
                glScalef(scale, scale, scale)
                glTranslated(renderX, renderY, 0.0)
                glPopMatrix()
                GlStateManager.pushMatrix()
                GlStateManager.translate(x, y, 0F)
                GlStateManager.scale(slideIn, slideIn, slideIn)
            }

            RenderUtils.drawRoundedBorderRect(0f, 0f, panelWidth, panelHeight, 0.2f,
                Color(0, 0, 0, opaiBackGroundAlpha).rgb, Color(0, 0, 0, opaiBackGroundAlpha).rgb, currentRadius)

            EmbeddedStencil.dispose()

            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glEnable(GL_LINE_SMOOTH)
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
            glLineWidth(1.5f)
            RenderUtils.drawRoundedBorderRect(-0.5f, -0.5f, panelWidth + 0.5f, panelHeight + 0.5f,
                0.5f, Color(0, 0, 0, 0).rgb, Color(255, 255, 255, 35).rgb, currentRadius + 0.5f)
            glLineWidth(1.0f)
            glDisable(GL_LINE_SMOOTH)
            glEnable(GL_TEXTURE_2D)

        } catch (e: Exception) {
            if (opaiShadowCheck) {
                val maxDist = (opaiShadowStrengh * 13f).toInt()
                for (i in maxDist downTo 1) {
                    val alpha = (100 * (1f - i.toFloat() / (maxDist + 1))).toInt().coerceIn(1, 100)
                    RenderUtils.drawRoundedRect(
                        -i.toFloat(), -i.toFloat(),
                        panelWidth + i.toFloat(), panelHeight + i.toFloat(),
                        Color(opaiThemeColor.red, opaiThemeColor.green, opaiThemeColor.blue, alpha).rgb,
                        currentRadius + i.toFloat()
                    )
                }
            }
            RenderUtils.drawRoundedBorderRect(0f, 0f, panelWidth, panelHeight, 0.2f,
                Color(0, 0, 0, opaiBackGroundAlpha).rgb, Color(0, 0, 0, opaiBackGroundAlpha).rgb, currentRadius)
        }

        val headTexture = mc.renderManager.getEntityRenderObject<Entity>(entity)
            ?.getEntityTexture(entity)
        if (headTexture != null) {
            val headX = 3.5f
            val headY = 3.5f
            val headSize = 30f
            val headRadius = 5f
            
            if (opaiPrevHealth < 0f) opaiPrevHealth = entity.health
            if (entity.health < opaiPrevHealth && !opaiIsHitAnimating) {
                opaiIsHitAnimating = true
                opaiHitAnimTimer = 0f
                opaiAvatarTargetScale = 0.85f
            }
            if (opaiIsHitAnimating) {
                opaiHitAnimTimer += deltaTime
                val progress = (opaiHitAnimTimer / opaiHitAnimDuration).coerceIn(0f, 1f)
                when {
                    progress < 0.4f -> {
                        opaiAvatarTargetScale = 0.85f
                        opaiAvatarTintAlpha = lerp(opaiAvatarTintAlpha, 1f, 0.25f)
                    }
                    progress < 0.75f -> {
                        opaiAvatarTargetScale = 1.12f
                        opaiAvatarTintAlpha = lerp(opaiAvatarTintAlpha, 0.6f, 0.25f)
                    }
                    else -> {
                        opaiAvatarTargetScale = 1f
                        opaiAvatarTintAlpha = lerp(opaiAvatarTintAlpha, 0f, 0.25f)
                    }
                }
                if (progress >= 1f) {
                    opaiIsHitAnimating = false
                    opaiAvatarTargetScale = 1f
                    opaiAvatarTintAlpha = 0f
                    opaiHitAnimTimer = 0f
                }
            } else {
                opaiAvatarTargetScale = 1f
                opaiAvatarTintAlpha = lerp(opaiAvatarTintAlpha, 0f, 0.12f)
            }
            opaiAvatarScale = lerp(opaiAvatarScale, opaiAvatarTargetScale, 0.22f)
            opaiPrevHealth = entity.health
            
            GlStateManager.pushMatrix()
            GlStateManager.translate(headX + headSize / 2f, headY + headSize / 2f, 0f)
            GlStateManager.scale(opaiAvatarScale, opaiAvatarScale, 1f)
            GlStateManager.translate(-headX - headSize / 2f, -headY - headSize / 2f, 0f)
            
            EmbeddedStencil.checkSetupFBO(mc.framebuffer)
            EmbeddedStencil.write(false)
            RenderUtils.drawRoundedRect(headX, headY, headX + headSize, headY + headSize, Color.WHITE.rgb, headRadius)
            EmbeddedStencil.erase(true)
            RenderUtils.drawHead(
                headTexture, headX.toInt(), headY.toInt(),
                8f, 8f, 8, 8, headSize.toInt(), headSize.toInt(), 64f, 64f, Color.WHITE
            )
            EmbeddedStencil.dispose()
            
            if (opaiAvatarTintAlpha > 0.01f) {
                val tint = Color(255, 50, 50, (opaiAvatarTintAlpha * 140).toInt())
                RenderUtils.drawRoundedRect(headX, headY, headX + headSize, headY + headSize, tint.rgb, headRadius)
            }
            
            GlStateManager.popMatrix()
        }

        RenderUtils.drawRoundedBorderRect(3.5f, 3.5f + 30f + 3.5f, resultProgressWidth, 3.5f + 30f + 3.5f + 5f,
            0.3f, Color(0, 0, 0, 200).rgb, Color(0, 0, 0, 200).rgb, 5f)

        RenderUtils.drawRoundedBorderRect(3.5f, 3.5f + 30f + 3.5f, opaiAnimX, 3.5f + 30f + 3.5f + 5f,
            0.3f, Color(opaiThemeColor.red, opaiThemeColor.green, opaiThemeColor.blue, 150).rgb,
            Color(opaiThemeColor.red, opaiThemeColor.green, opaiThemeColor.blue, 150).rgb, 4f)

        RenderUtils.drawRoundedBorderRect(3.5f, 3.5f + 30f + 3.5f, progressBarLength, 3.5f + 30f + 3.5f + 5f,
            0.3f, opaiThemeColor.rgb, opaiThemeColor.rgb, 4f)

        Fonts.fontRegular35.drawString(targetName, textsDrawBegin + 3.5f, 3.5f * 2, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString(targetHealth.toString(), textsDrawBegin + targetNameWidth + 3.5f, 3.5f * 2 - 1F, opaiThemeColor.rgb)

        if (entity is EntityPlayer) {
            glEnable(GL_TEXTURE_2D)
            GlStateManager.color(1F, 1F, 1F, 1F)
            enableGUIStandardItemLighting()
            var armorX = textsDrawBegin
            for (index in 3 downTo 0) {
                val stack = entity.inventory.armorInventory[index] ?: continue
                mc.renderItem.renderItemIntoGUI(stack, armorX.toInt(), (3.5f + 30f - 18).toInt())
                mc.renderItem.renderItemOverlays(mc.fontRendererObj, stack, armorX.toInt(), (3.5f + 30f - 18).toInt())
                armorX += 18f
            }
            disableStandardItemLighting()
        }

        GlStateManager.popMatrix()
        return Border(x, y, x + panelWidth, y + panelHeight)
    }

    private fun drawOpaiHead(target: EntityLivingBase, x: Float, y: Float) {
        val texture = mc.renderManager.getEntityRenderObject<Entity>(target)
            ?.getEntityTexture(target) ?: return

        withClipping(main = {
            drawRoundedRect(x, y, x + 30f, y + 30f, 0, 5f)
        }, toClip = {
            drawHead(
                texture, x.toInt(), y.toInt(),
                30, 30,
                Color.WHITE
            )
        })
    }

    private fun drawOpaiArmor(x: Float, y: Float, target: EntityLivingBase) {
        if (target !is EntityPlayer) return
        GlStateManager.pushMatrix()
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
        GlStateManager.popMatrix()
    }

    private fun drawCircleArc(x: Float, y: Float, radius: Float, lineWidth: Float, startAngle: Float, endAngle: Float, color: Color) {
        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(lineWidth)

        glColor4f(color.red / 255F, color.green / 255F, color.blue / 255F, color.alpha / 255F)

        glBegin(GL_LINE_STRIP)
        for (i in (startAngle / 360 * 100).toInt()..(endAngle / 360 * 100).toInt()) {
            val angle = (i / 100.0 * 360.0 * (PI / 180)).toFloat()
            glVertex2f(x + sin(angle) * radius, y + cos(angle) * radius)
        }
        glEnd()

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glPopMatrix()
        glColor4f(1f, 1f, 1f, 1f)
    }

    private fun drawCircle(x: Float, y: Float, radius: Float, color: Int) {
        val side = (radius * 2).toInt()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_POLYGON_SMOOTH)
        glBegin(GL_TRIANGLE_FAN)
        RenderUtils.glColor(Color(color))
        for (i in 0..side) {
            val angle = i * (Math.PI * 2) / side
            glVertex2d(x + sin(angle) * radius, y + cos(angle) * radius)
        }
        glEnd()
        glDisable(GL_POLYGON_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
    }

    private fun lerp(start: Float, end: Float, speed: Float): Float = start + (end - start) * speed * (deltaTime / (1000F / 60F))

    private fun updateAnim(targetHealth: Float) {
        easingHealth += ((targetHealth - easingHealth) / 2.0f.pow(10.0f - animSpeed * 10f)) * deltaTime
    }

    private fun updateHurtTime(entity: EntityLivingBase) {
        val targetHurtTime = if (entity.isEntityAlive) entity.hurtTime.toFloat() else 0F
        easingHurtTime = lerp(easingHurtTime, targetHurtTime, 0.5F)
    }

    private fun getHurtColor(baseColor: Color): Color {
        val scale = 1 - easingHurtTime / 10f
        return ColorUtils.interpolateColor(Color.RED, baseColor, scale)
    }

    private fun getHurtScale(): Float = 1 - easingHurtTime / 10f

    private fun getRainbowColor(): Color = Color.getHSBColor(hue, 1f, 1f)

    private fun renderNavenHUD(x: Float, y: Float): Border {
        val entity = target ?: return Border(0f, 0f, 0f, 0f)
        val width = 130f
        val height = 50f

        NavenEasingHealth = lerp(NavenEasingHealth, entity.health, animSpeed)

        NavenEasingHealth = lerp(NavenEasingHealth, entity.health, animSpeed * 1.1F)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        if (backgroundGlow) {
            GlowUtils.drawGlow(0F, 0F, width, height, (backgroundGlowStrength * 15F).toInt(), backgroundGlowColor)
        }

        if (enableNeon) {
            glPushMatrix()
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F / scale, 1F / scale, 1F)
            RenderUtils.drawNeonBorder(
                renderX.toFloat(), renderY.toFloat(),
                width, height,
                5f, neonColor, blurStrength,
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
                5f, blurStrength,
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
                5f, blurStrength
            )
            glPopMatrix()
        }

        ShowShadow(0F, 0F, width, height, 0.3F)

        drawRoundedRect(0F, 0F, width, height, Color(30, 30, 30, 180).rgb, 5f)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin?.let {
            drawHead(it, 7, 7, 30, 30, Color.WHITE)
        }

        val barX1 = 5f
        val barY1 = height - 10f
        val barX2 = width - 5f
        val barY2 = barY1 + 3f
        drawRoundedRect(barX1, barY1, barX2, barY2, Color(0, 0, 0, 200).rgb, 2f)

        val healthPercent = NavenEasingHealth / entity.maxHealth
        val fillX2 = barX1 + (barX2 - barX1) * healthPercent
        drawRoundedRect(barX1, barY1, fillX2, barY2, Color(160, 42, 42).rgb, 2f)

        if (textGlow) {
            GlowUtils.drawGlow(40f, 10f, 40f + Fonts.fontRegular35.getStringWidth(entity.name), 10f + Fonts.fontRegular35.FONT_HEIGHT, (textGlowStrength * 10F).toInt(), textGlowColor)
        }
        Fonts.fontRegular35.drawString(entity.name, 40f, 10f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Health: ${"%.2f".format(NavenEasingHealth)}", 40f, 22f, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString("Distance: ${"%.2f".format(entity.getDistanceToEntity(mc.thePlayer))}", 40f, 30f, Color.WHITE.rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    private fun drawRoundedHead(skinLocation: ResourceLocation, x: Int, y: Int, width: Int, height: Int, color: Color, radius: Float = 6f) {
        Stencil.write(false)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        drawRoundedRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), color.rgb, radius)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        Stencil.erase(true)
        drawHead(skinLocation, x, y, width, height, color)
        Stencil.dispose()
    }

    private fun drawHead(skinLocation: ResourceLocation, x: Int, y: Int, width: Int, height: Int, color: Color) {
        RenderUtils.drawHead(skinLocation, x, y, 8f, 8f, 8, 8, width, height, 64f, 64f, color)
    }

    private fun ShowShadow(startX: Float, startY: Float, width: Float, height: Float, shadowStrengh: Float) {
        GlowUtils.drawGlow(
            startX, startY,
            width, height,
            (shadowStrengh * 13F).toInt(),
            Color(0, 0, 0, 100)
        )
    }

    private fun renderFluxHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        val width = 140F
        val height = 46F

        easingHealth = lerp(easingHealth, entity.health, animSpeed)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)
        GlStateManager.translate(-x, -y, 0F)

        RenderUtils.drawRoundedRect(x, y, x + width, y + height, Color(25, 25, 25, (200 * slideIn).toInt()).rgb, 4F)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            drawHead(it.locationSkin, (x + 6).toInt(), (y + 6).toInt(), 34, 34, Color.WHITE)
        }

        drawFontString(entity.name, x + 46, y + 8, Color.WHITE.rgb)

        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val healthBarWidth = (width - 52) * healthPercent
        val barColor = when (fluxColorMode) {
            "Custom" -> Color(fluxColorRed, fluxColorGreen, fluxColorBlue)
            "Rainbow" -> Color.getHSBColor(hue, 0.7f, 0.9f)
            else -> ColorUtils.getHealthColor(easingHealth, entity.maxHealth)
        }

        RenderUtils.drawRect(x + 46, y + 22, x + width - 6, y + 30, Color(45, 45, 45).rgb)
        RenderUtils.drawRect(x + 46, y + 22, x + 46 + healthBarWidth, y + 30, barColor.rgb)

        val healthText = decimalFormat.format(easingHealth)
        drawFontString(healthText, x + 48, y + 23, Color.WHITE.rgb)

        val distance = mc.thePlayer.getDistanceToEntity(entity)
        val distanceText = "${decimalFormat.format(distance)}m"
        drawFontString(distanceText, x + 46, y + 33, Color(200, 200, 200).rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    private fun renderArcHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        val size = 50F

        easingHealth = lerp(easingHealth, entity.health, animSpeed)

        GlStateManager.pushMatrix()
        val scale = slideIn.pow(0.5f)
        GlStateManager.translate(x + size / 2, y + size / 2, 0F)
        GlStateManager.scale(scale, scale, scale)
        GlStateManager.translate(-(x + size / 2), -(y + size / 2), 0F)

        RenderUtils.withClipping({ drawCircle(x + size / 2, y + size / 2, size / 2 - 3, Color.WHITE.rgb) }, {
            mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
                drawHead(
                    it.locationSkin,
                    x.toInt() + 3,
                    y.toInt() + 3,
                    (size - 6).toInt(),
                    (size - 6).toInt(),
                    Color.WHITE
                )
            }
        })

        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val arcColor = if (arcRainbow) Color.getHSBColor(hue, 0.6f, 1f) else Color(arcColorRed, arcColorGreen, arcColorBlue)

        drawCircleArc(x + size / 2, y + size / 2, size / 2 - 1.5F, 3F, 0F, 360F, Color(40, 40, 40, (200 * slideIn).toInt()))
        if (healthPercent > 0) {
            drawCircleArc(x + size / 2, y + size / 2, size / 2 - 1.5F, 3F, -90F, 360F * healthPercent, arcColor)
        }

        val textX = x + size + 5
        val nameColor = Color(255, 255, 255, (255 * slideIn).toInt()).rgb
        val healthColor = Color(200, 200, 200, (255 * slideIn).toInt()).rgb
        drawFontString(entity.name, textX, y + 8, nameColor)
        drawFontString("HP: ${decimalFormat.format(easingHealth)}", textX, y + 24, healthColor)

        GlStateManager.popMatrix()
        return Border(x, y, x + size + 80, y + size)
    }

    private fun renderCompactHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        val width = 120F
        val height = 18F

        if (target != null) {
            easingHealth = lerp(easingHealth, entity.health, animSpeed * 1.5f)
            if (abs(entity.health - damageHealth) > 0.1f && easingHealth < damageHealth) {
                damageHealth = lerp(damageHealth, easingHealth, animSpeed * 0.5f)
            } else {
                damageHealth = easingHealth
            }
        } else {
            easingHealth = lerp(easingHealth, 0f, animSpeed)
            damageHealth = lerp(damageHealth, 0f, animSpeed)
        }

        if (target != null && target != lastTarget) {
            damageHealth = entity.maxHealth
        }

        GlStateManager.pushMatrix()
        val scale = slideIn.pow(2f)
        GlStateManager.translate(x + width / 2, y + height / 2, 0F)
        GlStateManager.scale(1f, scale, 1f)
        GlStateManager.translate(-(x + width / 2), -(y + height / 2), 0F)

        if (scale < 0.05f) {
            GlStateManager.popMatrix()
            return Border(x, y, x + width, y + height)
        }

        RenderUtils.drawRect(x, y, x + width, y + height, Color(20, 20, 20, (180 * scale).toInt()).rgb)

        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val damagePercent = (damageHealth / entity.maxHealth).coerceIn(0F, 1F)
        val barColor = ColorUtils.getHealthColor(easingHealth, entity.maxHealth)

        RenderUtils.drawRect(x + 2, y + 2, x + 2 + (width - 4) * damagePercent, y + height - 2, barColor.brighter().rgb)
        RenderUtils.drawRect(x + 2, y + 2, x + 2 + (width - 4) * healthPercent, y + height - 2, barColor.rgb)

        val text = "${entity.name} - ${decimalFormat.format(easingHealth)} HP"
        when (fontMode) {
            "Minecraft" -> {
                val textWidth = mc.fontRendererObj.getStringWidth(text)
                mc.fontRendererObj.drawString(
                    text,
                    x + width / 2 - textWidth / 2,
                    y + height / 2 - mc.fontRendererObj.FONT_HEIGHT / 2 + 1,
                    Color.WHITE.rgb,
                    true
                )
            }
            "HarmonyOS" -> Fonts.font35.drawCenteredString(
                text,
                x + width / 2,
                y + height / 2 - Fonts.font35.FONT_HEIGHT / 2f + 1f,
                Color.WHITE.rgb,
                true
            )
        }

        GlStateManager.popMatrix()
        return Border(x, y, x + width, y + height)
    }

    private fun renderMoon4HUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)

        val currentHealth = target?.health ?: 0f
        moon4EasingHealth += ((currentHealth - moon4EasingHealth) / 2.0F.pow(10.0F - moon4AnimSpeed)) * deltaTime

        val mainColor = Color(moon4BarColorR, moon4BarColorG, moon4BarColorB)
        val bgColor = Color(moon4BGColorR, moon4BGColorG, moon4BGColorB, moon4BGColorA)

        val boldName = "$BOLD${entity.name}"
        val healthInt = entity.health.toInt()
        val percentText = "$BOLD${healthInt}HP"

        val nameLength = (Fonts.fontSF40.getStringWidth(boldName)).coerceAtLeast(
            Fonts.fontSF35.getStringWidth(percentText)
        ).toFloat() + 20F

        val healthPercent = (entity.health / entity.maxHealth).coerceIn(0F, 1F)
        val barWidth = healthPercent * (nameLength - 2F)
        val animateThingy = (moon4EasingHealth.coerceIn(entity.health, entity.maxHealth) / entity.maxHealth) * (nameLength - 2F)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        RenderUtils.drawRoundedRect(-2F, -2F, 3F + nameLength + 36F, 2F + 36F, bgColor.rgb, 3f)
        RenderUtils.drawRoundedRect(-1F, -1F, 2F + nameLength + 36F, 1F + 36F, Color(0, 0, 0, 50).rgb, 3f)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let { playerInfo ->
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.drawRoundedRect(1f, 0.5f, 1f + 35f, 0.5f + 35f, Color.WHITE.rgb, 7F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, 1, 0, 35, 35, Color.WHITE)
            Stencil.dispose()
        }

        Fonts.fontSF40.drawStringWithShadow(boldName, 2F + 36F, 2F, -1)
        Fonts.fontSF35.drawStringWithShadow(percentText, 38F, 15F, Color.WHITE.rgb)

        RenderUtils.drawRoundedRect(37F, 23F, 37F + nameLength, 33f, Color(0, 0, 0, 100).rgb, 3f)
        if (animateThingy > 0) {
            RenderUtils.drawRoundedRect(38F, 24F, 38F + animateThingy, 32f, mainColor.darker().rgb, 3f)
        }
        RenderUtils.drawRoundedRect(38F, 24F, 38F + barWidth, 32f, mainColor.rgb, 3f)

        GlStateManager.popMatrix()
        return Border(x, y, x + nameLength + 40, y + 38)
    }

    private fun renderChillHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)

        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val tWidth = (45F + chillFont.getStringWidth(name)
            .coerceAtLeast(chillFont.getStringWidth("%.1f".format(health)))).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 48F, Color(0, 0, 0, 120).rgb, 7F)
        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        if (playerInfo != null) {
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(4F, 4F, 34F, 34F, 7F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, 4, 4, 30, 30, Color.WHITE)
            Stencil.dispose()
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        chillFont.drawString(name, 38, 6, Color.WHITE.rgb)
        val newTranslateX = chillCharRenderer.renderChar(
            health,
            chillCalcTranslateX,
            chillCalcTranslateY,
            38F,
            17F,
            chillCalcScaleX,
            chillCalcScaleY,
            false,
            chillFontSpeed,
            Color.WHITE.rgb
        )
        chillCalcTranslateX = newTranslateX

        RenderUtils.drawRoundedRect(4F, 38F, tWidth - 4F, 44F, Color(50, 50, 50).rgb, 3F)

        Stencil.write(false)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(4F, 38F, tWidth - 4F, 44F, 3F)
        glDisable(GL_BLEND)
        Stencil.erase(true)
        if (chillRoundedBar)
            RenderUtils.customRounded(
                4F,
                38F,
                4F + (easingHealth / entity.maxHealth) * (tWidth - 8F),
                44F,
                0F,
                3F,
                3F,
                0F,
                ColorUtils.getHealthColor(easingHealth, entity.maxHealth).rgb
            )
        else
            RenderUtils.drawRect(
                4F,
                38F,
                4F + (easingHealth / entity.maxHealth) * (tWidth - 8F),
                44F,
                ColorUtils.getHealthColor(easingHealth, entity.maxHealth).rgb
            )
        Stencil.dispose()

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 48)
    }

    private fun renderAstolfoHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        val font = Fonts.minecraftFont
        val healthString = "%.1f ".format(entity.health)

        if (entity != astolfoLastTarget || easingHealth < 0 || easingHealth > entity.maxHealth ||
            abs(easingHealth - entity.health) < 0.01
        ) {
            easingHealth = entity.health
        }
        val width = (38 + Fonts.minecraftFont.getStringWidth(entity.name))
            .coerceAtLeast(118)
            .toFloat()

        updateAnim(entity.health)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        RenderUtils.drawRect(2F, -1F, width - 3F, 42F, astolfoBgColor.rgb)

        val healthLength = (entity.health / entity.maxHealth).coerceIn(0F, 1F)
        val darkerColor = Color(
            (astolfoBarColor.red * 0.7f).toInt().coerceIn(0, 255),
            (astolfoBarColor.green * 0.7f).toInt().coerceIn(0, 255),
            (astolfoBarColor.blue * 0.7f).toInt().coerceIn(0, 255)
        )
        RenderUtils.drawRect(
            36F,
            32.5F,
            42F + 69F,
            39F,
            darkerColor.rgb
        )
        RenderUtils.drawRect(
            36F,
            32.5F,
            36F + (easingHealth / entity.maxHealth).coerceIn(0F, entity.maxHealth) * (healthLength + 74F),
            39F,
            astolfoBarColor.rgb
        )

        font.drawStringWithShadow(entity.name, 36F, 4F, Color.WHITE.rgb)

        glPushMatrix()
        glScalef(1.5F, 1.5F, 1.5F)
        font.drawStringWithShadow("$healthString❤", 24F, 11F, astolfoBarColor.rgb)
        glPopMatrix()

        GlStateManager.resetColor()
        GlStateManager.color(1.0f, 1.0f, 1.0f)
        RenderUtils.drawEntityOnScreen(19, 38, 19, entity)
        GlStateManager.resetColor()

        GlStateManager.popMatrix()
        return Border(x + 2F, y - 1F, x + width - 3F, y + 42F)
    }

    private fun renderMyauHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        val nameWidth = Fonts.font35.getStringWidth(entity.name)
        val hudWidth = maxOf(80f, nameWidth + 20f)
        val hudHeight = 25f
        val avatarSize = hudHeight

        val borderColor = if (rainbow) getRainbowColor() else Color(borderRed, borderGreen, borderBlue)
        val healthBarColor = if (rainbow) getRainbowColor() else Color(
            maxOf(borderRed - 50, 0),
            maxOf(borderGreen - 50, 0),
            maxOf(borderBlue - 50, 0)
        )

        val totalWidth = if (showAvatar) hudWidth + avatarSize else hudWidth

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)
        GlStateManager.translate(-x, -y, 0F)

        RenderUtils.drawRect(x - 1, y - 1, x + totalWidth + 1, y, borderColor.rgb)
        RenderUtils.drawRect(x - 1, y + hudHeight, x + totalWidth + 1, y + hudHeight + 1, borderColor.rgb)
        RenderUtils.drawRect(x - 1, y, x, y + hudHeight, borderColor.rgb)
        RenderUtils.drawRect(x + totalWidth, y, x + totalWidth + 1, y + hudHeight, borderColor.rgb)
        RenderUtils.drawRect(x, y, x + totalWidth, y + hudHeight, Color(0, 0, 0, 100).rgb)

        if (showAvatar) {
            mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin?.let {
                drawHead(it, x.toInt(), y.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            }
        }

        val textX = if (showAvatar) x + avatarSize + 3 else x + 3
        drawFontString(entity.name, textX, y + 1, Color.WHITE.rgb)
        val healthText = String.format("%.1f", entity.health)
        drawFontString(healthText, textX, y + 11, healthBarColor.rgb)
        drawFontString("\u2764", textX + getFontWidth(healthText) + 2, y + 11, healthBarColor.rgb)

        val barY = y + 21
        val barWidth = hudWidth - 5f
        RenderUtils.drawRect(textX, barY, textX + barWidth, barY + 3, Color(64, 64, 64).rgb)
        val targetFill = (entity.health / entity.maxHealth) * barWidth
        easingHealth = lerp(easingHealth, targetFill, 0.1f)
        RenderUtils.drawRect(textX, barY, textX + easingHealth, barY + 3, healthBarColor.rgb)

        val playerHealth = mc.thePlayer.health
        val (winLoss, wlColor) = when {
            playerHealth > entity.health -> "W" to Color(0, 255, 0)
            playerHealth < entity.health -> "L" to Color(255, 0, 0)
            else -> "D" to Color(255, 255, 0)
        }
        drawFontString(winLoss, x + totalWidth - getFontWidth(winLoss) - 1, y + 1, wlColor.rgb)

        val diff = playerHealth - entity.health
        val diffText = if (diff > 0) "+${"%.1f".format(diff)}" else String.format("%.1f", diff)
        val diffColor = when {
            diff > 0 -> Color(0, 255, 0)
            diff < 0 -> Color(255, 0, 0)
            else -> Color(255, 255, 0)
        }
        drawFontString(diffText, maxOf(x + totalWidth - getFontWidth(diffText) - 1, textX), y + 11, diffColor.rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + totalWidth, y + hudHeight)
    }

    private fun renderRavenB4HUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        val font = when (fontMode) {
            "Minecraft" -> mc.fontRendererObj
            "HarmonyOS" -> Fonts.font40
            else -> mc.fontRendererObj
        }
        val hp = decimalFormat.format(entity.health)
        val hplength = font.getStringWidth(hp)
        val length = font.getStringWidth(entity.displayName.formattedText)
        val barColor = Color(barColorR, barColorG, barColorB)
        val totalWidth = x + length + hplength + 23F
        val totalHeight = y + 35F

        GlStateManager.pushMatrix()
        updateRavenB4Anim(entity.health)
        RenderUtils.drawRoundedGradientOutlineCorner(x, y, totalWidth, totalHeight, 2F, 8F, barColor.rgb, barColor.rgb)
        RenderUtils.drawRoundedRect(x, y, totalWidth, totalHeight, Color(0, 0, 0, 100).rgb, 4F)
        GlStateManager.enableBlend()
        font.drawStringWithShadow(entity.displayName.formattedText, x + 6F, y + 8F, Color.WHITE.rgb)

        val winOrLose = if (entity.health < mc.thePlayer.health) "W" else "L"
        val wlColor = if (winOrLose == "W") Color(0, 255, 0).rgb else Color(139, 0, 0).rgb
        font.drawStringWithShadow(winOrLose, x + length + hplength + 11.6F, y + 8F, wlColor)

        font.drawStringWithShadow(
            hp,
            x + length + 8F,
            y + 8F,
            ColorUtils.reAlpha(ColorUtils.getHealthColor(entity.health, entity.maxHealth), 255).rgb
        )

        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        RenderUtils.drawRoundedRect(
            x + 5.0F,
            y + 29.55F,
            x + length + hplength + 18F,
            y + 25F,
            Color(0, 0, 0, 110).rgb,
            2F
        )
        RenderUtils.drawRoundedGradientRectCorner(
            x + 5F,
            y + 25F,
            x + 5F + (easingHealth / entity.maxHealth) * (length + hplength + 13F),
            y + 29.5F,
            4F,
            barColor.rgb,
            barColor.rgb
        )
        GlStateManager.popMatrix()
        return Border(x, y, totalWidth, totalHeight)
    }

    private fun updateRavenB4Anim(targetHealth: Float) {
        easingHealth += ((targetHealth - easingHealth) / 2.0F.pow(10.0F - animSpeedRB4)) * deltaTime
    }

    private fun renderAnimatedHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)

        val width = 190f
        val height = 52f
        val padding = 8f
        val avatarSize = 36f

        easingHealth = lerp(easingHealth, entity.health, animSpeed * 1.5f)

        if (prevHealth < 0f) prevHealth = entity.health

        if (entity.health < prevHealth && !isHitAnimating) {
            isHitAnimating = true
            hitAnimTimer = 0f
            avatarTargetScale = 0.85f
        }

        if (isHitAnimating) {
            hitAnimTimer += deltaTime
            val progress = (hitAnimTimer / hitAnimDuration).coerceIn(0f, 1f)

            when {
                progress < 0.4f -> {
                    avatarTargetScale = 0.85f
                    val desiredTint = 1f
                    avatarTintAlpha = lerp(avatarTintAlpha, desiredTint, 0.25f)
                }
                progress < 0.75f -> {
                    avatarTargetScale = 1.12f
                    val desiredTint = 0.6f
                    avatarTintAlpha = lerp(avatarTintAlpha, desiredTint, 0.25f)
                }
                else -> {
                    avatarTargetScale = 1f
                    avatarTintAlpha = lerp(avatarTintAlpha, 0f, 0.25f)
                }
            }

            if (progress >= 1f) {
                isHitAnimating = false
                avatarTargetScale = 1f
                avatarTintAlpha = 0f
                hitAnimTimer = 0f
            }
        } else {
            avatarTargetScale = 1f
            avatarTintAlpha = lerp(avatarTintAlpha, 0f, 0.12f)
        }

        avatarScale = lerp(avatarScale, avatarTargetScale, 0.22f)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)
        GlStateManager.scale(slideIn, slideIn, 1f)

        RenderUtils.drawRoundedRect(4f, 4f, width - 4f, height - 4f, Color(245, 245, 245).rgb, 6f)

        val ax = padding
        val ay = (height - avatarSize) / 2f

        GlStateManager.pushMatrix()
        GlStateManager.translate(ax, ay, 0f)
        GlStateManager.translate(avatarSize / 2f, avatarSize / 2f, 0f)
        GlStateManager.scale(avatarScale, avatarScale, 1f)
        GlStateManager.translate(-avatarSize / 2f, -avatarSize / 2f, 0f)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            drawHead(it.locationSkin, 0, 0, avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
        }
        GlStateManager.popMatrix()

        if (avatarTintAlpha > 0.01f) {
            val tint = Color(255, 50, 50, (avatarTintAlpha * 140).toInt())
            RenderUtils.drawRoundedRect(ax, ay, ax + avatarSize, ay + avatarSize, tint.rgb, 6f)
        }

        val nameX = ax + avatarSize + 10f
        val barH = 10f
        val nameFontHeight = Fonts.font40.FONT_HEIGHT.toFloat()
        val contentBlockHeight = nameFontHeight + 4f + barH
        val contentBlockY = (height - contentBlockHeight) / 2f

        val nameY = contentBlockY
        drawFontString(entity.name, nameX, nameY, Color(30, 30, 30).rgb)

        val barX = nameX
        val barY = nameY + nameFontHeight + 4f
        val barW = width - barX - padding

        RenderUtils.drawRoundedRect(barX, barY, barX + barW, barY + barH, Color(0, 0, 0, 70).rgb, 6f)

        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0f, 1f)
        val fillW = barW * healthPercent
        val barColor = ColorUtils.getHealthColor(easingHealth, entity.maxHealth)

        RenderUtils.drawRoundedRect(barX, barY, barX + fillW, barY + barH, barColor.rgb, 6f)

        GlStateManager.popMatrix()

        prevHealth = entity.health
        return Border(x, y, x + width, y + height)
    }

    private fun projectEntity(entity: Entity): FloatArray? {
        try {
            val renderX = mc.renderManager.renderPosX
            val renderY = mc.renderManager.renderPosY
            val renderZ = mc.renderManager.renderPosZ

            val x = entity.posX - renderX
            val y = entity.posY + entity.height / 2f - renderY
            val z = entity.posZ - renderZ

            val viewport = org.lwjgl.BufferUtils.createIntBuffer(16)
            val modelView = org.lwjgl.BufferUtils.createFloatBuffer(16)
            val projection = org.lwjgl.BufferUtils.createFloatBuffer(16)
            val screenCoords = org.lwjgl.BufferUtils.createFloatBuffer(3)

            glGetFloat(GL_MODELVIEW_MATRIX, modelView)
            glGetFloat(GL_PROJECTION_MATRIX, projection)
            glGetInteger(GL_VIEWPORT, viewport)

            val result = org.lwjgl.util.glu.GLU.gluProject(x.toFloat(), y.toFloat(), z.toFloat(), modelView, projection, viewport, screenCoords)

            if (result) {
                val screenX = screenCoords.get(0)
                val screenY = org.lwjgl.opengl.Display.getHeight() - screenCoords.get(1)
                val screenZ = screenCoords.get(2)

                if (screenZ >= 0.0 && screenZ < 1.0) {
                    return floatArrayOf(screenX, screenY, screenZ)
                }
            }
            return null
        } catch (_: Exception) {
            return null
        }
    }

    private var pulseTime = 0L
    private var hurtAlpha = 0F
    
    private fun renderPulseHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val currentTime = System.currentTimeMillis()
        val pulseProgress = ((currentTime - pulseTime) % 1000L) / 1000F
        val pulseScale = 1F + 0.03F * sin(pulseProgress * Math.PI * 2).toFloat()
        val healthPercent = easingHealth / maxHealth
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 45F, Color(0, 0, 0, 140).rgb, 8F)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            GlStateManager.pushMatrix()
            GlStateManager.translate(avatarX + avatarSize / 2F, avatarY + avatarSize / 2F, 0F)
            GlStateManager.scale(pulseScale, pulseScale, 1F)
            GlStateManager.translate(-avatarSize / 2F, -avatarSize / 2F, 0F)
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(0F, 0F, avatarSize, avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, 0, 0, avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(0F, 0F, avatarSize, avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
            GlStateManager.popMatrix()
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX, 6F, Color.WHITE.rgb)
        Fonts.font35.drawString("Health: ${String.format("%.1f", health)}", textX, 20F, Color(200, 200, 200).rgb)

        val barWidth = tWidth - 8F
        RenderUtils.drawRoundedRect(4F, 34F, tWidth - 4F, 42F, Color(40, 40, 40).rgb, 4F)

        val pulseColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        val pulseAlpha = (200 + 55 * sin(pulseProgress * Math.PI * 2)).toInt().coerceIn(0, 255)
        val barColor = Color(pulseColor.red, pulseColor.green, pulseColor.blue, pulseAlpha)

        RenderUtils.customRounded(
            4F, 34F,
            4F + healthPercent * barWidth, 42F,
            0F, 4F, 4F, 0F,
            barColor.rgb
        )

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderLiquidHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val liquidTime = System.currentTimeMillis() % 3000L
        val liquidProgress = liquidTime / 3000F
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 45F, Color(0, 0, 0, 160).rgb, 8F)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX, 8F, Color.WHITE.rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} / ${String.format("%.1f", maxHealth)}", textX, 22F, Color(180, 180, 180).rgb)

        val barWidth = tWidth - 8F
        val barHeight = 6F
        val barY = 35F
        val healthPercent = easingHealth / maxHealth

        RenderUtils.drawRoundedRect(4F, barY, tWidth - 4F, barY + barHeight, Color(40, 40, 50).rgb, 3F)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val liquidColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        val fillWidth = healthPercent * barWidth

        glBegin(GL_TRIANGLE_STRIP)
        for (i in 0..30) {
            val progress = i.toFloat() / 30
            val xPos = 4F + progress * fillWidth
            val waveY = sin((progress * Math.PI * 3) + liquidProgress * Math.PI * 2).toFloat() * 1.5F
            
            glColor4f(liquidColor.red / 255F, liquidColor.green / 255F, liquidColor.blue / 255F, 0.9F)
            glVertex3f(xPos, barY + waveY, 0F)
            glVertex3f(xPos, barY + barHeight, 0F)
        }
        glEnd()

        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderCircleHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val circleTime = System.currentTimeMillis() % 3000L
        val circleProgress = circleTime / 3000F
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 45F, Color(20, 20, 30, 200).rgb, 10F)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            val centerX = avatarX + avatarSize / 2F
            val centerY = avatarY + avatarSize / 2F
            
            val healthPercent = easingHealth / maxHealth
            val segments = 50
            
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glLineWidth(3F)
            
            val healthColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
            glColor4f(healthColor.red / 255F, healthColor.green / 255F, healthColor.blue / 255F, 0.8F)
            glBegin(GL_LINE_STRIP)
            for (i in 0..segments) {
                val angle = (i.toFloat() / segments * Math.PI * 2).toFloat()
                val radius = avatarSize / 2F + 3F
                glVertex3f(centerX + radius * cos(angle), centerY + radius * sin(angle), 0F)
            }
            glEnd()
            
            glColor4f(healthColor.red / 255F, healthColor.green / 255F, healthColor.blue / 255F, 1F)
            glBegin(GL_LINE_STRIP)
            for (i in 0..(segments * healthPercent).toInt()) {
                val angle = (i.toFloat() / segments * Math.PI * 2).toFloat() - Math.PI.toFloat() / 2F
                val radius = avatarSize / 2F + 3F
                glVertex3f(centerX + radius * cos(angle), centerY + radius * sin(angle), 0F)
            }
            glEnd()
            
            glEnable(GL_TEXTURE_2D)
            glDisable(GL_BLEND)
            
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, avatarSize / 2F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, avatarSize / 2F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX, 8F, Color.WHITE.rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} HP", textX, 22F, Color(180, 180, 180).rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderRadarHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val radarTime = System.currentTimeMillis() % 2000L
        val radarProgress = radarTime / 2000F
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 45F, Color(10, 20, 10, 200).rgb, 8F)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            val centerX = avatarX + avatarSize / 2F
            val centerY = avatarY + avatarSize / 2F
            
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            
            for (r in 1..3) {
                val radius = avatarSize / 2F + r * 5F
                glColor4f(0F, 1F, 0F, 0.3F / r)
                glBegin(GL_LINE_LOOP)
                for (i in 0..50) {
                    val angle = (i.toFloat() / 50 * Math.PI * 2).toFloat()
                    glVertex3f(centerX + radius * cos(angle), centerY + radius * sin(angle), 0F)
                }
                glEnd()
            }
            
            glColor4f(0F, 1F, 0F, 0.8F)
            glLineWidth(2F)
            glBegin(GL_LINES)
            val sweepAngle = radarProgress * Math.PI * 2
            glVertex3f(centerX, centerY, 0F)
            glVertex3f(centerX + (avatarSize / 2F + 15F) * cos(sweepAngle).toFloat(), 
                       centerY + (avatarSize / 2F + 15F) * sin(sweepAngle).toFloat(), 0F)
            glEnd()
            glLineWidth(1F)
            
            glEnable(GL_TEXTURE_2D)
            glDisable(GL_BLEND)
            
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, avatarSize / 2F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, avatarSize / 2F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX, 8F, Color(0, 255, 0).rgb)
        Fonts.font35.drawString("TARGET DETECTED", textX, 22F, Color(0, 200, 0).rgb)

        val barWidth = tWidth - 8F
        val barY = 36F
        val healthPercent = easingHealth / maxHealth
        RenderUtils.drawRoundedRect(4F, barY, tWidth - 4F, barY + 4F, Color(0, 50, 0).rgb, 2F)
        RenderUtils.customRounded(4F, barY, 4F + healthPercent * barWidth, barY + 4F, 0F, 2F, 2F, 0F, Color(0, 255, 0).rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderDigitalHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val digitalTime = System.currentTimeMillis()
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 45F, Color(0, 0, 0, 200).rgb, 4F)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        val glitchOffset = if (digitalTime % 100 < 20) ((digitalTime % 10) - 5).toFloat() * 0.5F else 0F
        
        glColor4f(0F, 1F, 1F, 0.3F)
        glBegin(GL_LINES)
        for (i in 0..20) {
            val lineY = (i * 2.5F) + glitchOffset
            if (lineY < 45F) {
                glVertex3f(0F, lineY, 0F)
                glVertex3f(tWidth, lineY, 0F)
            }
        }
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 4F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 4F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX + glitchOffset, 8F, Color(0, 255, 255).rgb)
        
        val healthText = "HP: ${String.format("%.1f", health)}/${String.format("%.1f", maxHealth)}"
        Fonts.font35.drawString(healthText, textX, 22F, Color(0, 200, 200).rgb)

        val barWidth = tWidth - 8F
        val barY = 36F
        val healthPercent = easingHealth / maxHealth
        RenderUtils.drawRoundedRect(4F, barY, tWidth - 4F, barY + 4F, Color(0, 50, 50).rgb, 2F)
        
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        val segments = (healthPercent * 30).toInt()
        glColor4f(0F, 1F, 1F, 1F)
        glBegin(GL_QUADS)
        for (i in 0 until segments) {
            val segX = 4F + i * (barWidth / 30F)
            val segWidth = barWidth / 30F - 1F
            glVertex3f(segX, barY, 0F)
            glVertex3f(segX, barY + 4F, 0F)
            glVertex3f(segX + segWidth, barY + 4F, 0F)
            glVertex3f(segX + segWidth, barY, 0F)
        }
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderCrystalHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val crystalTime = System.currentTimeMillis() % 4000L
        val crystalProgress = crystalTime / 4000F
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        val healthPercent = easingHealth / maxHealth
        val crystalColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        
        for (i in 1..5) {
            val alpha = (50 - i * 8).coerceAtLeast(10)
            RenderUtils.drawRoundedRect(-i.toFloat(), -i.toFloat(), tWidth + i, 45F + i, 
                Color(crystalColor.red, crystalColor.green, crystalColor.blue, alpha).rgb, 8F + i)
        }
        
        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 45F, Color(255, 255, 255, 30).rgb, 8F)
        RenderUtils.drawRoundedRect(1F, 1F, tWidth - 1F, 44F, Color(255, 255, 255, 20).rgb, 7F)
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX, 8F, Color.WHITE.rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} / ${String.format("%.1f", maxHealth)}", textX, 22F, Color(200, 200, 200).rgb)

        val barWidth = tWidth - 8F
        val barY = 36F
        RenderUtils.drawRoundedRect(4F, barY, tWidth - 4F, barY + 4F, Color(200, 200, 200, 100).rgb, 2F)
        
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        val shimmer = sin(crystalProgress * Math.PI * 2).toFloat() * 0.3F + 0.7F
        glColor4f(crystalColor.red / 255F * shimmer, crystalColor.green / 255F * shimmer, crystalColor.blue / 255F * shimmer, 0.8F)
        glBegin(GL_QUADS)
        glVertex3f(4F, barY, 0F)
        glVertex3f(4F, barY + 4F, 0F)
        glVertex3f(4F + healthPercent * barWidth, barY + 4F, 0F)
        glVertex3f(4F + healthPercent * barWidth, barY, 0F)
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderMatrixHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val matrixTime = System.currentTimeMillis()
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 45F, Color(0, 10, 0, 220).rgb, 4F)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        val chars = "01"
        val columnWidth = 8F
        val columns = (tWidth / columnWidth).toInt()
        
        for (col in 0 until columns) {
            val seed = (matrixTime / 100L + col * 7).toInt()
            val dropLength = (seed % 5) + 3
            val dropStart = ((seed * 13) % 10) * 4.5F
            
            for (row in 0 until dropLength) {
                val charY = dropStart + row * 4.5F
                if (charY < 45F && charY >= 0F) {
                    val alpha = if (row == 0) 1F else (1F - row.toFloat() / dropLength) * 0.8F
                    glColor4f(0F, 1F, 0F, alpha)
                    
                    val charIndex = (seed + row) % 2
                    glBegin(GL_QUADS)
                    glVertex3f(col * columnWidth + 2F, charY, 0F)
                    glVertex3f(col * columnWidth + 2F, charY + 4F, 0F)
                    glVertex3f(col * columnWidth + 6F, charY + 4F, 0F)
                    glVertex3f(col * columnWidth + 6F, charY, 0F)
                    glEnd()
                }
            }
        }
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 4F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(0, 255, 0, (hurtAlpha * 150).toInt()).rgb, 4F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX, 8F, Color(0, 255, 0).rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} / ${String.format("%.1f", maxHealth)}", textX, 22F, Color(0, 200, 0).rgb)

        val barWidth = tWidth - 8F
        val barY = 36F
        val healthPercent = easingHealth / maxHealth
        RenderUtils.drawRoundedRect(4F, barY, tWidth - 4F, barY + 4F, Color(0, 30, 0).rgb, 2F)
        RenderUtils.customRounded(4F, barY, 4F + healthPercent * barWidth, barY + 4F, 0F, 2F, 2F, 0F, Color(0, 255, 0).rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderHologramHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val holoTime = System.currentTimeMillis()
        val flicker = if (holoTime % 150 < 10) 0.5F else 1F
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        val healthPercent = easingHealth / maxHealth
        val holoColor = Color(0, 255, 255, (180 * flicker).toInt())
        
        glColor4f(0F, 1F, 1F, 0.15F * flicker)
        glBegin(GL_QUADS)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 45F, 0F)
        glVertex3f(tWidth, 45F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glColor4f(0F, 1F, 1F, 0.4F * flicker)
        glBegin(GL_LINES)
        for (i in 0..15) {
            val scanY = ((holoTime / 50L + i * 3) % 45).toFloat()
            glVertex3f(0F, scanY, 0F)
            glVertex3f(tWidth, scanY, 0F)
        }
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            
            glColor4f(0F, 1F, 1F, 0.3F * flicker)
            RenderUtils.drawRoundedRect(avatarX - 2, avatarY - 2, avatarX + avatarSize + 2, avatarY + avatarSize + 2, Color(0, 255, 255, (50 * flicker).toInt()).rgb, 6F)
            
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            glColor4f(1F, 1F, 1F, flicker)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color(255, 255, 255, (255 * flicker).toInt()))
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX, 8F, Color(0, 255, 255, (255 * flicker).toInt()).rgb)
        Fonts.font35.drawString("HOLOGRAM SCAN", textX, 22F, Color(0, 200, 200, (200 * flicker).toInt()).rgb)

        val barWidth = tWidth - 8F
        val barY = 36F
        RenderUtils.drawRoundedRect(4F, barY, tWidth - 4F, barY + 4F, Color(0, 50, 50, (100 * flicker).toInt()).rgb, 2F)
        RenderUtils.customRounded(4F, barY, 4F + healthPercent * barWidth, barY + 4F, 0F, 2F, 2F, 0F, Color(0, 255, 255, (255 * flicker).toInt()).rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderPixelHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        val healthPercent = easingHealth / maxHealth
        val pixelSize = 4F
        
        glColor4f(0.1F, 0.1F, 0.15F, 0.9F)
        glBegin(GL_QUADS)
        for (px in 0..(tWidth / pixelSize).toInt()) {
            for (py in 0..(45F / pixelSize).toInt()) {
                glVertex3f(px * pixelSize, py * pixelSize, 0F)
                glVertex3f(px * pixelSize, (py + 1) * pixelSize, 0F)
                glVertex3f((px + 1) * pixelSize, (py + 1) * pixelSize, 0F)
                glVertex3f((px + 1) * pixelSize, py * pixelSize, 0F)
            }
        }
        glEnd()
        
        glColor4f(0.3F, 0.3F, 0.4F, 1F)
        glBegin(GL_LINE_LOOP)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 45F, 0F)
        glVertex3f(tWidth, 45F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            
            glColor4f(0.5F, 0.5F, 0.6F, 1F)
            glBegin(GL_LINE_LOOP)
            glVertex3f(avatarX - 2, avatarY - 2, 0F)
            glVertex3f(avatarX - 2, avatarY + avatarSize + 2, 0F)
            glVertex3f(avatarX + avatarSize + 2, avatarY + avatarSize + 2, 0F)
            glVertex3f(avatarX + avatarSize + 2, avatarY - 2, 0F)
            glEnd()
            
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 0F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 0F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX, 8F, Color.WHITE.rgb)
        Fonts.font35.drawString("HP: ${String.format("%.0f", health)}/${String.format("%.0f", maxHealth)}", textX, 22F, Color(180, 180, 180).rgb)

        val barWidth = tWidth - 8F
        val barY = 36F
        val healthPixels = (healthPercent * (barWidth / pixelSize)).toInt()
        
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        glColor4f(0.2F, 0.2F, 0.2F, 1F)
        glBegin(GL_QUADS)
        for (i in 0..(barWidth / pixelSize).toInt()) {
            val px = 4F + i * pixelSize
            glVertex3f(px, barY, 0F)
            glVertex3f(px, barY + pixelSize, 0F)
            glVertex3f(px + pixelSize - 1, barY + pixelSize, 0F)
            glVertex3f(px + pixelSize - 1, barY, 0F)
        }
        glEnd()
        
        val healthColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        glColor4f(healthColor.red / 255F, healthColor.green / 255F, healthColor.blue / 255F, 1F)
        glBegin(GL_QUADS)
        for (i in 0 until healthPixels) {
            val px = 4F + i * pixelSize
            glVertex3f(px, barY, 0F)
            glVertex3f(px, barY + pixelSize, 0F)
            glVertex3f(px + pixelSize - 1, barY + pixelSize, 0F)
            glVertex3f(px + pixelSize - 1, barY, 0F)
        }
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderNeonHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val neonTime = System.currentTimeMillis() % 2000L
        val neonProgress = neonTime / 2000F
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        val healthPercent = easingHealth / maxHealth
        val neonColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        val glowIntensity = (0.5F + 0.5F * sin(neonProgress * Math.PI * 2).toFloat())
        
        for (i in 5 downTo 1) {
            val alpha = (40 * glowIntensity / i).toInt()
            glColor4f(neonColor.red / 255F, neonColor.green / 255F, neonColor.blue / 255F, alpha / 255F)
            glBegin(GL_LINE_LOOP)
            glVertex3f(-i.toFloat(), -i.toFloat(), 0F)
            glVertex3f(-i.toFloat(), 45F + i, 0F)
            glVertex3f(tWidth + i, 45F + i, 0F)
            glVertex3f(tWidth + i, -i.toFloat(), 0F)
            glEnd()
        }
        
        glColor4f(0.05F, 0.05F, 0.1F, 0.9F)
        glBegin(GL_QUADS)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 45F, 0F)
        glVertex3f(tWidth, 45F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            
            for (i in 3 downTo 1) {
                val alpha = (60 * glowIntensity / i).toInt()
                glColor4f(neonColor.red / 255F, neonColor.green / 255F, neonColor.blue / 255F, alpha / 255F)
                glBegin(GL_LINE_LOOP)
                glVertex3f(avatarX - i, avatarY - i, 0F)
                glVertex3f(avatarX - i, avatarY + avatarSize + i, 0F)
                glVertex3f(avatarX + avatarSize + i, avatarY + avatarSize + i, 0F)
                glVertex3f(avatarX + avatarSize + i, avatarY - i, 0F)
                glEnd()
            }
            
            glEnable(GL_TEXTURE_2D)
            glDisable(GL_BLEND)
            
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX, 8F, neonColor.rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} HP", textX, 22F, Color(200, 200, 200).rgb)

        val barWidth = tWidth - 8F
        val barY = 36F
        
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        for (i in 3 downTo 1) {
            val alpha = (40 * glowIntensity / i).toInt()
            glColor4f(neonColor.red / 255F, neonColor.green / 255F, neonColor.blue / 255F, alpha / 255F)
            glBegin(GL_LINE_LOOP)
            glVertex3f(4F - i, barY - i, 0F)
            glVertex3f(4F - i, barY + 4F + i, 0F)
            glVertex3f(tWidth - 4F + i, barY + 4F + i, 0F)
            glVertex3f(tWidth - 4F + i, barY - i, 0F)
            glEnd()
        }
        
        glColor4f(0.2F, 0.2F, 0.2F, 1F)
        glBegin(GL_QUADS)
        glVertex3f(4F, barY, 0F)
        glVertex3f(4F, barY + 4F, 0F)
        glVertex3f(tWidth - 4F, barY + 4F, 0F)
        glVertex3f(tWidth - 4F, barY, 0F)
        glEnd()
        
        glColor4f(neonColor.red / 255F, neonColor.green / 255F, neonColor.blue / 255F, glowIntensity)
        glBegin(GL_QUADS)
        glVertex3f(4F, barY, 0F)
        glVertex3f(4F, barY + 4F, 0F)
        glVertex3f(4F + healthPercent * barWidth, barY + 4F, 0F)
        glVertex3f(4F + healthPercent * barWidth, barY, 0F)
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderGlitchHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)

        val glitchTime = System.currentTimeMillis()
        val glitchActive = glitchTime % 500 < 50
        val glitchOffsetX = if (glitchActive) ((glitchTime % 10) - 5).toFloat() else 0F
        val glitchOffsetY = if (glitchActive) ((glitchTime % 7) - 3).toFloat() else 0F
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        val healthPercent = easingHealth / maxHealth
        val healthColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        
        glColor4f(1F, 0F, 0F, 0.3F)
        glBegin(GL_QUADS)
        glVertex3f(glitchOffsetX - 2, glitchOffsetY, 0F)
        glVertex3f(glitchOffsetX - 2, 45F + glitchOffsetY, 0F)
        glVertex3f(tWidth + glitchOffsetX - 2, 45F + glitchOffsetY, 0F)
        glVertex3f(tWidth + glitchOffsetX - 2, glitchOffsetY, 0F)
        glEnd()
        
        glColor4f(0F, 1F, 1F, 0.3F)
        glBegin(GL_QUADS)
        glVertex3f(-glitchOffsetX + 2, -glitchOffsetY, 0F)
        glVertex3f(-glitchOffsetX + 2, 45F - glitchOffsetY, 0F)
        glVertex3f(tWidth - glitchOffsetX + 2, 45F - glitchOffsetY, 0F)
        glVertex3f(tWidth - glitchOffsetX + 2, -glitchOffsetY, 0F)
        glEnd()
        
        glColor4f(0.1F, 0.1F, 0.15F, 0.9F)
        glBegin(GL_QUADS)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 45F, 0F)
        glVertex3f(tWidth, 45F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glColor4f(1F, 1F, 1F, 0.1F)
        glBegin(GL_LINES)
        for (i in 0..10) {
            if (glitchTime % (i + 1) == 0L) {
                val lineY = (glitchTime / 10L + i * 5) % 45
                glVertex3f(0F, lineY.toFloat(), 0F)
                glVertex3f(tWidth, lineY.toFloat(), 0F)
            }
        }
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 4F
            val avatarY = 5F
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 10F
        Fonts.font40.drawString(name, textX + glitchOffsetX, 8F + glitchOffsetY, Color.WHITE.rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} HP", textX, 22F, healthColor.rgb)

        val barWidth = tWidth - 8F
        val barY = 36F
        RenderUtils.drawRoundedRect(4F, barY, tWidth - 4F, barY + 4F, Color(30, 30, 40).rgb, 2F)
        RenderUtils.customRounded(4F, barY, 4F + healthPercent * barWidth, barY + 4F, 0F, 2F, 2F, 0F, healthColor.rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderShadowHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        val healthPercent = easingHealth / maxHealth
        val healthColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        
        RenderUtils.drawRoundedRect(3F, 3F, tWidth + 3F, 48F, Color(0, 0, 0, 60).rgb, 6F)
        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 45F, Color(20, 20, 25, 240).rgb, 6F)
        
        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 2F, healthColor.rgb, 0F)

        if (playerInfo != null) {
            val avatarX = 5F
            val avatarY = 6F
            RenderUtils.drawRoundedRect(avatarX - 1, avatarY - 1, avatarX + avatarSize + 1, avatarY + avatarSize + 1, Color(0, 0, 0, 100).rgb, 7F)
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 15F
        Fonts.font40.drawString(name, textX, 8F, Color.WHITE.rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} HP", textX, 22F, healthColor.rgb)

        val barWidth = tWidth - 10F
        val barY = 36F
        RenderUtils.drawRoundedRect(5F, barY, tWidth - 5F, barY + 4F, Color(40, 40, 45).rgb, 2F)
        RenderUtils.customRounded(5F, barY, 5F + healthPercent * barWidth, barY + 4F, 0F, 2F, 2F, 0F, healthColor.rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth + 3F, y + 48F)
    }

    private fun renderAquaHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (50F + Fonts.font40.getStringWidth(name)).coerceAtLeast(130F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        val healthPercent = easingHealth / maxHealth
        val healthColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        glColor4f(0.1F, 0.3F, 0.4F, 0.15F)
        glBegin(GL_QUADS)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 50F, 0F)
        glVertex3f(tWidth, 50F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glColor4f(0.2F, 0.6F, 0.8F, 0.8F)
        glLineWidth(1.5F)
        glBegin(GL_LINE_LOOP)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 50F, 0F)
        glVertex3f(tWidth, 50F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 6F
            val avatarY = 7F
            
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glColor4f(0.2F, 0.5F, 0.7F, 0.3F)
            glBegin(GL_QUADS)
            glVertex3f(avatarX - 2, avatarY - 2, 0F)
            glVertex3f(avatarX - 2, avatarY + avatarSize + 2, 0F)
            glVertex3f(avatarX + avatarSize + 2, avatarY + avatarSize + 2, 0F)
            glVertex3f(avatarX + avatarSize + 2, avatarY - 2, 0F)
            glEnd()
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 100, 100, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 18F
        Fonts.font40.drawString(name, textX, 9F, Color(200, 230, 255).rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} / ${String.format("%.1f", maxHealth)}", textX, 24F, healthColor.rgb)

        val barWidth = tWidth - 12F
        val barY = 41F
        RenderUtils.drawRoundedRect(6F, barY, tWidth - 6F, barY + 4F, Color(20, 50, 70, 150).rgb, 2F)
        RenderUtils.customRounded(6F, barY, 6F + healthPercent * barWidth, barY + 4F, 0F, 2F, 2F, 0F, Color(80, 180, 220).rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 50F)
    }

    private fun renderGlassHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        val healthPercent = easingHealth / maxHealth
        val healthColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        glColor4f(1F, 1F, 1F, 0.08F)
        glBegin(GL_QUADS)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 45F, 0F)
        glVertex3f(tWidth, 45F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glColor4f(1F, 1F, 1F, 0.2F)
        glLineWidth(1F)
        glBegin(GL_LINE_LOOP)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 45F, 0F)
        glVertex3f(tWidth, 45F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glColor4f(1F, 1F, 1F, 0.15F)
        glBegin(GL_QUADS)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 15F, 0F)
        glVertex3f(tWidth, 15F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 5F
            val avatarY = 5F
            
            glColor4f(1F, 1F, 1F, 0.1F)
            RenderUtils.drawRoundedRect(avatarX - 1, avatarY - 1, avatarX + avatarSize + 1, avatarY + avatarSize + 1, Color(255, 255, 255, 30).rgb, 7F)
            
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 100, 100, (hurtAlpha * 100).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 12F
        Fonts.font40.drawString(name, textX, 7F, Color.WHITE.rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} HP", textX, 21F, healthColor.rgb)

        val barWidth = tWidth - 10F
        val barY = 36F
        RenderUtils.drawRoundedRect(5F, barY, tWidth - 5F, barY + 4F, Color(255, 255, 255, 40).rgb, 2F)
        RenderUtils.customRounded(5F, barY, 5F + healthPercent * barWidth, barY + 4F, 0F, 2F, 2F, 0F, Color(255, 255, 255, 180).rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderOutlineHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (45F + Fonts.font40.getStringWidth(name)).coerceAtLeast(120F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        val healthPercent = easingHealth / maxHealth
        val healthColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        glColor4f(healthColor.red / 255F * 0.15F, healthColor.green / 255F * 0.15F, healthColor.blue / 255F * 0.15F, 0.9F)
        glBegin(GL_QUADS)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 45F, 0F)
        glVertex3f(tWidth, 45F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glColor4f(healthColor.red / 255F, healthColor.green / 255F, healthColor.blue / 255F, 1F)
        glLineWidth(2F)
        glBegin(GL_LINE_LOOP)
        glVertex3f(0F, 0F, 0F)
        glVertex3f(0F, 45F, 0F)
        glVertex3f(tWidth, 45F, 0F)
        glVertex3f(tWidth, 0F, 0F)
        glEnd()
        
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        if (playerInfo != null) {
            val avatarX = 5F
            val avatarY = 5F
            
            glColor4f(healthColor.red / 255F, healthColor.green / 255F, healthColor.blue / 255F, 0.5F)
            RenderUtils.drawRoundedRect(avatarX - 1, avatarY - 1, avatarX + avatarSize + 1, avatarY + avatarSize + 1, healthColor.rgb, 7F)
            
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 12F
        Fonts.font40.drawString(name, textX, 7F, Color.WHITE.rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} HP", textX, 21F, healthColor.rgb)

        val barWidth = tWidth - 10F
        val barY = 36F
        RenderUtils.drawRoundedRect(5F, barY, tWidth - 5F, barY + 4F, Color(30, 30, 35).rgb, 2F)
        RenderUtils.customRounded(5F, barY, 5F + healthPercent * barWidth, barY + 4F, 0F, 2F, 2F, 0F, healthColor.rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 45F)
    }

    private fun renderElegantHUD(x: Float, y: Float): Border {
        val entity = target ?: lastTarget ?: return Border(0f, 0f, 0f, 0f)
        updateAnim(entity.health)

        val name = entity.name
        val health = entity.health
        val maxHealth = entity.maxHealth
        val avatarSize = newStyleAvatarSize.toFloat()
        val tWidth = (50F + Fonts.font40.getStringWidth(name)).coerceAtLeast(130F)
        val playerInfo = mc.netHandler.getPlayerInfo(entity.uniqueID)
        
        if (entity.hurtTime > 0) {
            hurtAlpha = 1F
        } else {
            hurtAlpha = lerp(hurtAlpha, 0F, 0.1F)
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        val healthPercent = easingHealth / maxHealth
        val healthColor = ColorUtils.getHealthColor(easingHealth, maxHealth)
        
        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 50F, Color(15, 15, 20, 230).rgb, 8F)
        
        RenderUtils.drawRoundedRect(0F, 0F, tWidth, 3F, healthColor.rgb, 0F)

        if (playerInfo != null) {
            val avatarX = 8F
            val avatarY = 8F
            
            RenderUtils.drawRoundedRect(avatarX - 2, avatarY - 2, avatarX + avatarSize + 2, avatarY + avatarSize + 2, Color(40, 40, 50).rgb, 8F)
            
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.fastRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 6F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawHead(playerInfo.locationSkin, avatarX.toInt(), avatarY.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            Stencil.dispose()
            
            if (hurtAlpha > 0.01F) {
                RenderUtils.drawRoundedRect(avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, Color(255, 0, 0, (hurtAlpha * 150).toInt()).rgb, 6F)
            }
        }

        GlStateManager.resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val textX = avatarSize + 20F
        Fonts.font40.drawString(name, textX, 10F, Color.WHITE.rgb)
        Fonts.font35.drawString("${String.format("%.1f", health)} / ${String.format("%.1f", maxHealth)}", textX, 25F, healthColor.rgb)

        val barWidth = tWidth - 16F
        val barY = 41F
        RenderUtils.drawRoundedRect(8F, barY, tWidth - 8F, barY + 4F, Color(35, 35, 45).rgb, 2F)
        RenderUtils.customRounded(8F, barY, 8F + healthPercent * barWidth, barY + 4F, 0F, 2F, 2F, 0F, healthColor.rgb)

        GlStateManager.popMatrix()
        return Border(x, y, x + tWidth, y + 50F)
    }
}
