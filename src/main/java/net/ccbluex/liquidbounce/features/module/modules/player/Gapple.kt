package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedGradientRectCorner
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.item.ItemAppleGold
import net.minecraft.network.play.client.*
import java.awt.Color

object Gapple : Module("Gapple", Category.PLAYER) {
    private val c by int("C03PacketPlayer", 32, 32..40)
    private val progressBar2 by boolean("ProgressBar", true)
    private val chatMessage by boolean("ChatMessage", true)

    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var cancelMove = false
    private var r = false
    var ticks = 0
        private set
    private var pauseTicks = 0
    private var yaw = 0f
    private var pitch = 0f
    var isEating = false
        private set

    private var slot = -1

    fun getEatingProgress(): Float {
        return if (isEating && c > 0) {
            (ticks.toFloat() / c.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    override fun onEnable() {
        isEating = false
        ticks = 0
        pauseTicks = 0
        stopStuck()
        
        slot = getGApple()
        if (slot < 0) {
            if (chatMessage) chat("§c没有苹果了！")
            state = false
            return
        }
        
        isEating = true
        if (chatMessage) chat("§a开始吃金苹果")
    }

    override fun onDisable() {
        ticks = 0
        pauseTicks = 0
        isEating = false
        stopStuck()
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is C03PacketPlayer && cancelMove && ticks < c) {
            if (packet is C03PacketPlayer.C05PacketPlayerLook) {
                yaw = packet.yaw
                pitch = packet.pitch
            }
            ticks++
            event.cancelEvent()
        }
    }

    val onMove = handler<MoveEvent> { event ->
        if (cancelMove) {
            event.cancelEvent()
        }
    }

    val onTick = handler<GameTickEvent> {
        slot = getGApple()
        
        if (slot < 0) {
            if (chatMessage) chat("§c没有苹果了！")
            state = false
            return@handler
        }

        if (pauseTicks == 0) {
            stuck()
        } else {
            if (pauseTicks > 0) {
                stopStuck()
                pauseTicks--
            }
        }

        if (ticks >= c) {
            sendPacket(C09PacketHeldItemChange(slot))
            sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(slot)))
            release()
            sendPacket(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem)))
            pauseTicks++
            ticks = 0

            if (mc.thePlayer.ticksExisted % 20 == 0 && chatMessage) {
                chat("§6正在吃金苹果...")
            }
        }
    }

    val onRender2D = handler<Render2DEvent> {
        if (isEating && progressBar2) {
            val scaledScreen = ScaledResolution(mc)
            val width = scaledScreen.scaledWidth.toFloat()
            val height = scaledScreen.scaledHeight.toFloat()
            drawProgressBar(width, height)
        }
    }

    private fun stuck() {
        if (!r) {
            x = mc.thePlayer.motionX
            y = mc.thePlayer.motionY
            z = mc.thePlayer.motionZ
            r = true
        }
        cancelMove = true
    }

    private fun stopStuck() {
        cancelMove = false
        if (r) {
            mc.thePlayer.motionX = x
            mc.thePlayer.motionY = y
            mc.thePlayer.motionZ = z
            r = false
        }
    }

    private fun release() {
        sendPacket(C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround))
        for (i in 1 until ticks) {
            sendPacket(C03PacketPlayer(mc.thePlayer.onGround))
        }
    }

    private fun getGApple(): Int {
        for (i in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            if (stack != null && stack.item is ItemAppleGold) {
                return i
            }
        }
        return -1
    }

    private fun drawProgressBar(width: Float, height: Float) {
        val progressLength = 140F
        val startY = height / 4 * 3
        val startX = width / 2 - progressLength / 2

        val progressRatio = (ticks.toFloat() / c.toFloat()).coerceIn(0f, 1f)
        val currentProgress = progressLength * progressRatio
        val progressPercent = (progressRatio * 100).toInt()

        GlowUtils.drawGlow(
            startX - 2, startY - 2,
            progressLength + 4, 11F,
            4,
            Color(0, 0, 0, 120)
        )

        drawRoundedRect(startX, startY, startX + progressLength, startY + 7F, Color(0, 0, 0, 128).rgb, 2F)

        if (currentProgress != 0f) {
            drawRoundedGradientRectCorner(
                startX, startY,
                startX + currentProgress, startY + 7F,
                3f,
                Color(76, 157, 240, 255).rgb,
                Color(53, 200, 167, 255).rgb
            )
        }

        val percentText = "$progressPercent%"
        Fonts.fontRegular35.drawString(
            percentText,
            startX + progressLength + 5,
            startY,
            Color(255, 255, 255, 255).rgb,
            true
        )
    }
}
