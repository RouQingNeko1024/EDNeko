package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner

object HUDEdit : Module("HUDEdit", Category.CLIENT, canBeEnabled = false) {

    override fun onEnable() {
        super.onEnable()
        mc.displayGuiScreen(GuiHudDesigner())
    }
}
