package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.config.ColorValue
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.ColorManager
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.element.module.value.ValueElement
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MouseUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.math.roundToInt

class ColorElement(val savedValue: ColorValue) : ValueElement<Color>(savedValue) {

    private var expanded = false
    private var animExpanded = 0F
    
    private var isDragging = false
    
    init {
        valueHeight = 24F
    }

    override fun drawElement(
        mouseX: Int,
        mouseY: Int,
        x: Float,
        y: Float,
        width: Float,
        bgColor: Color,
        accentColor: Color
    ): Float {
        val currentColor = savedValue.selectedColor()
        
        val colorPreviewSize = 12
        val boxX = x + width - 30F
        val boxY = y + 6F
        
        Fonts.font40.drawString(savedValue.name, x + 10F, y + 10F - Fonts.font40.FONT_HEIGHT / 2F + 2F, -1)
        
        RenderUtils.originalRoundedRect(boxX, boxY, boxX + colorPreviewSize, boxY + colorPreviewSize, 2F, ColorManager.buttonOutline.rgb)
        RenderUtils.originalRoundedRect(boxX + 1, boxY + 1, boxX + colorPreviewSize - 1, boxY + colorPreviewSize - 1, 2F, currentColor.rgb)
        
        if (savedValue.rainbow) {
            val rainbowColor = ColorUtils.rainbow(alpha = savedValue.opacitySliderY)
            val rainbowColorInt = rainbowColor.rgb
            RenderUtils.originalRoundedRect(boxX + colorPreviewSize + 2, boxY, boxX + colorPreviewSize * 2 + 2, boxY + colorPreviewSize, 2F, ColorManager.buttonOutline.rgb)
            RenderUtils.originalRoundedRect(boxX + colorPreviewSize + 3, boxY + 1, boxX + colorPreviewSize * 2 + 1, boxY + colorPreviewSize - 1, 2F, rainbowColorInt)
        }
        
        animExpanded = if (expanded) (animExpanded + 0.2F * RenderUtils.deltaTime * 0.025F).coerceAtMost(1F)
        else (animExpanded - 0.2F * RenderUtils.deltaTime * 0.025F).coerceAtLeast(0F)
        
        if (animExpanded > 0F && expanded) {
            var totalHeight = valueHeight
            
            val pickerWidth = 100F
            val pickerHeight = 60F
            val sliderWidth = 10F
            val spacing = 5F
            
            val pickerX = x + 10F
            val pickerY = y + valueHeight
            
            val hueSliderX = pickerX + pickerWidth + spacing
            val opacitySliderX = hueSliderX + sliderWidth + spacing
            
            val hue = if (savedValue.rainbow) {
                Color.RGBtoHSB(currentColor.red, currentColor.green, currentColor.blue, null)[0]
            } else {
                savedValue.hueSliderY
            }
            
            drawColorPicker(pickerX, pickerY, pickerWidth, pickerHeight, hue)
            drawHueSlider(hueSliderX, pickerY, sliderWidth, pickerHeight)
            drawOpacitySlider(opacitySliderX, pickerY, sliderWidth, pickerHeight, currentColor)
            
            if (!savedValue.rainbow) {
                val markerX = pickerX + savedValue.colorPickerPos.x * pickerWidth
                val markerY = pickerY + savedValue.colorPickerPos.y * pickerHeight
                RenderUtils.originalRoundedRect(markerX - 2, markerY - 2, markerX + 2, markerY + 2, 1F, Color.WHITE.rgb)
            }
            
            val hueMarkerY = pickerY + hue * pickerHeight
            RenderUtils.originalRoundedRect(hueSliderX - 1, hueMarkerY - 1, hueSliderX + sliderWidth + 1, hueMarkerY + 1, 1F, Color.WHITE.rgb)
            
            val opacityMarkerY = pickerY + (1 - savedValue.opacitySliderY) * pickerHeight
            RenderUtils.originalRoundedRect(opacitySliderX - 1, opacityMarkerY - 1, opacitySliderX + sliderWidth + 1, opacityMarkerY + 1, 1F, Color.WHITE.rgb)
            
            val rainbowY = pickerY + pickerHeight + 5F
            val isHoveringRainbow = MouseUtils.mouseWithinBounds(mouseX, mouseY, pickerX, rainbowY, pickerX + 80F, rainbowY + 14F)
            
            RenderUtils.originalRoundedRect(pickerX, rainbowY, pickerX + 80F, rainbowY + 14F, 3F,
                if (savedValue.rainbow) accentColor.rgb else if (isHoveringRainbow) ColorManager.buttonOutline.rgb else ColorManager.textBox.rgb)
            Fonts.font35.drawString("Rainbow", pickerX + 8F, rainbowY + 7F - Fonts.font35.FONT_HEIGHT / 2F, -1)
            
            totalHeight += pickerHeight + 25F
            
            return totalHeight
        }
        
        return valueHeight
    }
    
