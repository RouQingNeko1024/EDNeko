/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.config.FloatValue
import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.block.BlockAir
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper

object AACPortFDP : SpeedMode("AACPortFDP") {
    private val portMode = ListValue("AACPortFDP-Mode", arrayOf("AACPort", "AACYPort", "AACYPort2"), "AACPort")
    private val portLength = FloatValue("AACPortFDP-Length", 1F, 1F..20F)

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (portMode.get() != "AACPort" || !player.isMoving) return

        val yaw = player.rotationYaw * 0.017453292f
        var distance = 0.2
        while (distance <= portLength.get()) {
            val x = player.posX - MathHelper.sin(yaw) * distance
            val z = player.posZ + MathHelper.cos(yaw) * distance

            if (player.posY < player.posY.toInt() + 0.5 &&
                world.getBlockState(BlockPos(x, player.posY, z)).block !is BlockAir
            ) {
                break
            }

            player.sendQueue.addToSendQueue(C04PacketPlayerPosition(x, player.posY, z, true))
            distance += 0.2
        }
    }

    override fun onMotion() {
        val player = mc.thePlayer ?: return

        when (portMode.get()) {
            "AACYPort" -> {
                if (player.isMoving && !player.isSneaking) {
                    player.cameraPitch = 0f
                    if (player.onGround) {
                        player.motionY = 0.3425
                        player.motionX *= 1.5893
                        player.motionZ *= 1.5893
                    } else {
                        player.motionY = -0.19
                    }
                }
            }

            "AACYPort2" -> {
                if (player.isMoving) {
                    player.cameraPitch = 0f
                    if (player.onGround) {
                        player.jump()
                        player.motionY = 0.3851
                        player.motionX *= 1.01
                        player.motionZ *= 1.01
                    } else {
                        player.motionY = -0.21
                    }
                }
            }
        }
    }
}
