/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS
import net.ccbluex.liquidbounce.utils.render.ColorSettingsFloat
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.ResourceLocation
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.AnimationUtils
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object HUD : Module("HUD", Category.RENDER, gameDetecting = false, defaultState = true, defaultHidden = true) {
    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    private fun lerp(start: Float, end: Float, percent: Float): Float {
        return start + (end - start) * percent
    }
    
    val customHotbar by boolean("CustomHotbar", true)
    private var hotBarX = 0F

    val smoothHotbarSlot by boolean("SmoothHotbarSlot", true) { customHotbar }

    val roundedHotbarRadius by float("RoundedHotbar-Radius", 3F, 0F..5F) { customHotbar }

    val inventoryParticle by boolean("InventoryParticle", false)
    private val blur by boolean("Blur", false)
    private val fontChat by choices("FontChat", arrayOf("Off", "Minecraft", "Regular35", "Regular40", "Semibold35", "Semibold40"), "Minecraft")
    val chatCombine by boolean("ChatCombine", true)
    val chatAnimation by boolean("ChatAnimation", true)
    val chatAnimationSpeed by float("Chat-AnimationSpeed", 0.1f, 0.01f..1.0f) { chatAnimation }
    val chatRect by boolean("ChatRect", true) { chatAnimation }

    val customHealthBar by boolean("自定义血条", true)
    val healthBarStyle by choices("血条样式", arrayOf("极简", "圆角", "动态", "原版"), "圆角") { customHealthBar }
    val healthBarWidth by int("血条宽度", 90, 50..200) { customHealthBar }
    val healthBarHeight by int("血条高度", 10, 5..20) { customHealthBar }
    val healthBarOffsetX by int("血条X偏移", 0, -500..500) { customHealthBar }
    val healthBarOffsetY by int("血条Y偏移", 0, -500..500) { customHealthBar }
    val healthColor1 by color("血条颜色1", Color(255, 50, 50)) { customHealthBar }
    val healthColor2 by color("血条颜色2", Color(255, 100, 100)) { customHealthBar && healthBarStyle == "渐变" }
    val healthBgColor by color("血条背景色", Color(50, 50, 50, 150)) { customHealthBar }
    val healthText by boolean("显示血量文字", true) { customHealthBar && healthBarStyle != "数字" && healthBarStyle != "百分比" }
    val healthTextShadow by boolean("血量文字阴影", false) { customHealthBar && healthText }
    val healthTextOffsetX by int("血量文字X偏移", 0, -100..100) { customHealthBar && healthText }
    val healthTextOffsetY by int("血量文字Y偏移", 1, -100..100) { customHealthBar && healthText }
    val healthRoundedRadius by int("血条圆角", 3, 0..10) { customHealthBar }
    val healthBorderWidth by int("血条边框粗细", 1, 0..3) { customHealthBar }
    val healthBorderColor by color("血条边框颜色", Color(0, 0, 0, 100)) { customHealthBar && healthBorderWidth > 0 }
    val healthAnimationSpeed by int("血条动画速度", 1, 1..5) { customHealthBar }

    val customFoodBar by boolean("自定义饥饿值", true)
    val foodBarStyle by choices("饥饿值样式", arrayOf("极简", "圆角", "动态", "原版"), "圆角") { customFoodBar }
    val foodBarWidth by int("饥饿值宽度", 90, 50..200) { customFoodBar }
    val foodBarHeight by int("饥饿值高度", 10, 5..20) { customFoodBar }
    val foodBarOffsetX by int("饥饿值X偏移", 0, -500..500) { customFoodBar }
    val foodBarOffsetY by int("饥饿值Y偏移", 0, -500..500) { customFoodBar }
    val foodColor1 by color("饥饿值颜色1", Color(139, 90, 43)) { customFoodBar }
    val foodColor2 by color("饥饿值颜色2", Color(194, 124, 57)) { customFoodBar && foodBarStyle == "渐变" }
    val foodBgColor by color("饥饿值背景色", Color(50, 50, 50, 150)) { customFoodBar }
    val foodText by boolean("显示饥饿值文字", true) { customFoodBar && foodBarStyle != "数字" && foodBarStyle != "百分比" }
    val foodTextShadow by boolean("饥饿值文字阴影", false) { customFoodBar && foodText }
    val foodTextOffsetX by int("饥饿值文字X偏移", 0, -100..100) { customFoodBar && foodText }
    val foodTextOffsetY by int("饥饿值文字Y偏移", 1, -100..100) { customFoodBar && foodText }
    val foodRoundedRadius by int("饥饿值圆角", 3, 0..10) { customFoodBar }
    val foodBorderWidth by int("饥饿值边框粗细", 1, 0..3) { customFoodBar }
    val foodBorderColor by color("饥饿值边框颜色", Color(0, 0, 0, 100)) { customFoodBar && foodBorderWidth > 0 }
    val foodAnimationSpeed by int("饥饿值动画速度", 1, 1..5) { customFoodBar }

    private var displayHealth = 0f
    private var lastHealth = 0f
    private var displayFood = 0f
    private var lastFood = 0
    private var easingHealth = 0f
    private var easingFood = 0f

    private val ICONS = ResourceLocation("textures/gui/icons.png")

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderGameOverlay(event: RenderGameOverlayEvent.Pre) {
        if (!handleEvents()) return

        val player = mc.thePlayer ?: return
        val resolution = ScaledResolution(mc)
        val width = resolution.scaledWidth
        val height = resolution.scaledHeight

        when (event.type) {
            RenderGameOverlayEvent.ElementType.HEALTH -> {
                if (customHealthBar) {
                    event.isCanceled = true
                    renderCustomHealthBar(player, width, height)
                }
            }
            RenderGameOverlayEvent.ElementType.FOOD -> {
                if (customFoodBar) {
                    event.isCanceled = true
                    renderCustomFoodBar(player, width, height)
                }
            }
            RenderGameOverlayEvent.ElementType.AIR -> {
                if (customHealthBar) {
                    event.isCanceled = true
                }
            }
            else -> {}
        }
    }

    private fun renderCustomHealthBar(player: net.minecraft.entity.player.EntityPlayer, width: Int, height: Int) {
        val health = player.health
        val maxHealth = player.maxHealth
        val absorption = player.absorptionAmount

        if (easingHealth < 0 || easingHealth > maxHealth || abs(easingHealth - health) > maxHealth * 0.5f) {
            easingHealth = health
        }
        easingHealth = lerp(easingHealth, health, healthAnimationSpeed * 0.1f)
        displayHealth = easingHealth
        lastHealth = health

        val healthPercent = max(0f, min(1f, displayHealth / maxHealth))
        val barX = width / 2f - 91f + healthBarOffsetX.toFloat()
        val barY = height - 39f + healthBarOffsetY.toFloat()
        val barWidth = healthBarWidth.toFloat()
        val barHeight = healthBarHeight.toFloat()
        val style = healthBarStyle
        val color1 = healthColor1
        val color2 = healthColor2
        val bgColor = healthBgColor
        val radius = healthRoundedRadius.toFloat()
        val borderWidth = healthBorderWidth.toFloat()
        val borderColor = healthBorderColor

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glShadeModel(GL11.GL_SMOOTH)

        val healthWidth = healthPercent * barWidth

        when (style) {
            "极简" -> {
                RenderUtils.drawRect(barX, barY, barX + barWidth, barY + 2, Color(50, 50, 50, 150).rgb)
                if (healthWidth > 0) {
                    RenderUtils.drawRect(barX, barY, barX + healthWidth, barY + 2, color1.rgb)
                }
            }
            "圆角" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (healthWidth > 0) {
                    RenderUtils.drawRoundedRect(barX, barY, barX + healthWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "动态" -> {
                val time = System.currentTimeMillis() % 2000 / 2000f
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (healthWidth > 0) {
                    val shimmerWidth = 30f
                    val shimmerX = (time * (barWidth + shimmerWidth)) - shimmerWidth

                    RenderUtils.drawRoundedRect(barX, barY, barX + healthWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)

                    GL11.glColor4f(1f, 1f, 1f, 0.3f)
                    RenderUtils.drawRoundedRect(
                        barX + max(0f, min(shimmerX, healthWidth)),
                        barY,
                        min(barX + healthWidth, barX + shimmerX + shimmerWidth),
                        barY + barHeight,
                        Color.WHITE.rgb,
                        radius,
                        RenderUtils.RoundedCorners.ALL
                    )
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "原版" -> {
                GL11.glEnable(GL11.GL_TEXTURE_2D)
                mc.textureManager.bindTexture(ICONS)
                GL11.glColor4f(1f, 1f, 1f, 1f)
                val maxHearts = ceil(maxHealth / 2f).toInt()
                val heartSize = 9
                val spacing = 2

                for (i in 0 until maxHearts) {
                    val heartX = barX + i * (heartSize + spacing)
                    val heartY = barY
                    val healthForHeart = health - i * 2

                    Gui.drawModalRectWithCustomSizedTexture(heartX.toInt(), heartY.toInt(), 16f, 0f, 9, 9, 256f, 256f)
                    if (healthForHeart >= 2) {
                        Gui.drawModalRectWithCustomSizedTexture(heartX.toInt(), heartY.toInt(), 52f, 0f, 9, 9, 256f, 256f)
                    } else if (healthForHeart >= 1) {
                        Gui.drawModalRectWithCustomSizedTexture(heartX.toInt(), heartY.toInt(), 61f, 0f, 9, 9, 256f, 256f)
                    }
                }
                GL11.glDisable(GL11.GL_TEXTURE_2D)
            }
        }

        if (healthText && style != "原版") {
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            val text = "${health.toInt()}/${maxHealth.toInt()}"
            val textX = barX + barWidth / 2 - Fonts.fontRegular35.getStringWidth(text) / 2 + healthTextOffsetX
            val textY = barY + barHeight / 2 - Fonts.fontRegular35.FONT_HEIGHT / 2 + healthTextOffsetY
            Fonts.fontRegular35.drawString(text, textX, textY, Color.WHITE.rgb, healthTextShadow)
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glShadeModel(GL11.GL_FLAT)
        GL11.glPopMatrix()
    }

    private fun renderCustomFoodBar(player: net.minecraft.entity.player.EntityPlayer, width: Int, height: Int) {
        val foodLevel = player.foodStats.foodLevel
        val maxFood = 20f

        if (easingFood < 0 || easingFood > maxFood || abs(easingFood - foodLevel) > maxFood * 0.5f) {
            easingFood = foodLevel.toFloat()
        }
        easingFood = lerp(easingFood, foodLevel.toFloat(), foodAnimationSpeed * 0.1f)
        displayFood = easingFood
        lastFood = foodLevel

        val foodPercent = max(0f, min(1f, displayFood / maxFood))
        val barX = width / 2f + 91f - foodBarWidth + foodBarOffsetX.toFloat()
        val barY = height - 39f + foodBarOffsetY.toFloat()
        val barWidth = foodBarWidth.toFloat()
        val barHeight = foodBarHeight.toFloat()
        val style = foodBarStyle
        val color1 = foodColor1
        val color2 = foodColor2
        val bgColor = foodBgColor
        val radius = foodRoundedRadius.toFloat()
        val borderWidth = foodBorderWidth.toFloat()
        val borderColor = foodBorderColor

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glShadeModel(GL11.GL_SMOOTH)

        val foodWidth = foodPercent * barWidth

        when (style) {
            "极简" -> {
                RenderUtils.drawRect(barX, barY, barX + barWidth, barY + 2, Color(50, 50, 50, 150).rgb)
                if (foodWidth > 0) {
                    RenderUtils.drawRect(barX, barY, barX + foodWidth, barY + 2, color1.rgb)
                }
            }
            "圆角" -> {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (foodWidth > 0) {
                    RenderUtils.drawRoundedRect(barX, barY, barX + foodWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "动态" -> {
                val time = System.currentTimeMillis() % 2000 / 2000f
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, bgColor.rgb, radius, RenderUtils.RoundedCorners.ALL)
                if (foodWidth > 0) {
                    val shimmerWidth = 30f
                    val shimmerX = (time * (barWidth + shimmerWidth)) - shimmerWidth

                    RenderUtils.drawRoundedRect(barX, barY, barX + foodWidth, barY + barHeight, color1.rgb, radius, RenderUtils.RoundedCorners.ALL)

                    GL11.glColor4f(1f, 1f, 1f, 0.3f)
                    RenderUtils.drawRoundedRect(
                        barX + max(0f, min(shimmerX, foodWidth)),
                        barY,
                        min(barX + foodWidth, barX + shimmerX + shimmerWidth),
                        barY + barHeight,
                        Color.WHITE.rgb,
                        radius,
                        RenderUtils.RoundedCorners.ALL
                    )
                }
                if (borderWidth > 0) {
                    RenderUtils.drawRoundedBorder(barX, barY, barX + barWidth, barY + barHeight, borderWidth, borderColor.rgb, radius)
                }
            }
            "原版" -> {
                GL11.glEnable(GL11.GL_TEXTURE_2D)
                mc.textureManager.bindTexture(ICONS)
                GL11.glColor4f(1f, 1f, 1f, 1f)
                val iconCount = 10
                val iconSize = 9
                val spacing = 2

                for (i in 0 until iconCount) {
                    val iconX = barX + i * (iconSize + spacing)
                    val foodForIcon = displayFood - i * 2
                    val isFull = foodForIcon >= 2
                    val isHalf = foodForIcon >= 1

                    Gui.drawModalRectWithCustomSizedTexture(iconX.toInt(), barY.toInt(), 16f, 27f, 9, 9, 256f, 256f)

                    if (isFull) {
                        Gui.drawModalRectWithCustomSizedTexture(iconX.toInt(), barY.toInt(), 52f, 27f, 9, 9, 256f, 256f)
                    } else if (isHalf) {
                        Gui.drawModalRectWithCustomSizedTexture(iconX.toInt(), barY.toInt(), 61f, 27f, 9, 9, 256f, 256f)
                    }
                }
                GL11.glDisable(GL11.GL_TEXTURE_2D)
            }
        }

        if (foodText && style != "原版") {
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            val text = "${displayFood.toInt()}/${maxFood.toInt()}"
            val textX = barX + barWidth / 2 - Fonts.fontRegular35.getStringWidth(text) / 2 + foodTextOffsetX
            val textY = barY + barHeight / 2 - Fonts.fontRegular35.FONT_HEIGHT / 2 + foodTextOffsetY
            Fonts.fontRegular35.drawString(text, textX, textY, Color.WHITE.rgb, foodTextShadow)
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glShadeModel(GL11.GL_FLAT)
        GL11.glPopMatrix()
    }

    val onRender2D = handler<Render2DEvent> {
        if (mc.currentScreen is GuiHudDesigner)
            return@handler

        hud.render(false)
    }

    val onUpdate = handler<UpdateEvent> {
        hud.update()
    }

    val onKey = handler<KeyEvent> { event ->
        hud.handleKey('a', event.key)
    }

    val onScreen = handler<ScreenEvent>(always = true) { event ->
        if (mc.theWorld == null || mc.thePlayer == null) return@handler
        if (state && blur && !mc.entityRenderer.isShaderActive && event.guiScreen != null &&
            !(event.guiScreen is GuiChat || event.guiScreen is GuiHudDesigner)
        ) mc.entityRenderer.loadShader(
            ResourceLocation(CLIENT_NAME.lowercase() + "/blur.json")
        ) else if (mc.entityRenderer.shaderGroup != null &&
            "edneko/blur.json" in mc.entityRenderer.shaderGroup.shaderGroupName
        ) mc.entityRenderer.stopUseShader()
    }

    fun shouldModifyChatFont() = handleEvents() && fontChat != "Off"

    fun getChatFont() = when (fontChat) {
        "Minecraft" -> Fonts.minecraftFont
        "Regular35" -> Fonts.fontRegular35
        "Regular40" -> Fonts.fontRegular40
        "Semibold35" -> Fonts.fontSemibold35
        "Semibold40" -> Fonts.fontSemibold40
        else -> Fonts.fontSemibold40
    }

    fun getAnimPos(pos: Float): Float {
        hotBarX = AnimationUtils.animate(pos, hotBarX, 0.02F * RenderUtils.deltaTime.toFloat())

        return hotBarX
    }
}
