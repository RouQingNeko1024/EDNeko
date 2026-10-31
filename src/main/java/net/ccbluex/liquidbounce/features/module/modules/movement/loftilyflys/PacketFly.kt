package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class PacketFly(parent: LoftilyFly) : LoftilyFlyMode("Packet", parent) {

    val speed by float("Speed", 11f, 1f..50f)

    override fun onPacketReceive(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            // 原框架 FlagHandler.flagSilentPos = true
            // 由于 EDNeko 未移植 PositionPacketHandler，此处留空处理静默位置标志
        }
    }

    override fun onMove(event: MoveEvent) {
        // EDNeko 的 MoveEvent 仅在玩家 moveEntity 时触发，等价于原框架 event.getEntity() == mc.player 的判断
        event.cancelEvent()
    }

    override fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) {
            PacketUtils.sendPacket(C03PacketPlayer.C04PacketPlayerPosition())
        } else {
            mc.thePlayer.setPosition(
                mc.thePlayer.posX + -Math.sin(MovementUtils.direction.toDouble()) * speed.toDouble(),
                mc.thePlayer.posY + 5.0E-4,
                mc.thePlayer.posZ + Math.cos(MovementUtils.direction.toDouble()) * speed.toDouble()
            )
        }
    }
}