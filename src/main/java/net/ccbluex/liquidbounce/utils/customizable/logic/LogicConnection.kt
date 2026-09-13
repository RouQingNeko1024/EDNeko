package net.ccbluex.liquidbounce.utils.customizable.logic

/**
 * 节点之间的连接
 */
data class LogicConnection(
    val id: String,
    val sourceNodeId: String,      // 源节点ID
    val sourcePortId: String,      // 源端口ID
    val targetNodeId: String,      // 目标节点ID
    val targetPortId: String       // 目标端口ID
) {
    /**
     * 验证连接类型是否匹配
     */
    fun validate(graph: LogicGraph): Boolean {
        val sourceNode = graph.getNode(sourceNodeId) ?: return false
        val targetNode = graph.getNode(targetNodeId) ?: return false

        val sourcePort = sourceNode.getOutputPort(sourcePortId) ?: return false
        val targetPort = targetNode.getInputPort(targetPortId) ?: return false

        // 执行端口只能连接执行端口
        if (sourcePort.isExecution != targetPort.isExecution) return false

        // 类型兼容性检查
        if (sourcePort.type != ValueType.ANY && targetPort.type != ValueType.ANY &&
            sourcePort.type != targetPort.type) return false

        return true
    }
}