/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values

import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

class AuroraListElement(value: ListValue) : AuroraValueElement<String>(value) {
    private var expanded = false
    private var animExpanded = 0F
    
    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: Color): Float {
        val listValue = value as ListValue
        
        Fonts.font35.drawString(listValue.name, x, y + 5F, Color.WHITE.rgb)
        
        val boxX = x + width - 100F
        val boxY = y + 2F
        val boxWidth = 90F
        val boxHeight = 18F
        
        RenderUtils.originalRoundedRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 4F, Color(45, 45, 50).rgb)
        
        val displayValue = listValue.get()
        val displayText = if (displayValue.length > 8) displayValue.take(7) + "..." else displayValue
        Fonts.font35.drawString(displayText, boxX + 8F, boxY + 6F, accentColor.rgb)
        
        Fonts.font35.drawString(if (expanded) "▲" else "▼", boxX + boxWidth - 15F, boxY + 6F, Color(150, 150, 155).rgb)
        
        if (expanded) {
            animExpanded += ((listValue.values.size * 20F) - animExpanded) * 0.2F
            
            RenderUtils.originalRoundedRect(boxX, boxY + boxHeight + 2F, boxX + boxWidth, boxY + boxHeight + 2F + animExpanded, 4F, Color(35, 35, 40).rgb)
            
            var optionY = boxY + boxHeight + 5F
            for (option in listValue.values) {
                val isSelected = option == displayValue
                val isHovered = mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= optionY && mouseY <= optionY + 16F
                
                if (isHovered) {
                    RenderUtils.originalRoundedRect(boxX + 2F, optionY - 1F, boxX + boxWidth - 2F, optionY + 15F, 3F, Color(50, 50, 55).rgb)
                }
                
                Fonts.font35.drawString(option, boxX + 8F, optionY + 4F, 
                    if (isSelected) accentColor.rgb else Color(180, 180, 185).rgb)
                
                optionY += 18F
            }
            
            valueHeight = 25F + animExpanded
        } else {
            animExpanded *= 0.8F
            valueHeight = 25F
        }
        
        return valueHeight
    }
    
    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean {
        val listValue = value as ListValue
        
        val boxX = x + width - 100F
        val boxY = y + 2F
        val boxWidth = 90F
        val boxHeight = 18F
        
        if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight) {
            expanded = !expanded
            return true
        }
        
        if (expanded) {
            var optionY = boxY + boxHeight + 5F
            for (option in listValue.values) {
                if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= optionY && mouseY <= optionY + 16F) {
                    listValue.set(option)
                    expanded = false
                    return true
                }
                optionY += 18F
            }
        }
        
        return false
    }
}
