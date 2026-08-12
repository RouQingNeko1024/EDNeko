package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.animations.TimerUtils
import net.ccbluex.liquidbounce.utils.render.AttackParticle
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.LinkedList

object AttackEffect : Module("AttackEffect", Category.RENDER, spacedName = "Attack Effect") {
    private val amount by int("Amount", 8, 1..50)
    private val lifeTime by int("LifeTime", 1000, 100..5000)
    private val speed by float("Speed", 1.0f, 0.1f..5.0f)
    private val gravity by float("Gravity", 1.0f, 0.0f..3.0f)
    private val scale by float("Scale", 1.0f, 0.1f..3.0f)
    
    private val style by choices("Style", arrayOf("Triangle", "Circle", "Square", "Star", "Diamond"), "Triangle")
    
    private val colorMode by choices("Color", arrayOf("Custom", "Health", "Rainbow", "Sky", "Fade", "Mixer"), "Custom")
    private val colorRed by int("Red", 255, 0..255)
    private val colorGreen by int("Green", 255, 0..255)
    private val colorBlue by int("Blue", 255, 0..255)
    private val colorAlpha by int("Alpha", 255, 0..255)
    private val saturation by float("Saturation", 1F, 0F..1F)
    private val brightness by float("Brightness", 1F, 0F..1F)
    private val mixerSeconds by int("Seconds", 2, 1..10)

    private val particles = LinkedList<AttackParticle>()
    private val timer = TimerUtils()

    private var lastHurtTime = 0

    val onUpdate = handler<UpdateEvent> {
        val killAura = LiquidBounce.moduleManager[KillAura::class.java] as? KillAura ?: return@handler
        if (killAura.state) {
            val target = killAura.target
            if (target != null && target.hurtTime != 0 && target.hurtTime != lastHurtTime) {
                lastHurtTime = target.hurtTime
                for (i in 1..amount) {
                    val posX = target.posX + (Math.random() - 0.5) * 0.5
                    val posY = target.posY + Math.random() + 0.5
                    val posZ = target.posZ + (Math.random() - 0.5) * 0.5
                    val particle = AttackParticle(Vec3(posX, posY, posZ), speed, gravity)
                    particle.maxAge = lifeTime.toLong()
                    particles.add(particle)
                }
            }
        }
    }

    val onRender3D = handler<Render3DEvent> {
        if (particles.isEmpty()) {
            return@handler
        }

        var i = 0
        while (i.toDouble() <= timer.time.toDouble() / 1.0E11) {
            particles.forEach(AttackParticle::updateWithoutPhysics)
            i++
        }

        particles.removeIf { particle ->
            particle.isDead() || mc.thePlayer.getDistanceSq(
                particle.position.xCoord,
                particle.position.yCoord,
                particle.position.zCoord
            ) > 300.0
        }

        timer.reset()
        renderParticles(particles, getColor())
    }

    private fun renderParticles(particles: List<AttackParticle>, color: Color) {
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_LINE_SMOOTH)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        var i = 0

