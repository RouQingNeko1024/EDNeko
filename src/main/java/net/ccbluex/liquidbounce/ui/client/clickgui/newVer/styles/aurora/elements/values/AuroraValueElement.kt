/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values

import net.ccbluex.liquidbounce.config.Value
import net.minecraft.client.Minecraft

abstract class AuroraValueElement<T>(val value: Value<T>) {
    protected val mc = Minecraft.getMinecraft()
    
    open var valueHeight = 25F
    
    abstract fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: java.awt.Color): Float
    abstract fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean
    open fun onDrag(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {}
    open fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {}
    open fun onKeyPress(typedChar: Char, keyCode: Int): Boolean = false
    open fun isTyping(): Boolean = false
    
    fun isDisplayable(): Boolean = value.shouldRender()
    
    fun getHeight(): Float = if (isDisplayable()) valueHeight else 0F
}
