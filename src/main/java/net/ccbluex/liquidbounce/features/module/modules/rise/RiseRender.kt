package net.ccbluex.liquidbounce.features.module.modules.rise

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.BlockPos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.colorFromDisplayName
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.client.EntityLookup
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isEntityHeightVisible
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.disableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.enableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetCaps
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawRect
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.GlStateManager.enableTexture2D
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.item.EntityItem
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.tileentity.TileEntityEnderChest
import net.minecraft.util.Vec3
import net.minecraft.init.Blocks
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object RiseESP : Module("RiseESP", Category.RISE, forcedDescription = "Rise - Entity ESP with multiple modes") {

    private val mode by choices("Mode", arrayOf("Box", "OtherBox", "WireFrame", "2D", "Gaussian", "Outline", "Glow"), "Box")
    private val glow by boolean("Glow", false)
    private val chams by boolean("Chams", false)
    private val skeletal by boolean("Skeletal", false)
    private val width by float("Width", 0.5f, 0.1f..1.5f)
    private val outlineWidth by float("OutlineWidth", 3f, 0.5f..5f)
    private val wireframeWidth by float("WireFrameWidth", 2f, 0.5f..5f)
    private val glowrenderScale by float("Glowrenderscale", 1f, 0.5f..2f)
    private val glowRadius by int("GlowRadius", 4, 1..5)
    private val glowFade by int("GlowFade", 10, 0..30)
    private val glowTargetAlpha by float("GlowTargetAlpha", 0f, 0f..1f)
    private val colorTeam by boolean("TeamColor", false)
    private val bot by boolean("Bots", true)
    private val maxrenderDistance by int("MaxrenderDistance", 50, 1..200)
    private val onLook by boolean("OnLook", false)
    private val maxAngleDifference by float("MaxAngleDifference", 90f, 5.0f..90f)
    private val thruBlocks by boolean("ThruBlocks", true)

    private val espColor = ColorSettingsInteger(this, "ESPColor").with(255, 255, 255)

    private var maxrenderDistanceSq = 0.0
        set(value) {
            field = if (value <= 0.0) maxrenderDistance.toDouble().pow(2.0) else value
        }

    var renderNameTags = true

    private val entities by EntityLookup<EntityLivingBase>().filter { shouldrender(it) }

    private var glowFrameSkip = 0

    fun getColor(entity: EntityLivingBase): Color {
        if (entity.hurtTime > 0)
            return Color.RED
        if (entity is EntityPlayer && entity.isClientFriend())
            return Color.BLUE
        if (colorTeam) {
            entity.colorFromDisplayName()?.let { return it }
        }
        return espColor.color()
    }

    fun shouldrender(entity: EntityLivingBase): Boolean {
        val player = mc.thePlayer ?: return false
        return (player.getDistanceSqToEntity(entity) <= maxrenderDistanceSq
                && (thruBlocks || isEntityHeightVisible(entity))
                && (!onLook || isLookingOnEntities(entity, maxAngleDifference.toDouble()))
                && isSelected(entity, false)
                && (bot || !isBot(entity)))
    }

    val onrender3D = handler<Render3DEvent> {
        if (entities.isEmpty()) return@handler

        val mvMatrix = WorldToScreen.getMatrix(GL_MODELVIEW_MATRIX)
        val projectionMatrix = WorldToScreen.getMatrix(GL_PROJECTION_MATRIX)
        val real2d = mode == "2D"

        if (real2d) {
            glPushAttrib(GL_ENABLE_BIT)
            glEnable(GL_BLEND)
            glDisable(GL_TEXTURE_2D)
            glDisable(GL_DEPTH_TEST)
            glMatrixMode(GL_PROJECTION)
            glPushMatrix()
            glLoadIdentity()
            glOrtho(0.0, mc.displayWidth.toDouble(), mc.displayHeight.toDouble(), 0.0, -1.0, 1.0)
            glMatrixMode(GL_MODELVIEW)
            glPushMatrix()
            glLoadIdentity()
            glDisable(GL_DEPTH_TEST)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            GlStateManager.enableTexture2D()
            glDepthMask(true)
            glLineWidth(width)
        }

        for (entity in entities) {
            val color = getColor(entity)
            val pos = entity.interpolatedPosition(entity.lastTickPos) - mc.renderManager.renderPos

            when (mode) {
                "Box", "OtherBox" -> drawEntityBox(entity, color, mode != "OtherBox")
                "WireFrame" -> drawEntityBox(entity, color, false)
                "Outline" -> drawEntityBox(entity, color, true)
                "2D" -> {
                    val bb = entity.hitBox.offset(-entity.currPos + pos)
                    val boxVertices = arrayOf(
                        doubleArrayOf(bb.minX, bb.minY, bb.minZ),
                        doubleArrayOf(bb.minX, bb.maxY, bb.minZ),
                        doubleArrayOf(bb.maxX, bb.maxY, bb.minZ),
                        doubleArrayOf(bb.maxX, bb.minY, bb.minZ),
                        doubleArrayOf(bb.minX, bb.minY, bb.maxZ),
                        doubleArrayOf(bb.minX, bb.maxY, bb.maxZ),
                        doubleArrayOf(bb.maxX, bb.maxY, bb.maxZ),
                        doubleArrayOf(bb.maxX, bb.minY, bb.maxZ)
                    )
                    var minX = Float.MAX_VALUE
                    var minY = Float.MAX_VALUE
                    var maxX = -1f
                    var maxY = -1f
                    for (boxVertex in boxVertices) {
                        val screenPos = WorldToScreen.worldToScreen(
                            org.lwjgl.util.vector.Vector3f(
                                boxVertex[0].toFloat(),
                                boxVertex[1].toFloat(),
                                boxVertex[2].toFloat()
                            ), mvMatrix, projectionMatrix, mc.displayWidth, mc.displayHeight
                        ) ?: continue
                        minX = min(screenPos.x, minX)
                        minY = min(screenPos.y, minY)
                        maxX = max(screenPos.x, maxX)
                        maxY = max(screenPos.y, maxY)
                    }
                    if (minX > 0 || minY > 0 || maxX <= mc.displayWidth || maxY <= mc.displayWidth) {
                        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
                        glBegin(GL_LINE_LOOP)
                        glVertex2f(minX, minY)
                        glVertex2f(minX, maxY)
                        glVertex2f(maxX, maxY)
                        glVertex2f(maxX, minY)
                        glEnd()
                    }
                }
            }
        }

        if (real2d) {
            glColor4f(1f, 1f, 1f, 1f)
            glEnable(GL_DEPTH_TEST)
            glMatrixMode(GL_PROJECTION)
            glPopMatrix()
            glMatrixMode(GL_MODELVIEW)
            glPopMatrix()
            glPopAttrib()
        }
    }

    val onrender2D = handler<Render2DEvent> { event ->
        if (mc.theWorld == null || !glow || entities.isEmpty()) return@handler

        glowFrameSkip++
        if (glowFrameSkip % 3 != 0) return@handler

        renderNameTags = false
        try {
            entities.groupBy(::getColor).forEach { (color, entities) ->
                GlowShader.startDraw(event.partialTicks, glowrenderScale)
                for (entity in entities) {
                    mc.renderManager.renderEntitySimple(entity, event.partialTicks)
                }
                GlowShader.stopDraw(color, glowRadius, glowFade, glowTargetAlpha)
            }
        } catch (ex: Exception) {
            LOGGER.error("An error occurred while rendering all entities for shader esp", ex)
        }
        renderNameTags = true
    }

    override val tag
        get() = mode
}

