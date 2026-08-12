package net.ccbluex.liquidbounce.ui.client.clickgui.opai.component

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.Constants
import net.ccbluex.liquidbounce.features.module.Category
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import kotlin.math.roundToInt
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import java.util.ArrayList
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.awt.Color

class MainOpaiPanel(panelIndex: Int, togglePanel: Consumer<String>, anySubPanelVisible: BooleanSupplier, panelVisibleResolver: PanelVisibleResolver, autoArrange: Runnable) : AbstractOpaiPanel("main", Constants.NAME, "", panelIndex) {

    private val entries = ArrayList<Entry>()
    private val settingsContent: SettingsContent
    private val autoArrangeAction: Runnable = autoArrange

    init {
        this.panelWidth = 130.0f
        this.settingsContent = SettingsContent(ArrayList(), ArrayList())
        setVisible(true)
        setOpened(true)
        for (category in Category.values()) {
            add({ category.displayName }, "category:${category.name}", togglePanel, panelVisibleResolver)
        }
    }

    private fun add(labelSupplier: () -> String, panelId: String, togglePanel: Consumer<String>, panelVisibleResolver: PanelVisibleResolver) {
        entries.add(Entry(labelSupplier, panelId, togglePanel, BooleanSupplier { panelVisibleResolver.getAsBoolean(panelId) }))
    }

    override fun drawBackground(renderer: OpaiRenderer) {
        super.drawBackground(renderer)
    }

    override fun computeContentHeight(): Float {
        val count = entries.size
        val btnH = 22.0f
        return 7.0f + count * (btnH + 3.0f) + 7.0f + 3.0f + btnH + 7.0f + settingsContent.computeContentHeight()
    }

    override fun drawPanelContent(renderer: OpaiRenderer, mouseX: Int, mouseY: Int, visibleHeight: Float) {
        var currentY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT - scroll + 7.0f
        renderer.rect().addRect(panelX + 7.0f, panelY + OpaiTheme.PANEL_HEADER_HEIGHT, panelWidth - 7.0f * 2.0f, 0.7f, MD3Theme.withAlpha(MD3Theme.OUTLINE, 55))

        val btnH = 22.0f
        for (entry in entries) {
            val entryY = currentY
            val hovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), panelX + 5.0f, entryY, panelWidth - 10.0f, btnH)
            entry.hoverAnim.run(if (hovered) 1.0f else 0.0f)
            val hover = entry.hoverAnim.getValue()
            entry.rippleAnim.run(if (entry.rippleActive) 1.0f else 0.0f)

            val bg = MD3Theme.lerp(
                if (entry.isActive()) MD3Theme.PRIMARY_CONTAINER else MD3Theme.SURFACE_CONTAINER_HIGH,
                MD3Theme.PRIMARY_CONTAINER, hover * 0.5f)
            renderer.roundRect().addRoundRect(panelX + 5.0f, entryY, panelWidth - 10.0f, btnH, OpaiTheme.BUTTON_RADIUS, OpaiTheme.surface(bg))

            if (entry.rippleActive) {
                entry.rippleAnim.run(1.0f)
            }
            val rp = entry.rippleAnim.getValue()
            if (rp > 0.02f && entry.rippleActive) {
                val elemW = panelWidth - 10.0f
                val elemH = btnH
                val cx = panelX + 5.0f + elemW * 0.5f
                val cy = entryY + elemH * 0.5f
                val maxRadius = (Math.sqrt((elemW * elemW + elemH * elemH).toDouble()) * 0.5).toFloat()
                val radius = maxRadius * rp
                val alpha = ((1.0f - rp) * 60).toInt().coerceIn(0, 60)

                val sr = ScaledResolution(Minecraft.getMinecraft())
                val s = sr.scaleFactor
                val guiH = sr.scaledHeight
                val rpScX = ((panelX + 5.0f) * s).roundToInt()
                val rpScY = ((guiH - entryY - elemH) * s).roundToInt()
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

            val labelScale = 0.64f
            val textY = entryY + (btnH - renderer.text().getHeight(labelScale)) * 0.5f
            renderer.text().addText(entry.labelSupplier(), panelX + 10.0f, textY, labelScale, if (entry.isActive()) MD3Theme.ON_PRIMARY_CONTAINER else MD3Theme.TEXT_PRIMARY)
            if (entry.isActive()) {
                val dotX = panelX + panelWidth - 12.0f
                val dotY = entryY + (btnH - 4.0f) * 0.5f
                renderer.roundRect().addRoundRect(dotX, dotY, 4.0f, 4.0f, 2.0f, MD3Theme.ON_PRIMARY_CONTAINER)
            }

            currentY += btnH + 3.0f
        }

