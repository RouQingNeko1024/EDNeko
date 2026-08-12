package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer

abstract class Component {

    var x = 0f
    var y = 0f
    var width = 0f

    abstract fun getHeight(): Float

    abstract fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int)

    open fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    open fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    open fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false

    open fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false

    open fun charTyped(typedText: String): Boolean = false

    fun setPosition(x: Float, y: Float, width: Float) {
        this.x = x
        this.y = y
        this.width = width
    }

    protected fun isHovered(mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float, h: Float): Boolean {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h
    }

    protected fun isHovered(mouseX: Double, mouseY: Double): Boolean {
        return isHovered(mouseX, mouseY, x, y, width, getHeight())
    }

}
