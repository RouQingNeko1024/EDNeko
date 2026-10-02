/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim.GrimCollide
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim.NewGrim
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.hypixel.HypixelHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.hypixel.HypixelLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.karhu.Karhu
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.karhu.Karhu2
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.polar.Polar
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spartan.SpartanYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreBHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreOnGround
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusFHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusLowHopNew
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanGround288
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanPredictionExploit
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.BalanceTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.Grim2Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.GroundPacket
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.GroundStrafeHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.MoraLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.StrafeHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.VanillaHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.LatestVerusHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim.Grim
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave.IntaveHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave.IntaveTimerHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.AACPortFDP
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import org.lwjgl.input.Keyboard

object Speed : Module("Speed", Category.MOVEMENT, Keyboard.KEY_X) {

    private val speedModes = arrayOf(

        // NCP
        NCPBHop,
        NCPFHop,
        SNCPBHop,
        NCPHop,
        NCPYPort,
        UNCPHop,
        UNCPHopNew,

        // AAC
        AACHop3313,
        AACHop350,
        AACHop4,
        AACHop5,
        AAC5LagHop,
        AAC5LagHop2,
        AAC5Infinite,
        AACPortFDP,

        // Spartan
        SpartanYPort,

        // Spectre
        SpectreLowHop,
        SpectreBHop,
        SpectreOnGround,

        // Verus
        VerusHop,
        VerusFHop,
        VerusLowHop,
        VerusLowHopNew,
        LatestVerusHop,

        // Vulcan
        VulcanHop,
        VulcanLowHop,
        VulcanGround288,
        VulcanPredictionExploit,

        // Matrix
        OldMatrixHop,
        MatrixHop,
        MatrixSlowHop,
        Matrix2,
        Matrix3,

        // Intave
        IntaveHop,
        IntaveTimerHop,
        Intave1255,
        Intave1309,
        Intave140xA,
        Intave140xB,
        Intave141x,
        IntaveTimer,
        IntaveHop14,
        Intave14New,
        Intave2,
        Intave3,

        // PolarAC
        Polar,


        // Server specific
        TeleportCubeCraft,
        HypixelHop,
        HypixelLowHop,
        BlocksMCHop,
        MinemenHop,
        MineBlaze,
        MineBlazeTimer,
        IntaveNew,

        // Karhu
        Karhu,
        Karhu2,

        // Grim
        Grim,
        GrimCollide,
        NewGrim,

        // Other
        Boost,
        Frame,
        MiJump,
        OnGround,
        SlowHop,
        Legit,
        CustomSpeed,
        LowHop,
        Pulldown,
        GroundTimerBoost,
        DoubleJump,
        SimpleBoost,
        PredictionTimer,
        PredictionTimer2,
        RotationExploit,
        AllAC,
        SkipTick,
        BalanceTimer,
        Grim2Speed,
        GroundPacket,
        GroundStrafeHop,
        MoraLowHop,
        StrafeHop,
        VanillaHop,

        // Rise
        RiseBlocksMC,
        RiseKoksCraft,
        RiseLegit,
        RiseStrafe,
        RiseNCP,
        RiseOldNCPYPort,
        RiseVanilla,
        RisePolar,
        RiseMatrix,
        RiseTatako,
        RiseMineMenClub,
        RiseGrim,
        RiseGrim2,
        RiseVerus,
        RiseVulcan,
        RiseWatchdog6Tick,
        RiseWatchdogPrediction,
        RiseWatchdog,
        RiseMiniBlox,
    )

    /**
     * Old/Deprecated Modes
     */
    private val deprecatedMode = arrayOf(
        TeleportCubeCraft,

        OldMatrixHop,

        VerusLowHop,

        SpectreLowHop, SpectreBHop, SpectreOnGround,

        AACHop3313, AACHop350, AACHop4,

        NCPBHop, NCPFHop, SNCPBHop, NCPHop, NCPYPort,

        MiJump, Frame
    )

    private val showDeprecated by boolean("DeprecatedMode", true).onChanged { value ->
        mode.changeValue(modesList.first { it !in deprecatedMode }.modeName)
        mode.updateValues(modesList.filter { value || it !in deprecatedMode }.map { it.modeName }.toTypedArray())
    }