        currentY += 7.0f
        renderer.rect().addRect(panelX + 7.0f, currentY - 3.0f, panelWidth - 7.0f * 2.0f, 0.7f, MD3Theme.withAlpha(MD3Theme.OUTLINE, 55))

        val autoBtnY = currentY
        val autoHovered = isHovered(mouseX.toDouble(), mouseY.toDouble(), panelX + 5.0f, autoBtnY, panelWidth - 10.0f, btnH)
        autoHoverAnim.run(if (autoHovered) 1.0f else 0.0f)
        val autoHover = autoHoverAnim.getValue()
        val autoBg = MD3Theme.lerp(MD3Theme.SECONDARY_CONTAINER, MD3Theme.SECONDARY, autoHover * 0.15f)
        renderer.roundRect().addRoundRect(panelX + 5.0f, autoBtnY, panelWidth - 10.0f, btnH, OpaiTheme.BUTTON_RADIUS, OpaiTheme.surface(autoBg))
        val autoTextY = autoBtnY + (btnH - renderer.text().getHeight(0.58f)) * 0.5f
        renderer.text().addText("Auto Arrange", panelX + 10.0f, autoTextY, 0.58f, MD3Theme.ON_SECONDARY_CONTAINER)

        currentY += btnH + 7.0f
        settingsContent.draw(renderer, mouseX, mouseY, panelX, currentY, panelWidth)
    }

    override fun mouseClickedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false
        val startY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT - scroll + 7.0f
        val btnH = 22.0f
        var currentY = startY
        for (entry in entries) {
            if (isHovered(mouseX, mouseY, panelX + 5.0f, currentY, panelWidth - 10.0f, btnH)) {
                entry.rippleActive = true
                entry.rippleAnim.setStartValue(0.0f)
                entry.action.accept(entry.panelId)
                return true
            }
            currentY += btnH + 3.0f
        }
        currentY += 7.0f
        if (isHovered(mouseX, mouseY, panelX + 5.0f, currentY, panelWidth - 10.0f, btnH)) {
            autoArrangeAction.run()
            return true
        }
        val settingsY = currentY + btnH + 7.0f
        return settingsContent.mouseClicked(mouseX, mouseY, button, panelX, settingsY, panelWidth)
    }

    override fun mouseReleasedContent(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val settingsY = panelY + OpaiTheme.PANEL_HEADER_HEIGHT - scroll + 7.0f + (entries.size + 1).toFloat() * (22.0f + 3.0f) + 7.0f + 7.0f
        return settingsContent.mouseReleased(mouseX, mouseY, button, panelX, settingsY, panelWidth)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return settingsContent.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(typedText: String): Boolean {
        return settingsContent.charTyped(typedText)
    }

    override fun hasActiveInput(): Boolean {
        return settingsContent.hasActiveInput()
    }

    private val autoHoverAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER)

    fun interface PanelVisibleResolver {
        fun getAsBoolean(panelId: String): Boolean
    }

    private class Entry(val labelSupplier: () -> String, val panelId: String, val action: Consumer<String>, val activeSupplier: BooleanSupplier) {
        val hoverAnim = Animation(Easing.EASE_OUT_CUBIC, OpaiTheme.ANIM_HOVER)
        val rippleAnim = Animation(Easing.EASE_OUT_CUBIC, 350L)
        var rippleActive = false
        fun isActive(): Boolean = activeSupplier.asBoolean
    }

}
