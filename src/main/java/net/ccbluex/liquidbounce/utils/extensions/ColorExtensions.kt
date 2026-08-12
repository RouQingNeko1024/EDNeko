/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.extensions

import java.awt.Color

fun Color.withAlpha(a: Int) = Color(red, green, blue, a)

fun Color.darker(factor: Float = 0.5F): Color {
    return Color(
        (red * factor).toInt().coerceIn(0, 255),
        (green * factor).toInt().coerceIn(0, 255),
        (blue * factor).toInt().coerceIn(0, 255),
        alpha
    )
}
