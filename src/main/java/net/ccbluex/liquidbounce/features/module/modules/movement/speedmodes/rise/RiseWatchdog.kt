package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseWatchdog : SpeedMode("RiseWatchdog") {

    private var suppressBoost = false
    private var recentlyCollided = false

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        when (Speed.riseWatchdogMode) {
            "Strafe" -> handleStrafe(player)
            "LowHop" -> handleLowHop(player)
            "Hop" -> handleHop(player)
        }
    }

    private fun handleStrafe(player: net.minecraft.client.entity.EntityPlayerSP) {
        if (player.isMoving) {
            if (player.onGround) {
                player.tryJump()
                MovementUtils.strafe(0.48f + speedPotionAmp(0.08).toFloat())
            } else {
                if (MovementUtils.airTicks == 1) {
                    MovementUtils.strafe(MovementUtils.speed * 0.97f)
                }
                if (MovementUtils.airTicks == 2) {
                    MovementUtils.strafe(MovementUtils.speed * 0.945f)
                }
            }
        }
    }

    private fun handleLowHop(player: net.minecraft.client.entity.EntityPlayerSP) {
        if (player.isMoving) {
            if (player.onGround) {
                player.tryJump()
                MovementUtils.strafe(0.48f + speedPotionAmp(0.08).toFloat())
            } else {
                if (MovementUtils.airTicks == 1) {
                    MovementUtils.strafe(MovementUtils.speed * 0.97f)
                }
                if (MovementUtils.airTicks == 3) {
                    player.motionY = -0.13
                }
                if (MovementUtils.airTicks == 4) {
                    player.motionY = -0.19
                }
            }
        }
    }

    private fun handleHop(player: net.minecraft.client.entity.EntityPlayerSP) {
        if (player.isMoving) {
            if (player.onGround) {
                player.tryJump()
                MovementUtils.strafe(0.48f + speedPotionAmp(0.08).toFloat())
            } else {
                if (MovementUtils.airTicks == 1) {
                    MovementUtils.strafe(MovementUtils.speed * 0.97f)
                }
            }
        }
    }

    private fun speedPotionAmp(base: Double): Double {
        val player = mc.thePlayer ?: return base
        return if (player.isPotionActive(Potion.moveSpeed)) {
            base * (player.getActivePotionEffect(Potion.moveSpeed).amplifier + 1)
        } else {
            base
        }
    }

    override fun onEnable() {
        suppressBoost = false
        recentlyCollided = false
    }

    override fun onDisable() {
        suppressBoost = false
    }
}