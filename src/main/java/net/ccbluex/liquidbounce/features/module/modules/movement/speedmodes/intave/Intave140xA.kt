package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Intave140xA : SpeedMode("Intave14.0.x-A") {
    override fun onMotion() {
        if (!mc.thePlayer.isMoving) {
            return
        }

        if (mc.thePlayer.onGround && mc.thePlayer.ticksExisted % 20 == 0) {
            mc.timer.timerSpeed = 300.0f
        }else mc.timer.timerSpeed = 0.05f

    }
}