/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * skid FDP Client
 * https://github.com/SkidderMC/FDPClient
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.item.*
import net.minecraft.util.*
import org.lwjgl.opengl.GL11
import java.awt.Color

object Projectiles : Module("Projectiles", Category.RENDER, gameDetecting = false) {

    private val colorRedValue by int("Color-Red", 255, 0..255)
    private val colorGreenValue by int("Color-Green", 255, 0..255)
    private val colorBlueValue by int("Color-Blue", 255, 0..255)
    private val colorAlphaValue by int("Color-Alpha", 70, 0..255)
    private val dynamicBowPower by boolean("DynamicBowPower", true)

    val onRender3D = handler<Render3DEvent> {
        mc.thePlayer.heldItem ?: return@handler

        val item = mc.thePlayer.heldItem.item
        val renderManager = mc.renderManager
        var isBow = false
        var motionFactor = 1.5F
        var motionSlowdown = 0.99F
        val gravity: Float
        val size: Float

        when (item) {
            is ItemBow -> {
                if (dynamicBowPower && !mc.thePlayer.isUsingItem) {
                    return@handler
                }

                isBow = true
                gravity = 0.05F
                size = 0.3F

                var power = (if (dynamicBowPower) mc.thePlayer.itemInUseDuration else item.getMaxItemUseDuration(ItemStack(item))) / 20f
                power = (power * power + power * 2F) / 3F
                if (power < 0.1F) {
                    return@handler
                }

                if (power > 1F) {
                    power = 1F
                }

                motionFactor = power * 3F
            }
            is ItemFishingRod -> {
                gravity = 0.04F
                size = 0.25F
                motionSlowdown = 0.92F
            }
            is ItemPotion -> {
                if (!ItemPotion.isSplash(mc.thePlayer.heldItem.itemDamage)) return@handler
                gravity = 0.05F
                size = 0.25F
                motionFactor = 0.5F
            }
            is ItemSnowball, is ItemEnderPearl, is ItemEgg -> {
                gravity = 0.03F
                size = 0.25F
            }
            else -> return@handler
        }

        val yaw = RotationUtils.currentRotation?.yaw ?: mc.thePlayer.rotationYaw
        val pitch = RotationUtils.currentRotation?.pitch ?: mc.thePlayer.rotationPitch

        var posX = renderManager.renderPosX - MathHelper.cos(yaw / 180F * 3.1415927F) * 0.16F
        var posY = renderManager.renderPosY + mc.thePlayer.eyeHeight - 0.10000000149011612
        var posZ = renderManager.renderPosZ - MathHelper.sin(yaw / 180F * 3.1415927F) * 0.16F

        var motionX = (-MathHelper.sin(yaw / 180f * 3.1415927F) * MathHelper.cos(pitch / 180F * 3.1415927F) *
                if (isBow) 1.0 else 0.4)
        var motionY = -MathHelper.sin((pitch +
                if (item is ItemPotion && ItemPotion.isSplash(mc.thePlayer.heldItem.itemDamage)) -20 else 0) /
                180f * 3.1415927f) * if (isBow) 1.0 else 0.4
        var motionZ = (MathHelper.cos(yaw / 180f * 3.1415927F) * MathHelper.cos(pitch / 180F * 3.1415927F) *
                if (isBow) 1.0 else 0.4)
        val distance = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ)

        motionX /= distance
        motionY /= distance
        motionZ /= distance
        motionX *= motionFactor
        motionY *= motionFactor
        motionZ *= motionFactor

        var landingPosition: MovingObjectPosition? = null
        var hasLanded = false
        var hitEntity = false

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        val pos = mutableListOf<Vec3>()