    private fun drawColorPicker(x: Float, y: Float, width: Float, height: Float, hue: Float) {
        for (px in 0 until width.toInt()) {
            for (py in 0 until height.toInt()) {
                val s = px / width
                val b = 1f - py / height
                val rgb = Color.HSBtoRGB(hue, s, b)
                RenderUtils.drawRect(x + px, y + py, x + px + 1, y + py + 1, rgb)
            }
        }
    }
    
    private fun drawHueSlider(x: Float, y: Float, width: Float, height: Float) {
        for (py in 0 until height.toInt()) {
            val hue = py / height
            val rgb = Color.HSBtoRGB(hue, 1f, 1f)
            RenderUtils.drawRect(x, y + py, x + width, y + py + 1, rgb)
        }
    }
    
    private fun drawOpacitySlider(x: Float, y: Float, width: Float, height: Float, color: Color) {
        for (py in 0 until height.toInt()) {
            val alpha = (1f - py / height) * 255
            val checkerboard = if ((py / 4).toInt() % 2 == 0) Color.WHITE else Color.GRAY
            val blended = blendColors(checkerboard, color, alpha / 255f)
            RenderUtils.drawRect(x, y + py, x + width, y + py + 1, blended.rgb)
        }
    }
    
    private fun blendColors(c1: Color, c2: Color, factor: Float): Color {
        val r = (c1.red * (1 - factor) + c2.red * factor).toInt()
        val g = (c1.green * (1 - factor) + c2.green * factor).toInt()
        val b = (c1.blue * (1 - factor) + c2.blue * factor).toInt()
        return Color(r, g, b)
    }
    
    private fun handlePickerInput(mouseX: Int, mouseY: Int, pickerX: Float, pickerY: Float, 
                                   pickerWidth: Float, pickerHeight: Float, 
                                   hueSliderX: Float, opacitySliderX: Float, sliderWidth: Float): Boolean {
        if (!Mouse.isButtonDown(0)) {
            savedValue.lastChosenSlider = null
            isDragging = false
            return false
        }
        
        val inPicker = mouseX >= pickerX && mouseX <= pickerX + pickerWidth && 
                       mouseY >= pickerY && mouseY <= pickerY + pickerHeight && !savedValue.rainbow
        val inHueSlider = mouseX >= hueSliderX && mouseX <= hueSliderX + sliderWidth && 
                          mouseY >= pickerY && mouseY <= pickerY + pickerHeight && !savedValue.rainbow
        val inOpacitySlider = mouseX >= opacitySliderX && mouseX <= opacitySliderX + sliderWidth && 
                              mouseY >= pickerY && mouseY <= pickerY + pickerHeight
        
        val sliderType = savedValue.lastChosenSlider
        
        if (inPicker || sliderType == ColorValue.SliderType.COLOR) {
            val newS = ((mouseX - pickerX) / pickerWidth).coerceIn(0f, 1f)
            val newB = (1f - (mouseY - pickerY) / pickerHeight).coerceIn(0f, 1f)
            savedValue.colorPickerPos.x = newS
            savedValue.colorPickerPos.y = 1 - newB
            updateColor()
            if (inPicker && sliderType == null) savedValue.lastChosenSlider = ColorValue.SliderType.COLOR
            isDragging = true
            return true
        }
        
        if (inHueSlider || sliderType == ColorValue.SliderType.HUE) {
            savedValue.hueSliderY = ((mouseY - pickerY) / pickerHeight).coerceIn(0f, 1f)
            updateColor()
            if (inHueSlider && sliderType == null) savedValue.lastChosenSlider = ColorValue.SliderType.HUE
            isDragging = true
            return true
        }
        
        if (inOpacitySlider || sliderType == ColorValue.SliderType.OPACITY) {
            savedValue.opacitySliderY = (1f - (mouseY - pickerY) / pickerHeight).coerceIn(0f, 1f)
            updateColor()
            if (inOpacitySlider && sliderType == null) savedValue.lastChosenSlider = ColorValue.SliderType.OPACITY
            isDragging = true
            return true
        }
        
        return false
    }
    
