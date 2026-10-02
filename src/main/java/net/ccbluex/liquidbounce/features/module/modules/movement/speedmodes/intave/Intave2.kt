package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.groundStrafe
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe

object Intave2 : SpeedMode("Intave2") {
    override fun onUpdate() {
        if (mc.thePlayer.isMoving) {
            if (mc.thePlayer.onGround) {
                mc.timer.timerSpeed = 1f
                if (groundStrafe) strafe()
                mc.thePlayer.jump()
            }
            if (mc.thePlayer.motionY > 0.003) {
                mc.thePlayer.motionX *= 1.0015
                mc.thePlayer.motionZ *= 1.0015
                mc.timer.timerSpeed = 1.06f
            }
        }
    }
}