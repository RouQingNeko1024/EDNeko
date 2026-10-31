package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import kotlin.math.abs
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class ReverseVelocity2(parent: LoftilyVelocity) : LoftilyVelocityMode("Reverse2", parent) {

    val horizontal by float("Horizontal", -1f, -2f..2f)
    val vertical by float("Vertical", -1f, -2f..2f)
    val horizontal2 by float("Horizontal2", -1f, -2f..2f)

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        packet.realMotionX *= horizontal
        packet.realMotionY *= vertical
        packet.realMotionZ *= horizontal2
    }
}