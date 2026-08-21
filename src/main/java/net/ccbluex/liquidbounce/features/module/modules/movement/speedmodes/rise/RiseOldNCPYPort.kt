package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object RiseOldNCPYPort : SpeedMode("RiseOldNCPYPort") {

    private var speed = 0.2873
    private var stage = 2
    private var lastHorizontalDistance = 0.0

    override fun onMotion() {
        val player = mc.thePlayer ?: return
        val dx = player.posX - player.lastTickPosX
        val dz = player.posZ - player.lastTickPosZ
        lastHorizontalDistance = Math.sqrt(dx * dx + dz * dz)

        if (stage == 3) {
            player.setPosition(player.posX, player.posY + 0.4, player.posZ)
        }
    }

    override fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

        when (stage) {
            2 -> {
                speed *= 2.14
                stage = 3
            }
            3 -> {
                stage = 2
                val d0 = 0.66 * (lastHorizontalDistance - 0.2873)
                speed = lastHorizontalDistance - d0
            }
            else -> {
                if (mc.theWorld.getCollidingBoundingBoxes(player, player.entityBoundingBox.offset(0.0, player.motionY, 0.0)).isNotEmpty()
                    || player.isCollidedVertically
                ) {
                    stage = 1
                }
                speed = lastHorizontalDistance - lastHorizontalDistance / 159.0
            }
        }

        if (player.isCollidedHorizontally) {
            speed = 0.2873
        }

        if (MovementUtils.airTicks == 1 && MovementUtils.groundTicks > 2) {
            speed = 0.3873
        }

        if (!player.onGround) {
            stage++
        }

        MovementUtils.strafe(speed.toFloat())
    }

    override fun onJump(event: JumpEvent) {
        event.motion = 0.4f
    }

    override fun onDisable() {
        speed = 0.0
    }

    override fun onEnable() {
        stage = 2
        speed = 0.2873
        lastHorizontalDistance = 0.0
    }
}