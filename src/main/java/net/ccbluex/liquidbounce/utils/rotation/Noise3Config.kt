package net.ccbluex.liquidbounce.utils.rotation

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.rotation.noise3.Noise3Calculator
import net.ccbluex.liquidbounce.utils.rotation.noise3.PlayerHandle
import net.ccbluex.liquidbounce.utils.rotation.noise3.TargetHandle
import net.minecraft.client.Minecraft

/**
 * Noise3 独立随机化模式：面向计算器入口 [Noise3Calculator.compute] 提供
 * 跨 tick 的持久化实例、玩家句柄适配与配置注入。
 */
object Noise3Rotation {

    private val mc = Minecraft.getMinecraft()
    private val calculator = Noise3Calculator()
    private val targetHandle = object : TargetHandle {}

    private val playerHandle: PlayerHandle = object : PlayerHandle {
        override fun currentRotation(): Rotation = Rotation(
            mc.thePlayer?.rotationYaw ?: 0f,
            mc.thePlayer?.rotationPitch ?: 0f
        )

        override fun getHurtTime(): Int = mc.thePlayer?.hurtTime ?: 0
    }

    private fun applyConfig(config: Noise3Config) {
        calculator.pointSelectionMode = config.pointSelectionMode.get()
        calculator.aimSpeed = config.aimSpeed
        calculator.speedAtLargeRot = config.speedAtLargeRot
        calculator.speedAtInitialRot = config.speedAtInitialRot
        calculator.largeRotThreshold = config.largeRotThreshold
        calculator.resyncChance = config.resyncChance
        calculator.resyncUpdateDelay = config.resyncUpdateDelay
        calculator.removeNoiseChance = config.removeNoiseChance
        calculator.noNoiseRecalcDelay = config.noNoiseRecalcDelay
    }

    fun process(current: Rotation, config: Noise3Config): Rotation {
        applyConfig(config)
        return calculator.compute(playerHandle, targetHandle, current, current)
    }
}

class Noise3Config(owner: Module, val generalApply: () -> Boolean = { true }) : Configurable("Noise3") {

    val pointSelectionMode = choices("Mode", arrayOf("Prediction", "Direct"), "Prediction") { generalApply() }
    val aimSpeed by float("AimSpeed", 15f, 0f..100f) { generalApply() }
    val speedAtLargeRot by float("SpeedAtLargeRot", 30f, 0f..100f) { generalApply() }
    val speedAtInitialRot by float("SpeedAtInitialRot", 30f, 0f..100f) { generalApply() }
    val largeRotThreshold by float("LargeRotThreshold", 30f, 0f..180f) { generalApply() }
    val resyncChance by float("ResyncChance", 0f, 0f..100f) { generalApply() }
    val resyncUpdateDelay by float("ResyncUpdateDelay", 1f, 0f..100f) { generalApply() }
    val removeNoiseChance by float("RemoveNoiseChance", 0f, 0f..100f) { generalApply() }
    val noNoiseRecalcDelay by float("NoNoiseRecalcDelay", 1f, 0f..100f) { generalApply() }
}