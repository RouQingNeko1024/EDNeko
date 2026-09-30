package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color
import java.util.HashMap
import kotlin.math.min
import kotlin.math.max

// ===== 扩展属性：存储每个模块的 HUD 状态 =====
private val arrayMap = HashMap<Module, Boolean>()
private val slideMap = HashMap<Module, Float>()
private val arrayYMap = HashMap<Module, Float>()
private val slideStepMap = HashMap<Module, Float>()
private val hueMap = HashMap<Module, Float>()

var Module.array: Boolean
    get() = arrayMap[this] ?: true
    set(value) { arrayMap[this] = value }

var Module.slide: Float
    get() = slideMap[this] ?: 0f
    set(value) { slideMap[this] = value }

var Module.arrayY: Float
    get() = arrayYMap[this] ?: 0f
    set(value) { arrayYMap[this] = value }

var Module.slideStep: Float
    get() = slideStepMap[this] ?: 0f
    set(value) { slideStepMap[this] = value }

var Module.hue: Float
    get() = hueMap[this] ?: 0f
    set(value) { hueMap[this] = value }

// ===== 辅助函数 =====
fun skyRainbow(index: Int, distance: Int, saturation: Float, brightness: Float): Int {
    val hue = (System.currentTimeMillis() % (distance * 1000L)) / (distance * 1000f) + index * 0.01f
    return Color.getHSBColor(hue % 1f, saturation, brightness).rgb
}

fun getRainbowOpaque(seconds: Int, saturation: Float, brightness: Float, offset: Int): Int {
    val hue = (System.currentTimeMillis() % (seconds * 1000L)) / (seconds * 1000f) + offset * 0.001f
    return Color.getHSBColor(hue % 1f, saturation, brightness).rgb
}

fun liquidSlowly(index: Int, distance: Int, saturation: Float, brightness: Float): Int {
    val hue = (System.currentTimeMillis() % (distance * 1000L)) / (distance * 1000f) + index * 0.01f
    return Color.getHSBColor(hue % 1f, saturation, brightness).rgb
}

fun animate(target: Float, current: Float, speed: Float): Float {
    return if (current < target) min(target, current + speed)
    else if (current > target) max(target, current - speed)
    else current
}

