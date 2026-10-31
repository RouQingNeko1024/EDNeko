package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class CancelVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Cancel", parent) {

    val noC03 by boolean("NoC03", false)

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        if (noC03) {
            packet.realMotionX = 0.0
            packet.realMotionY = 0.0
            packet.realMotionZ = 0.0
        } else {
            event.cancelEvent()
        }
    }
}