object RiseNameTags : Module("RiseNameTags", Category.RISE, forcedDescription = "Rise - Custom name tags") {

    private val mode by choices("Mode", arrayOf("Modern", "Vanilla", "Classic"), "Modern")
    private val showTargets by boolean("Targets", false)
    private val player by boolean("Player", true) { !showTargets }
    private val invisibles by boolean("Invisibles", false) { !showTargets }
    private val animals by boolean("Animals", false) { !showTargets }
    private val mobs by boolean("Mobs", false) { !showTargets }
    private val playerTeammates by boolean("PlayerTeammates", true) { !showTargets }
    private val maxrenderDistance by int("MaxrenderDistance", 50, 1..200)
    private val shadowcheck by boolean("ShadowCheck", true)
    private val shadowStrength by int("ShadowStrength", 1, 1..2)

    private var maxrenderDistanceSq = 0.0
        set(value) { field = if (value <= 0.0) maxrenderDistance.toDouble().pow(2.0) else value }

    private val entities by EntityLookup<EntityLivingBase>().filter { shouldrender(it) }

    private fun shouldrender(entity: EntityLivingBase): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        val dist = thePlayer.getDistanceSqToEntity(entity)
        if (dist > maxrenderDistanceSq) return false
        if (!isEntityHeightVisible(entity)) return false
        if (entity is EntityPlayer) {
            if (entity.isClientFriend()) return playerTeammates
            if (!player) return false
        }
        if (entity.isInvisible && !invisibles) return false
        return true
    }

    val onrender3D = handler<Render3DEvent> {
        if (entities.isEmpty()) return@handler

        for (entity in entities) {
            renderrenderNameTag(entity)
        }
    }

    private fun renderrenderNameTag(entity: EntityLivingBase) {
        val thePlayer = mc.thePlayer ?: return
        val fontrenderer = Fonts.fontRegular35

        glPushMatrix()
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)
        enableGlCap(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val name = entity.displayName.unformattedText
        val health = getHealth(entity)
        val healthText = health.toInt().toString()

        val renderManager = mc.renderManager
        val rotateX = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f

        val (x, y, z) = entity.interpolatedPosition(entity.lastTickPos) - renderManager.renderPos

        glTranslated(x, y + entity.eyeHeight.toDouble() + 0.55, z)
        glRotatef(-renderManager.playerViewY, 0f, 1f, 0f)
        glRotatef(renderManager.playerViewX * rotateX, 1f, 0f, 0f)

        val distance = thePlayer.getDistanceToEntity(entity)
        val scale = ((distance / 4f).coerceAtLeast(1f) / 150f) * 2f
        glScalef(-scale, -scale, scale)

        val nameWidth = fontrenderer.getStringWidth(name)
        val healthWidth = fontrenderer.getStringWidth(healthText)
        val maxWidth = maxOf(nameWidth, healthWidth) + 10
        val height = (fontrenderer.FONT_HEIGHT * 2) + 6

        glDisable(GL_TEXTURE_2D)

        if (shadowcheck) {
            GlowUtils.drawGlow(
                -maxWidth / 2f, -height / 2f,
                maxWidth.toFloat(), height.toFloat(),
                (shadowStrength * 13f).toInt(),
                Color(0, 0, 0, 140)
            )
        }

        RenderUtils.drawRoundedRect(
            -maxWidth / 2f, -height / 2f,
            maxWidth / 2f, height / 2f,
            Color(0, 0, 0, 178).rgb,
            5f
        )

        glEnable(GL_TEXTURE_2D)

        fontrenderer.drawString(
            name,
            -nameWidth / 2f,
            -height / 2f + 2f,
            Color(103, 216, 230).rgb,
            false
        )

        fontrenderer.drawString(
            healthText,
            -healthWidth / 2f,
            -height / 2f + fontrenderer.FONT_HEIGHT + 4f,
            Color.WHITE.rgb,
            false
        )

        resetCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }
}

