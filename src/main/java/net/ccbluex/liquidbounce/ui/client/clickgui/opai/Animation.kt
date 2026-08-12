package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import kotlin.math.*

class Animation(val easing: Easing, val duration: Long) {

    private var startValue = 0f
    private var targetValue = 0f
    private var currentValue = 0f
    private var startTime = 0L
    private var running = false

    fun setStartValue(value: Float) {
        this.startValue = value
        this.currentValue = value
    }

    fun run(target: Float) {
        if (target != targetValue) {
            targetValue = target
            startValue = currentValue
            startTime = System.currentTimeMillis()
            running = true
        }

        if (running) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= duration) {
                currentValue = targetValue
                running = false
            } else {
                val t = elapsed.toFloat() / duration
                currentValue = startValue + (targetValue - startValue) * map(t)
            }
        }
    }

    fun getValue(): Float = currentValue

    private fun map(t: Float): Float {
        var x = t
        return when (easing) {
            Easing.EASE_IN_QUAD -> x * x
            Easing.EASE_OUT_QUAD -> x * (2 - x)
            Easing.EASE_IN_OUT_QUAD -> if (x < 0.5f) 2 * x * x else -1 + (4 - 2 * x) * x
            Easing.EASE_IN_CUBIC -> x * x * x
            Easing.EASE_OUT_CUBIC -> {
                x -= 1f
                x * x * x + 1
            }
            Easing.EASE_IN_OUT_CUBIC -> if (x < 0.5f) 4 * x * x * x else (x - 1) * (2 * x - 2) * (2 * x - 2) + 1
            Easing.EASE_OUT_SINE -> sin((x * PI) / 2).toFloat()
            Easing.EASE_IN_OUT_SINE -> (-(cos(PI * x) - 1) / 2).toFloat()
            Easing.EASE_OUT_ELASTIC -> {
                val c4 = ((2 * PI) / 3).toFloat()
                when {
                    x == 0f -> 0f
                    x == 1f -> 1f
                    else -> (2.0.pow(-10.0 * x) * sin((x * 10 - 0.75) * c4) + 1).toFloat()
                }
            }
            Easing.EASE_IN_OUT_EXPO -> {
                when {
                    x == 0f -> 0f
                    x == 1f -> 1f
                    x < 0.5f -> (2.0.pow(20.0 * x - 10.0) / 2).toFloat()
                    else -> (2 - 2.0.pow(-20.0 * x + 10.0) / 2).toFloat()
                }
            }
            else -> x
        }
    }
}
