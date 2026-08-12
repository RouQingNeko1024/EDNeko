package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3

object AutoBlock : Module("AutoBlock", Category.COMBAT) {

    private val mode by choices("Mode", arrayOf("Custom", "Predict", "Adaptive"), "Custom")
    private val blockMethod by choices("BlockMethod", arrayOf("Packet", "Simulate"), "Packet")
    
    private val customWaitTicks by int("CustomWaitTicks", 10, 1..100) { mode == "Custom" }
    private val blockDuration by int("BlockTime", 3, 1..20)
    private val showChatInfo by boolean("Debug", true)
    
    private val predictMultiplier by float("PredictMultiplier", 0.8f, 0.5f..1.5f) { mode == "Predict" }
    
    private val adaptiveMinWait by int("AdaptiveMinWait", 5, 1..50) { mode == "Adaptive" }
    private val adaptiveMaxWait by int("AdaptiveMaxWait", 20, 1..50) { mode == "Adaptive" }
    private val adaptiveFactor by float("AdaptiveFactor", 0.7f, 0.1f..1.0f) { mode == "Adaptive" }
    
    private val requireSword by boolean("RequireSword", true)
    private val stopOnDisable by boolean("StopBlockOnDisable", true)
    private val cancelAttackWhileBlocking by boolean("CancelAttackWhileBlocking", true)
    
    private var lastHurtTick = 0L
    private var currentTick = 0L
    var isBlocking = false
    private var blockStartTick = 0L
    private var waitTicks = 0
    private var waitingToBlock = false
    private var waitStartTick = 0L
    
    private val damageIntervals = mutableListOf<Long>()
    
    private var prevHurtTime = 0

    override fun onEnable() {
        reset()
    }

    override fun onDisable() {
        if (stopOnDisable && isBlocking) {
            stopBlocking()
        }
        reset()
    }

    private fun reset() {
        lastHurtTick = 0
        currentTick = 0
        isBlocking = false
        blockStartTick = 0
        waitTicks = 0
        waitingToBlock = false
        waitStartTick = 0
        damageIntervals.clear()
        prevHurtTime = 0
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        
        if (cancelAttackWhileBlocking && isBlocking) {
            when (packet) {
                is C02PacketUseEntity -> {
                    if (packet.action == C02PacketUseEntity.Action.ATTACK) {
                        event.cancelEvent()
                        if (showChatInfo) {
                            chat("§c[ AutoBlock ] §f格挡中取消攻击")
                        }
                    }
                }
                is C0APacketAnimation -> {
                    event.cancelEvent()
                }
            }
        }
        
        if (packet is S12PacketEntityVelocity) {
            val player = mc.thePlayer ?: return@handler
            if (packet.entityID != player.entityId) return@handler
            
            if (player.hurtTime > 0 && prevHurtTime == 0) {
                onDamageReceived()
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        currentTick++
        
        if (player.hurtTime > 0 && prevHurtTime == 0) {
            onDamageReceived()
        }
        prevHurtTime = player.hurtTime
        
        if (requireSword && player.heldItem?.item !is ItemSword) {
            if (isBlocking) stopBlocking()
            return@handler
        }
        
        if (waitingToBlock) {
            val elapsed = (currentTick - waitStartTick).toInt()
            
            if (showChatInfo && elapsed % 5 == 0) {
                val remaining = waitTicks - elapsed
                if (remaining > 0) {
                    chat("§e[ AutoBlock ] §f等待格挡: §c${remaining} §ftick")
                }
            }
            
            if (elapsed >= waitTicks) {
                waitingToBlock = false
                startBlocking()
            }
        }
        
        if (isBlocking) {
            val blockElapsed = (currentTick - blockStartTick).toInt()
            
            if (blockElapsed >= blockDuration) {
                stopBlocking()
            }
        }
    }

    private fun onDamageReceived() {
        val player = mc.thePlayer ?: return
        
        if (requireSword && player.heldItem?.item !is ItemSword) return
        
        val currentHurtTick = currentTick
        
        if (lastHurtTick > 0) {
            val interval = currentHurtTick - lastHurtTick
            damageIntervals.add(interval)
            
            if (damageIntervals.size > 10) {
                damageIntervals.removeAt(0)
            }
        }
        
        lastHurtTick = currentHurtTick
        
        waitTicks = calculateWaitTicks()
        waitingToBlock = true
        waitStartTick = currentTick
        
        if (showChatInfo) {
            chat("§e[ AutoBlock ] §f受伤! 将在 §c${waitTicks} §ftick 后格挡")
        }
    }

    private fun calculateWaitTicks(): Int {
        return when (mode) {
            "Custom" -> customWaitTicks
            
            "Predict" -> {
                if (damageIntervals.isNotEmpty()) {
                    val avgInterval = damageIntervals.average()
                    (avgInterval * predictMultiplier).toInt().coerceIn(1, 50)
                } else {
                    customWaitTicks
                }
            }
            
            "Adaptive" -> {
                if (damageIntervals.isNotEmpty()) {
                    val recentAvg = damageIntervals.takeLast(3).average()
                    val calculated = (recentAvg * adaptiveFactor).toInt()
                    calculated.coerceIn(adaptiveMinWait, adaptiveMaxWait)
                } else {
                    adaptiveMinWait
                }
            }
            
            else -> customWaitTicks
        }
    }

    private fun startBlocking() {
        if (isBlocking) return
        
        val player = mc.thePlayer ?: return
        if (requireSword && player.heldItem?.item !is ItemSword) return
        
        when (blockMethod) {
            "Packet" -> {
                sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
            }
            "Simulate" -> {
                mc.gameSettings.keyBindUseItem.pressed = true
                mc.playerController?.onPlayerRightClick(player, mc.theWorld, player.heldItem, BlockPos.ORIGIN, EnumFacing.UP, Vec3(0.0, 0.0, 0.0))
            }
        }
        
        isBlocking = true
        blockStartTick = currentTick
        
        if (showChatInfo) {
            chat("§a[ AutoBlock ] §f开始格挡! 持续 §c${blockDuration} §ftick")
        }
    }

    private fun stopBlocking() {
        if (!isBlocking) return
        
        when (blockMethod) {
            "Packet" -> {
                sendPacket(C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
                ))
            }
            "Simulate" -> {
                mc.gameSettings.keyBindUseItem.pressed = false
            }
        }
        
        isBlocking = false
        
        if (showChatInfo) {
            chat("§c[ AutoBlock ] §f停止格挡")
        }
    }
}
