/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values

import net.ccbluex.liquidbounce.config.BlockValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

class AuroraBlockElement(value: BlockValue) : AuroraValueElement<Int>(value) {
    private var dragging = false
    private var animProgress = 0F

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: Color): Float {
        val blockValue = value as BlockValue
        valueHeight = 40F

        val displayValue = blockValue.get().coerceIn(blockValue.range)
        val progress = (displayValue - blockValue.minimum).toFloat() / (blockValue.maximum - blockValue.minimum)

        animProgress += (progress - animProgress) * 0.15F

        val blockName = BlockUtils.getBlockName(blockValue.get())
        val text = "${blockValue.name}: $blockName (${blockValue.get()})"
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
        val blockValue = value as BlockValue
        val sliderX = x
        val sliderWidth = width - 10F
        val newProgress = ((mouseX - sliderX) / sliderWidth).coerceIn(0F, 1F)
        val newValue = blockValue.minimum + ((blockValue.maximum - blockValue.minimum) * newProgress).toInt()
        blockValue.set(newValue.coerceIn(blockValue.range))
    }
}
