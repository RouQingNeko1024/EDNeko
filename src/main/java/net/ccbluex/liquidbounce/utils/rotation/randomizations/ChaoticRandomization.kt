package net.ccbluex.liquidbounce.utils.rotation.randomizations

import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextBoolean
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import java.util.*
import kotlin.math.*

class ChaoticRandomization(private val settings: RandomizationSettings) {
    private val mc = Minecraft.getMinecraft()!!

    private var lorenzX = 0.1f
    private var lorenzY = 0.0f
    private var lorenzZ = 0.0f
    private var rosslerX = 0.1f
    private var rosslerY = 0.0f
    private var rosslerZ = 0.0f
    private var henonX = 0.1f
    private var henonY = 0.0f
    private var doublePendulumTheta1 = PI.toFloat() / 2
    private var doublePendulumTheta2 = PI.toFloat() / 2
    private var doublePendulumOmega1 = 0.0f
    private var doublePendulumOmega2 = 0.0f
    private var lastChaoticTime = System.currentTimeMillis()
    private var chaoticHistory = ArrayDeque<FloatArray>(100)

    private var customChaosX = 0.1f
    private var customChaosY = 0.0f
    private var customChaosZ = 0.0f
    private var switchSystemState = 0f
    private var switchDirection = 1f
    private var currentSystemIndex = 0
    private var systemTransitionProgress = 0f
    private var previousSystem = "Lorenz"

    private var lorenzSigma = 10f
    private var lorenzRho = 28f
    private var lorenzBeta = 8f / 3f
    private var rosslerA = 0.2f
    private var rosslerB = 0.2f
    private var rosslerC = 5.7f

    private var lastHumanBehaviorTime = System.currentTimeMillis()
    private var currentBehaviorPattern = "normal"
    private var behaviorTransitionProgress = 0f
    private val behaviorPatterns = listOf("normal", "aggressive", "precise", "erratic", "fatigued")
    private var nextPatternChangeTime = System.currentTimeMillis() + (30000L..120000L).random()
    private var lastResetTime = System.currentTimeMillis()
    private var combatStartTime = System.currentTimeMillis()
    private var recentHitRate = 0.5f

    fun getEnhancedChaoticOffset(axis: Int): Float {
        if (!settings.chaoticAntiML) {
            return getTrueRandomChaoticOffset(axis)
        }

        val baseOffset = getTrueRandomChaoticOffset(axis)
        var enhancedOffset = baseOffset

        enhancedOffset = applyPatternRandomization(enhancedOffset, axis)
        enhancedOffset = applyTemporalNoise(enhancedOffset, axis)

        if (settings.chaoticBehaviorMimicry) {
            enhancedOffset = applyBehaviorMimicry(enhancedOffset, axis)
        }

        if (settings.chaoticHumanLikeTransitions) {
            enhancedOffset = applyHumanLikeTransitions(enhancedOffset, axis)
        }

        enhancedOffset = applyDynamicSystemSwitching(enhancedOffset, axis)

        return enhancedOffset.coerceIn(-25f, 25f)
    }

    private fun applyPatternRandomization(offset: Float, axis: Int): Float {
        if (nextFloat() < settings.chaoticPatternRandomization.random()) {
            val axisFactor = when (axis) {
                0 -> 1.0f
                1 -> 0.7f
                else -> 1.0f
            }
            return offset * (0.5f + nextFloat() * 1.5f) * axisFactor
        }
        return offset
    }

    private fun applyTemporalNoise(offset: Float, axis: Int): Float {
        val axisNoiseFactor = when (axis) {
            0 -> 1.0f
            1 -> 0.6f
            else -> 1.0f
        }
        val timeNoise = gaussianRandom(0f, settings.chaoticTemporalNoise.random() * axisNoiseFactor)
        return offset * (1f + timeNoise * 0.1f)
    }