object RiseTracers : Module("RiseTracers", Category.RISE, forcedDescription = "Rise - Draw tracers to entities") {

    private val mode by choices("Mode", arrayOf("Modern", "Classic"), "Modern")
    private val width by float("Width", 1.5f, 0.5f..5f)
    private val maxrenderDistance by int("MaxrenderDistance", 50, 1..200)
    private val colorTeam by boolean("TeamColor", false)

    private var maxrenderDistanceSq = 0.0
        set(value) { field = if (value <= 0.0) maxrenderDistance.toDouble().pow(2.0) else value }

    private val tracerColor = ColorSettingsInteger(this, "TracerColor").with(255, 255, 255)

    private val entities by EntityLookup<EntityLivingBase>().filter { shouldrender(it) }

    private fun shouldrender(entity: EntityLivingBase): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        return (thePlayer.getDistanceSqToEntity(entity) <= maxrenderDistanceSq
                && isEntityHeightVisible(entity)
                && isSelected(entity, false)
                && (!isBot(entity) || entity is EntityPlayer))
    }

    private fun getColor(entity: EntityLivingBase): Color {
        if (entity.hurtTime > 0) return Color.RED
        if (entity is EntityPlayer && entity.isClientFriend()) return Color.BLUE
        if (colorTeam) entity.colorFromDisplayName()?.let { return it }
        return tracerColor.color()
    }

    val onrender3D = handler<Render3DEvent> {
        if (entities.isEmpty()) return@handler

        glPushMatrix()
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)
        enableGlCap(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(width)

        val renderManager = mc.renderManager
        val eyeX = renderManager.viewerPosX
        val eyeY = renderManager.viewerPosY + mc.thePlayer!!.eyeHeight
        val eyeZ = renderManager.viewerPosZ

        for (entity in entities) {
            val color = getColor(entity)
            val pos = entity.interpolatedPosition(entity.lastTickPos) - renderManager.renderPos

            glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0.8f)
            glBegin(GL_LINE_STRIP)
            glVertex3d(eyeX, eyeY, eyeZ)
            glVertex3d(pos.xCoord, pos.yCoord + entity.eyeHeight / 2.0, pos.zCoord)
            glEnd()
        }

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        resetCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }
}

object RiseFullBright : Module("RiseFullBright", Category.RISE, forcedDescription = "Rise - Full brightness") {

    private val mode by choices("Mode", arrayOf("Gamma", "Effect"), "Gamma")
    private var prevGamma = 0f

    override fun onEnable() {
        prevGamma = mc.gameSettings.gammaSetting
    }

    override fun onDisable() {
        mc.gameSettings.gammaSetting = prevGamma
    }

    val onUpdate = handler<UpdateEvent> {
        when (mode) {
            "Gamma" -> mc.gameSettings.gammaSetting = 1000f
            "Effect" -> {
                mc.gameSettings.gammaSetting = 1000f
                if (mc.thePlayer?.activePotionEffects?.any { it.potionID == 16 } == false) {
                    mc.thePlayer?.addPotionEffect(net.minecraft.potion.PotionEffect(16, 32767, 0, false, false))
                }
            }
        }
    }
}

object RiseChestESP : Module("RiseChestESP", Category.RISE, forcedDescription = "Rise - Chest ESP") {

    private val mode by choices("Mode", arrayOf("Glow", "Outline", "Box"), "Glow")
    private val range by int("Range", 64, 1..256)
    private val chestColor = ColorSettingsInteger(this, "ChestColor").with(255, 170, 0)
    private val enderChestColor = ColorSettingsInteger(this, "EnderChestColor").with(170, 0, 255)

    private val rangeSq: Double get() = range.toDouble() * range.toDouble()

    private var tickCounter = 0
    private var cachedChests = mutableListOf<Pair<TileEntity, Color>>()

    private fun refreshCache() {
        val world = mc.theWorld ?: return
        val player = mc.thePlayer ?: return

        cachedChests.clear()

        for (tileEntity in world.loadedTileEntityList) {
            val color = when (tileEntity) {
                is TileEntityEnderChest -> enderChestColor.color()
                is TileEntityChest -> chestColor.color()
                else -> continue
            }

            val bp = tileEntity.getPos()
            val dx = bp.getX() - player.posX
            val dy = bp.getY() - player.posY
            val dz = bp.getZ() - player.posZ
            if (dx * dx + dy * dy + dz * dz <= rangeSq) {
                cachedChests.add(tileEntity to color)
            }
        }
    }

