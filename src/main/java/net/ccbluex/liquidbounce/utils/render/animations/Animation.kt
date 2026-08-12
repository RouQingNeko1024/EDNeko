package net.ccbluex.liquidbounce.utils.render.animations

import net.ccbluex.liquidbounce.utils.timer.TimerUtil

abstract class Animation(val duration: Int, val endPoint: Double, var direction: Direction = Direction.FORWARDS) {
    val timerUtil = TimerUtil()

    fun finished(direction: Direction): Boolean {
        return isDone && this.direction == direction
    }

    val isDone: Boolean
        get() = timerUtil.hasTimeElapsed(duration.toLong())

    fun changeDirection() {
        direction = direction.opposite()
    }

    fun setDirection(direction: Direction): Animation {
        if (this.direction != direction) {
            this.direction = direction
            timerUtil.time = System.currentTimeMillis() - (duration - timerUtil.getTime().coerceAtMost(duration.toLong()))
        }
        return this
    }

    open fun correctOutput(): Boolean = false

    val output: Double
        get() {
            return if (direction.forwards()) {
                if (isDone) {
                    endPoint
                } else {
                    getEquation(timerUtil.getTime() / duration.toDouble()) * endPoint
                }
            } else {
                if (isDone) {
                    0.0
                } else {
                    if (correctOutput()) {
                        val revTime = duration.toLong().coerceAtMost((duration.toLong() - timerUtil.getTime()).coerceAtLeast(0))
                        getEquation(revTime / duration.toDouble()) * endPoint
                    } else {
                        (1 - getEquation(timerUtil.getTime() / duration.toDouble())) * endPoint
                    }
                }
            }
        }

    protected abstract fun getEquation(x: Double): Double
}
