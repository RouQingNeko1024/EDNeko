package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos
import kotlin.math.floor

object MatrixSpoof : NoFallMode("MatrixSpoof") {

    private var timered = false

    override fun onEnable() {
        timered = false
    }

    override fun onDisable() {
        try {
            mc.timer.timerSpeed = 1f
        } catch (_: Throwable) {}
        timered = false
    }

    private fun fallDamage(): Boolean {
        val player = mc.thePlayer ?: return false
        return (player.fallDistance - player.motionY) > NoFall.matrixMinFallDistance
    }

    private fun inVoidCheck(): Boolean {
        if (!NoFall.matrixNoVoid) return true
        return !isInVoid()
    }

    private fun isInVoid(): Boolean {
        val player = mc.thePlayer ?: return false
        val world = mc.theWorld ?: return false

        val px = floor(player.posX).toInt()
        val pz = floor(player.posZ).toInt()
        val startY = floor(player.posY).toInt()

        for (y in startY downTo 0) {
            val block = world.getBlockState(BlockPos(px, y, pz)).block
            if (block != Blocks.air) {
                return false
            }
        }
        return true
    }

    override fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet

        try {
            if (fallDamage() && inVoidCheck()) {
                if (packet is C03PacketPlayer) {
                    event.cancelEvent()

                    val px = player.posX
                    val py = player.posY
                    val pz = player.posZ

                    sendPacket(C04PacketPlayerPosition(px, py, pz, true), false)
                    sendPacket(C04PacketPlayerPosition(px, py, pz, false), false)

                    player.fallDistance = 0f

                    if (NoFall.matrixLegitTimer) {
                        timered = true
                        try {
                            mc.timer.timerSpeed = 0.2f
                        } catch (_: Throwable) {}
                    }
                }
            } else if (timered) {
                try {
                    mc.timer.timerSpeed = 1f
                } catch (_: Throwable) {}
                timered = false
            }
        } catch (_: Throwable) {}
    }
}