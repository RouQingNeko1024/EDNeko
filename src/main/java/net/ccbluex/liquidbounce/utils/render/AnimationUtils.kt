/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.render

import kotlin.math.pow
import kotlin.math.sin

object AnimationUtils {
    /**
     * In-out-easing function
     * https://github.com/jesusgollonet/processing-penner-easing
     *
     * @param t Current iteration
     * @param d Total iterations
     * @return Eased value
     */
    fun easeOut(t: Float, d: Float) = (t / d - 1).pow(3) + 1

    /**
     * Source: https://easings.net/#easeOutElastic
     *
     * @return A value larger than 0
     */
    fun easeOutElastic(x: Float) =
        when (x) {
            0f, 1f -> x
            else -> 2f.pow(-10 * x) * sin((x * 10 - 0.75f) * (2 * Math.PI / 3f).toFloat()) + 1
        }

    /**
     * Animate a value towards a target
     *
     * @param target Target value
     * @param current Current value
     * @param speed Animation speed
     * @return Animated value
     */
    fun animate(target: Double, current: Double, speed: Double): Double {
        if (current == target) return current

        val larger = target > current
        val speed = speed.coerceIn(0.0, 1.0)

        val dif = maxOf(target, current) - minOf(target, current)
        var factor = dif * speed
        if (factor < 0.1) {
            factor = 0.1
        }

        var result = current
        if (larger) {
            result += factor
            if (result >= target) result = target
        } else if (target < current) {
            result -= factor
            if (result <= target) result = target
        }

        return result
    }

    /**
     * Animate a value towards a target
     *
     * @param target Target value
     * @param current Current value
     * @param speed Animation speed
     * @return Animated value
     */
    fun animate(target: Float, current: Float, speed: Float): Float {
        if (current == target) return current

        val larger = target > current
        val speed = speed.coerceIn(0.0f, 1.0f)

        val dif = maxOf(target, current) - minOf(target, current)
        var factor = dif * speed
        if (factor < 0.1f) {
            factor = 0.1f
        }

        var result = current
        if (larger) {
            result += factor
            if (result >= target) result = target
        } else if (target < current) {
            result -= factor
            if (result <= target) result = target
        }

        return result
    }
}
