package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object FullDisabler : Module("FullDisabler", Category.FUN) {
    override fun onEnable() {
        toggle()
        mc.shutdown()
    }
}
