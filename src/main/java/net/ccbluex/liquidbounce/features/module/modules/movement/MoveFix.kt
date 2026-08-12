package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.util.MathHelper
import kotlin.math.abs

object MoveFix : Module("MoveFix", Category.MOVEMENT) {
    private val silentFixValue = boolean("Silent", true)

    var silentFix = false
    var doFix = false
    private var isOverwrited = false

    override fun onEnable() {
        super.onEnable()
        doFix = true
    }

    override fun onDisable() {
        super.onDisable()
        doFix = false
    }

    val onStrafe = handler<StrafeEvent> { event ->
        if (doFix) {
            runStrafeFixLoop(silentFix, event)
        }
    }

    fun applyForceStrafe(isSilent: Boolean, runStrafeFix: Boolean) {
        silentFix = isSilent; doFix = runStrafeFix; isOverwrited = true
    }

    fun updateOverwrite() {
        isOverwrited = false; doFix = state; silentFix = silentFixValue.get()
    }

    fun runStrafeFixLoop(isSilent: Boolean, event: StrafeEvent) {
        if (event.isCancelled) return

        val player = mc.thePlayer ?: return
        val (yaw) = RotationUtils.currentRotation ?: return

        var strafe = event.strafe
        var forward = event.forward
        var friction = event.friction
        var factor = strafe * strafe + forward * forward

        val angleDiff = ((MathHelper.wrapAngleTo180_float(player.rotationYaw - yaw - 22.5f - 135.0f) + 180.0) / 45.0).toInt()
        val calcYaw = if (isSilent) yaw + 45.0f * angleDiff.toFloat() else yaw

        if (isSilent) {
            when (angleDiff) {
                1, 3, 5, 7, 9 -> {
                    val calcMoveDir = abs(strafe).coerceAtLeast(abs(forward))
                    val calcMultiplier = MathHelper.sqrt_float((calcMoveDir * calcMoveDir) / 1.0f.coerceAtMost(calcMoveDir * 2.0f))

                    if ((abs(forward) > 0.005 || abs(strafe) > 0.005) && !(abs(forward) > 0.005 && abs(strafe) > 0.005)) {
                        friction /= calcMultiplier
                    } else if (abs(forward) > 0.005 && abs(strafe) > 0.005) {
                        friction *= calcMultiplier
                    }
                }
            }
        }

        if (factor >= 1.0E-4F) {
            factor = MathHelper.sqrt_float(factor).coerceAtLeast(1.0F)
            factor = friction / factor

            strafe *= factor
            forward *= factor

            val yawSin = MathHelper.sin(calcYaw.toRadians())
            val yawCos = MathHelper.cos(calcYaw.toRadians())

            player.motionX += strafe * yawCos - forward * yawSin
            player.motionZ += forward * yawCos + strafe * yawSin
        }
        event.cancelEvent()
    }

    override val tag: String
        get() = "Grim"
}
