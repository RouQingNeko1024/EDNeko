package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.karhu

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.minecraft.init.Blocks

object Karhu : SpeedMode("Karhu") {
    private var wasSpeed = false
    override fun onUpdate() {
        mc.thePlayer.jumpMovementFactor = 0.0265f
        if (mc.thePlayer.isMoving) {
            if (mc.thePlayer.onGround) mc.thePlayer.jump()
            wasSpeed = true
        } else if (wasSpeed) {
            mc.thePlayer.stopXZ()
            wasSpeed = false
        }
    }

    override fun onDisable() {
        mc.gameSettings.keyBindJump.pressed = false
    }

    override fun onJump(event: JumpEvent) {
        val block = mc.theWorld.getBlockState(mc.thePlayer.position.down()).block
        event.motion = when (block) {
            Blocks.water, Blocks.lava -> 0f
            Blocks.slime_block, Blocks.soul_sand -> 0.39f
            else -> 0.415f
        }
    }
}