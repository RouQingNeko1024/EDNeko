package net.ccbluex.liquidbounce.utils.firefly.general

import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.network.play.client.C0BPacketEntityAction

@Suppress("UnusedReceiverParameter")
object SomeUtil {
    val mc = Minecraft.getMinecraft()!!

    fun EntityPlayerSP.isHurting(): Boolean {
        return hurtTime > 0
    }

    fun EntityPlayerSP.isFalling(): Boolean {
        return !onGround && motionY < 0.0
    }

    fun changeTimer(timerSpeed: Float) {
        mc.timer.timerSpeed = timerSpeed
    }

    fun reduceXZ(factor: Double) {
        val player = mc.thePlayer ?: return
        player.motionX *= factor
        player.motionZ *= factor
    }

    fun changeSprint(
        setState: Boolean = true,
        sendPacketToServer: Boolean = true,
        forceSilent: Boolean = false,
        forceChange: Boolean = false
    ) {
        val player = mc.thePlayer ?: return

        if (setState && (!player.isSprinting || forceChange)) {
            player.isSprinting = true
            if (sendPacketToServer) {
                sendPacket(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING))
            }
        } else if (!setState && (player.isSprinting || forceChange)) {
            player.isSprinting = false
            if (sendPacketToServer) {
                sendPacket(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING))
            }
        }
    }
}