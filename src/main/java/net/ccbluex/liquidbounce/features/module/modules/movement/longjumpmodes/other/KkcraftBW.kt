package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object KkcraftBW : LongJumpMode("KkcraftBW") {

    private var canBoost = false

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (!player.isMoving) return

        if (player.onGround && LongJump.kkcraftBWJump) {
            player.jump()
            canBoost = true
        }

        val yaw = MovementUtils.direction
        val spd = if (LongJump.kkcraftBWBoost && canBoost) {
            canBoost = false
            LongJump.kkcraftBWSpeed * LongJump.kkcraftBWBoostMultiplier
        } else {
            LongJump.kkcraftBWSpeed
        }

        player.motionX = -Math.sin(yaw) * spd
        player.motionZ = Math.cos(yaw) * spd
    }

    override fun onEnable() {
        canBoost = false
    }

    override fun onDisable() {
        canBoost = false
    }
}