package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object IntaveNew : SpeedMode("IntaveNew") {
    private var boosted = false
    override fun onUpdate() {
        if (!mc.thePlayer.isMoving) {
            val resetTimer = mc.timer.timerSpeed == 1.1f || mc.timer.timerSpeed == 0.95f || mc.timer.timerSpeed == 1.0015f
            if (resetTimer) SomeUtil.changeTimer(1.0f)
            return
        }
        if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown && Speed.intaveNewAutoJump) {
            mc.thePlayer.motionY = 0.22
        }

        when {
            mc.thePlayer.onGround -> {
                SomeUtil.reduceXZ(1.00666666666)
                SomeUtil.changeTimer(1.1f)
                boosted = false
            }
            !boosted -> {
                // 第一次空中加速
                SomeUtil.reduceXZ(1.05)
                SomeUtil.changeTimer(0.95f)
                boosted = true
            }
            else -> {
                // 持续空中加速
                SomeUtil.reduceXZ(1.0015)
                SomeUtil.changeTimer(1.0015f)
            }
        }
    }
}