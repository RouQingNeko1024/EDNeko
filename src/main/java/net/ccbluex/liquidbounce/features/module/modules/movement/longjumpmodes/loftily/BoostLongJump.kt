package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.loftily

import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object BoostLongJump : LongJumpMode("Boost") {

    val boostSpeed by float("Boost-Speed", 3.0f, 0f..5f)
    val motion by float("Boost-Motion", 0.42f, -5f..5f)

    override fun onEnable() {
        MovementUtils.setSpeed(boostSpeed.toDouble(), false)
        mc.thePlayer.motionY = motion.toDouble()
        LongJump.state = false
    }
}