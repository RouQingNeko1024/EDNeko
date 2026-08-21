package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseWatchdog6Tick : SpeedMode("RiseWatchdog6Tick") {

    private var tickCounter = 0
    private var onStairs = false

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (tickCounter < 8 || onStairs || player.hurtTime < 6) {
            if (player.onGround) {
                player.tryJump()
            }

            if (player.onGround && !player.isPotionActive(Potion.moveSpeed)) {
                MovementUtils.strafe(MovementUtils.speed - 0.01f)
            } else if (player.onGround) {
                MovementUtils.strafe(0.26f)
            }

            if (MovementUtils.speed < 0.125f) {
                MovementUtils.strafe(0.125f)
            }

            when (MovementUtils.airTicks) {
                1 -> {
                    MovementUtils.strafe()
                    player.motionY += 0.057
                }
                3 -> {
                    player.motionY -= 0.1309
                }
                4 -> {
                    player.motionY -= 0.2
                }
            }
        }

        if (tickCounter == 24) {
            MovementUtils.strafe(0.125f)
        }

        tickCounter++
    }

    override fun onMotion() {
        val player = mc.thePlayer ?: return

        if (tickCounter < 23) {
            if (player.onGround && tickCounter > 3) {
                player.setPosition(player.posX, player.posY + if (tickCounter % 2 != 0) 0.296875 else 0.001, player.posZ)
            }
        } else if (player.onGround) {
            player.setPosition(player.posX, player.posY + 0.001, player.posZ)
        }
    }

    override fun onJump(event: net.ccbluex.liquidbounce.event.JumpEvent) {
        if (tickCounter >= 23 && tickCounter >= 8 && !onStairs && mc.thePlayer?.hurtTime ?: 0 >= 6) {
            event.motion = 0.4f
        } else {
            event.motion = 0.42f
        }
    }

    override fun onEnable() {
        tickCounter = 0
        val player = mc.thePlayer ?: return
        if (player.onGround) {
            player.tryJump()
        }
    }

    override fun onDisable() {
        val player = mc.thePlayer ?: return
        if (player.onGround) {
            player.stopXZ()
        }
        player.stepHeight = 0.6f
    }
}