    val onrender3D = handler<Render3DEvent> {
        tickCounter++
        if (tickCounter % 10 == 0) {
            refreshCache()
        }

        if (cachedChests.isEmpty()) return@handler

        glPushMatrix()
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)
        enableGlCap(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(2f)

        val renderPos = mc.renderManager.renderPos

        for ((tileEntity, color) in cachedChests) {
            val bp = tileEntity.getPos()
            val rx = bp.getX() - renderPos.xCoord
            val ry = bp.getY() - renderPos.yCoord
            val rz = bp.getZ() - renderPos.zCoord

            glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0.6f)

            glBegin(GL_LINE_STRIP)
            glVertex3d(rx, ry, rz)
            glVertex3d(rx + 1.0, ry, rz)
            glVertex3d(rx + 1.0, ry + 1.0, rz)
            glVertex3d(rx, ry + 1.0, rz)
            glVertex3d(rx, ry, rz)
            glVertex3d(rx, ry, rz + 1.0)
            glVertex3d(rx + 1.0, ry, rz + 1.0)
            glVertex3d(rx + 1.0, ry + 1.0, rz + 1.0)
            glVertex3d(rx, ry + 1.0, rz + 1.0)
            glVertex3d(rx, ry, rz + 1.0)
            glEnd()

            glBegin(GL_LINES)
            glVertex3d(rx + 1.0, ry, rz); glVertex3d(rx + 1.0, ry, rz + 1.0)
            glVertex3d(rx + 1.0, ry + 1.0, rz); glVertex3d(rx + 1.0, ry + 1.0, rz + 1.0)
            glVertex3d(rx, ry + 1.0, rz); glVertex3d(rx, ry + 1.0, rz + 1.0)
            glEnd()
        }

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        resetCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }
}

object RiseFreeCam : Module("RiseFreeCam", Category.RISE, forcedDescription = "Rise - Free camera movement") {

    private val speed by float("Speed", 2f, 0.1f..10f)
    private val verticalSpeed by float("VerticalSpeed", 1f, 0.1f..10f)

    private var fakeX = 0.0
    private var fakeY = 0.0
    private var fakeZ = 0.0
    private var fakeYaw = 0f
    private var fakePitch = 0f

    override fun onEnable() {
        val thePlayer = mc.thePlayer ?: return
        fakeX = thePlayer.posX
        fakeY = thePlayer.posY
        fakeZ = thePlayer.posZ
        fakeYaw = thePlayer.rotationYaw
        fakePitch = thePlayer.rotationPitch
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val yaw = Math.toRadians(fakeYaw.toDouble())
        if (mc.gameSettings.keyBindForward.isKeyDown) {
            fakeX -= sin(yaw) * speed
            fakeZ += cos(yaw) * speed
        }
        if (mc.gameSettings.keyBindBack.isKeyDown) {
            fakeX += sin(yaw) * speed
            fakeZ -= cos(yaw) * speed
        }
        if (mc.gameSettings.keyBindLeft.isKeyDown) {
            fakeX -= cos(yaw) * speed
            fakeZ -= sin(yaw) * speed
        }
        if (mc.gameSettings.keyBindRight.isKeyDown) {
            fakeX += cos(yaw) * speed
            fakeZ += sin(yaw) * speed
        }
        if (mc.gameSettings.keyBindJump.isKeyDown) {
            fakeY += verticalSpeed
        }
        if (mc.gameSettings.keyBindSneak.isKeyDown) {
            fakeY -= verticalSpeed
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is C03PacketPlayer) {
            event.cancelEvent()
        }
    }

    fun getFakeX() = fakeX
    fun getFakeY() = fakeY
    fun getFakeZ() = fakeZ
}

object RiseFreeLook : Module("RiseFreeLook", Category.RISE, forcedDescription = "Rise - Free look without moving body") {

    private val mode by choices("Mode", arrayOf("Normal", "Cinematic"), "Normal")

    private var freeLookYaw = 0f
    private var freeLookPitch = 0f
    private var originalYaw = 0f
    private var originalPitch = 0f
    private var active = false

    override fun onEnable() {
        val thePlayer = mc.thePlayer ?: return
        freeLookYaw = thePlayer.rotationYaw
        freeLookPitch = thePlayer.rotationPitch
        active = true
    }

    override fun onDisable() {
        active = false
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (active) {
            thePlayer.rotationYaw = freeLookYaw
            thePlayer.rotationPitch = freeLookPitch
        }
    }
}

object RiseBreadCrumbs : Module("RiseBreadCrumbs", Category.RISE, forcedDescription = "Rise - Movement trail") {

    private val mode by choices("Mode", arrayOf("Modern", "Classic"), "Modern")
    private val maxPoints by int("MaxPoints", 500, 50..5000)

    private val positions = ArrayDeque<net.minecraft.util.Vec3>()

    override fun onEnable() {
        positions.clear()
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.isMoving) {
            positions.addLast(net.minecraft.util.Vec3(thePlayer.posX, thePlayer.posY + 0.01, thePlayer.posZ))
            if (positions.size > maxPoints) {
                positions.removeFirst()
            }
        }
    }

    fun getPositions(): List<net.minecraft.util.Vec3> = positions
}

object RiseHurtCamera : Module("RiseHurtCamera", Category.RISE, forcedDescription = "Rise - Hurt camera effect modifier") {

    private val mode by choices("Mode", arrayOf("None", "Shake", "Zoom"), "None")
    private val intensity by float("Intensity", 1f, 0f..5f)

    private var hurtTime = 0
    private var shakeX = 0f
    private var shakeY = 0f

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.hurtTime > 0 && hurtTime < thePlayer.hurtTime) {
            when (mode) {
                "Shake" -> {
                    shakeX = (Math.random() * 2 - 1).toFloat() * intensity
                    shakeY = (Math.random() * 2 - 1).toFloat() * intensity
                }
                "Zoom" -> {
                    mc.gameSettings.fovSetting = (mc.gameSettings.fovSetting / (1f + intensity * 0.5f))
                }
            }
        }
        hurtTime = thePlayer.hurtTime
    }

    fun getShakeX() = shakeX
    fun getShakeY() = shakeY
}

