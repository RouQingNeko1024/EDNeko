package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Intave1309 : SpeedMode("Intave13.0.9") {
    override fun onMotion() {
        if (!mc.thePlayer.isMoving) {
            return
        }

        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump()
            mc.thePlayer.motionY -= 0.2
        }
    }
}