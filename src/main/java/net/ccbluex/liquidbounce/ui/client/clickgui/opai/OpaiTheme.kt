package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import java.awt.Color

object OpaiTheme {

    const val SURFACE_ALPHA = 190
    const val ACCENT_ALPHA = 215

    val PANEL_WIDTH: Float get() = OpaiGUI.panelWidth
    val PANEL_HEADER_HEIGHT: Float get() = OpaiGUI.headerHeight
    val PANEL_RADIUS: Float get() = OpaiGUI.panelRadius
    val PANEL_GAP: Float get() = OpaiGUI.panelGap
    const val PANEL_MARGIN_X = 20.0f
    const val PANEL_MARGIN_Y = 20.0f
    const val PANEL_SHADOW_BLUR = 16.0f

    const val GROUP_HEADER_HEIGHT = 18.0f
    const val GROUP_INSET = 4.0f
    val GROUP_HEADER_TEXT_SCALE: Float get() = 0.60f * OpaiGUI.textSize
    const val GROUP_COUNT_CHIP_HEIGHT = 11.0f
    const val GROUP_COUNT_CHIP_PADDING = 6.0f
    val GROUP_COUNT_TEXT_SCALE: Float get() = 0.50f * OpaiGUI.textSize

    const val MODULE_HEIGHT = 19.0f
    const val MODULE_PADDING_X = 7.0f
    val MODULE_TEXT_SCALE: Float get() = 0.72f * OpaiGUI.textSize

    const val SETTING_PADDING_X = 6.0f
    val SETTING_HEIGHT: Float get() = OpaiGUI.settingHeight
    val SETTING_TEXT_SCALE: Float get() = 0.64f * OpaiGUI.textSize
    const val SETTING_GAP = 3.0f
    const val SETTING_INDENT = 5.0f

    const val SLIDER_HEIGHT = 4.0f
    const val SLIDER_RADIUS = 2.0f
    const val SLIDER_KNOB_RADIUS = 5.0f

    const val COLOR_PREVIEW_SIZE = 12.0f
    const val COLOR_PICKER_HEIGHT = 60.0f
    const val COLOR_HUE_HEIGHT = 7.0f
    const val COLOR_ALPHA_HEIGHT = 7.0f
    const val COLOR_RADIUS = 6.0f

    const val KEYBIND_WIDTH = 34.0f
    const val KEYBIND_HEIGHT = 16.0f
    const val KEYBIND_RADIUS = 6.0f

    const val INPUT_HEIGHT = 18.0f
    const val INPUT_RADIUS = 6.0f

    const val BUTTON_HEIGHT = 18.0f
    const val BUTTON_RADIUS = 6.0f

    const val SCROLL_SPEED = 14.0f

    const val PANEL_BOTTOM_PADDING = 8.0f

    val ANIM_OPEN: Long get() = OpaiGUI.getAnimTime(200L)
    val ANIM_TOGGLE: Long get() = OpaiGUI.getAnimTime(180L)
    val ANIM_HOVER: Long get() = OpaiGUI.getAnimTime(120L)
    val ANIM_EXPAND: Long get() = OpaiGUI.getAnimTime(220L)
    val ANIM_GROUP: Long get() = OpaiGUI.getAnimTime(180L)

    val HEADER_TEXT_SCALE: Float get() = 0.90f * OpaiGUI.textSize
    val HEADER_ICON_SCALE: Float get() = 0.94f * OpaiGUI.textSize

    fun surface(c: Color): Color = MD3Theme.withAlpha(c, SURFACE_ALPHA)

    fun accent(c: Color): Color = MD3Theme.withAlpha(c, ACCENT_ALPHA)

    fun panelBackground(): Color = surface(MD3Theme.SURFACE_CONTAINER)

    fun panelShadow(): Color = MD3Theme.withAlpha(MD3Theme.SHADOW, 48)

    fun moduleDivider(): Color = MD3Theme.withAlpha(MD3Theme.OUTLINE, 24)

    fun moduleEnabled(hoverProgress: Float): Color = surface(MD3Theme.lerp(MD3Theme.PRIMARY_CONTAINER, MD3Theme.lerp(MD3Theme.PRIMARY_CONTAINER, MD3Theme.PRIMARY, 0.15f), hoverProgress))

