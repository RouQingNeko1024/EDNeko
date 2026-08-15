package net.ccbluex.liquidbounce.utils.rotation.rotations

import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.rotation.AimAssistRotationUtil
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.minecraft.util.MathHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class AdvancedRotation(private val option: RotationSettings?) {
    private fun randomSin(): Float {
        return sin(nextFloat(0F, 2F * Math.PI.toFloat()).toDouble()).toFloat()
    }

    private fun randomFuck(): Float {
        return (Math.random() - Math.random() * Math.random() + Math.random() * Math.random() * Math.random() - Math.random() * Math.random() * Math.random() * Math.random()).toFloat()
    }

    private fun calculateRotationSpeed(
        speedBase: Float, speedUpRange: Float, speedDownRange: Float, speedUpChangeMax: Float, speedDownChangeMax: Float,
        randomizationTypeLayer1: String, randomizationRateLayer1: Float,
        speedBaseLargeRotation: Float, speedUpRangeLargeRotation: Float, speedDownRangeLargeRotation: Float,
        speedUpChangeMaxLargeRotation: Float, speedDownChangeMaxLargeRotation: Float,
        randomizationTypeLayer2: String, randomizationRateLayer2: Float,
        decelerationFactorMax: Float, decelerationFactorMin: Float,
        decelerationFactorMaxLargeRotation: Float, decelerationFactorMinLargeRotation: Float,
        deltaThresholdForLargeRotation: Float, overshotRate: Float, currentDelta: Float
    ): Float {
        if (currentDelta < abs(deltaThresholdForLargeRotation) && Math.random() > overshotRate) { // not at large speed
            val minSpeed = speedBase - speedDownRange
            val maxSpeed = speedBase + speedUpRange
            var randomSpeed = nextFloat(min(minSpeed, maxSpeed), max(minSpeed, maxSpeed))

            when (randomizationTypeLayer1) {
                "Noise" -> randomSpeed += randomFuck() * randomizationRateLayer1
                "Sinus" -> randomSpeed += randomSin() * randomizationRateLayer1
            }

            val clampedSpeed = MathHelper.clamp_float(
                MathHelper.clamp_float(randomSpeed, 0F, 180F),
                currentDelta + speedUpChangeMax,
                currentDelta - speedDownChangeMax
            )
            return clampedSpeed * nextFloat(decelerationFactorMin, decelerationFactorMax)
        } else {
            val minSpeedLarge = speedBaseLargeRotation - speedDownRangeLargeRotation
            val maxSpeedLarge = speedBaseLargeRotation + speedUpRangeLargeRotation
            var randomSpeedLarge = nextFloat(min(minSpeedLarge, maxSpeedLarge), max(minSpeedLarge, maxSpeedLarge))

            when (randomizationTypeLayer2) {
                "Noise" -> randomSpeedLarge += randomFuck() * randomizationRateLayer2
                "Sinus" -> randomSpeedLarge += randomSin() * randomizationRateLayer2
            }

            val clampedSpeedLarge = MathHelper.clamp_float(
                MathHelper.clamp_float(randomSpeedLarge, 0F, 180F),
                currentDelta + speedUpChangeMaxLargeRotation,
                currentDelta - speedDownChangeMaxLargeRotation
            )
            return clampedSpeedLarge * nextFloat(decelerationFactorMinLargeRotation, decelerationFactorMaxLargeRotation)
        }
    }

    fun getFinalRotationSpeed(yaw: Boolean, lastDelta: Float): Float {
        return if (yaw) {
            option?.let {
                calculateRotationSpeed(
                    it.yawSpeedBase, it.yawSpeedUpRange, it.yawSpeedDownRange,
                    it.yawSpeedUpChangeMax, it.yawSpeedDownChangeMax,
                    it.yawSpeedRandomizationTypeLayer1, it.yawSpeedRandomizationRateLayer1,
                    it.yawSpeedBaseLargeRotation, it.yawSpeedUpRangeLargeRotation, it.yawSpeedDownRangeLargeRotation,
                    it.yawSpeedUpChangeMaxLargeRotation, it.yawSpeedDownChangeMaxLargeRotation,
                    it.yawSpeedRandomizationTypeLayer2, it.yawSpeedRandomizationRateLayer2,
                    it.yawDecelerationFactorMax, it.yawDecelerationFactorMin,
                    it.yawDecelerationFactorMaxLargeRotation, it.yawDecelerationFactorMinLargeRotation,
                    it.yawDeltaThresholdForLargeRotation, it.yawOvershotRate, lastDelta
                )
            }
        } else {
            option?.let {
                calculateRotationSpeed(
                    it.pitchSpeedBase, it.pitchSpeedUpRange, it.pitchSpeedDownRange,
                    it.pitchSpeedUpChangeMax, it.pitchSpeedDownChangeMax,
                    it.pitchSpeedRandomizationTypeLayer1, it.pitchSpeedRandomizationRateLayer1,
                    it.pitchSpeedBaseLargeRotation, it.pitchSpeedUpRangeLargeRotation, it.pitchSpeedDownRangeLargeRotation,
                    it.pitchSpeedUpChangeMaxLargeRotation, it.pitchSpeedDownChangeMaxLargeRotation,
                    it.pitchSpeedRandomizationTypeLayer2, it.pitchSpeedRandomizationRateLayer2,
                    it.pitchDecelerationFactorMax, it.pitchDecelerationFactorMin,
                    it.pitchDecelerationFactorMaxLargeRotation, it.pitchDecelerationFactorMinLargeRotation,
                    it.pitchDeltaThresholdForLargeRotation, it.pitchOvershotRate, lastDelta
                )
            }
        } as Float
    }
    fun handleAdvancedRotationSpeedMode(
        current: Rotation,
        target: Rotation,
    ): Rotation {
        val lastYawDelta = abs(angleDifference(current.yaw, serverRotation.yaw))
        val lastPitchDelta = abs(current.pitch - serverRotation.pitch)

        val yawSpeed = AdvancedRotation(option).getFinalRotationSpeed(true, lastYawDelta)
        val pitchSpeed = AdvancedRotation(option).getFinalRotationSpeed(false, lastPitchDelta)

        val limitedYaw = AimAssistRotationUtil.updateRotation(current.yaw, target.yaw, abs(yawSpeed))
        val limitedPitch = AimAssistRotationUtil.updateRotation(current.pitch, target.pitch, abs(pitchSpeed))

        return Rotation(
            limitedYaw,
            limitedPitch
        ).fixedSensitivity()
    }
}