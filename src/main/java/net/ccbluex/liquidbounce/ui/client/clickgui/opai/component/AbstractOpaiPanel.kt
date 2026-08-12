package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.TranslateComponent
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.StaticFontLoader

abstract class AbstractOpaiPanel : OpaiPanel {

    protected val panelId: String
    protected val fixedTitle: String?
    protected val titleComponent: TranslateComponent?
    protected val titleSupplier: TitleSupplier?
    protected val icon: String?
    protected val openAnim = Animation(Easing.EASE_IN_OUT_CUBIC, OpaiTheme.ANIM_OPEN)
    protected val introAnim: Animation

    protected var panelX = 0f
    protected var panelY = 0f
    protected var panelWidth = OpaiTheme.PANEL_WIDTH
    protected var isPanelOpened = false
    protected var isPanelVisible = false
    protected var dragging = false
    protected var dragOffsetX = 0f
    protected var dragOffsetY = 0f
    protected var scroll = 0f
    protected var scrollVelocity = 0f
    protected var maxScroll = 0f
    protected var panelMaxHeight = 300.0f

    constructor(id: String, title: String, icon: String?, panelIndex: Int) {
        this.panelId = id
        this.fixedTitle = title
        this.titleComponent = null
        this.titleSupplier = null
        this.icon = icon
        this.introAnim = Animation(Easing.EASE_OUT_SINE, 120L + panelIndex * 45L)
    }

    constructor(id: String, titleComponent: TranslateComponent, icon: String?, panelIndex: Int) {
        this.panelId = id
        this.fixedTitle = null
        this.titleComponent = titleComponent
        this.titleSupplier = null
        this.icon = icon
        this.introAnim = Animation(Easing.EASE_OUT_SINE, 120L + panelIndex * 45L)
    }

    constructor(id: String, titleSupplier: TitleSupplier, icon: String?, panelIndex: Int) {
        this.panelId = id
        this.fixedTitle = null
        this.titleComponent = null
        this.titleSupplier = titleSupplier
        this.icon = icon
        this.introAnim = Animation(Easing.EASE_OUT_SINE, 120L + panelIndex * 45L)
    }

    override fun getId(): String = panelId

    override fun startIntro() {
        introAnim.setStartValue(0.0f)
        introAnim.run(0.0f)
        introAnim.run(1.0f)
        openAnim.setStartValue(if (isPanelOpened) 1.0f else 0.0f)
    }

    override fun getIntroValue(): Float {
        introAnim.run(1.0f)
        return introAnim.getValue()
    }

    override fun drawBackground(renderer: OpaiRenderer) {
        openAnim.run(if (isPanelOpened) 1.0f else 0.0f)
        val expand = openAnim.getValue()
        val contentHeight = computeContentHeight()
        val visibleHeight = computeVisibleContentHeight(contentHeight)
        val panelHeight = OpaiTheme.PANEL_HEADER_HEIGHT + (visibleHeight + OpaiTheme.PANEL_BOTTOM_PADDING) * expand

        renderer.shadow().addShadow(panelX, panelY, panelWidth, panelHeight, OpaiTheme.PANEL_RADIUS, OpaiTheme.PANEL_SHADOW_BLUR, OpaiTheme.panelShadow())
        renderer.roundRect().addRoundRect(panelX, panelY, panelWidth, panelHeight, OpaiTheme.PANEL_RADIUS, OpaiTheme.panelBackground())

        val iconX = panelX + 7.5f
        val textX = if (icon == null || icon.isEmpty()) panelX + 10.0f else iconX + 16.0f
        val textY = panelY + (OpaiTheme.PANEL_HEADER_HEIGHT - renderer.text().getHeight(OpaiTheme.HEADER_TEXT_SCALE)) * 0.5f
        if (icon != null && !icon.isEmpty()) {
            val iconY = panelY + (OpaiTheme.PANEL_HEADER_HEIGHT - renderer.text().getHeight(OpaiTheme.HEADER_ICON_SCALE)) * 0.5f
            renderer.text().addText(icon, iconX, iconY, OpaiTheme.HEADER_ICON_SCALE, MD3Theme.PRIMARY, StaticFontLoader.ICONS)
        }
        val headerTitle = getTitle()
        renderer.text().addText(headerTitle, textX, textY, OpaiTheme.HEADER_TEXT_SCALE, MD3Theme.TEXT_PRIMARY)
        // Arrow fixed: increased size to 4.0f
        renderer.triangle().addChevronTriangle(panelX + panelWidth - 10.0f, panelY + OpaiTheme.PANEL_HEADER_HEIGHT * 0.5f, 4.0f, expand, OpaiTheme.groupChevron(0.0f))

        if (contentHeight > visibleHeight && isPanelOpened && expand > 0.5f) {
            val scrollbarX = panelX + panelWidth - 2.5f
            val scrollbarTrackY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT
            val scrollbarTrackH = visibleHeight * expand
            val thumbRatio = visibleHeight / contentHeight
            val thumbH = 10.0f.coerceAtLeast(scrollbarTrackH * thumbRatio)
            val thumbY = scrollbarTrackY + (scrollbarTrackH - thumbH) * (if (maxScroll > 0) scroll / maxScroll else 0f)
            renderer.roundRect().addRoundRect(scrollbarX, thumbY, 2.0f, thumbH, 1.0f, OpaiTheme.scrollbar())
        }
    }

