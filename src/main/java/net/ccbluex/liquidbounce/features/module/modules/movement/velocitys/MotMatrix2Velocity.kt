package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import kotlin.math.abs
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class MotMatrix2Velocity(parent: LoftilyVelocity) : LoftilyVelocityMode("MotMatrix2", parent) {

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        if (event.isCancelled || mc.thePlayer == null) return

        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        val d = packet.realMotionY
        val d2 = packet.realMotionX
        val d3 = packet.realMotionZ

        if ((d < 0.1 || d > 0.5 || abs(d2) > 1.0 || abs(d3) > 1.0) &&
            (!mc.thePlayer.onGround || !mc.thePlayer.isCollidedVertically)) {
            event.cancelEvent()
        } else {
            mc.thePlayer.motionY = d
            event.cancelEvent()
        }
    }
}