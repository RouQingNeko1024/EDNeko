package net.ccbluex.liquidbounce.utils.rotation.rotationAdditions

import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.minecraft.util.MathHelper
import kotlin.math.*

class SmoothRotation {

    // 平滑旋转类型枚举
    enum class SmoothRotationType {
        INTERPOLATION_LINEAR,
        INTERPOLATION_SLERP,
        INTERPOLATION_EASE_IN_OUT,

        LAGRANGE,
        INTERPOLATION_HALF_DOWN,
        INTERPOLATION_HALF_UP,
        COSINE,
        SINUSOIDAL,
        LOGARITHM,

        HERMITE,
        SMOOTH_STEP,
        EASE_IN_QUAD,
        EASE_OUT_QUAD,
        EASE_IN_OUT_CUBIC,
        BOUNCE_OUT,
        ELASTIC_OUT,
        CIRCULAR,
        EXPONENTIAL
    }

    fun smoothRotation(
        current: Rotation,
        target: Rotation,
        alpha: ClosedFloatingPointRange<Float>,
        smoothRotationType: SmoothRotationType = SmoothRotationType.INTERPOLATION_LINEAR,
        tension: Float = 0.5f,
        bias: Float = 0.0f,
        amplitude: Float = 1.0f,
        period: Float = 0.3f
    ): Rotation {
        val alphaValue = alpha.random()

        return when (smoothRotationType) {
            SmoothRotationType.LAGRANGE -> current.lagrangeInterpolate(target, alphaValue)
            SmoothRotationType.INTERPOLATION_HALF_DOWN -> current.halfDownInterpolate(target, alphaValue)
            SmoothRotationType.INTERPOLATION_HALF_UP -> current.halfUpInterpolate(target, alphaValue)
            SmoothRotationType.COSINE -> current.cosineInterpolate(target, alphaValue)
            SmoothRotationType.SINUSOIDAL -> current.sineInterpolate(target, alphaValue)
            SmoothRotationType.LOGARITHM -> current.logInterpolate(target, alphaValue)
            SmoothRotationType.INTERPOLATION_LINEAR -> current.lerp(target, alphaValue)
            SmoothRotationType.INTERPOLATION_SLERP -> current.slerp(target, alphaValue)
            SmoothRotationType.INTERPOLATION_EASE_IN_OUT -> current.easeTo(target, alphaValue, EasingType.EASE_IN_OUT)
            SmoothRotationType.HERMITE -> {
                val prev = current.lerp(target, -0.1f)
                val next = current.lerp(target, 1.1f)
                current.hermiteInterpolate(prev, next, alphaValue, tension, bias)
            }
            SmoothRotationType.SMOOTH_STEP -> current.smoothStepTo(target, alphaValue)
            SmoothRotationType.EASE_IN_QUAD -> current.easeTo(target, alphaValue, EasingType.EASE_IN_QUAD)
            SmoothRotationType.EASE_OUT_QUAD -> current.easeTo(target, alphaValue, EasingType.EASE_OUT_QUAD)
            SmoothRotationType.EASE_IN_OUT_CUBIC -> current.easeTo(target, alphaValue, EasingType.EASE_IN_OUT_CUBIC)
            SmoothRotationType.BOUNCE_OUT -> current.bounceOutTo(target, alphaValue, amplitude)
            SmoothRotationType.ELASTIC_OUT -> current.elasticOutTo(target, alphaValue, amplitude, period)
            SmoothRotationType.CIRCULAR -> current.circularTo(target, alphaValue)
            SmoothRotationType.EXPONENTIAL -> current.exponentialTo(target, alphaValue)
        }
    }

    // 缓动类型枚举
    enum class EasingType {
        LINEAR,
        EASE_IN_OUT,
        EASE_OUT_BACK,
        EASE_IN_QUAD,
        EASE_OUT_QUAD,
        EASE_IN_OUT_CUBIC
    }

    // Rotation 扩展函数的实现

    fun Rotation.lerp(target: Rotation, alpha: Float): Rotation {
        val yawDiff = angleDifference(target.yaw, this.yaw)
        val pitchDiff = target.pitch - this.pitch
        return Rotation(
            this.yaw + yawDiff * alpha,
            this.pitch + pitchDiff * alpha
        ).withLimitedPitch()
    }

