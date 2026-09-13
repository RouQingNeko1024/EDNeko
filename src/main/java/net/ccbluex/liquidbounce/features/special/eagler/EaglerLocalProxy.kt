package net.ccbluex.liquidbounce.features.special.eagler

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class EaglerLocalProxy(
    private val eaglerUrl: String,
    private val username: String,
    private val debug: Boolean = false
) {
    private var serverChannel: Channel? = null
    private var bossGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null

    @Volatile
    var localPort: Int = 0
        private set

    private val running = AtomicBoolean(false)
    private val eaglerClientRef = AtomicReference<EaglerWebSocketClient?>(null)
    private val vanillaChannelRef = AtomicReference<Channel?>(null)

    fun start(): Boolean {
        if (running.get()) return true

        try {
            bossGroup = NioEventLoopGroup(1)
            workerGroup = NioEventLoopGroup(1)

            val proxyRef = this

            val bootstrap = ServerBootstrap()
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("frame_decoder", LengthFieldBasedFrameDecoder(2097152, 0, 3, 0, 3))
                        ch.pipeline().addLast("frame_prepender", LengthFieldPrepender(3))
                        ch.pipeline().addLast("handler", VanillaProxyHandler(proxyRef))
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 1)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)

            val future = bootstrap.bind(0).sync()
            serverChannel = future.channel()

            val addr = serverChannel!!.localAddress() as InetSocketAddress
            localPort = addr.port
            running.set(true)

            if (debug) LOGGER.info("[EaglerProxy] Local proxy listening on 127.0.0.1:$localPort")

            return true
        } catch (e: Exception) {
            LOGGER.error("[EaglerProxy] Failed to start local proxy: ${e.message}", e)
            stop()
            return false
        }
    }

    fun stop() {
        running.set(false)

        eaglerClientRef.getAndSet(null)?.close()
        vanillaChannelRef.getAndSet(null)?.close()

        serverChannel?.close()
        serverChannel = null

        workerGroup?.shutdownGracefully()
        bossGroup?.shutdownGracefully()
        workerGroup = null
        bossGroup = null

        if (debug) LOGGER.info("[EaglerProxy] Local proxy stopped")
    }

    fun isRunning(): Boolean = running.get()

    fun isConnectedToEagler(): Boolean {
        val client = eaglerClientRef.get()
        return client != null && client.ready && client.isConnected()
    }

    private class VanillaProxyHandler(private val proxy: EaglerLocalProxy) : ChannelInboundHandlerAdapter() {
        private var eaglerClient: EaglerWebSocketClient? = null
        private var handshakeDone = false

        override fun channelActive(ctx: ChannelHandlerContext) {
            if (proxy.debug) LOGGER.info("[EaglerProxy] Vanilla client connected from ${ctx.channel().remoteAddress()}")

            proxy.vanillaChannelRef.set(ctx.channel())

            Thread {
                try {
                    val client = EaglerWebSocketClient(
                        url = proxy.eaglerUrl,
                        username = proxy.username,
                        skinPreset = 0,
                        capePreset = 0,
                        debug = proxy.debug
                    )

                    client.addPacketListener { payload ->
                        if (ctx.channel().isActive) {
                            try {
                                val mcData = EaglerProtocol.convertEaglerPacketToMC(payload)
                                if (mcData != null) {
                                    val buf = Unpooled.wrappedBuffer(mcData)
                                    ctx.channel().writeAndFlush(buf)
                                }
                            } catch (e: Exception) {
                                if (proxy.debug) LOGGER.error("[EaglerProxy] Error sending to vanilla client", e)
                            }
                        }
                    }

                    eaglerClient = client
                    proxy.eaglerClientRef.set(client)
                    client.connect()

                    val success = client.awaitHandshake(30000)
                    if (!success) {
                        val error = client.getHandshakeError()
                        LOGGER.error("[EaglerProxy] Eagler handshake failed: $error")
                        ctx.channel().close()
                        return@Thread
                    }

                    handshakeDone = true
                    if (proxy.debug) LOGGER.info("[EaglerProxy] Eagler handshake complete, proxying packets")

                } catch (e: Exception) {
                    LOGGER.error("[EaglerProxy] Eagler connection failed: ${e.message}", e)
                    ctx.channel().close()
                }
            }.start()
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg !is ByteBuf) return

            try {
                if (!handshakeDone) {
                    if (proxy.debug) LOGGER.info("[EaglerProxy] Discarding pre-handshake packet from vanilla client")
                    return
                }

                val bytes = ByteArray(msg.readableBytes())
                msg.readBytes(bytes)

                val client = eaglerClient
                if (client != null && client.ready) {
                    client.sendMCPacket(bytes)
                }
            } finally {
                msg.release()
            }
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            if (proxy.debug) LOGGER.info("[EaglerProxy] Vanilla client disconnected")
            proxy.vanillaChannelRef.set(null)
            eaglerClient?.close()
            proxy.eaglerClientRef.set(null)
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            LOGGER.error("[EaglerProxy] Proxy error: ${cause.message}", cause)
            ctx.close()
        }
    }
}