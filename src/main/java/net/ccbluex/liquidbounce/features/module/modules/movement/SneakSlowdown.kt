/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.SneakSlowDownEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.util.MathHelper

object SneakSlowdown : Module("SneakSlowdown", Category.MOVEMENT, gameDetecting = false) {

    private val mode by choices(
        "Mode",
        arrayOf("Intave", "Vanilla", "Custom"),
        "Intave"
    )

    private val customMultiplier by float("Multiplier", 0.6f, 0.3f..1.0f) { mode == "Custom" }

    private val intaveTargetSpeed by float("IntaveTargetSpeed", 0.76f, 0.3f..1.0f) { mode == "Intave" }
    private val intaveMovingBoost by float("IntaveMovingBoost", 0.04f, 0.0f..0.3f) { mode == "Intave" }
    private val intaveGroundBoost by float("IntaveGroundBoost", 0.02f, 0.0f..0.1f) { mode == "Intave" }
    private val intaveEdgeSneakBoost by float("IntaveEdgeSneakBoost", 0.03f, 0.0f..0.1f) { mode == "Intave" }
    private val intaveSmallMotionBoost by float("IntaveSmallMotionBoost", 0.02f, 0.0f..0.1f) { mode == "Intave" }
    private val intaveTicksSinceSneakLimit by int("IntaveTicksSinceSneakLimit", 3, 1..10) { mode == "Intave" }

    private var ticksSneaking = 0
    private var lastSneaking = false

    override val tag: String?
        get() = when (mode) {
            "Intave" -> String.format("%.0f%%", intaveTargetSpeed * 100)
            "Custom" -> String.format("%.0f%%", customMultiplier * 100)
            else -> mode
        }

    override fun onEnable() {
        ticksSneaking = 0
        lastSneaking = false
    }

    override fun onDisable() {
        ticksSneaking = 0
        lastSneaking = false
    }

    val onSneakSlowDown = handler<SneakSlowDownEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (!player.isSneaking) return@handler

        when (mode.lowercase()) {
            "intave" -> handleIntave(event, player)
            "vanilla" -> handleVanilla(event)
            "custom" -> handleCustom(event)
        }
    }

    private fun handleIntave(event: SneakSlowDownEvent, player: net.minecraft.client.entity.EntityPlayerSP) {
        val isSneaking = player.isSneaking

        if (isSneaking && !lastSneaking) {
            ticksSneaking = 0
        }
        if (isSneaking) {
            ticksSneaking++
        } else {
            ticksSneaking = 0
        }
        lastSneaking = isSneaking

        var multiplier = intaveTargetSpeed

        if (player.isMoving) {
            multiplier += intaveMovingBoost
        }

        if (player.onGround) {
            multiplier += intaveGroundBoost
        }

        val absMotionX = MathHelper.abs(player.motionX.toFloat())
        val absMotionZ = MathHelper.abs(player.motionZ.toFloat())
        val isSmallMotion = absMotionX < 0.08f && absMotionZ < 0.08f

        if (isSmallMotion) {
            multiplier += intaveSmallMotionBoost
        }

        if (ticksSneaking <= intaveTicksSinceSneakLimit) {
            multiplier += intaveEdgeSneakBoost
        }

        multiplier = multiplier.coerceIn(0.3f, 1.0f)

        event.forward = event.forward / 0.3f * multiplier
        event.strafe = event.strafe / 0.3f * multiplier
    }

    private fun handleVanilla(event: SneakSlowDownEvent) {
        event.forward = event.forward / 0.3f
        event.strafe = event.strafe / 0.3f
    }

    private fun handleCustom(event: SneakSlowDownEvent) {
        event.forward = event.forward / 0.3f * customMultiplier
        event.strafe = event.strafe / 0.3f * customMultiplier
    }
}