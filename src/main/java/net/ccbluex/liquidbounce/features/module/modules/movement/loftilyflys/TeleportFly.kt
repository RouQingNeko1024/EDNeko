package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.minecraft.network.play.client.C03PacketPlayer

class TeleportFly(parent: LoftilyFly) : LoftilyFlyMode("TeleportFly", parent) {

    val packets by float("PacketAmounts", 1f, 1f..20f)
    val speed by float("Speed", 0.5f, 0f..20f)
    val tpWhenPacket by boolean("TPWhenPacketEvent", true)
    val reSendPacket by boolean("ReSendWhenPacketEvent", false)
    val reSendAmounts by float("ReSendAmounts", 1f, 1f..20f) { reSendPacket }
    val spoofGround by boolean("SpoofGround", false)

    override fun onUpdate() {
        val player = mc.thePlayer
        val yaw = Math.toRadians(player.rotationYaw.toDouble())
        val forward = speed.toDouble()
        val x = player.posX + -Math.sin(yaw) * forward
        val z = player.posZ + Math.cos(yaw) * forward
        val y = player.posY
        repeat(packets.toInt().coerceAtLeast(0)) {
            PacketUtils.sendPacket(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    x, y, z, player.rotationYaw, player.rotationPitch, spoofGround
                )
            )
        }
        if (!tpWhenPacket) {
            player.setPosition(x, y, z)
        }
    }

    override fun onPacketSend(event: PacketEvent) {
        val packet = event.packet
        if (packet !is C03PacketPlayer.C06PacketPlayerPosLook) {
            return
        }
        val player = mc.thePlayer
        val forward = speed.toDouble()
        val yaw = Math.toRadians(packet.yaw.toDouble())
        val x = packet.x + -Math.sin(yaw) * forward
        val z = packet.z + Math.cos(yaw) * forward
        val y = packet.y
        if (reSendPacket) {
            repeat(reSendAmounts.toInt().coerceAtLeast(0)) {
                PacketUtils.sendPacket(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        x, y, z, player.rotationYaw, player.rotationPitch, spoofGround
                    ),
                    false
                )
            }
        }
        if (tpWhenPacket) {
            player.setPosition(packet.x, packet.y, packet.z)
        }
    }
}