package net.ccbluex.liquidbounce.features.module.modules.killaura

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack
import net.ccbluex.liquidbounce.features.module.modules.combat.ForwardTrack
import net.ccbluex.liquidbounce.features.module.modules.combat.KeepSprint
import net.ccbluex.liquidbounce.features.module.modules.combat.NoFriends
import net.ccbluex.liquidbounce.features.module.modules.combat.NoisePresets
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.render.NoSwing
import net.ccbluex.liquidbounce.features.module.modules.world.Fucker
import net.ccbluex.liquidbounce.features.module.modules.world.Nuker
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Tower
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.getAttackCooldownProgress
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.resetLastAttackedTicks
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.ItemUtils.isConsumingItem
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
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
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.*
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
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

object KillAura : Module("KillAura", Category.KILLAURA, Keyboard.KEY_R) {

    val autoBlock get() = KillAuraAutoBlock.autoBlock
    val blinkAutoBlock get() = KillAuraAutoBlock.blinkAutoBlock
    val forceBlockRender get() = KillAuraAutoBlock.forceBlockRender

    var target: EntityLivingBase? = null
    private var lastTarget: EntityLivingBase? = null
    var hittable = false
    private val prevTargetEntities = mutableListOf<Int>()

    private val attackTimer = MSTimer()
    private var attackDelay = 0
    var clicks = 0
    private var attackTickTimes = mutableListOf<Pair<MovingObjectPosition, Int>>()

    private var containerOpen = -1L

    var renderBlocking = false
    var blockStatus = false
    private var blockStopInDead = false
    private var blockOnNoHitDelayTick = 0

    private var hurtTimeBlocking = false
    private var hurtTimeBlockStartTick = 0L
    private var hurtTimeWaitStartTick = 0L
    private var hurtTimeWaitTicksCalc = 0
    private var hurtTimeWaitingToBlock = false
    private var hurtTimeLastHurtTick = 0L
    private var hurtTimeCurrentTick = 0L
    private var hurtTimePrevHurtTime = 0
    private val hurtTimeDamageIntervals = mutableListOf<Long>()

    private val switchTimer = MSTimer()

    private var blinked = false

    private val swingFails = mutableListOf<SwingFailData>()

    private var espStart = 0.0
    private var espDirection = 1.0
    private var espYPos = 0.0
    private var prevEspYPos = 0.0
    private var espProgress = 0.0
    private var espAl = 0f
    private var espLastMS = System.currentTimeMillis()
    private var espLastDeltaMS = 0L

    override fun onToggle(state: Boolean) {
        target = null
        hittable = false
        prevTargetEntities.clear()
        attackTickTimes.clear()
        attackTimer.reset()
        clicks = 0
        KillAuraDebug.reset()

        if (KillAuraAutoBlock.blinkAutoBlock) {
            BlinkUtils.unblink()
            blinked = false
        }

        if (KillAuraBypass.autoF5) mc.gameSettings.thirdPersonView = 0

        if (KillAuraAutoBlock.autoBlock == "BlockOnNoHit" && KillAuraAutoBlock.blockOnNoHitMode == "RightClick") {
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
        if (cancelRun || (KillAuraBypass.noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < KillAuraBypass.noInventoryDelay))) return

        lastTarget?.let {
            if (it.isDead || it.health <= 0) {
                EventManager.call(EntityKilledEvent(it))
                lastTarget = null
            }
        }

        updateTarget()

        target?.let { lastTarget = it }

        if (KillAuraBypass.autoF5) {
            if (mc.gameSettings.thirdPersonView != 1 && target != null) {
                mc.gameSettings.thirdPersonView = 1
            }
        }

        KillAuraDebug.onTick(
            target, hittable, KillAuraRange.range, KillAuraRange.maxRange,
            KillAuraCPS.effectiveCPS, KillAuraAutoBlock.autoBlock, blockStatus, renderBlocking,
            KillAuraRotations.noiseFunction
        )
    }

    val onWorld = handler<WorldEvent> {
        attackTickTimes.clear()

        if (KillAuraAutoBlock.blinkAutoBlock && BlinkUtils.isBlinking) BlinkUtils.unblink()

        synchronized(swingFails) {
            swingFails.clear()
        }
    }

