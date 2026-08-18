/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object VanillaHop : SpeedMode("VanillaHop") {

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (!player.isMoving && Speed.vanillaHopFastStop) {
            player.stopXZ()
            return
        }

        if (player.onGround && Speed.vanillaHopJump && player.isMoving) {
            player.jump()
        }

        MovementUtils.strafe(Speed.vanillaHopSpeed)
    }
}