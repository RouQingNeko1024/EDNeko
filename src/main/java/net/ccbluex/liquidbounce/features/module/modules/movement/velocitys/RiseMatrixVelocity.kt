package net.ccbluex.liquidbounce.features.module.modules.movement.velocitys
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.config.*
import net.minecraft.network.play.server.S12PacketEntityVelocity
class RiseMatrixVelocity(parent: LoftilyVelocity) : LoftilyVelocityMode("RiseMatrix", parent) {

    override fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null) return
        if (event.eventType != EventState.RECEIVE) return

        val packet = event.packet
        if (packet !is S12PacketEntityVelocity) return
        if (packet.entityID != mc.thePlayer.entityId) return

        mc.thePlayer.motionY = packet.realMotionY

        if (!Speed.handleEvents() && mc.thePlayer.isMoving) {
            event.cancelEvent()
        }
    }

    override fun onStrafe(event: StrafeEvent) {
        if (mc.thePlayer == null) return

        val n = mc.thePlayer.hurtTime

        if (!mc.thePlayer.isMoving && n == 1) {
            mc.thePlayer.motionX *= -0.1
            mc.thePlayer.motionZ *= -0.1
        } else if (n == 1 && !Speed.handleEvents()) {
            MovementUtils.strafe()
        }

        if (n < 6 && Speed.handleEvents()) {
            MovementUtils.strafe()
        } else if (n > 1 && !Speed.handleEvents() && n < 15 && !Fly.handleEvents()) {
            mc.thePlayer.motionY -= 0.00348
        }
    }
}