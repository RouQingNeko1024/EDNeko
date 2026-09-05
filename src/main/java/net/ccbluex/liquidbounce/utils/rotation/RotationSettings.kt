/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.rotation

import net.ccbluex.liquidbounce.cape.CapeAPI.mc
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.extensions.withGCD
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.rotationAdditions.SmoothRotation
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.abs
import kotlin.random.Random

open class RotationSettings(owner: Module, generalApply: () -> Boolean = { true }) : Configurable("RotationSettings") {
    var failRotationActiveTicks: Float = 0F
    var currentFailedRotation: Rotation? = null
    open val rotationsValue = boolean("Rotations", true) { generalApply() }
    open val applyServerSideValue = boolean("ApplyServerSide", true) { rotationsActive && generalApply() }
    open val simulateShortStopValue = boolean("SimulateShortStop", false) { rotationsActive && generalApply() }
    open val rotationDiffBuildUpToStopValue = float("RotationDiffBuildUpToStop", 180f, 10f..720f) { simulateShortStop && generalApply() }
    open val maxThresholdAttemptsToStopValue = int("MaxThresholdAttemptsToStop", 1, 0..5) { simulateShortStop && generalApply() }
    open val shortStopDurationValue = intRange("ShortStopDuration", 1..2, 1..5) { simulateShortStop && generalApply() }
    open val strafeValue = boolean("Strafe", false) { rotationsActive && applyServerSide && generalApply() }
    open val strictValue = boolean("Strict", false) { strafeValue.isActive() && generalApply() }
    open val keepRotationValue = boolean("KeepRotation", true) { rotationsActive && applyServerSide && generalApply() }
    open val resetTicksValue = int("ResetTicks", 1, 1..20) {
        rotationsActive && applyServerSide && generalApply()
    }

    open val rotationMode by choices(
        "RotationMode",
        arrayOf(
            "Standard",
            "SlowPursuit",
            "Acceleration",
            "EntropyControlled",
            "AdvancedRotationSpeed",
            "PatternPrediction",
            "DynamicRotation"
        ),
        "Standard"
    ) { rotationsActive && generalApply() }
    open val dynamicEffectTypes = choices(
        "DynamicRotation/EffectTypes",
        arrayOf("SpeedBoost", "SpeedReduction", "Jitter", "Smooth"),
        "SpeedBoost"
    ) { rotationMode == "DynamicRotation" && rotationsActive && generalApply() }

    var currentDynamicEffect = "SpeedBoost"

    open val dynamicDuration = intRange("DynamicRotation/Duration", 3..10, 1..50, "ticks") {
        rotationMode == "DynamicRotation" && rotationsActive && generalApply()
    }

    open val dynamicUpdateInterval = int("DynamicRotation/UpdateInterval", 20, 5..100, "ticks") {
        rotationMode == "DynamicRotation" && rotationsActive && generalApply()
    }

    open val dynamicSpeedBoostMultiplier = floatRange("DynamicRotation/SpeedBoostMultiplier", 1.5f..2.5f, 0.5f..5.0f) {
        rotationMode == "DynamicRotation" && rotationsActive && generalApply()
    }

    open val dynamicSpeedReductionMultiplier = floatRange("DynamicRotation/SpeedReductionMultiplier", 0.3f..0.7f, 0.1f..1.0f) {
        rotationMode == "DynamicRotation" && rotationsActive && generalApply()
    }

    open val dynamicJitterStrength = floatRange("DynamicRotation/JitterStrength", 0.2f..0.5f, 0.0f..1.0f) {
        rotationMode == "DynamicRotation" && rotationsActive && generalApply()
    }

    open val dynamicSmoothMultiplier = floatRange("DynamicRotation/SmoothMultiplier", 0.8f..1.2f, 0.1f..2.0f) {
        rotationMode == "DynamicRotation" && rotationsActive && generalApply()
    }

    open val dynamicSmoothFactor = floatRange("DynamicRotation/SmoothFactor", 0.3f..0.7f, 0.1f..1.0f) {
        rotationMode == "DynamicRotation" && rotationsActive && generalApply()
    }

