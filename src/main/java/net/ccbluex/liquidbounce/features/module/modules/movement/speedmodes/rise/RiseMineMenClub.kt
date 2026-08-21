package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseMineMenClub : SpeedMode("RiseMineMenClub") {

    private var jumpCount = 0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.onGround) {
            player.tryJump()
        }

        if (MovementUtils.airTicks > 8) {
            if (player.onGround) {
                MovementUtils.strafe()
            }
        }
    }

    override fun onJump(event: net.ccbluex.liquidbounce.event.JumpEvent) {
        jumpCount++
    }

    override fun onEnable() {
        jumpCount = 0
    }
}