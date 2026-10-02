package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.motionBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeTimer
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object NewGrim : SpeedMode("NewGrim") {
    override fun onStrafe() {
        val player = mc.thePlayer ?: return

        if (mc.thePlayer.onGround && player.isMoving) {
            player.tryJump()
        }
        if (mc.thePlayer.ticksExisted % 2 == 1) {
            changeTimer(1.025f)
            if (motionBoost) {
                reduceXZ(1.01)
            }
        } else {
            changeTimer(0.99f)
            if (motionBoost) {
                reduceXZ(0.99)
            }
        }

    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (!mc.thePlayer.isBlocking && !mc.thePlayer.isSneaking && mc.thePlayer.isMoving && !mc.thePlayer.isCollidedVertically)
        player.isSprinting = player.movementInput.moveForward > 0.8
    }
}