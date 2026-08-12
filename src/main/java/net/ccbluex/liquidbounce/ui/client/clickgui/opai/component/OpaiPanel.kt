package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer

interface OpaiPanel {

    fun getId(): String

    fun startIntro()

    fun getIntroValue(): Float

    fun drawBackground(renderer: OpaiRenderer)

    fun drawContent(renderer: OpaiRenderer, mouseX: Int, mouseY: Int)

    fun getContentClipY(): Float

    fun getContentClipHeight(): Float

    fun getPanelHeight(): Float

    fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean

    fun mouseReleased(mouseX: Int, mouseY: Int, button: Int): Boolean

    fun mouseDragged(mouseX: Int, mouseY: Int)

    fun mouseScrolled(mouseX: Int, mouseY: Int, amount: Float): Boolean

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean

    fun charTyped(typedText: String): Boolean

    fun hasActiveInput(): Boolean

    fun setPosition(x: Float, y: Float)

    fun setMaxPanelHeight(maxPanelHeight: Float)

    fun getX(): Float

    fun getY(): Float

    fun getWidth(): Float

    fun isOpened(): Boolean

    fun setOpened(opened: Boolean)

    fun isVisible(): Boolean

    fun setVisible(visible: Boolean)

}
