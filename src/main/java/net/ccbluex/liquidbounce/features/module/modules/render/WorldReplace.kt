/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S2BPacketChangeGameState
import java.awt.Color

object WorldReplace : Module("WorldReplace", Category.RENDER, gameDetecting = false) {

    private val colorMode by choices("ColorMode", arrayOf("Theme", "Custom"), "Theme")
    private val skyColorSetting by color("SkyColor", Color(30, 30, 50)) { colorMode == "Custom" }
    private val fogDensity by float("FogDensity", 0.01f, 0f..0.1f)
    val xrayMode by boolean("XRayMode", true)
    private val clearWeather by boolean("ClearWeather", true)
    
    private var prevGammaLevel = 0f

    fun getSkyColorValue(): Color {
        return when (colorMode) {
            "Theme" -> ClientThemesUtils.getColor()
            else -> skyColorSetting
        }
    }

    override fun onEnable() {
        try {
            prevGammaLevel = mc.gameSettings?.gammaSetting ?: 0f
            mc.renderGlobal?.loadRenderers()
        } catch (e: Exception) {
        }
    }

    override fun onToggle(state: Boolean) {
        try {
            mc.renderGlobal?.loadRenderers()
        } catch (e: Exception) {
        }
    }

    override fun onDisable() {
        try {
            mc.gameSettings?.gammaSetting = prevGammaLevel
        } catch (e: Exception) {
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (xrayMode) {
            mc.gameSettings?.gammaSetting = 100f
        }
        
        if (clearWeather) {
            mc.theWorld?.setRainStrength(0f)
            mc.theWorld?.setThunderStrength(0f)
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if (clearWeather && packet is S2BPacketChangeGameState) {
            if (packet.gameState in 7..8) {
                event.cancelEvent()
            }
        }
    }

    fun shouldRenderBlock(blockState: net.minecraft.block.state.IBlockState): Boolean {
        if (!state || !xrayMode) return true
        val block = blockState.block
        return block == Blocks.air || 
               block == Blocks.water || 
               block == Blocks.flowing_water ||
               block == Blocks.lava ||
               block == Blocks.flowing_lava
    }
}
