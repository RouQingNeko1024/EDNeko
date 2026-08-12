package net.ccbluex.liquidbounce.utils.render.animations.impl

import net.ccbluex.liquidbounce.utils.render.animations.Animation
import net.ccbluex.liquidbounce.utils.render.animations.Direction

class EaseBackIn(ms: Int, endPoint: Double, private val easeAmount: Float) : Animation(ms, endPoint) {

    constructor(ms: Int, endPoint: Double, easeAmount: Float, direction: Direction) : this(ms, endPoint, easeAmount) {
        this.direction = direction
    }

    override fun correctOutput(): Boolean = true

    override fun getEquation(x: Double): Double {
        val shrink = easeAmount + 1
        return Math.max(0.0, 1 + shrink * Math.pow(x - 1, 3.0) + easeAmount * Math.pow(x - 1, 2.0))
    }
}
