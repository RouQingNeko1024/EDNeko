/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin

object DeathAnimation : Module("DeathAnimation", Category.RENDER, gameDetecting = false) {

    private val animationMode by choices("AnimationMode", arrayOf("Fade", "Particles", "Explode", "Soul", "Lightning"), "Fade")
    private val colorMode by choices("ColorMode", arrayOf("Theme", "Custom", "Rainbow"), "Theme")
    private val deathColor by color("DeathColor", Color(255, 0, 0)) { colorMode == "Custom" }
    private val fadeSpeed by float("FadeSpeed", 0.02f, 0.005f..0.1f)
    private val particleCount by int("ParticleCount", 50, 10..200) { animationMode == "Particles" || animationMode == "Explode" }
    private val soulSpeed by float("SoulSpeed", 0.5f, 0.1f..2f) { animationMode == "Soul" }
    
    private data class DeathEffect(
        val entityId: Int,
        val x: Double,
        val y: Double,
        val z: Double,
        val startTime: Long,
        var alpha: Float = 1f,
        val particles: MutableList<Particle> = mutableListOf()
    )
    
    private data class Particle(
        var x: Double,
        var y: Double,
        var z: Double,
        val motionX: Double,
        var motionY: Double,
        val motionZ: Double,
        var alpha: Float = 1f,
        val color: Color
    )
    
    private val deathEffects = CopyOnWriteArrayList<DeathEffect>()
    private val deadEntities = mutableSetOf<Int>()
    private val random = Random()

    val onRender3D = handler<Render3DEvent> {
        val currentTime = System.currentTimeMillis()
        val renderManager = mc.renderManager ?: return@handler
        val renderPosX = renderManager.viewerPosX
        val renderPosY = renderManager.viewerPosY
        val renderPosZ = renderManager.viewerPosZ
        
        mc.theWorld?.loadedEntityList?.filterIsInstance<EntityLivingBase>()?.forEach { entity ->
            if (entity.isDead && !deadEntities.contains(entity.entityId)) {
                deadEntities.add(entity.entityId)
                createDeathEffect(entity)
            }
        }
        
        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_DEPTH_TEST)
        
