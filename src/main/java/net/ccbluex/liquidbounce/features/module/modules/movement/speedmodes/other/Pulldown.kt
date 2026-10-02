package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.BoostFactor
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.BoostNumber
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.DebuggerPulldown
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.MaxTriggerChange
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.MotionYReduceMode
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.TimerPulldown
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Pulldown : SpeedMode("Pulldown") {
    private var airTick = 0
    private var TriggerTimes = 0
    override fun onTick() {
        if (mc.thePlayer.onGround) {
            airTick = 0
            TriggerTimes = 0
            if (mc.thePlayer.isMoving) {
                mc.thePlayer.motionY = 0.42
                if (DebuggerPulldown) {
                    chat("Jump")
                }
            }
            mc.timer.timerSpeed = 1F
        } else if (!mc.thePlayer.onGround) {
            airTick++
        }
        if (mc.thePlayer.motionY < 0 && airTick > 0 && TriggerTimes < MaxTriggerChange) {
            if (MotionYReduceMode == "Percent") {
                mc.thePlayer.motionY -= 0.1523351824467155 * BoostFactor
                if (DebuggerPulldown) {
                    chat("MotionYBoost | ${0.1523351824467155 * BoostFactor}")
                }
            }
            if (MotionYReduceMode == "Number") {
                mc.thePlayer.motionY -= BoostNumber
                if (DebuggerPulldown) {
                    chat("MotionYBoost | ${mc.thePlayer.motionY-BoostNumber}")
                }
            }
            mc.timer.timerSpeed = TimerPulldown
            TriggerTimes++
        }
    }

    override fun onEnable() {
        TriggerTimes = 114514
    }
}