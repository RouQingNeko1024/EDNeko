/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.font

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.file.FileManager.fontsDir
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.io.readJson
import net.ccbluex.liquidbounce.utils.io.writeJson
import net.minecraft.client.gui.FontRenderer
import java.awt.Font
import java.io.File
import kotlin.system.measureTimeMillis

data class FontInfo(val name: String, val size: Int = -1, val isCustom: Boolean = false)

data class CustomFontInfo(val name: String, val fontFile: String, val fontSize: Int)

private val FONT_REGISTRY = LinkedHashMap<FontInfo, FontRenderer>()

object Fonts : MinecraftInstance {

    private val configFile = File(fontsDir, "fonts.json")
    private var customFontInfoList: List<CustomFontInfo>
        get() = with(configFile) {
            if (exists()) {
                try {
                    readJson().asJsonArray.map {
                        it as JsonObject
                        val fontFile = it["fontFile"].asString
                        val fontSize = it["fontSize"].asInt
                        val name = if (it.has("name")) it["name"].asString else fontFile
                        CustomFontInfo(name, fontFile, fontSize)
                    }
                } catch (e: Exception) {
                    LOGGER.error("Failed to load fonts", e)
                    emptyList()
                }
            } else {
                createNewFile()
                writeText("[]")
                emptyList()
            }
        }
        set(value) = configFile.writeJson(value)

    val minecraftFontInfo = FontInfo(name = "Minecraft Font")
    val minecraftFont: FontRenderer by lazy {
        mc.fontRendererObj
    }

    lateinit var fontExtraBold35: GameFontRenderer
    lateinit var fontExtraBold40: GameFontRenderer
    lateinit var fontSemibold35: GameFontRenderer
    lateinit var fontSemibold40: GameFontRenderer
    lateinit var fontRegular40: GameFontRenderer
    lateinit var fontRegular45: GameFontRenderer
    lateinit var fontRegular35: GameFontRenderer
    lateinit var fontRegular30: GameFontRenderer
    lateinit var fontBold180: GameFontRenderer

    lateinit var fontNeutonBold35: GameFontRenderer
    lateinit var fontNeutonBold40: GameFontRenderer
    lateinit var fontNeutonBold50: GameFontRenderer
    lateinit var fontNeutonExtraBold35: GameFontRenderer
    lateinit var fontNeutonExtraBold40: GameFontRenderer
    lateinit var fontNeutonExtraBold50: GameFontRenderer
    lateinit var fontNeutonExtraLight35: GameFontRenderer
    lateinit var fontNeutonExtraLight40: GameFontRenderer
    lateinit var fontNeutonItalic35: GameFontRenderer
    lateinit var fontNeutonItalic40: GameFontRenderer
    lateinit var fontNeutonLight35: GameFontRenderer
    lateinit var fontNeutonLight40: GameFontRenderer
    lateinit var fontNeutonRegular35: GameFontRenderer
    lateinit var fontNeutonRegular40: GameFontRenderer
    lateinit var fontNeutonRegular50: GameFontRenderer
    lateinit var fontNosifer35: GameFontRenderer
    lateinit var fontNosifer40: GameFontRenderer
    lateinit var fontNosifer50: GameFontRenderer
    lateinit var fontZenDots35: GameFontRenderer
    lateinit var fontZenDots40: GameFontRenderer
    lateinit var fontZenDots50: GameFontRenderer

    lateinit var fontRobotoRegular35: GameFontRenderer
    lateinit var fontRobotoRegular40: GameFontRenderer
    lateinit var fontRobotoBold35: GameFontRenderer
    lateinit var fontRobotoBold40: GameFontRenderer
    lateinit var fontSFApple35: GameFontRenderer
    lateinit var fontSFApple40: GameFontRenderer
    lateinit var fontSFApple50: GameFontRenderer
    lateinit var fontRise35: GameFontRenderer
    lateinit var fontRise40: GameFontRenderer
    lateinit var fontRise50: GameFontRenderer
    lateinit var fontRiseIcon35: GameFontRenderer

