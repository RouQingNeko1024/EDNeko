/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

object FireballTrajectory : Module("FireballTrajectory", Category.RENDER, gameDetecting = false) {

    private val colorMode by choices("ColorMode", arrayOf("Theme", "Custom"), "Custom")
    private val trajectoryColor by color("TrajectoryColor", Color(255, 0, 0)) { colorMode == "Custom" }
    private val showExplosionRadius by boolean("ShowExplosionRadius", true)
    private val explosionRadius by float("ExplosionRadius", 1f, 0.5f..5f)
    private val predictionTicks by int("PredictionTicks", 50, 20..100)
    private val lineWidth by float("LineWidth", 2f, 1f..5f)
    private val showWarning by boolean("ShowWarning", true)
    
    private data class FireballData(
        val fireball: EntityFireball,
        val predictedPos: Vec3,
        val landingPos: BlockPos?
    )
    
    private val trackedFireballs = mutableListOf<FireballData>()

    val onRender3D = handler<Render3DEvent> {
        trackedFireballs.clear()
        
        mc.theWorld?.loadedEntityList?.filterIsInstance<EntityFireball>()?.forEach { fireball ->
            val predictedPos = predictLandingPosition(fireball)
            val landingBlock = findGroundPosition(predictedPos)
            trackedFireballs.add(FireballData(fireball, predictedPos, landingBlock))
        }
        
        if (trackedFireballs.isEmpty()) return@handler
        
        val renderManager = mc.renderManager ?: return@handler
        val renderPosX = renderManager.viewerPosX
        val renderPosY = renderManager.viewerPosY
        val renderPosZ = renderManager.viewerPosZ
        
        val color = when (colorMode) {
            "Theme" -> ClientThemesUtils.getColor()
            else -> trajectoryColor
        }
        
        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_DEPTH_TEST)
        glLineWidth(lineWidth)
        
        trackedFireballs.forEach { data ->
            renderTrajectory(data, renderPosX, renderPosY, renderPosZ, color)
            
            if (showExplosionRadius && data.landingPos != null) {
                renderExplosionCircle(data.landingPos, renderPosX, renderPosY, renderPosZ, color)
            }
        }
        
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_LIGHTING)
        glPopMatrix()
    }
    
    private fun predictLandingPosition(fireball: EntityFireball): Vec3 {
        var x = fireball.posX
        var y = fireball.posY
        var z = fireball.posZ
        
        val motionX = fireball.motionX
        val motionY = fireball.motionY
        val motionZ = fireball.motionZ
        
        for (i in 0 until predictionTicks) {
            x += motionX
            y += motionY
            z += motionZ
            
            if (y < 0) break
            
            val blockPos = BlockPos(x, y, z)
            val blockState = mc.theWorld?.getBlockState(blockPos)
            if (blockState != null && blockState.block?.isFullCube == true) {
                return Vec3(x, y, z)
            }
        }
        
        return Vec3(x, y, z)
    }
    
    private fun findGroundPosition(pos: Vec3): BlockPos? {
        for (y in pos.yCoord.toInt() downTo 0) {
            val blockPos = BlockPos(pos.xCoord.toInt(), y, pos.zCoord.toInt())
            val blockState = mc.theWorld?.getBlockState(blockPos)
            if (blockState != null && blockState.block?.isFullCube == true) {
                return blockPos
            }
        }
        return null
    }
    
    private fun renderTrajectory(data: FireballData, rx: Double, ry: Double, rz: Double, color: Color) {
        val fireball = data.fireball
        
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0.8f)
        glBegin(GL_LINE_STRIP)
        
        var x = fireball.posX
        var y = fireball.posY
        var z = fireball.posZ
        
        val motionX = fireball.motionX
        val motionY = fireball.motionY
        val motionZ = fireball.motionZ
        
        glVertex3d(x - rx, y - ry, z - rz)
        
        for (i in 0 until predictionTicks) {
            x += motionX
            y += motionY
            z += motionZ
            
            if (y < 0) break
            
            glVertex3d(x - rx, y - ry, z - rz)
            
            val blockPos = BlockPos(x, y, z)
            val blockState = mc.theWorld?.getBlockState(blockPos)
            if (blockState != null && blockState.block?.isFullCube == true) {
                break
            }
        }
        
        glEnd()
    }
    
    private fun renderExplosionCircle(pos: BlockPos, rx: Double, ry: Double, rz: Double, color: Color) {
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0.5f)
        glLineWidth(3f)
        
        glBegin(GL_LINE_LOOP)
        
        val segments = 32
        val radius = explosionRadius
        
        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * Math.PI * 2
            val x = pos.x + 0.5 + cos(angle) * radius - rx
            val z = pos.z + 0.5 + sin(angle) * radius - rz
            glVertex3d(x, pos.y + 0.1 - ry, z)
        }
        
        glEnd()
        
        glBegin(GL_LINE_LOOP)
        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * Math.PI * 2
            val x = pos.x + 0.5 + cos(angle) * radius - rx
            val z = pos.z + 0.5 + sin(angle) * radius - rz
            glVertex3d(x, pos.y + 0.5 - ry, z)
        }
        glEnd()
    }

    override fun onDisable() {
        trackedFireballs.clear()
    }
}
