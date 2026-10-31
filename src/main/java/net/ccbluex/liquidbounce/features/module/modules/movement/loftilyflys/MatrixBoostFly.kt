package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class MatrixBoostFly(parent: LoftilyFly) : LoftilyFlyMode("MatrixBoost", parent) {

    val jumpDamage by boolean("JumpDamage", false)
    val speedF by float("Matrix-Speed", 1.0f, 0.42f..7f)
    val height by float("Matrix-Height", 1.0f, 0.42f..7f)

    private var jumpCounter = 0
    private var receivedFlag = false
    private var canBoost = false
    private var lastMotionX = 0.0
    private var lastMotionY = 0.0
    private var lastMotionZ = 0.0

    override fun onEnable() {
        jumpCounter = 0
        canBoost = false
        receivedFlag = false
    }

    override fun onUpdate() {
        val player = mc.thePlayer
        if (canBoost) {
            setSpeed(speedF.toDouble(), false)
            player.motionY = height.toDouble()
        }
        if (player.hurtTime in 1..8) {
            if (player.onGround) {
                player.tryJump()
            } else if (player.motionY < 0.2) {
                canBoost = true
            }
        }
        if (jumpCounter < 4 && jumpDamage && player.onGround) {
            player.tryJump()
            ++jumpCounter
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (jumpCounter < 4 && jumpDamage) {
            event.onGround = false
        }
    }

    override fun onPacketSend(event: PacketEvent) {
        if (event.packet is C03PacketPlayer.C06PacketPlayerPosLook && receivedFlag) {
            receivedFlag = false
            val player = mc.thePlayer
            player.motionX = lastMotionX
            player.motionY = lastMotionY
            player.motionZ = lastMotionZ
            parent.disableModule()
        }
    }

    override fun onPacketReceive(event: PacketEvent) {
        if (canBoost && event.packet is S08PacketPlayerPosLook) {
            receivedFlag = true
            val player = mc.thePlayer
            lastMotionX = player.motionX
            lastMotionY = player.motionY
            lastMotionZ = player.motionZ
            canBoost = false
        }
    }
}