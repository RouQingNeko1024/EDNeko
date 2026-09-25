/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.LiquidBounce.commandManager
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.cape.CapeService
import net.ccbluex.liquidbounce.features.special.ClientFixes
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration
import net.ccbluex.liquidbounce.ui.client.GuiMainMenu
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening.Companion.apiKey
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.Targets
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.io.readJson
import java.io.*

class ValuesConfig(file: File) : FileConfig(file) {

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        val json = file.readJson() as? JsonObject ?: return

        val prevVersion = json["ClientVersion"]?.asString ?: "unknown"
        // Compare the versions
        if (prevVersion != LiquidBounce.clientVersionText) {
            // Run backup
            FileManager.backupAllConfigs(prevVersion, LiquidBounce.clientVersionText)
        }

        for ((key, value) in json.entrySet()) {
            when {
                key.equals("CommandPrefix", true) -> {
                    commandManager.prefix = value.asString
                }

                key.equals(Targets.name, true) -> {
                    Targets.fromJson(value)
                }

                key.equals(ClientFixes.name, true) -> {
                    ClientFixes.fromJson(value)
                }

                key.equals("thealtening", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("API-Key")) apiKey = jsonValue["API-Key"].asString
                }

                key.equals("DonatorCape", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("TransferCode")) {
                        CapeService.knownToken = jsonValue["TransferCode"].asString
                    }
                }

                key.equals(ClientConfiguration.name, true) -> {
                    ClientConfiguration.fromJson(value)
                }

                // Deprecated
                // Compatibility with old versions
                key.equals("background", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("Enabled")) ClientConfiguration.customBackground = jsonValue["Enabled"].asBoolean
                    if (jsonValue.has("Particles")) ClientConfiguration.particles = jsonValue["Particles"].asBoolean
                }

                key.equals("popup", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("lastWarningTime")) GuiMainMenu.lastWarningTime = jsonValue["lastWarningTime"].asLong
                }

                else -> {
                    val module = moduleManager[key] ?: continue

                    val jsonModule = value as JsonObject
                    for (moduleValue in module.values) {
                        val element = jsonModule[moduleValue.name]
                        if (element != null) moduleValue.fromJson(element)
                    }
                }
            }
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    override fun saveConfig() {
        println("[ValuesConfig] saveConfig() called for file: ${file.absolutePath}")

        val jsonObject = JsonObject()

        jsonObject.addProperty("CommandPrefix", commandManager.prefix)
        jsonObject.addProperty("ClientVersion", LiquidBounce.clientVersionText)

        try {
            jsonObject.add(Targets.name, Targets.toJson())
        } catch (e: Exception) {
            println("[ValuesConfig] FAILED to serialize Targets: ${e.message}")
            LOGGER.error("[ValuesConfig] Failed to serialize Targets", e)
        }

        try {
            jsonObject.add(ClientFixes.name, ClientFixes.toJson())
        } catch (e: Exception) {
            println("[ValuesConfig] FAILED to serialize ClientFixes: ${e.message}")
            LOGGER.error("[ValuesConfig] Failed to serialize ClientFixes", e)
        }

        val theAlteningObject = JsonObject()
        theAlteningObject.addProperty("API-Key", apiKey)
        jsonObject.add("thealtening", theAlteningObject)

        val capeObject = JsonObject()
        capeObject.addProperty("TransferCode", CapeService.knownToken)
        jsonObject.add("DonatorCape", capeObject)

        try {
            jsonObject.add(ClientConfiguration.name, ClientConfiguration.toJson())
        } catch (e: Exception) {
            println("[ValuesConfig] FAILED to serialize ClientConfiguration: ${e.message}")
            LOGGER.error("[ValuesConfig] Failed to serialize ClientConfiguration", e)
        }

        // Serialize all modules and their values
        var moduleCount = 0
        var valueCount = 0
        var errorCount = 0

        for (module in moduleManager) {
            if (module.values.isEmpty()) continue

            try {
                val jsonModule = JsonObject()
                for (value in module.values) {
                    try {
                        val jsonElement: JsonElement? = value.toJson()
                        if (jsonElement != null) {
                            jsonModule.add(value.name, jsonElement)
                            valueCount++
                        }
                    } catch (e: Exception) {
                        errorCount++
                        println("[ValuesConfig] FAILED to serialize value '${value.name}' in module '${module.name}': ${e.message}")
                        LOGGER.error("[ValuesConfig] Failed to serialize value '${value.name}' in module '${module.name}'", e)
                    }
                }
                if (jsonModule.entrySet().isNotEmpty()) {
                    jsonObject.add(module.name, jsonModule)
                    moduleCount++
                }
            } catch (e: Exception) {
                errorCount++
                println("[ValuesConfig] FAILED to serialize module '${module.name}': ${e.message}")
                LOGGER.error("[ValuesConfig] Failed to serialize module '${module.name}'", e)
            }
        }

        try {
            val popupData = JsonObject()
            GuiMainMenu.lastWarningTime?.let { popupData.addProperty("lastWarningTime", it) }
            jsonObject.add("popup", popupData)
        } catch (e: Exception) {
            println("[ValuesConfig] FAILED to serialize popup: ${e.message}")
            LOGGER.error("[ValuesConfig] Failed to serialize popup", e)
        }

        // Convert to JSON string
        val jsonString = PRETTY_GSON.toJson(jsonObject)

        println("[ValuesConfig] Serialized: $moduleCount modules, $valueCount values, $errorCount errors, ${jsonString.length} chars")

        // Write to file
        file.writeText(jsonString)

        println("[ValuesConfig] Successfully saved ${file.name} (${file.length()} bytes on disk)")
    }
}