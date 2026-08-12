/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.settings.GameSettings
import net.minecraft.item.ItemAxe

/**
 * Port of Helongyao's ArmorBreaker script: on attack, briefly switch to an axe
 * (or a configured hotbar slot) to break armor, then restore the previous slot.
 * Non-axe mode also simulates right-click while the delay is active.
 */
object ArmorBreaker : Module("ArmorBreaker", Category.COMBAT, spacedName = "Armor Breaker") {

    private val slot by int("Slot", 0, 0..8) { !axeMode }
    private val axeMode by boolean("AxeMode", true)
    private val delay by int("Delay", 3, 0..20, suffix = "ticks")
    private val debugMode by boolean("Debug", false)

    private var axeSwitchActive = false
    private var holdingUseKey = false
    private var ticksRemaining = 0
    private var previousSlot = 0

    override fun onEnable() {
        resetState()
        debug("Module enabled")
    }

    override fun onDisable() {
        resetState(releaseKeys = true)
        debug("Module disabled")
    }

    val onAttack = handler<AttackEvent> {
        val player = mc.thePlayer ?: return@handler
        val controller = mc.playerController ?: return@handler

        debug("Attack detected, axeMode: $axeMode")

        if (axeMode) {
            val axeSlot = findAxeSlot()
            debug("Found axe in slot: $axeSlot")
            if (axeSlot !in 0..8) return@handler

            previousSlot = player.inventory.currentItem
            player.inventory.currentItem = axeSlot
            controller.syncCurrentPlayItem()
            axeSwitchActive = true
            ticksRemaining = delay
            debug("Switched to axe slot: $axeSlot")
        } else {
            if (slot !in 0..8 || slot == player.inventory.currentItem) return@handler

            previousSlot = player.inventory.currentItem
            player.inventory.currentItem = slot
            controller.syncCurrentPlayItem()
            holdingUseKey = true
            ticksRemaining = delay
            debug("Switched to slot: $slot")
        }
    }

    val onTick = handler<GameTickEvent> {
        if (ticksRemaining > 0) {
            ticksRemaining--
        }

        if (axeSwitchActive && ticksRemaining <= 0) {
            restorePreviousSlot()
            axeSwitchActive = false
        }

        if (holdingUseKey) {
            if (ticksRemaining > 0) {
                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)) {
                    mc.gameSettings.keyBindUseItem.pressed = true
                }
            } else {
                releaseUseKey()
                holdingUseKey = false
            }
        }
    }

    private fun findAxeSlot(): Int {
        val inventory = mc.thePlayer?.inventory ?: return -1
        for (i in 0..8) {
            if (inventory.getStackInSlot(i)?.item is ItemAxe) {
                return i
            }
        }
        return -1
    }

    private fun restorePreviousSlot() {
        if (previousSlot !in 0..8) return
        val player = mc.thePlayer ?: return
        player.inventory.currentItem = previousSlot
        mc.playerController?.syncCurrentPlayItem()
        debug("Restored to original slot: $previousSlot")
    }

    private fun releaseUseKey() {
        if (mc.gameSettings.keyBindUseItem.pressed &&
            !GameSettings.isKeyDown(mc.gameSettings.keyBindUseItem)
        ) {
            mc.gameSettings.keyBindUseItem.pressed = false
        }
    }

    private fun resetState(releaseKeys: Boolean = false) {
        if (releaseKeys) {
            releaseUseKey()
            if (axeSwitchActive) {
                restorePreviousSlot()
            }
        }
        axeSwitchActive = false
        holdingUseKey = false
        ticksRemaining = 0
        previousSlot = 0
    }

    private fun debug(message: String) {
        if (debugMode) {
            chat("§7[ArmorBreaker] §f$message")
        }
    }
}