    val onTick = handler<GameTickEvent>(priority = 2) {
        val player = mc.thePlayer ?: return@handler

        NoisePresets.tick()
        KillAuraRotations.noiseFunction = generateNoise()
        KillAuraCPS.tickCPS()

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

        if (KillAuraCPS.clickOnly && !mc.gameSettings.keyBindAttack.isKeyDown) {
            clicks = 0
            return@handler
        }

        if (blockStatus && (KillAuraAutoBlock.autoBlock == "Packet" || KillAuraAutoBlock.autoBlock == "QuickMacro") && KillAuraAutoBlock.releaseAutoBlock && !KillAuraAutoBlock.ignoreTickRule) {
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

        if (KillAuraBypass.noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < KillAuraBypass.noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return@handler
        }

        if (KillAuraCPS.simulateCooldown && getAttackCooldownProgress() < 1f) {
            return@handler
        }

        if (target == null && !blockStopInDead) {
            blockStopInDead = true
            if (KillAuraAutoBlock.autoBlock == "BlockOnNoHit") {
                val player = mc.thePlayer
                if (player != null && player.heldItem?.item is ItemSword && !blockStatus) {
                    if (blockOnNoHitDelayTick >= KillAuraAutoBlock.blockOnNoHitDelay) {
                        when (KillAuraAutoBlock.blockOnNoHitMode) {
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

        if (KillAuraAutoBlock.blinkAutoBlock) {
            when (player.ticksExisted % (KillAuraAutoBlock.blinkBlockTicks + 1)) {
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
                KillAuraAutoBlock.blinkBlockTicks -> {
                    if (!blockStatus && blinked && BlinkUtils.isBlinking) {
                        BlinkUtils.unblink()
                        blinked = false
                        startBlocking(target!!, KillAuraAutoBlock.interactAutoBlock, KillAuraAutoBlock.autoBlock == "Fake")
                    }
                }
            }
        }

        if (target != null) {
            if (player.getDistanceToEntityBox(target!!) > KillAuraAutoBlock.blockMaxRange && blockStatus) {
                stopBlocking(true)
                return@handler
            } else {
                if (KillAuraAutoBlock.autoBlock != "Off" && !KillAuraAutoBlock.releaseAutoBlock) {
                    renderBlocking = true
                }
                if (KillAuraAutoBlock.autoBlock == "BlockOnNoHit" && !blockStatus && player.heldItem?.item is ItemSword) {
                    if (blockOnNoHitDelayTick >= KillAuraAutoBlock.blockOnNoHitDelay) {
                        when (KillAuraAutoBlock.blockOnNoHitMode) {
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

            val extraClicks = if (KillAuraCPS.simulateDoubleClicking && !KillAuraCPS.simulateCooldown) nextInt(-1, 1) else 0

            val generatedClicks = if (KillAuraBypass.generateClicksBasedOnDist) {
                val distance = player.getDistanceToEntityBox(target!!)
                ((distance / KillAuraBypass.distanceFactor.random()) * KillAuraBypass.cpsMultiplier.random()).roundToInt()
            } else 0

            var maxClicks = clicks + extraClicks + generatedClicks

            val prevHittable = hittable

            updateHittable()

            if (!prevHittable && hittable && maxClicks == 0 && KillAuraBypass.forceFirstHit) {
                maxClicks++
            }

            repeat(maxClicks) {
                val wasBlocking = blockStatus

                runAttack(it == 0, it + 1 == maxClicks)
                clicks--

                if (wasBlocking && !blockStatus && (KillAuraAutoBlock.releaseAutoBlock && !KillAuraAutoBlock.ignoreTickRule || KillAuraAutoBlock.autoBlock == "Off")) {
                    return@handler
                }
            }
        } else {
            renderBlocking = false
        }
    }

    val onRender3D = handler<Render3DEvent> { event ->
        handleFailedSwings()

        drawAimPointBox()

        if (cancelRun) {
            target = null
            hittable = false
            return@handler
        }

        if (KillAuraBypass.noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < KillAuraBypass.noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return@handler
        }

        if (KillAuraVisuals.rangeCircle) {
            renderRangeCircle()
        }

        target ?: return@handler

        if (attackTimer.hasTimePassed(attackDelay)) {
            if (KillAuraCPS.effectiveCPS.last > 0) clicks++
            attackTimer.reset()

            attackDelay = randomClickDelay(KillAuraCPS.effectiveCPS.first, KillAuraCPS.effectiveCPS.last)
        }

        val hittableColor = if (hittable) Color(37, 126, 255, 70) else Color(255, 0, 0, 70)

        if (KillAuraTargeting.targetMode != "Multi") {
            target ?: return@handler

            if (KillAuraVisuals.markPlatform) drawPlatform(target!!, hittableColor)
            if (KillAuraVisuals.markBox) drawEntityBox(target!!, hittableColor, KillAuraVisuals.boxOutline)
            if (KillAuraVisuals.markCircle) drawCircle(
                target!!,
                KillAuraVisuals.duration * 1000F,
                KillAuraVisuals.heightRange.takeIf { KillAuraVisuals.animateHeight } ?: (KillAuraVisuals.heightRange.endInclusive..KillAuraVisuals.heightRange.endInclusive),
                if (KillAuraVisuals.customCircleSize) KillAuraVisuals.circleSize else KillAuraVisuals.extraWidth,
                KillAuraVisuals.fillInnerCircle,
                KillAuraVisuals.withHeight,
                KillAuraVisuals.circleYRange.takeIf { KillAuraVisuals.animateCircleY },
                KillAuraVisuals.circleStartColor.rgb,
                KillAuraVisuals.circleEndColor.rgb
            )

            if (KillAuraVisuals.markJello) renderJelloESP(event)
            if (KillAuraVisuals.markZavz) renderZavzESP(event)
            if (KillAuraVisuals.markZywl) renderZywlESP(event)
            if (KillAuraVisuals.markSigma) renderSigmaESP(event)
            if (KillAuraVisuals.markFDP) renderFdPESP(event)
            if (KillAuraVisuals.markTracers) renderTracersESP(event)
            if (KillAuraVisuals.markLies) renderLiesESP(event)
            if (KillAuraVisuals.markSims) renderSimsESP(event)
        }
    }

    private fun runAttack(isFirstClick: Boolean, isLastClick: Boolean) {
        val currentTarget = this.target ?: return

        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (KillAuraBypass.noConsumeAttack == "NoHits" && isConsumingItem()) {
            return
        }

        if (KillAuraAutoBlock.autoBlock == "BlockOnNoHit" && KillAuraAutoBlock.cancelAttackWhenBlocking && blockStatus) {
            return
        }

        val multi = KillAuraTargeting.targetMode == "Multi"
        val manipulateInventory = KillAuraBypass.simulateClosingInventory && !KillAuraBypass.noInventoryAttack && serverOpenInventory

        if (hittable && currentTarget.hurtTime > KillAuraCPS.hurtTime) {
            return
        }

        if (!hittable && KillAuraRotations.options.rotationsActive) {
            if (KillAuraBypass.swing && KillAuraBypass.failSwing) {
                val rotation = currentRotation ?: player.rotation

                if (rotationDifference(rotation) > KillAuraBypass.maxRotationDifferenceToSwing) {
                    val shouldIgnore = KillAuraBypass.swingWhenTicksLate.isActive() && ticksSinceClick() >= KillAuraBypass.ticksLateToSwing

                    if (!shouldIgnore) {
                        return
                    }
                }

                runWithModifiedRaycastResult(rotation, KillAuraRange.range.toDouble(), KillAuraRange.throughWallsRange.toDouble()) {
                    if (KillAuraBypass.swingOnlyInAir && !it.typeOfHit.isMiss) {
                        return@runWithModifiedRaycastResult
                    }

                    if (KillAuraBypass.respectMissCooldown && ticksSinceClick() <= 1 && it.typeOfHit.isMiss) {
                        return@runWithModifiedRaycastResult
                    }

                    val shouldEnterBlockBreakProgress =
                        !shouldDelayClick(it.typeOfHit) || attackTickTimes.lastOrNull()?.first?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK

                    if (shouldEnterBlockBreakProgress) {
                        if (manipulateInventory && isFirstClick) serverOpenInventory = false
                    }

                    val prevCooldown = mc.leftClickCounter

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

                            if (entity is EntityLivingBase && isSelected(entity, true)) {
                                attackEntity(entity, isLastClick)
                            } else attackTickTimes -= it to runTimeTicks
                        } else {
                            mc.clickMouse()
                            KillAuraCPS.onMiss()

                            if (KillAuraBypass.renderBoxOnSwingFail) {
                                synchronized(swingFails) {
                                    val centerDistance = (currentTarget.hitBox.center - player.eyes).lengthVector()
                                    val spot = player.eyes + getVectorForRotation(rotation) * centerDistance

                                    swingFails += SwingFailData(spot, System.currentTimeMillis())
                                }
                            }
                        }
                    }

                    if (shouldEnterBlockBreakProgress && isLastClick) {
                        mc.sendClickBlockToController(true)
                        nextTick {
                            mc.sendClickBlockToController(false)
                            clicks = 0
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

                    if (KillAuraTargeting.limitedMultiTargets != 0 && KillAuraTargeting.limitedMultiTargets <= targets) break
                }
            }
        }

        if (!isLastClick) return

        val switchMode = KillAuraTargeting.targetMode == "Switch"

        if (!switchMode || switchTimer.hasTimePassed(KillAuraTargeting.switchDelay)) {
            prevTargetEntities += currentTarget.entityId

            if (switchMode) {
                switchTimer.reset()
            }
        }

        if (manipulateInventory) serverOpenInventory = true
    }

    private fun updateTarget() {
        if (shouldPrioritize()) return

        target = null

        val switchMode = KillAuraTargeting.targetMode == "Switch"

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

            if (switchMode && distance > KillAuraRange.range && prevTargetEntities.isNotEmpty()) continue

            val entityFov = rotationDifference(entity)

            if (distance > KillAuraRange.maxRange || KillAuraTargeting.fov != 180F && entityFov > KillAuraTargeting.fov) continue

            if (switchMode && !isLookingOnEntities(entity, KillAuraTargeting.maxSwitchFOV.toDouble())) continue

            val currentValue = when (KillAuraTargeting.priority.lowercase()) {
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

    private fun attackEntity(entity: EntityLivingBase, isLastClick: Boolean) {
        val thePlayer = mc.thePlayer

        if (shouldPrioritize()) return

        if (thePlayer.isBlocking && (KillAuraAutoBlock.autoBlock == "Off" && blockStatus || (KillAuraAutoBlock.autoBlock == "Packet" || KillAuraAutoBlock.autoBlock == "QuickMacro") && KillAuraAutoBlock.releaseAutoBlock)) {
            stopBlocking()

            if (!KillAuraAutoBlock.ignoreTickRule || KillAuraAutoBlock.autoBlock == "Off") {
                return
            }
        }

        if (shouldDelayClick(MovingObjectPosition.MovingObjectType.ENTITY)) {
            return
        }

        if (!KillAuraAutoBlock.blinkAutoBlock || !BlinkUtils.isBlinking) {
            val affectSprint = false.takeIf { KeepSprint.handleEvents() || KillAuraBypass.keepSprint }

            thePlayer.attackEntityWithModifiedSprint(entity, affectSprint) {
                val noSwingActive = NoSwing.handleEvents()
                val shouldRender = !noSwingActive || !NoSwing.clientSide
                val shouldSendPacket = !noSwingActive || NoSwing.serverSide

                if (KillAuraBypass.swing && shouldRender) {
                    thePlayer.swingItem()
                } else if (shouldSendPacket) {
                    sendPacket(C0APacketAnimation())
                }
            }

            if (KillAuraAutoBlock.autoBlock == "BlockOnNoHit") {
                blockOnNoHitDelayTick = 0
            }

            if (EnchantmentHelper.getModifierForCreature(
                    thePlayer.heldItem, entity.creatureAttribute
                ) <= 0F && KillAuraBypass.fakeSharp
            ) {
                thePlayer.onEnchantmentCritical(entity)
            }

            KillAuraDebug.onAttack(
                entity,
                KillAuraRotations.noiseFunction?.invoke(Rotation(0f, 0f)),
                KillAuraRotations.particleCount.random(),
                KillAuraRotations.swarmCognitive.random(),
                KillAuraRotations.annealingInitialTemp.random(),
                KillAuraRotations.geneticCrossoverRate.random()
            )

            KillAuraCPS.onHit()
        }

        if (KillAuraAutoBlock.autoBlock != "Off" && (thePlayer.isBlocking || canBlock) && (!KillAuraAutoBlock.blinkAutoBlock && isLastClick || KillAuraAutoBlock.blinkAutoBlock && (!blinked || !BlinkUtils.isBlinking))) {
            startBlocking(entity, KillAuraAutoBlock.interactAutoBlock, KillAuraAutoBlock.autoBlock == "Fake")
        }

        resetLastAttackedTicks()
    }

    private fun updateRotations(entity: Entity): Boolean {
        val player = mc.thePlayer ?: return false

        if (shouldPrioritize()) return false

        if (!KillAuraRotations.options.rotationsActive) {
            return player.getDistanceToEntityBox(entity) <= KillAuraRange.range
        }

        val prediction = entity.currPos.subtract(entity.prevPos).times(2 + KillAuraPrediction.predictEnemyPosition.toDouble())

        val boundingBox = entity.hitBox.offset(prediction)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)

        simPlayer.rotationYaw = (currentRotation ?: player.rotation).yaw

        var pos = currPos

        repeat(KillAuraPrediction.predictClientMovement) {
            val previousPos = simPlayer.pos

            simPlayer.tick()

            if (KillAuraPrediction.predictOnlyWhenOutOfRange) {
                player.setPosAndPrevPos(simPlayer.pos)

                val currDist = player.getDistanceToEntityBox(entity)

                player.setPosAndPrevPos(previousPos)

                val prevDist = player.getDistanceToEntityBox(entity)

                player.setPosAndPrevPos(currPos, oldPos)
                pos = simPlayer.pos

                if (currDist <= KillAuraRange.range && currDist <= prevDist) {
                    return@repeat
                }
            }

            pos = previousPos
        }

        player.setPosAndPrevPos(pos)

        val rotation = searchCenter(
            boundingBox,
            KillAuraBypass.generateSpotBasedOnDistance,
            KillAuraRotations.outBorder && !attackTimer.hasTimePassed(attackDelay / 2),
            KillAuraRotations.randomization,
            predict = false,
            lookRange = KillAuraRange.range + KillAuraRange.scanRange,
            attackRange = KillAuraRange.range,
            throughWallsRange = KillAuraRange.throughWallsRange,
            bodyPoints = listOf(KillAuraRotations.highestBodyPointToTarget, KillAuraRotations.lowestBodyPointToTarget),
            horizontalSearch = KillAuraRotations.horizontalBodySearchRange
        )

        if (rotation == null) {
            player.setPosAndPrevPos(currPos, oldPos)

            return false
        }

        setTargetRotation(rotation, options = KillAuraRotations.options)

        player.setPosAndPrevPos(currPos, oldPos)

        return true
    }

    private fun ticksSinceClick() = runTimeTicks - (attackTickTimes.lastOrNull()?.second ?: 0)

    private fun updateHittable() {
        val eyes = mc.thePlayer.eyes

        val currentRotation = currentRotation ?: mc.thePlayer.rotation
        val target = this.target ?: return

        if (shouldPrioritize()) return

        if (!KillAuraRotations.options.rotationsActive) {
            hittable = mc.thePlayer.getDistanceToEntityBox(target) <= KillAuraRange.range
            return
        }

        var chosenEntity: Entity? = null

        if (KillAuraRotations.raycast) {
            chosenEntity = raycastEntity(
                KillAuraRange.range.toDouble(), currentRotation.yaw, currentRotation.pitch
            ) { entity -> !KillAuraRotations.livingRaycast || entity is EntityLivingBase && entity !is EntityArmorStand }

            if (chosenEntity != null && chosenEntity is EntityLivingBase && (NoFriends.handleEvents() || !(chosenEntity is EntityPlayer && chosenEntity.isClientFriend()))) {
                if (KillAuraRotations.raycastIgnored && target != chosenEntity) {
                    this.target = chosenEntity
                }
            }

            hittable = this.target == chosenEntity
        } else {
            hittable = isRotationFaced(target, KillAuraRange.range.toDouble(), currentRotation)
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

        val intercept = targetToCheck.hitBox.calculateIntercept(
            eyes, eyes + getVectorForRotation(currentRotation) * KillAuraRange.range.toDouble()
        )

        hittable =
            isVisible(intercept.hitVec) || mc.thePlayer.getDistanceToEntityBox(targetToCheck) <= KillAuraRange.throughWallsRange
    }

    private fun startBlocking(interactEntity: Entity, interact: Boolean, fake: Boolean = false) {
        val player = mc.thePlayer ?: return

        if (blockStatus && (!KillAuraAutoBlock.uncpAutoBlock || !KillAuraAutoBlock.blinkAutoBlock) || shouldPrioritize()) return

        if (mc.thePlayer.isBlocking) {
            blockStatus = true
            renderBlocking = true
            return
        }

        if (KillAuraAutoBlock.unblockMode == "Empty" && player.inventory.firstEmptyStack !in 0..8) {
            return
        }

        if (!fake) {
            if (!(KillAuraAutoBlock.blockRate > 0 && nextInt(endExclusive = 100) <= KillAuraAutoBlock.blockRate)) return

            if (interact) {
                val positionEye = player.eyes

                val boundingBox = interactEntity.hitBox

                val (yaw, pitch) = currentRotation ?: player.rotation

                val vec = getVectorForRotation(Rotation(yaw, pitch))

                val lookAt = positionEye.add(vec * KillAuraRange.maxRange.toDouble())

                val movingObject = boundingBox.calculateIntercept(positionEye, lookAt) ?: return
                val hitVec = movingObject.hitVec

                sendPackets(
                    C02PacketUseEntity(interactEntity, hitVec - interactEntity.positionVector),
                    C02PacketUseEntity(interactEntity, INTERACT)
                )
            }

            if (KillAuraAutoBlock.switchStartBlock) {
                switchToSlot((SilentHotbar.currentSlot + 1) % 9)
            }

            if (KillAuraAutoBlock.autoBlock == "QuickMacro") {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -2, -1), 255, null, 0.0f, 0.0f, 0.0f))
            } else {
                sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
            }
            blockStatus = true
        }

        renderBlocking = true

        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
    }

    private fun stopBlocking(forceStop: Boolean = false) {
        val player = mc.thePlayer ?: return

        if (!forceStop) {
            if (blockStatus && !mc.thePlayer.isBlocking) {

                when (KillAuraAutoBlock.unblockMode.lowercase()) {
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

        if (KillAuraAutoBlock.autoBlock == "Off" || !KillAuraAutoBlock.blinkAutoBlock || !blinked) return@handler

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

    private fun shouldDelayClick(currentType: MovingObjectPosition.MovingObjectType): Boolean {
        if (!KillAuraBypass.useHitDelay) {
            return false
        }

        val lastAttack = attackTickTimes.lastOrNull()

        return lastAttack != null && lastAttack.first.typeOfHit != currentType && runTimeTicks - lastAttack.second <= KillAuraBypass.hitDelayTicks
    }

    private fun checkIfAimingAtBox(
        targetToCheck: Entity, currentRotation: Rotation, eyes: Vec3, onSuccess: () -> Unit,
        onFail: () -> Unit = { },
    ) {
        if (targetToCheck.hitBox.isVecInside(eyes)) {
            onSuccess()
            return
        }

        val intercept = targetToCheck.hitBox.calculateIntercept(
            eyes, eyes + getVectorForRotation(currentRotation) * KillAuraRange.range.toDouble()
        )

        if (intercept != null) {
            hittable =
                isVisible(intercept.hitVec) || mc.thePlayer.getDistanceToEntityBox(targetToCheck) <= KillAuraRange.throughWallsRange

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
        !KillAuraBypass.onScaffold && (Scaffold.handleEvents() && (Scaffold.placeRotation != null || currentRotation != null) || Tower.handleEvents() && Tower.isTowering) -> true
        !KillAuraBypass.onDestroyBlock && (Fucker.handleEvents() && !Fucker.noHit && Fucker.pos != null && !Fucker.isOwnBed || Nuker.handleEvents()) -> true
        KillAuraCPS.activationSlot && SilentHotbar.currentSlot != KillAuraCPS.preferredSlot - 1 -> true
        else -> false
    }

    private fun handleFailedSwings() {
        if (!KillAuraBypass.renderBoxOnSwingFail) return

        val box = AxisAlignedBB(0.0, 0.0, 0.0, 0.05, 0.05, 0.05)

        synchronized(swingFails) {
            val fadeSeconds = KillAuraBypass.renderBoxFadeSeconds * 1000L
            val colorSettings = KillAuraBypass.renderBoxColor

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

        if (!KillAuraRenderAimPointBox.renderPointBoxAim) {
            return
        }

        val f = KillAuraRenderAimPointBox.aimPointBoxSize.toDouble()

        val box = AxisAlignedBB(0.0, 0.0, 0.0, f, f, f)

        val renderManager = mc.renderManager

        Backtrack.runWithSimulatedPosition(player, player.interpolatedPosition(player.prevPos)) {
            Backtrack.runWithSimulatedPosition(target, target.interpolatedPosition(target.prevPos)) {
                val rotationVec = player.eyes + getVectorForRotation(
                    serverRotation.lerpWith(currentRotation ?: player.rotation, mc.timer.renderPartialTicks)
                ) * player.getDistanceToEntityBox(target).coerceAtMost(KillAuraRange.range.toDouble())

                val offSetBox = box.offset(rotationVec - renderManager.renderPos)

                RenderUtils.drawAxisAlignedBB(offSetBox, KillAuraRenderAimPointBox.aimPointBoxColor)
            }
        }
    }

    private val cancelRun
        inline get() = mc.thePlayer.isSpectator || !isAlive(mc.thePlayer) || KillAuraBypass.noConsumeAttack == "NoRotation" && isConsumingItem()

    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0

    private val canBlock: Boolean
        get() {
            val player = mc.thePlayer ?: return false

            if (target != null && player.heldItem?.item is ItemSword) {
                if (KillAuraAutoBlock.smartAutoBlock) {
                    if (player.isMoving && KillAuraAutoBlock.forceBlock) return false

                    if (KillAuraAutoBlock.checkWeapon && target?.heldItem?.item !is ItemSword && target?.heldItem?.item !is ItemAxe) return false

                    if (player.hurtTime > KillAuraAutoBlock.maxOwnHurtTime) return false

                    val rotationToPlayer = toRotation(player.hitBox.center, true, target!!)

                    if (rotationDifference(rotationToPlayer, target!!.rotation) > KillAuraAutoBlock.maxDirectionDiff) return false

                    if (target!!.swingProgressInt > KillAuraAutoBlock.maxSwingProgress) return false

                    if (target!!.getDistanceToEntityBox(player) > KillAuraRange.blockRange) return false
                }

                if (player.getDistanceToEntityBox(target!!) > KillAuraAutoBlock.blockMaxRange) return false

                return true
            }

            return false
        }

    private fun getRange(entity: Entity) =
        (if (mc.thePlayer.getDistanceToEntityBox(entity) >= KillAuraRange.throughWallsRange) KillAuraRange.range + KillAuraRange.scanRange else KillAuraRange.throughWallsRange) - if (mc.thePlayer.isSprinting) KillAuraRange.rangeSprintReduction else 0F

    override val tag
        get() = KillAuraTargeting.targetMode

    val isBlockingChestAura
        get() = handleEvents() && target != null

    private fun getESPColor(entity: Entity): Color {
        if (entity is EntityLivingBase) {
            if (KillAuraVisuals.espColorMode.equals("Health", ignoreCase = true)) {
                val health = entity.health / entity.maxHealth
                return Color(
                    (1.0 - health).toFloat().coerceIn(0f, 1f),
                    health.toFloat().coerceIn(0f, 1f),
                    0f,
                    KillAuraVisuals.espColorAlpha / 255f
                )
            }
        }
        return when (KillAuraVisuals.espColorMode.lowercase()) {
            "custom" -> Color(KillAuraVisuals.espColorRed, KillAuraVisuals.espColorGreen, KillAuraVisuals.espColorBlue, KillAuraVisuals.espColorAlpha)
            "rainbow" -> ColorUtils.rainbow().let { Color(it.red, it.green, it.blue, KillAuraVisuals.espColorAlpha) }
            else -> Color(KillAuraVisuals.espColorRed, KillAuraVisuals.espColorGreen, KillAuraVisuals.espColorBlue, KillAuraVisuals.espColorAlpha)
        }
    }

    private fun easeInOutQuad(x: Double): Double {
        return if (x < 0.5) 2 * x * x else 1 - (-2 * x + 2).pow(2) / 2
    }

    private fun easeInOutQuart(x: Double): Double {
        return if (x < 0.5) 8 * x * x * x * x else 1 - (-2 * x + 2).pow(4) / 2
    }

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

    private fun post3DESP() {
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
        GL11.glColor4f(1f, 1f, 1f, 1f)
    }

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

    private fun renderJelloESP(event: Render3DEvent) {
        val targetEntity = target ?: return

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
        val radius = (bb.maxX - bb.minX) / 2.0
        val height = bb.maxY - bb.minY
        val posX = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * mc.timer.renderPartialTicks
        val posY = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * mc.timer.renderPartialTicks
        val posZ = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * mc.timer.renderPartialTicks

        espYPos = easeInOutQuart(espProgress) * height
        val deltaY = (espYPos - prevEspYPos) * -espDirection * KillAuraVisuals.jelloGradientHeight
        prevEspYPos = espYPos

        espAl = animate(espAl, KillAuraVisuals.jelloFadeSpeed, 0f, 1f)

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
            GL11.glColor4f(r, g, b, espAl * KillAuraVisuals.jelloAlpha)
            GL11.glVertex3d(posX2, posY + espYPos, posZ2)
        }
        GL11.glEnd()

        drawJelloCircle(posX, posY + espYPos, posZ, KillAuraVisuals.jelloWidth, radius, r, g, b, espAl)

        post3DESP()
    }

    private fun animate(current: Float, speed: Float, min: Float, max: Float): Float {
        return (current + speed).coerceIn(min, max)
    }

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

        val bb = targetEntity.entityBoundingBox
        val radius = (bb.maxX - bb.minX) / 2.0
        val precision = 360
        var startPos = espStart % 360
        espStart += KillAuraVisuals.zavzSpeed

        val color = getESPColor(targetEntity)

        for (i in 0..precision) {
            val posX = x + radius * cos(startPos + i * Math.PI * 2 / (precision / 2.0))
            val posZ = z + radius * sin(startPos + i * Math.PI * 2 / (precision / 2.0))
            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
            GL11.glVertex3d(posX, y, posZ)
            y += targetEntity.height / precision
        }

        GL11.glEnd()
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)

        if (KillAuraVisuals.zavzDual) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(false)
            GL11.glLineWidth(2f)
            GL11.glBegin(GL11.GL_LINE_STRIP)

            startPos = espStart % 360
            espStart += KillAuraVisuals.zavzSpeed
            y = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * ticks - mc.renderManager.viewerPosY + targetEntity.height

            for (i in 0..precision) {
                val posX = x + radius * cos(-(startPos + i * Math.PI * 2 / (precision / 2.0)))
                val posZ = z + radius * sin(-(startPos + i * Math.PI * 2 / (precision / 2.0)))
                GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
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

    private fun renderZywlESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val ticks = event.partialTicks

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)

        renderZywlRing(targetEntity, ticks, false)
        if (KillAuraVisuals.zavzDual) renderZywlRing(targetEntity, ticks, true)

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun renderZywlRing(targetEntity: EntityLivingBase, ticks: Float, dualRing: Boolean) {
        val x = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * ticks - mc.renderManager.viewerPosX
        val z = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * ticks - mc.renderManager.viewerPosZ
        var y = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * ticks - mc.renderManager.viewerPosY

        val bb = targetEntity.entityBoundingBox
        val radius = (bb.maxX - bb.minX) / 2.0
        val precision = 360
        var startPos = espStart % 360
        espStart += KillAuraVisuals.zavzSpeed

        val baseColor = getESPColor(targetEntity)

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
            val alpha = if (dualRing) 0 else baseColor.alpha
            val color = ColorUtils.interpolateColor(baseColor, Color.BLACK, offset.toFloat())

            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, alpha / 255f)
            GL11.glVertex3d(posX, y, posZ)

            y += if (dualRing) -targetEntity.height / precision else targetEntity.height / precision
        }

        GL11.glEnd()
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glPopMatrix()
    }

    private fun renderSigmaESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val drawTime = System.currentTimeMillis() % 2000
        val drawMode = drawTime > 1000
        var drawPercent = drawTime / 1000.0

        drawPercent = if (!drawMode) 1 - drawPercent else drawPercent - 1
        drawPercent = easeInOutQuad(drawPercent)

        val points = mutableListOf<Vec3>()
        val bb = targetEntity.entityBoundingBox
        val radius = (bb.maxX - bb.minX) / 2.0
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
        val color = getESPColor(targetEntity)

        for (i in 0..20) {
            var moveFace = (height / 60.0) * i * baseMove
            if (drawMode) moveFace = -moveFace

            val firstPoint = points[0]
            GL11.glVertex3d(
                firstPoint.xCoord - mc.renderManager.viewerPosX,
                firstPoint.yCoord - moveFace - min - mc.renderManager.viewerPosY,
                firstPoint.zCoord - mc.renderManager.viewerPosZ
            )
            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0.7f * (i / 20f))

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
        val radius = ((bb.maxX - bb.minX) / 2.0).toFloat()

        val x = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ

        GL11.glLineWidth(radius * 8f)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        val baseColor = getESPColor(targetEntity)

        for (i in 0..360 step 10) {
            val hue = if (i < 180) i / 180f else (-(i - 360)) / 180f
            val color = if (KillAuraVisuals.espColorMode.equals("Custom", true)) {
                baseColor
            } else {
                Color.getHSBColor(hue, 0.7f, 1f)
            }
            GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, baseColor.alpha / 255f)
            GL11.glVertex3d(x - sin(i * Math.PI / 180.0) * radius, y, z + cos(i * Math.PI / 180.0) * radius)
        }

        GL11.glEnd()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun renderTracersESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val player = mc.thePlayer ?: return

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(KillAuraVisuals.tracersThickness)
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
        val radius = (bb.maxX - bb.minX) / 2.0
        val height = (bb.maxY - bb.minY).toFloat()
        val x = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = (targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY) + height * drawPercent
        val z = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ

        val eased = ((height / 3) * (if (drawPercent > 0.5) (1 - drawPercent) else drawPercent) * (if (drawMode) -1 else 1)).toFloat()
        val baseColor = getESPColor(targetEntity)

        for (i in 5..360 step 5) {
            val color = if (KillAuraVisuals.espColorMode.equals("Custom", true)) {
                baseColor
            } else {
                Color.getHSBColor(
                    if (i < 180) i / 180f else (-(i - 360)) / 180f,
                    0.7f,
                    1f
                )
            }
            val x1 = x - sin(i * Math.PI / 180.0) * radius
            val z1 = z + cos(i * Math.PI / 180.0) * radius
            val x2 = x - sin((i - 5) * Math.PI / 180.0) * radius
            val z2 = z + cos((i - 5) * Math.PI / 180.0) * radius

            GL11.glBegin(GL11.GL_QUADS)
            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0f)
            GL11.glVertex3d(x1, y + eased, z1)
            GL11.glVertex3d(x2, y + eased, z2)
            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, baseColor.alpha / 255f)
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

    private fun renderSimsESP(event: Render3DEvent) {
        val targetEntity = target ?: return
        val color = getESPColor(targetEntity)
        val hurtColor = if (targetEntity.hurtTime <= 0) color else Color(255, 0, 0, color.alpha)

        val x = targetEntity.lastTickPosX + (targetEntity.posX - targetEntity.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = targetEntity.lastTickPosY + (targetEntity.posY - targetEntity.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = targetEntity.lastTickPosZ + (targetEntity.posZ - targetEntity.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        val radius = targetEntity.width / 2.0

        GL11.glPushMatrix()
        GL11.glTranslated(x, y + 2, z)
        GL11.glRotatef(-targetEntity.width, 0f, 1f, 0f)

        GL11.glColor4f(hurtColor.red / 255f, hurtColor.green / 255f, hurtColor.blue / 255f, hurtColor.alpha / 255f)
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

        GL11.glLineWidth(KillAuraVisuals.rangeCircleThickness)
        GL11.glColor4f(KillAuraVisuals.rangeCircleRed / 255.0f, KillAuraVisuals.rangeCircleGreen / 255.0f, KillAuraVisuals.rangeCircleBlue / 255.0f, KillAuraVisuals.rangeCircleAlpha / 255.0f)

        GL11.glRotatef(90f, 1f, 0f, 0f)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        val attackRange = KillAuraRange.range.toDouble().toFloat()
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
        val presets = KillAuraRotations.noisePreset.get()
        if (presets.isEmpty() || (presets.size == 1 && presets.contains("Custom"))) return null

        val customNoise = { r: Rotation ->
            val yawOffset = (KillAuraRotations.yawMicroJitter.random() - KillAuraRotations.yawMicroJitter.start) * 0.25f +
                (KillAuraRotations.pitchMicroJitter.random() * (KillAuraRotations.sineAmplitude.random() * 0.05f)) +
                (KillAuraRotations.ouSigma.random() * 0.02f)
            val pitchOffset = (KillAuraRotations.pitchMicroJitter.random() - KillAuraRotations.pitchMicroJitter.start) * 0.25f +
                (KillAuraRotations.ouTheta.random() * 0.02f)
            Rotation(r.yaw + yawOffset, r.pitch + pitchOffset)
        }

        return NoisePresets.combinePresets(presets, customNoise)
    }
}

data class SwingFailData(val vec3: Vec3, val startTime: Long)