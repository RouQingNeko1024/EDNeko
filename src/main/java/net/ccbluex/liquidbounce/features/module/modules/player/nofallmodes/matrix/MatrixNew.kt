package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import kotlin.math.sqrt

object MatrixNew : NoFallMode("MatrixNew") {

    override fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return

        if (!isBlockUnder()) {
            return
        }

        val distance = player.fallDistance

        if (distance > 2) {
            MovementUtils.strafe(0.19f)
        }

        if (distance > 3 && getSpeed() < 0.2) {
            event.onGround = true
            player.fallDistance = 0f
        }
    }

    override fun onPacket(event: PacketEvent) {}

    private fun isBlockUnder(): Boolean {
        val player = mc.thePlayer ?: return false

        for (i in 0..1) {
            val blockPos = BlockPos(player.posX, player.posY - i, player.posZ)
            if (mc.theWorld.getBlockState(blockPos).block != Blocks.air) {
                return true
            }
        }
        return false
    }

    private fun getSpeed(): Double {
        val player = mc.thePlayer ?: return 0.0
        return sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)
    }
}