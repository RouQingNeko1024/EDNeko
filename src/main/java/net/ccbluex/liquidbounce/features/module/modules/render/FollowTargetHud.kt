/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.client.Teams
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.attack.EntityUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import kotlin.math.roundToInt

object FollowTargetHud : Module("FollowTargetHud", Category.RENDER) {

    private val zoomIn by boolean("ZoomIn", true)
    private val zoomTicks by int("ZoomInTicks", 4, 2..15) { zoomIn }
    private val mode by choices("Mode", arrayOf("Juul", "Jello", "Material", "Material2", "Arris", "FDP"), "Juul")
    private val font by font("Font", Fonts.font40)
    private val materialShadow by boolean("MaterialShadow", false) { mode == "Material" || mode == "Material2" }
    private val fdpVertical by boolean("FDPVertical", false) { mode == "FDP" }
    private val fdpText by boolean("FDPDrawText", true) { mode == "FDP" && !fdpVertical }
    private val fdpRed by boolean("FDPRed", false) { mode == "FDP" }
    private val smoothMove by boolean("SmoothHudMove", true)
    private val smoothValue by float("SmoothHudMoveValue", 5.2f, 1f..8f) { smoothMove }
    private val smoothRot by boolean("SmoothHudRotations", true)
    private val rotSmoothValue by float("SmothHudRotationValue", 2.1f, 1f..6f) { smoothRot }
    private val jelloColorValue by boolean("JelloHPColor", true) { mode == "Jello" }
    private val jelloAlphaValue by int("JelloAlpha", 170, 0..255) { mode == "Jello" }
    private val scaleValue by float("Scale", 1F, 1F..4F)
    private val staticScale by boolean("StaticScale", false)
    private val translateY by float("TanslateY", 0.55F, -2F..2F)
    private val translateX by float("TranslateX", 0F, -2F..2F)

    private var xChange = translateX * 20

    private var targetTicks = 0
    private var entityKeep = "yes"

    private var lastX = 0.0
    private var lastY = 0.0
    private var lastZ = 0.0

    private var lastYaw = 0.0f
    private var lastPitch = 0.0f

    private val HEALTH_FORMAT = DecimalFormat("#.#")
    private val DISTANCE_FORMAT = DecimalFormat("0.0")

    val onRender3D = handler<Render3DEvent> {
        if (mc.thePlayer == null) return@handler
        
        for (entity in mc.theWorld.loadedEntityList) {
            if (EntityUtils.isSelected(entity, false)) {
                renderNameTag(entity as net.minecraft.entity.EntityLivingBase, entity.name)
            }
        }
    }

    private fun getPlayerName(entity: net.minecraft.entity.EntityLivingBase): String {
        val name = entity.displayName.formattedText
        var pre = ""
        
        if (LiquidBounce.fileManager.friendsConfig.isFriend(entity.name)) {
            pre = "$pre§b[Friend] "
        }
        if (Teams.isInYourTeam(entity)) {
            pre = "$pre§a[TEAM] "
        }
        if (AntiBot.isBot(entity)) {
            pre = "$pre§e[BOT] "
        }
        if (!AntiBot.isBot(entity) && !Teams.isInYourTeam(entity)) {
            pre = if (LiquidBounce.fileManager.friendsConfig.isFriend(entity.name)) {
                "§b[Friend] §c"
            } else {
                "§c"
            }
        }
        return name + pre
    }

