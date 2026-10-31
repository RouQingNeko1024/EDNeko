package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S06PacketUpdateHealth
import net.minecraft.network.play.server.S07PacketRespawn
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S40PacketDisconnect
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class DelayJumpVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("DelayJump", parent) {

    val clickAmounts by float("AttackAmounts", 1f, 0f..8f)
    val maxTicks by int("MaxTicks", 7, 1..100)
    val rotation by boolean("Rotation", false)
    val rotationTimesValue by float("RotationTimes", 2f, 0f..5f) { rotation }
    val onlyGround by boolean("OnlyGround", false)

    private val packets = LinkedList<net.minecraft.network.Packet<*>>()
    private var velocityRotation: Rotation? = null
    private val timer = MSTimer()
    private var attack = false
    private var delay = false
    private var reduce = false
    private var ticks = 0
    private var ticksSinceJump = 0

    override fun onDisable() {
        attack = false
        delay = false
        while (packets.isNotEmpty()) {
            val packet = packets.poll()
            PacketUtils.handlePackets(packet)
        }
    }

    override fun onWorld(event: WorldEvent) {
        packets.clear()
        attack = false
    }

    override fun onMotion(event: MotionEvent) {
        ticksSinceJump++

        if (mc.thePlayer.hurtTime <= 0) {
            mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        }

        if (rotation) {
            if (velocityRotation != null && ticks > 0 && mc.currentScreen == null) {
                velocityRotation!!.fixedSensitivity().toPlayer()
                computeMovementKeys()
                ticks--
            } else {
                resetKeys()
            }
        }

        if (attack) {
            mc.gameSettings.keyBindJump.pressed = true
            attack = false
            reduce = true
            ticksSinceJump = 0
        }

        if (reduce && mc.currentScreen == null) {
            val targetEntity = KillAura.target ?: return
            if (clickAmounts > 0 && mc.thePlayer.isSprinting) {
                var i = 0
                while (i < clickAmounts.toInt()) {
                    mc.thePlayer.swingItem()
                    PacketUtils.sendPacket(C02PacketUseEntity(targetEntity, C02PacketUseEntity.Action.ATTACK))
                    i++
                }
                mc.thePlayer.motionX *= 0.6
                mc.thePlayer.motionZ *= 0.6
                mc.thePlayer.isSprinting = false
                reduce = false
            }
        }

        if (ticksSinceJump >= 5) {
            reduce = false
        }

        if (event.eventState == EventState.POST) {
            onGameLoop()
        }
    }

    private fun onGameLoop() {
        if (!delay) return
        val shouldRelease = !onlyGround || mc.thePlayer.onGround || timer.hasTimePassed(maxTicks * 50L) || mc.thePlayer.isDead
        if (!shouldRelease) return

        while (packets.isNotEmpty()) {
            val packet = packets.poll()
            PacketUtils.handlePackets(packet)
        }
        computeMovementKeys()
        attack = true
        delay = false
        ticks = rotationTimesValue.toInt()
    }

    override fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            packets.clear()
            attack = false
            delay = false
            return
        }

        if (event.isCancelled) return

        if (event.eventType != EventState.RECEIVE) return

        val packet = event.packet

        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId) {
            val x = packet.motionX
            val z = packet.motionZ
            if (!(x == 0 && z == 0 || mc.thePlayer.isOnLadder || mc.thePlayer.isInWater || mc.thePlayer.isInLava)) {
                event.cancelEvent()
                packets.add(packet)
                delay = true
                timer.reset()

                if (rotation) {
                    val knockback = Vec3(
                        x.toDouble() / -8000.0,
                        0.0,
                        z.toDouble() / -8000.0
                    )
                    val lookAt = mc.thePlayer.positionVector.addVector(knockback.xCoord, 0.0, knockback.zCoord)
                    val currentPitch = mc.thePlayer.rotationPitch
                    val rot = RotationUtils.toRotation(lookAt, false)
                    velocityRotation = smoothRotation(
                        Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch),
                        Rotation(rot.yaw, currentPitch),
                        500f, 500f
                    ).fixedSensitivity(0f)
                    computeMovementKeys()
                }
            }
            return
        }

        if (delay) {
            if (packet is S02PacketChat || packet is S06PacketUpdateHealth || packet is S40PacketDisconnect
                || packet is S03PacketTimeUpdate || packet is S29PacketSoundEffect) {
                return
            }
            if (packet is S07PacketRespawn) {
                onDisable()
                return
            }
            event.cancelEvent()
            packets.add(packet)
        }
    }

    private fun smoothRotation(from: Rotation, to: Rotation, yawSpeed: Float, pitchSpeed: Float): Rotation {
        val yawDiff = MathHelper.wrapAngleTo180_float(to.yaw - from.yaw)
        val pitchDiff = MathHelper.wrapAngleTo180_float(to.pitch - from.pitch)

        val stepYaw = yawDiff.coerceIn(-yawSpeed, yawSpeed)
        val stepPitch = pitchDiff.coerceIn(-pitchSpeed, pitchSpeed)

        return Rotation(from.yaw + stepYaw, from.pitch + stepPitch)
    }

    private fun resetKeys() {
        if (mc.currentScreen != null) {
            mc.gameSettings.keyBindForward.pressed = false
            mc.gameSettings.keyBindBack.pressed = false
            mc.gameSettings.keyBindLeft.pressed = false
            mc.gameSettings.keyBindRight.pressed = false
            return
        }
        mc.gameSettings.keyBindForward.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.keyCode)
        mc.gameSettings.keyBindBack.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.keyCode)
        mc.gameSettings.keyBindLeft.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.keyCode)
        mc.gameSettings.keyBindRight.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.keyCode)
    }

    private fun computeMovementKeys() {
        if (!rotation || mc.currentScreen != null) return

        val deltaYaw = MathHelper.wrapAngleTo180_float(velocityRotation!!.yaw - mc.thePlayer.rotationYaw)
        resetKeys()
        val radian = Math.toRadians(deltaYaw.toDouble())
        val x = Math.sin(radian)
        val z = Math.cos(radian)

        if (z > 0.5) mc.gameSettings.keyBindForward.pressed = true
        else if (z < -0.5) mc.gameSettings.keyBindBack.pressed = true
        if (x > 0.5) mc.gameSettings.keyBindRight.pressed = true
        else if (x < -0.5) mc.gameSettings.keyBindLeft.pressed = true
    }
}