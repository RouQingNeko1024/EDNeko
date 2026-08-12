package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import net.ccbluex.liquidbounce.features.module.modules.client.ClickGUI
import java.awt.Color
import kotlin.math.abs

object MD3Theme {

    var PRIMARY = Color.WHITE
    var ON_PRIMARY = Color.BLACK
    var PRIMARY_CONTAINER = Color.WHITE
    var ON_PRIMARY_CONTAINER = Color.BLACK
    var SECONDARY = Color.WHITE
    var ON_SECONDARY = Color.BLACK
    var SECONDARY_CONTAINER = Color.WHITE
    var ON_SECONDARY_CONTAINER = Color.BLACK
    var TERTIARY = Color.WHITE
    var ON_TERTIARY = Color.BLACK
    var TERTIARY_CONTAINER = Color.WHITE
    var ON_TERTIARY_CONTAINER = Color.BLACK
    var ERROR = Color.WHITE
    var ON_ERROR = Color.BLACK
    var ERROR_CONTAINER = Color.WHITE
    var ON_ERROR_CONTAINER = Color.BLACK
    var SURFACE = Color.WHITE
    var ON_SURFACE = Color.BLACK
    var SURFACE_VARIANT = Color.WHITE
    var ON_SURFACE_VARIANT = Color.BLACK
    var OUTLINE = Color.WHITE
    var OUTLINE_VARIANT = Color.WHITE
    var INVERSE_SURFACE = Color.WHITE
    var INVERSE_ON_SURFACE = Color.WHITE
    var INVERSE_PRIMARY = Color.WHITE
    var SURFACE_DIM = Color.WHITE
    var SURFACE_BRIGHT = Color.WHITE
    var SURFACE_CONTAINER_LOWEST = Color.WHITE
    var SURFACE_CONTAINER_LOW = Color.WHITE
    var SURFACE_CONTAINER = Color.WHITE
    var SURFACE_CONTAINER_HIGH = Color.WHITE
    var SURFACE_CONTAINER_HIGHEST = Color.WHITE

    var TEXT_PRIMARY = Color.WHITE
    var TEXT_SECONDARY = Color.WHITE
    var TEXT_MUTED = Color.WHITE
    var SHADOW = Color.BLACK