    open val dynamicMinMultiplier = float("DynamicRotation/MinMultiplier", 0.2f, 0.1f..1.0f) {
        rotationMode == "DynamicRotation" && rotationsActive && generalApply()
    }

    open val dynamicMaxMultiplier = float("DynamicRotation/MaxMultiplier", 3.0f, 1.0f..5.0f) {
        rotationMode == "DynamicRotation" && rotationsActive && generalApply()
    }



    open val mlPredictionWeight = float("PredictionWeight", 0.3f, 0f..1f) { rotationMode == "PatternPrediction" && generalApply() }
    open val neuralNetworkEnabled = boolean("NeuralNetwork", false) {
        rotationMode == "PatternPrediction" && rotationsActive && generalApply()
    }

    open val nnTrainingEnabled = boolean("NeuralNetwork/Training", false) {
        neuralNetworkEnabled.get() && generalApply()
    }

    open val nnPredictionWeight = float("NeuralNetwork/PredictionWeight", 0.2f, 0f..1f) {
        neuralNetworkEnabled.get() && generalApply()
    }

    open val nnMaxTrainingSamples = int("NeuralNetwork/MaxSamples", 5000, 100..20000) {
        neuralNetworkEnabled.get() && nnTrainingEnabled.get() && generalApply()
    }

    open val nnLearningRate = float("NeuralNetwork/LearningRate", 0.01f, 0.001f..0.1f) {
        neuralNetworkEnabled.get() && generalApply()
    }

    open val nnHiddenSize = int("NeuralNetwork/HiddenSize", 8, 4..32) {
        neuralNetworkEnabled.get() && generalApply()
    }

    open val nnAutoSave = boolean("NeuralNetwork/AutoSave", true) {
        neuralNetworkEnabled.get() && generalApply()
    }

    open val nnLoadOnStart = boolean("NeuralNetwork/LoadOnStart", true) {
        neuralNetworkEnabled.get() && generalApply()
    }

    open val nnTrainingInterval = int("NeuralNetwork/TrainingInterval", 5000, 1000..30000, "ms") {
        neuralNetworkEnabled.get() && nnTrainingEnabled.get() && generalApply()
    }

    open val nnEnableLogging = boolean("NeuralNetwork/EnableLogging", false) {
        neuralNetworkEnabled.get() && generalApply()
    }

    open val nnUseSpeedLimit = boolean("NeuralNetwork/UseSpeedLimit", true) {
        neuralNetworkEnabled.get() && generalApply()
    }

