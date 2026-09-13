package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object Matrix663 : NoFallMode("Matrix6.6.3") {
    private var send = false
    private var timer = false

    override fun onEnable() {
        send = false
        timer = false
    }

    override fun onUpdate() {
        if (timer) {
            mc.timer.timerSpeed = 1f
            timer = false
        }

        if (mc.thePlayer.fallDistance - mc.thePlayer.motionY > 3f) {
            mc.thePlayer.fallDistance = 0f
            mc.timer.timerSpeed = 0.5f
            send = true
            timer = true
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer && send) {
            send = false
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
        }
    }
}