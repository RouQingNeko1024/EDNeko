/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EntityDamageEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.minecraft.client.model.ModelBiped
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.max
import kotlin.math.sin

object DamageESP : Module("DamageESP", Category.RENDER) {
    
    private val mode by choices(
        "Mode",
        arrayOf("Box", "WireFrame", "Model"),
        "Box"
    )
    
    private val duration by float("Duration", 1f, 0.1f..5f)
    
    private val lineWidth by float("LineWidth", 2f, 0.5f..5f)
    
    private val fadeOut by boolean("FadeOut", true)
    
    private val maxTraces by int("MaxTraces", 10, 1..50)
    
    private val onlyOnDamage by boolean("OnlyOnDamage", false)
    
    private val color = ColorSettingsInteger(this, "Color").with(255, 0, 0)

    private val dummyOptions1 by boolean("The options below are useless", true)
    private val dummyOptions2 by boolean("下面的选项没有用", true)
    private val dummyOptions3 by boolean("是为了防止上面那个颜色选项", true)
    private val dummyOptions4 by boolean("跑到外面去，我懒得修这个bug", true)
    
    private data class DamageTrace(
        val posX: Double,
        val posY: Double,
        val posZ: Double,
        val width: Float,
        val height: Float,
        val yaw: Float,
        val pitch: Float,
        val limbSwingAmount: Float,
        val swingProgress: Float,
        val isPlayer: Boolean,
        val time: Long
    )
    
    private val traces = mutableListOf<DamageTrace>()
    private val model = ModelBiped()
    
    val onAttack = handler<AttackEvent> { event ->
        if (onlyOnDamage) return@handler
        
        val target = event.targetEntity as? EntityLivingBase ?: return@handler
        
        addTrace(target)
    }
    
    val onEntityDamage = handler<EntityDamageEvent> { event ->
        val target = event.entity as? EntityLivingBase ?: return@handler
        
        if (onlyOnDamage) {
            addTrace(target)
        }
    }
    
    private fun addTrace(target: EntityLivingBase) {
        traces.add(DamageTrace(
            posX = target.posX,
            posY = target.posY,
            posZ = target.posZ,
            width = target.width,
            height = target.height,
            yaw = target.rotationYaw,
            pitch = target.rotationPitch,
            limbSwingAmount = target.limbSwingAmount,
            swingProgress = target.swingProgress,
            isPlayer = target is EntityPlayer,
            time = System.currentTimeMillis()
        ))
        
        while (traces.size > maxTraces) {
            traces.removeAt(0)
        }
    }
    
    private fun setModelRotations(trace: DamageTrace, partialTicks: Float) {
        val limbSwing = trace.limbSwingAmount * 0.6662f
        model.setRotationAngles(
            limbSwing,
            trace.limbSwingAmount,
            0f,
            trace.yaw,
            trace.pitch,
            0.0625f,
            null
        )
        
        if (trace.swingProgress > 0f) {
            val swing = sin(trace.swingProgress * 3.1415927f)
            val swing2 = sin(trace.swingProgress * 3.1415927f * 2.0f)
            model.bipedRightArm.rotateAngleX += sin(swing * 1.5707964f) * 1.2f * swing2
            model.bipedLeftArm.rotateAngleX += sin(swing * 1.5707964f) * 1.2f * swing2
        }
    }
    
    val onRender3D = handler<Render3DEvent> {
        val currentTime = System.currentTimeMillis()
        val durationMs = (duration * 1000).toLong()
        
        traces.removeAll { currentTime - it.time > durationMs }
        
        if (traces.isEmpty()) return@handler
        
        val renderManager = mc.renderManager
        
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_LINE_SMOOTH)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
        glLineWidth(lineWidth)
        
