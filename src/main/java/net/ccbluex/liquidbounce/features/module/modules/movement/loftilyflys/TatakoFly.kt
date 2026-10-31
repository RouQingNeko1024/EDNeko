package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.via.ViaMCPHelper
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class TatakoFly(parent: LoftilyFly) : LoftilyFlyMode("Tatako", parent) {

    val horizontalSpeed by float("Tatako-HorizontalSpeed", 1.0f, 0f..5f)
    val verticalSpeed by float("Tatako-VerticalSpeed", 1.0f, 0f..5f)

    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    override fun onEnable() {
        x = mc.thePlayer.posX
        y = mc.thePlayer.posY
        z = mc.thePlayer.posZ
        super.onEnable()
    }

    override fun onDisable() {
        val player = mc.thePlayer
        PacketUtils.sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(player.posX + 0.05, player.posY, player.posZ, player.rotationYaw, player.rotationPitch, true),
            false
        )
        PacketUtils.sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(player.posX, player.posY + 0.42, player.posZ, player.rotationYaw, player.rotationPitch, true),
            false
        )
        PacketUtils.sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(player.posX, player.posY + 0.7532, player.posZ, player.rotationYaw, player.rotationPitch, true),
            false
        )
        PacketUtils.sendPacket(
            C03PacketPlayer.C06PacketPlayerPosLook(player.posX, player.posY + 1.0, player.posZ, player.rotationYaw, player.rotationPitch, true),
            false
        )
        super.onDisable()
    }

    override fun onPreMotion(event: MotionEvent) {
        stopMovement(true)
        setSpeed(horizontalSpeed.toDouble(), true)
        playerMotion()
    }

    private fun playerMotion() {
        val player = mc.thePlayer
        if (mc.gameSettings.keyBindJump.isKeyDown) {
            player.motionY = verticalSpeed.toDouble()
        }
        if (mc.gameSettings.keyBindSneak.isKeyDown) {
            player.motionY = -verticalSpeed.toDouble()
        }
    }

    override fun onPacketSend(event: PacketEvent) {
        val packet = event.packet
        if (packet is C03PacketPlayer) {
            y -= 0.09
            packet.x = x
            packet.y = y
            packet.z = z
            packet.onGround = true
        }
    }

    override fun onPacketReceive(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            x = packet.getX()
            y = packet.getY()
            z = packet.getZ()
            // Ship CPacketConfirmTeleport through the ViaMCP protocol stack at runtime.
            ViaMCPHelper.confirmTeleport()
            PacketUtils.sendPacket(
                C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, packet.getYaw(), packet.getPitch(), false),
                false
            )
            event.cancelEvent()
        }
    }
}