    private fun applyBehaviorMimicry(offset: Float, axis: Int): Float {
        updateBehaviorPattern()

        val baseMultiplier = when (currentBehaviorPattern) {
            "aggressive" -> 1.3f
            "precise" -> 0.7f
            "erratic" -> (0.5f + nextFloat() * 1.5f)
            "fatigued" -> 0.8f
            else -> 1.0f
        }

        val axisBehaviorFactor = when (axis) {
            0 -> 1.0f
            1 -> 0.8f
            else -> 1.0f
        }

        val adjustedMultiplier = if (currentBehaviorPattern == "fatigued") {
            baseMultiplier + gaussianRandom(0f, 0.5f) * axisBehaviorFactor
        } else {
            baseMultiplier * axisBehaviorFactor
        }

        return offset * adjustedMultiplier
    }

    private fun applyHumanLikeTransitions(offset: Float, axis: Int): Float {
        val reactionTime = when (axis) {
            0 -> 0.001f
            1 -> 0.002f
            else -> 0.001f
        }
        val reactionVariation = sin(System.currentTimeMillis() * reactionTime) * 0.2f

        val axisAmplitude = if (axis == 1) 0.15f else 0.2f
        return offset * (1f + reactionVariation * axisAmplitude)
    }

    private fun applyDynamicSystemSwitching(offset: Float, axis: Int): Float {
        if (settings.chaoticDynamicMixing && nextFloat() < settings.chaoticAutoSwitch.random()) {
            rotateChaoticSystem()
        }

        return if (settings.chaoticDynamicMixing) {
            getDynamicMixedOffset(axis)
        } else {
            offset
        }
    }

    private fun updateBehaviorPattern() {
        val currentTime = System.currentTimeMillis()
        val behaviorRandom = kotlin.random.Random

        if (currentTime >= nextPatternChangeTime) {
            val newPattern = behaviorPatterns.random(behaviorRandom)
            if (newPattern != currentBehaviorPattern) {
                currentBehaviorPattern = newPattern
                behaviorTransitionProgress = 0f
                lastHumanBehaviorTime = currentTime
                nextPatternChangeTime = currentTime + (30000L..120000L).random(behaviorRandom)
            }
        }

        val timeSinceLastChange = if (lastHumanBehaviorTime != 0L) {
            currentTime - lastHumanBehaviorTime
        } else {
            0L
        }
        behaviorTransitionProgress = (timeSinceLastChange / 5000f).coerceIn(0f, 1f)
    }

    private fun rotateChaoticSystem() {
        val availableSystems = listOf("Lorenz", "Rossler", "Henon", "DoublePendulum", "MultiChaotic", "Switch")
        previousSystem = settings.chaoticSystem
        currentSystemIndex = (currentSystemIndex + 1) % availableSystems.size
        settings.chaoticSystem = availableSystems[currentSystemIndex]
        systemTransitionProgress = 0f
    }

    private fun getDynamicMixedOffset(axis: Int): Float {
        val weights = if (settings.chaoticMixWeights) calculateAdaptiveWeights() else floatArrayOf(0.2f, 0.2f, 0.15f, 0.15f, 0.2f, 0.1f)

        return getLorenzOffset(axis) * weights[0] +
                getRosslerOffset(axis) * weights[1] +
                getHenonOffset(axis) * weights[2] +
                getPendulumOffset(axis) * weights[3] +
                getCustomChaoticOffset(axis) * weights[4] +
                getSwitchOffset(axis) * weights[5]
    }

    private fun calculateAdaptiveWeights(): FloatArray {
        val player = mc.thePlayer
        val target = mc.pointedEntity

        return when {
            player == null -> floatArrayOf(0.17f, 0.17f, 0.16f, 0.16f, 0.17f, 0.17f)
            target == null -> floatArrayOf(0.2f, 0.2f, 0.15f, 0.15f, 0.2f, 0.1f)
            player.getDistanceToEntity(target) < 3f -> floatArrayOf(0.3f, 0.2f, 0.1f, 0.1f, 0.2f, 0.1f)
            player.getDistanceToEntity(target) > 15f -> floatArrayOf(0.15f, 0.25f, 0.2f, 0.15f, 0.15f, 0.1f)
            else -> floatArrayOf(0.2f, 0.2f, 0.15f, 0.15f, 0.2f, 0.1f)
        }
    }