    @JvmStatic
    val font24: GameFontRenderer by lazy { fontRegular30 }
    @JvmStatic
    val font30: GameFontRenderer by lazy { fontRegular30 }
    @JvmStatic
    val font35: GameFontRenderer by lazy { fontRegular35 }
    @JvmStatic
    val font40: GameFontRenderer by lazy { fontRegular40 }
    @JvmStatic
    val font52: GameFontRenderer by lazy { fontRegular45 }
    @JvmStatic
    val fontSF35: GameFontRenderer by lazy { fontSemibold35 }
    @JvmStatic
    val fontSF40: GameFontRenderer by lazy { fontSemibold40 }
    @JvmStatic
    val font72: GameFontRenderer by lazy { fontBold180 }
    @JvmStatic
    val font32: GameFontRenderer by lazy { fontRegular35 }

    private fun <T : FontRenderer> register(fontInfo: FontInfo, fontRenderer: T): T {
        FONT_REGISTRY[fontInfo] = fontRenderer
        return fontRenderer
    }

    fun registerCustomAWTFont(customFontInfo: CustomFontInfo, save: Boolean = true): GameFontRenderer? {
        val font = getFontFromResource(customFontInfo.fontFile, customFontInfo.fontSize)
            ?: getFallbackFont(customFontInfo.fontSize)
            ?: return null

        val result = register(
            FontInfo(customFontInfo.name, customFontInfo.fontSize, isCustom = true),
            font.asGameFontRenderer()
        )

        if (save) {
            customFontInfoList += customFontInfo
        }

        return result
    }