    fun moduleDisabled(hoverProgress: Float): Color = surface(MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER_LOWEST, MD3Theme.SURFACE_DIM, hoverProgress))

    fun moduleTextEnabled(): Color = MD3Theme.ON_PRIMARY_CONTAINER

    fun moduleTextDisabled(hoverProgress: Float): Color = MD3Theme.lerp(MD3Theme.TEXT_SECONDARY, MD3Theme.TEXT_PRIMARY, hoverProgress)

    fun settingLabel(): Color = MD3Theme.TEXT_PRIMARY

    fun settingLabelMuted(): Color = MD3Theme.TEXT_MUTED

    fun settingSurface(): Color = surface(MD3Theme.SURFACE_CONTAINER_LOW)

    fun sliderTrack(): Color = surface(MD3Theme.SURFACE_CONTAINER_HIGHEST)

    fun sliderActive(): Color = accent(OpaiGUI.sliderColor)

    fun sliderKnob(): Color = OpaiGUI.sliderKnobColor

    fun chipSelected(): Color = surface(MD3Theme.SECONDARY_CONTAINER)

    fun chipSelectedText(): Color = MD3Theme.ON_SECONDARY_CONTAINER

    fun chipUnselected(): Color = surface(MD3Theme.SURFACE_CONTAINER_HIGH)

    fun chipUnselectedText(): Color = MD3Theme.TEXT_SECONDARY

    fun chipOutline(): Color = MD3Theme.withAlpha(MD3Theme.OUTLINE, 120)

    fun chipSurface(): Color = Color(0, 0, 0, 0)

    fun keybindSurface(listening: Boolean): Color = surface(if (listening) MD3Theme.PRIMARY_CONTAINER else MD3Theme.SECONDARY_CONTAINER)

    fun keybindText(listening: Boolean): Color = if (listening) MD3Theme.ON_PRIMARY_CONTAINER else MD3Theme.TEXT_PRIMARY

    fun inputSurface(focused: Boolean): Color = surface(if (focused) MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER_HIGH, MD3Theme.PRIMARY_CONTAINER, 0.3f) else MD3Theme.SURFACE_CONTAINER_HIGH)

    fun inputText(): Color = MD3Theme.TEXT_PRIMARY

    fun inputIndicator(focused: Boolean): Color = if (focused) MD3Theme.PRIMARY else MD3Theme.withAlpha(MD3Theme.OUTLINE, 96)

    fun buttonSurface(hoverProgress: Float): Color = surface(MD3Theme.lerp(MD3Theme.SECONDARY_CONTAINER, MD3Theme.lerp(MD3Theme.SECONDARY_CONTAINER, MD3Theme.SECONDARY, 0.12f), hoverProgress))

    fun buttonText(): Color = MD3Theme.ON_SECONDARY_CONTAINER

    fun expandArrow(toggleProgress: Float): Color = MD3Theme.lerp(MD3Theme.TEXT_MUTED, MD3Theme.ON_PRIMARY_CONTAINER, toggleProgress)

    fun scrollbar(): Color = MD3Theme.withAlpha(MD3Theme.OUTLINE, 64)

    fun scrim(): Color = Color(0, 0, 0, 50)

    fun groupBackground(): Color = surface(MD3Theme.SURFACE_CONTAINER_LOW)

    fun groupBackgroundHover(): Color = surface(MD3Theme.SURFACE_CONTAINER)

    fun groupText(): Color = MD3Theme.TEXT_PRIMARY

    fun groupCountChip(): Color = surface(MD3Theme.SECONDARY_CONTAINER)

    fun groupCountText(): Color = MD3Theme.ON_SECONDARY_CONTAINER

    fun groupChevron(hoverProgress: Float): Color = MD3Theme.lerp(MD3Theme.TEXT_MUTED, MD3Theme.PRIMARY, hoverProgress)

    fun groupDivider(): Color = MD3Theme.withAlpha(MD3Theme.OUTLINE, 48)

    fun switchTrack(t: Float): Color = MD3Theme.lerp(surface(MD3Theme.SURFACE_CONTAINER_HIGHEST), accent(MD3Theme.PRIMARY), t)

    fun switchTrackOutlineWidth(t: Float): Float = 1.5f * (1.0f - t)

    fun switchTrackOutline(t: Float, hover: Float): Color = MD3Theme.lerp(MD3Theme.withAlpha(MD3Theme.OUTLINE, 60), MD3Theme.PRIMARY, hover)

    fun switchKnob(t: Float): Color = MD3Theme.lerp(MD3Theme.OUTLINE, MD3Theme.ON_PRIMARY, t)

    fun stateLayer(c: Color, progress: Float, maxAlpha: Int): Color = MD3Theme.withAlpha(c, (maxAlpha * progress).toInt())
}
