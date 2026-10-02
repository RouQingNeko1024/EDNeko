package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object Matrix2 : SpeedMode("Matrix2") {
    override fun onUpdate()  {
        if (mc.thePlayer.isMoving) {
            if (mc.thePlayer.isAirBorne && mc.thePlayer.fallDistance > 1.215f) {
                mc.timer.timerSpeed = 1f
                return
            }

            if (mc.thePlayer.onGround) {
                strafe()
                mc.thePlayer.jump()
                if (mc.thePlayer.motionY > 0)
                    mc.timer.timerSpeed = 1.0953f
            } else if (mc.thePlayer.motionY < 0)
                mc.timer.timerSpeed = 0.9185f
        } else mc.timer.timerSpeed = 1f
    }
}