package net.ccbluex.liquidbounce.features.special.eagler

import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.gui.GuiMultiplayer
import net.ccbluex.liquidbounce.ui.client.GuiMainMenu

object EaglercraftManager {

    var enabled = false
    var eaglerUrl = ""
    var debug = false

    private var localProxy: EaglerLocalProxy? = null
    private var connecting = false

    fun connectToEaglerServer(url: String, username: String): Boolean {
        if (localProxy?.isRunning() == true) {
            disconnect()
        }

        eaglerUrl = url
        connecting = true

        try {
            val proxy = EaglerLocalProxy(
                eaglerUrl = url,
                username = username,
                debug = debug
            )

            localProxy = proxy

            val started = proxy.start()
            if (!started) {
                connecting = false
                LOGGER.error("[EaglercraftManager] Failed to start local proxy")
                return false
            }

            LOGGER.info("[EaglercraftManager] Local proxy started on 127.0.0.1:${proxy.localPort}")
            LOGGER.info("[EaglercraftManager] Now connect to '127.0.0.1:${proxy.localPort}' in multiplayer menu")

            return true

        } catch (e: Exception) {
            connecting = false
            LOGGER.error("[EaglercraftManager] Failed to connect: ${e.message}", e)
            return false
        }
    }

    fun autoConnectToProxy() {
        val proxy = localProxy ?: return
        if (!proxy.isRunning()) return

        try {
            val mc = Minecraft.getMinecraft()
            val port = proxy.localPort
            val serverData = ServerData("Eaglercraft", "127.0.0.1:$port", false)
            mc.addScheduledTask {
                mc.displayGuiScreen(GuiConnecting(
                    GuiMultiplayer(GuiMainMenu()),
                    mc,
                    serverData
                ))
            }
            if (debug) LOGGER.info("[EaglercraftManager] Auto-connecting to 127.0.0.1:$port")
        } catch (e: Exception) {
            LOGGER.error("[EaglercraftManager] Auto-connect failed: ${e.message}", e)
        }
    }

    fun disconnect() {
        localProxy?.stop()
        localProxy = null
        connecting = false
        LOGGER.info("[EaglercraftManager] Disconnected from Eaglercraft server")
    }

    fun isConnected(): Boolean = localProxy?.isRunning() == true && localProxy?.isConnectedToEagler() == true

    fun isProxyRunning(): Boolean = localProxy?.isRunning() == true

    fun getProxyPort(): Int = localProxy?.localPort ?: 0

    fun buildEaglerUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.startsWith("wss://") || trimmed.startsWith("ws://")) return trimmed

        if (trimmed.startsWith("wws://")) return "wss://" + trimmed.removePrefix("wws://")

        if (trimmed.startsWith("ww://")) return "ws://" + trimmed.removePrefix("ww://")

        if (!trimmed.contains(":")) return "wss://$trimmed"

        return "ws://$trimmed"
    }

    fun getStatusText(): String {
        if (!enabled) return "Disabled"

        val proxy = localProxy
        if (proxy == null) return if (connecting) "Starting proxy..." else "Not connected"

        if (!proxy.isRunning()) return "Proxy stopped"

        if (proxy.isConnectedToEagler()) return "Connected (port ${proxy.localPort})"

        return "Proxy ready (port ${proxy.localPort})"
    }

    fun getConnectHint(): String {
        val proxy = localProxy ?: return ""
        if (proxy.isRunning()) {
            return "Connect to: 127.0.0.1:${proxy.localPort}"
        }
        return ""
    }
}