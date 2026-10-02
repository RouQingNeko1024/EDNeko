package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.airBoostFactor
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.airBoostTimes
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.boostTime
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.groundBoostFactor
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.groundBoostTimes
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.notResetTimerSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.simpleAirTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.simpleGroundTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeTimer
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object SimpleBoost : SpeedMode("SimpleBoost") {

    private var nowAirBoostTimes = 0
    private var nowGroundBoostTimes = 0
    override fun onUpdate() {
        if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.isMoving) {
            mc.thePlayer.tryJump()
        }

        when {
            boostTime.contains("Air") && !mc.thePlayer.onGround -> {
                if (nowAirBoostTimes >= airBoostTimes) {
                    if (!notResetTimerSpeed) changeTimer(1.0f)
                    return
                }

                changeTimer(simpleAirTimer)
                reduceXZ(airBoostFactor.toDouble() + 1.0)
                nowAirBoostTimes++
                nowGroundBoostTimes = 0
            }
            boostTime.contains("Ground") && mc.thePlayer.onGround -> {
                if (nowGroundBoostTimes >= groundBoostTimes) {
                    if (!notResetTimerSpeed) changeTimer(1.0f)
                    return
                }

                changeTimer(simpleGroundTimer)
                reduceXZ(groundBoostFactor.toDouble() + 1.0)
                nowGroundBoostTimes++
                nowAirBoostTimes = 0
            }
            else -> {
                nowAirBoostTimes = 0
                nowGroundBoostTimes = 0
            }
        }
    }
    override fun onDisable() {
        nowAirBoostTimes = 0
        nowGroundBoostTimes = 0
    }
}