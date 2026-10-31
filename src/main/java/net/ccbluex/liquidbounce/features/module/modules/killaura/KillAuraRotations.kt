package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils

object KillAuraRotations : Module("KillAura-Rotations", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val options = RotationSettings(KillAura).withoutKeepRotation()

    val rotationsActive: Boolean get() = options.rotationsActive

    val raycastValue = boolean("RayCast", true) { options.rotationsActive }
    val raycast: Boolean get() = raycastValue.isActive()
    val raycastIgnored by boolean("RayCastIgnored", false) { raycastValue.isActive() && options.rotationsActive }
    val livingRaycast by boolean("LivingRayCast", true) { raycastValue.isActive() && options.rotationsActive }

    val randomization = RandomizationSettings(KillAura) { options.rotationsActive }
    val outBorder by boolean("OutBorder", false) { options.rotationsActive }

    var noiseFunction: ((Rotation) -> Rotation)? = null

    val highestBodyPointToTargetValue = choices(
        "HighestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Head"
    ) { options.rotationsActive }
    val highestBodyPointToTarget: String by highestBodyPointToTargetValue

    val lowestBodyPointToTargetValue = choices(
        "LowestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Feet"
    ) { options.rotationsActive }
    val lowestBodyPointToTarget: String by lowestBodyPointToTargetValue

    val horizontalBodySearchRange by floatRange("HorizontalBodySearchRange", 0f..1f, 0f..1f) { options.rotationsActive }

    val swarmCognitive by floatRange("SwarmCognitive", 1.5f..2.5f, 0f..4f) { options.rotationsActive }
    val annealingInitialTemp by floatRange("AnnealingInitTemp", 1f..10f, 0.01f..100f) { options.rotationsActive }
    val geneticCrossoverRate by floatRange("GeneticCrossoverRate", 0.6f..0.9f, 0f..1f) { options.rotationsActive }
    val particleCount by intRange("PF-ParticleCount", 100..500, 10..5000) { options.rotationsActive }
    val ouSigma by floatRange("OuSigma", 0.1f..0.5f, 0f..5f) { options.rotationsActive }
    val ouTheta by floatRange("OuTheta", 0.1f..0.5f, 0f..5f) { options.rotationsActive }

    val noisePreset = multiChoices(
        "NoisePreset",
        arrayOf(
            "Custom", "Vanilla", "Legit", "SemiBlatant", "Blatant",
            "AntiML1", "AntiML2", "AntiML3", "AntiML4", "AntiML5", "AntiML6", "AntiML7", "AntiML8", "AntiML9", "AntiML10",
            "NeuralBypass", "DeepLearningBypass", "ReinforcementBypass", "GANBypass", "TransformerBypass",
            "LSTMBypass", "GRUBypass", "AttentionBypass", "DiffusionBypass", "FlowMatchingBypass",
            "SSMBypass", "MambaBypass", "RWKVBypass", "JambaBypass", "MixtralBypass", "MoEBypass",
            "ChaosMax", "MicroJitter", "MacroSim", "HumanSim", "RobotSim", "Butterfly",
            "SineWave", "Sawtooth", "SquareWave", "TriangleWave", "PulseJitter",
            "FractalNoise", "WaveletNoise", "OrnsteinUhlenbeck", "LevyFlight", "PoissonJitter",
            "GammaBurst", "BetaBlend", "WeibullDrift", "CauchyJump", "StudentTNoise",
            "LogNormalPulse", "ExponentialDecay", "DoubleExponential", "GaussianMixture", "LaplaceShock",
            "VonMisesWrap", "DirichletMix", "MultinomialPick", "UniformBand", "TriangularTaper",
            "PiecewiseLinear", "CubicSpline", "BSplineWiggle", "NURBSCurve", "RBFInterpolation",
            "KNNBlend", "SVMDecision", "RandomForestAim", "XGBoostAim", "GradientBoostAim",
            "AdaBoostAim", "BaggingAim", "StackingAim", "VotingAim", "BayesianOptAim",
            "GaussianProcessAim", "MCMCSampleAim", "ParticleFilterAim", "KalmanFilterAim", "ExtendedKalmanAim",
            "UnscentedKalmanAim", "EnsembleKalmanAim", "ParticleSwarmAim", "AntColonyAim", "SimulatedAnnealingAim",
            "GeneticAlgorithmAim", "DifferentialEvolutionAim", "CMAESAim", "HillClimbingAim", "RandomSearchAim",
            "GridSearchAim", "BayesianSearchAim", "HyperbandAim", "BOHBAim"
        ),
        setOf("Custom")
    ) { options.rotationsActive }

    val yawMicroJitter by floatRange("YawMicroJitter", 0f..0f, 0f..5f) { options.rotationsActive }
    val pitchMicroJitter by floatRange("PitchMicroJitter", 0f..0f, 0f..5f) { options.rotationsActive }
    val sineAmplitude by floatRange("SineAmplitude", 0f..0f, 0f..10f) { options.rotationsActive }
}