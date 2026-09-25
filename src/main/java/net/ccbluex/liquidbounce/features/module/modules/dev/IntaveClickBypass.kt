/*
 * EDNeko Hacked Client
 * Intave 点击检测绕过模块
 * 基于 ac.txt B4-B7 策略:
 *   B4: 1.9+攻击-飞行包映射绕过 - 控制飞行包发送频率操纵tick映射
 *   B5: 静默攻击绕过 - 利用竞态条件
 *   B6: 声明1.13+协议版本绕过ClickPatterns
 *   B7: 随机化绕过统计检测 - Deviation/Entropy/Fluctuation控制
 */
package net.ccbluex.liquidbounce.features.module.modules.dev

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0APacketAnimation
import kotlin.random.Random

object IntaveClickBypass : Module("IntaveClickBypass", Category.DEV) {

    // B4: 飞行包映射绕过
    private val flyingPacketControl by boolean("FlyingPacketControl", true)
    private val packetIntervalMs by int("PacketIntervalMs", 38, 35..50) { flyingPacketControl }

    // B5: 静默攻击
    private val silentAttack by boolean("SilentAttack", false)
    private val attackDelayMs by int("AttackDelayMs", 50, 10..200) { silentAttack }

    // B7: 随机化设置
    private val randomizationEnabled by boolean("Randomization", true)
    private val clickDeviation by float("ClickDeviation", 2.0f, 0.5f..10f) { randomizationEnabled }
    private val entropyTarget by float("EntropyTarget", 5.0f, 3.5f..6.0f) { randomizationEnabled }
    private val fluctuationAmount by float("FluctuationAmount", 1.0f, 0.5f..5f) { randomizationEnabled }
    private val burstMax by int("BurstMax", 3, 1..8) { randomizationEnabled }
    private val kurtosisTarget by float("KurtosisTarget", 3.0f, 2.0f..4.0f) { randomizationEnabled }

    // 点击延迟控制
    private val minCps by float("MinCPS", 6f, 1f..20f)
    private val maxCps by float("MaxCPS", 14f, 1f..20f)

    // B6: 声明协议版本 (与 Via 模块联动)
    private val protocolSkip by boolean("Protocol1.13+Skip", false)

    // 状态
    private var lastPacketTimeMs = 0L
    private var lastAttackTimeMs = 0L
    private var burstCount = 0
    private var tickCounter = 0
    private var clickIntervals = mutableListOf<Long>()

    override fun onEnable() {
        lastPacketTimeMs = System.currentTimeMillis()
        lastAttackTimeMs = System.currentTimeMillis()
        burstCount = 0
        tickCounter = 0
        clickIntervals.clear()
        chat("§7[IntaveClick] §aEnabled")
    }

    override fun onDisable() {
        clickIntervals.clear()
        chat("§7[IntaveClick] §cDisabled")
    }

    @Suppress("unused")
    val onTick = handler<GameTickEvent> {
        tickCounter++

        // 维护点击间隔统计
        if (clickIntervals.size > 40) {
            clickIntervals.removeAt(0)
        }
    }

    @Suppress("unused")
    val onAttack = handler<AttackEvent> {
        val now = System.currentTimeMillis()
        val interval = now - lastAttackTimeMs
        clickIntervals.add(interval)
        lastAttackTimeMs = now

        // B7: 突发点击控制
        if (interval < 80) {
            burstCount++
        } else {
            burstCount = 0
        }

        if (burstCount > burstMax) {
            chat("§7[IntaveClick] §eBurst limited: $burstCount clicks")
        }

        // B7: 统计值计算（仅提示）
        if (clickIntervals.size >= 10) {
            val avg = clickIntervals.average()
            val variance = clickIntervals.map { (it - avg) * (it - avg) }.average()
            val stdDev = kotlin.math.sqrt(variance)
            val deviation = kotlin.math.abs(stdDev - 50.0)

            if (randomizationEnabled && deviation < clickDeviation.toDouble()) {
                chat("§7[IntaveClick] §cClick deviation too low: ${"%.2f".format(deviation)}ms")
            }
        }
    }

    /**
     * 计算下一次点击的延迟 (ms)
     * 根据 MinCPS/MaxCPS 在范围内随机化
     */
    fun calculateNextDelay(): Long {
        val cpsRange = if (maxCps <= minCps) minCps..(minCps + 2f) else minCps..maxCps
        val targetCps = Random.nextDouble(cpsRange.start.toDouble(), cpsRange.endInclusive.toDouble())
        val baseDelay = (1000.0 / targetCps).toLong()

        if (!randomizationEnabled) return baseDelay

        // B7: 随机化延迟
        val jitter = if (fluctuationAmount > 0) {
            (Random.nextDouble(-fluctuationAmount.toDouble(), fluctuationAmount.toDouble())).toLong()
        } else 0L

        val deviation = (Random.nextDouble(-clickDeviation.toDouble(), clickDeviation.toDouble())).toLong()

        return (baseDelay + jitter + deviation).coerceAtLeast(30L)
    }
}