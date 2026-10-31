package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import kotlin.math.abs
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class ReverseVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Reverse", parent) {

    val horizontal by float("Horizontal", -1f, -2f..2f)
    val vertical by float("Vertical", -1f, -2f..2f)
    val minMotion by float("MinMotion", 0.1f, 0f..1f)

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        if (abs(packet.realMotionX) > minMotion || abs(packet.realMotionZ) > minMotion) {
            packet.realMotionX *= horizontal
            packet.realMotionZ *= horizontal
        }
        if (abs(packet.realMotionY) > minMotion) {
            packet.realMotionY *= vertical
        }
    }
}