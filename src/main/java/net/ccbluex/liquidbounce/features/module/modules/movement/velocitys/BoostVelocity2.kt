package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import kotlin.math.abs
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class BoostVelocity2(parent: LoftilyVelocity) : LoftilyVelocityMode("Boost2", parent) {

    val horizontal by float("Horizontal", 0f, -100f..100f)
    val vertical by float("Vertical", 0f, -100f..100f)
    val minMotion by float("MinMotion", 0.1f, 0f..1f)

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        val h = horizontal / 100
        val v = vertical / 100

        if (abs(packet.realMotionX) > minMotion || abs(packet.realMotionZ) > minMotion) {
            packet.realMotionX = packet.realMotionX * (h + 1)
            packet.realMotionZ = packet.realMotionZ * (h + 1)
        }
        if (abs(packet.realMotionY) > minMotion) {
            packet.realMotionY = packet.realMotionY * (v + 1)
        }
    }
}