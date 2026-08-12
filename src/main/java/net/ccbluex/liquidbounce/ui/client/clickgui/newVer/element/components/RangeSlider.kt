/*
 * LiquidBounce++ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/PlusPlusMC/LiquidBouncePlusPlus/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element.components

import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.ColorManager
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.extensions.animSmooth
import net.ccbluex.liquidbounce.utils.render.RenderUtils

import java.awt.Color

class RangeSlider {
    private var smoothLeft = 0F
    private var smoothRight = 100F
    private var valueLeft = 0F
    private var valueRight = 100F

    fun onDraw(x: Float, y: Float, width: Float, accentColor: Color) {
        smoothLeft = smoothLeft.animSmooth(valueLeft, 0.5F)
        smoothRight = smoothRight.animSmooth(valueRight, 0.5F)
        
        val leftPos = width * (smoothLeft / 100F)
        val rightPos = width * (smoothRight / 100F)
        
        RenderUtils.originalRoundedRect(x - 1F, y - 1F, x + width + 1F, y + 1F, 1F, ColorManager.unusedSlider.rgb)
        RenderUtils.originalRoundedRect(x - 1F + leftPos, y - 1F, x + rightPos + 1F, y + 1F, 1F, accentColor.rgb)
        
        RenderUtils.drawFilledCircle((x + leftPos).toInt(), y.toInt(), 5F, Color.white)
        RenderUtils.drawFilledCircle((x + leftPos).toInt(), y.toInt(), 3F, ColorManager.background)
        
        RenderUtils.drawFilledCircle((x + rightPos).toInt(), y.toInt(), 5F, Color.white)
        RenderUtils.drawFilledCircle((x + rightPos).toInt(), y.toInt(), 3F, ColorManager.background)
    }

    fun setValues(min: Float, max: Float, rangeMin: Float, rangeMax: Float) {
        valueLeft = (min - rangeMin) / (rangeMax - rangeMin) * 100F
        valueRight = (max - rangeMin) / (rangeMax - rangeMin) * 100F
    }
}
