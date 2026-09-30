/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.utils.GlowUtils
import net.minecraft.client.gui.FontRenderer
import java.awt.Color

/**
 * 文字发光工具：用 GlowUtils 逐字符生成小矩形并逐个发光（在文字下层绘制）。
 * @param color       文字颜色（ARGB），adaptColor 为 true 时复用它作为发光色
 * @param strength    模糊半径（扩散强度）
 * @param spread      圆角 / 扩散系数
 * @param adaptColor  是否自适应文字颜色
 * @param customColor adaptColor 为 false 时使用的自定义颜色
 */
object TextGlowUtils {

    fun drawGlowText(
        text: String,
        x: Float,
        y: Float,
        color: Int,
        font: FontRenderer,
        strength: Float,
        spread: Float,
        adaptColor: Boolean,
        customColor: Int
    ) {
        if (text.isEmpty()) return
        // 自适应：文字颜色为 0（渐变/彩虹）时用白色兜底
        val resolved = if (adaptColor) {
            if ((color ushr 24) and 0xFF == 0) Color(255, 255, 255, 90).rgb else color
        } else customColor
        val blurRadius = strength.toInt().coerceIn(1, 55)
        val glowColor = Color(resolved, true)
        val fontHeight = font.FONT_HEIGHT.toFloat()

        var cx = x
        for (ch in text) {
            val w = font.getCharWidth(ch).toFloat()
            if (ch != ' ' && w > 0f) {
                GlowUtils.drawGlow(cx, y, w, fontHeight, blurRadius, glowColor)
            }
            cx += w
        }
    }
}