    private fun getTrueRandomChaoticOffset(axis: Int): Float {
        val chaoticPart = getChaoticOffset(axis)

        val randomPart = when (axis) {
            0 -> gaussianRandom(0f, settings.chaoticIntensity.random() * 0.5f)
            1 -> gaussianRandom(0f, settings.chaoticIntensity.random() * 0.3f)
            else -> 0f
        }

        val jumpPart = if (nextFloat() < 0.02f) {
            gaussianRandom(0f, settings.chaoticJumpIntensity.random())
        } else {
            0f
        }

        val mixRatio = settings.chaoticNoiseMix.random()

        return chaoticPart * (1 - mixRatio) +
                (randomPart + jumpPart) * mixRatio
    }

    private fun getChaoticOffset(axis: Int): Float {
        val currentTime = System.currentTimeMillis()
        val dt = (currentTime - lastChaoticTime) / 1000f
        lastChaoticTime = currentTime

        updateChaoticSystems(dt.coerceIn(0.001f, 0.1f))

        val baseOffset = when (settings.chaoticSystem) {
            "Lorenz" -> getLorenzOffset(axis)
            "Rossler" -> getRosslerOffset(axis)
            "Henon" -> getHenonOffset(axis)
            "DoublePendulum" -> getPendulumOffset(axis)
            "MultiChaotic" -> getMultiChaoticOffset(axis)
            "Switch" -> getSwitchOffset(axis)
            else -> 0f
        }

        var offset = baseOffset * settings.chaoticIntensity.random() * 10f

        if (axis == 1 && nextFloat() < settings.chaoticCoupling.random()) {
            offset *= 0.3f
        }

        if (settings.chaoticAdaptive) {
            offset = applyAdaptiveChaos(offset, axis)
        }

        return offset.coerceIn(-25f, 25f)
    }

    private fun getLorenzOffset(axis: Int): Float {
        return when (axis) {
            0 -> lorenzX * 0.1f
            1 -> lorenzY * 0.05f
            else -> 0f
        }
    }

    private fun getRosslerOffset(axis: Int): Float {
        return when (axis) {
            0 -> rosslerX * 0.2f
            1 -> rosslerY * 0.1f
            else -> 0f
        }
    }

    private fun getHenonOffset(axis: Int): Float {
        return when (axis) {
            0 -> henonX * 0.5f
            1 -> henonY * 0.3f
            else -> 0f
        }
    }

    private fun getPendulumOffset(axis: Int): Float {
        val angle = when (axis) {
            0 -> doublePendulumTheta1
            1 -> doublePendulumTheta2
            else -> 0f
        }
        return (angle % (2 * PI.toFloat())) * 2f
    }

    private fun getMultiChaoticOffset(axis: Int): Float {
        val lorenzPart = getLorenzOffset(axis) * 0.3f
        val rosslerPart = getRosslerOffset(axis) * 0.25f
        val henonPart = getHenonOffset(axis) * 0.2f
        val pendulumPart = getPendulumOffset(axis) * 0.15f
        val customPart = getCustomChaoticOffset(axis) * 0.1f

        return lorenzPart + rosslerPart + henonPart + pendulumPart + customPart
    }

    private fun getSwitchOffset(axis: Int): Float {
        return switchSystemState * when (axis) {
            0 -> 3f
            1 -> 1.5f
            else -> 0f
        }
    }

    private fun getCustomChaoticOffset(axis: Int): Float {
        return when (axis) {
            0 -> (customChaosX * 0.8f + sin(customChaosY) * 0.2f) * 2f
            1 -> (customChaosY * 0.6f + cos(customChaosZ) * 0.4f) * 1.5f
            else -> 0f
        }
    }

