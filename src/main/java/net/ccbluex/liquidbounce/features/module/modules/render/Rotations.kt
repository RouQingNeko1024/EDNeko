/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import java.awt.Color
import kotlin.math.*

object Rotations : Module("Rotations", Category.RENDER, gameDetecting = false) {

    private val realistic by boolean("Realistic", true)
    private val body by boolean("Body", true) { !realistic }

    private val smoothRotations by boolean("SmoothRotations", false)
    private val smoothingFactor by float("SmoothFactor", 0.15f, 0.1f..0.9f) { smoothRotations }

    val debugRotations by boolean("DebugRotations", false)

    var prevHeadPitch = 0f
    var headPitch = 0f

    private var lastRotation: Rotation? = null

    private val specialCases
        get() = arrayListOf(Derp.handleEvents(), FreeCam.shouldDisableRotations()).any { it }

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.POST)
            return@handler

        val thePlayer = mc.thePlayer ?: return@handler
        val targetRotation = getRotation() ?: serverRotation

        prevHeadPitch = headPitch
        headPitch = targetRotation.pitch

        thePlayer.rotationYawHead = targetRotation.yaw

        if (shouldRotate() && body && !realistic) {
            thePlayer.renderYawOffset = thePlayer.rotationYawHead
        }

        lastRotation = targetRotation
    }

    fun lerp(tickDelta: Float, old: Float, new: Float): Float {
        return old + (new - old) * tickDelta
    }

    fun shouldRotate() = state && (specialCases || currentRotation != null)

    private fun smoothRotation(from: Rotation, to: Rotation): Rotation {
        val diffYaw = to.yaw - from.yaw
        val diffPitch = to.pitch - from.pitch

        val smoothedYaw = from.yaw + diffYaw * smoothingFactor
        val smoothedPitch = from.pitch + diffPitch * smoothingFactor

        return Rotation(smoothedYaw, smoothedPitch)
    }

    fun shouldUseRealisticMode() = realistic && shouldRotate()

    fun getRotation(): Rotation? {
        val currRotation = if (specialCases) serverRotation else currentRotation

        return if (smoothRotations && currRotation != null) {
            smoothRotation(lastRotation ?: return currRotation, currRotation)
        } else {
            currRotation
        }
    }
}
