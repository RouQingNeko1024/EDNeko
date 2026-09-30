/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object GrimVertical : FlyMode("GrimVertical") {

    private var ticks = 0
    private var exploited = false
    private var playerStuckTicks = 0

    override fun onEnable() {
        ticks = 0
        exploited = false
        playerStuckTicks = 0
        mc.timer.timerSpeed = 1f
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    override fun onTick() {
        val player = mc.thePlayer ?: return

        if (player.fallDistance > 2f && Fly.autoDisable) {
            Fly.state = false
        }

        if (ticks == 0) {
            if (player.onGround) {
                player.jump()
            }
        } else if (ticks <= 5) {
            mc.timer.timerSpeed = Fly.grimVerticalTimer
        } else {
            mc.timer.timerSpeed = 1f
        }

        ticks++

        if (exploited || ticks == 2) {
            exploited = false
            sendPacket(
                C04PacketPlayerPosition(
                    player.posX + 114514.0,
                    -1.0,
                    player.posZ + 1919180.0,
                    false
                )
            )
        }

        if (ticks > 2) {
            playerStuckTicks++
        }
    }
}