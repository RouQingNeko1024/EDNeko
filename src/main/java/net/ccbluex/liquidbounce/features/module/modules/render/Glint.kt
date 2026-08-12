/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import java.awt.Color

object Glint : Module("Glint", Category.RENDER, gameDetecting = false) {

    private val mode by choices("Mode", arrayOf("Client", "Rainbow", "Custom"), "Custom")
    
    private val color = ColorSettingsInteger(this, "Color") { mode == "Custom" }.with(255, 0, 0)
    
    private val rainbowSpeed by float("RainbowSpeed", 0.5f, 0.1f..2f) { mode == "Rainbow" }
    private val rainbowSaturation by float("RainbowSaturation", 0.9f, 0.1f..1f) { mode == "Rainbow" }
    private val rainbowBrightness by float("RainbowBrightness", 1f, 0.1f..1f) { mode == "Rainbow" }
    
    val intensity by float("Intensity", 0.5f, 0.1f..1f)
    
    val targetItems by choices("TargetItems", arrayOf("All", "Swords", "Enchanted"), "Enchanted")

    fun getColor(): Color {
        return when (mode.lowercase()) {
            "client" -> ClientThemesUtils.getColor(1)
            "rainbow" -> {
                val time = System.currentTimeMillis() * rainbowSpeed * 0.001f
                Color.getHSBColor((time % 360) / 360f, rainbowSaturation, rainbowBrightness)
            }
            else -> color.color()
        }
    }
}
