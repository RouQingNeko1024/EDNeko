package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.EpsilonTranslateComponent
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.BoolSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.IntRangeSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.DoubleRangeSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.EnumSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.ColorSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.KeybindSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.StringSetting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.Setting
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.SettingGroup
import java.util.*

class SettingsContent(settings: List<Setting<*>>, orderedGroups: List<SettingGroup>) {

    private val noSettingsComponent = EpsilonTranslateComponent.create("gui", "addon.no_settings")

    private val sections = ArrayList<SettingSection>()
    private val groupHoverAnimations = HashMap<SettingGroup, Animation>()
    private val groupExpandAnimations = HashMap<SettingGroup, Animation>()

    init {
        val groupedSections = LinkedHashMap<SettingGroup, SettingSection>()

        for (setting in settings) {
            val widget = createWidget(setting) ?: continue
            val group = setting.getGroup()
            if (group != null) {
                var section = groupedSections[group]
                if (section == null) {
                    section = SettingSection(group, ArrayList())
                    groupedSections[group] = section
                    sections.add(section)
                }
                section.widgets.add(widget)
            } else {
                sections.add(SettingSection(null, arrayListOf(widget)))
            }
        }
    }

    companion object {
        fun createWidget(setting: Any?): SettingWidget<*>? {
            return when (setting) {
                is BoolSetting -> BoolWidget(setting)
                is IntSetting -> IntSliderWidget(setting)
                is DoubleSetting -> DoubleSliderWidget(setting)
                is IntRangeSetting -> IntRangeWidget(setting)
                is DoubleRangeSetting -> DoubleRangeWidget(setting)
                is EnumSetting -> EnumWidget(setting)
                is ColorSetting -> ColorWidget(setting)
                is KeybindSetting -> KeybindWidget(setting)
                is StringSetting -> StringWidget(setting)
                else -> null
            }
        }
    }

    fun computeContentHeight(): Float {
        if (sections.isEmpty()) return OpaiTheme.MODULE_HEIGHT
        var height = OpaiTheme.SETTING_GAP
        for (section in sections) {
            height += getSectionHeight(section)
        }
        return height
    }

    fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, panelX: Float, contentY: Float, panelWidth: Float) {
        if (sections.isEmpty()) {
            val label = noSettingsComponent.translatedName
            val labelScale = 0.66f
            val textW = renderer.text().getWidth(label, labelScale)
            renderer.text().addText(label, panelX + (panelWidth - textW) * 0.5f, contentY + 8.0f, labelScale, MD3Theme.TEXT_MUTED)
            return
        }

        var currentY = contentY + OpaiTheme.SETTING_GAP
        for (section in sections) {
            if (section.isGroup()) {
                drawGroupSection(renderer, mouseX, mouseY, section, panelX, currentY, panelWidth)
            } else {
                var widgetY = currentY
                for (widget in section.widgets) {
                    if (!widget.isVisible()) continue
                    widget.setPosition(panelX + OpaiTheme.SETTING_INDENT, widgetY, panelWidth - OpaiTheme.SETTING_INDENT * 2.0f)
                    widget.draw(renderer, mouseX, mouseY)
                    widgetY += widget.getHeight() + OpaiTheme.SETTING_GAP
                }
            }
            currentY += getSectionHeight(section)
        }
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int, panelX: Float, contentY: Float, panelWidth: Float): Boolean {
        var currentY = contentY + OpaiTheme.SETTING_GAP
        for (section in sections) {
            if (section.isGroup()) {
                val headerX = panelX + OpaiTheme.SETTING_INDENT
                val headerW = panelWidth - OpaiTheme.SETTING_INDENT * 2.0f
                if (isHovered(mouseX, mouseY, headerX.toDouble(), currentY.toDouble(), headerW.toDouble(), OpaiTheme.GROUP_HEADER_HEIGHT.toDouble())) {
                    section.group!!.toggleCollapsed()
                    ConfigManager.INSTANCE.saveNow()
                    return true
                }
                if (!section.group!!.isCollapsed()) {
                    for (widget in section.widgets) {
                        if (!widget.isVisible()) continue
                        if (widget.mouseClicked(mouseX, mouseY, button)) {
                            ConfigManager.INSTANCE.saveNow()
                            return true
                        }
                    }
                }
            } else {
                for (widget in section.widgets) {
                    if (!widget.isVisible()) continue
                    if (widget.mouseClicked(mouseX, mouseY, button)) {
                        ConfigManager.INSTANCE.saveNow()
                        return true
                    }
                }
            }
            currentY += getSectionHeight(section)
        }
        return false
    }

    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int, panelX: Float, contentY: Float, panelWidth: Float): Boolean {
        for (section in sections) {
            for (widget in section.widgets) {
                if (!widget.isVisible()) continue
                if (widget.mouseReleased(mouseX, mouseY, button)) {
                    ConfigManager.INSTANCE.saveNow()
                    return true
                }
            }
        }
        return false
    }

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        for (section in sections) {
            for (widget in section.widgets) {
                if (!widget.isVisible()) continue
                if (widget.keyPressed(keyCode, scanCode, modifiers)) {
                    ConfigManager.INSTANCE.saveNow()
                    return true
                }
            }
        }
        return false
    }

    fun charTyped(typedText: String): Boolean {
        for (section in sections) {
            for (widget in section.widgets) {
                if (!widget.isVisible()) continue
                if (widget.charTyped(typedText)) {
                    ConfigManager.INSTANCE.saveNow()
                    return true
                }
            }
        }
        return false
    }

    fun hasActiveInput(): Boolean {
        for (section in sections) {
            for (widget in section.widgets) {
                if (widget is KeybindWidget && widget.listening) return true
                if (widget is StringWidget && widget.isFocused()) return true
                if (widget is IntSliderWidget && widget.isFocused()) return true
                if (widget is DoubleSliderWidget && widget.isFocused()) return true
            }
        }
        return false
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

    private fun drawGroupSection(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, section: SettingSection, panelX: Float, sectionY: Float, panelWidth: Float) {
        val group = section.group!!
        val expandAnimG = groupExpandAnimations.getOrPut(group) {
            Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_GROUP).apply { setStartValue(if (group.isCollapsed()) 0.0f else 1.0f) }
        }
        val hAnim = groupHoverAnimations.getOrPut(group) { Animation(Easing.EASE_OUT_CUBIC, 120L) }
        
        val headerW = panelWidth - OpaiTheme.SETTING_INDENT * 2.0f
        val headerX = panelX + OpaiTheme.SETTING_INDENT
        val headerH = OpaiTheme.GROUP_HEADER_HEIGHT
        
        hAnim.run(if (isHovered(mouseX.toDouble(), mouseY.toDouble(), headerX.toDouble(), sectionY.toDouble(), headerW.toDouble(), headerH.toDouble())) 1.0f else 0.0f)
        expandAnimG.run(if (group.isCollapsed()) 0.0f else 1.0f)

        val hoverProgress = hAnim.getValue()
        val expandProgress = expandAnimG.getValue()
        renderer.roundRect().addRoundRect(headerX, sectionY, headerW, headerH, OpaiTheme.BUTTON_RADIUS,
                MD3Theme.lerp(OpaiTheme.groupBackground(), OpaiTheme.groupBackgroundHover(), hoverProgress))

        val label = trimToWidth(group.getDisplayName(), OpaiTheme.GROUP_HEADER_TEXT_SCALE, headerW - 74.0f, renderer)
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

        renderer.triangle().addChevronTriangle(headerX + headerW - OpaiTheme.SETTING_PADDING_X - 2.5f,
                sectionY + headerH * 0.5f, 2.5f, expandProgress, OpaiTheme.groupChevron(hoverProgress))

        if (!group.isCollapsed()) {
            var childY = sectionY + headerH + OpaiTheme.SETTING_GAP + OpaiTheme.GROUP_INSET
            val childX = panelX + OpaiTheme.SETTING_INDENT + OpaiTheme.GROUP_INSET
            val childW = panelWidth - (OpaiTheme.SETTING_INDENT + OpaiTheme.GROUP_INSET) * 2.0f
            for (widget in section.widgets) {
                if (!widget.isVisible()) continue
                widget.setPosition(childX, childY, childW)
                widget.draw(renderer, mouseX, mouseY)
                childY += widget.getHeight() + OpaiTheme.SETTING_GAP
            }
        }
    }

    private fun isHovered(mouseX: Double, mouseY: Double, x: Double, y: Double, w: Double, h: Double): Boolean {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h
    }

    private fun trimToWidth(value: String?, scale: Float, maxWidth: Float, renderer: OpaiRenderer): String {
        if (value == null || value.isEmpty()) return ""
        if (renderer.text().getWidth(value, scale) <= maxWidth) return value
        val ellipsis = "..."
        val ellipsisWidth = renderer.text().getWidth(ellipsis, scale)
        if (ellipsisWidth >= maxWidth) return ellipsis
        for (len in value.length - 1 downTo 0) {
            val candidate = value.substring(0, len) + ellipsis
            if (renderer.text().getWidth(candidate, scale) <= maxWidth) return candidate
        }
        return ellipsis
    }

    private class SettingSection(val group: SettingGroup?, val widgets: MutableList<SettingWidget<*>>) {
        fun isGroup(): Boolean = group != null
    }

}
