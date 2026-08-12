/*
 * edneko Hacked Client
 * A free open source mixin-based injection hacked client built on Liquidbounce legacy codebase.
 * https://github.com/lmx0721/edneko
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.moonlight

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.BoolValue
import net.ccbluex.liquidbounce.config.ColorValue
import net.ccbluex.liquidbounce.config.FloatValue
import net.ccbluex.liquidbounce.config.IntValue
import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.config.TextValue
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.file.FileManager.modulesConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import java.io.IOException
import kotlin.math.roundToInt

class MoonLightClickGui : GuiScreen() {

    private data class Panel(
        val category: Category,
        var x: Float,
        var y: Float,
        var opened: Boolean = true,
        var dragging: Boolean = false,
        var dragX: Float = 0f,
        var dragY: Float = 0f
    )

    private val panels = Category.entries.mapIndexed { index, category ->
        Panel(category, 50f + index * 125f, 20f)
    }.toMutableList()

    private val openedModules = HashSet<Module>()
    private var scroll = 0f
    private var focusedText: TextValue? = null
    private var textBuffer = ""
    private var bindingModule: Module? = null
    private var draggingNumber: Value<*>? = null
    private var valuesDirty = false
    private var modulesDirty = false

    private val panelWidth = 115f
    private val headerHeight = 21f
    private val moduleHeight = 19f
    private val valueHeight = 17f
    private val accent = Color(92, 148, 255)
    private val panelColor = Color(18, 23, 35, 232)
    private val moduleColor = Color(27, 34, 49, 235)

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        super.initGui()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        flushConfigs()
        super.onGuiClosed()
    }

    override fun doesGuiPauseGame() = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawRect(0f, 0f, width.toFloat(), height.toFloat(), Color(0, 0, 0, 80).rgb)
        handleWheel()

        if (!Mouse.isButtonDown(0)) {
            draggingNumber = null
        }

        val logicalMouseY = mouseY - scroll
        panels.forEachIndexed { index, panel ->
            if (panel.dragging) {
                panel.x = mouseX + panel.dragX
                panel.y = logicalMouseY + panel.dragY
            }
            drawPanel(panel, index, mouseX, logicalMouseY)
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val logicalMouseY = mouseY - scroll

        if (bindingModule != null) {
            bindingModule = null
            return
        }

        panels.asReversed().forEach { panel ->
            if (isHovered(panel.x, panel.y - 2f, panelWidth, headerHeight, mouseX, logicalMouseY)) {
                when (mouseButton) {
                    0 -> {
                        panel.dragging = true
                        panel.dragX = panel.x - mouseX
                        panel.dragY = panel.y - logicalMouseY
                    }
                    1 -> panel.opened = !panel.opened
                }
                focusedText = null
                return@mouseClicked
            }

            if (panel.opened && clickPanel(panel, mouseX, logicalMouseY, mouseButton)) {
                return@mouseClicked
            }
        }

        focusedText = null
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        panels.forEach { it.dragging = false }
        draggingNumber = null
        flushConfigs()
        super.mouseReleased(mouseX, mouseY, state)
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        bindingModule?.let { module ->
            module.keyBind = if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) Keyboard.KEY_NONE else keyCode
            bindingModule = null
            modulesDirty = true
            saveConfig(modulesConfig)
            return
        }

        focusedText?.let { value ->
            when (keyCode) {
                Keyboard.KEY_ESCAPE, Keyboard.KEY_RETURN -> {
                    focusedText = null
                    flushConfigs()
                }
                Keyboard.KEY_BACK -> if (textBuffer.isNotEmpty()) {
                    textBuffer = textBuffer.dropLast(1)
                    value.set(textBuffer)
                    valuesDirty = true
                }
                else -> if (typedChar.code >= 32 && typedChar.code != 127 && textBuffer.length < 64) {
                    textBuffer += typedChar
                    value.set(textBuffer)
                    valuesDirty = true
                }
            }
            return
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    private fun drawPanel(panel: Panel, index: Int, mouseX: Int, logicalMouseY: Float) {
        val modules = LiquidBounce.moduleManager[panel.category].sortedBy { it.name.lowercase() }
        val contentHeight = if (panel.opened) panelContentHeight(modules) else 0f
        val screenY = panel.y + scroll
        val panelHeight = headerHeight + contentHeight + 4f
        val color = accentAt(index)

        RoundedUtil.drawRound(panel.x, screenY - 2f, panelWidth, panelHeight, 6f, panelColor)
        RoundedUtil.drawRound(panel.x + 2f, screenY, panelWidth - 4f, headerHeight - 4f, 5f, Color(color.red, color.green, color.blue, 65))
        Fonts.font35.drawString(panel.category.displayName, panel.x + 8f, screenY + 6f, Color.WHITE.rgb)
        Fonts.font35.drawString(if (panel.opened) "-" else "+", panel.x + panelWidth - 13f, screenY + 6f, color.rgb)

        if (!panel.opened) return

        var y = panel.y + headerHeight
        modules.forEach { module ->
            drawModule(panel.x + 7f, y + scroll, panelWidth - 14f, module, mouseX, logicalMouseY)
            y += moduleHeight

            if (module in openedModules) {
                module.values.filter { it.shouldRender() }.forEach { value ->
                    drawValue(panel.x + 7f, y + scroll, panelWidth - 14f, value, mouseX, logicalMouseY)
                    y += valueRenderHeight(value)
                }
            }
        }
    }

    private fun drawModule(x: Float, screenY: Float, w: Float, module: Module, mouseX: Int, logicalMouseY: Float) {
        val logicalY = screenY - scroll
        val hovered = isHovered(x, logicalY, w, moduleHeight - 2f, mouseX, logicalMouseY)
        val bg = if (module.state) Color(accent.red, accent.green, accent.blue, 105) else moduleColor
        RoundedUtil.drawRound(x, screenY, w, moduleHeight - 4f, 3f, if (hovered && !module.state) Color(42, 50, 68, 235) else bg)

        val label = if (bindingModule == module) "press key..." else module.name
        val labelColor = if (module.state) Color.WHITE.rgb else Color(170, 178, 190).rgb
        Fonts.font35.drawString(label, x + w / 2f - Fonts.font35.getStringWidth(label) / 2f, screenY + 5f, labelColor)
        if (module.values.any { it.shouldRender() }) {
            drawRect(x + w - 9f, screenY + 6f, 4f, 4f, if (module in openedModules) accent.rgb else Color(120, 128, 142).rgb)
        }
    }

    private fun drawValue(x: Float, screenY: Float, w: Float, value: Value<*>, mouseX: Int, logicalMouseY: Float) {
        val logicalY = screenY - scroll
        RoundedUtil.drawRound(x, screenY, w, valueRenderHeight(value) - 3f, 3f, Color(22, 28, 42, 232))
        val label = value.name

        when (value) {
            is BoolValue -> {
                Fonts.font35.drawString(label, x + 5f, screenY + 5f, Color(190, 196, 205).rgb)
                RoundedUtil.drawRound(x + w - 18f, screenY + 4f, 11f, 8f, 4f, if (value.get()) accent else Color(60, 66, 80))
            }
            is IntValue -> drawNumberValue(x, screenY, w, label, value.get().toFloat(), value.minimum.toFloat(), value.maximum.toFloat())
            is FloatValue -> drawNumberValue(x, screenY, w, label, value.get(), value.minimum, value.maximum)
            is ListValue -> {
                Fonts.font35.drawString(label, x + 5f, screenY + 5f, Color(190, 196, 205).rgb)
                val mode = value.get()
                Fonts.font35.drawString(mode, x + w - Fonts.font35.getStringWidth(mode) - 5f, screenY + 5f, accent.rgb)
            }
            is TextValue -> {
                Fonts.font35.drawString(label, x + 5f, screenY + 4f, Color(190, 196, 205).rgb)
                val text = if (focusedText == value) textBuffer + "_" else value.get()
                RoundedUtil.drawRound(x + 5f, screenY + 15f, w - 10f, 12f, 2f, Color(12, 16, 26, 230))
                Fonts.font35.drawString(trimToWidth(if (text.isEmpty()) "Empty..." else text, (w - 16f).toInt()), x + 8f, screenY + 18f, if (focusedText == value) Color.WHITE.rgb else Color(160, 168, 180).rgb)
            }
            is ColorValue -> {
                Fonts.font35.drawString(label, x + 5f, screenY + 5f, Color(190, 196, 205).rgb)
                RoundedUtil.drawRound(x + w - 18f, screenY + 4f, 11f, 8f, 2f, value.selectedColor())
                if (value.rainbow) Fonts.font35.drawString("R", x + w - 30f, screenY + 5f, accent.rgb)
            }
            else -> Fonts.font35.drawString("$label: ${value.toText()}", x + 5f, screenY + 5f, Color(190, 196, 205).rgb)
        }

        if (draggingNumber == value && Mouse.isButtonDown(0)) {
            updateNumberValue(value, x + 5f, w - 10f, mouseX)
        }
    }

    private fun drawNumberValue(x: Float, screenY: Float, w: Float, label: String, current: Float, min: Float, max: Float) {
        val sliderX = x + 5f
        val sliderY = screenY + 16f
        val sliderW = w - 10f
        val progress = if (max == min) 0f else ((current - min) / (max - min)).coerceIn(0f, 1f)
        Fonts.font35.drawString(label, x + 5f, screenY + 4f, Color(190, 196, 205).rgb)
        Fonts.font35.drawString(formatNumber(current), x + w - Fonts.font35.getStringWidth(formatNumber(current)) - 5f, screenY + 4f, accent.rgb)
        RoundedUtil.drawRound(sliderX, sliderY, sliderW, 3f, 2f, Color(52, 58, 72))
        RoundedUtil.drawRound(sliderX, sliderY, sliderW * progress, 3f, 2f, accent)
        RoundedUtil.drawRound(sliderX + sliderW * progress - 2f, sliderY - 2f, 6f, 6f, 3f, Color.WHITE)
    }

    private fun clickPanel(panel: Panel, mouseX: Int, logicalMouseY: Float, mouseButton: Int): Boolean {
        var y = panel.y + headerHeight
        LiquidBounce.moduleManager[panel.category].sortedBy { it.name.lowercase() }.forEach { module ->
            if (isHovered(panel.x + 7f, y, panelWidth - 14f, moduleHeight - 2f, mouseX, logicalMouseY)) {
                when (mouseButton) {
                    0 -> {
                        module.toggle()
                        modulesDirty = true
                    }
                    1 -> if (module.values.any { it.shouldRender() }) {
                        if (!openedModules.add(module)) openedModules.remove(module)
                    }
                    2 -> bindingModule = module
                }
                focusedText = null
                return true
            }
            y += moduleHeight

            if (module in openedModules) {
                module.values.filter { it.shouldRender() }.forEach { value ->
                    val h = valueRenderHeight(value)
                    if (isHovered(panel.x + 7f, y, panelWidth - 14f, h - 3f, mouseX, logicalMouseY)) {
                        clickValue(value, panel.x + 7f, mouseX, mouseButton)
                        return true
                    }
                    y += h
                }
            }
        }
        return false
    }

    private fun clickValue(value: Value<*>, x: Float, mouseX: Int, mouseButton: Int) {
        when (value) {
            is BoolValue -> if (mouseButton == 0) {
                value.toggle()
                valuesDirty = true
            }
            is IntValue, is FloatValue -> if (mouseButton == 0) {
                draggingNumber = value
                updateNumberValue(value, x + 5f, panelWidth - 24f, mouseX)
            }
            is ListValue -> {
                val values = value.values
                val index = values.indexOf(value.get()).takeIf { it >= 0 } ?: 0
                val next = if (mouseButton == 1) (index - 1 + values.size) % values.size else (index + 1) % values.size
                value.set(values[next])
                valuesDirty = true
            }
            is TextValue -> if (mouseButton == 0) {
                focusedText = value
                textBuffer = value.get()
            }
            is ColorValue -> {
                if (mouseButton == 1) {
                    value.rainbow = !value.rainbow
                } else {
                    val color = Color.getHSBColor(((System.currentTimeMillis() / 14L) % 360L) / 360f, 0.48f, 1f)
                    value.set(Color(color.red, color.green, color.blue, value.selectedColor().alpha))
                }
                valuesDirty = true
            }
            else -> Unit
        }
    }

    private fun updateNumberValue(value: Value<*>, sliderX: Float, sliderW: Float, mouseX: Int) {
        val progress = ((mouseX - sliderX) / sliderW).coerceIn(0f, 1f)
        when (value) {
            is IntValue -> {
                value.set((value.minimum + (value.maximum - value.minimum) * progress).roundToInt())
                valuesDirty = true
            }
            is FloatValue -> {
                val next = value.minimum + (value.maximum - value.minimum) * progress
                value.set((next * 100f).roundToInt() / 100f)
                valuesDirty = true
            }
            else -> Unit
        }
    }

    private fun handleWheel() {
        val wheel = Mouse.getDWheel()
        if (wheel == 0) return
        scroll = (scroll + if (wheel > 0) 15f else -15f).coerceIn(minScroll(), 120f)
    }

    private fun minScroll(): Float {
        val lowest = panels.maxOfOrNull { panel ->
            panel.y + headerHeight + if (panel.opened) panelContentHeight(LiquidBounce.moduleManager[panel.category]) else 0f
        } ?: 0f
        return (height - lowest - 25f).coerceAtMost(0f)
    }

    private fun panelContentHeight(modules: List<Module>) = modules.sumOf { module ->
        (moduleHeight + if (module in openedModules) {
            module.values.filter { it.shouldRender() }.sumOf { valueRenderHeight(it).toDouble() }.toFloat()
        } else 0f).toDouble()
    }.toFloat()

    private fun valueRenderHeight(value: Value<*>) = when (value) {
        is IntValue, is FloatValue -> 30f
        is TextValue -> 32f
        else -> valueHeight
    }

    private fun flushConfigs() {
        if (valuesDirty) {
            saveConfig(valuesConfig)
            valuesDirty = false
        }
        if (modulesDirty) {
            saveConfig(modulesConfig)
            modulesDirty = false
        }
    }

    private fun accentAt(index: Int): Color {
        val hue = ((System.currentTimeMillis() % 6000L) / 6000f + index * 0.035f) % 1f
        return Color.getHSBColor(hue, 0.42f, 1f)
    }

    private fun trimToWidth(text: String, maxWidth: Int): String {
        if (Fonts.font35.getStringWidth(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.isNotEmpty() && Fonts.font35.getStringWidth("...$trimmed") > maxWidth) {
            trimmed = trimmed.drop(1)
        }
        return "...$trimmed"
    }

    private fun formatNumber(value: Float) =
        if (value % 1f == 0f) value.roundToInt().toString() else "%.2f".format(value)

    private fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Int) =
        RenderUtils.drawRect(x, y, x + w, y + h, color)

    private fun isHovered(x: Float, y: Float, w: Float, h: Float, mouseX: Int, mouseY: Float) =
        mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h
}
