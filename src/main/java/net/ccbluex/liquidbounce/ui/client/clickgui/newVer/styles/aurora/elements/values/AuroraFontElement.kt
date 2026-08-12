/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values

import net.ccbluex.liquidbounce.config.FontValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

class AuroraFontElement(value: FontValue) : AuroraValueElement<net.ccbluex.liquidbounce.ui.font.GameFontRenderer>(value as net.ccbluex.liquidbounce.config.Value<net.ccbluex.liquidbounce.ui.font.GameFontRenderer>) {
    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: Color): Float {
        val fontValue = value as FontValue
        
        Fonts.font35.drawString(fontValue.name, x, y + 5F, Color.WHITE.rgb)
        
        val boxX = x + width - 100F
        val boxY = y + 2F
        val boxWidth = 90F
        val boxHeight = 18F
        
        RenderUtils.originalRoundedRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 4F, Color(45, 45, 50).rgb)
        
        val displayText = fontValue.displayName
        Fonts.font35.drawString(displayText, boxX + 8F, boxY + 6F, accentColor.rgb)
        
        Fonts.font35.drawString("◀ ▶", boxX + boxWidth - 25F, boxY + 6F, Color(150, 150, 155).rgb)
        
        return valueHeight
    }
    
    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean {
        val fontValue = value as FontValue
        
        val boxX = x + width - 100F
        val boxY = y + 2F
        val boxWidth = 90F
        
        if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + 18F) {
            if (mouseX < boxX + boxWidth / 2) {
                fontValue.previous()
            } else {
                fontValue.next()
            }
            return true
        }
        
        return false
    }
}
