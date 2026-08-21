package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseBlocksMC : SpeedMode("RiseBlocksMC") {

    private var reset = false
    private var speed = 0.0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        val baseSpeed = getAllowedHorizontalDistance()

        if (player.isMoving) {
            when (MovementUtils.airTicks) {
                0 -> {
                    player.tryJump()
                    val hasSpeed = player.isPotionActive(Potion.moveSpeed)
                    speed = baseSpeed * (if (hasSpeed) 1.4 else 2.15)
                }
                1 -> {
                    speed -= 0.8 * (speed - baseSpeed)
                }
                else -> {
                    speed -= speed / 159.9
                }
            }
            reset = false
        } else if (!reset) {
            speed = 0.0
            reset = true
            speed = getAllowedHorizontalDistance()
        }

        if (player.isCollidedHorizontally) {
            speed = getAllowedHorizontalDistance()
        }

        MovementUtils.strafe(Math.max(speed, baseSpeed).toFloat())
    }

    private fun getAllowedHorizontalDistance(): Double {
        val player = mc.thePlayer ?: return 0.2873
        var base = 0.2873
        if (player.isPotionActive(Potion.moveSpeed)) {
            base *= 1.0 + 0.2 * (player.getActivePotionEffect(Potion.moveSpeed).amplifier + 1)
        }
        return base
    }

    override fun onDisable() {
        speed = 0.0
    }
}