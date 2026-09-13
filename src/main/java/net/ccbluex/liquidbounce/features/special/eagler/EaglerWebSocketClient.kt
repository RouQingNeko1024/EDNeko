package net.ccbluex.liquidbounce.features.special.eagler

import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import okhttp3.*
import okio.ByteString
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class EaglerWebSocketClient(
    private val url: String,
    private val username: String,
    private val skinPreset: Int = 0,
    private val capePreset: Int = 0,
    private val debug: Boolean = false
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val connected = AtomicBoolean(false)
    private val handshakeComplete = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    private val frameQueue = CopyOnWriteArrayList<ByteArray>()
    private val packetListeners = CopyOnWriteArrayList<(ByteArray) -> Unit>()

    var serverProtocol = 0
        private set
    var serverUsername = username
        private set
    var ready = false
        private set

    private val handshakeLatch = CountDownLatch(1)
    @Volatile
    private var handshakeError: String? = null

    private val fragmentBuffer = mutableListOf<ByteArray>()

    fun addPacketListener(listener: (ByteArray) -> Unit) {
        packetListeners.add(listener)
    }

    fun removePacketListener(listener: (ByteArray) -> Unit) {
        packetListeners.remove(listener)
    }

    fun connect(): Boolean {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (debug) LOGGER.info("[EaglerClient] WebSocket connected to $url")
                connected.set(true)

                try {
                    runHandshake()
                } catch (e: Exception) {
                    handshakeError = e.message
                    handshakeLatch.countDown()
                    if (debug) LOGGER.error("[EaglerClient] Handshake failed: ${e.message}", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (debug) LOGGER.info("[EaglerClient] Received text message: ${text.take(100)}")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                handleBinaryMessage(data)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (debug) LOGGER.info("[EaglerClient] WebSocket closing: $code $reason")
                closed.set(true)
                ready = false
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (debug) LOGGER.info("[EaglerClient] WebSocket closed: $code $reason")
                closed.set(true)
                ready = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handshakeError = t.message ?: "Connection failed"
                handshakeLatch.countDown()
                closed.set(true)
                ready = false
                LOGGER.error("[EaglerClient] WebSocket failure: ${t.message}", t)
            }
        })

        return true
    }

    fun awaitHandshake(timeoutMs: Long = 30000): Boolean {
        handshakeLatch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return handshakeComplete.get()
    }

    fun getHandshakeError(): String? = handshakeError

    private fun runHandshake() {
        val ws = webSocket ?: throw IllegalStateException("WebSocket not connected")

        val versionPacket = EaglerProtocol.buildClientVersionPacket(username)
        ws.send(ByteString.of(*versionPacket))

        val versionResponse = waitForFrame(10000)
            ?: throw IllegalStateException("Timeout waiting for server version")

        when (versionResponse[0].toInt() and 0xFF) {
            EaglerProtocol.PKT_VERSION_MISMATCH ->
                throw IllegalStateException("Eagler server version mismatch")
            EaglerProtocol.PKT_SERVER_ERROR ->
                throw IllegalStateException(EaglerProtocol.parseServerError(versionResponse, false))
        }
        if (versionResponse[0].toInt() and 0xFF != EaglerProtocol.PKT_SERVER_VERSION) {
            throw IllegalStateException("Unexpected packet: 0x${(versionResponse[0].toInt() and 0xFF).toString(16)}")
        }

        val serverInfo = EaglerProtocol.parseServerVersion(versionResponse)
        serverProtocol = serverInfo.protocolVersion

        if (serverInfo.gameVersion != EaglerProtocol.GAME_PROTOCOL) {
            throw IllegalStateException("Server does not support MC 1.8 (protocol ${serverInfo.gameVersion})")
        }
        if (serverInfo.protocolVersion !in 3..5) {
            throw IllegalStateException("Unsupported Eagler protocol: ${serverInfo.protocolVersion}")
        }

        if (debug) LOGGER.info("[EaglerClient] Server protocol v${serverInfo.protocolVersion}, brand=${serverInfo.brand}")

        val loginPacket = EaglerProtocol.buildRequestLoginPacket(username, serverInfo.protocolVersion, serverInfo.nicknameSelection)
        ws.send(ByteString.of(*loginPacket))

        val allowResponse = waitForFrame(10000)
            ?: throw IllegalStateException("Timeout waiting for login response")

        when (allowResponse[0].toInt() and 0xFF) {
            EaglerProtocol.PKT_SERVER_DENY_LOGIN ->
                throw IllegalStateException("Login denied: ${EaglerProtocol.parseDenyLogin(allowResponse)}")
            EaglerProtocol.PKT_SERVER_ERROR ->
                throw IllegalStateException(EaglerProtocol.parseServerError(allowResponse, true))
        }
        if (allowResponse[0].toInt() and 0xFF != EaglerProtocol.PKT_SERVER_ALLOW_LOGIN) {
            throw IllegalStateException("Unexpected login response: 0x${(allowResponse[0].toInt() and 0xFF).toString(16)}")
        }

        serverUsername = EaglerProtocol.parseAllowLogin(allowResponse, serverInfo.protocolVersion)

        val skinV3 = buildDefaultSkinV3()
        val capeV3 = buildDefaultCapeV3()
        val profilePacket = EaglerProtocol.buildProfileDataPacket(skinV3, capeV3, serverInfo.protocolVersion)
        ws.send(ByteString.of(*profilePacket))

        val finishLoginPacket = EaglerProtocol.buildFinishLoginPacket()
        ws.send(ByteString.of(*finishLoginPacket))

        val finishResponse = waitForFrame(10000)
            ?: throw IllegalStateException("Timeout waiting for finish login")

        when (finishResponse[0].toInt() and 0xFF) {
            EaglerProtocol.PKT_SERVER_DENY_LOGIN ->
                throw IllegalStateException("Login denied after finish: ${EaglerProtocol.parseDenyLogin(finishResponse)}")
            EaglerProtocol.PKT_SERVER_ERROR ->
                throw IllegalStateException(EaglerProtocol.parseServerError(finishResponse, true))
            EaglerProtocol.PKT_SERVER_REDIRECT_TO ->
                throw IllegalStateException("Server requested redirect (not supported)")
        }
        if (finishResponse[0].toInt() and 0xFF != EaglerProtocol.PKT_SERVER_FINISH_LOGIN) {
            throw IllegalStateException("Unexpected finish response: 0x${(finishResponse[0].toInt() and 0xFF).toString(16)}")
        }

        ready = true
        handshakeComplete.set(true)
        handshakeLatch.countDown()

        if (debug) LOGGER.info("[EaglerClient] Handshake complete, entering PLAY mode")

        flushQueuedFrames()
    }

    private fun waitForFrame(timeoutMs: Long): ByteArray? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (frameQueue.isNotEmpty()) {
                return frameQueue.removeAt(0)
            }
            Thread.sleep(10)
        }
        return null
    }

    private fun handleBinaryMessage(data: ByteArray) {
        if (!ready) {
            frameQueue.add(data)
            return
        }

        val header = data[0].toInt() and 0xFF

        if (header == 1) {
            fragmentBuffer.add(data)
            return
        }

        if (header == 2) {
            fragmentBuffer.add(data)
            val total = fragmentBuffer.sumOf { it.size - 1 }
            val fullData = ByteArray(total)
            var off = 0
            for (frag in fragmentBuffer) {
                System.arraycopy(frag, 1, fullData, off, frag.size - 1)
                off += frag.size - 1
            }
            fragmentBuffer.clear()

            try {
                val inflated = EaglerProtocol.inflatePacket(fullData)
                dispatchPacket(inflated)
            } catch (e: Exception) {
                if (debug) LOGGER.error("[EaglerClient] Failed to inflate packet", e)
            }
            return
        }

        var payload: ByteArray
        if (fragmentBuffer.isNotEmpty()) {
            fragmentBuffer.add(data)
            val total = fragmentBuffer.sumOf { it.size - 1 }
            val fullData = ByteArray(total)
            var off = 0
            for (frag in fragmentBuffer) {
                System.arraycopy(frag, 1, fullData, off, frag.size - 1)
                off += frag.size - 1
            }
            fragmentBuffer.clear()
            payload = fullData
        } else {
            payload = data.copyOfRange(1, data.size)
        }

        dispatchPacket(payload)
    }

    private fun dispatchPacket(payload: ByteArray) {
        for (listener in packetListeners) {
            try {
                listener(payload)
            } catch (e: Exception) {
                LOGGER.error("[EaglerClient] Packet listener error", e)
            }
        }
    }

    private fun flushQueuedFrames() {
        while (frameQueue.isNotEmpty()) {
            val frame = frameQueue.removeAt(0)
            handleBinaryMessage(frame)
        }
    }

    fun sendRaw(data: ByteArray): Boolean {
        val ws = webSocket ?: return false
        return ws.send(ByteString.of(*data))
    }

    fun sendMCPacket(data: ByteArray): Boolean {
        return sendRaw(data)
    }

    fun close() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        ready = false
        closed.set(true)
        client.dispatcher.executorService.shutdown()
    }

    fun isClosed(): Boolean = closed.get()

    fun isConnected(): Boolean = connected.get() && !closed.get()

    private fun buildDefaultSkinV3(): ByteArray {
        val buf = ByteArray(5)
        buf[0] = 0x01
        buf[1] = ((skinPreset ushr 24) and 0xFF).toByte()
        buf[2] = ((skinPreset ushr 16) and 0xFF).toByte()
        buf[3] = ((skinPreset ushr 8) and 0xFF).toByte()
        buf[4] = (skinPreset and 0xFF).toByte()
        return buf
    }

    private fun buildDefaultCapeV3(): ByteArray {
        val buf = ByteArray(5)
        buf[0] = 0x01
        buf[1] = ((capePreset ushr 24) and 0xFF).toByte()
        buf[2] = ((capePreset ushr 16) and 0xFF).toByte()
        buf[3] = ((capePreset ushr 8) and 0xFF).toByte()
        buf[4] = (capePreset and 0xFF).toByte()
        return buf
    }
}