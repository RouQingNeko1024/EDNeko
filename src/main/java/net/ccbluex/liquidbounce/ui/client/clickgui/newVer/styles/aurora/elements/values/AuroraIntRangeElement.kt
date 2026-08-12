/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values

import net.ccbluex.liquidbounce.config.IntRangeValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color
import kotlin.math.abs

class AuroraIntRangeElement(value: IntRangeValue) : AuroraValueElement<IntRange>(value) {
    private var draggingLeft = false
    private var draggingRight = false
    private var animLeft = 0F
    private var animRight = 1F

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: Color): Float {
        val rangeValue = value as IntRangeValue
        valueHeight = 40F

        val range = rangeValue.get()
        val progressLeft = (range.first - rangeValue.minimum).toFloat() / (rangeValue.maximum - rangeValue.minimum)
        val progressRight = (range.last - rangeValue.minimum).toFloat() / (rangeValue.maximum - rangeValue.minimum)

        animLeft += (progressLeft - animLeft) * 0.15F
        animRight += (progressRight - animRight) * 0.15F

        val text = "${rangeValue.name}: ${range.first} - ${range.last}${rangeValue.suffix ?: ""}"
        Fonts.font35.drawString(text, x, y + 5F, Color.WHITE.rgb)

        val sliderX = x
        val sliderY = y + 22F
        val sliderWidth = width - 10F
        val sliderHeight = 6F

        RenderUtils.originalRoundedRect(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 3F, Color(50, 50, 55).rgb)

        val leftX = sliderX + sliderWidth * animLeft
        val rightX = sliderX + sliderWidth * animRight
        val handleY = sliderY + sliderHeight / 2F
        RenderUtils.originalRoundedRect(leftX, sliderY, rightX, sliderY + sliderHeight, 3F, accentColor.rgb)

        RenderUtils.drawFilledCircle(leftX, handleY, 5F, Color.WHITE)
        RenderUtils.drawFilledCircle(rightX, handleY, 5F, Color.WHITE)

        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean {
        val rangeValue = value as IntRangeValue
        val sliderX = x
        val sliderY = y + 22F
        val sliderWidth = width - 10F
        val sliderHeight = 6F

        val range = rangeValue.get()
        val leftX = sliderX + sliderWidth * (range.first - rangeValue.minimum) / (rangeValue.maximum - rangeValue.minimum)
        val rightX = sliderX + sliderWidth * (range.last - rangeValue.minimum) / (rangeValue.maximum - rangeValue.minimum)

        if (mouseX.toFloat() >= sliderX && mouseX.toFloat() <= sliderX + sliderWidth &&
            mouseY.toFloat() >= sliderY - 5F && mouseY.toFloat() <= sliderY + sliderHeight + 5F) {
            if (abs(mouseX - leftX) < abs(mouseX - rightX)) {
                draggingLeft = true
            } else {
                draggingRight = true
            }
            updateValue(mouseX, x, width)
            return true
        }
        return false
    }

    override fun onDrag(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (draggingLeft || draggingRight) {
            updateValue(mouseX, x, width)
        }
    }

    override fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        draggingLeft = false
        draggingRight = false
    }

    private fun updateValue(mouseX: Int, x: Float, width: Float) {
        val rangeValue = value as IntRangeValue
        val sliderX = x
        val sliderWidth = width - 10F
        val newProgress = ((mouseX - sliderX) / sliderWidth).coerceIn(0F, 1F)
        val newValue = rangeValue.minimum + ((rangeValue.maximum - rangeValue.minimum) * newProgress).toInt()
        val range = rangeValue.get()

        if (draggingLeft) {
            val newLeft = newValue.coerceIn(rangeValue.minimum, rangeValue.maximum)
            if (newLeft > range.last) {
                rangeValue.setFirst(range.last)
                rangeValue.setLast(newLeft)
                draggingLeft = false
                draggingRight = true
            } else {
                rangeValue.setFirst(newLeft)
            }
        } else if (draggingRight) {
            val newRight = newValue.coerceIn(rangeValue.minimum, rangeValue.maximum)
            if (newRight < range.first) {
                rangeValue.setLast(range.first)
                rangeValue.setFirst(newRight)
                draggingRight = false
                draggingLeft = true
            } else {
                rangeValue.setLast(newRight)
            }
        }
    }
}
