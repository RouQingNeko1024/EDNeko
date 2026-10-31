package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.cos
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class StrafeVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Strafe", parent) {

    val percent by float("Percent", 0f, 0f..100f)
    val onlyGround by boolean("OnlyGround", false)
    val onlyMove by boolean("OnlyMove", false)

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        if (onlyGround && !mc.thePlayer.onGround) return
        if (onlyMove && !mc.thePlayer.isMoving) return

        val p = 1f - percent / 100f
        val motionX = packet.realMotionX * p
        val motionZ = packet.realMotionZ * p

        val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
        val speed = Math.sqrt(motionX * motionX + motionZ * motionZ)

        packet.realMotionX = -sin(yaw) * speed
        packet.realMotionZ = cos(yaw) * speed
        packet.realMotionY *= p
    }
}