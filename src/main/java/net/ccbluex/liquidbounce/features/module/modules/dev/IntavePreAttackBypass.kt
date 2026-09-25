/*
 * EDNeko Hacked Client
 * Intave Pre-Attack 检测绕过模块
 * 基于 ac.txt B14-B16 策略:
 *   B14: Pre-Attack模式模拟 - 先swing再attack
 *   B15: 综合玩家行为模拟
 *   B16: MLP对抗样本 - 添加微小噪声干扰神经网络
 */
package net.ccbluex.liquidbounce.features.module.modules.dev

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import kotlin.random.Random

object IntavePreAttackBypass : Module("IntavePreAttackBypass", Category.DEV) {

    // B14: Pre-Attack 模式
    private val preAttackEnabled by boolean("PreAttack", true)
    private val swingFirst by boolean("SwingFirst", true) { preAttackEnabled }
    private val swingDelay by int("SwingDelayMs", 20, 0..200) { swingFirst && preAttackEnabled }

    // B15: 玩家行为模拟
    private val behaviorSimulation by boolean("BehaviorSimulation", true)
    private val fakePrepareAttack by boolean("FakePrepareAttack", true) { behaviorSimulation }
    private val prepareInterval by int("PrepareInterval", 100, 50..500) { fakePrepareAttack && behaviorSimulation }
    private val fakeAttackCount by int("FakeAttackCount", 4, 1..10) { fakePrepareAttack && behaviorSimulation }

    // B16: MLP对抗样本
    private val adversarialNoise by boolean("AdversarialNoise", true)
    private val noiseStrength by float("NoiseStrength", 0.01f, 0.001f..0.1f) { adversarialNoise }

    // 状态
    private var tickCounter = 0
    private var lastPrepareTick = 0
    private var fakeAttacksRemaining = 0

    override fun onEnable() {
        tickCounter = 0
        lastPrepareTick = 0
        fakeAttacksRemaining = 0
        chat("§7[IntavePreAttack] §aEnabled")
    }

    override fun onDisable() {
        chat("§7[IntavePreAttack] §cDisabled")
    }

    @Suppress("unused")
    val onTick = handler<GameTickEvent> {
        tickCounter++
        val player = mc.thePlayer ?: return@handler

        // B15: 模拟假准备攻击
        if (behaviorSimulation && fakePrepareAttack) {
            if (fakeAttacksRemaining > 0) {
                player.swingItem()
                fakeAttacksRemaining--
                return@handler
            }

            if (tickCounter - lastPrepareTick >= prepareInterval && Random.nextInt(100) < 15) {
                fakeAttacksRemaining = fakeAttackCount
                lastPrepareTick = tickCounter
            }
        }
    }

    /**
     * B14: 执行 Pre-Attack (swing + attack 分离)
     */
    fun executePreAttack(target: EntityLivingBase) {
        if (!preAttackEnabled || !swingFirst) {
            mc.thePlayer?.swingItem()
            mc.netHandler?.addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
            return
        }

        // 先发 swing
        mc.netHandler?.addToSendQueue(C0APacketAnimation())

        // 延迟后发 attack
        val delayThread = Thread {
            try {
                Thread.sleep(swingDelay.toLong())
            } catch (_: InterruptedException) {
            }
            mc.netHandler?.addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))
        }
        delayThread.isDaemon = true
        delayThread.start()
    }

    /**
     * B16: 生成对抗样本噪声
     */
    fun generateAdversarialNoise(): Double {
        if (!adversarialNoise) return 0.0
        return Random.nextDouble(-noiseStrength.toDouble(), noiseStrength.toDouble())
    }
}