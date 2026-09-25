/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

object FakeF3 : Module("FakeF3", Category.RENDER) {
    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    // ==================== FPS 相关 ====================
    private val fpsEnabled by boolean("自定义FPS", false)
    private val fpsMultiplier by float("FPS乘数", 1.5f, 0.1f..100000000000.0f) { fpsEnabled }
    private val fpsRandomization by float("FPS随机化范围", 0.2f, 0f..1.0f) { fpsEnabled }
    private val lowFpsMultiplier by float("低帧乘数", 2.0f, 0.1f..100000000000.0f) { fpsEnabled }
    private val lowFpsThreshold by int("低帧阈值", 30, 5..60) { fpsEnabled }

    // FPS 缓存 — 每 1000ms 才重新计算一次 FPS，避免闪烁过快
    private val cachedFakeFps = mutableMapOf<Int, Int>() // 行号索引 -> 缓存值
    private var lastFpsCalculationTime = 0L // 上次计算时间戳 (ms)

    // ==================== 显卡 ====================
    private val gpuEnabled by boolean("自定义显卡", false)
    private val gpuName by text("显卡型号", "NVIDIA RTX PRO 6000 Blackwell/PCIe/SSE2") { gpuEnabled }

    // ==================== 内存 ====================
    private val memoryEnabled by boolean("自定义最大内存", false)
    private val maxMemory by text("最大内存", "8388608") { memoryEnabled }
    private val totalMemory by text("总内存", "8388608") { memoryEnabled }

    // ==================== CPU ====================
    private val cpuEnabled by boolean("自定义CPU", false)
    private val cpuName by text("CPU型号", "AMD EPYC 9965 192-Core Processor") { cpuEnabled }

    // ==================== 版本信息 ====================
    private val mcpEnabled by boolean("自定义MCP版本", false)
    private val mcpVersion by text("MCP版本", "9.19") { mcpEnabled }

    private val forgeEnabled by boolean("自定义Forge版本", false)
    private val forgeVersion by text("Forge版本", "14.23.5.2868") { forgeEnabled }

    private val optifineEnabled by boolean("自定义Optifine版本", false)
    private val optifineVersion by text("Optifine版本", "HD U G5") { optifineEnabled }

    @SubscribeEvent
    fun onTextEvent(event: RenderGameOverlayEvent.Text) {
        if (!state) return

        // 每 1000ms 刷新一次 FPS 缓存
        val now = Minecraft.getSystemTime()
        if (now - lastFpsCalculationTime >= 1000L) {
            cachedFakeFps.clear()
            lastFpsCalculationTime = now
        }

        val left = event.left
        val right = event.right

        // 处理左侧列表
        processFps(left)
        processDisplayGpu(left)
        processMemory(left)
        processAllocation(left)
        processCpu(left)
        processVersion(left, "MCP", mcpEnabled, mcpVersion)
        processVersion(left, "Forge", forgeEnabled, forgeVersion)
        processVersion(left, "Optifine", optifineEnabled, optifineVersion)

        // 处理右侧列表（某些版本可能有信息）
        processFps(right)
        processDisplayGpu(right)
        processMemory(right)
        processAllocation(right)
        processCpu(right)
        processVersion(right, "MCP", mcpEnabled, mcpVersion)
        processVersion(right, "Forge", forgeEnabled, forgeVersion)
        processVersion(right, "Optifine", optifineEnabled, optifineVersion)
    }

    /**
     * 处理 FPS 行
     * Forge 1.8.9 格式: "fps: 60, T: 10, E: 5"
     * 使用缓存值，每秒才重新计算一次，避免 fps 闪烁过快
     */
    private fun processFps(lines: MutableList<String>) {
        if (!fpsEnabled) return

        for (i in lines.indices) {
            val line = lines[i]
            if (!line.contains("fps", true)) continue

            // 提取第一个数字（就是 FPS 值）
            val numMatch = Regex("""\d+""").find(line) ?: continue
            val originalFps = numMatch.value.toIntOrNull() ?: continue

            // 使用缓存，避免每帧重新计算
            val fakeFps = if (cachedFakeFps.containsKey(i)) {
                cachedFakeFps[i]!!
            } else {
                // 计算假的 FPS
                val multiplyBy = if (originalFps < lowFpsThreshold) lowFpsMultiplier else fpsMultiplier
                val randomOffset = fpsRandomization * Random.nextFloat() * 2f - fpsRandomization
                (originalFps * (multiplyBy + randomOffset)).toInt().coerceAtLeast(1).also {
                    cachedFakeFps[i] = it
                }
            }

            // 替换第一个数字
            lines[i] = line.replaceFirst(Regex("""\d+"""), fakeFps.toString())
            break // 只处理第一行包含 fps 的
        }
    }

