/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.client.mainmenu

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import java.awt.Color
import java.util.*
import kotlin.math.*

class ParticleEngine : MinecraftInstance {
    private val particles = mutableListOf<Particle>()
    private var prevWidth = 0
    private var prevHeight = 0
    
    init {
        create()
    }
    
    fun render() {
        if (particles.isEmpty() || prevWidth != mc.displayWidth || prevHeight != mc.displayHeight) {
            particles.clear()
            create()
        }
        
        prevWidth = mc.displayWidth
        prevHeight = mc.displayHeight
        
        for (particle in particles) {
            particle.fall()
            particle.interpolation()
            
            // Simple particle rendering without connections for now
            RenderUtils.drawFilledCircle(particle.x.toInt(), particle.y.toInt(), particle.size, Color(255, 255, 255, 180))
        }
    }
    
    private fun create() {
        val random = Random()
        for (i in 0 until 100) {
            particles.add(Particle(random.nextInt(mc.displayWidth).toFloat(), random.nextInt(mc.displayHeight).toFloat()))
        }
    }
    
    inner class Particle(var x: Float, var y: Float) {
        val size: Float = genRandom()
        private val ySpeed = Random().nextFloat() * 2
        private val xSpeed = Random().nextFloat() * 2
        
        fun interpolation() {
            for (n in 0..64) {
                val f = n / 64f
                val p1 = lint1(f)
                val p2 = lint2(f)
                
                if (p1 != p2) {
                    y -= f
                    x -= f
                }
            }
        }
        
        fun fall() {
            val scaledResolution = ScaledResolution(mc)
            y += ySpeed
            x += xSpeed
            
            if (y > mc.displayHeight.toFloat()) y = 1f
            if (x > mc.displayWidth.toFloat()) x = 1f
            if (x < 1) x = scaledResolution.scaledWidth.toFloat()
            if (y < 1) y = scaledResolution.scaledHeight.toFloat()
        }
        
        private fun lint1(f: Float): Float {
            return 1.02f * (1f - f) + f
        }
        
        private fun lint2(f: Float): Float {
            return 1.02f + f * (1.0f - 1.02f)
        }
        
        private fun genRandom(): Float {
            return (0.3f + Math.random() * 0.3f).toFloat()
        }
    }
}