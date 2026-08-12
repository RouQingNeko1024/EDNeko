/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.guiColor
import net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI.scale
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui.clamp
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts.fontSemibold35
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.blendColors
import net.ccbluex.liquidbounce.utils.render.ColorUtils.minecraftRed
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexture
import net.ccbluex.liquidbounce.utils.render.RenderUtils.updateTextureCache
import net.ccbluex.liquidbounce.utils.ui.EditableText
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.StringUtils
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color
import kotlin.math.abs
import kotlin.math.roundToInt

@SideOnly(Side.CLIENT)
object MinimalStyle : Style() {
    private val backgroundColor = Color(250, 250, 252)
    private val headerColor = Color(255, 255, 255)
    private val borderColor = Color(230, 230, 235)
    private val textColor = Color(50, 50, 55)
    private val secondaryTextColor = Color(140, 140, 145)
    private val sliderTrackColor = Color(235, 235, 240)
    
    private fun drawMinimalPanel(x: Int, y: Int, x2: Int, y2: Int, accentColor: Color) {
        drawRect(x, y, x2, y2, backgroundColor.rgb)
        drawBorderedRect(x, y, x2, y2, 1, borderColor.rgb, backgroundColor.rgb)
        
        drawRect(x, y, x2, y + 20, headerColor.rgb)
        drawRect(x, y + 19, x2, y + 20, accentColor.rgb)
    }
    
    private fun drawMinimalSlider(x: Float, y: Float, width: Float, height: Float, progress: Float, color: Color) {
        drawRect(x, y, x + width, y + height, sliderTrackColor.rgb)
        
        val sliderWidth = width * progress
        drawRect(x, y, x + sliderWidth, y + height, color.rgb)
        
        val handleX = x + sliderWidth - 3
        drawRect(handleX, y - 1, handleX + 6, y + height + 1, Color.WHITE.rgb)
        drawBorderedRect(handleX, y - 1, handleX + 6, y + height + 1, 1F, color.rgb, Color.WHITE.rgb)
    }
    
    private fun drawMinimalToggle(x: Float, y: Float, width: Float, height: Float, enabled: Boolean, color: Color) {
        val trackColor = if (enabled) color else sliderTrackColor
        drawRect(x, y, x + width, y + height, trackColor.rgb)
        
        val handleSize = height - 4
        val handleX = if (enabled) x + width - handleSize - 2 else x + 2
        drawRect(handleX, y + 2, handleX + handleSize, y + 2 + handleSize, Color.WHITE.rgb)
    }

    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        val accentColor = Color(guiColor)
        
        drawMinimalPanel(panel.x, panel.y, panel.x + panel.width, panel.y + panel.height + panel.fade, accentColor)
        
        val xPos = panel.x - (fontSemibold35.getStringWidth(StringUtils.stripControlCodes(panel.name)) - 100) / 2
        fontSemibold35.drawString(panel.name, xPos, panel.y + 7, textColor.rgb)

        if (panel.scrollbar && panel.fade > 0) {
            drawRect(panel.x - 2, panel.y + 21, panel.x, panel.y + 16 + panel.fade, sliderTrackColor.rgb)

            val visibleRange = panel.getVisibleRange()
            val minY =
                panel.y + 21 + panel.fade * if (visibleRange.first > 0) visibleRange.first / panel.elements.lastIndex.toFloat()
                else 0f
            val maxY =
                panel.y + 16 + panel.fade * if (visibleRange.last > 0) visibleRange.last / panel.elements.lastIndex.toFloat()
                else 0f

            drawRect(panel.x - 2, minY.roundToInt(), panel.x, maxY.roundToInt(), accentColor.rgb)
        }
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.lines()

        val width = lines.maxOfOrNull { fontSemibold35.getStringWidth(it) + 14 }
            ?: return
        val height = fontSemibold35.fontHeight * lines.size + 3

        val (scaledWidth, scaledHeight) = ScaledResolution(mc)
        val x = mouseX.clamp(0, (scaledWidth / scale - width).roundToInt())
        val y = mouseY.clamp(0, (scaledHeight / scale - height).roundToInt())

        drawRect(x + 9, y, x + width, y + height, backgroundColor.rgb)
        drawBorderedRect(x + 9, y, x + width, y + height, 1, borderColor.rgb, backgroundColor.rgb)
        
