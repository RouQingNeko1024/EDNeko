/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import io.netty.buffer.Unpooled
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.hasMotion
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.block.*
import net.minecraft.init.Blocks
import net.minecraft.item.*
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.DROP_ITEM
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.server.S09PacketHeldItemChange
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.network.status.server.S01PacketPong
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition

object NoSlow : Module("NoSlow", Category.MOVEMENT, gameDetecting = false) {

    private val swordMode by choices(
        "SwordMode",
        arrayOf("None", "NCP", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "Blink", "postplace", "Matrix", "PredictionSemi", "Prediction", "GrimAC", "GrimAC1.9+", "aug"),
        "None"
    )

    private val reblinkTicks by int("ReblinkTicks", 10, 1..20) { swordMode == "Blink" }
    private val predictionCancelTick by int("PredictionSemiCancelTick", 1, 0..2) { swordMode == "PredictionSemi" }
    private val predictionCancelTick2 by int("PredictionSemiCancelTick2", 1, 0..2) { swordMode == "PredictionSemi" }
    private val predictionSwapDelay by int("PredictionSwapDelay", 0, 0..3) { swordMode == "Prediction" }
    private val predictionBlink by boolean("PredictionBlink", false) { swordMode == "Prediction" }
    private val predictionC17 by boolean("PredictionC17", false) { swordMode == "Prediction" }

    private val grim19KeepSprinting by boolean("Grim19KeepSprinting", true) { swordMode == "GrimAC1.9+" }
    private val grim19NoC0F by boolean("Grim19NoC0F", true) { swordMode == "GrimAC1.9+" }

    // aug模式 - Blocking子选项
    private val augBlockingSwitch by boolean("AugBlockingSwitch", false) { swordMode == "aug" }
    private val augBlockingExtra by boolean("AugBlockingExtra", false) { swordMode == "aug" }
    private val augBlockingAAC5 by boolean("AugBlockingAAC5", false) { swordMode == "aug" }
    private val augBlockingNoGround by boolean("AugBlockingNoGround", false) { swordMode == "aug" }
    private val augBlockingJump by boolean("AugBlockingJump", false) { swordMode == "aug" }
    private val augBlockingHandPacket by boolean("AugBlockingHandPacket", false) { swordMode == "aug" }
    private val augBlockingMainHand by boolean("AugBlockingMainHand", false) { swordMode == "aug" }
    private val augBlockingOffHandPlace by boolean("AugBlockingOffHandPlace", false) { swordMode == "aug" }
    private val augBlockingOldGrim by boolean("AugBlockingOldGrim", false) { swordMode == "aug" }
    private val augBlockingPost by boolean("AugBlockingPost", false) { swordMode == "aug" }

    // aug模式 - Food子选项
    private val augFoodSwitch by boolean("AugFoodSwitch", false) { swordMode == "aug" }
    private val augFoodExtra by boolean("AugFoodExtra", false) { swordMode == "aug" }
    private val augFoodJump by boolean("AugFoodJump", false) { swordMode == "aug" }
    private val augFoodNoGround by boolean("AugFoodNoGround", false) { swordMode == "aug" }
    private val augFoodClip by boolean("AugFoodClip", false) { swordMode == "aug" }
    private val augFoodHandPacket by boolean("AugFoodHandPacket", false) { swordMode == "aug" }
    private val augFoodOldGrim by boolean("AugFoodOldGrim", false) { swordMode == "aug" }
    private val augFoodMainHand by boolean("AugFoodMainHand", false) { swordMode == "aug" }

    // aug模式 - Bow子选项
    private val augBowSwitch by boolean("AugBowSwitch", false) { swordMode == "aug" }
    private val augBowExtra by boolean("AugBowExtra", false) { swordMode == "aug" }
    private val augBowHandPacket by boolean("AugBowHandPacket", false) { swordMode == "aug" }
    private val augBowOldGrim by boolean("AugBowOldGrim", false) { swordMode == "aug" }
    private val augBowMainHand by boolean("AugBowMainHand", false) { swordMode == "aug" }

