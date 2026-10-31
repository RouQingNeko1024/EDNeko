package net.ccbluex.liquidbounce.utils.rotation.noise3

import net.ccbluex.liquidbounce.utils.rotation.Rotation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.random.Random

interface PlayerHandle {
    fun currentRotation(): Rotation
    fun getHurtTime(): Int
}

interface TargetHandle

class Noise3Calculator {

    var pointSelectionMode: String = "Prediction"

    var aimSpeed: Float = 15f
    var decelPre: Float = 1f
    var decelPost: Float = 1f
    var decelPostHighspeed: Float = 2f
    var highspeedThreshold: Float = 10f
    var speedAtLargeRot: Float = 30f
    var speedAtInitialRot: Float = 30f
    var largeRotThreshold: Float = 30f
    var resyncChance: Float = 0f
    var resyncUpdateDelay: Float = 1f
    var removeNoiseChance: Float = 0f
    var noNoiseRecalcDelay: Float = 1f
    var resyncDelayMs: Float = 0f
    var resyncRateMin: Float = 0.4f
    var resyncRateMax: Float = 0.6f

    private var actualYaw: Double = 0.0
    private var actualPitch: Double = 0.0
    private var targetYaw: Double = 0.0
    private var targetPitch: Double = 0.0
    private var firstTime: Boolean = true
    private var resyncCounter: Int = 0
    private var noNoiseCounter: Int = 0
    private var enablePrediction: Boolean = true
    private var removeNoise: Boolean = false
    private var lastTarget: TargetHandle? = null
    private var lastHurtTime: Int = -1

    val planner: C40N = C40N()

    private val random: Random = Random.Default

    /**
     * 主计算入口，每 tick 调用一次
     * @param player 玩家句柄
     * @param target 目标句柄
     * @param aim1 瞄准计算1
     * @param aim2 瞄准计算2
     * @return 本 tick 应设置的玩家 rotation
     */
    fun compute(
        player: PlayerHandle,
        target: TargetHandle?,
        aim1: Rotation,
        aim2: Rotation
    ): Rotation {
        val current = player.currentRotation()
        val curYaw = wrapAngle(current.yaw.toDouble())
        val curPitch = current.pitch.toDouble()

        // 首次调用时初始化所有状态，以玩家当前朝向为基准
        if (firstTime) {
            actualYaw = curYaw
            actualPitch = curPitch
            targetYaw = curYaw
            targetPitch = curPitch
            firstTime = false
            return current
        }

        // 触发 resync 的条件：偏差过大 / 目标变化 / 玩家受伤
        val needResync = abs(curYaw - targetYaw) > 1.0 ||
                abs(actualPitch - curPitch) > 1.0 ||
                lastTarget != target ||
                lastHurtTime != player.getHurtTime()

        if (needResync) {
            actualYaw = curYaw
            actualPitch = curPitch
            targetYaw = curYaw
            targetPitch = curPitch
        }
        lastTarget = target
        lastHurtTime = player.getHurtTime()

        // resync 计数器：控制是否启用预测
        if (resyncCounter == 0) {
            enablePrediction = random.nextFloat() - resyncChance / 100.0 > 0
            resyncCounter = max(1, resyncUpdateDelay.toInt())
        }
        if (resyncCounter > 0) resyncCounter--

        // 噪声移除计数器
        if (noNoiseCounter == 0) {
            removeNoise = random.nextFloat() - removeNoiseChance / 100.0 > 0
            noNoiseCounter = max(1, noNoiseRecalcDelay.toInt())
        }
        if (noNoiseCounter > 0) noNoiseCounter--

        // 目标旋转：Prediction 模式取两个瞄准点的均值
        val targetRot = if (pointSelectionMode == "Prediction") {
            Rotation(
                (aim1.yaw + aim2.yaw) / 2f,
                (aim1.pitch + aim2.pitch) / 2f
            )
        } else {
            aim1
        }

        val dYaw = wrapAngle((targetRot.yaw - targetYaw).toDouble())
        val dPitch = targetRot.pitch - targetPitch
        val dist = sqrt(dYaw * dYaw + dPitch * dPitch)

        // 根据与目标的距离选择大旋转速度或常规速度
        planner.largeRotSpeed = if (dist > largeRotThreshold) {
            speedAtLargeRot.toDouble()
        } else {
            speedAtInitialRot.toDouble()
        }

        // planMulti 固定使用 3 步
        val step = planner.planMulti(
            dYawPitch = Vec2(dYaw, dPitch),
            dt = DT,
            totalSteps = 3
        )

        // aimSpeed 转换为速度乘数 —— 经验魔法数，保持原值
        val speedFactor = aimSpeed / 100.0 * 8.0 + 2.0   // 15 → 3.2

        val dy = (step.velocity.x * speedFactor * DT)
            .coerceIn(-speedAtLargeRot * DT, speedAtLargeRot * DT)
        val dp = (step.velocity.y * speedFactor * DT)
            .coerceIn(-speedAtInitialRot * DT, speedAtInitialRot * DT)

        actualYaw = wrapAngle(actualYaw + dy)
        actualPitch = (actualPitch + dp).coerceIn(-90.0, 90.0)
        targetYaw = wrapAngle(targetYaw + dy)
        targetPitch = (targetPitch + dp).coerceIn(-90.0, 90.0)

        return Rotation(targetYaw.toFloat(), targetPitch.toFloat())
    }

    companion object {
        fun wrapAngle(a: Double): Double {
            var result = a % 360.0
            if (result >= 180.0) result -= 360.0
            if (result < -180.0) result += 360.0
            return result
        }
    }
}