    private fun updateColor() {
        if (!savedValue.rainbow) {
            val rgb = Color.HSBtoRGB(savedValue.hueSliderY, savedValue.colorPickerPos.x, 1 - savedValue.colorPickerPos.y)
            val baseColor = Color(rgb)
            val color = Color(baseColor.red, baseColor.green, baseColor.blue, (savedValue.opacitySliderY * 255).roundToInt())
            savedValue.set(color)
        }
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean {
        val colorPreviewSize = 12
        val boxX = x + width - 30F
        val boxY = y + 6F
        
        if (MouseUtils.mouseWithinBounds(mouseX, mouseY, boxX, boxY, boxX + colorPreviewSize, boxY + colorPreviewSize)) {
            if (savedValue.rainbow) savedValue.rainbow = false
            expanded = !expanded
            return true
        }
        
        if (MouseUtils.mouseWithinBounds(mouseX, mouseY, boxX + colorPreviewSize + 2, boxY, boxX + colorPreviewSize * 2 + 2, boxY + colorPreviewSize)) {
            savedValue.rainbow = true
            expanded = !expanded
            return true
        }
        
        if (expanded) {
            val pickerWidth = 100F
            val pickerHeight = 60F
            val sliderWidth = 10F
            val spacing = 5F
            
            val pickerX = x + 10F
            val pickerY = y + valueHeight
            val hueSliderX = pickerX + pickerWidth + spacing
            val opacitySliderX = hueSliderX + sliderWidth + spacing
            
            val rainbowY = pickerY + pickerHeight + 5F
            val totalHeight = pickerHeight + 25F
            
            if (MouseUtils.mouseWithinBounds(mouseX, mouseY, pickerX, rainbowY, pickerX + 80F, rainbowY + 14F)) {
                savedValue.rainbow = !savedValue.rainbow
                return true
            }
            
            if (MouseUtils.mouseWithinBounds(mouseX, mouseY, pickerX, pickerY, opacitySliderX + sliderWidth, pickerY + pickerHeight)) {
                return true
            }
            
            if (MouseUtils.mouseWithinBounds(mouseX, mouseY, x, y, x + width, y + valueHeight + totalHeight)) {
                return true
            }
        }
        
        return false
    }
    
    override fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        savedValue.lastChosenSlider = null
        isDragging = false
    }
    
    override fun onDrag(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (!expanded) return
        
        val pickerWidth = 100F
        val pickerHeight = 60F
        val sliderWidth = 10F
        val spacing = 5F
        
        val pickerX = x + 10F
        val pickerY = y + valueHeight
        val hueSliderX = pickerX + pickerWidth + spacing
        val opacitySliderX = hueSliderX + sliderWidth + spacing
        
        handlePickerInput(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight, hueSliderX, opacitySliderX, sliderWidth)
    }
    
    override fun onKeyPress(typed: Char, keyCode: Int): Boolean = false
    
    fun isExpanded(): Boolean = expanded
    
    fun isDraggingColor(): Boolean = isDragging
}
