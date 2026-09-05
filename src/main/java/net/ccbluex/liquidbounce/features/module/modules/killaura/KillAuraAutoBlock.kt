package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object KillAuraAutoBlock : Module("KillAura-AutoBlock", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val autoBlock by choices(
        "AutoBlock", arrayOf("Off", "Packet", "Fake", "QuickMacro", "BlockOnNoHit", "HurtTime"), "Packet"
    ) { true }

    val blockMaxRange by float("BlockMaxRange", 3f, 0f..8f) {
        (autoBlock == "Packet" || autoBlock == "QuickMacro")
    }

    val unblockMode by choices(
        "UnblockMode", arrayOf("Stop", "Switch", "Empty"), "Stop"
    ) {
        (autoBlock == "Packet" || autoBlock == "QuickMacro" || autoBlock == "HurtTime")
    }

    val releaseAutoBlock by boolean("ReleaseAutoBlock", true) {
        autoBlock !in arrayOf("Off", "Fake", "BlockOnNoHit", "HurtTime")
    }

    val forceBlockRender by boolean("ForceBlockRender", true) {
        autoBlock !in arrayOf("Off", "Fake") && releaseAutoBlock
    }

    val ignoreTickRule by boolean("IgnoreTickRule", false) {
        autoBlock !in arrayOf("Off", "Fake", "HurtTime") && releaseAutoBlock
    }

    val blockRate by int("BlockRate", 100, 1..100) {
        autoBlock !in arrayOf("Off", "Fake", "HurtTime") && releaseAutoBlock
    }

    val uncpAutoBlock by boolean("UpdatedNCPAutoBlock", false) {
        autoBlock !in arrayOf("Off", "Fake") && !releaseAutoBlock
    }

    val switchStartBlock by boolean("SwitchStartBlock", false) {
        autoBlock !in arrayOf("Off", "Fake")
    }

    val interactAutoBlock by boolean("InteractAutoBlock", true) {
        autoBlock !in arrayOf("Off", "Fake", "HurtTime")
    }

    val blinkAutoBlock by boolean("BlinkAutoBlock", false) {
        autoBlock !in arrayOf("Off", "Fake")
    }

    val blinkBlockTicks by int("BlinkBlockTicks", 3, 2..5) {
        autoBlock !in arrayOf("Off", "Fake") && blinkAutoBlock
    }

    val smartAutoBlock by boolean("SmartAutoBlock", false) {
        autoBlock == "Packet"
    }

    val blockOnNoHitMode by choices("BlockOnNoHitMode", arrayOf("Packet", "RightClick"), "Packet") {
        autoBlock == "BlockOnNoHit"
    }
    val cancelAttackWhenBlocking by boolean("CancelAttackWhenBlocking", true) {
        autoBlock == "BlockOnNoHit"
    }
    val blockOnNoHitDelay by int("BlockOnNoHitDelay", 1, 0..20) {
        autoBlock == "BlockOnNoHit"
    }

    val hurtTimeMode by choices("HurtTimeMode", arrayOf("Custom", "Predict", "Adaptive"), "Custom") {
        autoBlock == "HurtTime"
    }
    val hurtTimeWaitTicks by int("HurtTimeWaitTicks", 10, 1..100) {
        autoBlock == "HurtTime" && hurtTimeMode == "Custom"
    }
    val hurtTimeBlockDuration by int("HurtTimeBlockDuration", 3, 1..20) {
        autoBlock == "HurtTime"
    }
    val hurtTimePredictMultiplier by float("HurtTimePredictMultiplier", 0.8f, 0.5f..1.5f) {
        autoBlock == "HurtTime" && hurtTimeMode == "Predict"
    }
    val hurtTimeAdaptiveMin by int("HurtTimeAdaptiveMin", 5, 1..50) {
        autoBlock == "HurtTime" && hurtTimeMode == "Adaptive"
    }
    val hurtTimeAdaptiveMax by int("HurtTimeAdaptiveMax", 20, 1..50) {
        autoBlock == "HurtTime" && hurtTimeMode == "Adaptive"
    }
    val hurtTimeAdaptiveFactor by float("HurtTimeAdaptiveFactor", 0.7f, 0.1f..1.0f) {
        autoBlock == "HurtTime" && hurtTimeMode == "Adaptive"
    }
    val hurtTimeCancelAttack by boolean("HurtTimeCancelAttack", true) {
        autoBlock == "HurtTime"
    }

    val forceBlock by boolean("ForceBlockWhenStill", true) {
        smartAutoBlock
    }

    val checkWeapon by boolean("CheckEnemyWeapon", true) {
        smartAutoBlock
    }

    val maxOwnHurtTime by int("MaxOwnHurtTime", 3, 0..10) {
        smartAutoBlock
    }

    val maxDirectionDiff by float("MaxOpponentDirectionDiff", 60f, 30f..180f) {
        smartAutoBlock
    }

    val maxSwingProgress by int("MaxOpponentSwingProgress", 1, 0..5) {
        smartAutoBlock
    }
}