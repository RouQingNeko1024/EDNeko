package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object RiseVanilla : SpeedMode("RiseVanilla") {

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.isMoving && player.onGround) {
            player.jump()
        }

        MovementUtils.strafe(Speed.riseVanillaSpeed)
    }
}