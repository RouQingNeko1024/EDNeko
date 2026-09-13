package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object Matrix6632 : NoFallMode("Matrix6.6.3-2") {

    private var send = false
    private var wasWorking = false

    override fun onEnable() {
        send = false
        wasWorking = false
    }

    override fun onDisable() {
        if (wasWorking) {
            mc.timer.timerSpeed = 1f
        }
    }

    override fun onUpdate() {
        val isWorking = mc.thePlayer.fallDistance - mc.thePlayer.motionY > 3

        if (isWorking) {
            mc.thePlayer.fallDistance = 0f
            send = true
            mc.timer.timerSpeed = 0.5f
        } else if (wasWorking) {
            mc.timer.timerSpeed = 1f
        }

        wasWorking = isWorking
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