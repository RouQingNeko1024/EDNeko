/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.minecraft.block.Block
import net.minecraft.util.BlockPos
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin

object BlockBreakFX : Module("BlockBreakFX", Category.RENDER, gameDetecting = false) {

    private val effectMode by choices("EffectMode", arrayOf("Fire", "Pixels", "Sparkle", "Smoke", "Lightning"), "Pixels")
    private val colorMode by choices("ColorMode", arrayOf("Theme", "Custom", "BlockBased"), "Theme")
    private val effectColor by color("EffectColor", Color(255, 165, 0)) { colorMode == "Custom" }
    private val particleCount by int("ParticleCount", 30, 10..100)
    private val effectDuration by int("EffectDuration", 1000, 500..3000)
    private val particleSpeed by float("ParticleSpeed", 0.1f, 0.01f..0.5f)
    private val particleSize by float("ParticleSize", 3f, 1f..10f)
    
    private data class BreakParticle(
        var x: Double,
        var y: Double,
        var z: Double,
        val motionX: Double,
        var motionY: Double,
        val motionZ: Double,
        var alpha: Float,
        val color: Color,
        val startTime: Long
    )
    
    private data class BreakEffect(
        val x: Double,
        val y: Double,
        val z: Double,
        val particles: MutableList<BreakParticle>,
        val startTime: Long,
        val blockColor: Color
    )
    
    private val breakEffects = CopyOnWriteArrayList<BreakEffect>()
    private val random = Random()

    private fun getBlockColor(block: Block): Color {
        return try {
            val material = block.material
            when {
                material == net.minecraft.block.material.Material.rock -> Color(128, 128, 128)
                material == net.minecraft.block.material.Material.grass -> Color(86, 176, 0)
                material == net.minecraft.block.material.Material.ground -> Color(134, 96, 67)
                material == net.minecraft.block.material.Material.wood -> Color(136, 109, 64)
                material == net.minecraft.block.material.Material.sand -> Color(219, 206, 156)
                material == net.minecraft.block.material.Material.glass -> Color(200, 220, 255)
                else -> Color(180, 180, 180)
            }
        } catch (e: Exception) {
            Color(180, 180, 180)
        }
    }

