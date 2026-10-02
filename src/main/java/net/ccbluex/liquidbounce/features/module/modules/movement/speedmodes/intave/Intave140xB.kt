package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.intaveBTick
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Intave140xB : SpeedMode("Intave14.0.x-B") {
    override fun onMotion() {
        if (!mc.thePlayer.isMoving) {
            return
        }

        if (mc.thePlayer.onGround) {
            if (mc.thePlayer.ticksExisted % intaveBTick.toDouble() == 0.0) {
                mc.thePlayer.jump()
            }

            mc.timer.timerSpeed = 0.1f
        } else {
            mc.timer.timerSpeed = 1.75f
        }
    }
}