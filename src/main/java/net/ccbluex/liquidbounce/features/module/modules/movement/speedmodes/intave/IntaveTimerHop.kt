/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object IntaveTimerHop : SpeedMode("IntaveTimerHop") {
    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isMoving) {
            if (player.onGround) {
                mc.gameSettings.keyBindJump.pressed = false
                player.jump()
            }
            if (!player.onGround && player.fallDistance <= 0.1) {
                mc.timer.timerSpeed = 1.4f
            }
            if (player.fallDistance > 0.1 && player.fallDistance < 1.3) {
                mc.timer.timerSpeed = 0.7f
            }
            if (player.fallDistance >= 1.3) {
                mc.timer.timerSpeed = 1f
            }
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }
}
