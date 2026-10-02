/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
@file:Suppress("VulnerableCodeUsages")

package net.ccbluex.liquidbounce.features.module.modules.movement

import io.netty.buffer.Unpooled
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.EventState.POST
import net.ccbluex.liquidbounce.event.EventState.PRE
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockById
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PingSpoofUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.setSprintSafely
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.hasMotion
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.item.*
import net.minecraft.network.PacketBuffer
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.DROP_ITEM
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.network.status.server.S01PacketPong
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3

object NoSlow : Module("NoSlow", Category.MOVEMENT, gameDetecting = false) {

    private val tagMode by choices("TagMode",arrayOf("Normal","None","Custom"),"Normal")
    private val customTagText by text("TagText","") {tagMode == "Custom"}
    private val specialMode by choices("SpecialMode",arrayOf("None","Matrix","Matrix2","LatestGrim"),"None")

    private val matrix2NoSlowMS by int("Matrix2NoSlowMS",100,10..1000,"ms") {specialMode == "Matrix2"}
    private val matrix2SlowMS by int("Matrix2SlowMS",50,10..1000,"ms") {specialMode == "Matrix2"}
    private val matrixNoSlowTick by int("MatrixNoSlowTicks",15,1..50) {specialMode == "Matrix"}

    private val swordMode by choices(
        "SwordMode",
        arrayOf(
            "None",
            "NCP",
            "UpdatedNCP",
            "AAC5",
            "SwitchItem",
            "InvalidC08",
            "Blink",
            "Grim",
            "PostPlace",
            "BlocksMC",
            "HYTBW32",
            "Intave14",
            "OldIntave",
            "GrimNew",
            "GrimTick",
            "Tatako",
            "IntaveBlink",
            "Prediction",
            ),
        "None"
    ) {specialMode == "None"}

    private val bmcTicks by int("BMCTicks", 1, 1..20) { swordMode == "BlocksMC" && specialMode == "None" }
    private val bmcOldOffset by boolean("BMCOldOffset", false) { swordMode == "BlocksMC" && specialMode == "None" }
    private val reblinkTicks by int("ReblinkTicks", 10, 1..20) { swordMode == "Blink" && specialMode == "None" }
    private val predictionMaxPingSpoof by int("PredictionMaxPingSpoof", 8, 1..50) {
        swordMode == "Prediction" && specialMode == "None"
    }
    private val predictionLetGo by int("PredictionLetGo", 30, 20..36) {
        swordMode == "Prediction" && specialMode == "None"
    }
    private val predictionSprintBypass by boolean("PredictionSprintBypass", true) {
        swordMode == "Prediction" && specialMode == "None"
    }
    private val blockForwardMultiplier by float("BlockForwardMultiplier", 1f, 0.2F..1f) {specialMode == "None"}
    private val blockStrafeMultiplier by float("BlockStrafeMultiplier", 1f, 0.2F..1f) {specialMode == "None"}

    private val consumeMode by choices(
        "ConsumeMode",
        arrayOf(
            "None",
            "UpdatedNCP",
            "AAC5",
            "SwitchItem",
            "InvalidC08",
            "Drop",
            "IntaveNew",
            "HYTBW32",
            "OldIntave",
            "Intave14",
            "GrimNew",
            "GrimTick",
            "IntaveBlink",
            "BlocksMC",

        ),
        "None"
    ) {specialMode == "None"}

