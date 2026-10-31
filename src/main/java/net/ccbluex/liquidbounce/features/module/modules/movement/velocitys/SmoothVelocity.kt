package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class SmoothVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Smooth", parent) {

    val reduceFactor by float("ReduceFactor", 0.5f, 0f..1f)

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        packet.realMotionX = packet.realMotionX * reduceFactor
        packet.realMotionY = packet.realMotionY * reduceFactor
        packet.realMotionZ = packet.realMotionZ * reduceFactor
    }
}