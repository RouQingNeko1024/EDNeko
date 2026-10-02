package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extras.ColorManager
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object TargetStrafe : Module("TargetStrafe", Category.MOVEMENT) {

    private val mode by choices("Mode", arrayOf("Normal", "Fake"), "Normal")
    private val thirdPersonView by boolean("ThirdPersonView", false)
    private val renderMode by choices("RenderMode", arrayOf("Circle", "Polygon", "Exhibition", "None"), "Polygon")
    private val lineWidth by float("LineWidth", 1f, 1f..10f) { renderMode != "None" }
    private val radiusMode by choices("RadiusMode", arrayOf("Normal", "Strict"), "Normal")
    private val radius by float("Radius", 0.5f, 0.1f..5.0f)
    private val onlyOnGround by boolean("OnlyOnGround", false)
    private val holdSpace by boolean("HoldSpace", false)
    private val onlySpeed by boolean("OnlySpeed", true)
    private val onlyFlight by boolean("OnlyFlight", true)
    private val onlyVisual by boolean("OnlyVisual", false)
    private val circleColorMode by choices("CircleColorMode", arrayOf("Rainbow", "Custom"), "Rainbow")
    private val circleColorRed by int("CircleColorRed", 255, 0..255) { circleColorMode == "Custom" }
    private val circleColorGreen by int("CircleColorGreen", 255, 0..255) { circleColorMode == "Custom" }
    private val circleColorBlue by int("CircleColorBlue", 255, 0..255) { circleColorMode == "Custom" }
    private val fakeOrbitSpeed by float("FakeOrbitSpeed", 0.1f, 0.01f..1.0f) { mode == "Fake" }

    private var direction = -1.0
    var targetEntity: EntityLivingBase? = null
    var isEnabled = false
    var doStrafe = true
    var fakeActive = false
    var callBackYaw = 0.0

    private var fakeOrbitAngle = 0.0
    private var fakeOrbitPos: Vec3? = null
    private var fakeLastOrbitPos: Vec3? = null

    val onCameraPosition = handler<CameraPositionEvent> { event ->
        if (mode != "Fake" || !fakeActive || targetEntity == null) return@handler
        if (FreeCam.handleEvents()) return@handler

        val currentPos = fakeOrbitPos ?: return@handler
        val lastPos = fakeLastOrbitPos ?: currentPos

        event.result = FreeCam.PositionPair(currentPos, lastPos, lastPos)
    }

    val onRender3D = handler<Render3DEvent> { event ->
        val shouldRender = if (mode == "Fake") {
            fakeActive && targetEntity != null
        } else {
            canStrafe()
        }

        if (renderMode != "None" && shouldRender) {
            if (targetEntity == null || !doStrafe) return@handler

            when (renderMode) {
                "Circle" -> drawCircle(event)
                "Exhibition" -> drawExhibition(event)
                "Polygon" -> drawPolygon(event)
            }
        }
    }

    private fun drawCircle(event: Render3DEvent) {
        GL11.glPushMatrix()
        GL11.glDisable(3553)
        GL11.glEnable(2848)
        GL11.glEnable(2881)
        GL11.glEnable(2832)
        GL11.glEnable(3042)
        GL11.glBlendFunc(770, 771)
        GL11.glHint(3154, 4354)
        GL11.glHint(3155, 4354)
        GL11.glHint(3153, 4354)
        GL11.glDisable(2929)
        GL11.glDepthMask(false)
        GL11.glLineWidth(lineWidth)
        GL11.glBegin(3)
        val x = targetEntity!!.lastTickPosX + (targetEntity!!.posX - targetEntity!!.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = targetEntity!!.lastTickPosY + (targetEntity!!.posY - targetEntity!!.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = targetEntity!!.lastTickPosZ + (targetEntity!!.posZ - targetEntity!!.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        for (i in 0..359) {
            val color = if (circleColorMode == "Rainbow") {
                Color(Color.HSBtoRGB(((mc.thePlayer.ticksExisted / 70.0 + sin(i / 50.0 * 1.75)) % 1.0f).toFloat(), 0.7f, 1.0f))
            } else {
                Color(circleColorRed, circleColorGreen, circleColorBlue)
            }
            GL11.glColor3f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f)
            GL11.glVertex3d(
                x + radius * cos(i * 6.283185307179586 / 45.0),
                y,
                z + radius * sin(i * 6.283185307179586 / 45.0)
            )
        }
        GL11.glEnd()
        GL11.glDepthMask(true)
        GL11.glEnable(2929)
        GL11.glDisable(2848)
        GL11.glDisable(2881)
        GL11.glEnable(2832)
        GL11.glEnable(3553)
        GL11.glPopMatrix()
    }

    private fun drawExhibition(event: Render3DEvent) {
        val x = targetEntity!!.lastTickPosX + (targetEntity!!.posX - targetEntity!!.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = targetEntity!!.lastTickPosY + (targetEntity!!.posY - targetEntity!!.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = targetEntity!!.lastTickPosZ + (targetEntity!!.posZ - targetEntity!!.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ

        GL11.glPushMatrix()
        GL11.glDisable(3553)
        GL11.glEnable(2848)
        GL11.glEnable(2881)
        GL11.glEnable(2832)
        GL11.glEnable(3042)
        GL11.glBlendFunc(770, 771)
        GL11.glHint(3154, 4354)
        GL11.glHint(3155, 4354)
        GL11.glHint(3153, 4354)
        GL11.glDisable(2929)
        GL11.glDepthMask(false)

        GL11.glLineWidth(lineWidth + 2.0f)
        GL11.glBegin(3)
        GL11.glColor3f(0.0f, 0.0f, 0.0f)
        for (i in 0..10) {
            val angle = i * 2 * Math.PI / 10
            GL11.glVertex3d(x + radius * cos(angle), y, z + radius * sin(angle))
        }
        GL11.glVertex3d(x + radius * cos(0.0), y, z + radius * sin(0.0))
        GL11.glEnd()

        GL11.glLineWidth(lineWidth)
        GL11.glBegin(3)
        for (i in 0..10) {
            val color = if (circleColorMode == "Rainbow") {
                Color(Color.HSBtoRGB(((mc.thePlayer.ticksExisted / 70.0 + sin(i / 10.0 * 1.75)) % 1.0f).toFloat(), 0.7f, 1.0f))
            } else {
                Color(circleColorRed, circleColorGreen, circleColorBlue)
            }
            GL11.glColor3f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f)
            val angle = i * 2 * Math.PI / 10
            GL11.glVertex3d(x + radius * cos(angle), y, z + radius * sin(angle))
        }
        GL11.glVertex3d(x + radius * cos(0.0), y, z + radius * sin(0.0))
        GL11.glEnd()

        GL11.glDepthMask(true)
        GL11.glEnable(2929)
        GL11.glDisable(2848)
        GL11.glDisable(2881)
        GL11.glEnable(2832)
        GL11.glEnable(3553)
        GL11.glPopMatrix()
    }

    private fun drawPolygon(event: Render3DEvent) {
        val rad = radius
        val counter = intArrayOf(0)
        GL11.glPushMatrix()
        GL11.glDisable(3553)
        GL11.glDisable(2929)
        GL11.glDepthMask(false)
        GL11.glLineWidth(lineWidth)
        GL11.glBegin(3)
        val x = targetEntity!!.lastTickPosX + (targetEntity!!.posX - targetEntity!!.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = targetEntity!!.lastTickPosY + (targetEntity!!.posY - targetEntity!!.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = targetEntity!!.lastTickPosZ + (targetEntity!!.posZ - targetEntity!!.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        for (i in 0..10) {
            counter[0]++
            val segments = when {
                rad < 0.8 -> 3.0
                rad < 1.5 -> 4.0
                rad < 2.0 -> 5.0
                rad < 2.4 -> 6.0
                rad < 2.7 -> 7.0
                rad < 6.0 -> 8.0
                rad < 7.0 -> 9.0
                else -> 10.0
            }
            val color = if (circleColorMode == "Rainbow") {
                Color(ColorManager.astolfoRainbow(counter[0] * 100, 5, 107))
            } else {
                Color(circleColorRed, circleColorGreen, circleColorBlue)
            }
            GL11.glColor3f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f)
            GL11.glVertex3d(
                x + rad * cos(i * 6.283185307179586 / segments),
                y,
                z + rad * sin(i * 6.283185307179586 / segments)
            )
        }
        GL11.glEnd()
        GL11.glDepthMask(true)
        GL11.glEnable(2929)
        GL11.glEnable(3553)
        GL11.glPopMatrix()
    }

    val onMove = handler<MoveEvent> { event ->
        if (mode == "Fake") return@handler

        if (doStrafe && (!onlyOnGround || mc.thePlayer.onGround)) {
            if (!canStrafe()) {
                isEnabled = false
                return@handler
            }
            if (onlyVisual) return@handler

            var aroundVoid = false
            for (x in -1..0) for (z in -1..0)
                if (isVoid(x, z))
                    aroundVoid = true
            if (aroundVoid)
                direction *= -1

            val strict = if (radiusMode == "Strict") 1 else 0

            MovementUtils.doTargetStrafe(
                targetEntity!!,
                direction,
                radius.toDouble(),
                event,
                strict
            )
            callBackYaw = RotationUtils.getRotationsEntity(targetEntity!!).yaw.toDouble()
            isEnabled = true

            if (thirdPersonView) {
                mc.gameSettings.thirdPersonView = if (canStrafe()) 3 else 0
            }
        } else {
            isEnabled = false
            if (thirdPersonView && mc.gameSettings.thirdPersonView == 3) {
                mc.gameSettings.thirdPersonView = 0
            }
        }
    }

    private fun canStrafe(): Boolean {
        targetEntity = if (KillAura.state) KillAura.target else null
        return state && targetEntity != null
                && (!holdSpace || mc.gameSettings.keyBindJump.isKeyDown)
                && (!onlySpeed || LiquidBounce.moduleManager[Speed::class.java]?.state == true)
                && (!onlyFlight || LiquidBounce.moduleManager[Fly::class.java]?.state == true)
    }

    val onStrafe = handler<UpdateEvent> {
        targetEntity = if (KillAura.state) KillAura.target else null

        if (mode == "Fake") {
            val player = mc.thePlayer

            if (player.isMoving) {
                fakeActive = false
                fakeOrbitPos = null
                fakeLastOrbitPos = null
                isEnabled = false
                return@handler
            }

            if (targetEntity != null) {
                val target = targetEntity!!

                if (!fakeActive) {
                    val dx = player.posX - target.posX
                    val dz = player.posZ - target.posZ
                    fakeOrbitAngle = atan2(dz, dx)
                    fakeOrbitPos = Vec3(player.posX, player.posY, player.posZ)
                    fakeLastOrbitPos = fakeOrbitPos
                    fakeActive = true
                }

                fakeOrbitAngle += direction * fakeOrbitSpeed

                val orbitX = target.posX + radius * cos(fakeOrbitAngle)
                val orbitZ = target.posZ + radius * sin(fakeOrbitAngle)
                val orbitY = player.posY

                fakeLastOrbitPos = fakeOrbitPos
                fakeOrbitPos = Vec3(orbitX, orbitY, orbitZ)

                isEnabled = true
            } else {
                fakeActive = false
                fakeOrbitPos = null
                fakeLastOrbitPos = null
                isEnabled = false
            }
            return@handler
        }

        isEnabled = canStrafe()

        if (thirdPersonView) {
            if (isEnabled) {
                mc.gameSettings.thirdPersonView = 3
            } else if (mc.gameSettings.thirdPersonView == 3) {
                mc.gameSettings.thirdPersonView = 0
            }
        }
    }

    private fun isVoid(xPos: Int, zPos: Int): Boolean {
        if (mc.thePlayer.posY < 0.0) return true
        var off = 0
        while (off < mc.thePlayer.posY.toInt() + 2) {
            val bb = mc.thePlayer.entityBoundingBox.offset(xPos.toDouble(), -off.toDouble(), zPos.toDouble())
            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                off += 2
                continue
            }
            return false
        }
        return true
    }

    override fun onDisable() {
        fakeActive = false
        fakeOrbitPos = null
        fakeLastOrbitPos = null
        isEnabled = false
    }

    override val tag get() = if (mode == "Fake") "Fake" else renderMode
}