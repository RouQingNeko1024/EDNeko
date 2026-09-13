package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import java.util.concurrent.CopyOnWriteArrayList

object AAC5 : FlyMode("AAC5") {

    private var aac5FlyClip = false
    private var aac5FlyStart = false
    private var aac5nextFlag = false

    private val aac5C03List = CopyOnWriteArrayList<C03PacketPlayer>()

    override fun onEnable() {
        aac5FlyClip = false
        aac5FlyStart = false
        aac5nextFlag = false
        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.42, mc.thePlayer.posZ)
    }

    override fun onDisable() {
        if (mc.isSingleplayer) return
        sendAAC5Packets()
        mc.thePlayer.noClip = false
        mc.timer.timerSpeed = 1f
    }

    override fun onUpdate() {
        mc.thePlayer.noClip = !mc.thePlayer.isMoving
        if (!aac5FlyStart) {
            mc.thePlayer.motionY = 0.0
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.jumpMovementFactor = 0.00f
            mc.timer.timerSpeed = 0.32f
            return
        } else {
            if (!aac5FlyClip) {
                mc.timer.timerSpeed = 0.19f
            } else {
                aac5FlyClip = false
                mc.timer.timerSpeed = 1.2f
            }
        }
        mc.thePlayer.motionY = 0.0
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        mc.thePlayer.capabilities.isFlying = false
        if (mc.gameSettings.keyBindJump.isKeyDown) mc.thePlayer.motionY += 3.0
        if (mc.gameSettings.keyBindSneak.isKeyDown) mc.thePlayer.motionY -= 3.0
        MovementUtils.strafe(3.0f)
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            aac5FlyStart = true
            aac5FlyClip = true
            mc.timer.timerSpeed = 1.3f
            aac5nextFlag = true
        }
        if (packet is C03PacketPlayer) {
            event.cancelEvent()
            val f = mc.thePlayer.width / 2.0
            if (aac5nextFlag || !mc.theWorld.checkBlockCollision(
                    AxisAlignedBB(
                        packet.x - f,
                        packet.y,
                        packet.z - f,
                        packet.x + f,
                        packet.y + mc.thePlayer.height,
                        packet.z + f
                    )
                )
            ) {
                aac5C03List.add(packet)
                aac5nextFlag = false
                event.cancelEvent()
                if (aac5C03List.size > 7) {
                    sendAAC5Packets()
                }
            }
        }
    }

    private fun sendAAC5Packets() {
        var yaw = mc.thePlayer.rotationYaw
        var pitch = mc.thePlayer.rotationPitch
        for (pkt in aac5C03List) {
            if (pkt.isMoving) {
                PacketUtils.sendPacket(pkt, false)
                if (pkt.getRotating()) {
                    yaw = pkt.yaw
                    pitch = pkt.pitch
                }
                PacketUtils.sendPacket(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        pkt.x,
                        -1e+159,
                        pkt.z + 10,
                        yaw,
                        pitch,
                        true
                    ), false
                )
                PacketUtils.sendPacket(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        pkt.x,
                        pkt.y,
                        pkt.z,
                        yaw,
                        pitch,
                        true
                    )
                )
            }
        }
        aac5C03List.clear()
    }
}