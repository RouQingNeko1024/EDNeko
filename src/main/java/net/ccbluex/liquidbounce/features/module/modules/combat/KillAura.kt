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
}

data class SwingFailData(val vec3: Vec3, val startTime: Long)

