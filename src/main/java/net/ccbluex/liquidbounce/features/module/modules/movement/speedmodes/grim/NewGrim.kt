package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object NewGrim : SpeedMode("NewGrim") {
    override fun onStrafe() {
        val player = mc.thePlayer ?: return

        if (mc.thePlayer.onGround && player.isMoving) {
            player.tryJump()
        }
        if (mc.thePlayer.ticksExisted % 2 == 1) {
            mc.timer.timerSpeed = 1.025f
            MovementUtils.strafe((MovementUtils.speed * 1.01).toFloat())
        } else {
            mc.timer.timerSpeed = 0.99f
            MovementUtils.strafe((MovementUtils.speed * 0.99).toFloat())
        }
    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (!mc.thePlayer.isBlocking && !mc.thePlayer.isSneaking && mc.thePlayer.isMoving && !mc.thePlayer.isCollidedVertically)
            player.isSprinting = player.movementInput.moveForward > 0.8
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }
}