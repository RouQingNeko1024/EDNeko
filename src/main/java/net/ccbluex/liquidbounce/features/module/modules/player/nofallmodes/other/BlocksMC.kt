package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object BlocksMC : NoFallMode("BlocksMC") {
    private var shouldClip = false

    override fun onTick() {
        if (mc.thePlayer.motionY < -0.7) {
            shouldClip = true
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer) {
            if (mc.thePlayer.onGround && shouldClip) {
                packet.y -= 0.1
            }
        } else if (packet is S08PacketPlayerPosLook) {
            shouldClip = false
        }
    }
}