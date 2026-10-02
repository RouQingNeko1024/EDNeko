package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object MinemenHop : SpeedMode("MinemenHop") {
    override fun onMotion() {
        if (mc.thePlayer.isMoving) {
            mc.gameSettings.keyBindJump.pressed = false
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump()
                mc.timer.timerSpeed = 1.02f
            } else mc.timer.timerSpeed = 1.0f
        }
    }
}