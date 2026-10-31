package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.minecraft.network.play.client.C03PacketPlayer
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import kotlin.math.abs
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class NormalVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("Normal", parent) {

    val horizontal by float("Horizontal", 0f, -100f..100f)
    val vertical by float("Vertical", 0f, -100f..100f)
    val delay by boolean("Delay", false)
    val skipTick by boolean("SkipTick", false)
    val skipticksValue by int("SkipTicksValue", 4, 1..20) { skipTick }
    val explosion by boolean("Explosion", false)
    val hurtTime by boolean("HurtTime", false)
    val moveCheck by boolean("MoveCheck", false)
    val basicDamageBoost by float("BasicDamageBoost", 12f, -100f..100f)
    val minValue by floatRange("MinValue", 0.5f..1f, 0.1f..1f)
    val speed by boolean("Speed", false)
    val speedTimer by boolean("SpeedTimer", false)
    val hurts by boolean("Hurts", false)
    val timeractive by boolean("TimerActive", false)
    val timerSpeed by float("TimerSpeed", 1f, 0.1f..5f) { timeractive }

    private var ticks = 0
    private var wasOnGround = false

    override fun onEnable() {
        ticks = 0
        wasOnGround = false
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return
        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        val h = horizontal / 100f
        val v = vertical / 100f

        if (KillAura.target != null && KillAura.handleEvents()) {
            val dist = mc.thePlayer.getDistanceToEntity(KillAura.target)
            if (dist < basicDamageBoost) {
                packet.realMotionX = (packet.realMotionX * (h + 1) * 1.5)
                packet.realMotionY = (packet.realMotionY * (v + 1) * 1.5)
                packet.realMotionZ = (packet.realMotionZ * (h + 1) * 1.5)
                return
            }
        }

        if (skipTick && ticks < skipticksValue) return

        if (delay) wasOnGround = mc.thePlayer.onGround

        if (explosion && packet.realMotionY > 0) return

        if (hurtTime && mc.thePlayer.hurtTime > 0) return

        if (moveCheck && !mc.thePlayer.isMoving) {
            packet.realMotionX = 0.0
            packet.realMotionZ = 0.0
        }

        val finalH = h + 1.0
        val finalV = v + 1.0

        val minPct = minValue.start
        val maxPct = minValue.endInclusive

        val actualH = if (abs(finalH) < minPct) {
            if (finalH >= 0) maxPct * packet.realMotionX - packet.realMotionX
            else minPct * packet.realMotionX - packet.realMotionX
        } else finalH

        val actualV = if (abs(finalV) < minPct) {
            if (finalV >= 0) maxPct * packet.realMotionY - packet.realMotionY
            else minPct * packet.realMotionY - packet.realMotionY
        } else finalV

        packet.realMotionX = packet.realMotionX * actualH
        packet.realMotionY = packet.realMotionY * actualV
        packet.realMotionZ = packet.realMotionZ * actualH

        if (hurts && packet.realMotionY < 0.004 || mc.thePlayer.hurtTime > 0) {
            packet.realMotionX = 0.0
            packet.realMotionY = 0.0
            packet.realMotionZ = 0.0
        }
    }

    override fun onMotion(event: MotionEvent) {
        ticks++

        if (event.eventState != EventState.PRE) return

        if (delay && wasOnGround && mc.thePlayer.onGround) return

        if (speed && KillAura.target != null && mc.thePlayer.isMoving) {
            MovementUtils.speed = 0.5f
        }

        if (speedTimer && KillAura.target != null) {
            if (!mc.thePlayer.isMoving) {
                mc.timer.timerSpeed = 1f
                return
            }

            mc.timer.timerSpeed = timerSpeed
            val motionX = mc.thePlayer.motionX * (1f / timerSpeed + 0.2)
            val motionZ = mc.thePlayer.motionZ * (1f / timerSpeed + 0.2)
            if (KillAura.target != null && KillAura.handleEvents() && mc.thePlayer.isMoving) {
                mc.thePlayer.motionX = motionX
                mc.thePlayer.motionZ = motionZ
            }
        }

        if (timeractive) {
            mc.timer.timerSpeed = timerSpeed
        }
    }
}