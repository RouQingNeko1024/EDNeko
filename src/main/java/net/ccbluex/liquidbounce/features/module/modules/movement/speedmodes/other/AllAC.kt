package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object AllAC : SpeedMode("AllAC") {
    private var jumpCount = 0


    override fun onMotion() {
        val player = mc.thePlayer ?: return
        mc.timer.timerSpeed = 1f

        if (!mc.thePlayer.isMoving || player.isInWater || player.isInLava || player.isOnLadder || player.isRiding)
            return

        mc.timer.timerSpeed = when(jumpCount%5){
            0-> 0.97f
            1-> 1.01f
            2-> 1.03f
            3-> 0.96f
            4-> 1.025f
            else -> 1f
        }
        if (player.onGround) {
            player.jump()
            jumpCount++
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

}