package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer

object RiseGrim2 : SpeedMode("RiseGrim2") {

    private var strafeTicks = 0
    private var groundTicks = 0
    private var shouldJump = false

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        groundTicks = if (player.onGround) groundTicks + 1 else 0

        if (strafeTicks > -1) {
            val d3 = if (strafeTicks % 2 == 0) {
                if (player.onGround) 0.085 else 0.03
            } else {
                0.03
            }
            moveFlying(d3 * Speed.riseGrim2Speed)
        }
        strafeTicks++
    }

    override fun onMotion() {
        val player = mc.thePlayer ?: return

        if (strafeTicks % 2 == 0) {
            if (!Speed.riseGrim2HighPing) {
                mc.netHandler.addToSendQueue(C03PacketPlayer(true))
                mc.netHandler.addToSendQueue(C03PacketPlayer(false))
            } else {
                mc.netHandler.addToSendQueue(C03PacketPlayer(false))
                mc.netHandler.addToSendQueue(C03PacketPlayer(false))
            }
        }

        if (shouldJump) {
            player.tryJump()
            shouldJump = false
        }
    }

    override fun onPacket(event: net.ccbluex.liquidbounce.event.PacketEvent) {
        val packet = event.packet

        if (packet is net.minecraft.network.play.server.S08PacketPlayerPosLook) {
            if (strafeTicks % 2 == 1) {
                strafeTicks++
            }
            mc.timer.timerSpeed = 1f
        }

        if (packet is net.minecraft.network.play.server.S12PacketEntityVelocity) {
            if (packet.entityID == mc.thePlayer?.entityId) {
                shouldJump = true
            }
        }
    }

    private fun moveFlying(amount: Double) {
        val player = mc.thePlayer ?: return
        if (!player.isMoving) return
        val yaw = MovementUtils.direction
        player.motionX += -Math.sin(yaw) * amount
        player.motionZ += Math.cos(yaw) * amount
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    override fun onEnable() {
        if (!Speed.riseGrim2HighPing) {
            mc.netHandler.addToSendQueue(C03PacketPlayer(true))
            mc.netHandler.addToSendQueue(C03PacketPlayer(false))
        } else {
            mc.netHandler.addToSendQueue(C03PacketPlayer(false))
            mc.netHandler.addToSendQueue(C03PacketPlayer(false))
        }
        strafeTicks = 0
        groundTicks = 0
        mc.timer.timerSpeed = 1f
    }
}