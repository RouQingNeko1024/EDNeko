/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.config.BoolValue
import net.ccbluex.liquidbounce.config.FloatValue
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object LatestVerusHop : SpeedMode("LatestVerusHop") {
    private val customSpeed = BoolValue("LatestVerusHop-CustomSpeed", false)
    private val jumpMovementFactorPotion = FloatValue("LatestVerusHop-JumpMovementFactorWithPotion", 0.02f, 0.01f..0.04f)
    private val jumpMovementFactorNoPotion = FloatValue("LatestVerusHop-JumpMovementFactorWithoutPotion", 0.02f, 0.01f..0.04f)
    private val frictionPotion = FloatValue("LatestVerusHop-FrictionWithPotion", 0.48f, 0.1f..2f)
    private val frictionNoPotion = FloatValue("LatestVerusHop-FrictionWithoutPotion", 0.48f, 0.1f..2f)
    private val speedPotion = FloatValue("LatestVerusHop-SpeedWithPotion", 2.8f, 1f..4f)
    private val speedNoPotion = FloatValue("LatestVerusHop-SpeedWithoutPotion", 2.0f, 1f..4f)
    private val damageBoost = BoolValue("LatestVerusHop-DamageBoost", false)
    private val boostSpeed = FloatValue("LatestVerusHop-BoostSpeed", 1f, 0.1f..9f)

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        val hasSpeed = player.isPotionActive(Potion.moveSpeed)

        player.jumpMovementFactor = if (customSpeed.get()) {
            if (hasSpeed) jumpMovementFactorPotion.get() else jumpMovementFactorNoPotion.get()
        } else {
            0.02f
        }

        player.speedInAir = if (customSpeed.get()) {
            (if (hasSpeed) speedPotion.get() else speedNoPotion.get()) / 100f
        } else if (hasSpeed) {
            0.028f
        } else {
            0.02f
        }

        mc.gameSettings.keyBindJump.pressed = false

        if (damageBoost.get() && player.hurtTime == 9) {
            MovementUtils.strafe(boostSpeed.get())
        }

        if (player.onGround && player.isMoving) {
            player.jump()
            player.motionY = 0.41999998688697815
            MovementUtils.strafe(if (customSpeed.get()) {
                if (hasSpeed) frictionPotion.get() else frictionNoPotion.get()
            } else {
                0.48f
            })
        }

        MovementUtils.strafe()
    }

    override fun onDisable() {
        mc.thePlayer?.speedInAir = 0.02f
    }
}
