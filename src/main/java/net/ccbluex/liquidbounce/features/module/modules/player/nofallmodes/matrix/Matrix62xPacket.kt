package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object Matrix62xPacket : NoFallMode("Matrix6.2.xPacket") {
    private var needPacket = false

    override fun onUpdate() {
        if (mc.thePlayer.fallDistance > 3.0f) {
            needPacket = true
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer && needPacket) {
            event.cancelEvent()
            sendPacket(
                C03PacketPlayer.C04PacketPlayerPosition(
                    event.packet.x,
                    event.packet.y,
                    event.packet.z,
                    true
                )
            )
            sendPacket(
                C03PacketPlayer.C04PacketPlayerPosition(
                    event.packet.x,
                    event.packet.y,
                    event.packet.z,
                    false
                )
            )
            mc.thePlayer.fallDistance = 0f
            needPacket = false
        }
    }
}