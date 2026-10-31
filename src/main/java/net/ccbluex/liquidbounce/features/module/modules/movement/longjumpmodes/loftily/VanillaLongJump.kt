package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.loftily

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object VanillaLongJump : LongJumpMode("Vanilla") {

    val boostSpeed by float("Vanilla-BoostSpeed", 3.0f, 0f..5f)
    val motion by float("Vanilla-Motion", 0.42f, -1f..1f)

    override fun onJump(event: JumpEvent) {
        event.cancelEvent()
        MovementUtils.setSpeed(boostSpeed.toDouble(), true)
        mc.thePlayer.motionY = motion.toDouble()
        if (LongJump.autoDisable) {
            LongJump.state = false
        }
    }
}