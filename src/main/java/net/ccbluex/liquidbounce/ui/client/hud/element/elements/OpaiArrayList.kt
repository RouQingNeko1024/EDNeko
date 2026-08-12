package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.EmbeddedStencil
import net.ccbluex.liquidbounce.utils.render.InternalBlurShader
import net.ccbluex.liquidbounce.utils.render.RenderUtils.RoundedCorners
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import org.lwjgl.opengl.GL11
import java.awt.Color

@ElementInfo(name = "OpaiArrayList", single = true)
class OpaiArrayList(
    x: Double = 0.0, y: Double = 0.0, scale: Float = 1F,
    side: Side = Side(Horizontal.RIGHT, Vertical.UP),
) : Element("OpaiArrayList", x, y, scale, side) {

    private val themeColor by color("ThemeColor", Color(65, 130, 225))
    private val backgroundAlpha by int("BackgroundAlpha", 120, 0..255)
    private val blur by boolean("Blur", true)
    private val blurRadius by float("BlurStrength", 10f, 1f..50f)
    private val maxRadius by float("MaxRadius", 5f, 2f..12f)
    private val stripeWidth by float("StripeWidth", 3f, 1f..8f)
    private val hideRender by boolean("HideRender", true)
    private val showTags by boolean("ShowTags", true)
    private val font by font("Font", Fonts.fontSF35)
    private val textHeight by float("TextHeight", 11F, 1F..20F)
    private val textY by float("TextY", 3.25F, 0F..20F)
    private val space by float("Space", 1F, 0F..5F)
    private val animationSpeed by float("AnimationSpeed", 0.2F, 0.01F..1F)

    private var modules = emptyList<Module>()

    private data class ItemInfo(
        val mod: Module,
        val bgLeft: Float,
        val bgTop: Float,
        val bgRight: Float,
        val bgBottom: Float,
        val stripeX: Float,
        val textX: Float,
        val textY: Float,
        val radius: Float,
        val corners: RoundedCorners
    )

    override fun drawElement(): Border? {
        assumeNonVolatile {
            val activeModules = moduleManager.filter { 
                it.state && !it.isHidden && (it.category != Category.RENDER || !hideRender) 
            }
            
            if (activeModules.isEmpty()) {
                if (mc.currentScreen is GuiHudDesigner) {
                    return if (side.horizontal == Horizontal.LEFT) Border(0F, -1F, 20F, 20F)
                    else Border(0F, -1F, -20F, 20F)
                }
                return null
            }

            val sorted = activeModules.sortedByDescending { mod ->
                val nameW = font.getStringWidth(mod.getName(false)).toFloat()
                val tagW = if (showTags && mod.tag != null) font.getStringWidth(" ${mod.tag}").toFloat() else 0f
                nameW + tagW
            }

            val stripeW = stripeWidth
            val elemPadding = 5f
            val bgPadding = 12f
            val elemH = font.FONT_HEIGHT + elemPadding * 2
            val textSpacer = textHeight + space

            val widths = sorted.map { mod ->
                val nameW = font.getStringWidth(mod.getName(false)).toFloat()
                val tagW = if (showTags && mod.tag != null) font.getStringWidth(" ${mod.tag}").toFloat() else 0f
                nameW + tagW
            }

            val items = sorted.mapIndexed { idx, mod ->
                val textW = widths[idx]
                val yPos = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * if (side.vertical == Vertical.DOWN) idx + 1 else idx
                
                val animY = AnimationUtil.base(mod.yAnim.toDouble(), yPos.toDouble(), animationSpeed.toDouble()).toFloat()
                mod.yAnim = animY
                
                val elemY = animY
                val elemX = -stripeW
                val bgLeft = elemX - textW - bgPadding
                val bgRight = elemX + stripeW

                val diff = if (idx + 1 < widths.size) textW - widths[idx + 1] else maxRadius
                val radius = diff.coerceIn(0f, maxRadius).coerceAtMost(elemH / 2f)

                val corners = RoundedCorners.BOTTOM_LEFT_ONLY
                ItemInfo(mod, bgLeft, elemY, bgRight, elemY + elemH, elemX, bgLeft + bgPadding / 2, elemY + elemPadding, radius, corners)
            }

            if (blur) {
                GL11.glPushMatrix()
                GL11.glTranslated(-renderX, -renderY, 0.0)
                GL11.glScalef(1F / scale, 1F / scale, 1F)

                try {
                    EmbeddedStencil.checkSetupFBO(mc.framebuffer)
                    EmbeddedStencil.write(false)
                    for (item in items) {
                        drawRoundedRect(item.bgLeft, item.bgTop, item.bgRight, item.bgBottom, Color.WHITE.rgb, item.radius, item.corners)
                    }
                    EmbeddedStencil.erase(true)
                    val blurR = blurRadius * (backgroundAlpha / 255f)
                    val listLeft = items.minOf { it.bgLeft }
                    val listTop = items.first().bgTop
                    val listRight = items.first().bgRight
                    val listBottom = items.last().bgBottom
                    InternalBlurShader.blurArea(listLeft, listTop, listRight - listLeft, listBottom - listTop, blurR)
                    EmbeddedStencil.dispose()
                } catch (_: Exception) {}

                GL11.glScalef(scale, scale, scale)
                GL11.glTranslated(renderX, renderY, 0.0)
                GL11.glPopMatrix()
            }

            val bgColor = Color(0, 0, 0, backgroundAlpha).rgb
            for (item in items) {
                drawRoundedRect(item.bgLeft, item.bgTop, item.bgRight, item.bgBottom, bgColor, item.radius, item.corners)
                drawRoundedRect(item.stripeX, item.bgTop, item.stripeX + stripeW, item.bgBottom, themeColor.rgb, item.radius, item.corners)
                font.drawString(item.mod.getName(false), item.textX, item.textY, Color.WHITE.rgb, false)
                if (showTags && item.mod.tag != null) {
                    val tagText = " ${item.mod.tag}"
                    font.drawString(tagText, item.textX + font.getStringWidth(item.mod.getName(false)), item.textY, Color(128, 128, 128).rgb, false)
                }
            }

            if (mc.currentScreen is GuiHudDesigner) {
                val x2 = items.minOf { it.bgLeft }.toInt()
                val y2 = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * items.size
                return Border(0F, 0F, x2 - 7F, y2 - if (side.vertical == Vertical.DOWN) 1F else 0F)
            }
        }
        return null
    }

    override fun updateElement() {
        modules = moduleManager.filter { it.state && !it.isHidden && (it.category != Category.RENDER || !hideRender) }
            .sortedByDescending { mod ->
                val nameW = font.getStringWidth(mod.getName(false)).toFloat()
                val tagW = if (showTags && mod.tag != null) font.getStringWidth(" ${mod.tag}").toFloat() else 0f
                nameW + tagW
            }
    }
}
