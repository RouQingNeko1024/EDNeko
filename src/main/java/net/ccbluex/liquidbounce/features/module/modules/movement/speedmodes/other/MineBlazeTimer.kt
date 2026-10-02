package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object MineBlazeTimer : SpeedMode("MineBlazeTimer") {
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f

        if (!mc.thePlayer.isMoving)
            return

        if (thePlayer.onGround)
            thePlayer.jump()
        else {
            if (thePlayer.fallDistance <= 0.1)
                mc.timer.timerSpeed = 1.7f
            else if (thePlayer.fallDistance < 1.3)
                mc.timer.timerSpeed = 0.8f
            else
                mc.timer.timerSpeed = 1f
        }
    }
}