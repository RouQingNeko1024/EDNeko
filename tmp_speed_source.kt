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
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Speed : Module("Speed", Category.MOVEMENT) {

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
    val boostTime by multiChoices("BoostTime",arrayOf("Ground","Air"),arrayOf("Ground","Air")) {mode.get() == "SimpleBoost"}
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
