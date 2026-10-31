package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class Matrix7158(parent: LoftilyFly) : LoftilyFlyMode("Matrix7158", parent) {

    val motion by float("Motion", 1.97f, 0f..2f)
    val timer1 by float("TPTimer", 0.5f, 0.1f..1f)
    val timer2 by float("BoostTimer", 0.9f, 0.1f..1f)
    val xzOffset by float("XZOffset", 50f, -100f..100f)
    val yOffset by float("YOffset", 50f, -100f..100f)

    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var teleported = false
    private var canBoost = false
    private var boosted = false
    private var holy = false

    override fun onEnable() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.tryJump()
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
        teleported = false
        canBoost = false
        boosted = false
        holy = false
    }

    override fun onUpdate() {
        val player = mc.thePlayer
        if (canBoost) {
            setSpeed(motion.toDouble(), false)
            player.motionY = 0.42
            boosted = true
            mc.timer.timerSpeed = timer2
        }
        if (teleported && !canBoost && player.motionY <= -0.2) {
            teleported = false
            boosted = false
        }
    }

    override fun onPacketSend(event: PacketEvent) {
        if (event.packet is C03PacketPlayer.C06PacketPlayerPosLook && boosted && holy) {
            mc.thePlayer.motionX = x
            mc.thePlayer.motionY = y
            mc.thePlayer.motionZ = z
            holy = false
        }
    }

    override fun onPacketReceive(event: PacketEvent) {
        if (event.packet !is S08PacketPlayerPosLook) {
            return
        }
        val player = mc.thePlayer
        if (boosted) {
            canBoost = false
            holy = true
            x = player.motionX
            y = player.motionY
            z = player.motionZ
        }
        if (!teleported) {
            teleported = true
            canBoost = true
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (!teleported) {
            mc.timer.timerSpeed = timer1
            event.x += xzOffset.toDouble()
            event.y += yOffset.toDouble()
            event.z += xzOffset.toDouble()
        }
    }
}