object RiseKillEffect : Module("RiseKillEffect", Category.RISE, forcedDescription = "Rise - Kill effect animation") {

    private val mode by choices("Mode", arrayOf("None", "Lightning", "Explosion", "Particles"), "Lightning")

    private var lastKillPos: Vec3? = null
    private var killEffectTimer = 0

    val onAttack = handler<AttackEvent> { event ->
        val target = event.targetEntity
        if (target is EntityLivingBase && target.health <= 0f) {
            lastKillPos = Vec3(target.posX, target.posY, target.posZ)
            killEffectTimer = 20
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (killEffectTimer > 0) killEffectTimer--
    }

    val onrender3D = handler<Render3DEvent> {
        if (killEffectTimer <= 0 || lastKillPos == null) return@handler
        val world = mc.theWorld ?: return@handler
        val pos = lastKillPos!!

        when (mode) {
            "Lightning" -> {
                if (killEffectTimer == 20) {
                    world.addWeatherEffect(
                        net.minecraft.entity.effect.EntityLightningBolt(world, pos.xCoord, pos.yCoord, pos.zCoord)
                    )
                }
            }
            "Explosion" -> {
                if (killEffectTimer == 20) {
                    world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.EXPLOSION_HUGE,
                        pos.xCoord, pos.yCoord, pos.zCoord, 0.0, 0.0, 0.0
                    )
                }
            }
            "Particles" -> {
                for (i in 0..9) {
                    world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.FLAME,
                        pos.xCoord + (Math.random() - 0.5) * 2,
                        pos.yCoord + Math.random(),
                        pos.zCoord + (Math.random() - 0.5) * 2,
                        0.0, 0.0, 0.0
                    )
                }
            }
        }
    }
}

object RiseTargetInfo : Module("RiseTargetInfo", Category.RISE, forcedDescription = "Rise - Target info HUD") {

    private val mode by choices("Mode", arrayOf("Modern", "Classic"), "Modern")
    private val showHealth by boolean("Health", true)
    private val showDistance by boolean("Distance", true)
    private val showName by boolean("Name", true)
    private val showArmor by boolean("Armor", true)
    private val showPotion by boolean("Potions", true)

    private var target: EntityPlayer? = null

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        target = mc.theWorld?.loadedEntityList
            ?.filterIsInstance<EntityPlayer>()
            ?.filter { it != thePlayer && it.health > 0 }
            ?.filter { thePlayer.getDistanceToEntity(it) <= 8f }
            ?.minByOrNull { thePlayer.getDistanceToEntity(it) }
    }

    val onrender2D = handler<Render2DEvent> {
        val t = target ?: return@handler
        val thePlayer = mc.thePlayer ?: return@handler
        val fontrenderer = Fonts.fontRegular35
        var yOffset = 5f

        if (showName) {
            fontrenderer.drawString(t.displayName.unformattedText, 5f, yOffset, Color.WHITE.rgb, true)
            yOffset += fontrenderer.FONT_HEIGHT + 2
        }
        if (showHealth) {
            fontrenderer.drawString("HP: ${t.health.toInt()}", 5f, yOffset, Color.RED.rgb, true)
            yOffset += fontrenderer.FONT_HEIGHT + 2
        }
        if (showDistance) {
            val dist = thePlayer.getDistanceToEntity(t).toInt()
            fontrenderer.drawString("Dist: ${dist}m", 5f, yOffset, Color.WHITE.rgb, true)
            yOffset += fontrenderer.FONT_HEIGHT + 2
        }
        if (showArmor) {
            val totalArmor = t.totalArmorValue
            fontrenderer.drawString("Armor: $totalArmor", 5f, yOffset, Color.CYAN.rgb, true)
            yOffset += fontrenderer.FONT_HEIGHT + 2
        }
        if (showPotion) {
            for (effect in t.activePotionEffects) {
                val name = effect.effectName
                val duration = effect.duration / 20
                val amplifier = effect.amplifier + 1
                fontrenderer.drawString("$name $amplifier (${duration}s)", 5f, yOffset, Color.GREEN.rgb, true)
                yOffset += fontrenderer.FONT_HEIGHT + 2
            }
        }
    }
}

object RiseSessionStats : Module("RiseSessionStats", Category.RISE, forcedDescription = "Rise - Session statistics") {

    private val mode by choices("Mode", arrayOf("Modern", "Classic"), "Modern")
    private val showKills by boolean("Kills", true)
    private val showDeaths by boolean("Deaths", true)
    private val showKD by boolean("K/D", true)
    private val showPlayTime by boolean("PlayTime", true)

    private var kills = 0
    private var deaths = 0
    private var lastHealth = 20f
    private var startTime = 0L

    override fun onEnable() {
        if (startTime == 0L) startTime = System.currentTimeMillis()
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.health <= 0f && lastHealth > 0f) {
            deaths++
        }
        lastHealth = thePlayer.health
    }

    val onrender2D = handler<Render2DEvent> {
        val fontrenderer = Fonts.fontRegular35
        var yOffset = 5f

        if (showKills) {
            fontrenderer.drawString("Kills: $kills", 5f, yOffset, Color.WHITE.rgb, true)
            yOffset += fontrenderer.FONT_HEIGHT + 2
        }
        if (showDeaths) {
            fontrenderer.drawString("Deaths: $deaths", 5f, yOffset, Color.WHITE.rgb, true)
            yOffset += fontrenderer.FONT_HEIGHT + 2
        }
        if (showKD) {
            val kd = if (deaths > 0) kills.toFloat() / deaths else kills.toFloat()
            fontrenderer.drawString("K/D: ${"%.2f".format(kd)}", 5f, yOffset, Color.WHITE.rgb, true)
            yOffset += fontrenderer.FONT_HEIGHT + 2
        }
        if (showPlayTime) {
            val elapsed = (System.currentTimeMillis() - startTime) / 1000
            val hours = elapsed / 3600
            val minutes = (elapsed % 3600) / 60
            val seconds = elapsed % 60
            fontrenderer.drawString("Time: ${"%02d".format(hours)}:${"%02d".format(minutes)}:${"%02d".format(seconds)}", 5f, yOffset, Color.WHITE.rgb, true)
        }
    }
}

