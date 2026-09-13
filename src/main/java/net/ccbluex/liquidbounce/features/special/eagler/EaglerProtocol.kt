package net.ccbluex.liquidbounce.features.special.eagler

import io.netty.buffer.Unpooled
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.minecraft.network.PacketBuffer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.Inflater

object EaglerVarInt {

    fun writeVarInt(value: Int): ByteArray {
        val out = ByteArrayOutputStream()
        var v = value ushr 0
        do {
            var b = v and 0x7F
            v = v ushr 7
            if (v != 0) b = b or 0x80
            out.write(b)
        } while (v != 0)
        return out.toByteArray()
    }

    fun readVarInt(buf: ByteArray, offset: Int = 0): VarIntResult {
        var value = 0
        var shift = 0
        var index = offset
        while (index < buf.size) {
            val b = buf[index++].toInt() and 0xFF
            value = value or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) {
                return VarIntResult(value ushr 0, index - offset, index)
            }
            shift += 7
            if (shift >= 35) throw IllegalArgumentException("VarInt is too big")
        }
        throw IllegalArgumentException("Incomplete VarInt")
    }

    fun writeShort(value: Int): ByteArray {
        return byteArrayOf(
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    fun writeASCII(str: String): ByteArray {
        val body = str.toByteArray(Charsets.ISO_8859_1)
        if (body.size > 255) throw IllegalArgumentException("ASCII string too long: ${body.size}")
        return byteArrayOf(body.size.toByte()) + body
    }

    fun readASCII(buf: ByteArray, offset: Int, len: Int): String {
        return String(buf, offset, len, Charsets.ISO_8859_1)
    }

    fun writeMCString(str: String): ByteArray {
        val body = str.toByteArray(Charsets.UTF_8)
        return writeVarInt(body.size) + body
    }

    data class VarIntResult(val value: Int, val size: Int, val next: Int)
}

object EaglerProtocol {

    const val PKT_CLIENT_VERSION = 0x01
    const val PKT_SERVER_VERSION = 0x02
    const val PKT_VERSION_MISMATCH = 0x03
    const val PKT_CLIENT_REQUEST_LOGIN = 0x04
    const val PKT_SERVER_ALLOW_LOGIN = 0x05
    const val PKT_SERVER_DENY_LOGIN = 0x06
    const val PKT_CLIENT_PROFILE_DATA = 0x07
    const val PKT_CLIENT_FINISH_LOGIN = 0x08
    const val PKT_SERVER_FINISH_LOGIN = 0x09
    const val PKT_SERVER_REDIRECT_TO = 0x0A
    const val PKT_SERVER_ERROR = 0xFF.toByte().toInt()

    const val CLIENT_BRAND = "EDNeko"
    const val CLIENT_VERSION = "1.0.0"
    const val GAME_PROTOCOL = 47

    fun buildClientVersionPacket(username: String): ByteArray {
        return byteArrayOf(
            PKT_CLIENT_VERSION.toByte(),
            0x02
        ) +
            EaglerVarInt.writeShort(3) +
            EaglerVarInt.writeShort(3) +
            EaglerVarInt.writeShort(4) +
            EaglerVarInt.writeShort(5) +
            EaglerVarInt.writeShort(1) +
            EaglerVarInt.writeShort(GAME_PROTOCOL) +
            EaglerVarInt.writeASCII(CLIENT_BRAND) +
            EaglerVarInt.writeASCII(CLIENT_VERSION) +
            byteArrayOf(0) +
            EaglerVarInt.writeASCII(username)
    }

    fun buildRequestLoginPacket(username: String, protocolVersion: Int, nicknameSelection: Boolean): ByteArray {
        val parts = mutableListOf<Byte>()
        parts.add(PKT_CLIENT_REQUEST_LOGIN.toByte())

        if (protocolVersion >= 5 && !nicknameSelection) {
            parts.add(0)
        } else {
            val usernameBytes = EaglerVarInt.writeASCII(username)
            parts.addAll(usernameBytes.toList())
        }

        parts.addAll(EaglerVarInt.writeASCII("default").toList())
        parts.add(0)

        if (protocolVersion >= 4) {
            parts.add(0)
            parts.add(0)
        }
        if (protocolVersion >= 5) {
            parts.add(0)
            parts.add(0)
        }

        return parts.toByteArray()
    }

    fun buildProfileDataPacket(skinV3: ByteArray, capeV3: ByteArray, protocolVersion: Int): ByteArray {
        if (protocolVersion >= 4) {
            val entries = listOf(
                "brand_uuid_v1" to clientBrandUUID(),
                "skin_v1" to skinV3,
                "cape_v1" to capeV3
            )
            val parts = mutableListOf<Byte>()
            parts.add(PKT_CLIENT_PROFILE_DATA.toByte())
            parts.add(entries.size.toByte())

            for ((type, data) in entries) {
                parts.addAll(EaglerVarInt.writeASCII(type).toList())
                parts.addAll(EaglerVarInt.writeShort(data.size).toList())
                parts.addAll(data.toList())
            }
            return parts.toByteArray()
        } else {
            return profileEntry("skin_v1", skinV3) + profileEntry("cape_v1", capeV3)
        }
    }

    private fun profileEntry(type: String, data: ByteArray): ByteArray {
        return byteArrayOf(PKT_CLIENT_PROFILE_DATA.toByte()) +
            EaglerVarInt.writeASCII(type) +
            EaglerVarInt.writeShort(data.size) +
            data
    }

    fun buildFinishLoginPacket(): ByteArray {
        return byteArrayOf(PKT_CLIENT_FINISH_LOGIN.toByte())
    }

    fun clientBrandUUID(): ByteArray {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest("EaglercraftXClient:$CLIENT_BRAND".toByteArray(Charsets.UTF_8))
        digest[6] = ((digest[6].toInt() and 0x0F) or 0x30).toByte()
        digest[8] = ((digest[8].toInt() and 0x3F) or 0x80).toByte()
        return digest
    }

    fun parseServerVersion(packet: ByteArray): ServerVersionInfo {
        var off = 1
        val protocolVersion = ((packet[off].toInt() and 0xFF) shl 8) or (packet[off + 1].toInt() and 0xFF)
        off += 2
        val gameVersion = ((packet[off].toInt() and 0xFF) shl 8) or (packet[off + 1].toInt() and 0xFF)
        off += 2

        val brandLen = packet[off++].toInt() and 0xFF
        val brand = EaglerVarInt.readASCII(packet, off, brandLen)
        off += brandLen

        val serverVersionLen = packet[off++].toInt() and 0xFF
        val serverVersion = EaglerVarInt.readASCII(packet, off, serverVersionLen)
        off += serverVersionLen

        val authType = packet[off++].toInt() and 0xFF

        val saltLen = ((packet[off].toInt() and 0xFF) shl 8) or (packet[off + 1].toInt() and 0xFF)
        off += 2
        off += saltLen

        var nicknameSelection = true
        if (protocolVersion >= 5) {
            nicknameSelection = packet[off++].toInt() != 0
        }

        return ServerVersionInfo(protocolVersion, gameVersion, brand, serverVersion, authType, nicknameSelection)
    }

    fun parseAllowLogin(packet: ByteArray, protocolVersion: Int): String {
        var off = 1
        val serverUsernameLen = packet[off++].toInt() and 0xFF
        val serverUsername = EaglerVarInt.readASCII(packet, off, serverUsernameLen)
        off += serverUsernameLen
        off += 16

        if (protocolVersion >= 5) {
            val caps = EaglerVarInt.readVarInt(packet, off)
            off = caps.next
            off += bitCount(caps.value)
            val extCount = packet[off++].toInt() and 0xFF
            off += extCount * 17
        }

        return serverUsername
    }

    fun parseDenyLogin(packet: ByteArray): String {
        var off = 1
        val len = ((packet[off].toInt() and 0xFF) shl 8) or (packet[off + 1].toInt() and 0xFF)
        off += 2
        return String(packet, off, len, Charsets.UTF_8)
    }

    fun parseServerError(packet: ByteArray, v3Style: Boolean): String {
        var off = 1
        val code = packet[off++].toInt() and 0xFF
        val len: Int
        if (v3Style) {
            len = ((packet[off].toInt() and 0xFF) shl 8) or (packet[off + 1].toInt() and 0xFF)
            off += 2
        } else {
            len = packet[off++].toInt() and 0xFF
        }
        val msg = String(packet, off, len, Charsets.UTF_8)
        return "Eagler server error $code: $msg"
    }

    private fun bitCount(x: Int): Int {
        var v = x ushr 0
        v = v - ((v ushr 1) and 0x55555555)
        v = (v and 0x33333333) + ((v ushr 2) and 0x33333333)
        return (((v + (v ushr 4)) and 0x0F0F0F0F) * 0x01010101) ushr 24
    }

    fun inflatePacket(data: ByteArray): ByteArray {
        if (data.size < 5) return data
        val expected = ((data[0].toInt() and 0xFF) shl 24) or
            ((data[1].toInt() and 0xFF) shl 16) or
            ((data[2].toInt() and 0xFF) shl 8) or
            (data[3].toInt() and 0xFF)

        val inflater = Inflater()
        inflater.setInput(data, 4, data.size - 4)
        val output = ByteArray(expected)
        val resultLen = inflater.inflate(output)
        inflater.end()

        return if (resultLen == expected) output else output.copyOf(resultLen)
    }

    fun convertEaglerPacketToMC(eaglerData: ByteArray): ByteArray? {
        if (eaglerData.isEmpty()) return null

        val pktId = eaglerData[0].toInt() and 0xFF

        if (pktId == 0xEE) return null
        if (pktId == 0x3F) return null
        if (pktId == 0x03) return null

        return eaglerData
    }

    fun convertMCPacketToEagler(mcData: ByteArray): ByteArray {
        return mcData
    }

    data class ServerVersionInfo(
        val protocolVersion: Int,
        val gameVersion: Int,
        val brand: String,
        val serverVersion: String,
        val authType: Int,
        val nicknameSelection: Boolean
    )
}