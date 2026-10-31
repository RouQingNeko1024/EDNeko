package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.loftily

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object GrimLongJump : LongJumpMode("Grim1.9") {

    private var offGroundTicks = 0

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
        offGroundTicks = 0
    }

    override fun onMotion(event: MotionEvent) {
        if (!mc.thePlayer.onGround) {
            offGroundTicks++
        } else {
            offGroundTicks = 0
        }

        if (event.eventState == EventState.POST
            && mc.thePlayer.onGround
            && mc.thePlayer.isMoving
        ) {
            mc.thePlayer.tryJump()
        }
        if (offGroundTicks >= 2) {
            mc.timer.timerSpeed = 2.0f
        }
    }
}