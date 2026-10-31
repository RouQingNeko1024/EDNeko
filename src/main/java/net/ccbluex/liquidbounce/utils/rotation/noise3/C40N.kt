package net.ccbluex.liquidbounce.utils.rotation.noise3

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

const val EPS = 1.0e-9
const val MAX_BISECT = 100
const val MAX_SIM_STEPS = 1000
const val INITIAL_SPEED = 3.0
const val ACCEL_COEF = 1.0
const val REGULAR_SPEED = 1.0
const val LARGE_ROT_SPEED_DEFAULT = 1.0
const val LARGE_ROT_THRESHOLD = 10.0
const val RESYNC_INTERVAL = 0.5
const val DT = 0.15
const val OVERSHOOT_CLAMP = 1200.0

/**
 * 对应原版轴对称状态。
 * 构造参数顺序: (accelFactor, totalRemaining, acceleration, baseVelocity, timeStep, currentVelocity)
 * maxVelocity 不在构造参数中，默认 = INITIAL_SPEED (3.0)，由外部或 tuneSteps 覆盖
 */
data class AxisState(
    var accelFactor: Double,    // 加速度系数，构造时 = 3.0
    var totalRemaining: Double,  // 目标位移（带符号）
    var acceleration: Double,    // vmax = sqrt(2*a*|d|) 里的 a
    var baseVelocity: Double,    // 基础速度，通常 0
    var timeStep: Double,        // dt = 0.15
    var currentVelocity: Double  // 当前速度，会被 simulate 更新
) {
    var maxVelocity: Double = INITIAL_SPEED   // 速度上限，默认 = 3.0，由 tuneSteps 二分调整

    fun copy(): AxisState {
        val c = AxisState(accelFactor, totalRemaining, acceleration, baseVelocity, timeStep, currentVelocity)
        c.maxVelocity = this.maxVelocity
        return c
    }
}

data class PathPoint(
    val velocity: Double,
    val position: Double,
    val acceleration: Double
)

data class Vec2(val x: Double, val y: Double) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
}

data class StepResult(
    val targetPos: Vec2,
    val velocity: Vec2,
    val needResync: Boolean
)

class C40N {

    var largeRotSpeed: Double = LARGE_ROT_SPEED_DEFAULT

    var onStep: (() -> Unit)? = null
    var onResync: (() -> Unit)? = null
    var pendingSteps: Int = -1

    /**
     * 半隐式 Euler 物理模拟，返回路径点列表。
     * @param s 会被原地修改，调用前请先 copy 如果需要保留原状态
     */
    fun simulate(s: AxisState): List<PathPoint> {
        val out = mutableListOf<PathPoint>()
        var position = 0.0
        var remaining = s.totalRemaining
        val total = s.totalRemaining

        for (i in 0 until MAX_SIM_STEPS) {
            if (remaining == 0.0) break

            // 抛物线刹车曲线：vmax = baseVelocity + sqrt(2 * a * accelCoef * |remaining|)
            val vmax = s.baseVelocity + sqrt(2.0 * s.acceleration * ACCEL_COEF * abs(remaining))
            val targetV = vmax * sign(remaining) - s.currentVelocity

            // 加速度上限选择
            var accelLimit = s.maxVelocity
            if (sign(remaining) != sign(s.currentVelocity)) {
                accelLimit = if (abs(s.currentVelocity) > LARGE_ROT_THRESHOLD / DT / 3.0) {
                    largeRotSpeed
                } else {
                    REGULAR_SPEED
                }
            }
            accelLimit *= s.accelFactor

            // 反向直接到 —— 等价于 +∞
            if (sign(targetV) != sign(remaining)) {
                accelLimit = Double.MAX_VALUE
            }

            val deltaRaw = targetV.coerceIn(-accelLimit, accelLimit)

            // 半隐式 Euler
            s.currentVelocity += deltaRaw * s.timeStep
            remaining -= s.currentVelocity * s.timeStep
            position += s.currentVelocity * s.timeStep

            // 反向截断：过冲时硬切到终点
            if (sign(remaining) != sign(total)) {
                s.currentVelocity = s.baseVelocity * sign(total)
                remaining = 0.0
                position = total
            }

            out.add(PathPoint(s.currentVelocity, position, deltaRaw))

            // 单 tick 位移钳：超出 1200 则截断并反向
            if (abs(remaining) > OVERSHOOT_CLAMP) {
                remaining = (abs(remaining) - OVERSHOOT_CLAMP) * -sign(remaining)
            }
        }

        return out
    }

