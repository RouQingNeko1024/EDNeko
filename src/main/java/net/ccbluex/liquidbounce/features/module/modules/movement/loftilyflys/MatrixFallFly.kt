package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity

class MatrixFallFly(parent: LoftilyFly) : LoftilyFlyMode("MatrixFall", parent) {

    val height by float("MatrixFall-JumpHeight", 0.8f, 0.8f..0.995f)
    val boostHeight by float("MatrixFall-BoostHeight", 0.42f, 0f..0.42f)
    val speed by float("MatrixFall-BoostSpeed", 4.5f, 3.5f..10f)
    val timer by float("MatrixFall-Timer", 0.5f, 0.1f..1f)
    val autoDisable by boolean("MatrixFall-AutoDisable", false)

    private var ticks = 0
    private var active = false
    private var receiveFlag = false
    private var jumped = false
    private var canBoost = false

    override fun onDisable() {
        canBoost = false
        jumped = false
        receiveFlag = false
        active = false
        ticks = 0
        mc.timer.timerSpeed = 1.0f
    }

    override fun onMotion(event: MotionEvent) {
        if (ticks >= 1) {
            event.onGround = false
        }
    }

    override fun onPacketReceive(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            if (canBoost) {
                receiveFlag = true
            } else if (autoDisable) {
                parent.disableModule()
            }
        }
        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            event.cancelEvent()
        }
    }

    override fun onUpdate() {
        mc.timer.timerSpeed = timer
        if (mc.thePlayer.isCollidedVertically) {
            if (!mc.thePlayer.isMoving) {
                setSpeed(0.15, false)
            }
            active = true
        }
        if (ticks == 1) {
            mc.thePlayer.motionY = height.toDouble()
            mc.thePlayer.onGround = false
        }
        if (active) {
            ++ticks
        }
        if (!mc.thePlayer.onGround) {
            if (active) {
                jumped = true
            }
        } else if (jumped) {
            canBoost = true
        }
        if (canBoost) {
            setSpeed(speed.toDouble(), false)
            mc.thePlayer.motionY = boostHeight.toDouble()
        }
        if (receiveFlag) {
            if (autoDisable) {
                parent.disableModule()
            } else {
                onDisable()
            }
        }
    }
}