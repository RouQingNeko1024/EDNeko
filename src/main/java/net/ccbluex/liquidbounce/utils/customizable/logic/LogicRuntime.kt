package net.ccbluex.liquidbounce.utils.customizable.logic

import net.ccbluex.liquidbounce.utils.customizable.CustomizableController
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.*

/**
 * 逻辑运行时 - 执行逻辑图并控制 KillAura 行为
 *
 * 此运行时在 Minecraft Tick 线程中执行，不直接与 Win32 GUI 交互。
 */
class LogicRuntime(private val graph: LogicGraph) {

    // Minecraft 实例
    private val mc = Minecraft.getMinecraft()

    // 运行时状态
    data class RuntimeState(
        var target: EntityLivingBase? = null,
        var currentRotation: Rotation? = null,
        var targetRotation: Rotation? = null,
        var lastCalculatedYaw: Float = 0f,
        var lastCalculatedPitch: Float = 0f,
        var shouldAttack: Boolean = false,
        var shouldBlock: Boolean = false,
        var isBlocking: Boolean = false,
        var blockTimer: Int = 0,
        var attackTimer: Int = 0,
        var attackCooldownTimer: Int = 0,
        var cps: Float = 8f,
        var cpsCounter: Int = 0,
        var lastClickTime: Long = 0L,
        var rotationSpeed: Float = 90f,
        var currentSpeed: Float = 90f,
        var inertiaValue: Float = 0f,
        var noiseValue: Float = 0f,
        var lastYaw: Float = 0f,
        var lastPitch: Float = 0f,
        var yawVelocity: Float = 0f,
        var pitchVelocity: Float = 0f
    )

    val state = RuntimeState()

    // 变量存储
    private val variables = mutableMapOf<String, Any?>()

    // 延迟节点计数
    private val delayCounters = mutableMapOf<String, Int>()

    // 随机种子映射
    private val randomGenerators = mutableMapOf<String, java.util.Random>()

    // 调试日志
    val debugLog = mutableListOf<String>()
    var currentNodeId: String? = null
    var executionTime: Long = 0L
    var isRunning = false

    // 上次执行的节点链（用于调试）
    val lastExecutionChain = mutableListOf<String>()

    /**
     * 重置运行时状态
     */
    fun reset() {
        state.target = null
        state.currentRotation = null
        state.targetRotation = null
        state.shouldAttack = false
        state.shouldBlock = false
        state.isBlocking = false
        state.blockTimer = 0
        state.attackTimer = 0
        state.cpsCounter = 0
        state.inertiaValue = 0f
        state.noiseValue = 0f
        state.yawVelocity = 0f
        state.pitchVelocity = 0f
        variables.clear()
        delayCounters.clear()
        debugLog.clear()
        lastExecutionChain.clear()

        // 重置变量到默认值
        graph.variables.forEach { (name, def) ->
            variables[name] = def.defaultValue
        }
    }

    /**
     * 重置Tick级变量
     */
    private fun resetTickVariables() {
        graph.variables.forEach { (name, def) ->
            if (def.scope == LogicGraph.VariableScope.TICK) {
                variables[name] = def.defaultValue
            }
        }
    }

    /**
     * 重置Target级变量
     */
    fun resetTargetVariables() {
        graph.variables.forEach { (name, def) ->
            if (def.scope == LogicGraph.VariableScope.TARGET) {
                variables[name] = def.defaultValue
            }
        }
    }

    /**
     * 执行逻辑图的主Tick更新
     * @return CustomizableController.ControlOutput 输出
     */
    fun executeTick(): CustomizableController.CustomRotationOutput {
        val startTime = System.nanoTime()
        isRunning = true
        resetTickVariables()
        debugLog.clear()
        lastExecutionChain.clear()

        val output = CustomizableController.CustomRotationOutput()
        state.currentRotation = RotationUtils.currentRotation ?: mc.thePlayer?.let {
            Rotation(it.rotationYaw, it.rotationPitch)
        }

        try {
            // 找到所有入口节点（事件节点）
            val entryNodes = graph.findEntryNodes()

            for (entryNode in entryNodes) {
                when (entryNode.type) {
                    "event_tick" -> {
                        currentNodeId = entryNode.id
                        executeExecutionChain(entryNode, output)
                    }
                    "event_rotation_update" -> {
                        // RotationUpdate 事件也在Tick中执行
                        currentNodeId = entryNode.id
                        executeExecutionChain(entryNode, output)
                    }
                    "event_pre" -> {
                        currentNodeId = entryNode.id
                        executeExecutionChain(entryNode, output)
                    }
                }
            }

            // 应用最终结果
            applyResults(output)

        } catch (e: Exception) {
            e.printStackTrace()
            debugLog.add("ERROR: ${e.message}")
            // 确保异常不导致崩溃 - 使用安全默认值
            applySafeDefaults(output)
        }

        executionTime = (System.nanoTime() - startTime) / 1_000_000
        isRunning = false
        return output
    }