    private val consumeForwardMultiplier by float("ConsumeForwardMultiplier", 1f, 0.2F..1f) {specialMode == "None"}
    private val consumeStrafeMultiplier by float("ConsumeStrafeMultiplier", 1f, 0.2F..1f) {specialMode == "None"}
    private val consumeFoodOnly by boolean(
        "ConsumeFood",
        true
    ) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2f && specialMode == "None" }
    private val consumeDrinkOnly by boolean(
        "ConsumeDrink",
        true
    ) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2F && specialMode == "None" }

    private val bowPacket by choices(
        "BowMode",
        arrayOf(
            "None",
            "UpdatedNCP",
            "AAC5",
            "SwitchItem",
            "InvalidC08",
            "Intave14",
            "GrimNew",
            "GrimTick",
            "IntaveBlink",
            "OldIntave",

        ),
        "None"
    ) {specialMode == "None"}

    private val bowForwardMultiplier by float("BowForwardMultiplier", 1f, 0.2F..1f) {specialMode == "None"}
    private val bowStrafeMultiplier by float("BowStrafeMultiplier", 1f, 0.2F..1f) {specialMode == "None"}
    // Blocks
    val soulSand by boolean("SoulSand", true)
    val liquidPush by boolean("LiquidPush", true)

    private var shouldSwap = false
    private var shouldBlink = true
    private var shouldNoSlow = false

    private var hasDropped = false

    private val BlinkTimer = TickTimer()
    private val in14MS = MSTimer()
    private val matrix2Timer = MSTimer()

    private var cancelTicks = 0
    private var matrixSlowing = false

    private var grim2371DoNotSlow = false
    private val grim2371Timer = TickTimer()

    private var randomFactor = 0f
    private var sent = false
    private var consumeTickCycle = 0

    private var predictionUsingItem = false
    private var predictionSpoofed = false
    private var predictionTimer = 0

    override fun onDisable() {
        shouldSwap = false
        shouldBlink = true
        BlinkTimer.reset()
        BlinkUtils.unblink()
        grim2371DoNotSlow = false
        grim2371Timer.reset()
        predictionUsingItem = false
        predictionSpoofed = false
        shouldBlink = false
        if (BlinkUtils.isBlinking) {
            BlinkUtils.unblink()
        }
        PingSpoofUtils.stopSpoof()
    }

    override val tag: String?
        get() =
        when (tagMode) {
            "Normal" -> if (specialMode == "None") {
                    ("$swordMode $consumeMode $bowPacket")
                } else "Matrix"
            "Custom" -> customTagText
            else -> ""
        }

    val onMotion = handler<MotionEvent> { event ->
        if (specialMode != "None") return@handler
        val player = mc.thePlayer ?: return@handler
        val heldItem = player.heldItem ?: return@handler
        val isUsingItem = usingItemFunc()
        val stack = mc.thePlayer.heldItem
        if (!hasMotion && !shouldSwap)
            return@handler

        if (isUsingItem || shouldSwap) {
            if (heldItem.item !is ItemSword && heldItem.item !is ItemBow && (consumeFoodOnly && heldItem.item is ItemFood ||
                        consumeDrinkOnly && (heldItem.item is ItemPotion || heldItem.item is ItemBucketMilk))
            ) {
                when (consumeMode.lowercase()) {
                    "blocksmc" -> {
                        if (event.eventState == PRE) {
                            consumeTickCycle++
                            if (consumeTickCycle == 1) {
                                mc.netHandler.addToSendQueue(C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9))
                                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 0, heldItem, 0f, 0f, 0f))
                            }
                        }
                    }
                    "grimtick" -> {
                        val currentTick = player.ticksExisted % 32
                        if (event.eventState == PRE) {
                            if (currentTick % 4 < 2) {
                                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                            }
                        }
                    }
                    "aac5" -> handleAACMode(event)

                    "switchitem" ->
                        if (event.eventState == PRE) {
                            updateSlot()
                        }

                    "updatedncp" ->
                        if (event.eventState == PRE && shouldSwap) {
                            updateSlot()
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f))
                            shouldSwap = false
                        }

                    "invalidc08" -> {
                        if (event.eventState == PRE) {
                            if (InventoryUtils.hasSpaceInInventory()) {
                                if (player.ticksExisted % 3 == 0)
                                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                            }
                        }
                    }

                    "oldintave" -> {
                        if (event.eventState == PRE) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP))
                        }
                    }
                    "hytbw32" -> {
                        if (event.eventState.stateName == "PRE") {
                            if (stack.item is ItemFood) {
                                mc.netHandler.addToSendQueue(
                                    C07PacketPlayerDigging(
                                        C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                                        BlockPos(mc.thePlayer.position.up()),
                                        EnumFacing.UP
                                    )
                                )
                            }
                        }
                    }
                    "intave14" -> {
                        handleIntaveMode(event)
                    }
                    "grimnew" -> {
                        mc.netHandler
                            .addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1))
                        mc.netHandler
                            .addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 7 + 2))
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                    }
                    "intaveblink" -> {
                        handleIntaveBlinkMode(event)
                    }
                }
            }
        }

        if (heldItem.item is ItemBow && (isUsingItem || shouldSwap)) {
            when (bowPacket.lowercase()) {
                "grimtick" -> {
                    val currentTick = player.ticksExisted % 32
                    if (event.eventState == PRE) {
                        if (currentTick % 4 < 2) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        }
                    }
                }
                "oldintave" -> {
                    if (event.eventState == PRE) {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP))
                    }
                }
                "intaveblink" -> {
                    handleIntaveBlinkMode(event)
                }
                "aac5" -> handleAACMode(event)

                "switchitem" ->
                    if (event.eventState == PRE) {
                        updateSlot()
                    }

                "updatedncp" ->
                    if (event.eventState == PRE && shouldSwap) {
                        updateSlot()
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f))
                        shouldSwap = false
                    }

                "invalidc08" -> {
                    if (event.eventState == PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }
                "intave14" -> {
                    handleIntaveMode(event)
                }
                "grimnew" -> {
                    mc.netHandler
                        .addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1))
                    mc.netHandler
                        .addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 7 + 2))
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                }
            }
        }

        if (heldItem.item is ItemSword && isUsingItem) {
            when (swordMode.lowercase()) {
                "oldintave" -> {
                    if (event.eventState == PRE) {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP))
                    }
                }
                "grimtick" -> {
                    val currentTick = player.ticksExisted % 32
                    if (event.eventState == PRE) {
                        if (currentTick % 4 < 2) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        }
                    }
                }
                "tatako" -> {
                    if (event.eventState == PRE) mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                }
                "intave14" -> {
                    handleIntaveMode(event)
                }
                "ncp" ->
                    when (event.eventState) {
                        PRE -> sendPacket(
                            C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN)
                        )

                        POST -> sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f
                            )
                        )

                        else -> return@handler
                    }
                "updatedncp" ->
                    if (event.eventState == POST) {
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f))
                    }

                "aac5" -> handleAACMode(event)

                "switchitem" ->
                    if (event.eventState == PRE) {
                        updateSlot()
                    }

                "invalidc08" -> {
                    if (event.eventState == PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }
                "grimac" -> {
                    if (event.eventState == PRE) {
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1))
                        mc.netHandler.addToSendQueue(C17PacketCustomPayload("许锦良", PacketBuffer(Unpooled.buffer())))
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                    } else {
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        sendOffHandUseItem()
                    }
                }
                "postplace" -> {
                    if (event.eventState == PRE) {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    } else {
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        sendOffHandUseItem()
                    }
                }
                "BlocksMC" -> {
                    if (!sent) {
                        sent = true
                        if (player.onGround) {
                            player.jump()
                        }
                    }

                    val slot = player.inventory.currentItem
                    val item = player.inventory.getStackInSlot(slot)

                    if (item != null && (item.unlocalizedName.contains(
                            "apple",
                            true
                        ) || item.unlocalizedName.contains("bow", true) || item.unlocalizedName.contains(
                            "potion",
                            true
                        ))
                    ) {
                        randomFactor = if (bmcOldOffset) {
                            0.5f + (Math.random() * 0.44).toFloat()
                        } else {
                            (Math.random() * 0.96).toFloat()
                        }

                        val playerPosition = player.position
                        val adjustedY = if (playerPosition.y > 0) playerPosition.y - 255 else playerPosition.y + 255
                        val inter = Vec3(playerPosition.x.toDouble(), adjustedY.toDouble(), playerPosition.z.toDouble())

                        sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(
                                    inter.xCoord.toInt(),
                                    inter.yCoord.toInt(),
                                    inter.zCoord.toInt()
                                ), 0, item, 0f, randomFactor, 0f
                            )
                        )

                        if (player.ticksExisted % bmcTicks == 0) {
                            // Additional logic if needed
                        }
                    }
                    return@handler
                }
                "hytbw32" -> {
                    if (event.eventState.stateName == "PRE") {
                        if (stack.item is ItemSword || stack.item is ItemBow) {
                            mc.netHandler
                                .addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem + 1))
                            mc.netHandler
                                .addToSendQueue(C17PacketCustomPayload("sbhyt", PacketBuffer(Unpooled.buffer())))
                            mc.netHandler
                                .addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                        }
                    }
                    if (event.eventState.stateName == "POST") {
                        if (stack.item is ItemSword || stack.item is ItemBow) {
                            mc.netHandler
                                .addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                        }
                    }
                }

                "grimnew" -> {
                    mc.netHandler
                        .addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1))
                    mc.netHandler
                        .addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 7 + 2))
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                }
                "intaveblink" -> {
                    handleIntaveBlinkMode(event)
                }
                "prediction" -> {
                    if (event.eventState == PRE) {
                        val player = mc.thePlayer ?: return@handler
                        val heldItem = player.heldItem ?: return@handler

                        val isUsingItem = usingItemFunc()

                        if (isUsingItem) {
                            if (!predictionUsingItem) {
                                // 开始使用物品，记录开始时间
                                predictionUsingItem = true
                                predictionTimer = player.ticksExisted
                                predictionSpoofed = false
                                shouldBlink = false
                            }

                            // 计算已经使用的tick数
                            val elapsedTicks = player.ticksExisted - predictionTimer

                            // Rise 逻辑：每8tick触发一次PingSpoof
                            if (elapsedTicks >= predictionMaxPingSpoof && !predictionSpoofed) {
                                predictionSpoofed = true
                            }

                            // Rise 核心逻辑：状态切换
                            if (elapsedTicks >= 5) {
                                // 在开始后的第5tick执行第一次状态切换
                                // 然后每5tick执行一次状态切换
                                if (elapsedTicks % 5 == 0) {
                                    // 1. 发送Release包（停止使用）
                                    sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))

                                    // 2. 如果是第5tick，触发Blink
                                    if (elapsedTicks == 5) {
                                        shouldBlink = true
                                    }

                                    // 3. 重新发送Use包（重新开始使用）
                                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                                }
                            }

                            // Rise 逻辑：到达letGo时间自动释放按键
                            if (elapsedTicks >= predictionLetGo) {
                                mc.gameSettings.keyBindUseItem.pressed = false
                            }

                            // Rise 逻辑：疾跑保持（只在未触发PingSpoof时）
                            if (predictionSprintBypass &&
                                player.moveForward > 0.0f &&
                                elapsedTicks <= predictionMaxPingSpoof) {
                                player.isSprinting = true
                            }

                        } else if (predictionUsingItem) {
                            // 停止使用物品时，清理状态
                            predictionUsingItem = false
                            predictionSpoofed = false
                            shouldBlink = false

                            // 停止Blink
                            if (BlinkUtils.isBlinking) {
                                BlinkUtils.unblink()
                            }

                            // 停止PingSpoof
                            PingSpoofUtils.stopSpoof()

                            // 确保使用键状态正确
                            if (!mc.gameSettings.keyBindUseItem.isKeyDown) {
                                mc.gameSettings.keyBindUseItem.pressed = false
                            }
                        }
                    }
                }

            }
        }
        if (consumeMode == "IntaveNew" && event.eventState == PRE) {
            mc.netHandler.addToSendQueue(
                C07PacketPlayerDigging(
                    RELEASE_USE_ITEM,
                    BlockPos(mc.thePlayer.posX, mc.thePlayer.getPositionEyes(1.0f).yCoord, mc.thePlayer.posZ),
                    EnumFacing.DOWN
                )
            )
        }
    }

    fun handleAACMode(e: MotionEvent) {
        if (e.eventState == PRE) {
            mc.thePlayer.sendQueue.addToSendQueue(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.START_SPRINTING
                )
            )
            mc.thePlayer.sendQueue.addToSendQueue(
                C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                    BlockPos(-1, -1, -1),
                    EnumFacing.DOWN
                )
            )
        } else {
            mc.thePlayer.sendQueue.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
            mc.thePlayer.sendQueue.addToSendQueue(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.STOP_SPRINTING
                )
            )
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (specialMode != "None") return@handler
        val packet = event.packet
        val player = mc.thePlayer ?: return@handler

        if (event.isCancelled || shouldSwap)
            return@handler

        // IntaveBlink 的包处理逻辑
        if (swordMode == "IntaveBlink" || consumeMode == "IntaveBlink" || bowPacket == "IntaveBlink") {
            if (!usingItemFunc()) {
                // 不在使用物品时，取消blink
                if (shouldBlink) {
                    BlinkUtils.unblink()
                    shouldBlink = false
                    BlinkTimer.reset()
                }
                return@handler
            }

            when (packet) {
                is C00Handshake, is C00PacketServerQuery, is C01PacketPing,
                is C01PacketChatMessage, is S01PacketPong -> return@handler

                is C07PacketPlayerDigging, is C02PacketUseEntity,
                is C12PacketUpdateSign, is C19PacketResourcePackStatus -> {
                    BlinkTimer.update()
                    // 只在应该blink时处理这些包
                    if (shouldBlink) {
                        BlinkUtils.blink(packet, event)
                    }
                    return@handler
                }

                is S12PacketEntityVelocity -> {
                    if (mc.thePlayer.entityId == packet.entityID) {
                        // 收到击退时取消blink
                        BlinkUtils.unblink()
                        shouldBlink = false
                        return@handler
                    }
                }

                is S27PacketExplosion -> {
                    if (packet.field_149153_g != 0f || packet.field_149152_f != 0f || packet.field_149159_h != 0f) {
                        // 收到爆炸时取消blink
                        BlinkUtils.unblink()
                        shouldBlink = false
                        return@handler
                    }
                }

                is C03PacketPlayer -> {
                    // 只在应该blink时缓存移动包
                    if (shouldBlink) {
                        BlinkUtils.blink(packet, event)
                    }
                }
            }
        }
        // Prediction 模式的包处理
        if (swordMode == "Prediction" && predictionUsingItem) {
            val player = mc.thePlayer ?: return@handler
            val elapsedTicks = player.ticksExisted - predictionTimer

            // Blink 逻辑
            if (shouldBlink) {
                when (packet) {
                    is C00Handshake, is C00PacketServerQuery, is C01PacketPing,
                    is C01PacketChatMessage, is S01PacketPong -> return@handler

                    is C07PacketPlayerDigging, is C02PacketUseEntity,
                    is C12PacketUpdateSign, is C19PacketResourcePackStatus -> {
                        BlinkTimer.update()
                        if (shouldBlink) {
                            BlinkUtils.blink(packet, event)
                        }
                        return@handler
                    }

                    is S12PacketEntityVelocity -> {
                        if (mc.thePlayer.entityId == packet.entityID) {
                            BlinkUtils.unblink()
                            shouldBlink = false
                            return@handler
                        }
                    }

                    is S27PacketExplosion -> {
                        if (packet.field_149153_g != 0f || packet.field_149152_f != 0f || packet.field_149159_h != 0f) {
                            BlinkUtils.unblink()
                            shouldBlink = false
                            return@handler
                        }
                    }

                    is C03PacketPlayer -> {
                        if (shouldBlink) {
                            BlinkUtils.blink(packet, event)
                        }
                    }
                }
            }

            // PingSpoof 逻辑
            if (predictionSpoofed && elapsedTicks >= predictionMaxPingSpoof) {
                val shouldDelay = PingSpoofUtils.spoof(
                    packet = packet,
                    pingOnly = false,
                    spoofDelay = 30000
                )

                if (shouldDelay) {
                    event.cancelEvent()
                    return@handler
                }
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
        val player = mc.thePlayer ?: return@handler

        if (swordMode == "Prediction" && heldItem is ItemSword && predictionUsingItem) {
            val elapsedTicks = player.ticksExisted - predictionTimer

            if (elapsedTicks > 0) {
                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))

                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, player.heldItem, 0f, 0f, 0f))

                if (shouldBlink && BlinkUtils.isBlinking) {
                    BlinkUtils.unblink()
                }

                event.forward = 1.0F
                event.strafe = 1.0F
                return@handler
            }
        }

        if (heldItem !is ItemSword) {
            if (!consumeFoodOnly && heldItem is ItemFood ||
                !consumeDrinkOnly && (heldItem is ItemPotion || heldItem is ItemBucketMilk)
            ) {
                return@handler
            }

            if (consumeMode == "Drop" && !shouldNoSlow)
                return@handler
        }

        event.forward = getMultiplier(heldItem, true)
        event.strafe = getMultiplier(heldItem, false)
    }
    private fun getMultiplier(item: Item?, isForward: Boolean) = when (specialMode) {
        "None" -> when (item) {
            is ItemFood, is ItemPotion, is ItemBucketMilk -> if (isForward) consumeForwardMultiplier else consumeStrafeMultiplier

            is ItemSword -> if (isForward) blockForwardMultiplier else blockStrafeMultiplier

            is ItemBow -> if (isForward) bowForwardMultiplier else bowStrafeMultiplier

            else -> 0.2F
        }
        "Matrix" -> if (cancelTicks != 0) {
            1.0F
        } else {
            if (Sprint.mode == "Matrix" || (Sprint.mode == "Vanilla" && Sprint.allDirections)) {
                val strafed = mc.gameSettings.keyBindLeft.isKeyDown || mc.gameSettings.keyBindRight.isKeyDown
                if (!isForward) {
                    if (
                        ((mc.gameSettings.keyBindForward.isKeyDown && strafed) ||
                                (mc.gameSettings.keyBindBack.isKeyDown && strafed))
                    ) 0.38f
                    else 0.5f
                } else {
                    if (
                        ((mc.gameSettings.keyBindForward.isKeyDown && strafed) ||
                                (mc.gameSettings.keyBindBack.isKeyDown && strafed))
                    ) 0.39f
                    else 0.5f
                }
            } else 0.5f
        }

        "Matrix2" -> {
            if (matrixSlowing) {
                val strafed = mc.gameSettings.keyBindLeft.isKeyDown || mc.gameSettings.keyBindRight.isKeyDown
                if (!isForward) {
                    if (
                        ((mc.gameSettings.keyBindForward.isKeyDown && strafed) ||
                                (mc.gameSettings.keyBindBack.isKeyDown && strafed))
                    ) 0.38f
                    else 0.5f
                } else {
                    if (
                        ((mc.gameSettings.keyBindForward.isKeyDown && strafed) ||
                                (mc.gameSettings.keyBindBack.isKeyDown && strafed))) 0.39f
                    else 0.5f
                }
            } else {
                1.0f
            }
        }
        "LatestGrim" -> {
            if (mc.thePlayer.ticksExisted % 3 == 0) {
                mc.thePlayer setSprintSafely false
                0.2f
            } else {
                mc.thePlayer setSprintSafely true
                1.0f
            }
        }
        else -> 1.0f
    }

    val onUpdate = handler<UpdateEvent> {
        if (shouldBlink && BlinkTimer.hasTimePassed(5)) {
            BlinkUtils.unblink()
            shouldBlink = false
            BlinkTimer.reset()
        }
        PingSpoofUtils.tick()
        when (specialMode) {
            "Matrix" -> {
                if (mc.thePlayer.hurtTime >= 9) {
                    cancelTicks = matrixNoSlowTick
                } else if (cancelTicks != 0) {
                    cancelTicks--
                }
            }
            "Matrix2" -> {
                when {
                    matrixSlowing -> {
                        // 减速阶段
                        if (matrix2Timer.hasTimePassed(matrix2SlowMS.toLong())) {
                            // 减速阶段结束，切换到无减速阶段
                            matrixSlowing = false
                            matrix2Timer.reset()
                        }
                    }
                    else -> {
                        // 无减速阶段
                        if (matrix2Timer.hasTimePassed(matrix2NoSlowMS.toLong())) {
                            // 无减速阶段结束，切换到减速阶段
                            matrixSlowing = true
                            matrix2Timer.reset()
                        }
                    }
                }
            }
        }
        if (swordMode == "Prediction" && predictionUsingItem && specialMode == "None") {
            val player = mc.thePlayer ?: return@handler
            val elapsedTicks = player.ticksExisted - predictionTimer

            if (predictionSprintBypass &&
                player.moveForward > 0.707f &&
                elapsedTicks <= predictionMaxPingSpoof) {
                player.isSprinting = true
            }
        }
    }

    fun isUNCPBlocking() =
        swordMode == "UpdatedNCP" && mc.gameSettings.keyBindUseItem.isKeyDown && (mc.thePlayer.heldItem?.item is ItemSword)

    fun usingItemFunc() =
        mc.thePlayer?.heldItem != null && (mc.thePlayer.isUsingItem || (mc.thePlayer.heldItem?.item is ItemSword && KillAura.blockStatus) || isUNCPBlocking())

    private fun updateSlot() {
        SilentHotbar.selectSlotSilently(this, (SilentHotbar.currentSlot + 1) % 9, immediate = true)
        SilentHotbar.resetSlot(this, true)
    }
    fun sendOffHandUseItem() {
        mc.netHandler.addToSendQueue(
            C08PacketPlayerBlockPlacement(BlockPos(-1, -2, -1), 255, null, 0.0f, 0.0f, 0.0f)
        )
    }
    private fun handleIntaveMode(event: MotionEvent) {
        if (in14MS.hasTimePassed(800L)) {
            mc.thePlayer.sendQueue.addToSendQueue(
                C07PacketPlayerDigging(
                    RELEASE_USE_ITEM,
                    BlockPos(-1, -1, -1),
                    EnumFacing.DOWN
                )
            )
            in14MS.reset()
        } else if (event.eventState == PRE) {
            mc.thePlayer.sendQueue.addToSendQueue(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.START_SPRINTING
                )
            )
        } else {
            mc.thePlayer.sendQueue.addToSendQueue(
                C0EPacketClickWindow(
                    0, 36, 0, 1,
                    ItemStack(getBlockById(166)),
                    (0).toShort()
                )
            )
            mc.thePlayer.sendQueue.addToSendQueue(C09PacketHeldItemChange())
            mc.thePlayer.sendQueue.addToSendQueue(
                C08PacketPlayerBlockPlacement(
                    mc.thePlayer.inventory.getCurrentItem()
                )
            )
            mc.thePlayer.sendQueue.addToSendQueue(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.STOP_SPRINTING
                )
            )
            mc.thePlayer.sendQueue.addToSendQueue(C0FPacketConfirmTransaction())
        }
    }
    private fun handleIntaveBlinkMode(event: MotionEvent) {
        val player = mc.thePlayer ?: return
        val heldItem = player.heldItem ?: return

        val isUsingItem = usingItemFunc()

        // 不在使用物品时，立即取消blink
        if (!isUsingItem) {
            if (shouldBlink) {
                BlinkUtils.unblink()
                shouldBlink = false
                BlinkTimer.reset()
            }
            return
        }

        when (event.eventState) {
            PRE -> {
                if (player.ticksExisted % 5 == 0 && shouldBlink) { // 每5 tick执行一次
                    // 释放blink并立即重新blink
                    BlinkUtils.unblink()
                    sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    sendPacket(C08PacketPlayerBlockPlacement(heldItem))
                    BlinkTimer.reset()
                    // shouldBlink 保持为 true 以继续blink
                } else if (!shouldBlink) {
                    // 开始blink
                    sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    sendPacket(C08PacketPlayerBlockPlacement(heldItem))
                    shouldBlink = true
                    BlinkTimer.reset()
                }
            }
            else -> {}
        }
    }
}