    private fun applyAdaptiveChaos(offset: Float, axis: Int): Float {
        val player = mc.thePlayer ?: return offset

        val (baseSensitivity, stability) = when (axis) {
            0 -> Pair(1.0f, 0.8f)
            1 -> Pair(0.7f, 0.9f)
            else -> Pair(1.0f, 0.8f)
        }

        val stateFactor = when {
            player.isSneaking -> 0.4f * stability
            player.isSprinting -> 1.3f * (1.1f - stability)
            player.isMoving -> 0.9f
            else -> 0.6f * stability
        }

        val target = mc.pointedEntity
        val distance = target?.let { player.getDistanceToEntity(it) } ?: 10f
        val distanceFactor = when {
            distance < 3f -> 1.6f - (stability * 0.3f)
            distance > 15f -> 0.5f + (stability * 0.3f)
            else -> 1.0f
        }

        return offset * baseSensitivity * stateFactor * distanceFactor
    }

    private fun updateChaoticSystems(dt: Float) {
        val effectiveDt = dt * settings.chaoticSpeed.random()

        maybeResetChaoticSystem()

        when (settings.chaoticSystem) {
            "Lorenz" -> updateLorenz(effectiveDt)
            "Rossler" -> updateRossler(effectiveDt)
            "Henon" -> updateHenon()
            "DoublePendulum" -> updateDoublePendulum(effectiveDt)
            "MultiChaotic" -> updateMultiChaotic(effectiveDt)
            "Switch" -> updateSwitchSystem(effectiveDt)
        }

        updateAllChaoticSystems(effectiveDt)
        evolveChaoticParameters()

        if (nextFloat() < 0.1f) {
            // 这里可能需要访问 RandomizationSettings 中的 noiseTime
        }

        if (settings.chaoticMemory) {
            saveChaoticHistory()
        }
    }

    private fun updateAllChaoticSystems(dt: Float) {
        updateLorenz(dt * 0.5f)
        updateRossler(dt * 0.6f)
        updateHenon()
        updateDoublePendulum(dt * 0.8f)
        updateCustomChaoticSystem(dt * 0.7f)
    }

    private fun updateLorenz(dt: Float) {
        val dx = (lorenzSigma * (lorenzY - lorenzX)) * dt
        val dy = (lorenzX * (lorenzRho - lorenzZ) - lorenzY) * dt
        val dz = (lorenzX * lorenzY - lorenzBeta * lorenzZ) * dt

        val forceRandomness = settings.chaoticParamRandomness.random() * 0.1f
        lorenzX += dx + gaussianRandom(0f, 0.01f) * forceRandomness
        lorenzY += dy + gaussianRandom(0f, 0.01f) * forceRandomness
        lorenzZ += dz + gaussianRandom(0f, 0.005f) * forceRandomness
    }

    private fun updateRossler(dt: Float) {
        val dx = (-rosslerY - rosslerZ) * dt
        val dy = (rosslerX + rosslerA * rosslerY) * dt
        val dz = (rosslerB + rosslerZ * (rosslerX - rosslerC)) * dt

        val forceRandomness = settings.chaoticParamRandomness.random() * 0.1f
        rosslerX += dx + gaussianRandom(0f, 0.01f) * forceRandomness
        rosslerY += dy + gaussianRandom(0f, 0.01f) * forceRandomness
        rosslerZ += dz + gaussianRandom(0f, 0.005f) * forceRandomness
    }

    private fun updateHenon() {
        val newX = 1 - rosslerA * henonX * henonX + henonY
        val newY = rosslerB * henonX

        val noise = settings.chaoticParamRandomness.random() * 0.05f
        henonX = newX + gaussianRandom(0f, noise)
        henonY = newY + gaussianRandom(0f, noise * 0.5f)
    }