        for (trace in traces) {
            val elapsed = currentTime - trace.time
            val progress = elapsed.toFloat() / durationMs
            
            val alpha = if (fadeOut) {
                max(0f, 1f - progress)
            } else {
                1f
            }
            
            if (alpha <= 0) continue
            
            val baseColor = color.color()
            val renderColor = Color(baseColor.red, baseColor.green, baseColor.blue, (alpha * 255).toInt())
            
            val x = trace.posX - renderManager.renderPosX
            val y = trace.posY - renderManager.renderPosY
            val z = trace.posZ - renderManager.renderPosZ
            
            val halfWidth = trace.width / 2.0
            val height = trace.height.toDouble()
            
            glColor(renderColor)
            
            when (mode) {
                "Box" -> {
                    glBegin(GL_LINES)
                    renderBox(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth)
                    glEnd()
                }
                
                "WireFrame" -> {
                    GlStateManager.pushMatrix()
                    GlStateManager.translate(x, y, z)
                    GlStateManager.rotate(-trace.yaw, 0f, 1f, 0f)
                    GlStateManager.scale(-1.0, -1.0, 1.0)
                    GlStateManager.translate(0.0, -1.5, 0.0)
                    
                    model.isChild = false
                    model.isRiding = false
                    setModelRotations(trace, it.partialTicks)
                    
                    glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                    model.bipedBody.render(0.0625f)
                    model.bipedRightArm.render(0.0625f)
                    model.bipedLeftArm.render(0.0625f)
                    model.bipedRightLeg.render(0.0625f)
                    model.bipedLeftLeg.render(0.0625f)
                    
                    GlStateManager.pushMatrix()
                    GlStateManager.rotate(-trace.pitch, 1f, 0f, 0f)
                    model.bipedHead.render(0.0625f)
                    GlStateManager.popMatrix()
                    
                    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
                    
                    GlStateManager.popMatrix()
                }
                
                "Model" -> {
                    GlStateManager.pushMatrix()
                    GlStateManager.translate(x, y, z)
                    GlStateManager.rotate(-trace.yaw, 0f, 1f, 0f)
                    GlStateManager.scale(-1.0, -1.0, 1.0)
                    GlStateManager.translate(0.0, -1.5, 0.0)
                    
                    model.isChild = false
                    model.isRiding = false
                    setModelRotations(trace, it.partialTicks)
                    
                    glEnable(GL_POLYGON_OFFSET_FILL)
                    glPolygonOffset(1f, 1f)
                    glColor4f(renderColor.red / 255f * 0.3f, renderColor.green / 255f * 0.3f, renderColor.blue / 255f * 0.3f, alpha * 0.5f)
                    model.bipedBody.render(0.0625f)
                    model.bipedRightArm.render(0.0625f)
                    model.bipedLeftArm.render(0.0625f)
                    model.bipedRightLeg.render(0.0625f)
                    model.bipedLeftLeg.render(0.0625f)
                    
                    GlStateManager.pushMatrix()
                    GlStateManager.rotate(-trace.pitch, 1f, 0f, 0f)
                    model.bipedHead.render(0.0625f)
                    GlStateManager.popMatrix()
                    
                    glDisable(GL_POLYGON_OFFSET_FILL)
                    
                    glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                    glColor(renderColor)
                    model.bipedBody.render(0.0625f)
                    model.bipedRightArm.render(0.0625f)
                    model.bipedLeftArm.render(0.0625f)
                    model.bipedRightLeg.render(0.0625f)
                    model.bipedLeftLeg.render(0.0625f)
                    
                    GlStateManager.pushMatrix()
                    GlStateManager.rotate(-trace.pitch, 1f, 0f, 0f)
                    model.bipedHead.render(0.0625f)
                    GlStateManager.popMatrix()
                    
                    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
                    
                    GlStateManager.popMatrix()
                }
            }
        }
        
        glPopAttrib()
        glColor4f(1f, 1f, 1f, 1f)
    }
    
    private fun renderBox(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) {
        glVertex3d(minX, minY, minZ)
        glVertex3d(maxX, minY, minZ)
        
        glVertex3d(maxX, minY, minZ)
        glVertex3d(maxX, minY, maxZ)
        
        glVertex3d(maxX, minY, maxZ)
        glVertex3d(minX, minY, maxZ)
        
        glVertex3d(minX, minY, maxZ)
        glVertex3d(minX, minY, minZ)
        
        glVertex3d(minX, maxY, minZ)
        glVertex3d(maxX, maxY, minZ)
        
        glVertex3d(maxX, maxY, minZ)
        glVertex3d(maxX, maxY, maxZ)
        
        glVertex3d(maxX, maxY, maxZ)
        glVertex3d(minX, maxY, maxZ)
        
        glVertex3d(minX, maxY, maxZ)
        glVertex3d(minX, maxY, minZ)
        
        glVertex3d(minX, minY, minZ)
        glVertex3d(minX, maxY, minZ)
        
        glVertex3d(maxX, minY, minZ)
        glVertex3d(maxX, maxY, minZ)
        
        glVertex3d(maxX, minY, maxZ)
        glVertex3d(maxX, maxY, maxZ)
        
        glVertex3d(minX, minY, maxZ)
        glVertex3d(minX, maxY, maxZ)
    }
    
    override val tag: String
        get() = mode
}