    /**
     * 应用输出结果到KillAura状态
     */
    private fun applyResults(output: CustomizableController.CustomRotationOutput) {
        // 设置目标旋转
        if (output.hasRotation) {
            state.targetRotation = Rotation(output.targetYaw, output.targetPitch)
            output.finalYaw = output.targetYaw
            output.finalPitch = output.targetPitch
        } else {
            // 使用安全默认值 - 不修改当前旋转
            output.finalYaw = state.currentRotation?.yaw ?: 0f
            output.finalPitch = state.currentRotation?.pitch ?: 0f
        }
    }

    /**
     * 安全默认值
     */
    private fun applySafeDefaults(output: CustomizableController.CustomRotationOutput) {
        output.hasRotation = false
        output.shouldAttack = false
        output.shouldBlock = false
    }

    /**
     * 执行执行链
     */
    private fun executeExecutionChain(startNode: LogicNode, output: CustomizableController.CustomRotationOutput) {
        val visited = mutableSetOf<String>()
        executeNode(startNode, output, visited)
    }

    /**
     * 执行单个节点
     */
    private fun executeNode(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>, depth: Int = 0) {
        // 防止无限循环
        if (node.id in visited || depth > 100) return
        visited.add(node.id)
        currentNodeId = node.id
        lastExecutionChain.add("${node.definition.displayName} [$depth]")

        try {
            when (node.type) {
                // ===== Target Nodes =====
                "target_get_current" -> executeTargetGetCurrent(node, output, visited)
                "target_get_nearest" -> executeTargetGetNearest(node, output, visited)
                "target_exists" -> {/* handled by condition resolution */}
                "target_get_distance" -> {/* handled by data flow */}

                // ===== Rotation Nodes =====
                "rotation_calculate" -> executeRotationCalculate(node, output)
                "rotation_set" -> executeRotationSet(node, output)
                "rotation_offset" -> executeRotationOffset(node, output)

                // ===== Behavior Nodes =====
                "behavior_smooth" -> executeBehaviorSmooth(node, output, visited)
                "behavior_inertia" -> executeBehaviorInertia(node, output, visited)
                "behavior_random_offset" -> executeBehaviorRandomOffset(node, output, visited)
                "behavior_jitter" -> executeBehaviorJitter(node, output, visited)
                "behavior_angle_limit" -> executeBehaviorAngleLimit(node, output, visited)
                "behavior_speed_limit" -> executeBehaviorSpeedLimit(node, output, visited)
                "behavior_delay" -> executeBehaviorDelay(node, output, visited)

                // ===== Attack Nodes =====
                "attack_execute" -> executeAttackExecute(node, output, visited)
                "attack_left_click" -> output.shouldAttack = true
                "attack_delay" -> {
                    val delay = (node.getParameter("delayTicks") as? Int) ?: 2
                    if (state.attackTimer <= 0) {
                        state.attackTimer = delay
                        executeNextExecution(node, output, visited, depth)
                    } else {
                        state.attackTimer--
                    }
                }

                // ===== Block Nodes =====
                "block_start" -> executeBlockStart(node, output, visited)
                "block_stop" -> { output.shouldBlock = false; output.forceStopBlock = true }
                "block_condition" -> executeBlockCondition(node, output, visited)
                "block_duration" -> executeBlockDuration(node, output, visited)

                // ===== Condition Nodes =====
                "condition_if" -> executeConditionIf(node, output, visited, depth)

                // ===== Debug Nodes =====
                "debug_print" -> {
                    val msg = node.getParameter("message") as? String ?: ""
                    debugLog.add("PRINT: $msg")
                    executeNextExecution(node, output, visited, depth)
                }
                "debug_log_value" -> {
                    val label = node.getParameter("label") as? String ?: "Value"
                    val value = resolveInputValue(node, "value", output, visited)
                    debugLog.add("$label: $value")
                    executeNextExecution(node, output, visited, depth)
                }
                "debug_log_rotation" -> {
                    val yaw = (resolveInputValue(node, "yaw", output, visited) as? Number)?.toFloat() ?: 0f
                    val pitch = (resolveInputValue(node, "pitch", output, visited) as? Number)?.toFloat() ?: 0f
                    debugLog.add("Rotation: yaw=$yaw pitch=$pitch")
                    executeNextExecution(node, output, visited, depth)
                }
                "debug_log_target" -> {
                    val target = state.target
                    debugLog.add("Target: ${target?.name ?: "None"} (${target?.health ?: 0} HP)")
                    executeNextExecution(node, output, visited, depth)
                }
            }
        } catch (e: Exception) {
            debugLog.add("ERROR at ${node.definition.displayName}: ${e.message}")
        }

        visited.remove(node.id)
    }

