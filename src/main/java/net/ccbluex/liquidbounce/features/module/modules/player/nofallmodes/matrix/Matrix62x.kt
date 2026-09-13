package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object Matrix62x : NoFallMode("Matrix6.2.x") {
    private var tick = 0
    private var needPacket = false

    override fun onEnable() {
        tick = 0
        needPacket = false
    }

    override fun onUpdate() {
        if (mc.thePlayer.fallDistance > 3.0f) {
            needPacket = true
            tick = 0
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer && needPacket) {
            tick++
            if (tick == 1) {
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
}