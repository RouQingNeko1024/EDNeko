package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object KillAuraTargeting : Module("KillAura-Targeting", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val priority by choices(
        "Priority", arrayOf(
            "Health", "Distance", "Direction", "LivingTime", "Armor",
            "HurtResistance", "HurtTime", "HealthAbsorption", "RegenAmplifier",
            "OnLadder", "InLiquid", "InWeb"
        ), "Distance"
    ) { true }

    val targetMode by choices(
        "TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch"
    ) { true }

    val limitedMultiTargets by int("LimitedMultiTargets", 0, 0..50) {
        targetMode == "Multi"
    }

    val maxSwitchFOV by float("MaxSwitchFOV", 90f, 30f..180f) {
        targetMode == "Switch"
    }

    val switchDelay by int("SwitchDelay", 15, 1..1000) {
        targetMode == "Switch"
    }

    val fov by float("FOV", 180f, 0f..180f) { true }
}