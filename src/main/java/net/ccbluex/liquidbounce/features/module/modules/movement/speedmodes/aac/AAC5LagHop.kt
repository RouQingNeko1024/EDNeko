/*
 * FireBounce Hacked Client
 * A mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving


object AAC5LagHop : SpeedMode("AAC5LagHop") {

    override fun onMotion() {
        val player = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f

        if (!mc.thePlayer.isMoving || player.isInWater || player.isInLava || player.isOnLadder || player.isRiding)
            return

        mc.timer.timerSpeed = 2.5f
        player.speedInAir = 0.068f
        mc.gameSettings.keyBindJump.pressed = true
        if (player.onGround) player.jump()
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        mc.thePlayer.speedInAir = 0.02f
        mc.gameSettings.keyBindJump.pressed = false
    }

}