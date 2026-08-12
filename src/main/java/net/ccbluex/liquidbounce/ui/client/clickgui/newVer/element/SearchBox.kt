/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiTextField

class SearchBox(componentId: Int, x: Int, y: Int, width: Int, height: Int): GuiTextField(componentId, Minecraft.getMinecraft().fontRendererObj, x, y, width, height) {
    override fun getEnableBackgroundDrawing() = false
    
    init {
        setMaxStringLength(50)
    }
}