    private fun updateDoublePendulum(dt: Float) {
        val num1 = -9.8f * (2 * 1.0f + 1.0f) * sin(doublePendulumTheta1)
        val num2 = -1.0f * 9.8f * sin(doublePendulumTheta1 - 2 * doublePendulumTheta2)
        val num3 = -2 * sin(doublePendulumTheta1 - doublePendulumTheta2) * 1.0f
        val num4 = doublePendulumOmega2 * doublePendulumOmega2 * 1.0f + doublePendulumOmega1 * doublePendulumOmega1 * 1.0f * cos(doublePendulumTheta1 - doublePendulumTheta2)
        val den = 1.0f * (2 * 1.0f + 1.0f - 1.0f * cos(2 * doublePendulumTheta1 - 2 * doublePendulumTheta2))

        val alpha1 = (num1 + num2 + num3 * num4) / den

        val num5 = 2 * sin(doublePendulumTheta1 - doublePendulumTheta2)
        val num6 = doublePendulumOmega1 * doublePendulumOmega1 * 1.0f * (1.0f + 1.0f)
        val num7 = 9.8f * (1.0f + 1.0f) * cos(doublePendulumTheta1)
        val num8 = doublePendulumOmega2 * doublePendulumOmega2 * 1.0f * 1.0f * cos(doublePendulumTheta1 - doublePendulumTheta2)
        val den2 = 1.0f * (2 * 1.0f + 1.0f - 1.0f * cos(2 * doublePendulumTheta1 - 2 * doublePendulumTheta2))

        val alpha2 = (num5 * (num6 + num7 + num8)) / den2

        val forceRandomness = settings.chaoticParamRandomness.random() * 0.5f
        doublePendulumOmega1 += alpha1 * dt + gaussianRandom(0f, 0.1f) * forceRandomness
        doublePendulumOmega2 += alpha2 * dt + gaussianRandom(0f, 0.1f) * forceRandomness
        doublePendulumTheta1 += doublePendulumOmega1 * dt
        doublePendulumTheta2 += doublePendulumOmega2 * dt
    }

    private fun updateMultiChaotic(dt: Float) {
        updateLorenz(dt * 0.3f)
        updateRossler(dt * 0.4f)
        updateHenon()
        updateCustomChaoticSystem(dt * 0.3f)
    }

    private fun updateSwitchSystem(dt: Float) {
        switchSystemState += switchDirection * dt * 5f

        if (abs(switchSystemState) > 1f) {
            switchDirection *= -1f
            switchSystemState = switchSystemState.coerceIn(-1f, 1f)
        }

        if (nextFloat() < 0.1f) {
            switchSystemState += gaussianRandom(0f, 0.1f)
        }
    }

    private fun updateCustomChaoticSystem(dt: Float) {
        val a = 1.5f + gaussianRandom(0f, 0.2f) * settings.chaoticParamRandomness.random()
        val b = 0.8f + gaussianRandom(0f, 0.1f) * settings.chaoticParamRandomness.random()
        val c = 1.2f + gaussianRandom(0f, 0.15f) * settings.chaoticParamRandomness.random()

        val dx = (a * (customChaosY - customChaosX) + c * sin(customChaosZ)) * dt
        val dy = (customChaosX * (b - customChaosZ) - customChaosY + a * cos(customChaosX)) * dt
        val dz = (customChaosX * customChaosY - c * customChaosZ + b * sin(customChaosY)) * dt

        customChaosX += dx + gaussianRandom(0f, 0.01f) * settings.chaoticParamRandomness.random()
        customChaosY += dy + gaussianRandom(0f, 0.01f) * settings.chaoticParamRandomness.random()
        customChaosZ += dz + gaussianRandom(0f, 0.005f) * settings.chaoticParamRandomness.random()
    }

    private fun evolveChaoticParameters() {
        if (!settings.chaoticParamEvolution) return

        val evolutionRate = settings.chaoticEvolutionRate.random()

        lorenzSigma += gaussianRandom(0f, 0.01f) * evolutionRate
        lorenzRho += gaussianRandom(0f, 0.02f) * evolutionRate
        lorenzBeta += gaussianRandom(0f, 0.005f) * evolutionRate
        rosslerA += gaussianRandom(0f, 0.005f) * evolutionRate
        rosslerB += gaussianRandom(0f, 0.005f) * evolutionRate
        rosslerC += gaussianRandom(0f, 0.01f) * evolutionRate

        lorenzSigma = lorenzSigma.coerceIn(8f, 12f)
        lorenzRho = lorenzRho.coerceIn(20f, 35f)
        lorenzBeta = lorenzBeta.coerceIn(2f, 3f)
        rosslerA = rosslerA.coerceIn(0.1f, 0.3f)
        rosslerB = rosslerB.coerceIn(0.1f, 0.3f)
        rosslerC = rosslerC.coerceIn(4f, 7f)
    }

