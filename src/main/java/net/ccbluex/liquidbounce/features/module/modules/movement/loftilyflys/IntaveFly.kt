package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S32PacketConfirmTransaction

class IntaveFly(parent: LoftilyFly) : LoftilyFlyMode("Intave", parent) {

    private var receivedFlag = false
    private val serverBuffer = ArrayDeque<Packet<*>>()

    override fun onDisable() {
        mc.thePlayer.noClip = false
        mc.timer.timerSpeed = 1.0f
        mc.thePlayer.capabilities.isFlying = false
        if (serverBuffer.isNotEmpty()) {
            serverBuffer.forEach { PacketUtils.handlePackets(it) }
        }
        serverBuffer.clear()
        receivedFlag = false
    }

    override fun onMotion(event: MotionEvent) {
        event.onGround = true
    }

    override fun onUpdate() {
        val player = mc.thePlayer
        mc.timer.timerSpeed = 10.0f
        player.noClip = true
        if (!player.isMoving) {
            player.motionX *= 0.0
            player.motionZ *= 0.0
            player.motionY *= 0.0
        }
        player.capabilities.isFlying = true
        player.capabilities.flySpeed = 0.0085f
    }

    override fun onPacketReceive(event: PacketEvent) {
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook) {
            receivedFlag = true
        }
        if ((packet is S32PacketConfirmTransaction || packet is S12PacketEntityVelocity) && receivedFlag) {
            event.cancelEvent()
            serverBuffer.add(packet)
        }
    }
}