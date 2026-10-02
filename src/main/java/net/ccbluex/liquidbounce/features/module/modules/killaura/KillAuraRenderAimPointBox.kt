package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import java.awt.Color

object KillAuraRenderAimPointBox : Module("KillAura-RenderAimPointBox", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val renderPointBoxAim by boolean("RenderAimPointBox", false) { true }
    val aimPointStyle by choices("AimPointStyle", arrayOf("Box", "Circle", "Square", "SquareOutline"), "Box") { renderPointBoxAim }.subjective()
    val aimPointBoxColor by color("AimPointBoxColor", Color.CYAN) { renderPointBoxAim }.subjective()
    val aimPointBoxSize by float("AimPointBoxSize", 0.1f, 0f..0.2F) { renderPointBoxAim }.subjective()
    val aimPointGlow by boolean("AimPointGlow", true) { renderPointBoxAim }.subjective()
    val aimPointGlowIntensity by float("AimPointGlowIntensity", 5f, 1f..20f) { renderPointBoxAim && aimPointGlow }.subjective()
    val aimPointLine by boolean("AimPointLine", true) { renderPointBoxAim }.subjective()
    val aimPointLineColor by color("AimPointLineColor", Color.CYAN) { renderPointBoxAim && aimPointLine }.subjective()
    val aimPointLineWidth by float("AimPointLineWidth", 1f, 0.5f..5f) { renderPointBoxAim && aimPointLine }.subjective()
}