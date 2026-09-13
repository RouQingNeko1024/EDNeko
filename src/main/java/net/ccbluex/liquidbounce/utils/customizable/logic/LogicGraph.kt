package net.ccbluex.liquidbounce.utils.customizable.logic

import java.util.UUID

/**
 * 完整的逻辑图
 */
class LogicGraph(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled",
    val nodes: MutableMap<String, LogicNode> = LinkedHashMap(),
    val connections: MutableMap<String, LogicConnection> = LinkedHashMap(),
    val variables: MutableMap<String, VariableDefinition> = mutableMapOf(),
    val version: Int = 1
) {
    /**
     * 变量定义
     */
    data class VariableDefinition(
        val name: String,
        val type: ValueType,
        val defaultValue: Any? = null,
        val scope: VariableScope = VariableScope.GLOBAL
    )

    enum class VariableScope {
        TICK,     // 每Tick重置
        TARGET,   // 每目标重置
        GLOBAL    // 全局持久
    }

    /**
     * 添加节点
     */
    fun addNode(node: LogicNode) {
        nodes[node.id] = node
    }

    /**
     * 移除节点及其所有连接
     */
    fun removeNode(nodeId: String) {
        nodes.remove(nodeId)
        connections.values.removeAll {
            it.sourceNodeId == nodeId || it.targetNodeId == nodeId
        }
    }

    /**
     * 获取节点
     */
    fun getNode(nodeId: String): LogicNode? = nodes[nodeId]

    /**
     * 添加连接
     */
    fun addConnection(connection: LogicConnection): Boolean {
        if (!connection.validate(this)) return false
        connections[connection.id] = connection
        return true
    }

    /**
     * 移除连接
     */
    fun removeConnection(connectionId: String) {
        connections.remove(connectionId)
    }

    /**
     * 获取节点的所有输入连接
     */
    fun getInputConnections(nodeId: String): List<LogicConnection> {
        return connections.values.filter { it.targetNodeId == nodeId }
    }

    /**
     * 获取节点的所有输出连接
     */
    fun getOutputConnections(nodeId: String): List<LogicConnection> {
        return connections.values.filter { it.sourceNodeId == nodeId }
    }

    /**
     * 获取从指定端口输出的连接
     */
    fun getOutputConnections(nodeId: String, portId: String): List<LogicConnection> {
        return connections.values.filter {
            it.sourceNodeId == nodeId && it.sourcePortId == portId
        }
    }

    /**
     * 找到所有起始节点（没有执行输入的节点，通常是事件节点）
     */
    fun findEntryNodes(): List<LogicNode> {
        return nodes.values.filter { node ->
            node.definition.inputs.none { it.isExecution } ||
            connections.values.none { it.targetNodeId == node.id && 
                it.targetPortId == node.getExecutionInput()?.id }
        }
    }

    /**
     * 深拷贝
     */
    fun copy(newId: String = UUID.randomUUID().toString()): LogicGraph {
        val newNodeMap = mutableMapOf<String, String>() // oldId -> newId
        val newGraph = LogicGraph(id = newId, name = name, version = version)
        
        nodes.values.forEach { node ->
            val newId = UUID.randomUUID().toString()
            newNodeMap[node.id] = newId
            newGraph.addNode(node.copyWithNewId(newId))
        }
        
        connections.values.forEach { conn ->
            val newConn = conn.copy(
                id = UUID.randomUUID().toString(),
                sourceNodeId = newNodeMap[conn.sourceNodeId] ?: conn.sourceNodeId,
                targetNodeId = newNodeMap[conn.targetNodeId] ?: conn.targetNodeId
            )
            newGraph.addConnection(newConn)
        }
        
        variables.forEach { (name, def) ->
            newGraph.variables[name] = def
        }
        
        return newGraph
    }
}