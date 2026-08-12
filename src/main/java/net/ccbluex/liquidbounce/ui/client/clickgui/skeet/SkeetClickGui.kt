/*
 * edneko Hacked Client
 * A free open source mixin-based injection hacked client built on Liquidbounce legacy codebase.
 * https://github.com/lmx0721/edneko
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.skeet

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.file.FileManager.modulesConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.IOException
import javax.vecmath.Vector2f
import kotlin.math.roundToInt

class SkeetClickGui : GuiScreen() {

    private enum class UiEvent { DRAW, CLICK, RELEASE }

    private data class ColorState(
        var draggingColor: Boolean = false,
        var draggingHue: Boolean = false,
        var draggingAlpha: Boolean = false
    )

    private var x = 120f
    private var y = 80f
    private var w = 560f
    private var h = 340f
    private var dragX = 0f
    private var dragY = 0f
    private var dragging = false
    private var resizing = false
    private var selectedCategory = Category.COMBAT
    private var selectedModule: Module? = null
    private var moduleScroll = 0f
    private var valueScroll = 0f
    private var keyListening = false
    private var draggedFloat: FloatValue? = null
    private var draggedInt: IntValue? = null
    private val textFields = HashMap<TextValue, GuiTextField>()
    private val colorStates = HashMap<ColorValue, ColorState>()

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        if (x + w > width || y + h > height) {
            x = width / 2f - w / 2f
            y = height / 2f - h / 2f
        }
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        saveConfig(valuesConfig)
        saveConfig(modulesConfig)
    }

    override fun doesGuiPauseGame() = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        handle(mouseX, mouseY, -1, UiEvent.DRAW)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        handle(mouseX, mouseY, mouseButton, UiEvent.CLICK)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        handle(mouseX, mouseY, state, UiEvent.RELEASE)
        super.mouseReleased(mouseX, mouseY, state)
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyListening) {
            selectedModule?.keyBind = if (keyCode == Keyboard.KEY_ESCAPE) Keyboard.KEY_NONE else keyCode
            keyListening = false
            saveConfig(modulesConfig)
            return
        }

        var typedText = false
        textFields.values.forEach {
            if (it.isFocused) {
                it.textboxKeyTyped(typedChar, keyCode)
                typedText = true
            }
        }
        if (typedText) return

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    private fun handle(mouseX: Int, mouseY: Int, mouseButton: Int, event: UiEvent) {
        if (event == UiEvent.RELEASE) {
            dragging = false
            resizing = false
            draggedFloat = null
            draggedInt = null
            colorStates.values.forEach {
                it.draggingColor = false
                it.draggingHue = false
                it.draggingAlpha = false
            }
            saveConfig(valuesConfig)
            saveConfig(modulesConfig)
        }

        if (event == UiEvent.CLICK) {
            if (mouseButton == 0 && hovered(mouseX, mouseY, x, y, w, 20f)) {
                dragging = true
                dragX = mouseX - x
                dragY = mouseY - y
            }
            if (mouseButton == 0 && hovered(mouseX, mouseY, x + w - 12f, y + h - 12f, 12f, 12f)) {
                resizing = true
            }
        }

        if (event == UiEvent.DRAW) {
            if (dragging && Mouse.isButtonDown(0)) {
                x = mouseX - dragX
                y = mouseY - dragY
            }
            if (resizing && Mouse.isButtonDown(0)) {
                w = (mouseX - x).coerceAtLeast(450f)
                h = (mouseY - y).coerceAtLeast(260f)
            }
        }

        drawFrame(event, mouseX, mouseY, mouseButton)
        updateDraggedValues(mouseX)
    }

    private fun drawFrame(event: UiEvent, mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (event == UiEvent.DRAW) {
            drawRect(x - 1f, y - 1f, w + 2f, h + 2f, Color(10, 10, 10).rgb)
            drawRect(x, y, w, h, Color(24, 24, 24).rgb)
            drawGradient(x + 1f, y + 1f, w - 2f, 18f, Color(48, 48, 48).rgb, Color(31, 31, 31).rgb)
            drawRect(x + 1f, y + 20f, w - 2f, 1f, Color(6, 6, 6).rgb)
            drawRect(x + 1f, y + 21f, w - 2f, 1f, accent.rgb)
            drawRect(x + 95f, y + 22f, 1f, h - 23f, Color(10, 10, 10).rgb)
            drawRect(x + 96f, y + 22f, 1f, h - 23f, Color(45, 45, 45).rgb)
            drawRect(x + 97f, y + 50f, w - 98f, 1f, Color(10, 10, 10).rgb)
            Fonts.font35.drawString("edneko", x + 7f, y + 7f, Color(230, 230, 230).rgb)
            drawRect(x + w - 11f, y + h - 11f, 8f, 8f, Color(70, 70, 70).rgb)
        }

        drawCategories(event, mouseX, mouseY, mouseButton)
        drawModules(event, mouseX, mouseY, mouseButton)
        drawValues(event, mouseX, mouseY, mouseButton)
    }

    private fun drawCategories(event: UiEvent, mouseX: Int, mouseY: Int, mouseButton: Int) {
        var categoryX = x + 110f
        if (event == UiEvent.DRAW) scissor(x + 98f, y + 24f, w - 100f, 26f)
        Category.entries.forEach { category ->
            val text = category.displayName
            val textWidth = Fonts.font35.getStringWidth(text).toFloat()
            val active = category == selectedCategory
            val over = hovered(mouseX, mouseY, categoryX, y + 30f, textWidth, 12f)
            if (event == UiEvent.DRAW) {
                Fonts.font35.drawString(text, categoryX, y + 31f, if (active) accent.rgb else if (over) Color.WHITE.rgb else Color(180, 180, 180).rgb)
                if (active) drawRect(categoryX, y + 44f, textWidth, 2f, accent.rgb)
            } else if (event == UiEvent.CLICK && mouseButton == 0 && over) {
                selectedCategory = category
                selectedModule = modulesIn(category).firstOrNull()
                moduleScroll = 0f
                valueScroll = 0f
                colorStates.clear()
            }
            categoryX += textWidth + 18f
        }
        if (event == UiEvent.DRAW) endScissor()
    }

    private fun drawModules(event: UiEvent, mouseX: Int, mouseY: Int, mouseButton: Int) {
        val listX = x + 1f
        val listY = y + 23f
        val listW = 94f
        val listH = h - 24f
        val modules = modulesIn(selectedCategory)
        moduleScroll = clampScroll(moduleScroll, modules.size * 16f, listH - 8f)
        if (event == UiEvent.DRAW && hovered(mouseX, mouseY, listX, listY, listW, listH)) {
            moduleScroll = clampScroll(moduleScroll + Mouse.getDWheel() / 10f, modules.size * 16f, listH - 8f)
        }
        if (event == UiEvent.DRAW) scissor(listX, listY, listW, listH)

        var moduleY = y + 30f + moduleScroll
        modules.forEach { module ->
            val visible = moduleY >= listY - 12f && moduleY <= listY + listH
            val over = visible && hovered(mouseX, mouseY, x + 7f, moduleY - 3f, 82f, 14f)
            if (event == UiEvent.DRAW && visible) {
                if (module == selectedModule) drawRect(x + 4f, moduleY - 4f, 88f, 15f, Color(38, 38, 38).rgb)
                Fonts.font35.drawString(module.name, x + 9f, moduleY, if (module.state) accent.rgb else if (over) Color.WHITE.rgb else Color(205, 205, 205).rgb)
            } else if (event == UiEvent.CLICK && over) {
                if (mouseButton == 0) module.toggle()
                if (mouseButton == 1) {
                    selectedModule = module
                    valueScroll = 0f
                    keyListening = false
                    colorStates.clear()
                }
            }
            moduleY += 16f
        }
        if (event == UiEvent.DRAW) endScissor()
    }

    private fun drawValues(event: UiEvent, mouseX: Int, mouseY: Int, mouseButton: Int) {
        val module = selectedModule ?: return
        val areaX = x + 108f
        var valueY = y + 61f
        if (event == UiEvent.DRAW) {
            Fonts.font35.drawString("${module.name}:", areaX, valueY, Color(225, 225, 225).rgb)
        }
        valueY += 17f

        val keyText = if (keyListening) "Key: ..." else "Key: ${Keyboard.getKeyName(module.keyBind) ?: "None"}"
        val keyOver = hovered(mouseX, mouseY, areaX, valueY, Fonts.font35.getStringWidth(keyText).toFloat(), 12f)
        val hideText = "Hide: ${module.isHidden}"
        val hideOver = hovered(mouseX, mouseY, areaX + 85f, valueY, Fonts.font35.getStringWidth(hideText).toFloat(), 12f)
        if (event == UiEvent.DRAW) {
            Fonts.font35.drawString(keyText, areaX, valueY, if (keyListening) accent.rgb else Color(165, 165, 165).rgb)
            Fonts.font35.drawString("Hide: ", areaX + 85f, valueY, Color(165, 165, 165).rgb)
            Fonts.font35.drawString(module.isHidden.toString(), areaX + 85f + Fonts.font35.getStringWidth("Hide: "), valueY, if (module.isHidden) Color(60, 190, 90).rgb else Color(190, 60, 60).rgb)
        } else if (event == UiEvent.CLICK && mouseButton == 0) {
            if (keyOver) keyListening = true
            if (hideOver) {
                module.isHidden = !module.isHidden
                saveConfig(modulesConfig)
            }
        }
        valueY += 22f

        val clipY = valueY - 3f
        val clipH = h - (clipY - y) - 8f
        val contentHeight = module.values.filter { it.shouldRender() }.sumOf { valueHeight(it).toDouble() }.toFloat()
        if (event == UiEvent.DRAW && hovered(mouseX, mouseY, x + 98f, clipY, w - 102f, clipH)) {
            valueScroll = clampScroll(valueScroll + Mouse.getDWheel() / 10f, contentHeight, clipH)
        }
        valueScroll = clampScroll(valueScroll, contentHeight, clipH)
        if (event == UiEvent.DRAW) scissor(x + 98f, clipY, w - 102f, clipH)
        valueY += valueScroll

        module.values.filter { it.shouldRender() }.forEach { value ->
            valueY = drawValue(value, valueY, event, mouseX, mouseY, mouseButton, clipY, clipH)
        }
        if (event == UiEvent.DRAW) endScissor()
    }

    private fun drawValue(value: Value<*>, valueY: Float, event: UiEvent, mouseX: Int, mouseY: Int, mouseButton: Int, clipY: Float, clipH: Float): Float {
        if (valueY + valueHeight(value) < clipY || valueY > clipY + clipH) return valueY + valueHeight(value)
        val labelX = x + 108f
        val label = "${value.name}: "

        when (value) {
            is BoolValue -> {
                val full = label + value.get()
                if (event == UiEvent.DRAW) {
                    Fonts.font35.drawString(label, labelX, valueY, text.rgb)
                    Fonts.font35.drawString(value.get().toString(), labelX + Fonts.font35.getStringWidth(label), valueY, if (value.get()) Color(70, 190, 100).rgb else Color(190, 70, 70).rgb)
                } else if (event == UiEvent.CLICK && mouseButton == 0 && hovered(mouseX, mouseY, labelX, valueY, Fonts.font35.getStringWidth(full).toFloat(), 12f)) {
                    value.toggle()
                }
                return valueY + 17f
            }
            is TextValue -> {
                val field = textFields.getOrPut(value) {
                    GuiTextField(0, mc.fontRendererObj, 0, 0, 120, 14).apply {
                        maxStringLength = 256
                        text = value.get()
                        enableBackgroundDrawing = false
                    }
                }
                field.xPosition = (labelX + Fonts.font35.getStringWidth(label) + 5f).toInt()
                field.yPosition = (valueY - 2f).toInt()
                field.width = 130
                if (event == UiEvent.DRAW) {
                    Fonts.font35.drawString(label, labelX, valueY, text.rgb)
                    drawRect(field.xPosition - 2f, field.yPosition - 1f, field.width + 4f, 14f, Color(38, 38, 38).rgb)
                    field.updateCursorCounter()
                    field.drawTextBox()
                    if (value.get() != field.text) value.set(field.text)
                } else if (event == UiEvent.CLICK) {
                    field.mouseClicked(mouseX, mouseY, mouseButton)
                }
                return valueY + 18f
            }
            is IntValue -> {
                drawSlider(value, valueY, value.get().toFloat(), value.minimum.toFloat(), value.maximum.toFloat(), event, mouseX, mouseY, mouseButton)
                return valueY + 18f
            }
            is FloatValue -> {
                drawSlider(value, valueY, value.get(), value.minimum, value.maximum, event, mouseX, mouseY, mouseButton)
                return valueY + 18f
            }
            is ListValue -> {
                var modeX = labelX + Fonts.font35.getStringWidth(label)
                var modeY = valueY
                if (event == UiEvent.DRAW) Fonts.font35.drawString(label, labelX, valueY, text.rgb)
                value.values.forEachIndexed { index, option ->
                    if (modeX > x + w - 70f) {
                        modeX = labelX + Fonts.font35.getStringWidth(label)
                        modeY += 14f
                    }
                    val selected = option.equals(value.get(), true)
                    if (event == UiEvent.DRAW) {
                        Fonts.font35.drawString(option, modeX, modeY, if (selected) accent.rgb else text.rgb)
                        if (index < value.values.lastIndex) Fonts.font35.drawString(",", modeX + Fonts.font35.getStringWidth(option), modeY, text.rgb)
                    } else if (event == UiEvent.CLICK && mouseButton == 0 && hovered(mouseX, mouseY, modeX, modeY, Fonts.font35.getStringWidth(option).toFloat(), 12f)) {
                        value.set(option)
                    }
                    modeX += Fonts.font35.getStringWidth("$option, ").toFloat()
                }
                return modeY + 17f
            }
            is ColorValue -> return drawColorValue(value, valueY, event, mouseX, mouseY, mouseButton)
            else -> {
                if (event == UiEvent.DRAW) Fonts.font35.drawString(label + value.toText(), labelX, valueY, text.rgb)
                return valueY + 17f
            }
        }
    }

    private fun drawSlider(value: Value<*>, valueY: Float, current: Float, min: Float, max: Float, event: UiEvent, mouseX: Int, mouseY: Int, mouseButton: Int) {
        val labelX = x + 108f
        val sliderX = labelX + Fonts.font35.getStringWidth("${value.name}: ") + 5f
        val sliderW = 112f
        if (event == UiEvent.DRAW) {
            Fonts.font35.drawString("${value.name}: ", labelX, valueY, text.rgb)
            val progress = if (max == min) 0f else ((current - min) / (max - min)).coerceIn(0f, 1f)
            drawRect(sliderX, valueY - 2f, sliderW, 10f, Color(37, 37, 37).rgb)
            drawRect(sliderX + 1f, valueY - 1f, (sliderW - 2f) * progress, 8f, accent.rgb)
            val valueText = if (current == current.roundToInt().toFloat()) current.roundToInt().toString() else "%.2f".format(current)
            Fonts.font35.drawString(valueText, sliderX + sliderW / 2f - Fonts.font35.getStringWidth(valueText) / 2f, valueY - 1f, Color.WHITE.rgb)
        } else if (event == UiEvent.CLICK && mouseButton == 0 && hovered(mouseX, mouseY, sliderX, valueY - 4f, sliderW, 14f)) {
            if (value is FloatValue) draggedFloat = value
            if (value is IntValue) draggedInt = value
            updateSlider(mouseX)
        }
    }

    private fun drawColorValue(value: ColorValue, valueY: Float, event: UiEvent, mouseX: Int, mouseY: Int, mouseButton: Int): Float {
        val labelX = x + 108f
        val state = colorStates.getOrPut(value) { ColorState() }
        val previewX = labelX + Fonts.font35.getStringWidth("${value.name}: ") + 5f
        val pickerY = valueY + 17f
        if (event == UiEvent.DRAW) {
            Fonts.font35.drawString("${value.name}: ", labelX, valueY, text.rgb)
            val selected = value.selectedColor()
            drawRect(previewX, valueY - 1f, 24f, 10f, selected.rgb)
            if (value.showPicker) {
                val hueColor = Color.getHSBColor(value.hueSliderY, 1f, 1f)
                drawRect(labelX, pickerY, 100f, 50f, hueColor.rgb)
                for (i in 0 until 100) drawRect(labelX + i, pickerY, 1f, 50f, Color(255, 255, 255, (255 * (1f - i / 99f)).roundToInt()).rgb)
                for (i in 0 until 50) drawRect(labelX, pickerY + i, 100f, 1f, Color(0, 0, 0, (255 * (i / 49f)).roundToInt()).rgb)
                for (i in 0 until 100) drawRect(labelX + i, pickerY + 55f, 1f, 5f, Color.getHSBColor(i / 99f, 1f, 1f).rgb)
                for (i in 0 until 100) drawRect(labelX + i, pickerY + 64f, 1f, 5f, Color(selected.red, selected.green, selected.blue, (255 * (i / 99f)).roundToInt()).rgb)
            }
        } else if (event == UiEvent.CLICK && mouseButton == 0) {
            if (hovered(mouseX, mouseY, previewX, valueY - 1f, 24f, 10f)) value.showPicker = !value.showPicker
            if (value.showPicker) {
                if (hovered(mouseX, mouseY, labelX, pickerY, 100f, 50f)) state.draggingColor = true
                if (hovered(mouseX, mouseY, labelX, pickerY + 55f, 100f, 5f)) state.draggingHue = true
                if (hovered(mouseX, mouseY, labelX, pickerY + 64f, 100f, 5f)) state.draggingAlpha = true
                updateColor(mouseX, mouseY, value, state, labelX, pickerY)
            }
        }
        if (event == UiEvent.DRAW) updateColor(mouseX, mouseY, value, state, labelX, pickerY)
        return valueY + if (value.showPicker) 92f else 18f
    }

    private fun updateDraggedValues(mouseX: Int) = updateSlider(mouseX)

    private fun updateSlider(mouseX: Int) {
        draggedFloat?.let {
            val sliderX = x + 108f + Fonts.font35.getStringWidth("${it.name}: ") + 5f
            val raw = ((mouseX - sliderX) / 112f).coerceIn(0f, 1f)
            it.set(it.minimum + (it.maximum - it.minimum) * raw)
        }
        draggedInt?.let {
            val sliderX = x + 108f + Fonts.font35.getStringWidth("${it.name}: ") + 5f
            val raw = ((mouseX - sliderX) / 112f).coerceIn(0f, 1f)
            it.set((it.minimum + (it.maximum - it.minimum) * raw).roundToInt())
        }
    }

    private fun updateColor(mouseX: Int, mouseY: Int, value: ColorValue, state: ColorState, pickerX: Float, pickerY: Float) {
        if (state.draggingHue && Mouse.isButtonDown(0)) value.hueSliderY = ((mouseX - pickerX) / 100f).coerceIn(0f, 1f)
        if (state.draggingAlpha && Mouse.isButtonDown(0)) value.opacitySliderY = ((mouseX - pickerX) / 100f).coerceIn(0f, 1f)
        if (state.draggingColor && Mouse.isButtonDown(0)) value.colorPickerPos = Vector2f(((mouseX - pickerX) / 100f).coerceIn(0f, 1f), ((mouseY - pickerY) / 50f).coerceIn(0f, 1f))
        if ((state.draggingColor || state.draggingHue || state.draggingAlpha) && Mouse.isButtonDown(0)) {
            val pos = value.colorPickerPos
            val color = Color(Color.HSBtoRGB(value.hueSliderY, pos.x, 1f - pos.y))
            value.set(Color(color.red, color.green, color.blue, (value.opacitySliderY * 255).roundToInt().coerceIn(0, 255)))
        }
    }

    private fun valueHeight(value: Value<*>) = when (value) {
        is ColorValue -> if (value.showPicker) 92f else 18f
        is ListValue -> {
            var modeX = x + 108f + Fonts.font35.getStringWidth("${value.name}: ")
            var rows = 1
            value.values.forEach {
                if (modeX > x + w - 70f) {
                    modeX = x + 108f + Fonts.font35.getStringWidth("${value.name}: ")
                    rows++
                }
                modeX += Fonts.font35.getStringWidth("$it, ").toFloat()
            }
            rows * 14f + 3f
        }
        else -> 18f
    }

    private fun modulesIn(category: Category) = LiquidBounce.moduleManager[category].sortedBy { it.name.lowercase() }

    private fun hovered(mouseX: Int, mouseY: Int, x: Float, y: Float, w: Float, h: Float) =
        mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h

    private fun clampScroll(scroll: Float, contentHeight: Float, visibleHeight: Float): Float {
        val min = (visibleHeight - contentHeight).coerceAtMost(0f)
        return scroll.coerceIn(min, 0f)
    }

    private fun drawRect(x: Float, y: Float, w: Float, h: Float, color: Int) =
        RenderUtils.drawRect(x, y, x + w, y + h, color)

    private fun drawGradient(x: Float, y: Float, w: Float, h: Float, top: Int, bottom: Int) {
        RenderUtils.drawGradientSideways(x.toDouble(), y.toDouble(), (x + w).toDouble(), (y + h).toDouble(), top, bottom)
    }

    private fun scissor(x: Float, y: Float, w: Float, h: Float) {
        val sr = ScaledResolution(mc)
        val factor = sr.scaleFactor
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor((x * factor).toInt(), ((sr.scaledHeight - y - h) * factor).toInt(), (w * factor).toInt(), (h * factor).toInt())
    }

    private fun endScissor() = GL11.glDisable(GL11.GL_SCISSOR_TEST)

    private companion object {
        val accent = Color(81, 149, 219)
        val text = Color(205, 205, 205)
    }
}
