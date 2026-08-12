/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values

import net.ccbluex.liquidbounce.config.TextValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import java.awt.Color

class AuroraTextElement(value: TextValue) : AuroraValueElement<String>(value) {
    private var focused = false
    private var text = ""
    private var cursorPos = 0
    private var cursorVisible = true
    private var cursorTimer = 0L
    
    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: Color): Float {
        val textValue = value as TextValue
        valueHeight = 35F
        
        if (!focused) {
            text = textValue.get()
        }
        
        Fonts.font35.drawString(textValue.name, x, y + 5F, Color.WHITE.rgb)
        
        val boxX = x
        val boxY = y + 18F
        val boxWidth = width - 10F
        val boxHeight = 14F
        
        RenderUtils.originalRoundedRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 3F, 
            (if (focused) Color(45, 45, 50) else Color(35, 35, 40)).rgb)
        
        if (focused) {
            RenderUtils.originalRoundedRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 3F, 
                Color(accentColor.red, accentColor.green, accentColor.blue, 50).rgb)
        }
        
        val displayText = if (text.length > 20) "..." + text.takeLast(17) else text
        Fonts.font35.drawString(displayText, boxX + 5F, boxY + 4F, Color.WHITE.rgb)
        
        if (focused) {
            if (System.currentTimeMillis() - cursorTimer > 500) {
                cursorVisible = !cursorVisible
                cursorTimer = System.currentTimeMillis()
            }
            
            if (cursorVisible) {
                val cursorX = boxX + 5F + Fonts.font35.getStringWidth(displayText.take(cursorPos.coerceIn(0, displayText.length)))
                RenderUtils.drawRect(cursorX, boxY + 3F, cursorX + 1F, boxY + 11F, Color.WHITE.rgb)
            }
        }
        
        return valueHeight
    }
    
    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean {
        val boxX = x
        val boxY = y + 18F
        val boxWidth = width - 10F
        val boxHeight = 14F
        
        if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight) {
            focused = true
            text = (value as TextValue).get()
            cursorPos = text.length
            cursorTimer = System.currentTimeMillis()
            cursorVisible = true
            return true
        } else {
            if (focused) {
                (value as TextValue).set(text)
            }
            focused = false
        }
        return false
    }
    
    override fun onKeyPress(typedChar: Char, keyCode: Int): Boolean {
        if (!focused) return false
        
        when (keyCode) {
            Keyboard.KEY_BACK -> {
                if (cursorPos > 0) {
                    text = text.removeRange(cursorPos - 1, cursorPos)
                    cursorPos--
                }
            }
            Keyboard.KEY_DELETE -> {
                if (cursorPos < text.length) {
                    text = text.removeRange(cursorPos, cursorPos + 1)
                }
            }
            Keyboard.KEY_LEFT -> {
                if (cursorPos > 0) cursorPos--
            }
            Keyboard.KEY_RIGHT -> {
                if (cursorPos < text.length) cursorPos++
            }
            Keyboard.KEY_RETURN -> {
                (value as TextValue).set(text)
                focused = false
            }
            Keyboard.KEY_ESCAPE -> {
                focused = false
            }
            else -> {
                if (typedChar.isLetterOrDigit() || typedChar in " _-+.@#$%^&*()[]{}|;:'\",<.>/?\\") {
                    text = text.substring(0, cursorPos) + typedChar + text.substring(cursorPos)
                    cursorPos++
                }
            }
        }
        
        return true
    }
    
    override fun isTyping(): Boolean = focused
}
