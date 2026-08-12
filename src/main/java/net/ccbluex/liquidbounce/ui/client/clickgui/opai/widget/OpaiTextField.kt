package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.MD3Theme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.IMEFocusHelper
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import java.util.function.Predicate
import kotlin.math.min

class OpaiTextField(val maxLength: Int, val inputFilter: Predicate<String>? = Predicate { true }) {

    var focused = false
    var text = ""
    var cursor = 0

    fun draw(renderer: OpaiRenderer, x: Float, y: Float, width: Float, height: Float, mouseX: Int, mouseY: Int, placeholder: String, textScale: Float) {
        renderer.roundRect().addRoundRect(x, y, width, height, OpaiTheme.INPUT_RADIUS,
                OpaiTheme.inputSurface(focused))
        renderer.outline().addOutline(x, y, width, height, OpaiTheme.INPUT_RADIUS,
                if (focused) 1.2f else 0.7f,
                if (focused) MD3Theme.PRIMARY else MD3Theme.withAlpha(MD3Theme.OUTLINE, 120))

        val showPlaceholder = text.isEmpty() && !focused
        var display = if (showPlaceholder) placeholder else text
        if (focused && System.currentTimeMillis() % 1000 > 500) {
            val safeCursor = min(cursor, display.length)
            display = display.substring(0, safeCursor) + "|" + display.substring(safeCursor)
        }
        val textY = y + (height - renderer.text().getLineHeight(textScale)) / 2.0f
        renderer.text().addText(trimToWidth(display, textScale, width - 8.0f, renderer), x + 4.0f, textY, textScale, if (showPlaceholder) MD3Theme.TEXT_MUTED else MD3Theme.TEXT_PRIMARY)

        if (focused) {
            val safeCursor = min(cursor, text.length)
            val caretX = x + 4.0f + renderer.text().getWidth(text.substring(0, safeCursor), textScale)
            IMEFocusHelper.updateCursorPos(caretX, textY)
        }
    }

    fun focusIfContains(mouseX: Double, mouseY: Double, x: Float, y: Float, width: Float, height: Float): Boolean {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false
        }
        focused = true
        cursor = text.length
        IMEFocusHelper.activate()
        return true
    }

    fun blur() {
        if (focused) {
            focused = false
            IMEFocusHelper.deactivate()
        }
    }

    fun keyPressed(keyCode: Int): Boolean {
        if (!focused) return false
        if (GuiScreen.isCtrlKeyDown()) {
            return handleControlShortcut(keyCode)
        }
        when (keyCode) {
            Keyboard.KEY_BACK -> {
                if (cursor > 0 && text.isNotEmpty()) {
                    text = text.substring(0, cursor - 1) + text.substring(cursor)
                    cursor--
                }
                return true
            }
            Keyboard.KEY_DELETE -> {
                if (cursor < text.length) {
                    text = text.substring(0, cursor) + text.substring(cursor + 1)
                }
                return true
            }
            Keyboard.KEY_LEFT -> {
                cursor = 0.coerceAtLeast(cursor - 1)
                return true
            }
            Keyboard.KEY_RIGHT -> {
                cursor = text.length.coerceAtMost(cursor + 1)
                return true
            }
            Keyboard.KEY_HOME -> {
                cursor = 0
                return true
            }
            Keyboard.KEY_END -> {
                cursor = text.length
                return true
            }
            else -> return false
        }
    }

    fun charTyped(typedChar: Char): Boolean {
        if (!focused) return false
        insertText(typedChar.toString())
        return true
    }

    private fun insertText(inserted: String?) {
        if (inserted == null || inserted.isEmpty()) return
        val accepted = StringBuilder()
        for (c in inserted.toCharArray()) {
            val candidate = c.toString()
            if (inputFilter?.test(candidate) != false) accepted.append(candidate)
        }
        if (accepted.isEmpty()) return
        val available = maxLength - text.length
        if (available <= 0) return
        val safe = if (accepted.length > available) accepted.substring(0, available) else accepted.toString()
        text = text.substring(0, cursor) + safe + text.substring(cursor)
        cursor += safe.length
    }

    private fun handleControlShortcut(keyCode: Int): Boolean {
        when (keyCode) {
            Keyboard.KEY_A -> {
                cursor = text.length
                return true
            }
            Keyboard.KEY_V -> {
                insertText(GuiScreen.getClipboardString())
                return true
            }
            else -> return false
        }
    }

    private fun clamp(value: String?): String {
        val v = value ?: ""
        return if (v.length > maxLength) v.substring(0, maxLength) else v
    }

    private fun trimToWidth(value: String?, scale: Float, maxWidth: Float, renderer: OpaiRenderer): String {
        if (value == null || value.isEmpty()) return ""
        if (renderer.text().getWidth(value, scale) <= maxWidth) return value
        val ellipsis = "..."
        val ellipsisWidth = renderer.text().getWidth(ellipsis, scale)
        if (ellipsisWidth >= maxWidth) return ellipsis
        for (len in value.length - 1 downTo 0) {
            val candidate = value.substring(0, len) + ellipsis
            if (renderer.text().getWidth(candidate, scale) <= maxWidth) return candidate
        }
        return ellipsis
    }

    fun clear() {
        text = ""
        cursor = 0
    }

}
