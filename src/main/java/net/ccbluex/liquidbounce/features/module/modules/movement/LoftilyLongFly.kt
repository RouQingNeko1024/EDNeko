package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LoftilyLongFly : Module("LoftilyLongFly", Category.MOVEMENT) {

    private val timer1 by float("LagTimer", 1f, 0.1f..1f)
    private val timer2 by float("LaunchTimer", 1f, 0.1f..1f)
    private val lagSpeed by float("LagSpeed", 1.4f, -2.4f..2.4f)
    private val boostSpeed by float("BoostSpeed", 8.3f, 0f..10f)
    private val noHeadMove by boolean("NoHeadMove", false)
    private val ticks by float("Ticks", 5f, 1f..12f)
    private val silent by boolean("Silent", false)
    private val strafe by boolean("Strafe", false)
    private val autoToggle by boolean("AutoToggle", false)
    private val resetMotion by boolean("ResetMotion", false) { !autoToggle }

    private var release = false
    private var boost = false
    private var flag = false
    private var doBoost = false
    private var send = false
    private var canStrafe = false
    private var offGroundTicks = 0

    override fun onDisable() {
        if (!autoToggle && resetMotion && boost) {
            mc.thePlayer?.stop()
        }
        release = false
        boost = false
        flag = false
        doBoost = false
        send = false
        canStrafe = false
        offGroundTicks = 0
        mc.timer.timerSpeed = 1f
    }

    private fun disable() {
        if (autoToggle) {
            toggle()
        } else {
            doBoost = false
        }
    }

    private fun getDirection(): Double {
        val p = mc.thePlayer ?: return 0.0
        var yaw = p.rotationYaw.toDouble()
        val forward = p.moveForward
        val strafeAmount = p.moveStrafing
        if (forward < 0f) yaw += 180.0
        var f = 1f
        if (forward < 0f) f = -0.5f else if (forward > 0f) f = 0.5f
        if (strafeAmount > 0f) yaw -= 90.0 * f
        if (strafeAmount < 0f) yaw += 90.0 * f
        return Math.toRadians(yaw)
    }

    private fun getMovingYaw(): Double {
        val p = mc.thePlayer ?: return 0.0
        var yaw = p.rotationYaw.toDouble()
        val forward = p.moveForward
        val strafeAmount = p.moveStrafing
        if (forward == 0f && strafeAmount == 0f) return Math.toRadians(yaw)
        if (forward != 0f) {
            if (strafeAmount > 0f) yaw += (if (forward > 0f) -45 else 45).toDouble()
            else if (strafeAmount < 0f) yaw += (if (forward > 0f) 45 else -45).toDouble()
            else if (forward < 0f) yaw += 180.0
        } else {
            yaw += if (strafeAmount > 0f) -90.0 else 90.0
        }
        return Math.toRadians(yaw)
    }

    val onStrafe = handler<StrafeEvent> { event ->
        val p = mc.thePlayer ?: return@handler
        if (p.onGround) {
            canStrafe = false
        }
        if (canStrafe && strafe && p.isMoving) {
            val shotSpeed = sqrt(p.motionX * p.motionX + p.motionZ * p.motionZ)
            val speed = shotSpeed * 0.9
            val motionX = p.motionX * 0.09999999999999998
            val motionZ = p.motionZ * 0.09999999999999998
            val yaw = getMovingYaw()
            p.motionX = -sin(yaw) * speed + motionX
            p.motionZ = cos(yaw) * speed + motionZ
        }
    }

    val onPacketSend = handler<PacketEvent> { event ->
        if (event.eventType != EventState.SEND) return@handler
        val p = mc.thePlayer ?: return@handler
        val packet = event.packet

        if (silent && (packet is C03PacketPlayer.C05PacketPlayerLook || packet is C03PacketPlayer.C06PacketPlayerPosLook) && noHeadMove) {
            event.cancelEvent()
            if (packet is C03PacketPlayer.C04PacketPlayerPosition || packet is C03PacketPlayer.C06PacketPlayerPosLook) {
                PacketUtils.sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(packet.x, packet.y, packet.z, packet.onGround)
                )
            }
        }

        if (doBoost && packet is C03PacketPlayer) {
            if (!flag) {
                event.cancelEvent()
                val x = p.posX + -sin(getDirection()) * boostSpeed
                val z = p.posZ + cos(getDirection()) * boostSpeed
                PacketUtils.sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(x, p.posY + 0.42, z, false),
                    false
                )
                boost = true
            }
            if (flag && packet is C03PacketPlayer.C06PacketPlayerPosLook) {
                MovementUtils.setSpeed(boostSpeed.toDouble(), false)
                p.motionY = 0.42
                disable()
            }
        }
    }

    val onPacketReceive = handler<PacketEvent> { event ->
        if (event.eventType != EventState.RECEIVE) return@handler
        if (event.packet is S08PacketPlayerPosLook) {
            if (!doBoost) {
                flag = true
            } else if (boost) {
                flag = true
            }
            canStrafe = true
        }
    }

    val onMove = handler<MoveEvent> { event ->
        val p = mc.thePlayer ?: return@handler
        if (silent && doBoost && !flag) {
            event.isSafeWalk = true
            event.cancelEvent()
            p.motionX = 0.0
            p.motionY = 0.0
            p.motionZ = 0.0
        }
    }

    val onUpdate = handler<GameTickEvent> {
        val p = mc.thePlayer ?: return@handler

        offGroundTicks = if (p.onGround) 0 else offGroundTicks + 1

        if (!doBoost && !(flag && boost)) {
            if (release && offGroundTicks.toDouble() >= ticks) {
                doBoost = true
            }
            if (p.fallDistance > 0.5f && !release) {
                send = true
            }
            if (send && !flag) {
                MovementUtils.setSpeed(lagSpeed.toDouble(), false)
                p.motionY = 0.42
            }
            if (flag && send) {
                MovementUtils.setSpeed(lagSpeed.toDouble(), false)
                p.motionY = 0.42
                offGroundTicks = 0
                release = true
                flag = false
                send = false
                p.fallDistance = 0f
            }
        }

        if (doBoost) {
            if (!silent) {
                MovementUtils.setSpeed(boostSpeed.toDouble(), false)
                p.motionY = 0.42
                boost = true
                if (flag) {
                    disable()
                }
            } else {
                p.stop()
                p.setPosition(p.lastTickPosX, p.lastTickPosY, p.lastTickPosZ)
            }
        }

        mc.timer.timerSpeed = if (release) {
            if (flag) timer2 else timer1
        } else 1f
    }
}