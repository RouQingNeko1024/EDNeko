package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiScreen
import org.lwjgl.input.Keyboard
import java.awt.Color

object OpaiGUI : Module("OpaiGUI", Category.CLIENT, Keyboard.KEY_RSHIFT, canBeEnabled = false, gameDetecting = false) {

    val style by choices("Style", arrayOf("Style1", "Style2"), "Style1")
    
    val primaryColor by color("PrimaryColor", Color(0, 160, 255))
    val enabledModuleColor by color("EnabledModuleColor", Color(0, 160, 255))
    val sliderColor by color("SliderColor", Color(0, 160, 255))
    val sliderKnobColor by color("SliderKnobColor", Color(0, 160, 255))
    
    val backgroundBlur by float("BackgroundBlur", 15f, 0f..30f)
    val animSpeed by float("AnimSpeed", 1f, 0.5f..3f)
    
    val panelWidth by float("PanelWidth", 130f, 80f..200f)
    val panelHeight by float("PanelHeight", 350f, 200f..500f)
    val panelRadius by float("PanelRadius", 12f, 0f..20f)
    val panelGap by float("PanelGap", 6f, 2f..30f)
    
    val headerHeight by float("HeaderHeight", 28f, 20f..40f)
    val settingHeight by float("SettingHeight", 22f, 16f..32f)
    val textSize by float("TextSize", 1f, 0.6f..1.5f)

    fun getAnimTime(baseMs: Long): Long = (baseMs / animSpeed).toLong()

    override fun onEnable() {
        if (mc.currentScreen is OpaiScreen) {
            mc.displayGuiScreen(null)
        } else {
            mc.displayGuiScreen(OpaiScreen.INSTANCE)
        }
    }

}
