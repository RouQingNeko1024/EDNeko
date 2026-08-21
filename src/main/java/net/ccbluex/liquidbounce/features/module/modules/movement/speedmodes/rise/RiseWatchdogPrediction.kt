package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object RiseWatchdogPrediction : SpeedMode("RiseWatchdogPrediction") {

    private var savedMotionX = 0.0
    private var savedMotionY = 0.0
    private var savedMotionZ = 0.0
    private var hasSavedMotion = false
    private var boostPending = false

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.onGround && player.isMoving && !player.isInWater) {
            player.tryJump()
        }

        if (!player.isInWater) {
            when (MovementUtils.airTicks) {
                5 -> {
                    if (MovementUtils.groundTicks > 7) {
                        savedMotionX = player.motionX
                        savedMotionY = player.motionY
                        savedMotionZ = player.motionZ
                        hasSavedMotion = true
                        player.motionY = 0.0
                        player.stopXZ()
                    }
                }
                6 -> {
                    if (hasSavedMotion && MovementUtils.groundTicks > 7) {
                        val d0 = if (MovementUtils.airTicks <= 1) 0.5460000157356262 else 1.0
                        player.motionX = savedMotionX * d0
                        player.motionY = savedMotionY - 0.02 * d0
                        player.motionZ = savedMotionZ * d0
                        moveFlying(0.1)
                        hasSavedMotion = false
                        boostPending = true
                    }
                }
                7 -> {
                    if (boostPending && MovementUtils.groundTicks > 7) {
                        moveFlying(0.06)
                        boostPending = false
                    }
                }
            }
        } else {
            if ((MovementUtils.airTicks - 1) % 3 == 0 && MovementUtils.groundTicks > 1) {
                savedMotionX = player.motionX
                savedMotionY = player.motionY
                savedMotionZ = player.motionZ
                hasSavedMotion = true
                player.motionY = 0.0
                player.stopXZ()
            } else if (hasSavedMotion && MovementUtils.groundTicks > 2) {
                val d0 = if (MovementUtils.airTicks <= 1) 0.5460000157356262 else 1.0
                player.motionX = savedMotionX * d0
                player.motionY = (savedMotionY - 1.0E-14)
                player.motionZ = savedMotionZ * d0
                moveFlying(0.087)
                hasSavedMotion = false
                boostPending = true
            } else if (boostPending && MovementUtils.groundTicks > 2) {
                boostPending = false
                moveFlying(0.086)
            }
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
        if (hasSavedMotion) {
            mc.thePlayer?.let {
                it.motionX = savedMotionX * 0.91f
                it.motionY = savedMotionY
                it.motionZ = savedMotionZ * 0.91f
            }
            hasSavedMotion = false
        }
        mc.timer.timerSpeed = 1f
    }

    override fun onEnable() {
        hasSavedMotion = false
        boostPending = false
    }
}