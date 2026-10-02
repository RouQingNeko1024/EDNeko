package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other


import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.DebuggerLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object LowHop : SpeedMode("LowHop") {
    private var airTicks = 0
    override fun onTick() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.tryJump()
            if (DebuggerLowHop) {
                chat("Jump")
            }
            mc.thePlayer.motionY *= 0.8
            if (DebuggerLowHop) {
                chat("motionY*0.8")
            }
            airTicks = 0
        }
        if (!mc.thePlayer.onGround && airTicks == 1) {
            mc.thePlayer.motionY *= 0.5
            if (DebuggerLowHop) {
                chat("motionY*0.5")
            }
            airTicks++
        } else if (!mc.thePlayer.onGround) {
            airTicks++
        }
    }
}