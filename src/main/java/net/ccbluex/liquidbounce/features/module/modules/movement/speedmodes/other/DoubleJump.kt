package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object DoubleJump : SpeedMode("DoubleJump") {
    override fun onUpdate() {
        if (mc.thePlayer.onGround && mc.thePlayer.isMoving) {
            mc.thePlayer.jump()
            mc.gameSettings.keyBindJump.pressed = true
        } else mc.gameSettings.keyBindJump.pressed = false
    }
}