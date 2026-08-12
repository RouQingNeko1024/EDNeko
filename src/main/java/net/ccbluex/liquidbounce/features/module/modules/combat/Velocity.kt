/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.exploit.Disabler
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.isOnGround
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockSoulSand
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing.DOWN
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.WorldSettings
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.random.Random

object Velocity : Module("Velocity", Category.COMBAT) {

    /**
     * OPTIONS
     */
    private val mode by choices(
        "Mode", arrayOf(
            "Simple", "AAC", "AACPush", "AACZero", "AACv4",
            "Reverse", "SmoothReverse", "Jump", "Glitch", "Legit",
            "GhostBlock", "Vulcan", "S32Packet", "MatrixReduce",
            "IntaveReduce", "Intave14", "Intave14.3.3", "IntaveStrong", "AttackReduce",
            "Delay", "GrimC03", "Hypixel", "HypixelAir",
            "Click", "BlocksMC", "Polar", "Intave/Polar-Flag", "Buffer", "Prediction",
            "SmartJumpReset", "MatrixNoXZ", "Intave13KeepLow", "Intave13Reverse",
            "Intave13GommeZero", "AAC3.3.12", "AAC3.3.14", "Intave13Wall",
            "Intave13Old", "Matrix6.6.1", "Vulcan2.0.1", "GrimCombat",
            "AAC4Reduce", "AAC5Reduce", "AAC5.2.0", "AAC5.2.0Combat",
            "Grim", "Grim1.17", "GrimC07", "GrimDamage", "MatrixReverse",
            "MatrixSimple", "HypixelBoost", "Minemen", "Phase", "SideStrafe",
            "Spoof", "Tick"
        ), "Simple"
    )

    private val horizontal by float("Horizontal", 0F, -1F..1F) { mode in arrayOf("Simple", "AAC", "Legit") }
    private val vertical by float("Vertical", 0F, -1F..1F) { mode in arrayOf("Simple", "Legit") }

    // Reverse
    private val reverseStrength by float("ReverseStrength", 1F, 0.1F..1F) { mode == "Reverse" }
    private val reverse2Strength by float("SmoothReverseStrength", 0.05F, 0.02F..0.1F) { mode == "SmoothReverse" }

    private val onLook by boolean("onLook", false) { mode in arrayOf("Reverse", "SmoothReverse") }
    private val range by float("Range", 3.0F, 1F..5.0F) {
        onLook && mode in arrayOf("Reverse", "SmoothReverse")
    }
    private val maxAngleDifference by float("MaxAngleDifference", 45.0f, 5.0f..90f) {
        onLook && mode in arrayOf("Reverse", "SmoothReverse")
    }

    // AAC Push
    private val aacPushXZReducer by float("AACPushXZReducer", 2F, 1F..3F) { mode == "AACPush" }
    private val aacPushYReducer by boolean("AACPushYReducer", true) { mode == "AACPush" }

    // AAC v4
    private val aacv4MotionReducer by float("AACv4MotionReducer", 0.62F, 0F..1F) { mode == "AACv4" }

    // Legit
    private val legitDisableInAir by boolean("DisableInAir", true) { mode == "Legit" }

    // Chance
    private val chance by int("Chance", 100, 0..100) { mode == "Jump" || mode == "Legit" }

    // Jump
    private val jumpCooldownMode by choices("JumpCooldownMode", arrayOf("Ticks", "ReceivedHits"), "Ticks")
    { mode == "Jump" }
    private val ticksUntilJump by int("TicksUntilJump", 4, 0..20)
    { jumpCooldownMode == "Ticks" && mode == "Jump" }
    private val hitsUntilJump by int("ReceivedHitsUntilJump", 2, 0..5)
    { jumpCooldownMode == "ReceivedHits" && mode == "Jump" }

    // Ghost Block
    private val hurtTimeRange by intRange("HurtTime", 1..9, 1..10) {
        mode == "GhostBlock"
    }

    // Delay
    private val spoofDelay by int("SpoofDelay", 500, 0..5000) { mode == "Delay" }
    var delayMode = false

    // IntaveReduce
    private val reduceFactor by float("Factor", 0.6f, 0.6f..1f) { mode == "IntaveReduce" }
    private val hurtTime by int("HurtTime", 9, 1..10) { mode == "IntaveReduce" }

    // MatrixReduce
    private val matrixReduceFactor by float("MatrixReduceFactor", 0.6f, 0.0f..1f) { mode == "MatrixReduce" }
    private val matrixReduceDebug by boolean("MatrixReduceDebug", false) { mode == "MatrixReduce" }

    // Intave14
    private val intave14Timer1 by float("Intave14-Timer1", 0.3f, 0.1f..2.0f) { mode == "Intave14" }
    private val intave14Timer2 by float("Intave14-Timer2", 5.0f, 1.0f..10.0f) { mode == "Intave14" }
    private val intave14TriggerTimes by int("Intave14-TriggerTimes", 2, 1..3) { mode == "Intave14" }
    private val intave14FirstReduce by int("Intave14-FirstReduce", 9, 1..10) { mode == "Intave14" && intave14TriggerTimes >= 1 }
    private val intave14SecondReduce by int("Intave14-SecondReduce", 8, 1..10) { mode == "Intave14" && intave14TriggerTimes >= 2 }
    private val intave14ThirdReduce by int("Intave14-ThirdReduce", 6, 1..10) { mode == "Intave14" && intave14TriggerTimes >= 3 }
    private val intave14OnlyWhenBackward by boolean("Intave14-OnlyWhenBackward", true) { mode == "Intave14" }
    private val intave14FinalReverse by boolean("Intave14-FinalReverse", false) { mode == "Intave14" }
    private val intave14FinalReverseFactor by float("Intave14-FinalReverseFactor", 1.0f, 0.0f..5.0f) { mode == "Intave14" && intave14FinalReverse }
    private val intave14Debug by boolean("Intave14-Debug", false) { mode == "Intave14" }

    // SmartJumpReset
    private val smartJumpResetEnabled by boolean("SmartJumpReset", true) { mode == "SmartJumpReset" }
    private val sneakReduce by boolean("SneakReduce", false) { mode == "SmartJumpReset" && smartJumpResetEnabled }
    private val backward by boolean("Backward", false) { mode == "SmartJumpReset" && smartJumpResetEnabled }

    // IntaveStrong
    private val intaveStrongFactor by float("IntaveStrong-Factor", 0.6f, 0.0f..1.0f) { mode == "IntaveStrong" }

    // AttackReduce
    private val attackReduceFactor by float("AttackReduce-Factor", 0.6f, -1.0f..1.0f) { mode == "AttackReduce" }
    private val attackReduceSprintFactor by float("AttackReduce-SprintFactor", 0.6f, -1.0f..1.0f) { mode == "AttackReduce" }
    private val attackReduceHurtTime by intRange("AttackReduce-HurtTime", 9..9, 1..10) { mode == "AttackReduce" }

    private val pauseOnExplosion by boolean("PauseOnExplosion", true)
    private val ticksToPause by int("TicksToPause", 20, 1..50) { pauseOnExplosion }

    // Direction Check
    private val checkDirection by boolean("CheckDirection", false)
    private val applyOnFront by boolean("ApplyOnFront", true) { checkDirection }
    private val applyOnSide by boolean("ApplyOnSide", true) { checkDirection }
    private val applyOnBack by boolean("ApplyOnBack", true) { checkDirection }

    // TODO: Could this be useful in other modes? (Jump?)
    // Limits
    private val limitMaxMotionValue = boolean("LimitMaxMotion", false) { mode == "Simple" }
    private val maxXZMotion by float("MaxXZMotion", 0.4f, 0f..1.9f) { limitMaxMotionValue.isActive() }
    private val maxYMotion by float("MaxYMotion", 0.36f, 0f..0.46f) { limitMaxMotionValue.isActive() }
    //0.00075 is added silently

    // Vanilla XZ limits
    // Non-KB: 0.4 (no sprint), 0.9 (sprint)
    // KB 1: 0.9 (no sprint), 1.4 (sprint)
    // KB 2: 1.4 (no sprint), 1.9 (sprint)
    // Vanilla Y limits
    // 0.36075 (no sprint), 0.46075 (sprint)

