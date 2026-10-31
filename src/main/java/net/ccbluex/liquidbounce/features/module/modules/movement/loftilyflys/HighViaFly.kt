package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class HighViaFly(parent: LoftilyFly) : LoftilyFlyMode("HighViaPacket", parent) {

    val horizontalSpeed by float("HighVia-HorizontalSpeed", 1.0f, 0f..5f)
    val verticalSpeed by float("HighVia-VerticalSpeed", 1.0f, 0f..5f)
    val noClip by boolean("NoClip", false)
    val packets by float("OffsetPackets", 1f, 0f..10f)
    val yPackets by float("InvalidYPackets", 1f, 0f..10f)
    val teleportSpeed by float("TeleportSpeed", 11f, 0f..999f)
    val silentFlag by boolean("SilentFlag", false)

    private val packetBus = ArrayDeque<Packet<*>>()

    override fun onDisable() {
        mc.thePlayer.capabilities.isFlying = false
        mc.thePlayer.capabilities.flySpeed = 0.05f
        if (packetBus.isNotEmpty()) {
            packetBus.forEach { PacketUtils.handlePackets(it) }
            packetBus.clear()
        }
        mc.thePlayer.noClip = false
    }

    override fun onPacketReceive(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            if (!silentFlag) {
                event.cancelEvent()
                packetBus.add(packet)
            }
        }
    }

    override fun onPacketSend(event: PacketEvent) {
        val packet = event.packet
        if (packet is C03PacketPlayer.C06PacketPlayerPosLook) {
            event.cancelEvent()
            PacketUtils.sendPacket(
                C03PacketPlayer.C04PacketPlayerPosition(
                    packet.x, packet.y, packet.z, packet.onGround
                )
            )
        }
    }

    override fun onUpdate() {
        val player = mc.thePlayer
        stopMovement(true)
        setSpeed(horizontalSpeed.toDouble(), true)
        if (mc.gameSettings.keyBindJump.isKeyDown) {
            player.motionY = verticalSpeed.toDouble()
        }
        if (mc.gameSettings.keyBindSneak.isKeyDown) {
            player.motionY = -verticalSpeed.toDouble()
        }
        if (noClip) {
            player.noClip = true
        }
        repeat(packets.toInt().coerceAtLeast(0)) {
            PacketUtils.sendPacket(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    player.posX + 999.0, player.posY, player.posZ + 999.0,
                    player.rotationYaw, player.rotationPitch, false
                )
            )
        }
        repeat(yPackets.toInt().coerceAtLeast(0)) {
            PacketUtils.sendPacket(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    player.posX + 999.0, player.posY - 50.0, player.posZ + 999.0,
                    player.rotationYaw, player.rotationPitch, false
                )
            )
        }
        if (teleportSpeed != 0f) {
            player.setPosition(
                player.posX + player.motionX * teleportSpeed.toDouble(),
                player.posY,
                player.posZ + player.motionZ * teleportSpeed.toDouble()
            )
        }
    }
}