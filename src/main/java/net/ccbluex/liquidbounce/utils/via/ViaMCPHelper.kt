package net.ccbluex.liquidbounce.utils.via

/**
 * Reflection-based helper for interacting with the runtime ViaMCP.
 * ViaMCP classes are loaded at runtime from pre-compiled jars;
 * this helper uses reflection to avoid compile-time dependency on ViaVersion API.
 */
object ViaMCPHelper {

    @Volatile
    var lastTeleportId: Int = -1

    private var reflectionInitialized: Boolean = false
    private var viaMCPInstance: Any? = null
    private var getViaMethod: java.lang.reflect.Method? = null
    private var packetWrapperClass: Class<*>? = null
    private var typesClass: Class<*>? = null
    private var protocol1_9To1_8Class: Class<*>? = null

    init {
        initReflection()
    }

    private fun initReflection() {
        try {
            val viaMCPClass = Class.forName("de.florianmichael.viamcp.ViaMCP")
            val instanceField = viaMCPClass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            viaMCPInstance = instanceField.get(null)

            val viaManagerClass = Class.forName("com.viaversion.viaversion.api.Via")
            getViaMethod = viaManagerClass.getMethod("getManager")

            packetWrapperClass = Class.forName("com.viaversion.viaversion.api.protocol.packet.PacketWrapper")
            typesClass = Class.forName("com.viaversion.viaversion.api.type.Types")
            protocol1_9To1_8Class = Class.forName("com.viaversion.viarewind.protocol.v1_9to1_8.Protocol1_9To1_8")

            reflectionInitialized = true
        } catch (t: Throwable) {
            System.err.println("[EDNeko] ViaMCPHelper: reflection init failed: ${t.message}")
        }
    }

    fun confirmTeleport(): Boolean {
        if (!reflectionInitialized || viaMCPInstance == null) {
            return false
        }
        try {
            val viaManager = getViaMethod!!.invoke(null)
            val protocolManager = viaManager.javaClass.getMethod("getProtocolManager").invoke(viaManager)

            val protocol = protocolManager.javaClass.getMethod("getProtocol", Class::class.java)
                .invoke(protocolManager, protocol1_9To1_8Class)
                ?: return false

            val userField = viaMCPInstance!!.javaClass.getField("user")
            val userConnection = userField.get(viaMCPInstance) ?: return false

            val serverboundPacketsClass = Class.forName(
                "com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3")
            val confirmTeleportPacket = serverboundPacketsClass.getField("CONFIRM_TELEPORT").get(null)

            val wrapper = packetWrapperClass!!.getMethod("create",
                Class.forName("com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType"),
                Class.forName("com.viaversion.viaversion.api.connection.UserConnection")
            ).invoke(null, confirmTeleportPacket, userConnection)

            val varIntType = typesClass!!.getField("VAR_INT").get(null)
            wrapper.javaClass.getMethod("write", varIntType.javaClass, Int::class.java)
                .invoke(wrapper, varIntType, lastTeleportId)

            wrapper.javaClass.getMethod("sendToServer", Class::class.java)
                .invoke(wrapper, protocol1_9To1_8Class)

            return true
        } catch (t: Throwable) {
            System.err.println("[EDNeko] confirmTeleport failed: ${t.message}")
            return false
        }
    }

    fun getSequence(): Int {
        return ViaVersionFix.sequence()
    }
}
