package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.OpaiTextField
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.FriendManager
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager
import org.lwjgl.input.Keyboard
import java.util.stream.Collectors

class FriendOpaiPanel(panelIndex: Int) : AbstractOpaiPanel("friend", EpsilonTranslateComponent.create("gui", "tab.friend"), "", panelIndex) {

    private val noFriendsComponent = EpsilonTranslateComponent.create("gui", "friend.empty")
    private val placeholderComponent = EpsilonTranslateComponent.create("gui", "friend.input.placeholder")

    private val inputField = OpaiTextField(32)

    override fun computeContentHeight(): Float {
        val friendCount = FriendManager.getFriends().size
        return 6.0f * 2.0f + 18.0f + 4.0f + 20.0f.coerceAtLeast(friendCount.toFloat() * (20.0f + 4.0f))
    }

    override fun drawPanelContent(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, visibleHeight: Float) {
        val fieldX = panelX + 6.0f
        val fieldY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT + 6.0f - scroll
        val fieldW = panelWidth - 6.0f * 2.0f - 24.0f
        inputField.draw(renderer, fieldX, fieldY, fieldW, 18.0f, mouseX, mouseY, placeholderComponent.translatedName, OpaiTheme.SETTING_TEXT_SCALE)

        val addX = fieldX + fieldW + 4.0f
        renderer.roundRect().addRoundRect(addX, fieldY, 20.0f, 18.0f, OpaiTheme.BUTTON_RADIUS,
                if (isHovered(mouseX.toDouble(), mouseY.toDouble(), addX, fieldY, 20.0f, 18.0f)) OpaiTheme.accent(MD3Theme.PRIMARY) else OpaiTheme.surface(MD3Theme.PRIMARY_CONTAINER))
        renderer.text().addText("+", addX + 7.0f, fieldY + 2.0f, 0.72f, MD3Theme.ON_PRIMARY_CONTAINER)

        val friends = FriendManager.getFriends().sortedWith(java.lang.String.CASE_INSENSITIVE_ORDER)
        var rowY = fieldY + 18.0f + 4.0f
        if (friends.isEmpty()) {
            renderer.text().addText(noFriendsComponent.translatedName, panelX + 6.0f, rowY + 4.0f, 0.62f, MD3Theme.TEXT_MUTED)
            return
        }
        for (name in friends) {
            val hovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), panelX + 6.0f, rowY, panelWidth - 6.0f * 2.0f, 20.0f)
            renderer.roundRect().addRoundRect(panelX + 6.0f, rowY, panelWidth - 6.0f * 2.0f, 20.0f, OpaiTheme.BUTTON_RADIUS,
                    if (hovered) OpaiTheme.surface(MD3Theme.SURFACE_CONTAINER_HIGH) else OpaiTheme.surface(MD3Theme.SURFACE_CONTAINER_LOW))
            renderer.text().addText(trimToWidth(name, OpaiTheme.SETTING_TEXT_SCALE, panelWidth - 38.0f, renderer),
                    panelX + 6.0f + 6.0f, rowY + (20.0f - renderer.text().getHeight(OpaiTheme.SETTING_TEXT_SCALE)) * 0.5f,
                    OpaiTheme.SETTING_TEXT_SCALE, MD3Theme.TEXT_PRIMARY)
            val removeX = panelX + panelWidth - 6.0f - 18.0f
            renderer.text().addText("x", removeX + 5.0f, rowY + 3.0f, 0.62f,
                    if (isHovered(mouseX.toDouble(), mouseY.toDouble(), removeX, rowY + 1.0f, 16.0f, 16.0f)) MD3Theme.ERROR else MD3Theme.TEXT_MUTED)
            rowY += 20.0f + 4.0f
        }
    }

    override fun mouseClickedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false
        val fieldX = panelX + 6.0f
        val fieldY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT + 6.0f - scroll
        val fieldW = panelWidth - 6.0f * 2.0f - 24.0f
        if (inputField.focusIfContains(mouseX, mouseY, fieldX, fieldY, fieldW, 18.0f)) {
            return true
        }
        if (isHovered(mouseX, mouseY, fieldX + fieldW + 4.0f, fieldY, 20.0f, 18.0f)) {
            addFriend()
            return true
        }
        inputField.blur()

        var rowY = fieldY + 18.0f + 4.0f
        for (name in FriendManager.getFriends().sortedWith(java.lang.String.CASE_INSENSITIVE_ORDER)) {
            val removeX = panelX + panelWidth - 6.0f - 18.0f
            if (isHovered(mouseX, mouseY, removeX, rowY + 1.0f, 16.0f, 16.0f)) {
                FriendManager.removeFriend(name)
                ConfigManager.saveNow()
                return true
            }
            rowY += 20.0f + 4.0f
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!inputField.focused) return false
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            addFriend()
            return true
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            inputField.blur()
            return true
        }
        return inputField.keyPressed(keyCode)
    }

    override fun charTyped(typedText: String): Boolean {
        return if (typedText.isNotEmpty()) inputField.charTyped(typedText[0]) else false
    }

    override fun hasActiveInput(): Boolean = inputField.focused

    private fun addFriend() {
        val name = inputField.text.trim()
        if (name.isNotEmpty() && !FriendManager.isFriend(name)) {
            FriendManager.addFriend(name)
            ConfigManager.saveNow()
        }
        inputField.clear()
    }

}
