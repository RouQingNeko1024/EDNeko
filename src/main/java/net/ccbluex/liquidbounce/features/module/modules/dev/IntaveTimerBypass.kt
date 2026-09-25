/*
 * EDNeko Hacked Client
 * Intave Timer/频率检测绕过模块
 * 基于 ac.txt M8-M11 策略:
 *   M8: 频率控制 - 保持时间差在35-39ms(刚好低于40ms阈值)
 *   M9: Blink检测绕过 - 每3秒发送移动包
 *   M10: Teleport状态利用
 *   M11: 攻击取消规避 - 进攻间隔>600ms
 */
package net.ccbluex.liquidbounce.features.module.modules.dev

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import kotlin.random.Random

object IntaveTimerBypass : Module("IntaveTimerBypass", Category.DEV) {

    // M8: 频率控制
    private val freqControl by boolean("FrequencyControl", true)
    private val targetIntervalMs by int("TargetIntervalMs", 38, 35..50) { freqControl }
    private val intervalJitter by float("IntervalJitter", 2.0f, 0.5f..10f) { freqControl }

    // 发包率控制
    private val maxPacketsPerTick by int("MaxPacketsPerTick", 1, 1..5) { freqControl }
    private val burstProtection by boolean("BurstProtection", true) { freqControl }

    // M9: Blink 检测绕过
    private val blinkBypass by boolean("BlinkBypass", true)
    private val blinkIntervalTicks by int("BlinkIntervalTicks", 60, 30..200) { blinkBypass }

    // M10: Teleport 利用
    private val teleportReset by boolean("TeleportReset", true)

    // M11: 攻击取消规避
    private val attackCancelBypass by boolean("AttackCancelBypass", true)
    private val minAttackIntervalMs by int("MinAttackIntervalMs", 650, 600..2000) { attackCancelBypass }

    // 状态
    private var lastPacketSentMs = 0L
    private var lastBlinkPacketTick = 0
    private var teleportedRecently = false
    private var tickCounter = 0
    private var packetsThisTick = 0
    private var lastAttackMs = 0L

    override fun onEnable() {
        lastPacketSentMs = System.currentTimeMillis()
        lastBlinkPacketTick = 0
        teleportedRecently = false
        tickCounter = 0
        packetsThisTick = 0
        lastAttackMs = 0L
        chat("§7[IntaveTimer] §aEnabled")
    }

    override fun onDisable() {
        chat("§7[IntaveTimer] §cDisabled")
    }

    @Suppress("unused")
    val onTick = handler<GameTickEvent> {
        tickCounter++
        packetsThisTick = 0
    }

    @Suppress("unused")
    val onPacket = handler<PacketEvent> { event ->
        // M9: Blink 检测绕过 - 每 blinkIntervalTicks 发送一个移动包
        if (blinkBypass && tickCounter - lastBlinkPacketTick >= blinkIntervalTicks) {
            if (mc.thePlayer != null) {
                val player = mc.thePlayer
                // 必须先更新计数器，再发送包，防止 addToSendQueue 触发 PacketEvent 递归
                lastBlinkPacketTick = tickCounter
                // 发送一个假移动包保持连接
                val fakePacket = C03PacketPlayer.C04PacketPlayerPosition(
                    player.posX, player.posY, player.posZ, player.onGround
                )
                mc.netHandler?.addToSendQueue(fakePacket)
            }
        }

        // M10: Teleport 状态检测
        if (teleportReset && event.packet is S08PacketPlayerPosLook) {
            teleportedRecently = true
            chat("§7[IntaveTimer] §aTeleport detected, resetting timer")
        }

        // 发送包频率统计
        val packet = event.packet
        if (packet is C03PacketPlayer) {
            val now = System.nanoTime()
            val diffMs = (now - lastPacketSentMs) / 1_000_000

            packetsThisTick++
            if (freqControl && packetsThisTick > maxPacketsPerTick && burstProtection) {
                event.cancelEvent()
                chat("§7[IntaveTimer] §eBurst packet cancelled")
                return@handler
            }

            if (freqControl && diffMs < targetIntervalMs - intervalJitter) {
                // 等几毫秒再发
                try {
                    Thread.sleep(1)
                } catch (_: InterruptedException) {
                }
            }

            lastPacketSentMs = now
        }
    }

    /**
     * M8: 计算合适的发包间隔
     */
    fun getOptimalInterval(): Long {
        val base = targetIntervalMs.toLong()
        val jitter = Random.nextLong(
            -(intervalJitter.toLong()),
            (intervalJitter.toLong() + 1)
        )
        return (base + jitter).coerceIn(35L, 50L)
    }

    /**
     * M11: 检查是否可以攻击 (规避攻击取消)
     */
    fun canAttack(): Boolean {
        if (!attackCancelBypass) return true
        val now = System.currentTimeMillis()
        if (now - lastAttackMs < minAttackIntervalMs) return false
        lastAttackMs = now
        return true
    }
}