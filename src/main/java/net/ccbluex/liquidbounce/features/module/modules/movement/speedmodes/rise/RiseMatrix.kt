package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion

object RiseMatrix : SpeedMode("RiseMatrix") {

    private var ticksSinceSneak = 0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        val isOnGrid = player.posY % 0.015625 == 0.0
        val isSlow = MovementUtils.speed < 0.2

        if (isOnGrid || isSlow) {
            MovementUtils.strafe()
        }

        if (MovementUtils.speed < 0.195 && !player.isUsingItem) {
            MovementUtils.strafe(0.195f)
        }

        if (player.onGround) {
            player.motionX *= 1.001
            player.motionZ *= 1.001
            MovementUtils.strafe()
        }

        if (MovementUtils.airTicks > 1) {
            player.motionY -= 0.00348
        }

        if (MovementUtils.airTicks == 1) {
            partialStrafePercent(65.0)
        }

        if (predictedFalling(player.motionY)) {
            partialStrafePercent(65.0)
        }

        if (MovementUtils.airTicks < 12) {
            MovementUtils.strafe()
        }

        if (mc.gameSettings.keyBindSneak.isKeyDown) {
            ticksSinceSneak = 0
            MovementUtils.strafe(0.07f)
            mc.timer.timerSpeed = Speed.riseMatrixTimerSneak
        } else {
            ticksSinceSneak++
            if (ticksSinceSneak == 1) {
                mc.netHandler.addToSendQueue(C03PacketPlayer.C06PacketPlayerPosLook(
                    player.posX, player.posY, player.posZ,
                    player.rotationYaw, player.rotationPitch, player.onGround
                ))
                mc.netHandler.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(
                    player.posX, player.posY, player.posZ, player.onGround
                ))
                mc.netHandler.addToSendQueue(C03PacketPlayer.C06PacketPlayerPosLook(
                    player.posX, player.posY, player.posZ,
                    player.rotationYaw, player.rotationPitch, player.onGround
                ))
            }
        }

        if (player.onGround && !player.isUsingItem) {
            player.tryJump()
        }
    }

    private fun predictedFalling(motionY: Double): Boolean {
        return (motionY - 0.08) * 0.98 < 0
    }

    private fun partialStrafePercent(percent: Double) {
        val player = mc.thePlayer ?: return
        if (!player.isMoving) return
        val yaw = MovementUtils.direction
        val currentSpeed = MovementUtils.speed.toDouble()
        val targetSpeed = currentSpeed * percent / 100.0
        player.motionX = -Math.sin(yaw) * targetSpeed
        player.motionZ = Math.cos(yaw) * targetSpeed
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }
}