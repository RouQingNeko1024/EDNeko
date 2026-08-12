/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.command.commands.TacoCommand.tacoToggle
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.ResourceLocation

@ElementInfo(name = "Taco", priority = 1)
class Taco(x: Double = 2.0, y: Double = 441.0) : Element("Taco", x = x, y = y) {

    private val frameSpeed by float("frameSpeed", 50f, 0f..200f)
    private val animationSpeed by float("animationSpeed", 0.15f, 0.01f..1.0f)

    private var lastFrameTime = System.currentTimeMillis()
    private var currentFrame = 0
    private var positionX = 0f

    private val textures = arrayOf(
        ResourceLocation("edneko/taco/1.png"),
        ResourceLocation("edneko/taco/2.png"),
        ResourceLocation("edneko/taco/3.png"),
        ResourceLocation("edneko/taco/4.png")
    )

    override fun drawElement(): Border {
        val player = mc.thePlayer ?: return Border(0F, 0F, 0F, 0F)

        if (tacoToggle || player.ticksExisted < 20)
            return Border(0F, 0F, 0F, 0F)

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime >= frameSpeed) {
            currentFrame = (currentFrame + 1) % textures.size
            lastFrameTime = currentTime
        }

        val scaledScreen = ScaledResolution(mc)
        
        positionX += animationSpeed * deltaTime
        
        if (positionX > scaledScreen.scaledWidth) {
            positionX = -64f
        }

        RenderUtils.drawImage(textures[currentFrame], positionX.toInt(), 0, 64, 64)

        return Border(0F, 0F, 64F, 64F)
    }
}
