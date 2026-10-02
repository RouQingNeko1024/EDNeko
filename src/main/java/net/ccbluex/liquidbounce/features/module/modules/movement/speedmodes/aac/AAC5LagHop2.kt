package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object AAC5LagHop2 : SpeedMode("AAC5LagHop2") {
    override fun onMotion() {
        val player = mc.thePlayer ?: return
        mc.timer.timerSpeed = 1f

        if (!mc.thePlayer.isMoving || player.isInWater || player.isInLava || player.isOnLadder || player.isRiding)
            return

        mc.timer.timerSpeed = when {
            (mc.thePlayer.motionY>0) -> 300f
            (mc.thePlayer.motionY<0) -> 0.12f
            else -> 1f
        }
        if (player.onGround) {
            player.jump()
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }
}