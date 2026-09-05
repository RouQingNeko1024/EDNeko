package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object KillAuraPrediction : Module("KillAura-Prediction", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val predictClientMovement by int("PredictClientMovement", 2, 0..5) { true }

    val predictOnlyWhenOutOfRange by boolean("PredictOnlyWhenOutOfRange", false) {
        predictClientMovement != 0
    }

    val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, -1f..2f) { true }
}