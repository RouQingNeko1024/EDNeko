package net.ccbluex.liquidbounce.utils.customizable

import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.customizable.logic.*
import net.ccbluex.liquidbounce.utils.customizable.editor.LogicEditor
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 自定义逻辑控制器
 *
 * 管理 KillAura 自定义逻辑的整个生命周期：
 * - 控制 Customizable 开关
 * - 管理 LogicGraph 加载/保存
 * - 桥接 LogicRuntime 和 KillAura
 * - 线程安全
 */
object CustomizableController {

    /**
     * 自定义旋转输出 - KillAura 读取此输出
     */
    data class CustomRotationOutput(
        var hasRotation: Boolean = false,
        var targetYaw: Float = 0f,
        var targetPitch: Float = 0f,
        var finalYaw: Float? = null,      // 最终旋转值
        var finalPitch: Float? = null,
        var shouldAttack: Boolean = false,
        var shouldBlock: Boolean = false,
        var forceStopBlock: Boolean = false,
        var shouldSwitchTarget: Boolean = false,
        var cpsValue: Float = 8f,
        var executionChain: List<String> = emptyList(),
        var debugInfo: Map<String, String> = emptyMap()
    )

    // ==================== 状态 ====================

    /**
     * 自定义功能是否启用
     */
    @Volatile
    var enabled: Boolean = false
        private set

    /**
     * 是否为 Customizable 激活状态（enabled + customizable 选项开启）
     */
    @Volatile
    var isCustomizableActive: Boolean = false
        private set

    /**
     * 当前加载的逻辑图
     */
    @Volatile
    private var currentGraph: LogicGraph? = null

    /**
     * 当前逻辑运行时
     */
    @Volatile
    private var currentRuntime: LogicRuntime? = null

    /**
     * 编辑器实例
     */
    @Volatile
    private var editor: LogicEditor? = null

    /**
     * 当前选中的目标（由 KillAura 的 target selection 提供或自定义提供）
     */
    @Volatile
    var currentTarget: EntityLivingBase? = null

    // 当前输出 - 线程安全
    private val currentOutput = java.util.concurrent.atomic.AtomicReference<CustomRotationOutput>()

    // MC 实例
    private val mc = Minecraft.getMinecraft()

    // 数据保存目录
    val saveDir: File
        get() = File(FileManager.dir, "edneko/customizable/killaura")

    // 默认逻辑文件
    val defaultFile: File
        get() = File(saveDir, "default.json")

    // ==================== 初始化 ====================

    init {
        saveDir.mkdirs()
    }

    // ==================== 启用/禁用 ====================

    /**
     * 启用自定义逻辑
     */
    fun enable() {
        if (enabled) return
        enabled = true
        isCustomizableActive = true

        // 加载默认逻辑
        loadDefault()

        // 重置运行时
        currentRuntime?.reset()
    }

    /**
     * 禁用自定义逻辑
     */
    fun disable() {
        if (!enabled) return
        enabled = false
        isCustomizableActive = false
        currentRuntime = null
        currentGraph = null
        currentTarget = null
        currentOutput.set(null)
    }

    // ==================== 逻辑管理 ====================

    /**
     * 加载默认逻辑
     */
    private fun loadDefault() {
        if (defaultFile.exists()) {
            val graph = LogicSerializer.load(defaultFile)
            if (graph != null) {
                setGraph(graph)
                return
            }
        }
        // 没有默认逻辑，创建一个空的
        createDefaultGraph()
    }

