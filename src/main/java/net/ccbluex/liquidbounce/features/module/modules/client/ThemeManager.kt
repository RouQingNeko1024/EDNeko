/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.theme.ThemeSelector
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import org.lwjgl.input.Keyboard
import java.awt.Color

object ThemeManager : Module("ThemeManager", Category.CLIENT, Keyboard.KEY_NONE, canBeEnabled = false) {

    private val themeValue = text("Theme", "MoonPurple").onChanged { syncToClientThemesUtils() }
    private val themeFadeSpeedValue = int("ThemeFadeSpeed", 7, 1..10).onChanged { syncToClientThemesUtils() }
    private val updownValue = boolean("UpDown", false).onChanged { syncToClientThemesUtils() }
    private val backgroundModeValue = choices("BackgroundMode", arrayOf("Synced", "Dark", "Custom", "Neverlose", "None"), "Synced").onChanged { syncToClientThemesUtils() }
    
    private val panelColorRed by int("PanelColorRed", 35, 0..255)
    private val panelColorGreen by int("PanelColorGreen", 35, 0..255)
    private val panelColorBlue by int("PanelColorBlue", 35, 0..255)
    private val panelColorAlpha by int("PanelColorAlpha", 200, 0..255)

    val theme: String
        get() = themeValue.get()

    val panelColor: Color
        get() = Color(panelColorRed, panelColorGreen, panelColorBlue, panelColorAlpha)

    val themeFadeSpeed: Int
        get() = themeFadeSpeedValue.get()
    
    val updown: Boolean
        get() = updownValue.get()
    
    val backgroundMode: String
        get() = backgroundModeValue.get()

    override fun onEnable() {
        mc.displayGuiScreen(ThemeSelector.getInstance())
    }
    
    fun syncToClientThemesUtils() {
        ClientThemesUtils.ClientColorMode = themeValue.get()
        ClientThemesUtils.ThemeFadeSpeed = themeFadeSpeedValue.get()
        ClientThemesUtils.updown = updownValue.get()
        ClientThemesUtils.BackgroundMode = backgroundModeValue.get()
    }
    
    fun setTheme(newTheme: String) {
        themeValue.set(newTheme)
        ClientThemesUtils.ClientColorMode = newTheme
    }
    
    fun getCurrentTheme(): String = themeValue.get()
}