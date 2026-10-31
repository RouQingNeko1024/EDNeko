package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.math.cos
import kotlin.math.sin

class GrimVerticalVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("GrimVertical", parent) {

    val ticks by int("Ticks", 3, 1..20)
    val yBoost by float("YBoost", 0.12f, 0f..1f)
    val waitMode by choices("WaitMode", arrayOf("None", "C06Timer", "C06", "CCDC"), "C06Timer")

    private var velocityTicks = 0
    private var shouldProcess = false
    private var motionX = .0
    private var motionZ = .0

    override fun onEnable() {
        velocityTicks = 0
        shouldProcess = false
    }

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        packet.realMotionY = (packet.realMotionY + yBoost).coerceAtLeast(0.0)
        motionX = packet.realMotionX
        motionZ = packet.realMotionZ
        shouldProcess = true
    }

    override fun onMotion(event: MotionEvent) {
        if (!shouldProcess) return
        if (event.eventState != EventState.PRE) return

        if (velocityTicks > 0) {
            velocityTicks--

            val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
            val speed = motionX * motionX + motionZ * motionZ

            when (waitMode) {
                "C06Timer" -> {
                    PacketUtils.sendPacket(C03PacketPlayer.C06PacketPlayerPosLook(
                        mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                        mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false
                    ), false)
                }
                "C06" -> {
                    mc.thePlayer.motionX = -sin(yaw) * speed
                    mc.thePlayer.motionZ = cos(yaw) * speed
                }
                "CCDC" -> {
                    PacketUtils.sendPacket(C03PacketPlayer.C06PacketPlayerPosLook(
                        mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                        mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false
                    ), false)
                    mc.thePlayer.motionX = -sin(yaw) * speed
                    mc.thePlayer.motionZ = cos(yaw) * speed
                }
            }

            if (velocityTicks == 0) {
                shouldProcess = false
            }
            return
        }

        velocityTicks = ticks
    }
}