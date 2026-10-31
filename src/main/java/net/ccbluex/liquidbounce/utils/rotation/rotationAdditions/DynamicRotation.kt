
/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.rotation.rotations

import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import kotlin.math.abs
import kotlin.random.Random

class DynamicRotation {

    // 动态状态变量
    private var currentDynamicMultiplier: Float = 1.0f
    private var dynamicTicksRemaining: Int = 0
    private var lastUpdateTick: Long = 0

    /**
     * 处理动态旋转模式
     */
    fun handleDynamicRotationMode(
        currentRotation: Rotation,
        targetRotation: Rotation,
        settings: RotationSettings
    ): Rotation {
        // 检查是否需要更新动态效果
        val currentTick = System.currentTimeMillis() / 50
        if (dynamicTicksRemaining <= 0 || currentTick - lastUpdateTick >= settings.dynamicUpdateInterval.get()) {
            updateDynamicEffect(settings)
            lastUpdateTick = currentTick
        }

        dynamicTicksRemaining--

        // 获取基础速度
        val baseHSpeed = settings.horizontalSpeed
        val baseVSpeed = settings.verticalSpeed

        // 应用动态乘数
        val dynamicHSpeed = baseHSpeed * currentDynamicMultiplier
        val dynamicVSpeed = baseVSpeed * currentDynamicMultiplier

        // 处理旋转变化
        val (yawDiff, pitchDiff) = RotationUtils.angleDifferences(targetRotation, currentRotation)

        // 根据效果类型处理
        return when (settings.currentDynamicEffect) {
            "SpeedBoost", "SpeedReduction" -> {
                // 标准速度调整
                RotationUtils.performAngleChange(
                    currentRotation,
                    targetRotation,
                    dynamicHSpeed,
                    dynamicVSpeed,
                    settings.legitimize,
                    settings.legitimizeFactor.range,
                    settings.minRotationDifference,
                    settings.minRotationDifferenceResetTiming
                )
            }
            "Jitter" -> {
                // 添加抖动效果
                val maxJitterYaw = dynamicHSpeed.coerceAtMost(abs(yawDiff))
                val maxJitterPitch = dynamicVSpeed.coerceAtMost(abs(pitchDiff))

                // 添加随机抖动
                val jitterStrength = settings.dynamicJitterStrength.random
                val randomYaw = if (yawDiff != 0f) {
                    maxJitterYaw * (1 + Random.nextFloat() * jitterStrength - jitterStrength / 2)
                } else {
                    0f
                }

                val randomPitch = if (pitchDiff != 0f) {
                    maxJitterPitch * (1 + Random.nextFloat() * jitterStrength - jitterStrength / 2)
                } else {
                    0f
                }

                // 保持旋转方向
                val finalYaw = if (yawDiff > 0) {
                    currentRotation.yaw + abs(randomYaw)
                } else {
                    currentRotation.yaw - abs(randomYaw)
                }

                val finalPitch = if (pitchDiff > 0) {
                    currentRotation.pitch + abs(randomPitch)
                } else {
                    currentRotation.pitch - abs(randomPitch)
                }

                Rotation(finalYaw, finalPitch)
            }
            "Smooth" -> {
                // 平滑过渡
                val smoothFactor = settings.dynamicSmoothFactor.random
                val smoothYaw = currentRotation.yaw + yawDiff * smoothFactor
                val smoothPitch = currentRotation.pitch + pitchDiff * smoothFactor

                Rotation(smoothYaw, smoothPitch)
            }
            else -> {
                // 默认使用标准速度调整
                RotationUtils.performAngleChange(
                    currentRotation,
                    targetRotation,
                    dynamicHSpeed,
                    dynamicVSpeed,
                    settings.legitimize,
                    settings.legitimizeFactor.range,
                    settings.minRotationDifference,
                    settings.minRotationDifferenceResetTiming
                )
            }
        }
    }

    /**
     * 更新动态效果
     */
    private fun updateDynamicEffect(settings: RotationSettings) {
        // 随机选择新的动态效果
        val effectTypes = settings.dynamicEffectTypes.get()
        if (effectTypes.isEmpty()) return

        val randomEffect = effectTypes.random()
        settings.currentDynamicEffect = randomEffect

        // 设置持续时间和乘数
        dynamicTicksRemaining = settings.dynamicDuration.get().random()

        when (randomEffect) {
            "SpeedBoost" -> {
                currentDynamicMultiplier = settings.dynamicSpeedBoostMultiplier.get().random()
            }
            "SpeedReduction" -> {
                currentDynamicMultiplier = settings.dynamicSpeedReductionMultiplier.get().random()
            }
            "Jitter" -> {
                currentDynamicMultiplier = 1.0f
            }
            "Smooth" -> {
                currentDynamicMultiplier = settings.dynamicSmoothMultiplier.get().random()
            }
        }

        // 限制在最小/最大值范围内
        currentDynamicMultiplier = currentDynamicMultiplier.coerceIn(
            settings.dynamicMinMultiplier.get(),
            settings.dynamicMaxMultiplier.get()
        )
    }

    /**
     * 重置动态旋转状态
     */
    fun reset() {
        currentDynamicMultiplier = 1.0f
        dynamicTicksRemaining = 0
        lastUpdateTick = 0
    }
}