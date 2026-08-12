package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.IntValue
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import java.awt.Color

object ColorMixer : Module("ColorMixer", Category.RENDER, canBeEnabled = false) {
    var lastFraction = floatArrayOf()
    var lastColors = arrayOf<Color>()

    val blendAmount = IntValue("Mixer-Amount", 2, 2..10).onChanged { 
        regenerateColors(true)
    }

    private val col1RedValue = colorElement(1, ColorElementMaterial.RED)
    private val col1GreenValue = colorElement(1, ColorElementMaterial.GREEN)
    private val col1BlueValue = colorElement(1, ColorElementMaterial.BLUE)

    private val col2RedValue = colorElement(2, ColorElementMaterial.RED)
    private val col2GreenValue = colorElement(2, ColorElementMaterial.GREEN)
    private val col2BlueValue = colorElement(2, ColorElementMaterial.BLUE)

    private val col3RedValue = colorElement(3, ColorElementMaterial.RED)
    private val col3GreenValue = colorElement(3, ColorElementMaterial.GREEN)
    private val col3BlueValue = colorElement(3, ColorElementMaterial.BLUE)

    private val col4RedValue = colorElement(4, ColorElementMaterial.RED)
    private val col4GreenValue = colorElement(4, ColorElementMaterial.GREEN)
    private val col4BlueValue = colorElement(4, ColorElementMaterial.BLUE)

    private val col5RedValue = colorElement(5, ColorElementMaterial.RED)
    private val col5GreenValue = colorElement(5, ColorElementMaterial.GREEN)
    private val col5BlueValue = colorElement(5, ColorElementMaterial.BLUE)

    private val col6RedValue = colorElement(6, ColorElementMaterial.RED)
    private val col6GreenValue = colorElement(6, ColorElementMaterial.GREEN)
    private val col6BlueValue = colorElement(6, ColorElementMaterial.BLUE)

    private val col7RedValue = colorElement(7, ColorElementMaterial.RED)
    private val col7GreenValue = colorElement(7, ColorElementMaterial.GREEN)
    private val col7BlueValue = colorElement(7, ColorElementMaterial.BLUE)

    private val col8RedValue = colorElement(8, ColorElementMaterial.RED)
    private val col8GreenValue = colorElement(8, ColorElementMaterial.GREEN)
    private val col8BlueValue = colorElement(8, ColorElementMaterial.BLUE)

    private val col9RedValue = colorElement(9, ColorElementMaterial.RED)
    private val col9GreenValue = colorElement(9, ColorElementMaterial.GREEN)
    private val col9BlueValue = colorElement(9, ColorElementMaterial.BLUE)

    private val col10RedValue = colorElement(10, ColorElementMaterial.RED)
    private val col10GreenValue = colorElement(10, ColorElementMaterial.GREEN)
    private val col10BlueValue = colorElement(10, ColorElementMaterial.BLUE)

    private fun colorElement(index: Int, material: ColorElementMaterial): Value<Int> {
        val default = when (index) {
            1 -> if (material == ColorElementMaterial.RED) 255 else if (material == ColorElementMaterial.GREEN) 0 else 0
            2 -> if (material == ColorElementMaterial.RED) 0 else if (material == ColorElementMaterial.GREEN) 255 else 0
            else -> 255
        }
        return IntValue("Col$index-${material.name}", default, 0..255).onChanged { 
            regenerateColors(true)
        }
    }

    fun regenerateColors(forceValue: Boolean) {
        if (forceValue || lastColors.isEmpty() || lastColors.size != (blendAmount.get() * 2) - 1) {
            val generator = arrayOfNulls<Color>((blendAmount.get() * 2) - 1)

            for (i in 1..blendAmount.get()) {
                var result = Color.WHITE
                try {
                    val red = ColorMixer::class.java.getDeclaredField("col${i}RedValue")
                    val green = ColorMixer::class.java.getDeclaredField("col${i}GreenValue")
                    val blue = ColorMixer::class.java.getDeclaredField("col${i}BlueValue")

                    red.isAccessible = true
                    green.isAccessible = true
                    blue.isAccessible = true

                    @Suppress("UNCHECKED_CAST")
                    val r = (red.get(this) as Value<Int>).get()
                    @Suppress("UNCHECKED_CAST")
                    val g = (green.get(this) as Value<Int>).get()
                    @Suppress("UNCHECKED_CAST")
                    val b = (blue.get(this) as Value<Int>).get()

                    result = Color(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                generator[i - 1] = result
            }

            var h = blendAmount.get()
            for (z in blendAmount.get() - 2 downTo 0) {
                generator[h] = generator[z]
                h++
            }

            lastColors = generator.filterNotNull().toTypedArray()
        }

        if (forceValue || lastFraction.isEmpty() || lastFraction.size != (blendAmount.get() * 2) - 1) {
            val colorFraction = FloatArray((blendAmount.get() * 2) - 1)

            for (i in 0..(blendAmount.get() * 2) - 2) {
                colorFraction[i] = i.toFloat() / ((blendAmount.get() * 2) - 2).toFloat()
            }

            lastFraction = colorFraction
        }
    }
}

fun getMixedColor(index: Int, seconds: Int): Color {
    val colMixer = LiquidBounce.moduleManager[ColorMixer::class.java] as? ColorMixer ?: return Color.WHITE

    if (colMixer.lastColors.isEmpty() || colMixer.lastFraction.isEmpty()) {
        colMixer.regenerateColors(true)
    }

    return ColorUtils.blendColors(
        colMixer.lastFraction,
        colMixer.lastColors,
        ((System.currentTimeMillis() + index) % (seconds * 1000)) / (seconds * 1000).toFloat()
    )
}

enum class ColorElementMaterial {
    RED, GREEN, BLUE
}
