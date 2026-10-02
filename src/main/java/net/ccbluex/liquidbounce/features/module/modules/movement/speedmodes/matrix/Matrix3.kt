package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.intaveNewAutoJump
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.setSpeed
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import kotlin.math.pow
import kotlin.math.sqrt

object Matrix3 : SpeedMode("Matrix3") {
    override fun onJump(event: JumpEvent) {
        if (!mc.thePlayer.isSprinting) return
    }

    override fun onStrafe() {
        if (mc.thePlayer.isMoving) {
            if (mc.thePlayer.onGround) {
                MovementUtils.strafe(speed)
                if (intaveNewAutoJump) mc.thePlayer.tryJump()
            }
            val speed = sqrt(mc.thePlayer.motionX.pow(2) + mc.thePlayer.motionZ.pow(2))
            if (speed > 0.2 && mc.thePlayer.isCollidedHorizontally) {
                setSpeed(0.2,false)
            }
            if (mc.thePlayer.hurtTime <= 0) {
                mc.thePlayer.motionY -= 0.0032
            }
        }
    }
}