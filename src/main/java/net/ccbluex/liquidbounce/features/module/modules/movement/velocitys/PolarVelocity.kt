package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class PolarVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Polar", parent) {

    val horizontal by float("Horizontal", 0f, -100f..100f)

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        val h = horizontal / 100

        packet.realMotionX *= (h + 1)
        packet.realMotionZ *= (h + 1)
    }
}