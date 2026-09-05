package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import java.awt.Color

object KillAuraBypass : Module("KillAura-Bypass", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val swing by boolean("Swing", true) { true }
    val keepSprint by boolean("KeepSprint", true) { true }

    val autoF5 by boolean("AutoF5", false) { true }
    val onScaffold by boolean("OnScaffold", false) { true }
    val onDestroyBlock by boolean("OnDestroyBlock", false) { true }

    val useHitDelay by boolean("UseHitDelay", false) { true }
    val hitDelayTicks by int("HitDelayTicks", 1, 1..5) {
        useHitDelay
    }

    val generateClicksBasedOnDist by boolean("GenerateClicksBasedOnDistance", false) { true }
    val cpsMultiplier by intRange("CPS-Multiplier", 1..2, 1..10) {
        generateClicksBasedOnDist
    }
    val distanceFactor by floatRange("DistanceFactor", 5F..10F, 1F..10F) {
        generateClicksBasedOnDist
    }

    val generateSpotBasedOnDistance by boolean("GenerateSpotBasedOnDistance", false) {
        KillAuraRotations.options.rotationsActive
    }

    val noInventoryAttack by boolean("NoInvAttack", false) { true }
    val noInventoryDelay by int("NoInvDelay", 200, 0..500) {
        noInventoryAttack
    }
    val simulateClosingInventory by boolean("SimulateClosingInventory", false) {
        !noInventoryAttack
    }

    val noConsumeAttack by choices(
        "NoConsumeAttack", arrayOf("Off", "NoHits", "NoRotation"), "Off"
    ) { true }.subjective()

    val forceFirstHit by boolean("ForceFirstHit", false) {
        !respectMissCooldown && !useHitDelay
    }

    val failSwing by boolean("FailSwing", true) {
        swing && KillAuraRotations.options.rotationsActive
    }
    val respectMissCooldown by boolean("RespectMissCooldown", false) {
        swing && failSwing && KillAuraRotations.options.rotationsActive
    }
    val swingOnlyInAir by boolean("SwingOnlyInAir", true) {
        swing && failSwing && KillAuraRotations.options.rotationsActive
    }
    val maxRotationDifferenceToSwing by float("MaxRotationDifferenceToSwing", 180f, 0f..180f) {
        swing && failSwing && KillAuraRotations.options.rotationsActive
    }
    val swingWhenTicksLate = boolean("SwingWhenTicksLate", false) {
        swing && failSwing && maxRotationDifferenceToSwing != 180f && KillAuraRotations.options.rotationsActive
    }
    val ticksLateToSwing by int("TicksLateToSwing", 4, 0..20) {
        swing && failSwing && swingWhenTicksLate.isActive() && KillAuraRotations.options.rotationsActive
    }
    val renderBoxOnSwingFail by boolean("RenderBoxOnSwingFail", false) {
        failSwing
    }
    val renderBoxColor = ColorSettingsInteger(this, "RenderBoxColor") {
        renderBoxOnSwingFail
    }.with(Color.CYAN)
    val renderBoxFadeSeconds by float("RenderBoxFadeSeconds", 1f, 0f..5f) {
        renderBoxOnSwingFail
    }

    val fakeSharp by boolean("FakeSharp", true).subjective()
}