    private val clicks by intRange("Clicks", 3..5, 1..20) { mode == "Click" }
    private val hurtTimeToClick by int("HurtTimeToClick", 10, 0..10) { mode == "Click" }
    private val whenFacingEnemyOnly by boolean("WhenFacingEnemyOnly", true) { mode == "Click" }
    private val ignoreBlocking by boolean("IgnoreBlocking", false) { mode == "Click" }
    private val clickRange by float("ClickRange", 3f, 1f..6f) { mode == "Click" }
    private val swingMode by choices("SwingMode", arrayOf("Off", "Normal", "Packet"), "Normal") { mode == "Click" }

    // GrimCombat
    private val grimRange by float("Range", 3.5f, 0f..6f) { mode == "GrimCombat" }
    private val attackCountValue by int("AttackCounts", 12, 1..16) { mode == "GrimCombat" }
    private val fireCheckValue by boolean("FireCheck", false) { mode == "GrimCombat" }
    private val waterCheckValue by boolean("WaterCheck", false) { mode == "GrimCombat" }
    private val fallCheckValue by boolean("FallCheck", false) { mode == "GrimCombat" }
    private val consumeCheck by boolean("ConsumableCheck", false) { mode == "GrimCombat" }
    private val raycastValue by boolean("RayCast", false) { mode == "GrimCombat" }
    private val debugMessageValue by boolean("Debug", true) { mode == "GrimCombat" }

    // Prediction
    private val predictionClicks by intRange("PredictionClicks", 1..2, 1..20) { mode == "Prediction" }
    private val predictionJump by boolean("PredictionJump", true) { mode == "Prediction" }
    private val predictionDelay by boolean("PredictionDelay", false) { mode == "Prediction" }
    private val predictionDelayTicks by int("PredictionDelayTicks", 2, 0..10) { mode == "Prediction" && predictionDelay }
    private val predictionGroundDelay by boolean("PredictionGroundDelay", false) { mode == "Prediction" && predictionDelay }
    private val predictionAirBuffer by boolean("PredictionAirBuffer", false) { mode == "Prediction" && predictionDelay }
    private val predictionRotate by boolean("PredictionRotate", false) { mode == "Prediction" }
    private val predictionRotateTicks by int("PredictionRotateTicks", 2, 1..10) { mode == "Prediction" && predictionRotate }
    private val predictionAutoMove by boolean("PredictionAutoMove", false) { mode == "Prediction" && predictionRotate }
    private val predictionReduce by boolean("PredictionReduce", false) { mode == "Prediction" }
    private val predictionOnlySprinting by boolean("PredictionOnlySprinting", true) { mode == "Prediction" && predictionReduce }
    private val predictionReduceWhenCanAttack by boolean("PredictionReduceWhenCanAttack", false) { mode == "Prediction" && predictionReduce }
    private val predictionAttackTimes by int("PredictionAttackTimes", 1, 1..5) { mode == "Prediction" && predictionReduce }
    private val predictionRotationSettings = RotationSettings(this) { mode == "Prediction" && predictionRotate }.withoutKeepRotation()

    // Buffer Mode
    private val bufferDelay by int("BufferDelay", 3, 1..10) { mode == "Buffer" }

    // FDP Phase
    private val phaseHeight by float("PhaseHeight", 0.5F, 0F..1F) { mode == "Phase" }
    private val phaseOnlyGround by boolean("PhaseOnlyGround", true) { mode == "Phase" }
    private val phaseMode by choices("PhaseMode", arrayOf("Normal", "Packet"), "Normal") { mode == "Phase" }

    // FDP GrimC07
    private val grimC07Always by boolean("GrimC07Always", true) { mode == "GrimC07" }
    private val grimC07OnlyAir by boolean("GrimC07OnlyBreakAir", true) { mode == "GrimC07" }
    private val grimC07BreakOnWorld by boolean("GrimC07BreakOnWorld", false) { mode == "GrimC07" }
    private val grimC07SendC03 by boolean("GrimC07SendC03", false) { mode == "GrimC07" }
    private val grimC07SendC06 by boolean("GrimC07Send1.17C06", false) { mode == "GrimC07" && grimC07SendC03 }
    private val grimC07FlagPause by int("GrimC07FlagPause", 50, 0..5000) { mode == "GrimC07" }

    // FDP Spoof
    private val spoofModifyTimer by boolean("SpoofModifyTimer", true) { mode == "Spoof" }
    private val spoofTimer by float("SpoofTimer", 0.6F, 0.1F..1F) { mode == "Spoof" && spoofModifyTimer }

    // FDP Tick
    private val tickDelay by int("TickDelay", 1, 0..10) { mode == "Tick" }
    private val tickReduction by float("TickReductionAmount", 1F, 0F..1F) { mode == "Tick" }
    private val tickResetMotionY by boolean("TickResetMotionY", true) { mode == "Tick" }
    private val tickBypass by boolean("TickBypass", true) { mode == "Tick" }
    private val tickHorizontal by float("TickHorizontal", 0F, -1F..1F) { mode == "Tick" }
    private val tickVertical by float("TickVertical", 0F, -1F..1F) { mode == "Tick" }

    // FDP SideStrafe
    private val sideStrafeSetMotion by boolean("SideStrafeStrafe", false) { mode == "SideStrafe" }
    private val sideStrafeFace by boolean("SideStrafeFace", true) { mode == "SideStrafe" }
    private val sideStrafeRotationSettings = RotationSettings(this) { mode == "SideStrafe" && sideStrafeFace }.withoutKeepRotation()

    /**
     * VALUES
     */
    private val velocityTimer = MSTimer()
    private var hasReceivedVelocity = false

    // SmoothReverse
    private var reverseHurt = false

    // AACPush
    private var jump = false

    // Jump
    private var limitUntilJump = 0

    // IntaveReduce
    private var intaveTick = 0
    private var lastAttackTime = 0L
    private var intaveDamageTick = 0

    // Delay
    private val packets = LinkedHashMap<Packet<*>, Long>()

    // Grim
    private var timerTicks = 0

    // Vulcan
    private var transaction = false

    // Hypixel
    private var absorbedVelocity = false

    // MatrixNoXZ
    private var matrixNoXZAbsorbed = false

    // Pause On Explosion
    private var pauseTicks = 0

    // GrimCombat
    private var velocityInput = false
    private var attacked = false
    private var reduceXZ = 1.0
    private var velX = 0
    private var velY = 0
    private var velZ = 0

    // Intave13KeepLow
    private var wasOnGround = false

    // Polar
    private var polarHurtTime = kotlin.random.Random.nextInt(8, 10)

    // Intave14
    private var intave14OnGround = false
    private var intave14NotTriggered1 = true
    private var intave14NotTriggered2 = true
    private var intave14NotTriggered3 = true
    private var intave14NotTriggeredA = true
    private var intave14FinalReverseCondition = 0
    private var intave14FinalReverseTriggered = false

    // Prediction
    private var predictionRotateTickCounter = 0
    private var predictionKnockbackX = 0.0
    private var predictionKnockbackZ = 0.0
    private var predictionTargetRotation: Rotation? = null
    private var predictionDelayFlag = false
    private var predictionTicksSinceVelocity = -1
    private var predictionHandleReset = false
    private var predictionReduceTick = 0
    private var predictionAllowNext = true
    private var predictionPendingExplosion = false
    private var predictionIsFallDamage = false
    private var predictionDelayedPacket: Packet<*>? = null
    private var predictionDelayedTicks = 0

    // Buffer Mode
    private val bufferedPackets = mutableListOf<BufferedPacket>()

    // FDP modes
    private var aac520TemplateX = 0
    private var aac520TemplateY = 0
    private var aac520TemplateZ = 0
    private var grimTCancel = 0
    private var grimUpdates = 0
    private var grimC07GotVelocity = false
    private val grimC07FlagTimer = MSTimer()
    private var minemenTicks = 0
    private var minemenLastCancel = false
    private var minemenCanCancel = false
    private var tickVelocityTicks = 0
    private var sideStrafePos: BlockPos? = null

