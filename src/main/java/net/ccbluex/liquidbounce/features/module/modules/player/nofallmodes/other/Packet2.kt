package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode

object Packet2 : NoFallMode("Packet2") {
    override fun onUpdate() {
        mc.thePlayer.fallDistance = 0f
    }
}