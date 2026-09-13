/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.event.async.loopSequence
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.randomString

object Spammer : Module("Spammer", Category.MISC, subjective = true) {

    private val delay by intRange("Delay", 500..1000, 0..5000)

    private val mode by choices("Mode", arrayOf("Lines", "Custom", "Joke", "Client", "Hello"), "Custom")

    private val custom by boolean("Custom", false) { mode == "Custom" || mode == "Lines" }

    private val message by text("Message", "$CLIENT_NAME Client | %s | j01n at 722573066") { mode == "Custom" }

    private val lines by text("Lines", "$CLIENT_NAME Client,$CLIENT_NAME on top,GG") { mode == "Lines" }
    private val separator by text("Separator", ",") { mode == "Lines" }
    private val randomSuffix by boolean("RandomSuffix", true) { mode == "Lines" }
    private val randomLength by intRange("RandomLength", 5..11, 0..30) { mode == "Custom" && !custom }

    private var i = 0

    private val jokes = arrayOf(
        "%p, why are cheaters' diamonds always white? Because they bypass the color check!",
        "If Minecraft was real, %p would have been banned for speed hacks long ago.",
        "%p's PvP skills remind me of my first time playing - absolutely terrible.",
        "%p is currently looking for the Ctrl key... on their mouse :>",
        "Warning: Testing %p's IQ took less than a second. Please restart.",
        "%p's build made me do a double take - horrific indeed.",
        "If cheating is a resource, %p is the server crash.",
        "%p's PvP technique reminds me of a bot - slow and predictable.",
        "Breaking news: %p just tried to use wood sword in a diamond lobby.",
        "%p's survival skills make me question how they're still alive.",
        "Science fact: Watching %p gameplay will cause our brain cells to die.",
        "%p just attempted to use killaura without turning it on - classic!",
        "If %p's Minecraft skills were currency, they'd be bankrupt.",
        "%p's build gave me a heart attack: why though?",
        "Warning: %p is currently thinking - this might take a while.",
        "%p's logic: never read the manual and still complain about it.",
        "If %p's gaming skills were a superpower, it'd be the ability to lose.",
        "%p just tried to bridge with dirt blocks - brave!",
        "Fun fact: %p's death count is higher than this server's player count.",
        "%p's career skills make me cry - literally8.",
        "If %p's brainpower was light, they'd live in total darkness.",
        "%p just tried to eat a golden apple in creative - natural selection.",
        "Urgent report: %p is currently thinking - abort immediately.",
        "%p's risk technique: walking into every trap without checking.",
        "If %p was a Minecraft mob, they'd be the lost kind.",
        "%p's building philosophy: if it stands, it's a success.",
        "Why does %p's furnace never smelt? Because the client they use makes it think it's lagging!",
        "%p's brain is so secure, even high-level hackers thought it was a honeypot.",
        "Heard %p is mining with a wooden pickaxe? Turns out they>they haven't enabled 'speed mine' yet.",
        "%p's build is called 'absolutely gorgeous', but everyone else chose to disagree.",
        "Final boss: %p player's speed, still hasn't broken their own record!",
        "%p's killaura technique is so bad, even the anti-cheat felt bad banning them.",
        "%p's skywars strat: bridge into the void and hope for the best.",
        "%p's crosshair is so oversized, even snipers are jealous.",
        "%p's double jump success rate: 0%--and they blame the ping.",
        "%p's parkour record: fell into lava on the first jump.",
        "%p's map art is so abstract, even modern artists are confused.",
        "%p's click speed is so impressive--1 CPS per minute!",
        "%p's bedwars rush: died before even reaching the bridge.",
        "%p's wool placement accuracy is so low, even the sheep are disappointed.",
        "%p, the final boss is actually your own speed test.",
        "%p's texture pack is so bright, even the sun wears sunglasses.",
        "%p's sensitivity is so high, even a sneeze causes a 360.",
        "%p's sprint key is broken, so they just walk everywhere sadly.",
        "%p, the final boss is actually just your own speed test."
    )

    private val clientMessages = arrayOf(
        "$CLIENT_NAME Client - The Best Free Client!",
        "$CLIENT_NAME on top! Get rekt skids!",
        "Using $CLIENT_NAME Client - No one can beat me!",
        "$CLIENT_NAME Client | liquidbounce.net | Best Anti-Cheat Bypass",
        "Get $CLIENT_NAME Client at liquidbounce.net!",
        "$CLIENT_NAME Client - Powered by LiquidBounce",
        "Why use other clients when you have $CLIENT_NAME?",
        "$CLIENT_NAME Client - Free and Open Source!",
        "$CLIENT_NAME > All other clients",
        "Join the $CLIENT_NAME revolution today!",
        "$CLIENT_NAME Client - Making Minecraft Great Again",
        "Switch to $CLIENT_NAME - You won't regret it!",
        "$CLIENT_NAME Client - The Ultimate PvP Experience",
        "With $CLIENT_NAME, winning is guaranteed!",
        "$CLIENT_NAME Client - Bypassing since 20XX"
    )

    val onUpdate = loopSequence {
        var finalMsg = when (mode) {
            "Joke" -> {
                val msg = jokes[i % jokes.size]
                i++
                replace(msg)
            }
            "Client" -> {
                val msg = clientMessages[i % clientMessages.size]
                i++
                replace(msg)
            }
            "Hello" -> replace("%p Hello!")
            "Custom" -> {
                if (custom) replace(message)
                else message + " [" + randomString(nextInt(randomLength.first, randomLength.last)) + "]"
            }
            else -> {
                val lineList = lines.split(separator).map { it.trim() }.filter { it.isNotEmpty() }
                val selectedLine = lineList.randomOrNull() ?: lines
                if (randomSuffix) selectedLine + " >" + randomString(nextInt(5, 11)) + "<"
                else selectedLine
            }
        }

        mc.thePlayer?.sendChatMessage(finalMsg)

        delay(delay.random().toLong())
    }

    private fun replace(text: String): String {
        var replacedStr = text

        replaceMap.forEach { (key, valueFunc) ->
            replacedStr = replacedStr.replace(key, valueFunc)
        }

        return replacedStr
    }

    private inline fun String.replace(oldValue: String, newValueProvider: () -> Any): String {
        var index = 0
        val newString = StringBuilder(this)
        while (true) {
            index = newString.indexOf(oldValue, startIndex = index)
            if (index == -1) {
                break
            }

            // You have to replace them one by one, otherwise all parameters like %s would be set to the same random string.
            val newValue = newValueProvider().toString()
            newString.replace(index, index + oldValue.length, newValue)

            index += newValue.length
        }
        return newString.toString()
    }

    private fun randomPlayer() =
        mc.netHandler.playerInfoMap
            .map { playerInfo -> playerInfo.gameProfile.name }
            .filter { name -> name != mc.thePlayer.name }
            .randomOrNull() ?: "none"

    private val replaceMap = mapOf(
        "%f" to { nextFloat().toString() },
        "%i" to { nextInt(0, 10000).toString() },
        "%ss" to { randomString(nextInt(1, 6)) },
        "%s" to { randomString(nextInt(1, 10)) },
        "%ls" to { randomString(nextInt(1, 17)) },
        "%p" to { randomPlayer() }
    )
}