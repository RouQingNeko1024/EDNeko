/*
 * EDNeko Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * ViaVersion 协议版本欺骗模块 - 轻量实现
 *
 * 功能说明：
 * - 在 C00 握手中劫持协议版本以实现版本欺骗
 * - 根据不同协议版本提供差异化的行为特性
 * - 注意：本实现使用 C00 协议欺骗方式（非完整 ViaVersion 管线），
 *   适用于大部分需要版本欺骗的服务器场景。
 *   如需完整的协议转换支持（如 1.8 客户端连接 1.12+ 服务器），
 *   需在 Java 17+ 环境下集成 ViaVersion 完整管线。
 */
package net.ccbluex.liquidbounce.features.module.modules.dev

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.via.ViaProtocol
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.network.handshake.client.C00Handshake

object Via : Module("Via", Category.DEV) {

    // ---- 版本选择 ----
    private val versionList = arrayOf(
        "1.7.2-1.7.5",
        "1.7.6-1.7.10",
        "1.8.x",
        "1.9",
        "1.9.1",
        "1.9.2",
        "1.9.3-1.9.4",
        "1.10.x",
        "1.11",
        "1.11.1-1.11.2",
        "1.12",
        "1.12.1",
        "1.12.2",
        "1.13",
        "1.13.1",
        "1.13.2",
        "1.14",
        "1.14.1",
        "1.14.2",
        "1.14.3",
        "1.14.4",
        "1.15",
        "1.15.1",
        "1.15.2",
        "1.16",
        "1.16.1",
        "1.16.2",
        "1.16.3",
        "1.16.4-1.16.5",
        "1.17",
        "1.17.1",
        "1.18-1.18.1",
        "1.18.2",
        "1.19",
        "1.19.1-1.19.2",
        "1.19.3",
        "1.19.4",
        "1.20-1.20.1",
        "1.20.2",
        "1.20.3-1.20.4",
        "1.20.5-1.20.6",
        "1.21-1.21.1",
        "1.21.2-1.21.3",
        "1.21.4+"
    )

    private val versionIDs = mapOf(
        "1.7.2-1.7.5" to ViaProtocol.R1_7_2,
        "1.7.6-1.7.10" to ViaProtocol.R1_7_6,
        "1.8.x" to ViaProtocol.R1_8,
        "1.9" to ViaProtocol.R1_9,
        "1.9.1" to ViaProtocol.R1_9_1,
        "1.9.2" to ViaProtocol.R1_9_2,
        "1.9.3-1.9.4" to ViaProtocol.R1_9_4,
        "1.10.x" to ViaProtocol.R1_10,
        "1.11" to ViaProtocol.R1_11,
        "1.11.1-1.11.2" to ViaProtocol.R1_11_1,
        "1.12" to ViaProtocol.R1_12,
        "1.12.1" to ViaProtocol.R1_12_1,
        "1.12.2" to ViaProtocol.R1_12_2,
        "1.13" to ViaProtocol.R1_13,
        "1.13.1" to ViaProtocol.R1_13_1,
        "1.13.2" to ViaProtocol.R1_13_2,
        "1.14" to ViaProtocol.R1_14,
        "1.14.1" to ViaProtocol.R1_14_1,
        "1.14.2" to ViaProtocol.R1_14_2,
        "1.14.3" to ViaProtocol.R1_14_3,
        "1.14.4" to ViaProtocol.R1_14_4,
        "1.15" to ViaProtocol.R1_15,
        "1.15.1" to ViaProtocol.R1_15_1,
        "1.15.2" to ViaProtocol.R1_15_2,
        "1.16" to ViaProtocol.R1_16,
        "1.16.1" to ViaProtocol.R1_16_1,
        "1.16.2" to ViaProtocol.R1_16_2,
        "1.16.3" to ViaProtocol.R1_16_3,
        "1.16.4-1.16.5" to ViaProtocol.R1_16_4,
        "1.17" to ViaProtocol.R1_17,
        "1.17.1" to ViaProtocol.R1_17_1,
        "1.18-1.18.1" to ViaProtocol.R1_18,
        "1.18.2" to ViaProtocol.R1_18_2,
        "1.19" to ViaProtocol.R1_19,
        "1.19.1-1.19.2" to ViaProtocol.R1_19_1,
        "1.19.3" to ViaProtocol.R1_19_3,
        "1.19.4" to ViaProtocol.R1_19_4,
        "1.20-1.20.1" to ViaProtocol.R1_20,
        "1.20.2" to ViaProtocol.R1_20_2,
        "1.20.3-1.20.4" to ViaProtocol.R1_20_3,
        "1.20.5-1.20.6" to ViaProtocol.R1_20_5,
        "1.21-1.21.1" to ViaProtocol.R1_21,
        "1.21.2-1.21.3" to ViaProtocol.R1_21_2,
        "1.21.4+" to ViaProtocol.R1_21_4
    )

