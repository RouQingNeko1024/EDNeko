package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils

object KillAuraRotations : Module("KillAura-Rotations", Category.KILLAURA, canBeEnabled = false, defaultState = true) {

    val options = RotationSettings(KillAura).withoutKeepRotation()

    val rotationsActive: Boolean
        get() = options.rotationsActive

    val raycastValue = boolean("RayCast", true) { options.rotationsActive }
    val raycast: Boolean get() = raycastValue.isActive()
    val raycastIgnored by boolean("RayCastIgnored", false) {
        raycastValue.isActive() && options.rotationsActive
    }
    val livingRaycast by boolean("LivingRayCast", true) {
        raycastValue.isActive() && options.rotationsActive
    }

    val randomization = RandomizationSettings(KillAura) { options.rotationsActive }
    val outBorder by boolean("OutBorder", false) { options.rotationsActive }

    val noisePreset = multiChoices(
        "NoisePreset", arrayOf(
            "Custom", "Vanilla", "Legit", "SemiBlatant", "Blatant",
            "AntiML1", "AntiML2", "AntiML3", "AntiML4", "AntiML5",
            "AntiML6", "AntiML7", "AntiML8", "AntiML9", "AntiML10",
            "NeuralBypass", "DeepLearningBypass", "ReinforcementBypass", "GANBypass",
            "TransformerBypass", "LSTMBypass", "GRUBypass", "AttentionBypass",
            "DiffusionBypass", "FlowMatchingBypass", "SSMBypass", "MambaBypass",
            "RWKVBypass", "JambaBypass", "MixtralBypass", "MoEBypass",
            "ChaosMax", "MicroJitter", "MacroSim", "HumanSim", "RobotSim",
            "Butterfly", "SineWave", "Sawtooth", "SquareWave", "TriangleWave",
            "PulseJitter", "FractalNoise", "WaveletNoise", "OrnsteinUhlenbeck",
            "LevyFlight", "PoissonJitter", "GammaBurst", "BetaBlend",
            "WeibullDrift", "CauchyJump", "StudentTNoise", "LogNormalPulse",
            "ExponentialDecay", "DoubleExponential", "GaussianMixture", "LaplaceShock",
            "VonMisesWrap", "DirichletMix", "MultinomialPick", "UniformBand",
            "TriangularTaper", "PiecewiseLinear", "CubicSpline", "BSplineWiggle",
            "NURBSCurve", "RBFInterpolation", "KNNBlend", "SVMDecision",
            "RandomForestAim", "XGBoostAim", "GradientBoostAim", "AdaBoostAim",
            "BaggingAim", "StackingAim", "VotingAim", "BayesianOptAim",
            "GaussianProcessAim", "MCMCSampleAim", "ParticleFilterAim",
            "KalmanFilterAim", "ExtendedKalmanAim", "UnscentedKalmanAim",
            "EnsembleKalmanAim", "ParticleSwarmAim", "AntColonyAim",
            "SimulatedAnnealingAim", "GeneticAlgorithmAim", "DifferentialEvolutionAim",
            "CMAESAim", "HillClimbingAim", "RandomSearchAim", "GridSearchAim",
            "BayesianSearchAim", "HyperbandAim", "BOHBAim"
        ), setOf("Custom")
    ) { options.rotationsActive }

