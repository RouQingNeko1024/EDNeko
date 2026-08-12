/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin

object BlockPlaceFX : Module("BlockPlaceFX", Category.RENDER, gameDetecting = false) {

    private val colorMode by choices("ColorMode", arrayOf("Theme", "Custom", "Rainbow"), "Theme")
    private val placeColor by color("PlaceColor", Color(0, 255, 128)) { colorMode == "Custom" }
    private val waveRadius by float("WaveRadius", 1.5f, 0.5f..5f)
    private val waveDuration by int("WaveDuration", 1000, 500..3000)
    private val waveCount by int("WaveCount", 3, 1..10)
    private val ringWidth by float("RingWidth", 2f, 1f..5f)
    
    private data class PlaceEffect(
        val x: Double,
        val y: Double,
        val z: Double,
        val startTime: Long,
        val color: Color
    )
    
    private val placeEffects = CopyOnWriteArrayList<PlaceEffect>()

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is C08PacketPlayerBlockPlacement) {
            val pos = packet.position
            if (pos != BlockPos.ORIGIN && packet.placedBlockDirection in 0..5) {
                val color = when (colorMode) {
                    "Theme" -> ClientThemesUtils.getColor()
                    "Rainbow" -> Color.getHSBColor((System.currentTimeMillis() % 3000) / 3000f, 0.7f, 1f)
                    else -> placeColor
                }
                
                placeEffects.add(PlaceEffect(
                    x = pos.x + 0.5,
                    y = pos.y.toDouble(),
                    z = pos.z + 0.5,
                    startTime = System.currentTimeMillis(),
                    color = color
                ))
            }
        }
    }

    val onRender3D = handler<Render3DEvent> {
        val currentTime = System.currentTimeMillis()
        val renderManager = mc.renderManager ?: return@handler
        val renderPosX = renderManager.viewerPosX
        val renderPosY = renderManager.viewerPosY
        val renderPosZ = renderManager.viewerPosZ
        
        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_DEPTH_TEST)
        
        placeEffects.removeIf { effect ->
            val elapsed = currentTime - effect.startTime
            if (elapsed > waveDuration) return@removeIf true
            
            renderWaveEffect(effect, renderPosX, renderPosY, renderPosZ, elapsed)
            false
        }
        
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_LIGHTING)
        glPopMatrix()
    }
    
    private fun renderWaveEffect(effect: PlaceEffect, rx: Double, ry: Double, rz: Double, elapsed: Long) {
        val progress = elapsed.toFloat() / waveDuration
        val alpha = 1f - progress
        
        glLineWidth(ringWidth)
        
        for (ring in 0 until waveCount) {
            val ringProgress = (progress + ring * 0.1f).coerceIn(0f, 1f)
            val ringRadius = waveRadius * ringProgress
            val ringAlpha = (1f - ringProgress) * alpha
            
            if (ringAlpha <= 0) continue
            
            glColor4f(effect.color.red / 255f, effect.color.green / 255f, effect.color.blue / 255f, ringAlpha)
            
            glBegin(GL_LINE_LOOP)
            
            val segments = 32
            for (i in 0 until segments) {
                val angle = (i.toFloat() / segments) * Math.PI * 2
                val x = effect.x + cos(angle) * ringRadius - rx
                val z = effect.z + sin(angle) * ringRadius - rz
                glVertex3d(x, effect.y - ry + 0.01, z)
            }
            
            glEnd()
        }
        
        glBegin(GL_POINTS)
        for (i in 0 until 16) {
            val angle = (i.toFloat() / 16) * Math.PI * 2
            val currentRadius = waveRadius * progress
            val sparkleAlpha = alpha * (0.5f + 0.5f * sin(System.currentTimeMillis() * 0.01 + i).toFloat())
            glColor4f(1f, 1f, 1f, sparkleAlpha)
            val x = effect.x + cos(angle) * currentRadius - rx
            val z = effect.z + sin(angle) * currentRadius - rz
            glVertex3d(x, effect.y - ry + 0.02, z)
        }
        glEnd()
    }

    override fun onDisable() {
        placeEffects.clear()
    }
}