    /**
     * 二分配置 axis 的 maxVelocity 和 acceleration，使路径步数逼近 targetSteps
     */
    fun tuneSteps(base: AxisState, targetSteps: Int): AxisState {
        val s = base.copy()
        var low = 0.001
        var high = s.maxVelocity

        for (i in 0 until MAX_BISECT) {
            val mid = 0.5 * (low + high)
            s.maxVelocity = mid
            s.acceleration = mid
            val count = simulate(s).size
            when {
                count < targetSteps -> high = mid
                count > targetSteps -> low = mid
                else -> return s
            }
            if (abs(high - low) < EPS) break
            // 重置 currentVelocity 为 0，避免上次模拟残留
            s.currentVelocity = 0.0
        }

        s.maxVelocity = 0.5 * (low + high)
        s.acceleration = s.maxVelocity
        return s
    }

    /**
     * 单次规划：给定单轴位移 dYaw/dPitch，返回 remainSteps 步后的状态
     */
    fun planSingle(
        dYaw: Double,
        dPitch: Double,
        dt: Double,
        remainSteps: Int
    ): StepResult {
        // dt 的符号用于方向一致性检查，与原版行为等价（仅在 planMulti 调用链中用于方向验证）
        @Suppress("UNUSED_VALUE")
        val actualDt = if (sign(dt) != sign(dYaw) || dt == 0.0) 0.0 else abs(dt)

        val yaw = AxisState(
            accelFactor = 3.0,
            totalRemaining = dYaw,
            acceleration = 3.0,
            baseVelocity = 0.0,
            timeStep = DT,
            currentVelocity = 0.0
        )
        val pitch = AxisState(
            accelFactor = 3.0,
            totalRemaining = dPitch,
            acceleration = 3.0,
            baseVelocity = 0.0,
            timeStep = DT,
            currentVelocity = 0.0
        )

        // 大旋转覆盖（> 22.22 度/tick）
        if (abs(dYaw) > LARGE_ROT_THRESHOLD / DT / 3.0) {
            yaw.maxVelocity = largeRotSpeed
        }
        if (abs(dPitch) > LARGE_ROT_THRESHOLD / DT / 3.0) {
            pitch.maxVelocity = largeRotSpeed
        }

        val tunedYaw = tuneSteps(yaw, remainSteps)
        val tunedPitch = tuneSteps(pitch, remainSteps)

        val pathYaw = simulate(tunedYaw)
        val pathPitch = simulate(tunedPitch)

        val padYaw = pathYaw.toMutableList()
        val padPitch = pathPitch.toMutableList()

        while (padYaw.size < remainSteps) padYaw.add(padYaw.lastOrNull() ?: PathPoint(0.0, 0.0, 0.0))
        while (padPitch.size < remainSteps) padPitch.add(padPitch.lastOrNull() ?: PathPoint(0.0, 0.0, 0.0))

        val selYaw = padYaw[remainSteps - 1]
        val selPitch = padPitch[remainSteps - 1]

        val needResync = pathYaw.size != remainSteps || pathPitch.size != remainSteps

        return StepResult(
            targetPos = Vec2(selYaw.position, selPitch.position),
            velocity = Vec2(selYaw.velocity, selPitch.velocity),
            needResync = needResync
        )
    }

    /**
     * 多次规划：将总位移拆分为 totalSteps 次 planSingle 调用，累积结果
     */
    fun planMulti(
        dYawPitch: Vec2,
        dt: Double,
        totalSteps: Int
    ): StepResult {
        var accumPos = Vec2(0.0, 0.0)
        var accumVel = Vec2(0.0, 0.0)
        var anyResync = false

        for (i in 0 until totalSteps) {
            val remainYaw = dYawPitch.x / DT - accumPos.x
            val remainPitch = dYawPitch.y / DT - accumPos.y
            val step = planSingle(remainYaw, remainPitch, dt, totalSteps - i)
            accumPos += step.targetPos
            accumVel += step.velocity
            anyResync = anyResync || step.needResync
            onStep?.invoke()
        }

        if (anyResync) {
            onResync?.invoke()
            pendingSteps = -1
        }

        return StepResult(targetPos = accumPos, velocity = accumVel, needResync = anyResync)
    }
}