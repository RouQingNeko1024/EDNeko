package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import kotlin.math.*
import kotlin.random.Random

typealias NoiseApplier = (Rotation) -> Rotation

object NoisePresets {
    private var tickCounter = 0L
    private val noiseHistory = mutableMapOf<String, DoubleArray>()
    private val random = Random(System.nanoTime())

    fun tick() { tickCounter++ }

    private fun nextGaussian(): Double {
        val u1 = random.nextDouble()
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
    }

    private fun gaussianNoise(mean: Double = 0.0, std: Double = 1.0): Double {
        return mean + std * nextGaussian()
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    fun getPreset(name: String): NoiseApplier = when (name) {
        "Custom" -> { r -> r }
        "Vanilla" -> vanillaPreset
        "Legit" -> legitPreset
        "SemiBlatant" -> semiBlatantPreset
        "Blatant" -> blatantPreset
        "AntiML1" -> antiML1Preset
        "AntiML2" -> antiML2Preset
        "AntiML3" -> antiML3Preset
        "AntiML4" -> antiML4Preset
        "AntiML5" -> antiML5Preset
        "AntiML6" -> antiML6Preset
        "AntiML7" -> antiML7Preset
        "AntiML8" -> antiML8Preset
        "AntiML9" -> antiML9Preset
        "AntiML10" -> antiML10Preset
        "NeuralBypass" -> neuralBypassPreset
        "DeepLearningBypass" -> deepLearningBypassPreset
        "ReinforcementBypass" -> reinforcementBypassPreset
        "GANBypass" -> ganBypassPreset
        "TransformerBypass" -> transformerBypassPreset
        "LSTMBypass" -> lstmBypassPreset
        "GRUBypass" -> gruBypassPreset
        "AttentionBypass" -> attentionBypassPreset
        "DiffusionBypass" -> diffusionBypassPreset
        "FlowMatchingBypass" -> flowMatchingBypassPreset
        "SSMBypass" -> ssmBypassPreset
        "MambaBypass" -> mambaBypassPreset
        "RWKVBypass" -> rwkvBypassPreset
        "JambaBypass" -> jambaBypassPreset
        "MixtralBypass" -> mixtralBypassPreset
        "MoEBypass" -> moeBypassPreset
        "ChaosMax" -> chaosMaxPreset
        "MicroJitter" -> microJitterPreset
        "MacroSim" -> macroSimPreset
        "HumanSim" -> humanSimPreset
        "RobotSim" -> robotSimPreset
        "Butterfly" -> butterflyPreset
        "SineWave" -> sineWavePreset
        "Sawtooth" -> sawtoothPreset
        "SquareWave" -> squareWavePreset
        "TriangleWave" -> triangleWavePreset
        "PulseJitter" -> pulseJitterPreset
        "FractalNoise" -> fractalNoisePreset
        "WaveletNoise" -> waveletNoisePreset
        "OrnsteinUhlenbeck" -> ornsteinUhlenbeckPreset
        "LevyFlight" -> levyFlightPreset
        "PoissonJitter" -> poissonJitterPreset
        "GammaBurst" -> gammaBurstPreset
        "BetaBlend" -> betaBlendPreset
        "WeibullDrift" -> weibullDriftPreset
        "CauchyJump" -> cauchyJumpPreset
        "StudentTNoise" -> studentTNoisePreset
        "LogNormalPulse" -> logNormalPulsePreset
        "ExponentialDecay" -> exponentialDecayPreset
        "DoubleExponential" -> doubleExponentialPreset
        "GaussianMixture" -> gaussianMixturePreset
        "LaplaceShock" -> laplaceShockPreset
        "VonMisesWrap" -> vonMisesWrapPreset
        "DirichletMix" -> dirichletMixPreset
        "MultinomialPick" -> multinomialPickPreset
        "UniformBand" -> uniformBandPreset
        "TriangularTaper" -> triangularTaperPreset
        "PiecewiseLinear" -> piecewiseLinearPreset
        "CubicSpline" -> cubicSplinePreset
        "BSplineWiggle" -> bSplineWigglePreset
        "NURBSCurve" -> nurbsCurvePreset
        "RBFInterpolation" -> rbfInterpolationPreset
        "KNNBlend" -> knnBlendPreset
        "SVMDecision" -> svmDecisionPreset
        "RandomForestAim" -> randomForestAimPreset
        "XGBoostAim" -> xgboostAimPreset
        "GradientBoostAim" -> gradientBoostAimPreset
        "AdaBoostAim" -> adaBoostAimPreset
        "BaggingAim" -> baggingAimPreset
        "StackingAim" -> stackingAimPreset
        "VotingAim" -> votingAimPreset
        "BayesianOptAim" -> bayesianOptAimPreset
        "GaussianProcessAim" -> gaussianProcessAimPreset
        "MCMCSampleAim" -> mcmcSampleAimPreset
        "ParticleFilterAim" -> particleFilterAimPreset
        "KalmanFilterAim" -> kalmanFilterAimPreset
        "ExtendedKalmanAim" -> extendedKalmanAimPreset
        "UnscentedKalmanAim" -> unscentedKalmanAimPreset
        "EnsembleKalmanAim" -> ensembleKalmanAimPreset
        "ParticleSwarmAim" -> particleSwarmAimPreset
        "AntColonyAim" -> antColonyAimPreset
        "SimulatedAnnealingAim" -> simulatedAnnealingAimPreset
        "GeneticAlgorithmAim" -> geneticAlgorithmAimPreset
        "DifferentialEvolutionAim" -> differentialEvolutionAimPreset
        "CMAESAim" -> cmaesAimPreset
        "HillClimbingAim" -> hillClimbingAimPreset
        "RandomSearchAim" -> randomSearchAimPreset
        "GridSearchAim" -> gridSearchAimPreset
        "BayesianSearchAim" -> bayesianSearchAimPreset
        "HyperbandAim" -> hyperbandAimPreset
        "BOHBAim" -> bohbAimPreset
        else -> { r -> r }
    }

    private val vanillaPreset: NoiseApplier = { r ->
        val yawJitter = (random.nextFloat() - 0.5f) * 0.05f
        val pitchJitter = (random.nextFloat() - 0.5f) * 0.03f
        Rotation(r.yaw + yawJitter, r.pitch + pitchJitter)
    }

    private val legitPreset: NoiseApplier = { r ->
        val yawJitter = (random.nextFloat() - 0.5f) * 0.3f
        val pitchJitter = (random.nextFloat() - 0.5f) * 0.15f
        val microAdj = sin(tickCounter * 0.1f) * 0.05f
        Rotation(r.yaw + yawJitter + microAdj, r.pitch + pitchJitter)
    }

    private val semiBlatantPreset: NoiseApplier = { r ->
        val yawJitter = (random.nextFloat() - 0.5f) * 1.5f
        val pitchJitter = (random.nextFloat() - 0.5f) * 0.8f
        Rotation(r.yaw + yawJitter, r.pitch + pitchJitter)
    }

    private val blatantPreset: NoiseApplier = { r ->
        val yawJitter = (random.nextFloat() - 0.5f) * 5f
        val pitchJitter = (random.nextFloat() - 0.5f) * 3f
        Rotation(r.yaw + yawJitter, r.pitch + pitchJitter)
    }

    private val antiML1Preset: NoiseApplier = { r ->
        val adversarial = (random.nextFloat() - 0.5f) * 0.15f
        val temporal = sin(tickCounter * 0.05f + random.nextDouble() * PI) * 0.1f
        val freqNoise = cos(tickCounter * 0.13f) * 0.08f
        Rotation(r.yaw + adversarial + temporal + freqNoise, r.pitch + adversarial * 0.4f + temporal * 0.5f)
    }

    private val antiML2Preset: NoiseApplier = { r ->
        val pgdNoise = (random.nextFloat() - 0.5f) * 0.12f
        val fgsmSign = if (random.nextBoolean()) 1f else -1f
        val fgsmNoise = fgsmSign * 0.08f
        val cwNoise = gaussianNoise(0.0, 0.06)
        Rotation(r.yaw + pgdNoise + fgsmNoise, r.pitch + cwNoise.toFloat() + pgdNoise * 0.5f)
    }

    private val antiML3Preset: NoiseApplier = { r ->
        val deepFool = (random.nextFloat() - 0.5f) * 0.2f
        val autoAttack = sin(tickCounter * 0.07f) * 0.1f
        val boundaryNoise = cos(tickCounter * 0.11f) * 0.12f
        Rotation(r.yaw + deepFool + autoAttack, r.pitch + deepFool * 0.6f + boundaryNoise)
    }

    private val antiML4Preset: NoiseApplier = { r ->
        val featureSqueeze = (random.nextFloat().roundToInt() % 4).toFloat() * 0.04f
        val magnetNoise = gaussianNoise(0.0, 0.05)
        val gaussSmooth = gaussianNoise(0.0, 0.03)
        Rotation(r.yaw + featureSqueeze + magnetNoise.toFloat(), r.pitch + gaussSmooth.toFloat() + featureSqueeze * 0.5f)
    }

    private val antiML5Preset: NoiseApplier = { r ->
        val mixup = sin(tickCounter * 0.03f) * 0.08f
        val cutMix = cos(tickCounter * 0.09f) * 0.06f
        val labelSmooth = (random.nextFloat() - 0.5f) * 0.05f
        val vat = gaussianNoise(0.0, 0.04)
        Rotation(r.yaw + mixup + cutMix + labelSmooth, r.pitch + vat.toFloat() + mixup * 0.3f)
    }

    private val antiML6Preset: NoiseApplier = { r ->
        val temporalCorr = sin(tickCounter * 0.04f) * 0.1f
        val autocorr = cos(tickCounter * 0.06f) * 0.08f
        val spectralDensity = sin(tickCounter * 0.12f) * 0.07f
        val fourier = cos(tickCounter * 0.15f) * 0.06f
        Rotation(r.yaw + temporalCorr + autocorr + spectralDensity, r.pitch + fourier + temporalCorr * 0.5f)
    }

    private val antiML7Preset: NoiseApplier = { r ->
        val blackBox = (random.nextFloat() - 0.5f) * 0.18f
        val evasion = gaussianNoise(0.0, 0.07)
        val poisoning = if (random.nextFloat() < 0.03f) (random.nextFloat() - 0.5f) * 0.5f else 0f
        val backdoor = if (random.nextFloat() < 0.02f) (random.nextFloat() - 0.5f) * 0.8f else 0f
        Rotation(r.yaw + blackBox + evasion.toFloat() + poisoning + backdoor,
            r.pitch + blackBox * 0.5f + evasion.toFloat() * 0.4f)
    }

    private val antiML8Preset: NoiseApplier = { r ->
        val modelExtract = sin(tickCounter * 0.08f) * 0.1f
        val membershipInf = cos(tickCounter * 0.14f) * 0.08f
        val modelInversion = gaussianNoise(0.0, 0.06)
        val attrInference = (random.nextFloat() - 0.5f) * 0.07f
        Rotation(r.yaw + modelExtract + membershipInf, r.pitch + modelInversion.toFloat() + attrInference)
    }

    private val antiML9Preset: NoiseApplier = { r ->
        val ganLatent = gaussianNoise(0.0, 0.08)
        val vaeKLD = sin(tickCounter * 0.05f) * 0.09f
        val transformerAttn = cos(tickCounter * 0.07f) * 0.07f
        val rlEntropy = (random.nextFloat() - 0.5f) * 0.06f
        Rotation(r.yaw + ganLatent.toFloat() + vaeKLD + rlEntropy,
            r.pitch + transformerAttn + ganLatent.toFloat() * 0.3f)
    }

    private val antiML10Preset: NoiseApplier = { r ->
        val dpEpsilon = gaussianNoise(0.0, 0.05)
        val federated = sin(tickCounter * 0.06f) * 0.08f
        val nasSearch = cos(tickCounter * 0.1f) * 0.07f
        val kdTemp = (random.nextFloat() - 0.5f) * 0.06f
        val pruning = (random.nextFloat() - 0.5f) * 0.05f
        val quantize = (random.nextFloat().roundToInt() % 8).toFloat() * 0.02f
        Rotation(r.yaw + dpEpsilon.toFloat() + federated + nasSearch + kdTemp + pruning + quantize,
            r.pitch + dpEpsilon.toFloat() * 0.5f + federated * 0.4f + kdTemp * 0.3f)
    }

    private val neuralBypassPreset: NoiseApplier = { r ->
        val nnObfuscation = sin(tickCounter * 0.04f) * 0.15f
        val patternBreak = if (random.nextFloat() < 0.05f) (random.nextFloat() - 0.5f) * 0.8f else 0f
        val entropy = cos(tickCounter * 0.09f) * 0.12f
        Rotation(r.yaw + nnObfuscation + patternBreak + entropy,
            r.pitch + nnObfuscation * 0.6f + entropy * 0.4f)
    }

    private val deepLearningBypassPreset: NoiseApplier = { r ->
        val dlNoise = gaussianNoise(0.0, 0.1)
        val gradPerturb = (random.nextFloat() - 0.5f) * 0.15f
        val weightNoise = sin(tickCounter * 0.05f) * 0.08f
        Rotation(r.yaw + dlNoise.toFloat() + gradPerturb + weightNoise,
            r.pitch + dlNoise.toFloat() * 0.5f + gradPerturb * 0.4f)
    }

    private val reinforcementBypassPreset: NoiseApplier = { r ->
        val rlExploration = (random.nextFloat() - 0.5f) * 0.2f
        val rewardNoise = gaussianNoise(0.0, 0.08)
        val policyNoise = sin(tickCounter * 0.06f) * 0.1f
        Rotation(r.yaw + rlExploration + rewardNoise.toFloat() + policyNoise,
            r.pitch + rlExploration * 0.5f + policyNoise * 0.6f)
    }

    private val ganBypassPreset: NoiseApplier = { r ->
        val genNoise = gaussianNoise(0.0, 0.12)
        val discNoise = (random.nextFloat() - 0.5f) * 0.15f
        val latentNoise = sin(tickCounter * 0.07f) * 0.1f
        Rotation(r.yaw + genNoise.toFloat() + discNoise + latentNoise,
            r.pitch + genNoise.toFloat() * 0.4f + latentNoise * 0.5f)
    }

    private val transformerBypassPreset: NoiseApplier = { r ->
        val attnNoise = sin(tickCounter * 0.05f) * 0.12f
        val posEncNoise = cos(tickCounter * 0.08f) * 0.1f
        val normNoise = gaussianNoise(0.0, 0.06)
        val dropoutNoise = if (random.nextFloat() < 0.1f) 0f else (random.nextFloat() - 0.5f) * 0.08f
        Rotation(r.yaw + attnNoise + posEncNoise + normNoise.toFloat() + dropoutNoise,
            r.pitch + attnNoise * 0.5f + posEncNoise * 0.6f)
    }

    private val lstmBypassPreset: NoiseApplier = { r ->
        val hiddenNoise = gaussianNoise(0.0, 0.08)
        val cellNoise = sin(tickCounter * 0.04f) * 0.1f
        val forgetNoise = cos(tickCounter * 0.09f) * 0.07f
        Rotation(r.yaw + hiddenNoise.toFloat() + cellNoise + forgetNoise,
            r.pitch + hiddenNoise.toFloat() * 0.5f + cellNoise * 0.4f)
    }

    private val gruBypassPreset: NoiseApplier = { r ->
        val resetNoise = sin(tickCounter * 0.06f) * 0.1f
        val updateNoise = cos(tickCounter * 0.08f) * 0.08f
        val hiddenNoise = gaussianNoise(0.0, 0.06)
        Rotation(r.yaw + resetNoise + updateNoise + hiddenNoise.toFloat(),
            r.pitch + resetNoise * 0.5f + hiddenNoise.toFloat() * 0.4f)
    }

    private val attentionBypassPreset: NoiseApplier = { r ->
        val qkvNoise = gaussianNoise(0.0, 0.1)
        val scoreNoise = sin(tickCounter * 0.07f) * 0.12f
        val softmaxNoise = cos(tickCounter * 0.11f) * 0.08f
        Rotation(r.yaw + qkvNoise.toFloat() + scoreNoise + softmaxNoise,
            r.pitch + qkvNoise.toFloat() * 0.5f + scoreNoise * 0.4f)
    }

    private val diffusionBypassPreset: NoiseApplier = { r ->
        val forwardNoise = gaussianNoise(0.0, 0.1)
        val reverseNoise = sin(tickCounter * 0.05f) * 0.12f
        val scheduleNoise = cos(tickCounter * 0.09f) * 0.08f
        val timestepNoise = (random.nextFloat() - 0.5f) * 0.06f
        Rotation(r.yaw + forwardNoise.toFloat() + reverseNoise + scheduleNoise + timestepNoise,
            r.pitch + forwardNoise.toFloat() * 0.5f + reverseNoise * 0.4f)
    }

    private val flowMatchingBypassPreset: NoiseApplier = { r ->
        val otNoise = sin(tickCounter * 0.06f) * 0.15f
        val cfmNoise = cos(tickCounter * 0.1f) * 0.1f
        val velocityNoise = gaussianNoise(0.0, 0.08)
        Rotation(r.yaw + otNoise + cfmNoise + velocityNoise.toFloat(),
            r.pitch + otNoise * 0.5f + velocityNoise.toFloat() * 0.4f)
    }

    private val ssmBypassPreset: NoiseApplier = { r ->
        val stateNoise = sin(tickCounter * 0.04f) * 0.12f
        val measureNoise = cos(tickCounter * 0.08f) * 0.1f
        val hippoNoise = gaussianNoise(0.0, 0.06)
        Rotation(r.yaw + stateNoise + measureNoise + hippoNoise.toFloat(),
            r.pitch + stateNoise * 0.5f + measureNoise * 0.4f)
    }

    private val mambaBypassPreset: NoiseApplier = { r ->
        val convNoise = sin(tickCounter * 0.05f) * 0.13f
        val stateNoise = cos(tickCounter * 0.09f) * 0.11f
        val selectiveNoise = gaussianNoise(0.0, 0.07)
        Rotation(r.yaw + convNoise + stateNoise + selectiveNoise.toFloat(),
            r.pitch + convNoise * 0.5f + stateNoise * 0.4f)
    }

    private val rwkvBypassPreset: NoiseApplier = { r ->
        val timeMixNoise = sin(tickCounter * 0.06f) * 0.12f
        val tokenShiftNoise = cos(tickCounter * 0.1f) * 0.1f
        val wkvNoise = gaussianNoise(0.0, 0.06)
        Rotation(r.yaw + timeMixNoise + tokenShiftNoise + wkvNoise.toFloat(),
            r.pitch + timeMixNoise * 0.5f + wkvNoise.toFloat() * 0.4f)
    }

    private val jambaBypassPreset: NoiseApplier = { r ->
        val hybridNoise = sin(tickCounter * 0.05f) * 0.14f
        val mambaNoise = cos(tickCounter * 0.09f) * 0.12f
        val attnNoise = gaussianNoise(0.0, 0.07)
        Rotation(r.yaw + hybridNoise + mambaNoise + attnNoise.toFloat(),
            r.pitch + hybridNoise * 0.5f + mambaNoise * 0.4f)
    }

    private val mixtralBypassPreset: NoiseApplier = { r ->
        val expertNoise = (random.nextFloat() - 0.5f) * 0.2f
        val routerNoise = sin(tickCounter * 0.07f) * 0.15f
        val topKNoise = gaussianNoise(0.0, 0.08)
        Rotation(r.yaw + expertNoise + routerNoise + topKNoise.toFloat(),
            r.pitch + expertNoise * 0.5f + routerNoise * 0.4f)
    }

    private val moeBypassPreset: NoiseApplier = { r ->
        val gateNoise = sin(tickCounter * 0.06f) * 0.16f
        val expertNoise = cos(tickCounter * 0.1f) * 0.14f
        val loadBalanceNoise = gaussianNoise(0.0, 0.07)
        Rotation(r.yaw + gateNoise + expertNoise + loadBalanceNoise.toFloat(),
            r.pitch + gateNoise * 0.5f + expertNoise * 0.4f)
    }

    private val chaosMaxPreset: NoiseApplier = { r ->
        val lorenz = sin(tickCounter * 0.03f) * 0.3f
        val rossler = cos(tickCounter * 0.07f) * 0.25f
        val henon = sin(tickCounter * 0.11f) * cos(tickCounter * 0.13f) * 0.2f
        val doublePendulum = sin(tickCounter * 0.05f + cos(tickCounter * 0.09f)) * 0.3f
        Rotation(r.yaw + lorenz + rossler + henon + doublePendulum,
            r.pitch + lorenz * 0.5f + rossler * 0.4f + doublePendulum * 0.3f)
    }

    private val microJitterPreset: NoiseApplier = { r ->
        val yawJitter = (random.nextFloat() - 0.5f) * 0.4f
        val pitchJitter = (random.nextFloat() - 0.5f) * 0.2f
        Rotation(r.yaw + yawJitter, r.pitch + pitchJitter)
    }

    private val macroSimPreset: NoiseApplier = { r ->
        val step = sin(tickCounter * 0.02f) * 2f
        val accel = cos(tickCounter * 0.04f) * 1.5f
        val smooth = sin(tickCounter * 0.06f) * 0.5f
        Rotation(r.yaw + step + accel, r.pitch + smooth)
    }

    private val humanSimPreset: NoiseApplier = { r ->
        val reaction = sin(tickCounter * 0.01f) * 0.5f
        val overshoot = if (random.nextFloat() < 0.05f) (random.nextFloat() - 0.5f) * 0.8f else 0f
        val microCorrection = (random.nextFloat() - 0.5f) * 0.15f
        val fatigue = (1.0 - exp(-tickCounter * 0.001f)) * 0.1f
        Rotation(r.yaw + reaction + overshoot + microCorrection + fatigue.toFloat(),
            r.pitch + reaction * 0.5f + microCorrection * 0.6f)
    }

    private val robotSimPreset: NoiseApplier = { r ->
        val precisionJitter = (random.nextFloat() - 0.5f) * 0.1f
        val servoNoise = sin(tickCounter * 0.1f) * 0.05f
        val quantization = (random.nextFloat().roundToInt() % 16).toFloat() * 0.01f
        Rotation(r.yaw + precisionJitter + servoNoise + quantization,
            r.pitch + precisionJitter * 0.5f + servoNoise * 0.3f)
    }

    private val butterflyPreset: NoiseApplier = { r ->
        val wingScale = sin(tickCounter * 0.15f) * 0.8f
        val flapFreq = cos(tickCounter * 0.2f) * 0.6f
        val chaos = sin(tickCounter * 0.05f) * cos(tickCounter * 0.07f) * 0.4f
        Rotation(r.yaw + wingScale + flapFreq + chaos,
            r.pitch + wingScale * 0.5f + chaos * 0.6f)
    }

    private val sineWavePreset: NoiseApplier = { r ->
        val amplitude = 1.5f
        val freq = 0.5f + random.nextFloat() * 1.5f
        val phase = random.nextFloat() * 2f * PI.toFloat()
        val harmonic = sin(tickCounter * 0.1f) * 0.3f
        val noise = sin(tickCounter * freq + phase) * amplitude + harmonic
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val sawtoothPreset: NoiseApplier = { r ->
        val amplitude = 1.5f
        val freq = 0.5f + random.nextFloat() * 1.5f
        val phase = (tickCounter * freq) % 1f
        val noise = (phase * 2f - 1f) * amplitude
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val squareWavePreset: NoiseApplier = { r ->
        val amplitude = 1.5f
        val freq = 0.5f + random.nextFloat() * 1.5f
        val dutyCycle = 0.3f + random.nextFloat() * 0.4f
        val phase = (tickCounter * freq) % 1f
        val noise = if (phase < dutyCycle) amplitude else -amplitude
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val triangleWavePreset: NoiseApplier = { r ->
        val amplitude = 1.5f
        val freq = 0.5f + random.nextFloat() * 1.5f
        val phase = (tickCounter * freq) % 1f
        val noise = (4f * abs(phase - 0.5f) - 1f) * amplitude
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val pulseJitterPreset: NoiseApplier = { r ->
        val interval = (5 + random.nextInt(6)).toLong()
        val burst = if (tickCounter % interval == 0L) {
            (random.nextFloat() - 0.5f) * 3f
        } else {
            (random.nextFloat() - 0.5f) * 0.2f * 0.9f.pow((tickCounter % interval).toDouble()).toFloat()
        }
        Rotation(r.yaw + burst, r.pitch + burst * 0.5f)
    }

    private val fractalNoisePreset: NoiseApplier = { r ->
        var noise = 0f
        var amplitude = 1f
        var frequency = 1f
        var maxAmp = 0f
        for (i in 0 until 6) {
            noise += sin(tickCounter * 0.05f * frequency + i * 1.7f) * amplitude
            maxAmp += amplitude
            amplitude *= 0.5f
            frequency *= 2f
        }
        noise = noise / maxAmp * 2f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val waveletNoisePreset: NoiseApplier = { r ->
        val haar = sin(tickCounter * 0.08f) * 0.5f
        val daubechies = cos(tickCounter * 0.12f) * 0.4f
        val symlet = sin(tickCounter * 0.18f) * 0.3f
        val coiflet = cos(tickCounter * 0.25f) * 0.2f
        Rotation(r.yaw + haar + daubechies + symlet + coiflet,
            r.pitch + haar * 0.5f + daubechies * 0.3f + symlet * 0.2f)
    }

    private var ouState = 0.0
    private val ornsteinUhlenbeckPreset: NoiseApplier = { r ->
        val theta = 0.2
        val mu = 0.0
        val sigma = 0.05
        val dt = 1.0
        ouState += theta * (mu - ouState) * dt + sigma * sqrt(dt) * random.nextGaussian()
        val noise = ouState.toFloat() * 50f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val levyFlightPreset: NoiseApplier = { r ->
        val alpha = 1.5
        val u = random.nextGaussian()
        val v = random.nextGaussian()
        val levy = (u / abs(v).pow(1.0 / alpha)) * 0.1f
        Rotation(r.yaw + levy.toFloat() * 30f, r.pitch + levy.toFloat() * 15f)
    }

    private val poissonJitterPreset: NoiseApplier = { r ->
        val lambda = 2.0
        val l = exp(-lambda)
        var k = 0
        var p = 1.0
        while (p > l) {
            k++
            p *= random.nextDouble()
        }
        val noise = ((k - 1) - lambda).toFloat() * 0.3f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val gammaBurstPreset: NoiseApplier = { r ->
        val shape = 2.0
        val scale = 0.05
        val threshold = 4.0
        var gamma = 0.0
        for (i in 0 until shape.toInt()) {
            gamma -= scale * ln(random.nextDouble())
        }
        val burst = if (gamma > threshold) gamma.toFloat() * 10f else 0f
        Rotation(r.yaw + burst, r.pitch + burst * 0.5f)
    }

    private val betaBlendPreset: NoiseApplier = { r ->
        val alpha = 2.0
        val beta = 5.0
        val x = gammaSample(alpha) / (gammaSample(alpha) + gammaSample(beta))
        val noise = (x - 0.5f).toFloat() * 3f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private fun gammaSample(shape: Double): Double {
        var sum = 0.0
        for (i in 0 until shape.toInt()) {
            sum -= ln(random.nextDouble())
        }
        return sum
    }

    private val weibullDriftPreset: NoiseApplier = { r ->
        val shape = 1.5
        val scale = 0.5
        val weibull = scale * (-ln(random.nextDouble())).pow(1.0 / shape)
        val drift = sin(tickCounter * 0.05f) * weibull.toFloat() * 0.5f
        Rotation(r.yaw + drift, r.pitch + drift * 0.5f)
    }

    private val cauchyJumpPreset: NoiseApplier = { r ->
        val scale = 0.05
        val probability = 0.03
        val jump = if (random.nextFloat() < probability) {
            scale * tan(PI * (random.nextDouble() - 0.5)).toFloat() * 30f
        } else 0f
        Rotation(r.yaw + jump, r.pitch + jump * 0.5f)
    }

    private val studentTNoisePreset: NoiseApplier = { r ->
        val dof = 5.0
        val normalSamples = (0 until dof.toInt()).map { nextGaussian() }
        val chiSq = normalSamples.sumOf { it.toDouble() * it.toDouble() }
        val t = nextGaussian() / sqrt(chiSq / dof) * 0.3
        Rotation(r.yaw + t.toFloat() * 10f, r.pitch + t.toFloat() * 5f)
    }

    private val logNormalPulsePreset: NoiseApplier = { r ->
        val mu = -0.5
        val sigma = 0.8
        val pulseRate = 0.03
        val pulse = if (random.nextFloat() < pulseRate) {
            exp(mu + sigma * random.nextGaussian()).toFloat() * 5f
        } else 0f
        Rotation(r.yaw + pulse, r.pitch + pulse * 0.5f)
    }

    private val exponentialDecayPreset: NoiseApplier = { r ->
        val rate = 1.0
        val amplitude = 1.5f
        val triggerProb = 0.03f
        val decay = if (random.nextFloat() < triggerProb) {
            amplitude * exp(-rate * (tickCounter % 20).toDouble()).toFloat()
        } else 0f
        Rotation(r.yaw + decay, r.pitch + decay * 0.5f)
    }

    private val doubleExponentialPreset: NoiseApplier = { r ->
        val scale = 0.8
        val asymmetry = 0.2
        val u = random.nextDouble() - 0.5
        val sign = if (u > 0) 1.0 else -1.0
        val noise = -sign * scale * ln(1.0 - 2.0 * abs(u)) * (1.0 + sign * asymmetry)
        Rotation(r.yaw + noise.toFloat() * 5f, r.pitch + noise.toFloat() * 2.5f)
    }

    private val gaussianMixturePreset: NoiseApplier = { r ->
        val weight1 = 0.6
        val mu1 = -0.5
        val sigma1 = 0.2
        val mu2 = 0.5
        val sigma2 = 0.5
        val sample = if (random.nextFloat() < weight1) {
            mu1 + sigma1 * random.nextGaussian()
        } else {
            mu2 + sigma2 * random.nextGaussian()
        }
        Rotation(r.yaw + sample.toFloat() * 5f, r.pitch + sample.toFloat() * 2.5f)
    }

    private val laplaceShockPreset: NoiseApplier = { r ->
        val mu = 0.0
        val b = 0.2
        val shockProb = 0.03
        val shockScale = 3.0
        val u = random.nextDouble() - 0.5
        val laplace = mu - b * signum(u) * ln(1.0 - 2.0 * abs(u))
        val shock = if (random.nextFloat() < shockProb) {
            laplace * shockScale
        } else {
            laplace * 0.1
        }
        Rotation(r.yaw + shock.toFloat() * 10f, r.pitch + shock.toFloat() * 5f)
    }

    private fun signum(d: Double): Double = if (d > 0) 1.0 else if (d < 0) -1.0 else 0.0

    private val vonMisesWrapPreset: NoiseApplier = { r ->
        val mu = 0.0
        val kappa = 2.0
        val a = 1.0 + sqrt(1.0 + 4.0 * kappa * kappa)
        val b = (a - sqrt(2.0 * a)) / (2.0 * kappa)
        val r_val = (1.0 + b * b) / (2.0 * b)
        var result: Double
        do {
            val u1 = random.nextDouble()
            val z = cos(PI * u1)
            val f = (1.0 + r_val * z) / (r_val + z)
            val c = kappa * (r_val - f)
            val u2 = random.nextDouble()
            result = if (c * (2.0 - c) - u2 > 0 || ln(c / u2) + 1.0 - c >= 0) {
                signum(u1 - 0.5) * acos(f) + mu
            } else {
                Double.NaN
            }
        } while (result.isNaN())
        Rotation(r.yaw + result.toFloat() * 3f, r.pitch + result.toFloat() * 1.5f)
    }

    private val dirichletMixPreset: NoiseApplier = { r ->
        val alpha = doubleArrayOf(2.0, 5.0, 3.0)
        val gammaSamples = alpha.map { gammaSample(it) }
        val sum = gammaSamples.sum()
        val dir = gammaSamples.map { it / sum }
        val noise = (dir[0] - dir[1] + dir[2] * 0.5).toFloat() * 2f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val multinomialPickPreset: NoiseApplier = { r ->
        val probs = doubleArrayOf(0.1, 0.2, 0.3, 0.2, 0.1, 0.05, 0.05)
        val values = doubleArrayOf(-2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 5.0)
        val p = random.nextDouble()
        var cumulative = 0.0
        var pick = 0.0
        for (i in probs.indices) {
            cumulative += probs[i]
            if (p <= cumulative) {
                pick = values[i]
                break
            }
        }
        Rotation(r.yaw + pick.toFloat() * 0.5f, r.pitch + pick.toFloat() * 0.25f)
    }

    private val uniformBandPreset: NoiseApplier = { r ->
        val width = 1.5f
        val center = 0f
        val noise = (random.nextFloat() - 0.5f) * width * 2f + center
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val triangularTaperPreset: NoiseApplier = { r ->
        val peak = 0f
        val width = 2f
        val u = random.nextFloat()
        val noise = if (u < 0.5f) {
            peak - width + sqrt(2f * u) * width
        } else {
            peak + width - sqrt(2f * (1f - u)) * width
        }
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val piecewiseLinearPreset: NoiseApplier = { r ->
        val segments = listOf(-1f, 0.5f, 0f, -0.5f, 1f)
        val t = (tickCounter % 20).toFloat() / 20f
        val idx = (t * (segments.size - 1)).toInt().coerceIn(0, segments.size - 2)
        val localT = (t * (segments.size - 1)) - idx
        val noise = lerp(segments[idx], segments[idx + 1], localT)
        Rotation(r.yaw + noise * 2f, r.pitch + noise)
    }

    private val cubicSplinePreset: NoiseApplier = { r ->
        val controlPoints = listOf(0f, 1.5f, -1f, 0.5f, -1.5f, 0f)
        val t = (tickCounter % 30).toFloat() / 30f
        val n = controlPoints.size - 1
        val idx = (t * n).toInt().coerceIn(0, n - 1)
        val localT = (t * n) - idx
        val p0 = controlPoints[idx.coerceIn(0, n)]
        val p1 = controlPoints[(idx + 1).coerceIn(0, n)]
        val p2 = controlPoints[(idx + 2).coerceIn(0, n)]
        val p3 = controlPoints[(idx + 3).coerceIn(0, n)]
        val t2 = localT * localT
        val t3 = t2 * localT
        val noise = 0.5f * ((2f * p1) +
            (-p0 + p2) * localT +
            (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
            (-p0 + 3f * p1 - 3f * p2 + p3) * t3)
        Rotation(r.yaw + noise * 2f, r.pitch + noise)
    }

    private val bSplineWigglePreset: NoiseApplier = { r ->
        val degree = 3
        val knots = (0..8).map { it.toFloat() / 8f }
        val t = (tickCounter % 40).toFloat() / 40f
        var noise = 0f
        for (i in 0 until knots.size - degree - 1) {
            val basis = bsplineBasis(i, degree, knots, t)
            noise += basis * sin(i * 1.5f + tickCounter * 0.1f) * 0.5f
        }
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private fun bsplineBasis(i: Int, k: Int, knots: List<Float>, t: Float): Float {
        if (k == 0) {
            return if (t in knots[i]..knots[i + 1]) 1f else 0f
        }
        val left = if (knots[i + k] - knots[i] != 0f)
            (t - knots[i]) / (knots[i + k] - knots[i]) * bsplineBasis(i, k - 1, knots, t) else 0f
        val right = if (knots[i + k + 1] - knots[i + 1] != 0f)
            (knots[i + k + 1] - t) / (knots[i + k + 1] - knots[i + 1]) * bsplineBasis(i + 1, k - 1, knots, t) else 0f
        return left + right
    }

    private val nurbsCurvePreset: NoiseApplier = { r ->
        val controlPoints = listOf(0f, 1.5f, -1f, 0.5f, -1.5f, 0f, 0.8f, -0.8f)
        val weights = listOf(1f, 0.8f, 1.2f, 0.9f, 1.1f, 1f, 0.7f, 1.3f)
        val t = (tickCounter % 35).toFloat() / 35f
        val n = controlPoints.size - 1
        val idx = (t * n).toInt().coerceIn(0, n - 1)
        val localT = (t * n) - idx
        val p0 = controlPoints[idx] * weights[idx]
        val p1 = controlPoints[idx + 1] * weights[idx + 1]
        val noise = lerp(p0, p1, localT) / lerp(weights[idx], weights[idx + 1], localT)
        Rotation(r.yaw + noise * 2f, r.pitch + noise)
    }

    private val rbfInterpolationPreset: NoiseApplier = { r ->
        val centers = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
        val kernelWidth = 0.8f
        val t = (tickCounter % 25).toFloat() / 25f
        var noise = 0f
        var weightSum = 0f
        for ((i, center) in centers.withIndex()) {
            val dist = (t - center) * (t - center)
            val weight = exp(-dist / kernelWidth)
            noise += weight * sin(i * 2f + tickCounter * 0.1f)
            weightSum += weight
        }
        noise /= weightSum
        Rotation(r.yaw + noise * 2f, r.pitch + noise)
    }

    private val knnBlendPreset: NoiseApplier = { r ->
        val k = 3
        val neighbors = (0 until k).map {
            (random.nextFloat() - 0.5f) * 2f
        }
        val noise = neighbors.average().toFloat()
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val svmDecisionPreset: NoiseApplier = { r ->
        val w1 = random.nextFloat() * 2f - 1f
        val w2 = random.nextFloat() * 2f - 1f
        val b = random.nextFloat() * 0.5f
        val x = sin(tickCounter * 0.1f)
        val y = cos(tickCounter * 0.15f)
        val decision = (w1 * x + w2 * y + b).coerceIn(-1f, 1f)
        Rotation(r.yaw + decision * 1.5f, r.pitch + decision * 0.75f)
    }

    private val randomForestAimPreset: NoiseApplier = { r ->
        val trees = (0 until 5).map { random.nextFloat() * 2f - 1f }
        val noise = trees.sum() / trees.size * 0.5f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val xgboostAimPreset: NoiseApplier = { r ->
        val eta = 0.3f
        val maxDepth = 3
        val noise = (0 until maxDepth).sumOf {
            (random.nextFloat() - 0.5f) * eta * 0.5.pow(it).toDouble()
        }.toFloat()
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val gradientBoostAimPreset: NoiseApplier = { r ->
        val lr = 0.1f
        val residual = sin(tickCounter * 0.08f) * 0.5f
        val noise = residual * lr
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val adaBoostAimPreset: NoiseApplier = { r ->
        val alpha = 0.5f
        val weights = (0 until 3).map { exp(-alpha * it.toDouble()).toFloat() }
        val noise = weights.sum() / weights.size * 0.5f * (random.nextFloat() * 2f - 1f)
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val baggingAimPreset: NoiseApplier = { r ->
        val samples = (0 until 10).map { (random.nextFloat() - 0.5f) * 2f }
        val noise = samples.average().toFloat() * 0.5f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val stackingAimPreset: NoiseApplier = { r ->
        val baseModels = (0 until 3).map { (random.nextFloat() - 0.5f) * 0.3f }
        val metaModel = baseModels.average() * 0.5f
        Rotation(r.yaw + metaModel.toFloat(), r.pitch + metaModel.toFloat() * 0.5f)
    }

    private val votingAimPreset: NoiseApplier = { r ->
        val votes = (0 until 5).map { if (random.nextBoolean()) 1f else -1f }
        val noise = votes.sum().toFloat() / votes.size * 0.5f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val bayesianOptAimPreset: NoiseApplier = { r ->
        val exploration = 0.2f
        val exploitation = 0.8f
        val ei = (random.nextFloat() - 0.5f) * exploration + exploitation * sin(tickCounter * 0.1f) * 0.3f
        Rotation(r.yaw + ei, r.pitch + ei * 0.5f)
    }

    private val gaussianProcessAimPreset: NoiseApplier = { r ->
        val lengthScale = 1.0
        val gpNoise = 0.05
        val kernel = exp(-0.5 * sin(tickCounter * 0.1f).pow(2) / lengthScale)
        val noise = (kernel * random.nextGaussian() + gpNoise * random.nextGaussian()).toFloat() * 0.5f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val mcmcSampleAimPreset: NoiseApplier = { r ->
        val proposal = (random.nextFloat() - 0.5f) * 0.5f
        val acceptance = min(1f, exp(-abs(proposal) * 2f))
        val noise = if (random.nextFloat() < acceptance) proposal else 0f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private var pfParticles = Array(50) { 0f }
    private var pfWeights = Array(50) { 1f / 50f }
    private val particleFilterAimPreset: NoiseApplier = { r ->
        for (i in pfParticles.indices) {
            pfParticles[i] += (random.nextFloat() - 0.5f) * 0.1f
            pfWeights[i] = exp(-pfParticles[i] * pfParticles[i] * 10f)
        }
        val totalWeight = pfWeights.sum()
        val noise = pfParticles.indices.sumOf { pfParticles[it] * pfWeights[it] } / totalWeight
        if (totalWeight < 0.3f) {
            val newParticles = FloatArray(50)
            val newWeights = FloatArray(50)
            for (i in newParticles.indices) {
                val idx = random.nextInt(50)
                newParticles[i] = pfParticles[idx] + (random.nextFloat() - 0.5f) * 0.05f
                newWeights[i] = 1f / 50f
            }
            pfParticles = newParticles
            pfWeights = newWeights
        }
        Rotation(r.yaw + noise.toFloat(), r.pitch + noise.toFloat() * 0.5f)
    }

    private var kalmanEstimate = 0f
    private var kalmanError = 1f
    private val kalmanFilterAimPreset: NoiseApplier = { r ->
        val processNoise = 0.05f
        val measurementNoise = 0.3f
        kalmanError += processNoise
        val measurement = (random.nextFloat() - 0.5f) * 0.5f
        val kalmanGain = kalmanError / (kalmanError + measurementNoise)
        kalmanEstimate += kalmanGain * (measurement - kalmanEstimate)
        kalmanError *= (1f - kalmanGain)
        Rotation(r.yaw + kalmanEstimate, r.pitch + kalmanEstimate * 0.5f)
    }

    private val extendedKalmanAimPreset: NoiseApplier = { r ->
        val state = sin(tickCounter * 0.1f) * 0.5f
        val jacobian = cos(tickCounter * 0.1f) * 0.5f
        val processNoise = 0.05f
        val measurementNoise = 0.3f
        kalmanError += processNoise * jacobian * jacobian
        val measurement = (random.nextFloat() - 0.5f) * 0.5f
        val kalmanGain = kalmanError / (kalmanError + measurementNoise)
        kalmanEstimate += kalmanGain * (measurement - state)
        kalmanError *= (1f - kalmanGain)
        Rotation(r.yaw + kalmanEstimate + state, r.pitch + (kalmanEstimate + state) * 0.5f)
    }

    private var ukfSigmaPoints = Array(5) { FloatArray(5) { 0f } }
    private val unscentedKalmanAimPreset: NoiseApplier = { r ->
        val n = 3
        val alpha = 0.1f
        val lambda = alpha * alpha * n - n
        val noise = ukfSigmaPoints.map { it.sum() / it.size }.average().toFloat() * 0.3f
        for (i in ukfSigmaPoints.indices) {
            for (j in ukfSigmaPoints[i].indices) {
                ukfSigmaPoints[i][j] += (random.nextFloat() - 0.5f) * 0.05f
            }
        }
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val ensembleKalmanAimPreset: NoiseApplier = { r ->
        val ensemble = (0 until 10).map { (random.nextFloat() - 0.5f) * 0.5f }
        val mean = ensemble.average().toFloat()
        val spread = ensemble.map { (it - mean) * (it - mean) }.average().toFloat() * 0.5f
        Rotation(r.yaw + mean + spread, r.pitch + (mean + spread) * 0.5f)
    }

    private var psoVelocity = 0f
    private var psoPosition = 0f
    private var psoBest = 0f
    private var psoGlobalBest = 0f
    private val particleSwarmAimPreset: NoiseApplier = { r ->
        val inertia = 0.6f
        val cognitive = 1.5f
        val social = 1.5f
        val r1 = random.nextFloat()
        val r2 = random.nextFloat()
        psoVelocity = inertia * psoVelocity +
            cognitive * r1 * (psoBest - psoPosition) +
            social * r2 * (psoGlobalBest - psoPosition)
        psoVelocity = psoVelocity.coerceIn(-1f, 1f)
        psoPosition += psoVelocity
        if (abs(psoPosition) < abs(psoBest)) psoBest = psoPosition
        if (abs(psoBest) < abs(psoGlobalBest)) psoGlobalBest = psoBest
        Rotation(r.yaw + psoPosition * 0.5f, r.pitch + psoPosition * 0.25f)
    }

    private val antColonyAimPreset: NoiseApplier = { r ->
        val pheromone = exp(-tickCounter % 20 * 0.1f).toFloat()
        val evaporation = 0.95f
        val noise = pheromone * evaporation * (random.nextFloat() - 0.5f) * 2f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private var saTemp = 5f
    private val simulatedAnnealingAimPreset: NoiseApplier = { r ->
        val coolingRate = 0.98f
        saTemp *= coolingRate
        if (saTemp < 0.01f) saTemp = 5f
        val proposal = (random.nextFloat() - 0.5f) * saTemp * 0.5f
        val acceptance = exp(-abs(proposal) / saTemp)
        val noise = if (random.nextFloat() < acceptance) proposal else 0f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private var gaPopulation = Array(20) { random.nextFloat() * 2f - 1f }
    private val geneticAlgorithmAimPreset: NoiseApplier = { r ->
        if (tickCounter % 20 == 0L) {
            gaPopulation.sortBy { abs(it) }
            val newPop = FloatArray(20)
            for (i in 0 until 20 step 2) {
                val parent1 = gaPopulation[i]
                val parent2 = gaPopulation[i + 1]
                val crossover = (parent1 + parent2) * 0.5f
                val mutation = if (random.nextFloat() < 0.05f) (random.nextFloat() - 0.5f) * 0.5f else 0f
                newPop[i] = crossover + mutation
                newPop[i + 1] = crossover - mutation
            }
            gaPopulation = newPop
        }
        val idx = (tickCounter % 20).toInt()
        Rotation(r.yaw + gaPopulation[idx] * 0.5f, r.pitch + gaPopulation[idx] * 0.25f)
    }

    private val differentialEvolutionAimPreset: NoiseApplier = { r ->
        val F = 0.5f
        val CR = 0.9f
        val pop = (0 until 5).map { random.nextFloat() * 2f - 1f }
        val a = pop[random.nextInt(5)]
        val b = pop[random.nextInt(5)]
        val c = pop[random.nextInt(5)]
        val mutant = a + F * (b - c)
        val trial = if (random.nextFloat() < CR) mutant else pop[0]
        val noise = trial.coerceIn(-1f, 1f) * 0.5f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private var cmaesMean = 0f
    private var cmaesSigma = 0.5f
    private val cmaesAimPreset: NoiseApplier = { r ->
        val popSize = 10
        val samples = (0 until popSize).map { cmaesMean + cmaesSigma * random.nextGaussian().toFloat() }
        val sorted = samples.sortedBy { abs(it) }
        cmaesMean = sorted.take(popSize / 2).average().toFloat()
        cmaesSigma = (cmaesSigma * 0.95f).coerceAtLeast(0.01f)
        Rotation(r.yaw + cmaesMean * 0.5f, r.pitch + cmaesMean * 0.25f)
    }

    private val hillClimbingAimPreset: NoiseApplier = { r ->
        val current = sin(tickCounter * 0.1f) * 0.5f
        val neighbor = current + (random.nextFloat() - 0.5f) * 0.3f
        val noise = if (abs(neighbor) < abs(current)) neighbor else current
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val randomSearchAimPreset: NoiseApplier = { r ->
        val noise = (random.nextFloat() - 0.5f) * 3f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val gridSearchAimPreset: NoiseApplier = { r ->
        val grid = (-5..5).map { it * 0.2f }
        val idx = ((tickCounter % grid.size).toInt() + grid.size) % grid.size
        Rotation(r.yaw + grid[idx], r.pitch + grid[idx] * 0.5f)
    }

    private val bayesianSearchAimPreset: NoiseApplier = { r ->
        val exploration = 0.3f
        val exploitation = 0.7f
        val ucb = exploitation * sin(tickCounter * 0.1f) * 0.5f + exploration * sqrt(ln(tickCounter.toFloat() + 1)) * 0.3f
        Rotation(r.yaw + ucb, r.pitch + ucb * 0.5f)
    }

    private val hyperbandAimPreset: NoiseApplier = { r ->
        val maxIter = 81
        val eta = 3
        val sMax = (ln(maxIter.toFloat()) / ln(eta.toFloat())).toInt()
        val s = (tickCounter % sMax).toInt()
        val n = (maxIter * eta.toFloat().pow(sMax - s)).toInt()
        val r = maxIter / n
        val noise = sin(tickCounter * 0.1f) * r.toFloat() / maxIter * 0.5f
        Rotation(r.yaw + noise, r.pitch + noise * 0.5f)
    }

    private val bohbAimPreset: NoiseApplier = { r ->
        val dim = 3
        val budget = 100
        val t = (tickCounter % budget).toFloat() / budget
        val ei = exp(-t * 2f) * (1f - t) * 0.5f
        Rotation(r.yaw + ei, r.pitch + ei * 0.5f)
    }

    fun combinePresets(presets: Set<String>, customNoise: NoiseApplier?): NoiseApplier {
        if (presets.isEmpty() || (presets.size == 1 && presets.contains("Custom"))) {
            return customNoise ?: { r -> r }
        }

        val appliers = presets.filter { it != "Custom" }.mapNotNull { name ->
            try {
                getPreset(name)
            } catch (e: Exception) {
                null
            }
        }

        if (appliers.isEmpty()) return customNoise ?: { r -> r }

        return { r ->
            var result = customNoise?.invoke(r) ?: r
            val count = appliers.size.toFloat()
            for (applier in appliers) {
                val noiseResult = applier(result)
                result = Rotation(
                    result.yaw + (noiseResult.yaw - result.yaw) / count,
                    result.pitch + (noiseResult.pitch - result.pitch) / count
                )
            }
            result
        }
    }
}