        deathEffects.removeIf { effect ->
            val elapsed = (currentTime - effect.startTime) / 1000f
            
            if (elapsed > 5f) {
                return@removeIf true
            }
            
            effect.alpha -= fadeSpeed * 0.05f
            
            when (animationMode) {
                "Fade" -> renderFadeEffect(effect, renderPosX, renderPosY, renderPosZ)
                "Particles" -> renderParticlesEffect(effect, renderPosX, renderPosY, renderPosZ)
                "Explode" -> renderExplodeEffect(effect, renderPosX, renderPosY, renderPosZ)
                "Soul" -> renderSoulEffect(effect, renderPosX, renderPosY, renderPosZ, elapsed)
                "Lightning" -> renderLightningEffect(effect, renderPosX, renderPosY, renderPosZ)
            }
            
            effect.alpha <= 0
        }
        
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_LIGHTING)
        glPopMatrix()
    }
    
    private fun createDeathEffect(entity: EntityLivingBase) {
        val effect = DeathEffect(
            entityId = entity.entityId,
            x = entity.posX,
            y = entity.posY + entity.height / 2,
            z = entity.posZ,
            startTime = System.currentTimeMillis()
        )
        
        if (animationMode == "Particles" || animationMode == "Explode") {
            for (i in 0 until particleCount) {
                val color = when (colorMode) {
                    "Theme" -> ClientThemesUtils.getColor()
                    "Rainbow" -> Color.getHSBColor(random.nextFloat(), 0.7f, 1f)
                    else -> deathColor
                }
                val angle = random.nextDouble() * Math.PI * 2
                val speed = if (animationMode == "Explode") random.nextDouble() * 0.3 + 0.1 else random.nextDouble() * 0.1
                effect.particles.add(Particle(
                    x = effect.x,
                    y = effect.y,
                    z = effect.z,
                    motionX = cos(angle) * speed,
                    motionY = (random.nextDouble() - 0.5) * speed,
                    motionZ = sin(angle) * speed,
                    color = color
                ))
            }
        }
        
        deathEffects.add(effect)
    }
    
    private fun getColor(): Color {
        return when (colorMode) {
            "Theme" -> ClientThemesUtils.getColor()
            "Rainbow" -> Color.getHSBColor((System.currentTimeMillis() % 3000) / 3000f, 0.7f, 1f)
            else -> deathColor
        }
    }
    
    private fun renderFadeEffect(effect: DeathEffect, rx: Double, ry: Double, rz: Double) {
        val color = getColor()
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, effect.alpha * 0.5f)
        
        glPointSize(10f)
        glBegin(GL_POINTS)
        glVertex3d(effect.x - rx, effect.y - ry, effect.z - rz)
        glEnd()
    }
    
    private fun renderParticlesEffect(effect: DeathEffect, rx: Double, ry: Double, rz: Double) {
        glPointSize(3f)
        glBegin(GL_POINTS)
        
        effect.particles.forEach { particle ->
            particle.x += particle.motionX
            particle.y += particle.motionY
            particle.z += particle.motionZ
            particle.motionY -= 0.01
            particle.alpha -= 0.02f
            
            if (particle.alpha > 0) {
                glColor4f(particle.color.red / 255f, particle.color.green / 255f, particle.color.blue / 255f, particle.alpha * effect.alpha)
                glVertex3d(particle.x - rx, particle.y - ry, particle.z - rz)
            }
        }
        
        glEnd()
    }
    
    private fun renderExplodeEffect(effect: DeathEffect, rx: Double, ry: Double, rz: Double) {
        glPointSize(4f)
        glBegin(GL_POINTS)
        
        effect.particles.forEach { particle ->
            particle.x += particle.motionX * 2
            particle.y += particle.motionY * 2
            particle.z += particle.motionZ * 2
            particle.alpha -= 0.03f
            
            if (particle.alpha > 0) {
                glColor4f(particle.color.red / 255f, particle.color.green / 255f, particle.color.blue / 255f, particle.alpha * effect.alpha)
                glVertex3d(particle.x - rx, particle.y - ry, particle.z - rz)
            }
        }
        
        glEnd()
    }
    
    private fun renderSoulEffect(effect: DeathEffect, rx: Double, ry: Double, rz: Double, elapsed: Float) {
        val color = getColor()
        val yOffset = elapsed * soulSpeed
        
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, effect.alpha * 0.7f)
        glPointSize(5f)
        glBegin(GL_POINTS)
        
        for (i in 0 until 20) {
            val angle = (i / 20f) * Math.PI * 2 + elapsed * 2
            val radius = 0.3
            val x = effect.x + cos(angle) * radius - rx
            val y = effect.y + yOffset + sin(elapsed * 3 + i) * 0.1 - ry
            val z = effect.z + sin(angle) * radius - rz
            glVertex3d(x, y, z)
        }
        
        glEnd()
    }
    
    private fun renderLightningEffect(effect: DeathEffect, rx: Double, ry: Double, rz: Double) {
        val color = getColor()
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, effect.alpha)
        glLineWidth(2f)
        
        glBegin(GL_LINES)
        
        var x = effect.x - rx
        var y = effect.y - ry
        var z = effect.z - rz
        
        for (i in 0 until 10) {
            val newX = x + (random.nextDouble() - 0.5) * 0.5
            val newY = y + random.nextDouble() * 0.5
            val newZ = z + (random.nextDouble() - 0.5) * 0.5
            
            glVertex3d(x, y, z)
            glVertex3d(newX, newY, newZ)
            
            x = newX
            y = newY
            z = newZ
        }
        
        glEnd()
    }

    override fun onDisable() {
        deathEffects.clear()
        deadEntities.clear()
    }
}
