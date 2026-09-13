package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.grim

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.block.BlockAir
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.BlockPos
import kotlin.math.sqrt

object OldGrim : NoFallMode("OldGrim") {

    private var shouldCancel = false
    private var waitingForGround = false
    private var fallState = 0

    override fun onEnable() {
        shouldCancel = false
        waitingForGround = false
        fallState = 0
    }

    override fun onDisable() {
        shouldCancel = false
        waitingForGround = false
        fallState = 0
    }

    override fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return

        if (player.onGround) {
            if (fallState > 0) {
                fallState = 0
                shouldCancel = false
                waitingForGround = false
            }
            return
        }

        if (player.fallDistance >= 3.0f && fallState == 0) {
            fallState = 1
            shouldCancel = true
            waitingForGround = true
            player.motionX *= 0.2
            player.motionZ *= 0.2
        }

        if (fallState == 1 && player.fallDistance >= 4.0f) {
            val speed = sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)

            if (isCloseToGround(2.0) && speed > 0.19) {
                player.motionX = player.motionX / speed * 0.19
                player.motionZ = player.motionZ / speed * 0.19
            }

            if (isCloseToGround(1.0) && speed < 0.2) {
                event.onGround = true
                player.fallDistance = 0.0f
                fallState = 2
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet

        if (packet is C03PacketPlayer && shouldCancel) {
            when (fallState) {
                1 -> {
                    if (!player.onGround) {
                        event.cancelEvent()
                    } else {
                        event.cancelEvent()
                        sendPacket(C03PacketPlayer.C04PacketPlayerPosition(
                            packet.x, packet.y, packet.z, true
                        ))
                        shouldCancel = false
                        waitingForGround = false
                    }
                }
                2 -> {
                    if (packet is C03PacketPlayer.C04PacketPlayerPosition ||
                        packet is C03PacketPlayer.C06PacketPlayerPosLook) {
                        event.cancelEvent()
                        sendPacket(C03PacketPlayer.C04PacketPlayerPosition(
                            packet.x, packet.y, packet.z, true
                        ))
                    }
                }
            }
        }
    }

    private fun isCloseToGround(distance: Double): Boolean {
        val player = mc.thePlayer ?: return false
        for (i in 0 until distance.toInt() + 2) {
            val pos = BlockPos(player.posX, player.posY - i, player.posZ)
            val block = mc.theWorld.getBlockState(pos).block
            if (block !is BlockAir) {
                return true
            }
        }
        return false
    }
}