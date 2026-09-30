package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import java.awt.Color

object BlurSettings : Module("BlurSettings", Category.RENDER) {
    val enabled by boolean("Enabled", false)
    val mode by choices(
        "Mode",
        arrayOf("Offset", "Radial", "Shader", "Gaussian", "Kawase", "Naven"),
        "Offset"
    ) { enabled }

    val offsetStrength by float("OffsetStrength", 8f, 0f..35f) { enabled && mode == "Offset" }
    val offsetDirections by int("OffsetDirections", 12, 4..64) { enabled && mode == "Offset" }
    val offsetLayers by int("OffsetLayers", 6, 4..128) { enabled && mode == "Offset" }
    val passes by int("Passes", 8, 0..50) { enabled && mode == "Offset" }

    val radialStrength by float("RadialStrength", 8f, 0f..35f) { enabled && mode == "Radial" }
    val radialSamples by int("RadialSamples", 8, 1..50) { enabled && mode == "Radial" }

    val shaderStrength by float("ShaderStrength", 4f, 0f..15f) { enabled && mode == "Shader" }
    val quality by int("Quality", 0, 0..50) { enabled && mode == "Shader" }
    val shaderIterations by int("ShaderIterations", 1, 0..20) { enabled && mode == "Shader" }

    val gaussianStrength by float("GaussianStrength", 4f, 0f..15f) { enabled && mode == "Gaussian" }
    val gaussianQuality by int("GaussianQuality", 0, 0..50) { enabled && mode == "Gaussian" }
    val gaussianOpacity by int("GaussianOpacity", 100, 0..100) { enabled && mode == "Gaussian" }
    val gaussianDirections by int("GaussianDirections", 24, 4..48) { enabled && mode == "Gaussian" }
    val gaussianLayers by int("GaussianLayers", 16, 2..32) { enabled && mode == "Gaussian" }

    val kawaseIterations by int("KawaseIterations", 3, 1..10) { enabled && mode == "Kawase" }
    val kawaseOffset by int("KawaseOffset", 2, 1..5) { enabled && mode == "Kawase" }

    val navenRadius by float("NavenRadius", 12f, 0f..120f) { enabled && mode == "Naven" }
    val navenLayers by int("NavenLayers", 8, 1..24) { enabled && mode == "Naven" }
    val navenDirections by int("NavenDirections", 16, 8..32) { enabled && mode == "Naven" }
    val navenOpacity by int("NavenOpacity", 100, 0..100) { enabled && mode == "Naven" }

    // 原版 GUI 按钮（单人/多人模式等）的模糊与阴影
    val guiButtonBlur by boolean("GuiButtonBlur", true) { enabled }
    val guiButtonShadow by boolean("GuiButtonShadow", true) { enabled }
    val guiButtonShadowMask by boolean("GuiButtonShadowMask", false) { enabled && guiButtonShadow }
    val guiButtonShadowStrength by int("GuiButtonShadowStrength", 10, 0..40) { enabled && guiButtonShadow }
    val guiButtonRadius by float("GuiButtonRadius", 4F, 0F..10F) { enabled }
    val guiButtonShadowRadius by float("GuiButtonShadowRadius", 4F, 0F..10F) { enabled && guiButtonShadow }

    val guiButtonBlurMethod by choices("GuiButtonBlurMethod", arrayOf("Blur", "Acrylic"), "Blur") { enabled && guiButtonBlur }

    // 原版 GUI 按钮颜色（自定义）
    val guiButtonColor by color("GuiButtonColor", Color(33, 33, 33, 200)) { enabled }
    val guiButtonDisabledColor by color("GuiButtonDisabledColor", Color(100, 100, 100, 150)) { enabled }
    val guiButtonTextColor by color("GuiButtonTextColor", Color(255, 255, 255)) { enabled }
    val guiButtonDisabledTextColor by color("GuiButtonDisabledTextColor", Color(170, 170, 170)) { enabled }
    val guiButtonShadowColor by color("GuiButtonShadowColor", Color(0, 0, 0, 90)) { enabled && guiButtonShadow }

