package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import org.lwjgl.input.Keyboard
import kotlin.math.*

private fun ClosedFloatingPointRange<Float>.random(): Float =
    start + Math.random().toFloat() * (endInclusive - start)

object KillAuraCPS : Module("KillAura-CPS", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    // ===== CPS Mode Selection =====
    val cpsMode by choices("CPSMode", arrayOf(
        "Simple", "Dynamic", "Wave", "Burst", "Random", "Adaptive",
        "Step", "Jitter", "Spike", "Smart", "Reactive", "Momentum",
        "Fatigue", "Combo", "Stamina"
    ), "Simple") { true }

    // ===== Simple Mode =====
    val cps by intRange("CPS", 5..8, 1..50) {
        !simulateCooldown
    }

    // ===== Common Settings =====
    val simulateCooldown by boolean("SimulateCooldown", false) { true }
    val simulateDoubleClicking by boolean("SimulateDoubleClicking", false) {
        !simulateCooldown
    }
    val hurtTime by int("HurtTime", 10, 0..10) { !simulateCooldown }
    val activationSlot by boolean("ActivationSlot", false) { true }
    val preferredSlot by int("PreferredSlot", 1, 1..9) { activationSlot }
    val clickOnly by boolean("ClickOnly", false) { true }

    // ===== CPS Variation =====
    val cpsVariation by floatRange("CPSVariation", 0f..0.15f, 0f..1f) { true }
    val cpsDropChance by float("CPSDropChance", 0.03f, 0f..0.5f) { true }
    val cpsDropAmount by float("CPSDropAmount", 0.35f, 0f..0.8f) { true }
    val cpsSpikeChance by float("CPSSpikeChance", 0.02f, 0f..0.3f) { true }
    val cpsSpikeAmount by float("CPSSpikeAmount", 0.5f, 0f..1.5f) { true }
    val cpsSmoothing by floatRange("CPSSmoothing", 0.5f..0.8f, 0f..1f) { true }

    // ===== Dynamic Mode =====
    val dynamicBaseCPS by intRange("DynamicBaseCPS", 5..8, 1..30) { cpsMode == "Dynamic" }
    val dynamicMaxExtraCPS by int("DynamicMaxExtraCPS", 6, 0..20) { cpsMode == "Dynamic" }
    val dynamicSpeedFactor by float("DynamicSpeedFactor", 0.8f, 0f..3f) { cpsMode == "Dynamic" }
    val dynamicDistanceFactor by float("DynamicDistanceFactor", 0.6f, 0f..3f) { cpsMode == "Dynamic" }
    val dynamicHealthFactor by float("DynamicHealthFactor", 0.4f, 0f..2f) { cpsMode == "Dynamic" }
    val dynamicHurtFactor by float("DynamicHurtFactor", 0.3f, 0f..2f) { cpsMode == "Dynamic" }
    val dynamicComboFactor by float("DynamicComboFactor", 0.2f, 0f..1f) { cpsMode == "Dynamic" }

    // ===== Wave Mode =====
    val waveBaseCPS by int("WaveBaseCPS", 8, 1..30) { cpsMode == "Wave" }
    val waveAmplitude by int("WaveAmplitude", 3, 0..15) { cpsMode == "Wave" }
    val waveFrequency by float("WaveFrequency", 0.5f, 0.01f..5f) { cpsMode == "Wave" }
    val wavePhaseOffset by float("WavePhaseOffset", 0f, 0f..6.28f) { cpsMode == "Wave" }
    val waveType by choices("WaveType", arrayOf("Sine", "Triangle", "Sawtooth", "Square", "Custom"), "Sine") { cpsMode == "Wave" }
    val waveHarmonics by int("WaveHarmonics", 1, 1..8) { cpsMode == "Wave" && waveType == "Custom" }
    val waveHarmonicDecay by float("WaveHarmonicDecay", 0.5f, 0.1f..1f) { cpsMode == "Wave" && waveType == "Custom" }

    // ===== Burst Mode =====
    val burstBaseCPS by int("BurstBaseCPS", 4, 1..20) { cpsMode == "Burst" }
    val burstPeakCPS by int("BurstPeakCPS", 14, 5..50) { cpsMode == "Burst" }
    val burstDuration by intRange("BurstDuration", 2..4, 1..20) { cpsMode == "Burst" }
    val burstCooldown by intRange("BurstCooldown", 8..15, 1..60) { cpsMode == "Burst" }
    val burstRampUp by boolean("BurstRampUp", true) { cpsMode == "Burst" }
    val burstRampDown by boolean("BurstRampDown", true) { cpsMode == "Burst" }
    val burstRandomness by float("BurstRandomness", 0.2f, 0f..1f) { cpsMode == "Burst" }

    // ===== Random Mode =====
    val randomMinCPS by int("RandomMinCPS", 5, 1..30) { cpsMode == "Random" }
    val randomMaxCPS by int("RandomMaxCPS", 14, 5..50) { cpsMode == "Random" }
    val randomUpdateInterval by int("RandomUpdateInterval", 8, 1..60) { cpsMode == "Random" }
    val randomDistribution by choices("RandomDistribution", arrayOf(
        "Uniform", "Gaussian", "Exponential", "Beta", "Weibull", "Gamma",
        "Pareto", "LogNormal", "Cauchy", "Laplace", "Rayleigh", "Poisson"
    ), "Uniform") { cpsMode == "Random" }

    // ===== Adaptive Mode =====
    val adaptiveBaseCPS by intRange("AdaptiveBaseCPS", 5..8, 1..30) { cpsMode == "Adaptive" }
    val adaptiveMaxCPS by int("AdaptiveMaxCPS", 16, 8..50) { cpsMode == "Adaptive" }
    val adaptiveLearningRate by float("AdaptiveLearningRate", 0.15f, 0.01f..1f) { cpsMode == "Adaptive" }
    val adaptiveResponseTime by float("AdaptiveResponseTime", 1f, 0.1f..5f) { cpsMode == "Adaptive" }
    val adaptiveSensitivity by float("AdaptiveSensitivity", 0.5f, 0.1f..2f) { cpsMode == "Adaptive" }
    val adaptiveMemory by int("AdaptiveMemory", 20, 5..100) { cpsMode == "Adaptive" }

    // ===== Step Mode =====
    val stepLevels by int("StepLevels", 3, 2..10) { cpsMode == "Step" }
    val stepMinCPS by int("StepMinCPS", 5, 1..20) { cpsMode == "Step" }
    val stepMaxCPS by int("StepMaxCPS", 14, 8..50) { cpsMode == "Step" }
    val stepInterval by intRange("StepInterval", 10..30, 1..100) { cpsMode == "Step" }
    val stepDirection by choices("StepDirection", arrayOf("Up", "Down", "Random", "Bounce"), "Random") { cpsMode == "Step" }
    val stepEasing by float("StepEasing", 0.5f, 0f..1f) { cpsMode == "Step" }

    // ===== Jitter Mode =====
    val jitterBaseCPS by int("JitterBaseCPS", 8, 1..30) { cpsMode == "Jitter" }
    val jitterIntensity by floatRange("JitterIntensity", 0.1f..0.4f, 0f..1f) { cpsMode == "Jitter" }
    val jitterSpeed by floatRange("JitterSpeed", 1f..3f, 0.1f..10f) { cpsMode == "Jitter" }
    val jitterPattern by choices("JitterPattern", arrayOf(
        "WhiteNoise", "PinkNoise", "BrownianNoise", "PerlinNoise",
        "SimplexNoise", "WorleyNoise", "ValueNoise", "GradientNoise"
    ), "PerlinNoise") { cpsMode == "Jitter" }

    // ===== Spike Mode =====
    val spikeBaseCPS by intRange("SpikeBaseCPS", 5..8, 1..30) { cpsMode == "Spike" }
    val spikePeakCPS by int("SpikePeakCPS", 16, 8..50) { cpsMode == "Spike" }
    val spikeChance by float("SpikeChance", 0.12f, 0.01f..0.5f) { cpsMode == "Spike" }
    val spikeDuration by int("SpikeDuration", 2, 1..10) { cpsMode == "Spike" }
    val spikeDecay by float("SpikeDecay", 0.7f, 0.1f..1f) { cpsMode == "Spike" }
    val spikeCluster by boolean("SpikeCluster", false) { cpsMode == "Spike" }
    val spikeClusterSize by int("SpikeClusterSize", 3, 1..8) { cpsMode == "Spike" && spikeCluster }

    // ===== Smart Mode =====
    val smartBaseCPS by intRange("SmartBaseCPS", 5..8, 1..30) { cpsMode == "Smart" }
    val smartMaxCPS by int("SmartMaxCPS", 14, 8..50) { cpsMode == "Smart" }
    val smartConsistency by float("SmartConsistency", 0.7f, 0.1f..1f) { cpsMode == "Smart" }
    val smartReactionTime by float("SmartReactionTime", 0.2f, 0.05f..1f) { cpsMode == "Smart" }
    val smartFatigueRate by float("SmartFatigueRate", 0.008f, 0f..0.05f) { cpsMode == "Smart" }
    val smartRecoveryRate by float("SmartRecoveryRate", 0.04f, 0f..0.1f) { cpsMode == "Smart" }
    val smartWarmupTime by float("SmartWarmupTime", 2f, 0f..10f) { cpsMode == "Smart" }

    // ===== Reactive Mode =====
    val reactiveBaseCPS by intRange("ReactiveBaseCPS", 5..8, 1..30) { cpsMode == "Reactive" }
    val reactiveTriggerCPS by int("ReactiveTriggerCPS", 14, 8..50) { cpsMode == "Reactive" }
    val reactiveTriggerDelay by int("ReactiveTriggerDelay", 3, 0..15) { cpsMode == "Reactive" }
    val reactiveDuration by int("ReactiveDuration", 8, 2..30) { cpsMode == "Reactive" }
    val reactiveThreshold by float("ReactiveThreshold", 0.5f, 0.1f..1f) { cpsMode == "Reactive" }

    // ===== Momentum Mode =====
    val momentumMinCPS by int("MomentumMinCPS", 4, 1..20) { cpsMode == "Momentum" }
    val momentumMaxCPS by int("MomentumMaxCPS", 16, 8..50) { cpsMode == "Momentum" }
    val momentumBuildRate by float("MomentumBuildRate", 0.15f, 0.01f..0.5f) { cpsMode == "Momentum" }
    val momentumDecayRate by float("MomentumDecayRate", 0.08f, 0.01f..0.3f) { cpsMode == "Momentum" }
    val momentumThreshold by float("MomentumThreshold", 0.3f, 0f..0.8f) { cpsMode == "Momentum" }

    // ===== Fatigue Mode =====
    val fatigueMaxCPS by int("FatigueMaxCPS", 14, 8..50) { cpsMode == "Fatigue" }
    val fatigueMinCPS by int("FatigueMinCPS", 4, 1..20) { cpsMode == "Fatigue" }
    val fatigueRate by float("FatigueRate", 0.02f, 0.001f..0.1f) { cpsMode == "Fatigue" }
    val fatigueRecovery by float("FatigueRecovery", 0.01f, 0.001f..0.05f) { cpsMode == "Fatigue" }
    val fatigueFloor by float("FatigueFloor", 0.3f, 0.1f..0.8f) { cpsMode == "Fatigue" }

    // ===== Combo Mode =====
    val comboBaseCPS by int("ComboBaseCPS", 5, 1..20) { cpsMode == "Combo" }
    val comboPerHitCPS by float("ComboPerHitCPS", 0.5f, 0.1f..3f) { cpsMode == "Combo" }
    val comboMaxCPS by int("ComboMaxCPS", 16, 8..50) { cpsMode == "Combo" }
    val comboResetTime by float("ComboResetTime", 2f, 0.5f..10f) { cpsMode == "Combo" }
    val comboDecayRate by float("ComboDecayRate", 0.3f, 0.01f..1f) { cpsMode == "Combo" }

    // ===== Stamina Mode =====
    val staminaMaxCPS by int("StaminaMaxCPS", 14, 8..50) { cpsMode == "Stamina" }
    val staminaMinCPS by int("StaminaMinCPS", 4, 1..20) { cpsMode == "Stamina" }
    val staminaPool by float("StaminaPool", 100f, 10f..500f) { cpsMode == "Stamina" }
    val staminaCostPerClick by float("StaminaCostPerClick", 2f, 0.5f..10f) { cpsMode == "Stamina" }
    val staminaRegenRate by float("StaminaRegenRate", 1.5f, 0.1f..5f) { cpsMode == "Stamina" }

    // ===== CPS Noise Presets (100+) =====
    val cpsNoisePreset = multiChoices("CPSNoisePreset", arrayOf(
        "Custom", "Vanilla", "Legit", "SemiBlatant", "Blatant",
        "AntiML1", "AntiML2", "AntiML3", "AntiML4", "AntiML5",
        "AntiML6", "AntiML7", "AntiML8", "AntiML9", "AntiML10",
        "NeuralBypass", "DeepLearningBypass", "ReinforcementBypass", "GANBypass",
        "TransformerBypass", "LSTMBypass", "GRUBypass", "AttentionBypass",
        "DiffusionBypass", "FlowMatchingBypass", "SSMBypass", "MambaBypass",
        "PerlinNoise", "SimplexNoise", "WorleyNoise", "VoronoiNoise",
        "WhiteNoise", "PinkNoise", "BrownianNoise", "BlueNoise",
        "VioletNoise", "GreyNoise", "RedNoise", "FractalNoise",
        "WaveletNoise", "TurbulenceNoise", "RidgeNoise", "BillowNoise",
        "OrnsteinUhlenbeck", "LevyFlight", "PoissonJitter", "GammaBurst",
        "BetaBlend", "WeibullDrift", "CauchyJump", "StudentTNoise",
        "LogNormalPulse", "ExponentialDecay", "DoubleExponential", "GaussianMixture",
        "LaplaceShock", "VonMisesWrap", "DirichletMix", "MultinomialPick",
        "UniformBand", "TriangularTaper", "PiecewiseLinear", "CubicSpline",
        "BSplineWiggle", "NURBSCurve", "RBFInterpolation", "KNNBlend",
        "SVMDecision", "RandomForestAim", "XGBoostAim", "GradientBoostAim",
        "AdaBoostAim", "BaggingAim", "StackingAim", "VotingAim",
        "BayesianOptAim", "GaussianProcessAim", "MCMCSampleAim", "ParticleFilterAim",
        "KalmanFilterAim", "ExtendedKalmanAim", "UnscentedKalmanAim", "EnsembleKalmanAim",
        "ParticleSwarmAim", "AntColonyAim", "SimulatedAnnealingAim", "GeneticAlgorithmAim",
        "DifferentialEvolutionAim", "CMAESAim", "HillClimbingAim", "RandomSearchAim",
        "GridSearchAim", "BayesianSearchAim", "HyperbandAim", "BOHBAim",
        "MarkovChain", "HiddenMarkov", "MarkovJump", "SemiMarkov",
        "AutoRegressive", "MovingAverage", "ARMA", "ARIMA",
        "GARCH", "EGARCH", "TGARCH", "STGARCH",
        "HurstExponent", "DetrendedFluctuation", "Multifractal", "FractionalBrownian",
        "LongMemory", "ShortMemory", "SelfSimilar", "HeavyTailed",
        "StableDistribution", "TemperedStable", "NormalInverseGaussian", "VarianceGamma",
        "CGMY", "KouJump", "MertonJump", "BatesJump",
        "HestonStochastic", "SVJD", "SABR", "LocalVolatility",
        "ButterflyEffect", "ChaosTheory", "LorenzAttractor", "RosslerAttractor",
        "HenonMap", "LogisticMap", "TentMap", "ArnoldCat",
        "MandelbrotSet", "JuliaSet", "BurningShip", "NewtonFractal",
        "Heartbeat", "Breathing", "EyeBlink", "MuscleTwitch",
        "Tremor", "MicroAdjustment", "FatigueTremor", "AdrenalineRush",
        "ReactionDelay", "Anticipation", "Overcorrection", "Hesitation",
        "RhythmTap", "Metronome", "SwingTiming", "Syncopation",
        "Polyrhythm", "CrossRhythm", "Hemiola", "Tuplet",
        "Accelerando", "Ritardando", "Rubato", "Staccato",
        "Legato", "Marcato", "Tenuto", "Portato"
    ), setOf("Custom")) { true }

    // ===== CPS Noise Parameters =====
    val noiseStrength by floatRange("NoiseStrength", 0f..0.3f, 0f..2f) { true }
    val noiseFrequency by floatRange("NoiseFrequency", 0.5f..2f, 0.01f..10f) { true }
    val noiseSmoothing by floatRange("NoiseSmoothing", 0.5f..0.9f, 0f..1f) { true }
    val noisePhaseShift by floatRange("NoisePhaseShift", 0f..0f, 0f..6.28f) { true }
    val noiseOctaves by intRange("NoiseOctaves", 1..3, 1..8) { true }
    val noisePersistence by floatRange("NoisePersistence", 0.3f..0.7f, 0f..1f) { true }
    val noiseLacunarity by floatRange("NoiseLacunarity", 1.5f..2.5f, 1f..5f) { true }
    val noiseSeed by int("NoiseSeed", 0, 0..99999) { true }

    // ===== Timing Randomization =====
    val timingJitter by floatRange("TimingJitter", 0f..0.05f, 0f..0.5f) { true }
    val timingBurstChance by float("TimingBurstChance", 0.03f, 0f..0.3f) { true }
    val timingBurstAmount by int("TimingBurstAmount", 2, 1..8) { true }
    val timingDrift by float("TimingDrift", 0f, 0f..0.1f) { true }
    val timingLagSpike by float("TimingLagSpike", 0.01f, 0f..0.1f) { true }

    // ===== Internal State =====
    private var noiseTime = 0.0
    private var lastCPSUpdate = 0
    private var currentEffectiveCPS = 5..8
    private var burstState = false
    private var burstTimer = 0
    private var burstCooldownTimer = 0
    private var stepIndex = 0
    private var stepDirectionInternal = 1
    private var spikeActive = false
    private var spikeTimer = 0
    private var spikeClusterCount = 0
    private var smartFatigue = 0.0
    private var smartWarmup = 0.0
    private var reactiveActive = false
    private var reactiveTimer = 0
    private var momentumLevel = 0.0
    private var fatigueLevel = 0.0
    private var comboCount = 0
    private var comboTimer = 0f
    private var staminaCurrent = 0f
    private val noisePermutations = IntArray(512)
    private val adaptiveHistory = mutableListOf<Float>()

    init {
        val seed = noiseSeed
        val random = java.util.Random(seed.toLong())
        for (i in 0 until 256) {
            noisePermutations[i] = i
        }
        for (i in 255 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = noisePermutations[i]
            noisePermutations[i] = noisePermutations[j]
            noisePermutations[j] = temp
        }
        for (i in 0 until 256) {
            noisePermutations[256 + i] = noisePermutations[i]
        }
        staminaCurrent = staminaPool
    }

    val effectiveCPS: IntRange
        get() = currentEffectiveCPS

    fun tickCPS() {
        noiseTime += 0.05

        val baseRange = computeBaseCPS()
        val noised = applyNoise(baseRange)
        val varied = applyVariation(noised)
        currentEffectiveCPS = varied
    }

    private fun computeBaseCPS(): IntRange {
        return when (cpsMode) {
            "Simple" -> cps
            "Dynamic" -> computeDynamicCPS()
            "Wave" -> computeWaveCPS()
            "Burst" -> computeBurstCPS()
            "Random" -> computeRandomCPS()
            "Adaptive" -> computeAdaptiveCPS()
            "Step" -> computeStepCPS()
            "Jitter" -> computeJitterCPS()
            "Spike" -> computeSpikeCPS()
            "Smart" -> computeSmartCPS()
            "Reactive" -> computeReactiveCPS()
            "Momentum" -> computeMomentumCPS()
            "Fatigue" -> computeFatigueCPS()
            "Combo" -> computeComboCPS()
            "Stamina" -> computeStaminaCPS()
            else -> cps
        }
    }

    private fun computeDynamicCPS(): IntRange {
        val player = mc.thePlayer
        val base = dynamicBaseCPS.first + (dynamicBaseCPS.last - dynamicBaseCPS.first) / 2
        var extra = 0f

        if (player != null) {
            val speed = (player.motionX * player.motionX + player.motionZ * player.motionZ).toFloat()
            extra += speed * dynamicSpeedFactor

            val target = KillAura.target
            if (target != null) {
                val dist = player.getDistanceToEntity(target)
                extra += (1f / (dist + 0.5f)) * dynamicDistanceFactor * 3f

                val healthPercent = target.health / target.maxHealth
                extra += (1f - healthPercent) * dynamicHealthFactor * 5f

                extra += (target.hurtTime / 10f) * dynamicHurtFactor * 3f
            }
        }

        extra += (comboCount * dynamicComboFactor).toFloat()
        extra = extra.coerceIn(0f, dynamicMaxExtraCPS.toFloat())
        val center = base + extra
        val spread = (dynamicBaseCPS.last - dynamicBaseCPS.first) / 2
        return (center - spread).roundToInt().coerceAtLeast(1)..(center + spread).roundToInt().coerceAtMost(50)
    }

    private fun computeWaveCPS(): IntRange {
        val phase = noiseTime * waveFrequency * 2.0 * PI + wavePhaseOffset
        val value = when (waveType) {
            "Sine" -> sin(phase)
            "Triangle" -> 2.0 / PI * asin(sin(phase))
            "Sawtooth" -> 2.0 * (phase / (2.0 * PI) - floor(phase / (2.0 * PI) + 0.5))
            "Square" -> sin(phase).coerceIn(-0.999, 0.999).let { if (it >= 0) 1.0 else -1.0 }
            "Custom" -> {
                var sum = 0.0
                for (h in 1..waveHarmonics) {
                    sum += sin(phase * h) * waveHarmonicDecay.pow(h - 1)
                }
                sum / (1..waveHarmonics).sumOf { waveHarmonicDecay.pow(it - 1).toDouble() }
            }
            else -> sin(phase)
        }
        val center = waveBaseCPS.toDouble()
        val amplitude = waveAmplitude.toDouble()
        val cps = (center + value * amplitude).roundToInt().coerceIn(1, 50)
        return cps..cps
    }

    private fun computeBurstCPS(): IntRange {
        if (burstState) {
            burstTimer--
            if (burstTimer <= 0) {
                burstState = false
                burstCooldownTimer = burstCooldown.random()
            }
            val progress = if (burstCooldown.random() > 0) {
                burstTimer.toFloat() / burstDuration.random()
            } else 0f
            val cps = if (burstRampDown) {
                (burstPeakCPS - (burstPeakCPS - burstBaseCPS) * (1f - progress)).roundToInt()
            } else {
                burstPeakCPS
            }
            return (cps - burstBaseCPS / 2).coerceAtLeast(1)..cps
        } else {
            burstCooldownTimer--
            if (burstCooldownTimer <= 0) {
                burstState = true
                burstTimer = burstDuration.random()
            }
            return burstBaseCPS..(burstBaseCPS + 1)
        }
    }

    private fun computeRandomCPS(): IntRange {
        if (runTimeTicks - lastCPSUpdate >= randomUpdateInterval) {
            lastCPSUpdate = runTimeTicks
        }
        val range = randomMaxCPS - randomMinCPS
        val value = when (randomDistribution) {
            "Gaussian" -> {
                val g = nextGaussian()
                (randomMinCPS + range / 2 + (g * range / 4).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "Exponential" -> {
                val lambda = 1.0 / (range / 3.0)
                (randomMinCPS + (-ln(Math.random()) / lambda).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "Beta" -> {
                val alpha = 2.0; val beta = 5.0
                val x = nextBeta(alpha, beta)
                (randomMinCPS + (x * range).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "Weibull" -> {
                val u = Math.random()
                (randomMinCPS + (range * (-ln(1 - u)).pow(0.5)).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "Gamma" -> {
                var sum = 0.0; repeat(4) { sum += -ln(Math.random()) }
                (randomMinCPS + (sum / 4 * range / 3).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "Pareto" -> {
                val u = Math.random()
                (randomMinCPS + (1.0 / u.pow(0.3) - 1).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "LogNormal" -> {
                val g = nextGaussian()
                (randomMinCPS + (exp(g * 0.5) * range / 4).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "Cauchy" -> {
                val u = Math.random()
                (randomMinCPS + (tan(PI * (u - 0.5)) * range / 4).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "Laplace" -> {
                val u = Math.random() - 0.5
                val v = -sign(u) * ln(1 - 2 * abs(u))
                (randomMinCPS + range / 2 + (v * range / 4).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "Rayleigh" -> {
                val u = Math.random()
                (randomMinCPS + (sqrt(-2 * ln(1 - u)) * range / 3).roundToInt()).coerceIn(randomMinCPS, randomMaxCPS)
            }
            "Poisson" -> {
                val lambda = (randomMinCPS + randomMaxCPS) / 2.0
                var l = exp(-lambda); var k = 0; var p = 1.0
                while (p > l) { k++; p *= Math.random() }
                (k - 1).coerceIn(randomMinCPS, randomMaxCPS)
            }
            else -> nextInt(randomMinCPS, randomMaxCPS + 1)
        }
        return value..value
    }

    private fun computeAdaptiveCPS(): IntRange {
        val player = mc.thePlayer ?: return adaptiveBaseCPS
        val target = KillAura.target
        if (target != null) {
            val dist = player.getDistanceToEntity(target)
            adaptiveHistory.add(dist)
            if (adaptiveHistory.size > adaptiveMemory) adaptiveHistory.removeAt(0)
        }
        if (adaptiveHistory.isEmpty()) return adaptiveBaseCPS
        val avgDist = adaptiveHistory.average().toFloat()
        val distVariance = adaptiveHistory.map { (it - avgDist).pow(2) }.average().toFloat()
        val sensitivity = adaptiveSensitivity * (1f + distVariance / 10f)
        val range = adaptiveMaxCPS - adaptiveBaseCPS.first
        val extra = (sensitivity * adaptiveLearningRate * range).roundToInt()
        return adaptiveBaseCPS.first..(adaptiveBaseCPS.last + extra).coerceAtMost(adaptiveMaxCPS)
    }

    private fun computeStepCPS(): IntRange {
        if (runTimeTicks - lastCPSUpdate >= stepInterval.random()) {
            lastCPSUpdate = runTimeTicks
            when (stepDirection) {
                "Up" -> stepIndex = (stepIndex + 1) % stepLevels
                "Down" -> stepIndex = (stepIndex - 1 + stepLevels) % stepLevels
                "Random" -> stepIndex = nextInt(0, stepLevels)
                "Bounce" -> {
                    stepIndex += stepDirectionInternal
                    if (stepIndex >= stepLevels - 1 || stepIndex <= 0) stepDirectionInternal *= -1
                    stepIndex = stepIndex.coerceIn(0, stepLevels - 1)
                }
            }
        }
        val range = stepMaxCPS - stepMinCPS
        val stepSize = range.toFloat() / (stepLevels - 1).coerceAtLeast(1)
        val cps = (stepMinCPS + stepSize * stepIndex).roundToInt()
        val prevCPS = (stepMinCPS + stepSize * (stepIndex - 1).coerceAtLeast(0)).roundToInt()
        val eased = (prevCPS + (cps - prevCPS) * stepEasing).roundToInt()
        return eased..eased
    }

    private fun computeJitterCPS(): IntRange {
        val noiseVal = when (jitterPattern) {
            "WhiteNoise" -> Math.random() * 2 - 1
            "PinkNoise" -> pinkNoise(noiseTime)
            "BrownianNoise" -> brownianNoise(noiseTime)
            "PerlinNoise" -> perlinNoise(noiseTime * jitterSpeed.random(), 0.0)
            "SimplexNoise" -> simplexNoise(noiseTime * jitterSpeed.random(), 0.0)
            "WorleyNoise" -> worleyNoise(noiseTime * jitterSpeed.random(), 0.0)
            "ValueNoise" -> valueNoise(noiseTime * jitterSpeed.random(), 0.0)
            "GradientNoise" -> gradientNoise(noiseTime * jitterSpeed.random(), 0.0)
            else -> Math.random() * 2 - 1
        }
        val intensity = jitterIntensity.random()
        val cps = (jitterBaseCPS + (noiseVal * intensity.toDouble() * jitterBaseCPS)).roundToInt().coerceIn(1, 50)
        return cps..cps
    }

    private fun computeSpikeCPS(): IntRange {
        if (spikeActive) {
            spikeTimer--
            if (spikeTimer <= 0) {
                spikeActive = false
                if (spikeCluster && spikeClusterCount < spikeClusterSize && Math.random() < 0.6) {
                    spikeActive = true
                    spikeTimer = spikeDuration
                    spikeClusterCount++
                } else {
                    spikeClusterCount = 0
                }
            }
            val progress = 1f - spikeTimer.toFloat() / spikeDuration
            val cps = (spikePeakCPS - (spikePeakCPS - spikeBaseCPS.last) * progress * spikeDecay).roundToInt()
            return spikeBaseCPS.first..cps
        } else {
            if (Math.random() < spikeChance) {
                spikeActive = true
                spikeTimer = spikeDuration
                spikeClusterCount = 1
            }
            return spikeBaseCPS
        }
    }

    private fun computeSmartCPS(): IntRange {
        val player = mc.thePlayer ?: return smartBaseCPS
        val target = KillAura.target
        if (target != null && smartWarmup < smartWarmupTime) {
            smartWarmup += 0.05
        }
        if (target == null) {
            smartWarmup = (smartWarmup - 0.02).coerceAtLeast(0.0)
        }
        val warmupFactor = (smartWarmup / smartWarmupTime).coerceIn(0.0, 1.0)

        if (target != null) {
            smartFatigue = (smartFatigue + smartFatigueRate).coerceAtMost(1.0)
        } else {
            smartFatigue = (smartFatigue - smartRecoveryRate).coerceAtLeast(0.0)
        }

        val fatigueFactor = 1.0 - smartFatigue * smartConsistency
        val range = smartMaxCPS - smartBaseCPS.first
        val maxCPS = (smartBaseCPS.first + range * warmupFactor * fatigueFactor).roundToInt()
        return smartBaseCPS.first..maxCPS.coerceIn(smartBaseCPS.first, smartMaxCPS)
    }

    private fun computeReactiveCPS(): IntRange {
        val target = KillAura.target
        if (target != null && !reactiveActive) {
            val healthPct = target.health / target.maxHealth
            if (healthPct < reactiveThreshold) {
                reactiveActive = true
                reactiveTimer = reactiveDuration
            }
        }
        if (reactiveActive) {
            reactiveTimer--
            if (reactiveTimer <= 0) reactiveActive = false
        }
        return if (reactiveActive) {
            val progress = reactiveTimer.toFloat() / reactiveDuration
            val cps = (reactiveTriggerCPS - (reactiveTriggerCPS - reactiveBaseCPS.last) * (1f - progress)).roundToInt()
            reactiveBaseCPS.first..cps
        } else {
            reactiveBaseCPS
        }
    }

    private fun computeMomentumCPS(): IntRange {
        val target = KillAura.target
        if (target != null) {
            momentumLevel = (momentumLevel + momentumBuildRate).coerceAtMost(1.0)
        } else {
            momentumLevel = (momentumLevel - momentumDecayRate).coerceAtLeast(0.0)
        }
        if (momentumLevel < momentumThreshold) return momentumMinCPS..momentumMinCPS
        val range = momentumMaxCPS - momentumMinCPS
        val cps = (momentumMinCPS + range * momentumLevel).roundToInt()
        return (cps - 2).coerceAtLeast(1)..cps
    }

    private fun computeFatigueCPS(): IntRange {
        val target = KillAura.target
        if (target != null) {
            fatigueLevel = (fatigueLevel + fatigueRate).coerceAtMost(1.0)
        } else {
            fatigueLevel = (fatigueLevel - fatigueRecovery).coerceAtLeast(0.0)
        }
        val range = fatigueMaxCPS - fatigueMinCPS
        val floorRange = (range * fatigueFloor).roundToInt()
        val cps = fatigueMaxCPS - (range * fatigueLevel).roundToInt()
        return (cps - floorRange).coerceAtLeast(fatigueMinCPS)..cps.coerceAtLeast(fatigueMinCPS)
    }

    private fun computeComboCPS(): IntRange {
        val target = KillAura.target
        if (target != null) {
            comboTimer = 0f
        } else {
            comboTimer += 0.05f
            if (comboTimer >= comboResetTime) {
                comboCount = (comboCount - comboDecayRate).roundToInt().coerceAtLeast(0)
            }
        }
        val cps = (comboBaseCPS + comboCount * comboPerHitCPS).roundToInt().coerceIn(comboBaseCPS, comboMaxCPS)
        return cps..cps
    }

    private fun computeStaminaCPS(): IntRange {
        val target = KillAura.target
        if (target != null) {
            staminaCurrent = (staminaCurrent - staminaCostPerClick).coerceAtLeast(0f)
        } else {
            staminaCurrent = (staminaCurrent + staminaRegenRate).coerceAtMost(staminaPool)
        }
        val staminaRatio = staminaCurrent / staminaPool
        val range = staminaMaxCPS - staminaMinCPS
        val cps = (staminaMinCPS + range * staminaRatio).roundToInt()
        return cps..cps
    }

    fun onHit() {
        comboCount++
        comboTimer = 0f
    }

    fun onMiss() {
        comboCount = (comboCount - 1).coerceAtLeast(0)
    }

    // ===== Noise Application =====
    private fun applyNoise(range: IntRange): IntRange {
        if (cpsNoisePreset.contains("Custom") || cpsNoisePreset.get().isEmpty() || cpsNoisePreset.get().size == 1 && cpsNoisePreset.contains("Custom")) {
            val strength = noiseStrength.random()
            val freq = noiseFrequency.random()
            val smooth = noiseSmoothing.random()
            val phase = noisePhaseShift.random()

            val noiseVal = when {
                cpsNoisePreset.contains("PerlinNoise") || cpsNoisePreset.contains("Custom") ->
                    perlinNoise(noiseTime * freq + phase, 0.0)
                cpsNoisePreset.contains("SimplexNoise") ->
                    simplexNoise(noiseTime * freq + phase, 0.0)
                cpsNoisePreset.contains("WhiteNoise") ->
                    Math.random() * 2 - 1
                cpsNoisePreset.contains("PinkNoise") ->
                    pinkNoise(noiseTime * freq)
                cpsNoisePreset.contains("BrownianNoise") ->
                    brownianNoise(noiseTime * freq)
                else -> perlinNoise(noiseTime * freq + phase, 0.0)
            }

            val noiseOffset = (noiseVal * strength.toDouble() * (range.last - range.first)).roundToInt()
            val min = (range.first + noiseOffset).coerceAtLeast(1)
            val max = (range.last + noiseOffset).coerceAtLeast(min)
            return min..max
        }
        return range
    }

    private fun applyVariation(range: IntRange): IntRange {
        var min = range.first
        var max = range.last

        val variation = cpsVariation.random()
        if (variation > 0f) {
            val offset = (variation * (max - min)).roundToInt()
            min = (min - offset / 2).coerceAtLeast(1)
            max = (max + offset / 2).coerceAtMost(50)
        }

        if (Math.random() < cpsDropChance) {
            val drop = (cpsDropAmount * (max - min)).roundToInt()
            max = (max - drop).coerceAtLeast(min)
        }

        if (Math.random() < cpsSpikeChance) {
            val spike = (cpsSpikeAmount * (max - min)).roundToInt()
            max = (max + spike).coerceAtMost(50)
        }

        return min..max
    }

    // ===== Noise Functions =====
    private fun fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

    private fun lerp(a: Double, b: Double, t: Double): Double = a + t * (b - a)

    private fun grad(hash: Int, x: Double, y: Double): Double {
        val h = hash and 3
        val u = if (h and 2 == 0) x else -x
        val v = if (h and 1 == 0) y else -y
        return u + v
    }

    private fun perlinNoise(x: Double, y: Double): Double {
        val xi = x.toInt() and 255
        val yi = y.toInt() and 255
        val xf = x - x.toInt()
        val yf = y - y.toInt()
        val u = fade(xf)
        val v = fade(yf)
        val aa = noisePermutations[noisePermutations[xi] + yi]
        val ab = noisePermutations[noisePermutations[xi] + yi + 1]
        val ba = noisePermutations[noisePermutations[xi + 1] + yi]
        val bb = noisePermutations[noisePermutations[xi + 1] + yi + 1]
        return lerp(
            lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u),
            lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u), v
        )
    }

    private fun simplexNoise(x: Double, y: Double): Double {
        val f2 = 0.5 * (sqrt(3.0) - 1.0)
        val s = (x + y) * f2
        val i = (x + s).toInt()
        val j = (y + s).toInt()
        val g2 = (3.0 - sqrt(3.0)) / 6.0
        val t = (i + j) * g2
        val x0 = i - t
        val y0 = j - t
        val dx0 = x - x0
        val dy0 = y - y0
        return (1.0 - dx0 * dx0 - dy0 * dy0).coerceAtLeast(0.0) * 2.0 - 1.0
    }

    private var brownianValue = 0.0
    private fun brownianNoise(dt: Double): Double {
        brownianValue += (Math.random() - 0.5) * 2.0 * dt
        brownianValue = brownianValue.coerceIn(-1.0, 1.0)
        return brownianValue
    }

    private val pinkNoiseState = DoubleArray(7)
    private fun pinkNoise(dt: Double): Double {
        val white = Math.random() * 2 - 1
        pinkNoiseState[0] = 0.99886 * pinkNoiseState[0] + white * 0.0555179
        pinkNoiseState[1] = 0.99332 * pinkNoiseState[1] + white * 0.0750759
        pinkNoiseState[2] = 0.96900 * pinkNoiseState[2] + white * 0.1538520
        pinkNoiseState[3] = 0.86650 * pinkNoiseState[3] + white * 0.3104856
        pinkNoiseState[4] = 0.55000 * pinkNoiseState[4] + white * 0.5329522
        pinkNoiseState[5] = -0.7616 * pinkNoiseState[5] - white * 0.0168980
        var pink = pinkNoiseState[0] + pinkNoiseState[1] + pinkNoiseState[2] + pinkNoiseState[3] + pinkNoiseState[4] + pinkNoiseState[5] + pinkNoiseState[6] + white * 0.5362
        pinkNoiseState[6] = white * 0.115926
        pink *= 0.11
        return pink.coerceIn(-1.0, 1.0)
    }

    private fun worleyNoise(x: Double, y: Double): Double {
        val cx = x.toInt()
        val cy = y.toInt()
        var minDist = Double.MAX_VALUE
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = cx + dx
                val ny = cy + dy
                val hash = noisePermutations[(noisePermutations[nx and 255] + ny) and 255]
                val px = nx + (hash / 256.0)
                val py = ny + ((hash * 7) % 256 / 256.0)
                val dist = sqrt((x - px) * (x - px) + (y - py) * (y - py))
                if (dist < minDist) minDist = dist
            }
        }
        return (minDist * 2.0 - 1.0).coerceIn(-1.0, 1.0)
    }

    private fun valueNoise(x: Double, y: Double): Double {
        val xi = x.toInt() and 255
        val yi = y.toInt() and 255
        val xf = x - x.toInt()
        val yf = y - y.toInt()
        val u = fade(xf)
        val v = fade(yf)
        val a = noisePermutations[noisePermutations[xi] + yi] / 256.0
        val b = noisePermutations[noisePermutations[xi + 1] + yi] / 256.0
        val c = noisePermutations[noisePermutations[xi] + yi + 1] / 256.0
        val d = noisePermutations[noisePermutations[xi + 1] + yi + 1] / 256.0
        return lerp(lerp(a, b, u), lerp(c, d, u), v) * 2.0 - 1.0
    }

    private fun gradientNoise(x: Double, y: Double): Double {
        return (perlinNoise(x, y) + simplexNoise(x, y)) * 0.5
    }

    private var nextGaussianHasNext = false
    private var nextGaussianNextValue = 0.0

    private fun nextGaussian(): Double {
        if (nextGaussianHasNext) {
            nextGaussianHasNext = false
            return nextGaussianNextValue
        }
        var v1: Double
        var v2: Double
        var s: Double
        do {
            v1 = Math.random() * 2 - 1
            v2 = Math.random() * 2 - 1
            s = v1 * v1 + v2 * v2
        } while (s >= 1 || s == 0.0)
        val multiplier = sqrt(-2 * ln(s) / s)
        nextGaussianNextValue = v2 * multiplier
        nextGaussianHasNext = true
        return v1 * multiplier
    }

    private fun nextBeta(alpha: Double, beta: Double): Double {
        val x = if (alpha <= 1) {
            val u = Math.random()
            u.pow(1.0 / alpha)
        } else {
            var xVal: Double
            do {
                val y = -ln(Math.random())
                val z = Math.random()
                xVal = if (y < 1) y.pow(1.0 / (alpha - 1)) else 1.0
            } while (z > (1 + (alpha - 1) * ln(xVal) - (alpha - 1) * (xVal - 1)))
            xVal
        }
        val y = if (beta <= 1) {
            val u = Math.random()
            u.pow(1.0 / beta)
        } else {
            var yVal: Double
            do {
                val w = -ln(Math.random())
                val z = Math.random()
                yVal = if (w < 1) w.pow(1.0 / (beta - 1)) else 1.0
            } while (z > (1 + (beta - 1) * ln(yVal) - (beta - 1) * (yVal - 1)))
            yVal
        }
        return x / (x + y)
    }
}