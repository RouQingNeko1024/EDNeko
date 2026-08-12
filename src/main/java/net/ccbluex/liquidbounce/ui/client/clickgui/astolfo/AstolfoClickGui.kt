/*
 * edneko Hacked Client
 * A free open source mixin-based injection hacked client built on Liquidbounce legacy codebase.
 * https://github.com/lmx0721/edneko
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.astolfo

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
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.opengl.GL11
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

class AstolfoClickGui : GuiScreen() {

    private data class Window(
        val category: Category,
        var x: Float,
        var y: Float,
        var expanded: Boolean = true,
        var dragging: Boolean = false,
        var dragX: Float = 0f,
        var dragY: Float = 0f,
        var scroll: Float = 0f
    )

    private val windows = Category.entries.mapIndexed { index, category ->
        Window(category, 50f + index * 110f, 50f)
    }.toMutableList()

    private val expandedModules = HashSet<Module>()
    private var focusedText: TextValue? = null
    private var textBuffer = ""
    private var bindingModule: Module? = null
    private var draggingNumber: Value<*>? = null
    private var valuesDirty = false
    private var modulesDirty = false

    private val panelWidth = 100f
    private val headerHeight = 15f
    private val moduleHeight = 13f
    private val valueHeight = 13f
    private val accent = Color(164, 53, 144)
    private val wheelStep = 18f

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
        drawRect(0, 0, width, height, Color(0, 0, 0, 150).rgb)

        if (!Mouse.isButtonDown(0)) {
            draggingNumber = null
        }

        handleMouseWheel(mouseX, mouseY)

        windows.forEachIndexed { index, window ->
            if (window.dragging) {
                window.x = mouseX + window.dragX
                window.y = mouseY + window.dragY
            }
            drawWindow(window, index, mouseX, mouseY)
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (bindingModule != null) {
            bindingModule = null
            return
        }

        focusedText?.let {
            if (mouseButton == 1) {
                it.set("")
                textBuffer = ""
                valuesDirty = true
            }
        }

        windows.asReversed().forEach { window ->
            if (isHovered(window.x, window.y, panelWidth, headerHeight, mouseX, mouseY)) {
                if (mouseButton == 0) {
                    window.dragging = true
                    window.dragX = window.x - mouseX
                    window.dragY = window.y - mouseY
                } else if (mouseButton == 1) {
                    window.expanded = !window.expanded
                }
                return@mouseClicked
            }

            if (window.expanded && clickWindowBody(window, mouseX, mouseY, mouseButton)) {
                return@mouseClicked
            }
        }

        focusedText = null
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        windows.forEach { it.dragging = false }
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
                else -> if (typedChar.code >= 32 && typedChar.code != 127) {
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

    private fun drawWindow(window: Window, index: Int, mouseX: Int, mouseY: Int) {
        val modules = LiquidBounce.moduleManager[window.category].sortedBy { it.name.lowercase() }
        val bodyHeight = bodyHeight(modules)
        val visibleBodyHeight = if (window.expanded) visibleBodyHeight(window, bodyHeight) else 0f
        val contentHeight = headerHeight + visibleBodyHeight

        clampScroll(window, bodyHeight, visibleBodyHeight)

        RenderUtils.drawBorderedRect(
            window.x,
            window.y,
            window.x + panelWidth,
            window.y + contentHeight,
            1f,
            accentAt(index).rgb,
            Color(25, 25, 25).rgb
        )
        drawRect(window.x + 1f, window.y + 1f, panelWidth - 2f, headerHeight - 2f, Color(31, 31, 31).rgb)
        Fonts.font35.drawString(window.category.displayName.lowercase(), window.x + 5f, window.y + 4f, Color.WHITE.rgb)
        Fonts.font35.drawString(if (window.expanded) "-" else "+", window.x + panelWidth - 10f, window.y + 4f, accentAt(index).rgb)

        if (!window.expanded) return

        startScissor(window.x + 1f, window.y + headerHeight, panelWidth - 2f, visibleBodyHeight)
        var y = window.y + headerHeight - window.scroll
        modules.forEach { module ->
            drawModule(window.x, y, module, mouseX, mouseY)
            y += moduleHeight

            if (module in expandedModules) {
                module.values.filter { it.shouldRender() }.forEach { value ->
                    drawValue(window.x, y, value, mouseX, mouseY)
                    y += valueRenderHeight(value)
                }
            }
        }
        endScissor()

        if (bodyHeight > visibleBodyHeight) {
            val barHeight = max(16f, visibleBodyHeight * (visibleBodyHeight / bodyHeight))
            val barY = window.y + headerHeight + (visibleBodyHeight - barHeight) * (window.scroll / (bodyHeight - visibleBodyHeight))
            drawRect(window.x + panelWidth - 4f, window.y + headerHeight + 1f, 2f, visibleBodyHeight - 2f, Color(45, 45, 45).rgb)
            drawRect(window.x + panelWidth - 4f, barY, 2f, barHeight, accentAt(index).rgb)
        }
    }

    private fun drawModule(x: Float, y: Float, module: Module, mouseX: Int, mouseY: Int) {
        val hovered = isHovered(x, y, panelWidth, moduleHeight, mouseX, mouseY)
        val activeColor = if (module.state) accent.rgb else Color(36, 36, 36).rgb
        drawRect(x + 3f, y + 1f, panelWidth - 5f, moduleHeight - 2f, activeColor)
        if (hovered && !module.state) drawRect(x + 3f, y + 1f, panelWidth - 5f, moduleHeight - 2f, Color(255, 255, 255, 42).rgb)

        val label = if (bindingModule == module) "press key..." else module.name.lowercase()
        val color = if (module.state && module in expandedModules) accent.rgb else Color(172, 172, 172).rgb
        Fonts.font35.drawString(label, x + panelWidth - Fonts.font35.getStringWidth(label) - 4f, y + 4f, color)
    }

    private fun drawValue(x: Float, y: Float, value: Value<*>, mouseX: Int, mouseY: Int) {
        val label = value.name.lowercase()
        drawRect(x + 3f, y, panelWidth - 5f, valueRenderHeight(value), Color(29, 29, 29).rgb)

        when (value) {
            is BoolValue -> {
                Fonts.font35.drawString(label, x + 8f, y + 4f, Color(150, 150, 150).rgb)
                drawRect(x + panelWidth - 14f, y + 3f, 7f, 7f, if (value.get()) accent.rgb else Color(48, 48, 48).rgb)
            }
            is IntValue -> drawNumberValue(x, y, label, value.get().toFloat(), value.minimum.toFloat(), value.maximum.toFloat())
            is FloatValue -> drawNumberValue(x, y, label, value.get(), value.minimum, value.maximum)
            is ListValue -> {
                val mode = value.get().lowercase()
                Fonts.font35.drawString(label, x + 8f, y + 4f, Color(150, 150, 150).rgb)
                Fonts.font35.drawString(mode, x + panelWidth - Fonts.font35.getStringWidth(mode) - 8f, y + 4f, accent.rgb)
            }
            is TextValue -> {
                val text = if (focusedText == value) textBuffer + "_" else value.get()
                Fonts.font35.drawString(label, x + 8f, y + 4f, Color(150, 150, 150).rgb)
                Fonts.font35.drawString(trimToWidth(text, 45), x + panelWidth - 50f, y + 4f, if (focusedText == value) Color.WHITE.rgb else accent.rgb)
            }
            is ColorValue -> {
                Fonts.font35.drawString(label, x + 8f, y + 4f, Color(150, 150, 150).rgb)
                drawRect(x + panelWidth - 18f, y + 3f, 10f, 7f, value.selectedColor().rgb)
                if (value.rainbow) Fonts.font35.drawString("r", x + panelWidth - 28f, y + 4f, accent.rgb)
            }
            else -> Fonts.font35.drawString("$label: ${value.toText()}", x + 8f, y + 4f, Color(150, 150, 150).rgb)
        }

        if (draggingNumber == value && Mouse.isButtonDown(0)) {
            updateNumberValue(value, x + 7f, panelWidth - 14f, mouseX)
        }
    }

    private fun drawNumberValue(x: Float, y: Float, label: String, current: Float, min: Float, max: Float) {
        val sliderX = x + 8f
        val sliderY = y + 12f
        val sliderW = panelWidth - 16f
        val progress = if (max == min) 0f else ((current - min) / (max - min)).coerceIn(0f, 1f)
        Fonts.font35.drawString(label, x + 8f, y + 4f, Color(150, 150, 150).rgb)
        Fonts.font35.drawString(formatNumber(current), x + panelWidth - Fonts.font35.getStringWidth(formatNumber(current)) - 8f, y + 4f, accent.rgb)
        drawRect(sliderX, sliderY, sliderW, 2f, Color(48, 48, 48).rgb)
        drawRect(sliderX, sliderY, sliderW * progress, 2f, accent.rgb)
    }

    private fun clickWindowBody(window: Window, mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        val modules = LiquidBounce.moduleManager[window.category].sortedBy { it.name.lowercase() }
        val bodyHeight = bodyHeight(modules)
        val visibleBodyHeight = visibleBodyHeight(window, bodyHeight)
        if (!isHovered(window.x, window.y + headerHeight, panelWidth, visibleBodyHeight, mouseX, mouseY)) return false

        var y = window.y + headerHeight - window.scroll
        modules.forEach { module ->
            if (isHovered(window.x, y, panelWidth, moduleHeight, mouseX, mouseY)) {
                when (mouseButton) {
                    0 -> {
                        module.toggle()
                        modulesDirty = true
                    }
                    1 -> if (module.values.any { it.shouldRender() }) {
                        if (!expandedModules.add(module)) expandedModules.remove(module)
                    }
                    2 -> bindingModule = module
                }
                focusedText = null
                return true
            }
            y += moduleHeight

            if (module in expandedModules) {
                module.values.filter { it.shouldRender() }.forEach { value ->
                    val h = valueRenderHeight(value)
                    if (isHovered(window.x + 3f, y, panelWidth - 5f, h, mouseX, mouseY)) {
                        clickValue(value, window.x, mouseX, mouseButton)
                        return true
                    }
                    y += h
                }
            }
        }

        return false
    }

    private fun handleMouseWheel(mouseX: Int, mouseY: Int) {
        val wheel = Mouse.getDWheel()
        if (wheel == 0) return

        windows.asReversed().firstOrNull { window ->
            window.expanded && isHovered(window.x, window.y, panelWidth, headerHeight + visibleBodyHeight(window, bodyHeight(LiquidBounce.moduleManager[window.category])), mouseX, mouseY)
        }?.let { window ->
            val modules = LiquidBounce.moduleManager[window.category]
            val bodyHeight = bodyHeight(modules)
            val visibleBodyHeight = visibleBodyHeight(window, bodyHeight)
            window.scroll -= wheel.sign() * wheelStep
            clampScroll(window, bodyHeight, visibleBodyHeight)
        }
    }

    private fun clickValue(value: Value<*>, windowX: Float, mouseX: Int, mouseButton: Int) {
        when (value) {
            is BoolValue -> if (mouseButton == 0) {
                value.toggle()
                valuesDirty = true
            }
            is IntValue, is FloatValue -> if (mouseButton == 0) {
                draggingNumber = value
                updateNumberValue(value, windowX + 7f, panelWidth - 14f, mouseX)
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
                    val color = Color.getHSBColor(((System.currentTimeMillis() / 12L) % 360L) / 360f, 0.55f, 1f)
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
                val next = (value.minimum + (value.maximum - value.minimum) * progress).roundToInt()
                value.set(next)
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

    private fun valueRenderHeight(value: Value<*>) = when (value) {
        is IntValue, is FloatValue -> 18f
        else -> valueHeight
    }

    private fun bodyHeight(modules: List<Module>) = modules.sumOf { module ->
        (moduleHeight + if (module in expandedModules) {
            module.values.filter { it.shouldRender() }.sumOf { valueRenderHeight(it).toDouble() }.toFloat()
        } else 0f).toDouble()
    }.toFloat()

    private fun visibleBodyHeight(window: Window, bodyHeight: Float): Float {
        val availableHeight = (height - window.y - 20f - headerHeight).coerceAtLeast(moduleHeight)
        return bodyHeight.coerceAtMost(availableHeight).coerceAtLeast(0f)
    }

    private fun clampScroll(window: Window, bodyHeight: Float, visibleBodyHeight: Float) {
        val maxScroll = (bodyHeight - visibleBodyHeight).coerceAtLeast(0f)
        window.scroll = window.scroll.coerceIn(0f, maxScroll)
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
        val hue = ((System.currentTimeMillis() % 5000L) / 5000f + index * 0.045f) % 1f
        return Color.getHSBColor(hue, 0.45f, 1f)
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

    private fun isHovered(x: Float, y: Float, w: Float, h: Float, mouseX: Int, mouseY: Int) =
        mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h

    private fun startScissor(x: Float, y: Float, w: Float, h: Float) {
        RenderUtils.makeScissorBox(x, y, x + w, y + h)
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
    }

    private fun endScissor() = GL11.glDisable(GL11.GL_SCISSOR_TEST)

    private fun Int.sign() = when {
        this > 0 -> 1
        this < 0 -> -1
        else -> 0
    }
}
