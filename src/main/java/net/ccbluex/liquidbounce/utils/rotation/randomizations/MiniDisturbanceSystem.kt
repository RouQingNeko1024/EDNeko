package net.ccbluex.liquidbounce.utils.rotation.randomizations

import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.minecraft.client.Minecraft
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 微扰动系统
 * @author 0x16z
 * @author FireFly_Legit
 */
class MiniDisturbanceSystem {

    private val mc = Minecraft.getMinecraft()

    private var tickCounter = 0
    private lateinit var yawCore: DisturbanceCore
    private lateinit var pitchCore: DisturbanceCore

    /**
     * 主调用方法
     * @param currentRotation 当前要处理的旋转
     * @param lastRotations 历史旋转数组
     * @param size 系统大小
     * @param lowRate 低扰动率
     * @param highRate 高扰动率
     * @param interval 扰动间隔（tick数）
     */
    fun applyDisturbanceToRotation(
        currentRotation: Rotation,
        lastRotations: Array<Rotation>,
        size: Int = 30,
        lowRate: Float = 0.1f,
        highRate: Float = 0.3f,
        interval: Int = 3
    ): Rotation {
        ensureInitialized(size, lowRate, highRate)

        val playerMoving = mc.thePlayer?.isMoving ?: false
        val enemyMoving = calculateEnemyMoving()

        val disturbedRotation = currentRotation.copy()

        tickCounter++


        if (tickCounter >= interval) {
            tickCounter = 0
            executeSmartDisturbance(lastRotations)
        }

        applyDisturbanceOffsets(disturbedRotation, lastRotations, playerMoving, enemyMoving)

        return disturbedRotation
    }

    /**
     * 重置扰动系统（如切换目标时调用）
     */
    fun reset() {
        tickCounter = 0
        if (::yawCore.isInitialized) {
            yawCore.reset()
            pitchCore.reset()
        }
    }


    private fun ensureInitialized(size: Int, lowRate: Float, highRate: Float) {
        if (!::yawCore.isInitialized || yawCore.size != size) {
            yawCore = DisturbanceCore(lowRate, highRate, size)
            pitchCore = DisturbanceCore(lowRate, highRate, size)
        } else {
            yawCore.updateRates(lowRate, highRate)
            pitchCore.updateRates(lowRate, highRate)
        }
    }

    private fun calculateEnemyMoving(): Boolean {
        val target = mc.pointedEntity ?: return false
        return sqrt(target.motionX * target.motionX + target.motionZ * target.motionZ) > 0.01
    }

    private fun executeSmartDisturbance(lastRotations: Array<Rotation>) {
        if (lastRotations.size < 2) return

        val lastYawSpeed = calculateAngleSpeed(lastRotations[0].yaw, lastRotations[1].yaw)
        val lastPitchSpeed = abs(lastRotations[0].pitch - lastRotations[1].pitch)

        yawCore.disturb(lastYawSpeed)
        pitchCore.disturb(lastPitchSpeed)
    }

    private fun applyDisturbanceOffsets(
        rotation: Rotation,
        lastRotations: Array<Rotation>,
        playerMoving: Boolean,
        enemyMoving: Boolean
    ) {
        if (lastRotations.isEmpty()) return

        val lastRotation = lastRotations[0]
        val yawDiff = abs(rotation.yaw - lastRotation.yaw)
        val pitchDiff = abs(rotation.pitch - lastRotation.pitch)

        rotation.yaw += yawCore.getDisturbance(yawDiff, playerMoving, enemyMoving)
        rotation.pitch += pitchCore.getDisturbance(pitchDiff, playerMoving, enemyMoving)
    }

    private fun calculateAngleSpeed(current: Float, last: Float): Float {
        var diff = current - last
        diff = ((diff + 180) % 360) - 180
        return abs(diff)
    }

    /**
     * 扰动核心类
     */
    private class DisturbanceCore(
        private var lowRate: Float,
        private var highRate: Float,
        val size: Int
    ) {
        private val dataArray = BooleanArray(size) { false }

        fun updateRates(newLowRate: Float, newHighRate: Float) {
            this.lowRate = newLowRate
            this.highRate = newHighRate
        }

        fun disturb(speed: Float) {
            val at = (speed.toInt() % size).coerceIn(0, size - 1)

            // 向左传播
            for (i in at downTo 0) {
                if (Math.random() < lowRate) {
                    dataArray[i] = !dataArray[i]
                } else {
                    break
                }
            }

            // 向右传播
            for (i in at until size) {
                if (Math.random() < highRate) {
                    dataArray[i] = !dataArray[i]
                } else {
                    break
                }
            }
        }

        fun getDisturbance(angleDiff: Float, playerMoving: Boolean, enemyMoving: Boolean): Float {
            val at = (angleDiff.toInt() % size).coerceIn(0, size - 1)
            var result = 0.0

            // 向左累加
            for (i in at downTo 0) {
                val weight = lowRate.toDouble().pow((at - i).toDouble())
                result += (if (dataArray[i]) 1.0 else 0.0) * weight
            }

            // 向右累加（带负号）
            for (i in at until size) {
                val weight = highRate.toDouble().pow((i - at).toDouble())
                result -= (if (dataArray[i]) 1.0 else 0.0) * weight
            }

            var d = result.toFloat()

            // 特殊处理：玩家和敌人都静止
            if (!playerMoving && !enemyMoving && d != -1f) {
                d = d / (d + 1)
            }

            return d
        }

        fun reset() {
            for (i in dataArray.indices) {
                dataArray[i] = false
            }
        }
    }

    /**
     * 扰动设置数据类
     */
    data class DisturbanceSettings(
        val enabled: Boolean,
        val size: Int,
        val lowRate: Float,
        val highRate: Float,
        val interval: Int
    )
}