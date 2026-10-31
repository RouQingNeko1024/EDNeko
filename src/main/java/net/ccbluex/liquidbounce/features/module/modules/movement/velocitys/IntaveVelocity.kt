package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import kotlin.math.abs
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class IntaveVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Intave", parent) {

    val horizontal by float("Horizontal", 0f, -100f..100f)
    val vertical by float("Vertical", 0f, -100f..100f)
    val balanceCheck by boolean("BalanceCheck", false)
    val balanceThreshold by float("BalanceThreshold", 0.01f, 0f..1f) { balanceCheck }

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        val h = horizontal / 100
        val v = vertical / 100

        packet.realMotionX = packet.realMotionX * (h + 1)
        packet.realMotionY = packet.realMotionY * (v + 1)
        packet.realMotionZ = packet.realMotionZ * (h + 1)

        if (balanceCheck) {
            val absY = abs(packet.realMotionY)
            val absXZ = abs(packet.realMotionX) + abs(packet.realMotionZ)

            if (absXZ > balanceThreshold && absY > balanceThreshold) {
                if (absY > absXZ) {
                    packet.realMotionX *= 0.5
                    packet.realMotionZ *= 0.5
                }
            }
        }
    }
}