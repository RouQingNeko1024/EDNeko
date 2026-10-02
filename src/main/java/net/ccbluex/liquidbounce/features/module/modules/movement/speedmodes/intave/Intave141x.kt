package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Intave141x : SpeedMode("Intave14.1.x") {
    override fun onMotion() {
        if (!mc.thePlayer.isMoving || mc.thePlayer.isInWater || mc.thePlayer.isInLava || mc.thePlayer.isInWeb || mc.thePlayer.isOnLadder) {
            return
        }

        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump()
        }

        if (mc.thePlayer.motionY > 0.003 && mc.thePlayer.isSprinting) {
            reduceXZ(1.0015)
        }
    }
}