    // 四元数辅助类
    private data class Quaternion(val w: Float, val x: Float, val y: Float, val z: Float) {
        fun slerp(target: Quaternion, alpha: Float): Quaternion {
            var dot = w * target.w + x * target.x + y * target.y + z * target.z

            // 如果点积为负，四元数取反以保证最短路径
            val targetAdjusted = if (dot < 0) {
                dot = -dot
                target.copy(w = -target.w, x = -target.x, y = -target.y, z = -target.z)
            } else {
                target
            }

            return if (dot > 0.9995f) {
                // 线性插值当角度非常小时
                lerp(targetAdjusted, alpha).normalized()
            } else {
                val theta0 = acos(dot) // 角度
                val theta = theta0 * alpha // 插值角度

                val s0 = cos(theta) - dot * sin(theta) / sin(theta0)
                val s1 = sin(theta) / sin(theta0)

                Quaternion(
                    s0 * w + s1 * targetAdjusted.w,
                    s0 * x + s1 * targetAdjusted.x,
                    s0 * y + s1 * targetAdjusted.y,
                    s0 * z + s1 * targetAdjusted.z
                ).normalized()
            }
        }

        private fun lerp(target: Quaternion, alpha: Float): Quaternion {
            return Quaternion(
                w + alpha * (target.w - w),
                x + alpha * (target.x - x),
                y + alpha * (target.y - y),
                z + alpha * (target.z - z)
            )
        }

        private fun normalized(): Quaternion {
            val len = sqrt(w * w + x * x + y * y + z * z)
            return if (len > 0) {
                Quaternion(w / len, x / len, y / len, z / len)
            } else {
                this
            }
        }
    }

    private fun Rotation.toQuaternion(): Quaternion {
        val halfYaw = yaw.toRadians() * 0.5f
        val halfPitch = pitch.toRadians() * 0.5f

        val cy = cos(halfYaw)
        val sy = sin(halfYaw)
        val cp = cos(halfPitch)
        val sp = sin(halfPitch)

        return Quaternion(
            cy * cp,
            sy * sp,
            sy * cp,
            cy * sp
        )
    }

    private fun Quaternion.toRotation(): Rotation {
        // 四元数转欧拉角
        val yaw = atan2(2f * (w * z + x * y), 1f - 2f * (y * y + z * z)).toDegreesF()
        val pitch = asin(2f * (w * y - z * x)).toDegreesF()
        return Rotation(yaw, pitch)
    }

    fun Rotation.slerp(target: Rotation, alpha: Float): Rotation {
        return this.toQuaternion()
            .slerp(target.toQuaternion(), alpha)
            .toRotation()
            .withLimitedPitch()
    }

    fun Rotation.easeTo(target: Rotation, alpha: Float, easingType: EasingType = EasingType.EASE_IN_OUT): Rotation {
        val easedAlpha = when (easingType) {
            EasingType.LINEAR -> alpha
            EasingType.EASE_IN_OUT -> 0.5f - 0.5f * cos(alpha * Math.PI.toFloat())
            EasingType.EASE_OUT_BACK -> {
                val c1 = 1.70158f
                val c3 = c1 + 1f
                1f + c3 * (alpha - 1f).pow(3) + c1 * (alpha - 1f).pow(2)
            }
            EasingType.EASE_IN_QUAD -> alpha * alpha
            EasingType.EASE_OUT_QUAD -> 1f - (1f - alpha) * (1f - alpha)
            EasingType.EASE_IN_OUT_CUBIC -> {
                if (alpha < 0.5f) 4f * alpha * alpha * alpha
                else 1f - (-2f * alpha + 2f).pow(3) / 2f
            }
        }
        return this.lerp(target, easedAlpha.coerceIn(0f, 1f))
    }

    fun Rotation.logInterpolate(target: Rotation, alpha: Float): Rotation {
        val percent = ln(1 + alpha)
        return this.lerp(target, percent)
    }

    fun Rotation.sineInterpolate(target: Rotation, alpha: Float): Rotation {
        val percent = sin(alpha * Math.PI.toFloat()) / 2
        return this.lerp(target, percent)
    }

    fun Rotation.cosineInterpolate(target: Rotation, alpha: Float): Rotation {
        val percent = 1 - cos(alpha * Math.PI.toFloat()) / 2
        return this.lerp(target, percent)
    }

    fun Rotation.halfUpInterpolate(
        target: Rotation,
        alpha: Float
    ): Rotation {
        return Rotation(
            (this.yaw + target.yaw) / 2f + alpha * (target.yaw - this.yaw),
            (this.pitch + target.pitch) / 2f + alpha * (target.pitch - this.pitch)
        ).withLimitedPitch()
    }

    fun Rotation.halfDownInterpolate(
        target: Rotation,
        alpha: Float
    ): Rotation {
        return Rotation(
            (this.yaw + target.yaw) / 2f - alpha * (target.yaw - this.yaw),
            (this.pitch + target.pitch) / 2f - alpha * (target.pitch - this.pitch)
        ).withLimitedPitch()
    }

    fun Rotation.lagrangeInterpolate(
        target: Rotation,
        alpha: Float
    ): Rotation {
        // L(x) = y0 * l0(x) + y1 * l1(x)
        // l0(x) = (x - x1)/(x0 - x1), l1(x) = (x - x0)/(x1 - x0)
        val l0 = 1 - alpha
        val l1 = alpha
        return Rotation(
            this.yaw * l0 + target.yaw * l1,
            this.pitch * l0 + target.pitch * l1
        ).withLimitedPitch()
    }

