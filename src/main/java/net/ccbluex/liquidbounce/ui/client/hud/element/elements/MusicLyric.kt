package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.music.MusicPlayer
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.ColorSettingsFloat
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorder
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.utils.render.toColorArray
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor
import org.lwjgl.opengl.GL11.*
import java.awt.Color

@ElementInfo(name = "MusicLyric")
class MusicLyric(x: Double = 10.0, y: Double = 100.0, scale: Float = 1F, side: Side = Side.default()) : Element(
    "MusicLyric",
    x,
    y,
    scale,
    side
) {

    private val enableBounce by boolean("Bounce", true)
    private val animTension by float("BounceTension", 0.01f, 0.01f..1.0f) { enableBounce }
    private val animFriction by float("BounceFriction", 0.12f, 0.01f..1.0f) { enableBounce }

    private val textPositionMode by choices("Text-Position", arrayOf("Left", "Center"), "Center")

    private val textColorMode by choices("Text-ColorMode", arrayOf("Custom", "Rainbow", "Gradient", "Theme", "Sky", "Mixer"), "Gradient")
    private val themeGradientMode by choices("Theme-GradientMode", arrayOf("Sync", "LeftToRight", "RightToLeft"), "Sync") { textColorMode == "Theme" || backgroundMode == "Theme" }

    private val colors = ColorSettingsInteger(this, "TextColor", applyMax = true) { textColorMode == "Custom" }
        .with(r = 255, g = 255, b = 255, a = 255)

    private val skySaturation by float("Sky-Saturation", 0.9f, 0f..1f) { textColorMode == "Sky" || backgroundMode == "Sky" }
    private val skyBrightness by float("Sky-Brightness", 1f, 0f..1f) { textColorMode == "Sky" || backgroundMode == "Sky" }
    private val skySpeed by float("Sky-Speed", 1f, 0.1f..5f) { textColorMode == "Sky" || backgroundMode == "Sky" }
    private val mixerSeconds by int("Mixer-Seconds", 2, 1..10) { textColorMode == "Mixer" || backgroundMode == "Mixer" }

    private val gradientTextSpeed by float("Text-Gradient-Speed", 1f, 0.5f..10f) { textColorMode == "Gradient" }

    private val maxTextGradientColors by int("Max-Text-Gradient-Colors", 4, 1..10)
    { textColorMode == "Gradient" }
    private val textGradColors = ColorSettingsFloat.create(this, "Text-Gradient")
    { index -> textColorMode == "Gradient" && index <= maxTextGradientColors }

    private val roundedBackgroundRadius by float("RoundedBackGround-Radius", 5F, 0F..10F)

    private var backgroundScale by float("Background-Scale", 1.2F, 1F..3F)

    private val backgroundMode by choices("Background-ColorMode", arrayOf("Custom", "Rainbow", "Gradient", "Theme", "Sky", "Mixer"), "Custom")

    private val bgColors = ColorSettingsInteger(this, "BackgroundColor")
    { backgroundMode == "Custom" }.with(r = 0, g = 0, b = 0, a = 150)

    private val gradientBackgroundSpeed by float("Background-Gradient-Speed", 1f, 0.5f..10f)
    { backgroundMode == "Gradient" }

    private val maxBackgroundGradientColors by int("Max-Background-Gradient-Colors", 4, 1..10)
    { backgroundMode == "Gradient" }
    private val bgGradColors = ColorSettingsFloat.create(this, "Background-Gradient")
    { index -> backgroundMode == "Gradient" && index <= maxBackgroundGradientColors }

    private val backgroundBorder by float("BackgroundBorder-Width", 1F, 0.5F..5F)

    private val bgBorderColors = ColorSettingsInteger(this, "BackgroundBorderColor")
        .with(r = 100, g = 150, b = 255, a = 200)

    private val blur by boolean("Blur", false)
    private val blurStrength by float("Blur-Strength", 10F, 1F..50F) { blur }

    private fun isColorModeUsed(value: String) = textColorMode == value || backgroundMode == value

    private val rainbowX by float("Rainbow-X", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val rainbowY by float("Rainbow-Y", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val gradientX by float("Gradient-X", -500F, -2000F..2000F) { isColorModeUsed("Gradient") }
    private val gradientY by float("Gradient-Y", -1500F, -2000F..2000F) { isColorModeUsed("Gradient") }

    private var shadow by boolean("Shadow", true)
    private val font = font("Font", Fonts.fontRegular40)

    private val showMusicName by boolean("ShowMusicName", true)
    private val showLyric by boolean("ShowLyric", true)
    
    private val customWidth by int("Width", 200, 50..500)
    private val customHeight by int("Height", 50, 20..200)
    private val customScale by float("Scale", 1.0F, 0.5F..2.0F)

    private var animWidth = 0F
    private var animHeight = 0F
    private var animAlpha = 0F

    private var velWidth = 0f
    private var velHeight = 0f
    private var velAlpha = 0f

    private var lastLyric = ""
    private var lastMusicName = ""

    private val currentLyric: String
        get() = MusicPlayer.currentLyricDisplay

    private val currentMusicName: String
        get() = MusicPlayer.currentMusicName

    private val isPlaying: Boolean
        get() = MusicPlayer.isCurrentlyPlaying

    private fun spring(current: Float, target: Float, velocity: Float, tension: Float = animTension, friction: Float = animFriction): Pair<Float, Float> {
        val displacement = target - current
        val force = displacement * tension
        val drag = velocity * friction
        val acceleration = force - drag
        val newVelocity = velocity + acceleration
        val newPosition = current + newVelocity
        return newPosition to newVelocity
    }

    override fun drawElement(): Border {
        val fontRenderer = font.get()
        val fontHeight = fontRenderer.FONT_HEIGHT + 2

        val displayLines = mutableListOf<String>()
        
        if (showMusicName && currentMusicName != "None") {
            displayLines.add(currentMusicName)
        }
        
        if (showLyric && currentLyric.isNotEmpty()) {
            displayLines.add(currentLyric)
        }

        val targetAlpha = if (isPlaying && displayLines.isNotEmpty()) 1F else 0F

        if (!isPlaying || displayLines.isEmpty()) {
            if (enableBounce) {
                val (nextAlpha, vA) = spring(animAlpha, targetAlpha, velAlpha)
                animAlpha = nextAlpha.coerceIn(0F, 1F)
                velAlpha = vA
            } else {
                animAlpha = targetAlpha
            }
            
            if (animAlpha < 0.01f) {
                return Border(0F, 0F, 0F, 0F)
            }
        }

        val maxWidth = displayLines.maxOf { fontRenderer.getStringWidth(it).toFloat() }.coerceAtMost(customWidth.toFloat())
        val totalHeight = displayLines.size * fontHeight

        val bgScale = backgroundScale
        val horizontalPadding = 8F * bgScale
        val verticalPadding = (4F) * bgScale

        val targetWidth = (maxWidth + horizontalPadding * 2).coerceAtMost(customWidth.toFloat())
        val targetHeight = (totalHeight + verticalPadding * 2).coerceAtMost(customHeight.toFloat())

        if (enableBounce) {
            val (nextW, vW) = spring(animWidth, targetWidth, velWidth)
            animWidth = nextW.coerceAtLeast(0F)
            velWidth = vW

            val (nextH, vH) = spring(animHeight, targetHeight, velHeight)
            animHeight = nextH.coerceAtLeast(0F)
            velHeight = vH

            val (nextAlpha, vA) = spring(animAlpha, targetAlpha, velAlpha)
            animAlpha = nextAlpha.coerceIn(0F, 1F)
            velAlpha = vA
        } else {
            animWidth = targetWidth
            animHeight = targetHeight
            animAlpha = targetAlpha
        }

        if (animWidth < 1f || animHeight < 1f || animAlpha < 0.01f) {
            return Border(0F, 0F, 0F, 0F)
        }

        val drawWidth = animWidth
        val drawHeight = animHeight

        val rectPos = floatArrayOf(
            -(drawWidth / 2F),
            -verticalPadding,
            drawWidth / 2F,
            drawHeight - verticalPadding
        )

        val totalScale = scale * customScale

        if (blur) {
            glTranslated(-renderX, -renderY, 0.0)
            glScalef(1F, 1F, 1F)
            glPushMatrix()

            BlurUtils.blurArea(
                renderX.toFloat() + rectPos[0] * totalScale,
                renderY.toFloat() + rectPos[1] * totalScale,
                renderX.toFloat() + rectPos[2] * totalScale,
                renderY.toFloat() + rectPos[3] * totalScale,
                blurStrength
            )

            glPopMatrix()
            glScalef(scale, scale, scale)
            glTranslated(renderX, renderY, 0.0)
        }

        glPushMatrix()
        glScalef(customScale, customScale, customScale)

        val rainbow = textColorMode == "Rainbow"
        val gradient = textColorMode == "Gradient"
        val theme = textColorMode == "Theme"

        val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
        val gradientXVal = if (gradientX == 0f) 0f else 1f / gradientX
        val gradientYVal = if (gradientY == 0f) 0f else 1f / gradientY

        val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
        val rainbowXVal = if (rainbowX == 0f) 0f else 1f / rainbowX
        val rainbowYVal = if (rainbowY == 0f) 0f else 1f / rainbowY

        val alphaMultiplier = animAlpha

        val bgThemeGradient = backgroundMode == "Theme" && themeGradientMode != "Sync"
        val bgThemeGradientColors = if (bgThemeGradient) {
            val startColor = ClientThemesUtils.setColor("start", 255)
            val endColor = ClientThemesUtils.setColor("end", 255)
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
        
        val actualBgGradientSpeed = if (bgThemeGradient) ClientThemesUtils.ThemeFadeSpeed / 5f else gradientBackgroundSpeed

        GradientShader.begin(
            backgroundMode == "Gradient" || bgThemeGradient,
            gradientXVal,
            gradientYVal,
            bgThemeGradientColors,
            actualBgGradientSpeed,
            gradientOffset
        ).use {
            RainbowShader.begin(backgroundMode == "Rainbow", rainbowXVal, rainbowYVal, rainbowOffset).use {
                val bgColor = when (backgroundMode) {
                    "Gradient" -> Color(0, 0, 0, (150 * alphaMultiplier).toInt())
                    "Rainbow" -> Color(0, 0, 0, (150 * alphaMultiplier).toInt())
                    "Theme" -> {
                        if (themeGradientMode == "Sync") {
                            val themeColor = ClientThemesUtils.getColor()
                            Color(themeColor.red, themeColor.green, themeColor.blue, (150 * alphaMultiplier).toInt())
                        } else {
                            Color(0, 0, 0, (150 * alphaMultiplier).toInt())
                        }
                    }
                    "Sky" -> {
                        val skyColor = ColorUtils.skyRainbow(0, skySaturation, skyBrightness, skySpeed)
                        Color(skyColor.red, skyColor.green, skyColor.blue, (150 * alphaMultiplier).toInt())
                    }
                    "Mixer" -> {
                        val mixerColor = getMixedColor(0, mixerSeconds)
                        Color(mixerColor.red, mixerColor.green, mixerColor.blue, (150 * alphaMultiplier).toInt())
                    }
                    else -> Color(bgColors.color().red, bgColors.color().green, bgColors.color().blue, (bgColors.color().alpha * alphaMultiplier).toInt())
                }
                drawRoundedRect(
                    rectPos[0], rectPos[1], rectPos[2], rectPos[3],
                    bgColor.rgb,
                    roundedBackgroundRadius
                )
            }
        }

        if (bgBorderColors.color().alpha > 0) {
            val borderColor = Color(
                bgBorderColors.color().red,
                bgBorderColors.color().green,
                bgBorderColors.color().blue,
                (bgBorderColors.color().alpha * alphaMultiplier).toInt()
            )
            drawRoundedBorder(
                rectPos[0],
                rectPos[1],
                rectPos[2],
                rectPos[3],
                backgroundBorder,
                borderColor.rgb,
                roundedBackgroundRadius
            )
        }

        val themeGradient = theme && themeGradientMode != "Sync"
        val themeGradientColors = if (themeGradient) {
            val startColor = ClientThemesUtils.setColor("start", 255)
            val endColor = ClientThemesUtils.setColor("end", 255)
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
        
        val actualTextGradientSpeed = if (themeGradient) ClientThemesUtils.ThemeFadeSpeed / 5f else gradientTextSpeed

        val colorToUse = when {
            rainbow || gradient || themeGradient -> 0
            theme -> ClientThemesUtils.getColor().rgb
            textColorMode == "Sky" -> ColorUtils.skyRainbow(0, skySaturation, skyBrightness, skySpeed).rgb
            textColorMode == "Mixer" -> getMixedColor(0, mixerSeconds).rgb
            else -> {
                val c = colors.color()
                Color(c.red, c.green, c.blue, (c.alpha * alphaMultiplier).toInt()).rgb
            }
        }

        GradientFontShader.begin(
            gradient || themeGradient,
            gradientXVal,
            gradientYVal,
            themeGradientColors,
            actualTextGradientSpeed,
            gradientOffset
        ).use {
            RainbowFontShader.begin(rainbow, rainbowXVal, rainbowYVal, rainbowOffset).use {
                displayLines.forEachIndexed { index, line ->
                    val yOffset = index * fontHeight
                    val textX = when (textPositionMode) {
                        "Center" -> {
                            -fontRenderer.getStringWidth(line) / 2F
                        }
                        else -> -maxWidth / 2F
                    }
                    fontRenderer.drawString(line, textX, yOffset.toFloat(), colorToUse, shadow)
                }
            }
        }

        glPopMatrix()

        return Border(rectPos[0] * customScale, rectPos[1] * customScale, rectPos[2] * customScale, rectPos[3] * customScale)
    }

    override fun updateElement() {
    }
}