    // aug模式 - 速度倍率
    private val augBlockingForward by float("AugBlockingForward", 0.2f, 0.0f..1.0f) { swordMode == "aug" }
    private val augBlockingStrafe by float("AugBlockingStrafe", 0.2f, 0.0f..1.0f) { swordMode == "aug" }
    private val augFoodForward by float("AugFoodForward", 0.2f, 0.0f..1.0f) { swordMode == "aug" }
    private val augFoodStrafe by float("AugFoodStrafe", 0.2f, 0.0f..1.0f) { swordMode == "aug" }
    private val augBowForward by float("AugBowForward", 0.2f, 0.0f..1.0f) { swordMode == "aug" }
    private val augBowStrafe by float("AugBowStrafe", 0.2f, 0.0f..1.0f) { swordMode == "aug" }

    // aug模式 - 其他选项
    private val augIgnoreServerItemChange by boolean("AugIgnoreServerItemChange", false) { swordMode == "aug" }

    private val blockForwardMultiplier by float("BlockForwardMultiplier", 1f, 0.2F..1f)
    private val blockStrafeMultiplier by float("BlockStrafeMultiplier", 1f, 0.2F..1f)

    private val consumeMode by choices(
        "ConsumeMode",
        arrayOf("None", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "Intave", "OldIntave", "GrimAC", "Drop"),
        "None"
    )

