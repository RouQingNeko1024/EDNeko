package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class MatrixTestFly(parent: LoftilyFly) : LoftilyFlyMode("MatrixTest", parent) {

    val timerSpeed by float("Matrix-Timer", 1.0f, 1f..10f)
    val matrixSpeed by float("Matrix-Speed", 0.16f, 0.1f..0.7f)
    val packets by float("Matrix-Packets", 4f, 1f..10f)
    val packetMode by choices("Matrix-PacketMode", arrayOf("C06", "C04"), "C06")

    private val serverBuffer = ArrayDeque<Packet<*>>()

    override fun onDisable() {
        if (serverBuffer.isNotEmpty()) {
            serverBuffer.forEach { PacketUtils.handlePackets(it) }
        }
        serverBuffer.clear()
        mc.timer.timerSpeed = 1.0f
    }

    override fun onPacketReceive(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            event.cancelEvent()
            serverBuffer.add(event.packet)
        }
    }

    override fun onWorld() {
        serverBuffer.clear()
    }

    override fun onPacketSend(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }
        if (event.packet is C03PacketPlayer) {
            event.cancelEvent()
        }
    }

    override fun onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }
        val player = mc.thePlayer
        mc.timer.timerSpeed = timerSpeed
        player.noClip = true
        stopMovement(true)
        setSpeed(matrixSpeed.toDouble(), true)
        when (packetMode) {
            "C06" -> repeat(packets.toInt().coerceAtLeast(0)) {
                PacketUtils.sendPacket(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        player.posX, player.posY, player.posZ,
                        currentYaw, currentPitch, player.onGround
                    ),
                    false
                )
            }
            else -> repeat(packets.toInt().coerceAtLeast(0)) {
                PacketUtils.sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        player.posX, player.posY, player.posZ, player.onGround
                    ),
                    false
                )
            }
        }
    }
}