package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object RiseTatako : SpeedMode("RiseTatako") {

    private var strafeTicks = 0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.isMoving && player.onGround) {
            player.tryJump()
            strafeTicks++
        }

        if (player.onGround) {
            moveFlying(0.005)
            partialStrafePercent(95.0)
        }

        when (MovementUtils.airTicks) {
            1 -> {
                partialStrafePercent(60.0)
                player.motionY = predictedMotion(player.motionY + 0.12, 2)
            }
            2 -> {
                partialStrafePercent(50.0)
            }
            else -> {
                partialStrafePercent(1.0)
            }
        }

        moveFlying(8.0E-4)
    }

    private fun predictedMotion(motionY: Double, ticks: Int): Double {
        var y = motionY
        for (i in 0 until ticks) {
            y = (y - 0.08) * 0.98
        }
        return y
    }

    private fun moveFlying(amount: Double) {
        val player = mc.thePlayer ?: return
        if (!player.isMoving) return
        val yaw = MovementUtils.direction
        player.motionX += -Math.sin(yaw) * amount
        player.motionZ += Math.cos(yaw) * amount
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

    override fun onEnable() {
        strafeTicks = 0
    }
}