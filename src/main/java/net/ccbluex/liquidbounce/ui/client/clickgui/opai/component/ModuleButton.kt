package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.StaticFontLoader
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.OpaiModuleManager
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.BoolSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.ColorSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleRangeSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.EnumSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntRangeSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.SettingGroup
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.StringSetting
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.config.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import kotlin.math.roundToInt
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import org.lwjgl.BufferUtils

class ModuleButton(val module: Module) : Component() {

    private val visibleComponent = EpsilonTranslateComponent.create("module", "visible")
    private val hiddenComponent = EpsilonTranslateComponent.create("module", "hidden")

    private val sections = ArrayList<SettingSection>()
    private val groupHoverAnimations = HashMap<SettingGroup, Animation>()
    private val groupExpandAnimations = HashMap<SettingGroup, Animation>()
    private val expandAnim = Animation(Easing.EASE_IN_OUT_CUBIC, OpaiTheme.ANIM_OPEN)
    private val toggleAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_TOGGLE)
    private val hoverAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER)
    private val keybindHoverAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER)
    private val rippleAnim = Animation(Easing.EASE_OUT_CUBIC, 350L)
    private var rippleActive = false
    val titleAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER)
    var expanded = false
    var listeningKeybind = false

    init {
        val groupedWidgets = LinkedHashMap<SettingGroup, MutableList<SettingWidget<*>>>()
        val ungroupedWidgets = ArrayList<SettingWidget<*>>()

        for (value in module.values) {
            val widget = createWidget(value) ?: continue

            val group: SettingGroup? = null 
            if (group != null) {
                groupedWidgets.getOrPut(group) { ArrayList() }.add(widget)
            } else {
                ungroupedWidgets.add(widget)
            }
        }

        for (widget in ungroupedWidgets) {
            sections.add(SettingSection(null, mutableListOf(widget)))
        }

        for ((group, widgets) in groupedWidgets) {
            sections.add(SettingSection(group, widgets))
        }
    }

    private fun createWidget(value: Value<*>): SettingWidget<*>? {
        return when (value) {
            is BoolValue -> BoolWidget(BoolSetting(value))
            is IntValue -> IntSliderWidget(IntSetting(value))
            is FloatValue -> DoubleSliderWidget(DoubleSetting(value))
            is IntRangeValue -> IntRangeWidget(IntRangeSetting(value))
            is FloatRangeValue -> DoubleRangeWidget(DoubleRangeSetting(value))
            is ListValue -> EnumWidget(EnumSetting(value))
            is ColorValue -> ColorWidget(ColorSetting(value))
            is TextValue -> StringWidget(StringSetting(value))
            else -> null
        }
    }

    override fun getHeight(): Float {
        expandAnim.run(if (expanded) 1.0f else 0.0f)
        val settingsHeight = computeSettingsHeight()
        return OpaiTheme.MODULE_HEIGHT + settingsHeight * expandAnim.getValue()
    }

    private fun computeSettingsHeight(): Float {
        if (sections.isEmpty()) return 0.0f
        var height = OpaiTheme.SETTING_GAP
        for (section in sections) {
            if (section.isGroup()) {
                height += OpaiTheme.GROUP_HEADER_HEIGHT
                if (!section.group!!.isCollapsed()) {
                    height += OpaiTheme.GROUP_INSET
                    for (widget in section.widgets) {
                        if (widget.isVisible()) {
                            height += widget.getHeight() + OpaiTheme.SETTING_GAP
                        }
                    }
                }
                height += OpaiTheme.SETTING_GAP
            } else {
                for (widget in section.widgets) {
                    if (widget.isVisible()) {
                        height += widget.getHeight() + OpaiTheme.SETTING_GAP
                    }
                }
            }
        }
        return height
    }

    private fun getSectionHeight(section: SettingSection): Float {
        if (!section.isGroup()) {
            var h = 0.0f
            for (widget in section.widgets) {
                if (widget.isVisible()) {
                    h += widget.getHeight() + OpaiTheme.SETTING_GAP
                }
            }
            return h
        }

        if (section.group!!.isCollapsed()) {
            return OpaiTheme.GROUP_HEADER_HEIGHT + OpaiTheme.SETTING_GAP
        }

        var h = OpaiTheme.GROUP_HEADER_HEIGHT + OpaiTheme.SETTING_GAP + OpaiTheme.GROUP_INSET
        for (widget in section.widgets) {
            if (widget.isVisible()) {
                h += widget.getHeight() + OpaiTheme.SETTING_GAP
            }
        }
        return h
    }

    override fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        expandAnim.run(if (expanded) 1.0f else 0.0f)
        toggleAnim.run(if (module.state) 1.0f else 0.0f)
        val headerHovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), x, y, width, OpaiTheme.MODULE_HEIGHT)
        hoverAnim.run(if (headerHovered) 1.0f else 0.0f)

        val hover = hoverAnim.getValue()
        val toggle = toggleAnim.getValue()

        val bg = MD3Theme.lerp(OpaiTheme.moduleDisabled(hover), OpaiTheme.moduleEnabled(hover), toggle)
        renderer.rect().addRect(x + 2.0f, y, width - 4.0f, OpaiTheme.MODULE_HEIGHT, bg)

        if (rippleActive) {
            rippleAnim.run(1.0f)
        }
        val rp = rippleAnim.getValue()
        if (rp > 0.02f && rippleActive) {
            val elemW = width - 4.0f
            val elemH = OpaiTheme.MODULE_HEIGHT
            val cx = x + 2.0f + elemW * 0.5f
            val cy = y + elemH * 0.5f
            val maxRadius = (Math.sqrt((elemW * elemW + elemH * elemH).toDouble()) * 0.5).toFloat()
            val radius = maxRadius * rp
            val alpha = ((1.0f - rp) * 60).toInt().coerceIn(0, 60)

            val sr = ScaledResolution(Minecraft.getMinecraft())
            val s = sr.scaleFactor
            val guiH = sr.scaledHeight
            val rpScX = ((x + 2.0f) * s).roundToInt()
            val rpScY = ((guiH - y - elemH) * s).roundToInt()
            val rpScW = (elemW * s).roundToInt()
            val rpScH = (elemH * s).roundToInt()
            val wasScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
            val oldBox = BufferUtils.createIntBuffer(16)
            if (wasScissor) GL11.glGetInteger(GL11.GL_SCISSOR_BOX, oldBox)
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            GL11.glScissor(rpScX, rpScY, rpScW, rpScH)
            renderer.roundRect().addRoundRect(cx - radius, cy - radius, radius * 2.0f, radius * 2.0f, radius, Color(255, 255, 255, alpha))
            if (wasScissor) {
                GL11.glScissor(oldBox.get(0), oldBox.get(1), oldBox.get(2), oldBox.get(3))
                GL11.glEnable(GL11.GL_SCISSOR_TEST)
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST)
            }
        }

        renderer.rect().addRect(x + 3.0f, y + OpaiTheme.MODULE_HEIGHT - 0.5f, width - 6.0f, 0.5f, OpaiTheme.moduleDivider())

        val textColor = MD3Theme.lerp(OpaiTheme.moduleTextDisabled(hover), OpaiTheme.moduleTextEnabled(), toggle)
        val textY = y + (OpaiTheme.MODULE_HEIGHT - renderer.text().getHeight(OpaiTheme.MODULE_TEXT_SCALE)) * 0.5f
        renderer.text().addText(module.name, x + OpaiTheme.MODULE_PADDING_X, textY, OpaiTheme.MODULE_TEXT_SCALE, textColor)

        drawKeybindButton(renderer, mouseX, mouseY, toggle)

        val expand = expandAnim.getValue()

        for (section in sections) {
            if (section.isGroup()) {
                runGroupAnimations(section)
            }
        }

        if (expand > 0.01f) {
            var settingY = y + OpaiTheme.MODULE_HEIGHT + OpaiTheme.SETTING_GAP
            for (section in sections) {
                val sectionH = getSectionHeight(section)
                if (section.isGroup()) {
                    if (expand > 0.5f) {
                        drawGroupSection(renderer, mouseX, mouseY, section, settingY)
                    }
                    settingY += sectionH
                } else {
                    for (widget in section.widgets) {
                        if (!widget.isVisible()) continue
                        widget.setPosition(x + OpaiTheme.SETTING_INDENT, settingY, width - OpaiTheme.SETTING_INDENT * 2.0f)
                        if (expand > 0.5f) {
                            widget.draw(renderer, mouseX, mouseY)
                        }
                        settingY += widget.getHeight() + OpaiTheme.SETTING_GAP
                    }
                }
            }
        }
    }

    private fun runGroupAnimations(section: SettingSection) {
        val group = section.group!!
        val expandAnimG = groupExpandAnimations.getOrPut(group) {
            Animation(Easing.EASE_OUT_CUBIC, 180L).apply { setStartValue(if (group.isCollapsed()) 0.0f else 1.0f) }
        }
        expandAnimG.run(if (group.isCollapsed()) 0.0f else 1.0f)
    }

    private fun drawGroupSection(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, section: SettingSection, sectionY: Float) {
        val group = section.group!!

        val headerW = width - OpaiTheme.SETTING_INDENT * 2.0f
        val headerX = x + OpaiTheme.SETTING_INDENT
        val headerH = OpaiTheme.GROUP_HEADER_HEIGHT

        val hAnim = groupHoverAnimations.getOrPut(group) { Animation(Easing.EASE_OUT_CUBIC, 120L) }
        hAnim.run(if (isHovered(mouseX.toDouble(), mouseY.toDouble(), headerX, sectionY, headerW, headerH)) 1.0f else 0.0f)
        val hoverProgress = hAnim.getValue()

        val expandAnimG = groupExpandAnimations[group]
        val expandProgress = expandAnimG?.getValue() ?: if (group.isCollapsed()) 0.0f else 1.0f

        val headerBg = MD3Theme.lerp(OpaiTheme.groupBackground(), OpaiTheme.groupBackgroundHover(), hoverProgress)
        val headerRadius = OpaiTheme.BUTTON_RADIUS
        renderer.roundRect().addRoundRect(headerX, sectionY, headerW, headerH, headerRadius, headerBg)

        val label = AbstractOpaiPanel.trimToWidth(group.getDisplayName(), OpaiTheme.GROUP_HEADER_TEXT_SCALE, headerW - 74.0f, renderer)
        val labelY = sectionY + (headerH - renderer.text().getHeight(OpaiTheme.GROUP_HEADER_TEXT_SCALE)) * 0.5f
        renderer.text().addText(label, headerX + OpaiTheme.SETTING_PADDING_X, labelY, OpaiTheme.GROUP_HEADER_TEXT_SCALE, OpaiTheme.groupText())

        val countLabel = section.widgets.size.toString()
        val countWidth = renderer.text().getWidth(countLabel, OpaiTheme.GROUP_COUNT_TEXT_SCALE) + OpaiTheme.GROUP_COUNT_CHIP_PADDING * 2.0f
        val countX = headerX + headerW - OpaiTheme.SETTING_PADDING_X - countWidth - 12.0f
        val chipH = OpaiTheme.GROUP_COUNT_CHIP_HEIGHT
        val countY = sectionY + (headerH - chipH) * 0.5f
        renderer.roundRect().addRoundRect(countX, countY, countWidth, chipH, chipH / 2.0f, OpaiTheme.groupCountChip())
        val countTextY = countY + (chipH - renderer.text().getHeight(OpaiTheme.GROUP_COUNT_TEXT_SCALE)) * 0.5f
        renderer.text().addText(countLabel, countX + OpaiTheme.GROUP_COUNT_CHIP_PADDING, countTextY, OpaiTheme.GROUP_COUNT_TEXT_SCALE, OpaiTheme.groupCountText())

        val chevronSize = 2.5f
        val chevronCenterX = headerX + headerW - OpaiTheme.SETTING_PADDING_X - chevronSize
        val chevronCenterY = sectionY + headerH * 0.5f
        renderer.triangle().addChevronTriangle(chevronCenterX, chevronCenterY, chevronSize, expandProgress, OpaiTheme.groupChevron(hoverProgress))

        if (!group.isCollapsed()) {
            var childY = sectionY + headerH + OpaiTheme.SETTING_GAP + OpaiTheme.GROUP_INSET
            val childX = x + OpaiTheme.SETTING_INDENT + OpaiTheme.GROUP_INSET
            val childW = width - (OpaiTheme.SETTING_INDENT + OpaiTheme.GROUP_INSET) * 2.0f
            for (widget in section.widgets) {
                if (!widget.isVisible()) continue
                widget.setPosition(childX, childY, childW)
                widget.draw(renderer, mouseX, mouseY)
                childY += widget.getHeight() + OpaiTheme.SETTING_GAP
            }
        }
    }

    private fun drawKeybindButton(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, toggle: Float) {
        val btnW = OpaiTheme.KEYBIND_WIDTH
        val btnH = OpaiTheme.KEYBIND_HEIGHT
        val btnX = x + width - OpaiTheme.MODULE_PADDING_X - btnW
        val btnY = y + (OpaiTheme.MODULE_HEIGHT - btnH) * 0.5f
        val radius = OpaiTheme.KEYBIND_RADIUS
        val btnHovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), btnX, btnY, btnW, btnH)
        keybindHoverAnim.run(if (btnHovered) 1.0f else 0.0f)
        val kbHover = keybindHoverAnim.getValue()

        val keyText = if (listeningKeybind) "..." else Keyboard.getKeyName(module.keyBind)
        val textScale = if (keyText.length >= 3) 0.54f else 0.60f
        val textW = renderer.text().getWidth(keyText, textScale)
        val textH = renderer.text().getHeight(textScale)

        val text = OpaiTheme.keybindText(listeningKeybind)
        val surface: Color
        val outline: Color
        if (listeningKeybind) {
            surface = OpaiTheme.keybindSurface(true)
            outline = MD3Theme.withAlpha(MD3Theme.PRIMARY, 140)
        } else {
            surface = MD3Theme.lerp(OpaiTheme.keybindSurface(false), OpaiTheme.accent(MD3Theme.PRIMARY), toggle)
            outline = MD3Theme.withAlpha(MD3Theme.TEXT_PRIMARY, 60)
        }

        renderer.roundRect().addRoundRect(btnX, btnY, btnW, btnH, radius, surface)
        renderer.outline().addOutline(btnX, btnY, btnW, btnH, radius, 0.8f, outline)

        val textX = btnX + (btnW - textW) * 0.5f
        val textY = btnY + (btnH - textH) * 0.5f - 0.5f
        renderer.text().addText(keyText, textX, textY, textScale, text)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (listeningKeybind) {
            module.keyBind = if (button == 0) 0 else button + 1000 
            listeningKeybind = false
            return true
        }

        if (isHovered(mouseX, mouseY, x, y, width, OpaiTheme.MODULE_HEIGHT)) {
            if (isKeybindButtonHovered(mouseX, mouseY)) {
                if (button == 0) {
                    listeningKeybind = true
                    return true
                }
            }
            if (button == 0) {
                module.state = !module.state
                rippleActive = true
                rippleAnim.setStartValue(0.0f)
                return true
            }
            if (button == 1) {
                expanded = !expanded
                rippleActive = true
                rippleAnim.setStartValue(0.0f)
                return true
            }
        }

        if (expanded && expandAnim.getValue() > 0.5f) {
            var settingY = y + OpaiTheme.MODULE_HEIGHT + OpaiTheme.SETTING_GAP
            for (section in sections) {
                if (section.isGroup()) {
                    val headerX = x + OpaiTheme.SETTING_INDENT
                    if (isGroupHeaderHovered(mouseX, mouseY, headerX, settingY)) {
                        section.group!!.toggleCollapsed()
                        return true
                    }
                    if (!section.group!!.isCollapsed()) {
                        for (widget in section.widgets) {
                            if (!widget.isVisible()) continue
                            if (widget.mouseClicked(mouseX, mouseY, button)) {
                                return true
                            }
                        }
                    }
                } else {
                    for (widget in section.widgets) {
                        if (!widget.isVisible()) continue
                        if (widget.mouseClicked(mouseX, mouseY, button)) {
                            return true
                        }
                    }
                }
                settingY += getSectionHeight(section)
            }
        }
        return false
    }

    private fun isKeybindButtonHovered(mouseX: Double, mouseY: Double): Boolean {
        val btnX = x + width - OpaiTheme.MODULE_PADDING_X - OpaiTheme.KEYBIND_WIDTH
        val btnH = OpaiTheme.KEYBIND_HEIGHT
        val btnY = y + (OpaiTheme.MODULE_HEIGHT - btnH) * 0.5f
        return isHovered(mouseX, mouseY, btnX, btnY, OpaiTheme.KEYBIND_WIDTH, btnH)
    }

    private fun isGroupHeaderHovered(mouseX: Double, mouseY: Double, headerX: Float, headerY: Float): Boolean {
        val headerW = width - OpaiTheme.SETTING_INDENT * 2.0f
        return isHovered(mouseX, mouseY, headerX, headerY, headerW, OpaiTheme.GROUP_HEADER_HEIGHT)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (expanded) {
            for (section in sections) {
                for (widget in section.widgets) {
                    if (!widget.isVisible()) continue
                    if (widget.mouseReleased(mouseX, mouseY, button)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (listeningKeybind) {
            module.keyBind = if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK) 0 else keyCode
            listeningKeybind = false
            return true
        }

        if (expanded) {
            for (section in sections) {
                for (widget in section.widgets) {
                    if (!widget.isVisible()) continue
                    if (widget.keyPressed(keyCode, scanCode, modifiers)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun charTyped(typedText: String): Boolean {
        if (expanded) {
            for (section in sections) {
                for (widget in section.widgets) {
                    if (!widget.isVisible()) continue
                    if (widget.charTyped(typedText)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun hasListeningKeybind(): Boolean {
        if (listeningKeybind) return true
        for (section in sections) {
            for (widget in section.widgets) {
                if (widget is KeybindWidget && widget.listening) return true
            }
        }
        return false
    }

    fun hasFocusedInput(): Boolean {
        for (section in sections) {
            for (widget in section.widgets) {
                if (widget is StringWidget && widget.isFocused()) return true
                if (widget is IntSliderWidget && widget.isFocused()) return true
                if (widget is DoubleSliderWidget && widget.isFocused()) return true
            }
        }
        return false
    }

    private class SettingSection(val group: SettingGroup?, val widgets: MutableList<SettingWidget<*>>) {
        fun isGroup(): Boolean = group != null
    }

}
