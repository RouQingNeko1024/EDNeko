package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.features.module.Module

object OpaiManagers {

    object ConfigManager {
        val INSTANCE = ConfigManager
        fun getConfigDir(): java.nio.file.Path = FileManager.settingsDir.toPath()
        fun saveNow() { FileManager.saveAllConfigs() }
        fun listConfigs(): List<String> = emptyList()
        fun getActiveConfigName(): String = "default"
        fun saveAsConfig(name: String): String = name
        fun reloadOrThrow() {}
        fun exportActiveConfigToZip(name: String): java.nio.file.Path? = null
        fun importConfigFromZip(name: String): String = name
        fun openConfigFolder(): String = ""
        fun switchConfig(name: String) {}
        fun deleteConfig(name: String) {}
    }

    object AddonManager {
        val INSTANCE = AddonManager
        fun getAddons(): List<Any> = emptyList()
    }

    object FriendManager {
        val INSTANCE = FriendManager
        fun getFriends(): List<String> = emptyList()
        fun isFriend(name: String): Boolean = false
        fun addFriend(name: String) {}
        fun removeFriend(name: String) {}
    }

    object OpaiModuleManager {
        val INSTANCE = OpaiModuleManager
        fun getModules(): List<Module> = ArrayList(ModuleManager)
    }

    object SoundManager {
        val INSTANCE = SoundManager
        fun playInUi(key: Any?) {}
    }

    enum class SoundKey {
        SETTINGS_OPEN, SETTINGS_CLOSE
    }
}
