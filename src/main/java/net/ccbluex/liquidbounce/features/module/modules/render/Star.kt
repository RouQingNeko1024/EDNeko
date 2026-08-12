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
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

object Star : Module("Star", Category.RENDER, gameDetecting = false) {

    private val colorMode by choices("ColorMode", arrayOf("Theme", "Custom", "Rainbow"), "Theme")
    private val starColor by color("StarColor", Color(255, 255, 255)) { colorMode == "Custom" }
    private val starSize by float("StarSize", 2f, 0.5f..5f)
    private val twinkleSpeed by float("TwinkleSpeed", 1f, 0.1f..5f)
    private val starCount by int("StarCount", 200, 50..1000)
    private val renderHeight by float("RenderHeight", 80f, 30f..200f)
    private val renderDistance by float("RenderDistance", 100f, 50f..300f)
    private val twinkle by boolean("Twinkle", true)
    
    private data class StarData(
        val offsetX: Double,
        val offsetZ: Double,
        val height: Double,
        val phase: Float
    )
    
    private var stars = mutableListOf<StarData>()
    private var generated = false
    private val random = Random()

    override fun onEnable() {
        generateStars()
    }

    private fun generateStars() {
        stars.clear()
        for (i in 0 until starCount) {
            val angle = random.nextDouble() * Math.PI * 2
            val distance = random.nextDouble() * renderDistance
            val height = renderHeight.toDouble() + random.nextDouble() * 50
            val phase = random.nextFloat() * (Math.PI * 2).toFloat()
            stars.add(StarData(
                cos(angle) * distance,
                sin(angle) * distance,
                height,
                phase
            ))
        }
        generated = true
    }

    val onRender3D = handler<Render3DEvent> {
        if (!generated || stars.size != starCount) {
            generateStars()
        }
        
        val player = mc.thePlayer ?: return@handler
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
        glEnable(GL_POINT_SMOOTH)
        glHint(GL_POINT_SMOOTH_HINT, GL_NICEST)
        glPointSize(starSize)
        
        val time = System.currentTimeMillis() / 1000f * twinkleSpeed
        
        glBegin(GL_POINTS)
        
        for (star in stars) {
            val x = player.posX + star.offsetX - renderPosX
            val y = star.height - renderPosY
            val z = player.posZ + star.offsetZ - renderPosZ
            
            val alpha = if (twinkle) {
                ((sin((time + star.phase).toDouble()) + 1.0) / 2.0 * 0.7 + 0.3).toFloat()
            } else {
                1f
            }
            
            val color = when (colorMode) {
                "Theme" -> ClientThemesUtils.getColor()
                "Rainbow" -> Color.getHSBColor((time * 0.05f + star.phase) % 1f, 0.5f, 1f)
                else -> starColor
            }
            
            glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, alpha)
            glVertex3d(x, y, z)
        }
        
        glEnd()
        
        glDisable(GL_POINT_SMOOTH)
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_LIGHTING)
        glPopMatrix()
    }
}
