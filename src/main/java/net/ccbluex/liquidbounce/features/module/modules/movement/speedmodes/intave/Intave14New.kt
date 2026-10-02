package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.Debugger
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.useTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.airTicks


object Intave14New : SpeedMode("Intave14.8.4") {

    override fun onTick() {
        if (mc.thePlayer.onGround && mc.thePlayer.isMoving) mc.thePlayer.tryJump()
        when (airTicks) {
            1 -> {
                factorXZ(1.04)
                if (Debugger) {
                    chat("Boost")
                }
            }

            2, 3, 4 -> {
                factorXZ(1.02)
            }
        }
        if (useTimer) {
            mc.timer.timerSpeed = 1.002f
        } else mc.timer.timerSpeed = 1.0f
    }
    fun factorXZ(factor: Double) {
        mc.thePlayer.motionX *= factor
        mc.thePlayer.motionZ *= factor
    }
}