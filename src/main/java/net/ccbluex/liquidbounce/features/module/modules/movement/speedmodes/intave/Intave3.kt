package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Intave3 : SpeedMode("Intave3") {
    override fun onUpdate() {
        if (mc.thePlayer.isMoving) {
            if (mc.thePlayer.onGround)
                mc.thePlayer.jump()
            when {
                mc.thePlayer.fallDistance >= 1.3 -> mc.timer.timerSpeed = 1f
                mc.thePlayer.fallDistance > 0.1 && mc.thePlayer.fallDistance < 1.3 -> mc.timer.timerSpeed = 0.7f
                !mc.thePlayer.onGround && mc.thePlayer.fallDistance <= 0.1 -> mc.timer.timerSpeed = 1.4f
            }
        }
    }
}