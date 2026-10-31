package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.minecraft.network.play.client.C03PacketPlayer

class ClipFly(parent: LoftilyFly) : LoftilyFlyMode("Clip", parent) {

    val xValue by float("X", 0f, -10f..10f)
    val yValue by float("Y", 0f, -10f..10f)
    val zValue by float("Z", 0f, -10f..10f)
    val delay by float("Delay", 0f, 0f..5000f)
    val timerValue by float("Timer", 1.0f, 0.1f..3f)
    val resetXZ by boolean("ResetXZ", false)
    val resetY by boolean("ResetY", false)
    val spoofGround by boolean("SpoofGround", false)
    val spoofGroundOnlyClip by boolean("SpoofGroundOnlyClip", false) { spoofGround }

    private var lastClipTime = now()
    private var clipped = false

    private fun now() = System.currentTimeMillis()

    override fun onEnable() {
        clipped = false
        lastClipTime = now()
        mc.timer.timerSpeed = 1.0f
    }

    override fun onPacketSend(event: PacketEvent) {
        val packet = event.packet
        if (packet is C03PacketPlayer && spoofGround && (!spoofGroundOnlyClip || clipped)) {
            packet.onGround = true
            clipped = false
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST) {
            return
        }
        val player = mc.thePlayer
        mc.timer.timerSpeed = timerValue
        if (System.currentTimeMillis() - lastClipTime >= delay.toLong()) {
            val yaw = Math.toRadians(player.rotationYaw.toDouble())
            player.setPosition(
                player.posX + -Math.sin(yaw) * xValue.toDouble(),
                player.posY + yValue.toDouble(),
                player.posZ + Math.cos(yaw) * zValue.toDouble()
            )
            lastClipTime = System.currentTimeMillis()
            clipped = true
        }
        if (resetXZ) {
            stopMovement(false)
        }
        if (resetY) {
            player.motionY = 0.0
        }
    }
}