/*
 * edneko Hacked Client
 * A free open source mixin-based injection hacked client built on Liquidbounce legacy codebase.
 * https://github.com/lmx0721/edneko
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.neverlose

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

class NeverloseScreen : GuiScreen() {

    private val panels = Category.entries.associateWith { category ->
        LiquidBounce.moduleManager[category].sortedBy { it.name.lowercase() }
    }
    private var selectedCategory = Category.COMBAT
    private var selectedModule: Module? = panels[selectedCategory]?.firstOrNull()
    private var search = ""
    private var dragging = false
    private var dragX = 0
    private var dragY = 0
    private var posX = 40
    private var posY = 40
    private var moduleScroll = 0
    private var valueScroll = 0
    private var modulesConfigDirty = false
    private var valuesConfigDirty = false
    private var focusedTextValue: TextValue? = null
    private var focusedTextBuffer = ""
    private var focusedTextSelected = false
    private var searchFocused = false

    private val guiWidth = 680
    private val guiHeight = 430
    private val sidebarWidth = 150
    private val headerHeight = 48

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        posX = ((width - guiWidth) / 2).coerceAtLeast(20)
        posY = ((height - guiHeight) / 2).coerceAtLeast(20)
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        commitFocusedTextValue()
        saveDirtyConfigs()
    }

    override fun doesGuiPauseGame() = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()

        if (dragging) {
            posX = mouseX + dragX
            posY = mouseY + dragY
        }

        drawShell()
        drawSidebar(mouseX, mouseY)
        drawModules(mouseX, mouseY)
        drawValues(mouseX, mouseY)
        drawUserFooter()

        handleWheel(mouseX, mouseY)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawShell() {
        RoundedUtil.drawRound(posX.toFloat(), posY.toFloat(), guiWidth.toFloat(), guiHeight.toFloat(), 6f, BG_OVERLAY)
        RoundedUtil.drawRound((posX + sidebarWidth).toFloat(), posY.toFloat(), (guiWidth - sidebarWidth).toFloat(), guiHeight.toFloat(), 6f, BG)
        RoundedUtil.drawRound((posX + sidebarWidth).toFloat(), posY.toFloat(), (guiWidth - sidebarWidth).toFloat(), headerHeight.toFloat(), 6f, TOP)
        RenderUtils.drawRect((posX + sidebarWidth).toFloat(), posY.toFloat(), (posX + sidebarWidth + 4).toFloat(), (posY + guiHeight).toFloat(), BG)
        RenderUtils.drawRect((posX + sidebarWidth).toFloat(), (posY + headerHeight - 1).toFloat(), (posX + guiWidth).toFloat(), (posY + headerHeight).toFloat(), LINE)
        RenderUtils.drawRect((posX + sidebarWidth - 1).toFloat(), posY.toFloat(), (posX + sidebarWidth).toFloat(), (posY + guiHeight).toFloat(), LINE)

        Fonts.font40.drawStringWithShadow("edneko", (posX + 18).toFloat(), (posY + 14).toFloat(), TEXT.rgb)
        Fonts.font35.drawString("Neverlose", (posX + sidebarWidth + 16).toFloat(), (posY + 16).toFloat(), TEXT.rgb)

        val searchX = posX + guiWidth - 150
        val searchY = posY + 14
        RoundedUtil.drawRound(searchX.toFloat(), searchY.toFloat(), 128f, 22f, 4f, SEARCH_BG)
        Fonts.font30.drawString(if (search.isBlank()) "Search" else search, (searchX + 8).toFloat(), (searchY + 7).toFloat(), if (search.isBlank()) MUTED.rgb else TEXT.rgb)
        if (searchFocused) {
            RenderUtils.drawRect(searchX.toFloat(), (searchY + 21).toFloat(), (searchX + 128).toFloat(), (searchY + 22).toFloat(), ACCENT.rgb)
        }
    }

    private fun drawSidebar(mouseX: Int, mouseY: Int) {
        val groups = listOf(
            "Combat" to listOf(Category.COMBAT, Category.PLAYER),
            "Common" to listOf(Category.MOVEMENT, Category.WORLD, Category.MISC, Category.EXPLOIT),
            "Visuals" to listOf(Category.RENDER, Category.FUN),
            "Presets" to listOf(Category.CLIENT)
        )

        var y = posY + 42
        groups.forEach { (title, categories) ->
            Fonts.font30.drawString(title, (posX + 14).toFloat(), y.toFloat(), MUTED.rgb)
            y += 18

            categories.forEach { category ->
                val hovered = isHovered(posX + 8, y - 4, 120, 19, mouseX, mouseY)
                if (category == selectedCategory || hovered) {
                    RoundedUtil.drawRound((posX + 8).toFloat(), (y - 4).toFloat(), 120f, 19f, 5f, if (category == selectedCategory) SELECTED else HOVER)
                }

                Fonts.font35.drawString(categoryIcon(category), (posX + 14).toFloat(), (y + 1).toFloat(), ACCENT.rgb)
                Fonts.font30.drawString(category.displayName, (posX + 34).toFloat(), y.toFloat(), TEXT.rgb)
                y += 24
            }
            y += 10
        }
    }

    private fun drawModules(mouseX: Int, mouseY: Int) {
        val listX = posX + sidebarWidth + 12
        val listY = posY + headerHeight + 12
        val listWidth = 190
        val listHeight = guiHeight - headerHeight - 24
        val modules = filteredModules()

        RoundedUtil.drawRound(listX.toFloat(), listY.toFloat(), listWidth.toFloat(), listHeight.toFloat(), 4f, CARD)
        Fonts.font30.drawString("Modules", (listX + 10).toFloat(), (listY + 8).toFloat(), MUTED.rgb)

        var y = listY + 28 + moduleScroll
        modules.forEach { module ->
            if (y > listY + 20 && y < listY + listHeight - 8) {
                val selected = module == selectedModule
                val hovered = isHovered(listX + 8, y, listWidth - 16, 22, mouseX, mouseY)
                if (selected || hovered) {
                    RoundedUtil.drawRound((listX + 8).toFloat(), y.toFloat(), (listWidth - 16).toFloat(), 22f, 4f, if (selected) SELECTED else HOVER)
                }

                val dotColor = if (module.state) ACCENT else MUTED
                RoundedUtil.drawRound((listX + 14).toFloat(), (y + 8).toFloat(), 6f, 6f, 3f, dotColor)
                Fonts.font30.drawString(module.name, (listX + 28).toFloat(), (y + 7).toFloat(), if (module.state) TEXT.rgb else DISABLED.rgb)
            }
            y += 26
        }
    }

    private fun drawValues(mouseX: Int, mouseY: Int) {
        val valueX = posX + sidebarWidth + 218
        val valueY = posY + headerHeight + 12
        val valueWidth = guiWidth - sidebarWidth - 230
        val valueHeight = guiHeight - headerHeight - 24
        val module = selectedModule

        RoundedUtil.drawRound(valueX.toFloat(), valueY.toFloat(), valueWidth.toFloat(), valueHeight.toFloat(), 4f, CARD)

        if (module == null) {
            Fonts.font35.drawCenteredString("No module", (valueX + valueWidth / 2).toFloat(), (valueY + 120).toFloat(), MUTED.rgb)
            return
        }

        Fonts.font35.drawString(module.name, (valueX + 12).toFloat(), (valueY + 10).toFloat(), TEXT.rgb)
        val stateText = if (module.state) "Enabled" else "Disabled"
        Fonts.font30.drawString(stateText, (valueX + valueWidth - Fonts.font30.getStringWidth(stateText) - 12).toFloat(), (valueY + 13).toFloat(), if (module.state) ACCENT.rgb else MUTED.rgb)
        RenderUtils.drawRect((valueX + 12).toFloat(), (valueY + 34).toFloat(), (valueX + valueWidth - 12).toFloat(), (valueY + 35).toFloat(), LINE)

        val values = module.values.filter { it.shouldRender() }
        if (values.isEmpty()) {
            Fonts.font30.drawString("No settings", (valueX + 12).toFloat(), (valueY + 50).toFloat(), MUTED.rgb)
            return
        }

        var y = valueY + 48 + valueScroll
        values.forEach { value ->
            if (y > valueY + 34 && y < valueY + valueHeight - 12) {
                drawValue(value, valueX + 12, y, valueWidth - 24, mouseX, mouseY)
            }
            y += valueHeight(value)
        }
    }

    private fun drawValue(value: Value<*>, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int) {
        val hovered = isHovered(x, y, width, 24, mouseX, mouseY)
        if (hovered) {
            RoundedUtil.drawRound(x.toFloat(), y.toFloat(), width.toFloat(), 24f, 4f, HOVER)
        }

        val maxNameWidth = width - 160
        val displayName = fitText(value.name, maxNameWidth)
        Fonts.font30.drawString(displayName, (x + 6).toFloat(), (y + 8).toFloat(), TEXT.rgb)

        when (value) {
            is BoolValue -> drawBooleanValue(value, x, y, width)
            is IntValue -> drawNumberValue(value.get().toFloat(), value.minimum.toFloat(), value.maximum.toFloat(), x, y, width, value.get().toString())
            is FloatValue -> drawNumberValue(value.get(), value.minimum, value.maximum, x, y, width, "%.2f".format(value.get()))
            is ListValue -> drawChoiceValue(value, x, y, width)
            is TextValue -> drawTextValue(value, x, y, width)
            is ColorValue -> drawColorValue(value, x, y, width)
            else -> Fonts.font30.drawString(fitText(value.toText(), 120), (x + width - Fonts.font30.getStringWidth(fitText(value.toText(), 120)) - 6).toFloat(), (y + 8).toFloat(), MUTED.rgb)
        }
    }

    private fun drawBooleanValue(value: BoolValue, x: Int, y: Int, width: Int) {
        val enabled = value.get()
        val bg = if (enabled) BOOL_ON_BG else BOOL_OFF_BG
        val circle = if (enabled) ACCENT else MUTED
        val switchX = x + width - 38
        RoundedUtil.drawRound(switchX.toFloat(), (y + 7).toFloat(), 28f, 12f, 6f, bg)
        RoundedUtil.drawRound((switchX + if (enabled) 16 else 2).toFloat(), (y + 9).toFloat(), 8f, 8f, 4f, circle)
    }

    private fun drawNumberValue(current: Float, min: Float, max: Float, x: Int, y: Int, width: Int, text: String) {
        val textWidth = Fonts.font30.getStringWidth(text)
        val sliderX = x + width - 152
        val sliderY = y + 17
        val sliderWidth = 96
        val progress = if (max == min) 0f else ((current - min) / (max - min)).coerceIn(0f, 1f)
        Fonts.font30.drawString(text, (x + width - textWidth - 6).toFloat(), (y + 8).toFloat(), MUTED.rgb)
        RoundedUtil.drawRound(sliderX.toFloat(), sliderY.toFloat(), sliderWidth.toFloat(), 4f, 2f, SLIDER_BG)
        RoundedUtil.drawRound(sliderX.toFloat(), sliderY.toFloat(), sliderWidth * progress, 4f, 2f, ACCENT)
        RoundedUtil.drawRound((sliderX + sliderWidth * progress - 3).toFloat(), (sliderY - 2).toFloat(), 8f, 8f, 4f, ACCENT)
    }

    private fun drawChoiceValue(value: ListValue, x: Int, y: Int, width: Int) {
        val text = fitText(value.get(), 100)
        Fonts.font30.drawString(text, (x + width - Fonts.font30.getStringWidth(text) - 6).toFloat(), (y + 8).toFloat(), ACCENT.rgb)
    }

    private fun drawTextValue(value: TextValue, x: Int, y: Int, width: Int) {
        val editing = focusedTextValue === value
        val rawText = if (editing) focusedTextBuffer else value.get()
        val text = fitText(rawText.ifEmpty { if (editing) "" else "..." }, 100)
        val textWidth = Fonts.font30.getStringWidth(text)
        val inputX = x + width - 116

        RoundedUtil.drawRound(inputX.toFloat(), (y + 4).toFloat(), 110f, 18f, 3f, if (editing) SEARCH_BG else CARD)
        if (editing && focusedTextSelected && text.isNotEmpty()) {
            RenderUtils.drawRect((x + width - textWidth - 11).toFloat(), (y + 6).toFloat(), (x + width - 9).toFloat(), (y + 20).toFloat(), SELECTED.rgb)
        }
        Fonts.font30.drawString(text, (x + width - textWidth - 10).toFloat(), (y + 8).toFloat(), if (editing) TEXT.rgb else MUTED.rgb)

        if (editing && !focusedTextSelected && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            val caretX = (x + width - 10).coerceAtMost(inputX + 104)
            RenderUtils.drawRect(caretX.toFloat(), (y + 7).toFloat(), (caretX + 1).toFloat(), (y + 18).toFloat(), ACCENT.rgb)
        }
    }

    private fun drawColorValue(value: ColorValue, x: Int, y: Int, width: Int) {
        val color = value.selectedColor()
        val previewX = x + width - 24
        val hueX = x + width - 126
        val hueY = y + 21
        val hueWidth = 72
        val hex = if (value.rainbow) "Rainbow" else "#%02X%02X%02X".format(color.red, color.green, color.blue)
        val displayText = fitText(hex, 64)

        RoundedUtil.drawRound(previewX.toFloat(), (y + 6).toFloat(), 16f, 16f, 4f, Color(color.red, color.green, color.blue, color.alpha))
        Fonts.font30.drawString(displayText, (previewX - Fonts.font30.getStringWidth(displayText) - 8).toFloat(), (y + 8).toFloat(), if (value.rainbow) ACCENT.rgb else MUTED.rgb)
        RenderUtils.drawRect((hueX - 1).toFloat(), (hueY - 1).toFloat(), (hueX + hueWidth + 1).toFloat(), (hueY + 4).toFloat(), LINE)

        for (i in 0 until hueWidth) {
            val hueColor = Color(Color.HSBtoRGB(i / hueWidth.toFloat(), 1f, 1f))
            RenderUtils.drawRect((hueX + i).toFloat(), hueY.toFloat(), (hueX + i + 1).toFloat(), (hueY + 3).toFloat(), hueColor.rgb)
        }

        val markerX = hueX + (value.hueSliderY.coerceIn(0f, 1f) * hueWidth).roundToInt()
        RenderUtils.drawRect((markerX - 1).toFloat(), (hueY - 2).toFloat(), (markerX + 1).toFloat(), (hueY + 5).toFloat(), TEXT.rgb)
    }

    private fun drawUserFooter() {
        RenderUtils.drawRect(posX.toFloat(), (posY + guiHeight - 36).toFloat(), (posX + sidebarWidth - 2).toFloat(), (posY + guiHeight - 35).toFloat(), LINE)
        val playerName = mc.thePlayer?.name ?: "Offline"
        Fonts.font30.drawString(playerName, (posX + 14).toFloat(), (posY + guiHeight - 24).toFloat(), TEXT.rgb)
        Fonts.font30.drawString("Lifetime", (posX + 14).toFloat(), (posY + guiHeight - 13).toFloat(), ACCENT.rgb)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val clickedTextValue = textValueAt(mouseX, mouseY)
        if (clickedTextValue == null) {
            commitFocusedTextValue()
        }

        searchFocused = isHovered(posX + guiWidth - 150, posY + 14, 128, 22, mouseX, mouseY)

        if (mouseButton == 0 && isHovered(posX, posY, sidebarWidth, 42, mouseX, mouseY)) {
            dragging = true
            dragX = posX - mouseX
            dragY = posY - mouseY
        }

        handleSidebarClick(mouseX, mouseY, mouseButton)
        handleModuleClick(mouseX, mouseY, mouseButton)
        handleValueClick(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun handleSidebarClick(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return

        val groups = listOf(
            listOf(Category.COMBAT, Category.PLAYER),
            listOf(Category.MOVEMENT, Category.WORLD, Category.MISC, Category.EXPLOIT),
            listOf(Category.RENDER, Category.FUN),
            listOf(Category.CLIENT)
        )

        var y = posY + 60
        groups.forEach { categories ->
            categories.forEach { category ->
                if (isHovered(posX + 8, y - 4, 120, 19, mouseX, mouseY)) {
                    selectedCategory = category
                    selectedModule = filteredModules().firstOrNull()
                    moduleScroll = 0
                    valueScroll = 0
                    return
                }
                y += 24
            }
            y += 28
        }
    }

    private fun handleModuleClick(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val listX = posX + sidebarWidth + 12
        val listY = posY + headerHeight + 12
        val modules = filteredModules()
        var y = listY + 28 + moduleScroll

        modules.forEach { module ->
            if (isHovered(listX + 8, y, 174, 22, mouseX, mouseY)) {
                when (mouseButton) {
                    0 -> {
                        selectedModule = module
                        valueScroll = 0
                    }
                    1 -> {
                        module.toggle()
                        modulesConfigDirty = true
                    }
                }
                return
            }
            y += 26
        }
    }

    private fun handleValueClick(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val module = selectedModule ?: return
        val valueX = posX + sidebarWidth + 218
        val valueY = posY + headerHeight + 12
        val valueWidth = guiWidth - sidebarWidth - 230
        var y = valueY + 48 + valueScroll

        module.values.filter { it.shouldRender() }.forEach { value ->
            if (isHovered(valueX + 12, y, valueWidth - 24, 24, mouseX, mouseY)) {
                when (value) {
                    is BoolValue -> if (mouseButton == 0) {
                        value.set(!value.get(), false)
                        valuesConfigDirty = true
                    }
                    is IntValue -> if (mouseButton == 0) {
                        setIntValueByMouse(value, mouseX, valueX + valueWidth - 140, 96)
                        valuesConfigDirty = true
                    }
                    is FloatValue -> if (mouseButton == 0) {
                        setFloatValueByMouse(value, mouseX, valueX + valueWidth - 140, 96)
                        valuesConfigDirty = true
                    }
                    is ListValue -> {
                        if (mouseButton == 0) {
                            nextChoice(value)
                            valuesConfigDirty = true
                        } else if (mouseButton == 1) {
                            previousChoice(value)
                            valuesConfigDirty = true
                        }
                    }
                    is TextValue -> {
                        if (mouseButton == 0) {
                            focusTextValue(value)
                        } else if (mouseButton == 1) {
                            focusTextValue(value)
                            focusedTextBuffer = ""
                            focusedTextSelected = false
                            valuesConfigDirty = true
                        }
                    }
                    is ColorValue -> {
                        if (mouseButton == 0) {
                            setColorValueByMouse(value, mouseX, valueX + valueWidth - 138, 72)
                            valuesConfigDirty = true
                        } else if (mouseButton == 1) {
                            value.rainbow = !value.rainbow
                            value.set(value.selectedColor(), false)
                            valuesConfigDirty = true
                        }
                    }
                    else -> Unit
                }
                return
            }
            y += valueHeight(value)
        }
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (focusedTextValue != null) {
            handleFocusedTextInput(typedChar, keyCode)
            return
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            return
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (search.isNotEmpty()) search = search.dropLast(1)
            return
        }

        if (keyCode == Keyboard.KEY_RETURN) {
            search = ""
            return
        }

        if (!Character.isISOControl(typedChar)) {
            search += typedChar
            searchFocused = true
            selectedModule = filteredModules().firstOrNull() ?: selectedModule
            moduleScroll = 0
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (state == 0) dragging = false
        saveDirtyConfigs()
        super.mouseReleased(mouseX, mouseY, state)
    }

    private fun saveDirtyConfigs() {
        if (modulesConfigDirty) {
            saveConfig(modulesConfig)
            modulesConfigDirty = false
        }

        if (valuesConfigDirty) {
            saveConfig(valuesConfig)
            valuesConfigDirty = false
        }
    }

    private fun handleWheel(mouseX: Int, mouseY: Int) {
        val wheel = Mouse.getDWheel()
        if (wheel == 0) return

        if (isHovered(posX + sidebarWidth + 12, posY + headerHeight + 12, 190, guiHeight - headerHeight - 24, mouseX, mouseY)) {
            moduleScroll = (moduleScroll + if (wheel > 0) 18 else -18).coerceIn(-maxModuleScroll(), 0)
        }

        if (isHovered(posX + sidebarWidth + 218, posY + headerHeight + 12, guiWidth - sidebarWidth - 230, guiHeight - headerHeight - 24, mouseX, mouseY)) {
            valueScroll = (valueScroll + if (wheel > 0) 18 else -18).coerceIn(-maxValueScroll(), 0)
        }
    }

    private fun filteredModules(): List<Module> {
        val modules = panels[selectedCategory].orEmpty()
        if (search.isBlank()) return modules
        return modules.filter { module ->
            module.name.contains(search, ignoreCase = true) || module.spacedName.contains(search, ignoreCase = true)
        }
    }

    private fun valueHeight(value: Value<*>) = when (value) {
        is IntValue, is FloatValue, is ColorValue -> 30
        else -> 26
    }

    private fun maxModuleScroll(): Int {
        val visibleHeight = guiHeight - headerHeight - 52
        val contentHeight = filteredModules().size * 26
        return (contentHeight - visibleHeight).coerceAtLeast(0)
    }

    private fun maxValueScroll(): Int {
        val visibleHeight = guiHeight - headerHeight - 72
        val contentHeight = selectedModule?.values
            ?.filter { it.shouldRender() }
            ?.sumOf { valueHeight(it) }
            ?: 0
        return (contentHeight - visibleHeight).coerceAtLeast(0)
    }

    private fun fitText(text: String, maxWidth: Int): String {
        if (Fonts.font30.getStringWidth(text) <= maxWidth) return text

        var clipped = text
        while (clipped.isNotEmpty() && Fonts.font30.getStringWidth("$clipped...") > maxWidth) {
            clipped = clipped.dropLast(1)
        }

        return if (clipped.isEmpty()) "..." else "$clipped..."
    }

    private fun setIntValueByMouse(value: IntValue, mouseX: Int, sliderX: Int, sliderWidth: Int) {
        val progress = ((mouseX - sliderX).toFloat() / sliderWidth).coerceIn(0f, 1f)
        value.set((value.minimum + (value.maximum - value.minimum) * progress).roundToInt(), false)
    }

    private fun setFloatValueByMouse(value: FloatValue, mouseX: Int, sliderX: Int, sliderWidth: Int) {
        val progress = ((mouseX - sliderX).toFloat() / sliderWidth).coerceIn(0f, 1f)
        value.set(value.minimum + (value.maximum - value.minimum) * progress, false)
    }

    private fun setColorValueByMouse(value: ColorValue, mouseX: Int, hueX: Int, hueWidth: Int) {
        val hue = ((mouseX - hueX).toFloat() / hueWidth).coerceIn(0f, 1f)
        val saturation = value.colorPickerPos.x.coerceIn(0f, 1f).takeIf { it > 0f } ?: 1f
        val brightness = (1f - value.colorPickerPos.y).coerceIn(0f, 1f).takeIf { it > 0f } ?: 1f
        val alpha = (value.opacitySliderY.coerceIn(0f, 1f) * 255).roundToInt().coerceIn(0, 255)
        val color = Color(Color.HSBtoRGB(hue, saturation, brightness), true)

        value.rainbow = false
        value.hueSliderY = hue
        value.set(Color(color.red, color.green, color.blue, alpha), false)
    }

    private fun nextChoice(value: ListValue) {
        val index = value.values.indexOfFirst { it.equals(value.get(), ignoreCase = true) }
        value.set(value.values[(index + 1).floorMod(value.values.size)], false)
    }

    private fun previousChoice(value: ListValue) {
        val index = value.values.indexOfFirst { it.equals(value.get(), ignoreCase = true) }
        value.set(value.values[(index - 1).floorMod(value.values.size)], false)
    }

    private fun focusTextValue(value: TextValue) {
        if (focusedTextValue !== value) {
            commitFocusedTextValue()
            focusedTextValue = value
            focusedTextBuffer = value.get()
        }
        focusedTextSelected = false
        searchFocused = false
    }

    private fun commitFocusedTextValue() {
        val value = focusedTextValue ?: return
        if (value.set(focusedTextBuffer, false)) {
            valuesConfigDirty = true
        }
        focusedTextValue = null
        focusedTextBuffer = ""
        focusedTextSelected = false
    }

    private fun handleFocusedTextInput(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE, Keyboard.KEY_RETURN -> {
                commitFocusedTextValue()
                saveDirtyConfigs()
                return
            }

            Keyboard.KEY_BACK -> {
                removeTextInputSelectionOrLastCharacter()
                return
            }

            Keyboard.KEY_DELETE -> {
                if (focusedTextSelected || focusedTextBuffer.isNotEmpty()) {
                    focusedTextBuffer = ""
                    focusedTextSelected = false
                    valuesConfigDirty = true
                }
                return
            }
        }

        if (isCtrlKeyDown()) {
            when (keyCode) {
                Keyboard.KEY_A -> {
                    focusedTextSelected = focusedTextBuffer.isNotEmpty()
                }

                Keyboard.KEY_V -> {
                    val clipboard = getClipboardString() ?: ""
                    if (clipboard.isNotEmpty()) {
                        replaceOrAppendFocusedText(clipboard.filterNot { Character.isISOControl(it) })
                        valuesConfigDirty = true
                    }
                }
            }
            return
        }

        if (!Character.isISOControl(typedChar)) {
            replaceOrAppendFocusedText(typedChar.toString())
            valuesConfigDirty = true
        }
    }

    private fun removeTextInputSelectionOrLastCharacter() {
        if (focusedTextSelected) {
            focusedTextBuffer = ""
            focusedTextSelected = false
            valuesConfigDirty = true
            return
        }

        if (focusedTextBuffer.isNotEmpty()) {
            focusedTextBuffer = focusedTextBuffer.dropLast(1)
            valuesConfigDirty = true
        }
    }

    private fun replaceOrAppendFocusedText(text: String) {
        focusedTextBuffer = if (focusedTextSelected) text else focusedTextBuffer + text
        focusedTextSelected = false
    }

    private fun textValueAt(mouseX: Int, mouseY: Int): TextValue? {
        val module = selectedModule ?: return null
        val valueX = posX + sidebarWidth + 218
        val valueY = posY + headerHeight + 12
        val valueWidth = guiWidth - sidebarWidth - 230
        var y = valueY + 48 + valueScroll

        module.values.filter { it.shouldRender() }.forEach { value ->
            if (value is TextValue && isHovered(valueX + 12, y, valueWidth - 24, 24, mouseX, mouseY)) {
                return value
            }
            y += valueHeight(value)
        }

        return null
    }

    private fun Int.floorMod(mod: Int) = ((this % mod) + mod) % mod

    private fun categoryIcon(category: Category) = when (category) {
        Category.COMBAT -> "⚔"
        Category.PLAYER -> "●"
        Category.MOVEMENT -> "➜"
        Category.RENDER -> "◈"
        Category.WORLD -> "◆"
        Category.MISC -> "◇"
        Category.EXPLOIT -> "◎"
        Category.FUN -> "★"
        Category.CLIENT -> "⚙"
        Category.RISE -> "R"
    }

    private fun isHovered(x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }

    private companion object {
        val BG = Color(0x000C18)
        val BG_OVERLAY = Color(0xDA081222.toInt(), true)
        val TOP = Color(0x080D13)
        val CARD = Color(0x07111D)
        val SEARCH_BG = Color(0x000314)
        val LINE = Color(0x131C29)
        val SELECTED = Color(0x003454)
        val HOVER = Color(0x182637)
        val ACCENT = Color(0x00BBFF)
        val TEXT = Color(0xF4F7FB)
        val MUTED = Color(0x7A899A)
        val DISABLED = Color(0x4A5260)
        val BOOL_ON_BG = Color(0x00173A)
        val BOOL_OFF_BG = Color(0x000314)
        val SLIDER_BG = Color(0x000F25)
    }
}