    fun syncFromSettings() {
        val seed = OpaiGUI.primaryColor
        val dark = false

        val primary = buildPalette(seed, 0f, 1f)
        val secondary = buildPalette(seed, 30f / 360f, 0.8f)
        val tertiary = buildPalette(seed, 60f / 360f, 0.6f)
        val neutral = buildPalette(seed, 0f, 0.05f)
        val neutralVariant = buildPalette(seed, 0f, 0.10f)
        val error = buildPalette(Color(180, 40, 40), 0f, 1f)

        if (dark) {
            PRIMARY = primary.get(80)
            ON_PRIMARY = primary.get(20)
            PRIMARY_CONTAINER = primary.get(30)
            ON_PRIMARY_CONTAINER = primary.get(90)
            SECONDARY = secondary.get(80)
            ON_SECONDARY = secondary.get(20)
            SECONDARY_CONTAINER = secondary.get(30)
            ON_SECONDARY_CONTAINER = secondary.get(90)
            TERTIARY = tertiary.get(80)
            ON_TERTIARY = tertiary.get(20)
            TERTIARY_CONTAINER = tertiary.get(30)
            ON_TERTIARY_CONTAINER = tertiary.get(90)
            ERROR = error.get(80)
            ON_ERROR = error.get(20)
            ERROR_CONTAINER = error.get(30)
            ON_ERROR_CONTAINER = error.get(90)
            SURFACE = neutral.get(6)
            ON_SURFACE = neutral.get(90)
            SURFACE_VARIANT = neutralVariant.get(30)
            ON_SURFACE_VARIANT = neutralVariant.get(80)
            OUTLINE = neutralVariant.get(60)
            OUTLINE_VARIANT = neutralVariant.get(30)
            INVERSE_SURFACE = neutral.get(90)
            INVERSE_ON_SURFACE = neutral.get(20)
            INVERSE_PRIMARY = primary.get(40)
            SURFACE_DIM = neutral.get(6)
            SURFACE_BRIGHT = neutral.get(24)
            SURFACE_CONTAINER_LOWEST = neutral.get(4)
            SURFACE_CONTAINER_LOW = neutral.get(10)
            SURFACE_CONTAINER = neutral.get(12)
            SURFACE_CONTAINER_HIGH = neutral.get(17)
            SURFACE_CONTAINER_HIGHEST = neutral.get(22)

            TEXT_PRIMARY = ON_SURFACE
            TEXT_SECONDARY = ON_SURFACE_VARIANT
            TEXT_MUTED = OUTLINE
        } else {
            PRIMARY = primary.get(40)
            ON_PRIMARY = primary.get(100)
            PRIMARY_CONTAINER = primary.get(90)
            ON_PRIMARY_CONTAINER = primary.get(10)
            SECONDARY = secondary.get(40)
            ON_SECONDARY = secondary.get(100)
            SECONDARY_CONTAINER = secondary.get(90)
            ON_SECONDARY_CONTAINER = secondary.get(10)
            TERTIARY = tertiary.get(40)
            ON_TERTIARY = tertiary.get(100)
            TERTIARY_CONTAINER = tertiary.get(90)
            ON_TERTIARY_CONTAINER = tertiary.get(10)
            ERROR = error.get(40)
            ON_ERROR = error.get(100)
            ERROR_CONTAINER = error.get(90)
            ON_ERROR_CONTAINER = error.get(10)
            SURFACE = neutral.get(98)
            ON_SURFACE = neutral.get(10)
            SURFACE_VARIANT = neutralVariant.get(90)
            ON_SURFACE_VARIANT = neutralVariant.get(30)
            OUTLINE = neutralVariant.get(50)
            OUTLINE_VARIANT = neutralVariant.get(80)
            INVERSE_SURFACE = neutral.get(20)
            INVERSE_ON_SURFACE = neutral.get(95)
            INVERSE_PRIMARY = primary.get(80)
            SURFACE_DIM = neutral.get(87)
            SURFACE_BRIGHT = neutral.get(98)
            SURFACE_CONTAINER_LOWEST = neutral.get(100)
            SURFACE_CONTAINER_LOW = neutral.get(96)
            SURFACE_CONTAINER = neutral.get(94)
            SURFACE_CONTAINER_HIGH = neutral.get(92)
            SURFACE_CONTAINER_HIGHEST = neutral.get(90)

            TEXT_PRIMARY = ON_SURFACE
            TEXT_SECONDARY = ON_SURFACE_VARIANT
            TEXT_MUTED = OUTLINE
        }
        SHADOW = Color(0, 0, 0)
    }

    private fun buildPalette(seed: Color, hueShift: Float, satMul: Float): TonalPalette {
        val hsb = Color.RGBtoHSB(seed.red, seed.green, seed.blue, null)
        val hue = (hsb[0] + hueShift) % 1.0f
        val sat = (hsb[1] * satMul).coerceAtMost(1.0f)
        val levels = intArrayOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100)
        val colors = Array(levels.size) { i ->
            val tone = levels[i] / 100f
            val satFactor = 1.0f - 0.25f * abs(levels[i] - 50) / 50f
            Color(Color.HSBtoRGB(hue, sat * satFactor, tone))
        }
        return TonalPalette(colors)
    }

    private class TonalPalette(val colors: Array<Color>) {
        fun get(tone: Int): Color {
            val idx = when {
                tone <= 0 -> 0
                tone <= 10 -> 1
                tone <= 20 -> 2
                tone <= 30 -> 3
                tone <= 40 -> 4
                tone <= 50 -> 5
                tone <= 60 -> 6
                tone <= 70 -> 7
                tone <= 80 -> 8
                tone <= 90 -> 9
                tone <= 95 -> 10
                tone <= 99 -> 11
                else -> 12
            }
            return colors[idx]
        }
    }

    fun withAlpha(c: Color, alpha: Int): Color = Color(c.red, c.green, c.blue, alpha)

    fun lerp(a: Color, b: Color, t: Float): Color {
        val i = 1.0f - t
        return Color(
            (a.red * i + b.red * t).toInt(),
            (a.green * i + b.green * t).toInt(),
            (a.blue * i + b.blue * t).toInt(),
            (a.alpha * i + b.alpha * t).toInt()
        )
    }

}