    val yawMicroJitter by floatRange("YawMicroJitter", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val pitchMicroJitter by floatRange("PitchMicroJitter", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val yawMacroJitter by floatRange("YawMacroJitter", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val pitchMacroJitter by floatRange("PitchMacroJitter", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val jitterDecayRate by floatRange("JitterDecayRate", 0.95f..Float.MAX_VALUE, 0.5f..Float.MAX_VALUE) { options.rotationsActive }
    val jitterBurstProbability by floatRange("JitterBurstProbability", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val jitterBurstStrength by floatRange("JitterBurstStrength", 1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val jitterAdaptiveDecay by boolean("JitterAdaptiveDecay", false) { options.rotationsActive }
    val jitterTimeCorrelation by floatRange("JitterTimeCorrelation", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val jitterAxisCoupling by floatRange("JitterAxisCoupling", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val sineAmplitude by floatRange("SineAmplitude", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val sineFrequency by floatRange("SineFrequency", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val sinePhaseOffset by floatRange("SinePhaseOffset", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val sinePitchRatio by floatRange("SinePitchRatio", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val sineHarmonicCount by intRange("SineHarmonicCount", 1..Int.MAX_VALUE, 1..Int.MAX_VALUE) { options.rotationsActive }
    val sineHarmonicDecay by floatRange("SineHarmonicDecay", 0.5f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }

    val sawtoothAmplitude by floatRange("SawtoothAmplitude", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val sawtoothFrequency by floatRange("SawtoothFrequency", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val sawtoothPitchRatio by floatRange("SawtoothPitchRatio", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val squareAmplitude by floatRange("SquareAmplitude", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val squareFrequency by floatRange("SquareFrequency", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val squareDutyCycle by floatRange("SquareDutyCycle", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val squarePitchRatio by floatRange("SquarePitchRatio", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val triangleAmplitude by floatRange("TriangleAmplitude", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val triangleFrequency by floatRange("TriangleFrequency", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val trianglePitchRatio by floatRange("TrianglePitchRatio", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val pulseJitterAmplitude by floatRange("PulseJitterAmplitude", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val pulseJitterInterval by intRange("PulseJitterInterval", 5..Int.MAX_VALUE, 1..Int.MAX_VALUE) { options.rotationsActive }
    val pulseJitterDecay by floatRange("PulseJitterDecay", 0.8f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }

    val ouTheta by floatRange("OU-Theta", 0.1f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val ouMu by floatRange("OU-Mu", 0f..Float.MAX_VALUE, -Float.MAX_VALUE..Float.MAX_VALUE) { options.rotationsActive }
    val ouSigma by floatRange("OU-Sigma", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val ouYawScale by floatRange("OU-YawScale", 1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val ouPitchScale by floatRange("OU-PitchScale", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val levyAlpha by floatRange("Levy-Alpha", 1.5f..Float.MAX_VALUE, 0.5f..Float.MAX_VALUE) { options.rotationsActive }
    val levyScale by floatRange("Levy-Scale", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val levyYawWeight by floatRange("Levy-YawWeight", 1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val levyPitchWeight by floatRange("Levy-PitchWeight", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val poissonLambda by floatRange("Poisson-Lambda", 1f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val poissonJitterScale by floatRange("Poisson-JitterScale", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val gammaShape by floatRange("Gamma-Shape", 2f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val gammaScale by floatRange("Gamma-Scale", 0.01f..Float.MAX_VALUE, 0.001f..Float.MAX_VALUE) { options.rotationsActive }
    val gammaBurstThreshold by floatRange("Gamma-BurstThreshold", 3f..Float.MAX_VALUE, 1f..Float.MAX_VALUE) { options.rotationsActive }

    val betaAlpha by floatRange("Beta-Alpha", 2f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val betaBetaParam by floatRange("Beta-Beta", 2f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val betaBlendStrength by floatRange("Beta-BlendStrength", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val weibullShape by floatRange("Weibull-Shape", 1f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val weibullScale by floatRange("Weibull-Scale", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val weibullDriftRate by floatRange("Weibull-DriftRate", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val cauchyScale by floatRange("Cauchy-Scale", 0.01f..Float.MAX_VALUE, 0.001f..Float.MAX_VALUE) { options.rotationsActive }
    val cauchyJumpProbability by floatRange("Cauchy-JumpProbability", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val studentTDof by floatRange("StudentT-DOF", 3f..Float.MAX_VALUE, 1f..Float.MAX_VALUE) { options.rotationsActive }
    val studentTScale by floatRange("StudentT-Scale", 0.1f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }

    val logNormalMu by floatRange("LogNormal-Mu", -Float.MAX_VALUE..Float.MAX_VALUE, -Float.MAX_VALUE..Float.MAX_VALUE) { options.rotationsActive }
    val logNormalSigma by floatRange("LogNormal-Sigma", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val logNormalPulseRate by floatRange("LogNormal-PulseRate", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val expDecayRate by floatRange("ExpDecay-Rate", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val expDecayAmplitude by floatRange("ExpDecay-Amplitude", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val expDecayTriggerProb by floatRange("ExpDecay-TriggerProbability", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val doubleExpScale by floatRange("DoubleExp-Scale", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val doubleExpAsymmetry by floatRange("DoubleExp-Asymmetry", 0f..Float.MAX_VALUE, -Float.MAX_VALUE..Float.MAX_VALUE) { options.rotationsActive }

    val gaussianMixWeight1 by floatRange("GaussianMix-Weight1", 0.3f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val gaussianMixMu1 by floatRange("GaussianMix-Mu1", -Float.MAX_VALUE..Float.MAX_VALUE, -Float.MAX_VALUE..Float.MAX_VALUE) { options.rotationsActive }
    val gaussianMixSigma1 by floatRange("GaussianMix-Sigma1", 0.1f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val gaussianMixMu2 by floatRange("GaussianMix-Mu2", 0f..Float.MAX_VALUE, -Float.MAX_VALUE..Float.MAX_VALUE) { options.rotationsActive }
    val gaussianMixSigma2 by floatRange("GaussianMix-Sigma2", 0.3f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }

    val laplaceMu by floatRange("Laplace-Mu", 0f..Float.MAX_VALUE, -Float.MAX_VALUE..Float.MAX_VALUE) { options.rotationsActive }
    val laplaceB by floatRange("Laplace-B", 0.1f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val laplaceShockProb by floatRange("Laplace-ShockProbability", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val laplaceShockScale by floatRange("Laplace-ShockScale", 2f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val vonMisesMu by floatRange("VonMises-Mu", 0f..Float.MAX_VALUE, -Float.MAX_VALUE..Float.MAX_VALUE) { options.rotationsActive }
    val vonMisesKappa by floatRange("VonMises-Kappa", 1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val uniformBandWidth by floatRange("UniformBand-Width", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val uniformBandCenter by floatRange("UniformBand-Center", 0f..Float.MAX_VALUE, -Float.MAX_VALUE..Float.MAX_VALUE) { options.rotationsActive }
    val uniformBandPitchRatio by floatRange("UniformBand-PitchRatio", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val triangularPeak by floatRange("Triangular-Peak", 0f..Float.MAX_VALUE, -Float.MAX_VALUE..Float.MAX_VALUE) { options.rotationsActive }
    val triangularWidth by floatRange("Triangular-Width", 1f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }

    val cubicSplineTension by floatRange("CubicSpline-Tension", 0.3f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val cubicSplineSmoothness by floatRange("CubicSpline-Smoothness", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val cubicSplineControlPoints by intRange("CubicSpline-ControlPoints", 4..Int.MAX_VALUE, 2..Int.MAX_VALUE) { options.rotationsActive }

    val bSplineDegree by intRange("BSpline-Degree", 3..Int.MAX_VALUE, 1..Int.MAX_VALUE) { options.rotationsActive }
    val bSplineKnotDensity by floatRange("BSpline-KnotDensity", 0.5f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val bSplineWiggleAmplitude by floatRange("BSpline-WiggleAmplitude", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val rbfKernelWidth by floatRange("RBF-KernelWidth", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val rbfRegularization by floatRange("RBF-Regularization", 0.01f..Float.MAX_VALUE, 0.001f..Float.MAX_VALUE) { options.rotationsActive }
    val rbfCenterCount by intRange("RBF-CenterCount", 5..Int.MAX_VALUE, 2..Int.MAX_VALUE) { options.rotationsActive }

    val kalmanProcessNoise by floatRange("Kalman-ProcessNoise", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val kalmanMeasurementNoise by floatRange("Kalman-MeasurementNoise", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val kalmanInitialUncertainty by floatRange("Kalman-InitialUncertainty", 1f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val kalmanAdaptive by boolean("Kalman-Adaptive", false) { options.rotationsActive }
    val kalmanAdaptiveRate by floatRange("Kalman-AdaptiveRate", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive && kalmanAdaptive }

    val particleCount by intRange("ParticleFilter-Count", 50..Int.MAX_VALUE, 10..Int.MAX_VALUE) { options.rotationsActive }
    val particleResampleThreshold by floatRange("ParticleFilter-ResampleThreshold", 0.5f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val particleDiffusion by floatRange("ParticleFilter-Diffusion", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val swarmParticleCount by intRange("Swarm-ParticleCount", 20..Int.MAX_VALUE, 5..Int.MAX_VALUE) { options.rotationsActive }
    val swarmInertia by floatRange("Swarm-Inertia", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val swarmCognitive by floatRange("Swarm-Cognitive", 1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val swarmSocial by floatRange("Swarm-Social", 1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val swarmMaxVelocity by floatRange("Swarm-MaxVelocity", 0.1f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }

    val annealingInitialTemp by floatRange("Annealing-InitialTemp", 1f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val annealingCoolingRate by floatRange("Annealing-CoolingRate", 0.95f..Float.MAX_VALUE, 0.5f..Float.MAX_VALUE) { options.rotationsActive }
    val annealingMinTemp by floatRange("Annealing-MinTemp", 0.01f..Float.MAX_VALUE, 0.001f..Float.MAX_VALUE) { options.rotationsActive }

    val geneticPopSize by intRange("Genetic-PopSize", 20..Int.MAX_VALUE, 5..Int.MAX_VALUE) { options.rotationsActive }
    val geneticMutationRate by floatRange("Genetic-MutationRate", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val geneticCrossoverRate by floatRange("Genetic-CrossoverRate", 0.7f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val geneticElitism by intRange("Genetic-Elitism", 1..Int.MAX_VALUE, 0..Int.MAX_VALUE) { options.rotationsActive }

    val cmaesPopulation by intRange("CMAES-Population", 10..Int.MAX_VALUE, 5..Int.MAX_VALUE) { options.rotationsActive }
    val cmaesInitialStep by floatRange("CMAES-InitialStepSize", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }

    val bayesianExploration by floatRange("Bayesian-Exploration", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val bayesianExploitation by floatRange("Bayesian-Exploitation", 0.7f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val bayesianGPNoise by floatRange("Bayesian-GPNoise", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val fractalDimension by floatRange("Fractal-Dimension", 1.5f..Float.MAX_VALUE, 0.5f..Float.MAX_VALUE) { options.rotationsActive }
    val fractalOctaves by intRange("Fractal-Octaves", 3..Int.MAX_VALUE, 1..Int.MAX_VALUE) { options.rotationsActive }
    val fractalLacunarity by floatRange("Fractal-Lacunarity", 2f..Float.MAX_VALUE, 1f..Float.MAX_VALUE) { options.rotationsActive }
    val fractalGain by floatRange("Fractal-Gain", 0.5f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }

    val waveletType by choices("Wavelet-Type", arrayOf("Haar", "Daubechies4", "Symlet4", "Coiflet3", "Biorthogonal"), "Haar") { options.rotationsActive }
    val waveletDecompositionLevel by intRange("Wavelet-DecompositionLevel", 2..Int.MAX_VALUE, 1..Int.MAX_VALUE) { options.rotationsActive }
    val waveletThreshold by floatRange("Wavelet-Threshold", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val humanReactionDelay by intRange("Human-ReactionDelay", 50..Int.MAX_VALUE, 0..Int.MAX_VALUE, "ms") { options.rotationsActive }
    val humanAccelerationPhase by floatRange("Human-AccelerationPhase", 0.3f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val humanDecelerationPhase by floatRange("Human-DecelerationPhase", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val humanOvershootProbability by floatRange("Human-OvershootProbability", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val humanOvershootAmount by floatRange("Human-OvershootAmount", 1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val humanMicroCorrection by floatRange("Human-MicroCorrection", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val humanFatigueRate by floatRange("Human-FatigueRate", 0.001f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val humanVariability by floatRange("Human-Variability", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val macroStepInterval by intRange("Macro-StepInterval", 50..Int.MAX_VALUE, 10..Int.MAX_VALUE, "ms") { options.rotationsActive }
    val macroAcceleration by floatRange("Macro-Acceleration", 1f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val macroMaxSpeed by floatRange("Macro-MaxSpeed", 5f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val macroSmoothness by floatRange("Macro-Smoothness", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }

    val butterflyWingScale by floatRange("Butterfly-WingScale", 0.5f..Float.MAX_VALUE, 0.01f..Float.MAX_VALUE) { options.rotationsActive }
    val butterflyFlapFrequency by floatRange("Butterfly-FlapFrequency", 1f..Float.MAX_VALUE, 0.1f..Float.MAX_VALUE) { options.rotationsActive }
    val butterflyChaosFactor by floatRange("Butterfly-ChaosFactor", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val antiMLObfuscationStrength by floatRange("AntiML-ObfuscationStrength", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val antiMLTemporalNoise by floatRange("AntiML-TemporalNoise", 0.05f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val antiMLFrequencyNoise by floatRange("AntiML-FrequencyNoise", 0.05f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val antiMLBehaviorMimicry by boolean("AntiML-BehaviorMimicry", true) { options.rotationsActive }
    val antiMLPatternBreak by floatRange("AntiML-PatternBreak", 0.01f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val antiMLDistributionMatch by floatRange("AntiML-DistributionMatch", 0.5f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val antiMLEntropyTarget by floatRange("AntiML-EntropyTarget", 0.7f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val antiMLKLDivergenceLimit by floatRange("AntiML-KLDivergenceLimit", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }
    val antiMLWassersteinLimit by floatRange("AntiML-WassersteinLimit", 0.1f..Float.MAX_VALUE, 0f..Float.MAX_VALUE) { options.rotationsActive }

    val highestBodyPointToTargetValue = choices(
        "HighestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Head"
    ) { options.rotationsActive }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val lowestPoint = RotationUtils.BodyPoint.fromString(lowestBodyPointToTarget)
        RotationUtils.coerceBodyPoint(newPoint, lowestPoint, RotationUtils.BodyPoint.HEAD).displayName
    }
    val highestBodyPointToTarget: String by highestBodyPointToTargetValue

    val lowestBodyPointToTargetValue = choices(
        "LowestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Feet"
    ) { options.rotationsActive }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val highestPoint = RotationUtils.BodyPoint.fromString(highestBodyPointToTarget)
        RotationUtils.coerceBodyPoint(newPoint, RotationUtils.BodyPoint.FEET, highestPoint).displayName
    }
    val lowestBodyPointToTarget: String by lowestBodyPointToTargetValue

    val horizontalBodySearchRange by floatRange(
        "HorizontalBodySearchRange", 0f..Float.MAX_VALUE, 0f..Float.MAX_VALUE
    ) { options.rotationsActive }

    var noiseFunction: ((Rotation) -> Rotation)? = null
}