package net.ccbluex.liquidbounce.utils.customizable.logic

import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.EVENT
import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.BEHAVIOR
import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.RANDOM
import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.MATH
import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.CONDITION
import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.ATTACK
import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.BLOCK
import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.PLAYER
import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.VARIABLE
import net.ccbluex.liquidbounce.utils.customizable.logic.NodeCategory.DEBUG
import net.ccbluex.liquidbounce.utils.customizable.logic.ValueType.EXECUTION
import net.ccbluex.liquidbounce.utils.customizable.logic.ValueType.BOOLEAN
import net.ccbluex.liquidbounce.utils.customizable.logic.ValueType.INTEGER
import net.ccbluex.liquidbounce.utils.customizable.logic.ValueType.FLOAT
import net.ccbluex.liquidbounce.utils.customizable.logic.ValueType.DOUBLE
import net.ccbluex.liquidbounce.utils.customizable.logic.ValueType.STRING
import net.ccbluex.liquidbounce.utils.customizable.logic.ValueType.VECTOR
import net.ccbluex.liquidbounce.utils.customizable.logic.ValueType.ANY

/**
 * 节点注册表 - 管理所有可用的节点定义
 */
object NodeRegistry {

    private val definitions = mutableMapOf<String, NodeDefinition>()
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            initialized = true
            registerAll()
        }
    }

    /**
     * 注册所有节点
     */
    private fun registerAll() {
        // ==================== Event Nodes ====================
        register(EventTick)
        register(EventAttack)
        register(EventTargetUpdate)
        register(EventRotationUpdate)
        register(EventPre)
        register(EventPost)

        // ==================== Target Nodes ====================
        register(TargetGetCurrent)
        register(TargetGetNearest)
        register(TargetExists)
        register(TargetIsValid)
        register(TargetGetDistance)
        register(TargetGetHealth)
        register(TargetGetPosition)
        register(TargetGetEyes)
        register(TargetGetVelocity)
        register(TargetInRange)
        register(TargetInFOV)
        register(TargetGetYaw)
        register(TargetGetPitch)

        // ==================== Rotation Nodes ====================
        register(RotationSet)
        register(RotationGetCurrent)
        register(RotationGetTarget)
        register(RotationCalculate)
        register(RotationHorizontal)
        register(RotationVertical)
        register(RotationSpeed)
        register(RotationYawSpeed)
        register(RotationPitchSpeed)
        register(RotationOffset)
        register(RotationClamp)
        register(RotationDifference)
        register(RotationDistance)

        // ==================== Behavior Nodes ====================
        register(BehaviorSmooth)
        register(BehaviorAccelerate)
        register(BehaviorDecelerate)
        register(BehaviorInertia)
        register(BehaviorDamping)
        register(BehaviorDelay)
        register(BehaviorRandomOffset)
        register(BehaviorJitter)
        register(BehaviorAngleLimit)
        register(BehaviorSpeedLimit)
        register(BehaviorTargetPrediction)
        register(BehaviorPositionInterpolate)

        // ==================== Random Nodes ====================
        register(RandomFloat)
        register(RandomInt)
        register(RandomBoolean)
        register(RandomRange)
        register(RandomNoiseValue)

        // ==================== Math Nodes ====================
        register(MathAdd)
        register(MathSubtract)
        register(MathMultiply)
        register(MathDivide)
        register(MathClamp)
        register(MathMin)
        register(MathMax)
        register(MathAbs)
        register(MathFloor)
        register(MathCeil)
        register(MathRound)
        register(MathSin)
        register(MathCos)
        register(MathDistance)
        register(MathLerp)

        // ==================== Condition Nodes ====================
        register(ConditionIf)
        register(ConditionAnd)
        register(ConditionOr)
        register(ConditionNot)
        register(ConditionEqual)
        register(ConditionNotEqual)
        register(ConditionGreater)
        register(ConditionLess)
        register(ConditionBetween)

        // ==================== Attack Nodes ====================
        register(AttackExecute)
        register(AttackCanAttack)
        register(AttackCooldown)
        register(AttackCPS)
        register(AttackLeftClick)
        register(AttackRightClick)
        register(AttackDelay)
        register(AttackTarget)
        register(AttackResetTimer)

        // ==================== Block Nodes ====================
        register(BlockStart)
        register(BlockStop)
        register(BlockIsBlocking)
        register(BlockState)
        register(BlockDuration)
        register(BlockCondition)

        // ==================== Player Nodes ====================
        register(PlayerPosition)
        register(PlayerRotation)
        register(PlayerMotion)
        register(PlayerGround)
        register(PlayerSprint)
        register(PlayerHurtTime)
        register(PlayerMovementDirection)

        // ==================== Variable Nodes ====================
        register(VariableSet)
        register(VariableGet)
        register(VariableAdd)
        register(VariableMultiply)
        register(VariableReset)

        // ==================== Debug Nodes ====================
        register(DebugPrint)
        register(DebugLogValue)
        register(DebugLogRotation)
        register(DebugLogTarget)
        register(DebugRuntimeState)
    }

    private fun register(def: NodeDefinition) {
        definitions[def.type] = def
    }

    fun getDefinition(type: String): NodeDefinition? { ensureInitialized(); return definitions[type] }

    fun getAllDefinitions(): Collection<NodeDefinition> { ensureInitialized(); return definitions.values }

    fun getDefinitions(): Collection<NodeDefinition> { ensureInitialized(); return definitions.values }

    fun getDefinitionsByCategory(category: NodeCategory): List<NodeDefinition> {
        ensureInitialized()
        return definitions.values.filter { it.category == category }
    }

    fun getAllCategories(): List<NodeCategory> = NodeCategory.values().toList()

    // ==================== Event Node Definitions ====================
    val EventTick = NodeDefinition(
        type = "event_tick",
        category = EVENT,
        displayName = "Tick",
        description = "每游戏Tick触发",
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val EventAttack = NodeDefinition(
        type = "event_attack",
        category = EVENT,
        displayName = "Attack",
        description = "攻击事件触发",
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val EventTargetUpdate = NodeDefinition(
        type = "event_target_update",
        category = EVENT,
        displayName = "TargetUpdate",
        description = "目标更新时触发",
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val EventRotationUpdate = NodeDefinition(
        type = "event_rotation_update",
        category = EVENT,
        displayName = "RotationUpdate",
        description = "旋转更新时触发",
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val EventPre = NodeDefinition(
        type = "event_pre",
        category = EVENT,
        displayName = "Pre",
        description = "KillAura Pre 事件",
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val EventPost = NodeDefinition(
        type = "event_post",
        category = EVENT,
        displayName = "Post",
        description = "KillAura Post 事件",
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    // ==================== Target Node Definitions ====================
    val TargetGetCurrent = NodeDefinition(
        type = "target_get_current",
        category = NodeCategory.TARGET,
        displayName = "Get Current Target",
        description = "获取当前目标",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("target", "Target", ValueType.TARGET, isInput = false)
        )
    )

    val TargetGetNearest = NodeDefinition(
        type = "target_get_nearest",
        category = NodeCategory.TARGET,
        displayName = "Get Nearest Target",
        description = "获取最近的目标",
        parameters = listOf(
            NodeParameter("range", "Range", FLOAT, 4.5f, 1f, 10f),
            NodeParameter("fov", "FOV", FLOAT, 180f, 0f, 180f)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("target", "Target", ValueType.TARGET, isInput = false)
        )
    )

    val TargetExists = NodeDefinition(
        type = "target_exists",
        category = NodeCategory.TARGET,
        displayName = "Target Exists",
        description = "判断目标是否存在",
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val TargetIsValid = NodeDefinition(
        type = "target_is_valid",
        category = NodeCategory.TARGET,
        displayName = "Target Is Valid",
        description = "判断目标是否有效",
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val TargetGetDistance = NodeDefinition(
        type = "target_get_distance",
        category = NodeCategory.TARGET,
        displayName = "Get Target Distance",
        description = "获取目标距离",
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("distance", "Distance", FLOAT, isInput = false)
        )
    )

    val TargetGetHealth = NodeDefinition(
        type = "target_get_health",
        category = NodeCategory.TARGET,
        displayName = "Get Target Health",
        description = "获取目标血量",
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("health", "Health", FLOAT, isInput = false)
        )
    )

    val TargetGetPosition = NodeDefinition(
        type = "target_get_position",
        category = NodeCategory.TARGET,
        displayName = "Get Target Position",
        description = "获取目标位置",
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("x", "X", DOUBLE, isInput = false),
            NodePort("y", "Y", DOUBLE, isInput = false),
            NodePort("z", "Z", DOUBLE, isInput = false)
        )
    )

    val TargetGetEyes = NodeDefinition(
        type = "target_get_eyes",
        category = NodeCategory.TARGET,
        displayName = "Get Target Eyes",
        description = "获取目标眼睛位置",
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("x", "X", DOUBLE, isInput = false),
            NodePort("y", "Y", DOUBLE, isInput = false),
            NodePort("z", "Z", DOUBLE, isInput = false)
        )
    )

    val TargetGetVelocity = NodeDefinition(
        type = "target_get_velocity",
        category = NodeCategory.TARGET,
        displayName = "Get Target Velocity",
        description = "获取目标速度",
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("velocity", "Velocity", FLOAT, isInput = false)
        )
    )

    val TargetInRange = NodeDefinition(
        type = "target_in_range",
        category = NodeCategory.TARGET,
        displayName = "Target In Range",
        description = "判断目标是否在攻击范围",
        parameters = listOf(
            NodeParameter("range", "Range", FLOAT, 4.5f, 1f, 10f)
        ),
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val TargetInFOV = NodeDefinition(
        type = "target_in_fov",
        category = NodeCategory.TARGET,
        displayName = "Target In FOV",
        description = "判断目标是否在视野范围",
        parameters = listOf(
            NodeParameter("fov", "FOV", FLOAT, 90f, 0f, 180f)
        ),
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val TargetGetYaw = NodeDefinition(
        type = "target_get_yaw",
        category = NodeCategory.TARGET,
        displayName = "Get Target Yaw",
        description = "获取目标偏航角",
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("yaw", "Yaw", FLOAT, isInput = false)
        )
    )

    val TargetGetPitch = NodeDefinition(
        type = "target_get_pitch",
        category = NodeCategory.TARGET,
        displayName = "Get Target Pitch",
        description = "获取目标俯仰角",
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    // ==================== Rotation Node Definitions ====================
    val RotationSet = NodeDefinition(
        type = "rotation_set",
        category = NodeCategory.ROTATION,
        displayName = "Set Rotation",
        description = "设置玩家旋转",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val RotationGetCurrent = NodeDefinition(
        type = "rotation_get_current",
        category = NodeCategory.ROTATION,
        displayName = "Get Current Rotation",
        description = "获取当前旋转",
        outputs = listOf(
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val RotationGetTarget = NodeDefinition(
        type = "rotation_get_target",
        category = NodeCategory.ROTATION,
        displayName = "Get Target Rotation",
        description = "获取目标旋转",
        outputs = listOf(
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val RotationCalculate = NodeDefinition(
        type = "rotation_calculate",
        category = NodeCategory.ROTATION,
        displayName = "Calculate Target Angle",
        description = "计算目标角度",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val RotationHorizontal = NodeDefinition(
        type = "rotation_horizontal",
        category = NodeCategory.ROTATION,
        displayName = "Horizontal Rotation",
        description = "水平旋转",
        parameters = listOf(
            NodeParameter("angle", "Angle", FLOAT, 0f, -180f, 180f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val RotationVertical = NodeDefinition(
        type = "rotation_vertical",
        category = NodeCategory.ROTATION,
        displayName = "Vertical Rotation",
        description = "垂直旋转",
        parameters = listOf(
            NodeParameter("angle", "Angle", FLOAT, 0f, -90f, 90f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val RotationSpeed = NodeDefinition(
        type = "rotation_speed",
        category = NodeCategory.ROTATION,
        displayName = "Rotation Speed",
        description = "转头速度",
        parameters = listOf(
            NodeParameter("minSpeed", "Min Speed", FLOAT, 30f, 1f, 180f),
            NodeParameter("maxSpeed", "Max Speed", FLOAT, 90f, 1f, 180f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val RotationYawSpeed = NodeDefinition(
        type = "rotation_yaw_speed",
        category = NodeCategory.ROTATION,
        displayName = "Yaw Speed",
        description = "偏航速度",
        parameters = listOf(
            NodeParameter("speed", "Speed", FLOAT, 90f, 1f, 360f)
        ),
        outputs = listOf(
            NodePort("speed", "Speed", FLOAT, isInput = false)
        )
    )

    val RotationPitchSpeed = NodeDefinition(
        type = "rotation_pitch_speed",
        category = NodeCategory.ROTATION,
        displayName = "Pitch Speed",
        description = "俯仰速度",
        parameters = listOf(
            NodeParameter("speed", "Speed", FLOAT, 60f, 1f, 180f)
        ),
        outputs = listOf(
            NodePort("speed", "Speed", FLOAT, isInput = false)
        )
    )

    val RotationOffset = NodeDefinition(
        type = "rotation_offset",
        category = NodeCategory.ROTATION,
        displayName = "Rotation Offset",
        description = "旋转偏移",
        parameters = listOf(
            NodeParameter("yawOffset", "Yaw Offset", FLOAT, 0f, -180f, 180f),
            NodeParameter("pitchOffset", "Pitch Offset", FLOAT, 0f, -90f, 90f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val RotationClamp = NodeDefinition(
        type = "rotation_clamp",
        category = NodeCategory.ROTATION,
        displayName = "Rotation Clamp",
        description = "旋转角度限制",
        parameters = listOf(
            NodeParameter("maxYaw", "Max Yaw", FLOAT, 180f, 0f, 180f),
            NodeParameter("maxPitch", "Max Pitch", FLOAT, 90f, 0f, 90f)
        ),
        inputs = listOf(
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val RotationDifference = NodeDefinition(
        type = "rotation_difference",
        category = NodeCategory.ROTATION,
        displayName = "Rotation Difference",
        description = "旋转差值",
        inputs = listOf(
            NodePort("yaw1", "Yaw 1", FLOAT, isInput = true),
            NodePort("pitch1", "Pitch 1", FLOAT, isInput = true),
            NodePort("yaw2", "Yaw 2", FLOAT, isInput = true),
            NodePort("pitch2", "Pitch 2", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("yawDiff", "Yaw Diff", FLOAT, isInput = false),
            NodePort("pitchDiff", "Pitch Diff", FLOAT, isInput = false)
        )
    )

    val RotationDistance = NodeDefinition(
        type = "rotation_distance",
        category = NodeCategory.ROTATION,
        displayName = "Rotation Distance",
        description = "旋转距离",
        inputs = listOf(
            NodePort("yaw1", "Yaw 1", FLOAT, isInput = true),
            NodePort("pitch1", "Pitch 1", FLOAT, isInput = true),
            NodePort("yaw2", "Yaw 2", FLOAT, isInput = true),
            NodePort("pitch2", "Pitch 2", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("distance", "Distance", FLOAT, isInput = false)
        )
    )

    // ==================== Behavior Node Definitions ====================
    val BehaviorSmooth = NodeDefinition(
        type = "behavior_smooth",
        category = BEHAVIOR,
        displayName = "Smooth",
        description = "平滑旋转",
        parameters = listOf(
            NodeParameter("smoothFactor", "Smooth Factor", FLOAT, 0.3f, 0.01f, 1f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val BehaviorAccelerate = NodeDefinition(
        type = "behavior_accelerate",
        category = BEHAVIOR,
        displayName = "Accelerate",
        description = "加速旋转",
        parameters = listOf(
            NodeParameter("acceleration", "Acceleration", FLOAT, 1.2f, 1.0f, 5.0f),
            NodeParameter("maxSpeed", "Max Speed", FLOAT, 180f, 10f, 360f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val BehaviorDecelerate = NodeDefinition(
        type = "behavior_decelerate",
        category = BEHAVIOR,
        displayName = "Decelerate",
        description = "减速旋转",
        parameters = listOf(
            NodeParameter("deceleration", "Deceleration", FLOAT, 0.8f, 0.1f, 1.0f),
            NodeParameter("minSpeed", "Min Speed", FLOAT, 10f, 1f, 90f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val BehaviorInertia = NodeDefinition(
        type = "behavior_inertia",
        category = BEHAVIOR,
        displayName = "Inertia",
        description = "惯性效果",
        parameters = listOf(
            NodeParameter("inertiaMin", "Inertia Min", FLOAT, 0.1f, 0.01f, 0.5f),
            NodeParameter("inertiaMax", "Inertia Max", FLOAT, 0.3f, 0.01f, 0.5f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val BehaviorDamping = NodeDefinition(
        type = "behavior_damping",
        category = BEHAVIOR,
        displayName = "Damping",
        description = "阻尼效果",
        parameters = listOf(
            NodeParameter("dampingFactor", "Damping Factor", FLOAT, 0.9f, 0.5f, 0.99f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val BehaviorDelay = NodeDefinition(
        type = "behavior_delay",
        category = BEHAVIOR,
        displayName = "Delay",
        description = "延迟执行",
        parameters = listOf(
            NodeParameter("delayTicks", "Delay Ticks", INTEGER, 1, 0, 40)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val BehaviorRandomOffset = NodeDefinition(
        type = "behavior_random_offset",
        category = BEHAVIOR,
        displayName = "Random Offset",
        description = "随机偏移",
        parameters = listOf(
            NodeParameter("yawRange", "Yaw Range", FLOAT, 1f, 0f, 10f),
            NodeParameter("pitchRange", "Pitch Range", FLOAT, 0.5f, 0f, 5f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val BehaviorJitter = NodeDefinition(
        type = "behavior_jitter",
        category = BEHAVIOR,
        displayName = "Micro Jitter",
        description = "微小抖动",
        parameters = listOf(
            NodeParameter("jitterStrength", "Jitter Strength", FLOAT, 0.05f, 0.001f, 1f),
            NodeParameter("jitterSpeed", "Jitter Speed", FLOAT, 1f, 0.1f, 10f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val BehaviorAngleLimit = NodeDefinition(
        type = "behavior_angle_limit",
        category = BEHAVIOR,
        displayName = "Angle Limit",
        description = "角度限制",
        parameters = listOf(
            NodeParameter("maxYawDiff", "Max Yaw Diff", FLOAT, 180f, 1f, 360f),
            NodeParameter("maxPitchDiff", "Max Pitch Diff", FLOAT, 90f, 1f, 180f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true),
            NodePort("currentYaw", "Current Yaw", FLOAT, isInput = true),
            NodePort("currentPitch", "Current Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val BehaviorSpeedLimit = NodeDefinition(
        type = "behavior_speed_limit",
        category = BEHAVIOR,
        displayName = "Speed Limit",
        description = "速度限制",
        parameters = listOf(
            NodeParameter("maxYawSpeed", "Max Yaw Speed", FLOAT, 180f, 1f, 360f),
            NodeParameter("maxPitchSpeed", "Max Pitch Speed", FLOAT, 90f, 1f, 180f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val BehaviorTargetPrediction = NodeDefinition(
        type = "behavior_target_prediction",
        category = BEHAVIOR,
        displayName = "Target Prediction",
        description = "目标位置预测",
        parameters = listOf(
            NodeParameter("predictTicks", "Predict Ticks", INTEGER, 2, 0, 10)
        ),
        inputs = listOf(
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("x", "X", DOUBLE, isInput = false),
            NodePort("y", "Y", DOUBLE, isInput = false),
            NodePort("z", "Z", DOUBLE, isInput = false)
        )
    )

    val BehaviorPositionInterpolate = NodeDefinition(
        type = "behavior_position_interpolate",
        category = BEHAVIOR,
        displayName = "Position Interpolate",
        description = "位置插值",
        parameters = listOf(
            NodeParameter("interpFactor", "Interp Factor", FLOAT, 0.5f, 0.01f, 1f)
        ),
        inputs = listOf(
            NodePort("x1", "X 1", DOUBLE, isInput = true),
            NodePort("y1", "Y 1", DOUBLE, isInput = true),
            NodePort("z1", "Z 1", DOUBLE, isInput = true),
            NodePort("x2", "X 2", DOUBLE, isInput = true),
            NodePort("y2", "Y 2", DOUBLE, isInput = true),
            NodePort("z2", "Z 2", DOUBLE, isInput = true)
        ),
        outputs = listOf(
            NodePort("x", "X", DOUBLE, isInput = false),
            NodePort("y", "Y", DOUBLE, isInput = false),
            NodePort("z", "Z", DOUBLE, isInput = false)
        )
    )

    // ==================== Random Node Definitions ====================
    val RandomFloat = NodeDefinition(
        type = "random_float",
        category = RANDOM,
        displayName = "Random Float",
        description = "随机浮点数",
        parameters = listOf(
            NodeParameter("min", "Min", FLOAT, 0f, -10000f, 10000f),
            NodeParameter("max", "Max", FLOAT, 1f, -10000f, 10000f),
            NodeParameter("seed", "Seed", INTEGER, 0, 0, 999999),
            NodeParameter("updateMode", "Update Mode", STRING, defaultValue = "Tick", options = listOf("Tick", "Target", "Fixed"))
        ),
        outputs = listOf(
            NodePort("value", "Value", FLOAT, isInput = false)
        )
    )

    val RandomInt = NodeDefinition(
        type = "random_int",
        category = RANDOM,
        displayName = "Random Int",
        description = "随机整数",
        parameters = listOf(
            NodeParameter("min", "Min", INTEGER, 0, -10000, 10000),
            NodeParameter("max", "Max", INTEGER, 10, -10000, 10000),
            NodeParameter("seed", "Seed", INTEGER, 0, 0, 999999),
            NodeParameter("updateMode", "Update Mode", STRING, defaultValue = "Tick", options = listOf("Tick", "Target", "Fixed"))
        ),
        outputs = listOf(
            NodePort("value", "Value", INTEGER, isInput = false)
        )
    )

    val RandomBoolean = NodeDefinition(
        type = "random_boolean",
        category = RANDOM,
        displayName = "Random Boolean",
        description = "随机布尔值",
        parameters = listOf(
            NodeParameter("chance", "Chance", FLOAT, 0.5f, 0f, 1f),
            NodeParameter("seed", "Seed", INTEGER, 0, 0, 999999)
        ),
        outputs = listOf(
            NodePort("value", "Value", BOOLEAN, isInput = false)
        )
    )

    val RandomRange = NodeDefinition(
        type = "random_range",
        category = RANDOM,
        displayName = "Random Range",
        description = "随机范围",
        parameters = listOf(
            NodeParameter("min", "Min", FLOAT, 0f, -10000f, 10000f),
            NodeParameter("max", "Max", FLOAT, 1f, -10000f, 10000f),
            NodeParameter("smooth", "Smooth", BOOLEAN, false)
        ),
        outputs = listOf(
            NodePort("value", "Value", FLOAT, isInput = false)
        )
    )

    val RandomNoiseValue = NodeDefinition(
        type = "random_noise",
        category = RANDOM,
        displayName = "Noise Value",
        description = "噪声值",
        parameters = listOf(
            NodeParameter("strength", "Strength", FLOAT, 0.1f, 0f, 1f),
            NodeParameter("frequency", "Frequency", FLOAT, 1f, 0.1f, 10f),
            NodeParameter("seed", "Seed", INTEGER, 0, 0, 999999)
        ),
        outputs = listOf(
            NodePort("value", "Value", FLOAT, isInput = false)
        )
    )

    // ==================== Math Node Definitions ====================
    val MathAdd = NodeDefinition(
        type = "math_add",
        category = MATH,
        displayName = "Add",
        description = "加法",
        inputs = listOf(
            NodePort("a", "A", FLOAT, isInput = true),
            NodePort("b", "B", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathSubtract = NodeDefinition(
        type = "math_subtract",
        category = MATH,
        displayName = "Subtract",
        description = "减法",
        inputs = listOf(
            NodePort("a", "A", FLOAT, isInput = true),
            NodePort("b", "B", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathMultiply = NodeDefinition(
        type = "math_multiply",
        category = MATH,
        displayName = "Multiply",
        description = "乘法",
        inputs = listOf(
            NodePort("a", "A", FLOAT, isInput = true),
            NodePort("b", "B", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathDivide = NodeDefinition(
        type = "math_divide",
        category = MATH,
        displayName = "Divide",
        description = "除法",
        inputs = listOf(
            NodePort("a", "A", FLOAT, isInput = true),
            NodePort("b", "B", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathClamp = NodeDefinition(
        type = "math_clamp",
        category = MATH,
        displayName = "Clamp",
        description = "限制值范围",
        parameters = listOf(
            NodeParameter("min", "Min", FLOAT, 0f, -10000f, 10000f),
            NodeParameter("max", "Max", FLOAT, 100f, -10000f, 10000f)
        ),
        inputs = listOf(
            NodePort("value", "Value", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathMin = NodeDefinition(
        type = "math_min",
        category = MATH,
        displayName = "Min",
        description = "最小值",
        inputs = listOf(
            NodePort("a", "A", FLOAT, isInput = true),
            NodePort("b", "B", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathMax = NodeDefinition(
        type = "math_max",
        category = MATH,
        displayName = "Max",
        description = "最大值",
        inputs = listOf(
            NodePort("a", "A", FLOAT, isInput = true),
            NodePort("b", "B", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathAbs = NodeDefinition(
        type = "math_abs",
        category = MATH,
        displayName = "Abs",
        description = "绝对值",
        inputs = listOf(
            NodePort("value", "Value", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathFloor = NodeDefinition(
        type = "math_floor",
        category = MATH,
        displayName = "Floor",
        description = "向下取整",
        inputs = listOf(
            NodePort("value", "Value", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", INTEGER, isInput = false)
        )
    )

    val MathCeil = NodeDefinition(
        type = "math_ceil",
        category = MATH,
        displayName = "Ceil",
        description = "向上取整",
        inputs = listOf(
            NodePort("value", "Value", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", INTEGER, isInput = false)
        )
    )

    val MathRound = NodeDefinition(
        type = "math_round",
        category = MATH,
        displayName = "Round",
        description = "四舍五入",
        inputs = listOf(
            NodePort("value", "Value", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", INTEGER, isInput = false)
        )
    )

    val MathSin = NodeDefinition(
        type = "math_sin",
        category = MATH,
        displayName = "Sin",
        description = "正弦",
        inputs = listOf(
            NodePort("value", "Value (deg)", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathCos = NodeDefinition(
        type = "math_cos",
        category = MATH,
        displayName = "Cos",
        description = "余弦",
        inputs = listOf(
            NodePort("value", "Value (deg)", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val MathDistance = NodeDefinition(
        type = "math_distance",
        category = MATH,
        displayName = "Distance",
        description = "两点距离",
        inputs = listOf(
            NodePort("x1", "X 1", DOUBLE, isInput = true),
            NodePort("y1", "Y 1", DOUBLE, isInput = true),
            NodePort("z1", "Z 1", DOUBLE, isInput = true),
            NodePort("x2", "X 2", DOUBLE, isInput = true),
            NodePort("y2", "Y 2", DOUBLE, isInput = true),
            NodePort("z2", "Z 2", DOUBLE, isInput = true)
        ),
        outputs = listOf(
            NodePort("distance", "Distance", DOUBLE, isInput = false)
        )
    )

    val MathLerp = NodeDefinition(
        type = "math_lerp",
        category = MATH,
        displayName = "Lerp",
        description = "线性插值",
        parameters = listOf(
            NodeParameter("factor", "Factor", FLOAT, 0.5f, 0f, 1f)
        ),
        inputs = listOf(
            NodePort("a", "A", FLOAT, isInput = true),
            NodePort("b", "B", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    // ==================== Condition Node Definitions ====================
    val ConditionIf = NodeDefinition(
        type = "condition_if",
        category = CONDITION,
        displayName = "If",
        description = "条件判断",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("condition", "Condition", BOOLEAN, isInput = true)
        ),
        outputs = listOf(
            NodePort("true", "True", EXECUTION, isInput = false, isExecution = true),
            NodePort("false", "False", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val ConditionAnd = NodeDefinition(
        type = "condition_and",
        category = CONDITION,
        displayName = "AND",
        description = "逻辑与",
        inputs = listOf(
            NodePort("a", "A", BOOLEAN, isInput = true),
            NodePort("b", "B", BOOLEAN, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val ConditionOr = NodeDefinition(
        type = "condition_or",
        category = CONDITION,
        displayName = "OR",
        description = "逻辑或",
        inputs = listOf(
            NodePort("a", "A", BOOLEAN, isInput = true),
            NodePort("b", "B", BOOLEAN, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val ConditionNot = NodeDefinition(
        type = "condition_not",
        category = CONDITION,
        displayName = "NOT",
        description = "逻辑非",
        inputs = listOf(
            NodePort("value", "Value", BOOLEAN, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val ConditionEqual = NodeDefinition(
        type = "condition_equal",
        category = CONDITION,
        displayName = "Equal",
        description = "等于",
        inputs = listOf(
            NodePort("a", "A", ANY, isInput = true),
            NodePort("b", "B", ANY, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val ConditionNotEqual = NodeDefinition(
        type = "condition_not_equal",
        category = CONDITION,
        displayName = "Not Equal",
        description = "不等于",
        inputs = listOf(
            NodePort("a", "A", ANY, isInput = true),
            NodePort("b", "B", ANY, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val ConditionGreater = NodeDefinition(
        type = "condition_greater",
        category = CONDITION,
        displayName = "Greater",
        description = "大于",
        inputs = listOf(
            NodePort("a", "A", FLOAT, isInput = true),
            NodePort("b", "B", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val ConditionLess = NodeDefinition(
        type = "condition_less",
        category = CONDITION,
        displayName = "Less",
        description = "小于",
        inputs = listOf(
            NodePort("a", "A", FLOAT, isInput = true),
            NodePort("b", "B", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val ConditionBetween = NodeDefinition(
        type = "condition_between",
        category = CONDITION,
        displayName = "Between",
        description = "介于之间",
        parameters = listOf(
            NodeParameter("min", "Min", FLOAT, 0f, -10000f, 10000f),
            NodeParameter("max", "Max", FLOAT, 10f, -10000f, 10000f)
        ),
        inputs = listOf(
            NodePort("value", "Value", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    // ==================== Attack Node Definitions ====================
    val AttackExecute = NodeDefinition(
        type = "attack_execute",
        category = ATTACK,
        displayName = "Attack",
        description = "执行攻击",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val AttackCanAttack = NodeDefinition(
        type = "attack_can_attack",
        category = ATTACK,
        displayName = "Can Attack",
        description = "是否可以攻击",
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val AttackCooldown = NodeDefinition(
        type = "attack_cooldown",
        category = ATTACK,
        displayName = "Attack Cooldown",
        description = "攻击冷却",
        outputs = listOf(
            NodePort("progress", "Progress", FLOAT, isInput = false)
        )
    )

    val AttackCPS = NodeDefinition(
        type = "attack_cps",
        category = ATTACK,
        displayName = "CPS",
        description = "攻击速度",
        parameters = listOf(
            NodeParameter("minCPS", "Min CPS", INTEGER, 5, 1, 20),
            NodeParameter("maxCPS", "Max CPS", INTEGER, 8, 1, 20)
        ),
        outputs = listOf(
            NodePort("cps", "CPS", FLOAT, isInput = false)
        )
    )

    val AttackLeftClick = NodeDefinition(
        type = "attack_left_click",
        category = ATTACK,
        displayName = "Left Click",
        description = "左键攻击",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val AttackRightClick = NodeDefinition(
        type = "attack_right_click",
        category = ATTACK,
        displayName = "Right Click",
        description = "右键操作",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val AttackDelay = NodeDefinition(
        type = "attack_delay",
        category = ATTACK,
        displayName = "Attack Delay",
        description = "攻击延迟",
        parameters = listOf(
            NodeParameter("delayTicks", "Delay Ticks", INTEGER, 2, 0, 20)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val AttackTarget = NodeDefinition(
        type = "attack_target",
        category = ATTACK,
        displayName = "Attack Target",
        description = "攻击指定目标",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val AttackResetTimer = NodeDefinition(
        type = "attack_reset_timer",
        category = ATTACK,
        displayName = "Reset Attack Timer",
        description = "重置攻击计时器",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    // ==================== Block Node Definitions ====================
    val BlockStart = NodeDefinition(
        type = "block_start",
        category = BLOCK,
        displayName = "Start Blocking",
        description = "开始格挡",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val BlockStop = NodeDefinition(
        type = "block_stop",
        category = BLOCK,
        displayName = "Stop Blocking",
        description = "停止格挡",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val BlockIsBlocking = NodeDefinition(
        type = "block_is_blocking",
        category = BLOCK,
        displayName = "Is Blocking",
        description = "是否正在格挡",
        outputs = listOf(
            NodePort("result", "Result", BOOLEAN, isInput = false)
        )
    )

    val BlockState = NodeDefinition(
        type = "block_state",
        category = BLOCK,
        displayName = "Block State",
        description = "格挡状态",
        outputs = listOf(
            NodePort("state", "State", BOOLEAN, isInput = false)
        )
    )

    val BlockDuration = NodeDefinition(
        type = "block_duration",
        category = BLOCK,
        displayName = "Block Duration",
        description = "格挡持续时间",
        parameters = listOf(
            NodeParameter("duration", "Duration (ticks)", INTEGER, 5, 1, 100)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val BlockCondition = NodeDefinition(
        type = "block_condition",
        category = BLOCK,
        displayName = "Block Condition",
        description = "条件格挡",
        parameters = listOf(
            NodeParameter("blockRange", "Block Range", FLOAT, 4.5f, 1f, 10f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("condition", "Condition", BOOLEAN, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    // ==================== Player Node Definitions ====================
    val PlayerPosition = NodeDefinition(
        type = "player_position",
        category = PLAYER,
        displayName = "Player Position",
        description = "玩家位置",
        outputs = listOf(
            NodePort("x", "X", DOUBLE, isInput = false),
            NodePort("y", "Y", DOUBLE, isInput = false),
            NodePort("z", "Z", DOUBLE, isInput = false)
        )
    )

    val PlayerRotation = NodeDefinition(
        type = "player_rotation",
        category = PLAYER,
        displayName = "Player Rotation",
        description = "玩家旋转",
        outputs = listOf(
            NodePort("yaw", "Yaw", FLOAT, isInput = false),
            NodePort("pitch", "Pitch", FLOAT, isInput = false)
        )
    )

    val PlayerMotion = NodeDefinition(
        type = "player_motion",
        category = PLAYER,
        displayName = "Player Motion",
        description = "玩家运动",
        outputs = listOf(
            NodePort("motionX", "Motion X", DOUBLE, isInput = false),
            NodePort("motionY", "Motion Y", DOUBLE, isInput = false),
            NodePort("motionZ", "Motion Z", DOUBLE, isInput = false)
        )
    )

    val PlayerGround = NodeDefinition(
        type = "player_ground",
        category = PLAYER,
        displayName = "Player Ground",
        description = "玩家是否在地面",
        outputs = listOf(
            NodePort("ground", "Ground", BOOLEAN, isInput = false)
        )
    )

    val PlayerSprint = NodeDefinition(
        type = "player_sprint",
        category = PLAYER,
        displayName = "Player Sprint",
        description = "玩家是否在疾跑",
        outputs = listOf(
            NodePort("sprint", "Sprint", BOOLEAN, isInput = false)
        )
    )

    val PlayerHurtTime = NodeDefinition(
        type = "player_hurt_time",
        category = PLAYER,
        displayName = "Player Hurt Time",
        description = "玩家受伤时间",
        outputs = listOf(
            NodePort("hurtTime", "Hurt Time", INTEGER, isInput = false)
        )
    )

    val PlayerMovementDirection = NodeDefinition(
        type = "player_movement_direction",
        category = PLAYER,
        displayName = "Movement Direction",
        description = "玩家移动方向",
        outputs = listOf(
            NodePort("forward", "Forward", FLOAT, isInput = false),
            NodePort("strafe", "Strafe", FLOAT, isInput = false)
        )
    )

    // ==================== Variable Node Definitions ====================
    val VariableSet = NodeDefinition(
        type = "variable_set",
        category = VARIABLE,
        displayName = "Set Variable",
        description = "设置变量",
        parameters = listOf(
            NodeParameter("name", "Name", STRING, defaultValue = "var"),
            NodeParameter("initialValue", "Initial Value", FLOAT, 0f)
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("value", "Value", ANY, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("value", "Value", ANY, isInput = false)
        )
    )

    val VariableGet = NodeDefinition(
        type = "variable_get",
        category = VARIABLE,
        displayName = "Get Variable",
        description = "获取变量",
        parameters = listOf(
            NodeParameter("name", "Name", STRING, defaultValue = "var")
        ),
        outputs = listOf(
            NodePort("value", "Value", ANY, isInput = false)
        )
    )

    val VariableAdd = NodeDefinition(
        type = "variable_add",
        category = VARIABLE,
        displayName = "Add Variable",
        description = "变量加法",
        parameters = listOf(
            NodeParameter("name", "Name", STRING, defaultValue = "var")
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("value", "Value", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val VariableMultiply = NodeDefinition(
        type = "variable_multiply",
        category = VARIABLE,
        displayName = "Multiply Variable",
        description = "变量乘法",
        parameters = listOf(
            NodeParameter("name", "Name", STRING, defaultValue = "var")
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("value", "Value", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true),
            NodePort("result", "Result", FLOAT, isInput = false)
        )
    )

    val VariableReset = NodeDefinition(
        type = "variable_reset",
        category = VARIABLE,
        displayName = "Reset Variable",
        description = "重置变量",
        parameters = listOf(
            NodeParameter("name", "Name", STRING, defaultValue = "var")
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    // ==================== Debug Node Definitions ====================
    val DebugPrint = NodeDefinition(
        type = "debug_print",
        category = DEBUG,
        displayName = "Print",
        description = "打印消息",
        parameters = listOf(
            NodeParameter("message", "Message", STRING, defaultValue = "")
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val DebugLogValue = NodeDefinition(
        type = "debug_log_value",
        category = DEBUG,
        displayName = "Log Value",
        description = "记录数值",
        parameters = listOf(
            NodeParameter("label", "Label", STRING, defaultValue = "Value")
        ),
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("value", "Value", ANY, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val DebugLogRotation = NodeDefinition(
        type = "debug_log_rotation",
        category = DEBUG,
        displayName = "Log Rotation",
        description = "记录旋转值",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("yaw", "Yaw", FLOAT, isInput = true),
            NodePort("pitch", "Pitch", FLOAT, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val DebugLogTarget = NodeDefinition(
        type = "debug_log_target",
        category = DEBUG,
        displayName = "Log Target",
        description = "记录目标信息",
        inputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = true, isExecution = true),
            NodePort("target", "Target", ValueType.TARGET, isInput = true)
        ),
        outputs = listOf(
            NodePort("exec", "执行", EXECUTION, isInput = false, isExecution = true)
        )
    )

    val DebugRuntimeState = NodeDefinition(
        type = "debug_runtime_state",
        category = DEBUG,
        displayName = "Runtime State",
        description = "运行时状态",
        outputs = listOf(
            NodePort("state", "State", STRING, isInput = false)
        )
    )
}