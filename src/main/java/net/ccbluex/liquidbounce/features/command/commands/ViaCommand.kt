/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.dev.Via
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.via.ViaProtocol

object ViaCommand : Command("via", "viaproto") {

    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            when (args[1].lowercase()) {
                "open" -> {
                    Via.openProtocolSelector()
                }
                "status" -> {
                    val versionName = Via.getCurrentVersionName()
                    val versionId = Via.getCurrentVersionId()
                    chat("§7[Via] §fCurrent Protocol: §a$versionName §7($versionId)")
                    chat("§7[Via] §fNative Protocol: §a${ViaProtocol.getVersionName(ViaProtocol.NATIVE_VERSION)} §7(${ViaProtocol.NATIVE_VERSION})")
                    chat("§7[Via] §fSpoofing: ${if (ViaProtocol.isSpoofing()) "§aActive" else "§cInactive"}")
                    chat("§7[Via] §fModule: ${if (Via.state) "§aEnabled" else "§cDisabled"}")
                }
                "list" -> {
                    chat("§7[Via] §fAvailable protocol versions:")
                    val versions = listOf(
                        ViaProtocol.R1_7_2, ViaProtocol.R1_7_6, ViaProtocol.R1_8,
                        ViaProtocol.R1_9_4, ViaProtocol.R1_10, ViaProtocol.R1_11,
                        ViaProtocol.R1_12_2, ViaProtocol.R1_13_2, ViaProtocol.R1_14_4,
                        ViaProtocol.R1_15_2, ViaProtocol.R1_16_4, ViaProtocol.R1_17_1,
                        ViaProtocol.R1_18_2, ViaProtocol.R1_19_4, ViaProtocol.R1_20_3,
                        ViaProtocol.R1_21_2
                    )
                    versions.forEach { id ->
                        val active = id == Via.getCurrentVersionId()
                        chat("${if (active) "§a»" else "  "} §f${ViaProtocol.getVersionName(id)} §7($id)${if (active) " §a<--" else ""}")
                    }
                }
                else -> chatSyntax("via <open|status|list>")
            }
            return
        }
        chatSyntax("via <open|status|list>")
    }

}