    /**
     * 创建默认逻辑图
     */
    private fun createDefaultGraph() {
        val graph = LogicGraph(
            id = UUID.randomUUID().toString(),
            name = "Default KillAura Logic"
        )

        // 添加默认 Tick 事件节点
        val tickNode = LogicNode(
            id = UUID.randomUUID().toString(),
            type = "event_tick",
            definition = NodeRegistry.EventTick,
            x = 50.0, y = 50.0
        )
        graph.addNode(tickNode)

        // 添加获取目标节点
        val getTargetNode = LogicNode(
            id = UUID.randomUUID().toString(),
            type = "target_get_current",
            definition = NodeRegistry.TargetGetCurrent,
            x = 50.0, y = 150.0
        )
        graph.addNode(getTargetNode)

        // 连接 Tick -> GetTarget
        graph.addConnection(LogicConnection(
            id = UUID.randomUUID().toString(),
            sourceNodeId = tickNode.id, sourcePortId = "exec",
            targetNodeId = getTargetNode.id, targetPortId = "exec"
        ))

        // 添加计算角度节点
        val calcRotationNode = LogicNode(
            id = UUID.randomUUID().toString(),
            type = "rotation_calculate",
            definition = NodeRegistry.RotationCalculate,
            x = 50.0, y = 250.0
        )
        graph.addNode(calcRotationNode)

        // 连接 GetTarget(exec) -> CalcRotation(exec)
        graph.addConnection(LogicConnection(
            id = UUID.randomUUID().toString(),
            sourceNodeId = getTargetNode.id, sourcePortId = "exec",
            targetNodeId = calcRotationNode.id, targetPortId = "exec"
        ))

        // 添加平滑节点
        val smoothNode = LogicNode(
            id = UUID.randomUUID().toString(),
            type = "behavior_smooth",
            definition = NodeRegistry.BehaviorSmooth,
            x = 50.0, y = 350.0
        )
        smoothNode.setParameter("smoothFactor", 0.3f)
        graph.addNode(smoothNode)

        // 连接 CalcRotation(exec) -> Smooth(exec)
        graph.addConnection(LogicConnection(
            id = UUID.randomUUID().toString(),
            sourceNodeId = calcRotationNode.id, sourcePortId = "exec",
            targetNodeId = smoothNode.id, targetPortId = "exec"
        ))

        // 添加设置旋转节点
        val setRotationNode = LogicNode(
            id = UUID.randomUUID().toString(),
            type = "rotation_set",
            definition = NodeRegistry.RotationSet,
            x = 50.0, y = 450.0
        )
        graph.addNode(setRotationNode)

        // 连接 Smooth(exec) -> SetRotation(exec)
        graph.addConnection(LogicConnection(
            id = UUID.randomUUID().toString(),
            sourceNodeId = smoothNode.id, sourcePortId = "exec",
            targetNodeId = setRotationNode.id, targetPortId = "exec"
        ))

        setGraph(graph)
    }

    /**
     * 设置当前逻辑图
     */
    fun setGraph(graph: LogicGraph) {
        currentGraph = graph
        currentRuntime = LogicRuntime(graph)
        currentRuntime?.reset()
    }

    /**
     * 获取当前逻辑图
     */
    fun getGraph(): LogicGraph? = currentGraph

    /**
     * 获取当前运行时
     */
    fun getRuntime(): LogicRuntime? = currentRuntime

    // ==================== Tick 执行 ====================

    /**
     * 执行自定义逻辑 Tick
     * 在 Minecraft Tick 线程中由 KillAura 调用
     *
     * @return 控制输出
     */
    fun executeTick(): CustomRotationOutput {
        if (!isCustomizableActive) {
            return CustomRotationOutput()
        }

        val runtime = currentRuntime
        if (runtime == null) {
            // 没有运行时，返回安全默认输出
            return CustomRotationOutput()
        }

        try {
            val output = runtime.executeTick()

            // 构建调试信息
            val debugInfo = mutableMapOf<String, String>()
            debugInfo["target"] = currentTarget?.let {
                "${it.name} (${String.format("%.1f", it.health)} HP)"
            } ?: "None"
            debugInfo["targetDistance"] = currentTarget?.let {
                String.format("%.2f", mc.thePlayer?.getDistanceToEntityBox(it) ?: 0.0)
            } ?: "N/A"
            debugInfo["executionTime"] = "${runtime.executionTime} ms"
            debugInfo["currentNode"] = runtime.lastExecutionChain.lastOrNull() ?: "None"

            val result = CustomRotationOutput(
                hasRotation = output.hasRotation,
                targetYaw = output.targetYaw,
                targetPitch = output.targetPitch,
                finalYaw = output.finalYaw,
                finalPitch = output.finalPitch,
                shouldAttack = output.shouldAttack,
                shouldBlock = output.shouldBlock,
                forceStopBlock = output.forceStopBlock,
                executionChain = runtime.lastExecutionChain.toList(),
                debugInfo = debugInfo
            )

            currentOutput.set(result)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return CustomRotationOutput()
        }
    }

    /**
     * 获取上一次执行输出
     */
    fun getLastOutput(): CustomRotationOutput? = currentOutput.get()

    // ==================== 编辑器 ====================

    /**
     * 打开逻辑编辑器
     */
    fun openEditor() {
        if (editor != null && editor!!.isVisible) {
            javax.swing.SwingUtilities.invokeLater { editor!!.toFront() }
            return
        }

        // 确保有逻辑图
        if (currentGraph == null) {
            createDefaultGraph()
        }

        val graph = currentGraph ?: return

        // 在 Swing EDT 中创建并启动编辑器，避免在 Minecraft 线程操作 Swing 组件导致崩溃
        javax.swing.SwingUtilities.invokeLater {
            try {
                val newEditor = LogicEditor(graph)
                editor = newEditor
                newEditor.start()
            } catch (e: Exception) {
                e.printStackTrace()
                editor = null
            }
        }
    }

