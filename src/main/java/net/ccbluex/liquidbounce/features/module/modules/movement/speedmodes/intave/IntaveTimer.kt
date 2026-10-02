package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object IntaveTimer : SpeedMode("IntaveTimer") {
    override fun onMotion() {
        if (!mc.thePlayer.isMoving) {
            return
        }

        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump()
            return
        } else if (mc.thePlayer.fallDistance <= 0.1) {
            mc.timer.timerSpeed = 1.7f
            return
        } else {
            if (mc.thePlayer.fallDistance < 1.3) {
                mc.timer.timerSpeed = 0.8f
            } else {
                mc.timer.timerSpeed = 1.0f
            }

            return
        }
    }
}