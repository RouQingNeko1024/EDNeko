package net.ccbluex.liquidbounce.utils.customizable.logic

/**
 * 逻辑图中的单个节点实例
 */
data class LogicNode(
    val id: String,                          // 唯一ID
    val type: String,                        // 节点类型
    val definition: NodeDefinition,          // 节点定义
    var x: Double = 0.0,                     // Canvas X 位置
    var y: Double = 0.0,                     // Canvas Y 位置
    val parameterValues: MutableMap<String, Any?> = mutableMapOf(),  // 参数值
    val variableBindings: MutableMap<String, String> = mutableMapOf() // 变量绑定
) {
    /**
     * 获取参数值，如果未设置则使用默认值
     */
    fun getParameter(id: String): Any? {
        return parameterValues[id] ?: definition.parameters.find { it.id == id }?.defaultValue
    }

    /**
     * 设置参数值
     */
    fun setParameter(id: String, value: Any?) {
        parameterValues[id] = value
    }

    /**
     * 获取输入端口
     */
    fun getInputPort(portId: String): NodePort? = definition.inputs.find { it.id == portId }

    /**
     * 获取输出端口
     */
    fun getOutputPort(portId: String): NodePort? = definition.outputs.find { it.id == portId }

    /**
     * 获取执行输入端口（第一个执行类型输入）
     */
    fun getExecutionInput(): NodePort? = definition.inputs.find { it.isExecution }

    /**
     * 获取执行输出端口（第一个执行类型输出）
     */
    fun getExecutionOutput(): NodePort? = definition.outputs.find { it.isExecution }

    /**
     * 深拷贝
     */
    fun copyWithNewId(newId: String): LogicNode {
        return copy(
            id = newId,
            parameterValues = HashMap(parameterValues),
            variableBindings = HashMap(variableBindings)
        )
    }
}