    /**
     * 关闭编辑器并自动保存
     */
    fun closeEditor() {
        val ed = editor
        if (ed != null) {
            save()
            javax.swing.SwingUtilities.invokeLater {
                try {
                    ed.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            editor = null
        }
    }

    /**
     * 编辑器是否打开
     */
    fun isEditorOpen(): Boolean = editor?.isVisible == true

    // ==================== 文件操作 ====================

    /**
     * 保存当前逻辑
     */
    fun save(file: File? = null): Boolean {
        val graph = currentGraph ?: return false
        val targetFile = file ?: defaultFile
        return LogicSerializer.save(graph, targetFile)
    }

    /**
     * 加载逻辑
     */
    fun load(file: File? = null): Boolean {
        val targetFile = file ?: defaultFile
        if (!targetFile.exists()) return false

        val graph = LogicSerializer.load(targetFile) ?: return false
        setGraph(graph)
        return true
    }

    /**
     * 重新加载逻辑
     */
    fun reload() {
        if (defaultFile.exists()) {
            load(defaultFile)
        }
    }

    /**
     * 列出所有保存的逻辑文件
     */
    fun listSaves(): List<File> {
        if (!saveDir.exists()) return emptyList()
        return saveDir.listFiles { f -> f.extension == "json" }?.toList() ?: emptyList()
    }

    // ==================== 调试 ====================

    /**
     * 获取当前调试信息
     */
    fun getDebugInfo(): Map<String, String> {
        val output = currentOutput.get() ?: return emptyMap()
        val runtime = currentRuntime

        val info = mutableMapOf<String, String>()
        info.putAll(output.debugInfo)

        if (runtime != null) {
            info["executionChain"] = runtime.lastExecutionChain.joinToString(" -> ")
            info["debugLog"] = runtime.debugLog.joinToString("\n")
            info["state"] = if (runtime.isRunning) "RUNNING" else "IDLE"

            // 转头状态
            val state = runtime.state
            info["noiseValue"] = String.format("%.4f", state.noiseValue)
            info["inertiaValue"] = String.format("%.4f", state.inertiaValue)

            // 旋转信息
            info["playerYaw"] = String.format("%.2f", state.currentRotation?.yaw ?: 0f)
            info["playerPitch"] = String.format("%.2f", state.currentRotation?.pitch ?: 0f)
            info["targetYaw"] = String.format("%.2f", state.targetRotation?.yaw ?: 0f)
            info["targetPitch"] = String.format("%.2f", state.targetRotation?.pitch ?: 0f)
        }

        return info
    }

    /**
     * 验证当前逻辑图
     */
    fun validate(): List<String> {
        val graph = currentGraph ?: return listOf("No graph loaded")
        val errors = mutableListOf<String>()

        if (graph.nodes.isEmpty()) {
            errors.add("Empty graph - no nodes")
        }

        // 检查入口节点
        val entryNodes = graph.findEntryNodes()
        if (entryNodes.isEmpty()) {
            errors.add("No entry node (event node) found")
        }

        // 检查孤立节点
        val connectedNodes = mutableSetOf<String>()
        entryNodes.forEach { entry ->
            val visited = mutableSetOf<String>()
            traverseGraph(entry, graph, visited)
            connectedNodes.addAll(visited)
        }

        graph.nodes.keys.forEach { nodeId ->
            if (nodeId !in connectedNodes) {
                val node = graph.getNode(nodeId)
                errors.add("Unreachable node: ${node?.definition?.displayName ?: nodeId}")
            }
        }

        // 检查无效连接
        graph.connections.values.forEach { conn ->
            if (!conn.validate(graph)) {
                errors.add("Invalid connection: ${conn.id}")
            }
        }

        return errors
    }

    private fun traverseGraph(node: LogicNode, graph: LogicGraph, visited: MutableSet<String>) {
        if (node.id in visited) return
        visited.add(node.id)

        val execOutput = node.getExecutionOutput()
        if (execOutput != null) {
            graph.getOutputConnections(node.id, execOutput.id).forEach { conn ->
                graph.getNode(conn.targetNodeId)?.let { traverseGraph(it, graph, visited) }
            }
        }
    }

    /**
     * 重置运行时状态
     */
    fun resetRuntime() {
        currentRuntime?.reset()
    }
}