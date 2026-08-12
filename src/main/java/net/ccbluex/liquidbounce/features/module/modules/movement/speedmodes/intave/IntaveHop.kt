/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.config.BoolValue
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.client.settings.GameSettings

object IntaveHop : SpeedMode("IntaveHop") {
    private val groundStrafe = BoolValue("IntaveHopStrafe", false)

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)

        if (player.isMoving) {
            if (player.onGround) {
                mc.gameSettings.keyBindJump.pressed = false
                mc.timer.timerSpeed = 1.0f
                if (groundStrafe.get()) MovementUtils.strafe()
                player.jump()
            }

            if (player.motionY > 0.003) {
                player.motionX *= 1.0015
                player.motionZ *= 1.0015
                mc.timer.timerSpeed = 1.06f
            }
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }
}
