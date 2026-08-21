package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseKoksCraft : SpeedMode("RiseKoksCraft") {

    private var jumps = 0

    override fun onMotion() {
        val player = mc.thePlayer ?: return

        if (player.onGround) {
            if (player.hurtTime == 0) {
                MovementUtils.strafe(getAllowedHorizontalDistance().toFloat() * 0.99f)
            }
            player.tryJump()
            jumps++
        }

        if (MovementUtils.airTicks == 1 && player.hurtTime == 0) {
            player.motionY = predictedMotion(player.motionY, if (jumps % 2 == 0) 2 else 4)
        }
    }

    private fun predictedMotion(motionY: Double, ticks: Int): Double {
        var y = motionY
        for (i in 0 until ticks) {
            y = (y - 0.08) * 0.98
        }
        return y
    }

    private fun getAllowedHorizontalDistance(): Double {
        val player = mc.thePlayer ?: return 0.2873
        var base = 0.2873
        if (player.isPotionActive(Potion.moveSpeed)) {
            base *= 1.0 + 0.2 * (player.getActivePotionEffect(Potion.moveSpeed).amplifier + 1)
        }
        return base
    }

    override fun onEnable() {
        jumps = 0
    }
}