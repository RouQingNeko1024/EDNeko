/*skid gold bounce
 *https://github.com/bzym2/GoldBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.SysUtils
import net.ccbluex.liquidbounce.utils.skid.fpsmaster.RawInputMod

object RawInput : Module("RawInput", Category.CLIENT) {
    override fun onEnable(){
        if(SysUtils().isAndroid()){
            chat("警告: RawInput模块在安卓上无法使用，可能会导致视角无法转动!")
            RawInputMod().stop()
            return
        } else if (SysUtils().isLinux()){
            chat("警告: RawInput模块在Linux上无法使用，可能会导致视角无法转�?")
            RawInputMod().stop()
            return
        }
        RawInputMod().start()
    }
    override fun onDisable(){
        RawInputMod().stop()
    }
}
