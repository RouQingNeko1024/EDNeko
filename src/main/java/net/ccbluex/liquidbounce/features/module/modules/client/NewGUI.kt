/*
 * skid gold bounce
 * https://github.com/bzym2/GoldBounce/
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.NewUi
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.AuroraGUI
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.fade
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*

object NewGUI : Module("NewGUI", Category.CLIENT, Keyboard.KEY_RSHIFT, canBeEnabled = false) {

    private val styleValue by choices("Style", arrayOf("Material", "Aurora"), "Material")

    override fun onEnable() {
        when (styleValue) {
            "Aurora" -> mc.displayGuiScreen(AuroraGUI.getInstance())
            else -> mc.displayGuiScreen(NewUi.getInstance())
        }
    }

    val fastRenderValue by boolean("FastRender", false)

    val fontMode by choices("Font", arrayOf("Minecraft", "HarmonyOS"), "HarmonyOS")

    val colorModeValue by choices("Color", arrayOf("Custom", "Fade"), "Custom")

    val colorRedValue by int("Red", 0, 0..255)

    val colorGreenValue by int("Green", 140, 0..255)

    val colorBlueValue by int("Blue", 255, 0..255)

    val scaleWidth by float("ScaleWidth", 0.7F, 0.3F..0.95F)
    
    val scaleHeight by float("ScaleHeight", 0.85F, 0.3F..0.95F)

    val accentColor: Color?
        get() {
            var c: Color? = Color(255, 255, 255, 255)
            when (colorModeValue.lowercase(Locale.getDefault())) {
                "custom" -> c = Color(colorRedValue, colorGreenValue, colorBlueValue)
                "fade" -> c = fade(Color(colorRedValue, colorGreenValue, colorBlueValue), 0, 100)
            }
            return c
        }
}
