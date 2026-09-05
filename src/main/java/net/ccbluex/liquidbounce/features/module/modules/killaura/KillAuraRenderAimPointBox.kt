package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import java.awt.Color

object KillAuraRenderAimPointBox : Module("KillAura-RenderAimPointBox", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val renderPointBoxAim by boolean("RenderAimPointBox", false).subjective()
    val aimPointBoxColor by color("AimPointBoxColor", Color.CYAN) { renderPointBoxAim }.subjective()
    val aimPointBoxSize by float("AimPointBoxSize", 0.1f, 0f..0.2F) { renderPointBoxAim }.subjective()
}