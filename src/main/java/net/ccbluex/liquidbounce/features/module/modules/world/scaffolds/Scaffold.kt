/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffolds

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.async.loopSequence
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeSprint
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.changeTimer
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.isFalling
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.isHurting
import net.ccbluex.liquidbounce.utils.firefly.general.SomeUtil.reduceXZ
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils.checkGroundBelow
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.FreezeUtil
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.blocksAmount
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.hotBarSlot
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.rotation.PlaceRotation
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.canUpdateRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getFixedAngleDelta
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.*
import net.minecraft.block.Block
import net.minecraft.block.BlockBush
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks.air
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.util.*
import net.minecraft.world.WorldSettings
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeEventFactory
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.*

object Scaffold : Module("Scaffold", Category.WORLD) {

    private val towerMode by Tower.towerModeValues

    init {
        addValues(Tower.values)
    }

    @JvmField
    val scaffoldModeValue = choices(
        "ScaffoldMode", arrayOf("Normal", "Rewinside", "Expand", "Telly", "GodBridge"), "Normal"
    )
    val scaffoldMode by scaffoldModeValue

    private val omniDirectionalExpand by boolean("OmniDirectionalExpand", false) { scaffoldMode == "Expand" }
    private val expandLength by int("ExpandLength", 1, 1..6) { scaffoldMode == "Expand" }

    private val placeDelayValue = boolean("PlaceDelay", true) { scaffoldMode != "GodBridge" }
    private val delay by intRange("Delay", 0..0, 0..1000) { placeDelayValue.isActive() }

    private val extraClicks by boolean("DoExtraClicks", false)
    private val simulateDoubleClicking by boolean("SimulateDoubleClicking", false) { extraClicks }
    private val extraClickCPS by intRange("ExtraClickCPS", 3..7, 0..100) { extraClicks }
    private val placementAttempt by choices(
        "PlacementAttempt", arrayOf("Fail", "Independent"), "Fail"
    ) { extraClicks }

