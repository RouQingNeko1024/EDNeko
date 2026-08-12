/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * skid FDP Client
 * https://github.com/SkidderMC/FDPClient
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RoundedUtil
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

@ElementInfo(name = "Watermark", single = true)
class Watermark(
    x: Double = 0.0, y: Double = 0.0, scale: Float = 1F,
    side: Side = Side(Side.Horizontal.LEFT, Side.Vertical.UP),
) : Element("Watermark", x, y, scale, side) {

    private val mode by choices("Mode", arrayOf("Blur", "ZAVZ", "edneko"), "edneko")
    private val colorMode by choices("Color", arrayOf("Custom", "Theme"), "Theme")
    private val customColor by color("CustomColor", Color(24, 114, 165))

    private val dateFormat = SimpleDateFormat("HH:mm")

    override fun drawElement(): Border {
        assumeNonVolatile {
            val name = "edneko"
            
            when (mode) {
                "Blur" -> drawBlurStyle(name)
                "ZAVZ" -> drawZavzStyle(name)
                "edneko" -> drawednekoStyle(name)
            }
        }
        
        return calculateBorder()
    }

    private fun calculateBorder(): Border {
        val name = "edneko"
        val text = EnumChatFormatting.DARK_GRAY.toString() + "   |  " + EnumChatFormatting.WHITE + mc.thePlayer.name + EnumChatFormatting.DARK_GRAY + "  |  " + EnumChatFormatting.WHITE + mc.thePlayer.getPing() +
                "ms" + EnumChatFormatting.DARK_GRAY + "  |  " + EnumChatFormatting.WHITE + dateFormat.format(Date()) + EnumChatFormatting.DARK_GRAY + "  |  " + EnumChatFormatting.WHITE

        val width = 5f + Fonts.fontSF35.getStringWidth(text) + Fonts.fontSF35.getStringWidth(name)
        val height = 20f
        
        return Border(0f, 0f, width, height)
    }

    private fun drawBlurStyle(name: String) {
        val text =
            EnumChatFormatting.DARK_GRAY.toString() + "   |  " + EnumChatFormatting.WHITE + mc.thePlayer.name + EnumChatFormatting.DARK_GRAY + "  |  " + EnumChatFormatting.WHITE + mc.thePlayer.getPing() +
                    "ms" + EnumChatFormatting.DARK_GRAY + "  |  " + EnumChatFormatting.WHITE + dateFormat.format(
                Date()
            ) + EnumChatFormatting.DARK_GRAY + "  |  " + EnumChatFormatting.WHITE

        val width = 5f + Fonts.fontSF35.getStringWidth(text) + Fonts.fontSF35.getStringWidth(name)
        val height = 20f

        glPushMatrix()
        glTranslated(-renderX, -renderY, 0.0)
        glScalef(1F / scale, 1F / scale, 1F)
        BlurUtils.blurAreaRounded(
            renderX.toFloat(), renderY.toFloat(),
            renderX.toFloat() + width, renderY.toFloat() + height,
            5.4f, 10f
        )
        glPopMatrix()

        RenderUtils.drawRoundedRect(0f, 0f, width, height, Color(0, 0, 0, 100).rgb, 5.4f)
        
        Fonts.fontSF35.drawString(" $name", 4f, 5f, getColor().rgb)
        Fonts.fontSF35.drawString(text, 3 + Fonts.fontSF35.getStringWidth(name), 6, Color.WHITE.rgb)
    }

    private fun drawZavzStyle(name: String) {
        val username = mc.thePlayer.name
        val servername = if (mc.isSingleplayer) "Singleplayer" else mc.currentServerData?.serverIP ?: "Unknown"
        val times = dateFormat.format(Date())
        
        val width = (Fonts.fontSF35.getStringWidth(name) + Fonts.font35.getStringWidth(
            " | $username | $servername | $times"
        ) + 3 + 5).toFloat()
        val height = 12f
        
        RoundedUtil.drawRound(0f, 0f, width, height, 1f, Color(0, 0, 0, 100))
        Fonts.fontSF35.drawString(name, 3f, 2f, getColor().rgb)
        Fonts.fontSF35.drawString(name, 2f, 2f, -1)
        Fonts.font35.drawString(
            " | $username | $servername | $times",
            Fonts.fontSF35.getStringWidth(name) + 5f,
            3f,
            -1
        )
    }

    private fun drawednekoStyle(name: String) {
        val text =
            EnumChatFormatting.DARK_GRAY.toString() + "   |  " + EnumChatFormatting.WHITE + mc.thePlayer.name + EnumChatFormatting.DARK_GRAY + "  |  " + EnumChatFormatting.WHITE + mc.thePlayer.getPing() +
                    "ms" + EnumChatFormatting.DARK_GRAY + "  |  " + EnumChatFormatting.WHITE + dateFormat.format(
                Date()
            ) + EnumChatFormatting.DARK_GRAY + "  |  " + EnumChatFormatting.WHITE

        val width = 5f + Fonts.fontSF35.getStringWidth(text) + Fonts.fontSF35.getStringWidth(name)
        val height = 20f

        glPushMatrix()
        glTranslated(-renderX, -renderY, 0.0)
        glScalef(1F / scale, 1F / scale, 1F)
        BlurUtils.blurAreaRounded(
            renderX.toFloat(), renderY.toFloat(),
            renderX.toFloat() + width, renderY.toFloat() + height,
            5.4f, 10f
        )
        glPopMatrix()

        RenderUtils.drawRoundedRect(0f, 0f, width, height, Color(0, 0, 0, 100).rgb, 5.4f)

        Fonts.fontSF35.drawStringWithShadow(" $name", 2f, 6f, getColor().rgb)
        Fonts.fontSF35.drawString(text, 1 + Fonts.fontSF35.getStringWidth(name), 6, Color.WHITE.rgb)
    }

    private fun getColor(): Color {
        return when (colorMode) {
            "Theme" -> ClientThemesUtils.getColor(1)
            else -> customColor
        }
    }
}