object RiseViewBobbing : Module("RiseViewBobbing", Category.RISE, forcedDescription = "Rise - View bobbing modifier") {

    private val mode by choices("Mode", arrayOf("None", "Custom", "Exaggerated"), "None")
    private val intensity by float("Intensity", 1f, 0f..5f)

    fun getBobbingIntensity(): Float = when (mode) {
        "None" -> 0f
        "Custom" -> intensity
        "Exaggerated" -> intensity * 3f
        else -> 1f
    }
}

object RiseNoCameraClip : Module("RiseNoCameraClip", Category.RISE, forcedDescription = "Rise - No camera clip through walls") {

    private val mode by choices("Mode", arrayOf("Vanilla", "NCP"), "Vanilla")

    val onUpdate = handler<UpdateEvent> {
        if (mc.gameSettings.thirdPersonView != 0) {
            val thePlayer = mc.thePlayer ?: return@handler
            val rayTrace = mc.theWorld?.rayTraceBlocks(
                net.minecraft.util.Vec3(thePlayer.posX, thePlayer.posY + thePlayer.eyeHeight, thePlayer.posZ),
                net.minecraft.util.Vec3(mc.renderViewEntity.posX, mc.renderViewEntity.posY + mc.renderViewEntity.eyeHeight, mc.renderViewEntity.posZ),
                false, false, true
            )
            if (rayTrace != null && rayTrace.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                mc.gameSettings.thirdPersonView = 0
            }
        }
    }
}

object RiseUnlimitedChat : Module("RiseUnlimitedChat", Category.RISE, forcedDescription = "Rise - Unlimited chat lines") {

    private val maxLines by int("MaxLines", 256, 100..10000)

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is net.minecraft.network.play.server.S02PacketChat) {
            val chat = mc.ingameGUI.chatGUI
            if (chat.sentMessages.size > maxLines) {
                chat.sentMessages.removeAt(0)
            }
        }
    }
}

object RiseOreESP : Module("RiseOreESP", Category.RISE, forcedDescription = "Rise - Ore ESP") {

    private val range by int("Range", 64, 1..256)
    private val diamond by boolean("Diamond", true)
    private val emerald by boolean("Emerald", true)
    private val gold by boolean("Gold", true)
    private val iron by boolean("Iron", false)
    private val coal by boolean("Coal", false)
    private val lapis by boolean("Lapis", false)
    private val redstone by boolean("Redstone", false)

    private val rangeSq: Int get() = range * range

    private fun getOreColor(block: net.minecraft.block.Block): Color? = when {
        block == Blocks.diamond_ore && diamond -> Color(0, 255, 255)
        block == Blocks.emerald_ore && emerald -> Color(0, 255, 0)
        block == Blocks.gold_ore && gold -> Color(255, 255, 0)
        block == Blocks.iron_ore && iron -> Color(200, 150, 100)
        block == Blocks.coal_ore && coal -> Color(60, 60, 60)
        block == Blocks.lapis_ore && lapis -> Color(0, 0, 200)
        block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore && redstone -> Color(255, 0, 0)
        else -> null
    }

    val onrender3D = handler<Render3DEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        glPushMatrix()
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)
        enableGlCap(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(2f)

        val renderPos = mc.renderManager.renderPos
        val px = thePlayer.posX.toInt()
        val py = thePlayer.posY.toInt()
        val pz = thePlayer.posZ.toInt()

        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    val bx = px + x
                    val by = py + y
                    val bz = pz + z
                    val dist = x * x + y * y + z * z
                    if (dist > rangeSq) continue

                    val blockPos = BlockPos(bx, by, bz)
                    val block = world.getBlockState(blockPos).block
                    val color = getOreColor(block) ?: continue

                    val rx = bx - renderPos.xCoord
                    val ry = by - renderPos.yCoord
                    val rz = bz - renderPos.zCoord

                    glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0.7f)

                    glBegin(GL_LINE_STRIP)
                    glVertex3d(rx, ry, rz)
                    glVertex3d(rx + 1.0, ry, rz)
                    glVertex3d(rx + 1.0, ry + 1.0, rz)
                    glVertex3d(rx, ry + 1.0, rz)
                    glVertex3d(rx, ry, rz)
                    glVertex3d(rx, ry, rz + 1.0)
                    glVertex3d(rx + 1.0, ry, rz + 1.0)
                    glVertex3d(rx + 1.0, ry + 1.0, rz + 1.0)
                    glVertex3d(rx, ry + 1.0, rz + 1.0)
                    glVertex3d(rx, ry, rz + 1.0)
                    glEnd()

                    glBegin(GL_LINES)
                    glVertex3d(rx + 1.0, ry, rz); glVertex3d(rx + 1.0, ry, rz + 1.0)
                    glVertex3d(rx + 1.0, ry + 1.0, rz); glVertex3d(rx + 1.0, ry + 1.0, rz + 1.0)
                    glVertex3d(rx, ry + 1.0, rz); glVertex3d(rx, ry + 1.0, rz + 1.0)
                    glEnd()
                }
            }
        }

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        resetCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }
}

