package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Mouse
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

object WaterMark : Module("WaterMark", Category.RENDER) {

    private val mode by choices("Mode", arrayOf("Simple", "OnlyText", "NL", "Imitate"), "Simple")

    private var renderX = 40.0f
    private var renderY = 10.0f
    private var dragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    override fun onDisable() {
        dragging = false
    }

    private fun isHovered(mouseX: Int, mouseY: Int, x: Float, y: Float, w: Float, h: Float): Boolean {
        return mouseX.toFloat() >= x && mouseX.toFloat() <= x + w
            && mouseY.toFloat() >= y && mouseY.toFloat() <= y + h
    }

    private fun updateDrag(mouseX: Int, mouseY: Int, totalW: Float, totalH: Float, screenW: Int, screenH: Int) {
        if (dragging) {
            renderX = (mouseX - dragOffsetX).coerceIn(0f, (screenW - totalW).coerceAtLeast(0f))
            renderY = (mouseY - dragOffsetY).coerceIn(0f, (screenH - totalH).coerceAtLeast(0f))
        }
        val hovered = isHovered(mouseX, mouseY, renderX, renderY, totalW, totalH)
        if (Mouse.isButtonDown(0)) {
            if (hovered && !dragging) {
                dragging = true
                dragOffsetX = mouseX - renderX
                dragOffsetY = mouseY - renderY
            }
        } else {
            dragging = false
        }
    }

    val onRender2D = handler<Render2DEvent> {
        val sr = ScaledResolution(mc)
        val screenW = sr.scaledWidth
        val screenH = sr.scaledHeight
        val fr = mc.fontRendererObj

        if (fr == null) return@handler

        val time = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
        val name = mc.thePlayer?.name ?: "Player"
        val fps = "${Minecraft.getDebugFPS()} fps"

        val mouseX = (Mouse.getX().toFloat() * screenW / mc.displayWidth).toInt()
        val mouseY = (screenH - Mouse.getY().toFloat() * screenH / mc.displayHeight - 1f).toInt()

        when (mode) {
            "Simple" -> {
                val separator = "  |  "
                val text = "EDNeko$separator$name$separator$time$separator$fps"
                val textW = fr.getStringWidth(text).toFloat()
                val textH = fr.FONT_HEIGHT.toFloat()
                val totalW = textW + 8f
                val totalH = textH + 6f

                updateDrag(mouseX, mouseY, totalW, totalH, screenW, screenH)

                RenderUtils.drawRect(renderX, renderY, renderX + totalW, renderY + totalH, Color(9, 7, 35, 200).rgb)

                var cx = renderX + 4f
                val cy = renderY + 3f

                val parts = listOf("EDNeko", separator, name, separator, time, separator, fps)
                val accentColor = Color(0, 149, 200, 255).rgb
                for ((i, part) in parts.withIndex()) {
                    val color = if (i == 0) accentColor else if (i % 2 == 1) Color(100, 100, 100, 255).rgb else -1
                    fr.drawString(part, cx.toInt(), cy.toInt(), color)
                    cx += fr.getStringWidth(part)
                }
            }
            "OnlyText" -> {
                val text = "EDNeko"
                val textW = fr.getStringWidth(text).toFloat()
                val textH = fr.FONT_HEIGHT.toFloat()
                val totalW = textW + 8f
                val totalH = textH + 6f

                updateDrag(mouseX, mouseY, totalW, totalH, screenW, screenH)

                fr.drawString(text, renderX.toInt(), renderY.toInt(), Color(0, 149, 200, 255).rgb)
            }
            "NL" -> {
                val text = "$name  $time  $fps"
                val brandWidth = fr.getStringWidth("EDNeko").toFloat()
                val infoWidth = fr.getStringWidth(text).toFloat()
                val totalW = brandWidth + infoWidth + 22f
                val totalH = fr.FONT_HEIGHT + 6f

                updateDrag(mouseX, mouseY, totalW, totalH, screenW, screenH)

                RenderUtils.drawRoundedRect(renderX + 3f, renderY + 4.5f, brandWidth + 5f, fr.FONT_HEIGHT + 2f,
                    Color(9, 7, 35, 255).rgb, 3f)
                RenderUtils.drawRoundedRect(renderX + 12f + brandWidth, renderY + 4.5f, 11f + infoWidth,
                    fr.FONT_HEIGHT + 2f, Color(9, 7, 35, 255).rgb, 3f)

                val blue = Color(0, 149, 200, 255).rgb
                fr.drawString(text, (renderX + 19.5f + brandWidth).toInt(), (renderY + 9.5f).toInt(), blue)
                fr.drawString(text, (renderX + 19f + brandWidth).toInt(), (renderY + 9f).toInt(), -1)
                fr.drawString("EDNeko", (renderX + 5.5f).toInt(), (renderY + 8f).toInt(), blue)
                fr.drawString("EDNeko", (renderX + 5f).toInt(), (renderY + 7.5f).toInt(), -1)
            }
            "Imitate" -> {
                val info = "EDNeko     <|>   $name   <|>   $time   <|>   $fps  "
                val totalW = fr.getStringWidth(info) + 10f
                val totalH = fr.FONT_HEIGHT + 9f

                updateDrag(mouseX, mouseY, totalW, totalH, screenW, screenH)

                val x1 = renderX + 3f
                val y1 = renderY + 4.5f
                val x2 = totalW - 3f
                val y2 = totalH - 3f

                RenderUtils.drawRoundedRect(x1, y1, renderX + x2, renderY + y2,
                    Color(9, 7, 35, 200).rgb, 3f)

                val y = renderY + 4.5f + (totalH - 3f) / 2f - 3f
                var cx = renderX + 5f

                fr.drawString("EDNeko  ", cx.toInt(), y.toInt(), Color.CYAN.rgb)
                cx += fr.getStringWidth("EDNeko  ")
                fr.drawString("   <|>   $name", cx.toInt(), y.toInt(), Color.WHITE.rgb)
                cx += fr.getStringWidth("   <|>   $name")
                fr.drawString("   <|>   $time", cx.toInt(), y.toInt(), Color.WHITE.rgb)
            }
        }
    }
}