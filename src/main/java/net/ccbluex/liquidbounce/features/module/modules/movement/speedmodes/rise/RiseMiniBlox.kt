package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.rise

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.potion.Potion

object RiseMiniBlox : SpeedMode("RiseMiniBlox") {

    private var moveSpeed = 0.0
    private var payloadSent = false
    private var payloadTick = 0

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.isMoving) {
            if (player.onGround) {
                player.tryJump()
                player.motionY = 0.399
            }

            if (payloadSent && MovementUtils.groundTicks > 0) {
                MovementUtils.strafe((getBaseMoveSpeed() * 1.24).toFloat())
            } else if (MovementUtils.groundTicks > 0) {
                MovementUtils.strafe((getBaseMoveSpeed() * 1.24).toFloat())
            }
        }
    }

    private fun getBaseMoveSpeed(): Double {
        val player = mc.thePlayer ?: return 0.2873
        var base = 0.2873
        if (player.isPotionActive(Potion.moveSpeed)) {
            base *= 1.0 + 0.2 * (player.getActivePotionEffect(Potion.moveSpeed).amplifier + 1)
        }
        return base
    }

    override fun onEnable() {
        moveSpeed = 0.0
        payloadSent = false
    }

    override fun onDisable() {
        val player = mc.thePlayer ?: return
        player.stopXZ()
    }
}