    private val autoBlock by choices("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
    private val sortByHighestAmount by boolean("SortByHighestAmount", false) { autoBlock != "Off" }
    private val earlySwitch by boolean("EarlySwitch", false) { autoBlock != "Off" && !sortByHighestAmount }
    private val amountBeforeSwitch by int(
        "SlotAmountBeforeSwitch", 3, 1..10
    ) { earlySwitch && !sortByHighestAmount }

    private val autoF5 by boolean("AutoF5", false).subjective()

    val sprint by boolean("Sprint", false)
    val SprintControl by choices("SprintControl",arrayOf("InAir","OnGround","Always","OnFalling","OnUpping","Vanilla","Stop","Matrix"),"Vanilla") { sprint }
    private val swing by boolean("Swing", true).subjective()
    private val down by boolean("Down", true) { !sameY && scaffoldMode !in arrayOf("GodBridge", "Telly") }

    private val ticksUntilRotation by intRange("TicksUntilRotation", 3..3, 1..8) {
        scaffoldMode == "Telly"
    }

    private val waitForRots by boolean("WaitForRotations", false) { isGodBridgeEnabled }
    private val useOptimizedPitch by boolean("UseOptimizedPitch", false) { isGodBridgeEnabled }
    private val customGodPitch by float(
        "GodBridgePitch", 73.5f, 0f..90f
    ) { isGodBridgeEnabled && !useOptimizedPitch }

    val jumpAutomatically by boolean("JumpAutomatically", true) { scaffoldMode == "GodBridge" }
    private val blocksToJumpRange by intRange("BlocksToJumpRange", 4..4, 1..8) {  scaffoldMode == "GodBridge" && !jumpAutomatically }

    private val forceJumpOnFirst by boolean("ForceJumpOnEnable",false) {scaffoldMode == "Telly"}
    private val startHorizontally by boolean("StartHorizontally", true) { scaffoldMode == "Telly" }
    private val horizontalPlacementsRange by intRange("HorizontalPlacementsRange", 1..1, 1..10) { scaffoldMode == "Telly" }
    private val verticalPlacementsRange by intRange("VerticalPlacementsRange", 1..1, 1..10) { scaffoldMode == "Telly" }
    private val jumpTicksRange by intRange("JumpTicksRange", 0..0, 0..10) { scaffoldMode == "Telly" }
    private val stopJumpWhenSpeedEnabled by boolean("StopJumpWhenSpeedEnabled",true) {scaffoldMode == "Telly"}
    private val tryAntiFailToPlace by boolean("TryAntiFailToPlace",false) {scaffoldMode == "Telly"}
    private val blink by boolean("Blink",false) {scaffoldMode == "Telly" && tryAntiFailToPlace}
    private val allowClutching by boolean("AllowClutching", true) { scaffoldMode !in arrayOf("Telly", "Expand") }
    private val horizontalClutchBlocks by int("HorizontalClutchBlocks", 3, 1..5) {
        allowClutching && scaffoldMode !in arrayOf("Telly", "Expand")
    }
    private val verticalClutchBlocks by int("VerticalClutchBlocks", 2, 1..3) {
        allowClutching && scaffoldMode !in arrayOf("Telly", "Expand")
    }
    private val blockSafe by boolean("BlockSafe", false) { !isGodBridgeEnabled }

    @JvmField
    val eagleValue =
        choices("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal") { scaffoldMode != "GodBridge" }
    val eagle by eagleValue
    private val eagleMode by choices("EagleMode", arrayOf("Both", "OnGround", "InAir"), "Both")
    { eagle != "Off" && scaffoldMode != "GodBridge" }
    private val adjustedSneakSpeed by boolean("AdjustedSneakSpeed", true)
    { eagle == "Silent" && scaffoldMode != "GodBridge" }
    private val eagleSpeed by float("EagleSpeed", 0.3f, 0.3f..1.0f) { eagle != "Off" && scaffoldMode != "GodBridge" }
    val eagleSprint by boolean("EagleSprint", false) { eagle == "Normal" && scaffoldMode != "GodBridge" }
    private val blocksToEagle by intRange("BlocksToEagle", 0..0, 0..10) { eagle != "Off" && scaffoldMode != "GodBridge" }
    private val edgeDistance by float("EagleEdgeDistance", 0f, 0f..0.5f)
    { eagle != "Off" && scaffoldMode != "GodBridge" }
    private val useMaxSneakTime by boolean("UseMaxSneakTime", true) { eagle != "Off" && scaffoldMode != "GodBridge" }
    private val maxSneakTicks by intRange("MaxSneakTicks", 3..3, 0..10) { useMaxSneakTime }
    private val blockSneakingAgainUntilOnGround by boolean("BlockSneakingAgainUntilOnGround", true)
    { useMaxSneakTime && eagleMode != "OnGround" }
    private val autoParkour by boolean("AutoParkour", false) {scaffoldMode in arrayOf("Normal")}

    private val modeList =
        choices("ScaffoldRotationsMode", arrayOf(
            "Off",
            "Normal",
            "Stabilized",
            "ReverseYaw",
            "GodBridge",
        ),
            "Normal"
        )
    private val preAim by boolean("PreAim", true) { scaffoldMode != "GodBridge" && scaffoldMode != "Telly" }
    private val preAimDistance by float("PreAimDistance", 1.0f, 0.5f..3.0f) { preAim }
    private val preAimDelay by int("PreAimDelay", 2, 1..10) { preAim }
    private val options = RotationSettings(this) { modeList.get() != "Off" }.apply {
        strictValue.excludeWithState()
        resetTicksValue.setSupport { it && scaffoldMode != "Telly" }
    }

    val searchMode by choices("SearchMode", arrayOf("Area", "Center"), "Area") { scaffoldMode != "GodBridge" }
    private val minDist by float("MinDist", 0f, 0f..0.5f) { scaffoldMode !in arrayOf("GodBridge", "Telly") }

    private val zitterMode by choices("Zitter", arrayOf("Off", "Teleport", "Smooth"), "Off")
    private val zitterSpeed by float("ZitterSpeed", 0.13f, 0.1f..0.3f) { zitterMode == "Teleport" }
    private val zitterStrength by float("ZitterStrength", 0.05f, 0f..0.2f) { zitterMode == "Teleport" }
    private val zitterTicks by intRange("ZitterTicks", 2..3, 0..6) { zitterMode == "Smooth" }

    private val useSneakMidAir by boolean("UseSneakMidAir", false) { zitterMode == "Smooth" }

    private val timerEnable by boolean("Timer",false)
    private val groundTimer by float("GroundTimerSpeed", 1f, 0.1f..10f) {timerEnable}
    private val airTimer by float("AirTimer",1f,0.1f..10.0f) {timerEnable}
    private val speedModifier by float("SpeedModifier", 1f, 0f..2f)
    private val speedLimiter by boolean("SpeedLimiter", false) { !slow }
    private val speedLimit by float("SpeedLimit", 0.11f, 0.01f..0.12f) { !slow && speedLimiter }
    private val slow by boolean("Slow", false)
    private val slowGround by boolean("SlowOnlyGround", false) { slow }
    private val slowSpeed by float("SlowSpeed", 0.6f, 0.2f..0.8f) { slow }

    private val simpleSpeed by boolean("SimpleSpeed",false)
    private val groundBoostFactor by float("GroundBoostFactor",0.01f,0.0f..2.0f) {simpleSpeed}
    private val airBoostFactor by float("AIrBoostFactor",0.01f,0.0f..2.0f) {simpleSpeed}

    private val jumpStrafe by boolean("JumpStrafe", false)
    private val jumpStraightStrafe by floatRange("JumpStraightStrafe", 0.4f..0.45f, 0.1f..1f) { jumpStrafe }
    private val jumpDiagonalStrafe by floatRange("JumpDiagonalStrafe", 0.4f..0.45f, 0.1f..1f) { jumpStrafe }

    private val sameY by boolean("SameY", false) { scaffoldMode != "GodBridge" }
    private val AutoJump by boolean("AutoJump",false) { scaffoldMode != "GodBridge" && sameY && scaffoldMode != "Telly"}
    private val jumpOnUserInput by boolean("JumpOnUserInput", true) { sameY && scaffoldMode != "GodBridge" }
    private val smartUpdateSameY by boolean("SmartUpdateSameY",true) {sameY}
    private val smartUpdateSameYMode by multiChoices("SmartUpdateSameYMode",arrayOf("SmartUpdateSameYOnFalling","ResetSameYOnHurt"),
        setOf("SmartUpdateSameYOnFalling")
    ){sameY && smartUpdateSameY}
    private val smartUpdateSameYLevel by int("SmartUpdateSameYLevel",2,1..5) {sameY && smartUpdateSameY}

    private val safeWalkValue = boolean("SafeWalk", true) { scaffoldMode != "GodBridge" }
    private val airSafe by boolean("AirSafe", false) { safeWalkValue.isActive() }

    private val mark by boolean("Mark", false).subjective()
    private val trackCPS by boolean("TrackCPS", false).subjective()

    var placeRotation: PlaceRotation? = null

    private var launchY = -999

    private var jumpCounter = 0

    val shouldJumpOnInput: Boolean
        get() = (sameY && !jumpOnUserInput) ||
                (sameY && scaffoldMode != "Telly" && !mc.gameSettings.keyBindJump.isKeyDown) &&
                mc.thePlayer.posY >= launchY &&
                !mc.thePlayer.onGround

    private val shouldKeepLaunchPosition: Boolean
        get() = sameY && scaffoldMode != "GodBridge" &&
                !(scaffoldMode == "Telly" && mc.gameSettings.keyBindJump.isKeyDown)

    private var zitterDirection = false

    var placedBlock = false

    private val delayTimer = object : DelayTimer(delay.first, delay.last, MSTimer()) {
        override fun hasTimePassed() = !placeDelayValue.isActive() || super.hasTimePassed()
    }

    private val zitterTickTimer = TickDelayTimer(zitterTicks.first, zitterTicks.last)

    private var placedBlocksWithoutEagle = 0

    var eagleSneaking = false

    private var requestedStopSneak = false

    private val isEagleEnabled
        get() = eagle != "Off" && !shouldGoDown && scaffoldMode != "GodBridge"

    val shouldGoDown
        get() = down && !sameY && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && scaffoldMode !in arrayOf(
            "GodBridge", "Telly"
        ) && blocksAmount() > 1

    private val currRotation
        get() = RotationUtils.currentRotation ?: mc.thePlayer.rotation

    private var extraClick = ExtraClickInfo(TimeUtils.randomClickDelay(extraClickCPS.first, extraClickCPS.last, "RNG"), 0L, 0)

    private var blocksPlacedUntilJump = 0

    private val isManualJumpOptionActive
        get() = scaffoldMode == "GodBridge" && !jumpAutomatically

    private var blocksToJump = blocksToJumpRange.random()

    private val isGodBridgeEnabled
        get() = scaffoldMode == "GodBridge" || scaffoldMode == "Normal" && modeList.get() == "GodBridge"

    private var godBridgeTargetRotation: Rotation? = null

    private val isLookingDiagonally: Boolean
        get() {
            val player = mc.thePlayer ?: return false

            val directionDegree = MovementUtils.direction.toDegreesF()

            val yaw = round(abs(MathHelper.wrapAngleTo180_float(directionDegree)) / 45f) * 45f

            val isYawDiagonal = yaw % 90 != 0f
            val isMovingDiagonal = player.movementInput.moveForward != 0f && player.movementInput.moveStrafe == 0f
            val isStrafing = mc.gameSettings.keyBindRight.isKeyDown || mc.gameSettings.keyBindLeft.isKeyDown

            return isYawDiagonal && (isMovingDiagonal || isStrafing)
        }

    private var ticksUntilJump = 0
    private var blocksUntilAxisChange = 0
    private var jumpTicks = jumpTicksRange.random()
    private var horizontalPlacements = horizontalPlacementsRange.random()
    private var verticalPlacements = verticalPlacementsRange.random()
    private val shouldPlaceHorizontally
        get() = scaffoldMode == "Telly" && mc.thePlayer.isMoving && (startHorizontally && blocksUntilAxisChange <= horizontalPlacements || !startHorizontally && blocksUntilAxisChange > verticalPlacements)

    val showBlockValue = boolean("ShowBlockCountOnDynamicIsland",false)
    val showBlockCount by showBlockValue

    override fun onEnable() {
        val player = mc.thePlayer ?: return
        jumpCounter = 0
        MinecraftForge.EVENT_BUS.register(this)
        launchY = player.posY.roundToInt()
        blocksUntilAxisChange = 0
        ticksUntilJump = 0
        jumpTicks = jumpTicksRange.random()
    }

    val onUpdate2 = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        if (mc.thePlayer.isHurting() && scaffoldMode == "Telly" && "resetSameYOnHurt" in smartUpdateSameYMode && sameY) {
            launchY = player.posY.roundToInt()
        }
        if (mc.thePlayer.isFalling() && "SmartUpdateSameYOnFalling" in smartUpdateSameYMode && mc.thePlayer.fallDistance >= 1.5f) launchY = player.posY.roundToInt() - 1
        if (smartUpdateSameY && sameY) {
            if (abs(launchY - player.posY.roundToInt()) >= smartUpdateSameYLevel) {
                launchY = player.posY.roundToInt()
            }
        }
        if (!mc.thePlayer.isMoving) return@handler
        if (timerEnable) {
            if (mc.thePlayer.onGround) changeTimer(groundTimer) else changeTimer(airTimer)
        }
        if (simpleSpeed) {
            if (mc.thePlayer.onGround) reduceXZ(groundBoostFactor + 1.0) else reduceXZ(airBoostFactor.toDouble() + 1.0)
        }
    }

