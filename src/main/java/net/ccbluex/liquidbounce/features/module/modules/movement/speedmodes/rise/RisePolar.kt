package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object RisePolar : SpeedMode("RisePolar") {

    private var jumps = 0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1.009f

        if (MovementUtils.airTicks == 5 && jumps % 2 != 0) {
            player.motionY -= 0.03
        }

        if (player.onGround) {
            player.tryJump()
        }

        moveFlying(0.002)
    }

    override fun onJump(event: net.ccbluex.liquidbounce.event.JumpEvent) {
        jumps++
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