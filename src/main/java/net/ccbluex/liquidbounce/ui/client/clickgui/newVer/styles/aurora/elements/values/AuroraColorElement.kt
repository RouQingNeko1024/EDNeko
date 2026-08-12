/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values

import net.ccbluex.liquidbounce.config.ColorValue
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.AuroraGUI
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexture
import net.ccbluex.liquidbounce.utils.render.RenderUtils.updateTextureCache
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

class AuroraColorElement(value: ColorValue) : AuroraValueElement<Color>(value) {
    private var expanded = false
    private var animExpanded = 0F
    private var draggingColor = false
    private var draggingHue = false
    private var draggingOpacity = false

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: Color): Float {
        val colorValue = value as ColorValue
        val currentColor = colorValue.selectedColor()

        Fonts.font35.drawString(colorValue.name, x, y + 5F, Color.WHITE.rgb)

        val previewX = x + width - 80F
        val previewY = y + 2F
        val previewSize = 20F

        RenderUtils.originalRoundedRect(previewX, previewY, previewX + previewSize, previewY + previewSize, 4F, currentColor.rgb)

        val rainbowX = previewX + previewSize + 5F
        RenderUtils.originalRoundedRect(rainbowX, previewY, rainbowX + previewSize, previewY + previewSize, 4F,
            ColorUtils.rainbow().rgb)

        if (colorValue.rainbow) {
            RenderUtils.originalRoundedRect(rainbowX - 2F, previewY - 2F, rainbowX + previewSize + 2F, previewY + previewSize + 2F, 4F, Color.WHITE.rgb)
        } else {
            RenderUtils.originalRoundedRect(previewX - 2F, previewY - 2F, previewX + previewSize + 2F, previewY + previewSize + 2F, 4F, currentColor.rgb)
        }

        if (expanded) {
            val pickerWidth = 100
            val pickerHeight = 60
            val sliderWidth = 10
            val spacing = 5F
            val totalExpandedHeight = pickerHeight + 25F

            animExpanded += (totalExpandedHeight - animExpanded) * 0.15F
            valueHeight = 30F + animExpanded

            val pickerX = x + 10F
            val pickerY = y + 30F

            RenderUtils.originalRoundedRect(x, pickerY - 5F, x + width, pickerY + animExpanded, 4F, Color(45, 45, 50).rgb)

            if (animExpanded > 30F) {
                val hue = if (colorValue.rainbow) {
                    val hsb = FloatArray(3)
                    Color.RGBtoHSB(currentColor.red, currentColor.green, currentColor.blue, hsb)
                    hsb[0]
                } else {
                    colorValue.hueSliderY
                }

                colorValue.updateTextureCache(
                    id = 0,
                    hue = hue,
                    width = pickerWidth,
                    height = pickerHeight,
                    generateImage = { image: BufferedImage, _: Graphics2D ->
                        for (px in 0 until pickerWidth) {
                            for (py in 0 until pickerHeight) {
                                val s = px.toFloat() / pickerWidth
                                val b = 1f - py.toFloat() / pickerHeight
                                image.setRGB(px, py, Color.HSBtoRGB(hue, s, b))
                            }
                        }
                    },
                    drawAt = { textureID: Int ->
                        drawTexture(textureID, pickerX.toInt(), pickerY.toInt(), pickerWidth, pickerHeight)
                    }
                )

                if (!colorValue.rainbow) {
                    val markerX = pickerX + colorValue.colorPickerPos.x * pickerWidth
                    val markerY = pickerY + colorValue.colorPickerPos.y * pickerHeight
                    RenderUtils.originalRoundedRect(markerX - 2F, markerY - 2F, markerX + 2F, markerY + 2F, 1F, Color.WHITE.rgb)
                }

                val hueSliderX = pickerX + pickerWidth + spacing
                colorValue.updateTextureCache(
                    id = 1,
                    hue = 0F,
                    width = sliderWidth,
                    height = pickerHeight,
                    generateImage = { image: BufferedImage, _: Graphics2D ->
                        for (py in 0 until pickerHeight) {
                            val h = py.toFloat() / pickerHeight
                            val rgb = Color.HSBtoRGB(h, 1f, 1f)
                            for (px in 0 until sliderWidth) {
                                image.setRGB(px, py, rgb)
                            }
                        }
                    },
                    drawAt = { textureID: Int ->
                        drawTexture(textureID, hueSliderX.toInt(), pickerY.toInt(), sliderWidth, pickerHeight)
                    }
                )

                val hueMarkerY = pickerY + hue * pickerHeight
                RenderUtils.originalRoundedRect(hueSliderX - 1F, hueMarkerY - 1F, hueSliderX + sliderWidth + 1F, hueMarkerY + 1F, 1F, Color.WHITE.rgb)

                val opacitySliderX = hueSliderX + sliderWidth + spacing
                colorValue.updateTextureCache(
                    id = 2,
                    hue = 0F,
                    width = sliderWidth,
                    height = pickerHeight,
                    generateImage = { image: BufferedImage, _: Graphics2D ->
                        for (py in 0 until pickerHeight) {
                            val alpha = (1f - py.toFloat() / pickerHeight) * 255
                            val checkerboard = if ((py / 4) % 2 == 0) Color.WHITE else Color.GRAY
                            val blended = blendColors(checkerboard, currentColor, alpha / 255f)
                            for (px in 0 until sliderWidth) {
                                image.setRGB(px, py, blended.rgb)
                            }
                        }
                    },
                    drawAt = { textureID: Int ->
                        drawTexture(textureID, opacitySliderX.toInt(), pickerY.toInt(), sliderWidth, pickerHeight)
                    }
                )

                val opacityMarkerY = pickerY + (1F - colorValue.opacitySliderY) * pickerHeight
                RenderUtils.originalRoundedRect(opacitySliderX - 1F, opacityMarkerY - 1F, opacitySliderX + sliderWidth + 1F, opacityMarkerY + 1F, 1F, Color.WHITE.rgb)

                val rainbowY = pickerY + pickerHeight + 5F
                val isHoveringRainbow = mouseX >= pickerX && mouseX <= pickerX + 80F &&
                        mouseY >= rainbowY && mouseY <= rainbowY + 14F

                RenderUtils.originalRoundedRect(pickerX, rainbowY, pickerX + 80F, rainbowY + 14F, 3F,
                    if (colorValue.rainbow) accentColor.rgb else if (isHoveringRainbow) Color(60, 60, 65).rgb else Color(45, 45, 50).rgb)
                Fonts.font35.drawString("Rainbow", pickerX + 8F, rainbowY + 4F, Color.WHITE.rgb)
            }
        } else {
            animExpanded *= 0.8F
            valueHeight = 30F
        }

        return valueHeight
    }

    private fun blendColors(c1: Color, c2: Color, factor: Float): Color {
        val r = (c1.red * (1 - factor) + c2.red * factor).toInt()
        val g = (c1.green * (1 - factor) + c2.green * factor).toInt()
        val b = (c1.blue * (1 - factor) + c2.blue * factor).toInt()
        return Color(r, g, b)
    }

    private fun updateColor(colorValue: ColorValue) {
        if (!colorValue.rainbow) {
            val rgb = Color.HSBtoRGB(colorValue.hueSliderY, colorValue.colorPickerPos.x, 1 - colorValue.colorPickerPos.y)
            val baseColor = Color(rgb)
            val color = Color(baseColor.red, baseColor.green, baseColor.blue, (colorValue.opacitySliderY * 255).roundToInt())
            colorValue.set(color)
        }
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean {
        val colorValue = value as ColorValue

        val previewX = x + width - 80F
        val previewY = y + 2F
        val previewSize = 20F

        if (mouseX >= previewX && mouseX <= previewX + previewSize && mouseY >= previewY && mouseY <= previewY + previewSize) {
            colorValue.rainbow = false
            expanded = !expanded
            return true
        }

        val rainbowX = previewX + previewSize + 5F
        if (mouseX >= rainbowX && mouseX <= rainbowX + previewSize && mouseY >= previewY && mouseY <= previewY + previewSize) {
            colorValue.rainbow = !colorValue.rainbow
            return true
        }

        if (expanded) {
            val pickerWidth = 100F
            val pickerHeight = 60F
            val sliderWidth = 10F
            val spacing = 5F

            val pickerX = x + 10F
            val pickerY = y + 30F
            val hueSliderX = pickerX + pickerWidth + spacing
            val opacitySliderX = hueSliderX + sliderWidth + spacing

            if (mouseX >= pickerX && mouseX <= pickerX + pickerWidth &&
                mouseY >= pickerY && mouseY <= pickerY + pickerHeight && !colorValue.rainbow) {
                draggingColor = true
                handleColorDrag(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight, colorValue)
                return true
            }

            if (mouseX >= hueSliderX && mouseX <= hueSliderX + sliderWidth &&
                mouseY >= pickerY && mouseY <= pickerY + pickerHeight) {
                draggingHue = true
                colorValue.hueSliderY = ((mouseY - pickerY) / pickerHeight).coerceIn(0F, 1F)
                updateColor(colorValue)
                return true
            }

            if (mouseX >= opacitySliderX && mouseX <= opacitySliderX + sliderWidth &&
                mouseY >= pickerY && mouseY <= pickerY + pickerHeight) {
                draggingOpacity = true
                colorValue.opacitySliderY = (1F - (mouseY - pickerY) / pickerHeight).coerceIn(0F, 1F)
                updateColor(colorValue)
                return true
            }

            val rainbowY = pickerY + pickerHeight + 5F
            if (mouseX >= pickerX && mouseX <= pickerX + 80F &&
                mouseY >= rainbowY && mouseY <= rainbowY + 14F) {
                colorValue.rainbow = !colorValue.rainbow
                return true
            }
        }

        if (mouseY >= y && mouseY <= y + 25F) {
            expanded = !expanded
            return true
        }

        return false
    }

    override fun onDrag(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (!expanded) return
        val colorValue = value as ColorValue

        val pickerWidth = 100F
        val pickerHeight = 60F
        val pickerX = x + 10F
        val pickerY = y + 30F

        if (draggingColor && !colorValue.rainbow) {
            handleColorDrag(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight, colorValue)
        }

        if (draggingHue) {
            colorValue.hueSliderY = ((mouseY - pickerY) / pickerHeight).coerceIn(0F, 1F)
            updateColor(colorValue)
        }

        if (draggingOpacity) {
            colorValue.opacitySliderY = (1F - (mouseY - pickerY) / pickerHeight).coerceIn(0F, 1F)
            updateColor(colorValue)
        }
    }

    private fun handleColorDrag(mouseX: Int, mouseY: Int, pickerX: Float, pickerY: Float,
                                 pickerWidth: Float, pickerHeight: Float, colorValue: ColorValue) {
        val newS = ((mouseX - pickerX) / pickerWidth).coerceIn(0F, 1F)
        val newB = 1F - ((mouseY - pickerY) / pickerHeight).coerceIn(0F, 1F)
        colorValue.colorPickerPos.x = newS
        colorValue.colorPickerPos.y = 1F - newB
        updateColor(colorValue)
    }

    override fun onRelease(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        draggingColor = false
        draggingHue = false
        draggingOpacity = false
    }
}
