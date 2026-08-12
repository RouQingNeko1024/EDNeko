package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import net.ccbluex.liquidbounce.ui.client.clickgui.opai.component.OpaiPanel
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiManagers.ConfigManager
import net.ccbluex.liquidbounce.ui.client.clickgui.opai.OpaiCompat.Constants
import com.google.gson.*
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.List

object OpaiLayoutState {

    private const val FILE_NAME = "opai-layout.json"
    private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun load(panels: kotlin.collections.List<OpaiPanel>) {
        val file = getFile()
        if (!Files.exists(file)) return
        try {
            val parsed = JsonParser().parse(String(Files.readAllBytes(file), StandardCharsets.UTF_8))
            if (parsed == null || !parsed.isJsonObject) return
            val root = parsed.asJsonObject
            val panelStates = if (root.has("panels") && root.get("panels").isJsonObject) root.getAsJsonObject("panels") else JsonObject()
            for (panel in panels) {
                val element = panelStates.get(panel.getId())
                if (element == null || !element.isJsonObject) continue
                val `object` = element.asJsonObject
                val x = readFloat(`object`, "x", panel.getX())
                val y = readFloat(`object`, "y", panel.getY())
                panel.setPosition(x, y)
                if ("main" != panel.getId()) {
                    panel.setVisible(readBoolean(`object`, "visible", panel.isVisible()))
                }
                panel.setOpened(readBoolean(`object`, "opened", panel.isOpened()))
            }
        } catch (e: Exception) {
            Constants.LOGGER.error("读取 Opai GUI 布局失败", e)
        }
    }

    fun save(panels: kotlin.collections.List<OpaiPanel>) {
        val root = JsonObject()
        root.addProperty("version", 1)
        val panelStates = JsonObject()
        for (panel in panels) {
            val `object` = JsonObject()
            `object`.addProperty("x", panel.getX())
            `object`.addProperty("y", panel.getY())
            `object`.addProperty("opened", panel.isOpened())
            `object`.addProperty("visible", panel.isVisible())
            panelStates.add(panel.getId(), `object`)
        }
        root.add("panels", panelStates)

        val file = getFile()
        try {
            Files.createDirectories(file.parent)
            Files.write(file, GSON.toJson(root).toByteArray(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)
        } catch (e: IOException) {
            Constants.LOGGER.error("保存 Opai GUI 布局失败", e)
        }
    }

    private fun getFile(): Path {
        return ConfigManager.INSTANCE.getConfigDir().resolve(FILE_NAME)
    }

    private fun readFloat(`object`: JsonObject, key: String, fallback: Float): Float {
        val value = `object`.get(key)
        if (value == null || !value.isJsonPrimitive) return fallback
        return try {
            value.asFloat
        } catch (ignored: Exception) {
            fallback
        }
    }

    private fun readBoolean(`object`: JsonObject, key: String, fallback: Boolean): Boolean {
        val value = `object`.get(key)
        if (value == null || !value.isJsonPrimitive) return fallback
        return try {
            value.asBoolean
        } catch (ignored: Exception) {
            fallback
        }
    }

}
