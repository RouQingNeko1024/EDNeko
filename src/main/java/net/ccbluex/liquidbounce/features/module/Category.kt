/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.minecraft.util.ResourceLocation

enum class Category(val displayName: String) {

    COMBAT("战斗"),
    PLAYER("玩家"),
    MOVEMENT("移动"),
    RENDER("渲染"),
    WORLD("世界"),
    MISC("杂项"),
    EXPLOIT("漏洞"),
    FUN("娱乐"),
    CLIENT("客户端");

    companion object {
        @JvmStatic
        val INSTANCE: Array<Category>
            get() = values()
    }

    val iconResourceLocation = ResourceLocation("${CLIENT_NAME.lowercase()}/tabgui/${name.lowercase()}.png")

    fun shouldShow(): Boolean = true

}
