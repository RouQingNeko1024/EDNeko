package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseNCP : SpeedMode("RiseNCP") {

    private var reset = false
    private var speed = 0.0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (Speed.riseNCPLowHop && MovementUtils.airTicks == 4) {
            player.motionY = -0.09800000190734864
        }

        if (Speed.riseNCPYPortHop && MovementUtils.airTicks == 5 && Math.abs(player.motionY - 0.09800000190734864) < 0.12) {
            player.motionY = -0.09800000190734864
        }

        if (Speed.riseNCPCustomBoost && player.hurtTime <= Speed.riseNCPHurtTime) {
            speed = Speed.riseNCPBoostSpeed.toDouble()
        }

        val baseSpeed = getAllowedHorizontalDistance()

        if (player.isMoving) {
            when (MovementUtils.airTicks) {
                0 -> {
                    val jumpMotion = Speed.riseNCPJumpMotion
                    val actualJump = if (player.isCollidedHorizontally) 0.42f else if (jumpMotion == 0.4f) jumpMotion else 0.42f
                    player.motionY = jumpBoostMotion(actualJump)
                    speed = baseSpeed * Speed.riseNCPGroundSpeed
                }
                1 -> {
                    speed -= Speed.riseNCPBunnySlope * (speed - baseSpeed)
                }
                else -> {
                    speed -= speed / 159.9
                }
            }
            mc.timer.timerSpeed = Speed.riseNCPTimer
            reset = false
        } else if (!reset) {
            speed = getAllowedHorizontalDistance()
            mc.timer.timerSpeed = 1f
            reset = true
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

    private fun jumpBoostMotion(base: Float): Double {
        val player = mc.thePlayer ?: return base.toDouble()
        var motion = base.toDouble()
        if (player.isPotionActive(Potion.jump)) {
            motion += (player.getActivePotionEffect(Potion.jump).amplifier + 1) * 0.1
        }
        return motion
    }

    override fun onDisable() {
        speed = 0.0
        mc.timer.timerSpeed = 1f
    }
}