    fun loadFonts() {
        LOGGER.info("Start to load fonts.")
        val time = measureTimeMillis {
            register(minecraftFontInfo, minecraftFont)

            fontRegular30 = register(
                FontInfo(name = "HarmonyOS Sans SC Regular", size = 30),
                getFontOrDefault("HarmonyOS_Sans_SC_Regular.ttf", 30).asGameFontRenderer()
            )

            fontSemibold35 = register(
                FontInfo(name = "HarmonyOS Sans SC Medium", size = 35),
                getFontOrDefault("HarmonyOS_Sans_SC_Medium.ttf", 35).asGameFontRenderer()
            )

            fontRegular35 = register(
                FontInfo(name = "HarmonyOS Sans SC Regular", size = 35),
                getFontOrDefault("HarmonyOS_Sans_SC_Regular.ttf", 35).asGameFontRenderer()
            )

            fontRegular40 = register(
                FontInfo(name = "HarmonyOS Sans SC Regular", size = 40),
                getFontOrDefault("HarmonyOS_Sans_SC_Regular.ttf", 40).asGameFontRenderer()
            )

            fontSemibold40 = register(
                FontInfo(name = "HarmonyOS Sans SC Medium", size = 40),
                getFontOrDefault("HarmonyOS_Sans_SC_Medium.ttf", 40).asGameFontRenderer()
            )

            fontRegular45 = register(
                FontInfo(name = "HarmonyOS Sans SC Regular", size = 45),
                getFontOrDefault("HarmonyOS_Sans_SC_Regular.ttf", 45).asGameFontRenderer()
            )

            fontExtraBold35 = register(
                FontInfo(name = "HarmonyOS Sans SC Black", size = 35),
                getFontOrDefault("HarmonyOS_Sans_SC_Medium.ttf", 35).asGameFontRenderer()
            )

            fontExtraBold40 = register(
                FontInfo(name = "HarmonyOS Sans SC Black", size = 40),
                getFontOrDefault("HarmonyOS_Sans_SC_Medium.ttf", 40).asGameFontRenderer()
            )

            fontBold180 = register(
                FontInfo(name = "HarmonyOS Sans SC Bold", size = 180),
                getFontOrDefault("HarmonyOS_Sans_SC_Medium.ttf", 180).asGameFontRenderer()
            )

            fontNeutonBold35 = register(
                FontInfo(name = "Neuton Bold", size = 35),
                getFontOrDefault("Neuton-Bold.ttf", 35).asGameFontRenderer()
            )

            fontNeutonBold40 = register(
                FontInfo(name = "Neuton Bold", size = 40),
                getFontOrDefault("Neuton-Bold.ttf", 40).asGameFontRenderer()
            )

            fontNeutonBold50 = register(
                FontInfo(name = "Neuton Bold", size = 50),
                getFontOrDefault("Neuton-Bold.ttf", 50).asGameFontRenderer()
            )

            fontNeutonExtraBold35 = register(
                FontInfo(name = "Neuton ExtraBold", size = 35),
                getFontOrDefault("Neuton-ExtraBold.ttf", 35).asGameFontRenderer()
            )

            fontNeutonExtraBold40 = register(
                FontInfo(name = "Neuton ExtraBold", size = 40),
                getFontOrDefault("Neuton-ExtraBold.ttf", 40).asGameFontRenderer()
            )

            fontNeutonExtraBold50 = register(
                FontInfo(name = "Neuton ExtraBold", size = 50),
                getFontOrDefault("Neuton-ExtraBold.ttf", 50).asGameFontRenderer()
            )

            fontNeutonExtraLight35 = register(
                FontInfo(name = "Neuton ExtraLight", size = 35),
                getFontOrDefault("Neuton-ExtraLight.ttf", 35).asGameFontRenderer()
            )

            fontNeutonExtraLight40 = register(
                FontInfo(name = "Neuton ExtraLight", size = 40),
                getFontOrDefault("Neuton-ExtraLight.ttf", 40).asGameFontRenderer()
            )

            fontNeutonItalic35 = register(
                FontInfo(name = "Neuton Italic", size = 35),
                getFontOrDefault("Neuton-Italic.ttf", 35).asGameFontRenderer()
            )

            fontNeutonItalic40 = register(
                FontInfo(name = "Neuton Italic", size = 40),
                getFontOrDefault("Neuton-Italic.ttf", 40).asGameFontRenderer()
            )

            fontNeutonLight35 = register(
                FontInfo(name = "Neuton Light", size = 35),
                getFontOrDefault("Neuton-Light.ttf", 35).asGameFontRenderer()
            )

            fontNeutonLight40 = register(
                FontInfo(name = "Neuton Light", size = 40),
                getFontOrDefault("Neuton-Light.ttf", 40).asGameFontRenderer()
            )

            fontNeutonRegular35 = register(
                FontInfo(name = "Neuton Regular", size = 35),
                getFontOrDefault("Neuton-Regular.ttf", 35).asGameFontRenderer()
            )

            fontNeutonRegular40 = register(
                FontInfo(name = "Neuton Regular", size = 40),
                getFontOrDefault("Neuton-Regular.ttf", 40).asGameFontRenderer()
            )

            fontNeutonRegular50 = register(
                FontInfo(name = "Neuton Regular", size = 50),
                getFontOrDefault("Neuton-Regular.ttf", 50).asGameFontRenderer()
            )

            fontNosifer35 = register(
                FontInfo(name = "Nosifer", size = 35),
                getFontOrDefault("Nosifer-Regular.ttf", 35).asGameFontRenderer()
            )

            fontNosifer40 = register(
                FontInfo(name = "Nosifer", size = 40),
                getFontOrDefault("Nosifer-Regular.ttf", 40).asGameFontRenderer()
            )

            fontNosifer50 = register(
                FontInfo(name = "Nosifer", size = 50),
                getFontOrDefault("Nosifer-Regular.ttf", 50).asGameFontRenderer()
            )

            fontZenDots35 = register(
                FontInfo(name = "Zen Dots", size = 35),
                getFontOrDefault("ZenDots-Regular.ttf", 35).asGameFontRenderer()
            )

            fontZenDots40 = register(
                FontInfo(name = "Zen Dots", size = 40),
                getFontOrDefault("ZenDots-Regular.ttf", 40).asGameFontRenderer()
            )

            fontZenDots50 = register(
                FontInfo(name = "Zen Dots", size = 50),
                getFontOrDefault("ZenDots-Regular.ttf", 50).asGameFontRenderer()
            )

            fontRobotoRegular35 = register(
                FontInfo(name = "Roboto Regular", size = 35),
                getFontOrDefault("roboto-regular.ttf", 35).asGameFontRenderer()
            )

            fontRobotoRegular40 = register(
                FontInfo(name = "Roboto Regular", size = 40),
                getFontOrDefault("roboto-regular.ttf", 40).asGameFontRenderer()
            )

            fontRobotoBold35 = register(
                FontInfo(name = "Roboto Bold", size = 35),
                getFontOrDefault("Roboto-Bold.ttf", 35).asGameFontRenderer()
            )

            fontRobotoBold40 = register(
                FontInfo(name = "Roboto Bold", size = 40),
                getFontOrDefault("Roboto-Bold.ttf", 40).asGameFontRenderer()
            )

            fontSFApple35 = register(
                FontInfo(name = "SF Apple", size = 35),
                getFontOrDefault("SFApple.ttf", 35).asGameFontRenderer()
            )

            fontSFApple40 = register(
                FontInfo(name = "SF Apple", size = 40),
                getFontOrDefault("SFApple.ttf", 40).asGameFontRenderer()
            )

            fontSFApple50 = register(
                FontInfo(name = "SF Apple", size = 50),
                getFontOrDefault("SFApple.ttf", 50).asGameFontRenderer()
            )

            fontRise35 = register(
                FontInfo(name = "Rise SF UI Pro", size = 35),
                getFontOrDefault("SF-UI-Pro.ttf", 35).asGameFontRenderer()
            )

            fontRise40 = register(
                FontInfo(name = "Rise SF UI Pro", size = 40),
                getFontOrDefault("SF-UI-Pro.ttf", 40).asGameFontRenderer()
            )

            fontRise50 = register(
                FontInfo(name = "Rise SF UI Pro", size = 50),
                getFontOrDefault("SF-UI-Pro.ttf", 50).asGameFontRenderer()
            )

            fontRiseIcon35 = register(
                FontInfo(name = "Rise Icons", size = 35),
                getFontOrDefault("Icon-1.ttf", 35).asGameFontRenderer()
            )

            loadCustomFonts()
        }
        LOGGER.info("Loaded ${FONT_REGISTRY.size} fonts in ${time}ms")
    }