object RiseM2DESP : Module("RiseM2DESP", Category.RISE, forcedDescription = "Rise - 2D ESP") {

    private val mode by choices("Mode", arrayOf("Modern", "Classic"), "Modern")
    private val maxrenderDistance by int("MaxrenderDistance", 50, 1..200)
    private val colorTeam by boolean("TeamColor", false)

    private var maxrenderDistanceSq = 0.0
        set(value) { field = if (value <= 0.0) maxrenderDistance.toDouble().pow(2.0) else value }

    private val esp2DColor = ColorSettingsInteger(this, "2DColor").with(255, 255, 255)
    private val entities by EntityLookup<EntityLivingBase>().filter { shouldrender(it) }

    private fun shouldrender(entity: EntityLivingBase): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        return (thePlayer.getDistanceSqToEntity(entity) <= maxrenderDistanceSq
                && isEntityHeightVisible(entity)
                && isSelected(entity, false)
                && (!isBot(entity) || entity is EntityPlayer))
    }

    private fun getColor(entity: EntityLivingBase): Color {
        if (entity.hurtTime > 0) return Color.RED
        if (entity is EntityPlayer && entity.isClientFriend()) return Color.BLUE
        if (colorTeam) entity.colorFromDisplayName()?.let { return it }
        return esp2DColor.color()
    }

    val onrender2D = handler<Render2DEvent> {
        if (entities.isEmpty()) return@handler

        val mvMatrix = WorldToScreen.getMatrix(GL_MODELVIEW_MATRIX)
        val projectionMatrix = WorldToScreen.getMatrix(GL_PROJECTION_MATRIX)

        glPushMatrix()
        enableGlCap(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glLineWidth(2f)

        val renderPos = mc.renderManager.renderPos

        for (entity in entities) {
            val color = getColor(entity)
            val pos = entity.interpolatedPosition(entity.lastTickPos) - renderPos
            val bb = entity.hitBox.offset(-entity.currPos + pos)

            val vertices = arrayOf(
                doubleArrayOf(bb.minX, bb.minY, bb.minZ),
                doubleArrayOf(bb.minX, bb.maxY, bb.minZ),
                doubleArrayOf(bb.maxX, bb.maxY, bb.minZ),
                doubleArrayOf(bb.maxX, bb.minY, bb.minZ),
                doubleArrayOf(bb.minX, bb.minY, bb.maxZ),
                doubleArrayOf(bb.minX, bb.maxY, bb.maxZ),
                doubleArrayOf(bb.maxX, bb.maxY, bb.maxZ),
                doubleArrayOf(bb.maxX, bb.minY, bb.maxZ)
            )

            var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
            var maxX = -1f; var maxY = -1f

            for (v in vertices) {
                val screen = WorldToScreen.worldToScreen(
                    org.lwjgl.util.vector.Vector3f(v[0].toFloat(), v[1].toFloat(), v[2].toFloat()),
                    mvMatrix, projectionMatrix, mc.displayWidth, mc.displayHeight
                ) ?: continue
                minX = min(minX, screen.x)
                minY = min(minY, screen.y)
                maxX = max(maxX, screen.x)
                maxY = max(maxY, screen.y)
            }

            if (minX < mc.displayWidth && maxX > 0 && minY < mc.displayHeight && maxY > 0) {
                glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0.8f)
                glBegin(GL_LINE_LOOP)
                glVertex2f(minX, minY); glVertex2f(minX, maxY)
                glVertex2f(maxX, maxY); glVertex2f(maxX, minY)
                glEnd()
            }
        }

        glEnable(GL_TEXTURE_2D)
        resetCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }
}

object RiseJumpCircles : Module("RiseJumpCircles", Category.RISE, forcedDescription = "Rise - Jump circle effects") {

    private val mode by choices("Mode", arrayOf("Modern", "Classic"), "Modern")
    private val color = ColorSettingsInteger(this, "CircleColor").with(255, 255, 255)

    private val positions = ArrayDeque<Vec3>()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.onGround && thePlayer.isMoving) {
            positions.addLast(Vec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ))
            if (positions.size > 20) positions.removeFirst()
        }
    }

    val onrender3D = handler<Render3DEvent> {
        if (positions.isEmpty()) return@handler
        val renderPos = mc.renderManager.renderPos

        glPushMatrix()
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)
        enableGlCap(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(2f)

        val c = color.color()
        glColor4f(c.red / 255f, c.green / 255f, c.blue / 255f, 0.7f)

        for (pos in positions) {
            val rx = pos.xCoord - renderPos.xCoord
            val ry = pos.yCoord - renderPos.yCoord
            val rz = pos.zCoord - renderPos.zCoord

            glBegin(GL_LINE_LOOP)
            for (i in 0..36) {
                val angle = Math.toRadians((i * 10).toDouble())
                glVertex3d(rx + cos(angle) * 0.5, ry, rz + sin(angle) * 0.5)
            }
            glEnd()
        }

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        resetCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }
}

object RiseParticles : Module("RiseParticles", Category.RISE, forcedDescription = "Rise - Particle effects") {

    private val mode by choices("Mode", arrayOf("None", "Custom", "Rainbow"), "Custom")
    private val particleType by choices("ParticleType", arrayOf("Flame", "Portal", "Crit", "MagicCrit", "Smoke", "Heart", "Note"), "Flame")
    private val density by int("Density", 2, 1..20)
    private val spread by float("Spread", 0.5f, 0.1f..3f)

