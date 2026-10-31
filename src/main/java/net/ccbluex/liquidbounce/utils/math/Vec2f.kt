package net.ccbluex.liquidbounce.utils.math

/**
 * A simple 2D vector with float precision
 */
data class Vec2f(val x: Float, val y: Float) {
    constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())

    operator fun plus(other: Vec2f) = Vec2f(x + other.x, y + other.y)
    operator fun minus(other: Vec2f) = Vec2f(x - other.x, y - other.y)
    operator fun times(value: Float) = Vec2f(x * value, y * value)
    operator fun div(value: Float) = Vec2f(x / value, y / value)

    fun length() = kotlin.math.sqrt(x * x + y * y)
    fun normalize() = this / length()
}