        while (!hasLanded && posY > 0.0) {
            var posBefore = Vec3(posX, posY, posZ)
            var posAfter = Vec3(posX + motionX, posY + motionY, posZ + motionZ)

            landingPosition = mc.theWorld.rayTraceBlocks(posBefore, posAfter, false,
                true, false)

            posBefore = Vec3(posX, posY, posZ)
            posAfter = Vec3(posX + motionX, posY + motionY, posZ + motionZ)

            if (landingPosition != null) {
                hasLanded = true
                posAfter = Vec3(landingPosition.hitVec.xCoord, landingPosition.hitVec.yCoord, landingPosition.hitVec.zCoord)
            }

            val arrowBox = AxisAlignedBB(posX - size, posY - size, posZ - size, posX + size,
                posY + size, posZ + size).addCoord(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0)
            val chunkMinX = MathHelper.floor_double((arrowBox.minX - 2.0) / 16.0)
            val chunkMaxX = MathHelper.floor_double((arrowBox.maxX + 2.0) / 16.0)
            val chunkMinZ = MathHelper.floor_double((arrowBox.minZ - 2.0) / 16.0)
            val chunkMaxZ = MathHelper.floor_double((arrowBox.maxZ + 2.0) / 16.0)

            val collidedEntities = mutableListOf<Entity>()
            for (x in chunkMinX..chunkMaxX)
                for (z in chunkMinZ..chunkMaxZ)
                    mc.theWorld.getChunkFromChunkCoords(x, z)
                        .getEntitiesWithinAABBForEntity(mc.thePlayer, arrowBox, collidedEntities, null)

            for (possibleEntity in collidedEntities) {
                if (possibleEntity.canBeCollidedWith() && possibleEntity !== mc.thePlayer) {
                    val possibleEntityBoundingBox = possibleEntity.entityBoundingBox
                        .expand(size.toDouble(), size.toDouble(), size.toDouble())

                    val possibleEntityLanding = possibleEntityBoundingBox
                        .calculateIntercept(posBefore, posAfter) ?: continue

                    hitEntity = true
                    hasLanded = true
                    landingPosition = possibleEntityLanding
                }
            }

            posX += motionX
            posY += motionY
            posZ += motionZ

            if (mc.theWorld.getBlockState(BlockPos(posX, posY, posZ)).block.material === Material.water) {
                motionX *= 0.6
                motionY *= 0.6
                motionZ *= 0.6
            } else {
                motionX *= motionSlowdown.toDouble()
                motionY *= motionSlowdown.toDouble()
                motionZ *= motionSlowdown.toDouble()
            }

            motionY -= gravity.toDouble()

            pos.add(Vec3(posX - renderManager.renderPosX, posY - renderManager.renderPosY,
                posZ - renderManager.renderPosZ))
        }

        GL11.glDepthMask(false)
        RenderUtils.enableGlCap(GL11.GL_BLEND, GL11.GL_LINE_SMOOTH)
        RenderUtils.disableGlCap(GL11.GL_DEPTH_TEST, GL11.GL_ALPHA_TEST, GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        RenderUtils.glColor(if (hitEntity) { Color(255, 140, 140, colorAlphaValue) } else { Color(colorRedValue, colorGreenValue, colorBlueValue, colorAlphaValue) })
        GL11.glLineWidth(2f)

        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)

        pos.forEach {
            worldRenderer.pos(it.xCoord, it.yCoord, it.zCoord).endVertex()
        }

        tessellator.draw()
        GL11.glPushMatrix()
        GL11.glTranslated(posX - renderManager.renderPosX, posY - renderManager.renderPosY,
            posZ - renderManager.renderPosZ)

        if (landingPosition != null) {
            when (landingPosition.sideHit.axis.ordinal) {
                0 -> GL11.glRotatef(90F, 0F, 0F, 1F)
                2 -> GL11.glRotatef(90F, 1F, 0F, 0F)
            }

            RenderUtils.drawAxisAlignedBB(AxisAlignedBB(-0.5, 0.0, -0.5, 0.5, 0.1, 0.5), 
                if (hitEntity) { Color(255, 140, 140, colorAlphaValue) } else { Color(colorRedValue, colorGreenValue, colorBlueValue, colorAlphaValue) })
        }
        GL11.glPopMatrix()
        GL11.glDepthMask(true)
        RenderUtils.resetCaps()
        GL11.glColor4f(1F, 1F, 1F, 1F)
    }
}
