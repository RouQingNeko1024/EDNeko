/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce

import com.formdev.flatlaf.themes.FlatMacLightLaf
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.api.ClientUpdate
import net.ccbluex.liquidbounce.api.ClientUpdate.gitInfo
import net.ccbluex.liquidbounce.api.loadSettings
import net.ccbluex.liquidbounce.cape.CapeService
import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.StartupEvent
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.CommandManager.registerCommands
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.ModuleManager.registerModules
import net.ccbluex.liquidbounce.features.special.BungeeCordSpoof
import net.ccbluex.liquidbounce.features.special.ClientFixes
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.FileManager.loadAllConfigs
import net.ccbluex.liquidbounce.file.FileManager.saveAllConfigs
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration
import net.ccbluex.liquidbounce.file.configs.models.ClientConfiguration.updateClientWindow
import net.ccbluex.liquidbounce.lang.LanguageManager.loadLanguages
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.script.ScriptManager.enableScripts
import net.ccbluex.liquidbounce.script.ScriptManager.loadScripts
import net.ccbluex.liquidbounce.script.remapper.Remapper
import net.ccbluex.liquidbounce.script.remapper.Remapper.loadSrg
import net.ccbluex.liquidbounce.tabs.BlocksTab
import net.ccbluex.liquidbounce.tabs.ExploitsTab
import net.ccbluex.liquidbounce.tabs.HeadsTab
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager.Companion.loadActiveGenerators
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.ClassUtils.hasForge
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.ClientUtils.disableFastRender
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.io.MiscUtils
import net.ccbluex.liquidbounce.utils.io.MiscUtils.showErrorPopup
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes
import net.ccbluex.liquidbounce.utils.movement.BPSUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.TimerBalanceUtils
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister
import net.ccbluex.liquidbounce.utils.render.shader.Background
import net.ccbluex.liquidbounce.utils.render.shader.BuiltinShaderBackground
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.timing.TickedActions
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import javax.swing.UIManager

object LiquidBounce {

    /**
     * Client Information
     *
     * This has all the basic information.
     */
    const val CLIENT_NAME = "edneko"
    const val CLIENT_AUTHOR = "CCBlueX,LMX"
    const val CLIENT_CLOUD = "https://cloud.liquidbounce.net/LiquidBounce"
    const val CLIENT_WEBSITE = "liquidbounce.net"
    const val CLIENT_GITHUB = "https://github.com/lmx0721/edneko"

    const val MINECRAFT_VERSION = "1.8.9"
    
    val clientVersionText: String
    val clientVersionNumber: Int
    val clientCommit: String
    val clientBranch: String

    init {
        val props = java.util.Properties()
        try {
            val stream = javaClass.classLoader.getResourceAsStream("edneko.properties")
            if (stream != null) {
                props.load(stream)
                stream.close()
            }
        } catch (e: Exception) {
            LOGGER.warn("Failed to load version properties: ${e.message}")
        }
        
        clientVersionText = props.getProperty("version", "b1.3")
        clientVersionNumber = 100
        clientCommit = props.getProperty("commit", "unknown")
        clientBranch = props.getProperty("branch", "unknown")
    }

    /**
     * Defines if the client is in development mode.
     * This will enable update checking on commit time instead of regular legacy versioning.
     */
    const val IN_DEV = false

    val clientTitle = CLIENT_NAME + " " + clientVersionText + " " + MINECRAFT_VERSION + if (IN_DEV) " | DEVELOPMENT BUILD" else ""

    var isStarting = true

    // Managers
    val moduleManager = ModuleManager
    val commandManager = CommandManager
    val eventManager = EventManager
    val fileManager = FileManager
    val scriptManager = ScriptManager

    // HUD & ClickGUI
    val hud = HUD

    val clickGui = ClickGui

    // Menu Background
    var background: Background? = null
    var defaultMenuBackground: BuiltinShaderBackground? = null
    var customMenuBackground: BuiltinShaderBackground? = null