    private fun loadCustomFonts() {
        FONT_REGISTRY.keys.removeIf { it.isCustom }

        customFontInfoList.forEach {
            registerCustomAWTFont(it, save = false)
        }
    }

    fun getFontRenderer(name: String, size: Int): FontRenderer {
        return FONT_REGISTRY.entries.firstOrNull { (fontInfo, _) ->
            fontInfo.size == size && fontInfo.name.equals(name, true)
        }?.value ?: minecraftFont
    }

    fun getFontDetails(fontRenderer: FontRenderer): FontInfo? {
        return FONT_REGISTRY.keys.firstOrNull { FONT_REGISTRY[it] == fontRenderer }
    }

    val fonts: List<FontRenderer>
        get() = FONT_REGISTRY.values.toList()

    val customFonts: Map<FontInfo, FontRenderer>
        get() = FONT_REGISTRY.filterKeys { it.isCustom }

    fun removeCustomFont(fontInfo: FontInfo): CustomFontInfo? {
        if (!fontInfo.isCustom) {
            return null
        }

        FONT_REGISTRY.remove(fontInfo)
        return customFontInfoList.firstOrNull {
            it.name == fontInfo.name && it.fontSize == fontInfo.size
        }?.also {
            customFontInfoList -= it
        }
    }

    private fun getFontFromResource(file: String, size: Int): Font? {
        return try {
            val resourcePath = "/assets/minecraft/edneko/fonts/$file"
            val inputStream = javaClass.getResourceAsStream(resourcePath)
            if (inputStream == null) {
                LOGGER.warn("Font resource not found: $resourcePath")
                return null
            }
            inputStream.use { stream ->
                val font = Font.createFont(Font.TRUETYPE_FONT, stream)
                val derivedFont = font.deriveFont(Font.PLAIN, size.toFloat())
                LOGGER.info("Successfully loaded font from resource: $file (size=$size)")
                derivedFont
            }
        } catch (e: Exception) {
            LOGGER.error("Exception during loading font from resource[name=${file}, size=${size}]", e)
            null
        }
    }

    private fun getFallbackFont(size: Int): Font {
        val fallbackFonts = listOf(
            "Microsoft YaHei",
            "SimHei",
            "SimSun",
            "PingFang SC",
            "Noto Sans CJK SC",
            "WenQuanYi Micro Hei",
            "SansSerif"
        )

        for (fallbackName in fallbackFonts) {
            try {
                val fallbackFont = Font(fallbackName, Font.PLAIN, size)
                if (fallbackFont.canDisplay('\u4e2d') || fallbackFont.canDisplay('\u6587')) {
                    LOGGER.info("Using fallback font: $fallbackName (size=$size)")
                    return fallbackFont
                }
            } catch (e: Exception) {
                LOGGER.warn("Failed to create fallback font: $fallbackName", e)
            }
        }

        LOGGER.warn("No suitable fallback font found, using default SansSerif")
        return Font("SansSerif", Font.PLAIN, size)
    }

    private fun getFontOrDefault(file: String, size: Int): Font {
        return getFontFromResource(file, size) ?: getFallbackFont(size)
    }

    private fun Font.asGameFontRenderer(): GameFontRenderer {
        return GameFontRenderer(this@asGameFontRenderer)
    }

}