    private fun renderNameTag(entity: net.minecraft.entity.EntityLivingBase, tag: String) {
        xChange = translateX * 20

        val currentTarget = KillAura.target
        
        if (entity != currentTarget && entity.name != entityKeep) {
            return
        } else if (entity == currentTarget) {
            entityKeep = entity.name!!
            targetTicks++
            if (targetTicks >= zoomTicks + 2) {
                targetTicks = zoomTicks + 1
            }
        } else if (currentTarget == null) {
            targetTicks--
            if (targetTicks <= -1) {
                targetTicks = 0
                entityKeep = "dg636 top"
            }
        }

        if (targetTicks == 0) {
            return
        }

        val fontRenderer = font

        glPushMatrix()

        val renderManager = mc.renderManager
        val timer = mc.timer

        if (smoothMove) {
            lastX += ((entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX).toDouble() - lastX) / smoothValue.toDouble()
            lastY += ((entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY + entity.eyeHeight.toDouble() + translateY.toDouble()).toDouble() - lastY) / smoothValue.toDouble()
            lastZ += ((entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ).toDouble() - lastZ) / smoothValue.toDouble()

            glTranslated(lastX, lastY, lastZ)
        } else {
            glTranslated(
                entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY + entity.eyeHeight.toDouble() + translateY.toDouble(),
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
            )
        }

        if (smoothRot) {
            lastYaw += (-mc.renderManager.playerViewY - lastYaw) / rotSmoothValue
            lastPitch += (mc.renderManager.playerViewX - lastPitch) / rotSmoothValue

            glRotatef(lastYaw, 0F, 1F, 0F)
            glRotatef(lastPitch, 1F, 0F, 0F)
        } else {
            glRotatef(-mc.renderManager.playerViewY, 0F, 1F, 0F)
            glRotatef(mc.renderManager.playerViewX, 1F, 0F, 0F)
        }

        var distance = mc.thePlayer.getDistanceToEntity(entity) / 4F

        if (distance < 1F) distance = 1F

        if (staticScale) distance = 1F

        var scale = (distance / 150F) * scaleValue
        if (zoomIn) {
            scale *= (targetTicks.coerceAtMost(zoomTicks) / zoomTicks).toFloat()
        }

        glDisable(GL_LIGHTING)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val name = entity.displayName.unformattedText
        var healthPercent = entity.health / entity.maxHealth
        if (healthPercent > 1) healthPercent = 1F

        when (mode.lowercase()) {
            "juul" -> {
                glScalef(-scale * 2, -scale * 2, scale * 2)
                RenderUtils.drawRoundedRect(-120f + xChange, -16f, -50f + xChange, 10f, Color(64, 64, 64, 255).rgb, 5f)
                RenderUtils.drawRoundedRect(-110f + xChange, 0f, -20f + xChange, 35f, Color(96, 96, 96, 255).rgb, 5f)

                fontRenderer.drawString("Attacking", -105 + xChange.toInt(), -13, Color.WHITE.rgb)
                fontRenderer.drawString(tag, -106 + xChange.toInt(), 10, Color.WHITE.rgb)

                val distanceString = "⤢" + DISTANCE_FORMAT.format(mc.thePlayer.getDistanceToEntity(entity))
                fontRenderer.drawString(distanceString, -25 - fontRenderer.getStringWidth(distanceString).toInt() + xChange.toInt(), 10, Color.WHITE.rgb)

                RenderUtils.drawRoundedRect(-104f + xChange, 22f, -50f + xChange, 30f, Color(64, 64, 64, 255).rgb, 1f)
                RenderUtils.drawRoundedRect(-104f + xChange, 22f, -104f + (healthPercent * 54) + xChange, 30f, Color.WHITE.rgb, 1f)
            }

            "material" -> {
                glScalef(-scale * 2, -scale * 2, scale * 2)

                if (materialShadow) {
                    RenderUtils.drawRect(-40f + xChange, 0f, 40f + xChange, 29f, Color(72, 72, 72, 250).rgb)
                } else {
                    RenderUtils.drawRoundedRect(-40f + xChange, 0f, 40f + xChange, 29f, Color(72, 72, 72, 250).rgb, 5f)
                }

                RenderUtils.drawRoundedRect(-35f + xChange, 7f, -35f + (healthPercent * 70) + xChange, 12f, Color(10, 250, 10, 255).rgb, 2f)
                RenderUtils.drawRoundedRect(-35f + xChange, 17f, -35f + ((entity.totalArmorValue / 20F) * 70) + xChange, 22f, Color(10, 10, 250, 255).rgb, 2f)
            }

            "material2" -> {
                glScalef(-scale * 2, -scale * 2, scale * 2)

                if (materialShadow) {
                    RenderUtils.drawRect(-40f + xChange, 0f, 40f + xChange, 15f, Color(72, 72, 72, 250).rgb)
                    RenderUtils.drawRect(-40f + xChange, 20f, 40f + xChange, 35f, Color(72, 72, 72, 250).rgb)
                } else {
                    RenderUtils.drawRoundedRect(-40f + xChange, 0f, 40f + xChange, 15f, Color(72, 72, 72, 250).rgb, 5f)
                    RenderUtils.drawRoundedRect(-40f + xChange, 20f, 40f + xChange, 35f, Color(72, 72, 72, 250).rgb, 5f)
                }

                RenderUtils.drawRoundedRect(-35f + xChange, 5f, -35f + (healthPercent * 70) + xChange, 10f, Color(10, 250, 10, 255).rgb, 2f)
                RenderUtils.drawRoundedRect(-35f + xChange, 25f, -35f + ((entity.totalArmorValue / 20F) * 70) + xChange, 30f, Color(10, 10, 250, 255).rgb, 2f)
            }

            "arris" -> {
                glScalef(-scale * 2, -scale * 2, scale * 2)
                val hp = healthPercent
                val additionalWidth = font.getStringWidth("${entity.name}  $hp hp").coerceAtLeast(75)
                RenderUtils.drawRoundedRect(xChange, 0f, 45f + additionalWidth + xChange, 40f, Color(0, 0, 0, 110).rgb, 7f)

                font.drawString(entity.name!!, 40 + xChange.toInt(), 5, Color.WHITE.rgb)
                "${HEALTH_FORMAT.format(entity.health)} hp".also {
                    font.drawString(it, 40 + additionalWidth - font.getStringWidth(it) + xChange.toInt(), 5, Color.LIGHT_GRAY.rgb)
                }

                val yPos = 5 + font.FONT_HEIGHT + 3f
                RenderUtils.drawRect(40f + xChange, yPos, 40 + xChange + healthPercent * additionalWidth, yPos + 4, Color.GREEN.rgb)
                RenderUtils.drawRect(40f + xChange, yPos + 9, 40 + xChange + (entity.totalArmorValue / 20F) * additionalWidth, yPos + 13, Color(77, 128, 255).rgb)
            }

            "fdp" -> {
                glScalef(-scale * 2, -scale * 2, scale * 2)

                if (!fdpVertical) {
                    var addedLen = (60 + font.getStringWidth(entity.name!!) * 1.60f).toFloat()
                    if (!fdpText) addedLen = 110f

                    if (fdpRed) {
                        RenderUtils.drawRect(0f + xChange, 0f, addedLen + xChange, 47f, Color(212, 63, 63, 90).rgb)
                        RenderUtils.drawRoundedRect(0f + xChange, 0f, healthPercent * addedLen + xChange, 47f, Color(245, 52, 27, 90).rgb, 3f)
                    } else {
                        RenderUtils.drawRect(0f + xChange, 0f, addedLen + xChange, 47f, Color(0, 0, 0, 120).rgb)
                        RenderUtils.drawRoundedRect(0f + xChange, 0f, healthPercent * addedLen + xChange, 47f, Color(0, 0, 0, 90).rgb, 3f)
                    }

                    if (fdpText) {
                        fontRenderer.drawString(entity.name!!, 45 + xChange.toInt(), 8, Color.WHITE.rgb)
                        fontRenderer.drawString("Health ${entity.health.roundToInt()}", 45 + xChange.toInt(), 11 + font.FONT_HEIGHT, Color.WHITE.rgb)
                    }
                } else {
                    if (fdpRed) {
                        RenderUtils.drawRect(0f + xChange, 0f, 47f + xChange, 120f + xChange, Color(212, 63, 63, 90).rgb)
                        RenderUtils.drawRoundedRect(healthPercent * 120f + xChange, 0f, 47f + xChange, 0f, Color(245, 52, 27, 90).rgb, 3f)
                    } else {
                        RenderUtils.drawRect(0f + xChange, 0f, 47f + xChange, 120f, Color(0, 0, 0, 120).rgb)
                        RenderUtils.drawRoundedRect(0f + xChange, 0f, 47f + xChange, healthPercent * 120f, Color(0, 0, 0, 90).rgb, 3f)
                    }
                }
            }

            "jello" -> {
                var hpBarColor = Color(255, 255, 255, jelloAlphaValue)
                val entityName = entity.displayName.unformattedText
                if (jelloColorValue && entityName.startsWith("§")) {
                    val colorCode = entityName.substring(1, 2)
                    hpBarColor = ColorUtils.colorCode(colorCode, jelloAlphaValue)
                }
                val bgColor = Color(50, 50, 50, jelloAlphaValue)
                val width = fontRenderer.getStringWidth(tag)
                val maxWidth = (width + 4F) - (-width - 4F)

                glScalef(-scale * 2, -scale * 2, scale * 2)
                RenderUtils.drawRect(xChange, -fontRenderer.FONT_HEIGHT * 3F, width + 8F + xChange, -3F, bgColor)

                RenderUtils.drawRect(xChange, -3F, maxWidth * healthPercent + xChange, 1F, hpBarColor)
                RenderUtils.drawRect(maxWidth * healthPercent + xChange, -3F, width + 8F + xChange, 1F, bgColor)

                fontRenderer.drawString(tag, 4 + xChange.toInt(), -fontRenderer.FONT_HEIGHT * 2 - 4, Color.WHITE.rgb)
                glScalef(0.5F, 0.5F, 0.5F)
                fontRenderer.drawString("Health: " + entity.health.toInt(), 4 + xChange.toInt(), -fontRenderer.FONT_HEIGHT * 2, Color.WHITE.rgb)
            }
        }

        glEnable(GL_LIGHTING)
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glColor4f(1F, 1F, 1F, 1F)

        glPopMatrix()
    }
}
