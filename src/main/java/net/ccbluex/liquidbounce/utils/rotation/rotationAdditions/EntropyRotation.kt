package net.ccbluex.liquidbounce.utils.rotation.rotations

import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.lastRotations
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.performAngleChange
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.rotation.pow
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class EntropyRotation {
    /**
     * 熵控制旋转模式 - 专注于战斗时的自然转头行为
     */
    fun handleEntropyControlledMode(
        current: Rotation,
        target: Rotation,
        settings: RotationSettings
    ): Rotation {
        // 1. 计算当前熵值
        val currentEntropy = calculateCurrentEntropy(settings)
        settings.currentEntropy = currentEntropy

        // 2. 直接使用配置的目标熵值
        val targetEntropy = settings.entropyTarget
        val entropyDeviation = currentEntropy - targetEntropy

        // 3. 动态调节
        return when {
            abs(entropyDeviation) > settings.entropyTolerance -> {
                if (entropyDeviation > 0) {
                    applyEntropySmoothing(current, target, settings, entropyDeviation)
                } else {
                    applyEntropyRandomness(current, target, settings, entropyDeviation)
                }
            }
            else -> {
                // 熵值正常 - 使用标准算法
                performStandardRotation(current, target, settings)
            }
        }
    }
    /**
     * 更新熵历史记录
     */
    private fun updateEntropyHistory(settings: RotationSettings, currentEntropy: Float) {
        val now = System.currentTimeMillis()
        if (now - settings.lastEntropyUpdate > 50) {
            settings.entropyHistory.addLast(currentEntropy)
            if (settings.entropyHistory.size > 60) {
                settings.entropyHistory.removeFirst()
            }
            settings.lastEntropyUpdate = now
        }
    }

    /**
     * 计算平滑后的熵值
     */
    private fun calculateSmoothedEntropy(settings: RotationSettings): Float {
        if (settings.entropyHistory.isEmpty()) return 0.5f

        // 使用加权平均，近期值权重更高
        val recentCount = min(settings.entropyHistory.size, 10)
        val recentSum = settings.entropyHistory.takeLast(recentCount).sum()
        return (recentSum / recentCount).coerceIn(0f, 1f)
    }

    /**
     * 计算当前旋转行为的熵值
     */
    private fun calculateCurrentEntropy(settings: RotationSettings): Float {
        val rotationHistory = getRecentRotations()
        if (rotationHistory.size < 3) return 0.5f // 默认值

        val currentEntropy = calculateInstantEntropy(rotationHistory)

        // 更新熵历史记录
        updateEntropyHistory(settings, currentEntropy)

        // 使用历史平均值获得更稳定的熵值
        return calculateSmoothedEntropy(settings)
    }

    /**
     * 计算瞬时熵值（不包含历史平滑）
     */
    private fun calculateInstantEntropy(rotationHistory: List<Rotation>): Float {
        val velocityEntropy = calculateVelocityEntropy(rotationHistory)
        val directionEntropy = calculateDirectionEntropy(rotationHistory)
        return (velocityEntropy * 0.6f + directionEntropy * 0.4f)
    }

    /**
     * 计算速度变化的熵值
     */
    private fun calculateVelocityEntropy(rotations: List<Rotation>): Float {
        val velocities = mutableListOf<Float>()
        for (i in 1 until rotations.size) {
            val diff = rotationDifference(rotations[i], rotations[i-1])
            velocities.add(diff)
        }

        if (velocities.isEmpty()) return 0.5f

        val mean = velocities.average().toFloat()
        if (mean == 0f) return 0.5f

        val stdDev = sqrt(velocities.map { (it - mean).pow(2) }.average()).toFloat()
        return (stdDev / mean).coerceIn(0f, 1f)
    }

    /**
     * 计算方向变化的熵值
     */
    private fun calculateDirectionEntropy(rotations: List<Rotation>): Float {
        val directionChanges = mutableListOf<Float>()
        for (i in 2 until rotations.size) {
            val dir1 = angleDifference(rotations[i-1].yaw, rotations[i-2].yaw)
            val dir2 = angleDifference(rotations[i].yaw, rotations[i-1].yaw)
            directionChanges.add(abs(dir1 - dir2))
        }

        if (directionChanges.isEmpty()) return 0.5f

        return (directionChanges.average().toFloat() / 15f).coerceIn(0f, 1f)
    }

    /**
     * 应用熵平滑
     */
    private fun applyEntropySmoothing(
        current: Rotation,
        target: Rotation,
        settings: RotationSettings,
        entropyDeviation: Float
    ): Rotation {
        val strength = calculateAdjustmentStrength(entropyDeviation, settings)
        val smoothFactor = settings.entropySmoothingStrength.random() * strength

        val adjustedSpeed = settings.horizontalSpeed * (1f - smoothFactor * 0.3f)

        return performAngleChange(
            current, target,
            adjustedSpeed,
            settings.verticalSpeed * (1f - smoothFactor * 0.3f),
            true, (0.9f..1.1f), // 更小的随机范围
            settings.minRotationDifference + smoothFactor * 2.0f,
            "OnSlowDown"
        )
    }

    /**
     * 应用熵随机性
     */
    private fun applyEntropyRandomness(
        current: Rotation,
        target: Rotation,
        settings: RotationSettings,
        entropyDeviation: Float
    ): Rotation {
        val strength = calculateAdjustmentStrength(entropyDeviation, settings)
        val randomness = settings.entropyRandomnessStrength.random() * strength

        val baseRotation = performAngleChange(
            current, target,
            settings.horizontalSpeed * (1f + randomness * 0.2f),
            settings.verticalSpeed * (1f + randomness * 0.15f),
            true, (0.7f..1.4f + randomness * 0.3f), // 更大的随机范围
            settings.minRotationDifference,
            settings.minRotationDifferenceResetTiming
        )

        // 添加额外抖动
        return addControlledJitter(baseRotation, randomness, settings)
    }

    /**
     * 添加受控抖动
     */
    private fun addControlledJitter(rotation: Rotation, strength: Float, settings: RotationSettings): Rotation {
        // 基于设置调整抖动强度
        val baseStrength = settings.entropyRandomnessStrength.random()
        val adjustedStrength = strength * baseStrength

        val jitterYaw = nextFloat(-1f, 1f) * adjustedStrength * 1.5f
        val jitterPitch = nextFloat(-0.5f, 0.5f) * adjustedStrength * 1.0f

        return Rotation(
            rotation.yaw + jitterYaw,
            (rotation.pitch + jitterPitch).coerceIn(-90f, 90f)
        ).fixedSensitivity()
    }

    /**
     * 计算调节强度
     */
    private fun calculateAdjustmentStrength(deviation: Float, settings: RotationSettings): Float {
        val normalizedDeviation = abs(deviation) / settings.entropyTolerance
        return (normalizedDeviation.pow(1.5f) * settings.entropyAdjustmentSpeed).coerceIn(0f, 1f)
    }

    /**
     * 获取最近的旋转历史
     */
    private fun getRecentRotations(): List<Rotation> {
        return listOfNotNull(currentRotation, serverRotation) + lastRotations.take(3).filter { it != Rotation.ZERO }
    }

    /**
     * 标准旋转执行
     */
    private fun performStandardRotation(
        current: Rotation,
        target: Rotation,
        settings: RotationSettings
    ): Rotation {
        return performAngleChange(
            current, target,
            settings.horizontalSpeed, settings.verticalSpeed,
            settings.legitimize, settings.legitimizeFactor.range,
            settings.minRotationDifference, settings.minRotationDifferenceResetTiming
        )
    }
}