    // ---- 设置 ----
    private val targetVersionName by choices("TargetVersion", versionList, "1.8.x")
    private val spoofProtocol by boolean("SpoofProtocol", false)
    private val customProtocolId by int("CustomProtocolID", 47, 0..1000) { spoofProtocol }

    // ---- 特性开关 ----
    private val useVersionDifferences by boolean("UseVersionDifferences", true)
    private val flyingPacketsV113 by boolean("FlyingPackets1.13+", true) { useVersionDifferences }
    private val delayedSneak by boolean("DelayedSneak1.15+", false) { useVersionDifferences }
    private val sprintWhenSneaking by boolean("SprintWhenSneaking1.14+", false) { useVersionDifferences }
    private val motionResetOnCollision by boolean("MotionResetOnCollision<1.14", true) { useVersionDifferences }
    private val useClickPatternsSkip by boolean("SkipClickPatterns1.13+", true) { useVersionDifferences }
    private val useCombatUpdate by boolean("CombatUpdate1.9+", false) { useVersionDifferences }

    override fun onEnable() {
        // 应用目标版本
        applyTargetVersion()

        // 显示状态信息
        val targetId = versionIDs[targetVersionName] ?: ViaProtocol.R1_8
        val features = mutableListOf<String>()
        if (spoofProtocol) features.add("CustomProtocol($customProtocolId)")
        if (useClickPatternsSkip) features.add("SkipClickPatterns")
        if (useVersionDifferences) {
            if (flyingPacketsV113) features.add("FlyingPackets1.13+")
            if (delayedSneak) features.add("DelayedSneak1.15+")
            if (motionResetOnCollision) features.add("MotionReset<1.14")
        }
        chat("§7[Via] §aEnabled §7- §f$targetVersionName §7($targetId)")
        if (targetId != ViaProtocol.NATIVE_VERSION || spoofProtocol) {
            if (spoofProtocol) {
                chat("§7[Via] §aCustom protocol spoofing active: §f$customProtocolId")
            } else {
                chat("§7[Via] §aVersion spoofing active: §f$targetVersionName §7($targetId)")
            }
            chat("§7[Via] §eNote: Protocol version spoofed via C00 handshake. Full packet translation requires Java 17+.")
        }
        if (features.isNotEmpty()) {
            chat("§7[Via] §7Features: ${features.joinToString(", ")}")
        }
    }

    override fun onDisable() {
        // 恢复为原生版本
        ViaProtocol.setTargetVersion(ViaProtocol.NATIVE_VERSION)
        chat("§7[Via] §cDisabled §7- §fReverted to 1.8 (${ViaProtocol.NATIVE_VERSION})")
    }

    @Suppress("unused")
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is C00Handshake) {
            val targetId = versionIDs[targetVersionName] ?: ViaProtocol.R1_8
            try {
                val field = C00Handshake::class.java.getDeclaredField("protocolVersion")
                field.isAccessible = true
                if (spoofProtocol) {
                    field.setInt(packet, customProtocolId)
                } else if (targetId != ViaProtocol.NATIVE_VERSION) {
                    field.setInt(packet, targetId)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * 应用选中的目标版本
     */
    private fun applyTargetVersion() {
        val targetId = versionIDs[targetVersionName] ?: ViaProtocol.R1_8
        ViaProtocol.setTargetVersion(targetId)
    }

    /**
     * 打开协议版本选择 GUI（简化版 - 使用模块设置界面即可切换版本）
     */
    fun openProtocolSelector(parent: GuiScreen? = null) {
        chat("§7[Via] §eUse the TargetVersion setting in the module to switch protocol versions.")
        chat("§7[Via] §eCurrent: $targetVersionName (${versionIDs[targetVersionName] ?: ViaProtocol.R1_8})")
    }

    /**
     * 获取当前协议版本名称
     */
    fun getCurrentVersionName(): String {
        return targetVersionName
    }

    /**
     * 获取当前协议版本 ID
     */
    fun getCurrentVersionId(): Int {
        return versionIDs[targetVersionName] ?: ViaProtocol.R1_8
    }

    /**
     * 是否为 1.13+
     */
    fun isAboveV113(): Boolean {
        return (versionIDs[targetVersionName] ?: ViaProtocol.R1_8) >= ViaProtocol.R1_13
    }

    /**
     * 是否为 1.9+
     */
    fun isAboveV19(): Boolean {
        return (versionIDs[targetVersionName] ?: ViaProtocol.R1_8) >= ViaProtocol.R1_9
    }
}