    fun Rotation.hermiteInterpolate(
        prev: Rotation,
        next: Rotation,
        alpha: Float,
        tension: Float = 0.5f,
        bias: Float = 0.0f
    ): Rotation {
        val alpha2 = alpha * alpha
        val alpha3 = alpha2 * alpha

        // Hermite 基函数
        val h1 = 2 * alpha3 - 3 * alpha2 + 1
        val h2 = -2 * alpha3 + 3 * alpha2
        val h3 = alpha3 - 2 * alpha2 + alpha
        val h4 = alpha3 - alpha2

        // 计算切线（使用前后点）
        val tensionFactor = (1 - tension) * 0.5f
        val yawTangentPrev = angleDifference(this.yaw, prev.yaw) * tensionFactor
        val yawTangentNext = angleDifference(next.yaw, this.yaw) * tensionFactor

        val pitchTangentPrev = (this.pitch - prev.pitch) * tensionFactor
        val pitchTangentNext = (next.pitch - this.pitch) * tensionFactor

        // 应用 bias
        val yawTangent1 = (1 + bias) * tensionFactor * yawTangentPrev + (1 - bias) * tensionFactor * yawTangentNext
        val yawTangent2 = (1 + bias) * tensionFactor * yawTangentNext + (1 - bias) * tensionFactor * yawTangentPrev

        val pitchTangent1 = (1 + bias) * tensionFactor * pitchTangentPrev + (1 - bias) * tensionFactor * pitchTangentNext
        val pitchTangent2 = (1 + bias) * tensionFactor * pitchTangentNext + (1 - bias) * tensionFactor * pitchTangentPrev

        // 插值计算
        val yaw = this.yaw * h1 + next.yaw * h2 + yawTangent1 * h3 + yawTangent2 * h4
        val pitch = this.pitch * h1 + next.pitch * h2 + pitchTangent1 * h3 + pitchTangent2 * h4

        return Rotation(yaw, pitch).withLimitedPitch()
    }

    // 新增平滑旋转方法
    fun Rotation.smoothStepTo(target: Rotation, alpha: Float): Rotation {
        val easedAlpha = alpha * alpha * (3f - 2f * alpha) // smoothstep 函数
        return this.lerp(target, easedAlpha)
    }

    fun Rotation.bounceOutTo(target: Rotation, alpha: Float, amplitude: Float = 1.0f): Rotation {
        val easedAlpha = when {
            alpha < (1f / 2.75f) -> 7.5625f * alpha * alpha
            alpha < (2f / 2.75f) -> {
                val t = alpha - (1.5f / 2.75f)
                7.5625f * t * t + 0.75f
            }
            alpha < (2.5f / 2.75f) -> {
                val t = alpha - (2.25f / 2.75f)
                7.5625f * t * t + 0.9375f
            }
            else -> {
                val t = alpha - (2.625f / 2.75f)
                7.5625f * t * t + 0.984375f
            }
        }
        return this.lerp(target, easedAlpha.coerceIn(0f, 1f) * amplitude)
    }

    fun Rotation.elasticOutTo(target: Rotation, alpha: Float, amplitude: Float = 1.0f, period: Float = 0.3f): Rotation {
        val easedAlpha = when (alpha) {
            0f -> 0f
            1f -> 1f
            else -> {
                val s = period / 4f
                val p = period
                val a = amplitude
                val t = alpha - 1f
                -(a * 2f.pow(10f * t) * sin((t - s) * (2f * Math.PI.toFloat()) / p)) + 1f
            }
        }
        return this.lerp(target, easedAlpha)
    }

    fun Rotation.circularTo(target: Rotation, alpha: Float): Rotation {
        val easedAlpha = 1f - sqrt(1f - alpha * alpha)
        return this.lerp(target, easedAlpha)
    }

    fun Rotation.exponentialTo(target: Rotation, alpha: Float): Rotation {
        val easedAlpha = when {
            alpha == 0f -> 0f
            else -> 2f.pow(10f * (alpha - 1f))
        }
        return this.lerp(target, easedAlpha.coerceIn(0f, 1f))
    }

    private fun angleDifference(a: Float, b: Float) = MathHelper.wrapAngleTo180_float(a - b)

    private fun Rotation.withLimitedPitch(): Rotation {
        return Rotation(yaw, pitch.coerceIn(-90f, 90f))
    }

    private fun Float.toDegreesF(): Float = Math.toDegrees(this.toDouble()).toFloat()
    private fun Float.toRadians(): Float = Math.toRadians(this.toDouble()).toFloat()
    private fun Float.pow(n: Float): Float = this.toDouble().pow(n.toDouble()).toFloat()
}
