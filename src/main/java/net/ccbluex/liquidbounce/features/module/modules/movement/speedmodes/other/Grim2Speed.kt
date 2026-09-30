/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.riseskid.grimspeed.MoveUtil
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity

/**
 * Grim2 —— 移植自 Rise-6.9.5 的 Grim2Speed。
 * 双包欺骗 + 每隔一格交替的 moveFlying 微加速。
 */
object Grim2Speed : SpeedMode("Grim2") {

    private var shouldJump = false
    private var strafeTicks = 0
    private var groundTicks = 0

    override fun onEnable() {
        sendDoublePacket()
        strafeTicks = 0
        groundTicks = 0
        mc.timer.timerSpeed = 1f
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    override fun onMotion() {
        shouldJump = false
    }

    override fun onPostMotion() {
        // Rise 在 PostMotionEvent 发双包（在运动包之后）；lizz 现在有 POST 钩子了，必须放这里才与 Rise 一致
        if (strafeTicks % 2 == 0) {
            sendDoublePacket()
        }
    }

    override fun onStrafe() {
        val player = mc.thePlayer ?: return

        groundTicks = if (player.onGround) groundTicks + 1 else 0

        if (strafeTicks > -1) {
            var boost = 0.03
            if (strafeTicks % 2 == 0) {
                boost = if (player.onGround) 0.085 else 0.03
            }
            MoveUtil.moveFlying(boost * Speed.grim2Speed.toDouble())
        }
        strafeTicks++
    }

    override fun onJump(event: JumpEvent) {
        if (event.eventState == EventState.PRE && mc.thePlayer?.isJumping == false) {
            event.cancelEvent()
        }
    }

    override fun onMoveInput(event: MovementInputEvent) {
        if (shouldJump) {
            event.originalInput.jump = true
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.eventType != EventState.RECEIVE) return

        when (val packet = event.packet) {
            is S08PacketPlayerPosLook -> {
                if (strafeTicks % 2 == 1) strafeTicks++
                mc.timer.timerSpeed = 1f
            }
            is S12PacketEntityVelocity -> {
                if (packet.entityID == mc.thePlayer?.entityId) {
                    shouldJump = true
                }
            }
        }
    }

    private fun sendDoublePacket() {
        if (Speed.grim2HighPing) {
            PacketUtils.sendPacket(C03PacketPlayer(false), triggerEvent = false)
            PacketUtils.sendPacket(C03PacketPlayer(false), triggerEvent = false)
        } else {
            PacketUtils.sendPacket(C03PacketPlayer(true), triggerEvent = false)
            PacketUtils.sendPacket(C03PacketPlayer(false), triggerEvent = false)
        }
    }
}