    val onUpdate = loopSequence {
        val player = mc.thePlayer ?: return@loopSequence

        if (mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR) return@loopSequence

        if (autoParkour && player.isMoving && player.onGround && !player.isSneaking && scaffoldMode == "Normal" &&
            !mc.gameSettings.keyBindSneak.isKeyDown) {

            val simPlayer = SimulatedPlayer.fromClientPlayer(player.movementInput)
            simPlayer.posX = player.posX
            simPlayer.posY = player.posY
            simPlayer.posZ = player.posZ
            simPlayer.onGround = player.onGround
            simPlayer.motionX = player.motionX
            simPlayer.motionY = player.motionY
            simPlayer.motionZ = player.motionZ

            simPlayer.tick()

            if (!simPlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown) {
                player.tryJump()
            }
        }

        if (scaffoldMode == "Telly") {
            if (shouldFreeze()) {
                if (!FreezeUtil.frozen) {
                    val (frozenX, frozenY, frozenZ) = FreezeUtil.getFrozenPosition()
                    if (frozenX == 0.0 && frozenY == 0.0 && frozenZ == 0.0) {
                        FreezeUtil.freeze()
                    }
                }
            } else if (FreezeUtil.frozen) {
                FreezeUtil.unfreeze()

                if (BlinkUtils.isBlinking && blink) {
                    BlinkUtils.unblink()
                }
            }
        }

        if (player.onGround) ticksUntilJump++

        if (shouldGoDown) {
            mc.gameSettings.keyBindSneak.pressed = false
        }

        if (slow) {
            if (!slowGround || slowGround && player.onGround) {
                player.motionX *= slowSpeed
                player.motionZ *= slowSpeed
            }
        }

        if (sprint && mc.thePlayer.isMoving) {
            when (SprintControl) {
                "InAir" -> if (!mc.thePlayer.onGround && !mc.thePlayer.isSprinting) changeSprint(
                    setState = true,
                    sendPacketToServer = true,
                    forceChange = true
                )
                "OnGround" -> if (mc.thePlayer.onGround && !mc.thePlayer.isSprinting) changeSprint(
                    setState = true,
                    sendPacketToServer = true,
                    forceChange = true
                )
                "Always" -> if (!mc.thePlayer.isSprinting) changeSprint(
                    setState = true,
                    sendPacketToServer = true,
                    forceChange = true
                )
                "Stop" -> if (mc.thePlayer.isSprinting) changeSprint(
                    setState = false,
                    sendPacketToServer = true,
                    forceChange = true
                )
                "OnFalling" -> if (!mc.thePlayer.onGround && mc.thePlayer.motionY < 0.0 && !mc.thePlayer.isSprinting) changeSprint(
                    setState = true,
                    sendPacketToServer = true,
                    forceChange = true
                )
                "OnUpping" -> if (!mc.thePlayer.onGround && mc.thePlayer.motionY > 0.0 && !mc.thePlayer.isSprinting) changeSprint(
                    setState = true, sendPacketToServer = true,
                    forceChange = true
                )
                "Matrix" -> changeSprint(setState = true, sendPacketToServer = false,true)
            }
        }

        if (isEagleEnabled) {
            var dif = 0.5
            val blockPos = BlockPos(player).down()

            for (side in EnumFacing.entries) {
                if (side.axis == EnumFacing.Axis.Y) {
                    continue
                }

                val neighbor = blockPos.offset(side)

                if (neighbor.isReplaceable) {
                    val calcDif = (if (side.axis == EnumFacing.Axis.Z) {
                        abs(neighbor.z + 0.5 - player.posZ)
                    } else {
                        abs(neighbor.x + 0.5 - player.posX)
                    }) - 0.5

                    if (calcDif < dif) {
                        dif = calcDif
                    }
                }
            }

            val blockSneaking = WaitTickUtils.hasScheduled("block")
            val alreadySneaking = WaitTickUtils.hasScheduled("sneak")

            val options = mc.gameSettings

            run {
                if (placedBlocksWithoutEagle < blocksToEagle.random() && !alreadySneaking && !blockSneaking && !eagleSneaking && !requestedStopSneak) {
                    return@run
                }

                val eagleCondition = when (eagleMode) {
                    "OnGround" -> player.onGround
                    "InAir" -> !player.onGround
                    else -> true
                }

                val pressedOnKeyboard = Keyboard.isKeyDown(options.keyBindSneak.keyCode)

                var shouldEagle =
                    eagleCondition && (blockPos.isReplaceable || dif < edgeDistance) || pressedOnKeyboard

                val shouldSchedule = !requestedStopSneak

                if (requestedStopSneak) {
                    requestedStopSneak = false

                    if (!player.onGround) {
                        shouldEagle = pressedOnKeyboard
                    }
                } else if (blockSneaking || alreadySneaking) {
                    return@run
                }

                if (eagle == "Silent") {
                    if (eagleSneaking != shouldEagle) {
                        sendPacket(
                            C0BPacketEntityAction(
                                player, if (shouldEagle) {
                                    C0BPacketEntityAction.Action.START_SNEAKING
                                } else {
                                    C0BPacketEntityAction.Action.STOP_SNEAKING
                                }
                            )
                        )

                        if (adjustedSneakSpeed && shouldEagle) {
                            player.motionX *= eagleSpeed
                            player.motionZ *= eagleSpeed
                        }
                    }

                    eagleSneaking = shouldEagle
                } else {
                    options.keyBindSneak.pressed = shouldEagle
                    eagleSneaking = shouldEagle
                }

                if (eagleSneaking && shouldSchedule) {
                    if (useMaxSneakTime) {
                        WaitTickUtils.conditionalSchedule("sneak") { elapsed ->
                            (elapsed >= maxSneakTicks.random() + 1).also { requestedStopSneak = it }
                        }
                    }

                    if (blockSneakingAgainUntilOnGround && !player.onGround) {
                        WaitTickUtils.conditionalSchedule("block") {
                            mc.thePlayer?.onGround.also { if (it != false) requestedStopSneak = true } ?: true
                        }
                    }
                }

                placedBlocksWithoutEagle = 0
            }
        }

        if (shouldUpdatePreAim() && player.isMoving) {
            val predictedPos = predictNextBlockPosition()
            if (predictedPos != null) {
                if (predictedPos.isReplaceable) {
                    if (search(predictedPos, raycast = false, area = true, forPreAim = true)) {
                        if (mark && player.ticksExisted % 40 == 0) {
                            println("[PreAim] Successfully pre-aiming at: $predictedPos")
                        }
                    } else if (mark && player.ticksExisted % 40 == 0) {
                        println("[PreAim] Failed to find placement for predicted pos: $predictedPos")
                    }
                } else if (mark && player.ticksExisted % 40 == 0) {
                    println("[PreAim] Predicted pos not replaceable: $predictedPos")
                }
            }
        }
        if (AutoJump && shouldKeepLaunchPosition && player.onGround && mc.thePlayer.isMoving && !mc.gameSettings.keyBindJump.isKeyDown) {
            player.tryJump()
        }
        if (sameY && mc.gameSettings.keyBindJump.isKeyDown && player.onGround) {
            launchY = player.posY.roundToInt() + 1
        }

        if (player.onGround) {
            if (scaffoldMode == "Rewinside") {
                MovementUtils.strafe(0.2F)
                player.motionY = 0.0
            }
        }
        if (scaffoldMode == "Telly" && sameY && mc.gameSettings.keyBindJump.isKeyDown && player.onGround) {
            launchY = player.posY.roundToInt() + 1
        }
    }
    val onStrafe = handler<StrafeEvent> {
        val player = mc.thePlayer ?: return@handler
        if (scaffoldMode == "Telly" && mc.gameSettings.keyBindJump.isKeyDown) {
            return@handler
        }
        if (scaffoldMode == "Telly" && player.onGround && player.isMoving && currRotation == player.rotation && ticksUntilJump >= jumpTicks && checkSpeedModule()) {
            player.tryJump()
            jumpCounter++


            ticksUntilJump = 0
            jumpTicks = jumpTicksRange.random()
        } else if (scaffoldMode == "Telly" && player.onGround && player.isMoving && forceJumpOnFirst && jumpCounter == 0 && checkSpeedModule()) {
            player.tryJump()
            jumpCounter
        }
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        if (player.ticksExisted == 1) {
            launchY = player.posY.roundToInt()
        }

        val rotation = RotationUtils.currentRotation

        update()

        val ticks = if (options.keepRotation) {
            if (scaffoldMode == "Telly") 1 else options.resetTicks
        } else {
            if (isGodBridgeEnabled) options.resetTicks else RotationUtils.resetTicks
        }

        if (!Tower.isTowering && isGodBridgeEnabled && options.rotationsActive) {
            generateGodBridgeRotations(ticks)

            return@handler
        }

        if (options.rotationsActive && rotation != null) {
            val placeRotation = this.placeRotation?.rotation ?: rotation

            if (RotationUtils.resetTicks != 0 || options.keepRotation) {
                setRotation(placeRotation, ticks)
            }
        }
    }
    val onTick = handler<GameTickEvent> {
        val target = placeRotation?.placeInfo

        val raycastProperly = !(scaffoldMode == "Expand" && expandLength > 1 || shouldGoDown) && options.rotationsActive

        val raycast = performBlockRaytrace(currRotation, mc.playerController.blockReachDistance)

        var alreadyPlaced = false

        if (extraClicks) {
            val doubleClick = if (simulateDoubleClicking) RandomUtils.nextInt(-1, 1) else 0

            val clicks = extraClick.clicks + doubleClick

            repeat(clicks) {
                extraClick.clicks--

                doPlaceAttempt(raycast, it + 1 == clicks) { alreadyPlaced = true }
            }
        }

        if (target == null) {
            if (placeDelayValue.isActive()) {
                delayTimer.reset()
            }
            return@handler
        }

        if (alreadyPlaced || SilentHotbar.modifiedThisTick) {
            return@handler
        }

        raycast.let {
            if (!options.rotationsActive || it != null && it.blockPos == target.blockPos && (!raycastProperly || it.sideHit == target.enumFacing)) {
                val result = if (raycastProperly && it != null) {
                    PlaceInfo(it.blockPos, it.sideHit, it.hitVec)
                } else {
                    target
                }

                place(result)
            }
        }
    }

