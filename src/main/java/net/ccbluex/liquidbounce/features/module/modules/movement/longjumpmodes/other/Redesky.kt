/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode

object Redesky : LongJumpMode("Redesky") {
    override fun onUpdate() {
        mc.thePlayer.jumpMovementFactor = 0.15f
        mc.thePlayer.motionY += 0.05f
    }
}