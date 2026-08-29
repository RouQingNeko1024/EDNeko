/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import co.uk.hexeption.utils.OutlineUtils
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.ChestAura.clickedTileEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.block.toVec
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.ClientUtils.disableFastRender
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils.draw2D
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isEntityHeightVisible
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.entity.item.EntityMinecartChest
import net.minecraft.tileentity.*
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.pow

object StorageESP : Module("StorageESP", Category.RENDER) {
    private val mode by
    choices("Mode", arrayOf("Box", "OtherBox", "Outline", "Glow", "2D", "WireFrame"), "Outline")

    private val glowRenderScale by float("Glow-Renderscale", 1f, 0.5f..2f) { mode == "Glow" }
    private val glowRadius by int("Glow-Radius", 4, 1..5) { mode == "Glow" }
    private val glowFade by int("Glow-Fade", 10, 0..30) { mode == "Glow" }
    private val glowTargetAlpha by float("Glow-Target-Alpha", 0f, 0f..1f) { mode == "Glow" }

    private val espColorMode by choices("ESP-ColorMode", arrayOf("None", "Custom"), "None")
    private val espColor = ColorSettingsInteger(this, "ESPColor")
    { espColorMode == "Custom" }.with(255, 179, 72)

    private val maxRenderDistance by int("MaxRenderDistance", 100, 1..500).onChanged { value ->
        maxRenderDistanceSq = value.toDouble().pow(2)
    }

    private val onLook by boolean("OnLook", false)
    private val maxAngleDifference by float("MaxAngleDifference", 90f, 5.0f..90f) { onLook }

    private val thruBlocks by boolean("ThruBlocks", true)

    private var maxRenderDistanceSq = 0.0
        set(value) {
            field = if (value <= 0.0) maxRenderDistance.toDouble().pow(2.0) else value
        }

    private val chest by boolean("Chest", true)
    private val enderChest by boolean("EnderChest", true)
    private val furnace by boolean("Furnace", true)
    private val dispenser by boolean("Dispenser", true)
    private val hopper by boolean("Hopper", true)
    private val enchantmentTable by boolean("EnchantmentTable", false)
    private val brewingStand by boolean("BrewingStand", false)
    private val sign by boolean("Sign", false)

    private var tickCounter = 0
    private var cachedTileEntities = mutableListOf<TileEntity>()
    private var cachedMinecarts = mutableListOf<EntityMinecartChest>()
    private var frameSkipCounter = 0

    private fun getColor(tileEntity: TileEntity): Color? {
        return if (espColorMode == "Custom") {
            when {
                chest && tileEntity is TileEntityChest && tileEntity !in clickedTileEntities ->
                    Color(espColor.color().rgb)

                enderChest && tileEntity is TileEntityEnderChest && tileEntity !in clickedTileEntities ->
                    Color(espColor.color().rgb)

                furnace && tileEntity is TileEntityFurnace -> Color(espColor.color().rgb)
                dispenser && tileEntity is TileEntityDispenser -> Color(espColor.color().rgb)
                hopper && tileEntity is TileEntityHopper -> Color(espColor.color().rgb)
                enchantmentTable && tileEntity is TileEntityEnchantmentTable -> Color(espColor.color().rgb)
                brewingStand && tileEntity is TileEntityBrewingStand -> Color(espColor.color().rgb)
                sign && tileEntity is TileEntitySign -> Color(espColor.color().rgb)
                else -> null
            }
        } else {
            when {
                chest && tileEntity is TileEntityChest && tileEntity !in clickedTileEntities -> Color(0, 66, 255)
                enderChest && tileEntity is TileEntityEnderChest && tileEntity !in clickedTileEntities -> Color.MAGENTA
                furnace && tileEntity is TileEntityFurnace -> Color.BLACK
                dispenser && tileEntity is TileEntityDispenser -> Color.BLACK
                hopper && tileEntity is TileEntityHopper -> Color.GRAY
                enchantmentTable && tileEntity is TileEntityEnchantmentTable -> Color(166, 202, 240) // Light blue
                brewingStand && tileEntity is TileEntityBrewingStand -> Color.ORANGE
                sign && tileEntity is TileEntitySign -> Color.RED
                else -> null
            }
        }
    }

    private fun refreshCache() {
        val world = mc.theWorld ?: return
        val player = mc.thePlayer ?: return

        cachedTileEntities.clear()
        cachedMinecarts.clear()

        for (tileEntity in world.loadedTileEntityList) {
            val color = getColor(tileEntity) ?: continue
            val pos = tileEntity.pos
            if (player.getDistanceSq(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) <= maxRenderDistanceSq) {
                if (!thruBlocks || isEntityHeightVisible(tileEntity)) {
                    if (!onLook || isLookingOnEntities(tileEntity, maxAngleDifference.toDouble())) {
                        cachedTileEntities.add(tileEntity)
                    }
                }
            }
        }

        for (entity in world.loadedEntityList) {
            if (entity is EntityMinecartChest) {
                val dist = player.getDistanceSqToEntity(entity)
                if (dist <= maxRenderDistanceSq) {
                    if (!thruBlocks || isEntityHeightVisible(entity)) {
                        if (!onLook || isLookingOnEntities(entity, maxAngleDifference.toDouble())) {
                            cachedMinecarts.add(entity)
                        }
                    }
                }
            }
        }
    }

