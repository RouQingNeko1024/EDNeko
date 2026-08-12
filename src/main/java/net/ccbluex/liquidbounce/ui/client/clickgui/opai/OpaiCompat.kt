package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import net.ccbluex.liquidbounce.utils.client.ClientUtils
import net.minecraft.client.Minecraft

object OpaiCompat {

    object LuminRenderSystem {
        fun setActiveTarget(target: LuminRenderTarget?) {}
    }

    class LuminRenderTarget {
        companion object { fun create(name: String, w: Int, h: Int) = LuminRenderTarget() }
        fun clear() {}
        fun resize(w: Int, h: Int) {}
        fun getIdentifier(): Any? = null
        fun close() {}
    }

    object Constants {
        val mc: Minecraft = Minecraft.getMinecraft()
        const val NAME = "edneko"
        const val VERSION = ""
        val LOGGER: org.apache.logging.log4j.Logger = ClientUtils.LOGGER
    }

    class TranslateComponent(val translatedName: String)

    object EpsilonTranslateComponent {
        fun create(group: String, key: String): TranslateComponent {
            return TranslateComponent(key)
        }
    }

    object StaticFontLoader {
        val ICONS = net.ccbluex.liquidbounce.ui.font.Fonts.fontRegular35 // Fallback
    }
    
    object IMEFocusHelper {
        var activeCursorX = 0f
        var activeCursorY = 0f
        fun updateCursorPos(x: Float, y: Float) {
            activeCursorX = x
            activeCursorY = y
        }
        fun activate() {}
        fun deactivate() {}
    }
}
