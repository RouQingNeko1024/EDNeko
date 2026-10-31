package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import kotlin.math.sin
import kotlin.math.abs
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import kotlin.math.cos
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class MotMatrixVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("MotMatrix", parent) {

    private val delayTimer = MSTimer()
    private var ticks = 0
    private var active = false
    private var prevSpeed = 0.0

    override fun onEnable() {
        active = false
        ticks = 0
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
        ticks = 20
        delayTimer.reset()
        prevSpeed = MovementUtils.speed.toDouble()
        mc.thePlayer.motionY = packet.realMotionY
    }

    override fun onMotion(event: MotionEvent) {
        if (!active) return
        if (event.eventState != EventState.PRE) return

        if (ticks > 0) {
            ticks--
            if (ticks == 18 && MovementUtils.speed > 0.25) {
                val d = Math.toRadians((RotationUtils.currentRotation?.yaw ?: mc.thePlayer.rotationYaw).toDouble())
                val d2 = -sin(d) * 0.009
                val d3 = cos(d) * 0.009
                mc.thePlayer.motionX += d2
                mc.thePlayer.motionZ += d3
            }
        } else {
            active = false
        }
    }
}