package net.ccbluex.liquidbounce.utils.customizable.logic

/**
 * 节点类型分类
 */
enum class NodeCategory(val displayName: String) {
    EVENT("Event"),
    TARGET("Target"),
    ROTATION("Rotation"),
    BEHAVIOR("Behavior"),
    RANDOM("Random"),
    MATH("Math"),
    CONDITION("Condition"),
    ATTACK("Attack"),
    BLOCK("Block"),
    PLAYER("Player"),
    VARIABLE("Variable"),
    DEBUG("Debug")
}

/**
 * 数据值类型
 */
enum class ValueType {
    EXECUTION,      // 执行流
    BOOLEAN,        // 布尔
    INTEGER,        // 整数
    FLOAT,          // 浮点
    DOUBLE,         // 双精度
    STRING,         // 字符串
    TARGET,         // 目标实体
    ROTATION,       // 旋转
    VECTOR,         // 向量
    ANY             // 任意
}

/**
 * 节点端口定义
 */
data class NodePort(
    val id: String,
    val name: String,
    val type: ValueType,
    val isInput: Boolean,     // true=输入, false=输出
    val isExecution: Boolean = false  // 是否为执行流端口
)

/**
 * 节点定义描述
 */
data class NodeDefinition(
    val type: String,                    // 唯一类型标识
    val category: NodeCategory,          // 分类
    val displayName: String,             // 显示名称
    val description: String = "",        // 描述
    val inputs: List<NodePort> = emptyList(),      // 输入端口
    val outputs: List<NodePort> = emptyList(),     // 输出端口
    val parameters: List<NodeParameter> = emptyList(), // 可编辑参数
    val minInputs: Int = 0,
    val maxInputs: Int = 1,
    val minOutputs: Int = 0,
    val maxOutputs: Int = 1,
    val color: Int = 0xFF888888.toInt()   // 节点颜色
)

/**
 * 节点参数定义
 */
data class NodeParameter(
    val id: String,
    val name: String,
    val type: ValueType,
    val defaultValue: Any? = null,
    val minValue: Any? = null,
    val maxValue: Any? = null,
    val options: List<String>? = null,    // 选择类型参数的可选项
    val description: String = ""
)