package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.entity.EntityLivingBase
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance.Companion.mc

object KillAuraDebug : Module("KillAura-Debug", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val debugMode by boolean("DebugMode", false) { true }
    val debugNoise by boolean("DebugNoise", true) { debugMode }
    val debugRotation by boolean("DebugRotation", true) { debugMode }
    val debugTarget by boolean("DebugTarget", true) { debugMode }
    val debugRange by boolean("DebugRange", true) { debugMode }
    val debugHittable by boolean("DebugHittable", true) { debugMode }
    val debugCPS by boolean("DebugCPS", true) { debugMode }
    val debugAutoBlock by boolean("DebugAutoBlock", true) { debugMode }
    val debugInterval by int("DebugInterval", 20, 1..100, "ticks") { debugMode }

    private var debugTickCounter = 0
    private var lastNoiseDebug: Rotation? = null

    fun onTick(
        target: EntityLivingBase?,
        hittable: Boolean,
        range: Float,
        maxRange: Float,
        cps: IntRange,
        autoBlock: String,
        blockStatus: Boolean,
        renderBlocking: Boolean,
        noiseFunction: ((Rotation) -> Rotation)?
    ) {
        if (!debugMode) return

        debugTickCounter++
        if (debugTickCounter % debugInterval != 0) return

        val sb = StringBuilder()
        sb.append("§8[§bKillAura Debug§8] §7")

        if (debugTarget) {
            target?.let {
                sb.append("§eTarget: §f${it.name} §7(HP: §c${"%.1f".format(it.health)}§7) ")
            } ?: sb.append("§eTarget: §7null ")
        }

        if (debugRange) {
            target?.let {
                val dist = mc.thePlayer?.getDistanceToEntityBox(it) ?: 0f
                sb.append("§eDist: §f${"%.2f".format(dist)} §7| §eRange: §f${"%.2f".format(range)} §7| §eMaxRange: §f${"%.2f".format(maxRange)} ")
            }
        }

        if (debugHittable) {
            sb.append("§eHittable: §${if (hittable) "a" else "c"}$hittable§7 ")
        }

        if (debugCPS) {
            sb.append("§eCPS: §f${cps.first}-${cps.last} ")
        }

        if (debugAutoBlock) {
            sb.append("§eBlock: §f$autoBlock §7| §eBlocking: §${if (blockStatus) "a" else "c"}$blockStatus§7 §7| §eRender: §f$renderBlocking ")
        }

        if (debugRotation) {
            val currentRot = RotationUtils.currentRotation ?: mc.thePlayer?.let { Rotation(it.rotationYaw, it.rotationPitch) }
            val serverRot = RotationUtils.serverRotation
            if (currentRot != null) {
                sb.append("§eYaw: §f${"%.2f".format(currentRot.yaw)} §7| §ePitch: §f${"%.2f".format(currentRot.pitch)} ")
                sb.append("§eServerYaw: §f${"%.2f".format(serverRot.yaw)} §7| §eServerPitch: §f${"%.2f".format(serverRot.pitch)} ")
                sb.append("§eDiff: §f${"%.2f".format(RotationUtils.rotationDifference(currentRot))} ")
            }
        }

        if (debugNoise && noiseFunction != null) {
            val testRot = Rotation(0f, 0f)
            val noiseResult = noiseFunction(testRot)
            if (lastNoiseDebug != noiseResult) {
                lastNoiseDebug = noiseResult
                sb.append("§dNoiseYaw: §f${"%.4f".format(noiseResult.yaw)} §7| §dNoisePitch: §f${"%.4f".format(noiseResult.pitch)} ")
            }
        }

        chat(sb.toString().trimEnd())
    }

    fun onAttack(
        entity: EntityLivingBase,
        noiseResult: Rotation?,
        particles: Int,
        swarmBest: Float,
        annealingTemp: Float,
        geneticBestFitness: Float
    ) {
        if (!debugMode || !debugNoise) return

        val sb = StringBuilder()
        sb.append("§8[§bKillAura Attack§8] §7")
        sb.append("§eEntity: §f${entity.name} ")
        noiseResult?.let {
            sb.append("§dNoise→Yaw: §f${"%.4f".format(it.yaw)} §dPitch: §f${"%.4f".format(it.pitch)} ")
        }
        sb.append("§bParticles: §f$particles ")
        sb.append("§bSwarmBest: §f${"%.2f".format(swarmBest)} ")
        sb.append("§bAnnealTemp: §f${"%.4f".format(annealingTemp)} ")
        sb.append("§bGenFitness: §f${"%.4f".format(geneticBestFitness)} ")
        chat(sb.toString().trimEnd())
    }

    fun reset() {
        debugTickCounter = 0
        lastNoiseDebug = null
    }
}