    fun guiButtonBgArgb() =
        Color(guiButtonColor.red, guiButtonColor.green, guiButtonColor.blue, guiButtonColor.alpha).rgb
    fun guiButtonDisabledBgArgb() =
        Color(guiButtonDisabledColor.red, guiButtonDisabledColor.green, guiButtonDisabledColor.blue, guiButtonDisabledColor.alpha).rgb
    fun guiButtonTextArgb() =
        Color(guiButtonTextColor.red, guiButtonTextColor.green, guiButtonTextColor.blue, guiButtonTextColor.alpha).rgb
    fun guiButtonDisabledTextArgb() =
        Color(guiButtonDisabledTextColor.red, guiButtonDisabledTextColor.green, guiButtonDisabledTextColor.blue, guiButtonDisabledTextColor.alpha).rgb
    fun guiButtonShadowArgb() =
        Color(guiButtonShadowColor.red, guiButtonShadowColor.green, guiButtonShadowColor.blue, guiButtonShadowColor.alpha).rgb

    // 边框模糊设置
    val borderEnabled by boolean("BorderEnabled", false) { enabled }
    val borderWidth by float("BorderWidth", 4f, 0f..20f) { borderEnabled }
    val borderBlurStrength by float("BorderBlurStrength", 8f, 0f..35f) { borderEnabled }
    val borderColor by color("BorderColor", Color(255, 255, 255, 150)) { borderEnabled }
    val borderOpacity by int("BorderOpacity", 100, 0..100) { borderEnabled }
    val borderOnly by boolean("BorderOnly", true) { borderEnabled }

    // 各 HUD 元素的模糊开关
    val chatStyle by boolean("ChatStyle", true) { enabled }
    val watermark by boolean("Watermark", true) { enabled }
    val scoreboard by boolean("Scoreboard", true) { enabled }
    val notifications by boolean("Notifications", true) { enabled }
    val targetHUD by boolean("TargetHUD", true) { enabled }
    val blockRateDisplay by boolean("BlockRateDisplay", true) { enabled }
    val gapple by boolean("Gapple", true) { enabled }
    val arraylist by boolean("Arraylist", true) { enabled }
    val chestStealer by boolean("ChestStealer", true) { enabled }
    val inventory by boolean("Inventory", true) { enabled }
    val hotbar by boolean("Hotbar", true) { enabled }
    val armor by boolean("Armor", true) { enabled }
    val keystrokes by boolean("Keystrokes", true) { enabled }
    val potionEffects by boolean("PotionEffects", true) { enabled }
    val textElement by boolean("TextElement", true) { enabled }

    // 新增：ClientDetector 模块的模糊开关
    val clientDetector by boolean("ClientDetector", true) { enabled }

    // ===== Container/Inventory style (shadow + blur + rounded corners) =====
    val containerStyle by boolean("ContainerStyle", false)
    val containerBlur by boolean("ContainerBlur", true) { containerStyle }
    val containerPanelColor by color("ContainerPanelColor", Color(16, 16, 16, 190)) { containerStyle }
    val containerRadius by float("ContainerRadius", 6f, 0f..20f) { containerStyle }
    val containerShadow by boolean("ContainerShadow", true) { containerStyle }
    val containerShadowColor by color("ContainerShadowColor", Color(0, 0, 0, 150)) { containerStyle && containerShadow }
    val containerShadowStrength by int("ContainerShadowStrength", 16, 0..40) { containerStyle && containerShadow }
    val containerShadowMask by boolean("ContainerShadowMask", true) { containerStyle && containerShadow }
    val containerSlotBox by boolean("ContainerSlotBox", true) { containerStyle }
    val containerSlotColor by color("ContainerSlotColor", Color(255, 255, 255, 30)) { containerStyle && containerSlotBox }
    val containerSlotRadius by float("ContainerSlotRadius", 5f, 0f..10f) { containerStyle && containerSlotBox }
}