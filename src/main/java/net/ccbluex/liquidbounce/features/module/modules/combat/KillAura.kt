/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.render.NoSwing
import net.minecraft.network.play.client.C0APacketAnimation
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack.runWithSimulatedPosition
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.world.Fucker
import net.ccbluex.liquidbounce.features.module.modules.world.Nuker
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.*
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.getAttackCooldownProgress
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.resetLastAttackedTicks
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.ItemUtils.isConsumingItem
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawCircle
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isRotationFaced
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isVisible
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickedActions.nextTick
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.settings.GameSettings
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.*
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.potion.Potion
import net.minecraft.util.*
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

object KillAura : Module("KillAura", Category.COMBAT, Keyboard.KEY_R) {
    /**
     * OPTIONS
     */

    private val simulateCooldown by boolean("SimulateCooldown", false)
    private val simulateDoubleClicking by boolean("SimulateDoubleClicking", false) { !simulateCooldown }

    // CPS - Attack speed
    private val cps by intRange("CPS", 5..8, 1..50) { !simulateCooldown }.onChanged {
        attackDelay = randomClickDelay(it.first, it.last)
    }

    private val hurtTime by int("HurtTime", 10, 0..10) { !simulateCooldown }

    private val activationSlot by boolean("ActivationSlot", false)
    private val preferredSlot by int("PreferredSlot", 1, 1..9) { activationSlot }

    private val clickOnly by boolean("ClickOnly", false)

    // Range
    // TODO: Make block range independent from attack range
    private val range: Float by float("Range", 3.7f, 1f..8f).onChanged {
        blockRange = blockRange.coerceAtMost(it)
    }
    private val scanRange by float("ScanRange", 2f, 0f..10f)
    private val throughWallsRange by float("ThroughWallsRange", 3f, 0f..8f)
    private val rangeSprintReduction by float("RangeSprintReduction", 0f, 0f..0.4f)

