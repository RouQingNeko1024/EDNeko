package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.component.Component
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.Setting

abstract class SettingWidget<S>(val setting: S) : Component() {

    open fun isVisible(): Boolean {
        if (setting is Setting<*>) {
            return setting.isAvailable()
        }
        return true
    }

}
