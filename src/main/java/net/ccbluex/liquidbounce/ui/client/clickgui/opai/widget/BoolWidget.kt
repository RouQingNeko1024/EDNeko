package net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiRenderer
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiTheme
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.Animation
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.Easing
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.SettingAdapters.BoolSetting
import net.minecraft.util.MathHelper

class BoolWidget(setting: BoolSetting) : SettingWidget<BoolSetting>(setting) {

    private val toggleAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_TOGGLE)
    private val knobBounceAnim = Animation(Easing.EASE_OUT_ELASTIC, 450L)
    private val hoverAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER)

    init {
        val initial = if (setting.getValue()) 1.0f else 0.0f
        toggleAnim.setStartValue(initial)
        knobBounceAnim.setStartValue(initial)
    }

    override fun getHeight(): Float = OpaiTheme.SETTING_HEIGHT

    private fun lerp(t: Float, a: Float, b: Float): Float = a + t * (b - a)

    override fun draw(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        val target = if (setting.getValue()) 1.0f else 0.0f
        toggleAnim.run(target)
        knobBounceAnim.run(target)
        val t = toggleAnim.getValue()
        val bounce = knobBounceAnim.getValue()

        val sw = 22.0f
        val sh = 12.0f
        val sx = x + width - OpaiTheme.SETTING_PADDING_X - sw
        val sy = y + (getHeight() - sh) * 0.5f

        val hovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), sx - 2, sy - 2, sw + 4, sh + 4)
        hoverAnim.run(if (hovered) 1.0f else 0.0f)
        val hoverProgress = hoverAnim.getValue()

        renderer.text().addText(setting.getDisplayName(), x + OpaiTheme.SETTING_PADDING_X, y + (getHeight() - renderer.text().getHeight(OpaiTheme.SETTING_TEXT_SCALE)) * 0.5f, OpaiTheme.SETTING_TEXT_SCALE, OpaiTheme.settingLabel())

        renderer.roundRect().addRoundRect(sx, sy, sw, sh, 6.0f, OpaiTheme.switchTrack(t))

        val outlineW = OpaiTheme.switchTrackOutlineWidth(t)
        if (outlineW > 0.01f) {
            renderer.outline().addOutline(sx, sy, sw, sh, 6.0f, outlineW, OpaiTheme.switchTrackOutline(t, hoverProgress))
        }

        val knobSize = lerp(MathHelper.clamp_float(t, 0.0f, 1.0f), 6.0f, 9.0f)
        val stretchFactor = 4.0f * t * (1.0f - t)
        val knobW = knobSize + 3.5f * stretchFactor
        val inset = lerp(t, 3.5f, 2.0f)
        val knobMinX = sx + inset + knobW * 0.5f
        val knobMaxX = sx + sw - inset - knobW * 0.5f
        val knobCx = lerp(bounce, knobMinX, knobMaxX)
        val knobCy = sy + sh * 0.5f

        if (hoverProgress > 0.02f) {
            val haloX = knobCx - 16.0f * 0.5f
            val haloY = knobCy - 16.0f * 0.5f
            renderer.roundRect().addRoundRect(haloX, haloY, 16.0f, 16.0f, 8.0f, OpaiTheme.stateLayer(OpaiTheme.settingLabel(), hoverProgress, 18))
        }

        val knobRadius = knobSize * 0.5f
        renderer.roundRect().addRoundRect(knobCx - knobW * 0.5f, knobCy - knobSize * 0.5f, knobW, knobSize, knobRadius, OpaiTheme.switchKnob(t))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val sw = 22.0f
            val sh = 12.0f
            val sx = x + width - OpaiTheme.SETTING_PADDING_X - sw
            val sy = y + (getHeight() - sh) * 0.5f
            if (isHovered(mouseX, mouseY, sx - 2, sy - 2, sw + 4, sh + 4)) {
                setting.setValue(!setting.getValue())
                return true
            }
        }
        return false
    }

}
