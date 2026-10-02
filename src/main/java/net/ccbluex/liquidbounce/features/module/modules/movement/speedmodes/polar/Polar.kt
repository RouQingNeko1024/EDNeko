package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.polar

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.polarTick
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode

object Polar : SpeedMode("Polar") {
    override fun onMotion() {
        if (mc.thePlayer.motionY <= -0.1) {
            polarTick++
            if (polarTick % 2 == 0) {
                mc.thePlayer.motionY = -0.1
            } else {
                mc.thePlayer.motionY = -0.16
            }
            mc.thePlayer.jumpMovementFactor = 0.0265f
        } else polarTick = 0
    }
}