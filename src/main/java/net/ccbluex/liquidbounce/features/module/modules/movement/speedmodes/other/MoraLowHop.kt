/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object MoraLowHop : SpeedMode("MoraLowHop") {
    override fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return
        if (event.eventState == EventState.POST && player.isMoving && !player.isInLiquid) {
            player.jumpMovementFactor += 0.00222f
            if (player.fallDistance <= 1f) {
                if (player.onGround) {
                    player.jump()
                    player.motionX *= 1.0123
                    player.motionZ *= 1.0123
                } else {
                    player.motionY -= 0.0151
                    player.motionX *= 1.00156
                    player.motionZ *= 1.00156
                }
            }
        }
    }

    override fun onEnable() {
        val player = mc.thePlayer ?: return
        if (player.onGround) {
            player.motionZ = 0.0
            player.motionX = 0.0
        }
    }

    override fun onDisable() {
        mc.thePlayer?.jumpMovementFactor = 0.02f
    }
}