        lines.forEachIndexed { index, text ->
            fontSemibold35.drawString(text, x + 12, y + 3 + (fontSemibold35.fontHeight) * index, textColor.rgb)
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
        val xPos = buttonElement.x - (fontSemibold35.getStringWidth(buttonElement.displayName) - 100) / 2
        fontSemibold35.drawString(buttonElement.displayName, xPos, buttonElement.y + 6, buttonElement.color)
    }

    override fun drawModuleElementAndClick(
        mouseX: Int, mouseY: Int, moduleElement: ModuleElement, mouseButton: Int?
    ): Boolean {
        val accentColor = Color(guiColor)
        
        val isHovered = mouseX >= moduleElement.x && mouseX <= moduleElement.x + moduleElement.width &&
                       mouseY >= moduleElement.y && mouseY <= moduleElement.y + moduleElement.height
        
        if (isHovered) {
            drawRect(moduleElement.x, moduleElement.y, moduleElement.x + moduleElement.width, 
                    moduleElement.y + moduleElement.height, Color(245, 245, 248).rgb)
        }
        
        val xPos = moduleElement.x - (fontSemibold35.getStringWidth(moduleElement.displayName) - 100) / 2
        val textClr = when {
            moduleElement.module.state -> accentColor
            else -> textColor
        }
        
        fontSemibold35.drawString(moduleElement.displayName, xPos, moduleElement.y + 6, textClr.rgb)

        val moduleValues = moduleElement.module.values.filter { it.shouldRender() }
        if (moduleValues.isNotEmpty()) {
            val expandChar = if (moduleElement.showSettings) "−" else "+"
            fontSemibold35.drawString(
                expandChar,
                moduleElement.x + moduleElement.width - 10,
                moduleElement.y + moduleElement.height / 2,
                if (moduleElement.showSettings) accentColor.rgb else secondaryTextColor.rgb
            )

            if (moduleElement.showSettings) {
                var yPos = moduleElement.y + 4

                val minX = moduleElement.x + moduleElement.width + 4
                val maxX = moduleElement.x + moduleElement.width + moduleElement.settingsWidth

                if (moduleElement.settingsWidth > 0 && moduleElement.settingsHeight > 0) {
                    drawRect(minX, yPos, maxX, yPos + moduleElement.settingsHeight, backgroundColor.rgb)
                    drawBorderedRect(minX, yPos, maxX, yPos + moduleElement.settingsHeight, 1, borderColor.rgb, backgroundColor.rgb)
                }

                for (value in moduleValues) {
                    assumeNonVolatile = value.get() is Number

                    val suffix = value.suffix ?: ""

                    when (value) {
                        is BoolValue -> {
                            val text = value.name
                            moduleElement.settingsWidth = fontSemibold35.getStringWidth(text) + 40

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 2..yPos + 16) {
                                value.toggle()
                                clickSound()
                                return true
                            }

                            fontSemibold35.drawString(text, minX + 2, yPos + 5, textColor.rgb)
                            drawMinimalToggle(maxX.toFloat() - 28, yPos + 3F, 24F, 12F, value.get(), accentColor)

                            yPos += 18
                        }

                        is ListValue -> {
                            val text = value.name
                            moduleElement.settingsWidth = fontSemibold35.getStringWidth(text) + 16

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 2..yPos + 16) {
                                value.openList = !value.openList
                                clickSound()
                                return true
                            }

                            fontSemibold35.drawString(text, minX + 2, yPos + 5, textColor.rgb)
                            fontSemibold35.drawString(
                                if (value.openList) "▲" else "▼",
                                maxX - 10,
                                yPos + 5,
                                accentColor.rgb
                            )

                            yPos += 18

                            for (valueOfList in value.values) {
                                moduleElement.settingsWidth = fontSemibold35.getStringWidth(valueOfList) + 16

                                if (value.openList) {
                                    if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 2..yPos + 16) {
                                        value.set(valueOfList)
                                        clickSound()
                                        return true
                                    }

                                    val selected = value.get() == valueOfList
                                    if (selected) {
                                        drawRect(minX + 2, yPos + 4, minX + 4, yPos + 12, accentColor.rgb)
                                    }
                                    
                                    fontSemibold35.drawString(
                                        valueOfList,
                                        minX + 8,
                                        yPos + 5,
                                        if (selected) accentColor.rgb else secondaryTextColor.rgb
                                    )

                                    yPos += 16
                                }
                            }
                        }

                        is FloatValue -> {
                            val text = value.name + ": " + round(value.get()) + " ${suffix}"
                            moduleElement.settingsWidth = fontSemibold35.getStringWidth(text) + 8

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 23 || sliderValueHeld == value) {
                                val percentage = (mouseX - minX - 4) / (maxX - minX - 8).toFloat()
                                value.setAndSaveValueOnButtonRelease(
                                    round(value.minimum + (value.maximum - value.minimum) * percentage).coerceIn(
                                        value.range
                                    )
                                )

                                sliderValueHeld = value

                                if (mouseButton == 0) return true
                            }

                            fontSemibold35.drawString(text, minX + 2, yPos + 4, textColor.rgb)
                            
                            val displayValue = value.get().coerceIn(value.range)
                            val progress = (displayValue - value.minimum) / (value.maximum - value.minimum)
                            drawMinimalSlider(minX.toFloat() + 4, yPos + 18F, maxX.toFloat() - minX - 8F, 6F, progress, accentColor)

                            yPos += 28
                        }

                        is BlockValue -> {
                            val text = value.name + ": " + getBlockName(value.get()) + " (" + value.get() + ")" + " ${suffix}"
                            moduleElement.settingsWidth = fontSemibold35.getStringWidth(text) + 8

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 23 || sliderValueHeld == value) {
                                val percentage = (mouseX - minX - 4) / (maxX - minX - 8).toFloat()
                                value.setAndSaveValueOnButtonRelease(
                                    (value.minimum + (value.maximum - value.minimum) * percentage).roundToInt()
                                        .coerceIn(value.range)
                                )

                                sliderValueHeld = value

                                if (mouseButton == 0) return true
                            }

                            fontSemibold35.drawString(text, minX + 2, yPos + 4, textColor.rgb)
                            
                            val displayValue = value.get().coerceIn(value.range)
                            val progress = (displayValue - value.minimum).toFloat() / (value.maximum - value.minimum)
                            drawMinimalSlider(minX.toFloat() + 4, yPos + 18F, maxX.toFloat() - minX - 8F, 6F, progress, accentColor)

                            yPos += 28
                        }

