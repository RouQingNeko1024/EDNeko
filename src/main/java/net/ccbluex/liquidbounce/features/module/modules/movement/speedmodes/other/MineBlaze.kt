package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.mineBlazeBoostFactor
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.mineBlazeCheckEnvironment
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.mineBlazeTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object MineBlaze : SpeedMode("MineBlaze") {
    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.onGround && mc.thePlayer.isMoving && !mc.gameSettings.keyBindJump.isKeyDown) {
            player.jump()
        }
        val inWater = player.isInWater || player.isInLava
        val onLadder = player.isOnLadder
        if ((inWater || onLadder) && mineBlazeCheckEnvironment) return
        if (player.motionY > 0.003) {
            reduceXZ(mineBlazeBoostFactor.toDouble())
            if (mineBlazeTimer)
            mc.timer.timerSpeed = 1.06f
        }
    }
}