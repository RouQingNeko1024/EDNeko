package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.spiterFallSpeedThreshold
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.spiterUpwardMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.spiterTimerSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode

object SpiterAntivoid : FlyMode("SpiterAntivoid") {
    private var isApplyingMotion = false

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        val isFalling = player.motionY < 0 && !player.onGround

        val fallSpeedBps = Math.abs(player.motionY) * 20.0

        if (isFalling && fallSpeedBps > spiterFallSpeedThreshold.toDouble()) {
            isApplyingMotion = true
            player.motionY = spiterUpwardMotion.toDouble()
            player.onGround = false
            mc.timer.timerSpeed = spiterTimerSpeed
        } else {
            isApplyingMotion = false
            mc.timer.timerSpeed = 1f
        }
    }

    override fun onDisable() {
        isApplyingMotion = false
        mc.timer.timerSpeed = 1f
    }
}