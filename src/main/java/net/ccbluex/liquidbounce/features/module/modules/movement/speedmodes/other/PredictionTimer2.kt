package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object PredictionTimer2 : SpeedMode("PredictionTimer2") {
    private var tickCounter = 0
    private var hasJumpedThisCycle = false
    override fun onStrafe() {
        val player = mc.thePlayer ?: return
        if (player.isMoving && player.onGround && !hasJumpedThisCycle) {
            player.tryJump()
            hasJumpedThisCycle = true
        } else if (!player.isMoving) {
            hasJumpedThisCycle = false
        }
    }
    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        tickCounter = (tickCounter + 1) % Speed.prediction2CycleLength
        if (tickCounter < Speed.prediction2BoostDuration) {
            mc.timer.timerSpeed = Speed.prediction2TimerSpeed
        } else {
            mc.timer.timerSpeed = 1.0f
        }
        if (player.onGround) {
            hasJumpedThisCycle = false
        }
    }
    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
        tickCounter = 0
        hasJumpedThisCycle = false
    }
}