    /**
     * 执行下一个执行链节点
     */
    private fun executeNextExecution(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>, depth: Int) {
        val execOutput = node.getExecutionOutput() ?: return
        val connections = graph.getOutputConnections(node.id, execOutput.id)
        for (conn in connections) {
            val nextNode = graph.getNode(conn.targetNodeId) ?: continue
            if (nextNode.definition.inputs.any { it.id == conn.targetPortId && it.isExecution }) {
                executeNode(nextNode, output, visited, depth + 1)
            }
        }
    }

    /**
     * 解析输入端口的值（通过连接传递）
     */
    private fun resolveInputValue(node: LogicNode, portId: String, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>): Any? {
        val connections = graph.getInputConnections(node.id).filter { it.targetPortId == portId }
        if (connections.isEmpty()) return null

        val conn = connections.firstOrNull() ?: return null
        val sourceNode = graph.getNode(conn.sourceNodeId) ?: return null

        return evaluateNodeOutput(sourceNode, conn.sourcePortId, output, visited)
    }

    /**
     * 计算节点输出值
     */
    private fun evaluateNodeOutput(node: LogicNode, portId: String, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>): Any? {
        if (node.id in visited) return null
        visited.add(node.id)

        val result = try {
            when (node.type) {
                // Target
                "target_exists" -> state.target != null
                "target_is_valid" -> state.target != null && state.target!!.isEntityAlive && state.target!!.health > 0
                "target_get_distance" -> {
                    val target = state.target
                    if (target != null) mc.thePlayer?.getDistanceToEntityBox(target)?.toFloat() else null
                }
                "target_get_health" -> state.target?.health
                "target_in_range" -> {
                    val range = (node.getParameter("range") as? Number)?.toFloat() ?: 4.5f
                    val target = state.target
                    if (target != null) (mc.thePlayer?.getDistanceToEntityBox(target) ?: Double.MAX_VALUE) <= range else false
                }
                "target_in_fov" -> {
                    val fov = (node.getParameter("fov") as? Number)?.toFloat() ?: 90f
                    val target = state.target
                    if (target != null) true else false
                }

                // Rotation
                "rotation_get_current" -> when (portId) {
                    "yaw" -> state.currentRotation?.yaw ?: mc.thePlayer?.rotationYaw ?: 0f
                    "pitch" -> state.currentRotation?.pitch ?: mc.thePlayer?.rotationPitch ?: 0f
                    else -> null
                }
                "rotation_horizontal" -> {
                    val angle = (node.getParameter("angle") as? Number)?.toFloat() ?: 0f
                    when (portId) {
                        "yaw" -> (state.currentRotation?.yaw ?: 0f) + angle
                        else -> null
                    }
                }
                "rotation_vertical" -> {
                    val angle = (node.getParameter("angle") as? Number)?.toFloat() ?: 0f
                    when (portId) {
                        "pitch" -> (state.currentRotation?.pitch ?: 0f) + angle
                        else -> null
                    }
                }

                // Math
                "math_add" -> {
                    val a = (resolveInputValue(node, "a", output, visited) as? Number)?.toFloat() ?: 0f
                    val b = (resolveInputValue(node, "b", output, visited) as? Number)?.toFloat() ?: 0f
                    a + b
                }
                "math_subtract" -> {
                    val a = (resolveInputValue(node, "a", output, visited) as? Number)?.toFloat() ?: 0f
                    val b = (resolveInputValue(node, "b", output, visited) as? Number)?.toFloat() ?: 0f
                    a - b
                }
                "math_multiply" -> {
                    val a = (resolveInputValue(node, "a", output, visited) as? Number)?.toFloat() ?: 0f
                    val b = (resolveInputValue(node, "b", output, visited) as? Number)?.toFloat() ?: 0f
                    a * b
                }
                "math_divide" -> {
                    val a = (resolveInputValue(node, "a", output, visited) as? Number)?.toFloat() ?: 0f
                    val b = (resolveInputValue(node, "b", output, visited) as? Number)?.toFloat() ?: 1f
                    if (b == 0f) 0f else a / b
                }
                "math_clamp" -> {
                    val value = (resolveInputValue(node, "value", output, visited) as? Number)?.toFloat() ?: 0f
                    val min = (node.getParameter("min") as? Number)?.toFloat() ?: 0f
                    val max = (node.getParameter("max") as? Number)?.toFloat() ?: 100f
                    value.coerceIn(min, max)
                }
                "math_min" -> {
                    val a = (resolveInputValue(node, "a", output, visited) as? Number)?.toFloat() ?: 0f
                    val b = (resolveInputValue(node, "b", output, visited) as? Number)?.toFloat() ?: 0f
                    minOf(a, b)
                }
                "math_max" -> {
                    val a = (resolveInputValue(node, "a", output, visited) as? Number)?.toFloat() ?: 0f
                    val b = (resolveInputValue(node, "b", output, visited) as? Number)?.toFloat() ?: 0f
                    maxOf(a, b)
                }
                "math_abs" -> {
                    val value = (resolveInputValue(node, "value", output, visited) as? Number)?.toFloat() ?: 0f
                    abs(value)
                }
                "math_floor" -> {
                    val value = (resolveInputValue(node, "value", output, visited) as? Number)?.toFloat() ?: 0f
                    floor(value).toInt()
                }
                "math_ceil" -> {
                    val value = (resolveInputValue(node, "value", output, visited) as? Number)?.toFloat() ?: 0f
                    ceil(value).toInt()
                }
                "math_round" -> {
                    val value = (resolveInputValue(node, "value", output, visited) as? Number)?.toFloat() ?: 0f
                    round(value).toInt()
                }
                "math_sin" -> {
                    val value = (resolveInputValue(node, "value", output, visited) as? Number)?.toFloat() ?: 0f
                    sin(Math.toRadians(value.toDouble())).toFloat()
                }
                "math_cos" -> {
                    val value = (resolveInputValue(node, "value", output, visited) as? Number)?.toFloat() ?: 0f
                    cos(Math.toRadians(value.toDouble())).toFloat()
                }
                "math_lerp" -> {
                    val a = (resolveInputValue(node, "a", output, visited) as? Number)?.toFloat() ?: 0f
                    val b = (resolveInputValue(node, "b", output, visited) as? Number)?.toFloat() ?: 0f
                    val factor = (node.getParameter("factor") as? Number)?.toFloat() ?: 0.5f
                    a + (b - a) * factor
                }
                "math_distance" -> {
                    val x1 = (resolveInputValue(node, "x1", output, visited) as? Number)?.toDouble() ?: 0.0
                    val y1 = (resolveInputValue(node, "y1", output, visited) as? Number)?.toDouble() ?: 0.0
                    val z1 = (resolveInputValue(node, "z1", output, visited) as? Number)?.toDouble() ?: 0.0
                    val x2 = (resolveInputValue(node, "x2", output, visited) as? Number)?.toDouble() ?: 0.0
                    val y2 = (resolveInputValue(node, "y2", output, visited) as? Number)?.toDouble() ?: 0.0
                    val z2 = (resolveInputValue(node, "z2", output, visited) as? Number)?.toDouble() ?: 0.0
                    sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2) + (z2 - z1).pow(2))
                }

                // Random
                "random_float" -> {
                    val min = (node.getParameter("min") as? Number)?.toFloat() ?: 0f
                    val max = (node.getParameter("max") as? Number)?.toFloat() ?: 1f
                    val seed = (node.getParameter("seed") as? Number)?.toInt() ?: 0
                    val rand = getRandom(node.id, seed)
                    min + rand.nextFloat() * (max - min)
                }
                "random_int" -> {
                    val min = (node.getParameter("min") as? Number)?.toInt() ?: 0
                    val max = (node.getParameter("max") as? Number)?.toInt() ?: 10
                    val seed = (node.getParameter("seed") as? Number)?.toInt() ?: 0
                    val rand = getRandom(node.id, seed)
                    min + rand.nextInt(max - min + 1)
                }
                "random_boolean" -> {
                    val chance = (node.getParameter("chance") as? Number)?.toFloat() ?: 0.5f
                    val seed = (node.getParameter("seed") as? Number)?.toInt() ?: 0
                    val rand = getRandom(node.id, seed)
                    rand.nextFloat() < chance
                }
                "random_noise" -> {
                    val strength = (node.getParameter("strength") as? Number)?.toFloat() ?: 0.1f
                    val frequency = (node.getParameter("frequency") as? Number)?.toFloat() ?: 1f
                    val seed = (node.getParameter("seed") as? Number)?.toInt() ?: 0
                    val rand = getRandom(node.id, seed)
                    val noise = sin(rand.nextFloat() * Math.PI.toFloat() * 2f * frequency).toFloat()
                    state.noiseValue = noise * strength
                    state.noiseValue
                }

                // Variable
                "variable_get" -> {
                    val name = node.getParameter("name") as? String ?: "var"
                    variables[name]
                }

                // Condition
                "condition_and" -> {
                    val a = resolveInputValue(node, "a", output, visited) as? Boolean ?: false
                    val b = resolveInputValue(node, "b", output, visited) as? Boolean ?: false
                    a && b
                }
                "condition_or" -> {
                    val a = resolveInputValue(node, "a", output, visited) as? Boolean ?: false
                    val b = resolveInputValue(node, "b", output, visited) as? Boolean ?: false
                    a || b
                }
                "condition_not" -> {
                    val value = resolveInputValue(node, "value", output, visited) as? Boolean ?: false
                    !value
                }
                "condition_equal" -> {
                    val a = resolveInputValue(node, "a", output, visited)
                    val b = resolveInputValue(node, "b", output, visited)
                    if (a is Number && b is Number) a.toDouble() == b.toDouble()
                    else a == b
                }
                "condition_greater" -> {
                    val a = (resolveInputValue(node, "a", output, visited) as? Number)?.toDouble() ?: 0.0
                    val b = (resolveInputValue(node, "b", output, visited) as? Number)?.toDouble() ?: 0.0
                    a > b
                }
                "condition_less" -> {
                    val a = (resolveInputValue(node, "a", output, visited) as? Number)?.toDouble() ?: 0.0
                    val b = (resolveInputValue(node, "b", output, visited) as? Number)?.toDouble() ?: 0.0
                    a < b
                }
                "condition_between" -> {
                    val value = (resolveInputValue(node, "value", output, visited) as? Number)?.toDouble() ?: 0.0
                    val min = (node.getParameter("min") as? Number)?.toDouble() ?: 0.0
                    val max = (node.getParameter("max") as? Number)?.toDouble() ?: 10.0
                    value in min..max
                }

                // Block
                "block_is_blocking" -> state.isBlocking
                "block_state" -> state.isBlocking

                // Debug
                "debug_runtime_state" -> when (portId) {
                    "state" -> if (state.shouldAttack) "ATTACKING" else if (state.shouldBlock) "BLOCKING" else "IDLE"
                    else -> null
                }

                // Player
                "player_position" -> when (portId) {
                    "x" -> mc.thePlayer?.posX ?: 0.0
                    "y" -> mc.thePlayer?.posY ?: 0.0
                    "z" -> mc.thePlayer?.posZ ?: 0.0
                    else -> null
                }
                "player_rotation" -> when (portId) {
                    "yaw" -> mc.thePlayer?.rotationYaw ?: 0f
                    "pitch" -> mc.thePlayer?.rotationPitch ?: 0f
                    else -> null
                }
                "player_ground" -> mc.thePlayer?.onGround ?: false
                "player_hurt_time" -> mc.thePlayer?.hurtTime ?: 0
                "player_sprint" -> mc.thePlayer?.isSprinting ?: false

                else -> null
            }
        } catch (e: Exception) {
            debugLog.add("ERROR evaluating ${node.definition.displayName}.${portId}: ${e.message}")
            null
        }

        visited.remove(node.id)
        return result
    }

    /**
     * 获取或创建随机数生成器
     */
    private fun getRandom(nodeId: String, seed: Int): java.util.Random {
        val key = "$nodeId:$seed"
        return randomGenerators.getOrPut(key) {
            if (seed != 0) java.util.Random(seed.toLong())
            else java.util.Random()
        }
    }

    // ========== Target Node Executors ==========

    private fun executeTargetGetCurrent(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val target = CustomizableController.currentTarget
        state.target = target
        if (target != null) {
            debugLog.add("Target: ${target.name} (${target.health} HP)")
        }
        executeNextExecution(node, output, visited, depth = 0)
    }

    private fun executeTargetGetNearest(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val range = (node.getParameter("range") as? Number)?.toFloat() ?: 4.5f
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        var nearest: EntityLivingBase? = null
        var nearestDist = Float.MAX_VALUE

        for (entity in theWorld.loadedEntityList) {
            if (entity is EntityLivingBase && entity != thePlayer && entity.isEntityAlive && entity.health > 0) {
                val dist = thePlayer.getDistanceToEntity(entity)
                if (dist <= range && dist < nearestDist) {
                    nearestDist = dist
                    nearest = entity
                }
            }
        }

        state.target = nearest
        if (nearest != null) {
            debugLog.add("Nearest: ${nearest.name} (${nearestDist}m)")
        }
        executeNextExecution(node, output, visited = mutableSetOf(), depth = 0)
    }

    // ========== Rotation Node Executors ==========

    private fun executeRotationCalculate(node: LogicNode, output: CustomizableController.CustomRotationOutput) {
        val target = state.target ?: return
        val player = mc.thePlayer ?: return

        // 计算目标的角度
        val dx = target.posX - player.posX
        val dz = target.posZ - player.posZ
        val dy = target.posY + target.eyeHeight - (player.posY + player.getEyeHeight())

        val horizontalDistance = sqrt(dx * dx + dz * dz)
        val yaw = MathHelper.wrapAngleTo180_float(Math.toDegrees(atan2(dz, dx)).toFloat() - 90f)
        val pitch = MathHelper.wrapAngleTo180_float(-Math.toDegrees(atan2(dy, horizontalDistance)).toFloat())

        output.targetYaw = yaw
        output.targetPitch = pitch
        output.hasRotation = true

        state.lastCalculatedYaw = yaw
        state.lastCalculatedPitch = pitch

        debugLog.add("CalcRotation: yaw=${String.format("%.2f", yaw)} pitch=${String.format("%.2f", pitch)}")
        executeNextExecution(node, output, visited = mutableSetOf(), depth = 0)
    }

    private fun executeRotationSet(node: LogicNode, output: CustomizableController.CustomRotationOutput) {
        val yaw = (resolveInputValue(node, "yaw", output, mutableSetOf()) as? Number)?.toFloat() ?: output.targetYaw
        val pitch = (resolveInputValue(node, "pitch", output, mutableSetOf()) as? Number)?.toFloat() ?: output.targetPitch

        output.finalYaw = yaw
        output.finalPitch = pitch
        output.hasRotation = true

        debugLog.add("SetRotation: yaw=${String.format("%.2f", yaw)} pitch=${String.format("%.2f", pitch)}")
        executeNextExecution(node, output, visited = mutableSetOf(), depth = 0)
    }

    private fun executeRotationOffset(node: LogicNode, output: CustomizableController.CustomRotationOutput) {
        val yawOffset = (node.getParameter("yawOffset") as? Number)?.toFloat() ?: 0f
        val pitchOffset = (node.getParameter("pitchOffset") as? Number)?.toFloat() ?: 0f
        val yaw = (resolveInputValue(node, "yaw", output, mutableSetOf()) as? Number)?.toFloat() ?: output.targetYaw
        val pitch = (resolveInputValue(node, "pitch", output, mutableSetOf()) as? Number)?.toFloat() ?: output.targetPitch

        output.targetYaw = yaw + yawOffset
        output.targetPitch = pitch + pitchOffset
        output.hasRotation = true

        debugLog.add("Offset: yaw=$yawOffset pitch=$pitchOffset")
        executeNextExecution(node, output, visited = mutableSetOf(), depth = 0)
    }

    // ========== Behavior Node Executors ==========

    private fun executeBehaviorSmooth(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val factor = (node.getParameter("smoothFactor") as? Number)?.toFloat() ?: 0.3f
        val targetYaw = output.targetYaw
        val targetPitch = output.targetPitch
        val currentYaw = state.currentRotation?.yaw ?: mc.thePlayer?.rotationYaw ?: 0f
        val currentPitch = state.currentRotation?.pitch ?: mc.thePlayer?.rotationPitch ?: 0f

        // 平滑插值
        val yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - currentYaw)
        val pitchDiff = targetPitch - currentPitch

        output.targetYaw = currentYaw + yawDiff * factor
        output.targetPitch = currentPitch + pitchDiff * factor
        output.hasRotation = true

        debugLog.add("Smooth: factor=$factor")
        executeNextExecution(node, output, visited, 0)
    }

    private fun executeBehaviorInertia(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val inertiaMin = (node.getParameter("inertiaMin") as? Number)?.toFloat() ?: 0.1f
        val inertiaMax = (node.getParameter("inertiaMax") as? Number)?.toFloat() ?: 0.3f
        val targetYaw = output.targetYaw
        val targetPitch = output.targetPitch

        val yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - (state.currentRotation?.yaw ?: 0f))
        val pitchDiff = targetPitch - (state.currentRotation?.pitch ?: 0f)

        // 根据旋转差计算惯性系数
        val diffMagnitude = sqrt((yawDiff * yawDiff + pitchDiff * pitchDiff).toDouble()).toFloat()
        state.inertiaValue = (inertiaMin + (inertiaMax - inertiaMin) * (diffMagnitude / 180f).coerceAtMost(1f))

        debugLog.add("Inertia: ${String.format("%.3f", state.inertiaValue)}")
        executeNextExecution(node, output, visited, 0)
    }

    private fun executeBehaviorRandomOffset(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val yawRange = (node.getParameter("yawRange") as? Number)?.toFloat() ?: 1f
        val pitchRange = (node.getParameter("pitchRange") as? Number)?.toFloat() ?: 0.5f
        val rand = java.util.Random()

        val yawOffset = (rand.nextFloat() * 2f - 1f) * yawRange
        val pitchOffset = (rand.nextFloat() * 2f - 1f) * pitchRange

        output.targetYaw += yawOffset
        output.targetPitch += pitchOffset
        output.hasRotation = true

        debugLog.add("RandomOffset: yaw=${String.format("%.2f", yawOffset)} pitch=${String.format("%.2f", pitchOffset)}")
        executeNextExecution(node, output, visited, 0)
    }

    private fun executeBehaviorJitter(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val jitterStrength = (node.getParameter("jitterStrength") as? Number)?.toFloat() ?: 0.05f
        val jitterSpeed = (node.getParameter("jitterSpeed") as? Number)?.toFloat() ?: 1f
        val time = System.currentTimeMillis() / 50.0

        val yawJitter = sin(time * jitterSpeed).toFloat() * jitterStrength
        val pitchJitter = cos(time * jitterSpeed * 0.7f).toFloat() * jitterStrength

        output.finalYaw = (output.finalYaw ?: 0f) + yawJitter
        output.finalPitch = (output.finalPitch ?: 0f) + pitchJitter

        debugLog.add("Jitter: yaw=${String.format("%.4f", yawJitter)} pitch=${String.format("%.4f", pitchJitter)}")
        executeNextExecution(node, output, visited, 0)
    }

    private fun executeBehaviorAngleLimit(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val maxYawDiff = (node.getParameter("maxYawDiff") as? Number)?.toFloat() ?: 180f
        val maxPitchDiff = (node.getParameter("maxPitchDiff") as? Number)?.toFloat() ?: 90f
        val currentYaw = state.currentRotation?.yaw ?: mc.thePlayer?.rotationYaw ?: 0f
        val currentPitch = state.currentRotation?.pitch ?: mc.thePlayer?.rotationPitch ?: 0f

        val yawDiff = MathHelper.wrapAngleTo180_float(output.targetYaw - currentYaw)
        val pitchDiff = output.targetPitch - currentPitch

        output.targetYaw = currentYaw + yawDiff.coerceIn(-maxYawDiff, maxYawDiff)
        output.targetPitch = currentPitch + pitchDiff.coerceIn(-maxPitchDiff, maxPitchDiff)
        output.hasRotation = true

        executeNextExecution(node, output, visited, 0)
    }

    private fun executeBehaviorSpeedLimit(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val maxYawSpeed = (node.getParameter("maxYawSpeed") as? Number)?.toFloat() ?: 180f
        val maxPitchSpeed = (node.getParameter("maxPitchSpeed") as? Number)?.toFloat() ?: 90f
        val currentYaw = state.currentRotation?.yaw ?: mc.thePlayer?.rotationYaw ?: 0f
        val currentPitch = state.currentRotation?.pitch ?: mc.thePlayer?.rotationPitch ?: 0f

        val yawDiff = MathHelper.wrapAngleTo180_float(output.targetYaw - currentYaw)
        val pitchDiff = output.targetPitch - currentPitch

        val yawStep = yawDiff.coerceIn(-maxYawSpeed, maxYawSpeed) * 0.05f // per tick
        val pitchStep = pitchDiff.coerceIn(-maxPitchSpeed, maxPitchSpeed) * 0.05f

        output.targetYaw = currentYaw + yawStep
        output.targetPitch = currentPitch + pitchStep
        output.hasRotation = true

        executeNextExecution(node, output, visited, 0)
    }

    private fun executeBehaviorDelay(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val delayTicks = (node.getParameter("delayTicks") as? Number)?.toInt() ?: 1
        val counter = delayCounters.getOrDefault(node.id, 0)

        if (counter >= delayTicks) {
            delayCounters[node.id] = 0
            executeNextExecution(node, output, visited, 0)
        } else {
            delayCounters[node.id] = counter + 1
        }
    }

    // ========== Attack Node Executors ==========

    private fun executeAttackExecute(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        output.shouldAttack = true
        debugLog.add("Attack: TRUE")
        executeNextExecution(node, output, visited, 0)
    }

    // ========== Block Node Executors ==========

    private fun executeBlockStart(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        output.shouldBlock = true
        state.isBlocking = true
        debugLog.add("Block: START")
        executeNextExecution(node, output, visited, 0)
    }

    private fun executeBlockCondition(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val condition = resolveInputValue(node, "condition", output, visited) as? Boolean ?: false
        if (condition) {
            output.shouldBlock = true
            state.isBlocking = true
            debugLog.add("Block: Condition TRUE")
        } else {
            output.shouldBlock = false
            state.isBlocking = false
            debugLog.add("Block: Condition FALSE")
        }
        executeNextExecution(node, output, visited, 0)
    }

    private fun executeBlockDuration(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>) {
        val duration = (node.getParameter("duration") as? Number)?.toInt() ?: 5
        if (state.blockTimer <= 0) {
            output.shouldBlock = true
            state.blockTimer = duration
            debugLog.add("Block: Duration=$duration")
        } else {
            state.blockTimer--
            if (state.blockTimer <= 0) {
                output.shouldBlock = false
                output.forceStopBlock = true
            } else {
                output.shouldBlock = true
            }
        }
        executeNextExecution(node, output, visited, 0)
    }

    // ========== Condition Node Executors ==========

    private fun executeConditionIf(node: LogicNode, output: CustomizableController.CustomRotationOutput, visited: MutableSet<String>, depth: Int) {
        val condition = resolveInputValue(node, "condition", output, visited) as? Boolean ?: false
        debugLog.add("IF: $condition")

        val targetPortId = if (condition) "true" else "false"
        val connections = graph.getOutputConnections(node.id, targetPortId)

        for (conn in connections) {
            val nextNode = graph.getNode(conn.targetNodeId) ?: continue
            executeNode(nextNode, output, visited, depth + 1)
        }
    }

    companion object {
        /**
         * 计算两个角度差
         */
        private fun angleDifference(a: Float, b: Float): Float {
            return MathHelper.wrapAngleTo180_float(a - b)
        }
    }
}