    private var modesList = speedModes

    val mode = choices("Mode", modesList.map { it.modeName }.toTypedArray(), "Legit")

    // NewGrim
    val motionBoost by boolean("BoostMotion?",false) {mode.get() == "NewGrim"}

    // PredictionTimer
    val predictionGroundTimer by float("PredictionGroundTimer", 1.5f, 1f..3.0f) { mode.get() == "PredictionTimer" }

    // PredictionTimer2
    val prediction2TimerSpeed by float("Prediction2TimerSpeed", 1.5f, 1f..3.0f) { mode.get() == "PredictionTimer2" }
    val prediction2CycleLength by int("Prediction2CycleLength", 8, 5..60) { mode.get() == "PredictionTimer2" }
    val prediction2BoostDuration by int("Prediction2BoostDuration", 2, 1..5) { mode.get() == "PredictionTimer2" }

    val skipDelay by int("SkipDelay",3,1..20) {mode.get() == "SkipTick"}
    val skipTicks by int("SkipTicks",2,0..20) {mode.get() == "SkipTick"}.onChange { _,new -> new.coerceAtMost(skipDelay - 1)  }

    // IntaveNew
    val intaveNewAutoJump by boolean("AutoJump",true) {mode.get() in arrayOf("IntaveNew","Matrix3")}

    // SimpleBoost
    val boostTime by multiChoices("BoostTime",arrayOf("Ground","Air"),setOf("Ground","Air")) {mode.get() == "SimpleBoost"}
    val groundBoostFactor by float("GroundBoostFactor",0.01f,0.0f..2.0f) {mode.get() == "SimpleBoost" && "Ground" in boostTime}
    val simpleGroundTimer by float("GroundTimer",1.0f,0.01f..100.0f) {mode.get() == "SimpleBoost" && "Ground" in boostTime}
    val groundBoostTimes by int("MaxGroundChangeTimes",1,1..100) {mode.get() == "SimpleBoost" && "Air" in boostTime}
    val airBoostFactor by float("AirBoostFactor",0.01f,0.0f..2.0f) {mode.get() == "SimpleBoost" && "Air" in boostTime}
    val simpleAirTimer by float("AirTimer",1.0f,0.01f..100.0f) {mode.get() == "SimpleBoost" && "Air" in boostTime}
    val airBoostTimes by int("MaxAirChangeTimes",1,1..100) {mode.get() == "SimpleBoost" && "Air" in boostTime}
    val notResetTimerSpeed by boolean("NotResetTimerSpeedWhenReachedLimit",false) {mode.get() == "SimpleBoost"}

    // Intave2
    val groundStrafe by boolean("GroundStrafe", false) {mode.get() == "Intave2"}

    // Custom Speed
    val customY by float("CustomY", 0.42f, 0f..4f) { mode.get() == "Custom" }
    val customGroundStrafe by float("CustomGroundStrafe", 1.6f, 0f..2f) { mode.get() == "Custom" }
    val customAirStrafe by float("CustomAirStrafe", 0f, 0f..2f) { mode.get() == "Custom" }
    val customGroundTimer by float("CustomGroundTimer", 1f, 0.1f..2f) { mode.get() == "Custom" }
    val customAirTimerTick by int("CustomAirTimerTick", 5, 1..20) { mode.get() == "Custom" }
    val customAirTimer by float("CustomAirTimer", 1f, 0.1f..2f) { mode.get() == "Custom" }

    // Extra options
    val resetXZ by boolean("ResetXZ", false) { mode.get() == "Custom" }
    val resetY by boolean("ResetY", false) { mode.get() == "Custom" }
    val notOnConsuming by boolean("NotOnConsuming", false) { mode.get() == "Custom" }
    val notOnFalling by boolean("NotOnFalling", false) { mode.get() == "Custom" }
    val notOnVoid by boolean("NotOnVoid", true) { mode.get() == "Custom" }

    // TeleportCubecraft Speed
    val cubecraftPortLength by float("CubeCraft-PortLength", 1f, 0.1f..2f)
    { mode.get() == "TeleportCubeCraft" }