    val onBlockBreak = handler<BlockBreakEvent> { event ->
        val blockColor = getBlockColor(event.block)
        val pos = event.blockPos
        
        val effect = BreakEffect(
            x = pos.x + 0.5,
            y = pos.y + 0.5,
            z = pos.z + 0.5,
            particles = mutableListOf(),
            startTime = System.currentTimeMillis(),
            blockColor = blockColor
        )
        
        for (i in 0 until particleCount) {
            val color = when (colorMode) {
                "Theme" -> ClientThemesUtils.getColor()
                "BlockBased" -> blockColor
                else -> effectColor
            }
            
            val angle = random.nextDouble() * Math.PI * 2
            val speed = random.nextDouble() * particleSpeed
            val upSpeed = random.nextDouble() * particleSpeed * 2
            
            effect.particles.add(BreakParticle(
                x = effect.x + (random.nextDouble() - 0.5) * 0.5,
                y = effect.y + (random.nextDouble() - 0.5) * 0.5,
                z = effect.z + (random.nextDouble() - 0.5) * 0.5,
                motionX = cos(angle) * speed,
                motionY = upSpeed,
                motionZ = sin(angle) * speed,
                alpha = 1f,
                color = color,
                startTime = System.currentTimeMillis()
            ))
        }
        
        breakEffects.add(effect)
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
        glPointSize(particleSize)
        
        breakEffects.removeIf { effect ->
            val elapsed = currentTime - effect.startTime
            if (elapsed > effectDuration) return@removeIf true
            
            when (effectMode) {
                "Fire" -> renderFireEffect(effect, renderPosX, renderPosY, renderPosZ, currentTime)
                "Pixels" -> renderPixelsEffect(effect, renderPosX, renderPosY, renderPosZ)
                "Sparkle" -> renderSparkleEffect(effect, renderPosX, renderPosY, renderPosZ, currentTime)
                "Smoke" -> renderSmokeEffect(effect, renderPosX, renderPosY, renderPosZ)
                "Lightning" -> renderLightningEffect(effect, renderPosX, renderPosY, renderPosZ)
            }
            
            false
        }
        
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_LIGHTING)
        glPopMatrix()
    }
    
    private fun renderFireEffect(effect: BreakEffect, rx: Double, ry: Double, rz: Double, time: Long) {
        glBegin(GL_POINTS)
        
        effect.particles.forEach { particle ->
            val elapsed = (time - particle.startTime) / 1000.0
            particle.y += particle.motionY * 0.5
            particle.x += particle.motionX * 0.3
            particle.z += particle.motionZ * 0.3
            particle.alpha = (1f - elapsed / (effectDuration / 1000.0)).toFloat().coerceIn(0f, 1f)
            
            if (particle.alpha > 0) {
                val flicker = (sin(time * 0.01 + particle.x) + 1).toFloat() * 0.2f
                glColor4f(1f, 0.3f + flicker, 0f, particle.alpha)
                glVertex3d(particle.x - rx, particle.y - ry, particle.z - rz)
            }
        }
        
        glEnd()
    }
    
    private fun renderPixelsEffect(effect: BreakEffect, rx: Double, ry: Double, rz: Double) {
        glBegin(GL_POINTS)
        
        effect.particles.forEach { particle ->
            particle.x += particle.motionX
            particle.y += particle.motionY
            particle.z += particle.motionZ
            particle.motionY -= 0.01
            particle.alpha -= 0.02f
            
            if (particle.alpha > 0) {
                glColor4f(particle.color.red / 255f, particle.color.green / 255f, particle.color.blue / 255f, particle.alpha)
                glVertex3d(particle.x - rx, particle.y - ry, particle.z - rz)
            }
        }
        
        glEnd()
    }
    
    private fun renderSparkleEffect(effect: BreakEffect, rx: Double, ry: Double, rz: Double, time: Long) {
        glBegin(GL_POINTS)
        
        effect.particles.forEach { particle ->
            val sparkle = sin(time * 0.005 + particle.x * 10).toFloat()
            particle.alpha = (sparkle + 1) * 0.5f * (1f - (time - particle.startTime) / effectDuration.toFloat())
            
            if (particle.alpha > 0) {
                glColor4f(1f, 1f, 1f, particle.alpha.coerceIn(0f, 1f))
                glVertex3d(particle.x - rx, particle.y - ry, particle.z - rz)
            }
        }
        
        glEnd()
    }
    
    private fun renderSmokeEffect(effect: BreakEffect, rx: Double, ry: Double, rz: Double) {
        glBegin(GL_POINTS)
        
        effect.particles.forEach { particle ->
            particle.y += 0.02
            particle.x += particle.motionX * 0.5
            particle.z += particle.motionZ * 0.5
            particle.alpha -= 0.01f
            
            if (particle.alpha > 0) {
                glColor4f(0.5f, 0.5f, 0.5f, particle.alpha * 0.5f)
                glVertex3d(particle.x - rx, particle.y - ry, particle.z - rz)
            }
        }
        
        glEnd()
    }
    
    private fun renderLightningEffect(effect: BreakEffect, rx: Double, ry: Double, rz: Double) {
        val color = when (colorMode) {
            "Theme" -> ClientThemesUtils.getColor()
            "BlockBased" -> effect.blockColor
            else -> effectColor
        }
        
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
        glLineWidth(2f)
        
        effect.particles.forEach { particle ->
            glBegin(GL_LINES)
            
            var x = particle.x - rx
            var y = particle.y - ry
            var z = particle.z - rz
            
            for (i in 0 until 5) {
                val newX = x + (random.nextDouble() - 0.5) * 0.3
                val newY = y + random.nextDouble() * 0.3
                val newZ = z + (random.nextDouble() - 0.5) * 0.3
                
                glVertex3d(x, y, z)
                glVertex3d(newX, newY, newZ)
                
                x = newX
                y = newY
                z = newZ
            }
            
            glEnd()
        }
    }

    override fun onDisable() {
        breakEffects.clear()
    }
}