    override val tag
        get() = if (mode == "Simple" || mode == "Legit") {
            val horizontalPercentage = (horizontal * 100).toInt()
            val verticalPercentage = (vertical * 100).toInt()

            "$horizontalPercentage% $verticalPercentage%"
        } else mode

    override fun onDisable() {
        pauseTicks = 0
        mc.thePlayer?.speedInAir = 0.02F
        timerTicks = 0
        matrixNoXZAbsorbed = false
        mc.timer.timerSpeed = 1.0f
        aac520TemplateX = 0
        aac520TemplateY = 0
        aac520TemplateZ = 0
        grimTCancel = 0
        grimUpdates = 0
        grimC07GotVelocity = false
        grimC07FlagTimer.reset()
        minemenTicks = 0
        minemenLastCancel = false
        minemenCanCancel = false
        tickVelocityTicks = 0
        sideStrafePos = null
        if (mc.currentScreen == null) {
            mc.gameSettings.keyBindForward.pressed = net.minecraft.client.settings.GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
            mc.gameSettings.keyBindBack.pressed = net.minecraft.client.settings.GameSettings.isKeyDown(mc.gameSettings.keyBindBack)
            mc.gameSettings.keyBindLeft.pressed = net.minecraft.client.settings.GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
            mc.gameSettings.keyBindRight.pressed = net.minecraft.client.settings.GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
            mc.gameSettings.keyBindJump.pressed = net.minecraft.client.settings.GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
            mc.gameSettings.keyBindSneak.pressed = net.minecraft.client.settings.GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
        }
        bufferedPackets.clear()
        reset()
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (thePlayer.isInLiquid || thePlayer.isInWeb || thePlayer.isDead)
            return@handler

        when (mode.lowercase()) {
            "vulcan2.0.1" -> {
                if (thePlayer.hurtTime != 0) speed = 0.2f
            }

            "matrix6.6.1" -> {
                if (thePlayer.hurtTime > 2) speed = 0.14f
            }

            "intave13gommezero" -> {
                if (thePlayer.hurtTime != 0) {
                    mc.gameSettings.keyBindForward.pressed = false
                    mc.gameSettings.keyBindBack.pressed = false
                    mc.gameSettings.keyBindLeft.pressed = false
                    mc.gameSettings.keyBindRight.pressed = false
                }
            }

            "intave13keeplow" -> {
                when (thePlayer.hurtTime) {
                    10 -> if (thePlayer.onGround) wasOnGround = true
                    9 -> if (wasOnGround) thePlayer.motionY = 0.0
                    0 -> wasOnGround = false
                }
            }

            "intave13wall" -> {
                val wallVelocity = Random.nextDouble(0.3045, 0.3345).toFloat()
                if (thePlayer.isCollidedHorizontally && !thePlayer.onGround && !thePlayer.isCollidedVertically
                    && !thePlayer.isInWeb && !thePlayer.isInWater && !thePlayer.isInLava && thePlayer.hurtTime != 0
                ) {
                    speed = wallVelocity
                }
            }

            "intave13old" -> {
                if (thePlayer.hurtTime == 6) speed = 0.17f
            }

            "intave13reverse" -> {
                if (thePlayer.hurtTime > 0) {
                    thePlayer.setSprinting(false)
                    speed = 0.05f
                }
            }

            "aac3.3.12" -> {
                if (thePlayer.hurtTime > 0) {
                    thePlayer.motionX *= 0.6
                    thePlayer.motionZ *= 0.6
                }
            }

            "aac3.3.14" -> {
                if (thePlayer.hurtTime > 0 && !thePlayer.onGround) {
                    thePlayer.motionX *= 0.6
                    thePlayer.motionZ *= 0.6
                }
            }

            "aac4reduce" -> {
                if (thePlayer.hurtTime > 0 && !thePlayer.onGround && velocityInput && velocityTimer.hasTimePassed(80L)) {
                    thePlayer.motionX *= 0.62
                    thePlayer.motionZ *= 0.62
                }

                if (velocityInput && (thePlayer.hurtTime < 4 || thePlayer.onGround) && velocityTimer.hasTimePassed(120L)) {
                    velocityInput = false
                }
            }

            "aac5reduce" -> {
                if (thePlayer.hurtTime > 1 && velocityInput) {
                    thePlayer.motionX *= 0.81
                    thePlayer.motionZ *= 0.81
                }

                if (velocityInput && (thePlayer.hurtTime < 5 || thePlayer.onGround) && velocityTimer.hasTimePassed(120L)) {
                    velocityInput = false
                }
            }

            "aac5.2.0combat" -> {
                if (thePlayer.hurtTime > 0 && velocityInput) {
                    velocityInput = false
                    thePlayer.motionX = 0.0
                    thePlayer.motionY = 0.0
                    thePlayer.motionZ = 0.0
                    thePlayer.jumpMovementFactor = -0.002f
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, Double.MAX_VALUE, thePlayer.posZ, true))
                }

                if (velocityTimer.hasTimePassed(80L) && velocityInput) {
                    velocityInput = false
                    thePlayer.motionX = aac520TemplateX / 8000.0
                    thePlayer.motionY = aac520TemplateY / 8000.0
                    thePlayer.motionZ = aac520TemplateZ / 8000.0
                    thePlayer.jumpMovementFactor = -0.002f
                }
            }

            "intave14.3.3" -> applyXZReductionByHurtTime()

            "smartjumpreset" -> handleSmartJumpReset(thePlayer)

            "grim" -> {
                grimUpdates++
                if (grimUpdates >= 8) {
                    grimUpdates = 0
                    if (grimTCancel > 0) grimTCancel--
                }
            }

