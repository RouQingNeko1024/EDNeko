/*
 * EDNeko Hacked Client
 * Intave 移动检测绕过模块
 * 基于 ac.txt M1-M7 策略:
 *   M1: MotionRecognizer/MLP 绕过
 *   M2: Predictability 绕过
 *   M3: Speed叠加绕过
 *   M4: 水平速度限制绕过 - 利用协议版本差异
 *   M5: 堆叠乘数利用 - 违规移动后插入合法移动
 *   M6: 鞘翅飞行掩盖
 *   M7: 碰撞状态利用
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
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

object IntaveMovementBypass : Module("IntaveMovementBypass", Category.DEV) {

    // M1: MLP 绕过 - 运动模糊
    private val mlpBypass by boolean("MLPBypass", true)
    private val motionNoise by float("MotionNoise", 0.001f, 0f..0.01f) { mlpBypass }
    private val noiseAxis by choices("NoiseAxis", arrayOf("All", "Horizontal", "Vertical", "Random"), "All") { mlpBypass }

    // M2: 可预测性绕过
    private val predictabilityBypass by boolean("PredictabilityBypass", true)
    private val phaseShift by boolean("PhaseShift", true) { predictabilityBypass }
    private val entropyBoost by boolean("EntropyBoost", true) { predictabilityBypass }

    // M4: 速度限制
    private val speedLimitBypass by boolean("SpeedLimitBypass", true)
    private val maxHorizontalSpeed by float("MaxHorizontalSpeed", 0.12f, 0.01f..0.5f) { speedLimitBypass }
    private val useSprintSneak by boolean("SprintSneak1.14+", false) { speedLimitBypass }

    // M5: 堆叠乘数利用
    private val stackMultiplierBypass by boolean("StackMultiplierBypass", true)
    private val maxInvalidMovements by int("MaxInvalidMovements", 7, 1..20) { stackMultiplierBypass }
    private val legalMovementTicks by int("LegalMovementTicks", 2, 1..10) { stackMultiplierBypass }

    // M6: 鞘翅掩盖
    private val elytraMask by boolean("ElytraMask", false)
    private val elytraAbuseFactor by float("ElytraAbuseFactor", 0.6f, 0.1f..1.0f) { elytraMask }

    // M7: 碰撞状态利用
    private val collisionBypass by boolean("CollisionBypass", true)
    private val collisionMultiplier by float("CollisionMultiplier", 3.0f, 1.0f..5.0f) { collisionBypass }

    // 状态
    private var invalidMovementsInRow = 0
    private var legalMovementsCount = 0
    private var lastLegalDistance = 0.0
    private var tickCounter = 0
    private var teleported = false
    private var wasElytraFlying = false

    // 运动历史
    private val motionHistory = mutableListOf<Pair<Double, Double>>() // (horizontal, vertical)

    override fun onEnable() {
        invalidMovementsInRow = 0
        legalMovementsCount = 0
        lastLegalDistance = 0.0
        tickCounter = 0
        teleported = false
        wasElytraFlying = false
        motionHistory.clear()
        chat("§7[IntaveMovement] §aEnabled")
    }

    override fun onDisable() {
        motionHistory.clear()
        chat("§7[IntaveMovement] §cDisabled")
    }

    @Suppress("unused")
    val onTick = handler<GameTickEvent> {
        tickCounter++
        val player = mc.thePlayer ?: return@handler

        if (teleported) {
            teleported = false
            return@handler
        }

        // 计算当前水平速度
        val dx = player.posX - player.prevPosX
        val dz = player.posZ - player.prevPosZ
        val horizontalSpeed = sqrt(dx * dx + dz * dz)
        val verticalSpeed = abs(player.motionY)

        motionHistory.add(horizontalSpeed to verticalSpeed)
        if (motionHistory.size > 20) {
            motionHistory.removeAt(0)
        }

        // M5: 堆叠乘数检测绕过
        if (stackMultiplierBypass && horizontalSpeed > maxHorizontalSpeed.toDouble()) {
            invalidMovementsInRow++
            if (invalidMovementsInRow >= maxInvalidMovements) {
                chat("§7[IntaveMovement] §eStack limit reached, inserting legal moves")
                legalMovementsCount = legalMovementTicks
                invalidMovementsInRow = 0
            }
        } else if (horizontalSpeed <= maxHorizontalSpeed.toDouble()) {
            invalidMovementsInRow = (invalidMovementsInRow * 0.9).toInt().coerceAtLeast(0)
        }

        // M2: 熵/相位转移
        if (predictabilityBypass && phaseShift && tickCounter % 30 == 0) {
            // 每30 tick 做一次小的相位变化
            val shift = Random.nextDouble(-0.02, 0.02)
            player.motionX += shift
            player.motionZ += Random.nextDouble(-0.02, 0.02)
        }
    }

    @Suppress("unused")
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        // M10 (from Timer): Teleport 检测
        if (packet is S08PacketPlayerPosLook) {
            teleported = true
            invalidMovementsInRow = 0
        }

        if (packet is C03PacketPlayer) {
            // M1: 向运动包添加微小噪声
            if (mlpBypass && motionNoise > 0f) {
                val noiseX = when (noiseAxis) {
                    "All", "Horizontal" -> Random.nextDouble(-motionNoise.toDouble(), motionNoise.toDouble())
                    else -> 0.0
                }
                val noiseY = when (noiseAxis) {
                    "All", "Vertical" -> Random.nextDouble(-motionNoise.toDouble(), motionNoise.toDouble())
                    else -> 0.0
                }
                val noiseZ = when (noiseAxis) {
                    "All", "Horizontal" -> Random.nextDouble(-motionNoise.toDouble(), motionNoise.toDouble())
                    else -> 0.0
                }

                // 通过反射添加噪声到位置包
                try {
                    if (packet is C03PacketPlayer.C04PacketPlayerPosition || packet is C03PacketPlayer.C06PacketPlayerPosLook) {
                        val xField = C03PacketPlayer::class.java.getDeclaredField("x")
                        xField.isAccessible = true
                        xField.setDouble(packet, xField.getDouble(packet) + noiseX)

                        val yField = C03PacketPlayer::class.java.getDeclaredField("y")
                        yField.isAccessible = true
                        yField.setDouble(packet, yField.getDouble(packet) + noiseY)

                        val zField = C03PacketPlayer::class.java.getDeclaredField("z")
                        zField.isAccessible = true
                        zField.setDouble(packet, zField.getDouble(packet) + noiseZ)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * M5: 检查移动是否合法 (距离 < maxHorizontalSpeed)
     */
    fun isLegalMovement(horizontalDistance: Double): Boolean {
        if (legalMovementsCount > 0) {
            legalMovementsCount--
            return true
        }
        return horizontalDistance <= maxHorizontalSpeed.toDouble()
    }

    /**
     * M4: 计算当前协议版本下的速度限制
     */
    fun getSpeedLimit(isSneaking: Boolean, isSprinting: Boolean, useV113: Boolean): Float {
        return when {
            isSneaking && isSprinting && useSprintSneak -> 0.4f  // 1.14+ sprint when sneaking
            isSneaking -> 0.12f
            else -> maxHorizontalSpeed
        }
    }

    /**
     * M7: 根据碰撞状态获取乘数
     */
    fun getCollisionMultiplier(hasNoCollisions: Boolean): Float {
        if (!collisionBypass) return 1.0f
        return if (hasNoCollisions) collisionMultiplier else 1.0f
    }
}