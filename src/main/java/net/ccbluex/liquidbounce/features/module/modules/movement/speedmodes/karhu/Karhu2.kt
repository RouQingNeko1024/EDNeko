package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.karhu

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Karhu2 : SpeedMode("Karhu2") {
    override fun onMotion() {
        if (mc.thePlayer.onGround && mc.thePlayer.isMoving) {
            mc.thePlayer.jump()
            mc.thePlayer.motionY -= 0.135
        }
    }
}