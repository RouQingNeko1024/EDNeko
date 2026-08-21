package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object RiseLegit : SpeedMode("RiseLegit") {

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (Speed.riseLegitNoJumpDelay) {
            player.jumpTicks = 0
        }

        if (Speed.riseLegitTimerBoost) {
            mc.timer.timerSpeed = 1.004f
        }

        if (player.isMoving && player.onGround && mc.gameSettings.keyBindJump.isKeyDown && !player.isInWater) {
            player.jump()
        }

        if (MovementUtils.airTicks > 2) {
            moveFlying(1.0E-5)
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    private fun moveFlying(amount: Double) {
        val player = mc.thePlayer ?: return
        if (!player.isMoving) return
        val yaw = MovementUtils.direction
        player.motionX += -Math.sin(yaw) * amount
        player.motionZ += Math.cos(yaw) * amount
    }
}