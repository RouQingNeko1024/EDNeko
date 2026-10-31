package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import kotlin.math.sin
import net.minecraft.network.play.client.C03PacketPlayer
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import kotlin.math.abs
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import kotlin.math.cos
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
class LegitVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Legit", parent) {

    val jumponce by boolean("JumpOnce", false)
    val boost by float("Boost", 1f, 0f..10f)
    val motionReduce by float("MotionReduce", 0.6f, 0f..1f)
    val timer by boolean("Timer", false)
    val timerBoost by float("TimerBoost", 2f, 1f..10f) { timer }
    val speed by boolean("Speed", false)

    private var jumpNextTick = false
    private var boosted = false
    private var jumped = false

    override fun onEnable() {
        jumpNextTick = false
        boosted = false
        jumped = false
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        if (jumponce && boost > 1) {
            if (jumped) {
                packet.realMotionX *= motionReduce
                packet.realMotionZ *= motionReduce
                return
            }
            if (boosted) {
                packet.realMotionX *= boost
                packet.realMotionZ *= boost
                jumpNextTick = true
                boosted = false
            } else {
                boosted = true
            }
            return
        }

        if (timer) {
            mc.timer.timerSpeed = timerBoost
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.PRE) return

        if (jumpNextTick) {
            mc.thePlayer.jump()
            jumped = true
            jumpNextTick = false
        }

        if (speed && KillAura.handleEvents()) {
            val target = KillAura.target ?: return
            val distance = mc.thePlayer.getDistanceToEntity(target)
            val factor = min(boost.toDouble(), 4.0)
            if (distance < 2.5 && mc.thePlayer.isMoving) {
                MovementUtils.speed = (MovementUtils.speed * factor).toFloat()
            }
        }
    }

    override fun onJump(event: JumpEvent) {
        if (jumped && mc.thePlayer.isMoving) {
            val yaw = MovementUtils.direction
            mc.thePlayer.motionX += -Math.sin(yaw) * 0.2
            mc.thePlayer.motionZ += Math.cos(yaw) * 0.2
            jumped = false
        }
    }
}