    // Modes
    private val priority by choices(
        "Priority", arrayOf(
            "Health",
            "Distance",
            "Direction",
            "LivingTime",
            "Armor",
            "HurtResistance",
            "HurtTime",
            "HealthAbsorption",
            "RegenAmplifier",
            "OnLadder",
            "InLiquid",
            "InWeb"
        ), "Distance"
    )
    private val targetMode by choices("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val limitedMultiTargets by int("LimitedMultiTargets", 0, 0..50) { targetMode == "Multi" }
    private val maxSwitchFOV by float("MaxSwitchFOV", 90f, 30f..180f) { targetMode == "Switch" }

    // Delay
    private val switchDelay by int("SwitchDelay", 15, 1..1000) { targetMode == "Switch" }

    // Bypass
    private val swing by boolean("Swing", true)
    private val keepSprint by boolean("KeepSprint", true)

    // Settings
    private val autoF5 by boolean("AutoF5", false)
    private val onScaffold by boolean("OnScaffold", false)
    private val onDestroyBlock by boolean("OnDestroyBlock", false)

    // AutoBlock
    val autoBlock by choices("AutoBlock", arrayOf("Off", "Packet", "Fake", "QuickMacro", "BlockOnNoHit", "HurtTime"), "Packet")
    private val blockMaxRange by float("BlockMaxRange", 3f, 0f..8f) { autoBlock == "Packet" || autoBlock == "QuickMacro" }
    private val unblockMode by choices(
        "UnblockMode", arrayOf("Stop", "Switch", "Empty"), "Stop"
    ) { autoBlock == "Packet" || autoBlock == "QuickMacro" || autoBlock == "HurtTime" }
    private val releaseAutoBlock by boolean("ReleaseAutoBlock", true) { autoBlock !in arrayOf("Off", "Fake", "BlockOnNoHit", "HurtTime") }
    val forceBlockRender by boolean("ForceBlockRender", true) {
        autoBlock !in arrayOf(
            "Off", "Fake"
        ) && releaseAutoBlock
    }
    private val ignoreTickRule by boolean("IgnoreTickRule", false) {
        autoBlock !in arrayOf(
            "Off", "Fake", "HurtTime"
        ) && releaseAutoBlock
    }
    private val blockRate by int("BlockRate", 100, 1..100) { autoBlock !in arrayOf("Off", "Fake", "HurtTime") && releaseAutoBlock }

    private val uncpAutoBlock by boolean("UpdatedNCPAutoBlock", false) {
        autoBlock !in arrayOf(
            "Off", "Fake"
        ) && !releaseAutoBlock
    }

    private val switchStartBlock by boolean("SwitchStartBlock", false) { autoBlock !in arrayOf("Off", "Fake") }

    private val interactAutoBlock by boolean("InteractAutoBlock", true) { autoBlock !in arrayOf("Off", "Fake", "HurtTime") }

    val blinkAutoBlock by boolean("BlinkAutoBlock", false) { autoBlock !in arrayOf("Off", "Fake") }

    private val blinkBlockTicks by int("BlinkBlockTicks", 3, 2..5) {
        autoBlock !in arrayOf(
            "Off", "Fake"
        ) && blinkAutoBlock
    }

    // AutoBlock conditions
    private val smartAutoBlock by boolean("SmartAutoBlock", false) { autoBlock == "Packet" }
    
    // BlockOnNoHit settings
    private val blockOnNoHitMode by choices("BlockOnNoHitMode", arrayOf("Packet", "RightClick"), "Packet") { autoBlock == "BlockOnNoHit" }
    private val cancelAttackWhenBlocking by boolean("CancelAttackWhenBlocking", true) { autoBlock == "BlockOnNoHit" }
    private val blockOnNoHitDelay by int("BlockOnNoHitDelay", 1, 0..20) { autoBlock == "BlockOnNoHit" }
    
    // HurtTime AutoBlock settings
    private val hurtTimeMode by choices("HurtTimeMode", arrayOf("Custom", "Predict", "Adaptive"), "Custom") { autoBlock == "HurtTime" }
    private val hurtTimeWaitTicks by int("HurtTimeWaitTicks", 10, 1..100) { autoBlock == "HurtTime" && hurtTimeMode == "Custom" }
    private val hurtTimeBlockDuration by int("HurtTimeBlockDuration", 3, 1..20) { autoBlock == "HurtTime" }
    private val hurtTimePredictMultiplier by float("HurtTimePredictMultiplier", 0.8f, 0.5f..1.5f) { autoBlock == "HurtTime" && hurtTimeMode == "Predict" }
    private val hurtTimeAdaptiveMin by int("HurtTimeAdaptiveMin", 5, 1..50) { autoBlock == "HurtTime" && hurtTimeMode == "Adaptive" }
    private val hurtTimeAdaptiveMax by int("HurtTimeAdaptiveMax", 20, 1..50) { autoBlock == "HurtTime" && hurtTimeMode == "Adaptive" }
    private val hurtTimeAdaptiveFactor by float("HurtTimeAdaptiveFactor", 0.7f, 0.1f..1.0f) { autoBlock == "HurtTime" && hurtTimeMode == "Adaptive" }
    private val hurtTimeCancelAttack by boolean("HurtTimeCancelAttack", true) { autoBlock == "HurtTime" }

    // Ignore all blocking conditions, except for block rate, when standing still
    private val forceBlock by boolean("ForceBlockWhenStill", true) { smartAutoBlock }

    // Don't block if target isn't holding a sword or an axe
    private val checkWeapon by boolean("CheckEnemyWeapon", true) { smartAutoBlock }

    // TODO: Make block range independent from attack range
    private var blockRange: Float by float("BlockRange", range, 1f..8f) {
        smartAutoBlock
    }.onChange { _, new ->
        new.coerceAtMost(this@KillAura.range)
    }

    // Don't block when you can't get damaged
    private val maxOwnHurtTime by int("MaxOwnHurtTime", 3, 0..10) { smartAutoBlock }

    // Don't block if target isn't looking at you
    private val maxDirectionDiff by float("MaxOpponentDirectionDiff", 60f, 30f..180f) { smartAutoBlock }

    // Don't block if target is swinging an item and therefore cannot attack
    private val maxSwingProgress by int("MaxOpponentSwingProgress", 1, 0..5) { smartAutoBlock }

    // Rotations
    private val options = RotationSettings(this).withoutKeepRotation()

    // Raycast
    private val raycastValue = boolean("RayCast", true) { options.rotationsActive }
    private val raycast by raycastValue
    private val raycastIgnored by boolean(
        "RayCastIgnored", false
    ) { raycastValue.isActive() && options.rotationsActive }
    private val livingRaycast by boolean("LivingRayCast", true) { raycastValue.isActive() && options.rotationsActive }

    // Hit delay
    private val useHitDelay by boolean("UseHitDelay", false)
    private val hitDelayTicks by int("HitDelayTicks", 1, 1..5) { useHitDelay }

    private val generateClicksBasedOnDist by boolean("GenerateClicksBasedOnDistance", false)
    private val cpsMultiplier by intRange("CPS-Multiplier", 1..2, 1..10) { generateClicksBasedOnDist }
    private val distanceFactor by floatRange("DistanceFactor", 5F..10F, 1F..10F) { generateClicksBasedOnDist }

    private val generateSpotBasedOnDistance by boolean("GenerateSpotBasedOnDistance", false) { options.rotationsActive }

    private val randomization = RandomizationSettings(this) { options.rotationsActive }
    private val outBorder by boolean("OutBorder", false) { options.rotationsActive }

    private val noisePreset by multiChoices("NoisePreset", arrayOf("Custom", "Vanilla", "Legit", "SemiBlatant", "Blatant", "AntiML1", "AntiML2", "AntiML3", "AntiML4", "AntiML5", "AntiML6", "AntiML7", "AntiML8", "AntiML9", "AntiML10", "NeuralBypass", "DeepLearningBypass", "ReinforcementBypass", "GANBypass", "TransformerBypass", "LSTMBypass", "GRUBypass", "AttentionBypass", "DiffusionBypass", "FlowMatchingBypass", "SSMBypass", "MambaBypass", "RWKVBypass", "JambaBypass", "MixtralBypass", "MoEBypass", "ChaosMax", "MicroJitter", "MacroSim", "HumanSim", "RobotSim", "Butterfly", "SineWave", "Sawtooth", "SquareWave", "TriangleWave", "PulseJitter", "FractalNoise", "WaveletNoise", "OrnsteinUhlenbeck", "LevyFlight", "PoissonJitter", "GammaBurst", "BetaBlend", "WeibullDrift", "CauchyJump", "StudentTNoise", "LogNormalPulse", "ExponentialDecay", "DoubleExponential", "GaussianMixture", "LaplaceShock", "VonMisesWrap", "DirichletMix", "MultinomialPick", "UniformBand", "TriangularTaper", "PiecewiseLinear", "CubicSpline", "BSplineWiggle", "NURBSCurve", "RBFInterpolation", "KNNBlend", "SVMDecision", "RandomForestAim", "XGBoostAim", "GradientBoostAim", "AdaBoostAim", "BaggingAim", "StackingAim", "VotingAim", "BayesianOptAim", "GaussianProcessAim", "MCMCSampleAim", "ParticleFilterAim", "KalmanFilterAim", "ExtendedKalmanAim", "UnscentedKalmanAim", "EnsembleKalmanAim", "ParticleSwarmAim", "AntColonyAim", "SimulatedAnnealingAim", "GeneticAlgorithmAim", "DifferentialEvolutionAim", "CMAESAim", "HillClimbingAim", "RandomSearchAim", "GridSearchAim", "BayesianSearchAim", "HyperbandAim", "BOHBAim"), setOf("Custom")) { options.rotationsActive }

    private val yawMicroJitter by floatRange("YawMicroJitter", 0f..0f, 0f..5f) { options.rotationsActive }
    private val pitchMicroJitter by floatRange("PitchMicroJitter", 0f..0f, 0f..5f) { options.rotationsActive }
    private val yawMacroJitter by floatRange("YawMacroJitter", 0f..0f, 0f..30f) { options.rotationsActive }
    private val pitchMacroJitter by floatRange("PitchMacroJitter", 0f..0f, 0f..30f) { options.rotationsActive }
    private val jitterDecayRate by floatRange("JitterDecayRate", 0.95f..0.99f, 0.5f..1f) { options.rotationsActive }
    private val jitterBurstProbability by floatRange("JitterBurstProbability", 0f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val jitterBurstStrength by floatRange("JitterBurstStrength", 1f..3f, 0f..10f) { options.rotationsActive }
    private val jitterAdaptiveDecay by boolean("JitterAdaptiveDecay", false) { options.rotationsActive }
    private val jitterTimeCorrelation by floatRange("JitterTimeCorrelation", 0f..0.3f, 0f..1f) { options.rotationsActive }
    private val jitterAxisCoupling by floatRange("JitterAxisCoupling", 0f..0.2f, 0f..1f) { options.rotationsActive }

    private val sineAmplitude by floatRange("SineAmplitude", 0f..0f, 0f..10f) { options.rotationsActive }
    private val sineFrequency by floatRange("SineFrequency", 0.5f..2f, 0.01f..20f) { options.rotationsActive }
    private val sinePhaseOffset by floatRange("SinePhaseOffset", 0f..0f, 0f..6.28f) { options.rotationsActive }
    private val sinePitchRatio by floatRange("SinePitchRatio", 0.5f..0.5f, 0f..2f) { options.rotationsActive }
    private val sineHarmonicCount by intRange("SineHarmonicCount", 1..1, 1..10) { options.rotationsActive }
    private val sineHarmonicDecay by floatRange("SineHarmonicDecay", 0.5f..0.5f, 0.1f..1f) { options.rotationsActive }

    private val sawtoothAmplitude by floatRange("SawtoothAmplitude", 0f..0f, 0f..10f) { options.rotationsActive }
    private val sawtoothFrequency by floatRange("SawtoothFrequency", 0.5f..2f, 0.01f..20f) { options.rotationsActive }
    private val sawtoothPitchRatio by floatRange("SawtoothPitchRatio", 0.5f..0.5f, 0f..2f) { options.rotationsActive }

    private val squareAmplitude by floatRange("SquareAmplitude", 0f..0f, 0f..10f) { options.rotationsActive }
    private val squareFrequency by floatRange("SquareFrequency", 0.5f..2f, 0.01f..20f) { options.rotationsActive }
    private val squareDutyCycle by floatRange("SquareDutyCycle", 0.5f..0.5f, 0.01f..0.99f) { options.rotationsActive }
    private val squarePitchRatio by floatRange("SquarePitchRatio", 0.5f..0.5f, 0f..2f) { options.rotationsActive }

    private val triangleAmplitude by floatRange("TriangleAmplitude", 0f..0f, 0f..10f) { options.rotationsActive }
    private val triangleFrequency by floatRange("TriangleFrequency", 0.5f..2f, 0.01f..20f) { options.rotationsActive }
    private val trianglePitchRatio by floatRange("TrianglePitchRatio", 0.5f..0.5f, 0f..2f) { options.rotationsActive }

    private val pulseJitterAmplitude by floatRange("PulseJitterAmplitude", 0f..0f, 0f..15f) { options.rotationsActive }
    private val pulseJitterInterval by intRange("PulseJitterInterval", 5..10, 1..50) { options.rotationsActive }
    private val pulseJitterDecay by floatRange("PulseJitterDecay", 0.8f..0.9f, 0.1f..1f) { options.rotationsActive }

    private val ouTheta by floatRange("OU-Theta", 0.1f..0.3f, 0.01f..2f) { options.rotationsActive }
    private val ouMu by floatRange("OU-Mu", 0f..0f, -5f..5f) { options.rotationsActive }
    private val ouSigma by floatRange("OU-Sigma", 0.01f..0.05f, 0f..2f) { options.rotationsActive }
    private val ouYawScale by floatRange("OU-YawScale", 1f..1f, 0f..10f) { options.rotationsActive }
    private val ouPitchScale by floatRange("OU-PitchScale", 0.5f..0.5f, 0f..10f) { options.rotationsActive }

    private val levyAlpha by floatRange("Levy-Alpha", 1.5f..1.8f, 0.5f..2f) { options.rotationsActive }
    private val levyScale by floatRange("Levy-Scale", 0.01f..0.1f, 0f..5f) { options.rotationsActive }
    private val levyYawWeight by floatRange("Levy-YawWeight", 1f..1f, 0f..5f) { options.rotationsActive }
    private val levyPitchWeight by floatRange("Levy-PitchWeight", 0.5f..0.5f, 0f..5f) { options.rotationsActive }

    private val poissonLambda by floatRange("Poisson-Lambda", 1f..3f, 0.1f..10f) { options.rotationsActive }
    private val poissonJitterScale by floatRange("Poisson-JitterScale", 0.1f..0.5f, 0f..5f) { options.rotationsActive }

    private val gammaShape by floatRange("Gamma-Shape", 2f..5f, 0.1f..20f) { options.rotationsActive }
    private val gammaScale by floatRange("Gamma-Scale", 0.01f..0.05f, 0.001f..1f) { options.rotationsActive }
    private val gammaBurstThreshold by floatRange("Gamma-BurstThreshold", 3f..5f, 1f..20f) { options.rotationsActive }

    private val betaAlpha by floatRange("Beta-Alpha", 2f..5f, 0.1f..10f) { options.rotationsActive }
    private val betaBetaParam by floatRange("Beta-Beta", 2f..5f, 0.1f..10f) { options.rotationsActive }
    private val betaBlendStrength by floatRange("Beta-BlendStrength", 0.1f..0.3f, 0f..1f) { options.rotationsActive }

    private val weibullShape by floatRange("Weibull-Shape", 1f..3f, 0.1f..10f) { options.rotationsActive }
    private val weibullScale by floatRange("Weibull-Scale", 0.5f..2f, 0.01f..10f) { options.rotationsActive }
    private val weibullDriftRate by floatRange("Weibull-DriftRate", 0.01f..0.05f, 0f..1f) { options.rotationsActive }

    private val cauchyScale by floatRange("Cauchy-Scale", 0.01f..0.1f, 0.001f..5f) { options.rotationsActive }
    private val cauchyJumpProbability by floatRange("Cauchy-JumpProbability", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }

    private val studentTDof by floatRange("StudentT-DOF", 3f..7f, 1f..30f) { options.rotationsActive }
    private val studentTScale by floatRange("StudentT-Scale", 0.1f..0.5f, 0.01f..5f) { options.rotationsActive }

    private val logNormalMu by floatRange("LogNormal-Mu", -1f..0f, -5f..5f) { options.rotationsActive }
    private val logNormalSigma by floatRange("LogNormal-Sigma", 0.5f..1f, 0.01f..3f) { options.rotationsActive }
    private val logNormalPulseRate by floatRange("LogNormal-PulseRate", 0.01f..0.05f, 0f..1f) { options.rotationsActive }

    private val expDecayRate by floatRange("ExpDecay-Rate", 0.5f..2f, 0.01f..10f) { options.rotationsActive }
    private val expDecayAmplitude by floatRange("ExpDecay-Amplitude", 0.5f..2f, 0f..10f) { options.rotationsActive }
    private val expDecayTriggerProb by floatRange("ExpDecay-TriggerProbability", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }

    private val doubleExpScale by floatRange("DoubleExp-Scale", 0.5f..1f, 0.01f..5f) { options.rotationsActive }
    private val doubleExpAsymmetry by floatRange("DoubleExp-Asymmetry", 0f..0.3f, -1f..1f) { options.rotationsActive }

    private val gaussianMixWeight1 by floatRange("GaussianMix-Weight1", 0.3f..0.7f, 0f..1f) { options.rotationsActive }
    private val gaussianMixMu1 by floatRange("GaussianMix-Mu1", -1f..0f, -5f..5f) { options.rotationsActive }
    private val gaussianMixSigma1 by floatRange("GaussianMix-Sigma1", 0.1f..0.3f, 0.01f..5f) { options.rotationsActive }
    private val gaussianMixMu2 by floatRange("GaussianMix-Mu2", 0f..1f, -5f..5f) { options.rotationsActive }
    private val gaussianMixSigma2 by floatRange("GaussianMix-Sigma2", 0.3f..0.8f, 0.01f..5f) { options.rotationsActive }

    private val laplaceMu by floatRange("Laplace-Mu", 0f..0f, -5f..5f) { options.rotationsActive }
    private val laplaceB by floatRange("Laplace-B", 0.1f..0.3f, 0.01f..5f) { options.rotationsActive }
    private val laplaceShockProb by floatRange("Laplace-ShockProbability", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val laplaceShockScale by floatRange("Laplace-ShockScale", 2f..5f, 0f..20f) { options.rotationsActive }

    private val vonMisesMu by floatRange("VonMises-Mu", 0f..0f, -3.14f..3.14f) { options.rotationsActive }
    private val vonMisesKappa by floatRange("VonMises-Kappa", 1f..3f, 0f..20f) { options.rotationsActive }

    private val uniformBandWidth by floatRange("UniformBand-Width", 0.5f..2f, 0f..10f) { options.rotationsActive }
    private val uniformBandCenter by floatRange("UniformBand-Center", 0f..0f, -5f..5f) { options.rotationsActive }
    private val uniformBandPitchRatio by floatRange("UniformBand-PitchRatio", 0.5f..0.5f, 0f..2f) { options.rotationsActive }

    private val triangularPeak by floatRange("Triangular-Peak", 0f..0f, -5f..5f) { options.rotationsActive }
    private val triangularWidth by floatRange("Triangular-Width", 1f..3f, 0.1f..10f) { options.rotationsActive }

    private val cubicSplineTension by floatRange("CubicSpline-Tension", 0.3f..0.7f, 0f..2f) { options.rotationsActive }
    private val cubicSplineSmoothness by floatRange("CubicSpline-Smoothness", 0.5f..1f, 0.01f..5f) { options.rotationsActive }
    private val cubicSplineControlPoints by intRange("CubicSpline-ControlPoints", 4..8, 2..20) { options.rotationsActive }

    private val bSplineDegree by intRange("BSpline-Degree", 3..3, 1..5) { options.rotationsActive }
    private val bSplineKnotDensity by floatRange("BSpline-KnotDensity", 0.5f..1f, 0.1f..3f) { options.rotationsActive }
    private val bSplineWiggleAmplitude by floatRange("BSpline-WiggleAmplitude", 0f..0f, 0f..5f) { options.rotationsActive }

    private val rbfKernelWidth by floatRange("RBF-KernelWidth", 0.5f..1f, 0.01f..10f) { options.rotationsActive }
    private val rbfRegularization by floatRange("RBF-Regularization", 0.01f..0.1f, 0.001f..1f) { options.rotationsActive }
    private val rbfCenterCount by intRange("RBF-CenterCount", 5..10, 2..30) { options.rotationsActive }

    private val kalmanProcessNoise by floatRange("Kalman-ProcessNoise", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val kalmanMeasurementNoise by floatRange("Kalman-MeasurementNoise", 0.1f..0.5f, 0f..5f) { options.rotationsActive }
    private val kalmanInitialUncertainty by floatRange("Kalman-InitialUncertainty", 1f..5f, 0.1f..100f) { options.rotationsActive }
    private val kalmanAdaptive by boolean("Kalman-Adaptive", false) { options.rotationsActive }
    private val kalmanAdaptiveRate by floatRange("Kalman-AdaptiveRate", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive && kalmanAdaptive }

    private val particleCount by intRange("ParticleFilter-Count", 50..100, 10..500) { options.rotationsActive }
    private val particleResampleThreshold by floatRange("ParticleFilter-ResampleThreshold", 0.5f..0.7f, 0.1f..1f) { options.rotationsActive }
    private val particleDiffusion by floatRange("ParticleFilter-Diffusion", 0.01f..0.05f, 0f..1f) { options.rotationsActive }

    private val swarmParticleCount by intRange("Swarm-ParticleCount", 20..40, 5..200) { options.rotationsActive }
    private val swarmInertia by floatRange("Swarm-Inertia", 0.5f..0.7f, 0f..1f) { options.rotationsActive }
    private val swarmCognitive by floatRange("Swarm-Cognitive", 1f..2f, 0f..5f) { options.rotationsActive }
    private val swarmSocial by floatRange("Swarm-Social", 1f..2f, 0f..5f) { options.rotationsActive }
    private val swarmMaxVelocity by floatRange("Swarm-MaxVelocity", 0.1f..0.5f, 0.01f..5f) { options.rotationsActive }

    private val annealingInitialTemp by floatRange("Annealing-InitialTemp", 1f..5f, 0.1f..100f) { options.rotationsActive }
    private val annealingCoolingRate by floatRange("Annealing-CoolingRate", 0.95f..0.99f, 0.5f..0.999f) { options.rotationsActive }
    private val annealingMinTemp by floatRange("Annealing-MinTemp", 0.01f..0.05f, 0.001f..1f) { options.rotationsActive }

    private val geneticPopSize by intRange("Genetic-PopSize", 20..50, 5..200) { options.rotationsActive }
    private val geneticMutationRate by floatRange("Genetic-MutationRate", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val geneticCrossoverRate by floatRange("Genetic-CrossoverRate", 0.7f..0.9f, 0f..1f) { options.rotationsActive }
    private val geneticElitism by intRange("Genetic-Elitism", 1..2, 0..10) { options.rotationsActive }

    private val cmaesPopulation by intRange("CMAES-Population", 10..20, 5..100) { options.rotationsActive }
    private val cmaesInitialStep by floatRange("CMAES-InitialStepSize", 0.5f..1f, 0.01f..10f) { options.rotationsActive }

    private val bayesianExploration by floatRange("Bayesian-Exploration", 0.1f..0.3f, 0f..1f) { options.rotationsActive }
    private val bayesianExploitation by floatRange("Bayesian-Exploitation", 0.7f..0.9f, 0f..1f) { options.rotationsActive }
    private val bayesianGPNoise by floatRange("Bayesian-GPNoise", 0.01f..0.05f, 0f..1f) { options.rotationsActive }

    private val fractalDimension by floatRange("Fractal-Dimension", 1.5f..2.5f, 0.5f..3f) { options.rotationsActive }
    private val fractalOctaves by intRange("Fractal-Octaves", 3..6, 1..10) { options.rotationsActive }
    private val fractalLacunarity by floatRange("Fractal-Lacunarity", 2f..3f, 1f..5f) { options.rotationsActive }
    private val fractalGain by floatRange("Fractal-Gain", 0.5f..0.7f, 0.1f..1f) { options.rotationsActive }

    private val waveletType by choices("Wavelet-Type", arrayOf("Haar", "Daubechies4", "Symlet4", "Coiflet3", "Biorthogonal"), "Haar") { options.rotationsActive }
    private val waveletDecompositionLevel by intRange("Wavelet-DecompositionLevel", 2..4, 1..8) { options.rotationsActive }
    private val waveletThreshold by floatRange("Wavelet-Threshold", 0.1f..0.3f, 0f..1f) { options.rotationsActive }

    private val humanReactionDelay by intRange("Human-ReactionDelay", 50..150, 0..500, "ms") { options.rotationsActive }
    private val humanAccelerationPhase by floatRange("Human-AccelerationPhase", 0.3f..0.5f, 0f..1f) { options.rotationsActive }
    private val humanDecelerationPhase by floatRange("Human-DecelerationPhase", 0.5f..0.7f, 0f..1f) { options.rotationsActive }
    private val humanOvershootProbability by floatRange("Human-OvershootProbability", 0.1f..0.3f, 0f..1f) { options.rotationsActive }
    private val humanOvershootAmount by floatRange("Human-OvershootAmount", 1f..3f, 0f..10f) { options.rotationsActive }
    private val humanMicroCorrection by floatRange("Human-MicroCorrection", 0.1f..0.3f, 0f..2f) { options.rotationsActive }
    private val humanFatigueRate by floatRange("Human-FatigueRate", 0.001f..0.01f, 0f..0.1f) { options.rotationsActive }
    private val humanVariability by floatRange("Human-Variability", 0.5f..1.5f, 0f..5f) { options.rotationsActive }

    private val macroStepInterval by intRange("Macro-StepInterval", 50..100, 10..500, "ms") { options.rotationsActive }
    private val macroAcceleration by floatRange("Macro-Acceleration", 1f..2f, 0.1f..10f) { options.rotationsActive }
    private val macroMaxSpeed by floatRange("Macro-MaxSpeed", 5f..10f, 0.1f..30f) { options.rotationsActive }
    private val macroSmoothness by floatRange("Macro-Smoothness", 0.5f..0.8f, 0.01f..1f) { options.rotationsActive }

    private val butterflyWingScale by floatRange("Butterfly-WingScale", 0.5f..1f, 0.01f..5f) { options.rotationsActive }
    private val butterflyFlapFrequency by floatRange("Butterfly-FlapFrequency", 1f..3f, 0.1f..10f) { options.rotationsActive }
    private val butterflyChaosFactor by floatRange("Butterfly-ChaosFactor", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }

    private val antiMLObfuscationStrength by floatRange("AntiML-ObfuscationStrength", 0.1f..0.3f, 0f..1f) { options.rotationsActive }
    private val antiMLTemporalNoise by floatRange("AntiML-TemporalNoise", 0.05f..0.15f, 0f..1f) { options.rotationsActive }
    private val antiMLFrequencyNoise by floatRange("AntiML-FrequencyNoise", 0.05f..0.1f, 0f..1f) { options.rotationsActive }
    private val antiMLBehaviorMimicry by boolean("AntiML-BehaviorMimicry", true) { options.rotationsActive }
    private val antiMLPatternBreak by floatRange("AntiML-PatternBreak", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val antiMLDistributionMatch by floatRange("AntiML-DistributionMatch", 0.5f..0.8f, 0f..1f) { options.rotationsActive }
    private val antiMLEntropyTarget by floatRange("AntiML-EntropyTarget", 0.7f..0.9f, 0f..1f) { options.rotationsActive }
    private val antiMLKLDivergenceLimit by floatRange("AntiML-KLDivergenceLimit", 0.1f..0.3f, 0f..2f) { options.rotationsActive }
    private val antiMLWassersteinLimit by floatRange("AntiML-WassersteinLimit", 0.1f..0.3f, 0f..2f) { options.rotationsActive }

    private val antiMLAdversarialEpsilon by floatRange("AntiML-AdversarialEpsilon", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val antiMLAdversarialSteps by intRange("AntiML-AdversarialSteps", 3..7, 1..20) { options.rotationsActive }
    private val antiMLAdversarialNorm by choices("AntiML-AdversarialNorm", arrayOf("L1", "L2", "Linf"), "Linf") { options.rotationsActive }
    private val antiMLPGDAttackScale by floatRange("AntiML-PGDAttackScale", 0.01f..0.03f, 0f..0.5f) { options.rotationsActive }
    private val antiMLFGSMNoiseScale by floatRange("AntiML-FGSMNoiseScale", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val antiMLCWL2Confidence by floatRange("AntiML-CWL2-Confidence", 0f..10f, 0f..100f) { options.rotationsActive }
    private val antiMLCWL2LearningRate by floatRange("AntiML-CWL2-LR", 0.01f..0.05f, 0.001f..1f) { options.rotationsActive }
    private val antiMLDeepFoolMaxIter by intRange("AntiML-DeepFool-MaxIter", 10..50, 1..200) { options.rotationsActive }
    private val antiMLDeepFoolOvershoot by floatRange("AntiML-DeepFool-Overshoot", 0.02f..0.1f, 0f..1f) { options.rotationsActive }
    private val antiMLAutoAttackVersion by choices("AntiML-AutoAttack-Version", arrayOf("Standard", "Plus", "Rand"), "Standard") { options.rotationsActive }
    private val antiMLAutoAttackEps by floatRange("AntiML-AutoAttack-Eps", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val antiMLBoundaryAttackInitRadius by floatRange("AntiML-BoundaryAttack-InitRadius", 1f..3f, 0.1f..10f) { options.rotationsActive }
    private val antiMLBoundaryAttackSteps by intRange("AntiML-BoundaryAttack-Steps", 100..500, 10..5000) { options.rotationsActive }
    private val antiMLSquareAttackP by floatRange("AntiML-SquareAttack-P", 0.05f..0.15f, 0.01f..0.5f) { options.rotationsActive }
    private val antiMLFeatureSqueezingBitDepth by intRange("AntiML-FeatureSqueezing-BitDepth", 3..5, 1..8) { options.rotationsActive }
    private val antiMLFeatureSqueezingMedianSmooth by intRange("AntiML-FeatureSqueezing-MedianWindow", 3..5, 1..11) { options.rotationsActive }
    private val antiMLMagNetAutoencoderLR by floatRange("AntiML-MagNet-AE-LR", 0.001f..0.01f, 0.0001f..0.1f) { options.rotationsActive }
    private val antiMLMagNetReformerLR by floatRange("AntiML-MagNet-RF-LR", 0.001f..0.01f, 0.0001f..0.1f) { options.rotationsActive }
    private val antiMLMagNetThreshold by floatRange("AntiML-MagNet-Threshold", 0.5f..0.8f, 0.1f..1f) { options.rotationsActive }
    private val antiMLGaussianSmoothingSigma by floatRange("AntiML-GaussianSmoothing-Sigma", 0.1f..0.5f, 0.01f..5f) { options.rotationsActive }
    private val antiMLGaussianSmoothingSamples by intRange("AntiML-GaussianSmoothing-Samples", 10..30, 1..100) { options.rotationsActive }
    private val antiMLRandomizedSmoothingSigma by floatRange("AntiML-RandomizedSmoothing-Sigma", 0.25f..0.5f, 0.01f..5f) { options.rotationsActive }
    private val antiMLRandomizedSmoothingNCertify by intRange("AntiML-RandomizedSmoothing-NCertify", 100..500, 10..10000) { options.rotationsActive }
    private val antiMLInputTransformRotateMax by floatRange("AntiML-InputTransform-RotateMax", 5f..15f, 0f..90f) { options.rotationsActive }
    private val antiMLInputTransformTranslateMax by floatRange("AntiML-InputTransform-TranslateMax", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val antiMLInputTransformScaleRange by floatRange("AntiML-InputTransform-ScaleRange", 0.9f..1.1f, 0.5f..2f) { options.rotationsActive }
    private val antiMLInputTransformBitDepth by intRange("AntiML-InputTransform-BitDepth", 4..6, 1..8) { options.rotationsActive }
    private val antiMLMixupAlpha by floatRange("AntiML-Mixup-Alpha", 0.1f..0.5f, 0f..5f) { options.rotationsActive }
    private val antiMLCutMixAlpha by floatRange("AntiML-CutMix-Alpha", 0.1f..0.5f, 0f..5f) { options.rotationsActive }
    private val antiMLCutMixProb by floatRange("AntiML-CutMix-Probability", 0.1f..0.3f, 0f..1f) { options.rotationsActive }
    private val antiMLLabelSmoothing by floatRange("AntiML-LabelSmoothing", 0.05f..0.15f, 0f..0.5f) { options.rotationsActive }
    private val antiMLVirtualAdversarialEps by floatRange("AntiML-VAT-Epsilon", 1f..5f, 0.1f..20f) { options.rotationsActive }
    private val antiMLVirtualAdversarialXI by floatRange("AntiML-VAT-Xi", 1e-4f..1e-3f, 1e-6f..0.01f) { options.rotationsActive }
    private val antiMLVirtualAdversarialIP by intRange("AntiML-VAT-NumPowerIter", 1..3, 1..10) { options.rotationsActive }
    private val antiMLPerturbationBudget by floatRange("AntiML-PerturbationBudget", 0.05f..0.15f, 0f..1f) { options.rotationsActive }
    private val antiMLPerturbationDecay by floatRange("AntiML-PerturbationDecay", 0.95f..0.99f, 0.5f..1f) { options.rotationsActive }
    private val antiMLPerturbationRegrowRate by floatRange("AntiML-PerturbationRegrowRate", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val antiMLTemporalCorrelationWindow by intRange("AntiML-TemporalCorrelation-Window", 10..30, 1..100) { options.rotationsActive }
    private val antiMLTemporalCorrelationDecay by floatRange("AntiML-TemporalCorrelation-Decay", 0.9f..0.98f, 0.5f..1f) { options.rotationsActive }
    private val antiMLTemporalCorrelationMaxLag by intRange("AntiML-TemporalCorrelation-MaxLag", 5..20, 1..50) { options.rotationsActive }
    private val antiMLAutocorrelationTarget by floatRange("AntiML-Autocorrelation-Target", 0.1f..0.3f, -1f..1f) { options.rotationsActive }
    private val antiMLAutocorrelationTolerance by floatRange("AntiML-Autocorrelation-Tolerance", 0.05f..0.15f, 0f..1f) { options.rotationsActive }
    private val antiMLPartialAutocorrelationLimit by intRange("AntiML-PartialAutocorrelation-Limit", 5..10, 1..30) { options.rotationsActive }
    private val antiMLSpectralDensityTarget by floatRange("AntiML-SpectralDensity-Target", 0.5f..0.8f, 0f..1f) { options.rotationsActive }
    private val antiMLSpectralDensityBandwidth by floatRange("AntiML-SpectralDensity-Bandwidth", 0.05f..0.15f, 0.01f..1f) { options.rotationsActive }
    private val antiMLSpectralLeakageMax by floatRange("AntiML-SpectralLeakage-Max", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val antiMLWaveletScatteringJ by intRange("AntiML-WaveletScattering-J", 2..4, 1..8) { options.rotationsActive }
    private val antiMLWaveletScatteringL by intRange("AntiML-WaveletScattering-L", 2..4, 1..8) { options.rotationsActive }
    private val antiMLWaveletScatteringMaxOrder by intRange("AntiML-WaveletScattering-MaxOrder", 1..2, 1..3) { options.rotationsActive }
    private val antiMLFourierFeatureCount by intRange("AntiML-FourierFeatures-Count", 10..30, 1..100) { options.rotationsActive }
    private val antiMLFourierFeatureScale by floatRange("AntiML-FourierFeatures-Scale", 0.5f..2f, 0.01f..10f) { options.rotationsActive }
    private val antiMLFourierFeatureDecay by floatRange("AntiML-FourierFeatures-Decay", 0.5f..0.8f, 0.1f..1f) { options.rotationsActive }
    private val antiMLMutualInfoTarget by floatRange("AntiML-MutualInformation-Target", 0.3f..0.6f, 0f..2f) { options.rotationsActive }
    private val antiMLMutualInfoBins by intRange("AntiML-MutualInformation-Bins", 10..30, 2..100) { options.rotationsActive }
    private val antiMLMutualInfoK by intRange("AntiML-MutualInformation-KNN", 3..7, 1..30) { options.rotationsActive }
    private val antiMLTransferabilityCrossModel by boolean("AntiML-Transferability-CrossModel", true) { options.rotationsActive }
    private val antiMLTransferabilityEnsembleSize by intRange("AntiML-Transferability-EnsembleSize", 3..5, 1..10) { options.rotationsActive }
    private val antiMLTransferabilityDiversityWeight by floatRange("AntiML-Transferability-DiversityWeight", 0.1f..0.3f, 0f..1f) { options.rotationsActive }
    private val antiMLTransferabilityMIWeight by floatRange("AntiML-Transferability-MIWeight", 0.2f..0.5f, 0f..1f) { options.rotationsActive }
    private val antiMLBlackBoxQueryBudget by intRange("AntiML-BlackBox-QueryBudget", 100..500, 10..10000) { options.rotationsActive }
    private val antiMLBlackBoxEstimationVariance by floatRange("AntiML-BlackBox-EstimationVariance", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val antiMLBlackBoxFiniteDiffEps by floatRange("AntiML-BlackBox-FiniteDiffEps", 0.01f..0.05f, 0.001f..1f) { options.rotationsActive }
    private val antiMLBlackBoxNaturalEvolution by boolean("AntiML-BlackBox-NaturalEvolution", true) { options.rotationsActive }
    private val antiMLBlackBoxPopulationSize by intRange("AntiML-BlackBox-PopulationSize", 20..50, 5..200) { options.rotationsActive }
    private val antiMLEvasionRobustnessRadius by floatRange("AntiML-Evasion-RobustnessRadius", 0.05f..0.15f, 0f..1f) { options.rotationsActive }
    private val antiMLEvasionCertificationMethod by choices("AntiML-Evasion-CertMethod", arrayOf("RandomizedSmoothing", "IntervalBound", "Linear", "CROWN", "Beta-CROWN"), "RandomizedSmoothing") { options.rotationsActive }
    private val antiMLEvasionMaxCertTime by intRange("AntiML-Evasion-MaxCertTime", 100..500, 10..5000, "ms") { options.rotationsActive }
    private val antiMLPoisoningInjectionRate by floatRange("AntiML-Poisoning-InjectionRate", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val antiMLPoisoningOptimizationSteps by intRange("AntiML-Poisoning-OptSteps", 50..200, 10..1000) { options.rotationsActive }
    private val antiMLPoisoningTargetLabel by intRange("AntiML-Poisoning-TargetLabel", 0..0, 0..10) { options.rotationsActive }
    private val antiMLBackdoorTriggerSize by floatRange("AntiML-Backdoor-TriggerSize", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val antiMLBackdoorTriggerPosition by choices("AntiML-Backdoor-TriggerPos", arrayOf("Start", "Middle", "End", "Random"), "Random") { options.rotationsActive }
    private val antiMLBackdoorProbability by floatRange("AntiML-Backdoor-Probability", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val antiMLModelExtractionQueries by intRange("AntiML-ModelExtraction-Queries", 100..500, 10..10000) { options.rotationsActive }
    private val antiMLModelExtractionFidelityTarget by floatRange("AntiML-ModelExtraction-FidelityTarget", 0.8f..0.95f, 0f..1f) { options.rotationsActive }
    private val antiMLMembershipInferenceThreshold by floatRange("AntiML-MembershipInference-Threshold", 0.5f..0.7f, 0f..1f) { options.rotationsActive }
    private val antiMLMembershipInferenceShadowModels by intRange("AntiML-MembershipInference-ShadowModels", 3..5, 1..10) { options.rotationsActive }
    private val antiMLModelInversionLR by floatRange("AntiML-ModelInversion-LR", 0.01f..0.1f, 0.001f..1f) { options.rotationsActive }
    private val antiMLModelInversionIter by intRange("AntiML-ModelInversion-Iter", 100..500, 10..5000) { options.rotationsActive }
    private val antiMLAttributeInferenceTargetAttr by intRange("AntiML-AttributeInference-TargetAttr", 0..0, 0..20) { options.rotationsActive }
    private val antiMLAttributeInferenceConfidence by floatRange("AntiML-AttributeInference-Confidence", 0.7f..0.9f, 0f..1f) { options.rotationsActive }
    private val antiMLGANGeneratorLR by floatRange("AntiML-GAN-GeneratorLR", 0.0001f..0.001f, 1e-5f..0.01f) { options.rotationsActive }
    private val antiMLGANDiscriminatorLR by floatRange("AntiML-GAN-DiscriminatorLR", 0.0001f..0.001f, 1e-5f..0.01f) { options.rotationsActive }
    private val antiMLGANLatentDim by intRange("AntiML-GAN-LatentDim", 8..16, 2..128) { options.rotationsActive }
    private val antiMLGANHiddenDim by intRange("AntiML-GAN-HiddenDim", 32..64, 8..256) { options.rotationsActive }
    private val antiMLGANBatchSize by intRange("AntiML-GAN-BatchSize", 16..32, 1..128) { options.rotationsActive }
    private val antiMLGANNormType by choices("AntiML-GAN-NormType", arrayOf("None", "BatchNorm", "LayerNorm", "InstanceNorm", "GroupNorm"), "BatchNorm") { options.rotationsActive }
    private val antiMLGANActivation by choices("AntiML-GAN-Activation", arrayOf("ReLU", "LeakyReLU", "ELU", "GELU", "SiLU", "Mish"), "LeakyReLU") { options.rotationsActive }
    private val antiMLGANLossType by choices("AntiML-GAN-LossType", arrayOf("Vanilla", "Wasserstein", "LSGAN", "Hinge"), "Wasserstein") { options.rotationsActive }
    private val antiMLGANGPWeight by floatRange("AntiML-GAN-GPWeight", 10f..20f, 0f..100f) { options.rotationsActive }
    private val antiMLGANSpectralNorm by boolean("AntiML-GAN-SpectralNorm", true) { options.rotationsActive }
    private val antiMLGANNoiseInjection by boolean("AntiML-GAN-NoiseInjection", true) { options.rotationsActive }
    private val antiMLGANNoiseStd by floatRange("AntiML-GAN-NoiseStd", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val antiMLVAELatentDim by intRange("AntiML-VAE-LatentDim", 8..16, 2..128) { options.rotationsActive }
    private val antiMLVAEHiddenDim by intRange("AntiML-VAE-HiddenDim", 32..64, 8..256) { options.rotationsActive }
    private val antiMLVAELearningRate by floatRange("AntiML-VAE-LR", 0.0001f..0.001f, 1e-5f..0.01f) { options.rotationsActive }
    private val antiMLVAEBeta by floatRange("AntiML-VAE-Beta", 1f..4f, 0f..10f) { options.rotationsActive }
    private val antiMLVAEKLDWeight by floatRange("AntiML-VAE-KLDWeight", 0.5f..1f, 0f..5f) { options.rotationsActive }
    private val antiMLVAEFreeBits by floatRange("AntiML-VAE-FreeBits", 0f..0.1f, 0f..1f) { options.rotationsActive }
    private val antiMLVAEIWAEsamples by intRange("AntiML-VAE-IWAE-Samples", 5..10, 1..50) { options.rotationsActive }
    private val antiMLTransformerDModel by intRange("AntiML-Transformer-dModel", 32..64, 8..256) { options.rotationsActive }
    private val antiMLTransformerNHead by intRange("AntiML-Transformer-nHead", 2..4, 1..16) { options.rotationsActive }
    private val antiMLTransformerNumLayers by intRange("AntiML-Transformer-numLayers", 2..4, 1..12) { options.rotationsActive }
    private val antiMLTransformerDimFF by intRange("AntiML-Transformer-dimFF", 128..256, 16..1024) { options.rotationsActive }
    private val antiMLTransformerDropout by floatRange("AntiML-Transformer-Dropout", 0.1f..0.2f, 0f..0.5f) { options.rotationsActive }
    private val antiMLTransformerAttentionType by choices("AntiML-Transformer-AttentionType", arrayOf("ScaledDot", "MultiHead", "Linear", "Performer", "Flash", "Sparse", "Local", "Axial", "Perceiver"), "MultiHead") { options.rotationsActive }
    private val antiMLTransformerPosEnc by choices("AntiML-Transformer-PosEnc", arrayOf("Sinusoidal", "Learned", "RoPE", "ALiBi", "xPos"), "Sinusoidal") { options.rotationsActive }
    private val antiMLTransformerActivation by choices("AntiML-Transformer-Activation", arrayOf("ReLU", "GELU", "SwiGLU", "GeGLU", "ReLU2"), "GELU") { options.rotationsActive }
    private val antiMLTransformerNormType by choices("AntiML-Transformer-NormType", arrayOf("LayerNorm", "RMSNorm", "BatchNorm", "GroupNorm", "DeepNorm"), "LayerNorm") { options.rotationsActive }
    private val antiMLTransformerPreNorm by boolean("AntiML-Transformer-PreNorm", true) { options.rotationsActive }
    private val antiMLLSTMHiddenSize by intRange("AntiML-LSTM-HiddenSize", 32..64, 8..256) { options.rotationsActive }
    private val antiMLLSTMNumLayers by intRange("AntiML-LSTM-NumLayers", 1..3, 1..6) { options.rotationsActive }
    private val antiMLLSTMDropout by floatRange("AntiML-LSTM-Dropout", 0f..0.1f, 0f..0.5f) { options.rotationsActive }
    private val antiMLLSTMBidirectional by boolean("AntiML-LSTM-Bidirectional", false) { options.rotationsActive }
    private val antiMLLSTMCellType by choices("AntiML-LSTM-CellType", arrayOf("Vanilla", "Peephole", "GatedRecurrent", "NAS", "CoupledInputForgetGate"), "Vanilla") { options.rotationsActive }
    private val antiMLGRUHiddenSize by intRange("AntiML-GRU-HiddenSize", 32..64, 8..256) { options.rotationsActive }
    private val antiMLGRUNumLayers by intRange("AntiML-GRU-NumLayers", 1..3, 1..6) { options.rotationsActive }
    private val antiMLGRUDropout by floatRange("AntiML-GRU-Dropout", 0f..0.1f, 0f..0.5f) { options.rotationsActive }
    private val antiMLGRUResetGate by choices("AntiML-GRU-ResetGate", arrayOf("Standard", "Coupled", "Minimal"), "Standard") { options.rotationsActive }
    private val antiMLRLAlgo by choices("AntiML-RL-Algorithm", arrayOf("PPO", "SAC", "TD3", "DQN", "A2C", "DDPG", "D4PG", "MPO", "IMPALA", "RND", "ICM", "DIAYN"), "PPO") { options.rotationsActive }
    private val antiMLRLGamma by floatRange("AntiML-RL-Gamma", 0.95f..0.99f, 0f..1f) { options.rotationsActive }
    private val antiMLRLLearningRate by floatRange("AntiML-RL-LR", 0.0001f..0.001f, 1e-6f..0.01f) { options.rotationsActive }
    private val antiMLRLBatchSize by intRange("AntiML-RL-BatchSize", 32..64, 1..512) { options.rotationsActive }
    private val antiMLRLBufferSize by intRange("AntiML-RL-BufferSize", 1000..5000, 100..100000) { options.rotationsActive }
    private val antiMLRLEntropyWeight by floatRange("AntiML-RL-EntropyWeight", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val antiMLRLValueLossWeight by floatRange("AntiML-RL-ValueLossWeight", 0.5f..1f, 0f..5f) { options.rotationsActive }
    private val antiMLRLClipRange by floatRange("AntiML-RL-ClipRange", 0.1f..0.3f, 0.01f..1f) { options.rotationsActive }
    private val antiMLRLGAELambda by floatRange("AntiML-RL-GAE-Lambda", 0.9f..0.98f, 0f..1f) { options.rotationsActive }
    private val antiMLRLNStepReturn by intRange("AntiML-RL-NStepReturn", 1..5, 1..20) { options.rotationsActive }
    private val antiMLRLRewardScaling by floatRange("AntiML-RL-RewardScaling", 0.5f..2f, 0.01f..10f) { options.rotationsActive }
    private val antiMLRLAdvantageNorm by boolean("AntiML-RL-AdvantageNorm", true) { options.rotationsActive }
    private val antiMLRLRewardShapingDistWeight by floatRange("AntiML-RL-RewardShaping-DistWeight", 0.3f..0.7f, 0f..1f) { options.rotationsActive }
    private val antiMLRLRewardShapingSmoothWeight by floatRange("AntiML-RL-RewardShaping-SmoothWeight", 0.1f..0.3f, 0f..1f) { options.rotationsActive }
    private val antiMLRLRewardShapingHitBonus by floatRange("AntiML-RL-RewardShaping-HitBonus", 1f..3f, 0f..10f) { options.rotationsActive }
    private val antiMLRLCuriosityIntrinsicWeight by floatRange("AntiML-RL-Curiosity-IntrinsicWeight", 0.01f..0.1f, 0f..1f) { options.rotationsActive }
    private val antiMLRLCuriosityForwardDim by intRange("AntiML-RL-Curiosity-ForwardDim", 32..64, 8..256) { options.rotationsActive }
    private val antiMLRLCuriosityInverseDim by intRange("AntiML-RL-Curiosity-InverseDim", 32..64, 8..256) { options.rotationsActive }
    private val antiMLFederatedNumClients by intRange("AntiML-Federated-NumClients", 3..5, 1..20) { options.rotationsActive }
    private val antiMLFederatedLocalEpochs by intRange("AntiML-Federated-LocalEpochs", 1..3, 1..10) { options.rotationsActive }
    private val antiMLFederatedAggregation by choices("AntiML-Federated-Aggregation", arrayOf("FedAvg", "FedProx", "FedNova", "SCAFFOLD", "FedDyn"), "FedAvg") { options.rotationsActive }
    private val antiMLFederatedDPNoise by floatRange("AntiML-Federated-DPNoise", 0.01f..0.1f, 0f..5f) { options.rotationsActive }
    private val antiMLFederatedClippingNorm by floatRange("AntiML-Federated-ClippingNorm", 1f..5f, 0.1f..100f) { options.rotationsActive }
    private val antiMLDPEpsilon by floatRange("AntiML-DP-Epsilon", 1f..5f, 0.01f..100f) { options.rotationsActive }
    private val antiMLDPDelta by floatRange("AntiML-DP-Delta", 1e-5f..1e-4f, 1e-8f..0.01f) { options.rotationsActive }
    private val antiMLDPSensitivity by floatRange("AntiML-DP-Sensitivity", 1f..3f, 0.01f..10f) { options.rotationsActive }
    private val antiMLDPMechanism by choices("AntiML-DP-Mechanism", arrayOf("Gaussian", "Laplace", "Exponential", "SparseVector", "ReportNoisyMax"), "Gaussian") { options.rotationsActive }
    private val antiMLDPCompositionMethod by choices("AntiML-DP-Composition", arrayOf("Basic", "Advanced", "RDP", "zCDP", "PLD"), "Advanced") { options.rotationsActive }
    private val antiMLDPAccountingSteps by intRange("AntiML-DP-AccountingSteps", 100..500, 10..10000) { options.rotationsActive }
    private val antiMLNeuralArchSearchSpace by choices("AntiML-NAS-SearchSpace", arrayOf("DARTS", "ENAS", "ProxylessNAS", "OnceForAll", "BNAS"), "DARTS") { options.rotationsActive }
    private val antiMLNASEpochs by intRange("AntiML-NAS-Epochs", 10..30, 1..100) { options.rotationsActive }
    private val antiMLNASPopulationSize by intRange("AntiML-NAS-PopulationSize", 20..50, 5..200) { options.rotationsActive }
    private val antiMLNASCandidates by intRange("AntiML-NAS-Candidates", 5..10, 1..50) { options.rotationsActive }
    private val antiMLKnowledgeDistillTemp by floatRange("AntiML-KD-Temperature", 2f..5f, 0.1f..20f) { options.rotationsActive }
    private val antiMLKnowledgeDistillAlpha by floatRange("AntiML-KD-Alpha", 0.5f..0.8f, 0f..1f) { options.rotationsActive }
    private val antiMLKnowledgeDistillTeacherType by choices("AntiML-KD-TeacherType", arrayOf("CNN", "RNN", "Transformer", "Ensemble"), "Ensemble") { options.rotationsActive }
    private val antiMLPruningRatio by floatRange("AntiML-Pruning-Ratio", 0.2f..0.5f, 0f..0.9f) { options.rotationsActive }
    private val antiMLPruningMethod by choices("AntiML-Pruning-Method", arrayOf("Magnitude", "Gradient", "Movement", "LAMP", "Fisher"), "Magnitude") { options.rotationsActive }
    private val antiMLPruningSchedule by choices("AntiML-Pruning-Schedule", arrayOf("OneShot", "Gradual", "Iterative", "AGP"), "Gradual") { options.rotationsActive }
    private val antiMLQuantizationBits by intRange("AntiML-Quantization-Bits", 4..8, 1..16) { options.rotationsActive }
    private val antiMLQuantizationScheme by choices("AntiML-Quantization-Scheme", arrayOf("Symmetric", "Asymmetric", "QAT", "GPTQ", "AWQ", "SmoothQuant"), "Symmetric") { options.rotationsActive }
    private val antiMLQuantizationGranularity by choices("AntiML-Quantization-Granularity", arrayOf("PerTensor", "PerChannel", "PerGroup"), "PerChannel") { options.rotationsActive }
    private val antiMLEarlyStoppingPatience by intRange("AntiML-EarlyStopping-Patience", 5..10, 1..50) { options.rotationsActive }
    private val antiMLEarlyStoppingMinDelta by floatRange("AntiML-EarlyStopping-MinDelta", 0.001f..0.01f, 1e-6f..0.1f) { options.rotationsActive }
    private val antiMLCurriculumStrategy by choices("AntiML-Curriculum-Strategy", arrayOf("None", "Difficulty", "Diversity", "Competence", "SelfPaced", "TeacherStudent"), "Difficulty") { options.rotationsActive }
    private val antiMLCurriculumPacing by floatRange("AntiML-Curriculum-Pacing", 0.1f..0.3f, 0f..1f) { options.rotationsActive }
    private val antiMLCurriculumWarmup by intRange("AntiML-Curriculum-Warmup", 5..20, 0..100) { options.rotationsActive }
    private val antiMLOnlineLearningBufferSize by intRange("AntiML-OnlineLearning-BufferSize", 100..500, 10..10000) { options.rotationsActive }
    private val antiMLOnlineLearningForgettingFactor by floatRange("AntiML-OnlineLearning-ForgettingFactor", 0.95f..0.99f, 0.5f..1f) { options.rotationsActive }
    private val antiMLOnlineLearningDetectionWindow by intRange("AntiML-OnlineLearning-DetectionWindow", 10..30, 1..100) { options.rotationsActive }
    private val antiMLOnlineLearningDriftThreshold by floatRange("AntiML-OnlineLearning-DriftThreshold", 0.05f..0.15f, 0f..1f) { options.rotationsActive }
    private val antiMLConceptDriftMethod by choices("AntiML-ConceptDrift-Method", arrayOf("DDM", "EDDM", "ADWIN", "CUSUM", "PHTest", "PageHinkley"), "ADWIN") { options.rotationsActive }
    private val antiMLConceptDriftSensitivity by floatRange("AntiML-ConceptDrift-Sensitivity", 0.5f..1f, 0.01f..5f) { options.rotationsActive }
    private val antiMLAnomalyDetectionMethod by choices("AntiML-AnomalyDetection-Method", arrayOf("IsolationForest", "LOF", "OneClassSVM", "AutoEncoder", "VAE", "GMM", "HBOS"), "IsolationForest") { options.rotationsActive }
    private val antiMLAnomalyDetectionContamination by floatRange("AntiML-AnomalyDetection-Contamination", 0.01f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val antiMLAnomalyDetectionThreshold by floatRange("AntiML-AnomalyDetection-Threshold", 0.9f..0.99f, 0.5f..1f) { options.rotationsActive }
    private val antiMLAnomalyNeighbors by intRange("AntiML-AnomalyDetection-Neighbors", 5..20, 1..100) { options.rotationsActive }
    private val antiMLStatisticalTestType by choices("AntiML-StatisticalTest-Type", arrayOf("KS", "AD", "CVM", "Chi2", "SW", "Lilliefors", "JarqueBera"), "KS") { options.rotationsActive }
    private val antiMLStatisticalTestAlpha by floatRange("AntiML-StatisticalTest-Alpha", 0.01f..0.05f, 0.001f..0.1f) { options.rotationsActive }
    private val antiMLStatisticalTestMinSamples by intRange("AntiML-StatisticalTest-MinSamples", 30..100, 5..1000) { options.rotationsActive }
    private val antiMLCrossValidationFolds by intRange("AntiML-CrossValidation-Folds", 3..5, 2..20) { options.rotationsActive }
    private val antiMLCrossValidationMethod by choices("AntiML-CrossValidation-Method", arrayOf("KFold", "Stratified", "TimeSeries", "Group", "MonteCarlo"), "Stratified") { options.rotationsActive }
    private val antiMLHyperparamOptMethod by choices("AntiML-HyperOpt-Method", arrayOf("GridSearch", "RandomSearch", "Bayesian", "BOHB", "Hyperband", "SuccessiveHalving", "Optuna", "RayTune"), "Bayesian") { options.rotationsActive }
    private val antiMLHyperOptBudget by intRange("AntiML-HyperOpt-Budget", 50..200, 10..10000) { options.rotationsActive }
    private val antiMLHyperOptMetric by choices("AntiML-HyperOpt-Metric", arrayOf("Accuracy", "F1", "AUC", "Loss", "Custom"), "AUC") { options.rotationsActive }
    private val antiMLInterpretabilityMethod by choices("AntiML-Interpretability-Method", arrayOf("SHAP", "LIME", "IntegratedGradients", "GradCAM", "Attention", "Permutation", "PartialDependence"), "SHAP") { options.rotationsActive }
    private val antiMLInterpretabilitySamples by intRange("AntiML-Interpretability-Samples", 50..200, 10..1000) { options.rotationsActive }
    private val antiMLInterpretabilityBaseline by choices("AntiML-Interpretability-Baseline", arrayOf("Zero", "Mean", "Random", "Uniform"), "Mean") { options.rotationsActive }
    private val antiMLFairnessMetric by choices("AntiML-Fairness-Metric", arrayOf("DemographicParity", "EqualizedOdds", "EqualOpportunity", "Calibration", "Individual"), "EqualizedOdds") { options.rotationsActive }
    private val antiMLFairnessThreshold by floatRange("AntiML-Fairness-Threshold", 0.05f..0.15f, 0f..1f) { options.rotationsActive }
    private val antiMLRobustnessCertMethod by choices("AntiML-Robustness-CertMethod", arrayOf("IBP", "CROWN", "BetaCROWN", "MILP", "LP", "SDP"), "CROWN") { options.rotationsActive }
    private val antiMLRobustnessLpNorm by choices("AntiML-Robustness-LpNorm", arrayOf("L1", "L2", "Linf"), "Linf") { options.rotationsActive }
    private val antiMLRobustnessRadius by floatRange("AntiML-Robustness-Radius", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val antiMLOODDetectionMethod by choices("AntiML-OOD-DetectionMethod", arrayOf("MSP", "ODIN", "Energy", "Mahalanobis", "ReAct", "DICE", "ASH", "ViM"), "Energy") { options.rotationsActive }
    private val antiMLOODDetectionThreshold by floatRange("AntiML-OOD-Threshold", 0.5f..0.8f, 0f..1f) { options.rotationsActive }
    private val antiMLOODTemperature by floatRange("AntiML-OOD-Temperature", 1f..10f, 0.1f..100f) { options.rotationsActive }
    private val antiMLOODNoiseScale by floatRange("AntiML-OOD-NoiseScale", 0f..0.01f, 0f..1f) { options.rotationsActive }
    private val antiMLCalibrationMethod by choices("AntiML-Calibration-Method", arrayOf("None", "PlattScaling", "Isotonic", "TemperatureScaling", "BetaCalibration", "Dirichlet"), "TemperatureScaling") { options.rotationsActive }
    private val antiMLCalibrationTemp by floatRange("AntiML-Calibration-Temperature", 1f..2f, 0.1f..10f) { options.rotationsActive }
    private val antiMLCalibrationNBin by intRange("AntiML-Calibration-NBins", 10..20, 2..100) { options.rotationsActive }
    private val antiMLActiveLearningStrategy by choices("AntiML-ActiveLearning-Strategy", arrayOf("Uncertainty", "Margin", "Entropy", "Diversity", "CoreSet", "BALD", "ExpectedGradNorm"), "Uncertainty") { options.rotationsActive }
    private val antiMLActiveLearningBudget by intRange("AntiML-ActiveLearning-Budget", 10..50, 1..500) { options.rotationsActive }
    private val antiMLActiveLearningBatch by intRange("AntiML-ActiveLearning-Batch", 5..10, 1..50) { options.rotationsActive }
    private val antiMLSelfSupervisedMethod by choices("AntiML-SelfSupervised-Method", arrayOf("SimCLR", "MoCo", "BYOL", "SimSiam", "BarlowTwins", "VICReg", "MAE", "BEiT"), "SimCLR") { options.rotationsActive }
    private val antiMLSelfSupervisedProjDim by intRange("AntiML-SelfSupervised-ProjDim", 32..64, 8..256) { options.rotationsActive }
    private val antiMLSelfSupervisedTemp by floatRange("AntiML-SelfSupervised-Temperature", 0.1f..0.5f, 0.01f..2f) { options.rotationsActive }
    private val antiMLSelfSupervisedAugStrength by floatRange("AntiML-SelfSupervised-AugStrength", 0.5f..1f, 0.1f..2f) { options.rotationsActive }
    private val antiMLMetaLearningMethod by choices("AntiML-MetaLearning-Method", arrayOf("MAML", "Reptile", "MetaSGD", "ProtoNet", "MatchingNet", "LEO"), "MAML") { options.rotationsActive }
    private val antiMLMetaLearningInnerLR by floatRange("AntiML-MetaLearning-InnerLR", 0.01f..0.1f, 0.001f..1f) { options.rotationsActive }
    private val antiMLMetaLearningOuterLR by floatRange("AntiML-MetaLearning-OuterLR", 0.001f..0.01f, 1e-5f..0.1f) { options.rotationsActive }
    private val antiMLMetaLearningInnerSteps by intRange("AntiML-MetaLearning-InnerSteps", 1..5, 1..20) { options.rotationsActive }
    private val antiMLContinualLearningMethod by choices("AntiML-ContinualLearning-Method", arrayOf("EWC", "SI", "MAS", "LwF", "PackNet", "Progressive", "HAT", "OGD"), "EWC") { options.rotationsActive }
    private val antiMLContinualLearningEWCWeight by floatRange("AntiML-ContinualLearning-EWCWeight", 100f..1000f, 1f..10000f) { options.rotationsActive }
    private val antiMLContinualLearningMemorySize by intRange("AntiML-ContinualLearning-MemorySize", 100..500, 10..5000) { options.rotationsActive }
    private val antiMLGraphNNType by choices("AntiML-GNN-Type", arrayOf("GCN", "GAT", "GraphSAGE", "GIN", "EdgeConv", "MPNN"), "GCN") { options.rotationsActive }
    private val antiMLGNNHiddenDim by intRange("AntiML-GNN-HiddenDim", 16..32, 4..128) { options.rotationsActive }
    private val antiMLGNNNumLayers by intRange("AntiML-GNN-NumLayers", 2..4, 1..10) { options.rotationsActive }
    private val antiMLGNNNumHeads by intRange("AntiML-GNN-NumHeads", 2..4, 1..8) { options.rotationsActive }
    private val antiMLGNNPooling by choices("AntiML-GNN-Pooling", arrayOf("Mean", "Max", "Sum", "Attention", "Set2Set", "DiffPool"), "Mean") { options.rotationsActive }
    private val antiMLMambaDModel by intRange("AntiML-Mamba-dModel", 16..32, 4..128) { options.rotationsActive }
    private val antiMLMambaDState by intRange("AntiML-Mamba-dState", 8..16, 2..64) { options.rotationsActive }
    private val antiMLMambaDConv by intRange("AntiML-Mamba-dConv", 3..4, 1..8) { options.rotationsActive }
    private val antiMLMambaExpand by intRange("AntiML-Mamba-expand", 2..2, 1..4) { options.rotationsActive }
    private val antiMLSSMType by choices("AntiML-SSM-Type", arrayOf("S4", "S5", "S6", "Mamba", "Mamba2", "Hyena", "RWKV"), "Mamba") { options.rotationsActive }
    private val antiMLSSMMeasure by choices("AntiML-SSM-Measure", arrayOf("HiPPO-LegS", "HiPPO-LegT", "HiPPO-LagT", "LTR", "GLT"), "HiPPO-LegS") { options.rotationsActive }
    private val antiMLSSMInitialization by choices("AntiML-SSM-Initialization", arrayOf("S4D-Real", "S4D-Complex", "Random"), "S4D-Real") { options.rotationsActive }
    private val antiMLDiffusionTimesteps by intRange("AntiML-Diffusion-Timesteps", 50..200, 10..1000) { options.rotationsActive }
    private val antiMLDiffusionNoiseSchedule by choices("AntiML-Diffusion-NoiseSchedule", arrayOf("Linear", "Cosine", "Sqrt", "Sigmoid", "VP", "VE"), "Cosine") { options.rotationsActive }
    private val antiMLDiffusionBetaStart by floatRange("AntiML-Diffusion-BetaStart", 0.0001f..0.001f, 1e-6f..0.01f) { options.rotationsActive }
    private val antiMLDiffusionBetaEnd by floatRange("AntiML-Diffusion-BetaEnd", 0.01f..0.1f, 0.001f..1f) { options.rotationsActive }
    private val antiMLFlowMatchingType by choices("AntiML-FlowMatching-Type", arrayOf("OT", "RF", "CFM", "OTCFM", "VPSDE", "VESDE"), "OTCFM") { options.rotationsActive }
    private val antiMLFlowMatchingSigma by floatRange("AntiML-FlowMatching-Sigma", 0f..0.1f, 0f..1f) { options.rotationsActive }
    private val antiMLFlowMatchingSteps by intRange("AntiML-FlowMatching-Steps", 10..50, 1..500) { options.rotationsActive }
    private val antiMLNormalizingFlowType by choices("AntiML-NF-Type", arrayOf("RealNVP", "Glow", "NSF", "MAF", "Planar", "Radial", "Coupling"), "RealNVP") { options.rotationsActive }
    private val antiMLNFNumCoupling by intRange("AntiML-NF-NumCoupling", 4..8, 1..20) { options.rotationsActive }
    private val antiMLNFHiddenDim by intRange("AntiML-NF-HiddenDim", 32..64, 8..256) { options.rotationsActive }
    private val antiMLEnergyBasedModelType by choices("AntiML-EBM-Type", arrayOf("JointEBM", "ConditionalEBM", "Implicit"), "JointEBM") { options.rotationsActive }
    private val antiMLEBMLangevinSteps by intRange("AntiML-EBM-LangevinSteps", 10..30, 1..100) { options.rotationsActive }
    private val antiMLEBMLangevinStepSize by floatRange("AntiML-EBM-LangevinStepSize", 0.01f..0.1f, 0.001f..1f) { options.rotationsActive }
    private val antiMLEBMTemperature by floatRange("AntiML-EBM-Temperature", 1f..2f, 0.1f..10f) { options.rotationsActive }
    private val antiMLRKWVType by choices("AntiML-RWKV-Type", arrayOf("RWKV4", "RWKV5", "RWKV6", "RWKV7"), "RWKV6") { options.rotationsActive }
    private val antiMLRWKVEmbDim by intRange("AntiML-RWKV-EmbDim", 32..64, 8..256) { options.rotationsActive }
    private val antiMLRWKVNumLayers by intRange("AntiML-RWKV-NumLayers", 2..4, 1..12) { options.rotationsActive }
    private val antiMLRWKVAttType by choices("AntiML-RWKV-AttType", arrayOf("TimeMix", "TokenShift", "RetNet"), "TimeMix") { options.rotationsActive }
    private val antiMLJambaDModel by intRange("AntiML-Jamba-dModel", 32..64, 8..256) { options.rotationsActive }
    private val antiMLJambaNumLayers by intRange("AntiML-Jamba-NumLayers", 2..4, 1..12) { options.rotationsActive }
    private val antiMLJambaAttnLayers by intRange("AntiML-Jamba-AttnLayers", 1..2, 0..6) { options.rotationsActive }
    private val antiMLJambaMambaLayers by intRange("AntiML-Jamba-MambaLayers", 1..2, 0..6) { options.rotationsActive }
    private val antiMLMixtralNumExperts by intRange("AntiML-Mixtral-NumExperts", 4..8, 2..16) { options.rotationsActive }
    private val antiMLMixtralTopK by intRange("AntiML-Mixtral-TopK", 2..2, 1..8) { options.rotationsActive }
    private val antiMLMixtralCapacityFactor by floatRange("AntiML-Mixtral-CapacityFactor", 1f..1.5f, 0.5f..3f) { options.rotationsActive }
    private val antiMLMixtralLoadBalanceWeight by floatRange("AntiML-Mixtral-LoadBalanceWeight", 0.01f..0.1f, 0f..1f) { options.rotationsActive }
    private val antiMLLoRARank by intRange("AntiML-LoRA-Rank", 4..8, 1..64) { options.rotationsActive }
    private val antiMLLoRAAlpha by floatRange("AntiML-LoRA-Alpha", 8f..16f, 1f..64f) { options.rotationsActive }
    private val antiMLLoRADropout by floatRange("AntiML-LoRA-Dropout", 0f..0.05f, 0f..0.5f) { options.rotationsActive }
    private val antiMLLoRATargetModules by choices("AntiML-LoRA-Target", arrayOf("All", "Attention", "MLP", "Attention+MLP"), "Attention") { options.rotationsActive }
    private val antiMLPromptTuningNumTokens by intRange("AntiML-PromptTuning-NumTokens", 5..20, 1..100) { options.rotationsActive }
    private val antiMLPromptTuningInit by choices("AntiML-PromptTuning-Init", arrayOf("Random", "FromVocab", "Uniform", "Gaussian"), "Random") { options.rotationsActive }
    private val antiMLPrefixTuningNumPrefixes by intRange("AntiML-PrefixTuning-NumPrefixes", 5..20, 1..100) { options.rotationsActive }
    private val antiMLPrefixTuningDim by intRange("AntiML-PrefixTuning-Dim", 32..64, 8..256) { options.rotationsActive }
    private val antiMLAdapterType by choices("AntiML-Adapter-Type", arrayOf("Bottleneck", "LoRA", "IA3", "DoRA", "MoLoRA"), "Bottleneck") { options.rotationsActive }
    private val antiMLAdapterBottleneckDim by intRange("AntiML-Adapter-BottleneckDim", 16..32, 4..128) { options.rotationsActive }
    private val antiMLAdapterDropout by floatRange("AntiML-Adapter-Dropout", 0f..0.1f, 0f..0.5f) { options.rotationsActive }
    private val antiMLScalingLawMethod by choices("AntiML-ScalingLaw-Method", arrayOf("Chinchilla", "Kaplan", "Neural", "Broken"), "Chinchilla") { options.rotationsActive }
    private val antiMLScalingLawAlphaN by floatRange("AntiML-ScalingLaw-AlphaN", 0.3f..0.5f, 0f..1f) { options.rotationsActive }
    private val antiMLScalingLawAlphaD by floatRange("AntiML-ScalingLaw-AlphaD", 0.3f..0.5f, 0f..1f) { options.rotationsActive }
    private val antiMLEmergentAbilityThreshold by floatRange("AntiML-Emergent-Threshold", 0.5f..0.8f, 0f..1f) { options.rotationsActive }
    private val antiMLEmergentAbilityMetric by choices("AntiML-Emergent-Metric", arrayOf("Accuracy", "BrierScore", "Calibration", "OOD"), "Accuracy") { options.rotationsActive }
    private val antiMLWatermarkMethod by choices("AntiML-Watermark-Method", arrayOf("None", "LogitsBias", "Inverse", "ExpMin", "SynthID"), "LogitsBias") { options.rotationsActive }
    private val antiMLWatermarkStrength by floatRange("AntiML-Watermark-Strength", 1f..5f, 0.1f..20f) { options.rotationsActive }
    private val antiMLWatermarkGamma by floatRange("AntiML-Watermark-Gamma", 0.25f..0.5f, 0.01f..0.99f) { options.rotationsActive }
    private val antiMLWatermarkHashing by choices("AntiML-Watermark-Hashing", arrayOf("MinHash", "SimHash", "BloomFilter"), "MinHash") { options.rotationsActive }
    private val antiMLUnlearningMethod by choices("AntiML-Unlearning-Method", arrayOf("FineTune", "GradientAscent", "Influence", "TaskArithmetic", "KnowledgeDistill"), "FineTune") { options.rotationsActive }
    private val antiMLUnlearningEpsilon by floatRange("AntiML-Unlearning-Epsilon", 0.01f..0.1f, 0f..1f) { options.rotationsActive }
    private val antiMLUnlearningSteps by intRange("AntiML-Unlearning-Steps", 10..50, 1..500) { options.rotationsActive }
    private val antiMLConstitutionalAIPrincipleCount by intRange("AntiML-CAI-PrincipleCount", 3..5, 1..10) { options.rotationsActive }
    private val antiMLConstitutionalAIFeedbackWeight by floatRange("AntiML-CAI-FeedbackWeight", 0.5f..0.8f, 0f..1f) { options.rotationsActive }
    private val antiMLRLHFKLWeight by floatRange("AntiML-RLHF-KLWeight", 0.1f..0.3f, 0f..1f) { options.rotationsActive }
    private val antiMLRLHFRewardModelType by choices("AntiML-RLHF-RewardModel", arrayOf("BradleyTerry", "Thurstone", "PlackettLuce"), "BradleyTerry") { options.rotationsActive }
    private val antiMLDPOBeta by floatRange("AntiML-DPO-Beta", 0.1f..0.5f, 0.01f..5f) { options.rotationsActive }
    private val antiMLDPOReferenceWeight by floatRange("AntiML-DPO-ReferenceWeight", 0.5f..1f, 0f..2f) { options.rotationsActive }
    private val antiMLIPOStrength by floatRange("AntiML-IPO-Strength", 0.1f..0.5f, 0.01f..5f) { options.rotationsActive }
    private val antiMLKTOAsymmetry by floatRange("AntiML-KTO-Asymmetry", 0.5f..1.5f, 0.01f..5f) { options.rotationsActive }
    private val antiMLORPOAlpha by floatRange("AntiML-ORPO-Alpha", 0.1f..0.5f, 0.01f..5f) { options.rotationsActive }
    private val antiMLSimPOGamma by floatRange("AntiML-SimPO-Gamma", 0.5f..1f, 0.01f..5f) { options.rotationsActive }
    private val antiMLRetrievalAugmentedTopK by intRange("AntiML-RAG-TopK", 3..5, 1..20) { options.rotationsActive }
    private val antiMLRAGChunkSize by intRange("AntiML-RAG-ChunkSize", 64..128, 16..512) { options.rotationsActive }
    private val antiMLRAGOverlap by intRange("AntiML-RAG-Overlap", 10..20, 0..128) { options.rotationsActive }
    private val antiMLRAGEmbeddingModel by choices("AntiML-RAG-Embedding", arrayOf("MiniLM", "MPNet", "E5", "BGE", "Cohere"), "MiniLM") { options.rotationsActive }
    private val antiMLRAGReranker by choices("AntiML-RAG-Reranker", arrayOf("None", "CrossEncoder", "ColBERT", "MonoT5"), "CrossEncoder") { options.rotationsActive }
    private val antiMLMoEPrecision by choices("AntiML-MoE-Precision", arrayOf("FP32", "FP16", "BF16", "INT8", "INT4", "FP8"), "BF16") { options.rotationsActive }
    private val antiMLMoEExpertParallelism by intRange("AntiML-MoE-ExpertParallelism", 1..2, 1..8) { options.rotationsActive }
    private val antiMLMoEActivationCheckpointing by boolean("AntiML-MoE-ActivationCheckpointing", true) { options.rotationsActive }
    private val antiMLFlashAttentionVersion by choices("AntiML-FlashAttn-Version", arrayOf("None", "FA1", "FA2", "FA3"), "FA2") { options.rotationsActive }
    private val antiMLRingAttention by boolean("AntiML-RingAttention", false) { options.rotationsActive }
    private val antiMLSequenceParallelism by intRange("AntiML-SequenceParallelism", 1..1, 1..8) { options.rotationsActive }
    private val antiMLKVCacheQuantBits by intRange("AntiML-KVCache-QuantBits", 8..8, 2..8) { options.rotationsActive }
    private val antiMLKVCacheQuantGroup by intRange("AntiML-KVCache-QuantGroup", 32..64, 1..256) { options.rotationsActive }
    private val antiMLPagedAttentionBlockSize by intRange("AntiML-PagedAttn-BlockSize", 16..16, 1..64) { options.rotationsActive }
    private val antiMLSpeculativeDecodingDraftModel by choices("AntiML-SpecDec-DraftModel", arrayOf("None", "SmallLM", "MedLM", "Ngram"), "None") { options.rotationsActive }
    private val antiMLSpeculativeDecodingTopK by intRange("AntiML-SpecDec-TopK", 3..5, 1..10) { options.rotationsActive }
    private val antiMLMedusaHeads by intRange("AntiML-Medusa-Heads", 0..2, 0..8) { options.rotationsActive }
    private val antiMLMedusaTopK by intRange("AntiML-Medusa-TopK", 1..3, 1..10) { options.rotationsActive }
    private val antiMLLookaheadDecodingWindowSize by intRange("AntiML-LookaheadDecoding-WindowSize", 1..5, 1..20) { options.rotationsActive }
    private val antiMLLookaheadDecodingNgram by intRange("AntiML-LookaheadDecoding-Ngram", 1..3, 1..5) { options.rotationsActive }
    private val antiMLDistillationMethod by choices("AntiML-Distillation-Method", arrayOf("Response", "Feature", "Relation", "Attention", "SelfSupervised", "MultiTeacher"), "Response") { options.rotationsActive }
    private val antiMLDistillationTemperature by floatRange("AntiML-Distillation-Temperature", 2f..5f, 0.1f..20f) { options.rotationsActive }
    private val antiMLDistillationAlpha by floatRange("AntiML-Distillation-Alpha", 0.5f..0.8f, 0f..1f) { options.rotationsActive }
    private val antiMLCircuitComplexityDepth by intRange("AntiML-Circuit-Depth", 3..6, 1..20) { options.rotationsActive }
    private val antiMLCircuitComplexityWidth by intRange("AntiML-Circuit-Width", 32..64, 4..256) { options.rotationsActive }
    private val antiMLCircuitComplexityGates by choices("AntiML-Circuit-Gates", arrayOf("ANDORNOT", "Threshold", "ReLU", "Polynomial"), "ReLU") { options.rotationsActive }
    private val antiMLPACBayesBoundType by choices("AntiML-PACBayes-BoundType", arrayOf("KL", "KLInv", "Catoni", "Tolstikhin"), "KL") { options.rotationsActive }
    private val antiMLPACBayesPriorStrength by floatRange("AntiML-PACBayes-PriorStrength", 1f..10f, 0.01f..100f) { options.rotationsActive }
    private val antiMLRademacherEstimateSamples by intRange("AntiML-Rademacher-Samples", 100..500, 10..5000) { options.rotationsActive }
    private val antiMLVCDimensionEstimate by intRange("AntiML-VCDimension-Estimate", 10..50, 1..1000) { options.rotationsActive }
    private val antiMLCoveringNumberRadius by floatRange("AntiML-CoveringNumber-Radius", 0.01f..0.1f, 0.001f..1f) { options.rotationsActive }
    private val antiMLGeneralizationBoundMethod by choices("AntiML-GenBound-Method", arrayOf("VC", "Rademacher", "PACBayes", "Stability", "InformationTheoretic"), "Rademacher") { options.rotationsActive }
    private val antiMLInformationBottleneckBeta by floatRange("AntiML-IB-Beta", 0.01f..0.1f, 0f..1f) { options.rotationsActive }
    private val antiMLInformationBottleneckLayers by intRange("AntiML-IB-Layers", 1..3, 1..10) { options.rotationsActive }
    private val antiMLMDLComplexityPenalty by floatRange("AntiML-MDL-Penalty", 0.01f..0.1f, 0f..1f) { options.rotationsActive }
    private val antiMLBayesianModelComparison by choices("AntiML-Bayesian-Comparison", arrayOf("BF", "AIC", "BIC", "DIC", "WAIC", "LOO-CV"), "BF") { options.rotationsActive }
    private val antiMLBayesianPriorType by choices("AntiML-Bayesian-Prior", arrayOf("Gaussian", "Laplace", "Horseshoe", "SpikeSlab", "NormalInverseGamma"), "Gaussian") { options.rotationsActive }
    private val antiMLBayesianPriorScale by floatRange("AntiML-Bayesian-PriorScale", 1f..5f, 0.01f..100f) { options.rotationsActive }
    private val antiMLVariationalInferenceMethod by choices("AntiML-VI-Method", arrayOf("MeanField", "FullCov", "NormalizingFlow", "IAF", "Householder", "Stein"), "MeanField") { options.rotationsActive }
    private val antiMLVariationalInferenceSamples by intRange("AntiML-VI-Samples", 1..10, 1..100) { options.rotationsActive }
    private val antiMLSVILearningRate by floatRange("AntiML-SVI-LR", 0.001f..0.01f, 0.0001f..0.1f) { options.rotationsActive }
    private val antiMLHamiltonianMonteCarloStepSize by floatRange("AntiML-HMC-StepSize", 0.01f..0.1f, 0.001f..1f) { options.rotationsActive }
    private val antiMLHMCLemmaSteps by intRange("AntiML-HMC-LemmaSteps", 10..50, 1..200) { options.rotationsActive }
    private val antiMLHMCAdaptiveStepSize by boolean("AntiML-HMC-AdaptiveStepSize", true) { options.rotationsActive }
    private val antiMLNoisyHamiltonianScale by floatRange("AntiML-SGHMC-NoiseScale", 0.01f..0.05f, 0f..1f) { options.rotationsActive }
    private val antiMLStochasticVolatilityModel by choices("AntiML-SV-Model", arrayOf("GBM", "Heston", "SABR", "Vasicek", "CIR", "OU"), "Heston") { options.rotationsActive }
    private val antiMLStochasticVolatilityMeanReversion by floatRange("AntiML-SV-MeanReversion", 0.5f..2f, 0.01f..10f) { options.rotationsActive }
    private val antiMLStochasticVolatilityOfVol by floatRange("AntiML-SV-VolOfVol", 0.1f..0.5f, 0.01f..5f) { options.rotationsActive }
    private val antiMLStochasticCorrelation by floatRange("AntiML-SV-Correlation", -0.5f..-0.3f, -1f..1f) { options.rotationsActive }
    private val antiMLCopulaType by choices("AntiML-Copula-Type", arrayOf("Gaussian", "Clayton", "Gumbel", "Frank", "Joe", "StudentT", "Independence"), "Gaussian") { options.rotationsActive }
    private val antiMLCopulaParameter by floatRange("AntiML-Copula-Parameter", 1f..5f, 0.01f..50f) { options.rotationsActive }
    private val antiMLCopulaDF by intRange("AntiML-Copula-DF", 3..7, 1..30) { options.rotationsActive }
    private val antiMLVineCopulaTree by choices("AntiML-VineCopula-Tree", arrayOf("R", "D1", "D2", "C"), "R") { options.rotationsActive }
    private val antiMLVineCopulaTruncLevel by intRange("AntiML-VineCopula-TruncLevel", 1..3, 1..10) { options.rotationsActive }

    private val highestBodyPointToTargetValue = choices(
        "HighestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Head"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val lowestPoint = RotationUtils.BodyPoint.fromString(lowestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, lowestPoint, RotationUtils.BodyPoint.HEAD)
        coercedPoint.displayName
    }
    private val highestBodyPointToTarget: String by highestBodyPointToTargetValue

    private val lowestBodyPointToTargetValue = choices(
        "LowestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Feet"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val highestPoint = RotationUtils.BodyPoint.fromString(highestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, RotationUtils.BodyPoint.FEET, highestPoint)
        coercedPoint.displayName
    }

    private val lowestBodyPointToTarget: String by lowestBodyPointToTargetValue

    private val horizontalBodySearchRange by floatRange(
        "HorizontalBodySearchRange", 0f..1f, 0f..1f
    ) { options.rotationsActive }

    private val fov by float("FOV", 180f, 0f..180f)

    // Prediction
    private val predictClientMovement by int("PredictClientMovement", 2, 0..5)
    private val predictOnlyWhenOutOfRange by boolean(
        "PredictOnlyWhenOutOfRange", false
    ) { predictClientMovement != 0 }
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, -1f..2f)

    private val forceFirstHit by boolean("ForceFirstHit", false) { !respectMissCooldown && !useHitDelay }

    // Extra swing
    private val failSwing by boolean("FailSwing", true) { swing && options.rotationsActive }
    private val respectMissCooldown by boolean(
        "RespectMissCooldown", false
    ) { swing && failSwing && options.rotationsActive }
    private val swingOnlyInAir by boolean("SwingOnlyInAir", true) { swing && failSwing && options.rotationsActive }
    private val maxRotationDifferenceToSwing by float(
        "MaxRotationDifferenceToSwing", 180f, 0f..180f
    ) { swing && failSwing && options.rotationsActive }
    private val swingWhenTicksLate = boolean("SwingWhenTicksLate", false) {
        swing && failSwing && maxRotationDifferenceToSwing != 180f && options.rotationsActive
    }
    private val ticksLateToSwing by int(
        "TicksLateToSwing", 4, 0..20
    ) { swing && failSwing && swingWhenTicksLate.isActive() && options.rotationsActive }
    private val renderBoxOnSwingFail by boolean("RenderBoxOnSwingFail", false) { failSwing }
    private val renderBoxColor = ColorSettingsInteger(this, "RenderBoxColor") { renderBoxOnSwingFail }.with(Color.CYAN)
    private val renderBoxFadeSeconds by float("RenderBoxFadeSeconds", 1f, 0f..5f) { renderBoxOnSwingFail }

    // Inventory
    private val simulateClosingInventory by boolean("SimulateClosingInventory", false) { !noInventoryAttack }
    private val noInventoryAttack by boolean("NoInvAttack", false)
    private val noInventoryDelay by int("NoInvDelay", 200, 0..500) { noInventoryAttack }
    private val noConsumeAttack by choices(
        "NoConsumeAttack", arrayOf("Off", "NoHits", "NoRotation"), "Off"
    ).subjective()

    // Visuals - 视觉效果
    private val markNone by boolean("Mark-None", false)
    private val markPlatform by boolean("Mark-Platform", false)
    private val markBox by boolean("Mark-Box", false)
    private val markCircle by boolean("Mark-Circle", true)
    
    // More ESP 选项 - boolean 类型复选框
    private val markJello by boolean("Mark-Jello", false)
    private val markZavz by boolean("Mark-Zavz", false)
    private val markZywl by boolean("Mark-Zywl", false)
    private val markSigma by boolean("Mark-Sigma", false)
    private val markFDP by boolean("Mark-FDP", false)
    private val markTracers by boolean("Mark-Tracers", false)
    private val markLies by boolean("Mark-Lies", false)
    private val markSims by boolean("Mark-Sims", false)
    
    // 攻击范围圈
    private val rangeCircle by boolean("RangeCircle", false)
    private val rangeCircleRed by int("RangeCircle-Red", 255, 0..255) { rangeCircle }
    private val rangeCircleGreen by int("RangeCircle-Green", 255, 0..255) { rangeCircle }
    private val rangeCircleBlue by int("RangeCircle-Blue", 255, 0..255) { rangeCircle }
    private val rangeCircleAlpha by int("RangeCircle-Alpha", 255, 0..255) { rangeCircle }
    private val rangeCircleThickness by float("RangeCircle-Thickness", 2f, 1f..5f) { rangeCircle }
    
    private val fakeSharp by boolean("FakeSharp", true).subjective()
    private val renderPointBoxAim by boolean("RenderAimPointBox", false).subjective()
    private val aimPointBoxColor by color("AimPointBoxColor", Color.CYAN) { renderPointBoxAim }.subjective()
    private val aimPointBoxSize by float("AimPointBoxSize", 0.1f, 0f..0.2F) { renderPointBoxAim }.subjective()

    // Circle options - Circle 选项
    private val circleStartColor by color("CircleStartColor", Color.BLUE) { markCircle }.subjective()
    private val circleEndColor by color("CircleEndColor", Color.CYAN.withAlpha(0)) { markCircle }.subjective()
    private val fillInnerCircle by boolean("FillInnerCircle", false) { markCircle }.subjective()
    private val withHeight by boolean("WithHeight", true) { markCircle }.subjective()
    private val animateHeight by boolean("AnimateHeight", false) { withHeight }.subjective()
    private val heightRange by floatRange("HeightRange", 0.0f..0.4f, -2f..2f) { withHeight }.subjective()
    // 自定义Circle大小选项
    private val customCircleSize by boolean("CustomCircleSize", false) { markCircle }.subjective()
    private val circleSize by float("CircleSize", 0.5f, 0.1f..3.0f) { markCircle && customCircleSize }.subjective()
    private val extraWidth by float("ExtraWidth", 0F, 0F..2F) { markCircle && !customCircleSize }.subjective()
    private val animateCircleY by boolean("AnimateCircleY", true) { fillInnerCircle || withHeight }.subjective()
    private val circleYRange by floatRange("CircleYRange", 0F..0.5F, 0F..2F) { animateCircleY }.subjective()
    private val duration by float(
        "Duration", 1.5F, 0.5F..3F, suffix = "Seconds"
    ) { animateCircleY || animateHeight }.subjective()

    // Box option - Box 选项
    private val boxOutline by boolean("Outline", true) { markBox }.subjective()

    // Jello 选项
    private val jelloAlpha by float("JelloAlpha", 0.4f, 0f..1f) { markJello }.subjective()
    private val jelloWidth by float("JelloWidth", 3f, 0.01f..5f) { markJello }.subjective()
    private val jelloGradientHeight by float("JelloGradientHeight", 3f, 1f..8f) { markJello }.subjective()
    private val jelloFadeSpeed by float("JelloFadeSpeed", 0.1f, 0.01f..0.5f) { markJello }.subjective()

    // Zavz/Zywl 选项
    private val zavzSpeed by float("ZavzSpeed", 0.1f, 0f..10f) { markZavz || markZywl }.subjective()
    private val zavzDual by boolean("ZavzDual", true) { markZavz || markZywl }.subjective()

    // Tracers 选项
    private val tracersThickness by float("TracersThickness", 1f, 0.1f..5f) { markTracers }.subjective()

    // ESP 颜色选项
    private val espColorMode by choices("ESPColorMode", arrayOf("Custom", "Rainbow", "Health"), "Custom") 
        { markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims }.subjective()
    private val espColorRed by int("ESPRed", 255, 0..255) 
        { espColorMode == "Custom" && (markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims) }.subjective()
    private val espColorGreen by int("ESPGreen", 255, 0..255) 
        { espColorMode == "Custom" && (markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims) }.subjective()
    private val espColorBlue by int("ESPBlue", 255, 0..255) 
        { espColorMode == "Custom" && (markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims) }.subjective()
    private val espColorAlpha by int("ESPAlpha", 255, 0..255) 
        { markJello || markZavz || markZywl || markSigma || markFDP || markTracers || markLies || markSims }.subjective()

    /**
     * MODULE
     */

    // Target
    var target: EntityLivingBase? = null
    private var lastTarget: EntityLivingBase? = null
    private var hittable = false
    private val prevTargetEntities = mutableListOf<Int>()

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0
    private var clicks = 0
    private var attackTickTimes = mutableListOf<Pair<MovingObjectPosition, Int>>()

    // Container Delay
    private var containerOpen = -1L

    // Block status
    var renderBlocking = false
    var blockStatus = false
    private var blockStopInDead = false
    private var blockOnNoHitDelayTick = 0
    
    // HurtTime AutoBlock state
    private var hurtTimeBlocking = false
    private var hurtTimeBlockStartTick = 0L
    private var hurtTimeWaitStartTick = 0L
    private var hurtTimeWaitTicksCalc = 0
    private var hurtTimeWaitingToBlock = false
    private var hurtTimeLastHurtTick = 0L
    private var hurtTimeCurrentTick = 0L
    private var hurtTimePrevHurtTime = 0
    private val hurtTimeDamageIntervals = mutableListOf<Long>()

    // Switch Delay
    private val switchTimer = MSTimer()

    // Blink AutoBlock
    private var blinked = false

    // Swing fails
    private val swingFails = mutableListOf<SwingFailData>()

    // KillESP 状态变量（整合自 KillESP 模块）
    private var espStart = 0.0
    private var espDirection = 1.0
    private var espYPos = 0.0
    private var espProgress = 0.0
    private var espAl = 0f
    private var espLastMS = System.currentTimeMillis()
    private var espLastDeltaMS = 0L

    /**
     * Disable kill aura module
     */
    override fun onToggle(state: Boolean) {
        target = null
        hittable = false
        prevTargetEntities.clear()
        attackTickTimes.clear()
        attackTimer.reset()
        clicks = 0

        if (blinkAutoBlock) {
            BlinkUtils.unblink()
            blinked = false
        }

        if (autoF5) mc.gameSettings.thirdPersonView = 0
        
        if (autoBlock == "BlockOnNoHit" && blockOnNoHitMode == "RightClick") {
            mc.gameSettings.keyBindUseItem.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
        }

        stopBlocking(true)

        synchronized(swingFails) {
            swingFails.clear()
        }

        if (!state) {
            RotationUtils.resetRotation()
        }
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        update()
    }

    fun update() {
        if (cancelRun || (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay))) return

        // Check if last target died
        lastTarget?.let { 
            if (it.isDead || it.health <= 0) {
                EventManager.call(EntityKilledEvent(it))
                lastTarget = null
            }
        }

        // Update target
        updateTarget()

        // Record current target for kill detection
        target?.let { lastTarget = it }

        if (autoF5) {
            if (mc.gameSettings.thirdPersonView != 1 && target != null) {
                mc.gameSettings.thirdPersonView = 1
            }
        }
    }

    val onWorld = handler<WorldEvent> {
        attackTickTimes.clear()

        if (blinkAutoBlock && BlinkUtils.isBlinking) BlinkUtils.unblink()

        synchronized(swingFails) {
            swingFails.clear()
        }
    }

    /**
     * Tick event
     */
    val onTick = handler<GameTickEvent>(priority = 2) {
        val player = mc.thePlayer ?: return@handler

        NoisePresets.tick()
        options.noiseFunction = generateNoise()

        if (blockStatus && player.heldItem?.item !is ItemSword) {
            blockStatus = false
            renderBlocking = false
            return@handler
        }

        if (shouldPrioritize()) {
            target = null
            renderBlocking = false
            return@handler
        }

        if (clickOnly && !mc.gameSettings.keyBindAttack.isKeyDown) {
            clicks = 0
            return@handler
        }

        if (blockStatus && (autoBlock == "Packet" || autoBlock == "QuickMacro") && releaseAutoBlock && !ignoreTickRule) {
            clicks = 0
            stopBlocking()
            return@handler
        }

        if (cancelRun) {
            target = null
            hittable = false
            stopBlocking()
            return@handler
        }

        if (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return@handler
        }

        if (simulateCooldown && getAttackCooldownProgress() < 1f) {
            return@handler
        }

        if (target == null && !blockStopInDead) {
            blockStopInDead = true
            if (autoBlock == "BlockOnNoHit") {
                val player = mc.thePlayer
                if (player != null && player.heldItem?.item is ItemSword && !blockStatus) {
                    if (blockOnNoHitDelayTick >= blockOnNoHitDelay) {
                        when (blockOnNoHitMode) {
                            "Packet" -> {
                                sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
                                blockStatus = true
                                renderBlocking = true
                            }
                            "RightClick" -> {
                                mc.rightClickDelayTimer = 0
                                mc.gameSettings.keyBindUseItem.pressed = true
                                blockStatus = true
                                renderBlocking = true
                            }
                        }
                    } else {
                        blockOnNoHitDelayTick++
                    }
                }
            } else {
                stopBlocking()
            }
            return@handler
        }

        if (blinkAutoBlock) {
            when (player.ticksExisted % (blinkBlockTicks + 1)) {
                0 -> {
                    if (blockStatus && !blinked && !BlinkUtils.isBlinking) {
                        blinked = true
                    }
                }

                1 -> {
                    if (blockStatus && blinked && BlinkUtils.isBlinking) {
                        stopBlocking()
                    }
                }

                blinkBlockTicks -> {
                    if (!blockStatus && blinked && BlinkUtils.isBlinking) {
                        BlinkUtils.unblink()
                        blinked = false

                        startBlocking(target!!, interactAutoBlock, autoBlock == "Fake") // block again
                    }
                }
            }
        }

        if (target != null) {
            if (player.getDistanceToEntityBox(target!!) > blockMaxRange && blockStatus) {
                stopBlocking(true)
                return@handler
            } else {
                if (autoBlock != "Off" && !releaseAutoBlock) {
                    renderBlocking = true
                }
                if (autoBlock == "BlockOnNoHit" && !blockStatus && player.heldItem?.item is ItemSword) {
                    if (blockOnNoHitDelayTick >= blockOnNoHitDelay) {
                        when (blockOnNoHitMode) {
                            "Packet" -> {
                                sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
                                blockStatus = true
                                renderBlocking = true
                            }
                            "RightClick" -> {
                                mc.rightClickDelayTimer = 0
                                mc.gameSettings.keyBindUseItem.pressed = true
                                blockStatus = true
                                renderBlocking = true
                            }
                        }
                    } else {
                        blockOnNoHitDelayTick++
                    }
                }
            }

            // Usually when you butterfly click, you end up clicking two (and possibly more) times in a single tick.
            // Sometimes you also do not click. The positives outweigh the negatives, however.
            val extraClicks = if (simulateDoubleClicking && !simulateCooldown) nextInt(-1, 1) else 0

            // Generate clicks based on distance from us to target.
            val generatedClicks = if (generateClicksBasedOnDist) {
                val distance = player.getDistanceToEntityBox(target!!)
                ((distance / distanceFactor.random()) * cpsMultiplier.random()).roundToInt()
            } else 0

            var maxClicks = clicks + extraClicks + generatedClicks

            val prevHittable = hittable

            updateHittable()

            if (!prevHittable && hittable && maxClicks == 0 && forceFirstHit) {
                maxClicks++
            }

            repeat(maxClicks) {
                val wasBlocking = blockStatus

                runAttack(it == 0, it + 1 == maxClicks)
                clicks--

                if (wasBlocking && !blockStatus && (releaseAutoBlock && !ignoreTickRule || autoBlock == "Off")) {
                    return@handler
                }
            }
        } else {
            renderBlocking = false
        }
    }

    /**
     * Render event
     */
    val onRender3D = handler<Render3DEvent> { event ->
        handleFailedSwings()

        drawAimPointBox()

        if (cancelRun) {
            target = null
            hittable = false
            return@handler
        }

        if (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return@handler
        }

        // 绘制攻击范围圈 - 只要启用 KillAura 就显示
        if (rangeCircle) {
            renderRangeCircle()
        }

        target ?: return@handler

        if (attackTimer.hasTimePassed(attackDelay)) {
            if (cps.last > 0) clicks++
            attackTimer.reset()

            attackDelay = randomClickDelay(cps.first, cps.last)
        }

        val hittableColor = if (hittable) Color(37, 126, 255, 70) else Color(255, 0, 0, 70)

        if (targetMode != "Multi") {
            target ?: return@handler
            
            // 绘制各种 Mark
            if (markPlatform) drawPlatform(target!!, hittableColor)
            if (markBox) drawEntityBox(target!!, hittableColor, boxOutline)
            if (markCircle) drawCircle(
                target!!,
                duration * 1000F,
                heightRange.takeIf { animateHeight } ?: heightRange.endInclusive..heightRange.endInclusive,
                if (customCircleSize) circleSize else extraWidth,
                fillInnerCircle,
                withHeight,
                circleYRange.takeIf { animateCircleY },
                circleStartColor.rgb,
                circleEndColor.rgb
            )
            
            // More ESP 渲染
            if (markJello) renderJelloESP(event)
            if (markZavz) renderZavzESP(event)
            if (markZywl) renderZywlESP(event)
            if (markSigma) renderSigmaESP(event)
            if (markFDP) renderFdPESP(event)
            if (markTracers) renderTracersESP(event)
            if (markLies) renderLiesESP(event)
            if (markSims) renderSimsESP(event)
        }
    }

    /**
     * Attack enemy
     */
    private fun runAttack(isFirstClick: Boolean, isLastClick: Boolean) {
        val currentTarget = this.target ?: return

        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (noConsumeAttack == "NoHits" && isConsumingItem()) {
            return
        }
        
        if (autoBlock == "BlockOnNoHit" && cancelAttackWhenBlocking && blockStatus) {
            return
        }

        // Settings
        val multi = targetMode == "Multi"
        val manipulateInventory = simulateClosingInventory && !noInventoryAttack && serverOpenInventory

        if (hittable && currentTarget.hurtTime > hurtTime) {
            return
        }

        // Check if enemy is not hittable
        if (!hittable && options.rotationsActive) {
            if (swing && failSwing) {
                val rotation = currentRotation ?: player.rotation

                // Can humans keep click consistency when performing massive rotation changes?
                // (10-30 rotation difference/doing large mouse movements for example)
                // Maybe apply to attacks too?
                if (rotationDifference(rotation) > maxRotationDifferenceToSwing) {
                    // At the same time there is also a chance of the user clicking at least once in a while
                    // when the consistency has dropped a lot.
                    val shouldIgnore = swingWhenTicksLate.isActive() && ticksSinceClick() >= ticksLateToSwing

                    if (!shouldIgnore) {
                        return
                    }
                }

                runWithModifiedRaycastResult(rotation, range.toDouble(), throughWallsRange.toDouble()) {
                    if (swingOnlyInAir && !it.typeOfHit.isMiss) {
                        return@runWithModifiedRaycastResult
                    }

                    // Left click miss cool-down logic:
                    // When you click and miss, you receive a 10 tick cool down.
                    // It decreases gradually (tick by tick) when you hold the button.
                    // If you click and then release the button, the cool down drops from where it was immediately to 0.
                    // Most humans will release the button 1-2 ticks max after clicking, leaving them with an average of 10 CPS.
                    // The maximum CPS allowed when you miss a hit is 20 CPS, if you click and release immediately, which is highly unlikely.
                    // With that being said, we force an average of 10 CPS by doing this below, since 10 CPS when missing is possible.
                    if (respectMissCooldown && ticksSinceClick() <= 1 && it.typeOfHit.isMiss) {
                        return@runWithModifiedRaycastResult
                    }

                    val shouldEnterBlockBreakProgress =
                        !shouldDelayClick(it.typeOfHit) || attackTickTimes.lastOrNull()?.first?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK

                    if (shouldEnterBlockBreakProgress) {
                        // Close inventory when open
                        if (manipulateInventory && isFirstClick) serverOpenInventory = false
                    }

                    val prevCooldown = mc.leftClickCounter

                    // Is any GUI coming from our client?
                    val isAnyClientGuiActive = mc.currentScreen?.javaClass?.`package`?.name?.contains(
                        LiquidBounce.CLIENT_NAME, ignoreCase = true
                    ) == true

                    if (isAnyClientGuiActive) {
                        mc.leftClickCounter = 0
                    }

                    if (!shouldDelayClick(it.typeOfHit)) {
                        attackTickTimes += it to runTimeTicks

                        if (it.typeOfHit.isEntity) {
                            val entity = it.entityHit

                            // Use own function instead of clickMouse() to maintain keep sprint, auto block, etc
                            if (entity is EntityLivingBase && isSelected(entity, true)) {
                                attackEntity(entity, isLastClick)
                            } else attackTickTimes -= it to runTimeTicks
                        } else {
                            // Imitate game click
                            mc.clickMouse()

                            if (renderBoxOnSwingFail) {
                                synchronized(swingFails) {
                                    val centerDistance = (currentTarget.hitBox.center - player.eyes).lengthVector()
                                    val spot = player.eyes + getVectorForRotation(rotation) * centerDistance

                                    swingFails += SwingFailData(spot, System.currentTimeMillis())
                                }
                            }
                        }
                    }

                    if (shouldEnterBlockBreakProgress && isLastClick) {
                        /**
                         * This is used to update the block breaking progress, resulting in sending an animation packet.
                         *
                         * Setting this function's parameter to [false] would still obey vanilla clicking logic,
                         * but only if you were releasing the click button immediately after pressing. Does not seem legit
                         * in the long term, right? This is why we are going to set it to [true], so it can send the animation packet.
                         */
                        mc.sendClickBlockToController(true)
                        /**
                         * Since we want to simulate proper clicking behavior, we schedule the block break progress stop
                         * in the next tick, since that is a doable action by the average player.
                         */
                        nextTick {
                            mc.sendClickBlockToController(false)

                            // Swings are sent a tick after stopping the block break progress.
                            clicks = 0

                            // [manipulateInventory] could have been changed at that point, but it is okay because
                            // serverOpenInventory's backing fields check for same values.
                            if (manipulateInventory) serverOpenInventory = true
                        }
                    }

                    if (isAnyClientGuiActive) {
                        mc.leftClickCounter = prevCooldown
                    }
                }
            }

            return
        }

        // Close inventory when open
        if (manipulateInventory && isFirstClick) serverOpenInventory = false

        blockStopInDead = false

        if (!multi) {
            attackEntity(currentTarget, isLastClick)
        } else {
            var targets = 0

            for (entity in world.loadedEntityList) {
                val distance = player.getDistanceToEntityBox(entity)

                if (entity is EntityLivingBase && isSelected(entity, true) && distance <= getRange(entity)) {
                    attackEntity(entity, isLastClick)

                    targets += 1

                    if (limitedMultiTargets != 0 && limitedMultiTargets <= targets) break
                }
            }
        }

        if (!isLastClick) return

        val switchMode = targetMode == "Switch"

        if (!switchMode || switchTimer.hasTimePassed(switchDelay)) {
            prevTargetEntities += currentTarget.entityId

            if (switchMode) {
                switchTimer.reset()
            }
        }

        // Open inventory
        if (manipulateInventory) serverOpenInventory = true
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        if (shouldPrioritize()) return

        // Reset fixed target to null
        target = null

        val switchMode = targetMode == "Switch"

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        var bestTarget: EntityLivingBase? = null
        var bestValue: Double? = null

        for (entity in theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !isSelected(
                    entity, true
                ) || switchMode && entity.entityId in prevTargetEntities
            ) continue

            val distance = Backtrack.runWithNearestTrackedDistance(entity) { thePlayer.getDistanceToEntityBox(entity) }

            if (switchMode && distance > range && prevTargetEntities.isNotEmpty()) continue

            val entityFov = rotationDifference(entity)

            if (distance > maxRange || fov != 180F && entityFov > fov) continue

            if (switchMode && !isLookingOnEntities(entity, maxSwitchFOV.toDouble())) continue

            val currentValue = when (priority.lowercase()) {
                "distance" -> distance
                "direction" -> entityFov.toDouble()
                "health" -> entity.health.toDouble()
                "livingtime" -> -entity.ticksExisted.toDouble()
                "armor" -> entity.totalArmorValue.toDouble()
                "hurtresistance" -> entity.hurtResistantTime.toDouble()
                "hurttime" -> entity.hurtTime.toDouble()
                "healthabsorption" -> (entity.health + entity.absorptionAmount).toDouble()
                "regenamplifier" -> if (entity.isPotionActive(Potion.regeneration)) {
                    entity.getActivePotionEffect(Potion.regeneration).amplifier.toDouble()
                } else -1.0

                "inweb" -> if (entity.isInWeb) -1.0 else Double.MAX_VALUE
                "onladder" -> if (entity.isOnLadder) -1.0 else Double.MAX_VALUE
                "inliquid" -> if (entity.isInWater || entity.isInLava) -1.0 else Double.MAX_VALUE
                else -> null
            } ?: continue

            if (bestValue == null || currentValue < bestValue) {
                bestValue = currentValue
                bestTarget = entity
            }
        }

        if (bestTarget != null) {
            if (Backtrack.runWithNearestTrackedDistance(bestTarget) { updateRotations(bestTarget) }) {
                target = bestTarget
                return
            }
        }

        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: EntityLivingBase, isLastClick: Boolean) {
        val thePlayer = mc.thePlayer

        if (shouldPrioritize()) return

        if (thePlayer.isBlocking && (autoBlock == "Off" && blockStatus || (autoBlock == "Packet" || autoBlock == "QuickMacro") && releaseAutoBlock)) {
            stopBlocking()

            if (!ignoreTickRule || autoBlock == "Off") {
                return
            }
        }

        // The function is only called when we are facing an entity
        if (shouldDelayClick(MovingObjectPosition.MovingObjectType.ENTITY)) {
            return
        }

        if (!blinkAutoBlock || !BlinkUtils.isBlinking) {
            val affectSprint = false.takeIf { KeepSprint.handleEvents() || keepSprint }

            thePlayer.attackEntityWithModifiedSprint(entity, affectSprint) {
                val noSwingActive = NoSwing.handleEvents()
                val shouldRender = !noSwingActive || !NoSwing.clientSide
                val shouldSendPacket = !noSwingActive || NoSwing.serverSide
                
                if (swing && shouldRender) {
                    thePlayer.swingItem()
                } else if (shouldSendPacket) {
                    sendPacket(C0APacketAnimation())
                }
            }
            
            if (autoBlock == "BlockOnNoHit") {
                blockOnNoHitDelayTick = 0
            }

            // Apply enchantment critical effect if FakeSharp is enabled
            if (EnchantmentHelper.getModifierForCreature(
                    thePlayer.heldItem, entity.creatureAttribute
                ) <= 0F && fakeSharp
            ) {
                thePlayer.onEnchantmentCritical(entity)
            }
        }

        // Start blocking after attack
        if (autoBlock != "Off" && (thePlayer.isBlocking || canBlock) && (!blinkAutoBlock && isLastClick || blinkAutoBlock && (!blinked || !BlinkUtils.isBlinking))) {
            startBlocking(entity, interactAutoBlock, autoBlock == "Fake")
        }

        resetLastAttackedTicks()
    }

    /**
     * Update rotations to enemy
     */
    private fun updateRotations(entity: Entity): Boolean {
        val player = mc.thePlayer ?: return false

        if (shouldPrioritize()) return false

        if (!options.rotationsActive) {
            return player.getDistanceToEntityBox(entity) <= range
        }

        val prediction = entity.currPos.subtract(entity.prevPos).times(2 + predictEnemyPosition.toDouble())

        val boundingBox = entity.hitBox.offset(prediction)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)

        simPlayer.rotationYaw = (currentRotation ?: player.rotation).yaw

        var pos = currPos

        repeat(predictClientMovement) {
            val previousPos = simPlayer.pos

            simPlayer.tick()

            if (predictOnlyWhenOutOfRange) {
                player.setPosAndPrevPos(simPlayer.pos)

                val currDist = player.getDistanceToEntityBox(entity)

                player.setPosAndPrevPos(previousPos)

                val prevDist = player.getDistanceToEntityBox(entity)

                player.setPosAndPrevPos(currPos, oldPos)
                pos = simPlayer.pos

                if (currDist <= range && currDist <= prevDist) {
                    return@repeat
                }
            }

            pos = previousPos
        }

        player.setPosAndPrevPos(pos)

        val rotation = searchCenter(
            boundingBox,
            generateSpotBasedOnDistance,
            outBorder && !attackTimer.hasTimePassed(attackDelay / 2),
            randomization,
            predict = false,
            lookRange = range + scanRange,
            attackRange = range,
            throughWallsRange = throughWallsRange,
            bodyPoints = listOf(highestBodyPointToTarget, lowestBodyPointToTarget),
            horizontalSearch = horizontalBodySearchRange
        )

        if (rotation == null) {
            player.setPosAndPrevPos(currPos, oldPos)

            return false
        }

        setTargetRotation(rotation, options = options)

        player.setPosAndPrevPos(currPos, oldPos)

        return true
    }

    private fun ticksSinceClick() = runTimeTicks - (attackTickTimes.lastOrNull()?.second ?: 0)

    /**
     * Check if enemy is hittable with current rotations
     */
    private fun updateHittable() {
        val eyes = mc.thePlayer.eyes

        val currentRotation = currentRotation ?: mc.thePlayer.rotation
        val target = this.target ?: return

        if (shouldPrioritize()) return

        if (!options.rotationsActive) {
            hittable = mc.thePlayer.getDistanceToEntityBox(target) <= range
            return
        }

        var chosenEntity: Entity? = null

        if (raycast) {
            chosenEntity = raycastEntity(
                range.toDouble(), currentRotation.yaw, currentRotation.pitch
            ) { entity -> !livingRaycast || entity is EntityLivingBase && entity !is EntityArmorStand }

            if (chosenEntity != null && chosenEntity is EntityLivingBase && (NoFriends.handleEvents() || !(chosenEntity is EntityPlayer && chosenEntity.isClientFriend()))) {
                if (raycastIgnored && target != chosenEntity) {
                    this.target = chosenEntity
                }
            }

            hittable = this.target == chosenEntity
        } else {
            hittable = isRotationFaced(target, range.toDouble(), currentRotation)
        }

        var shouldExcept = false

        chosenEntity ?: this.target?.run {
            if (ForwardTrack.handleEvents()) {
                ForwardTrack.includeEntityTruePos(this) {
                    checkIfAimingAtBox(this, currentRotation, eyes, onSuccess = {
                        hittable = true

                        shouldExcept = true
                    })
                }
            }
        }

        if (!hittable || shouldExcept) {
            return
        }

        val targetToCheck = chosenEntity ?: this.target ?: return

        // If player is inside entity, automatic yes because the intercept below cannot check for that
        // Minecraft does the same, see #EntityRenderer line 353
        if (targetToCheck.hitBox.isVecInside(eyes)) {
            return
        }

        var checkNormally = true

        if (Backtrack.handleEvents()) {
            Backtrack.loopThroughBacktrackData(targetToCheck) {
                var result = false

                checkIfAimingAtBox(targetToCheck, currentRotation, eyes, onSuccess = {
                    checkNormally = false

                    result = true
                }, onFail = {
                    result = false
                })

                return@loopThroughBacktrackData result
            }
        } else if (ForwardTrack.handleEvents()) {
            ForwardTrack.includeEntityTruePos(targetToCheck) {
                checkIfAimingAtBox(targetToCheck, currentRotation, eyes, onSuccess = { checkNormally = false })
            }
        }

        if (!checkNormally) {
            return
        }

        // Recreate raycast logic
        val intercept = targetToCheck.hitBox.calculateIntercept(
            eyes, eyes + getVectorForRotation(currentRotation) * range.toDouble()
        )

        // Is the entity box raycast vector visible? If not, check through-wall range
        hittable =
            isVisible(intercept.hitVec) || mc.thePlayer.getDistanceToEntityBox(targetToCheck) <= throughWallsRange
    }

    /**
     * Start blocking
     */
    private fun startBlocking(interactEntity: Entity, interact: Boolean, fake: Boolean = false) {
        val player = mc.thePlayer ?: return

        if (blockStatus && (!uncpAutoBlock || !blinkAutoBlock) || shouldPrioritize()) return

        if (mc.thePlayer.isBlocking) {
            blockStatus = true
            renderBlocking = true
            return
        }

        if (unblockMode == "Empty" && player.inventory.firstEmptyStack !in 0..8) {
            return
        }

        if (!fake) {
            if (!(blockRate > 0 && nextInt(endExclusive = 100) <= blockRate)) return

            if (interact) {
                val positionEye = player.eyes

                val boundingBox = interactEntity.hitBox

                val (yaw, pitch) = currentRotation ?: player.rotation

                val vec = getVectorForRotation(Rotation(yaw, pitch))

                val lookAt = positionEye.add(vec * maxRange.toDouble())

                val movingObject = boundingBox.calculateIntercept(positionEye, lookAt) ?: return
                val hitVec = movingObject.hitVec

                sendPackets(
                    C02PacketUseEntity(interactEntity, hitVec - interactEntity.positionVector),
                    C02PacketUseEntity(interactEntity, INTERACT)
                )

            }

            if (switchStartBlock) {
                switchToSlot((SilentHotbar.currentSlot + 1) % 9)
            }

            if (autoBlock == "QuickMacro") {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -2, -1), 255, null, 0.0f, 0.0f, 0.0f))
            } else {
                sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
            }
            blockStatus = true
        }

        renderBlocking = true

        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
    }

    /**
     * Stop blocking
     */
    private fun stopBlocking(forceStop: Boolean = false) {
        val player = mc.thePlayer ?: return

        if (!forceStop) {
            if (blockStatus && !mc.thePlayer.isBlocking) {

                when (unblockMode.lowercase()) {
                    "stop" -> {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    }

                    "switch" -> {
                        switchToSlot((SilentHotbar.currentSlot + 1) % 9)
                    }

                    "empty" -> {
                        player.inventory.firstEmptyStack.takeIf { it in 0..8 }.let {
                            if (it == null) {
                                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                                return@let
                            }

                            switchToSlot(it)
                        }
                    }
                }

                blockStatus = false
            }
        } else {
            if (blockStatus) {
                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            }

            blockStatus = false
        }

        renderBlocking = false
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet

        if (autoBlock == "Off" || !blinkAutoBlock || !blinked) return@handler

        if (player.isDead || player.ticksExisted < 20) {
            BlinkUtils.unblink()
            return@handler
        }

        if (Blink.blinkingSend() || Blink.blinkingReceive()) {
            BlinkUtils.unblink()
            return@handler
        }

        BlinkUtils.blink(packet, event)
    }

    /**
     * Checks if raycast landed on a different object
     *
     * The game requires at least 1 tick of cool-down on raycast object type change (miss, block, entity)
     * We are doing the same thing here but allow more cool-down.
     */
    private fun shouldDelayClick(currentType: MovingObjectPosition.MovingObjectType): Boolean {
        if (!useHitDelay) {
            return false
        }

        val lastAttack = attackTickTimes.lastOrNull()

        return lastAttack != null && lastAttack.first.typeOfHit != currentType && runTimeTicks - lastAttack.second <= hitDelayTicks
    }

    private fun checkIfAimingAtBox(
        targetToCheck: Entity, currentRotation: Rotation, eyes: Vec3, onSuccess: () -> Unit,
        onFail: () -> Unit = { },
    ) {
        if (targetToCheck.hitBox.isVecInside(eyes)) {
            onSuccess()
            return
        }

        // Recreate raycast logic
        val intercept = targetToCheck.hitBox.calculateIntercept(
            eyes, eyes + getVectorForRotation(currentRotation) * range.toDouble()
        )

        if (intercept != null) {
            // Is the entity box raycast vector visible? If not, check through-wall range
            hittable =
                isVisible(intercept.hitVec) || mc.thePlayer.getDistanceToEntityBox(targetToCheck) <= throughWallsRange

            if (hittable) {
                onSuccess()
                return
            }
        }

        onFail()
    }

    private fun switchToSlot(slot: Int) {
        SilentHotbar.selectSlotSilently(this, slot, immediate = true)
        SilentHotbar.resetSlot(this, true)
    }

    private fun shouldPrioritize(): Boolean = when {
        !onScaffold && (Scaffold.handleEvents() && (Scaffold.placeRotation != null || currentRotation != null) || Tower.handleEvents() && Tower.isTowering) -> true

        !onDestroyBlock && (Fucker.handleEvents() && !Fucker.noHit && Fucker.pos != null && !Fucker.isOwnBed || Nuker.handleEvents()) -> true

        activationSlot && SilentHotbar.currentSlot != preferredSlot - 1 -> true

        else -> false
    }

    private fun handleFailedSwings() {
        if (!renderBoxOnSwingFail) return

        val box = AxisAlignedBB(0.0, 0.0, 0.0, 0.05, 0.05, 0.05)

        synchronized(swingFails) {
            val fadeSeconds = renderBoxFadeSeconds * 1000L
            val colorSettings = renderBoxColor

            val renderManager = mc.renderManager

            swingFails.removeAll {
                val timestamp = System.currentTimeMillis() - it.startTime
                val transparency = (0f..255f).lerpWith(1 - (timestamp / fadeSeconds).coerceAtMost(1.0F))

                val offsetBox = box.offset(it.vec3 - renderManager.renderPos)

                RenderUtils.drawAxisAlignedBB(offsetBox, colorSettings.color(a = transparency.roundToInt()))

                timestamp > fadeSeconds
            }
        }
    }

    private fun drawAimPointBox() {
        val player = mc.thePlayer ?: return
        val target = this.target ?: return

        if (!renderPointBoxAim) {
            return
        }

        val f = aimPointBoxSize.toDouble()

        val box = AxisAlignedBB(0.0, 0.0, 0.0, f, f, f)

        val renderManager = mc.renderManager

        runWithSimulatedPosition(player, player.interpolatedPosition(player.prevPos)) {
            runWithSimulatedPosition(target, target.interpolatedPosition(target.prevPos)) {
                val rotationVec = player.eyes + getVectorForRotation(
                    serverRotation.lerpWith(currentRotation ?: player.rotation, mc.timer.renderPartialTicks)
                ) * player.getDistanceToEntityBox(target).coerceAtMost(range.toDouble())

                val offSetBox = box.offset(rotationVec - renderManager.renderPos)

                RenderUtils.drawAxisAlignedBB(offSetBox, aimPointBoxColor)
            }
        }
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun
        inline get() = mc.thePlayer.isSpectator || !isAlive(mc.thePlayer) || noConsumeAttack == "NoRotation" && isConsumingItem()

    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0

    /**
     * Check if player is able to block
     */
    private val canBlock: Boolean
        get() {
            val player = mc.thePlayer ?: return false

            if (target != null && player.heldItem?.item is ItemSword) {
                if (smartAutoBlock) {
                    if (player.isMoving && forceBlock) return false

                    if (checkWeapon && target?.heldItem?.item !is ItemSword && target?.heldItem?.item !is ItemAxe) return false

                    if (player.hurtTime > maxOwnHurtTime) return false

                    val rotationToPlayer = toRotation(player.hitBox.center, true, target!!)

                    if (rotationDifference(rotationToPlayer, target!!.rotation) > maxDirectionDiff) return false

                    if (target!!.swingProgressInt > maxSwingProgress) return false

                    if (target!!.getDistanceToEntityBox(player) > blockRange) return false
                }

                if (player.getDistanceToEntityBox(target!!) > blockMaxRange) return false

                return true
            }

            return false
        }

    /**
     * Range
     */
    private val maxRange
        get() = max(range + scanRange, throughWallsRange)

    private fun getRange(entity: Entity) =
        (if (mc.thePlayer.getDistanceToEntityBox(entity) >= throughWallsRange) range + scanRange else throughWallsRange) - if (mc.thePlayer.isSprinting) rangeSprintReduction else 0F

    /**
     * HUD Tag
     */
    override val tag
        get() = targetMode

    val isBlockingChestAura
        get() = handleEvents() && target != null

    // ==================== KillESP 渲染方法（整合自 KillESP 模块）====================

    /**
     * 获取 ESP 颜色
     */
    private fun getESPColor(entity: Entity): Color {
        if (entity is EntityLivingBase) {
            if (espColorMode.equals("Health", ignoreCase = true)) {
                val health = entity.health / entity.maxHealth
                return Color(
                    (1.0 - health).toFloat().coerceIn(0f, 1f),
                    health.toFloat().coerceIn(0f, 1f),
                    0f,
                    espColorAlpha / 255f
                )
            }
        }
        return when (espColorMode.lowercase()) {
            "custom" -> Color(espColorRed, espColorGreen, espColorBlue, espColorAlpha)
            "rainbow" -> ColorUtils.rainbow().let { Color(it.red, it.green, it.blue, espColorAlpha) }
            else -> Color(espColorRed, espColorGreen, espColorBlue, espColorAlpha)
        }
    }

    /**
     * 缓动函数 - easeInOutQuad
     */
    private fun easeInOutQuad(x: Double): Double {
        return if (x < 0.5) 2 * x * x else 1 - (-2 * x + 2).pow(2) / 2
    }

    /**
     * 缓动函数 - easeInOutQuart
     */
    private fun easeInOutQuart(x: Double): Double {
        return if (x < 0.5) 8 * x * x * x * x else 1 - (-2 * x + 2).pow(4) / 2
    }

    /**
     * 预处理 3D 渲染
     */
    private fun pre3DESP() {
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glShadeModel(GL11.GL_SMOOTH)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDepthMask(false)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        GL11.glDisable(GL11.GL_CULL_FACE)
    }

    /**
     * 后处理 3D 渲染
     */
    private fun post3DESP() {
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
        GL11.glColor4f(1f, 1f, 1f, 1f)
    }

    /**
     * 绘制圆形（用于 Jello 模式）
     */
    private fun drawJelloCircle(x: Double, y: Double, z: Double, width: Float, radius: Double, r: Float, g: Float, b: Float, alpha: Float) {
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(width)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        for (i in 0..360) {
            val calc = i * Math.PI / 180
            GL11.glColor4f(r, g, b, alpha)
            GL11.glVertex3d(x - sin(calc) * radius, y, z + cos(calc) * radius)
        }
        GL11.glEnd()
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    /**
     * Jello ESP 渲染
     */
    private fun renderJelloESP(event: Render3DEvent) {
        val targetEntity = target ?: return

        // 更新动画
        if (espAl > 0f) {
            if (System.currentTimeMillis() - espLastMS >= 1000L) {
                espDirection = -espDirection
                espLastMS = System.currentTimeMillis()
            }
            val weird = if (espDirection > 0) System.currentTimeMillis() - espLastMS else 1000L - (System.currentTimeMillis() - espLastMS)
            espProgress = weird / 1000.0
            espLastDeltaMS = System.currentTimeMillis() - espLastMS
        } else {
            espLastMS = System.currentTimeMillis() - espLastDeltaMS
        }

        val bb = targetEntity.entityBoundingBox
        val radius = bb.maxX - bb.minX
        val height = bb.maxY - bb.minY
        val posX = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * mc.timer.renderPartialTicks
        val posY = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * mc.timer.renderPartialTicks
        val posZ = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * mc.timer.renderPartialTicks

        espYPos = easeInOutQuart(espProgress) * height
        val deltaY = (if (espDirection > 0) espYPos - espYPos else espYPos - espYPos) * -espDirection * jelloGradientHeight

        espAl = animate(espAl, jelloFadeSpeed, 0f, 1f)

        if (espAl <= 0f) return

        val colour = getESPColor(targetEntity)
        val r = colour.red / 255.0f
        val g = colour.green / 255.0f
        val b = colour.blue / 255.0f

        pre3DESP()
        GL11.glTranslated(-mc.renderManager.viewerPosX, -mc.renderManager.viewerPosY, -mc.renderManager.viewerPosZ)

        GL11.glBegin(GL11.GL_QUAD_STRIP)
        for (i in 0..360) {
            val calc = i * Math.PI / 180
            val posX2 = posX - sin(calc) * radius
            val posZ2 = posZ + cos(calc) * radius
            GL11.glColor4f(r, g, b, 0f)
            GL11.glVertex3d(posX2, posY + espYPos + deltaY, posZ2)
            GL11.glColor4f(r, g, b, espAl * jelloAlpha)
            GL11.glVertex3d(posX2, posY + espYPos, posZ2)
        }
        GL11.glEnd()

        drawJelloCircle(posX, posY + espYPos, posZ, jelloWidth, radius, r, g, b, espAl)

        post3DESP()
    }

    /**
     * 动画辅助函数
     */
    private fun animate(current: Float, speed: Float, min: Float, max: Float): Float {
        return (current + speed).coerceIn(min, max)
    }

    /**
     * Zavz ESP 渲染
     */
    private fun renderZavzESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val ticks = event.partialTicks

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)
        GL11.glLineWidth(2f)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        val x = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * ticks - mc.renderManager.viewerPosX
        val z = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * ticks - mc.renderManager.viewerPosZ
        var y = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * ticks - mc.renderManager.viewerPosY

        val radius = 0.65
        val precision = 360
        var startPos = espStart % 360
        espStart += zavzSpeed

        for (i in 0..precision) {
            val posX = x + radius * cos(startPos + i * Math.PI * 2 / (precision / 2.0))
            val posZ = z + radius * sin(startPos + i * Math.PI * 2 / (precision / 2.0))
            GL11.glColor4f(1f, 1f, 1f, 1f)
            GL11.glVertex3d(posX, y, posZ)
            y += targetEntity.height / precision
        }

        GL11.glEnd()
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)

        if (zavzDual) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(false)
            GL11.glLineWidth(2f)
            GL11.glBegin(GL11.GL_LINE_STRIP)

            startPos = espStart % 360
            espStart += zavzSpeed
            y = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * ticks - mc.renderManager.viewerPosY + targetEntity.height

            for (i in 0..precision) {
                val posX = x + radius * cos(-(startPos + i * Math.PI * 2 / (precision / 2.0)))
                val posZ = z + radius * sin(-(startPos + i * Math.PI * 2 / (precision / 2.0)))
                GL11.glColor4f(1f, 1f, 1f, 1f)
                GL11.glVertex3d(posX, y, posZ)
                y -= targetEntity.height / precision
            }

            GL11.glEnd()
            GL11.glDepthMask(true)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    /**
     * Zywl ESP 渲染
     */
    private fun renderZywlESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val ticks = event.partialTicks

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)

        renderZywlRing(targetEntity, ticks, false)
        if (zavzDual) renderZywlRing(targetEntity, ticks, true)

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun renderZywlRing(targetEntity: EntityLivingBase, ticks: Float, dualRing: Boolean) {
        val x = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * ticks - mc.renderManager.viewerPosX
        val z = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * ticks - mc.renderManager.viewerPosZ
        var y = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * ticks - mc.renderManager.viewerPosY

        val radius = 0.65
        val precision = 360
        var startPos = espStart % 360
        espStart += zavzSpeed

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)
        GL11.glLineWidth(2f)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        for (i in 0..precision) {
            val angle = startPos + i * Math.PI * 2.0 / precision
            val posX = x + radius * cos(angle)
            val posZ = z + radius * sin(angle)

            val offset = Math.abs(System.currentTimeMillis() / 10L) / 100.0 + y
            val alpha = if (dualRing) 0 else 170
            val color = ColorUtils.interpolateColor(Color.WHITE, Color.BLACK, offset.toFloat())

            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, alpha / 255f)
            GL11.glVertex3d(posX, y, posZ)

            y += if (dualRing) -targetEntity.height / precision else targetEntity.height / precision
        }

        GL11.glEnd()
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glPopMatrix()
    }

    /**
     * Sigma ESP 渲染
     */
    private fun renderSigmaESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val drawTime = System.currentTimeMillis() % 2000
        val drawMode = drawTime > 1000
        var drawPercent = drawTime / 1000.0

        drawPercent = if (!drawMode) 1 - drawPercent else drawPercent - 1
        drawPercent = easeInOutQuad(drawPercent)

        val points = mutableListOf<Vec3>()
        val bb = targetEntity.entityBoundingBox
        val radius = bb.maxX - bb.minX
        val height = bb.maxY - bb.minY
        val posX = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * mc.timer.renderPartialTicks
        var posY = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * mc.timer.renderPartialTicks

        if (drawMode) posY -= 0.5 else posY += 0.5

        val posZ = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * mc.timer.renderPartialTicks

        for (i in 0..360 step 7) {
            points.add(Vec3(
                posX - sin(i * Math.PI / 180.0) * radius,
                posY + height * drawPercent,
                posZ + cos(i * Math.PI / 180.0) * radius
            ))
        }
        points.add(points[0])

        mc.entityRenderer.disableLightmap()
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        val baseMove = if (drawPercent > 0.5) (1 - drawPercent) else drawPercent
        val min = (height / 60) * 20 * (1 - baseMove) * (if (drawMode) -1 else 1)

        for (i in 0..20) {
            var moveFace = (height / 60.0) * i * baseMove
            if (drawMode) moveFace = -moveFace

            val firstPoint = points[0]
            GL11.glVertex3d(
                firstPoint.xCoord - mc.renderManager.viewerPosX,
                firstPoint.yCoord - moveFace - min - mc.renderManager.viewerPosY,
                firstPoint.zCoord - mc.renderManager.viewerPosZ
            )
            GL11.glColor4f(1f, 1f, 1f, 0.7f * (i / 20f))

            for (vec3 in points) {
                GL11.glVertex3d(
                    vec3.xCoord - mc.renderManager.viewerPosX,
                    vec3.yCoord - moveFace - min - mc.renderManager.viewerPosY,
                    vec3.zCoord - mc.renderManager.viewerPosZ
                )
            }
            GL11.glColor4f(0f, 0f, 0f, 0f)
        }

        GL11.glEnd()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    /**
     * FDP ESP 渲染
     */
    private fun renderFdPESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val drawTime = (System.currentTimeMillis() % 1500).toInt()
        val drawMode = drawTime > 750
        var drawPercent = drawTime / 750.0

        drawPercent = if (!drawMode) 1 - drawPercent else drawPercent - 1

        mc.entityRenderer.disableLightmap()
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_DEPTH_TEST)

        val bb = targetEntity.entityBoundingBox
        val radius = ((bb.maxX - bb.minX + (bb.maxZ - bb.minZ)) * 0.5).toFloat()

        val x = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ

        GL11.glLineWidth(radius * 8f)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        for (i in 0..360 step 10) {
            val hue = if (i < 180) i / 180f else (-(i - 360)) / 180f
            val color = Color.getHSBColor(hue, 0.7f, 1f)
            GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
            GL11.glVertex3d(x - sin(i * Math.PI / 180.0) * radius, y, z + cos(i * Math.PI / 180.0) * radius)
        }

        GL11.glEnd()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    /**
     * Tracers ESP 渲染
     */
    private fun renderTracersESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val player = mc.thePlayer ?: return

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(tracersThickness)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        GL11.glBegin(GL11.GL_LINES)
        val color = getESPColor(targetEntity)
        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        GL11.glVertex3d(
            player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX,
            player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY + player.eyeHeight.toDouble(),
            player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        )

        GL11.glVertex3d(
            targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX,
            targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY + targetEntity.height / 2,
            targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        )

        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        GlStateManager.resetColor()
    }

    /**
     * Lies ESP 渲染
     */
    private fun renderLiesESP(event: Render3DEvent) {
        val targetEntity = target ?: return

        val interval = 3000
        val drawTime = System.currentTimeMillis() % interval
        val drawMode = drawTime > (interval / 2)
        var drawPercent = drawTime / (interval / 2.0)

        if (!drawMode) drawPercent = 1 - drawPercent else drawPercent -= 1
        drawPercent = easeInOutQuad(drawPercent)

        mc.entityRenderer.disableLightmap()
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glShadeModel(GL11.GL_FLAT)

        val bb = targetEntity.entityBoundingBox
        val radius = ((bb.maxX - bb.minX) + (bb.maxZ - bb.minZ)) * 0.5
        val height = (bb.maxY - bb.minY).toFloat()
        val x = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = (targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY) + height * drawPercent
        val z = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ

        val eased = ((height / 3) * (if (drawPercent > 0.5) (1 - drawPercent) else drawPercent) * (if (drawMode) -1 else 1)).toFloat()

        for (i in 5..360 step 5) {
            val color = Color.getHSBColor(
                if (i < 180) i / 180f else (-(i - 360)) / 180f,
                0.7f,
                1f
            )
            val x1 = x - sin(i * Math.PI / 180.0) * radius
            val z1 = z + cos(i * Math.PI / 180.0) * radius
            val x2 = x - sin((i - 5) * Math.PI / 180.0) * radius
            val z2 = z + cos((i - 5) * Math.PI / 180.0) * radius

            GL11.glBegin(GL11.GL_QUADS)
            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0f)
            GL11.glVertex3d(x1, y + eased, z1)
            GL11.glVertex3d(x2, y + eased, z2)
            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 150f / 255f)
            GL11.glVertex3d(x2, y, z2)
            GL11.glVertex3d(x1, y, z1)
            GL11.glEnd()
        }

        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glShadeModel(GL11.GL_FLAT)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    /**
     * Sims ESP 渲染
     */
    private fun renderSimsESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val color = if (targetEntity.hurtTime <= 0) Color(80, 255, 80, 200) else Color(255, 0, 0, 200)

        val x = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        val radius = 0.15f

        GL11.glPushMatrix()
        GL11.glTranslated(x, y + 2, z)
        GL11.glRotatef(-targetEntity.width, 0f, 1f, 0f)

        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(1.5f)

        GL11.glBegin(GL11.GL_LINE_STRIP)
        for (i in 0..360 step 10) {
            val angle = Math.toRadians(i.toDouble())
            GL11.glVertex3d(cos(angle) * radius, 0.0, sin(angle) * radius)
            GL11.glVertex3d(cos(angle) * radius, 0.3, sin(angle) * radius)
        }
        GL11.glEnd()

        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glPopMatrix()
    }

    /**
     * 渲染攻击范围圈
     */
    private fun renderRangeCircle() {
        val player = mc.thePlayer ?: return
        
        GL11.glPushMatrix()
        
        val interpolatedX = player.lastTickPosX + (player.posX - player.lastTickPosX) * mc.timer.renderPartialTicks - mc.renderManager.viewerPosX
        val interpolatedY = player.lastTickPosY + (player.posY - player.lastTickPosY) * mc.timer.renderPartialTicks - mc.renderManager.viewerPosY
        val interpolatedZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * mc.timer.renderPartialTicks - mc.renderManager.viewerPosZ
        
        GL11.glTranslated(interpolatedX, interpolatedY, interpolatedZ)
        
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        
        GL11.glLineWidth(rangeCircleThickness)
        GL11.glColor4f(rangeCircleRed / 255.0f, rangeCircleGreen / 255.0f, rangeCircleBlue / 255.0f, rangeCircleAlpha / 255.0f)
        
        GL11.glRotatef(90f, 1f, 0f, 0f)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        
        val attackRange = range.toDouble().toFloat()
        for (i in 0..360 step 5) {
            val angleRadians = Math.toRadians(i.toDouble())
            GL11.glVertex2f(
                cos(angleRadians).toFloat() * attackRange,
                sin(angleRadians).toFloat() * attackRange
            )
        }
        
        GL11.glEnd()
        
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        
        GL11.glPopMatrix()
    }

    private fun generateNoise(): ((Rotation) -> Rotation)? {
        val presets = noisePreset.get()
        if (presets.isEmpty() || (presets.size == 1 && presets.contains("Custom"))) return null

        val customNoise = { r: Rotation ->
            val yawOffset = (yawMicroJitter.random() - yawMicroJitter.start) * 0.25f +
                (pitchMicroJitter.random() * (sineAmplitude.random() * 0.05f)) +
                (ouSigma.random() * 0.02f)
            val pitchOffset = (pitchMicroJitter.random() - pitchMicroJitter.start) * 0.25f +
                (ouTheta.random() * 0.02f)
            Rotation(r.yaw + yawOffset, r.pitch + pitchOffset)
        }

        return NoisePresets.combinePresets(presets, customNoise)
    }
}

data class SwingFailData(val vec3: Vec3, val startTime: Long)