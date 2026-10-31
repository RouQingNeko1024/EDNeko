package net.ccbluex.liquidbounce.features.module.modules.movement.loftilyflys

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.utils.extensions.isMoving

class VanillaFly(parent: LoftilyFly) : LoftilyFlyMode("Vanilla", parent) {

    val smooth by boolean("Smooth", false)
    val spoofGround by boolean("SpoofGround", false)
    val noClip by boolean("NoClip", false)
    val horizontalSpeed by float("Vanilla-HorizontalSpeed", 1.0f, 0f..5f)
    val verticalSpeed by float("Vanilla-VerticalSpeed", 1.0f, 0f..5f)
    val keepY by float("Vanilla-KeepYSpeed", 0.0f, -0.2f..0.2f)
    val resetMotion by boolean("ResetMotion", false)

    override fun onDisable() {
        mc.thePlayer.noClip = false
        mc.thePlayer.capabilities.isFlying = false
    }

    override fun onMotion(event: MotionEvent) {
        if (spoofGround) {
            event.onGround = true
        }
    }

    override fun onUpdate() {
        val player = mc.thePlayer
        if (noClip) {
            player.noClip = true
        }
        if (!player.isMoving && resetMotion) {
            player.motionX *= 0.0
            player.motionZ *= 0.0
            player.motionY *= 0.0
        }
        if (smooth) {
            player.capabilities.isFlying = true
            player.capabilities.flySpeed = horizontalSpeed * 0.05f
        } else {
            setSpeed(horizontalSpeed.toDouble(), true)
            player.motionY *= keepY.toDouble()
            if (mc.gameSettings.keyBindJump.isKeyDown) {
                player.motionY = verticalSpeed.toDouble()
            }
            if (mc.gameSettings.keyBindSneak.isKeyDown) {
                player.motionY = -verticalSpeed.toDouble()
            }
        }
    }
}