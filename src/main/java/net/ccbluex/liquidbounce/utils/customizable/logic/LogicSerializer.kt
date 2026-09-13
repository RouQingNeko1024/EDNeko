package net.ccbluex.liquidbounce.utils.customizable.logic

import com.google.gson.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.UUID

/**
 * 逻辑序列化器 - 负责保存和加载逻辑图
 */
object LogicSerializer {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LogicGraph::class.java, LogicGraphSerializer())
        .registerTypeAdapter(LogicNode::class.java, LogicNodeSerializer())
        .registerTypeAdapter(LogicConnection::class.java, LogicConnectionSerializer())
        .create()

    /**
     * 保存逻辑图到文件
     */
    fun save(graph: LogicGraph, file: File): Boolean {
        return try {
            file.parentFile?.mkdirs()
            FileWriter(file).use { writer ->
                gson.toJson(graph, writer)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从文件加载逻辑图
     */
    fun load(file: File): LogicGraph? {
        return try {
            if (!file.exists()) return null
            FileReader(file).use { reader ->
                gson.fromJson(reader, LogicGraph::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 保存逻辑图为JSON字符串
     */
    fun toJson(graph: LogicGraph): String {
        return gson.toJson(graph)
    }

    /**
     * 从JSON字符串加载逻辑图
     */
    fun fromJson(json: String): LogicGraph? {
        return try {
            gson.fromJson(json, LogicGraph::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * LogicGraph JSON 序列化器
     */
    private class LogicGraphSerializer : JsonSerializer<LogicGraph>, JsonDeserializer<LogicGraph> {
        override fun serialize(src: LogicGraph, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext): JsonElement {
            val obj = JsonObject()
            obj.addProperty("id", src.id)
            obj.addProperty("name", src.name)
            obj.addProperty("version", src.version)
            obj.addProperty("formatVersion", 1)

            val nodesArray = JsonArray()
            src.nodes.values.forEach { node ->
                nodesArray.add(context.serialize(node, LogicNode::class.java))
            }
            obj.add("nodes", nodesArray)

            val connectionsArray = JsonArray()
            src.connections.values.forEach { conn ->
                connectionsArray.add(context.serialize(conn, LogicConnection::class.java))
            }
            obj.add("connections", connectionsArray)

            val variablesArray = JsonArray()
            src.variables.forEach { (name, def) ->
                val varObj = JsonObject()
                varObj.addProperty("name", name)
                varObj.addProperty("type", def.type.name)
                varObj.add("defaultValue", context.serialize(def.defaultValue))
                varObj.addProperty("scope", def.scope.name)
                variablesArray.add(varObj)
            }
            obj.add("variables", variablesArray)

            return obj
        }

        override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): LogicGraph {
            val obj = json.asJsonObject
            val graph = LogicGraph(
                id = obj.get("id")?.asString ?: UUID.randomUUID().toString(),
                name = obj.get("name")?.asString ?: "Untitled",
                version = obj.get("version")?.asInt ?: 1
            )

            // 加载节点
            obj.getAsJsonArray("nodes")?.forEach { element ->
                val node = context.deserialize<LogicNode>(element, LogicNode::class.java)
                graph.addNode(node)
            }

            // 加载连接
            obj.getAsJsonArray("connections")?.forEach { element ->
                val conn = context.deserialize<LogicConnection>(element, LogicConnection::class.java)
                graph.addConnection(conn)
            }

            // 加载变量
            obj.getAsJsonArray("variables")?.forEach { element ->
                val varObj = element.asJsonObject
                val name = varObj.get("name").asString
                val type = try {
                    ValueType.valueOf(varObj.get("type").asString)
                } catch (e: Exception) {
                    ValueType.FLOAT
                }
                val scope = try {
                    LogicGraph.VariableScope.valueOf(varObj.get("scope").asString)
                } catch (e: Exception) {
                    LogicGraph.VariableScope.GLOBAL
                }
                graph.variables[name] = LogicGraph.VariableDefinition(name, type, null, scope)
            }

            return graph
        }
    }

    /**
     * LogicNode JSON 序列化器
     */
    private class LogicNodeSerializer : JsonSerializer<LogicNode>, JsonDeserializer<LogicNode> {
        override fun serialize(src: LogicNode, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext): JsonElement {
            val obj = JsonObject()
            obj.addProperty("id", src.id)
            obj.addProperty("type", src.type)
            obj.addProperty("x", src.x)
            obj.addProperty("y", src.y)

            if (src.parameterValues.isNotEmpty()) {
                val paramsObj = JsonObject()
                src.parameterValues.forEach { (key, value) ->
                    paramsObj.add(key, context.serialize(value))
                }
                obj.add("parameters", paramsObj)
            }

            if (src.variableBindings.isNotEmpty()) {
                val bindingsObj = JsonObject()
                src.variableBindings.forEach { (key, value) ->
                    bindingsObj.addProperty(key, value)
                }
                obj.add("variableBindings", bindingsObj)
            }

            return obj
        }

        override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): LogicNode {
            val obj = json.asJsonObject
            val type = obj.get("type").asString
            val definition = NodeRegistry.getDefinition(type)
                ?: throw JsonParseException("Unknown node type: $type")

            val node = LogicNode(
                id = obj.get("id").asString,
                type = type,
                definition = definition,
                x = obj.get("x")?.asDouble ?: 0.0,
                y = obj.get("y")?.asDouble ?: 0.0
            )

            // 加载参数
            obj.getAsJsonObject("parameters")?.entrySet()?.forEach { entry ->
                node.parameterValues[entry.key] = context.deserialize<Any>(entry.value, Any::class.java)
            }

            // 加载变量绑定
            obj.getAsJsonObject("variableBindings")?.entrySet()?.forEach { entry ->
                node.variableBindings[entry.key] = entry.value.asString
            }

            return node
        }
    }

    /**
     * LogicConnection JSON 序列化器
     */
    private class LogicConnectionSerializer : JsonSerializer<LogicConnection>, JsonDeserializer<LogicConnection> {
        override fun serialize(src: LogicConnection, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext): JsonElement {
            val obj = JsonObject()
            obj.addProperty("id", src.id)
            obj.addProperty("sourceNodeId", src.sourceNodeId)
            obj.addProperty("sourcePortId", src.sourcePortId)
            obj.addProperty("targetNodeId", src.targetNodeId)
            obj.addProperty("targetPortId", src.targetPortId)
            return obj
        }

        override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): LogicConnection {
            val obj = json.asJsonObject
            return LogicConnection(
                id = obj.get("id").asString,
                sourceNodeId = obj.get("sourceNodeId").asString,
                sourcePortId = obj.get("sourcePortId").asString,
                targetNodeId = obj.get("targetNodeId").asString,
                targetPortId = obj.get("targetPortId").asString
            )
        }
    }
}