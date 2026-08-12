/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_AUTHOR
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.clientCommit
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura.blockStatus
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.client.PPSCounter
import net.ccbluex.liquidbounce.utils.client.ServerUtils
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.movement.BPSUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.movement.TimerBalanceUtils
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.ColorSettingsFloat
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.utils.render.toColorArray
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemSword
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import kotlin.math.max

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "Text")
class Text(x: Double = 10.0, y: Double = 10.0, scale: Float = 1F, side: Side = Side.default()) : Element(
    "Text",
    x,
    y,
    scale,
    side
) {

    companion object {

        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
        val HOUR_FORMAT = SimpleDateFormat("HH:mm")

        val DECIMAL_FORMAT = DecimalFormat("0.00")

        /**
         * Default Client Title
         */
        fun defaultClientTitle(): Text {
            val text = Text(x = 2.0, y = 1.0, scale = 2F)

            text.displayString = "%clientName%"
            text.shadow = true
            text.color = text.blueRibbon
            text.font.set(Fonts.fontRegular45)

            return text
        }

        /**
         * Default Client Version
         */
        fun defaultClientVersion(): Text {
            val text = Text(x = 107.0, y = 25.0, scale = 1F)

            text.displayString = "%clientversion%"
            text.shadow = true
            text.color = Color.WHITE
            text.font.set(Fonts.fontExtraBold35)

            return text
        }

        /**
         * Default Block Counter
         */
        fun defaultBlockCount(): Text {
            val text = Text(x = 520.0, y = 245.0, scale = 1F)

            text.displayString = "%blockamount%"
            text.shadow = true
            text.bgColors.with(Color.BLACK.withAlpha(128))
            text.onScaffold = true
            text.showBlock = true
            text.backgroundScale = 1F

            return text
        }

    }

    private var onScaffold by boolean("ScaffoldOnly", false)
    private var showBlock by boolean("ShowBlock", false)

    private var displayString by text("DisplayText", "")

    private val textColorMode by choices("Text-ColorMode", arrayOf("Custom", "Rainbow", "Gradient", "Theme", "Sky", "Mixer"), "Theme")
    private val themeGradientMode by choices("Theme-GradientMode", arrayOf("Sync", "LeftToRight", "RightToLeft"), "RightToLeft") { textColorMode == "Theme" || backgroundMode == "Theme" }

    private val colors = ColorSettingsInteger(this, "TextColor", applyMax = true) { textColorMode == "Custom" }

    private val skySaturation by float("Sky-Saturation", 0.9f, 0f..1f) { textColorMode == "Sky" || backgroundMode == "Sky" }
    private val skyBrightness by float("Sky-Brightness", 1f, 0f..1f) { textColorMode == "Sky" || backgroundMode == "Sky" }
    private val skySpeed by float("Sky-Speed", 1f, 0.1f..5f) { textColorMode == "Sky" || backgroundMode == "Sky" }
    private val mixerSeconds by int("Mixer-Seconds", 2, 1..10) { textColorMode == "Mixer" || backgroundMode == "Mixer" }

    private val gradientTextSpeed by float("Text-Gradient-Speed", 1f, 0.5f..10f) { textColorMode == "Gradient" }

    private val maxTextGradientColors by int("Max-Text-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS)
    { textColorMode == "Gradient" }
    private val textGradColors = ColorSettingsFloat.create(this, "Text-Gradient")
    { textColorMode == "Gradient" && it <= maxTextGradientColors }

    private val roundedBackgroundRadius by float("RoundedBackGround-Radius", 3F, 0F..5F)

    private var backgroundScale by float("Background-Scale", 1F, 1F..3F)

    private val backgroundMode by choices("Background-ColorMode", arrayOf("Custom", "Rainbow", "Gradient", "Theme", "Sky", "Mixer"), "Custom")

    private val bgColors = ColorSettingsInteger(this, "BackgroundColor")
    { backgroundMode == "Custom" }.with(a = 0)

    private val gradientBackgroundSpeed by float("Background-Gradient-Speed", 1f, 0.5f..10f)
    { backgroundMode == "Gradient" }

    private val maxBackgroundGradientColors by int("Max-Background-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS)
    { backgroundMode == "Gradient" }
    private val bgGradColors = ColorSettingsFloat.create(this, "Background-Gradient")
    { backgroundMode == "Gradient" && it <= maxBackgroundGradientColors }

    private val backgroundBorder by float("BackgroundBorder-Width", 0.5F, 0.5F..5F)

    private val bgBorderColors = ColorSettingsInteger(this, "BackgroundBorderColor").with(a = 0)

    private fun isColorModeUsed(value: String) = textColorMode == value || backgroundMode == value

    private val rainbowX by float("Rainbow-X", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val rainbowY by float("Rainbow-Y", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val gradientX by float("Gradient-X", -500F, -2000F..2000F) { isColorModeUsed("Gradient") }
    private val gradientY by float("Gradient-Y", -1500F, -2000F..2000F) { isColorModeUsed("Gradient") }

    private var shadow by boolean("Shadow", true)
    private val font = font("Font", Fonts.fontSemibold40)

    private val blur by boolean("Blur", false)
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
    private val textGlowStrength by float("TextGlowStrength", 1.0F, 0.1F..1.0F) { textGlow }
    private val textGlowColor by color("TextGlowColor", Color(0, 150, 255)) { textGlow }
    
    private val backgroundGlow by boolean("BackgroundGlow", false)
    private val backgroundGlowStrength by float("BackgroundGlowStrength", 0.5F, 0.1F..1.0F) { backgroundGlow }
    private val backgroundGlowColor by color("BackgroundGlowColor", Color(0, 150, 255)) { backgroundGlow }
    
    private val outlineGlow by boolean("OutlineGlow", false)
    private val outlineGlowColor by color("OutlineGlowColor", Color(0, 150, 255)) { outlineGlow }
    private val outlineGlowExpand by float("OutlineGlowExpand", 5F, 1F..20F) { outlineGlow }
    private val outlineGlowAlpha by int("OutlineGlowAlpha", 60, 10..255) { outlineGlow }
    private val outlineGlowLayers by int("OutlineGlowLayers", 2, 1..5) { outlineGlow }
    private val outlineGlowRadius by float("OutlineGlowRadius", 5F, 0F..20F) { outlineGlow }

    private val enableAnimation by boolean("EnableAnimation", true)
    private val animationType by choices("AnimationType", arrayOf("Fade", "Scale", "Slide", "Bounce", "Elastic", "Zoom"), "Bounce") { enableAnimation }
    private val animationSpeed by float("AnimationSpeed", 0.15F, 0.01F..0.5F) { enableAnimation }
    private val bounceTension by float("BounceTension", 0.08f, 0.01f..0.5f) { enableAnimation && animationType == "Bounce" }
    private val bounceFriction by float("BounceFriction", 0.2f, 0.01f..0.5f) { enableAnimation && animationType == "Bounce" }

    private var animAlpha = 1F
    private var animScale = 1F
    private var animSlideX = 0F
    private var animSlideY = 0F
    
    private var velAlpha = 0f
    private var velScale = 0f
    private var velSlideX = 0f
    private var velSlideY = 0f

    private var lastShouldRender = false

    private fun spring(current: Float, target: Float, velocity: Float, tension: Float = bounceTension, friction: Float = bounceFriction): Pair<Float, Float> {
        val displacement = target - current
        val force = displacement * tension
        val drag = velocity * friction
        val acceleration = force - drag
        val newVelocity = velocity + acceleration
        val newPosition = current + newVelocity
        return newPosition to newVelocity
    }

    private var editMode = false
    private var editTicks = 0
    private var prevClick = 0L

    private var displayText = display

    private val display: String
        get() {
            val textContent = if (displayString.isEmpty() && !editMode)
                "Text Element"
            else
                displayString

            return multiReplace(textContent)
        }

    private var color: Color
        get() = colors.color()
        set(value) {
            colors.with(value)
        }

    private fun getReplacement(str: String): Any? {
        val thePlayer = mc.thePlayer

        if (thePlayer != null) {
            when (str.lowercase()) {
                "x" -> return DECIMAL_FORMAT.format(thePlayer.posX)
                "y" -> return DECIMAL_FORMAT.format(thePlayer.posY)
                "z" -> return DECIMAL_FORMAT.format(thePlayer.posZ)
                "xdp" -> return thePlayer.posX
                "ydp" -> return thePlayer.posY
                "zdp" -> return thePlayer.posZ
                "velocity" -> return DECIMAL_FORMAT.format(speed)
                "ping" -> return thePlayer.getPing()
                "health" -> return DECIMAL_FORMAT.format(thePlayer.health)
                "maxhealth" -> return DECIMAL_FORMAT.format(thePlayer.maxHealth)
                "yaw" -> return DECIMAL_FORMAT.format(thePlayer.rotationYaw)
                "pitch" -> return DECIMAL_FORMAT.format(thePlayer.rotationPitch)
                "yawint" -> return DECIMAL_FORMAT.format(thePlayer.rotationYaw).toInt()
                "pitchint" -> return DECIMAL_FORMAT.format(thePlayer.rotationPitch).toInt()
                "food" -> return thePlayer.foodStats.foodLevel
                "onground" -> return thePlayer.onGround
                "tbalance", "timerbalance" -> return "${TimerBalanceUtils.balance}ms"
                "block", "blocking" -> return (thePlayer.heldItem?.item is ItemSword && (blockStatus || thePlayer.isUsingItem || thePlayer.isBlocking))
                "sneak", "sneaking" -> return (thePlayer.isSneaking || mc.gameSettings.keyBindSneak.isKeyDown)
                "sprint", "sprinting" -> return (thePlayer.serverSprintState || thePlayer.isSprinting || mc.gameSettings.keyBindSprint.isKeyDown)
                "inventory", "inv" -> return mc.currentScreen is GuiInventory || mc.currentScreen is GuiContainer
                "serverslot" -> return SilentHotbar.currentSlot
                "clientslot" -> return thePlayer.inventory?.currentItem
                "bps", "blockpersecond" -> return DECIMAL_FORMAT.format(BPSUtils.getBPS())
                "blockamount", "blockcount" -> return InventoryUtils.blocksAmount()
            }
        }

        return when (str.lowercase()) {
            "username" -> mc.session.username
            "clientname" -> CLIENT_NAME
            "clientversion" -> clientVersionText
            "clientcommit" -> clientCommit
            "clientauthor", "clientcreator" -> CLIENT_AUTHOR
            "fps" -> Minecraft.getDebugFPS()
            "date" -> DATE_FORMAT.format(System.currentTimeMillis())
            "time" -> HOUR_FORMAT.format(System.currentTimeMillis())
            "serverip" -> ServerUtils.remoteIp
            "cps", "lcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.LEFT)
            "mcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.MIDDLE)
            "rcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT)
            "pps_sent" -> return PPSCounter.getPPS(PPSCounter.PacketType.SEND)
            "pps_received" -> return PPSCounter.getPPS(PPSCounter.PacketType.RECEIVED)
            else -> null // Null = don't replace
        }
    }

    private fun multiReplace(str: String): String {
        var lastPercent = -1
        val result = StringBuilder()
        for (i in str.indices) {
            if (str[i] == '%') {
                if (lastPercent != -1) {
                    if (lastPercent + 1 != i) {
                        val replacement = getReplacement(str.substring(lastPercent + 1, i))

                        if (replacement != null) {
                            result.append(replacement)
                            lastPercent = -1
                            continue
                        }
                    }
                    result.append(str, lastPercent, i)
                }
                lastPercent = i
            } else if (lastPercent == -1) {
                result.append(str[i])
            }
        }

        if (lastPercent != -1) {
            result.append(str, lastPercent, str.length)
        }

        return result.toString()
    }

    /**
     * Draw element
     */
    @Suppress("UnclearPrecedenceOfBinaryExpression")
    override fun drawElement(): Border {
        val stack = mc.thePlayer?.inventory?.getStackInSlot(SilentHotbar.currentSlot)
        val shouldRender = showBlock && stack?.item is ItemBlock
        
        val isCurrentlyRendering = (Scaffold.handleEvents() && onScaffold) || !onScaffold || mc.currentScreen is GuiHudDesigner
        
        if (enableAnimation) {
            val targetAlpha = if (isCurrentlyRendering) 1F else 0F
            val targetScale = if (isCurrentlyRendering) 1F else 0F
            
            when (animationType) {
                "Fade" -> {
                    animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animationSpeed.toDouble()).toFloat()
                    animScale = 1F
                }
                "Scale" -> {
                    animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animScale.toDouble(), targetScale.toDouble(), animationSpeed.toDouble()).toFloat()
                    animAlpha = 1F
                }
                "Slide" -> {
                    animSlideX = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animSlideX.toDouble(), (if (isCurrentlyRendering) 0F else -50F).toDouble(), animationSpeed.toDouble()).toFloat()
                    animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animationSpeed.toDouble()).toFloat()
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
                "Elastic" -> {
                    val progress = if (isCurrentlyRendering) {
                        net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.easeOutElasticX(animAlpha.toDouble())
                    } else {
                        1.0 - net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.easeOutElasticX((1.0 - animAlpha))
                    }
                    animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animationSpeed.toDouble()).toFloat()
                    animScale = progress.toFloat().coerceIn(0F, 1.5F)
                }
                "Zoom" -> {
                    animScale = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animScale.toDouble(), targetScale.toDouble(), animationSpeed.toDouble() * 1.5).toFloat()
                    animAlpha = net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil.base(animAlpha.toDouble(), targetAlpha.toDouble(), animationSpeed.toDouble()).toFloat()
                }
            }
            
            if (!isCurrentlyRendering && animAlpha < 0.01f && animScale < 0.01f) {
                return Border(0F, 0F, 0F, 0F)
            }
        } else {
            animAlpha = 1F
            animScale = 1F
            animSlideX = 0F
        }
        
        if (!enableAnimation && !isCurrentlyRendering) {
            return Border(0F, 0F, 0F, 0F)
        }
        
        lastShouldRender = isCurrentlyRendering
        
        val blockScale = if (shouldRender) 2.5F else 1F
        val fontRenderer = font.get()
        val fontHeight = ((fontRenderer as? GameFontRenderer)?.height ?: fontRenderer.FONT_HEIGHT) + 2
        val underscore = if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40) "_" else ""

        // Calculate width only once
        val underscoreWidth = fontRenderer.getStringWidth(underscore).toFloat()
        val width = fontRenderer.getStringWidth(displayText) + underscoreWidth
        val heightPadding = if (fontRenderer == mc.fontRendererObj) 1F else 0F

        val bgScale = max(backgroundScale, 1F)
        val horizontalPadding = (if (shouldRender) 16F else 2F) + blockScale
        val verticalPadding = (if (shouldRender) 3F else 2F + heightPadding) + (blockScale - 1F)

        val scaledWidth = width + (horizontalPadding * bgScale)
        val scaledHeight = fontHeight + (verticalPadding * bgScale) - 1F

        val rectPos = floatArrayOf(
            -horizontalPadding * bgScale,
            -verticalPadding * bgScale,
            scaledWidth - if (shouldRender) 16F else 0F,
            scaledHeight
        )

        assumeNonVolatile {
            val shouldDraw = isCurrentlyRendering || (enableAnimation && (animAlpha > 0.01f || animScale > 0.01f))
            if (shouldDraw) {
                glPushMatrix()
                
                if (enableAnimation) {
                    val centerX = (rectPos[0] + rectPos[2]) / 2F
                    val centerY = (rectPos[1] + rectPos[3]) / 2F
                    
                    glTranslatef(centerX, centerY, 0F)
                    glScalef(animScale, animScale, animScale)
                    glTranslatef(-centerX, -centerY, 0F)
                    
                    glTranslatef(animSlideX, 0F, 0F)
                    
                    if (animationType == "Fade" || animationType == "Slide" || animationType == "Zoom") {
                        glEnable(GL_BLEND)
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                        glColor4f(1f, 1f, 1f, animAlpha)
                    }
                }
                
                val rainbow = textColorMode == "Rainbow"
                val gradient = textColorMode == "Gradient"

                val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
                val actualGradientX = if (gradientX == 0f) 0f else 1f / gradientX
                val actualGradientY = if (gradientY == 0f) 0f else 1f / gradientY

                val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
                val actualRainbowX = if (rainbowX == 0f) 0f else 1f / rainbowX
                val actualRainbowY = if (rainbowY == 0f) 0f else 1f / rainbowY

                if (backgroundGlow) {
                    GlowUtils.drawGlow(
                        rectPos[0], rectPos[1], 
                        rectPos[2] - rectPos[0], rectPos[3] - rectPos[1],
                        (backgroundGlowStrength * 15F).toInt(), backgroundGlowColor
                    )
                }

                if (enableNeon) {
                    glPushMatrix()
                    glTranslated(-renderX, -renderY, 0.0)
                    glScalef(1F / scale, 1F / scale, 1F)
                    RenderUtils.drawNeonBorder(
                        renderX.toFloat() + rectPos[0], renderY.toFloat() + rectPos[1],
                        rectPos[2] - rectPos[0], rectPos[3] - rectPos[1],
                        roundedBackgroundRadius, neonColor, blurStrength,
                        neonOuterGlowAlpha, neonInnerBorderAlpha, neonBackgroundAlpha
                    )
                    glPopMatrix()
                } else if (enableGlass) {
                    glPushMatrix()
                    glTranslated(-renderX, -renderY, 0.0)
                    glScalef(1F / scale, 1F / scale, 1F)
                    RenderUtils.drawGlassmorphism(
                        renderX.toFloat() + rectPos[0], renderY.toFloat() + rectPos[1],
                        rectPos[2] - rectPos[0], rectPos[3] - rectPos[1],
                        roundedBackgroundRadius, blurStrength,
                        glassBaseColor, glassHighlightAlpha, glassBorderColor
                    )
                    glPopMatrix()
                } else if (blur) {
                    glPushMatrix()
                    glTranslated(-renderX, -renderY, 0.0)
                    glScalef(1F / scale, 1F / scale, 1F)
                    BlurUtils.blurAreaRounded(
                        renderX.toFloat() + rectPos[0], renderY.toFloat() + rectPos[1],
                        renderX.toFloat() + rectPos[2], renderY.toFloat() + rectPos[3],
                        roundedBackgroundRadius, blurStrength
                    )
                    glPopMatrix()
                }

                val bgThemeGradient = backgroundMode == "Theme" && themeGradientMode != "Sync"
                val bgThemeGradientColors = if (bgThemeGradient) {
                    val startColor = net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.setColor("start", 255)
                    val endColor = net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.setColor("end", 255)
                    if (themeGradientMode == "LeftToRight") {
                        listOf(
                            floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f),
                            floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f)
                        )
                    } else {
                        listOf(
                            floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f),
                            floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f)
                        )
                    }
                } else {
                    bgGradColors.toColorArray(maxBackgroundGradientColors)
                }
                
                val actualBgGradientSpeed = if (bgThemeGradient) net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.ThemeFadeSpeed / 5f else gradientBackgroundSpeed

                GradientShader.begin(
                    backgroundMode == "Gradient" || bgThemeGradient,
                    actualGradientX,
                    actualGradientY,
                    bgThemeGradientColors,
                    actualBgGradientSpeed,
                    gradientOffset
                ).use {
                    RainbowShader.begin(backgroundMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset).use {
                        drawRoundedRect(
                            rectPos[0], rectPos[1], rectPos[2], rectPos[3],
                            when (backgroundMode) {
                                "Gradient" -> 0
                                "Rainbow" -> 0
                                "Theme" -> if (themeGradientMode == "Sync") net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor().rgb else 0
                                "Sky" -> ColorUtils.skyRainbow(0, skySaturation, skyBrightness, skySpeed).rgb
                                "Mixer" -> getMixedColor(0, mixerSeconds).rgb
                                else -> bgColors.color().rgb
                            },
                            roundedBackgroundRadius
                        )
                    }
                }

                if (outlineGlow) {
                    GlowUtils.drawOutlineGlow(
                        rectPos[0], rectPos[1],
                        rectPos[2] - rectPos[0], rectPos[3] - rectPos[1],
                        outlineGlowColor, outlineGlowExpand, outlineGlowAlpha,
                        outlineGlowLayers, outlineGlowRadius
                    )
                }

                if (bgBorderColors.color().alpha > 0) {
                    drawRoundedBorder(
                        rectPos[0],
                        rectPos[1],
                        rectPos[2],
                        rectPos[3],
                        backgroundBorder,
                        bgBorderColors.color().rgb,
                        roundedBackgroundRadius
                    )
                }

                if (showBlock) {
                    glPushMatrix()

                    enableGUIStandardItemLighting()

                    // Prevent overlapping while editing
                    if (mc.currentScreen is GuiHudDesigner) glDisable(GL_DEPTH_TEST)

                    if (shouldRender) {
                        mc.renderItem.renderItemAndEffectIntoGUI(stack, -18, -3)
                    }

                    disableStandardItemLighting()
                    enableAlpha()
                    disableBlend()
                    disableLighting()

                    if (mc.currentScreen is GuiHudDesigner) glEnable(GL_DEPTH_TEST)

                    glPopMatrix()
                }

                val colorToUse = when {
                    rainbow || gradient -> 0
                    textColorMode == "Theme" && themeGradientMode == "Sync" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor().rgb
                    textColorMode == "Theme" -> 0
                    textColorMode == "Sky" -> ColorUtils.skyRainbow(0, skySaturation, skyBrightness, skySpeed).rgb
                    textColorMode == "Mixer" -> getMixedColor(0, mixerSeconds).rgb
                    else -> color.rgb
                }

                val themeGradient = textColorMode == "Theme" && themeGradientMode != "Sync"
                val themeGradientColors = if (themeGradient) {
                    val startColor = net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.setColor("start", 255)
                    val endColor = net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.setColor("end", 255)
                    if (themeGradientMode == "LeftToRight") {
                        listOf(
                            floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f),
                            floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f)
                        )
                    } else {
                        listOf(
                            floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f),
                            floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f)
                        )
                    }
                } else {
                    textGradColors.toColorArray(maxTextGradientColors)
                }
                
                val actualTextGradientSpeed = if (themeGradient) net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.ThemeFadeSpeed / 5f else gradientTextSpeed

                if (textGlow) {
                    GlowUtils.drawGlow(
                        0F, 2 - heightPadding, 
                        fontRenderer.getStringWidth(displayText).toFloat(), 
                        (fontRenderer as? GameFontRenderer)?.height?.toFloat() ?: fontRenderer.FONT_HEIGHT.toFloat(),
                        (textGlowStrength * 10F).toInt(), textGlowColor
                    )
                }

                GradientFontShader.begin(
                    gradient || themeGradient,
                    actualGradientX,
                    actualGradientY,
                    themeGradientColors,
                    actualTextGradientSpeed,
                    gradientOffset
                ).use {
                    RainbowFontShader.begin(rainbow, actualRainbowX, actualRainbowY, rainbowOffset).use {
                        fontRenderer.drawString(displayText, 0F, 2 - heightPadding, colorToUse, shadow)

                        if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40) {
                            fontRenderer.drawString("_", width - underscoreWidth, 0F, colorToUse, shadow)
                        }
                    }
                }
                
                if (enableAnimation && (animationType == "Fade" || animationType == "Slide" || animationType == "Zoom")) {
                    glColor4f(1f, 1f, 1f, 1f)
                    glDisable(GL_BLEND)
                }
                
                glPopMatrix()
            }

            if (editMode && mc.currentScreen !is GuiHudDesigner) {
                editMode = false
                updateElement()
            }
        }

        return Border(rectPos[0], rectPos[1], rectPos[2], rectPos[3])
    }

    override fun updateElement() {
        editTicks += 5
        if (editTicks > 80) editTicks = 0

        displayText = if (editMode) displayString else display
    }

    override fun handleMouseClick(x: Double, y: Double, mouseButton: Int) {
        if (isInBorder(x, y) && mouseButton == 0) {
            if (System.currentTimeMillis() - prevClick <= 250L)
                editMode = true

            prevClick = System.currentTimeMillis()
        } else {
            editMode = false
        }
    }

    override fun handleKey(c: Char, keyCode: Int) {
        if (editMode && mc.currentScreen is GuiHudDesigner) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (displayString.isNotEmpty())
                    displayString = displayString.dropLast(1)

                updateElement()
                return
            }

            if (ColorUtils.isAllowedCharacter(c) || c == '§')
                displayString += c

            updateElement()
        }
    }
}