    val onUpdate = handler<UpdateEvent> {
        if (mode == "None") return@handler
        val thePlayer = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        for (i in 0 until density) {
            val x = thePlayer.posX + (Math.random() - 0.5) * spread
            val y = thePlayer.posY + Math.random() * spread * 2
            val z = thePlayer.posZ + (Math.random() - 0.5) * spread

            val particle = when (particleType) {
                "Flame" -> net.minecraft.util.EnumParticleTypes.FLAME
                "Portal" -> net.minecraft.util.EnumParticleTypes.PORTAL
                "Crit" -> net.minecraft.util.EnumParticleTypes.CRIT
                "MagicCrit" -> net.minecraft.util.EnumParticleTypes.CRIT_MAGIC
                "Smoke" -> net.minecraft.util.EnumParticleTypes.SMOKE_NORMAL
                "Heart" -> net.minecraft.util.EnumParticleTypes.HEART
                "Note" -> net.minecraft.util.EnumParticleTypes.NOTE
                else -> net.minecraft.util.EnumParticleTypes.FLAME
            }
            world.spawnParticle(particle, x, y, z, 0.0, 0.0, 0.0)
        }
    }
}

object RiseAmbience : Module("RiseAmbience", Category.RISE, forcedDescription = "Rise - Ambient effects") {

    private val mode by choices("Mode", arrayOf("None", "Cave", "Nether", "End"), "None")
    private val intensity by float("Intensity", 1f, 0f..5f)

    private var savedGamma = 1f

    override fun onEnable() {
        savedGamma = mc.gameSettings.gammaSetting
    }

    override fun onDisable() {
        mc.gameSettings.gammaSetting = savedGamma
    }

    val onUpdate = handler<UpdateEvent> {
        when (mode) {
            "None" -> mc.gameSettings.gammaSetting = savedGamma
            "Cave" -> mc.gameSettings.gammaSetting = 0.2f * intensity
            "Nether" -> mc.gameSettings.gammaSetting = 8f * intensity
            "End" -> mc.gameSettings.gammaSetting = 12f * intensity
        }
    }
}

object RiseBedPlates : Module("RiseBedPlates", Category.RISE, forcedDescription = "Rise - Bed plate indicators") {

    private val mode by choices("Mode", arrayOf("Modern", "Classic"), "Modern")
    private val range by int("Range", 64, 1..256)
    private val bedColor = ColorSettingsInteger(this, "BedColor").with(255, 0, 0)

    private val rangeSq: Int get() = range * range

    val onrender3D = handler<Render3DEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        glPushMatrix()
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)
        enableGlCap(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(2f)

        val renderPos = mc.renderManager.renderPos
        val px = thePlayer.posX.toInt()
        val py = thePlayer.posY.toInt()
        val pz = thePlayer.posZ.toInt()

        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    val bx = px + x; val by = py + y; val bz = pz + z
                    if (x * x + y * y + z * z > rangeSq) continue

                    val block = world.getBlockState(BlockPos(bx, by, bz)).block
                    if (block != Blocks.bed) continue

                    val c = bedColor.color()
                    val rx = bx - renderPos.xCoord
                    val ry = by - renderPos.yCoord
                    val rz = bz - renderPos.zCoord

                    glColor4f(c.red / 255f, c.green / 255f, c.blue / 255f, 0.6f)
                    glBegin(GL_LINE_STRIP)
                    glVertex3d(rx, ry, rz); glVertex3d(rx + 1.0, ry, rz)
                    glVertex3d(rx + 1.0, ry + 0.56, rz); glVertex3d(rx, ry + 0.56, rz)
                    glVertex3d(rx, ry, rz)
                    glEnd()
                }
            }
        }

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        resetCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }
}

object RiseBlackHoleOrbit : Module("RiseBlackHoleOrbit", Category.RISE, forcedDescription = "Rise - Black hole orbit effect") {

    private val speed by float("Speed", 1f, 0.1f..5f)
    private val radius by float("Radius", 50f, 10f..200f)

    private var angle = 0f

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        angle += speed * 0.05f
        if (angle > 360f) angle -= 360f

        val x = thePlayer.posX + cos(Math.toRadians(angle.toDouble())) * radius
        val z = thePlayer.posZ + sin(Math.toRadians(angle.toDouble())) * radius
        thePlayer.setPosition(x, thePlayer.posY, z)
    }
}

object RiseStreamer : Module("RiseStreamer", Category.RISE, forcedDescription = "Rise - Streamer mode (hide sensitive info)") {

    private val hideIP by boolean("HideIP", true)
    private val hideName by boolean("HideName", true)
    private val hideCoords by boolean("HideCoords", true)
    private val hideServerInfo by boolean("HideServerInfo", true)
    private val replacementName by text("ReplacementName", "You")

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is net.minecraft.network.play.server.S02PacketChat) {
            val packet = event.packet
            val text = packet.chatComponent.unformattedText
            if (hideName) {
                val name = mc.thePlayer?.name ?: return@handler
                if (text.contains(name)) {
                    event.cancelEvent()
                    val newText = text.replace(name, replacementName)
                    mc.thePlayer?.addChatMessage(
                        net.minecraft.util.ChatComponentText(newText)
                    )
                }
            }
        }
    }

    val onrender2D = handler<Render2DEvent> {
        if (hideCoords) {
            mc.fontRendererObj.drawString(
                "XYZ: $replacementName",
                2f, 2f, Color.WHITE.rgb, true
            )
        }
    }
}