    // IntaveHop14 Speed
    val boost by boolean("Boost", true) { mode.get() == "IntaveHop14" }
    val initialBoostMultiplier by float("InitialBoostMultiplier", 1f, 0.01f..10f)
    { boost && mode.get() == "IntaveHop14" }
    val intaveLowHop by boolean("LowHop", true) { mode.get() == "IntaveHop14" }
    val strafeStrength by float("StrafeStrength", 0.29f, 0.1f..0.29f)
    { mode.get() == "IntaveHop14" }
    val groundTimer by float("GroundTimer", 0.5f, 0.1f..5f) { mode.get() == "IntaveHop14" }
    val airTimer by float("AirTimer", 1.09f, 0.1f..5f) { mode.get() == "IntaveHop14" }

    // IntaveNew
    val useTimer by boolean("Timer",false) {mode.get() == "Intave14.8.4"}
    val Debugger by boolean("Debugger", false) {mode.get() == "Intave14.8.4" }

    // UNCPHopNew Speed
    private val pullDown by boolean("PullDown", true) { mode.get() == "UNCPHopNew" }
    val onTick by int("OnTick", 5, 5..9) { pullDown && mode.get() == "UNCPHopNew" }
    val onHurt by boolean("OnHurt", true) { pullDown && mode.get() == "UNCPHopNew" }
    val shouldBoost by boolean("ShouldBoost", true) { mode.get() == "UNCPHopNew" }
    val timerBoost by boolean("TimerBoost", true) { mode.get() == "UNCPHopNew" }
    val damageBoost by boolean("DamageBoost", true) { mode.get() == "UNCPHopNew" }
    val lowHop by boolean("LowHop", true) { mode.get() == "UNCPHopNew" }
    val airStrafe by boolean("AirStrafe", true) { mode.get() == "UNCPHopNew" }

    // MatrixHop Speed
    val matrixLowHop by boolean("LowHop", true)
    { mode.get() == "MatrixHop" || mode.get() == "MatrixSlowHop" }
    val extraGroundBoost by float("ExtraGroundBoost", 0.2f, 0f..0.5f)
    { mode.get() == "MatrixHop" || mode.get() == "MatrixSlowHop" }
    // GrimCollide Speed
    val boostSpeed by float("CollideBoostSpeed",0.08f,0.01f..0.08f,"b/s") { mode.get() == "GrimCollide" }

    // HypixelLowHop Speed
    val glide by boolean("Glide", true) { mode.get() == "HypixelLowHop" }

    // BlocksMCHop Speed
    val fullStrafe by boolean("FullStrafe", true) { mode.get() == "BlocksMCHop" }
    val bmcLowHop by boolean("LowHop", true) { mode.get() == "BlocksMCHop" }
    val bmcDamageBoost by boolean("DamageBoost", true) { mode.get() == "BlocksMCHop" }
    val damageLowHop by boolean("DamageLowHop", false) { mode.get() == "BlocksMCHop" }
    val safeY by boolean("SafeY", true) { mode.get() == "BlocksMCHop" }

    val DebuggerLowHop by boolean("Debugger",false) {mode.get() == "LowHop"}

    val MotionYReduceMode by choices("MotionYReduceMode",arrayOf("Percent","Number"),"Percent") {mode.get() == "Pulldown"}
    val BoostFactor by float("BoostFactor",0.5F,0.00f..2F) {mode.get() == "Pulldown" && MotionYReduceMode == "Percent"}
    val BoostNumber by float("BoostNumber",0.5F,0.00f..2F) {mode.get() == "Pulldown" && MotionYReduceMode == "Number"}
    val MaxTriggerChange by int("MaxTriggerTimes",3,1..20) {mode.get() == "Pulldown"}
    val TimerPulldown by float("Timer",1f,0.1f..10.0F) {mode.get() == "Pulldown"}
    val DebuggerPulldown by boolean("Debugger",false) {mode.get() == "Pulldown"}

    // GroundTimerBoost
    val ChargeTick by int("ChargeTick",3,1..20) {mode.get() == "GroundTimerBoost"}
    val ChargeTimer by float("ChargeTimer",0.1f,0.01f..2f) {mode.get() == "GroundTimerBoost"}
    val BoostTick by int("BoostTick",1,1..20) {mode.get() == "GroundTimerBoost"}
    val BoostTimer by float("BoostTimer",10f,0.1f..1000f) {mode.get() == "GroundTimerBoost"}


