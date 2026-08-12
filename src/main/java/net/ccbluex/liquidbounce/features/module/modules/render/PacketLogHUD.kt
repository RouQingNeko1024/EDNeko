/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */ 
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object PacketLogHUD : Module("PacketLogHUD", Category.RENDER) {
    private val posX by float("X", 0.01f, 0f..1f)
    private val posY by float("Y", 0.15f, 0f..1f)
    private val backgroundAlpha by int("Alpha", 150, 0..255)
    private val scale by float("Scale", 1f, 0.5f..2f)
    private val mode by choices("Mode", arrayOf("Tick", "Second", "Total"), "Second")

    private var sendCount = 0
    private var recvCount = 0
    private var totalSend = 0L
    private var totalRecv = 0L
    private var showSend = 0
    private var showRecv = 0
    private var tickCounter = 0
    private var lastSendCount = 0
    private var lastRecvCount = 0

    override fun onEnable() {
        sendCount = 0
        recvCount = 0
        totalSend = 0
        totalRecv = 0
        showSend = 0
        showRecv = 0
        tickCounter = 0
        lastSendCount = 0
        lastRecvCount = 0
    }

    val onPacket = handler<PacketEvent> { event ->
        when (event.eventType) {
            EventState.SEND -> {
                sendCount++
                totalSend++
            }
            EventState.RECEIVE -> {
                recvCount++
                totalRecv++
            }
            else -> {}
        }
    }

    val onTick = handler<PlayerTickEvent> {
        tickCounter++

        if (tickCounter >= 20) {
            tickCounter = 0
            if (mode == "Second") {
                showSend = sendCount
                showRecv = recvCount
                sendCount = 0
                recvCount = 0
            }
        }

        if (mode == "Tick") {
            if (sendCount > 0) {
                lastSendCount = sendCount
            }
            if (recvCount > 0) {
                lastRecvCount = recvCount
            }
            sendCount = 0
            recvCount = 0
        }
    }

    val onRender2D = handler<Render2DEvent> {
        val sr = ScaledResolution(mc)
        val font = Fonts.fontSemibold40

        val x = sr.scaledWidth * posX
        val y = sr.scaledHeight * posY

        glPushMatrix()
        glScalef(scale, scale, 1f)

        val clientPacket = when (mode) {
            "Tick" -> if (sendCount > 0) sendCount else lastSendCount
            "Second" -> showSend
            else -> totalSend.toInt()
        }

        val serverPacket = when (mode) {
            "Tick" -> if (recvCount > 0) recvCount else lastRecvCount
            "Second" -> showRecv
            else -> totalRecv.toInt()
        }

        val unit = if (mode == "Tick") "tick" else "Second"

        val txt1 = "ClientPacket: ${clientPacket}P/$unit"
        val txt2 = "ServerPacket: ${serverPacket}P/$unit"

        val w = maxOf(
            font.getStringWidth(txt1),
            font.getStringWidth(txt2)
        ) + 10f

        val h = font.FONT_HEIGHT * 2 + 8f

        val drawX = x / scale
        val drawY = y / scale

        RenderUtils.drawRect(drawX, drawY, drawX + w, drawY + h, Color(0, 0, 0, backgroundAlpha).rgb)

        font.drawString(txt1, drawX + 5, drawY + 3, Color.WHITE.rgb)
        font.drawString(txt2, drawX + 5, drawY + 3 + font.FONT_HEIGHT, Color.WHITE.rgb)

        glPopMatrix()
    }
}
