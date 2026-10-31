package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.minecraft.network.play.client.C03PacketPlayer
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class MatrixVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Matrix", parent) {

    private val delayTimer = MSTimer()
    private var tpId = 0
    private var active = false

    override fun onEnable() {
        active = false
        tpId = 0
        delayTimer.reset()
    }

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        if (event.isCancelled || mc.thePlayer == null) return

        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        event.cancelEvent()
        active = true
        delayTimer.reset()

        PacketUtils.sendPacket(C03PacketPlayer.C06PacketPlayerPosLook(
            0.0, 0.0, 0.0, 0f, 0f, false
        ))
        tpId++

        PacketUtils.sendPacket(C03PacketPlayer.C06PacketPlayerPosLook(
            mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
            mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false
        ))

        PacketUtils.sendPacket(C03PacketPlayer.C06PacketPlayerPosLook(
            0.0, 0.0, 0.0, 0f, 0f, false
        ))
        tpId++

        PacketUtils.sendPacket(C03PacketPlayer.C06PacketPlayerPosLook(
            mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
            mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false
        ))

        mc.thePlayer.motionY = packet.realMotionY
    }

    override fun onMotion(event: MotionEvent) {
        if (!active) return
        if (event.eventState != EventState.PRE) return
        if (!delayTimer.hasTimePassed(50)) return

        if (!mc.thePlayer.onGround) {
            mc.thePlayer.motionY = -0.078
            val d = MovementUtils.speed.toDouble()
            if (d < 0.2) {
                MovementUtils.strafe(0.1f)
            } else if (d > 0.2) {
                MovementUtils.strafe((d * 0.9).toFloat())
            }
        }
        active = false
    }
}