package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseVulcan : SpeedMode("RiseVulcan") {

    private var wasSprinting = false
    private var lastPosX = 0.0
    private var lastPosZ = 0.0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        when (Speed.riseVulcanMode) {
            "LowHop" -> {
                if (player.onGround && player.isMoving) {
                    player.tryJump()
                    MovementUtils.strafe()
                }
                if (MovementUtils.airTicks == 1) {
                    MovementUtils.strafe()
                }
                if (MovementUtils.airTicks == 2) {
                    MovementUtils.strafe()
                }
                if (MovementUtils.airTicks == 8) {
                    MovementUtils.strafe()
                }
            }
            "Yport" -> {
                val speedAmp = if (player.isPotionActive(Potion.moveSpeed)) player.getActivePotionEffect(Potion.moveSpeed).amplifier + 1 else 0

                if (player.onGround && player.isMoving) {
                    player.tryJump()
                }

                when (MovementUtils.airTicks) {
                    0 -> {
                        when (speedAmp) {
                            0 -> MovementUtils.strafe(0.55f)
                            1 -> MovementUtils.strafe(0.54f)
                            else -> MovementUtils.strafe(0.67f)
                        }
                        player.motionY = 0.2
                    }
                    1 -> {
                        player.motionY = -0.0784000015258789
                        when (speedAmp) {
                            0 -> MovementUtils.strafe(0.43f)
                            1 -> MovementUtils.strafe(0.49f)
                            else -> MovementUtils.strafe(0.57f)
                        }
                    }
                    2 -> {
                        when (speedAmp) {
                            0 -> MovementUtils.strafe(0.37f)
                            1 -> MovementUtils.strafe(0.44f)
                            else -> MovementUtils.strafe(0.47f)
                        }
                    }
                }
            }
            "Ground" -> {
                if (player.isMoving) {
                    MovementUtils.strafe(0.29f)
                }
            }
        }
    }

    override fun onDisable() {
        if (Speed.riseVulcanMode == "Ground") {
            MovementUtils.strafe(MovementUtils.speed * 0.6f - 0.02f)
        }
    }
}