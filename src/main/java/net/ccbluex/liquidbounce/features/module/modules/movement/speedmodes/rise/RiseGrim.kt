package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseGrim : SpeedMode("RiseGrim") {

    private var airTicks = 0
    private var groundSpoof = 0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (!player.onGround) {
            airTicks++
        } else {
            airTicks = 0
        }

        if (Speed.riseGrimFastFall) {
            if (groundSpoof == 0) {
                groundSpoof = if (player.onGround) 1 else 0
            } else {
                groundSpoof = 0
            }

            if (player.onGround) {
                player.tryJump()
            }

            if (!player.onGround && groundSpoof >= 0) {
                if (airTicks in 2..5) {
                    if (airTicks == 2) {
                        player.tryJump()
                    }
                }
            }
        }

        moveFlying(Speed.riseGrimMoveFlyingIncrease.toDouble())

        if (player.onGround && mc.gameSettings.keyBindJump.isKeyDown) {
            player.tryJump()
        }
    }

    private fun moveFlying(amount: Double) {
        val player = mc.thePlayer ?: return
        if (!player.isMoving) return
        val yaw = MovementUtils.direction
        player.motionX += -Math.sin(yaw) * amount
        player.motionZ += Math.cos(yaw) * amount
    }

    override fun onDisable() {
        airTicks = 0
        groundSpoof = 0
    }

    override fun onEnable() {
        airTicks = 0
        groundSpoof = 0
    }
}