package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.LagManager
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.Vec3

object LagRange : Module("LagRange", Category.COMBAT) {
    private val delay by int("Delay", 150, 0..1000)
    private val range by float("Range", 10.0f, 3.0f..100.0f)

    private var tickIndex = -1
    private var delayCounter = 0L
    private var hasTarget = false
    private var lastPosition: Vec3? = null
    private var currentPosition: Vec3? = null

    private fun isValidTarget(entityPlayer: EntityPlayer): Boolean {
        if (entityPlayer == mc.thePlayer || entityPlayer == mc.thePlayer.ridingEntity) {
            return false
        }

        if (entityPlayer == mc.renderViewEntity || entityPlayer == mc.renderViewEntity?.ridingEntity) {
            return false
        }

        if (entityPlayer.deathTime > 0) {
            return false
        }

        return true
    }

    private fun shouldResetOnPacket(packet: Any): Boolean {
        return when (packet) {
            is C02PacketUseEntity -> true
            is C07PacketPlayerDigging -> packet.status != C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
            is C08PacketPlayerBlockPlacement -> {
                val item = packet.stack
                item == null || item.item !is ItemSword
            }
            else -> false
        }
    }

    val onTick = handler<PlayerTickEvent> { event ->
        when (event.state) {
            EventState.PRE -> {
                LagManager.setDelay(0)
                hasTarget = false

                if (!mc.thePlayer.isUsingItem || mc.thePlayer.isBlocking) {
                    val players = mc.theWorld.playerEntities
                        .filter { isValidTarget(it) }

                    if (players.isEmpty()) {
                        tickIndex = -1
                    } else {
                        for (player in players) {
                            val distance = mc.thePlayer.getDistanceToEntityBox(player)
                            if (distance <= range) {
                                if (tickIndex < 0) {
                                    tickIndex = 0
                                    delayCounter += delay
                                    while (delayCounter > 0) {
                                        tickIndex++
                                        delayCounter -= 50
                                    }
                                }
                                LagManager.setDelay(tickIndex)
                                hasTarget = true
                                return@handler
                            }
                        }
                    }
                } else {
                    tickIndex = -1
                }
            }
            EventState.POST -> {
                val savedPosition = LagManager.getLastPosition()
                lastPosition = currentPosition ?: savedPosition
                currentPosition = savedPosition
            }
            else -> {}
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (shouldResetOnPacket(event.packet)) {
            LagManager.setDelay(0)
            tickIndex = -1
        }
    }

    override fun onDisable() {
        LagManager.setDelay(0)
        tickIndex = -1
        delayCounter = 0L
        hasTarget = false
        lastPosition = null
        currentPosition = null
    }

    override val tag: String
        get() = "${delay}ms"
}
