/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.config.FloatValue
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils

object VanillaHop : SpeedMode("VanillaHop") {
    private val vanillaSpeed = FloatValue("VanillaHop-Speed", 1F, 0.1F..9.5F)

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.onGround && player.isMoving) {
            player.jump()
            MovementUtils.strafe(vanillaSpeed.get())
        }
        MovementUtils.strafe()
    }
}
