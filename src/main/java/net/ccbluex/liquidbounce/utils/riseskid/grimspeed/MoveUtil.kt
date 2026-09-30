package net.ccbluex.liquidbounce.utils.riseskid.grimspeed

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import kotlin.math.cos
import kotlin.math.sin

object MoveUtil : MinecraftInstance {

    fun moveFlying(amount: Double) {
        val player = mc.thePlayer ?: return
        if (!player.isMoving) return
        val yaw = MovementUtils.direction
        player.motionX += -sin(yaw) * amount
        player.motionZ += cos(yaw) * amount
    }
}