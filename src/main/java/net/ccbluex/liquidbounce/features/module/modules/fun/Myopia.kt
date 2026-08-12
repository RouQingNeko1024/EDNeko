/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11.*

object Myopia : Module("Myopia", Category.FUN, gameDetecting = false) {

    private val blurStrength by float("BlurStrength", 10f, 1f..50f)
    private val blurMode by choices("BlurMode", arrayOf("Full", "Center", "Edges"), "Full")
    private val centerSize by float("CenterSize", 50f, 10f..200f) { blurMode == "Center" || blurMode == "Edges" }
    private val fadeEdge by float("FadeEdge", 20f, 0f..50f) { blurMode == "Center" || blurMode == "Edges" }

    val onRender2D = handler<Render2DEvent> {
        try {
            val mcInstance = mc ?: return@handler
            val sr = ScaledResolution(mcInstance)
            val width = sr.scaledWidth.toFloat()
            val height = sr.scaledHeight.toFloat()
            val centerX = width / 2
            val centerY = height / 2
            
            when (blurMode) {
                "Full" -> {
                    BlurUtils.blurArea(0f, 0f, width, height, blurStrength)
                }
                "Center" -> {
                    val halfSize = centerSize
                    BlurUtils.blurArea(
                        centerX - halfSize,
                        centerY - halfSize,
                        centerX + halfSize,
                        centerY + halfSize,
                        blurStrength
                    )
                }
                "Edges" -> {
                    val halfSize = centerSize
                    BlurUtils.blurArea(
                        0f, 0f, width, height, blurStrength
                    )
                }
            }
        } catch (e: Exception) {
        }
    }
}