                        is IntValue -> {
                            val text = value.name + ": " + value.get() + " ${suffix}"
                            moduleElement.settingsWidth = fontSemibold35.getStringWidth(text) + 8

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos + 15..yPos + 23 || sliderValueHeld == value) {
                                val percentage = (mouseX - minX - 4) / (maxX - minX - 8).toFloat()
                                value.setAndSaveValueOnButtonRelease(
                                    (value.minimum + (value.maximum - value.minimum) * percentage).roundToInt()
                                        .coerceIn(value.range)
                                )

                                sliderValueHeld = value

                                if (mouseButton == 0) return true
                            }

                            fontSemibold35.drawString(text, minX + 2, yPos + 4, textColor.rgb)
                            
                            val displayValue = value.get().coerceIn(value.range)
                            val progress = (displayValue - value.minimum).toFloat() / (value.maximum - value.minimum)
                            drawMinimalSlider(minX.toFloat() + 4, yPos + 18F, maxX.toFloat() - minX - 8F, 6F, progress, accentColor)

                            yPos += 28
                        }

                        is IntRangeValue -> {
                            val slider1 = value.get().first
                            val slider2 = value.get().last

                            val text = "${value.name}: $slider1 - $slider2 ${suffix}"
                            moduleElement.settingsWidth = fontSemibold35.getStringWidth(text) + 8

                            val startX = minX + 4
                            val startY = yPos + 14
                            val width = moduleElement.settingsWidth - 12
                            val endX = startX + width

                            val currSlider = value.lastChosenSlider

                            if (mouseButton == 0 && mouseX in startX..endX && mouseY in startY - 2..startY + 9 || sliderValueHeld == value) {
                                val leftSliderPos =
                                    startX + (slider1 - value.minimum).toFloat() / (value.maximum - value.minimum) * (endX - startX)
                                val rightSliderPos =
                                    startX + (slider2 - value.minimum).toFloat() / (value.maximum - value.minimum) * (endX - startX)

                                val distToSlider1 = mouseX - leftSliderPos
                                val distToSlider2 = mouseX - rightSliderPos

                                val closerToLeft = abs(distToSlider1) < abs(distToSlider2)

                                val isOnLeftSlider =
                                    (mouseX.toFloat() in startX.toFloat()..leftSliderPos || closerToLeft) && rightSliderPos > startX
                                val isOnRightSlider =
                                    (mouseX.toFloat() in rightSliderPos..endX.toFloat() || !closerToLeft) && leftSliderPos < endX

                                val percentage = (mouseX.toFloat() - startX) / (endX - startX)

                                if (isOnLeftSlider && currSlider == null || currSlider == RangeSlider.LEFT) {
                                    withDelayedSave {
                                        value.setFirst(
                                            value.lerpWith(percentage).coerceIn(value.minimum, slider2), false
                                        )
                                    }
                                }

                                if (isOnRightSlider && currSlider == null || currSlider == RangeSlider.RIGHT) {
                                    withDelayedSave {
                                        value.setLast(
                                            value.lerpWith(percentage).coerceIn(slider1, value.maximum), false
                                        )
                                    }
                                }

                                sliderValueHeld = value

                                if (mouseButton == 0) {
                                    value.lastChosenSlider = when {
                                        isOnLeftSlider -> RangeSlider.LEFT
                                        isOnRightSlider -> RangeSlider.RIGHT
                                        else -> null
                                    }
                                    return true
                                }
                            }

                            fontSemibold35.drawString(text, minX + 2, yPos + 4, textColor.rgb)
                            
                            drawRect(minX + 4, yPos + 18, maxX - 4, yPos + 24, sliderTrackColor.rgb)

                            val displayValue1 = value.get().first
                            val displayValue2 = value.get().last

                            val progress1 = (displayValue1 - value.minimum).toFloat() / (value.maximum - value.minimum)
                            val progress2 = (displayValue2 - value.minimum).toFloat() / (value.maximum - value.minimum)
                            
                            val sliderWidth = maxX - minX - 8
                            val sliderX1 = minX + 4 + sliderWidth * progress1
                            val sliderX2 = minX + 4 + sliderWidth * progress2
                            
                            drawRect(sliderX1 - 3, yPos + 16F, sliderX1 + 3, yPos + 26F, Color.WHITE.rgb)
                            drawBorderedRect(sliderX1 - 3, yPos + 16F, sliderX1 + 3, yPos + 26F, 1F, accentColor.rgb, Color.WHITE.rgb)
                            drawRect(sliderX2 - 3, yPos + 16F, sliderX2 + 3, yPos + 26F, Color.WHITE.rgb)
                            drawBorderedRect(sliderX2 - 3, yPos + 16F, sliderX2 + 3, yPos + 26F, 1F, accentColor.rgb, Color.WHITE.rgb)

                            yPos += 30
                        }

                        is FloatRangeValue -> {
                            val slider1 = value.get().start
                            val slider2 = value.get().endInclusive

                            val text = "${value.name}: ${round(slider1)} - ${round(slider2)} ${suffix}"
                            moduleElement.settingsWidth = fontSemibold35.getStringWidth(text) + 8

                            val startX = minX + 4
                            val startY = yPos + 14
                            val width = moduleElement.settingsWidth - 12
                            val endX = startX + width

                            val currSlider = value.lastChosenSlider

                            if (mouseButton == 0 && mouseX in startX..endX && mouseY in startY - 2..startY + 9 || sliderValueHeld == value) {
                                val leftSliderPos =
                                    startX + (slider1 - value.minimum) / (value.maximum - value.minimum) * (endX - startX)
                                val rightSliderPos =
                                    startX + (slider2 - value.minimum) / (value.maximum - value.minimum) * (endX - startX)

                                val distToSlider1 = mouseX - leftSliderPos
                                val distToSlider2 = mouseX - rightSliderPos

                                val closerToLeft = abs(distToSlider1) < abs(distToSlider2)

                                val isOnLeftSlider =
                                    (mouseX.toFloat() in startX.toFloat()..leftSliderPos || closerToLeft) && rightSliderPos > startX
                                val isOnRightSlider =
                                    (mouseX.toFloat() in rightSliderPos..endX.toFloat() || !closerToLeft) && leftSliderPos < endX

                                val percentage = (mouseX.toFloat() - startX) / (endX - startX)

                                if (isOnLeftSlider && currSlider == null || currSlider == RangeSlider.LEFT) {
                                    withDelayedSave {
                                        value.setFirst(
                                            value.lerpWith(percentage).coerceIn(value.minimum, slider2), false
                                        )
                                    }
                                }

                                if (isOnRightSlider && currSlider == null || currSlider == RangeSlider.RIGHT) {
                                    withDelayedSave {
                                        value.setLast(
                                            value.lerpWith(percentage).coerceIn(slider1, value.maximum), false
                                        )
                                    }
                                }

                                sliderValueHeld = value

                                if (mouseButton == 0) {
                                    value.lastChosenSlider = when {
                                        isOnLeftSlider -> RangeSlider.LEFT
                                        isOnRightSlider -> RangeSlider.RIGHT
                                        else -> null
                                    }
                                    return true
                                }
                            }

                            fontSemibold35.drawString(text, minX + 2, yPos + 4, textColor.rgb)
                            
                            drawRect(minX + 4, yPos + 18, maxX - 4, yPos + 24, sliderTrackColor.rgb)

                            val displayValue1 = value.get().start
                            val displayValue2 = value.get().endInclusive

                            val progress1 = (displayValue1 - value.minimum) / (value.maximum - value.minimum)
                            val progress2 = (displayValue2 - value.minimum) / (value.maximum - value.minimum)
                            
                            val sliderWidth = maxX - minX - 8
                            val sliderX1 = minX + 4 + sliderWidth * progress1
                            val sliderX2 = minX + 4 + sliderWidth * progress2
                            
                            drawRect(sliderX1 - 3, yPos + 16F, sliderX1 + 3, yPos + 26F, Color.WHITE.rgb)
                            drawBorderedRect(sliderX1 - 3, yPos + 16F, sliderX1 + 3, yPos + 26F, 1F, accentColor.rgb, Color.WHITE.rgb)
                            drawRect(sliderX2 - 3, yPos + 16F, sliderX2 + 3, yPos + 26F, Color.WHITE.rgb)
                            drawBorderedRect(sliderX2 - 3, yPos + 16F, sliderX2 + 3, yPos + 26F, 1F, accentColor.rgb, Color.WHITE.rgb)

                            yPos += 30
                        }

                        is FontValue -> {
                            val displayString = value.displayName
                            moduleElement.settingsWidth = fontSemibold35.getStringWidth(displayString) + 8

                            if (mouseButton != null && mouseX in minX..maxX && mouseY in yPos + 4..yPos + 14) {
                                if (mouseButton == 0) value.next()
                                else value.previous()
                                clickSound()
                                return true
                            }

                            fontSemibold35.drawString(displayString, minX + 2, yPos + 5, textColor.rgb)

                            yPos += 16
                        }

                        is ColorValue -> {
                            val currentColor = value.selectedColor()
                            val spacing = 12
                            val startX = moduleElement.x + moduleElement.width + 4
                            val startY = yPos - 1

                            val colorPreviewSize = 10
                            val colorPreviewX2 = maxX - colorPreviewSize
                            val colorPreviewX1 = colorPreviewX2 - colorPreviewSize
                            val colorPreviewY1 = startY + 2
                            val colorPreviewY2 = colorPreviewY1 + colorPreviewSize

                            val rainbowPreviewX2 = colorPreviewX1 - colorPreviewSize
                            val rainbowPreviewX1 = rainbowPreviewX2 - colorPreviewSize

                            val textX = startX + 2F
                            val textY = startY + 4F

                            val hueSliderWidth = 7
                            val hueSliderHeight = 50
                            val colorPickerWidth = 75
                            val colorPickerHeight = 50
                            val spacingBetweenSliders = 5
                            val rgbaOptionHeight = if (value.showOptions) fontSemibold35.height * 4 else 0

                            val colorPickerStartX = textX.toInt()
                            val colorPickerEndX = colorPickerStartX + colorPickerWidth
                            val colorPickerStartY = rgbaOptionHeight + colorPreviewY2 + spacing / 3
                            val colorPickerEndY = colorPickerStartY + colorPickerHeight

                            val hueSliderStartY = colorPickerStartY
                            val hueSliderEndY = colorPickerStartY + hueSliderHeight
                            val hueSliderX = colorPickerEndX + spacingBetweenSliders

                            val opacityStartX = hueSliderX + hueSliderWidth + spacingBetweenSliders
                            val opacityEndX = opacityStartX + hueSliderWidth

                            val rainbow = value.rainbow

                            if (mouseButton in arrayOf(0, 1)) {
                                val isColorPreview =
                                    mouseX in colorPreviewX1..colorPreviewX2 && mouseY in colorPreviewY1..colorPreviewY2
                                val isRainbowPreview =
                                    mouseX in rainbowPreviewX1..rainbowPreviewX2 && mouseY in colorPreviewY1..colorPreviewY2

                                when {
                                    isColorPreview -> {
                                        if (mouseButton == 0 && rainbow) value.rainbow = false
                                        if (mouseButton == 1) value.showPicker = !value.showPicker
                                        clickSound()
                                        return true
                                    }

                                    isRainbowPreview -> {
                                        if (mouseButton == 0) value.rainbow = true
                                        if (mouseButton == 1) value.showPicker = !value.showPicker
                                        clickSound()
                                        return true
                                    }
                                }
                            }

                            val startText = "${value.name}: "
                            val valueText = "#%08X".format(currentColor.rgb)
                            val combinedText = startText + valueText

                            val combinedWidth = opacityEndX - colorPickerStartX
                            val optimalWidth = maxOf(fontSemibold35.getStringWidth(combinedText), combinedWidth)
                            moduleElement.settingsWidth = optimalWidth + spacing * 4

                            val valueX = startX + fontSemibold35.getStringWidth(startText)
                            val valueWidth = fontSemibold35.getStringWidth(valueText)

                            if (mouseButton == 1 && mouseX in valueX..valueX + valueWidth && mouseY.toFloat() in textY - 2..textY + fontSemibold35.height - 3F) {
                                value.showOptions = !value.showOptions
                                if (!value.showOptions) {
                                    resetChosenText(value)
                                }
                            }

                            val widestLabel = rgbaLabels.maxOf { fontSemibold35.getStringWidth(it) }
                            var highlightCursor = {}

                            chosenText?.let {
                                if (it.value != value) return@let

                                val startValueX = textX + widestLabel + 3
                                val cursorY = textY + value.rgbaIndex * fontSemibold35.height + 10

                                if (it.selectionActive()) {
                                    val start = startValueX + fontSemibold35.getStringWidth(it.string.take(it.selectionStart!!))
                                    val end = startValueX + fontSemibold35.getStringWidth(it.string.take(it.selectionEnd!!))
                                    drawRect(start, cursorY - 3f, end, cursorY + fontSemibold35.fontHeight - 2, accentColor.rgb)
                                }

                                highlightCursor = {
                                    val cursorX = startValueX + fontSemibold35.getStringWidth(it.cursorString)
                                    drawRect(cursorX, cursorY - 3F, cursorX + 1F, cursorY + fontSemibold35.fontHeight - 2, textColor.rgb)
                                }
                            }

                            if (value.showOptions) {
                                val mainColor = value.get()
                                val rgbaValues = listOf(mainColor.red, mainColor.green, mainColor.blue, mainColor.alpha)
                                val rgbaYStart = textY + 10
                                var noClickAmount = 0
                                val maxWidth = fontSemibold35.getStringWidth("255")

                                rgbaLabels.forEachIndexed { index, label ->
                                    val rgbaValueText = "${rgbaValues[index]}"
                                    val colorX = textX + widestLabel + 4
                                    val yPosition = rgbaYStart + index * fontSemibold35.height
                                    val isEmpty = chosenText?.value == value && value.rgbaIndex == index && chosenText?.string.isNullOrEmpty()
                                    val extraSpacing = if (isEmpty) maxWidth + 4 else 0
                                    val finalX = colorX + extraSpacing
                                    val defaultColor = if (isEmpty) secondaryTextColor else accentColor
                                    val defaultText = if (isEmpty) "($rgbaValueText)" else rgbaValueText

                                    fontSemibold35.drawString(label, textX, yPosition, textColor.rgb)
                                    fontSemibold35.drawString(defaultText, finalX, yPosition, defaultColor.rgb)

                                    if (mouseButton == 0) {
                                        if (mouseX.toFloat() in finalX..finalX + maxWidth && mouseY.toFloat() in yPosition - 2..yPosition + 6) {
                                            chosenText = EditableText.forRGBA(value, index)
                                        } else {
                                            noClickAmount++
                                        }
                                    }
                                }

                                if (noClickAmount == rgbaLabels.size) {
                                    resetChosenText(value)
                                }
                            }

                            fontSemibold35.drawString(combinedText, textX, textY, textColor.rgb)
                            highlightCursor()

                            val normalBorderColor = if (rainbow) borderColor else accentColor
                            val rainbowBorderColor = if (rainbow) accentColor else borderColor

                            val hue = if (rainbow) {
                                Color.RGBtoHSB(currentColor.red, currentColor.green, currentColor.blue, null)[0]
                            } else {
                                value.hueSliderY
                            }

                            if (value.showPicker) {
                                value.updateTextureCache(
                                    id = 0,
                                    hue = hue,
                                    width = colorPickerWidth,
                                    height = colorPickerHeight,
                                    generateImage = { image, _ ->
                                        for (px in 0 until colorPickerWidth) {
                                            for (py in 0 until colorPickerHeight) {
                                                val localS = px / colorPickerWidth.toFloat()
                                                val localB = 1.0f - (py / colorPickerHeight.toFloat())
                                                val rgb = Color.HSBtoRGB(hue, localS, localB)
                                                image.setRGB(px, py, rgb)
                                            }
                                        }
                                    },
                                    drawAt = { id ->
                                        drawTexture(id, colorPickerStartX, colorPickerStartY, colorPickerWidth, colorPickerHeight)
                                    })

                                val markerX = (colorPickerStartX..colorPickerEndX).lerpWith(value.colorPickerPos.x)
                                val markerY = (colorPickerStartY..colorPickerEndY).lerpWith(value.colorPickerPos.y)

                                if (!rainbow) {
                                    RenderUtils.drawBorder(markerX - 2f, markerY - 2f, markerX + 3f, markerY + 3f, 1.5f, Color.WHITE.rgb)
                                }

                                value.updateTextureCache(
                                    id = 1,
                                    hue = hue,
                                    width = hueSliderWidth,
                                    height = hueSliderHeight,
                                    generateImage = { image, _ ->
                                        for (y in 0 until hueSliderHeight) {
                                            for (x in 0 until hueSliderWidth) {
                                                val localHue = y / hueSliderHeight.toFloat()
                                                val rgb = Color.HSBtoRGB(localHue, 1.0f, 1.0f)
                                                image.setRGB(x, y, rgb)
                                            }
                                        }
                                    },
                                    drawAt = { id ->
                                        drawTexture(id, hueSliderX, colorPickerStartY, hueSliderWidth, hueSliderHeight)
                                    })

                                value.updateTextureCache(
                                    id = 2,
                                    hue = currentColor.rgb.toFloat(),
                                    width = hueSliderWidth,
                                    height = hueSliderHeight,
                                    generateImage = { image, _ ->
                                        val gridSize = 1
                                        for (y in 0 until hueSliderHeight) {
                                            for (x in 0 until hueSliderWidth) {
                                                val gridX = x / gridSize
                                                val gridY = y / gridSize
                                                val checkerboardColor = if ((gridY + gridX) % 2 == 0) Color.WHITE.rgb else Color(220, 220, 220).rgb
                                                val alpha = ((1 - y.toFloat() / hueSliderHeight.toFloat()) * 255).roundToInt()
                                                val finalColor = blendColors(Color(checkerboardColor), currentColor.withAlpha(alpha))
                                                image.setRGB(x, y, finalColor.rgb)
                                            }
                                        }
                                    },
                                    drawAt = { id ->
                                        drawTexture(id, opacityStartX, colorPickerStartY, hueSliderWidth, hueSliderHeight)
                                    })

                                val opacityMarkerY = (hueSliderStartY..hueSliderEndY).lerpWith(1 - value.opacitySliderY)
                                val hueMarkerY = (hueSliderStartY..hueSliderEndY).lerpWith(hue)

                                RenderUtils.drawBorder(hueSliderX.toFloat() - 1, hueMarkerY - 1f, hueSliderX + hueSliderWidth + 1f, hueMarkerY + 1f, 1.5f, Color.WHITE.rgb)
                                RenderUtils.drawBorder(opacityStartX.toFloat() - 1, opacityMarkerY - 1f, opacityEndX + 1f, opacityMarkerY + 1f, 1.5f, Color.WHITE.rgb)

                                val inColorPicker = mouseX in colorPickerStartX until colorPickerEndX && mouseY in colorPickerStartY until colorPickerEndY && !rainbow
                                val inHueSlider = mouseX in hueSliderX - 1..hueSliderX + hueSliderWidth + 1 && mouseY in hueSliderStartY until hueSliderEndY && !rainbow
                                val inOpacitySlider = mouseX in opacityStartX - 1..opacityEndX + 1 && mouseY in hueSliderStartY until hueSliderEndY
                                val sliderType = value.lastChosenSlider

                                if (mouseButton == 0 && (inColorPicker || inHueSlider || inOpacitySlider) || sliderValueHeld == value && value.lastChosenSlider != null) {
                                    if (inColorPicker && sliderType == null || sliderType == ColorValue.SliderType.COLOR) {
                                        val newS = ((mouseX - colorPickerStartX) / colorPickerWidth.toFloat()).coerceIn(0f, 1f)
                                        val newB = (1.0f - (mouseY - colorPickerStartY) / colorPickerHeight.toFloat()).coerceIn(0f, 1f)
                                        value.colorPickerPos.x = newS
                                        value.colorPickerPos.y = 1 - newB
                                    }

                                    var finalColor = Color(Color.HSBtoRGB(value.hueSliderY, value.colorPickerPos.x, 1 - value.colorPickerPos.y))

                                    if (inHueSlider && sliderType == null || sliderType == ColorValue.SliderType.HUE) {
                                        value.hueSliderY = ((mouseY - hueSliderStartY) / hueSliderHeight.toFloat()).coerceIn(0f, 1f)
                                        finalColor = Color(Color.HSBtoRGB(value.hueSliderY, value.colorPickerPos.x, 1 - value.colorPickerPos.y))
                                    }

                                    if (inOpacitySlider && sliderType == null || sliderType == ColorValue.SliderType.OPACITY) {
                                        value.opacitySliderY = 1 - ((mouseY - hueSliderStartY) / hueSliderHeight.toFloat()).coerceIn(0f, 1f)
                                    }

                                    finalColor = finalColor.withAlpha((value.opacitySliderY * 255).roundToInt())
                                    sliderValueHeld = value
                                    value.setAndSaveValueOnButtonRelease(finalColor)

                                    if (mouseButton == 0) {
                                        value.lastChosenSlider = when {
                                            inColorPicker && !rainbow -> ColorValue.SliderType.COLOR
                                            inHueSlider && !rainbow -> ColorValue.SliderType.HUE
                                            inOpacitySlider -> ColorValue.SliderType.OPACITY
                                            else -> null
                                        }
                                        return true
                                    }
                                }
                                yPos += colorPickerHeight + colorPreviewSize - 6
                            }

                            drawBorderedRect(colorPreviewX1, colorPreviewY1, colorPreviewX2, colorPreviewY2, 1.5f, normalBorderColor.rgb, value.get().rgb)
                            drawBorderedRect(rainbowPreviewX1, colorPreviewY1, rainbowPreviewX2, colorPreviewY2, 1.5f, rainbowBorderColor.rgb, ColorUtils.rainbow(alpha = value.opacitySliderY).rgb)

                            yPos += spacing + rgbaOptionHeight
                        }

                        else -> {
                            val startText = value.name + ": "
                            var valueText = "${value.get()}"
                            val combinedWidth = fontSemibold35.getStringWidth(startText + valueText)
                            moduleElement.settingsWidth = combinedWidth + 8

                            val textY = yPos + 4
                            val startX = minX + 2
                            var textX = startX + fontSemibold35.getStringWidth(startText)

                            if (mouseButton == 0) {
                                chosenText = if (mouseX in textX..maxX && mouseY in textY - 2..textY + 6 && value is TextValue) {
                                    EditableText.forTextValue(value)
                                } else {
                                    null
                                }
                            }

                            val shouldPushToRight = value is TextValue && chosenText?.value == value && chosenText?.string != value.get()
                            var highlightCursor: (Int) -> Unit = {}

                            chosenText?.let {
                                if (it.value != value) return@let

                                val input = it.string

                                if (it.selectionActive()) {
                                    val start = textX - 1 + fontSemibold35.getStringWidth(input.take(it.selectionStart!!))
                                    val end = textX - 1 + fontSemibold35.getStringWidth(input.take(it.selectionEnd!!))
                                    drawRect(start, textY - 3, end, textY + fontSemibold35.fontHeight - 2, accentColor.rgb)
                                }

                                highlightCursor = { textX ->
                                    val cursorX = textX + fontSemibold35.getStringWidth(input.take(it.cursorIndex))
                                    drawRect(cursorX, textY - 3, cursorX + 1, textY + fontSemibold35.fontHeight - 2, textColor.rgb)
                                }
                            }

                            fontSemibold35.drawString(startText, startX, textY, textColor.rgb)

                            val defaultColor = if (shouldPushToRight) secondaryTextColor else accentColor
                            val originalX = textX - 1

                            if (shouldPushToRight) {
                                valueText = "($valueText)"
                                val valueWidth = fontSemibold35.getStringWidth(valueText)
                                moduleElement.settingsWidth = combinedWidth + valueWidth + 12
                                fontSemibold35.drawString(chosenText!!.string, textX, textY, accentColor.rgb)
                                textX += valueWidth + 4
                            }

                            fontSemibold35.drawString(valueText, textX, textY, defaultColor.rgb)
                            highlightCursor(originalX)

                            yPos += 16
                        }
                    }
                }

                moduleElement.adjustWidth()
                moduleElement.settingsHeight = yPos - moduleElement.y - 4

                if (moduleElement.settingsWidth > 0 && yPos > moduleElement.y + 4) {
                    if (mouseButton != null && mouseX in minX..maxX && mouseY in moduleElement.y + 6..yPos + 2) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