@ElementInfo(name = "Arraylist2", single = true)
class Arraylist2(
    x: Double = 1.0,
    y: Double = 2.0,
    scale: Float = 1F,
    side: Side = Side(Horizontal.RIGHT, Vertical.UP)
) : Element("Arraylist2", x, y, scale, side) {

    // ===== 颜色模式 =====
    private val colorMode by choices(
        "Color",
        arrayOf("Custom", "Random", "Sky", "CRainbow", "LiquidSlowly", "Fade", "Mixer"),
        "Custom"
    )

    private val blur by boolean("Blur", false)
    private val blurStrength by float("Blur-Strength", 0F, 0F..30F)

    private val colorRed by int("Red", 0, 0..255)
    private val colorGreen by int("Green", 111, 0..255)
    private val colorBlue by int("Blue", 255, 0..255)
    private val colorAlpha by int("Alpha", 255, 0..255)

    private val saturation by float("Saturation", 0.9f, 0f..1f)
    private val brightness by float("Brightness", 1f, 0f..1f)
    private val skyDistance by int("Sky-Distance", 2, 0..4)
    private val cRainbowSec by int("CRainbow-Seconds", 2, 1..10)
    private val cRainbowDist by int("CRainbow-Distance", 2, 1..6)
    private val mixerSec by int("Mixer-Seconds", 2, 1..10)
    private val mixerDist by int("Mixer-Distance", 2, 0..10)
    private val liquidSlowlyDistance by int("LiquidSlowly-Distance", 90, 1..90)
    private val fadeDistance by int("Fade-Distance", 50, 1..100)

    private val hAnimation by choices(
        "HorizontalAnimation",
        arrayOf("Default", "None", "Slide", "Astolfo"),
        "Default"
    )
    private val vAnimation by choices(
        "VerticalAnimation",
        arrayOf("None", "LiquidSense", "Slide", "Rise", "Astolfo"),
        "None"
    )
    private val animationSpeed by float("Animation-Speed", 0.25F, 0.01F..1F)

    private val nameBreak by boolean("NameBreak", true)
    private val abcOrder by boolean("Alphabetical-Order", false)
    private val tags by boolean("Tags", true)
    private val tagsStyle by choices("TagsStyle", arrayOf("-", "|", "()", "[]", "<>", "Default"), "-")
    private val shadow by boolean("ShadowText", true)
    private val tagsArrayColor by boolean("TagsArrayColor", false)

    private val backgroundColorRed by int("Background-R", 0, 0..255)
    private val backgroundColorGreen by int("Background-G", 0, 0..255)
    private val backgroundColorBlue by int("Background-B", 0, 0..255)
    private val backgroundColorAlpha by int("Background-Alpha", 0, 0..255)

    private val rectRight by choices("Rect-Right", arrayOf("None", "Left", "Right", "Outline", "Special", "Top"), "None")
    private val rectLeft by choices("Rect-Left", arrayOf("None", "Left", "Right"), "None")

    private val lowerCase by boolean("LowerCase", false)
    private val space by float("Space", 0F, 0F..5F)
    private val textHeight by float("TextHeight", 11F, 1F..20F)
    private val textY by float("TextY", 1F, 0F..20F)

    private val font by font("Font", Fonts.minecraftFont)

    private var sortedModules = emptyList<Module>()

    override fun drawElement(): Border? {
        val fontRenderer = font
        val counter = intArrayOf(0)

        AWTFontRenderer.assumeNonVolatile = true

        val delta = RenderUtils.deltaTime.toFloat()
        val customColor = Color(colorRed, colorGreen, colorBlue, colorAlpha).rgb
        val rectCustomColor = customColor
        val backgroundCustomColor = Color(
            backgroundColorRed, backgroundColorGreen, backgroundColorBlue, backgroundColorAlpha
        ).rgb
        val textSpacer = textHeight + space
        val colorModeLower = colorMode.lowercase()
        val rectRightLower = rectRight.lowercase()
        val rectLeftLower = rectLeft.lowercase()

        // 更新垂直动画
        var inx = 0
        for (module in sortedModules) {
            val shouldAdd = module.array && module.slide > 0F
            var yPos = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) *
                    if (side.vertical == Vertical.DOWN) inx + 1 else inx

            if (shouldAdd) {
                if (vAnimation.equals("Rise", true) && !module.state)
                    yPos = -fontRenderer.FONT_HEIGHT - textY

                val size = sortedModules.size * 2.0E-2f

                when (vAnimation.lowercase()) {
                    "liquidsense" -> {
                        if (module.state) {
                            if (module.arrayY < yPos) {
                                module.arrayY += (size - min(module.arrayY * 0.002f, size - module.arrayY * 0.0001f)) * delta
                                module.arrayY = min(yPos, module.arrayY)
                            } else {
                                module.arrayY -= (size - min(module.arrayY * 0.002f, size - module.arrayY * 0.0001f)) * delta
                                module.arrayY = max(module.arrayY, yPos)
                            }
                        }
                    }
                    "slide", "rise" -> module.arrayY = animate(yPos, module.arrayY, animationSpeed * delta * 0.025f)
                    "astolfo" -> {
                        if (module.arrayY < yPos) {
                            module.arrayY += animationSpeed / 2F * delta
                            module.arrayY = min(yPos, module.arrayY)
                        } else {
                            module.arrayY -= animationSpeed / 2F * delta
                            module.arrayY = max(module.arrayY, yPos)
                        }
                    }
                    else -> module.arrayY = yPos
                }
                inx++
            } else if (!vAnimation.equals("rise", true)) {
                module.arrayY = yPos
            }
        }

        // 更新水平动画
        for (module in LiquidBounce.moduleManager) {
            if (!module.array || (!module.state && module.slide == 0F)) continue

            val displayString = getModName(module)
            val width = fontRenderer.getStringWidth(displayString).toFloat()

            when (hAnimation.lowercase()) {
                "astolfo" -> {
                    if (module.state) {
                        if (module.slide < width) {
                            module.slide += animationSpeed * delta
                            module.slideStep = delta
                        }
                    } else if (module.slide > 0) {
                        module.slide -= animationSpeed * delta
                        module.slideStep = 0F
                    }
                    if (module.slide > width) module.slide = width
                }
                "slide" -> {
                    if (module.state) {
                        if (module.slide < width) {
                            module.slide = animate(width, module.slide, animationSpeed * delta * 0.025f)
                            module.slideStep = delta
                        }
                    } else if (module.slide > 0) {
                        module.slide = animate(0f, module.slide, animationSpeed * delta * 0.025f)
                        module.slideStep = 0F
                    }
                }
                "default" -> {
                    if (module.state) {
                        if (module.slide < width) {
                            module.slide = easeOut(module.slideStep, width) * width
                            module.slideStep += delta / 4F
                        }
                    } else if (module.slide > 0) {
                        module.slide = easeOut(module.slideStep, width) * width
                        module.slideStep -= delta / 4F
                    }
                }
                else -> {
                    module.slide = if (module.state) width else 0f
                    module.slideStep += if (module.state) delta else -delta
                }
            }

            module.slide = module.slide.coerceIn(0F, width)
            module.slideStep = module.slideStep.coerceIn(0F, width)
        }

        // 绘制
        when (side.horizontal) {
            Horizontal.RIGHT, Horizontal.MIDDLE -> {
                sortedModules.forEachIndexed { index, module ->
                    val displayString = getModName(module)
                    val width = fontRenderer.getStringWidth(displayString).toFloat()
                    val xPos = -module.slide - 2f

                    val moduleColor = Color.getHSBColor(module.hue, saturation, brightness).rgb
                    val sky = skyRainbow(counter[0] * (skyDistance * 50), skyDistance, saturation, brightness)
                    val cRainbow = getRainbowOpaque(cRainbowSec, saturation, brightness, counter[0] * (50 * cRainbowDist))
                    val fadeColor = ColorUtils.fade(Color(colorRed, colorGreen, colorBlue, colorAlpha), index * fadeDistance, 100).rgb
                    counter[0]--
                    val liquid = liquidSlowly(index * liquidSlowlyDistance, liquidSlowlyDistance, saturation, brightness)
                    val mixerColor = getRainbowOpaque(mixerSec, saturation, brightness, -index * mixerDist * 10)

                    RenderUtils.drawRect(
                        xPos - if (rectRightLower == "right") 3f else 2f,
                        module.arrayY,
                        if (rectRightLower == "right") -1f else 0f,
                        module.arrayY + textHeight,
                        backgroundCustomColor
                    )

                    fontRenderer.drawString(
                        displayString,
                        xPos - if (rectRightLower == "right") 1f else 0f,
                        module.arrayY + textY,
                        when {
                            colorModeLower == "random" -> moduleColor
                            colorModeLower == "sky" -> sky
                            colorModeLower == "crainbow" -> cRainbow
                            colorModeLower == "liquidslowly" -> liquid
                            colorModeLower == "fade" -> fadeColor
                            colorModeLower == "mixer" -> mixerColor
                            else -> customColor
                        },
                        shadow
                    )

                    if (rectRightLower != "none") {
                        val rectColor = when {
                            colorModeLower == "random" -> moduleColor
                            colorModeLower == "sky" -> sky
                            colorModeLower == "crainbow" -> cRainbow
                            colorModeLower == "liquidslowly" -> liquid
                            colorModeLower == "fade" -> fadeColor
                            colorModeLower == "mixer" -> mixerColor
                            else -> rectCustomColor
                        }

                        when (rectRightLower) {
                            "left" -> RenderUtils.drawRect(xPos - 5f, module.arrayY, xPos - 2f, module.arrayY + textHeight, rectColor)
                            "right" -> RenderUtils.drawRect(-1f, module.arrayY, 0f, module.arrayY + textHeight, rectColor)
                            "outline" -> {
                                RenderUtils.drawRect(-1f, module.arrayY - 1f, 0f, module.arrayY + textHeight, rectColor)
                                RenderUtils.drawRect(xPos - 3f, module.arrayY, xPos - 2f, module.arrayY + textHeight, rectColor)
                                if (module != sortedModules[0]) {
                                    val prevDisplay = getModName(sortedModules[index - 1])
                                    RenderUtils.drawRect(xPos - 3f - (fontRenderer.getStringWidth(prevDisplay).toFloat() - width), module.arrayY, xPos - 2f, module.arrayY + 1f, rectColor)
                                    if (module == sortedModules[sortedModules.size - 1]) {
                                        RenderUtils.drawRect(xPos - 3f, module.arrayY + textHeight, 0f, module.arrayY + textHeight + 1f, rectColor)
                                    }
                                } else {
                                    RenderUtils.drawRect(xPos - 3f, module.arrayY, 0f, module.arrayY - 1f, rectColor)
                                }
                            }
                            "special" -> {
                                if (module == sortedModules[0]) RenderUtils.drawRect(xPos - 2f, module.arrayY, 0f, module.arrayY - 1f, rectColor)
                                if (module == sortedModules[sortedModules.size - 1]) RenderUtils.drawRect(xPos - 2f, module.arrayY + textHeight, 0f, module.arrayY + textHeight + 1f, rectColor)
                            }
                            "top" -> if (module == sortedModules[0]) RenderUtils.drawRect(xPos - 2f, module.arrayY, 0f, module.arrayY - 1f, rectColor)
                        }
                    }
                }
            }

            Horizontal.LEFT -> {
                sortedModules.forEachIndexed { index, module ->
                    val displayString = getModName(module)
                    val width = fontRenderer.getStringWidth(displayString).toFloat()
                    val xPos = -(width - module.slide) + if (rectLeftLower == "left") 3f else 2f

                    val moduleColor = Color.getHSBColor(module.hue, saturation, brightness).rgb
                    val sky = skyRainbow(counter[0] * (skyDistance * 50), skyDistance, saturation, brightness)
                    val cRainbow = getRainbowOpaque(cRainbowSec, saturation, brightness, counter[0] * (50 * cRainbowDist))
                    val fadeColor = ColorUtils.fade(Color(colorRed, colorGreen, colorBlue, colorAlpha), index * fadeDistance, 100).rgb
                    counter[0]--
                    val liquid = liquidSlowly(index * liquidSlowlyDistance, liquidSlowlyDistance, saturation, brightness)
                    val mixerColor = getRainbowOpaque(mixerSec, saturation, brightness, -index * mixerDist * 10)

                    // 绘制背景矩形（确保 xPos 和 width 都是 Float）
                    RenderUtils.drawRect(
                        0f,
                        module.arrayY,
                        xPos + width + if (rectLeftLower == "right") 3f else 2f,
                        module.arrayY + textHeight,
                        backgroundCustomColor
                    )

                    fontRenderer.drawString(
                        displayString,
                        xPos,
                        module.arrayY + textY,
                        when {
                            colorModeLower == "random" -> moduleColor
                            colorModeLower == "sky" -> sky
                            colorModeLower == "crainbow" -> cRainbow
                            colorModeLower == "liquidslowly" -> liquid
                            colorModeLower == "fade" -> fadeColor
                            colorModeLower == "mixer" -> mixerColor
                            else -> customColor
                        },
                        shadow
                    )

                    if (rectLeftLower != "none") {
                        val rectColor = when {
                            colorModeLower == "random" -> moduleColor
                            colorModeLower == "sky" -> sky
                            colorModeLower == "crainbow" -> cRainbow
                            colorModeLower == "liquidslowly" -> liquid
                            colorModeLower == "fade" -> fadeColor
                            colorModeLower == "mixer" -> mixerColor
                            else -> rectCustomColor
                        }

                        when (rectLeftLower) {
                            "left" -> RenderUtils.drawRect(0f, module.arrayY - 1f, 1f, module.arrayY + textHeight, rectColor)
                            "right" -> RenderUtils.drawRect(xPos + width + 2f, module.arrayY, xPos + width + 3f, module.arrayY + textHeight, rectColor)
                        }
                    }
                }
            }
        }

        // 编辑模式边框
        if (mc.currentScreen is GuiHudDesigner) {
            val maxWidth = (sortedModules.maxOfOrNull { fontRenderer.getStringWidth(getModName(it)) } ?: 20).toFloat()
            val totalHeight = sortedModules.size * textSpacer
            return if (side.horizontal == Horizontal.LEFT) {
                Border(0f, 0f, maxWidth + 6f, totalHeight)
            } else {
                Border(-maxWidth - 6f, 0f, 0f, totalHeight)
            }
        }

        AWTFontRenderer.assumeNonVolatile = false
        GlStateManager.resetColor()
        return null
    }

    private fun easeOut(progress: Float, max: Float): Float {
        return 1f - (1f - progress.coerceIn(0f, 1f)).let { it * it }
    }

    override fun updateElement() {
        sortedModules = if (abcOrder) {
            LiquidBounce.moduleManager
                .filter { it.array && (if (hAnimation.equals("none", true)) it.state else it.slide > 0) }
                .sortedBy { getModName(it) }
        } else {
            LiquidBounce.moduleManager
                .filter { it.array && (if (hAnimation.equals("none", true)) it.state else it.slide > 0) }
                .sortedByDescending { font.getStringWidth(getModName(it)) }
        }
    }

    private fun getModName(mod: Module): String {
        var modTag = ""
        if (tags && mod.tag != null) {
            modTag += " "
            if (!tagsArrayColor) modTag += "§7"
            if (!tagsStyle.equals("default", true)) {
                modTag += tagsStyle.getOrNull(0)?.toString() ?: ""
                if (tagsStyle.equals("-", true) || tagsStyle.equals("|", true)) modTag += " "
            }
            modTag += mod.tag
            if (!tagsStyle.equals("default", true) && !tagsStyle.equals("-", true) && !tagsStyle.equals("|", true)) {
                modTag += tagsStyle.getOrNull(1)?.toString() ?: ""
            }
        }

        var displayName = (if (nameBreak) mod.name.replace(Regex("([a-z])([A-Z])"), "$1 $2") else mod.name) + modTag
        if (lowerCase) displayName = displayName.toLowerCase()
        return displayName
    }
}