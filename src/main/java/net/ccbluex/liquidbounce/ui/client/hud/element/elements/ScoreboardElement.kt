package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_WEBSITE
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.Gui
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import java.awt.Color

@ElementInfo(name = "Scoreboard")
class ScoreboardElement(
    x: Double = 5.0, y: Double = 0.0, scale: Float = 1F,
    side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.MIDDLE)
) : Element("Scoreboard", x, y, scale, side) {

    private val backgroundColorMode by choices("Background-ColorMode", arrayOf("Custom", "Theme"), "Custom")
    private val backgroundColorRed by int("Background-R", 0, 0..255) { backgroundColorMode == "Custom" }
    private val backgroundColorGreen by int("Background-G", 0, 0..255) { backgroundColorMode == "Custom" }
    private val backgroundColorBlue by int("Background-B", 0, 0..255) { backgroundColorMode == "Custom" }
    private val backgroundColorAlpha by int("Background-Alpha", 95, 0..255)

    private val gradientBackground by boolean("GradientBackground", false)
    private val leftColor by color("LeftColor", Color(0, 0, 0, 50)) { gradientBackground }
    private val rightColor by color("RightColor", Color(0, 0, 0, 200)) { gradientBackground }
    private val gradientDirection by choices("GradientDirection", arrayOf("LeftToRight", "RightToLeft", "TopToBottom", "BottomToTop"), "LeftToRight") { gradientBackground }

    private val rect by boolean("Rect", false)
    private val rectColorMode by choices("Rect-ColorMode", arrayOf("Custom", "Theme"), "Custom") { rect }
    private val rectHeight by int("Rect-Height", 1, 1..10) { rect }
    private val rectRed by int("Rect-R", 0, 0..255) { rect && rectColorMode == "Custom" }
    private val rectGreen by int("Rect-G", 111, 0..255) { rect && rectColorMode == "Custom" }
    private val rectBlue by int("Rect-B", 255, 0..255) { rect && rectColorMode == "Custom" }
    private val rectAlpha by int("Rect-Alpha", 255, 0..255) { rect }

    private val blur by boolean("Blur", false)
    private val blurStrength by float("Blur-Strength", 10F, 1F..50F) { blur }

    private val enableGlass by boolean("EnableGlass", false)
    private val enableNeon by boolean("EnableNeon", false)
    
    private val neonColor by color("NeonColor", Color(0, 255, 255)) { enableNeon }
    private val neonOuterGlowAlpha by int("Neon-OuterGlowAlpha", 50, 10..255) { enableNeon }
    private val neonInnerBorderAlpha by int("Neon-InnerBorderAlpha", 180, 50..255) { enableNeon }
    private val neonBackgroundAlpha by int("Neon-BackgroundAlpha", 80, 0..255) { enableNeon }
    
    private val glassBaseColor by color("Glass-BaseColor", Color(200, 220, 255, 15)) { enableGlass }
    private val glassHighlightAlpha by int("Glass-HighlightAlpha", 20, 0..100) { enableGlass }
    private val glassBorderColor by color("Glass-BorderColor", Color(255, 255, 255, 40)) { enableGlass }

    private val rounded by boolean("Rounded", false)
    private val roundedRadius by float("Rounded-Radius", 5F, 0F..30F) { rounded }

    private val serverIp by choices("ServerIP", arrayOf("Normal", "None", "Client", "Website", "qwq", "Cat", "Styles", "Flux", "FDP", "Senrenbanka"), "Client")

    private val textShadow by boolean("TextShadow", false)
    private val showNumbers by boolean("ShowNumbers", false)
    private val font by font("Font", Fonts.minecraftFont)

    private val domainRegex =
        Regex("\\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}\\b|欧|OMG|onmygod|@|工作室|KKC|Domcer|network|kkc|weipu|布吉岛|像素未来|国际版|庭", RegexOption.IGNORE_CASE)

    private fun drawGradientBackground(minX: Float, minY: Float, maxX: Float, maxY: Float) {
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glPushMatrix()

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glShadeModel(GL_SMOOTH)

        glBegin(GL_QUADS)

        when (gradientDirection) {
            "LeftToRight" -> {
                glColor4f(leftColor.red / 255f, leftColor.green / 255f, leftColor.blue / 255f, leftColor.alpha / 255f)
                glVertex2f(minX, minY)
                glVertex2f(minX, maxY)
                glColor4f(rightColor.red / 255f, rightColor.green / 255f, rightColor.blue / 255f, rightColor.alpha / 255f)
                glVertex2f(maxX, maxY)
                glVertex2f(maxX, minY)
            }
            "RightToLeft" -> {
                glColor4f(rightColor.red / 255f, rightColor.green / 255f, rightColor.blue / 255f, rightColor.alpha / 255f)
                glVertex2f(minX, minY)
                glVertex2f(minX, maxY)
                glColor4f(leftColor.red / 255f, leftColor.green / 255f, leftColor.blue / 255f, leftColor.alpha / 255f)
                glVertex2f(maxX, maxY)
                glVertex2f(maxX, minY)
            }
            "TopToBottom" -> {
                glColor4f(leftColor.red / 255f, leftColor.green / 255f, leftColor.blue / 255f, leftColor.alpha / 255f)
                glVertex2f(minX, minY)
                glVertex2f(maxX, minY)
                glColor4f(rightColor.red / 255f, rightColor.green / 255f, rightColor.blue / 255f, rightColor.alpha / 255f)
                glVertex2f(maxX, maxY)
                glVertex2f(minX, maxY)
            }
            "BottomToTop" -> {
                glColor4f(rightColor.red / 255f, rightColor.green / 255f, rightColor.blue / 255f, rightColor.alpha / 255f)
                glVertex2f(minX, minY)
                glVertex2f(maxX, minY)
                glColor4f(leftColor.red / 255f, leftColor.green / 255f, leftColor.blue / 255f, leftColor.alpha / 255f)
                glVertex2f(maxX, maxY)
                glVertex2f(minX, maxY)
            }
        }

        glEnd()

        glShadeModel(GL_FLAT)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)

        glPopMatrix()
        glPopAttrib()
    }

    private fun transformServerIp(name: String): String {
        if (serverIp == "Normal") return name

        try {
            val nameWithoutFormatting = name
                .replace(EnumChatFormatting.RESET.toString(), "")
                .replace(Regex("[\u00a7&][0-9a-fk-or]"), "")
                .trim()
            val trimmedServerIP = mc.currentServerData?.serverIP?.trim()?.lowercase() ?: ""
            val containsDomain = domainRegex.containsMatchIn(nameWithoutFormatting)

            if (nameWithoutFormatting.lowercase() == trimmedServerIP || containsDomain) {
                val colorCode = name.substring(0, minOf(2, name.length))
                return when (serverIp.lowercase()) {
                    "none" -> ""
                    "client" -> "$colorCode$CLIENT_NAME"
                    "website" -> "$colorCode$CLIENT_WEBSITE"
                    "qwq" -> "$colorCode§b猫娘客户端"
                    "cat" -> "$colorCode§b柔情猫娘"
                    "styles" -> "$colorCode§bstyle.wtf"
                    "flux" -> "$colorCode§bflux.today"
                    "fdp" -> "$colorCode§7[§b§lFDPClient§r§7]"
                    "senrenbanka" -> "$colorCode§d§lSenrenbanka.wtf"
                    else -> name
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error while transforming Scoreboard Server IP", e)
        }

        return name
    }

    override fun drawElement(): Border? {
        val fontRenderer = font
        val backColor = when (backgroundColorMode) {
            "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor().rgb
            else -> Color(backgroundColorRed, backgroundColorGreen, backgroundColorBlue, backgroundColorAlpha).rgb
        }
        val rectColor = when (rectColorMode) {
            "Theme" -> net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColor().rgb
            else -> Color(rectRed, rectGreen, rectBlue, rectAlpha).rgb
        }

        val worldScoreboard = mc.theWorld.scoreboard
        var currObjective: ScoreObjective? = null
        val playerTeam = worldScoreboard.getPlayersTeam(mc.thePlayer.name)

        if (playerTeam != null) {
            val colorIndex = playerTeam.chatFormat.colorIndex
            if (colorIndex >= 0)
                currObjective = worldScoreboard.getObjectiveInDisplaySlot(3 + colorIndex)
        }

        val objective = currObjective ?: worldScoreboard.getObjectiveInDisplaySlot(1) ?: return null

        val scoreboard = objective.scoreboard
        var scoreCollection = scoreboard.getSortedScores(objective)
        val scores = Lists.newArrayList(Iterables.filter(scoreCollection) { input ->
            input?.playerName != null && !input.playerName.startsWith("#")
        })

        scoreCollection = if (scores.size > 15)
            Lists.newArrayList(Iterables.skip(scores, scoreCollection.size - 15))
        else
            scores

        var maxWidth = fontRenderer.getStringWidth(objective.displayName)

        for (score in scoreCollection) {
            val scorePlayerTeam = scoreboard.getPlayersTeam(score.playerName)
            val name = ScorePlayerTeam.formatPlayerName(scorePlayerTeam, score.playerName)
            val width = if (showNumbers) "$name: ${EnumChatFormatting.RED}${score.scorePoints}" else name
            maxWidth = maxWidth.coerceAtLeast(fontRenderer.getStringWidth(width))
        }

        val fontHeight = fontRenderer.FONT_HEIGHT
        val maxHeight = scoreCollection.size * fontHeight
        val l1 = if (side.horizontal == Side.Horizontal.LEFT) maxWidth + 3 else -maxWidth - 3

        if (scoreCollection.isNotEmpty()) {
            val bgMinX = if (side.horizontal == Side.Horizontal.LEFT) (l1 + 2).toFloat() else (l1 - 2).toFloat()
            val bgMinY = if (rect) -2F - rectHeight else -2F
            val bgMaxX = if (side.horizontal == Side.Horizontal.LEFT) -5F else 5F
            val bgMaxY = (maxHeight + fontHeight).toFloat()

            if (enableNeon) {
                GL11.glTranslated(-renderX, -renderY, 0.0)
                GL11.glScalef(1F, 1F, 1F)
                GL11.glPushMatrix()
                RenderUtils.drawNeonBorder(
                    renderX.toFloat() + bgMinX * scale,
                    renderY.toFloat() + bgMinY * scale,
                    (bgMaxX - bgMinX) * scale,
                    (bgMaxY - bgMinY) * scale,
                    roundedRadius, neonColor, blurStrength,
                    neonOuterGlowAlpha, neonInnerBorderAlpha, neonBackgroundAlpha
                )
                GL11.glPopMatrix()
                GL11.glScalef(scale, scale, scale)
                GL11.glTranslated(renderX, renderY, 0.0)
            } else if (enableGlass) {
                GL11.glTranslated(-renderX, -renderY, 0.0)
                GL11.glScalef(1F, 1F, 1F)
                GL11.glPushMatrix()
                RenderUtils.drawGlassmorphism(
                    renderX.toFloat() + bgMinX * scale,
                    renderY.toFloat() + bgMinY * scale,
                    (bgMaxX - bgMinX) * scale,
                    (bgMaxY - bgMinY) * scale,
                    roundedRadius, blurStrength,
                    glassBaseColor, glassHighlightAlpha, glassBorderColor
                )
                GL11.glPopMatrix()
                GL11.glScalef(scale, scale, scale)
                GL11.glTranslated(renderX, renderY, 0.0)
            } else if (blur) {
                GL11.glTranslated(-renderX, -renderY, 0.0)
                GL11.glScalef(1F, 1F, 1F)
                GL11.glPushMatrix()

                if (rounded) {
                    BlurUtils.blurAreaRounded(
                        renderX.toFloat() + bgMinX * scale,
                        renderY.toFloat() + bgMinY * scale,
                        renderX.toFloat() + bgMaxX * scale,
                        renderY.toFloat() + bgMaxY * scale,
                        roundedRadius,
                        blurStrength
                    )
                } else {
                    BlurUtils.blurArea(
                        renderX.toFloat() + bgMinX * scale,
                        renderY.toFloat() + bgMinY * scale,
                        renderX.toFloat() + bgMaxX * scale,
                        renderY.toFloat() + bgMaxY * scale,
                        blurStrength
                    )
                }

                GL11.glPopMatrix()
                GL11.glScalef(scale, scale, scale)
                GL11.glTranslated(renderX, renderY, 0.0)
            }

            if (gradientBackground) {
                drawGradientBackground(bgMinX, bgMinY, bgMaxX, bgMaxY)
            } else if (rounded) {
                RenderUtils.drawRoundedRect(
                    bgMinX, bgMinY, bgMaxX, bgMaxY,
                    backColor,
                    roundedRadius
                )
            } else {
                Gui.drawRect(bgMinX.toInt(), bgMinY.toInt(), bgMaxX.toInt(), bgMaxY.toInt(), backColor)
            }

            if (rect) {
                if (rounded) {
                    RenderUtils.drawRoundedRect(
                        bgMinX, -2F - rectHeight, bgMaxX, -2F,
                        rectColor,
                        roundedRadius
                    )
                } else {
                    Gui.drawRect(bgMinX.toInt(), -2 - rectHeight, bgMaxX.toInt(), -2, rectColor)
                }
            }
        }

        scoreCollection.forEachIndexed { index, score ->
            val team = scoreboard.getPlayersTeam(score.playerName)
            var name = ScorePlayerTeam.formatPlayerName(team, score.playerName)
            val scorePoints = "${EnumChatFormatting.RED}${score.scorePoints}"

            name = transformServerIp(name)

            val height = maxHeight - index * fontHeight

            val textX = if (side.horizontal == Side.Horizontal.LEFT) (l1 + 2).toFloat() else (l1 - 2).toFloat()
            fontRenderer.drawString(name, textX, height.toFloat(), 0xFFFFFF, textShadow)

            if (showNumbers) {
                val numberX = if (side.horizontal == Side.Horizontal.LEFT) -5 else 5
                fontRenderer.drawString(
                    scorePoints,
                    (numberX - fontRenderer.getStringWidth(scorePoints)).toFloat(),
                    height.toFloat(),
                    0xFFFFFF,
                    textShadow
                )
            }

            if (index == scoreCollection.size - 1) {
                val title = transformServerIp(objective.displayName)
                val titleX = if (side.horizontal == Side.Horizontal.LEFT) {
                    (l1 + 2 + (maxWidth - fontRenderer.getStringWidth(title)) / 2).toFloat()
                } else {
                    (l1 - 2 + (maxWidth - fontRenderer.getStringWidth(title)) / 2).toFloat()
                }
                fontRenderer.drawString(title, titleX, (height - fontHeight).toFloat(), 0xFFFFFF, textShadow)
            }
        }

        return Border(
            if (side.horizontal == Side.Horizontal.LEFT) (l1 + 2).toFloat() else (l1 - 2).toFloat(),
            if (rect) -2F - rectHeight else -2F,
            if (side.horizontal == Side.Horizontal.LEFT) -5F else 5F,
            maxHeight + fontHeight.toFloat()
        )
    }
}