        try {
            for (particle in particles) {
                val v = particle.position
                var draw = true
                val x = v.xCoord - mc.renderManager.renderPosX
                val y = v.yCoord - mc.renderManager.renderPosY
                val z = v.zCoord - mc.renderManager.renderPosZ
                val distanceFromPlayer = mc.thePlayer.getDistance(v.xCoord, v.yCoord - 1.0, v.zCoord)
                
                val bb = net.minecraft.util.AxisAlignedBB(v.xCoord - 0.1, v.yCoord - 0.1, v.zCoord - 0.1, v.xCoord + 0.1, v.yCoord + 0.1, v.zCoord + 0.1)
                if (!RenderUtils.isBBInFrustum(bb)) {
                    draw = false
                }
                if (i % 10 != 0 && distanceFromPlayer > 25.0) {
                    draw = false
                }
                if (i % 3 == 0 && distanceFromPlayer > 15.0) {
                    draw = false
                }
                if (!draw) {
                    i++
                    continue
                }

                val lifeProgress = particle.age.toFloat() / particle.maxAge.toFloat()
                val alpha = (colorAlpha * (1f - lifeProgress * 0.5f)).toInt().coerceIn(0, 255)
                val particleColor = Color(color.red, color.green, color.blue, alpha)

                glPushMatrix()
                glTranslated(x, y, z)
                glScalef(-0.04f * scale, -0.04f * scale, -0.04f * scale)
                glRotated(-mc.renderManager.playerViewY.toDouble(), 0.0, 1.0, 0.0)
                glRotated(mc.renderManager.playerViewX.toDouble(), if (mc.gameSettings.thirdPersonView == 2) -1.0 else 1.0, 0.0, 0.0)

                when (style) {
                    "Triangle" -> drawTriangle(particleColor)
                    "Circle" -> drawCircle(particleColor)
                    "Square" -> drawSquare(particleColor)
                    "Star" -> drawStar(particleColor)
                    "Diamond" -> drawDiamond(particleColor)
                }

                if (distanceFromPlayer < 4.0) {
                    val glowColor = Color(particleColor.red, particleColor.green, particleColor.blue, 50)
                    when (style) {
                        "Triangle" -> drawTriangle(glowColor)
                        "Circle" -> drawCircle(glowColor)
                        "Square" -> drawSquare(glowColor)
                        "Star" -> drawStar(glowColor)
                        "Diamond" -> drawDiamond(glowColor)
                    }
                }

                glPopMatrix()
                i++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
    }
    
    private fun drawTriangle(color: Color) {
        RenderUtils.glColor(color)
        glBegin(GL_TRIANGLES)
        glVertex2d(0.0, -1.5)
        glVertex2d(-1.0, 0.5)
        glVertex2d(1.0, 0.5)
        glEnd()
    }
    
    private fun drawCircle(color: Color) {
        RenderUtils.glColor(color)
        glBegin(GL_TRIANGLE_FAN)
        glVertex2d(0.0, 0.0)
        for (i in 0..360 step 10) {
            val rad = Math.toRadians(i.toDouble())
            glVertex2d(Math.cos(rad) * 1.0, Math.sin(rad) * 1.0)
        }
        glEnd()
    }
    
    private fun drawSquare(color: Color) {
        RenderUtils.glColor(color)
        glBegin(GL_QUADS)
        glVertex2d(-1.0, -1.0)
        glVertex2d(1.0, -1.0)
        glVertex2d(1.0, 1.0)
        glVertex2d(-1.0, 1.0)
        glEnd()
    }
    
    private fun drawStar(color: Color) {
        RenderUtils.glColor(color)
        glBegin(GL_TRIANGLE_FAN)
        glVertex2d(0.0, 0.0)
        for (i in 0..360 step 36) {
            val rad = Math.toRadians(i.toDouble())
            val r = if (i % 72 == 0) 1.5 else 0.6
            glVertex2d(Math.cos(rad) * r, Math.sin(rad) * r)
        }
        glEnd()
    }
    
    private fun drawDiamond(color: Color) {
        RenderUtils.glColor(color)
        glBegin(GL_QUADS)
        glVertex2d(0.0, -1.5)
        glVertex2d(1.0, 0.0)
        glVertex2d(0.0, 1.5)
        glVertex2d(-1.0, 0.0)
        glEnd()
    }

    private fun getColor(): Color {
        return when (colorMode) {
            "Custom" -> Color(colorRed, colorGreen, colorBlue)
            "Health" -> ColorUtils.getHealthColor(mc.thePlayer.health, mc.thePlayer.maxHealth)
            "Rainbow" -> ColorUtils.rainbow(alpha = 1f)
            "Sky" -> ColorUtils.skyRainbow(0, saturation, brightness, 1f)
            "Fade" -> ColorUtils.fade(Color(colorRed, colorGreen, colorBlue), 0, 1)
            "Mixer" -> getMixedColor(0, mixerSeconds)
            else -> Color.WHITE
        }
    }
}
