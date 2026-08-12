/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * skid gold bounce
 * https://github.com/bzym2/GoldBounce/
 */     
package net.ccbluex.liquidbounce.features.module.modules.client

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.event.EntityKilledEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.FileUtils
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.minecraft.entity.player.EntityPlayer
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset

object Insult : Module("Insult", Category.CLIENT, spacedName = "LLL kid") {

    private val mode by choices("Mode", arrayOf("Clear", "WithWords", "RawWords"), "RawWords")
    private val waterMark by boolean("WaterMark", true)

    private val insultFile = File(FileManager.dir, "insult.json")
    private val insultWords = ArrayList<String>()

    init {
        loadFile()
    }

    private fun loadFile() {
        try {
            if (!insultFile.exists()) {
                FileUtils.unpackFile(insultFile, "assets/minecraft/edneko/insult.json")
            }

            val content = String(java.nio.file.Files.readAllBytes(insultFile.toPath()), Charset.forName("UTF-8"))
            val json = JsonParser().parse(content)

            insultWords.clear()

            if (json.isJsonArray) {
                json.asJsonArray.forEach { element ->
                    insultWords.add(element.asString)
                }
            } else {
                convertToJson(insultFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            convertToJson(insultFile)
        }
    }

    private fun convertToJson(file: File) {
        try {
            insultWords.clear()
            java.nio.file.Files.readAllLines(file.toPath(), Charset.forName("UTF-8")).forEach { line ->
                if (line.trim().isNotEmpty()) {
                    insultWords.add(line)
                }
            }

            val jsonArray = JsonArray()
            insultWords.forEach { word ->
                jsonArray.add(JsonPrimitive(word))
            }

            val fileWriter = FileWriter(file)
            FileManager.PRETTY_GSON.toJson(jsonArray, fileWriter)
            fileWriter.flush()
            fileWriter.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRandomOne(): String {
        if (insultWords.isEmpty()) return "L"
        val index = nextInt(0, insultWords.size - 1)
        return insultWords[index]
    }

    val onKilled = handler<EntityKilledEvent> { event ->
        val targetEntity = event.targetEntity
        if (targetEntity !is EntityPlayer) return@handler

        val name = targetEntity.name
        val message = when (mode.lowercase()) {
            "clear" -> "L $name"
            "withwords" -> "L $name ${getRandomOne()}"
            "rawwords" -> getRandomOne()
            else -> "L $name"
        }

        sendInsultWords(message, name)
    }

    private fun sendInsultWords(msg: String, name: String) {
        var message = msg.replace("%name%", name)
        if (waterMark) {
            message = "AirC1ient > $message"
        }

        mc.thePlayer?.sendChatMessage(message)
    }

    override val tag: String
        get() = mode
}
