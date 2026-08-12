package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.component.*
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.widget.OpaiTextField
import net.ccbluex.liquidbounce.features.module.Category
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import java.io.IOException
import java.util.ArrayList

import net.ccbluex.liquidbounce.utils.render.InternalBlurShader
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.util.ResourceLocation

class OpaiScreen : GuiScreen() {

    private val panels = ArrayList<OpaiPanel>()
    private val renderer = OpaiRenderer()
    private val scrimAnim = Animation(Easing.EASE_OUT_SINE, 200L)
    private val searchField = OpaiTextField(64)
    private val backgroundLocation = ResourceLocation("liquidbounce/textures/background.png")

    private var initialized = false
    private var currentStyle: String? = null

    override fun initGui() {
        super.initGui()
        scrimAnim.setStartValue(0.0f)
        scrimAnim.run(0.0f)
        scrimAnim.run(1.0f)

        val style = OpaiGUI.style
        if (!initialized || currentStyle != style) {
            buildPanels()
            initialized = true
            currentStyle = style
        }

        for (panel in panels) {
            panel.setMaxPanelHeight(resolveMaxPanelHeight(panel))
            panel.startIntro()
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        updateDrag(mouseX, mouseY)
        MD3Theme.syncFromSettings()
        drawGui(mouseX, mouseY)
    }

    private fun updateDrag(mouseX: Int, mouseY: Int) {
        for (panel in panels) {
            if (!panel.isVisible()) continue
            panel.mouseDragged(mouseX, mouseY)
        }
    }

    private fun drawGui(mouseX: Int, mouseY: Int) {
        scrimAnim.run(1.0f)
        renderer.beginFrame()

        renderer.beginPass()
        renderer.rect().addRect(0f, 0f, width.toFloat(), height.toFloat(), Color(0, 0, 0, (80 * scrimAnim.getValue()).toInt()))
        renderer.flush()

        InternalBlurShader.blurArea(0f, 0f, width.toFloat(), height.toFloat(), OpaiGUI.backgroundBlur * scrimAnim.getValue())

        val shadowPad = OpaiTheme.PANEL_SHADOW_BLUR + 4.0f

        for (panel in panels) {
            if (!panel.isVisible()) continue
            val intro = panel.getIntroValue()
            if (intro < 0.001f) continue

            val slideOffset = (1.0f - intro) * 10.0f
            val origY = panel.getY()
            panel.setPosition(panel.getX(), origY - slideOffset)

            val panelH = panel.getPanelHeight()
            val revealedH = panelH * intro

            renderer.beginPass()
            renderer.setScissor(
                    panel.getX() - shadowPad, panel.getY() - shadowPad,
                    panel.getWidth() + shadowPad * 2, revealedH + shadowPad * 2,
                    height)
            panel.drawBackground(renderer)
            renderer.flush()
            renderer.clearScissor()

            val clipY = panel.getContentClipY()
            val clipH = panel.getContentClipHeight()
            val revealedBottom = panel.getY() + revealedH
            val actualClipH = Math.min(clipH, revealedBottom - clipY)
            val clipTop = clipY.coerceAtLeast(0f)
            val clipBottom = (clipY + actualClipH).coerceAtMost(height.toFloat())
            val clippedHeight = clipBottom - clipTop
            if (clippedHeight > 0.5f) {
                renderer.beginPass()
                renderer.setScissor(panel.getX(), clipTop, panel.getWidth(), clippedHeight, height)
                panel.drawContent(renderer, mouseX, mouseY)
                renderer.flush()
                renderer.clearScissor()
            }

            panel.setPosition(panel.getX(), origY)
        }

        drawSearch(mouseX, mouseY)
    }

    private fun drawSearch(mouseX: Int, mouseY: Int) {
        renderer.beginPass()
        val searchX = getSearchX()
        val searchY = getSearchY()
        searchField.draw(renderer, searchX, searchY, getSearchWidth(), getSearchHeight(), mouseX, mouseY, "Search...", 0.66f)
        renderer.flush()
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)

        if (mouseButton == 0 && searchField.focusIfContains(mouseX.toDouble(), mouseY.toDouble(), getSearchX(), getSearchY(), getSearchWidth(), getSearchHeight())) {
            return
        } else if (mouseButton == 0 && searchField.focused) {
            searchField.blur()
        }

        for (i in panels.size - 1 downTo 0) {
            val panel = panels[i]
            if (!panel.isVisible()) continue
            if (panel.mouseClicked(mouseX, mouseY, mouseButton)) {
                OpaiLayoutState.save(panels)
                return
            }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        super.mouseReleased(mouseX, mouseY, state)
        for (panel in panels) {
            if (!panel.isVisible()) continue
            if (panel.mouseReleased(mouseX, mouseY, state)) {
                OpaiLayoutState.save(panels)
                return
            }
        }
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
        for (panel in panels) {
            if (!panel.isVisible()) continue
            panel.mouseDragged(mouseX, mouseY)
        }
        OpaiLayoutState.save(panels)
    }

    @Throws(IOException::class)
    override fun handleMouseInput() {
        super.handleMouseInput()
        val wheel = Mouse.getEventDWheel()
        if (wheel != 0) {
            val sr = net.minecraft.client.gui.ScaledResolution(net.minecraft.client.Minecraft.getMinecraft())
            val mouseX = Mouse.getEventX() * this.width / net.minecraft.client.Minecraft.getMinecraft().displayWidth
            val mouseY = this.height - Mouse.getEventY() * this.height / net.minecraft.client.Minecraft.getMinecraft().displayHeight - 1
            for (panel in panels) {
                if (!panel.isVisible()) continue
                if (panel.mouseScrolled(mouseX, mouseY, (wheel / 120.0f))) {
                    return
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (searchField.focused) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchField.blur()
                return
            }
            if (searchField.keyPressed(keyCode)) {
                syncSearchQuery()
                return
            }
        }

        val hasActiveInput = panels.filter { it.isVisible() }.any { it.hasActiveInput() }

        if (hasActiveInput) {
            for (panel in panels) {
                if (!panel.isVisible()) continue
                if (panel.keyPressed(keyCode, 0, 0)) {
                    return
                }
            }
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(null)
            return
        }

        for (panel in panels) {
            if (!panel.isVisible()) continue
            if (panel.keyPressed(keyCode, 0, 0)) {
                return
            }
        }
        
        if (searchField.charTyped(typedChar)) {
            syncSearchQuery()
            return
        }
        for (panel in panels) {
            if (!panel.isVisible()) continue
            if (panel.charTyped(typedChar.toString())) {
                return
            }
        }
    }

    override fun onGuiClosed() {
        OpaiCompat.IMEFocusHelper.deactivate()
        OpaiLayoutState.save(panels)
        super.onGuiClosed()
    }

    override fun doesGuiPauseGame(): Boolean = false

    private fun buildPanels() {
        panels.clear()
        
        if (OpaiGUI.style == "Style2") {
            buildStyle2Panels()
        } else {
            buildStyle1Panels()
            OpaiLayoutState.load(panels)
        }
    }
    
    private fun buildStyle1Panels() {
        var index = 0
        val mainPanel = MainOpaiPanel(index++, { handleMainPanelAction(it) }, { anySubPanelVisible() }, { isPanelVisible(it) }, { autoArrangePanels() })
        mainPanel.setPosition(OpaiTheme.PANEL_MARGIN_X, OpaiTheme.PANEL_MARGIN_Y)
        panels.add(mainPanel)

        val x = OpaiTheme.PANEL_MARGIN_X + mainPanel.getWidth() + OpaiTheme.PANEL_GAP
        var y = OpaiTheme.PANEL_MARGIN_Y
        for (category in Category.values()) {
            panels.add(createSubPanel(CategoryOpaiPanel(category, index++), x, y))
            y += OpaiTheme.PANEL_HEADER_HEIGHT + OpaiTheme.PANEL_GAP
        }
    }
    
    private fun buildStyle2Panels() {
        var index = 0
        val panelGap = OpaiGUI.panelGap
        val panelWidth = OpaiTheme.PANEL_WIDTH
        
        val categories = Category.values()
        val totalWidth = categories.size * panelWidth + (categories.size - 1) * panelGap
        var x = (width - totalWidth) / 2f
        val y = OpaiTheme.PANEL_MARGIN_Y
        
        for (category in categories) {
            val categoryPanel = CategoryOpaiPanel(category, index++)
            categoryPanel.setPosition(x, y)
            categoryPanel.setVisible(true)
            categoryPanel.setOpened(true)
            panels.add(categoryPanel)
            x += panelWidth + panelGap
        }
    }

    private fun autoArrangePanels() {
        val mainPanel = panels.find { it.getId() == "main" } ?: return
        val x = mainPanel.getX() + mainPanel.getWidth() + OpaiTheme.PANEL_GAP
        var y = OpaiTheme.PANEL_MARGIN_Y
        for (panel in panels) {
            if (panel.getId() == "main") continue
            panel.setPosition(x, y)
            panel.setVisible(true)
            panel.setOpened(false)
            y += OpaiTheme.PANEL_HEADER_HEIGHT + OpaiTheme.PANEL_GAP
        }
        OpaiLayoutState.save(panels)
    }

    private fun createSubPanel(panel: OpaiPanel, x: Float, y: Float): OpaiPanel {
        panel.setPosition(x, y)
        panel.setVisible(false)
        panel.setOpened(false)
        return panel
    }

    private fun handleMainPanelAction(panelId: String) {
        if ("__collapse_all__" == panelId) {
            for (panel in panels) {
                if ("main" != panel.getId()) {
                    panel.setVisible(false)
                    panel.setOpened(false)
                }
            }
            OpaiLayoutState.save(panels)
            return
        }

        for (panel in panels) {
            if (panel.getId() == panelId) {
                val nextVisible = !panel.isVisible()
                panel.setVisible(nextVisible)
                panel.setOpened(false)
                OpaiLayoutState.save(panels)
                return
            }
        }
    }

    private fun anySubPanelVisible(): Boolean {
        return panels.any { "main" != it.getId() && it.isVisible() }
    }

    private fun isPanelVisible(panelId: String): Boolean {
        return panels.any { it.getId() == panelId && it.isVisible() }
    }

    private fun resolveMaxPanelHeight(panel: OpaiPanel): Float {
        val screenLimited = height * 0.72f
        return when (panel.getId()) {
            "main" -> Math.min(screenLimited, 360.0f)
            else -> Math.min(screenLimited, 350.0f)
        }
    }

    private fun syncSearchQuery() {
        val query = searchField.text
        for (panel in panels) {
            if (panel is CategoryOpaiPanel) {
                panel.setSearchQuery(query)
            }
        }
    }

    private fun getSearchX(): Float = OpaiTheme.PANEL_MARGIN_X

    private fun getSearchY(): Float = height - OpaiTheme.PANEL_MARGIN_Y - getSearchHeight()

    private fun getSearchWidth(): Float = Math.min(200.0f, Math.max(140.0f, width - OpaiTheme.PANEL_MARGIN_X * 2.0f))

    private fun getSearchHeight(): Float = 20.0f

    companion object {
        val INSTANCE = OpaiScreen()
    }
}
