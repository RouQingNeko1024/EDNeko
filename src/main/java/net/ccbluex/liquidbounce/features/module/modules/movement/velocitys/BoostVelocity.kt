package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.minecraft.network.play.client.C03PacketPlayer
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import kotlin.math.abs
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class BoostVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Boost", parent) {

    val horizontal by float("Horizontal", 0f, -100f..100f)
    val vertical by float("Vertical", 0f, -100f..100f)
    val zeroVertical by float("ZeroVertical", 0.42f, 0f..1f)

    private var sendC03 = false

    override fun onEnable() {
        sendC03 = false
    }

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

        if (abs(packet.realMotionY) < 0.05 && abs(packet.realMotionX) < 0.05 && abs(packet.realMotionZ) < 0.05) {
            sendC03 = true
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (!sendC03) return
        if (event.eventState != EventState.PRE) return

        val posX = mc.thePlayer.posX
        val posY = mc.thePlayer.posY
        val posZ = mc.thePlayer.posZ

        for (i in 0 until 3) {
            PacketUtils.sendPacket(C03PacketPlayer.C04PacketPlayerPosition(posX, posY + zeroVertical, posZ, false), false)
            PacketUtils.sendPacket(C03PacketPlayer.C04PacketPlayerPosition(posX, posY, posZ, false), false)
        }

        PacketUtils.sendPacket(C03PacketPlayer.C04PacketPlayerPosition(posX, posY + 0.08, posZ, false), false)
        sendC03 = false
    }
}