    // Polar
    var polarTick = 0

    // Intave14.0.x-B
    var intaveBTick by int("Intave14.0.x-B-GroundTick",1,0..5) {mode.get() == "Intave14.0.x-B"}

    // Intave12.5.5
    var stage = 0
    var hasDamaged = false
    var intave1255Ticks = 0

    // MineBlaze
    val mineBlazeBoostFactor by float("MineBlazeBoostFactor",1.0015f,1.0f..2f) {mode.get() == "MineBlaze"}
    val mineBlazeTimer by boolean("MineBlazeTimer",false) {mode.get() == "MineBlaze"}
    val mineBlazeCheckEnvironment by boolean("MineBlazeCheckEnvironment",true) {mode.get() == "MineBlaze"}

    // ForceSprint
    val forceSprint by boolean("ForceSprinting",true) {mode.get() != "Legit"}

    // VanillaHop Speed
    val vanillaHopSpeed by float("VanillaHop-Speed", 0.4f, 0.1f..2f) { mode.get() == "VanillaHop" }
    val vanillaHopJump by boolean("VanillaHop-Jump", true) { mode.get() == "VanillaHop" }
    val vanillaHopFastStop by boolean("VanillaHop-FastStop", true) { mode.get() == "VanillaHop" }

    // BalanceTimer Speed
    val balanceTimerRiseTimer by float("BalanceTimer-RiseTimer", 1.5f, 0.1f..10f) { mode.get() == "BalanceTimer" }
    val balanceTimerFallTimer by float("BalanceTimer-FallTimer", 0.5f, 0.01f..10f) { mode.get() == "BalanceTimer" }
    val balanceTimerAutoBlink by boolean("BalanceTimer-AutoBlink", false) { mode.get() == "BalanceTimer" }
    val balanceTimerBlinkThreshold by int("BalanceTimer-BlinkThreshold", 500, 10..1000) { balanceTimerAutoBlink && mode.get() == "BalanceTimer" }
    val balanceTimerDebug by boolean("BalanceTimer-Debug", false) { mode.get() == "BalanceTimer" }

    // Rise Vanilla
    val riseVanillaSpeed by float("RiseVanilla-Speed", 1f, 0.1f..9.5f) { mode.get() == "RiseVanilla" }

    // Rise Legit
    val riseLegitNoJumpDelay by boolean("RiseLegit-NoJumpDelay", true) { mode.get() == "RiseLegit" }
    val riseLegitTimerBoost by boolean("RiseLegit-TimerBoost", true) { mode.get() == "RiseLegit" }

    // Rise Strafe
    val riseStrafeHurtBoost by boolean("RiseStrafe-HurtBoost", false) { mode.get() == "RiseStrafe" }
    val riseStrafeBoostSpeed by float("RiseStrafe-BoostSpeed", 1f, 0.1f..9.5f) { riseStrafeHurtBoost && mode.get() == "RiseStrafe" }

    // Rise NCP
    val riseNCPJumpMotion by float("RiseNCP-JumpMotion", 0.4f, 0.4f..0.42f) { mode.get() == "RiseNCP" }
    val riseNCPGroundSpeed by float("RiseNCP-GroundSpeed", 1.75f, 0.1f..2.5f) { mode.get() == "RiseNCP" }
    val riseNCPBunnySlope by float("RiseNCP-BunnySlope", 0.66f, 0f..1f) { mode.get() == "RiseNCP" }
    val riseNCPTimer by float("RiseNCP-Timer", 1f, 0.1f..10f) { mode.get() == "RiseNCP" }
    val riseNCPCustomBoost by boolean("RiseNCP-CustomBoost", false) { mode.get() == "RiseNCP" }
    val riseNCPBoostSpeed by float("RiseNCP-BoostSpeed", 0.8f, 0.1f..9.5f) { riseNCPCustomBoost && mode.get() == "RiseNCP" }
    val riseNCPLowHop by boolean("RiseNCP-LowHop", false) { mode.get() == "RiseNCP" }
    val riseNCPYPortHop by boolean("RiseNCP-YPortHop", false) { mode.get() == "RiseNCP" }
    val riseNCPHurtTime by int("RiseNCP-HurtTime", 6, 1..10) { riseNCPCustomBoost && mode.get() == "RiseNCP" }

