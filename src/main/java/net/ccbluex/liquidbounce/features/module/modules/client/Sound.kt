/*skid gold bounce
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 * https://github.com/the-OmegaLabs/GoldBounce
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.event.EntityKilledEvent
import net.ccbluex.liquidbounce.event.StartupEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.getMP3S
import net.ccbluex.liquidbounce.utils.getWAVS
import net.ccbluex.liquidbounce.utils.playMP3
import net.ccbluex.liquidbounce.utils.playWavSound

object Sound : Module("Sound", Category.CLIENT, canBeEnabled = false) {

    val enableSounds by choices(
        "Enable",
        getMP3S("assets/minecraft/edneko/sounds/Enable").toTypedArray().takeIf { it.isNotEmpty() } 
            ?: arrayOf("None"),
        getMP3S("assets/minecraft/edneko/sounds/Enable").firstOrNull() ?: "None"
    )

    val disableSounds by choices(
        "Disable",
        getMP3S("assets/minecraft/edneko/sounds/Disable").toTypedArray().takeIf { it.isNotEmpty() }
            ?: arrayOf("None"),
        getMP3S("assets/minecraft/edneko/sounds/Disable").firstOrNull() ?: "None"
    )

    private const val STARTUP_SOUND = "Win11"

    val killSoundEnabled by boolean("KillSound", true)
    
    val killCooldown by int("KillCooldown", 1, 0..60) { killSoundEnabled }
    
    private var lastKillSoundTime = 0L
    
    val killSounds by choices(
        "Kill",
        (getWAVS("assets/minecraft/edneko/sounds/Kill") + getMP3S("assets/minecraft/edneko/sounds/Kill")).toTypedArray().takeIf { it.isNotEmpty() }
            ?: arrayOf("None"),
        (getWAVS("assets/minecraft/edneko/sounds/Kill") + getMP3S("assets/minecraft/edneko/sounds/Kill")).firstOrNull() ?: "None"
    ) { killSoundEnabled }

    fun playEnableSound() {
        if (enableSounds == "None") return
        playMP3("edneko/sounds/Enable/${enableSounds}.mp3")
    }

    fun playDisableSound() {
        if (disableSounds == "None") return
        playMP3("edneko/sounds/Disable/${disableSounds}.mp3")
    }

    fun playToggleSound(enabled: Boolean) {
        if (enabled) {
            playEnableSound()
        } else {
            playDisableSound()
        }
    }

    fun playStartupSound() {
        playMP3("edneko/sounds/Startup/${STARTUP_SOUND}.mp3")
    }

    fun playKillSound() {
        if (!killSoundEnabled) return
        if (killSounds == "None") return
        
        val currentTime = System.currentTimeMillis()
        if (killCooldown > 0 && currentTime - lastKillSoundTime < killCooldown * 1000L) {
            return
        }
        lastKillSoundTime = currentTime
        
        val wavPath = "edneko/sounds/Kill/${killSounds}.wav"
        val mp3Path = "edneko/sounds/Kill/${killSounds}.mp3"
        
        val wavExists = javaClass.getResourceAsStream("/assets/minecraft/$wavPath") != null
        val mp3Exists = javaClass.getResourceAsStream("/assets/minecraft/$mp3Path") != null
        
        when {
            wavExists -> playWavSound(wavPath)
            mp3Exists -> playMP3(mp3Path)
        }
    }

    val onStartup = handler<StartupEvent>(always = true) {
        playStartupSound()
    }

    val onKilled = handler<EntityKilledEvent>(always = true) {
        playKillSound()
    }
}