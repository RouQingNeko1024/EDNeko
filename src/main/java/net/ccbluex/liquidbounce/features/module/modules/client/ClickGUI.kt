/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.LiquidBounce.clickGui
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.astolfo.AstolfoClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.augustus.AugustusClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.moonlight.MoonLightClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.neverlose.NeverloseScreen
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiScreen
import net.ccbluex.liquidbounce.ui.client.clickgui.rise.RiseClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.skeet.SkeetClickGui
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.BlackStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.GlassStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.LiquidBounceStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.MinimalStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.NeonStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.NullStyle
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle
import net.minecraft.network.play.server.S2EPacketCloseWindow
import org.lwjgl.input.Keyboard
import java.awt.Color

object ClickGUI : Module("ClickGUI", Category.CLIENT, Keyboard.KEY_RSHIFT, canBeEnabled = false) {
    private val style by choices(
        "Style",
        arrayOf("LiquidBounce", "Null", "Slowly", "Black", "Neon", "Minimal", "Glass", "Neverlose", "Augustus", "Opai", "Skeet", "Astolfo", "MoonLight", "Rise"),
        "LiquidBounce"
    ).onChanged {
        updateStyle()
    }
    var scale by float("Scale", 0.8f, 0.5f..1.5f)
    val maxElements by int("MaxElements", 15, 1..30)
    val fadeSpeed by float("FadeSpeed", 1f, 0.5f..4f)
    val scrolls by boolean("Scrolls", true)
    val spacedModules by boolean("SpacedModules", false)
    val panelsForcedInBoundaries by boolean("PanelsForcedInBoundaries", false)

    private val color by color("Color", Color(0, 160, 255)) { style !in arrayOf("Slowly", "Black", "Neverlose", "Augustus", "Opai", "Skeet", "Astolfo", "MoonLight", "Rise") }

    val guiColor
        get() = color.rgb

    override fun onEnable() {
        openSelectedStyle()
        Keyboard.enableRepeatEvents(true)
    }

    private fun openSelectedStyle() {
        when (style) {
            "Neverlose" -> mc.displayGuiScreen(NeverloseScreen())
            "Augustus" -> mc.displayGuiScreen(AugustusClickGui())
            "Opai" -> mc.displayGuiScreen(OpaiScreen.INSTANCE)
            "Skeet" -> mc.displayGuiScreen(SkeetClickGui())
            "Astolfo" -> mc.displayGuiScreen(AstolfoClickGui())
            "MoonLight" -> mc.displayGuiScreen(MoonLightClickGui())
            "Rise" -> mc.displayGuiScreen(RiseClickGui())
            else -> {
                updateStyle()
                mc.displayGuiScreen(clickGui)
            }
        }
    }

    private fun updateStyle() {
        clickGui.style = when (style) {
            "LiquidBounce" -> LiquidBounceStyle
            "Null" -> NullStyle
            "Slowly" -> SlowlyStyle
            "Black" -> BlackStyle
            "Neon" -> NeonStyle
            "Minimal" -> MinimalStyle
            "Glass" -> GlassStyle
            else -> return
        }
    }

    val onPacket = handler<PacketEvent>(always = true) { event ->
        if (event.packet is S2EPacketCloseWindow && mc.currentScreen is ClickGui) {
            event.cancelEvent()
        }
    }
}