    // Rise Matrix
    val riseMatrixTimerSneak by float("RiseMatrix-TimerSneak", 30f, 1f..100f) { mode.get() == "RiseMatrix" }

    // Rise Grim
    val riseGrimFastFall by boolean("RiseGrim-FastFall", true) { mode.get() == "RiseGrim" }
    val riseGrimMoveFlyingIncrease by float("RiseGrim-MoveFlyingIncrease", 0.0001f, 0f..0.001f) { mode.get() == "RiseGrim" }

    // Rise Grim2
    val riseGrim2Speed by float("RiseGrim2-Speed", 1f, 0f..1f) { mode.get() == "RiseGrim2" }
    val riseGrim2HighPing by boolean("RiseGrim2-HighPing", false) { mode.get() == "RiseGrim2" }

    // Grim2 Speed
    val grim2Speed by float("Grim2-Speed", 1f, 0f..1f) { mode.get() == "Grim2" }
    val grim2HighPing by boolean("Grim2-HighPing", false) { mode.get() == "Grim2" }

    // Rise Verus
    val riseVerusMode by choices("RiseVerus-Mode", arrayOf("LowHop", "Hop", "yPort"), "LowHop") { mode.get() == "RiseVerus" }

    // Rise Vulcan
    val riseVulcanMode by choices("RiseVulcan-Mode", arrayOf("LowHop", "Yport", "Ground"), "LowHop") { mode.get() == "RiseVulcan" }

    // Rise Watchdog
    val riseWatchdogMode by choices("RiseWatchdog-Mode", arrayOf("Strafe", "LowHop", "Hop"), "Strafe") { mode.get() == "RiseWatchdog" }

    // Grim Speed
    val grimAirTimer by float("GrimAirTimer", 1.08f, 1f..2f) { mode.get() == "Grim" }
    val grimTimerBoost by boolean("GrimTimerBoost", true) { mode.get() == "Grim" }
    val grimSpoofGround by boolean("GrimSpoofGround", true) { mode.get() == "Grim" }
    val grimPullDown by boolean("GrimPullDown", true) { mode.get() == "Grim" }
    val grimPullDownTicks by float("GrimPullDownTicks", 8f, 1f..20f)
    { grimPullDown && mode.get() == "Grim" }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (thePlayer.isSneaking)
            return@handler

        if (thePlayer.isMoving && !sprintManually)
            thePlayer.isSprinting = true

        modeModule.onUpdate()
    }

    val onMotion = handler<MotionEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        if (thePlayer.isSneaking || event.eventState != EventState.PRE)
            return@handler

        if (thePlayer.isMoving && !sprintManually)
            thePlayer.isSprinting = true

        modeModule.onMotion()
        modeModule.onMotion2(event)
    }

    val onPostMotion = handler<MotionEvent> { event ->
        if (mc.thePlayer?.isSneaking == true || event.eventState != EventState.POST)
            return@handler

        modeModule.onPostMotion()
    }

    val onMoveInput = handler<MovementInputEvent> { event ->
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onMoveInput(event)
    }

    val onMove = handler<MoveEvent> { event ->
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onMove(event)
    }

    val tickHandler = handler<GameTickEvent> {
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onTick()
    }
    val gameLoopHandler = handler<GameLoopEvent> { e->
        if (mc.thePlayer?.isSneaking == true)
            return@handler
        modeModule.onGameLoop(event = e)
    }

    val onStrafe = handler<StrafeEvent> {
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onStrafe()
    }

    val onJump = handler<JumpEvent> { event ->
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onJump(event)
    }

    val onPacket = handler<PacketEvent> { event ->
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onPacket(event)
    }

    override fun onEnable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f
        stage = 0

        modeModule.onEnable()
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f
        mc.thePlayer.speedInAir = 0.02f

        modeModule.onDisable()
    }

    override val tag
        get() = mode.get()

    private val modeModule
        get() = speedModes.find { it.modeName == mode.get() }!!

    private val sprintManually
        get() = modeModule === Legit || !forceSprint
}