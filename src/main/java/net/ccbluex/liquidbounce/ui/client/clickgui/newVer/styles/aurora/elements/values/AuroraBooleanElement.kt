/*
 * Air Client
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.elements.values

import net.ccbluex.liquidbounce.config.BoolValue
import net.ccbluex.liquidbounce.ui.client.clickgui.newVer.styles.aurora.AuroraGUI
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.opengl.GL11.*
import java.awt.Color

class AuroraBooleanElement(value: BoolValue) : AuroraValueElement<Boolean>(value) {
    private var animState = 0F

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, accentColor: Color): Float {
        val boolValue = value as BoolValue
        val state = boolValue.get()

        val targetState = if (state) 1F else 0F
        animState += (targetState - animState) * 0.15F

        Fonts.font35.drawString(boolValue.name, x, y + 8F, Color.WHITE.rgb)

        val toggleX = x + width - 40F
        val toggleY = y + 5F
        val toggleWidth = 35F
        val toggleHeight = 15F
        val handleRadius = 5.5F
        val padding = 2F

        val offColor = Color(50, 50, 55)
        val onColor = AuroraGUI.getAuroraColor()
        val bgColor = Color(
            (offColor.red + (onColor.red - offColor.red) * animState).toInt(),
            (offColor.green + (onColor.green - offColor.green) * animState).toInt(),
            (offColor.blue + (onColor.blue - offColor.blue) * animState).toInt(),
            (offColor.alpha + (180 - offColor.alpha) * animState).toInt()
        )

        RenderUtils.drawRoundedRect2(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, bgColor, toggleHeight / 2F)

        val handleTravel = toggleWidth - padding * 2F - handleRadius * 2F
        val handleCenterX = toggleX + padding + handleRadius + animState * handleTravel
        val handleCenterY = toggleY + toggleHeight / 2F

        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)

        glColor4f(1F, 1F, 1F, 1F)
        glBegin(GL_TRIANGLE_FAN)
        glVertex2f(handleCenterX, handleCenterY)
        for (i in 0..360) {
            val angle = i * Math.PI / 180.0
            glVertex2f(
                handleCenterX + (Math.sin(angle) * handleRadius).toFloat(),
                handleCenterY + (Math.cos(angle) * handleRadius).toFloat()
            )
        }
        glEnd()

        glEnable(GL_TEXTURE_2D)
        glDisable(GL_LINE_SMOOTH)
        glDisable(GL_BLEND)
        glPopMatrix()

        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float): Boolean {
        val boolValue = value as BoolValue
        val toggleX = x + width - 40F
        val toggleY = y + 5F

        if (mouseX >= toggleX && mouseX <= toggleX + 35F && mouseY >= toggleY && mouseY <= toggleY + 15F) {
            boolValue.toggle()
            return true
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + valueHeight) {
            boolValue.toggle()
            return true
        }

        return false
    }
}
