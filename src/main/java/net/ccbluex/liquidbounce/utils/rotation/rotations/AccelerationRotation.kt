package net.ccbluex.liquidbounce.utils.rotation.rotations

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifferences
import kotlin.math.abs
import kotlin.math.sign

class AccelerationRotation : MinecraftInstance {
    fun handleAccelerationMode(
        current: Rotation,
        target: Rotation,
        settings: RotationSettings
    ): Rotation {
        val now = System.currentTimeMillis()
        val (yawDiff, pitchDiff) = angleDifferences(target, current)

        val baseHSpeed = settings.horizontalSpeed
        val baseVSpeed = settings.verticalSpeed

        val acceleration = settings.accelerationSpeed.random()
        val maxSpeedMultiplier = settings.accelerationMaxSpeed.random()
        val smoothFactor = settings.accelerationSmoothFactor.random()

        val timeDiff = (now - settings.accelerationLastUpdateTime).coerceAtLeast(1L)
        settings.accelerationLastUpdateTime = now

        val ticksPassed = (timeDiff / 50f).coerceAtLeast(0.5f).coerceAtMost(3f)

        if (settings.accelerationCurrentSpeedYaw == 0f && yawDiff != 0f) {
            settings.accelerationCurrentSpeedYaw = baseHSpeed * yawDiff.sign * 0.5f
        }
        if (settings.accelerationCurrentSpeedPitch == 0f && pitchDiff != 0f) {
            settings.accelerationCurrentSpeedPitch = baseVSpeed * pitchDiff.sign * 0.5f
        }

        val yawDirection = yawDiff.sign
        val pitchDirection = pitchDiff.sign

        val maxAllowedYawSpeed = baseHSpeed * maxSpeedMultiplier
        val maxAllowedPitchSpeed = baseVSpeed * maxSpeedMultiplier

        if (abs(yawDiff) > 1f) {
            val accelThisTick = acceleration * ticksPassed * yawDirection
            settings.accelerationCurrentSpeedYaw += accelThisTick

            if (abs(settings.accelerationCurrentSpeedYaw) > maxAllowedYawSpeed) {
                settings.accelerationCurrentSpeedYaw = maxAllowedYawSpeed * yawDirection
            }

            if (settings.accelerationCurrentSpeedYaw.sign != yawDirection && yawDirection != 0f) {
                settings.accelerationCurrentSpeedYaw = baseHSpeed * yawDirection
            }
        } else {
            if (settings.accelerationSmartDeceleration) {
                val decelerationRate = settings.accelerationDecelerationRate.random()
                settings.accelerationCurrentSpeedYaw *= (1f - (1f - decelerationRate) * ticksPassed)
                if (abs(settings.accelerationCurrentSpeedYaw) < baseHSpeed * 0.05f) {
                    settings.accelerationCurrentSpeedYaw = 0f
                }
            } else {
                settings.accelerationCurrentSpeedYaw = 0f
            }
        }

        if (abs(pitchDiff) > 1f) {
            val accelThisTick = acceleration * ticksPassed * pitchDirection * 0.8f
            settings.accelerationCurrentSpeedPitch += accelThisTick

            if (abs(settings.accelerationCurrentSpeedPitch) > maxAllowedPitchSpeed) {
                settings.accelerationCurrentSpeedPitch = maxAllowedPitchSpeed * pitchDirection
            }

            if (settings.accelerationCurrentSpeedPitch.sign != pitchDirection && pitchDirection != 0f) {
                settings.accelerationCurrentSpeedPitch = baseVSpeed * pitchDirection
            }
        } else {
            if (settings.accelerationSmartDeceleration) {
                val decelerationRate = settings.accelerationDecelerationRate.random()
                settings.accelerationCurrentSpeedPitch *= (1f - (1f - decelerationRate) * ticksPassed)
                if (abs(settings.accelerationCurrentSpeedPitch) < baseVSpeed * 0.05f) {
                    settings.accelerationCurrentSpeedPitch = 0f
                }
            } else {
                settings.accelerationCurrentSpeedPitch = 0f
            }
        }

        val smoothedYawSpeed = settings.accelerationCurrentSpeedYaw * smoothFactor
        val smoothedPitchSpeed = settings.accelerationCurrentSpeedPitch * smoothFactor

        val yawMoveThisTick = smoothedYawSpeed * ticksPassed
        val pitchMoveThisTick = smoothedPitchSpeed * ticksPassed

        val maxYawMove = abs(yawDiff).coerceAtLeast(1f)
        val maxPitchMove = abs(pitchDiff).coerceAtLeast(1f)

        val actualYawMove = if (abs(yawMoveThisTick) > maxYawMove) {
            yawDiff
        } else {
            yawMoveThisTick
        }

        val actualPitchMove = if (abs(pitchMoveThisTick) > maxPitchMove) {
            pitchDiff
        } else {
            pitchMoveThisTick
        }

        val newYaw = current.yaw + actualYawMove
        val newPitch = current.pitch + actualPitchMove

        return Rotation(newYaw, newPitch)
    }
}