/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.network.play.client.C03PacketPlayer

object Grim : SpeedMode("Grim") {

    private var airTick = 0

    override fun onStrafe() {
        val player = mc.thePlayer ?: return

        if (player.onGround && player.isMoving) {
            player.tryJump()
        }
    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        player.isSprinting = player.movementInput.moveForward > 0.8

        if (player.isInLiquid || player.isInWeb || player.isOnLadder) {
            mc.timer.timerSpeed = 1f
            airTick = 0
            return
        }

        if (!player.isMoving) {
            mc.timer.timerSpeed = 1f
            airTick = 0
            return
        }

        if (player.onGround) {
            airTick = 0
        } else {
            airTick++
        }

        if (Speed.grimTimerBoost) {
            if (player.onGround) {
                mc.timer.timerSpeed = 1f
            } else {
                when (airTick % 3) {
                    0 -> mc.timer.timerSpeed = Speed.grimAirTimer
                    else -> mc.timer.timerSpeed = 1f
                }
            }
        } else {
            mc.timer.timerSpeed = 1f
        }

        if (Speed.grimPullDown && !player.onGround && airTick >= Speed.grimPullDownTicks.toInt() && player.motionY < 0) {
            player.motionY = -0.1523351824467155
        }
    }

    override fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet

        if (packet !is C03PacketPlayer) return

        if (Speed.grimSpoofGround && !player.onGround && player.fallDistance < 0.5f && player.motionY < 0) {
            packet.onGround = true
        }
    }

    override fun onDisable() {
        airTick = 0
        mc.timer.timerSpeed = 1f
    }
}