    val onSneakSlowDown = handler<SneakSlowDownEvent> { event ->
        if (!isEagleEnabled || eagle != "Normal") {
            return@handler
        }

        event.forward *= eagleSpeed / 0.3f
        event.strafe *= eagleSpeed / 0.3f
    }

    val onMovementInput = handler<MovementInputEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (!isGodBridgeEnabled || !player.onGround) return@handler

        if (waitForRots) {
            godBridgeTargetRotation?.run {
                event.originalInput.sneak =
                    event.originalInput.sneak || rotationDifference(this, currRotation) > getFixedAngleDelta()
            }
        }

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)

        simPlayer.rotationYaw = currRotation.yaw

        simPlayer.tick()

        if (!simPlayer.onGround && !isManualJumpOptionActive || blocksPlacedUntilJump > blocksToJump) {
            event.originalInput.jump = true

            blocksPlacedUntilJump = 0

            blocksToJump = blocksToJumpRange.random()
        }
    }

    fun update() {
        val player = mc.thePlayer ?: return
        val holdingItem = player.heldItem?.item is ItemBlock

        if (!holdingItem && (autoBlock == "Off" || InventoryUtils.findBlockInHotbar() == null)) {
            return
        }

        findBlock(scaffoldMode == "Expand" && expandLength > 1, searchMode == "Area")
    }

    private fun setRotation(rotation: Rotation, ticks: Int) {
        val player = mc.thePlayer ?: return

        if (scaffoldMode == "Telly" && player.isMoving) {
            if (player.airTicks < ticksUntilRotation.random() && ticksUntilJump >= jumpTicks) {
                return
            }
        }

        setTargetRotation(rotation, options, ticks)
    }

    private fun findBlock(expand: Boolean, area: Boolean) {
        val player = mc.thePlayer ?: return

        if (!shouldKeepLaunchPosition) launchY = player.posY.roundToInt()

        val blockPosition = if (shouldGoDown) {
            if (player.posY == player.posY.roundToInt() + 0.5) {
                BlockPos(player.posX, player.posY - 0.6, player.posZ)
            } else {
                BlockPos(player.posX, player.posY - 0.6, player.posZ).down()
            }
        } else if (shouldKeepLaunchPosition && launchY <= player.posY) {
            BlockPos(player.posX, launchY - 1.0, player.posZ)
        } else if (player.posY == player.posY.roundToInt() + 0.5) {
            BlockPos(player)
        } else {
            BlockPos(player).down()
        }

        if (!expand && (!blockPosition.isReplaceable || search(
                blockPosition, !shouldGoDown, area, shouldPlaceHorizontally
            ))
        ) {
            return
        }

        if (expand) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z

            repeat(expandLength) {
                if (search(blockPosition.add(x * it, 0, z * it), false, area)) return
            }
            return
        }

        val (horizontal, vertical) = if (scaffoldMode == "Telly") {
            5 to 3
        } else if (allowClutching) {
            horizontalClutchBlocks to verticalClutchBlocks
        } else {
            1 to 1
        }

        BlockPos.getAllInBox(
            blockPosition.add(-horizontal, 0, -horizontal), blockPosition.add(horizontal, -vertical, horizontal)
        ).sortedBy {
            BlockUtils.getCenterDistance(it)
        }.forEach {
            if (it.canBeClicked() || search(it, !shouldGoDown, area, shouldPlaceHorizontally)) {
                return
            }
        }
    }

    private fun place(placeInfo: PlaceInfo) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (!delayTimer.hasTimePassed() || shouldKeepLaunchPosition && launchY - 1 != placeInfo.vec3.yCoord.toInt() && scaffoldMode != "Expand") return

        val currentSlot = SilentHotbar.currentSlot

        var stack = player.hotBarSlot(currentSlot).stack

        if (stack == null || stack.item !is ItemBlock || (stack.item as ItemBlock).block is BlockBush || stack.stackSize <= 0 || sortByHighestAmount || earlySwitch) {
            val blockSlot = if (sortByHighestAmount) {
                InventoryUtils.findLargestBlockStackInHotbar() ?: return
            } else if (earlySwitch) {
                InventoryUtils.findBlockStackInHotbarGreaterThan(amountBeforeSwitch)
                    ?: InventoryUtils.findBlockInHotbar() ?: return
            } else {
                InventoryUtils.findBlockInHotbar() ?: return
            }

            stack = player.hotBarSlot(blockSlot).stack

            if ((stack.item as? ItemBlock)?.canPlaceBlockOnSide(
                    world, placeInfo.blockPos, placeInfo.enumFacing, player, stack
                ) == false
            ) {
                return
            }

            if (autoBlock != "Off") {
                SilentHotbar.selectSlotSilently(this, blockSlot, render = autoBlock == "Pick", resetManually = true)
                placedBlock
            }
        }

        tryToPlaceBlock(stack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3)

        if (autoBlock == "Switch") SilentHotbar.resetSlot(this, true)

        findBlockToSwitchNextTick(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    private fun doPlaceAttempt(raytrace: MovingObjectPosition?, lastClick: Boolean, onSuccess: () -> Unit = { }) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        val stack = player.hotBarSlot(SilentHotbar.currentSlot).stack ?: return

        if (stack.item !is ItemBlock || InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as ItemBlock).block)) {
            return
        }

        raytrace ?: return

        val block = stack.item as ItemBlock

        val canPlaceOnUpperFace = block.canPlaceBlockOnSide(
            world, raytrace.blockPos, EnumFacing.UP, player, stack
        )

        val shouldPlace = if (placementAttempt == "Fail") {
            !block.canPlaceBlockOnSide(world, raytrace.blockPos, raytrace.sideHit, player, stack)
        } else {
            if (shouldKeepLaunchPosition) {
                raytrace.blockPos.y == launchY - 1 && !canPlaceOnUpperFace
            } else if (shouldPlaceHorizontally) {
                !canPlaceOnUpperFace
            } else {
                raytrace.blockPos.y <= player.posY.toInt() - 1 && !(raytrace.blockPos.y == player.posY.toInt() - 1 && canPlaceOnUpperFace && raytrace.sideHit == EnumFacing.UP)
            }
        }

        if (!raytrace.typeOfHit.isBlock || !shouldPlace) {
            return
        }

        tryToPlaceBlock(stack, raytrace.blockPos, raytrace.sideHit, raytrace.hitVec, attempt = true) { onSuccess() }

        if (lastClick) {
            findBlockToSwitchNextTick(stack)
        }

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    override fun onDisable() {
        val player = mc.thePlayer ?: return

        if (FreezeUtil.frozen) {
            FreezeUtil.unfreeze()
        }

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking && player.isSneaking) {
                player.isSneaking = false
            }
        }

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
            mc.gameSettings.keyBindRight.pressed = false
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
            mc.gameSettings.keyBindLeft.pressed = false
        }

        if (autoF5) {
            mc.gameSettings.thirdPersonView = 0
        }

        placeRotation = null
        mc.timer.timerSpeed = 1f
        preAimTimer = 0  // 重置预测计时器

        SilentHotbar.resetSlot(this)

        options.instant = false
    }
    val onMove = handler<MoveEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (tryAntiFailToPlace && shouldFreeze()) {
            event.cancelEvent()
        }

        if (!safeWalkValue.isActive() || shouldGoDown) {
            return@handler
        }

        if (airSafe || player.onGround) {
            event.isSafeWalk = true
        }
    }

    val jumpHandler = handler<JumpEvent> { event ->
        if (!jumpStrafe) return@handler

        if (event.eventState == EventState.POST) {
            MovementUtils.strafe(
                (if (!isLookingDiagonally) jumpStraightStrafe else jumpDiagonalStrafe).random()
            )
        }
    }

    val onRender3D = handler<Render3DEvent> {
        val player = mc.thePlayer ?: return@handler

        val shouldBother =
            !(shouldGoDown || scaffoldMode == "Expand" && expandLength > 1) && extraClicks && (player.isMoving || MovementUtils.speed > 0.03)

        if (shouldBother) {
            currRotation.let {
                performBlockRaytrace(it, mc.playerController.blockReachDistance)?.let { raytrace ->
                    val timePassed = System.currentTimeMillis() - extraClick.lastClick >= extraClick.delay

                    if (raytrace.typeOfHit.isBlock && timePassed) {
                        extraClick = ExtraClickInfo(
                            TimeUtils.randomClickDelay(extraClickCPS.first, extraClickCPS.last, "RNG"),
                            System.currentTimeMillis(),
                            extraClick.clicks + 1
                        )
                    }
                }
            }
        }

        if (!mark) {
            return@handler
        }

        if (preAim && player.isMoving) {
            val predictedPos = predictNextBlockPosition()
            if (predictedPos != null) {
                val distance = player.getDistance(
                    predictedPos.x + 0.5,
                    predictedPos.y + 0.5,
                    predictedPos.z + 0.5
                )


                val alpha = (50 * (1.0 - min(distance / 3.0, 1.0))).toInt()

                RenderUtils.drawBlockBox(predictedPos, Color(0, 255, 0, alpha), false)
            }
        }

        repeat(if (scaffoldMode == "Expand") expandLength + 1 else 2) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            val blockPos = BlockPos(
                player.posX + x * it,
                if (shouldKeepLaunchPosition && launchY <= player.posY) launchY - 1.0 else player.posY - (if (player.posY == player.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                player.posZ + z * it
            )
            val placeInfo = PlaceInfo.get(blockPos)

            if (blockPos.isReplaceable && placeInfo != null) {
                RenderUtils.drawBlockBox(blockPos, Color(68, 117, 255, 100), false)
                return@handler
            }
        }
    }
    val onPacket = handler<PacketEvent> { e ->
        val packet = e.packet
        if (shouldFreeze()) {
            if (blink) BlinkUtils.blink(packet,e, sent = true, receive = false)

            if (packet.isMovePacket && !packet.isRotationPacket) e.cancelEvent()
        }
    }

    fun search(
        blockPosition: BlockPos,
        raycast: Boolean,
        area: Boolean,
        horizontalOnly: Boolean = false,
        forPreAim: Boolean = false
    ): Boolean {
        val player = mc.thePlayer ?: return false

        options.instant = false
        if (!blockPosition.isReplaceable) {
            if (autoF5) mc.gameSettings.thirdPersonView = 0
            return false
        } else {
            if (autoF5 && mc.gameSettings.thirdPersonView != 1) mc.gameSettings.thirdPersonView = 1
        }

        val maxReach = mc.playerController.blockReachDistance

        val eyes = player.eyes
        var placeRotation: PlaceRotation? = null

        var currPlaceRotation: PlaceRotation?

        for (side in EnumFacing.entries) {
            if (horizontalOnly && side.axis == EnumFacing.Axis.Y) {
                continue
            }

            val neighbor = blockPosition.offset(side)

            if (!neighbor.canBeClicked()) {
                continue
            }

            if (!area || isGodBridgeEnabled) {
                currPlaceRotation =
                    findTargetPlace(blockPosition, neighbor, Vec3(0.5, 0.5, 0.5), side, eyes, maxReach, raycast)
                        ?: continue

                placeRotation = compareDifferences(currPlaceRotation, placeRotation)
            } else {
                val samplePoints = getOptimizedSamplePoints(neighbor)

                for (vec3 in samplePoints) {
                    currPlaceRotation =
                        findTargetPlace(blockPosition, neighbor, vec3, side, eyes, maxReach, raycast)
                            ?: continue

                    placeRotation = compareDifferences(currPlaceRotation, placeRotation)
                }
            }
        }

        placeRotation ?: return false

        if (forPreAim && options.rotationsActive && !isGodBridgeEnabled) {
            placeRotation.rotation.let { rotation ->
                val keepTicks = min(preAimDelay * 2, 10)
                setTargetRotation(rotation, options, keepTicks)
            }
        } else if (options.rotationsActive && !isGodBridgeEnabled) {
            val rotationDifference = rotationDifference(placeRotation.rotation, currRotation)
            val rotationDifference2 = rotationDifference(placeRotation.rotation / 90F, currRotation / 90F)

            val simPlayer = SimulatedPlayer.fromClientPlayer(player.movementInput)
            simPlayer.tick()

            options.instant =
                blockSafe && simPlayer.fallDistance > player.fallDistance + 0.05 && rotationDifference > rotationDifference2 / 2

            setRotation(placeRotation.rotation, if (scaffoldMode == "Telly") 1 else options.resetTicks)
        }

        // 只在非预瞄模式下更新当前placeRotation
        if (!forPreAim) {
            this.placeRotation = placeRotation
        }

        return true
    }
    private fun getOptimizedSamplePoints(neighbor: BlockPos): List<Vec3> {
        val samplePoints = mutableListOf<Vec3>()

        samplePoints.add(Vec3(0.5, 0.5, 0.5))

        samplePoints.addAll(listOf(
            Vec3(0.1, 0.5, 0.5), Vec3(0.9, 0.5, 0.5),
            Vec3(0.5, 0.1, 0.5), Vec3(0.5, 0.9, 0.5),
            Vec3(0.5, 0.5, 0.1), Vec3(0.5, 0.5, 0.9)
        ))

        samplePoints.addAll(listOf(
            Vec3(0.1, 0.1, 0.1), Vec3(0.1, 0.1, 0.9),
            Vec3(0.1, 0.9, 0.1), Vec3(0.1, 0.9, 0.9),
            Vec3(0.9, 0.1, 0.1), Vec3(0.9, 0.1, 0.9),
            Vec3(0.9, 0.9, 0.1), Vec3(0.9, 0.9, 0.9)
        ))

        val block = mc.theWorld?.getBlockState(neighbor)?.block
        when {
            block?.isFullBlock == true -> {
                samplePoints.add(Vec3(0.5, 0.05, 0.5))
                samplePoints.add(Vec3(0.5, 0.95, 0.5))
            }
            block?.isSlab == true -> {
                samplePoints.addAll(listOf(
                    Vec3(0.5, 0.25, 0.5), Vec3(0.5, 0.75, 0.5),
                    Vec3(0.25, 0.5, 0.5), Vec3(0.75, 0.5, 0.5)
                ))
            }
            block?.isStairs == true -> {
                samplePoints.addAll(listOf(
                    Vec3(0.25, 0.5, 0.25), Vec3(0.25, 0.5, 0.75),
                    Vec3(0.75, 0.5, 0.25), Vec3(0.75, 0.5, 0.75)
                ))
            }
            else -> {
                samplePoints.addAll(listOf(
                    Vec3(0.25, 0.25, 0.25), Vec3(0.25, 0.25, 0.75),
                    Vec3(0.25, 0.75, 0.25), Vec3(0.25, 0.75, 0.75),
                    Vec3(0.75, 0.25, 0.25), Vec3(0.75, 0.25, 0.75),
                    Vec3(0.75, 0.75, 0.25), Vec3(0.75, 0.75, 0.75)
                ))
            }
        }

        return samplePoints.distinct()
    }


    private val Block.isSlab: Boolean
        get() = this.javaClass.simpleName.lowercase().contains("slab")


    private val Block.isStairs: Boolean
        get() = this.javaClass.simpleName.lowercase().contains("stair")

    private fun modifyVec(original: Vec3, direction: EnumFacing, pos: Vec3, shouldModify: Boolean): Vec3 {
        if (!shouldModify) {
            return original
        }

        val x = original.xCoord
        val y = original.yCoord
        val z = original.zCoord

        val side = direction.opposite

        return when (side.axis ?: return original) {
            EnumFacing.Axis.Y -> Vec3(x, pos.yCoord + side.directionVec.y.coerceAtLeast(0), z)
            EnumFacing.Axis.X -> Vec3(pos.xCoord + side.directionVec.x.coerceAtLeast(0), y, z)
            EnumFacing.Axis.Z -> Vec3(x, y, pos.zCoord + side.directionVec.z.coerceAtLeast(0))
        }

    }

    private fun findTargetPlace(
        pos: BlockPos, offsetPos: BlockPos, vec3: Vec3, side: EnumFacing, eyes: Vec3, maxReach: Float, raycast: Boolean,
    ): PlaceRotation? {
        val world = mc.theWorld ?: return null

        val vec = (Vec3(pos) + vec3).addVector(
            side.directionVec.x * vec3.xCoord, side.directionVec.y * vec3.yCoord, side.directionVec.z * vec3.zCoord
        )

        val distance = eyes.distanceTo(vec)

        if (raycast && (distance > maxReach || world.rayTraceBlocks(eyes, vec, false, true, false) != null)) {
            return null
        }

        val diff = vec - eyes

        if (side.axis != EnumFacing.Axis.Y) {
            val dist = abs(if (side.axis == EnumFacing.Axis.Z) diff.zCoord else diff.xCoord)

            if (dist < minDist && scaffoldMode != "Telly") {
                return null
            }
        }

        var rotation = toRotation(vec, false)

        val roundYaw90 = round(rotation.yaw / 90f) * 90f
        val roundYaw45 = round(rotation.yaw / 45f) * 45f

        val effectiveMode = if ((mc.thePlayer?.hurtTime ?: 0) > 0 && scaffoldMode !in arrayOf("Expand", "Telly")) {
            "Normal"
        } else {
            modeList.get()
        }

        rotation = when (effectiveMode) {
            "Stabilized" -> Rotation(roundYaw45, rotation.pitch)
            "ReverseYaw" -> Rotation(if (!isLookingDiagonally) roundYaw90 else roundYaw45, rotation.pitch)
            else -> rotation
        }.fixedSensitivity()

        performBlockRaytrace(currRotation, maxReach)?.let { raytrace ->
            if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
                return PlaceRotation(
                    PlaceInfo(
                        raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                    ), currRotation
                )
            }
        }

        val raytrace = performBlockRaytrace(rotation, maxReach) ?: return null

        val multiplier = if (options.legitimize) 3 else 1

        if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite) && canUpdateRotation(
                currRotation, rotation, multiplier
            )
        ) {
            return PlaceRotation(
                PlaceInfo(
                    raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                ), rotation
            )
        }

        return null
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): MovingObjectPosition? {
        val player = mc.thePlayer ?: return null
        val world = mc.theWorld ?: return null

        val eyes = player.eyes
        val rotationVec = getVectorForRotation(rotation)

        val reach = eyes + (rotationVec * maxReach.toDouble())

        return world.rayTraceBlocks(eyes, reach, false, false, true)
    }

    private fun compareDifferences(
        new: PlaceRotation, old: PlaceRotation?, rotation: Rotation = currRotation,
    ): PlaceRotation {
        if (old == null || rotationDifference(new.rotation, rotation) < rotationDifference(old.rotation, rotation)) {
            return new
        }

        return old
    }

    private fun findBlockToSwitchNextTick(stack: ItemStack) {
        if (autoBlock in arrayOf("Off", "Switch")) return

        val switchAmount = if (earlySwitch) amountBeforeSwitch else 0

        if (stack.stackSize > switchAmount) return

        val switchSlot = if (earlySwitch) {
            InventoryUtils.findBlockStackInHotbarGreaterThan(amountBeforeSwitch) ?: InventoryUtils.findBlockInHotbar()
            ?: return
        } else {
            InventoryUtils.findBlockInHotbar()
        } ?: return

        SilentHotbar.selectSlotSilently(this, switchSlot, render = autoBlock == "Pick", resetManually = true)
    }

    private fun updatePlacedBlocksForTelly() {
        if (blocksUntilAxisChange > horizontalPlacements + verticalPlacements) {
            blocksUntilAxisChange = 0

            horizontalPlacements = horizontalPlacementsRange.random()
            verticalPlacements = verticalPlacementsRange.random()
            return
        }

        blocksUntilAxisChange++
    }

    private fun tryToPlaceBlock(
        stack: ItemStack, clickPos: BlockPos, side: EnumFacing, hitVec: Vec3, attempt: Boolean = false,
        onSuccess: () -> Unit = { }
    ): Boolean {
        val thePlayer = mc.thePlayer ?: return false

        val prevSize = stack.stackSize
        val clickedSuccessfully = thePlayer.onPlayerRightClick(clickPos, side, hitVec, stack)

        if (clickedSuccessfully) {
            if (!attempt) {
                delayTimer.reset()

                if (thePlayer.onGround) {
                    thePlayer.motionX *= speedModifier
                    thePlayer.motionZ *= speedModifier
                }
            }

            if (swing) thePlayer.swingItem()
            else sendPacket(C0APacketAnimation())

            if (isManualJumpOptionActive) blocksPlacedUntilJump++

            updatePlacedBlocksForTelly()

            if (stack.stackSize <= 0) {
                thePlayer.inventory.mainInventory[SilentHotbar.currentSlot] = null
                ForgeEventFactory.onPlayerDestroyItem(thePlayer, stack)
            } else if (stack.stackSize != prevSize || mc.playerController.isInCreativeMode) mc.entityRenderer.itemRenderer.resetEquippedProgress()

            placeRotation = null

            placedBlocksWithoutEagle++

            onSuccess()
        } else {
            if (thePlayer.sendUseItem(stack)) mc.entityRenderer.itemRenderer.resetEquippedProgress2()
        }

        return clickedSuccessfully
    }

    fun handleMovementOptions(input: MovementInput) {
        val player = mc.thePlayer ?: return

        if (!state) {
            return
        }

        if (!slow && speedLimiter && MovementUtils.speed > speedLimit) {
            input.moveStrafe = 0f
            input.moveForward = 0f
            return
        }

        when (zitterMode.lowercase()) {
            "off" -> {
                return
            }

            "smooth" -> {
                val notOnGround = !player.onGround || !player.isCollidedVertically

                if (player.onGround) {
                    input.sneak = eagleSneaking || GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
                }

                if (input.jump || mc.gameSettings.keyBindJump.isKeyDown || notOnGround) {
                    zitterTickTimer.reset()

                    if (useSneakMidAir) {
                        input.sneak = true
                    }

                    if (!notOnGround && !input.jump) {
                        input.moveStrafe = if (zitterDirection) 1f else -1f
                    } else {
                        input.moveStrafe = 0f
                    }

                    zitterDirection = !zitterDirection

                    if (mc.gameSettings.keyBindLeft.isKeyDown) {
                        input.moveStrafe++
                    }

                    if (mc.gameSettings.keyBindRight.isKeyDown) {
                        input.moveStrafe--
                    }
                    return
                }

                if (zitterTickTimer.hasTimePassed()) {
                    zitterDirection = !zitterDirection
                    zitterTickTimer.reset()
                } else {
                    zitterTickTimer.update()
                }

                if (zitterDirection) {
                    input.moveStrafe = -1f
                } else {
                    input.moveStrafe = 1f
                }
            }

            "teleport" -> {
                MovementUtils.strafe(zitterSpeed)
                val yaw = (player.rotationYaw + if (zitterDirection) 90.0 else -90.0).toRadians()
                player.motionX -= sin(yaw) * zitterStrength
                player.motionZ += cos(yaw) * zitterStrength
                zitterDirection = !zitterDirection
            }
        }
    }

    private var isOnRightSide = false

    private fun generateGodBridgeRotations(ticks: Int) {
        val player = mc.thePlayer ?: return

        val direction = if (options.applyServerSide) {
            MovementUtils.direction.toDegreesF() + 180f
        } else MathHelper.wrapAngleTo180_float(player.rotationYaw)

        val movingYaw = round(direction / 45) * 45

        val steps45 = arrayListOf(-135f, -45f, 45f, 135f)

        val isMovingStraight = if (options.applyServerSide) {
            movingYaw % 90 == 0f
        } else movingYaw in steps45 && player.movementInput.isSideways

        if (!player.isNearEdge(2.5f)) return

        if (!player.isMoving) {
            placeRotation?.run {
                val axisMovement = floor(this.rotation.yaw / 90) * 90

                val yaw = axisMovement + 45f
                val pitch = 75f

                setRotation(Rotation(yaw, pitch), ticks)
                return
            }

            if (!options.keepRotation) return
        }

        val rotation = if (isMovingStraight) {
            if (player.onGround) {
                isOnRightSide = floor(player.posX + cos(movingYaw.toRadians()) * 0.5) != floor(player.posX) || floor(
                    player.posZ + sin(movingYaw.toRadians()) * 0.5
                ) != floor(player.posZ)

                val posInDirection =
                    BlockPos(player.positionVector.offset(EnumFacing.fromAngle(movingYaw.toDouble()), 0.6))

                val isLeaningOffBlock = player.position.down().block == air
                val nextBlockIsAir = posInDirection.down().block == air

                if (isLeaningOffBlock && nextBlockIsAir) {
                    isOnRightSide = !isOnRightSide
                }
            }

            val side = if (options.applyServerSide) {
                if (isOnRightSide) 45f else -45f
            } else 0f

            Rotation(movingYaw + side, if (useOptimizedPitch) 73.5f else customGodPitch)
        } else {
            Rotation(movingYaw, 75.6f)
        }.fixedSensitivity()

        godBridgeTargetRotation = rotation

        setRotation(rotation, ticks)
    }

    private fun shouldFreeze(): Boolean {
        val player = mc.thePlayer ?: return false
        val canRotateNow = player.airTicks >= ticksUntilRotation.random() + 2 && ticksUntilJump >= jumpTicks
        return canRotateNow && !checkGroundBelow(player, 2.5) && tryAntiFailToPlace && mc.thePlayer.motionY < 0
    }

    private fun checkSpeedModule(): Boolean {
        if (!stopJumpWhenSpeedEnabled) return true
        return !Speed.state
    }

    override val tag
        get() = if (towerMode != "None") ("$scaffoldMode | $towerMode") else scaffoldMode
    private fun predictNextBlockPosition(): BlockPos? {
        val player = mc.thePlayer ?: return null

        if (!player.isMoving || player.fallDistance > 3.0f) return null

        // 根据运动方向预测下一个位置
        val direction = MovementUtils.direction.toDegreesF()
        val rad = Math.toRadians(direction.toDouble())

        // 预测前方位置
        val forwardX = -sin(rad) * preAimDistance
        val forwardZ = cos(rad) * preAimDistance

        // 计算预测位置
        var predictedX = player.posX + forwardX
        var predictedZ = player.posZ + forwardZ

        // 模拟实际脚手架的逻辑来确定Y坐标
        val predictedY = when {
            // 保持同Y模式
            shouldKeepLaunchPosition -> launchY - 1.0

            // 向下搭路模式
            shouldGoDown -> {
                // 模拟shouldGoDown的逻辑
                if (player.posY == player.posY.roundToInt() + 0.5) {
                    player.posY - 0.6  // 注意：这里是-0.6，不是-1.0
                } else {
                    player.posY - 1.6  // 向下搭路时要再下一格
                }
            }

            // 普通模式 - 模拟findBlock函数中的逻辑
            else -> {
                if (player.posY == player.posY.roundToInt() + 0.5) {
                    // 玩家站在方块上半部分
                    BlockPos(player).y.toDouble()
                } else {
                    // 玩家站在方块内或下半部分
                    BlockPos(player).down().y.toDouble()
                }
            }
        }

        // 对于扩展模式，考虑扩展长度
        if (scaffoldMode == "Expand" && expandLength > 1) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw) else player.horizontalFacing.directionVec.x.toDouble()
            val z = if (omniDirectionalExpand) cos(yaw) else player.horizontalFacing.directionVec.z.toDouble()

            predictedX += x * (expandLength - 1)
            predictedZ += z * (expandLength - 1)
        }

        // 添加调试输出（可选）
        if (mark && preAim && player.ticksExisted % 20 == 0) {
            println("[PreAim] Predicted pos: ($predictedX, $predictedY, $predictedZ), Player pos: (${player.posX}, ${player.posY}, ${player.posZ})")
            println("[PreAim] shouldKeepLaunchPosition: $shouldKeepLaunchPosition, launchY: $launchY")
        }

        return BlockPos(predictedX, predictedY, predictedZ)
    }

    private var preAimTimer = 0

    private fun shouldUpdatePreAim(): Boolean {
        if (!preAim) return false
        if (scaffoldMode == "GodBridge" || scaffoldMode == "Telly") return false
        if (shouldGoDown) return false

        preAimTimer++
        if (preAimTimer >= preAimDelay) {
            preAimTimer = 0
            return true
        }
        return false
    }
    data class ExtraClickInfo(val delay: Int, val lastClick: Long, var clicks: Int)
}