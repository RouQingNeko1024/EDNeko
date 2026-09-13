package net.ccbluex.liquidbounce.features.module.modules.client

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.async.loopSequence
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S40PacketDisconnect
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.CopyOnWriteArraySet

object IRC : Module("IRC", Category.CLIENT, gameDetecting = false) {

    private val server by text("Server", "26.0.0.1")
    private val port by int("Port", 6667, 1..65535)
    private val channel by text("Channel", "#nekoirc")
    private val channelPassword by text("ChannelPassword", "123456")
    private val nickSuffix by text("NickSuffix", "_EDNeko")
    private val heartbeatInterval by int("Heartbeat", 30, 5..120)
    val showPrefix by boolean("ShowPrefix", true)

    private var ircSocket: Socket? = null
    private var ircWriter: PrintWriter? = null
    private var ircReader: BufferedReader? = null
    private var ircThread: Thread? = null
    @Volatile
    private var connected = false
    @Volatile
    private var running = false

    val ednekoPlayers = CopyOnWriteArraySet<String>()

    private val MAGIC_PREFIX = "\u00A7zEDNeko\u00A7z"

    override fun onEnable() {
        connectIRC()
    }

    override fun onDisable() {
        disconnectIRC()
        ednekoPlayers.clear()
    }

    private fun connectIRC() {
        if (running) return
        running = true
        ircThread = Thread({
            try {
                val socket = Socket(server, port)
                socket.soTimeout = 10000
                ircSocket = socket
                ircWriter = PrintWriter(socket.getOutputStream(), true)
                ircReader = BufferedReader(InputStreamReader(socket.getInputStream()))

                val playerName = mc.thePlayer?.name ?: "EDNekoGuest"
                val nick = playerName + nickSuffix

                sendRaw("NICK $nick")
                sendRaw("USER $nick 0 * :EDNeko IRC Client")

                var joined = false
                var line: String?
                while (running) {
                    line = ircReader?.readLine() ?: break
                    handleIRCLine(line)
                    if (!joined && line.contains("001")) {
                        if (channelPassword.isNotEmpty()) {
                            sendRaw("JOIN $channel $channelPassword")
                        } else {
                            sendRaw("JOIN $channel")
                        }
                        joined = true
                        connected = true
                        chat("\u00A7a[IRC] \u00A77Connected to $channel")
                    }
                }
            } catch (e: Exception) {
                if (running) {
                    chat("\u00A7c[IRC] \u00A77Connection failed: ${e.message}")
                }
            } finally {
                connected = false
                running = false
                try { ircSocket?.close() } catch (_: Exception) {}
                ircSocket = null
                ircWriter = null
                ircReader = null
            }
        }, "EDNeko-IRC")
        ircThread?.isDaemon = true
        ircThread?.start()
    }

    private fun disconnectIRC() {
        running = false
        connected = false
        try {
            sendRaw("QUIT :EDNeko IRC Client disconnecting")
        } catch (_: Exception) {}
        try { ircSocket?.close() } catch (_: Exception) {}
        ircSocket = null
        ircWriter = null
        ircReader = null
    }

    private fun sendRaw(line: String) {
        ircWriter?.println(line)
    }

    private fun handleIRCLine(line: String) {
        if (line.startsWith("PING")) {
            val parts = line.split(" ")
            if (parts.size >= 2) {
                sendRaw("PONG ${parts[1]}")
            }
            return
        }

        if (line.contains(" PRIVMSG ")) {
            val nick = extractNick(line) ?: return
            val msgIdx = line.indexOf(" :", 1)
            if (msgIdx < 0) return
            val msg = line.substring(msgIdx + 2)

            if (msg.startsWith(MAGIC_PREFIX)) {
                val payload = msg.substring(MAGIC_PREFIX.length)
                if (payload.startsWith("HELLO:")) {
                    val mcName = payload.substring(6)
                    if (mcName.isNotEmpty() && mcName != mc.thePlayer?.name) {
                        if (ednekoPlayers.add(mcName)) {
                            chat("\u00A7b[IRC] \u00A77EDNeko player detected: \u00A7b$mcName")
                        }
                    }
                } else if (payload.startsWith("BYE:")) {
                    val mcName = payload.substring(4)
                    if (ednekoPlayers.remove(mcName)) {
                        chat("\u00A7b[IRC] \u00A77EDNeko player left: \u00A7b$mcName")
                    }
                }
            }
        }

        if (line.contains(" 352 ") || line.contains(" JOIN ")) {
            val nick = extractNick(line) ?: return
            val mcName = nick.removeSuffix(nickSuffix)
            if (nick != mcName && mcName.isNotEmpty() && mcName != mc.thePlayer?.name) {
                if (ednekoPlayers.add(mcName)) {
                    chat("\u00A7b[IRC] \u00A77EDNeko player detected: \u00A7b$mcName")
                }
            }
        }

        if (line.contains(" QUIT ") || line.contains(" PART ")) {
            val nick = extractNick(line) ?: return
            val mcName = nick.removeSuffix(nickSuffix)
            if (nick != mcName && mcName.isNotEmpty()) {
                ednekoPlayers.remove(mcName)
            }
        }
    }

    private fun extractNick(line: String): String? {
        if (!line.startsWith(":")) return null
        val spaceIdx = line.indexOf(' ', 1)
        if (spaceIdx < 0) return null
        val prefix = line.substring(1, spaceIdx)
        val bangIdx = prefix.indexOf('!')
        return if (bangIdx > 0) prefix.substring(0, bangIdx) else prefix
    }

    val onHeartbeat = loopSequence {
        if (!connected) {
            delay(5000)
            return@loopSequence
        }
        val playerName = mc.thePlayer?.name ?: return@loopSequence
        sendRaw("PRIVMSG $channel :$MAGIC_PREFIX`HELLO:$playerName")
        delay(heartbeatInterval * 1000L)
    }

    val onPacket = handler<PacketEvent> { event ->
        if (!showPrefix) return@handler

        val packet = event.packet

        if (packet is S38PacketPlayerListItem) {
            if (packet.action == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                for (entry in packet.entries) {
                    val name = entry.profile?.name ?: continue
                    if (name == mc.thePlayer?.name) continue
                    if (name.removeSuffix(nickSuffix) in ednekoPlayers || name in ednekoPlayers) {
                        ednekoPlayers.add(name)
                    }
                }
            } else if (packet.action == S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
                for (entry in packet.entries) {
                    val name = entry.profile?.name ?: continue
                    ednekoPlayers.remove(name)
                }
            }
        }
    }

    val onWorld = handler<WorldEvent> {
        if (it.worldClient == null) {
            ednekoPlayers.clear()
        }
    }

    fun isEDNekoPlayer(name: String): Boolean {
        return showPrefix && name in ednekoPlayers
    }

    fun getPrefixedName(name: String): String {
        return if (isEDNekoPlayer(name)) "\u00A7b[\u00A7lEDNeko\u00A7b] \u00A7r$name" else name
    }

    override val tag: String
        get() = if (connected) "\u00A7aConnected" else "\u00A7cDisconnected"
}