    open val nnSpeedMultiplier = float("NeuralNetwork/SpeedMultiplier", 1.0f, 0.5f..2.0f) {
        neuralNetworkEnabled.get() && nnUseSpeedLimit.get() && generalApply()
    }
    // Yaw parameters
    open val yawSpeedBase by float("YawSpeedBase", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedUpRange by float("YawSpeedUpRange", 0F, 0F..90F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedDownRange by float("YawSpeedDownRange", 0F, 0F..90F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedUpChangeMax by float("YawSpeedUpChangeMax", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedDownChangeMax by float("YawSpeedDownChangeMax", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedRandomizationTypeLayer1 by choices("YawSpeedRandomizationLayer1", arrayOf("Noise", "Sinus"), "Noise") {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedRandomizationRateLayer1 by float("YawSpeedRandomizationLayer1Rate", 1F, 0F..1F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }

    open val yawSpeedBaseLargeRotation by float("YawSpeedBaseAtLargeRotation", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedUpRangeLargeRotation by float("YawSpeedUpRangeAtLargeRotation", 0F, 0F..90F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedDownRangeLargeRotation by float("YawSpeedDownRangeAtLargeRotation", 0F, 0F..90F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedUpChangeMaxLargeRotation by float("YawSpeedUpChangeMaxAtLargeRotation", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedDownChangeMaxLargeRotation by float("YawSpeedDownChangeMaxAtLargeRotation", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedRandomizationTypeLayer2 by choices("YawSpeedRandomizationLayer2", arrayOf("Noise", "Sinus"), "Noise") {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawSpeedRandomizationRateLayer2 by float("YawSpeedRandomizationLayer2Rate", 1F, 0F..1F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }

    open val yawDecelerationFactorMax by float("YawDecelerationFactorMax", 0.9F, 0.0F..1.0F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawDecelerationFactorMin by float("YawDecelerationFactorMin", 0.9F, 0.0F..1.0F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawDecelerationFactorMaxLargeRotation by float("YawDecelerationFactorMaxAtLargeRotation", 0.7F, 0.0F..1.0F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawDecelerationFactorMinLargeRotation by float("YawDecelerationFactorMinAtLargeRotation", 0.7F, 0.0F..1.0F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }

    open val yawDeltaThresholdForLargeRotation by float("YawDeltaThresholdForLargeRotation", 90F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val yawOvershotRate by float("YawOvershotRate", 0F, 0F..1F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }

    // Pitch parameters
    open val pitchSpeedBase by float("PitchSpeedBase", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedUpRange by float("PitchSpeedUpRange", 0F, 0F..90F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedDownRange by float("PitchSpeedDownRange", 0F, 0F..90F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedUpChangeMax by float("PitchSpeedUpChangeMax", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedDownChangeMax by float("PitchSpeedDownChangeMax", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedRandomizationTypeLayer1 by choices("PitchSpeedRandomizationLayer1", arrayOf("Noise", "Sinus"), "Noise") {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedRandomizationRateLayer1 by float("PitchSpeedRandomizationLayer1Rate", 1F, 0F..1F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }

    open val pitchSpeedBaseLargeRotation by float("PitchSpeedBaseAtLargeRotation", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedUpRangeLargeRotation by float("PitchSpeedUpRangeAtLargeRotation", 0F, 0F..90F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedDownRangeLargeRotation by float("PitchSpeedDownRangeAtLargeRotation", 0F, 0F..90F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedUpChangeMaxLargeRotation by float("PitchSpeedUpChangeMaxAtLargeRotation", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedDownChangeMaxLargeRotation by float("PitchSpeedDownChangeMaxAtLargeRotation", 180F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedRandomizationTypeLayer2 by choices("PitchSpeedRandomizationLayer2", arrayOf("Noise", "Sinus"), "Noise") {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchSpeedRandomizationRateLayer2 by float("PitchSpeedRandomizationLayer2Rate", 1F, 0F..1F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }

    open val pitchDecelerationFactorMax by float("PitchDecelerationFactorMax", 0.9F, 0.0F..1.0F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchDecelerationFactorMin by float("PitchDecelerationFactorMin", 0.9F, 0.0F..1.0F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchDecelerationFactorMaxLargeRotation by float("PitchDecelerationFactorMaxAtLargeRotation", 0.7F, 0.0F..1.0F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchDecelerationFactorMinLargeRotation by float("PitchDecelerationFactorMinAtLargeRotation", 0.7F, 0.0F..1.0F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }

    open val pitchDeltaThresholdForLargeRotation by float("PitchDeltaThresholdForLargeRotation", 90F, 0F..180F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }
    open val pitchOvershotRate by float("PitchOvershotRate", 0F, 0F..1F) {
        rotationMode == "AdvancedRotationSpeed" && rotationsActive && generalApply()
    }

    open val entropyTarget by float("Entropy/Target", 0.55f, 0.0f..1.0f) {
        rotationMode == "EntropyControlled" && rotationsActive && generalApply()
    }

    open val entropyTolerance by float("Entropy/Tolerance", 0.15f, 0.05f..0.3f) {
        rotationMode == "EntropyControlled" && rotationsActive && generalApply()
    }

    open val entropyAdjustmentSpeed by float("Entropy/AdjustSpeed", 0.1f, 0.01f..0.5f) {
        rotationMode == "EntropyControlled" && rotationsActive && generalApply()
    }

    open val entropySmoothingStrength by floatRange("Entropy/SmoothingStrength", 0.3f..0.8f, 0.1f..1.0f) {
        rotationMode == "EntropyControlled" && rotationsActive && generalApply()
    }

    open val entropyRandomnessStrength by floatRange("Entropy/RandomnessStrength", 0.2f..0.6f, 0.1f..1.0f) {
        rotationMode == "EntropyControlled" && rotationsActive && generalApply()
    }

    // 熵状态变量
    var currentEntropy: Float = 0.5f
    var entropyHistory = ArrayDeque<Float>(60)
    var lastEntropyUpdate: Long = System.currentTimeMillis()
    // ========== SlowPursuit Parameters ==========
    open val slowPursuitSpeed by floatRange(
        "SlowPursuit/Speed", 30f..50f, 0f..360f
    ) { rotationMode == "SlowPursuit" && rotationsActive && generalApply() }

    open val slowPursuitDelay by intRange(
        "SlowPursuit/Delay", 3..5, 1..20
    ) { rotationMode == "SlowPursuit" && rotationsActive && generalApply() }

    open val slowPursuitRandomness by floatRange(
        "SlowPursuit/Randomness", 0.1f..0.3f, 0f..1f
    ) { rotationMode == "SlowPursuit" && rotationsActive && generalApply() }
    var slowPursuitLastAttackTime: Long? = null
    var slowPursuitActive: Boolean = false
    // ========== Acceleration Mode Parameters ==========
    open val accelerationSpeed by floatRange(
        "Acceleration/Speed", 100f..200f, 10f..500f,
    ) { rotationMode == "Acceleration" && rotationsActive && generalApply() }

    open val accelerationMaxSpeed by floatRange(
        "Acceleration/MaxSpeed", 2.0f..3.0f, 1.0f..5.0f, "x"
    ) { rotationMode == "Acceleration" && rotationsActive && generalApply() }

    open val accelerationSmoothFactor by floatRange(
        "Acceleration/SmoothFactor", 0.8f..1.0f, 0.1f..1.0f
    ) { rotationMode == "Acceleration" && rotationsActive && generalApply() }

    open val accelerationSmartDeceleration by boolean(
        "Acceleration/SmartDeceleration", true
    ) { rotationMode == "Acceleration" && rotationsActive && generalApply() }

    open val accelerationDecelerationRate by floatRange(
        "Acceleration/DecelerationRate", 0.6f..0.8f, 0.1f..1.0f
    ) { rotationMode == "Acceleration" && accelerationSmartDeceleration && generalApply() }


    var accelerationCurrentSpeedYaw: Float = 0f
    var accelerationCurrentSpeedPitch: Float = 0f
    var accelerationLastUpdateTime: Long = System.currentTimeMillis()
    open val instantRotation by boolean("InstantRotation",false) {rotationsActive && generalApply() && rotationMode == "Center"}

    open val slowPursuitDurationTime by int("SlowPursuit/DurationTime",500,0..2000,"ms") { rotationMode == "SlowPursuit" && rotationsActive && generalApply() }

    // ========== Original Settings ==========
    open val manualRotationMixEnabled = boolean("ManualRotationMix", false) {
        rotationsActive && applyServerSide && generalApply()
    }
    open val manualMixRequireHitbox = boolean("ManualMix/RequireForceInTargetHitbox", true) {
        manualRotationMixEnabled.get() && generalApply()
    }
    open val manualMixStrength = floatRange("ManualMix/Strength", 0.1f..0.3f, 0f..3f) {
        manualRotationMixEnabled.get() && generalApply()
    }
    open val manualMixMaxDeviation = float("ManualMix/MaxDeviation", 45f, 5f..180f) {
        manualRotationMixEnabled.get() && generalApply()
    }
    open val manualMixCurve = choices("ManualMix/Curve",
        arrayOf("Linear", "Quadratic", "Exponential", "Sigmoid"), "Quadratic") {
        manualRotationMixEnabled.get() && generalApply()
    }
    open val damageBoostEnabled = boolean("DamageBoostRotation", false) { rotationsActive && generalApply() }
    open val damageBoostHurtTimeThreshold = int("DamageBoost/WorkHurtTime", 5, 0..10) {
        damageBoostEnabled.get() && rotationsActive && generalApply()
    }
    open val damageBoostMultiplier = floatRange("DamageBoost/BoostFacter", 1.5f..2f, 1f..360f) {
        damageBoostEnabled.get() && rotationsActive && generalApply()
    }

    open val cancelRotationInBorder = boolean("CancelRotationInBorder", false) { rotationsActive && generalApply() }
    open val legitimizeValue = boolean("Legitimize", false) { rotationsActive && generalApply() }
    open val legitimizeFactor = floatRange("LegitFactor", 0.9f..1.1f, 0.0f..2f) { rotationsActive && generalApply() && legitimize }
    open val allowExcessiveRotation = boolean("AllowExcessiveRotation", false) {
        rotationsActive && generalApply() && !legitimize
    }
    open val excessiveRotationMaxAngle = float("ExcessiveRotation/MaxAngle", 45f, 5f..180f) {
        allowExcessiveRotation.get() && rotationsActive && generalApply() && !legitimize
    }
    open val excessiveRotationCorrectionSpeed = floatRange("ExcessiveRotation/OvershootFactor", 1.1f..1.3f, 1.0f..2.0f) {
        allowExcessiveRotation.get() && rotationsActive && generalApply() && !legitimize
    }
    open val horizontalAngleChangeValue =
        floatRange("HorizontalAngleChange", 360f..360f, 0f..360f) { rotationsActive && generalApply() }
    open val verticalAngleChangeValue =
        floatRange("VerticalAngleChange", 360f..360f, 0f..360f) { rotationsActive && generalApply() }

    open val angleResetDifferenceValue = float("AngleResetDifference", 5f.withGCD(), 0.0f..180f) {
        rotationsActive && applyServerSide && generalApply()
    }

    open val minRotationDifferenceValue = float(
        "MinRotationDifference", 2f, 0f..8f
    ) { rotationsActive && generalApply() }

    open val minRotationDifferenceResetTimingValue = choices(
        "MinRotationDifferenceResetTiming", arrayOf("OnStart", "OnSlowDown", "Always"), "OnStart"
    ) { rotationsActive && generalApply() }

    // fatigue
    open val fatigueEnabled = boolean("FatigueRotation", false) { rotationsActive && generalApply() }
    open val fatigueMode by choices(
        "FatigueMode",
        arrayOf("Linear", "Exponential", "CustomCurve"),
        "Linear"
    ) { fatigueEnabled.get() && generalApply() }

    open val fatigueStartThreshold = float("FatigueStartThreshold", 30f, 0f..180f) {
        fatigueEnabled.get() && fatigueMode != "CustomCurve" && generalApply()
    }

    open val fatigueMaxThreshold = float("FatigueMaxThreshold", 90f, 0f..180f) {
        fatigueEnabled.get() && fatigueMode != "CustomCurve" && generalApply()
    }

    open val fatigueRecoveryRate = float("FatigueRecoveryRate", 0.8f, 0.1f..2f) {
        fatigueEnabled.get() && generalApply()
    }

    open val fatigueYawFactor = float("FatigueYawFactor", 1.0f, 0.1f..2f) {
        fatigueEnabled.get() && fatigueMode == "CustomCurve" && generalApply()
    }

    open val fatiguePitchFactor = float("FatiguePitchFactor", 0.7f, 0.1f..2f) {
        fatigueEnabled.get() && fatigueMode == "CustomCurve" && generalApply()
    }

    open val fatigueCustomCurve = floatRange("FatigueCustomCurve", 0.3f..0.7f, 0f..1f) {
        fatigueEnabled.get() && fatigueMode == "CustomCurve" && generalApply()
    }

    var fatigueLevel: Float = 0f
    var lastRotationDiff: Float = 0f
    var lastRotationTime: Long = 0L

    // FailRotation
    open val failRotationEnabled = boolean("FailRotation", false) { rotationsActive && generalApply()  && generalApply()}
    open val failRotationMode by choices("FailRotationMode",arrayOf("Cancel", "RandomOffset"),"RandomOffset") { failRotationEnabled.get()  && generalApply()}
    open val failChance = float("FailChance", 0.1f, 0f..1f) { failRotationEnabled.get()  && generalApply()}
    open val failMaxYawOffset = float("FailMaxYawOffset", 30f, 0f..180f) { failRotationEnabled.get() && failRotationMode == "RandomOffset" && generalApply()}
    open val failMaxPitchOffset = float("FailMaxPitchOffset", 15f, 0f..90f) { failRotationEnabled.get() && failRotationMode == "RandomOffset" && generalApply()}
    open val failDuration = intRange("FailDuration", 1..3, 1..10) { failRotationEnabled.get() && generalApply()}
    open val failOnlyWhenMoving = boolean("OnlyWhenUserMoving", false) { failRotationEnabled.get() && generalApply() }

    fun maybeApplyFailRotation(target: Rotation, player: EntityPlayer?): Rotation {
        if (failRotationActiveTicks > 0) {
            failRotationActiveTicks--
            return currentFailedRotation ?: target
        }

        currentFailedRotation = null

        if (!failRotationEnabled.get() || Random.nextFloat() > failChance.get()) {
            return target
        }

        if (failOnlyWhenMoving.get() && player != null && !mc.thePlayer.isMoving) {
            return target
        }

        return when (failRotationMode) {
            "RandomOffset" -> {
                val yawOffset = Random.nextFloat() * failMaxYawOffset.get() * (if (Random.nextBoolean()) 1 else -1)
                val pitchOffset = Random.nextFloat() * failMaxPitchOffset.get() * (if (Random.nextBoolean()) 1 else -1)

                currentFailedRotation = Rotation(
                    target.yaw + yawOffset,
                    (target.pitch + pitchOffset).coerceIn(-90f, 90f)
                )

                failRotationActiveTicks = failDuration.get().random().toFloat()
                currentFailedRotation!!
            }
            "Cancel" -> {
                failRotationActiveTicks = failDuration.get().random().toFloat()
                currentRotation ?: target
            }
            else -> target
        }
    }

    // ========== Smooth Rotation Settings ==========
    open val useSmoothRotationValue = boolean("SmoothRotation", false) { rotationsActive && generalApply() }
    open val smoothRotationTypeValue = choices(
        "SmoothRotationType",
        SmoothRotation.SmoothRotationType.entries.map { it.name }.toTypedArray(),
        SmoothRotation.SmoothRotationType.INTERPOLATION_LINEAR.name
    ) { useSmoothRotationValue.get() && generalApply() }
    val smoothRotationType: SmoothRotation.SmoothRotationType
        get() = SmoothRotation.SmoothRotationType.valueOf(smoothRotationTypeValue.get())
    open val hermiteTension = floatRange("HermiteTension", 0.3f..0.7f, 0.0f..1.0f) {
        useSmoothRotationValue.get() && smoothRotationTypeValue.get() == "HERMITE" && generalApply()
    }

    open val hermiteBias = floatRange("HermiteBias", -0.2f..0.2f, -1.0f..1.0f) {
        useSmoothRotationValue.get() && smoothRotationTypeValue.get() == "HERMITE" && generalApply()
    }

    // 弹性/反弹效果参数
    open val bounceAmplitude = float("BounceAmplitude", 1.0f, 0.1f..2.0f) {
        useSmoothRotationValue.get() && smoothRotationTypeValue.get() == "BOUNCE_OUT" && generalApply()
    }

    open val elasticPeriod = float("ElasticPeriod", 0.3f, 0.1f..1.0f) {
        useSmoothRotationValue.get() && smoothRotationTypeValue.get() == "ELASTIC_OUT" && generalApply()
    }

    val useSmoothRotation by useSmoothRotationValue
    val smoothRotationAlphaValue = floatRange("SmoothRotationFactor", 0.8f..0.8f, 0.01f..2f) { useSmoothRotation && generalApply() }
    val smoothRotationAlpha by smoothRotationAlphaValue

    open val bezierCurveEnabled = boolean("BezierCurveRotation", false) { rotationsActive && generalApply() }
    open val bezierSpeed = float("BezierSpeed", 0.2f, 0.01f..2f) { bezierCurveEnabled.get()  && generalApply() }
    open val dynamicBezierEnabled = boolean("DynamicBezier", false) { rotationsActive && generalApply() && bezierCurveEnabled.get() }
    open val dynamicYawStepAdjustmentStrength = float("DynamicYawStepAdjustment", 0f, 0f..2f) { bezierCurveEnabled.get() && dynamicBezierEnabled.get() && generalApply()  }
    open val dynamicPitchStepAdjustmentStrength = float("DynamicPitchStepAdjustment", 0f, 0f..2f) { bezierCurveEnabled.get() && dynamicBezierEnabled.get() && generalApply()  }
    open val minYawStep = float("MinYawStep", 0.25f, 0f..1.5f) { bezierCurveEnabled.get() && generalApply()  }
    open val maxYawStep = float("MaxYawStep", 0.64f, 0f..1.5f) { bezierCurveEnabled.get() && generalApply()  }
    open val minPitchStep = float("MinPitchStep", 0.23f, 0f..1.5f) { bezierCurveEnabled.get() && generalApply()  }
    open val maxPitchStep = float("MaxPitchStep", 0.34f, 0f..1.5f) { bezierCurveEnabled.get() && generalApply()  }
    open val distanceFactor = float("DistanceFactor", 1f, 0f..1.5f) { bezierCurveEnabled.get() && generalApply()  }
    open val heuristicRotationEnabled = boolean("HeuristicRotation", false) {
        rotationsActive && generalApply()
    }
    // PID控制设置
    open val pidEnabled = boolean("PIDRotation", false) { rotationsActive && generalApply() }
    open val pidProportionalGain = float("PID-PG", 60.64f, 0f..100f) { pidEnabled.get() && generalApply()  }
    open val pidIntegralGain = float("PID-IG", 21.28f, 0f..100f) { pidEnabled.get() && generalApply()  }
    open val pidDerivativeGain = float("PID-DG", 12.128f, 0f..100f) { pidEnabled.get() && generalApply()  }
    open val pidSmoothYaw = float("PID-SmoothYaw", 0f, 0f..1f) { pidEnabled.get()  && generalApply() }
    open val pidSmoothPitch = float("PID-SmoothPitch", 0.5f, 0f..1f) { pidEnabled.get() && generalApply()  }
    open val pidResetOnDisable = boolean("PID-ResetOnDisable", true) { pidEnabled.get() && generalApply()  }

    // PID状态变量
    var pidYawIntegral = 0f
    var pidPitchIntegral = 0f
    var pidPrevYawError = 0f
    var pidPrevPitchError = 0f

    fun resetPID() {
        pidYawIntegral = 0f
        pidPitchIntegral = 0f
        pidPrevYawError = 0f
        pidPrevPitchError = 0f
    }

    val rotations by rotationsValue
    val applyServerSide by applyServerSideValue
    val simulateShortStop by simulateShortStopValue
    val rotationDiffBuildUpToStop by rotationDiffBuildUpToStopValue
    val maxThresholdAttemptsToStop by maxThresholdAttemptsToStopValue
    val shortStopDuration by shortStopDurationValue
    val strafe by strafeValue
    val strict by strictValue
    val keepRotation by keepRotationValue
    val resetTicks by resetTicksValue
    val legitimize by legitimizeValue
    val horizontalAngleChange by horizontalAngleChangeValue
    val verticalAngleChange by verticalAngleChangeValue
    val angleResetDifference by angleResetDifferenceValue
    val minRotationDifference by minRotationDifferenceValue
    val minRotationDifferenceResetTiming by minRotationDifferenceResetTimingValue

    var prioritizeRequest = false
    var immediate = false
    var instant = false

    var rotDiffBuildUp = 0f
    var maxThresholdReachAttempts = 0

    open val rotationsActive
        get() = rotations

    val horizontalSpeed
        get() = horizontalAngleChange.random()

    val verticalSpeed
        get() = verticalAngleChange.random()

    fun withoutKeepRotation() = apply {
        keepRotationValue.excludeWithState()
    }

    fun updateSimulateShortStopData(diff: Float) {
        rotDiffBuildUp += diff
    }

    fun resetSimulateShortStopData() {
        rotDiffBuildUp = 0f
        maxThresholdReachAttempts = 0
    }

    fun shouldPerformShortStop(): Boolean {
        if (abs(rotDiffBuildUp) < rotationDiffBuildUpToStop || !simulateShortStop) return false

        if (maxThresholdReachAttempts < maxThresholdAttemptsToStop) {
            maxThresholdReachAttempts++
            return false
        }

        return true
    }

    init {
        owner.addValues(this.values)
    }

    var noiseFunction: ((Rotation) -> Rotation)? = null
}