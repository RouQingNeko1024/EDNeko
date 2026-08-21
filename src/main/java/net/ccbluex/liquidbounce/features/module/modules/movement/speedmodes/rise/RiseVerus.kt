package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseVerus : SpeedMode("RiseVerus") {

    private var pendingBoost = true
    private var recentlyAttacked = false
    private var attackTicks = 0
    private var lastStopped = false
    private var ticks = 0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (!player.isMoving) {
            if (!lastStopped) {
                pendingBoost = true
                lastStopped = true
            }
            return
        }
        lastStopped = false

        if (attackTicks > 0) attackTicks--
        if (attackTicks == 0) recentlyAttacked = false

        when (Speed.riseVerusMode) {
            "LowHop" -> {
                if (player.onGround) {
                    player.motionY = 0.42
                    MovementUtils.strafe(0.69f + speedPotionAmp(0.1).toFloat())
                    player.motionY = 0.0
                } else {
                    MovementUtils.strafe(0.41f + speedPotionAmp(0.055).toFloat())
                }

                if (player.hurtTime <= 20) {
                    MovementUtils.strafe(1.0f + speedPotionAmp(0.055).toFloat())
                }

                player.isSprinting = true
            }
            "Hop" -> {
                if (pendingBoost) {
                    pendingBoost = false
                }

                if (player.isPotionActive(Potion.moveSpeed)) {
                    MovementUtils.strafe((0.179 * (1 + player.getActivePotionEffect(Potion.moveSpeed).amplifier) + 0.46).toFloat())
                } else {
                    MovementUtils.strafe(0.4645f)
                }

                if (player.onGround) {
                    player.motionY = 0.0
                    if (player.isPotionActive(Potion.moveSpeed)) {
                        MovementUtils.strafe((0.092 * (1 + player.getActivePotionEffect(Potion.moveSpeed).amplifier) + 0.55).toFloat())
                    } else {
                        MovementUtils.strafe(0.558f)
                    }
                }
            }
            "yPort" -> {
                if (player.onGround) {
                    player.tryJump()
                    MovementUtils.strafe(0.6f)
                } else if (MovementUtils.airTicks == 1) {
                    player.motionY = -0.09800000190734864
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
        pendingBoost = true
        lastStopped = false
        ticks = 0
    }

    override fun onDisable() {
        pendingBoost = true
    }
}