/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.rotation

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextBoolean
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.lastRotations
import net.ccbluex.liquidbounce.utils.rotation.randomizations.ChaoticRandomization
import net.ccbluex.liquidbounce.utils.rotation.randomizations.HybridNoise
import net.ccbluex.liquidbounce.utils.rotation.randomizations.MiniDisturbanceSystem
import net.minecraft.client.Minecraft
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import java.util.*
import kotlin.math.*

class RandomizationSettings(owner: Module, val generalApply: () -> Boolean = { true }) : Configurable("Randomization") {
    val mc = Minecraft.getMinecraft()!!
    private val randomizationPattern by choices(
        "RandomizationPattern",
        arrayOf("None", "Zig-Zag","LazyFlick", "Noise", "TrueRandom", "Perlin", "Advanced", "HybridNoise","AugustusXIntave", "Chaotic"),
        "None"
    ) { generalApply() }
    val randomizationChance by floatRange(
        "All/RandomizationApplyChance",
        0.7f..1.0f,
        0f..1f
    ) { generalApply() && randomizationPattern != "None" }
    var chaoticSystem by choices(
        "Chaotic/System",
        arrayOf("Lorenz", "Rossler", "Henon", "DoublePendulum", "MultiChaotic", "Switch"),
        "Lorenz"
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticIntensity by floatRange(
        "Chaotic/Intensity", 0.5f..1.5f, 0.1f..5f
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticSpeed by floatRange(
        "Chaotic/Speed", 0.01f..0.05f, 0.001f..0.2f
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticCoupling by floatRange(
        "Chaotic/Coupling", 0.1f..0.3f, 0f..1f
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticMemory by boolean(
        "Chaotic/Memory", true
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticAdaptive by boolean(
        "Chaotic/Adaptive", true
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticResetChance by floatRange(
        "Chaotic/Random/ResetChance", 0.01f..0.05f, 0f..0.2f
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticJumpIntensity by floatRange(
        "Chaotic/Random/JumpIntensity", 0.5f..2f, 0f..5f
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticNoiseMix by floatRange(
        "Chaotic/Random/NoiseMix", 0.1f..0.3f, 0f..0.5f
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticParamRandomness by floatRange(
        "Chaotic/Random/ParamRandomness", 0.1f..0.3f, 0f..1f
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticAntiML by boolean(
        "Chaotic/AntiML", true
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticPatternRandomization by floatRange(
        "Chaotic/AntiML/PatternRandomization", 0.1f..0.3f, 0f..1f
    ) { randomizationPattern == "Chaotic" && chaoticAntiML }

    val chaoticTemporalNoise by floatRange(
        "Chaotic/AntiML/TemporalNoise", 0.05f..0.15f, 0f..0.5f
    ) { randomizationPattern == "Chaotic" && chaoticAntiML }

    val chaoticBehaviorMimicry by boolean(
        "Chaotic/AntiML/BehaviorMimicry", true
    ) { randomizationPattern == "Chaotic" && chaoticAntiML }

    val chaoticHumanLikeTransitions by boolean(
        "Chaotic/AntiML/HumanLikeTransitions", true
    ) { randomizationPattern == "Chaotic" && chaoticAntiML }

    val chaoticDynamicMixing by boolean(
        "Chaotic/Advanced/DynamicMixing", true
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticAutoSwitch by floatRange(
        "Chaotic/Advanced/AutoSwitch", 0.01f..0.05f, 0f..0.2f
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticMixWeights by boolean(
        "Chaotic/Advanced/AdaptiveWeights", true
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticParamEvolution by boolean(
        "Chaotic/Advanced/ParamEvolution", true
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    val chaoticEvolutionRate by floatRange(
        "Chaotic/Advanced/EvolutionRate", 0.001f..0.01f, 0f..0.1f
    ) { randomizationPattern == "Chaotic" && randomizationChosen }

    private val intaveRandomStrength by floatRange(
        "AugustusXIntave/RandomStrength", 1.0f..1.0f, 0f..10f
    ) { randomizationPattern == "AugustusXIntave" && randomizationChosen }
    private val intaveMode by choices(
        "AugustusXIntave/Mode", arrayOf("Basic", "OnlyRotation", "Doubled"), "Basic"
    ) { randomizationPattern == "AugustusXIntave" && randomizationChosen }
    val hybridNoiseMean by floatRange(
        "HybridNoise/Basic/Mean", -0.5f..0.5f, -5f..5f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridNoiseSigma by floatRange(
        "HybridNoise/Basic/Sigma", 0.05f..0.15f, 0f..10f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }
    private val hybridSpectralDensity by floatRange(
        "HybridNoise/Spectral/Density", 0.5f..1.5f, 0.1f..3f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    private val hybridFrequencyBands by choices(
        "HybridNoise/Spectral/Bands",
        arrayOf("Full", "LowPass", "HighPass", "BandPass", "Notch"),
        "Full"
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    private val hybridCutoffFreq by floatRange(
        "HybridNoise/Spectral/CutoffLow", 0.2f..0.5f, 0.01f..2f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    private val hybridCutoffFreqHigh by floatRange(
        "HybridNoise/Spectral/CutoffHigh", 1.0f..2.0f, 0.1f..5f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    private val hybridSpectralSlope by floatRange(
        "HybridNoise/Spectral/Slope", 1.0f..3.0f, 0.5f..6f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    private val harmonicBoost by floatRange(
        "HybridNoise/Spectral/HarmonicBoost", 0.0f..0.3f, 0f..1f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }
    val hybridNoiseTheta by floatRange(
        "HybridNoise/Process/Theta", 0.3f..0.7f, 0f..10f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridNoiseDt by floatRange(
        "HybridNoise/Process/DT", 0.01f..0.05f, 0f..5f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridNoiseScale by floatRange(
        "HybridNoise/Process/GlobalScale", 3f..8f, 0f..100f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridNoiseMixRatio by floatRange(
        "HybridNoise/Mix/BrownianToOURatio", 0.3f..0.7f, 0f..1f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridNoiseLayerCount by intRange(
        "HybridNoise/Mix/LayerCount", 2..4, 1..15,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridDynamicModulation by choices(
        "HybridNoise/Dynamic/ModulationType",
        arrayOf("Sine", "Sawtooth", "Square", "Random"),
        "Sine",
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridModulationAmplitude by floatRange(
        "HybridNoise/Dynamic/ModAmp", 0.1f..0.5f, 0f..2f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridModulationFrequency by floatRange(
        "HybridNoise/Dynamic/ModFreq", 0.5f..2f, 0f..10f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridYawPitchRatio by floatRange(
        "HybridNoise/Axis/YawPitchRatio", 1.5f..3f, 0f..10f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridAxisCoupling by floatRange(
        "HybridNoise/Axis/Coupling", 0f..0.5f, 0f..1f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridDistanceFactorMin by floatRange(
        "HybridNoise/Distance/MinMultiplier", 0.5f..0.8f, 0f..2f
    ) { randomizationPattern == "HybridNoise" && randomizationChosen}

    val hybridDistanceFactorMax by floatRange(
        "HybridNoise/Distance/MaxMultiplier", 1.2f..1.5f, 0f..3f
    ) { randomizationPattern == "HybridNoise" && randomizationChosen}

    val hybridDistanceRangeMin by floatRange(
        "HybridNoise/Distance/RangeMin", 2f..4f, 0f..20f
    ) { randomizationPattern == "HybridNoise" && randomizationChosen}

    val hybridDistanceRangeMax by floatRange(
        "HybridNoise/Distance/RangeMax", 8f..12f, 0f..50f
    ) { randomizationPattern == "HybridNoise" && randomizationChosen}

    val hybridJitterProbability by floatRange(
        "HybridNoise/Advanced/JitterProb", 0.05f..0.2f, 0f..1f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridJitterStrength by floatRange(
        "HybridNoise/Advanced/JitterStr", 1f..3f, 0f..180f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridMicroJitter by floatRange(
        "MicroJitter", 0.1f..0.3f, 0f..5f
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    val hybridSmoothness by floatRange(
        "HybridNoise/Advanced/Smoothness", 0.7f..1.3f, 0f..5f,
    ) { randomizationPattern == "HybridNoise" && randomizationChosen }

    private val advancedTrajectoryCount by intRange(
        "Advanced/Trajectories", 4..6, 1..20
    ) { randomizationPattern == "Advanced" && randomizationChosen }

    private val advancedNoiseLevel by floatRange(
        "Advanced/NoiseLevel", 0.01f..0.1f, 0f..1f
    ) { randomizationPattern == "Advanced" && randomizationChosen }

    private val advancedSmoothness by floatRange(
        "Advanced/Smoothness", 0.7f..1f, 0.1f..4f
    ) { randomizationPattern == "Advanced" && randomizationChosen }

    private val advancedContextAware by boolean(
        "Advanced/ContextAware", true
    ) { randomizationPattern == "Advanced" && randomizationChosen }

    private val advancedTrajectoryMemory by intRange(
        "Advanced/TrajectoryMemory", 1000..2000, 100..5000
    ) { randomizationPattern == "Advanced" && randomizationChosen }

    private val advancedInterpolation by choices(
        "Advanced/Interpolation", arrayOf("Hermite", "CatmullRom"), "CatmullRom"
    ) { randomizationPattern == "Advanced" && randomizationChosen }

    private val advancedAdaptiveSpeed by boolean(
        "Advanced/AdaptiveSpeed", true
    ) { randomizationPattern == "Advanced" && randomizationChosen }

    private val perlinOctaves by intRange(
        "PerlinOctaves", 3..4, 1..10
    ) { randomizationPattern == "Perlin" && randomizationChosen }

    private val perlinPersistence by floatRange(
        "PerlinPersistence", 0.7f..0.8f, 0.1f..2f
    ) { randomizationPattern == "Perlin" && randomizationChosen }

    private val perlinScale by floatRange(
        "PerlinScale", 2f..5f, 0.5f..30f
    ) { randomizationPattern == "Perlin" && randomizationChosen }

    private val perlinYawScale by floatRange(
        "Perlin/YawScale", 5f..8f, 0.1f..40f
    ) { randomizationPattern == "Perlin" && randomizationChosen }

    private val perlinPitchScale by floatRange(
        "Perlin/PitchScale", 3f..5f, 0.1f..30f
    ) { randomizationPattern == "Perlin" && randomizationChosen }

    private val trueRandomStrength by floatRange(
        "TrueRandomStrength", 3f..8f, 0f..30f
    ) { randomizationPattern == "TrueRandom" && randomizationChosen }

    private val trueRandomSmoothness by intRange(
        "TrueRandomSmoothness", 1..3, 1..10
    ) { randomizationPattern == "TrueRandom" && randomizationChosen }

    private val noiseSpeed by floatRange(
        "NoiseSpeed", 0.01f..0.05f, 0f..1f
    ) { randomizationPattern == "Noise" && randomizationChosen }

    private val noiseScale by floatRange(
        "NoiseScale", 3f..8f, 0f..60f
    ) { randomizationPattern == "Noise" && randomizationChosen }

    private val yawRandomizationChance by floatRange(
        "YawRandomizationChance", 0.8f..1.0f, 0f..1f
    ) { randomizationChosen && randomizationPattern !in listOf("Noise", "TrueRandom", "Perlin", "Advanced", "HybridNoise","AugustusXIntave", "Chaotic") }

    private val yawRandomizationRange by floatRange(
        "YawRandomizationRange", 5f..10f, 0f..30f
    ) { isZizZagActive && randomizationChosen && yawRandomizationChance.start != 1F &&
            randomizationPattern !in listOf("Noise", "TrueRandom", "Perlin", "Advanced", "HybridNoise","AugustusXIntave", "Chaotic") }

    private val yawSpeedIncreaseMultiplier by intRange(
        "YawSpeedIncreaseMultiplier", 50..120, 0..500, "%"
    ) { !isZizZagActive && randomizationChosen && yawRandomizationChance.start != 1F &&
            randomizationPattern !in listOf("Noise", "TrueRandom", "Perlin", "Advanced", "HybridNoise","AugustusXIntave", "Chaotic") }

    private val pitchRandomizationChance by floatRange(
        "PitchRandomizationChance", 0.8f..1.0f, 0f..1f
    ) { randomizationChosen && randomizationPattern !in listOf("Noise", "TrueRandom", "Perlin", "Advanced", "HybridNoise","AugustusXIntave", "Chaotic") }

    private val pitchRandomizationRange by floatRange(
        "PitchRandomizationRange", 2f..5f, 0f..30f
    ) { randomizationChosen && pitchRandomizationChance.start != 1F &&
            randomizationPattern !in listOf("Noise", "TrueRandom", "Perlin", "Advanced", "HybridNoise","AugustusXIntave", "Chaotic") }


    private val stopOnTargetAndPlayerNotMove by boolean("StopOnUserNotisMoving",false) { randomizationChosen }
    private val onlyWorkWhenCantAttackEntity by boolean(
        "OnlyWorkWhenCantAttackEntity", false
    ) { randomizationChosen }
    val allowWorkWhenRotating by boolean(
        "AllowWorkWhenRotating", true
    ) { randomizationChosen }

    private var noiseTime = 0L
    private var perlinSeed = nextFloat() * 1000f
    private var lastAdvancedTrajectory: RotationTrajectory? = null
    private var advancedProgress = 0f
    private var trajectoryRecording = ArrayList<Rotation>()

    val enableDisturbance by boolean("DisturbanceRandom", false) { generalApply() }

    val disturbanceSize by intRange(
        "Disturbance/Size",
        20..30,
        10..50
    ) { enableDisturbance }

    val disturbanceLowRate by floatRange(
        "Disturbance/LowRate",
        0.1f..0.3f,
        0f..1f
    ) { enableDisturbance }

    val disturbanceHighRate by floatRange(
        "Disturbance/HighRate",
        0.1f..0.3f,
        0f..1f
    ) { enableDisturbance }

    val disturbanceInterval by intRange(
        "Disturbance/Interval",
        3..5,
        1..20
    ) { enableDisturbance }
    private val disturbanceSystem = MiniDisturbanceSystem()

    private val isZizZagActive get() = randomizationPattern == "Zig-Zag"
    val randomizationChosen get() = randomizationPattern != "None" && generalApply()

    private val spectralHistory = ArrayDeque<FloatArray>(128)
    private var spectralUpdateTime = 0L
    private val harmonicAnalyzer = HarmonicAnalyzer()
    private val spectralFilter = SpectralFilter()
    private class TrajectoryDatabase {
        private val trajectories = ArrayList<RotationTrajectory>()
        private val rng = Random()

        fun count() = trajectories.size

        fun findSimilar(current: Rotation, count: Int, context: AimContext? = null): List<RotationTrajectory> {
            if (trajectories.isEmpty()) return emptyList()

            return trajectories
                .sortedBy { trajectory ->
                    var score = abs(trajectory.start.yaw - current.yaw) +
                            abs(trajectory.start.pitch - current.pitch)

                    context?.let {
                        score += abs(trajectory.context?.distance?.minus(it.distance) ?: 0f) * 0.1f
                        score += abs(trajectory.context?.targetSpeed?.minus(it.targetSpeed) ?: 0f) * 0.2f
                    }

                    score
                }
                .take(count)
        }

        fun addTrajectory(trajectory: RotationTrajectory, maxMemory: Int) {
            if (trajectories.size > maxMemory) {
                trajectories.removeAt(rng.nextInt(trajectories.size))
            }
            cleanupOldTrajectories(maxMemory)
            trajectories.add(trajectory)
        }

        private fun cleanupOldTrajectories(maxMemory: Int) {
            if (trajectories.size <= maxMemory) return

            val now = System.currentTimeMillis()

            trajectories.sortWith(compareBy {
                (now - it.lastUsedTime) * (1f - it.score)
            })

            val toRemove = trajectories.size - maxMemory
            repeat(toRemove) {
                if (trajectories.isNotEmpty()) {
                    trajectories.removeAt(0)
                }
            }
        }
    }

    private data class AimContext(
        val distance: Float,
        val targetSpeed: Float,
        val playerSpeed: Float
    )

    data class EnhancedAimContext(
        val distance: Float,
        val targetSpeed: Float,
        val playerSpeed: Float,
        val combatDuration: Long,
        val recentHitRate: Float,
        val threatLevel: Float
    )

    private data class RotationTrajectory(
        val start: Rotation,
        val points: List<Rotation>,
        val context: AimContext? = null,
        val score: Float = 1f,
        val creationTime: Long = System.currentTimeMillis(),
        var lastUsedTime: Long = System.currentTimeMillis()
    )
    private fun getCurrentAimContext(): AimContext? {
        val target = mc.pointedEntity ?: return null
        val player = mc.thePlayer ?: return null

        val distance = player.getDistanceToEntity(target)
        val targetSpeed = sqrt(target.motionX * target.motionX + target.motionZ * target.motionZ).toFloat()
        val playerSpeed = sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ).toFloat()

        return AimContext(distance, targetSpeed, playerSpeed)
    }



    private val trajectoryDB = TrajectoryDatabase()

    private object PerlinNoise {
        private val p = IntArray(512)
        private val permutation = intArrayOf(
            151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
            190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,88,237,149,56,87,174,20,
            125,136,171,168,68,175,74,165,71,134,139,48,27,166,77,146,158,231,83,111,229,122,60,211,133,230,220,
            105,92,41,55,46,245,40,244,102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,200,196,
            135,130,116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,250,124,123,5,202,38,147,118,126,255,
            82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,223,183,170,213,119,248,152,2,44,154,163,70,221,
            153,101,155,167,43,172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,218,246,97,228,
            251,34,242,193,238,210,144,12,191,179,162,241,81,51,145,235,249,14,239,107,49,192,214,31,181,199,106,
            157,184,84,204,176,115,121,50,45,127,4,150,254,138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,
            66,215,61,156,180
        )

        init {
            for (i in 0..255) {
                p[i] = permutation[i]
                p[256 + i] = p[i]
            }
        }

        private fun fade(t: Float): Float = t * t * t * (t * (t * 6 - 15) + 10)
        private fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)
        private fun grad(hash: Int, x: Float, y: Float, z: Float): Float {
            val h = hash and 15
            val u = if (h < 8) x else y
            val v = if (h < 4) y else if (h == 12 || h == 14) x else z
            return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
        }

        fun noise(x: Float, y: Float, z: Float): Float {
            val xi = floor(x).toInt() and 255
            val yi = floor(y).toInt() and 255
            val zi = floor(z).toInt() and 255
            val xf = x - floor(x)
            val yf = y - floor(y)
            val zf = z - floor(z)
            val u = fade(xf)
            val v = fade(yf)
            val w = fade(zf)

            val aaa = p[p[p[xi] + yi] + zi]
            val aba = p[p[p[xi] + yi + 1] + zi]
            val aab = p[p[p[xi] + yi] + zi + 1]
            val abb = p[p[p[xi] + yi + 1] + zi + 1]
            val baa = p[p[p[xi + 1] + yi] + zi]
            val bba = p[p[p[xi + 1] + yi + 1] + zi]
            val bab = p[p[p[xi + 1] + yi] + zi + 1]
            val bbb = p[p[p[xi + 1] + yi + 1] + zi + 1]

            val x1 = lerp(u, grad(aaa, xf, yf, zf), grad(baa, xf-1, yf, zf))
            val x2 = lerp(u, grad(aba, xf, yf-1, zf), grad(bba, xf-1, yf-1, zf))
            val y1 = lerp(v, x1, x2)

            val x3 = lerp(u, grad(aab, xf, yf, zf-1), grad(bab, xf-1, yf, zf-1))
            val x4 = lerp(u, grad(abb, xf, yf-1, zf-1), grad(bbb, xf-1, yf-1, zf-1))
            val y2 = lerp(v, x3, x4)

            return lerp(w, y1, y2)
        }

        fun fractalNoise(x: Float, y: Float, z: Float, octaves: Int, persistence: Float): Float {
            var total = 0f
            var frequency = 1f
            var amplitude = 1f
            var maxValue = 0f

            repeat(octaves) {
                total += noise(x * frequency, y * frequency, z * frequency) * amplitude
                maxValue += amplitude
                amplitude *= persistence
                frequency *= 2
            }

            return total / maxValue
        }
    }

    @Suppress("SameParameterValue")
    private fun gaussianRandom(mean: Float, sigma: Float): Float {
        val u1 = nextFloat()
        val u2 = nextFloat()
        val z0 = sqrt(-2.0 * ln(u1.toDouble())) * cos(2.0 * PI * u2.toDouble())
        return (z0 * sigma.toDouble() + mean.toDouble()).toFloat()
    }
    private fun getAugustusXIntaveOffset(axis: Int, currentRotation: Rotation, targetRotation: Rotation): Float {
        val currentAngle = if (axis == 0) currentRotation.yaw else currentRotation.pitch
        val targetAngle = if (axis == 0) targetRotation.yaw else targetRotation.pitch

        val angleDiff = if (axis == 0) {
            angleDifference(targetAngle, currentAngle)
        } else {
            targetAngle - currentAngle
        }

        val randomValue = when (intaveMode) {
            "Basic" -> {
                val time = System.currentTimeMillis() / 1000f
                val sinInput = if (axis == 0) time * 0.5f else time * 0.3f
                (nextFloat(1.0f, 2.0f)) * sin(sinInput * PI.toFloat()) * intaveRandomStrength.random()
            }
            "OnlyRotation" -> {
                if (abs(angleDiff) > 1f) {
                    val time = System.currentTimeMillis() / 1000f
                    val sinInput = if (axis == 0) time * 0.5f else time * 0.3f
                    (nextFloat(1.0f, 2.0f)) * sin(sinInput * PI.toFloat()) * intaveRandomStrength.random()
                } else {
                    nextFloat(-0.5f, 0.5f) * intaveRandomStrength.random() * 0.1f
                }
            }
            "Doubled" -> {
                val random1 = nextFloat(-1f, 1f)
                val random2 = nextFloat(-1f, 1f)
                min(random1, random2) + (max(random1, random2) - min(random1, random2)) * intaveRandomStrength.random()
            }
            else -> 0.0f
        }
        return randomValue
    }


    fun applySpectralProcessing(input: Float, axis: Int, timeBase: Float): Float {
        updateSpectralHistory(input, axis)

        var processed = input * hybridSpectralDensity.random()

        processed = spectralFilter.applyFilter(
            processed,
            hybridFrequencyBands,
            hybridCutoffFreq.random(),
            hybridCutoffFreqHigh.random(),
            hybridSpectralSlope.random()
        )

        if (harmonicBoost.random() > 0.01f) {
            val harmonics = harmonicAnalyzer.analyze(processed, timeBase)
            for (i in harmonics.indices) {
                if (i < 3) {
                    processed += harmonics[i] * harmonicBoost.random() * (0.8f / (i + 1))
                }
            }
        }
        val spectralPattern = detectSpectralPattern()
        val patternFactor = when (spectralPattern) {
            "low_freq" -> 1.1f
            "high_freq" -> 0.9f
            else -> 1.0f
        }
        processed *= patternFactor

        processed = when (axis) {
            0 -> {
                when (spectralPattern) {
                    "low_freq" -> processed * 0.95f
                    "high_freq" -> processed * 1.05f
                    else -> processed
                }
            }
            1 -> {
                val highFreqReduction = if (spectralPattern == "high_freq") 0.6f else 0.7f
                processed * (1 - (hybridSpectralSlope.random() * 0.1f * highFreqReduction))
            }
            else -> processed
        }

        processed = applySpectralDispersion(processed, axis)

        return processed
    }
    private fun updateSpectralHistory(signal: Float, axis: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - spectralUpdateTime > 16) {
            if (spectralHistory.size >= 128) {
                spectralHistory.removeFirst()
            }

            val freqData = FloatArray(64)
            for (i in freqData.indices) {
                val freq = (i + 1) * 0.1f
                freqData[i] = signal * sin(currentTime * 0.001f * freq * (1 + axis * 0.2f))
            }

            spectralHistory.addLast(freqData)
            spectralUpdateTime = currentTime
        }
    }

    private fun applySpectralDispersion(signal: Float, axis: Int): Float {

        if (spectralHistory.size >= 32) {
            val recentData = mutableListOf<FloatArray>()

            val startIdx = max(0, spectralHistory.size - 32)
            val iterator = spectralHistory.iterator()
            var count = 0
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (count >= startIdx) {
                    recentData.add(item)
                }
                count++
            }

            var spectralSpread = 0f

            for (i in 0 until 64) {
                var binSum = 0f
                for (frame in recentData) {
                    binSum += abs(frame[i])
                }
                spectralSpread += (binSum / recentData.size) * (i + 1)
            }

            val avgSpread = spectralSpread / 64

            if (avgSpread < 0.1f) {
                val dispersion = (0.2f - avgSpread) * 2f
                return signal * (1 + dispersion * (0.3f + axis * 0.1f))
            }
        }

        return signal
    }

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    private fun getTrueRandomOffset(axis: Int): Float {
        return when (axis) {
            0 -> (nextFloat() * 2 - 1) * trueRandomStrength.random()
            1 -> (nextFloat() * 2 - 1) * trueRandomStrength.random() * 0.5f
            else -> 0f
        }
    }

    private fun getNoiseOffset(axis: Int): Float {
        noiseTime += (10 + nextInt(20)).toLong()
        val t = noiseTime * 0.001 * noiseSpeed.random().toDouble()

        return when (axis) {
            0 -> (sin(t * 1.3) * 0.6 + sin(t * 0.7) * 0.4).toFloat() * noiseScale.random()
            1 -> (cos(t * 0.9) * 0.5 - cos(t * 0.4) * 0.3).toFloat() * noiseScale.random() * 0.5f
            else -> 0f
        }
    }

    private fun getPerlinOffset(axis: Int): Float {
        val time = System.currentTimeMillis() / 2000f
        val baseNoise = when (axis) {
            0 -> {
                val x = perlinSeed + time * 0.3f
                PerlinNoise.fractalNoise(x, 0f, 0f,
                    perlinOctaves.random(),
                    perlinPersistence.random()
                ) * perlinScale.random()
            }
            1 -> {
                val y = perlinSeed + time * 0.2f
                PerlinNoise.fractalNoise(0f, y, 0f,
                    perlinOctaves.random(),
                    perlinPersistence.random()
                ) * perlinScale.random()
            }
            else -> 0f
        }

        return when (axis) {
            0 -> baseNoise * perlinYawScale.random()
            1 -> baseNoise * perlinPitchScale.random()
            else -> 0f
        }.also { offset ->
            if (abs(offset) < 0.3f) {
                return (if (nextBoolean()) 0.5f else -0.5f) *
                        when (axis) {
                            0 -> perlinYawScale.random()
                            1 -> perlinPitchScale.random()
                            else -> 1f
                        }
            }
        }
    }

    private fun selectOptimalTrajectory(candidates: List<RotationTrajectory>, context: AimContext?, useContextAware: Boolean): RotationTrajectory {
        return candidates.maxBy { candidate ->
            var score = candidate.score


            if (useContextAware)
                context?.let { ctx ->
                    candidate.context?.let { trajCtx ->
                        if (abs(trajCtx.distance - ctx.distance) < 3) score += 0.3f
                        if (abs(trajCtx.targetSpeed - ctx.targetSpeed) < 0.5) score += 0.2f
                    }
                }

            val noise = (nextFloat() * 2 - 1) * advancedNoiseLevel.random()
            score + noise
        }
    }
    private fun catmullRomInterpolate(points: List<Rotation>, index: Int, t: Float, axis: Int): Float {
        val p0 = points.getOrNull(index - 1) ?: points[index]
        val p1 = points[index]
        val p2 = points[index + 1]
        val p3 = points.getOrNull(index + 2) ?: points[index + 1]

        val v0 = if (axis == 0) p0.yaw else p0.pitch
        val v1 = if (axis == 0) p1.yaw else p1.pitch
        val v2 = if (axis == 0) p2.yaw else p2.pitch
        val v3 = if (axis == 0) p3.yaw else p3.pitch

        return catmullRom(t, v0, v1, v2, v3)
    }

    private fun catmullRom(t: Float, v0: Float, v1: Float, v2: Float, v3: Float): Float {
        val t2 = t * t
        val t3 = t2 * t

        return 0.5f * (
                (2 * v1) +
                        (-v0 + v2) * t +
                        (2 * v0 - 5 * v1 + 4 * v2 - v3) * t2 +
                        (-v0 + 3 * v1 - 3 * v2 + v3) * t3
                )
    }

    private fun processAdvanced(rotation: Rotation) {
        if (trajectoryDB.count() < 10) {
            loadDefaultTrajectories()
        }

        val currentTarget = lastRotations[2]
        val currentContext = if (advancedContextAware) getCurrentAimContext() else null

        val candidates = trajectoryDB.findSimilar(
            currentTarget,
            advancedTrajectoryCount.random(),
            currentContext
        ).takeIf { it.isNotEmpty() } ?: run {
            loadDefaultTrajectories()
            trajectoryDB.findSimilar(currentTarget, 5, currentContext)
        }

        val effectiveNoise = advancedNoiseLevel.random().coerceIn(0f, 0.15f)
        val selected = selectOptimalTrajectory(candidates, currentContext, advancedContextAware).also {
            lastAdvancedTrajectory = it
        }

        if (selected.points.size >= 4) {
            val progress: Int =
                (advancedProgress % selected.points.size).coerceAtMost((selected.points.size - 4).toFloat()).toInt()
            val t = (advancedProgress - progress).coerceIn(0f, 1f)

            when (advancedInterpolation) {
                "CatmullRom" -> {
                    rotation.yaw = catmullRomInterpolate(selected.points, progress, t, 0)
                    rotation.pitch = catmullRomInterpolate(selected.points, progress, t, 1) * 0.8f
                }
                else -> {
                    rotation.yaw = hermiteInterpolate(
                        selected.points[progress].yaw,
                        selected.points[progress + 1].yaw,
                        selected.points[progress + 2].yaw,
                        selected.points[progress + 3].yaw,
                        t,
                        advancedSmoothness.random()
                    )
                    rotation.pitch = hermiteInterpolate(
                        selected.points[progress].pitch,
                        selected.points[progress + 1].pitch,
                        selected.points[progress + 2].pitch,
                        selected.points[progress + 3].pitch,
                        t,
                        advancedSmoothness.random() * 0.8f
                    )
                }
            }
        }

        val baseSpeed = 0.1f + effectiveNoise * 2f
        val finalSpeed = if (advancedAdaptiveSpeed && currentContext != null) {

            val distanceFactor = (currentContext.distance / 10f).coerceIn(0.5f, 2f)
            baseSpeed * distanceFactor
        } else {
            baseSpeed
        }

        advancedProgress += finalSpeed.coerceIn(0.05f, 0.3f)
        recordCurrentTrajectory(rotation, currentContext)
    }
    private fun hermiteInterpolate(
        y0: Float, y1: Float,
        y2: Float, y3: Float,
        mu: Float, tension: Float = 0.5f
    ): Float {
        val m0 = (y1 - y0) * tension
        val m1 = (y3 - y2) * tension

        val mu2 = mu * mu
        val mu3 = mu2 * mu

        return (2 * mu3 - 3 * mu2 + 1) * y1 +
                (mu3 - 2 * mu2 + mu) * m0 +
                (-2 * mu3 + 3 * mu2) * y2 +
                (mu3 - mu2) * m1
    }
    private fun recordCurrentTrajectory(rotation: Rotation, context: AimContext? = null) {
        trajectoryRecording.add(rotation.copy())
        if (trajectoryRecording.size > 50) {
            trajectoryDB.addTrajectory(
                RotationTrajectory(
                    start = trajectoryRecording.first(),
                    points = trajectoryRecording.toList(),
                    context = context,
                    score = calculateTrajectoryScore(trajectoryRecording),
                ),
                advancedTrajectoryMemory.random()
            )
            trajectoryRecording.clear()
        }
    }

    private fun calculateTrajectoryScore(points: List<Rotation>): Float {
        var score = 1f
        for (i in 1 until points.size) {
            val deltaYaw = abs(angleDifference(points[i].yaw, points[i-1].yaw))
            val deltaPitch = abs(points[i].pitch - points[i-1].pitch)

            if (deltaYaw > 30 || deltaPitch > 15) {
                score *= 0.7f
            } else if (deltaYaw < 0.5 && deltaPitch < 0.3) {
                score *= 1.05f
            }
        }
        return score.coerceIn(0.1f, 1.5f)
    }
    private fun applyContextualFineTuning(rotation: Rotation, context: EnhancedAimContext) {
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

    private fun performPostProcessing() {
        if (trajectoryRecording.size > 100) {
            trajectoryRecording.clear()
        }
    }

    fun processNextSpot(box: AxisAlignedBB, rotation: Rotation, eyes: Vec3, range: Double, option: RotationSettings) {
        if (stopOnTargetAndPlayerNotMove && !mc.thePlayer.isMoving) return
        if (onlyWorkWhenCantAttackEntity && mc.pointedEntity != null) return
        if (nextFloat() > randomizationChance.random()) { return }
        when (randomizationPattern) {
            "AugustusXIntave" -> {
                val targetRot = lastRotations[2]
                rotation.yaw += getAugustusXIntaveOffset(0, rotation, targetRot)
                rotation.pitch += getAugustusXIntaveOffset(1, rotation, targetRot)
            }
            "Advanced" -> processAdvanced(rotation)
            "Perlin" -> {
                rotation.yaw += getPerlinOffset(0)
                rotation.pitch += getPerlinOffset(1)

                if (abs(angleDifference(rotation.yaw, lastRotations[2].yaw)) < 0.5f) {
                    rotation.yaw += getPerlinOffset(0) * 1.5f
                }
            }
            "TrueRandom" -> {
                val smoothFactor = trueRandomSmoothness.random()
                val newYaw = rotation.yaw + getTrueRandomOffset(0)
                val newPitch = rotation.pitch + getTrueRandomOffset(1)

                rotation.yaw = lerp(rotation.yaw, newYaw, 1f/smoothFactor)
                rotation.pitch = lerp(rotation.pitch, newPitch, 1f/smoothFactor)
            }
            "Noise" -> {
                rotation.yaw += getNoiseOffset(0)
                rotation.pitch += getNoiseOffset(1)
            }
            "Zig-Zag" -> {
                val pitchSign = angleDifference(rotation.pitch, lastRotations[2].pitch).sign.let {
                    if (it != 0f) it else (-1..1).random().toFloat()
                }
                val yawSign = angleDifference(rotation.yaw, lastRotations[2].yaw).sign.let {
                    if (it != 0f) it else (-1..1).random().toFloat()
                }

                rotation.yaw += if (nextFloat() < yawRandomizationChance.random()) {
                    yawRandomizationRange.random() * yawSign
                } else 0f

                rotation.pitch += if (nextFloat() < pitchRandomizationChance.random()) {
                    pitchRandomizationRange.random() * pitchSign
                } else 0f
            }
            "HybridNoise" -> {
                val target = mc.pointedEntity
                val distance = target?.let { mc.thePlayer?.getDistanceToEntity(it) ?: 0f } ?: 0f

                val hybridNoise = HybridNoise(this)
                rotation.yaw += hybridNoise.getHybridNoiseOffset(0, distance)
                rotation.pitch += hybridNoise.getHybridNoiseOffset(1, distance)

                if (abs(angleDifference(rotation.yaw, lastRotations[2].yaw)) < 0.3f) {
                    rotation.yaw += gaussianRandom(0f, 0.5f)
                }
            }
            "Chaotic" -> {
                val chaoticNoise = ChaoticRandomization(this)
                rotation.yaw += chaoticNoise.getEnhancedChaoticOffset(0)
                rotation.pitch += chaoticNoise.getEnhancedChaoticOffset(1)

                val context = chaoticNoise.getEnhancedAimContext()
                context?.let {
                    applyContextualFineTuning(rotation, it)
                }

                if (nextFloat() < 0.3f) {
                    rotation.yaw += gaussianRandom(0f, 0.2f)
                    rotation.pitch += gaussianRandom(0f, 0.1f)
                }

                if (abs(angleDifference(rotation.yaw, lastRotations[2].yaw)) < 0.2f) {
                    rotation.yaw += chaoticNoise.getEnhancedChaoticOffset(0) * 0.5f
                }
            }
            else -> {
                rotation.yaw += if (nextFloat() < yawRandomizationChance.random()) {
                    (yawSpeedIncreaseMultiplier.random() / 100f) * angleDifference(rotation.yaw, lastRotations[2].yaw)
                } else 0f

                rotation.pitch += if (nextFloat() < pitchRandomizationChance.random()) {
                    pitchRandomizationRange.random() + angleDifference(rotation.pitch, lastRotations[2].pitch).sign
                } else 0f
            }
        }
        applyMiniDisturbanceIfEnabled(rotation)
        rotation.fixedSensitivity()

        performPostProcessing()
    }

    init {
        owner.addValues(this.values)
        loadDefaultTrajectories()
    }
    private fun applyMiniDisturbanceIfEnabled(rotation: Rotation) {
        if (!enableDisturbance) return


        val lastRotArray = if (lastRotations.size >= 2) {
            arrayOf(lastRotations[1], lastRotations.getOrNull(2) ?: lastRotations[1])
        } else {
            arrayOf(
                Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch),
                Rotation(mc.thePlayer.prevRotationYaw, mc.thePlayer.prevRotationPitch)
            )
        }

        val disturbedRotation = disturbanceSystem.applyDisturbanceToRotation(
            currentRotation = rotation.copy(),
            lastRotations = lastRotArray,
            size = disturbanceSize.random(),
            lowRate = disturbanceLowRate.random(),
            highRate = disturbanceHighRate.random(),
            interval = disturbanceInterval.random()
        )

        rotation.yaw = disturbedRotation.yaw
        rotation.pitch = disturbedRotation.pitch
    }
    private fun loadDefaultTrajectories() {
        val trajectories = listOf(
            listOf(
                Rotation(0f, 0f), Rotation(2f, 1f), Rotation(4f, 2f),
                Rotation(6f, 1f), Rotation(8f, 0f)
            ) to AimContext(5f, 0.5f, 0.3f),


            listOf(
                Rotation(0f, 0f), Rotation(12f, 4f), Rotation(20f, 2f),
                Rotation(15f, 1f), Rotation(10f, 0f)
            ) to AimContext(3f, 2f, 1f),

            listOf(
                Rotation(0f, 0f), Rotation(1f, 0.3f), Rotation(0f, 0f),
                Rotation(-1f, 0.2f), Rotation(0f, 0f)
            ) to AimContext(8f, 0.1f, 0f),

            listOf(
                Rotation(0f, 0f), Rotation(5f, 2f), Rotation(10f, 3f),
                Rotation(15f, 2f), Rotation(20f, 1f)
            ) to AimContext(12f, 0f, 0f)
        )

        trajectories.forEach { (points, context) ->
            trajectoryDB.addTrajectory(
                RotationTrajectory(
                    start = points.first(),
                    points = points,
                    context = context,
                    score = 1.5f
                ),
                advancedTrajectoryMemory.random())
        }
    }
    private fun calculateSpectralCentroid(): Float {
        if (spectralHistory.isEmpty()) return 0.5f

        var totalEnergy = 0f
        var weightedSum = 0f

        val latest = spectralHistory.last()
        for (i in latest.indices) {
            val energy = abs(latest[i])
            totalEnergy += energy
            weightedSum += energy * i
        }

        return if (totalEnergy > 0) weightedSum / totalEnergy / latest.size else 0.5f
    }

    private fun detectSpectralPattern(): String {
        if (spectralHistory.size < 16) return "random"

        val centroid = calculateSpectralCentroid()

        return when {
            centroid < 0.3f -> "low_freq"
            centroid > 0.7f -> "high_freq"
            else -> "balanced"
        }
    }
    private class HarmonicAnalyzer {
        private val harmonicAmplitudes = FloatArray(8) { 0f }
        private val harmonicPhases = FloatArray(8) { 0f }
        private val history = ArrayDeque<Float>(512)

        fun analyze(signal: Float, time: Float): FloatArray {
            history.addLast(signal)
            if (history.size > 512) {
                history.removeFirst()
            }

            if (history.size >= 64) {
                val samples = history.toFloatArray()
                for (i in 1..8) {
                    harmonicAmplitudes[i-1] = detectHarmonic(samples, i, time)
                    harmonicPhases[i-1] = (time * i) % (2 * PI.toFloat())
                }
            }

            return harmonicAmplitudes.copyOf()
        }

        private fun detectHarmonic(samples: FloatArray, harmonic: Int, time: Float): Float {
            var sum = 0f
            val freq = harmonic * 0.1f
            for (i in samples.indices) {
                val t = (i.toFloat() / samples.size) * 2 * PI.toFloat()
                sum += samples[i] * sin(freq * t + time * 0.01f)
            }
            return abs(sum / samples.size) * 2f
        }
    }

    private class SpectralFilter {
        private val buffer = FloatArray(4) { 0f }
        private var lastOutput = 0f

        fun applyFilter(input: Float, filterType: String,
                        cutoffLow: Float, cutoffHigh: Float,
                        slope: Float): Float {
            buffer[3] = buffer[2]
            buffer[2] = buffer[1]
            buffer[1] = buffer[0]
            buffer[0] = input

            return when (filterType) {
                "LowPass" -> lowPass(input, cutoffLow, slope)
                "HighPass" -> highPass(input, cutoffHigh, slope)
                "BandPass" -> bandPass(input, cutoffLow, cutoffHigh, slope)
                "Notch" -> notchFilter(input, (cutoffLow + cutoffHigh) / 2, slope * 0.5f)
                else -> input

            }
        }

        private fun lowPass(input: Float, cutoff: Float, slope: Float): Float {
            val alpha = (1f / (1f + slope * cutoff)).coerceIn(0.01f, 0.99f)
            lastOutput = lastOutput * alpha + input * (1 - alpha)
            return lastOutput
        }

        private fun highPass(input: Float, cutoff: Float, slope: Float): Float {
            val alpha = (slope * cutoff).coerceIn(0.01f, 0.99f)
            lastOutput = alpha * (lastOutput + input - buffer[1])
            return lastOutput
        }

        private fun bandPass(input: Float, cutoffLow: Float, cutoffHigh: Float, slope: Float): Float {
            val low = lowPass(input, cutoffLow, slope)
            val high = highPass(low, cutoffHigh, slope)
            return high
        }

        private fun notchFilter(input: Float, centerFreq: Float, slope: Float): Float {
            val omega = 2 * PI.toFloat() * centerFreq
            val alpha = sin(omega / 2) * slope

            val a0 = 1 + alpha
            val a1 = -2 * cos(omega)
            val a2 = 1 - alpha
            val b0 = 1
            val b1 = -2 * cos(omega)
            val b2 = 1

            return (b0/a0)*input + (b1/a0)*buffer[0] + (b2/a0)*buffer[1] -
                    (a1/a0)*lastOutput - (a2/a0)*buffer[2]
        }
    }
}