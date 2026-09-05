package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object KillAuraRange : Module("KillAura-Range", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val range: Float by float("Range", 3.7f, 1f..8f) {
        true
    }.onChanged {
        blockRange = blockRange.coerceAtMost(it)
    }

    val scanRange by float("ScanRange", 2f, 0f..10f) { true }

    val throughWallsRange by float("ThroughWallsRange", 3f, 0f..8f) { true }

    val rangeSprintReduction by float("RangeSprintReduction", 0f, 0f..0.4f) { true }

    var blockRange: Float by float("BlockRange", 3.7f, 1f..8f) {
        true
    }.onChange { _, new ->
        new.coerceAtMost(range)
    }

    val maxRange
        get() = maxOf(range + scanRange, throughWallsRange)
}