    override fun drawContent(renderer: OpaiRenderer, mouseX: Int, mouseY: Int) {
        openAnim.run(if (isPanelOpened) 1.0f else 0.0f)
        if (openAnim.getValue() < 0.01f) return

        val contentHeight = computeContentHeight()
        val visibleHeight = computeVisibleContentHeight(contentHeight)
        maxScroll = 0.0f.coerceAtLeast(contentHeight - visibleHeight)
        scroll += scrollVelocity
        scrollVelocity *= 0.82f
        if (kotlin.math.abs(scrollVelocity) < 0.5f) scrollVelocity = 0f
        if (scroll < 0f) {
            scroll += (0f - scroll) * 0.35f
            if (kotlin.math.abs(scroll) < 0.5f) scroll = 0f
            scrollVelocity = 0f
        }
        if (scroll > maxScroll) {
            scroll += (maxScroll - scroll) * 0.35f
            if (kotlin.math.abs(scroll - maxScroll) < 0.5f) scroll = maxScroll
            scrollVelocity = 0f
        }
        drawPanelContent(renderer, mouseX, mouseY, visibleHeight)
    }

    override fun getContentClipY(): Float = panelY + OpaiTheme.PANEL_HEADER_HEIGHT

    override fun getContentClipHeight(): Float {
        openAnim.run(if (isPanelOpened) 1.0f else 0.0f)
        val contentHeight = computeContentHeight()
        val visibleHeight = computeVisibleContentHeight(contentHeight)
        return visibleHeight * openAnim.getValue()
    }

    override fun getPanelHeight(): Float {
        openAnim.run(if (isPanelOpened) 1.0f else 0.0f)
        val contentHeight = computeContentHeight()
        val visibleHeight = computeVisibleContentHeight(contentHeight)
        return OpaiTheme.PANEL_HEADER_HEIGHT + (visibleHeight + OpaiTheme.PANEL_BOTTOM_PADDING) * openAnim.getValue()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (isHeaderHovered(mouseX.toDouble(), mouseY.toDouble())) {
            if (button == 0) {
                dragging = true
                dragOffsetX = panelX - mouseX
                dragOffsetY = panelY - mouseY
                return true
            }
            if (button == 1) {
                isPanelOpened = !isPanelOpened
                return true
            }
        }

        if (isPanelOpened && openAnim.getValue() > 0.5f && isContentHovered(mouseX.toDouble(), mouseY.toDouble())) {
            return mouseClickedContent(mouseX.toDouble(), mouseY.toDouble(), button)
        }
        return false
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button == 0 && dragging) {
            dragging = false
            return true
        }
        return mouseReleasedContent(mouseX.toDouble(), mouseY.toDouble(), button)
    }

    override fun mouseDragged(mouseX: Int, mouseY: Int) {
        if (dragging) {
            panelX = mouseX + dragOffsetX
            panelY = mouseY + dragOffsetY
        }
    }

    override fun mouseScrolled(mouseX: Int, mouseY: Int, amount: Float): Boolean {
        if (!isPanelOpened) return false
        if (isPanelHovered(mouseX.toDouble(), mouseY.toDouble())) {
            scrollVelocity -= amount * OpaiTheme.SCROLL_SPEED * 1.5f
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false

    override fun charTyped(typedText: String): Boolean = false

    override fun hasActiveInput(): Boolean = false

    override fun setPosition(x: Float, y: Float) {
        this.panelX = x
        this.panelY = y
    }

    override fun setMaxPanelHeight(maxPanelHeight: Float) {
        this.panelMaxHeight = maxPanelHeight
    }

    override fun getX(): Float = panelX

    override fun getY(): Float = panelY

    override fun getWidth(): Float = panelWidth

    override fun isOpened(): Boolean = isPanelOpened

    override fun setOpened(opened: Boolean) {
        this.isPanelOpened = opened
        if (!opened) { scroll = 0.0f; scrollVelocity = 0.0f }
    }

    override fun isVisible(): Boolean = isPanelVisible

    override fun setVisible(visible: Boolean) {
        this.isPanelVisible = visible
    }

    protected abstract fun computeContentHeight(): Float

    protected abstract fun drawPanelContent(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, visibleHeight: Float)

    protected open fun mouseClickedContent(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    protected open fun mouseReleasedContent(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    protected fun getTitle(): String {
        return titleSupplier?.get() ?: titleComponent?.translatedName ?: fixedTitle ?: ""
    }

    fun interface TitleSupplier {
        fun get(): String
    }

    protected fun computeVisibleContentHeight(contentHeight: Float): Float {
        val maxContentHeight = 0.0f.coerceAtLeast(panelMaxHeight - OpaiTheme.PANEL_HEADER_HEIGHT - OpaiTheme.PANEL_BOTTOM_PADDING)
        return contentHeight.coerceAtMost(maxContentHeight)
    }

    protected fun isHeaderHovered(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + OpaiTheme.PANEL_HEADER_HEIGHT
    }

    protected fun isContentHovered(mouseX: Double, mouseY: Double): Boolean {
        val clipY = getContentClipY()
        val clipH = getContentClipHeight()
        return mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= clipY && mouseY <= clipY + clipH
    }

    protected fun isPanelHovered(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + getPanelHeight()
    }

    protected fun isHovered(mouseX: Double, mouseY: Double, x: Float, y: Float, w: Float, h: Float): Boolean {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h
    }

    protected fun trimToWidth(value: String?, scale: Float, maxWidth: Float, renderer: OpaiRenderer): String {
        return Companion.trimToWidth(value, scale, maxWidth, renderer)
    }
    
    companion object {
        fun trimToWidth(value: String?, scale: Float, maxWidth: Float, renderer: OpaiRenderer): String {
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
    }

}
