/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.config.IntRangeValue
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.ColorManager
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element.components.RangeSlider
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils

import java.awt.Color

class IntRangeElement(val savedValue: IntRangeValue) : ValueElement<IntRange>(savedValue) {
    private val slider = RangeSlider()
    private var draggedLeft = false
    private var draggedRight = false

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        val maxLength = Fonts.font40.getStringWidth("${savedValue.maximum}")
        val minLength = Fonts.font40.getStringWidth("${savedValue.minimum}")
        val nameLength = Fonts.font40.getStringWidth(savedValue.name)
        val valueDisplay = 50F + Fonts.font40.getStringWidth("${savedValue.maximum}-${savedValue.maximum}")
        val sliderWidth = width - 60F - nameLength - maxLength - minLength - valueDisplay
        val startPoint = x + width - 20F - sliderWidth - maxLength - valueDisplay
        
        val range = savedValue.get()
        if (draggedLeft) {
            val newFirst = (savedValue.minimum + (savedValue.maximum - savedValue.minimum) / sliderWidth * (mouseX - startPoint)).toInt()
                .coerceIn(savedValue.minimum, range.last)
            savedValue.setFirst(newFirst)
        }
        if (draggedRight) {
            val newLast = (savedValue.minimum + (savedValue.maximum - savedValue.minimum) / sliderWidth * (mouseX - startPoint)).toInt()
                .coerceIn(range.first, savedValue.maximum)
            savedValue.setLast(newLast)
        }
        
        Fonts.font40.drawString(savedValue.name, x + 10F, y + 10F - Fonts.font40.FONT_HEIGHT / 2F + 2F, -1)
        Fonts.font40.drawString("${savedValue.maximum}", 
                                x + width - 10F - maxLength - valueDisplay, 
                                y + 10F - Fonts.font40.FONT_HEIGHT / 2F + 2F, -1)
        Fonts.font40.drawString("${savedValue.minimum}", 
                                x + width - 30F - sliderWidth - maxLength - minLength - valueDisplay, 
                                y + 10F - Fonts.font40.FONT_HEIGHT / 2F + 2F, -1)
        
        slider.setValues(range.first.toFloat(), range.last.toFloat(), savedValue.minimum.toFloat(), savedValue.maximum.toFloat())
        slider.onDraw(x + width - 20F - sliderWidth - maxLength - valueDisplay, y + 10F, sliderWidth, accentColor)
        
        RenderUtils.originalRoundedRect(x + width - 5F - valueDisplay, y + 2F, x + width - 10F, y + 18F, 4F, ColorManager.button.rgb)
        RenderUtils.customRounded(x + width - 5F - valueDisplay, y + 2F, x + width + 3F - valueDisplay, y + 18F, 4F, 0F, 0F, 4F, ColorManager.buttonOutline.rgb)
        
        Fonts.font40.drawString("${range.first}-${range.last}", x + width + 6F - valueDisplay, y + 10F - Fonts.font40.FONT_HEIGHT / 2F + 2F, -1)

        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean {
        val maxLength = Fonts.font40.getStringWidth("${savedValue.maximum}")
        val minLength = Fonts.font40.getStringWidth("${savedValue.minimum}")
        val nameLength = Fonts.font40.getStringWidth(savedValue.name)
        val valueDisplay = 50F + Fonts.font40.getStringWidth("${savedValue.maximum}-${savedValue.maximum}")
        val sliderWidth = width - 60F - nameLength - maxLength - minLength - valueDisplay
        val startPoint = x + width - 20F - sliderWidth - maxLength - valueDisplay
        
        val range = savedValue.get()
        val leftPos = startPoint + sliderWidth * (range.first - savedValue.minimum) / (savedValue.maximum - savedValue.minimum)
        val rightPos = startPoint + sliderWidth * (range.last - savedValue.minimum) / (savedValue.maximum - savedValue.minimum)
        
        if (MouseUtils.mouseWithinBounds(mouseX, mouseY, startPoint, y + 5F, rightPos - 10F, y + 15F)) {
            draggedLeft = true
            return true
        } else if (MouseUtils.mouseWithinBounds(mouseX, mouseY, leftPos + 10F, y + 5F, startPoint + sliderWidth, y + 15F)) {
            draggedRight = true
            return true
        } else if (MouseUtils.mouseWithinBounds(mouseX, mouseY, startPoint, y + 5F, startPoint + sliderWidth, y + 15F)) {
            val clickPos = mouseX - startPoint
            val leftDist = Math.abs(clickPos - (leftPos - startPoint))
            val rightDist = Math.abs(clickPos - (rightPos - startPoint))
            if (leftDist < rightDist) {
                draggedLeft = true
            } else {
                draggedRight = true
            }
            return true
        }
        return false
    }

    override fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        draggedLeft = false
        draggedRight = false
    }
}