    private val consumeForwardMultiplier by float("ConsumeForwardMultiplier", 1f, 0.2F..1f)
    private val consumeStrafeMultiplier by float("ConsumeStrafeMultiplier", 1f, 0.2F..1f)
    private val consumeFoodOnly by boolean(
        "ConsumeFood",
        true
    ) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2F }
    private val consumeDrinkOnly by boolean(
        "ConsumeDrink",
        true
    ) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2F }

    private val bowPacket by choices(
        "BowMode",
        arrayOf("None", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "GrimAC"),
        "None"
    )

    private val bowForwardMultiplier by float("BowForwardMultiplier", 1f, 0.2F..1f)
    private val bowStrafeMultiplier by float("BowStrafeMultiplier", 1f, 0.2F..1f)

    // Blocks
    val soulSand by boolean("SoulSand", true)
    val liquidPush by boolean("LiquidPush", true)

    private var shouldSwap = false
    private var shouldBlink = true
    private var shouldNoSlow = false

    private var hasDropped = false
    private var grimEating = true
    private var grimLastFoodAmount = 0
    private var grimFoodSpeed = 0.2f
    private var grim19Blocking = false
    private var predictionDelay = 0
    private var predictionPost = false
    private var predictionBlockTick = 0

    // Matrix模式相关变量
    private var nextTemp = false
    private var lastBlockingStat = false
    private var waitC03 = false
    private val packetBuf = mutableListOf<Packet<*>>()
    private val msTimer = MSTimer()

    // aug模式相关变量
    private var augBlocking = false

    private val BlinkTimer = TickTimer()

    override val tag: String?
        get() = if (swordMode == "aug") {
            val tags = mutableListOf<String>()
            if (augBlockingSwitch) tags.add("Switch")
            if (augBlockingExtra) tags.add("Extra")
            if (augBlockingAAC5) tags.add("AAC5")
            if (augBlockingNoGround) tags.add("NoGround")
            if (augBlockingJump) tags.add("Jump")
            if (augBlockingHandPacket) tags.add("HandPacket")
            if (augBlockingMainHand) tags.add("MainHand")
            if (augBlockingOffHandPlace) tags.add("OffHandPlace")
            if (augBlockingOldGrim) tags.add("OldGrim")
            if (augBlockingPost) tags.add("Post")
            if (tags.isNotEmpty()) "aug | ${tags.joinToString(",")}" else "aug"
        } else null

    override fun onDisable() {
        shouldSwap = false
        shouldBlink = true
        BlinkTimer.reset()
        BlinkUtils.unblink()
        hasDropped = false
        grimEating = true
        grimLastFoodAmount = 0
        grimFoodSpeed = 0.2f
        grim19Blocking = false
        predictionDelay = 0
        predictionPost = false
        predictionBlockTick = 0

        // 重置Matrix模式相关变量
        nextTemp = false
        lastBlockingStat = false
        waitC03 = false
        packetBuf.clear()
        msTimer.reset()

        // 重置aug模式相关变量
        augBlocking = false
    }

    val onMotion = handler<MotionEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val heldItem = player.heldItem ?: return@handler
        val isUsingItem = usingItemFunc()

        if (!hasMotion && !shouldSwap)
            return@handler

        // GrimAC1.9+ 模式: 不在使用物品时重置状态
        if (swordMode == "GrimAC1.9+" && !isUsingItem) {
            grim19Blocking = false
        }

        // aug模式: 不在使用物品时重置状态
        if (swordMode == "aug" && !isUsingItem) {
            augBlocking = false
        }

        // aug模式处理
        if (swordMode == "aug" && isUsingItem) {
            handleAugMotion(event, player, heldItem)
            return@handler
        }

        // Matrix模式处理
        if (swordMode == "Matrix" && (lastBlockingStat || isUsingItem)) {
            if (msTimer.hasTimePassed(230) && nextTemp) {
                nextTemp = false
                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos(-1, -1, -1), EnumFacing.DOWN))

                if (packetBuf.isNotEmpty()) {
                    var canAttack = false
                    for (packet in packetBuf) {
                        if (packet is C03PacketPlayer) {
                            canAttack = true
                        }
                        if (!((packet is C02PacketUseEntity || packet is C0APacketAnimation) && !canAttack)) {
                            sendPacket(packet)
                        }
                    }
                    packetBuf.clear()
                }
            }

            if (!nextTemp) {
                lastBlockingStat = isUsingItem
                if (!isUsingItem) {
                    return@handler
                }
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, player.inventory.getCurrentItem(), 0f, 0f, 0f))
                nextTemp = true
                waitC03 = false
                msTimer.reset()
            }
        }

        if (isUsingItem || shouldSwap) {
            if (heldItem.item !is ItemSword && heldItem.item !is ItemBow && (consumeFoodOnly && heldItem.item is ItemFood ||
                        consumeDrinkOnly && (heldItem.item is ItemPotion || heldItem.item is ItemBucketMilk))
            ) {
                when (consumeMode.lowercase()) {
                    "aac5" ->
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))

                    "switchitem" ->
                        if (event.eventState == EventState.PRE) {
                            updateSlot()
                        }

                    "updatedncp" ->
                        if (event.eventState == EventState.PRE && shouldSwap) {
                            updateSlot()
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f))
                            shouldSwap = false
                        }

                    "invalidc08" -> {
                        if (event.eventState == EventState.PRE) {
                            if (InventoryUtils.hasSpaceInInventory()) {
                                if (player.ticksExisted % 3 == 0)
                                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                            }
                        }
                    }

                    "intave" -> {
                        if (event.eventState == EventState.PRE) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP), false)
                        }
                    }

                    "oldintave" -> {
                        if (event.eventState == EventState.PRE) {
                            sendPacket(C09PacketHeldItemChange(player.inventory.currentItem % 8 + 1), false)
                            sendPacket(C09PacketHeldItemChange(player.inventory.currentItem), false)
                        }
                    }

                    "grimac" -> {
                        handleGrimConsumeMotion(player, heldItem)
                    }
                }
            }
        }

        if (heldItem.item is ItemBow && (isUsingItem || shouldSwap)) {
            when (bowPacket.lowercase()) {
                "aac5" ->
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))

                "switchitem" ->
                    if (event.eventState == EventState.PRE) {
                        updateSlot()
                    }

                "updatedncp" ->
                    if (event.eventState == EventState.PRE && shouldSwap) {
                        updateSlot()
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f))
                        shouldSwap = false
                    }

                "invalidc08" -> {
                    if (event.eventState == EventState.PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }

                "grimac" ->
                    if (event.eventState == EventState.POST) {
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f), false)
                    }
            }
        }

        if (heldItem.item is ItemSword && isUsingItem) {
            if (event.eventState == EventState.PRE) {
                updatePredictionState()
            }

            when (swordMode.lowercase()) {
                "ncp" ->
                    when (event.eventState) {
                        EventState.PRE -> sendPacket(
                            C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN)
                        )

                        EventState.POST -> sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f
                            )
                        )

                        else -> return@handler
                    }

                "updatedncp" ->
                    if (event.eventState == EventState.POST) {
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f))
                    }

                "aac5" ->
                    if (event.eventState == EventState.POST) {
                        sendPacket(
                            C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, player.heldItem, 0f, 0f, 0f)
                        )
                    }

                "switchitem" ->
                    if (event.eventState == EventState.PRE) {
                        updateSlot()
                    }

                "invalidc08" -> {
                    if (event.eventState == EventState.PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }

                "postplace" ->
                    if (event.eventState == EventState.PRE) {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    } else {
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                    }

                "predictionsemi" -> {
                }

                "prediction" -> {
                    if (event.eventState == EventState.PRE) {
                        predictionDelay--

                        if (predictionDelay < 0) {
                            sendPredictionSwap()
                            predictionPost = true
                            predictionDelay = predictionSwapDelay
                        }
                    } else if (predictionPost) {
                        if (predictionBlink) {
                            sendPacket(C08PacketPlayerBlockPlacement(heldItem))
                            BlinkUtils.unblink()
                        }

                        predictionPost = false
                    }
                }

                "grimac1.9+" -> {
                    if (event.eventState == EventState.PRE) {
                        grim19Blocking = true
                        if (grim19NoC0F) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                    }
                }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val player = mc.thePlayer ?: return@handler

        if (event.isCancelled || shouldSwap)
            return@handler

        if (consumeMode == "GrimAC") {
            handleGrimConsumePacket(event, packet, player)
            if (event.isCancelled)
                return@handler
        }

        // GrimAC1.9+ 模式: 取消原始的C08包(使用物品触发的), 我们在onMotion中发送了伪造的C08
        if (swordMode == "GrimAC1.9+" && grim19Blocking && grim19NoC0F) {
            if (packet is C08PacketPlayerBlockPlacement && packet.placedBlockDirection == 255 && player.heldItem?.item is ItemSword) {
                event.cancelEvent()
                return@handler
            }
        }

        // aug模式包处理
        if (swordMode == "aug") {
            handleAugPacket(event, packet, player)
            if (event.isCancelled)
                return@handler
        }

        if (swordMode == "Prediction" && predictionBlink && predictionPost && packet is C03PacketPlayer) {
            BlinkUtils.blink(packet, event, sent = true, receive = false)
            return@handler
        }

        // Matrix模式包处理
        if (swordMode == "Matrix" && nextTemp) {
            if ((packet is C07PacketPlayerDigging || packet is C08PacketPlayerBlockPlacement) && usingItemFunc()) {
                event.cancelEvent()
            } else if (packet is C03PacketPlayer || packet is C0APacketAnimation || packet is C0BPacketEntityAction ||
                packet is C02PacketUseEntity || packet is C07PacketPlayerDigging || packet is C08PacketPlayerBlockPlacement) {
                packetBuf.add(packet)
                event.cancelEvent()
            }
        }

        // Credit: @ManInMyVan
        // TODO: Not sure how to fix random grim simulation flag. (Seem to only happen in Loyisa).
        if (consumeMode == "Drop") {
            if (player.heldItem?.item !is ItemFood || !player.isMoving) {
                shouldNoSlow = false
                return@handler
            }

            val isUsingItem = packet is C08PacketPlayerBlockPlacement && packet.placedBlockDirection == 255

            if (!player.isUsingItem) {
                shouldNoSlow = false
                hasDropped = false
            }

            if (isUsingItem && !hasDropped) {
                sendPacket(C07PacketPlayerDigging(DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                shouldNoSlow = false
                hasDropped = true
            } else if (packet is S2FPacketSetSlot && player.isUsingItem) {
                if (packet.func_149175_c() != 0 || packet.func_149173_d() != SilentHotbar.currentSlot + 36) return@handler

                event.cancelEvent()
                shouldNoSlow = true

                player.itemInUse = packet.func_149174_e()
                if (!player.isUsingItem) player.itemInUseCount = 0
                player.inventory.mainInventory[SilentHotbar.currentSlot] = packet.func_149174_e()
            }
        }

        if (swordMode == "Blink") {
            when (packet) {
                is C00Handshake, is C00PacketServerQuery, is C01PacketPing, is C01PacketChatMessage, is S01PacketPong -> return@handler

                is C07PacketPlayerDigging, is C02PacketUseEntity, is C12PacketUpdateSign, is C19PacketResourcePackStatus -> {
                    BlinkTimer.update()
                    if (shouldBlink && BlinkTimer.hasTimePassed(reblinkTicks) && (BlinkUtils.packetsReceived.isNotEmpty() || BlinkUtils.packets.isNotEmpty())) {
                        BlinkUtils.unblink()
                        BlinkTimer.reset()
                        shouldBlink = false
                    } else if (!BlinkTimer.hasTimePassed(reblinkTicks)) {
                        shouldBlink = true
                    }
                    return@handler
                }

                // Flush on kb
                is S12PacketEntityVelocity -> {
                    if (mc.thePlayer.entityId == packet.entityID) {
                        BlinkUtils.unblink()
                        return@handler
                    }
                }

                // Flush on explosion
                is S27PacketExplosion -> {
                    if (packet.field_149153_g != 0f || packet.field_149152_f != 0f || packet.field_149159_h != 0f) {
                        BlinkUtils.unblink()
                        return@handler
                    }
                }

                is C03PacketPlayer -> {
                    if (swordMode == "Blink") {
                        if (player.isMoving) {
                            if (player.heldItem?.item is ItemSword && usingItemFunc()) {
                                if (shouldBlink)
                                    BlinkUtils.blink(packet, event)
                            } else {
                                shouldBlink = true
                                BlinkUtils.unblink()
                            }
                        }
                    }
                }
            }
        }

        when (packet) {
            is C08PacketPlayerBlockPlacement -> {
                if (packet.stack?.item != null && player.heldItem?.item != null && packet.stack.item == mc.thePlayer.heldItem?.item) {
                    if ((consumeMode == "UpdatedNCP" && (
                                packet.stack.item is ItemFood ||
                                        packet.stack.item is ItemPotion ||
                                        packet.stack.item is ItemBucketMilk)) ||
                        (bowPacket == "UpdatedNCP" && packet.stack.item is ItemBow)
                    ) {
                        shouldSwap = true
                    }
                }
            }
        }
    }

    val onSlowDown = handler<SlowDownEvent> { event ->
        val heldItem = mc.thePlayer.heldItem?.item

        if (heldItem !is ItemSword) {
            if (!consumeFoodOnly && heldItem is ItemFood ||
                !consumeDrinkOnly && (heldItem is ItemPotion || heldItem is ItemBucketMilk)
            ) {
                return@handler
            }

            if (consumeMode == "Drop" && !shouldNoSlow)
                return@handler
        }

        if (heldItem is ItemSword && swordMode == "PredictionSemi" && predictionBlockTick != predictionCancelTick && predictionBlockTick != predictionCancelTick2)
            return@handler

        if (heldItem is ItemSword && swordMode == "GrimAC1.9+" && grim19Blocking) {
            event.forward = 1f
            event.strafe = 1f
            if (grim19KeepSprinting) {
                mc.thePlayer?.setSprinting(true)
            }
            return@handler
        }

        // aug模式减速处理
        if (swordMode == "aug") {
            when (heldItem) {
                is ItemSword -> {
                    event.forward = augBlockingForward
                    event.strafe = augBlockingStrafe
                }
                is ItemFood, is ItemPotion, is ItemBucketMilk -> {
                    event.forward = augFoodForward
                    event.strafe = augFoodStrafe
                }
                is ItemBow -> {
                    event.forward = augBowForward
                    event.strafe = augBowStrafe
                }
            }
            return@handler
        }

        event.forward = getMultiplier(heldItem, true)
        event.strafe = getMultiplier(heldItem, false)
    }

    private fun getMultiplier(item: Item?, isForward: Boolean) = when (item) {
        is ItemFood, is ItemPotion, is ItemBucketMilk ->
            if (consumeMode == "GrimAC" && isUsingConsumable()) {
                grimFoodSpeed
            } else if (isForward) {
                consumeForwardMultiplier
            } else {
                consumeStrafeMultiplier
            }

        is ItemSword -> if (isForward) blockForwardMultiplier else blockStrafeMultiplier

        is ItemBow -> if (isForward) bowForwardMultiplier else bowStrafeMultiplier

        else -> 0.2F
    }

    private fun handleGrimConsumeMotion(player: net.minecraft.client.entity.EntityPlayerSP, heldItem: net.minecraft.item.ItemStack) {
        if (heldItem.item !is ItemAppleGold)
            return

        if (!mc.gameSettings.keyBindUseItem.isKeyDown) {
            if (!grimEating) {
                grimEating = true
                grimLastFoodAmount = 0
            }
            return
        }

        if (grimEating) {
            if (heldItem.stackSize <= 1)
                return

            grimLastFoodAmount = heldItem.stackSize
            val anti = canUseGrimAppleDrop(heldItem)
            grimFoodSpeed = if (anti) 1f else 0.2f

            if (anti) {
                sendPacket(C07PacketPlayerDigging(DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN), false)
                sendPacket(C0FPacketConfirmTransaction(), true)
            }

            grimEating = false
        }

        if (!grimEating && player.heldItem?.stackSize != grimLastFoodAmount) {
            grimFoodSpeed = 0.2f
            grimEating = true
        }
    }

    private fun handleGrimConsumePacket(event: PacketEvent, packet: Packet<*>, player: net.minecraft.client.entity.EntityPlayerSP) {
        val heldItem = player.heldItem ?: return

        if (player.isUsingItem && heldItem.item is ItemAppleGold && mc.gameSettings.keyBindUseItem.isKeyDown &&
            packet is C07PacketPlayerDigging && packet.status == DROP_ITEM
        ) {
            event.cancelEvent()
            return
        }

        if (event.eventType != EventState.RECEIVE || packet !is S2FPacketSetSlot || heldItem.item !is ItemAppleGold)
            return

        if (canUseGrimAppleDrop(heldItem)) {
            event.cancelEvent()
        }
    }

    private fun canUseGrimAppleDrop(heldItem: net.minecraft.item.ItemStack): Boolean {
        if (heldItem.stackSize <= 1)
            return false

        val movingObjectPosition = mc.objectMouseOver ?: return true
        if (movingObjectPosition.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
            return true

        val world = mc.theWorld ?: return true
        val block = world.getBlockState(movingObjectPosition.blockPos).block
        return heldItem.item !is ItemFood || !isInteractBlock(block)
    }

    private fun isUsingConsumable(): Boolean {
        val heldItem = mc.thePlayer?.heldItem?.item ?: return false
        return mc.thePlayer.isUsingItem && isConsumable(heldItem)
    }

    private fun isConsumable(item: Item) =
        item is ItemFood || item is ItemBucketMilk || item is ItemPotion && !ItemPotion.isSplash(mc.thePlayer.heldItem.metadata)

    private fun isInteractBlock(block: Block) =
        block is BlockFence ||
            block is BlockFenceGate ||
            block is BlockDoor ||
            block is BlockChest ||
            block is BlockEnderChest ||
            block is BlockEnchantmentTable ||
            block is BlockFurnace ||
            block is BlockAnvil ||
            block is BlockBed ||
            block is BlockWorkbench ||
            block is BlockNote ||
            block is BlockTrapDoor ||
            block is BlockHopper ||
            block is BlockDispenser ||
            block is BlockDaylightDetector ||
            block is BlockRedstoneRepeater ||
            block is BlockRedstoneComparator ||
            block is BlockButton ||
            block is BlockBeacon ||
            block is BlockBrewingStand ||
            block is BlockSign

    fun isUNCPBlocking() =
        swordMode == "UpdatedNCP" && mc.gameSettings.keyBindUseItem.isKeyDown && (mc.thePlayer.heldItem?.item is ItemSword)

    fun usingItemFunc() =
        mc.thePlayer?.heldItem != null && (mc.thePlayer.isUsingItem || (mc.thePlayer.heldItem?.item is ItemSword && (KillAura.blockStatus || AutoBlock.isBlocking)) || isUNCPBlocking())

    private fun updatePredictionState() {
        if (swordMode == "PredictionSemi") {
            predictionBlockTick = (predictionBlockTick + 1) % 3
        } else if (swordMode != "Prediction") {
            predictionBlockTick = 0
        }
    }

    private fun sendPredictionSwap() {
        val player = mc.thePlayer ?: return
        val currentSlot = player.inventory.currentItem
        val swapSlot = (currentSlot + 1) % 9

        sendPacket(C09PacketHeldItemChange(swapSlot))

        if (predictionC17) {
            sendPacket(C17PacketCustomPayload("woshijiejue", PacketBuffer(Unpooled.buffer())))
        }

        sendPacket(C09PacketHeldItemChange(currentSlot))
    }

    private fun updateSlot() {
        SilentHotbar.selectSlotSilently(this, (SilentHotbar.currentSlot + 1) % 9, immediate = true)
        SilentHotbar.resetSlot(this, true)
    }

    private fun handleAugMotion(event: MotionEvent, player: net.minecraft.client.entity.EntityPlayerSP, heldItem: net.minecraft.item.ItemStack) {
        val isPre = event.eventState == EventState.PRE
        val currentItem = heldItem.item

        if (currentItem is ItemSword) {
            if (augBlockingSwitch && isPre) {
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem % 8 + 1))
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem))
            }
            if (augBlockingNoGround) {
                event.onGround = false
            }
            if (augBlockingJump && player.onGround && !isPre) {
                player.jump()
            }
            if (augBlockingAAC5 && !isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
            }
            if (augBlockingExtra && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
            }
            if (augBlockingHandPacket && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, null, 0f, 0f, 0f))
            }
            if (augBlockingPost) {
                if (isPre) {
                    sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                } else {
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                }
            }
            if (augBlockingOffHandPlace && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, null, 0f, 0f, 0f))
            }
            if (augBlockingOldGrim) {
                if (isPre) {
                    if (augBlocking) {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        augBlocking = false
                    }
                } else {
                    sendPacket(C0FPacketConfirmTransaction(1, nextInt(-999999, 999999).toShort(), true), false)
                    sendPacket(C08PacketPlayerBlockPlacement(heldItem))
                    augBlocking = true
                }
            }
            if (augBlockingMainHand && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(heldItem))
            }
        }

        if (currentItem is ItemFood || currentItem is ItemPotion || currentItem is ItemBucketMilk) {
            if (augFoodSwitch && isPre) {
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem % 8 + 1))
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem))
            }
            if (augFoodNoGround) {
                event.onGround = false
            }
            if (augFoodJump && player.onGround && !isPre) {
                player.jump()
            }
            if (augFoodExtra && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
            }
            if (augFoodClip) {
                event.y += 1e-14
            }
            if (augFoodHandPacket && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, null, 0f, 0f, 0f))
            }
            if (augFoodOldGrim) {
                if (isPre) {
                    sendPacket(C0EPacketClickWindow(0, 36, 0, 2, ItemStack(Blocks.barrier), 0.toShort()), false)
                }
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem % 8 + 1))
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem % 7 + 2))
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem))
            }
            if (augFoodMainHand && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(heldItem))
            }
        }

        if (currentItem is ItemBow) {
            if (augBowSwitch && isPre) {
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem % 8 + 1))
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem))
            }
            if (augBowExtra && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
            }
            if (augBowHandPacket && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, null, 0f, 0f, 0f))
            }
            if (augBowOldGrim) {
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem % 8 + 1))
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem % 7 + 2))
                sendPacket(C09PacketHeldItemChange(player.inventory.currentItem))
            }
            if (augBowMainHand && isPre) {
                sendPacket(C08PacketPlayerBlockPlacement(heldItem))
            }
        }
    }

    private fun handleAugPacket(event: PacketEvent, packet: Packet<*>, player: net.minecraft.client.entity.EntityPlayerSP) {
        val heldItem = player.heldItem?.item ?: return

        // OldGrim food/bow: 发送使用物品包时清零移动
        if ((augFoodOldGrim && (heldItem is ItemFood || heldItem is ItemBucketMilk || heldItem is ItemPotion)) ||
            (augBowOldGrim && heldItem is ItemBow)
        ) {
            if (event.eventType == EventState.SEND) {
                if (packet is C08PacketPlayerBlockPlacement && packet.placedBlockDirection == 255) {
                    player.motionX = 0.0
                    player.motionZ = 0.0
                }
                if (packet is C07PacketPlayerDigging && packet.status == RELEASE_USE_ITEM) {
                    player.motionX = 0.0
                    player.motionZ = 0.0
                }
            }
        }

        // OldGrim food/bow: 取消S30PacketWindowItems
        if (event.eventType == EventState.RECEIVE) {
            if ((augFoodOldGrim && (heldItem is ItemFood || heldItem is ItemBucketMilk || heldItem is ItemPotion)) ||
                (augBowOldGrim && heldItem is ItemBow)
            ) {
                if (packet is S30PacketWindowItems) {
                    event.cancelEvent()
                    return
                }
            }
        }

        // IgnoreServerItemChange: 取消S09PacketHeldItemChange
        if (augIgnoreServerItemChange && event.eventType == EventState.RECEIVE) {
            if (packet is S09PacketHeldItemChange) {
                event.cancelEvent()
                return
            }
        }
    }
}