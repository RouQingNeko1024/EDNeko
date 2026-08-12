package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object TargetMark : Module("TargetMark", Category.RENDER) {
    private val sizeValue by float("Size", 0.05f, 0.05f..0.1f)
    private val rotationSpeed by float("RotationSpeed", 180f, 0f..360f)
    private val color by color("Color", Color(255, 255, 255, 150))
    private val choiceImage by choices("Image", arrayOf("Outline", "Outline2", "Circle", "CCBlueX", "Cry", "Creeper"), "Outline")

    private var rotation = 0f

    val onRender3D = handler<Render3DEvent> { event ->
        KillAura.target?.let { entity ->
            val x = (entity.prevPosX + (entity.posX - entity.prevPosX) * event.partialTicks) - mc.renderManager.viewerPosX
            val y = (entity.prevPosY + (entity.posY - entity.prevPosY) * event.partialTicks) + entity.height * 0.6 - mc.renderManager.viewerPosY
            val z = (entity.prevPosZ + (entity.posZ - entity.prevPosZ) * event.partialTicks) - mc.renderManager.viewerPosZ
            glPushMatrix()
            glTranslated(x, y, z)
            glRotatef(-mc.renderManager.playerViewY, 0f, 1f, 0f)
            glRotatef(mc.renderManager.playerViewX * if (mc.gameSettings.thirdPersonView == 2) -1 else 1, 1f, 0f, 0f)
            if (choiceImage != "Outline") {
                rotation = 180f
            } else {
                rotation += rotationSpeed * (event.partialTicks / 20f)
            }
            glRotatef(rotation % 360, 0f, 0f, 1f)
            val finalSize = sizeValue * 0.8f
            glScalef(finalSize, finalSize, finalSize)

            drawTargetMark()

            glDisable(GL_BLEND)
            glEnable(GL_DEPTH_TEST)
            glPopMatrix()
        }
    }

    private fun drawTargetMark() {
        val texture: ResourceLocation = when (choiceImage) {
            "Outline" -> ResourceLocation("edneko/targetimage/target.png")
            "Outline2" -> ResourceLocation("edneko/targetimage/target2.png")
            "Circle" -> ResourceLocation("edneko/targetimage/glow_circle.png")
            "CCBlueX" -> ResourceLocation("edneko/targetimage/ccbluex.png")
            "Cry" -> ResourceLocation("edneko/targetimage/cry.png")
            "Creeper" -> ResourceLocation("edneko/targetimage/creeper.png")
            else -> ResourceLocation("edneko/targetimage/target.png")
        }
        RenderUtils.drawImage(texture, -16, -16, 32, 32, color)
    }
}
