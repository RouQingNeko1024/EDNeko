package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.BoostTick
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.BoostTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.ChargeTick
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.ChargeTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode

object GroundTimerBoost : SpeedMode("GroundTimerBoost") {

    var ChargeTicks = 0
    var BoostTicks = 0

    override fun onTick() {
        if (mc.thePlayer.onGround && ChargeTick > ChargeTicks) {
            mc.timer.timerSpeed = ChargeTimer
            ChargeTicks++
        } else if (mc.thePlayer.onGround && BoostTicks < BoostTick) {
            mc.timer.timerSpeed = BoostTimer
            BoostTicks++
        } else {
            BoostTicks = 0
            ChargeTicks = 0
            mc.timer.timerSpeed = 1f
        }
    }
}