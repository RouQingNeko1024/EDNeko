/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.GameDetector
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.safeDiv
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.render.*
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.EmbeddedStencil
import net.ccbluex.liquidbounce.utils.render.InternalBlurShader
import net.ccbluex.liquidbounce.utils.render.ColorUtils.fade
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.features.module.modules.render.getMixedColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.minecraft.client.renderer.GlStateManager.resetColor
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * CustomHUD Arraylist element
 *
 * Shows a list of enabled modules
 */
@ElementInfo(name = "Arraylist", single = true)
class Arraylist(
    x: Double = 0.0, y: Double = 0.0, scale: Float = 1F,
    side: Side = Side(Horizontal.RIGHT, Vertical.UP),
) : Element("Arraylist", x, y, scale, side) {

    private val textColorMode by choices(
        "Text-Mode", arrayOf("Custom", "Fade", "Random", "Rainbow", "Gradient", "Theme", "Sky", "Mixer"), "Theme"
    )
    private val textColors = ColorSettingsInteger(this, "TextColor") { textColorMode == "Custom" }.with(blueRibbon)
    private val textFadeColors = ColorSettingsInteger(this, "Text-Fade") { textColorMode == "Fade" }.with(0, 111, 255)

    private val textFadeDistance by int("Text-Fade-Distance", 50, 0..100) { textColorMode == "Fade" }

    private val gradientTextSpeed by float("Text-Gradient-Speed", 1f, 0.5f..10f) { textColorMode == "Gradient" }

    private val maxTextGradientColors by int(
        "Max-Text-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS
    ) { textColorMode == "Gradient" }
    private val textGradColors =
        ColorSettingsFloat.create(this, "Text-Gradient") { textColorMode == "Gradient" && it <= maxTextGradientColors }

    private val rectMode by choices("Rect-Mode", arrayOf("None", "Left", "Right", "Outline"), "Right")
    private val roundedRectRadius by float("RoundedRect-Radius", 0F, 0F..2F) { rectMode !in setOf("None", "Outline") }
    private val rectColorMode by choices(
        "Rect-ColorMode", arrayOf("Custom", "Fade", "Random", "Rainbow", "Gradient", "Theme", "Sky", "Mixer"), "Theme"
    ) { rectMode != "None" }
    private val rectColors =
        ColorSettingsInteger(this, "RectColor", applyMax = true) { isCustomRectSupported }.with(blueRibbon)
    private val rectFadeColors = ColorSettingsInteger(this, "Rect-Fade", applyMax = true) { rectColorMode == "Fade" }

    private val rectFadeDistance by int("Rect-Fade-Distance", 50, 0..100) { rectColorMode == "Fade" }

    private val gradientRectSpeed by float("Rect-Gradient-Speed", 1f, 0.5f..10f) { isCustomRectGradientSupported }

    private val maxRectGradientColors by int(
        "Max-Rect-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS
    ) { isCustomRectGradientSupported }
    private val rectGradColors = ColorSettingsFloat.create(
        this, "Rect-Gradient"
    ) { isCustomRectGradientSupported && it <= maxRectGradientColors }

    private val roundedBackgroundRadius by float("RoundedBackGround-Radius", 1F, 0F..5F) { bgColors.color().alpha > 0 }

    private val backgroundMode by choices(
        "Background-Mode", arrayOf("Custom", "Fade", "Random", "Rainbow", "Gradient", "Theme", "Sky", "Mixer"), "Custom"
    )
    private val bgColors =
        ColorSettingsInteger(this, "BackgroundColor") { backgroundMode == "Custom" }.with(Color.BLACK.withAlpha(150))
    private val bgFadeColors = ColorSettingsInteger(this, "Background-Fade") { backgroundMode == "Fade" }

    private val bgFadeDistance by int("Background-Fade-Distance", 50, 0..100) { backgroundMode == "Fade" }

    private val gradientBackgroundSpeed by float(
        "Background-Gradient-Speed", 1f, 0.5f..10f
    ) { backgroundMode == "Gradient" }

    private val maxBackgroundGradientColors by int(
        "Max-Background-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS
    ) { backgroundMode == "Gradient" }
    private val bgGradColors = ColorSettingsFloat.create(
        this, "Background-Gradient"
    ) { backgroundMode == "Gradient" && it <= maxBackgroundGradientColors }

    private val enableGlow by boolean("EnableGlow", false)
    private val glowMode by choices("GlowMode", arrayOf("Text", "Background", "Both"), "Text") { enableGlow }
    private val glowColorMode by choices("GlowColorMode", arrayOf("Custom", "Fade", "Random", "Rainbow", "Gradient", "Theme"), "Custom") { enableGlow }
    private val glowColors = ColorSettingsInteger(this, "GlowColor", applyMax = true) { enableGlow && glowColorMode == "Custom" }.with(0, 111, 255, 100)
    private val glowFadeColors = ColorSettingsInteger(this, "Glow-Fade", applyMax = true) { enableGlow && glowColorMode == "Fade" }
    private val glowFadeDistance by int("Glow-Fade-Distance", 50, 0..100) { enableGlow && glowColorMode == "Fade" }
    private val glowGradientSpeed by float("Glow-Gradient-Speed", 1f, 0.5f..10f) { enableGlow && glowColorMode == "Gradient" }
    private val maxGlowGradientColors by int("Max-Glow-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS) { enableGlow && glowColorMode == "Gradient" }
    private val glowGradColors = ColorSettingsFloat.create(this, "Glow-Gradient") { enableGlow && glowColorMode == "Gradient" && it <= maxGlowGradientColors }
    private val glowBlurRadius by int("GlowBlurRadius", 10, 1..30) { enableGlow }
    private val glowStrength by int("GlowStrength", 1, 1..2) { enableGlow }

    private val blur by boolean("Blur", false)
    private val blurStrength by float("Blur-Strength", 10F, 1F..50F) { blur }

    private val displayIcons by boolean("DisplayIcons", true)
    private val iconShadows by boolean("IconShadows", true) { displayIcons }
    private val xDistance by float("ShadowXDistance", 0F, -2F..2F) { iconShadows }
    private val yDistance by float("ShadowYDistance", 0F, -2F..2F) { iconShadows }
    private val shadowColor by color("ShadowColor", Color.BLACK.withAlpha(128), rainbow = true) { iconShadows }

    private val iconColorMode by choices(
        "IconColorMode", arrayOf("Custom", "Fade"), "Custom"
    ) { displayIcons }
    private val iconColor by color("IconColor", Color.WHITE) { iconColorMode == "Custom" && displayIcons }
    private val iconFadeColor by color("IconFadeColor", Color.WHITE) { iconColorMode == "Fade" && displayIcons }
    private val iconFadeDistance by int("IconFadeDistance", 50, 0..100) { iconColorMode == "Fade" && displayIcons }

    private fun isColorModeUsed(value: String) = value in listOf(textColorMode, rectColorMode, backgroundMode, iconColorMode, glowColorMode, tagsColorMode)

    private val saturation by float("Random-Saturation", 0.9f, 0f..1f) { isColorModeUsed("Random") }
    private val brightness by float("Random-Brightness", 1f, 0f..1f) { isColorModeUsed("Random") }
    private val rainbowX by float("Rainbow-X", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val rainbowY by float("Rainbow-Y", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val gradientX by float("Gradient-X", -1000F, -2000F..2000F) { isColorModeUsed("Gradient") }
    private val gradientY by float("Gradient-Y", -1000F, -2000F..2000F) { isColorModeUsed("Gradient") }
    
    private val skySaturation by float("Sky-Saturation", 0.9f, 0f..1f) { isColorModeUsed("Sky") }
    private val skyBrightness by float("Sky-Brightness", 1f, 0f..1f) { isColorModeUsed("Sky") }
    private val skySpeed by float("Sky-Speed", 1f, 0.1f..5f) { isColorModeUsed("Sky") }
    private val mixerSeconds by int("Mixer-Seconds", 2, 1..10) { isColorModeUsed("Mixer") }

    private val tags by boolean("Tags", true)
    private val tagsStyle by choices("TagsStyle", arrayOf("[]", "()", "<>", "-", "|", "Space"), "Space") {
        tags
    }.onChanged { updateTagDetails() }
    private val tagsCase by choices("TagsCase", arrayOf("Normal", "Uppercase", "Lowercase"), "Normal") { tags }
    private val tagsArrayColor by boolean("TagsArrayColor", false) {
        tags
    }.onChanged { updateTagDetails() }

    private val tagsColorMode by choices(
        "Tags-ColorMode", arrayOf("Custom", "Fade", "Random", "Rainbow", "Gradient", "Theme", "Sky", "Mixer"), "Custom"
    ) { tags }
    private val tagsColors = ColorSettingsInteger(this, "TagsColor") { tagsColorMode == "Custom" }.with(Color.GRAY)
    private val tagsFadeColors = ColorSettingsInteger(this, "Tags-Fade") { tagsColorMode == "Fade" }.with(Color.LIGHT_GRAY)

    private val tagsFadeDistance by int("Tags-Fade-Distance", 50, 0..100) { tagsColorMode == "Fade" }

    private val gradientTagsSpeed by float("Tags-Gradient-Speed", 1f, 0.5f..10f) { tagsColorMode == "Gradient" }

    private val maxTagsGradientColors by int(
        "Max-Tags-Gradient-Colors", 4, 1..MAX_GRADIENT_COLORS
    ) { tagsColorMode == "Gradient" }
    private val tagsGradColors =
        ColorSettingsFloat.create(this, "Tags-Gradient") { tagsColorMode == "Gradient" && it <= maxTagsGradientColors }

    private val font by font("Font", Fonts.fontSemibold35)
    private val textShadow by boolean("ShadowText", true)
    private val moduleCase by choices("ModuleCase", arrayOf("Normal", "Uppercase", "Lowercase"), "Normal")
    private val space by float("Space", 1F, 0F..5F)
    private val textHeight by float("TextHeight", 11F, 1F..20F)
    private val textY by float("TextY", 3.25F, 0F..20F)

    private val animation by choices("Animation", arrayOf("Slide", "Smooth"), "Smooth") { tags }
    private val animationSpeed by float("AnimationSpeed", 0.2F, 0.01F..1F) { animation == "Smooth" }

    companion object : Configurable("StandaloneArraylist") {
        val spacedModulesValue = boolean("SpacedModules", false)
    }

    private val spacedModules: Boolean by +spacedModulesValue

    private val inactiveStyle by choices(
        "InactiveModulesStyle", arrayOf("Normal", "Color", "Hide"), "Color"
    ) { GameDetector.state }

    private var x2 = 0
    private var y2 = 0F

    private lateinit var tagPrefix: String
    private lateinit var tagSuffix: String

    private var modules = emptyList<Module>()

    private val inactiveColor = Color(255, 255, 255, 100).rgb

    private val isCustomRectSupported
        get() = rectMode != "None" && rectColorMode == "Custom"

    private val isCustomRectGradientSupported
        get() = rectMode != "None" && rectColorMode == "Gradient"

    init {
        updateTagDetails()
    }

    fun updateTagDetails() {
        val pair: Pair<String, String> = when (tagsStyle) {
            "[]", "()", "<>" -> tagsStyle[0].toString() to tagsStyle[1].toString()
            "-", "|" -> tagsStyle[0] + " " to ""
            else -> "" to ""
        }

        tagPrefix = (if (tagsArrayColor) " " else " ") + pair.first
        tagSuffix = pair.second
    }

    private fun getDisplayString(module: Module): String {
        val moduleName = when (moduleCase) {
            "Uppercase" -> module.getName().uppercase()
            "Lowercase" -> module.getName().lowercase()
            else -> module.getName()
        }

        var tag = module.tag ?: ""

        tag = when (tagsCase) {
            "Uppercase" -> tag.uppercase()
            "Lowercase" -> tag.lowercase()
            else -> tag
        }

        val moduleTag = if (tags && !module.tag.isNullOrEmpty()) tagPrefix + tag + tagSuffix else ""

        return moduleName + moduleTag
    }

    private fun drawGlowEffect(startX: Float, startY: Float, width: Float, height: Float, index: Int = 0) {
        if (enableGlow) {
            val glowColor = when (glowColorMode) {
                "Gradient" -> Color(0, 111, 255, 100)
                "Rainbow" -> Color(0, 111, 255, 100)
                "Random" -> Color.getHSBColor(index * 0.05f % 1f, saturation, brightness).withAlpha(100)
                "Fade" -> fade(glowFadeColors, index * glowFadeDistance, 100).withAlpha(100)
                "Theme" -> {
                    val themeColor = net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor(index)
                    Color(themeColor.red, themeColor.green, themeColor.blue, 100)
                }
                else -> glowColors.color()
            }
            GlowUtils.drawGlow(
                startX, startY,
                width, height,
                (glowStrength * glowBlurRadius).toInt(),
                glowColor
            )
        }
    }

    override fun drawElement(): Border? {
        assumeNonVolatile {
            val delta = deltaTime

            val padding = if (displayIcons) 15 else 0

            for (module in moduleManager) {
                val shouldShow = (!module.isHidden && module.state && (inactiveStyle != "Hide" || module.isActive))

                if (!shouldShow && module.slide <= 0f) continue

                val displayString = getDisplayString(module)

                val width = font.getStringWidth(displayString) + padding

                when (animation) {
                    "Slide" -> {
                        module.slideStep += if (shouldShow) delta / 4F else -delta / 4F
                        if (shouldShow) {
                            if (module.slide < width) {
                                module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                            }
                        } else {
                            module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                        }

                        module.slide = module.slide.coerceIn(0F, width.toFloat())
                        module.slideStep = module.slideStep.coerceIn(0F, width.toFloat())
                    }

                    "Smooth" -> {
                        val target = if (shouldShow) width.toDouble() else -width / 5.0
                        module.slide =
                            AnimationUtil.base(module.slide.toDouble(), target, animationSpeed.toDouble()).toFloat()
                    }
                }
            }

            val textCustomColor = textColors.color().rgb
            val rectCustomColor = rectColors.color().rgb
            val backgroundCustomColor = bgColors.color().rgb
            val tagsCustomColor = tagsColors.color().rgb
            val textSpacer = textHeight + space

            val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
            val actualRainbowX = 1f safeDiv rainbowX
            val actualRainbowY = 1f safeDiv rainbowY

            val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
            val actualGradientX = 1f safeDiv gradientX
            val actualGradientY = 1f safeDiv gradientY

            modules.forEachIndexed { index, module ->
                var yPos =
                    (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Vertical.DOWN) index + 1 else index
                if (animation == "Smooth") {
                    module.yAnim = AnimationUtil.base(module.yAnim.toDouble(), yPos.toDouble(), 0.2).toFloat()
                    yPos = module.yAnim
                }
                val moduleColor = Color.getHSBColor(module.hue, saturation, brightness).rgb

                val textFadeColor = fade(textFadeColors, index * textFadeDistance, 100).rgb
                val bgFadeColor = fade(bgFadeColors, index * bgFadeDistance, 100).rgb
                val rectFadeColor = fade(rectFadeColors, index * rectFadeDistance, 100).rgb
                val iconFadeColor = fade(iconFadeColor, index * iconFadeDistance, 100).rgb
                val tagsFadeColor = fade(tagsFadeColors, index * tagsFadeDistance, 100).rgb

                val markAsInactive = inactiveStyle == "Color" && !module.isActive

                val displayString = getDisplayString(module)
                val moduleName = when (moduleCase) {
                    "Uppercase" -> module.getName().uppercase()
                    "Lowercase" -> module.getName().lowercase()
                    else -> module.getName()
                }
                val moduleTag = if (tags && !module.tag.isNullOrEmpty()) {
                    var tag = module.tag ?: ""
                    tag = when (tagsCase) {
                        "Uppercase" -> tag.uppercase()
                        "Lowercase" -> tag.lowercase()
                        else -> tag
                    }
                    tagPrefix + tag + tagSuffix
                } else ""

                val displayStringWidth = font.getStringWidth(displayString)
                val moduleNameWidth = font.getStringWidth(moduleName)

                val previousDisplayString = getDisplayString(modules[(if (index > 0) index else 1) - 1])
                val previousDisplayStringWidth = font.getStringWidth(previousDisplayString)

                when (side.horizontal) {
                    Horizontal.RIGHT, Horizontal.MIDDLE -> {
                        val xPos = -module.slide - if (displayIcons) 2 else 3

                        if (blur && module.slide > 0) {
                            val blurX = if (side.horizontal == Horizontal.LEFT) renderX.toFloat() else renderX.toFloat() + xPos - if (rectMode == "Right") 5 else 2
                            val blurY = renderY.toFloat() + yPos
                            val blurWidth = -xPos + if (rectMode == "Right") 2 else 0
                            val blurHeight = textSpacer

                            GL11.glPushMatrix()
                            GL11.glTranslated(-renderX, -renderY, 0.0)
                            GL11.glScalef(1F / scale, 1F / scale, 1F)

                            try {
                                EmbeddedStencil.checkSetupFBO(mc.framebuffer)
                                EmbeddedStencil.write(false)
                                drawRoundedRect(
                                    blurX, blurY, blurX + blurWidth, blurY + blurHeight,
                                    Color.WHITE.rgb, roundedBackgroundRadius,
                                    if (rectMode == "Left") {
                                        RenderUtils.RoundedCorners.NONE
                                    } else {
                                        RenderUtils.RoundedCorners.LEFT_ONLY
                                    }
                                )
                                EmbeddedStencil.erase(true)
                                val scaledBlurRadius = blurStrength * (bgColors.color().alpha / 255f)
                                InternalBlurShader.blurArea(blurX, blurY, blurWidth, blurHeight, scaledBlurRadius)
                                EmbeddedStencil.dispose()
                            } catch (_: Exception) {}

                            GL11.glScalef(scale, scale, scale)
                            GL11.glTranslated(renderX, renderY, 0.0)
                            GL11.glPopMatrix()
                        }

                        when (glowMode) {
                            "Background", "Both" -> {
                                drawGlowEffect(
                                    xPos - if (rectMode == "Right") 5 else 2,
                                    yPos,
                                    (if (rectMode == "Right") -3F else -1F) - (xPos - if (rectMode == "Right") 5 else 2),
                                    textSpacer,
                                    index
                                )
                            }
                        }

                        GradientShader.begin(
                            !markAsInactive && backgroundMode == "Gradient",
                            actualGradientX,
                            actualGradientY,
                            bgGradColors.toColorArray(maxBackgroundGradientColors),
                            gradientBackgroundSpeed,
                            gradientOffset
                        ).use {
                            RainbowShader.begin(backgroundMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset).use {
                                drawRoundedRect(
                                    xPos - if (rectMode == "Right") 5 else 2,
                                    yPos,
                                    if (rectMode == "Right") -3F else -1F,
                                    yPos + textSpacer,
                                    when (backgroundMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> bgFadeColor
                                        "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor(index).rgb
                                        "Sky" -> ColorUtils.skyRainbow(index, skySaturation, skyBrightness, skySpeed).rgb
                                        "Mixer" -> getMixedColor(index, mixerSeconds).rgb
                                        else -> backgroundCustomColor
                                    },
                                    roundedBackgroundRadius,
                                    if (rectMode == "Left") {
                                        RenderUtils.RoundedCorners.NONE
                                    } else {
                                        RenderUtils.RoundedCorners.LEFT_ONLY
                                    }
                                )
                            }
                        }

                        GradientFontShader.begin(
                            !markAsInactive && textColorMode == "Gradient",
                            actualGradientX,
                            actualGradientY,
                            textGradColors.toColorArray(maxTextGradientColors),
                            gradientTextSpeed,
                            gradientOffset
                        ).use {
                            RainbowFontShader.begin(
                                !markAsInactive && textColorMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset
                            ).use {
                                font.drawString(
                                    moduleName,
                                    xPos + 1 - if (rectMode == "Right") 3 else 0,
                                    yPos + textY,
                                    if (markAsInactive) inactiveColor
                                    else when (textColorMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> textFadeColor
                                        "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor(index).rgb
                                        "Sky" -> ColorUtils.skyRainbow(index, skySaturation, skyBrightness, skySpeed).rgb
                                        "Mixer" -> getMixedColor(index, mixerSeconds).rgb
                                        else -> textCustomColor
                                    },
                                    textShadow,
                                )
                            }
                        }

                        if (moduleTag.isNotEmpty()) {
                            GradientFontShader.begin(
                                !markAsInactive && tagsColorMode == "Gradient",
                                actualGradientX,
                                actualGradientY,
                                tagsGradColors.toColorArray(maxTagsGradientColors),
                                gradientTagsSpeed,
                                gradientOffset
                            ).use {
                                RainbowFontShader.begin(
                                    !markAsInactive && tagsColorMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset
                                ).use {
                                    font.drawString(
                                        moduleTag,
                                        xPos + 1 - (if (rectMode == "Right") 3 else 0) + moduleNameWidth,
                                        yPos + textY,
                                        if (markAsInactive) inactiveColor
                                        else when (tagsColorMode) {
                                            "Gradient" -> 0
                                            "Rainbow" -> 0
                                            "Random" -> moduleColor
                                            "Fade" -> tagsFadeColor
                                            "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor(index).rgb
                                            "Sky" -> ColorUtils.skyRainbow(index, skySaturation, skyBrightness, skySpeed).rgb
                                            "Mixer" -> getMixedColor(index, mixerSeconds).rgb
                                            else -> tagsCustomColor
                                        },
                                        textShadow,
                                    )
                                }
                            }
                        }

                        GradientShader.begin(
                            !markAsInactive && isCustomRectGradientSupported,
                            actualGradientX,
                            actualGradientY,
                            rectGradColors.toColorArray(maxRectGradientColors),
                            gradientRectSpeed,
                            gradientOffset
                        ).use {
                            if (rectMode != "None") {
                                RainbowShader.begin(
                                    !markAsInactive && rectColorMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset
                                ).use {
                                    val rectColor = if (markAsInactive) inactiveColor
                                    else when (rectColorMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> rectFadeColor
                                        "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor(index).rgb
                                        "Sky" -> ColorUtils.skyRainbow(index, skySaturation, skyBrightness, skySpeed).rgb
                                        "Mixer" -> getMixedColor(index, mixerSeconds).rgb
                                        else -> rectCustomColor
                                    }

                                    when (rectMode) {
                                        "Left" -> drawRoundedRect(
                                            xPos - 5f,
                                            yPos,
                                            xPos - 2f,
                                            yPos + textSpacer,
                                            rectColor,
                                            roundedRectRadius,
                                            RenderUtils.RoundedCorners.LEFT_ONLY
                                        )

                                        "Right" -> drawRoundedRect(
                                            -3F,
                                            yPos,
                                            0F,
                                            yPos + textSpacer,
                                            rectColor,
                                            roundedRectRadius,
                                            if (modules.lastIndex == 0) {
                                                RenderUtils.RoundedCorners.RIGHT_ONLY
                                            } else when (module) {
                                                modules.first() -> RenderUtils.RoundedCorners.TOP_RIGHT_ONLY
                                                modules.last() -> RenderUtils.RoundedCorners.BOTTOM_RIGHT_ONLY
                                                else -> RenderUtils.RoundedCorners.NONE
                                            }
                                        )

                                        "Outline" -> {
                                            drawRect(-1F, yPos - 1F, 0F, yPos + textSpacer, rectColor)
                                            drawRect(xPos - 3f, yPos, xPos - 2f, yPos + textSpacer, rectColor)

                                            if (module == modules.first()) {
                                                drawRect(xPos - 3f, yPos - 1F, 0F, yPos, rectColor)
                                            }

                                            val widthDiff1 = (previousDisplayStringWidth - displayStringWidth).toFloat()
                                            drawRect(
                                                xPos - 3f - widthDiff1,
                                                yPos,
                                                xPos - 2f,
                                                yPos + 1f,
                                                rectColor
                                            )

                                            if (module == modules.last()) {
                                                drawRect(
                                                    xPos - 3f, yPos + textSpacer, 0F, yPos + textSpacer + 1f, rectColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Horizontal.LEFT -> {
                        val textWidth = font.getStringWidth(displayString)
                        val xPos = -(textWidth - module.slide) + if (rectMode == "Left") 6 else 3

                        if (blur && module.slide > 0) {
                            val blurX = renderX.toFloat() + if (rectMode == "Left") 1f else 0f
                            val blurY = renderY.toFloat() + yPos
                            val blurWidth = (textWidth + if (rectMode == "Right") 4 else 1).toFloat()
                            val blurHeight = textSpacer

                            GL11.glPushMatrix()
                            GL11.glTranslated(-renderX, -renderY, 0.0)
                            GL11.glScalef(1F / scale, 1F / scale, 1F)

                            try {
                                EmbeddedStencil.checkSetupFBO(mc.framebuffer)
                                EmbeddedStencil.write(false)
                                drawRoundedRect(
                                    blurX, blurY, blurX + blurWidth, blurY + blurHeight,
                                    Color.WHITE.rgb, roundedBackgroundRadius,
                                    if (rectMode == "Right") {
                                        RenderUtils.RoundedCorners.NONE
                                    } else {
                                        RenderUtils.RoundedCorners.RIGHT_ONLY
                                    }
                                )
                                EmbeddedStencil.erase(true)
                                val scaledBlurRadius = blurStrength * (bgColors.color().alpha / 255f)
                                InternalBlurShader.blurArea(blurX, blurY, blurWidth, blurHeight, scaledBlurRadius)
                                EmbeddedStencil.dispose()
                            } catch (_: Exception) {}

                            GL11.glScalef(scale, scale, scale)
                            GL11.glTranslated(renderX, renderY, 0.0)
                            GL11.glPopMatrix()
                        }

                        when (glowMode) {
                            "Background", "Both" -> {
                                drawGlowEffect(
                                    if (rectMode == "Left") 1f else 0f,
                                    yPos,
                                    (xPos + textWidth + if (rectMode == "Right") 4 else 1) - (if (rectMode == "Left") 1f else 0f),
                                    textSpacer,
                                    index
                                )
                            }
                        }

                        GradientShader.begin(
                            !markAsInactive && backgroundMode == "Gradient",
                            actualGradientX,
                            actualGradientY,
                            bgGradColors.toColorArray(maxBackgroundGradientColors),
                            gradientBackgroundSpeed,
                            gradientOffset
                        ).use {
                            RainbowShader.begin(backgroundMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset).use {
                                drawRoundedRect(
                                    if (rectMode == "Left") 1f else 0f,
                                    yPos,
                                    xPos + textWidth + if (rectMode == "Right") 4 else 1,
                                    yPos + textSpacer,
                                    when (backgroundMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> bgFadeColor
                                        "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor(index).rgb
                                        "Sky" -> ColorUtils.skyRainbow(index, skySaturation, skyBrightness, skySpeed).rgb
                                        "Mixer" -> getMixedColor(index, mixerSeconds).rgb
                                        else -> backgroundCustomColor
                                    },
                                    roundedBackgroundRadius,
                                    if (rectMode == "Right") {
                                        RenderUtils.RoundedCorners.NONE
                                    } else {
                                        RenderUtils.RoundedCorners.RIGHT_ONLY
                                    }
                                )
                            }
                        }

                        GradientFontShader.begin(
                            !markAsInactive && textColorMode == "Gradient",
                            actualGradientX,
                            actualGradientY,
                            textGradColors.toColorArray(maxTextGradientColors),
                            gradientTextSpeed,
                            gradientOffset
                        ).use {
                            RainbowFontShader.begin(
                                !markAsInactive && textColorMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset
                            ).use {
                                font.drawString(
                                    moduleName, xPos - 1, yPos + textY, if (markAsInactive) inactiveColor
                                    else when (textColorMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> textFadeColor
                                        "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor(index).rgb
                                        "Sky" -> ColorUtils.skyRainbow(index, skySaturation, skyBrightness, skySpeed).rgb
                                        "Mixer" -> getMixedColor(index, mixerSeconds).rgb
                                        else -> textCustomColor
                                    }, textShadow
                                )
                            }
                        }

                        if (moduleTag.isNotEmpty()) {
                            GradientFontShader.begin(
                                !markAsInactive && tagsColorMode == "Gradient",
                                actualGradientX,
                                actualGradientY,
                                tagsGradColors.toColorArray(maxTagsGradientColors),
                                gradientTagsSpeed,
                                gradientOffset
                            ).use {
                                RainbowFontShader.begin(
                                    !markAsInactive && tagsColorMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset
                                ).use {
                                    font.drawString(
                                        moduleTag,
                                        xPos - 1 + font.getStringWidth(moduleName),
                                        yPos + textY,
                                        if (markAsInactive) inactiveColor
                                        else when (tagsColorMode) {
                                            "Gradient" -> 0
                                            "Rainbow" -> 0
                                            "Random" -> moduleColor
                                            "Fade" -> tagsFadeColor
                                            "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor(index).rgb
                                            "Sky" -> ColorUtils.skyRainbow(index, skySaturation, skyBrightness, skySpeed).rgb
                                            "Mixer" -> getMixedColor(index, mixerSeconds).rgb
                                            else -> tagsCustomColor
                                        }, textShadow
                                    )
                                }
                            }
                        }

                        GradientShader.begin(
                            !markAsInactive && isCustomRectGradientSupported,
                            actualGradientX,
                            actualGradientY,
                            rectGradColors.toColorArray(maxRectGradientColors),
                            gradientRectSpeed,
                            gradientOffset
                        ).use {
                            if (rectMode != "None") {
                                RainbowShader.begin(
                                    !markAsInactive && rectColorMode == "Rainbow", actualRainbowX, actualRainbowY, rainbowOffset
                                ).use {
                                    val rectColor = if (markAsInactive) inactiveColor
                                    else when (rectColorMode) {
                                        "Gradient" -> 0
                                        "Rainbow" -> 0
                                        "Random" -> moduleColor
                                        "Fade" -> rectFadeColor
                                        "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor(index).rgb
                                        "Sky" -> ColorUtils.skyRainbow(index, skySaturation, skyBrightness, skySpeed).rgb
                                        "Mixer" -> getMixedColor(index, mixerSeconds).rgb
                                        else -> rectCustomColor
                                    }

                                    when (rectMode) {
                                        "Left" -> drawRoundedRect(
                                            0F,
                                            yPos,
                                            3F,
                                            yPos + textSpacer,
                                            rectColor,
                                            roundedRectRadius,
                                            if (modules.lastIndex == 0) {
                                                RenderUtils.RoundedCorners.LEFT_ONLY
                                            } else when (module) {
                                                modules.first() -> RenderUtils.RoundedCorners.TOP_LEFT_ONLY
                                                modules.last() -> RenderUtils.RoundedCorners.BOTTOM_LEFT_ONLY
                                                else -> RenderUtils.RoundedCorners.NONE
                                            }
                                        )

                                        "Right" -> drawRoundedRect(
                                            xPos + textWidth + 2f,
                                            yPos,
                                            xPos + textWidth + 5f,
                                            yPos + textSpacer,
                                            rectColor,
                                            roundedRectRadius,
                                            RenderUtils.RoundedCorners.RIGHT_ONLY
                                        )

                                        "Outline" -> {
                                            drawRect(-1F, yPos - 1F, 0F, yPos + textSpacer, rectColor)
                                            drawRect(
                                                xPos + textWidth + 1f,
                                                yPos - 1F,
                                                xPos + textWidth + 2f,
                                                yPos + textSpacer,
                                                rectColor
                                            )

                                            if (module == modules.first()) {
                                                drawRect(xPos + textWidth + 2f, yPos - 1f, xPos + textWidth + 2f, yPos, rectColor)
                                                drawRect(-1F, yPos - 1f, xPos + textWidth + 2f, yPos, rectColor)
                                            }

                                            val widthDiff2 = (previousDisplayStringWidth - displayStringWidth).toFloat()
                                            drawRect(
                                                xPos + textWidth + 1f,
                                                yPos - 1f,
                                                xPos + textWidth + 2f + widthDiff2,
                                                yPos,
                                                rectColor
                                            )

                                            if (module == modules.last()) {
                                                drawRect(
                                                    xPos + textWidth + 1f,
                                                    yPos + textSpacer,
                                                    xPos + textWidth + 2f,
                                                    yPos + textSpacer + 1f,
                                                    rectColor
                                                )
                                                drawRect(
                                                    -1F,
                                                    yPos + textSpacer,
                                                    xPos + textWidth + 2f,
                                                    yPos + textSpacer + 1f,
                                                    rectColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (displayIcons) {
                    val textWidth = font.getStringWidth(displayString)

                    val iconX = if (side.horizontal == Side.Horizontal.LEFT) {
                        (-textWidth + module.slide) / 6 + if (rectMode == "Left") 3 else 0
                    } else {
                        -module.slide - 2 + textWidth + if (rectMode == "Right") 0 else 2
                    }

                    val resource = module.category.iconResourceLocation

                    if (iconShadows) {
                        drawImage(resource, iconX + xDistance, yPos + yDistance, 12, 12, shadowColor)
                    }

                    val iconColor = if (markAsInactive) {
                        inactiveColor
                    } else when (iconColorMode) {
                        "Gradient" -> 0
                        "Rainbow" -> 0
                        "Fade" -> iconFadeColor
                        else -> this.iconColor.rgb
                    }

                    drawImage(resource, iconX, yPos, 12, 12, Color(iconColor, true))
                }
            }

            if (mc.currentScreen is GuiHudDesigner) {
                x2 = Int.MIN_VALUE

                if (modules.isEmpty()) {
                    return if (side.horizontal == Horizontal.LEFT) Border(0F, -1F, 20F, 20F)
                    else Border(0F, -1F, -20F, 20F)
                }

                for (module in modules) {
                    when (side.horizontal) {
                        Horizontal.RIGHT, Horizontal.MIDDLE -> {
                            val xPos = -module.slide.toInt() - 2
                            if (x2 == Int.MIN_VALUE || xPos < x2) x2 = xPos
                        }

                        Horizontal.LEFT -> {
                            val xPos = module.slide.toInt() + 16
                            if (x2 == Int.MIN_VALUE || xPos > x2) x2 = xPos
                        }
                    }
                }

                y2 = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * modules.size

                return Border(0F, 0F, x2 - 7F, y2 - if (side.vertical == Vertical.DOWN) 1F else 0F)
            }
        }

        resetColor()
        return null
    }

    override fun updateElement() {
        modules = moduleManager.filter { it.slide > 0 && !it.isHidden }
            .sortedBy { -font.getStringWidth(getDisplayString(it)) }
    }
}