    fun getCurrentBackground(): Background? {
        val customBgFile = FileManager.backgroundImageFile
        val customShaderFile = FileManager.backgroundShaderFile
        
        if (customBgFile.exists() || customShaderFile.exists()) {
            return background
        }
        
        return if (ClientConfiguration.mainMenuStyle == "Custom") {
            customMenuBackground ?: Background.fromBuiltin(ClientConfiguration.customMenuBackgroundIndex).also { customMenuBackground = it }
        } else {
            defaultMenuBackground ?: Background.fromBuiltin(ClientConfiguration.defaultMenuBackgroundIndex).also { defaultMenuBackground = it }
        }
    }


    /**
     * Start IO tasks
     */
    fun preload(): Future<*> {

        net.ccbluex.liquidbounce.utils.client.javaVersion

        // Change theme of Swing
        UIManager.setLookAndFeel(FlatMacLightLaf())

        val future = CompletableFuture<Unit>()

        SharedScopes.IO.launch {
            try {
                LOGGER.info("Starting preload tasks of $CLIENT_NAME")

                // Load languages
                loadLanguages()

                // Load alt generators
                loadActiveGenerators()

                // Load SRG file
                loadSrg()

                LOGGER.info("Preload tasks of $CLIENT_NAME are completed!")

                future.complete(Unit)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    /**
     * Execute if client will be started
     */
    fun startClient() {
        isStarting = true

        LOGGER.info("Starting $CLIENT_NAME $clientVersionText $clientCommit, by $CLIENT_AUTHOR")

        try {
            // Load client fonts
            Fonts.loadFonts()

            // Register listeners
            RotationUtils
            ClientFixes
            BungeeCordSpoof
            CapeService
            InventoryUtils
            InventoryManager
            MiniMapRegister
            TickedActions
            MovementUtils
            PacketUtils
            TimerBalanceUtils
            BPSUtils
            WaitTickUtils
            SilentHotbar
            BlinkUtils

            // Load settings
            loadSettings(false) {
                LOGGER.info("Successfully loaded ${it.size} settings.")
            }

            // Register commands
            registerCommands()

            // Setup module manager and register modules
            registerModules()

            runCatching {
                // Remapper
                loadSrg()

                if (!Remapper.mappingsLoaded) {
                    error("Failed to load SRG mappings.")
                }



                // ScriptManager
                loadScripts()
                enableScripts()
            }.onFailure {
                LOGGER.error("Failed to load scripts.", it)
            }

            // Load configs
            loadAllConfigs()
            
            // Sync theme settings from ThemeManager to ClientThemesUtils
            net.ccbluex.liquidbounce.features.module.modules.client.ThemeManager.syncToClientThemesUtils()

            // Update client window
            updateClientWindow()

            // Tabs (Only for Forge!)
            if (hasForge()) {
                BlocksTab()
                ExploitsTab()
                HeadsTab()
            }

            // Disable Optifine FastRender
            disableFastRender()

            // Initialize InputFix
            net.ccbluex.liquidbounce.utils.inputfix.InputFixInit.init()


            // Login into known token if not empty
            if (CapeService.knownToken.isNotBlank()) {
                SharedScopes.IO.launch {
                    runCatching {
                        CapeService.login(CapeService.knownToken)
                    }.onFailure {
                        LOGGER.error("Failed to login into known cape token.", it)
                    }.onSuccess {
                        LOGGER.info("Successfully logged in into known cape token.")
                    }
                }
            }

            // Refresh cape service
            CapeService.refreshCapeCarriers {
                LOGGER.info("Successfully loaded ${it.size} cape carriers.")
            }

            // Load background
            FileManager.loadBackground()
        } catch (e: Exception) {
            LOGGER.error("Failed to start client: ${e.message}")
            e.showErrorPopup()
        } finally {
            // Set is starting status
            isStarting = false

            if (!FileManager.firstStart && FileManager.backedup) {
                SharedScopes.IO.launch {
                    MiscUtils.showMessageDialog("Warning: backup triggered", "Client update detected! Please check the config folder.")
                }
            }

            EventManager.call(StartupEvent)
            LOGGER.info("Successfully started client")
        }
    }

    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        // Call client shutdown
        EventManager.call(ClientShutdownEvent)

        // Stop all CoroutineScopes
        SharedScopes.stop()

        // Save all available configs
        saveAllConfigs()
    }

}