    /**
     * 处理显卡/Display 行
     * Optifine 的 F3 布局为两行:
     *   Display: 1920x1200 (NVIDIA GeForce GTX 1080)  ← 第一行
     *   NVIDIA GeForce GTX 1080                        ← 第二行（纯显卡名）
     * 替换第二行的显卡名为自定义名
     * 如果 Display 行没有下一行（纯 Forge），不做任何修改
     */
    private fun processDisplayGpu(lines: MutableList<String>) {
        if (!gpuEnabled) return

        for (i in lines.indices) {
            val line = lines[i]
            if (!line.contains("Display", true)) continue

            // 替换下一行（第二行）为自定义显卡名
            val nextIdx = i + 1
            if (nextIdx < lines.size) {
                lines[nextIdx] = gpuName
            }
            // 如果 Display 行没有下一行（纯 Forge），不做修改
            return
        }
    }

    /**
     * 处理内存行 (Mem)
     * Forge 1.8.9 格式: "Mem: 245/1024MB"
     */
    private fun processMemory(lines: MutableList<String>) {
        if (!memoryEnabled) return

        for (i in lines.indices) {
            val line = lines[i]
            if (!line.contains("Mem:", true)) continue

            // Mem: 使用过的内存/已分配的最大内存MB
            val usedMatch = Regex("""Mem\s*:\s*(\d+)""", RegexOption.IGNORE_CASE).find(line)
            if (usedMatch != null) {
                val used = usedMatch.groupValues[1]
                lines[i] = "Mem: ${used}/$maxMemory" + "MB"
            }
            break
        }
    }

    /**
     * 处理分配内存行 (Allocation)
     * Forge 1.8.9 格式: "Allocation: 150/512MB"
     */
    private fun processAllocation(lines: MutableList<String>) {
        if (!memoryEnabled) return

        for (i in lines.indices) {
            val line = lines[i]
            if (!line.contains("Allocation:", true)) continue

            val usedMatch = Regex("""Allocation\s*:\s*(\d+)""", RegexOption.IGNORE_CASE).find(line)
            if (usedMatch != null) {
                val used = usedMatch.groupValues[1]
                lines[i] = "Allocation: ${used}/$totalMemory" + "MB"
            }
            break
        }
    }

    /**
     * 处理 CPU 行（Optifine 添加）
     * 格式: "CPU: Intel Core i7-6700HQ"
     * 如果没有 CPU 行，在 Display 和 GPU 之间插入
     */
    private fun processCpu(lines: MutableList<String>) {
        if (!cpuEnabled) return

        // 先找现有 CPU 行并替换
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("CPU:", true)) {
                lines[i] = "CPU: $cpuName"
                return
            }
        }

        // 没有找到 CPU 行，尝试在 Display 行后插入
        for (i in lines.indices) {
            if (lines[i].contains("Display", true)) {
                // 往后找一个合适的位置：跳过可能存在的 GPU 行
                var insertPos = i + 1
                while (insertPos < lines.size && lines[insertPos].contains("GPU:", true)) {
                    insertPos++
                }
                // 如果找到的位置不是 CPU 行，并且也没有越界，则插入
                if (insertPos < lines.size && !lines[insertPos].contains("CPU:", true)) {
                    lines.add(insertPos, "CPU: $cpuName")
                } else if (insertPos >= lines.size) {
                    lines.add("CPU: $cpuName")
                }
                return
            }
        }
    }

    /**
     * 处理版本信息行（MCP/Forge/Optifine）
     */
    private fun processVersion(
        lines: MutableList<String>,
        name: String,
        enabled: Boolean,
        version: String
    ) {
        if (!enabled) return

        for (i in lines.indices) {
            val line = lines[i]
            if (!line.contains(name, true)) continue

            // 替换版本号部分
            // MCP 9.18 -> MCP 9.19
            val prefix = Regex("""$name\s*:?\s*""", RegexOption.IGNORE_CASE).find(line)?.value
            if (prefix != null) {
                lines[i] = "$prefix$version"
            } else {
                lines[i] = "$name: $version"
            }
            break
        }
    }
}