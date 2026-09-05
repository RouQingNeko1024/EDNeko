package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import java.awt.Color

object KillAuraVisuals : Module("KillAura-Visuals", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val markNone by boolean("Mark-None", false) { true }
    val markPlatform by boolean("Mark-Platform", false) { true }
    val markBox by boolean("Mark-Box", false) { true }
    val markCircle by boolean("Mark-Circle", true) { true }

    val markJello by boolean("Mark-Jello", false) { true }
    val markZavz by boolean("Mark-Zavz", false) { true }
    val markZywl by boolean("Mark-Zywl", false) { true }
    val markSigma by boolean("Mark-Sigma", false) { true }
    val markFDP by boolean("Mark-FDP", false) { true }
    val markTracers by boolean("Mark-Tracers", false) { true }
    val markLies by boolean("Mark-Lies", false) { true }
    val markSims by boolean("Mark-Sims", false) { true }

    val rangeCircle by boolean("RangeCircle", false) { true }
    val rangeCircleRed by int("RangeCircle-Red", 255, 0..255) { rangeCircle }
    val rangeCircleGreen by int("RangeCircle-Green", 255, 0..255) { rangeCircle }
    val rangeCircleBlue by int("RangeCircle-Blue", 255, 0..255) { rangeCircle }
    val rangeCircleAlpha by int("RangeCircle-Alpha", 255, 0..255) { rangeCircle }
    val rangeCircleThickness by float("RangeCircle-Thickness", 2f, 1f..5f) { rangeCircle }

    val circleStartColor by color("CircleStartColor", Color.BLUE) { markCircle }.subjective()
    val circleEndColor by color("CircleEndColor", Color.CYAN.withAlpha(0)) { markCircle }.subjective()
    val fillInnerCircle by boolean("FillInnerCircle", false) { markCircle }.subjective()
    val withHeight by boolean("WithHeight", true) { markCircle }.subjective()
    val animateHeight by boolean("AnimateHeight", false) { withHeight }.subjective()
    val heightRange by floatRange("HeightRange", 0.0f..0.4f, -2f..2f) { withHeight }.subjective()
    val customCircleSize by boolean("CustomCircleSize", false) { markCircle }.subjective()
    val circleSize by float("CircleSize", 0.5f, 0.1f..3.0f) { markCircle && customCircleSize }.subjective()
    val extraWidth by float("ExtraWidth", 0F, 0F..2F) { markCircle && !customCircleSize }.subjective()
    val animateCircleY by boolean("AnimateCircleY", true) { (fillInnerCircle || withHeight) }.subjective()
    val circleYRange by floatRange("CircleYRange", 0F..0.5F, 0F..2F) { animateCircleY }.subjective()
    val duration by float("Duration", 1.5F, 0.5F..3F, suffix = "Seconds") {
        (animateCircleY || animateHeight)
    }.subjective()

    val boxOutline by boolean("Outline", true) { markBox }.subjective()

    val jelloAlpha by float("JelloAlpha", 0.4f, 0f..1f) { markJello }.subjective()
    val jelloWidth by float("JelloWidth", 3f, 0.01f..5f) { markJello }.subjective()
    val jelloGradientHeight by float("JelloGradientHeight", 3f, 1f..8f) { markJello }.subjective()
    val jelloFadeSpeed by float("JelloFadeSpeed", 0.1f, 0.01f..0.5f) { markJello }.subjective()

    val zavzSpeed by float("ZavzSpeed", 0.1f, 0f..10f) { (markZavz || markZywl) }.subjective()
    val zavzDual by boolean("ZavzDual", true) { (markZavz || markZywl) }.subjective()

    val tracersThickness by float("TracersThickness", 1f, 0.1f..5f) { markTracers }.subjective()

    val espColorMode by choices("ESPColorMode", arrayOf("Custom", "Rainbow", "Health"), "Custom") {
        (markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims)
    }.subjective()
    val espColorRed by int("ESPRed", 255, 0..255) {
        espColorMode == "Custom" && (markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims)
    }.subjective()
    val espColorGreen by int("ESPGreen", 255, 0..255) {
        espColorMode == "Custom" && (markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims)
    }.subjective()
    val espColorBlue by int("ESPBlue", 255, 0..255) {
        espColorMode == "Custom" && (markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims)
    }.subjective()
    val espColorAlpha by int("ESPAlpha", 255, 0..255) {
        (markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims)
    }.subjective()
}