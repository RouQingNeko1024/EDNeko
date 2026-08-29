/*
 * edneko Hacked Client
 * A free open source mixin-based injection hacked client built on Liquidbounce legacy codebase.
 * https://github.com/lmx0721/edneko
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.rise

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
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt

class RiseClickGui : GuiScreen() {

    private var x = -1f
    private var y = -1f
    private val w = 400f
    private val h = 300f
    private val sidebarW = 100f
    private val moduleW = 283f
    private val moduleBaseH = 38f

    private var dragging = false
    private var dragX = 0f
    private var dragY = 0f
    private var selectedCategory = Category.COMBAT
    private var moduleScroll = 0f
    private var search = ""
    private var searchFocused = false
    private val expandedModules = HashSet<Module>()
    private var focusedText: TextValue? = null
    private var textBuffer = ""
    private var bindingModule: Module? = null
    private var draggingNumber: Value<*>? = null
    private var valuesDirty = false
    private var modulesDirty = false
    private var openProgress = 0f
    private var sidebarSelectorY = 0f
    private val expansionAnimations = HashMap<Module, Float>()

    private val background = Color(23, 26, 33, 254)
    private val sidebar = Color(18, 20, 25, 255)
    private val overlay = Color(0, 0, 0, 50)
    private val overlayHover = Color(255, 255, 255, 20)
    private val text = Color(235, 238, 245)
    private val muted = Color(255, 255, 255, 130)
    private val accent = Color(93, 149, 255)

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        if (x < 0f || y < 0f || x + w > width || y + h > height) {
            x = width / 2f - w / 2f
            y = height / 2f - h / 2f
        }
        openProgress = 0f
        sidebarSelectorY = selectedCategoryY()
        super.initGui()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        flushConfigs()
        super.onGuiClosed()
    }

    override fun doesGuiPauseGame() = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (dragging) {
            x = mouseX + dragX
            y = mouseY + dragY
        }
        if (!Mouse.isButtonDown(0)) {
            draggingNumber = null
        }

        handleWheel(mouseX, mouseY)
        openProgress = animate(openProgress, 1f, 0.18f)
        val scale = 0.92f + 0.08f * easeOut(openProgress)

        GL11.glPushMatrix()
        GL11.glTranslatef(x + w / 2f, y + h / 2f, 0f)
        GL11.glScalef(scale, scale, 1f)
        GL11.glTranslatef(-(x + w / 2f), -(y + h / 2f), 0f)
        drawShadow()
        RoundedUtil.drawRound(x, y, w, h, 12f, background)
        startScissor(x + 1f, y + 1f, w - 2f, h - 2f)
        drawSidebar(mouseX, mouseY)
        drawContent(mouseX, mouseY)
        endScissor()
        GL11.glPopMatrix()

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (bindingModule != null) {
            bindingModule = null
            return
        }

        if (isHovered(x, y, w, 16f, mouseX, mouseY) && mouseButton == 0 && focusedText == null) {
            dragging = true
            dragX = x - mouseX
            dragY = y - mouseY
            return
        }

        if (!isHovered(x, y, w, h, mouseX, mouseY)) {
            focusedText = null
            searchFocused = false
            return
        }

        clickSidebar(mouseX, mouseY, mouseButton)
        clickSearch(mouseX, mouseY, mouseButton)
        clickModules(mouseX, mouseY, mouseButton)

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        dragging = false
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
            handleTextInput(value, typedChar, keyCode)
            return
        }

        if (searchFocused) {
            when (keyCode) {
                Keyboard.KEY_ESCAPE -> {
                    searchFocused = false
                    return
                }
                Keyboard.KEY_BACK -> if (search.isNotEmpty()) {
                    search = search.dropLast(1)
                    moduleScroll = 0f
                    return
                }
                Keyboard.KEY_DELETE -> {
                    search = ""
                    moduleScroll = 0f
                    return
                }
            }
            if (!Character.isISOControl(typedChar)) {
                search += typedChar
                moduleScroll = 0f
                return
            }
        }

        if (!Character.isISOControl(typedChar)) {
            searchFocused = true
            search += typedChar
            moduleScroll = 0f
            return
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    private fun drawShadow() {
        for (i in 0..5) {
            RoundedUtil.drawRound(x - i, y - i, w + i * 2f, h + i * 2f, 12f + i, Color(0, 0, 0, 18 - i * 2))
        }
    }

    private fun drawSidebar(mouseX: Int, mouseY: Int) {
        RoundedUtil.drawRound(x, y, sidebarW, h, 12f, sidebar)
        for (i in 0..7) {
            val radius = i * 42f
            drawCircle(x + sidebarW - radius / 2f, y + h / 2f - radius / 2f, radius, Color(accent.red, accent.green, accent.blue, 10))
        }

        Fonts.fontRise50.drawString("Rise", x + 14f, y + 13f, text.rgb)

        sidebarSelectorY = animate(sidebarSelectorY, selectedCategoryY(), 0.22f)
        val selectedLabel = selectedCategory.displayName
        val selectedPillW = (Fonts.fontRise35.getStringWidth(selectedLabel) + 30f).coerceAtMost(sidebarW - 16f)
        RoundedUtil.drawRound(x + 9f, sidebarSelectorY - 4f, selectedPillW, 17f, 5f, Color(accent.red, accent.green, accent.blue, 105))

        var cy = y + 50f
        Category.entries.forEach { category ->
            val selected = category == selectedCategory
            val hovered = isHovered(x + 10f, cy - 2f, sidebarW - 18f, 18f, mouseX, mouseY)
            val label = category.displayName
            val icon = category.riseIcon()
            val labelX = x + 33f
            if (hovered && !selected) {
                val pillW = Fonts.fontRise35.getStringWidth(label) + 30f
                RoundedUtil.drawRound(x + 9f, cy - 4f, pillW.coerceAtMost(sidebarW - 16f), 17f, 5f, Color(255, 255, 255, 18))
            }
            Fonts.fontRiseIcon35.drawString(icon, x + 17f, cy + 1f, if (selected) Color.WHITE.rgb else Color(255, 255, 255, 200).rgb)
            Fonts.fontRise35.drawString(label, labelX, cy + 1f, if (selected) Color.WHITE.rgb else Color(255, 255, 255, 200).rgb)
            cy += 21f
        }
    }

    private fun drawContent(mouseX: Int, mouseY: Int) {
        val contentX = x + sidebarW
        val contentW = w - sidebarW
        val searchX = contentX + 13f
        val searchY = y + 11f
        RoundedUtil.drawRound(searchX, searchY, contentW - 27f, 22f, 6f, Color(12, 15, 23, 220))
        Fonts.fontRise35.drawString(if (search.isBlank()) "Search" else search, searchX + 8f, searchY + 7f, if (search.isBlank()) muted.rgb else text.rgb)
        if (searchFocused) {
            RenderUtils.drawRect(searchX + 7f, searchY + 20f, searchX + contentW - 35f, searchY + 21f, accent.rgb)
        }

        startScissor(contentX + 7f, y + 42f, contentW - 13f, h - 49f)
        var my = y + 48f + moduleScroll
        filteredModules().forEach { module ->
            val cardH = moduleHeight(module)
            drawModule(contentX + 8f, my, module, mouseX, mouseY)
            my += cardH + 7f
        }
        endScissor()

        drawScrollbar(filteredHeight(), contentX + contentW - 5f, y + 42f, h - 50f)
    }

    private fun drawModule(mx: Float, my: Float, module: Module, mouseX: Int, mouseY: Int) {
        val expanded = module in expandedModules
        val expansion = updateExpansion(module)
        val cardH = moduleHeight(module)
        if (my + cardH < y + 42f || my > y + h - 7f) return

        val hovered = isHovered(mx, my, moduleW, moduleBaseH, mouseX, mouseY)
        RoundedUtil.drawRound(mx, my, moduleW, cardH, 6f, if (hovered) overlayHover else overlay)
        Fonts.fontRise40.drawString(if (bindingModule == module) "Press key..." else module.name, mx + 8f, my + 8f, if (module.state) accent.rgb else text.rgb)
        Fonts.fontRise35.drawString(module.category.displayName, mx + 8f, my + 25f, Color(255, 255, 255, 70).rgb)
        if (module.values.any { it.shouldRender() }) {
            val symbol = if (expanded) "-" else "+"
            Fonts.fontRise35.drawString(symbol, mx + moduleW - 15f, my + 13f + (1f - expansion) * 2f, muted.rgb)
        }

        if (expansion <= 0.02f) return

        var vy = my + moduleBaseH + 2f
        module.values.filter { it.shouldRender() }.forEach { value ->
            if (vy + valueHeight(value) <= my + cardH) {
                drawValue(mx + 8f, vy, moduleW - 16f, value, mouseX, mouseY)
            }
            vy += valueHeight(value)
        }
    }

    private fun drawValue(vx: Float, vy: Float, vw: Float, value: Value<*>, mouseX: Int, mouseY: Int) {
        val label = value.name
        when (value) {
            is BoolValue -> {
                Fonts.fontRise35.drawString(label, vx, vy + 5f, muted.rgb)
                RoundedUtil.drawRound(vx + vw - 18f, vy + 4f, 10f, 10f, 5f, if (value.get()) accent else Color(47, 53, 68))
            }
            is IntValue -> drawNumber(vx, vy, vw, label, value.get().toFloat(), value.minimum.toFloat(), value.maximum.toFloat())
            is FloatValue -> drawNumber(vx, vy, vw, label, value.get(), value.minimum, value.maximum)
            is ListValue -> {
                Fonts.fontRise35.drawString(label, vx, vy + 5f, muted.rgb)
                val mode = value.get()
                Fonts.fontRise35.drawString(mode, vx + vw - Fonts.fontRise35.getStringWidth(mode) - 7f, vy + 5f, accent.rgb)
            }
            is TextValue -> {
                Fonts.fontRise35.drawString(label, vx, vy + 4f, muted.rgb)
                val display = if (focusedText == value) textBuffer + "_" else value.get().ifBlank { "Empty..." }
                RoundedUtil.drawRound(vx, vy + 17f, vw, 15f, 4f, Color(12, 15, 23, 230))
                Fonts.fontRise35.drawString(trimToWidth(display, (vw - 12f).toInt()), vx + 6f, vy + 21f, if (focusedText == value) text.rgb else Color(170, 178, 190).rgb)
            }
            is ColorValue -> {
                Fonts.fontRise35.drawString(label, vx, vy + 5f, muted.rgb)
                RoundedUtil.drawRound(vx + vw - 21f, vy + 3f, 14f, 12f, 3f, value.selectedColor())
                if (value.rainbow) Fonts.fontRise35.drawString("R", vx + vw - 36f, vy + 5f, accent.rgb)
            }
            else -> Fonts.fontRise35.drawString("$label: ${value.toText()}", vx, vy + 5f, muted.rgb)
        }

        if (draggingNumber == value && Mouse.isButtonDown(0)) {
            updateNumber(value, vx + 2f, vw - 4f, mouseX)
        }
    }

    private fun drawNumber(vx: Float, vy: Float, vw: Float, label: String, current: Float, min: Float, max: Float) {
        val progress = if (max == min) 0f else ((current - min) / (max - min)).coerceIn(0f, 1f)
        val sliderY = vy + 19f
        Fonts.fontRise35.drawString(label, vx, vy + 4f, muted.rgb)
        Fonts.fontRise35.drawString(formatNumber(current), vx + vw - Fonts.fontRise35.getStringWidth(formatNumber(current)) - 5f, vy + 4f, accent.rgb)
        RoundedUtil.drawRound(vx + 2f, sliderY, vw - 4f, 3f, 2f, Color(48, 54, 68))
        RoundedUtil.drawRound(vx + 2f, sliderY, (vw - 4f) * progress, 3f, 2f, accent)
        RoundedUtil.drawRound(vx + 2f + (vw - 4f) * progress - 2f, sliderY - 2f, 7f, 7f, 3.5f, Color.WHITE)
    }

    private fun clickSidebar(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        var cy = y + 50f
        Category.entries.forEach { category ->
            if (isHovered(x + 10f, cy - 2f, sidebarW - 18f, 18f, mouseX, mouseY)) {
                selectedCategory = category
                moduleScroll = 0f
                searchFocused = false
                focusedText = null
                return
            }
            cy += 21f
        }
    }

    private fun clickSearch(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        val searchX = x + sidebarW + 13f
        val searchY = y + 11f
        searchFocused = isHovered(searchX, searchY, w - sidebarW - 27f, 22f, mouseX, mouseY)
        if (searchFocused) focusedText = null
    }

    private fun clickModules(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val contentX = x + sidebarW
        if (!isHovered(contentX + 7f, y + 42f, w - sidebarW - 13f, h - 49f, mouseX, mouseY)) return
        var my = y + 48f + moduleScroll
        filteredModules().forEach { module ->
            val cardH = moduleHeight(module)
            if (isHovered(contentX + 8f, my, moduleW, moduleBaseH, mouseX, mouseY)) {
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
                searchFocused = false
                return
            }

            if (module in expandedModules) {
                var vy = my + moduleBaseH + 2f
                module.values.filter { it.shouldRender() }.forEach { value ->
                    if (isHovered(contentX + 16f, vy, moduleW - 16f, valueHeight(value), mouseX, mouseY)) {
                        clickValue(value, contentX + 16f, mouseX, mouseButton)
                        searchFocused = false
                        return
                    }
                    vy += valueHeight(value)
                }
            }
            my += cardH + 7f
        }
    }

    private fun clickValue(value: Value<*>, sliderX: Float, mouseX: Int, mouseButton: Int) {
        when (value) {
            is BoolValue -> if (mouseButton == 0) {
                value.toggle()
                valuesDirty = true
            }
            is IntValue, is FloatValue -> if (mouseButton == 0) {
                draggingNumber = value
                updateNumber(value, sliderX + 2f, moduleW - 20f, mouseX)
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
                    val color = Color.getHSBColor(((System.currentTimeMillis() / 16L) % 360L) / 360f, 0.46f, 1f)
                    value.set(Color(color.red, color.green, color.blue, value.selectedColor().alpha))
                }
                valuesDirty = true
            }
            else -> Unit
        }
    }

    private fun handleTextInput(value: TextValue, typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE, Keyboard.KEY_RETURN -> {
                focusedText = null
                flushConfigs()
                return
            }
            Keyboard.KEY_BACK -> if (textBuffer.isNotEmpty()) {
                textBuffer = textBuffer.dropLast(1)
                value.set(textBuffer)
                valuesDirty = true
                return
            }
            Keyboard.KEY_DELETE -> {
                textBuffer = ""
                value.set("")
                valuesDirty = true
                return
            }
        }

        if (!Character.isISOControl(typedChar) && textBuffer.length < 64) {
            textBuffer += typedChar
            value.set(textBuffer)
            valuesDirty = true
        }
    }

    private fun updateNumber(value: Value<*>, sliderX: Float, sliderW: Float, mouseX: Int) {
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

    private fun handleWheel(mouseX: Int, mouseY: Int) {
        val wheel = Mouse.getDWheel()
        if (wheel == 0 || !isHovered(x + sidebarW, y, w - sidebarW, h, mouseX, mouseY)) return
        moduleScroll = (moduleScroll + if (wheel > 0) 18f else -18f).coerceIn(minScroll(), 0f)
    }

    private fun filteredModules() = LiquidBounce.moduleManager[selectedCategory]
        .filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }
        .sortedBy { it.name.lowercase() }

    private fun filteredHeight() = filteredModules().sumOf { (moduleHeight(it) + 7f).toDouble() }.toFloat()

    private fun moduleHeight(module: Module): Float {
        val extraHeight = module.values.filter { it.shouldRender() }.sumOf { valueHeight(it).toDouble() }.toFloat()
        val expansion = expansionAnimations[module] ?: if (module in expandedModules) 1f else 0f
        return moduleBaseH + (extraHeight + 4f) * expansion
    }

    private fun valueHeight(value: Value<*>) = when (value) {
        is IntValue, is FloatValue -> 30f
        is TextValue -> 38f
        else -> 21f
    }

    private fun minScroll(): Float {
        val visible = h - 49f
        return (visible - filteredHeight()).coerceAtMost(0f)
    }

    private fun drawScrollbar(total: Float, sx: Float, sy: Float, sh: Float) {
        if (total <= sh) return
        val barH = (sh * (sh / total)).coerceAtLeast(24f)
        val progress = (-moduleScroll / (total - sh)).coerceIn(0f, 1f)
        RoundedUtil.drawRound(sx, sy, 2f, sh, 1f, Color(255, 255, 255, 28))
        RoundedUtil.drawRound(sx, sy + (sh - barH) * progress, 2f, barH, 1f, accent)
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

    private fun drawCircle(cx: Float, cy: Float, radius: Float, color: Color) {
        if (radius <= 0f) return
        RenderUtils.drawFilledCircle(cx + radius / 2f, cy + radius / 2f, radius / 2f, color)
    }

    private fun selectedCategoryY(): Float {
        return y + 50f + Category.entries.indexOf(selectedCategory).coerceAtLeast(0) * 21f
    }

    private fun updateExpansion(module: Module): Float {
        val current = expansionAnimations[module] ?: if (module in expandedModules) 1f else 0f
        val target = if (module in expandedModules) 1f else 0f
        val next = animate(current, target, 0.24f)
        if (next <= 0.01f && target == 0f) {
            expansionAnimations.remove(module)
        } else {
            expansionAnimations[module] = next
        }
        return next
    }

    private fun animate(current: Float, target: Float, speed: Float): Float {
        if (abs(target - current) < 0.01f) return target
        return current + (target - current) * speed
    }

    private fun easeOut(value: Float): Float {
        val clamped = value.coerceIn(0f, 1f)
        return 1f - (1f - clamped) * (1f - clamped)
    }

    private fun startScissor(sx: Float, sy: Float, sw: Float, sh: Float) {
        val sr = ScaledResolution(mc)
        val factor = sr.scaleFactor
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor((sx * factor).toInt(), ((sr.scaledHeight - sy - sh) * factor).toInt(), (sw * factor).toInt(), (sh * factor).toInt())
    }

    private fun endScissor() = GL11.glDisable(GL11.GL_SCISSOR_TEST)

    private fun trimToWidth(text: String, maxWidth: Int): String {
        if (Fonts.fontRise35.getStringWidth(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.isNotEmpty() && Fonts.fontRise35.getStringWidth("...$trimmed") > maxWidth) {
            trimmed = trimmed.drop(1)
        }
        return "...$trimmed"
    }

    private fun formatNumber(value: Float) =
        if (value % 1f == 0f) value.roundToInt().toString() else "%.2f".format(value)

    private fun isHovered(hx: Float, hy: Float, hw: Float, hh: Float, mouseX: Int, mouseY: Int) =
        mouseX >= hx && mouseX <= hx + hw && mouseY >= hy && mouseY <= hy + hh

    private fun Category.riseIcon() = when (this) {
        Category.COMBAT -> "a"
        Category.MOVEMENT -> "b"
        Category.PLAYER -> "c"
        Category.RENDER -> "g"
        Category.WORLD -> "g"
        Category.MISC -> "e"
        Category.EXPLOIT -> "a"
        Category.FUN -> "f"
        Category.CLIENT -> "e"
        Category.RISE -> "r"
    }
}