    val onRender3D = handler<Render3DEvent> { event ->
        try {
            tickCounter++
            if (tickCounter % 10 == 0) {
                refreshCache()
            }

            if (cachedTileEntities.isEmpty() && cachedMinecarts.isEmpty()) return@handler

            if (mode == "Outline") {
                disableFastRender()
                OutlineUtils.checkSetupFBO()
            }

            val gamma = mc.gameSettings.gammaSetting
            mc.gameSettings.gammaSetting = 100000f

            for (tileEntity in cachedTileEntities) {
                val color = getColor(tileEntity) ?: continue
                val tileEntityPos = tileEntity.pos

                if (!(tileEntity is TileEntityChest || tileEntity is TileEntityEnderChest)) {
                    drawBlockBox(tileEntity.pos, color, mode != "OtherBox")

                    if (tileEntity !is TileEntityEnchantmentTable)
                        continue
                }

                when (mode) {
                    "OtherBox", "Box" -> drawBlockBox(tileEntity.pos, color, mode != "OtherBox")
                    "2D" -> draw2D(tileEntity.pos, color.rgb, Color.BLACK.rgb)
                    "Outline" -> {
                        glColor(color)
                        OutlineUtils.renderOne(3F)
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        OutlineUtils.renderTwo()
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        OutlineUtils.renderThree()
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        OutlineUtils.renderFour(color)
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        OutlineUtils.renderFive()
                        OutlineUtils.setColor(Color.WHITE)
                    }

                    "WireFrame" -> {
                        glPushMatrix()
                        glPushAttrib(GL_ALL_ATTRIB_BITS)
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                        glDisable(GL_TEXTURE_2D)
                        glDisable(GL_LIGHTING)
                        glDisable(GL_DEPTH_TEST)
                        glEnable(GL_LINE_SMOOTH)
                        glEnable(GL_BLEND)
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                        glLineWidth(1.5f)
                        glColor(color)
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        glColor(color)
                        TileEntityRendererDispatcher.instance.renderTileEntity(tileEntity, event.partialTicks, -1)
                        glPopAttrib()
                        glPopMatrix()
                    }
                }
            }

            for (entity in cachedMinecarts) {
                when (mode) {
                    "OtherBox", "Box" -> drawEntityBox(entity, Color(0, 66, 255), mode != "OtherBox")

                    "2d" -> draw2D(entity.position, Color(0, 66, 255).rgb, Color.BLACK.rgb)
                    "Outline" -> {
                        val entityShadow = mc.gameSettings.entityShadows
                        mc.gameSettings.entityShadows = false
                        glColor(Color(0, 66, 255))
                        OutlineUtils.renderOne(3f)
                        mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                        OutlineUtils.renderTwo()
                        mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                        OutlineUtils.renderThree()
                        mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                        OutlineUtils.renderFour(Color(0, 66, 255))
                        mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                        OutlineUtils.renderFive()
                        OutlineUtils.setColor(Color.WHITE)
                        mc.gameSettings.entityShadows = entityShadow
                    }

                    "WireFrame" -> {
                        val entityShadow = mc.gameSettings.entityShadows
                        mc.gameSettings.entityShadows = false
                        glPushMatrix()
                        glPushAttrib(GL_ALL_ATTRIB_BITS)
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                        glDisable(GL_TEXTURE_2D)
                        glDisable(GL_LIGHTING)
                        glDisable(GL_DEPTH_TEST)
                        glEnable(GL_LINE_SMOOTH)
                        glEnable(GL_BLEND)
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                        glColor(Color(0, 66, 255))
                        mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                        glColor(Color(0, 66, 255))
                        glLineWidth(1.5f)
                        mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
                        glPopAttrib()
                        glPopMatrix()
                        mc.gameSettings.entityShadows = entityShadow
                    }
                }
            }

            glColor(Color(255, 255, 255, 255))
            mc.gameSettings.gammaSetting = gamma
        } catch (_: Exception) {
        }
    }

    val onRender2D = handler<Render2DEvent> { event ->
        if (mc.theWorld == null || mode != "Glow")
            return@handler

        frameSkipCounter++
        if (frameSkipCounter % 3 != 0) return@handler

        if (cachedTileEntities.isEmpty()) return@handler

        val renderManager = mc.renderManager

        try {
            cachedTileEntities
                .groupBy { getColor(it) }
                .forEach { (color, tileEntities) ->
                    color ?: return@forEach

                    GlowShader.startDraw(event.partialTicks, glowRenderScale)

                    for (entity in tileEntities) {
                        val pos = entity.pos.toVec()
                        val (x, y, z) = pos - renderManager.renderPos
                        TileEntityRendererDispatcher.instance.renderTileEntityAt(entity, x, y, z, event.partialTicks)
                    }

                    GlowShader.stopDraw(color, glowRadius, glowFade, glowTargetAlpha)
                }
        } catch (ex: Exception) {
            LOGGER.error("An error occurred while rendering all storages for shader esp", ex)
        }
    }
}