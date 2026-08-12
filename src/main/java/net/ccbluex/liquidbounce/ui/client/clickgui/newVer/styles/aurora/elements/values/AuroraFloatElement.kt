/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values

import net.ccbluex.liquidbounce.config.FloatValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color
import kotlin.math.round

class AuroraFloatElement(value: FloatValue) : AuroraValueElement<Float>(value) {
    private var dragging = false
    private var animProgress = 0F

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: Color): Float {
        val floatValue = value as FloatValue
        valueHeight = 40F

        val displayValue = floatValue.get().coerceIn(floatValue.range)
        val progress = (displayValue - floatValue.minimum) / (floatValue.maximum - floatValue.minimum)

        animProgress += (progress - animProgress) * 0.15F

        val text = "${floatValue.name}: ${String.format("%.2f", floatValue.get())}${floatValue.suffix ?: ""}"
        Fonts.font35.drawString(text, x, y + 5F, Color.WHITE.rgb)

        val sliderX = x
        val sliderY = y + 22F
        val sliderWidth = width - 10F
        val sliderHeight = 6F

        RenderUtils.originalRoundedRect(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 3F, Color(50, 50, 55).rgb)

        val filledWidth = sliderWidth * animProgress
        RenderUtils.originalRoundedRect(sliderX, sliderY, sliderX + filledWidth, sliderY + sliderHeight, 3F, accentColor.rgb)

        val handleX = sliderX + filledWidth
        val handleY = sliderY + sliderHeight / 2F
        RenderUtils.drawFilledCircle(handleX, handleY, 5F, Color.WHITE)
        RenderUtils.drawFilledCircle(handleX, handleY, 3F, accentColor)

        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean {
        val sliderX = x
        val sliderY = y + 22F
        val sliderWidth = width - 10F
        val sliderHeight = 6F

        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= sliderY - 5F && mouseY <= sliderY + sliderHeight + 5F) {
            dragging = true
            updateValue(mouseX, x, width)
            return true
        }
        return false
    }

    override fun onDrag(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (dragging) {
            updateValue(mouseX, x, width)
        }
    }

    override fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        dragging = false
    }

    private fun updateValue(mouseX: Int, x: Float, width: Float) {
        val floatValue = value as FloatValue
        val sliderX = x
        val sliderWidth = width - 10F
        val newProgress = ((mouseX - sliderX) / sliderWidth).coerceIn(0F, 1F)
        val newValue = floatValue.minimum + (floatValue.maximum - floatValue.minimum) * newProgress
        floatValue.set(newValue.coerceIn(floatValue.range))
    }
}
