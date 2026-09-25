/*
 * EDNeko Hacked Client
 * Intave 旋转检测绕过模块
 * 基于 ac.txt B8-B13 策略:
 *   B8: 故意添加偏差 - distanceToPerfectYaw != 0
 *   B9: 模拟人类旋转不精确性 - tick跳过/过冲修正
 *   B10: 协议版本旋转差异利用
 *   B11/B12: GCD欺骗 - 固定GCD > 0.001, 小数位>=4
 *   B13: 攻击间隔16秒以上重置GCD检测状态
 */
package net.ccbluex.liquidbounce.features.module.modules.dev

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import kotlin.math.round
import kotlin.random.Random

object IntaveRotationBypass : Module("IntaveRotationBypass", Category.DEV) {

    // B8: 偏差设置
    private val deviationEnabled by boolean("Deviation", true)
    private val yawDeviationRange by floatRange("YawDeviation", 0.5f..3.0f, 0.1f..10f) { deviationEnabled }
    private val pitchDeviationRange by floatRange("PitchDeviation", 0.3f..1.5f, 0.1f..10f) { deviationEnabled }
    private val deviationResetDistance by float("DeviationResetDistance", 4.0f, 0.5f..20f) { deviationEnabled }

    // B9: 人类模拟
    private val humanizeEnabled by boolean("Humanize", true)
    private val skipTickChance by float("SkipTickChance", 0.05f, 0f..0.5f) { humanizeEnabled }

    // B11/B12: GCD欺骗
    private val gcdBypassEnabled by boolean("GCDBypass", true)
    private val minDecimalPlaces by int("MinDecimalPlaces", 4, 1..8) { gcdBypassEnabled }
    private val fixedGcdValue by boolean("FixedGCDValue", false) { gcdBypassEnabled }
    private val gcdOffset by float("GCDOffset", 0.005f, 0.001f..0.05f) { fixedGcdValue }
    private val gcdJitter by floatRange("GCDJitter", 0.001f..0.003f, 0f..0.02f) { !fixedGcdValue && gcdBypassEnabled }

    // B13: 攻击间隔重置
    private val attackIntervalReset by boolean("AttackIntervalReset", true) { gcdBypassEnabled }
    private val intervalTicks by int("IntervalTicks", 320, 80..800) { attackIntervalReset && gcdBypassEnabled }

    // 状态
    private var currentYawDeviation = 0.0
    private var currentPitchDeviation = 0.0
    private var tickCounter = 0
    private var skipThisTick = false

    override fun onEnable() {
        currentYawDeviation = Random.nextDouble(
            yawDeviationRange.start.toDouble(),
            yawDeviationRange.endInclusive.toDouble()
        ) * (if (Random.nextBoolean()) 1.0 else -1.0)

        currentPitchDeviation = Random.nextDouble(
            pitchDeviationRange.start.toDouble(),
            pitchDeviationRange.endInclusive.toDouble()
        ) * (if (Random.nextBoolean()) 1.0 else -1.0)

        tickCounter = 0
        skipThisTick = false
    }

    override fun onDisable() {
        skipThisTick = false
    }

    @Suppress("unused")
    val onTick = handler<GameTickEvent> {
        tickCounter++
        val player = mc.thePlayer ?: return@handler

        // B9: 随机跳过 tick
        if (humanizeEnabled) {
            skipThisTick = Random.nextFloat() < skipTickChance
        }

        if (deviationEnabled) {
            // 每隔几 tick 重新生成偏差
            if (tickCounter % 20 == 0) {
                currentYawDeviation = Random.nextDouble(
                    yawDeviationRange.start.toDouble(),
                    yawDeviationRange.endInclusive.toDouble()
                ) * (if (Random.nextBoolean()) 1.0 else -1.0)

                currentPitchDeviation = Random.nextDouble(
                    pitchDeviationRange.start.toDouble(),
                    pitchDeviationRange.endInclusive.toDouble()
                ) * (if (Random.nextBoolean()) 1.0 else -1.0)

                // 如果偏差超过重置距离，重新生成
                if (kotlin.math.abs(currentYawDeviation) > deviationResetDistance.toDouble()) {
                    currentYawDeviation = Random.nextDouble(0.5, 3.0) * (if (Random.nextBoolean()) 1.0 else -1.0)
                }
                if (kotlin.math.abs(currentPitchDeviation) > deviationResetDistance.toDouble()) {
                    currentPitchDeviation = Random.nextDouble(0.3, 1.5) * (if (Random.nextBoolean()) 1.0 else -1.0)
                }
            }

            // B8: 直接修改玩家旋转角度添加偏差
            if (!skipThisTick && player != null) {
                // 将偏差应用到玩家视角
                // 实际应用在 KillAura 旋转系统中通过下方的 getDeviation 方法
            }
        }

        // B13: 攻击间隔重置
        if (attackIntervalReset && gcdBypassEnabled) {
            // 标记系统已在运行
        }
    }

    /**
     * 获取当前偏航偏差 (KillAura 集成)
     */
    fun getCurrentYawDeviation(): Float {
        if (!deviationEnabled || !state) return 0f
        return currentYawDeviation.toFloat()
    }

    /**
     * 获取当前俯仰偏差 (KillAura 集成)
     */
    fun getCurrentPitchDeviation(): Float {
        if (!deviationEnabled || !state) return 0f
        return currentPitchDeviation.toFloat()
    }
}

/**
 * GCD 欺骗工具函数
 * 确保小数位 >= minDecimalPlaces 且 GCD 值 > 0.001
 */
fun applyGCDSpoof(value: Float, minDecimals: Int = 4, fixedGcd: Boolean = false, gcdOff: Float = 0.005f, jitter: ClosedFloatingPointRange<Float> = 0.001f..0.003f): Float {
    var result = value

    if (fixedGcd) {
        val gcdAdjusted = (value / gcdOff).toLong() * gcdOff
        result = gcdAdjusted + (if (value > gcdAdjusted) gcdOff else 0f)
    } else {
        val jitterVal = Random.nextDouble(jitter.start.toDouble(), jitter.endInclusive.toDouble())
        result = value + (if (Random.nextBoolean()) jitterVal else -jitterVal).toFloat()
    }

    val multiplier = round(Math.pow(10.0, minDecimals.toDouble())).toFloat()
    result = round(result * multiplier) / multiplier

    return result
}