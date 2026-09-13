package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.grim

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object Grim117 : NoFallMode("Grim1.17+") {
    override fun onTick() {
        if (!mc.thePlayer.onGround && mc.thePlayer.fallDistance > 2f) {
            sendPacket(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY + 1.0E-9,
                    mc.thePlayer.posZ,
                    mc.thePlayer.rotationYaw,
                    mc.thePlayer.rotationPitch,
                    mc.thePlayer.onGround
                )
            )
            mc.thePlayer.fallDistance = 0f
        }
    }
}