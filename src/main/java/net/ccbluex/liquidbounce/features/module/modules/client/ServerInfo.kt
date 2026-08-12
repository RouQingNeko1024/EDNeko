package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ServerUtils.serverData
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.multiplayer.ServerAddress
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.network.play.server.S03PacketTimeUpdate
import kotlin.math.roundToInt

object ServerInfo : Module("ServerInfo", Category.CLIENT) {

    private var lastTimeUpdate: Long = 0
    private var timeStamps = mutableListOf<Long>()
    private var currentTPS = 20.0

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is S03PacketTimeUpdate) {
            val now = System.currentTimeMillis()
            if (lastTimeUpdate != 0L) {
                val diff = now - lastTimeUpdate
                timeStamps.add(diff)
                if (timeStamps.size > 20) timeStamps.removeAt(0)
                if (timeStamps.isNotEmpty()) {
                    val avgDiff = timeStamps.average()
                    if (avgDiff > 0) {
                        currentTPS = (20000.0 / avgDiff).coerceIn(0.0, 20.0)
                    }
                }
            }
            lastTimeUpdate = now
        }
    }

    override fun onEnable() {
        try {
            if (mc.thePlayer == null || mc.theWorld == null) {
                chat("§c此模块需要在游戏中使用")
                state = false
                return
            }

            if (mc.currentServerData == null || mc.isSingleplayer) {
                chat("§c此模块在单人游戏中无法使用")
                state = false
                return
            }

            val serverAddress = ServerAddress.fromString(serverData?.serverIP)
            val data = mc.currentServerData

            val ping = mc.netHandler.getPlayerInfo(mc.thePlayer.uniqueID)?.responseTime ?: -1

            val serverBrand = mc.getIntegratedServer()?.serverModName ?: "Unknown"

            chat("§c§l=== 服务器信息 ===")
            chat("")
            chat("§e§l基本信息:")
            chat("§7名称: §f${data.serverName}")
            chat("§7地址: §f${serverAddress?.ip}:${serverAddress?.port}")
            chat("§7MOTD: §f${data.serverMOTD}")
            chat("")
            chat("§e§l版本信息:")
            chat("§7游戏版本: §f${data.gameVersion}")
            chat("§7协议版本: §f${data.version}")
            chat("§7服务器品牌: §f$serverBrand")
            chat("")
            chat("§e§l玩家信息:")
            chat("§7玩家数: §f${data.populationInfo}")
            chat("§7延迟: §f${ping}ms")
            chat("§7TPS: §f${(currentTPS * 100.0).roundToInt() / 100.0}")
            chat("")
            chat("§e§l其他信息:")
            chat("§7资源包: §f${getResourceModeText(data.resourceMode)}")
            
            val antiCheat = guessAntiCheat(data.serverIP, serverBrand)
            if (antiCheat != null) {
                chat("§7疑似反作弊: §f$antiCheat")
            }

            chat("")
            chat("§c§l==================")
        } catch (e: Exception) {
            chat("§c获取服务器信息时发生错误: ${e.message}")
        }

        state = false
    }

    private fun getResourceModeText(mode: ServerData.ServerResourceMode): String {
        return when (mode) {
            ServerData.ServerResourceMode.ENABLED -> "强制"
            ServerData.ServerResourceMode.PROMPT -> "可选"
            ServerData.ServerResourceMode.DISABLED -> "禁用"
            else -> "未知"
        }
    }

    private fun guessAntiCheat(serverIP: String?, serverBrand: String): String? {
        val ip = serverIP?.lowercase() ?: ""
        val brand = serverBrand.lowercase()
        
        return when {
            ip.contains("hypixel") -> "Watchdog"
            ip.contains("mineplex") -> "GWEN"
            ip.contains("cubecraft") -> "Sentinel"
            ip.contains("shotbow") -> "Shotbow AC"
            ip.contains("badlion") -> "BAC"
            ip.contains("faithful") -> "Verus"
            ip.contains("mineman") -> "Verus"
            ip.contains("minemen") -> "Verus"
            ip.contains("pvp.land") -> "Verus"
            ip.contains("vine") -> "Vulcan"
            ip.contains("mmc") -> "Matrix"
            ip.contains("minemora") -> "Matrix"
            ip.contains("blocks") && ip.contains("mc") -> "Matrix"
            brand.contains("spigot") && brand.contains("paper") -> "Possible AAC/Matrix"
            brand.contains("craftbukkit") -> "Possible NCP/Spigot"
            else -> null
        }
    }
}
