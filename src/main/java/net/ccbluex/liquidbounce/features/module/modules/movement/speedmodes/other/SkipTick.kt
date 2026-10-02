package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.event.GameLoopEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.skipDelay
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.skipTicks
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.util.Timer

object SkipTick : SpeedMode("SkipTick") {
    val lastRunTime = MSTimer()
    var shouldSkip = false
    var skipedTicks = 0
    override fun onUpdate() {
        if (mc.thePlayer.isMoving && mc.thePlayer.ticksExisted % skipDelay == 0) shouldSkip = true
    }
    override fun onGameLoop(event: GameLoopEvent) {
        fun Timer.reset() {
            this.timerSpeed = 1.0f
        }
        if (shouldSkip) {
            if (lastRunTime.hasTimePassed(50L)) {
                mc.runTick()
                skipedTicks++
                if (mc.timer.timerSpeed == 0f) mc.timer.reset()
                lastRunTime.reset()
            } else mc.timer.timerSpeed = 0f
            if (skipedTicks > skipTicks) {
                shouldSkip = false
                skipedTicks = 0
            }
        }

    }
}