    private fun saveChaoticHistory() {
        val state = floatArrayOf(
            lorenzX, lorenzY, lorenzZ,
            rosslerX, rosslerY, rosslerZ,
            henonX, henonY,
            doublePendulumTheta1, doublePendulumTheta2,
            customChaosX, customChaosY, customChaosZ,
            switchSystemState
        )

        if (chaoticHistory.size >= 100) {
            chaoticHistory.removeFirst()
        }
        chaoticHistory.addLast(state)
    }

    private fun maybeResetChaoticSystem() {
        if (nextFloat() < settings.chaoticResetChance.random()) {
            when (settings.chaoticSystem) {
                "Lorenz" -> {
                    lorenzX = gaussianRandom(0f, 1f)
                    lorenzY = gaussianRandom(0f, 1f)
                    lorenzZ = gaussianRandom(0f, 1f)
                }
                "Rossler" -> {
                    rosslerX = gaussianRandom(0f, 0.5f)
                    rosslerY = gaussianRandom(0f, 0.5f)
                    rosslerZ = gaussianRandom(0f, 0.5f)
                }
                "Henon" -> {
                    henonX = gaussianRandom(0f, 0.3f)
                    henonY = gaussianRandom(0f, 0.3f)
                }
                "DoublePendulum" -> {
                    doublePendulumTheta1 = gaussianRandom(PI.toFloat() / 4, PI.toFloat() / 8)
                    doublePendulumTheta2 = gaussianRandom(PI.toFloat() / 4, PI.toFloat() / 8)
                    doublePendulumOmega1 = gaussianRandom(0f, 1f)
                    doublePendulumOmega2 = gaussianRandom(0f, 1f)
                }
                "Switch" -> {
                    switchSystemState = 0f
                    switchDirection = if (nextBoolean()) 1f else -1f
                }
            }
        }
    }

    private fun gaussianRandom(mean: Float, sigma: Float): Float {
        val u1 = nextFloat()
        val u2 = nextFloat()
        val z0 = sqrt(-2.0 * ln(u1.toDouble())) * cos(2.0 * PI * u2.toDouble())
        return (z0 * sigma.toDouble() + mean.toDouble()).toFloat()
    }

    fun getEnhancedAimContext(): RandomizationSettings.EnhancedAimContext? {
        val target = mc.pointedEntity ?: return null
        val player = mc.thePlayer ?: return null

        val distance = player.getDistanceToEntity(target)
        val targetSpeed = sqrt(target.motionX * target.motionX + target.motionZ * target.motionZ).toFloat()
        val playerSpeed = sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ).toFloat()

        val combatDuration = System.currentTimeMillis() - combatStartTime

        val threatLevel = calculateThreatLevel(player, target)

        return RandomizationSettings.EnhancedAimContext(distance, targetSpeed, playerSpeed, combatDuration, recentHitRate, threatLevel)
    }

    private fun calculateThreatLevel(player: EntityPlayerSP, target: Entity): Float {
        var threat = 0f
        threat += (10f - player.getDistanceToEntity(target)).coerceAtLeast(0f) * 0.1f
        val nearbyEnemies = player.worldObj.getEntitiesWithinAABBExcludingEntity(
            player, player.entityBoundingBox.expand(10.0, 10.0, 10.0)
        ).count { it is EntityLivingBase && it !== player && it !== target }
        threat += nearbyEnemies * 0.2f

        return threat.coerceIn(0f, 1f)
    }

    private fun applyContextualFineTuning(rotation: Rotation, context: RandomizationSettings.EnhancedAimContext) {
        when {
            context.combatDuration > 30000 -> {
                rotation.yaw *= 0.95f
                rotation.pitch *= 0.95f
            }
            context.threatLevel > 0.7f -> {
                rotation.yaw *= 1.1f
                rotation.pitch *= 1.05f
            }
        }
    }
}
