/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.config.FloatValue
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.direction
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import kotlin.math.cos
import kotlin.math.sin

object GroundPacket : SpeedMode("GroundPacket") {
    private val moveSpeed = FloatValue("GroundPacketSpeed", 0.6f, 0.27f..5f)
    private val baseSpeed = FloatValue("GroundPacketDistPerPacket", 0.15f, 0.12f..0.2873f)

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (!player.onGround) return

        val maxDistance = moveSpeed.get().toDouble()
        val step = baseSpeed.get().toDouble()
        val yaw = direction
        var travelled = step

        while (travelled <= maxDistance) {
            val distance = if (travelled + step > maxDistance) maxDistance - travelled else step
            val motionX = -sin(yaw) * distance
            val motionZ = cos(yaw) * distance

            player.setPosition(player.posX + motionX, player.posY, player.posZ + motionZ)
            mc.netHandler.addToSendQueue(
                C04PacketPlayerPosition(player.posX + motionX, player.posY, player.posZ + motionZ, player.onGround)
            )
            travelled += step
        }
    }
}
