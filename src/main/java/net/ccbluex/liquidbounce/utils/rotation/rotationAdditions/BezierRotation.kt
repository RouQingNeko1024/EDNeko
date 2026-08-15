
/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.rotation.rotationAdditions

import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class BezierRotation {

    /**
     * 标准Bezier曲线旋转
     */
    fun bezierRotationTo(
        targetRotation: Rotation,
        currentRotation: Rotation,
        settings: RotationSettings,
        yawStep: Float,
        pitchStep: Float
    ): Rotation {
        val normalizedCurrentYaw = MathHelper.wrapAngleTo180_float(currentRotation.yaw)
        val normalizedTargetYaw = MathHelper.wrapAngleTo180_float(targetRotation.yaw)

        val yawIncrement = RotationUtils.angleDifference(normalizedTargetYaw, normalizedCurrentYaw)
        val pitchIncrement = targetRotation.pitch - currentRotation.pitch

        // 使用 bezierSpeed 控制整体速度
        val bezierSpeed = settings.bezierSpeed.get()

        // 应用 bezierSpeed 到步长
        val adjustedYawStep = yawStep * bezierSpeed
        val adjustedPitchStep = pitchStep * bezierSpeed

        // 控制点计算
        val control1 = Rotation(
            normalizedCurrentYaw + yawIncrement / 3,
            currentRotation.pitch + pitchIncrement / 3
        )

        val control2 = Rotation(
            normalizedCurrentYaw + 2 * yawIncrement / 3,
            currentRotation.pitch + 2 * pitchIncrement / 3
        )

        // 应用步长限制
        val yawFactor = adjustedYawStep.coerceIn(settings.minYawStep.get(), settings.maxYawStep.get())
        val pitchFactor = adjustedPitchStep.coerceIn(settings.minPitchStep.get(), settings.maxPitchStep.get())

        // Bezier混合
        val smoothYawFactor = bezierBlend(yawFactor)
        val smoothPitchFactor = bezierBlend(pitchFactor)

        // 插值新旋转
        val newRotation = bezierInterpolate(
            currentRotation,
            control1,
            control2,
            targetRotation,
            smoothYawFactor,
            smoothPitchFactor
        )

        // 应用疲劳效果
        val newRotationDiff = fatigueRotation(
            currentRotation.yaw, newRotation.yaw - currentRotation.yaw,
            currentRotation.pitch, newRotation.pitch - currentRotation.pitch,
            settings.horizontalAngleChange.random() * bezierSpeed,
            settings.verticalAngleChange.random() * bezierSpeed
        )

        return Rotation(
            currentRotation.yaw + newRotationDiff.yaw,
            currentRotation.pitch + newRotationDiff.pitch
        )
    }

    /**
     * 动态Bezier曲线旋转
     */
    fun dynamicBezierRotationTo(
        targetRotation: Rotation,
        currentRotation: Rotation,
        settings: RotationSettings,
        player: EntityPlayer,
        target: Entity
    ): Rotation {
        val minYawStep: Float = settings.minYawStep.get()
        val maxYawStep: Float = settings.maxYawStep.get()
        val minPitchStep: Float = settings.minPitchStep.get()
        val maxPitchStep: Float = settings.maxPitchStep.get()

        val bezierSpeed = settings.bezierSpeed.get()

        val playerBps = sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ).toFloat()
        val playerVerticalMotion = abs(player.motionY + 0.0784f)

        // 应用动态步长调整
        var yawStep = (sin(2f * Random.nextFloat() * PI.toFloat()) + playerBps * settings.dynamicYawStepAdjustmentStrength.get()) * bezierSpeed
        var pitchStep = (sin(2f * Random.nextFloat() * PI.toFloat()) + playerVerticalMotion * settings.dynamicPitchStepAdjustmentStrength.get()).toFloat() * bezierSpeed

        // 限制步长在最小最大值之间
        yawStep = yawStep.coerceIn(minYawStep, maxYawStep)
        pitchStep = pitchStep.coerceIn(minPitchStep, maxPitchStep)

        // 计算距离因子影响
        val distance = player.getDistanceToEntity(target)
        val maxDistance = 6.0f
        val normalizedDistance = (distance / maxDistance).coerceIn(0f, 1f)
        val distanceFactor = settings.distanceFactor.get() * normalizedDistance

        // 标准化当前和目标旋转
        val normalizedCurrentYaw = MathHelper.wrapAngleTo180_float(currentRotation.yaw)
        val normalizedTargetYaw = MathHelper.wrapAngleTo180_float(targetRotation.yaw)

        // 计算旋转差异
        val yawIncrement = RotationUtils.angleDifference(normalizedTargetYaw, normalizedCurrentYaw)
        val pitchIncrement = targetRotation.pitch - currentRotation.pitch

        // 控制点计算
        val control1 = Rotation(
            normalizedCurrentYaw + yawIncrement / 3f + (Random.nextFloat() * 40f - 20f) * distanceFactor,
            currentRotation.pitch + pitchIncrement / 3f + (Random.nextFloat() * 40f - 20f) * distanceFactor
        )

        val control2 = Rotation(
            normalizedCurrentYaw + 2f * yawIncrement / 3f + (Random.nextFloat() * 40f - 20f) * distanceFactor,
            currentRotation.pitch + 2f * pitchIncrement / 3f + (Random.nextFloat() * 40f - 20f) * distanceFactor
        )

        // 应用步长限制
        val yawFactor = yawStep.coerceIn(0.0f, 1.0f)
        val pitchFactor = pitchStep.coerceIn(0.0f, 1.0f)

        // Bezier混合
        val smoothYawFactor = bezierBlend(yawFactor)
        val smoothPitchFactor = bezierBlend(pitchFactor)

        // 插值新旋转
        val newRotation = bezierInterpolate(
            currentRotation,
            control1,
            control2,
            targetRotation,
            smoothYawFactor,
            smoothPitchFactor
        )

        // 应用疲劳效果
        val newRotationDiff = fatigueRotation(
            currentRotation.yaw, newRotation.yaw - currentRotation.yaw,
            currentRotation.pitch, newRotation.pitch - currentRotation.pitch,
            settings.horizontalAngleChange.random() * bezierSpeed,
            settings.verticalAngleChange.random() * bezierSpeed
        )

        return Rotation(
            currentRotation.yaw + newRotationDiff.yaw,
            currentRotation.pitch + newRotationDiff.pitch
        )
    }

    /**
     * Bezier混合函数
     */
    private fun bezierBlend(t: Float): Float {
        return t * t * (3f - 2f * t)
    }

    /**
     * Bezier插值函数
     */
    private fun bezierInterpolate(
        start: Rotation,
        control1: Rotation,
        control2: Rotation,
        end: Rotation,
        yawT: Float,
        pitchT: Float
    ): Rotation {
        val yaw = start.yaw + RotationUtils.angleDifference(end.yaw, start.yaw) * bezierBlend(yawT)

        val pitchU = 1f - pitchT
        val pitchUU = pitchU * pitchU
        val pitchUUU = pitchUU * pitchU
        val pitchTT = pitchT * pitchT
        val pitchTTT = pitchTT * pitchT

        var pitch = start.pitch * pitchUUU
        pitch += 3f * control1.pitch * pitchUU * pitchT
        pitch += 3f * control2.pitch * pitchU * pitchTT
        pitch += end.pitch * pitchTTT

        return Rotation(yaw, pitch)
    }

    /**
     * 疲劳效果计算
     */
    private fun fatigueRotation(
        currentYaw: Float,
        yawOutput: Float,
        currentPitch: Float,
        pitchOutput: Float,
        yawMaxSpeed: Float,
        pitchMaxSpeed: Float
    ): Rotation {
        val targetYaw = currentYaw + yawOutput.coerceIn(-yawMaxSpeed, yawMaxSpeed)
        var yawDiff = targetYaw - currentYaw

        val targetPitch = currentPitch + pitchOutput.coerceIn(-pitchMaxSpeed, pitchMaxSpeed)
        var pitchDiff = targetPitch - currentPitch

        var fatigueFactorPitch = 0f
        var fatigueFactorYaw = 0f

        if (abs(yawDiff) >= yawMaxSpeed) {
            val t = abs(yawOutput % yawMaxSpeed) / abs(yawMaxSpeed)
            val fatigueFactor = 1 - t
            yawDiff *= fatigueFactor
            fatigueFactorPitch = abs(yawDiff / yawMaxSpeed)
        }

        if (abs(pitchDiff) >= pitchMaxSpeed) {
            val t = abs(pitchOutput % pitchMaxSpeed) / abs(pitchMaxSpeed)
            val fatigueFactor = 1 - t
            pitchDiff *= fatigueFactor
            fatigueFactorYaw = abs(pitchDiff / pitchMaxSpeed)
        }

        return Rotation(yawDiff + fatigueFactorYaw, pitchDiff + fatigueFactorPitch)
    }
}
