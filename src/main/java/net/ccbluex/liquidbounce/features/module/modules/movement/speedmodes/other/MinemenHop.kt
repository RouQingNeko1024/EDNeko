/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.config.BoolValue
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object MinemenHop : SpeedMode("MinemenHop") {
    private val knockbackAbuse = BoolValue("MinemenHopKnockbackAbuse", false)

    override fun onMotion() {
        val player = mc.thePlayer ?: return
        if (player.isMoving) {
            if (player.hurtTime < 6 || knockbackAbuse.get()) {
                MovementUtils.strafe()
            }
            if (player.onGround) {
                player.jump()
                MovementUtils.strafe()
            }
        } else {
            player.motionX = 0.0
            player.motionZ = 0.0
        }
    }
}