            "grimdamage" -> {
                if (thePlayer.hurtTime == 9) {
                    val target = getNearestEntityInRange(3f) ?: return@handler
                    repeat(12) {
                        sendPackets(
                            C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK),
                            C0APacketAnimation()
                        )
                    }
                    thePlayer.motionX *= 0.07776
                    thePlayer.motionZ *= 0.07776
                }
            }

            "hypixelboost" -> {
                if (thePlayer.hurtTime == 8) {
                    MovementUtils.strafe(speed * 0.7f)
                }
            }

            "minemen" -> handleMinemenUpdate(thePlayer)

            "tick" -> handleTickVelocityUpdate(thePlayer)


            "grimcombat" -> {
                if (attacked) {
                    if (thePlayer.hurtTime > 0 && thePlayer.onGround) {
                        thePlayer.addVelocity(-1.3E-10, -1.3E-10, -1.3E-10)
                        thePlayer.isSprinting = false
                    }
                    if (thePlayer.hurtTime == 0) {
                        velocityInput = false
                        attacked = false
                    }
                }
            }
        }
    }

    val onClick = handler<GameTickEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (mode == "Prediction") {
            handlePredictionDelayedPacket(thePlayer)
            handlePredictionReduce(thePlayer)
            return@handler
        }

        if (mode != "Click" || thePlayer.hurtTime != hurtTimeToClick || ignoreBlocking && (thePlayer.isBlocking || KillAura.blockStatus))
            return@handler

        var entity = mc.objectMouseOver?.entityHit

        if (entity == null) {
            if (whenFacingEnemyOnly) {
                var result: Entity? = null

                runWithModifiedRaycastResult(
                    currentRotation ?: thePlayer.rotation,
                    clickRange.toDouble(),
                    0.0
                ) {
                    result = it.entityHit?.takeIf { isSelected(it, true) }
                }

                entity = result
            } else getNearestEntityInRange(clickRange)?.takeIf { isSelected(it, true) }
        }

        entity ?: return@handler

        val swingHand = {
            when (swingMode.lowercase()) {
                "normal" -> thePlayer.swingItem()
                "packet" -> sendPacket(C0APacketAnimation())
            }
        }

        repeat(clicks.random()) {
            thePlayer.attackEntityWithModifiedSprint(entity, true) { swingHand() }
        }
    }

    val onAttack = handler<AttackEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        when (mode.lowercase()) {
            "intavereduce" -> {
                if (!hasReceivedVelocity) return@handler
                if (player.hurtTime == hurtTime && System.currentTimeMillis() - lastAttackTime <= 8000) {
                    player.motionX *= reduceFactor
                    player.motionZ *= reduceFactor
                }
                lastAttackTime = System.currentTimeMillis()
            }

            "attackreduce" -> {
                if (mc.thePlayer.hurtTime in attackReduceHurtTime) {
                    if (mc.thePlayer.isSprinting) {
                        mc.thePlayer.motionX *= attackReduceSprintFactor
                        mc.thePlayer.motionZ *= attackReduceSprintFactor
                    } else {
                        mc.thePlayer.motionX *= attackReduceFactor
                        mc.thePlayer.motionZ *= attackReduceFactor
                    }
                }
            }

            "intavestrong" -> {
                if (mc.thePlayer.hurtTime > 0) {
                    mc.thePlayer.motionX *= intaveStrongFactor
                    mc.thePlayer.motionZ *= intaveStrongFactor
                }
            }

            "intave14" -> {
                if (intave14OnlyWhenBackward && !isMovingBackwards()) return@handler
                if (!hasReceivedVelocity) return@handler
                when (mc.thePlayer.hurtTime) {
                    intave14FirstReduce -> if (intave14NotTriggered1 && intave14TriggerTimes >= 1) {
                        mc.thePlayer.motionX *= 0.6
                        mc.thePlayer.motionZ *= 0.6
                        intave14FinalReverseCondition++
                        intave14NotTriggered1 = false
                        intave14NotTriggeredA = false
                        if (intave14Debug) chat("Intave14 Reduce Phase1")
                    }

                    intave14SecondReduce -> if (intave14NotTriggered2 && intave14TriggerTimes >= 2) {
                        if (intave14NotTriggeredA) {
                            mc.thePlayer.motionX *= 0.6
                            mc.thePlayer.motionZ *= 0.6
                            intave14NotTriggeredA = false
                        } else {
                            mc.thePlayer.motionX *= 0.35
                            mc.thePlayer.motionZ *= 0.35
                        }
                        intave14FinalReverseCondition++
                        intave14NotTriggered2 = false
                        if (intave14Debug) chat("Intave14 Reduce Phase2")
                    }

                    intave14ThirdReduce -> if (intave14NotTriggered3 && intave14TriggerTimes >= 3) {
                        if (intave14NotTriggeredA) {
                            mc.thePlayer.motionX *= 0.6
                            mc.thePlayer.motionZ *= 0.6
                            intave14NotTriggeredA = false
                        } else {
                            mc.thePlayer.motionX *= 0.35
                            mc.thePlayer.motionZ *= 0.35
                        }
                        intave14FinalReverseCondition++
                        intave14NotTriggered3 = false
                        if (intave14Debug) chat("Intave14 Reduce Phase3")
                    }
                }

                if (intave14FinalReverse && mc.thePlayer.hurtTime == 1 && !intave14FinalReverseTriggered) {
                    if (intave14FinalReverseCondition >= intave14TriggerTimes) {
                        mc.thePlayer.motionX *= -intave14FinalReverseFactor
                        mc.thePlayer.motionZ *= -intave14FinalReverseFactor
                        intave14FinalReverseTriggered = true
                        if (intave14Debug) chat("Intave14 FinalReverse")
                    }
                }
            }
        }
    }

    private fun isMovingBackwards(): Boolean {
        val player = mc.thePlayer ?: return false
        val motionX = player.motionX
        val motionZ = player.motionZ

        if (sqrt(motionX * motionX + motionZ * motionZ) < 0.1) return true

        val moveAngle = Math.toDegrees(atan2(motionX, motionZ)).toFloat().normalizeAngle()
        val lookAngle = player.rotationYaw.normalizeAngle()
        val angleDiff = minOf(
            abs(moveAngle - lookAngle),
            360 - abs(moveAngle - lookAngle)
        )
        return angleDiff >= 60
    }

    private fun Float.normalizeAngle(): Float {
        return ((this % 360) + 360) % 360
    }

    private fun checkAir(blockPos: BlockPos): Boolean {
        val world = mc.theWorld ?: return false

        if (!world.isAirBlock(blockPos)) {
            return false
        }

        timerTicks = 20

        sendPackets(
            C03PacketPlayer(true),
            C07PacketPlayerDigging(STOP_DESTROY_BLOCK, blockPos, DOWN)
        )

        world.setBlockToAir(blockPos)

        return true
    }

    // TODO: Recode
    private fun getDirection(): Double {
        var moveYaw = mc.thePlayer.rotationYaw
        when {
            mc.thePlayer.moveForward != 0f && mc.thePlayer.moveStrafing == 0f -> {
                moveYaw += if (mc.thePlayer.moveForward > 0) 0 else 180
            }

            mc.thePlayer.moveForward != 0f && mc.thePlayer.moveStrafing != 0f -> {
                if (mc.thePlayer.moveForward > 0) moveYaw += if (mc.thePlayer.moveStrafing > 0) -45 else 45 else moveYaw -= if (mc.thePlayer.moveStrafing > 0) -45 else 45
                moveYaw += if (mc.thePlayer.moveForward > 0) 0 else 180
            }

            mc.thePlayer.moveStrafing != 0f && mc.thePlayer.moveForward == 0f -> {
                moveYaw += if (mc.thePlayer.moveStrafing > 0) -90 else 90
            }
        }
        return Math.floorMod(moveYaw.toInt(), 360).toDouble()
    }

    private fun shouldApplyVelocity(packetDirection: Double): Boolean {
        if (!checkDirection) return true

        val playerDirection = getDirection()
        val packetDegree = Math.floorMod(packetDirection.toDegrees().toInt(), 360).toDouble()

        val relativeAngle = Math.floorMod((packetDegree - playerDirection).toInt(), 360).toDouble()

        // 鍑婚€€鏂瑰悜鎸囧悜鍚庢柟 = 鍑婚€€鏉ヨ嚜鍓嶆柟
        val isFront = relativeAngle in 135.0..225.0
        // 鍑婚€€鏂瑰悜鎸囧悜鍓嶆柟 = 鍑婚€€鏉ヨ嚜鍚庢柟
        val isBack = relativeAngle in 315.0..360.0 || relativeAngle in 0.0..45.0
        // 鍑婚€€鏂瑰悜鎸囧悜渚ф柟 = 鍑婚€€鏉ヨ嚜渚ф柟
        val isSide = relativeAngle in 45.0..135.0 || relativeAngle in 225.0..315.0

        return (isFront && applyOnFront) || (isSide && applyOnSide) || (isBack && applyOnBack)
    }

    private fun getPacketDirection(packet: Packet<*>): Double? {
        return when (packet) {
            is S12PacketEntityVelocity -> {
                if (packet.entityID != mc.thePlayer?.entityId) return null
                val motionX = packet.motionX.toDouble()
                val motionZ = packet.motionZ.toDouble()
                if (motionX == 0.0 && motionZ == 0.0) return null
                atan2(motionX, motionZ)
            }
            is S27PacketExplosion -> {
                val motionX = mc.thePlayer?.motionX?.plus(packet.field_149152_f) ?: return null
                val motionZ = mc.thePlayer?.motionZ?.plus(packet.field_149159_h) ?: return null
                if (motionX == 0.0 && motionZ == 0.0) return null
                atan2(motionX, motionZ)
            }
            else -> null
        }
    }

    val onPacket = handler<PacketEvent>(priority = 1) { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        val packet = event.packet

        if (!handleEvents())
            return@handler

        if (pauseTicks > 0) {
            pauseTicks--
            return@handler
        }

        if (event.isCancelled)
            return@handler

        if (mode == "GrimC07" && packet is S08PacketPlayerPosLook) {
            grimC07FlagTimer.reset()
            grimC07GotVelocity = false
            return@handler
        }

        val fdpRawVelocityMode = mode in arrayOf(
            "AAC4Reduce", "AAC5Reduce", "AAC5.2.0", "AAC5.2.0Combat",
            "Grim", "Grim1.17", "GrimC07", "MatrixReverse", "MatrixSimple",
            "Minemen", "Phase", "SideStrafe", "Spoof", "Tick"
        )

        if ((packet is S12PacketEntityVelocity && thePlayer.entityId == packet.entityID &&
                (fdpRawVelocityMode || packet.motionY > 0 && (packet.motionX != 0 || packet.motionZ != 0)))
            || (packet is S27PacketExplosion && (thePlayer.motionY + packet.field_149153_g) > 0.0
                    && ((thePlayer.motionX + packet.field_149152_f) != 0.0 || (thePlayer.motionZ + packet.field_149159_h) != 0.0)
                || packet is S27PacketExplosion && mode == "GrimC07")
        ) {
            velocityTimer.reset()

            if (pauseOnExplosion && packet is S27PacketExplosion && (thePlayer.motionY + packet.field_149153_g) > 0.0
                && ((thePlayer.motionX + packet.field_149152_f) != 0.0 || (thePlayer.motionZ + packet.field_149159_h) != 0.0)
            ) {
                pauseTicks = ticksToPause
            }

            val packetDirection = getPacketDirection(packet)
            if (packetDirection != null && !shouldApplyVelocity(packetDirection)) {
                return@handler
            }

            when (mode.lowercase()) {
                "simple" -> handleVelocity(event)

                "aac", "reverse", "smoothreverse", "aaczero", "ghostblock", "intavereduce" -> hasReceivedVelocity = true

                "jump" -> {
                    hasReceivedVelocity = true
                }

                "glitch" -> {
                    if (!thePlayer.onGround)
                        return@handler

                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "matrixreduce" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        val factor = matrixReduceFactor.toDouble()
                        packet.motionX = (packet.getMotionX() * factor).toInt()
                        packet.motionZ = (packet.getMotionZ() * factor).toInt()

                        if (thePlayer.onGround) {
                            packet.motionX = (packet.getMotionX() * 0.86).toInt()
                            packet.motionZ = (packet.getMotionZ() * 0.86).toInt()
                        }

                        if (matrixReduceDebug) {
                            chat("搂7[MatrixReduce] 搂fApplied factor: 搂a${(factor * 100).toInt()}%")
                        }
                    }
                }

                "aac4reduce" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        velocityInput = true
                        packet.motionX = (packet.motionX * 0.6).toInt()
                        packet.motionZ = (packet.motionZ * 0.6).toInt()
                    }
                }

                "aac5reduce" -> velocityInput = true

                "aac5.2.0" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        event.cancelEvent()
                        sendPacket(C03PacketPlayer.C04PacketPlayerPosition(thePlayer.posX, Double.MAX_VALUE, thePlayer.posZ, true))
                    }
                }

                "aac5.2.0combat" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        event.cancelEvent()
                        velocityInput = true
                        aac520TemplateX = packet.motionX
                        aac520TemplateY = packet.motionY
                        aac520TemplateZ = packet.motionZ
                    }
                }

                "grim" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        event.cancelEvent()
                        grimTCancel = 6
                    }
                }

                "grim1.17" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        repeat(4) {
                            sendPacket(
                                C03PacketPlayer.C06PacketPlayerPosLook(
                                    thePlayer.posX,
                                    thePlayer.posY,
                                    thePlayer.posZ,
                                    thePlayer.rotationYaw,
                                    thePlayer.rotationPitch,
                                    thePlayer.onGround
                                )
                            )
                        }
                        sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, thePlayer.position, DOWN))
                        event.cancelEvent()
                    }
                }

                "grimc07" -> handleGrimC07Packet(event, packet, thePlayer)

                "matrixreverse" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        thePlayer.motionX = packet.motionX / 8000.0
                        thePlayer.motionZ = packet.motionZ / 8000.0
                        MovementUtils.strafe()
                    }
                }

                "matrixsimple" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        packet.motionX = (packet.motionX * 0.36).toInt()
                        packet.motionZ = (packet.motionZ * 0.36).toInt()
                        if (thePlayer.onGround) {
                            packet.motionX = (packet.motionX * 0.9).toInt()
                            packet.motionZ = (packet.motionZ * 0.9).toInt()
                        }
                    }
                }

                "minemen" -> handleMinemenPacket(event, packet, thePlayer)

                "phase" -> handlePhasePacket(event, packet, thePlayer)

                "sidestrafe" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        sideStrafePos = BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)
                    }
                }

                "spoof" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        event.cancelEvent()
                        sendPacket(
                            C03PacketPlayer.C04PacketPlayerPosition(
                                thePlayer.posX + packet.motionX / 8000.0,
                                thePlayer.posY + packet.motionY / 8000.0,
                                thePlayer.posZ + packet.motionZ / 8000.0,
                                false
                            )
                        )
                        if (spoofModifyTimer) {
                            mc.timer.timerSpeed = spoofTimer
                        }
                    }
                }

                "tick" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        velocityInput = true
                        tickVelocityTicks = 0

                        if (tickHorizontal == 0F && tickVertical == 0F) {
                            event.cancelEvent()
                        }

                        packet.motionX = (packet.motionX * tickHorizontal).toInt()
                        packet.motionY = (packet.motionY * tickVertical).toInt()
                        packet.motionZ = (packet.motionZ * tickHorizontal).toInt()
                    }
                }

                // Credit: @LiquidSquid / Ported from NextGen
                "blocksmc" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        hasReceivedVelocity = true
                        event.cancelEvent()

                        sendPacket(C0BPacketEntityAction(thePlayer, START_SNEAKING))
                        sendPacket(C0BPacketEntityAction(thePlayer, STOP_SNEAKING))
                    }
                }

                "grimc03" -> {
                    // Checks to prevent from getting flagged (BadPacketsE)
                    if (thePlayer.isMoving) {
                        hasReceivedVelocity = true
                        event.cancelEvent()
                    }
                }

                "hypixel" -> {
                    hasReceivedVelocity = true
                    if (!thePlayer.onGround) {
                        if (!absorbedVelocity) {
                            event.cancelEvent()
                            absorbedVelocity = true
                            return@handler
                        }
                    }

                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        packet.motionX = (thePlayer.motionX * 8000).toInt()
                        packet.motionZ = (thePlayer.motionZ * 8000).toInt()
                    }
                }

                "hypixelair" -> {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "matrixnoxz" -> {
                    hasReceivedVelocity = true
                    if (!thePlayer.onGround) {
                        if (!matrixNoXZAbsorbed) {
                            event.cancelEvent()
                            matrixNoXZAbsorbed = true
                            return@handler
                        }
                    }

                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        packet.motionX = 0
                        packet.motionZ = 0
                    }
                }

                "intave/polar-flag" -> {
                    if (packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId) {
                        hasReceivedVelocity = true
                        packet.motionX = (packet.motionX * 0.8).toInt()
                        packet.motionZ = (packet.motionZ * 0.8).toInt()
                    }
                }

                "grimcombat" -> handleGrimCombatPacket(event, packet, thePlayer)

                "vulcan" -> {
                    event.cancelEvent()
                }

                "s32packet" -> {
                    hasReceivedVelocity = true
                    event.cancelEvent()
                }

                "prediction" -> handlePredictionVelocityPacket(event, packet, thePlayer)

                "polar" -> {
                    hasReceivedVelocity = true
                }

                "intave14" -> {
                    intave14FinalReverseTriggered = false
                    hasReceivedVelocity = true
                    intave14NotTriggered1 = true
                    intave14NotTriggered2 = true
                    intave14NotTriggered3 = true
                    intave14NotTriggeredA = true
                    intave14FinalReverseCondition = 0
                }

                "intave14.3.3" -> {
                    hasReceivedVelocity = true
                }

                "buffer" -> {
                    event.cancelEvent()
                    bufferedPackets.add(BufferedPacket(packet, bufferDelay))
                }
            }
        }

        if (mode == "BlocksMC" && hasReceivedVelocity) {
            if (packet is C0BPacketEntityAction) {
                hasReceivedVelocity = false
                event.cancelEvent()
            }
        }

        if (mode == "Vulcan") {
            if (Disabler.handleEvents() && Disabler.verusCombat && (!Disabler.onlyCombat || Disabler.isOnCombat)) return@handler

            if (packet is S32PacketConfirmTransaction) {
                event.cancelEvent()
                sendPacket(
                    C0FPacketConfirmTransaction(
                        if (transaction) 1 else -1,
                        if (transaction) -1 else 1,
                        transaction
                    ), false
                )
                transaction = !transaction
            }
        }

        if (mode == "Grim" && packet is S32PacketConfirmTransaction && grimTCancel > 0) {
            event.cancelEvent()
            grimTCancel--
        }

        if (mode == "S32Packet" && packet is S32PacketConfirmTransaction) {

            if (!hasReceivedVelocity)
                return@handler

            event.cancelEvent()
            hasReceivedVelocity = false
        }
    }

    /**
     * Tick Event (Abuse Timer Balance)
     */
    val onTick = handler<GameTickEvent> {
        val player = mc.thePlayer ?: return@handler

        if (mode == "GrimC07") {
            handleGrimC07Tick(player)
            return@handler
        }

        if (mode != "GrimC03")
            return@handler

        // Timer Abuse (https://github.com/CCBlueX/LiquidBounce/issues/2519)
        if (timerTicks > 0 && mc.timer.timerSpeed <= 1) {
            val timerSpeed = 0.8f + (0.2f * (20 - timerTicks) / 20)
            mc.timer.timerSpeed = timerSpeed.coerceAtMost(1f)
            --timerTicks
        } else if (mc.timer.timerSpeed <= 1) {
            mc.timer.timerSpeed = 1f
        }

        if (hasReceivedVelocity) {
            val pos = BlockPos(player.posX, player.posY, player.posZ)

            if (checkAir(pos))
                hasReceivedVelocity = false
        }
    }

    /**
     * Delay Mode
     */
    val onDelayPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (event.isCancelled)
            return@handler

        if (mode == "Delay") {
            if (packet is S32PacketConfirmTransaction || packet is S12PacketEntityVelocity) {

                event.cancelEvent()

                // Delaying packet like PingSpoof
                synchronized(packets) {
                    packets[packet] = System.currentTimeMillis()
                }
            }
            delayMode = true
        } else {
            delayMode = false
        }
    }

    /**
     * Reset on world change
     */
    val onWorld = handler<WorldEvent> {
        packets.clear()
        bufferedPackets.clear()
        velocityInput = false
        grimTCancel = 0
        grimC07GotVelocity = false
        minemenTicks = 0
        minemenLastCancel = false
        minemenCanCancel = false
        tickVelocityTicks = 0
        sideStrafePos = null
    }

    val onGameLoop = handler<GameLoopEvent> {
        if (mode == "Delay")
            sendPacketsByOrder(false)
    }

    private fun sendPacketsByOrder(velocity: Boolean) {
        synchronized(packets) {
            packets.entries.removeAll { (packet, timestamp) ->
                if (velocity || timestamp <= System.currentTimeMillis() - spoofDelay) {
                    PacketUtils.schedulePacketProcess(packet)
                    true
                } else false
            }
        }
    }

    private fun reset() {
        sendPacketsByOrder(true)

        packets.clear()
        bufferedPackets.clear()

        velocityInput = false
        attacked = false
        grimTCancel = 0
        grimC07GotVelocity = false
        minemenTicks = 0
        minemenLastCancel = false
        minemenCanCancel = false
        tickVelocityTicks = 0
        sideStrafePos = null
    }

    val onJump = handler<JumpEvent> { event ->
        val thePlayer = mc.thePlayer

        if (thePlayer == null || thePlayer.isInLiquid || thePlayer.isInWeb)
            return@handler

        when (mode.lowercase()) {
            "aacpush" -> {
                jump = true

                if (!thePlayer.isCollidedVertically)
                    event.cancelEvent()
            }

            "aaczero" ->
                if (thePlayer.hurtTime > 0)
                    event.cancelEvent()
        }
    }

    val onMotion = handler<MotionEvent> {
        val player = mc.thePlayer ?: return@handler

        when (mode) {
            "Intave14.3.3" -> {
                if (player.hurtTime == 10) {
                    player.motionX *= -1.0
                    player.motionZ *= -1.0
                } else if (player.hurtTime == 9 && player.onGround) {
                    player.motionX *= 0.9
                    player.motionZ *= 0.9
                }
            }
        }
    }

    val onStrafe = handler<StrafeEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (mode == "Jump" && hasReceivedVelocity) {
            if (!player.isJumping && nextInt(endExclusive = 100) < chance && shouldJump() && player.isSprinting && player.onGround && player.hurtTime == 9) {
                player.tryJump()
                limitUntilJump = 0
            }
            hasReceivedVelocity = false
            return@handler
        }

        if (mode == "Prediction" && predictionRotate && predictionRotateTickCounter > 0) {
            predictionTargetRotation?.let { rotation ->
                event.cancelEvent()

                if (predictionAutoMove) {
                    val yawRad = rotation.yaw / 180.0 * PI
                    player.motionX += -sin(yawRad) * event.friction
                    player.motionZ += cos(yawRad) * event.friction
                } else {
                    rotation.applyStrafeToPlayer(event)
                }
            }
            return@handler
        }

        if (mode == "SideStrafe") {
            handleSideStrafe(event, player)
            return@handler
        }

        when (jumpCooldownMode.lowercase()) {
            "ticks" -> limitUntilJump++
            "receivedhits" -> if (player.hurtTime == 9) limitUntilJump++
        }
    }

    val onBlockBB = handler<BlockBBEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (mode == "GhostBlock") {
            if (hasReceivedVelocity) {
                if (player.hurtTime in hurtTimeRange) {
                    // Check if there is air exactly 1 level above the player's Y position
                    if (event.block is BlockAir && event.y == mc.thePlayer.posY.toInt() + 1) {
                        event.boundingBox = AxisAlignedBB(
                            event.x.toDouble(),
                            event.y.toDouble(),
                            event.z.toDouble(),
                            event.x + 1.0,
                            event.y + 1.0,
                            event.z + 1.0
                        )
                    }
                } else if (player.hurtTime == 0) {
                    hasReceivedVelocity = false
                }
            }
        }
    }

    private fun shouldJump() = when (jumpCooldownMode.lowercase()) {
        "ticks" -> limitUntilJump >= ticksUntilJump
        "receivedhits" -> limitUntilJump >= hitsUntilJump
        else -> false
    }

    private fun resetPrediction() {
        predictionRotateTickCounter = 0
        predictionKnockbackX = 0.0
        predictionKnockbackZ = 0.0
        predictionTargetRotation = null
        predictionDelayFlag = false
        predictionTicksSinceVelocity = -1
        predictionHandleReset = false
        predictionReduceTick = 0
        predictionAllowNext = true
        predictionPendingExplosion = false
        predictionIsFallDamage = false
        predictionDelayedPacket = null
        predictionDelayedTicks = 0
    }

    private fun handlePredictionTick(player: EntityPlayerSP) {
        if (predictionTicksSinceVelocity >= 0) {
            if (predictionTicksSinceVelocity < 10) {
                predictionTicksSinceVelocity++
            } else {
                predictionTicksSinceVelocity = -1
            }
        }

        if (predictionRotateTickCounter > 0) {
            predictionTargetRotation?.let { setTargetRotation(it, predictionRotationSettings, predictionRotateTicks) }
            predictionRotateTickCounter--
        } else {
            predictionTargetRotation = null
        }

        if (predictionJump && predictionHandleReset) {
            if (!player.isJumping && player.isSprinting && player.onGround && player.hurtTime >= 8) {
                player.tryJump()
                limitUntilJump = 0
                predictionHandleReset = false
            } else if (player.hurtTime == 0 && predictionTicksSinceVelocity > 2) {
                predictionHandleReset = false
            }
        }
    }

    private fun handlePredictionDelayedPacket(player: EntityPlayerSP) {
        val packet = predictionDelayedPacket ?: return

        predictionDelayedTicks++

        val shouldRelease = if (predictionAirBuffer) {
            player.onGround && predictionDelayedTicks > 0
        } else {
            predictionDelayedTicks >= predictionDelayTicks && (!predictionGroundDelay || player.onGround)
        }

        if (shouldRelease) {
            applyPredictionPacket(packet, player)
            predictionDelayedPacket = null
            predictionDelayedTicks = 0
            predictionDelayFlag = false
        }
    }

    private fun handlePredictionReduce(player: EntityPlayerSP) {
        if (!predictionReduce || predictionReduceTick <= 0 || player.hurtTime <= 0 || player.isBlocking || KillAura.blockStatus)
            return

        if (predictionOnlySprinting && !player.isSprinting)
            return

        val target = findPredictionTarget() ?: return

        if (predictionReduceWhenCanAttack && player.hurtResistantTime <= 0)
            return

        repeat(predictionAttackTimes) {
            player.attackEntityWithModifiedSprint(target, true) { player.swingItem() }
        }

        player.motionX *= 0.6
        player.motionZ *= 0.6
        player.isSprinting = false
        predictionReduceTick--
    }

    private fun findPredictionTarget(): Entity? {
        mc.objectMouseOver?.entityHit?.takeIf { isSelected(it, true) }?.let { return it }

        var target: Entity? = KillAura.target?.takeIf { isSelected(it, true) }

        if (target == null) {
            runWithModifiedRaycastResult(
                currentRotation ?: mc.thePlayer.rotation,
                3.0,
                0.0
            ) {
                target = it.entityHit?.takeIf { entity -> isSelected(entity, true) }
            }
        }

        return target
    }

    private fun handlePredictionVelocityPacket(event: PacketEvent, packet: Packet<*>, player: EntityPlayerSP) {
        hasReceivedVelocity = true
        predictionTicksSinceVelocity = 0
        predictionHandleReset = true
        predictionAllowNext = false
        predictionPendingExplosion = packet is S27PacketExplosion
        predictionIsFallDamage = player.fallDistance > 0F
        predictionReduceTick = if (predictionReduce) predictionAttackTimes else 0
        setupPredictionRotation(packet, player)

        if (predictionDelay) {
            event.cancelEvent()
            predictionDelayedPacket = packet
            predictionDelayedTicks = 0
            predictionDelayFlag = true
            return
        }
    }

    private fun setupPredictionRotation(packet: Packet<*>, player: EntityPlayerSP) {
        if (!predictionRotate)
            return

        val motion = when (packet) {
            is S12PacketEntityVelocity -> {
                if (packet.entityID != player.entityId) return
                packet.motionX / 8000.0 to packet.motionZ / 8000.0
            }
            is S27PacketExplosion -> packet.field_149152_f.toDouble() to packet.field_149159_h.toDouble()
            else -> return
        }

        predictionKnockbackX = motion.first
        predictionKnockbackZ = motion.second

        if (predictionKnockbackX == 0.0 && predictionKnockbackZ == 0.0)
            return

        val yaw = (atan2(predictionKnockbackZ, predictionKnockbackX) * 180.0 / PI - 90.0).toFloat()
        predictionTargetRotation = Rotation(yaw, player.rotationPitch)
        predictionRotateTickCounter = predictionRotateTicks
        setTargetRotation(predictionTargetRotation!!, predictionRotationSettings, predictionRotateTicks)
    }

    private fun applyPredictionPacket(packet: Packet<*>, player: EntityPlayerSP) {
        when (packet) {
            is S12PacketEntityVelocity -> {
                if (packet.entityID != player.entityId) return
                player.motionX = packet.motionX / 8000.0
                player.motionY = packet.motionY / 8000.0
                player.motionZ = packet.motionZ / 8000.0
            }
            is S27PacketExplosion -> {
                player.motionX += packet.field_149152_f.toDouble()
                player.motionY += packet.field_149153_g.toDouble()
                player.motionZ += packet.field_149159_h.toDouble()
            }
        }
    }

    private fun handleVelocity(event: PacketEvent) {
        val packet = event.packet

        if (packet is S12PacketEntityVelocity) {
            // Always cancel event and handle motion from here
            event.cancelEvent()

            if (horizontal == 0f && vertical == 0f)
                return

            // Don't modify player's motionXZ when horizontal value is 0
            if (horizontal != 0f) {
                var motionX = packet.realMotionX
                var motionZ = packet.realMotionZ

                if (limitMaxMotionValue.get()) {
                    val distXZ = sqrt(motionX * motionX + motionZ * motionZ)

                    if (distXZ > maxXZMotion) {
                        val ratioXZ = maxXZMotion / distXZ

                        motionX *= ratioXZ
                        motionZ *= ratioXZ
                    }
                }

                mc.thePlayer.motionX = motionX * horizontal
                mc.thePlayer.motionZ = motionZ * horizontal
            }

            // Don't modify player's motionY when vertical value is 0
            if (vertical != 0f) {
                var motionY = packet.realMotionY

                if (limitMaxMotionValue.get())
                    motionY = motionY.coerceAtMost(maxYMotion + 0.00075)

                mc.thePlayer.motionY = motionY * vertical
            }
        } else if (packet is S27PacketExplosion) {
            // Don't cancel explosions, modify them, they could change blocks in the world
            if (horizontal != 0f && vertical != 0f) {
                packet.field_149152_f = 0f
                packet.field_149153_g = 0f
                packet.field_149159_h = 0f

                return
            }

            // Unlike with S12PacketEntityVelocity explosion packet motions get added to player motion, doesn't replace it
            // Velocity might behave a bit differently, especially LimitMaxMotion
            packet.field_149152_f *= horizontal // motionX
            packet.field_149153_g *= vertical // motionY
            packet.field_149159_h *= horizontal // motionZ

            if (limitMaxMotionValue.get()) {
                val distXZ =
                    sqrt(packet.field_149152_f * packet.field_149152_f + packet.field_149159_h * packet.field_149159_h)
                val distY = packet.field_149153_g
                val maxYMotion = maxYMotion + 0.00075f

                if (distXZ > maxXZMotion) {
                    val ratioXZ = maxXZMotion / distXZ

                    packet.field_149152_f *= ratioXZ
                    packet.field_149159_h *= ratioXZ
                }

                if (distY > maxYMotion) {
                    packet.field_149153_g *= maxYMotion / distY
                }
            }
        }
    }

    private fun handleMinemenUpdate(player: EntityPlayerSP) {
        minemenTicks++
        if (minemenTicks > 23) {
            minemenCanCancel = true
        }

        if (minemenTicks in 2..4 && !minemenLastCancel) {
            player.motionX *= 0.99
            player.motionZ *= 0.99
        } else if (minemenTicks == 5 && !minemenLastCancel) {
            MovementUtils.strafe()
        }
    }

    private fun handleMinemenPacket(event: PacketEvent, packet: Packet<*>, player: EntityPlayerSP) {
        if (packet !is S12PacketEntityVelocity || packet.entityID != player.entityId)
            return

        minemenTicks = 0
        if (minemenCanCancel) {
            event.cancelEvent()
            minemenLastCancel = true
            minemenCanCancel = false
        } else {
            player.jump()
            minemenLastCancel = false
        }
    }

    private fun handleTickVelocityUpdate(player: EntityPlayerSP) {
        if (!velocityInput)
            return

        tickVelocityTicks++

        if (tickVelocityTicks > tickDelay) {
            if (player.motionY > 0 && tickResetMotionY) {
                player.motionY = 0.0
            }

            val factor = 1.0 - tickReduction
            player.motionX *= factor
            player.motionZ *= factor
            player.jumpMovementFactor = if (tickBypass) -0.001f else 0.0f
            velocityInput = false
        }

        if (player.onGround && tickVelocityTicks > 1) {
            velocityInput = false
        }
    }

    private fun handlePhasePacket(event: PacketEvent, packet: Packet<*>, player: EntityPlayerSP) {
        if (packet !is S12PacketEntityVelocity || packet.entityID != player.entityId)
            return

        if (!player.onGround && phaseOnlyGround)
            return

        when (phaseMode.lowercase()) {
            "normal" -> {
                velocityInput = true
                player.setPositionAndUpdate(player.posX, player.posY - phaseHeight, player.posZ)
            }

            "packet" -> {
                if (packet.motionX < 500 && packet.motionY < 500)
                    return

                sendPacket(
                    C03PacketPlayer.C04PacketPlayerPosition(
                        player.posX,
                        player.posY - phaseHeight,
                        player.posZ,
                        false
                    )
                )
            }
        }

        event.cancelEvent()
        packet.motionX = 0
        packet.motionY = 0
        packet.motionZ = 0
    }

    private fun handleGrimC07Packet(event: PacketEvent, packet: Packet<*>, player: EntityPlayerSP) {
        if (!grimC07FlagTimer.hasTimePassed(grimC07FlagPause.toLong())) {
            grimC07GotVelocity = false
            return
        }

        when (packet) {
            is S12PacketEntityVelocity -> {
                if (packet.entityID != player.entityId)
                    return

                event.cancelEvent()
                grimC07GotVelocity = true
            }

            is S27PacketExplosion -> {
                event.cancelEvent()
                grimC07GotVelocity = true
            }
        }
    }

    private fun handleGrimC07Tick(player: EntityPlayerSP) {
        if (!grimC07FlagTimer.hasTimePassed(grimC07FlagPause.toLong())) {
            grimC07GotVelocity = false
            return
        }

        if (!grimC07GotVelocity && !grimC07Always)
            return

        val pos = BlockPos(player.posX, player.posY, player.posZ)
        if (sendGrimC07BreakPacket(pos, player) || sendGrimC07BreakPacket(pos.up(), player)) {
            grimC07GotVelocity = false
        }
    }

    private fun sendGrimC07BreakPacket(pos: BlockPos, player: EntityPlayerSP): Boolean {
        val world = mc.theWorld ?: return false

        if (grimC07OnlyAir && !world.isAirBlock(pos))
            return false

        if (grimC07SendC03) {
            if (grimC07SendC06) {
                sendPacket(
                    C03PacketPlayer.C06PacketPlayerPosLook(
                        player.posX,
                        player.posY,
                        player.posZ,
                        player.rotationYaw,
                        player.rotationPitch,
                        player.onGround
                    )
                )
            } else {
                sendPacket(C03PacketPlayer(player.onGround))
            }
        }

        sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, pos, DOWN))
        if (grimC07BreakOnWorld) {
            world.setBlockToAir(pos)
        }
        return true
    }

    private fun handleSideStrafe(event: StrafeEvent, player: EntityPlayerSP) {
        val pos = sideStrafePos ?: return
        if (player.hurtTime <= 0)
            return

        val dx = pos.x + 0.5 - player.posX
        val dz = pos.z + 0.5 - player.posZ
        val yaw = (atan2(dz, dx) * 180.0 / PI - 90.0).toFloat()

        if (sideStrafeFace) {
            setTargetRotation(Rotation(yaw, player.rotationPitch), sideStrafeRotationSettings, 1)
        }

        if (sideStrafeSetMotion) {
            val currentSpeed = speed
            val yawRad = Math.toRadians(yaw.toDouble())
            player.motionX = -sin(yawRad) * currentSpeed
            player.motionZ = cos(yawRad) * currentSpeed
            return
        }

        var strafe = event.strafe
        var forward = event.forward
        val friction = event.friction
        var length = strafe * strafe + forward * forward

        if (length >= 1.0E-4F) {
            length = sqrt(length)
            if (length < 1.0F) {
                length = 1.0F
            }

            val scaled = friction / length
            strafe *= scaled
            forward *= scaled

            val yawSin = sin(yaw * PI.toFloat() / 180F)
            val yawCos = cos(yaw * PI.toFloat() / 180F)

            player.motionX += strafe * yawCos - forward * yawSin
            player.motionZ += forward * yawCos + strafe * yawSin
        }
    }

    private fun applyXZReductionByHurtTime() {
        val player = mc.thePlayer ?: return

        when {
            player.hurtTime == 10 -> reduceXZ(-1.0)
            player.hurtTime == 9 && player.onGround -> reduceXZ(0.9)
        }
    }

    private fun handleSmartJumpReset(player: EntityPlayerSP) {
        if (!smartJumpResetEnabled || !hasReceivedVelocity)
            return

        if (player.hurtTime == 9 && player.onGround) {
            if (backward) {
                mc.gameSettings.keyBindBack.pressed = true
            }

            if (sneakReduce) {
                sendPacket(C0BPacketEntityAction(player, START_SNEAKING))
                sendPacket(C0BPacketEntityAction(player, STOP_SNEAKING))
            }

            if (!player.isJumping && player.isSprinting) {
                player.tryJump()
            }

            hasReceivedVelocity = false
        } else if (player.hurtTime == 0) {
            hasReceivedVelocity = false
        }
    }

    private fun handleGrimCombatPacket(event: PacketEvent, packet: Packet<*>, player: EntityPlayerSP) {
        if (packet !is S12PacketEntityVelocity || packet.entityID != player.entityId)
            return

        if (mc.currentScreen is GuiGameOver)
            return

        if (mc.playerController.currentGameType === WorldSettings.GameType.SPECTATOR)
            return

        if (player.isOnLadder || player.isBurning && fireCheckValue || player.isInWater && waterCheckValue)
            return

        if (player.fallDistance > 1.5f && fallCheckValue)
            return

        if ((player.isEating || player.isUsingItem) && consumeCheck)
            return

        if (isInsideSoulSand())
            return

        val horizontalStrength = sqrt(packet.motionX.toDouble() * packet.motionX + packet.motionZ.toDouble() * packet.motionZ)
        if (horizontalStrength <= 1000.0)
            return

        velocityInput = true
        reduceXZ = 1.0

        val target = findGrimCombatTarget(player) ?: return
        val sprinting = player.serverSprintState

        if (!sprinting) {
            sendPacket(C0BPacketEntityAction(player, START_SPRINTING))
        }

        repeat(attackCountValue) {
            sendPackets(
                C0APacketAnimation(),
                C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK)
            )
        }

        if (!sprinting) {
            sendPacket(C0BPacketEntityAction(player, STOP_SPRINTING))
        }

        velX = packet.motionX
        velY = packet.motionY
        velZ = packet.motionZ
        attacked = true
        event.cancelEvent()
    }

    private fun findGrimCombatTarget(player: EntityPlayerSP): Entity? {
        mc.objectMouseOver?.let { mouse ->
            if (mouse.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
                mouse.entityHit is EntityLivingBase &&
                player.getDistanceToEntityBox(mouse.entityHit) <= grimRange
            ) {
                return mouse.entityHit
            }
        }

        if (!raycastValue) {
            KillAura.target?.takeIf {
                isSelected(it, true) && player.getDistanceToEntityBox(it) <= grimRange
            }?.let { return it }
        }

        return null
    }

    private fun reduceXZ(factor: Double) {
        val player = mc.thePlayer ?: return
        player.motionX *= factor
        player.motionZ *= factor
    }

    private fun isInsideSoulSand(): Boolean {
        val world = mc.theWorld ?: return false
        val player = mc.thePlayer ?: return false
        val box = player.entityBoundingBox.contract(0.001, 0.001, 0.001)

        val minX = kotlin.math.floor(box.minX).toInt()
        val maxX = kotlin.math.floor(box.maxX + 1.0).toInt()
        val minY = kotlin.math.floor(box.minY).toInt()
        val maxY = kotlin.math.floor(box.maxY + 1.0).toInt()
        val minZ = kotlin.math.floor(box.minZ).toInt()
        val maxZ = kotlin.math.floor(box.maxZ + 1.0).toInt()

        for (x in minX until maxX) {
            for (y in minY until maxY) {
                for (z in minZ until maxZ) {
                    if (world.getBlockState(BlockPos(x, y, z)).block is BlockSoulSand) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun getNearestEntityInRange(range: Float = this.range): Entity? {
        val player = mc.thePlayer ?: return null

        return mc.theWorld.loadedEntityList.filter {
            isSelected(it, true) && player.getDistanceToEntityBox(it) <= range
        }.minByOrNull { player.getDistanceToEntityBox(it) }
    }

    data class BufferedPacket(val packet: Packet<*>, var remainingTicks: Int)
}
