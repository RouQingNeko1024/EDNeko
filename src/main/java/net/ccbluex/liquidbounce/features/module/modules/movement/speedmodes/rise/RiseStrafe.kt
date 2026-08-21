package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object RiseStrafe : SpeedMode("RiseStrafe") {

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (!player.isMoving) {
            player.stopXZ()
        } else {
            if (player.onGround) {
                player.tryJump()
            }

            if (Speed.riseStrafeHurtBoost && player.hurtTime == 9) {
                MovementUtils.strafe(Speed.riseStrafeBoostSpeed)
            }

            MovementUtils.strafe()
        }
    }
}