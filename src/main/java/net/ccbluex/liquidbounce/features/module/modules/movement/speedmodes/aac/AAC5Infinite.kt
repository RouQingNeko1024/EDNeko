package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object AAC5Infinite : SpeedMode("AAC5Infinite") {

    var jumpAAC = 0

    override fun onMotion() {
        val player = mc.thePlayer ?: return
        mc.timer.timerSpeed = 1f

        if (!mc.thePlayer.isMoving || player.isInWater || player.isInLava || player.isOnLadder || player.isRiding)
            return

        mc.timer.timerSpeed = when(jumpAAC%3){
            0-> 1.02f
            1 -> 0.88f
            2 -> 1.12f
            else -> 1f
        }
        if (player.onGround) {
            player.jump()
            jumpAAC++
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

}