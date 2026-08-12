/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce
 * Code By GoldBounce,Lizz,NightSky,FDP
 * https://github.com/SkidderMC/FDPClient
 * https://github.com/qm123pz/NightSky-Client
 * https://github.com/bzym2/GoldBounce/
 */
// skid neko bounce 
// https://github.com/RouQingNeko1024/NekoBounce
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.ScreenEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.music.MusicPlayer
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.client.ServerUtils
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.BlurEffects
import net.ccbluex.liquidbounce.utils.render.EmbeddedStencil
import net.ccbluex.liquidbounce.utils.render.InternalBlurShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import net.minecraft.util.ResourceLocation
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiPlayerTabOverlay
import net.minecraft.util.IChatComponent
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.EXTFramebufferObject
import org.lwjgl.opengl.EXTPackedDepthStencil
import java.awt.Color
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.cos
import kotlin.math.sin
object Island : Module("Island", Category.RENDER) {
    private val ClientName by text("ClientName", "Air")
    private val animTension by float("BounceTension", 0.01f, 0.01f..1.0f)
    private val animFriction by float("BounceFriction", 0.12f, 0.01f..1.0f)
    private val styles = "New Opai"
    
    private val logoIcon by choices("LogoIcon", arrayOf("Default", "A", "Diamond", "Radioactive", "Start", "Start2", "Earth"), "A")
    private val pingIcon by choices("PingIcon", arrayOf("Default", "Ping2", "Ping3", "Ping4", "Ping5"), "Ping3")
    private val gappleIcon by choices("GappleIcon", arrayOf("Default", "Heart", "Heart2"), "Default")
    private val userIcon by choices("UserIcon", arrayOf("Default", "User2", "User3", "User4"), "Default")
    
    private val customip by boolean("customIP", false)
    private val ip by text("IP", "hidden.ip") { customip }
    private val ColorA_ by int("Red", 255, 0..255)
    private val ColorB_ by int("Green", 255, 0..255)
    private val ColorC_ by int("Blue", 255, 0..255)

    private val BackgroundAlpha by int("BackGroundAlpha", 160, 0..255)

    private val ShadowCheck by boolean("Shadow", false)
    private val shadowRadiusValue by float("Shadow-Radius", 15F, 1F..50F) { ShadowCheck }
    private val shadowColor by color("Shadow-Color", Color(0, 0, 0, 120)) { ShadowCheck }

    private val blurCheck by boolean("Blur", true)
    private val blurMode by choices("BlurMode", arrayOf("Gaussian", "Dual", "Better"), "Better") { blurCheck }
    private val blurRadius by float("BlurStrength", 10F, 1F..50F) { blurCheck }

    private val notifyDuration by int("NotifyTime(ms)", 1000, 100..10000)
    private val versionNameUp by text("VersionName", "development") { styles == "Opal" }
    private val ButtonColor by color("Button-Color", Color(20, 150, 180, 255))
    private val ModuleNotify by boolean("Notification", true)
    private val isScaffold by boolean("Scaffold", true)
    private val scaffoldStyle by choices("ScaffoldStyle", arrayOf("Classic", "Bar", "Minimal", "Compact", "Standard", "Circle", "Split", "Modern", "Outline"), "Classic") { isScaffold }
    private val ScaffoldTheme by color("ScaffoldTheme", Color(65, 130, 225))
    private val maxBlocks by int("maxBlocks", 576, 64..576)
    private val standardShowBlockCount by boolean("StandardShowBlockCount", true) { isScaffold && scaffoldStyle == "Standard" }

    private val breakProgressCheck by boolean("BreakProgress", true)
    private val breakProgressTheme by color("BreakProgressTheme", Color(225, 150, 65))
    private val breakProgressOnlyFuckerNuker by boolean("OnlyFuckerAndNuker", true) { breakProgressCheck }
    
    private val showGappleProgress by boolean("GappleProgress", true)
    private val gappleProgressTheme by color("GappleProgressTheme", Color(255, 215, 0))
    
    private val ChestTheme by boolean("Chest", true)
    private val ChestRounded by float("ChestRoundRadius", 4F, 0.0F..8.0F)

    private val tabListCheck by boolean("TabList", true)
    private val tabListMaxRows by int("TabList-MaxRows", 20, 5..100)

    private val bpsUpdateInterval by int("BPS-Update-Interval(ms)", 100, 50..500)
    private val versionNameDown = clientVersionText

    private val showLyricOnIsland by boolean("ShowLyric", true)
    private val lyricDisplayMode by choices("LyricMode", arrayOf("None", "Below", "Inside", "Float", "Full"), "None") { showLyricOnIsland }
    private val lyricHeight by int("LyricHeight", 40, 20..80) { showLyricOnIsland && lyricDisplayMode != "None" }
    private val lyricFloatOffsetY by int("LyricFloatOffsetY", 20, 0..200) { showLyricOnIsland && (lyricDisplayMode == "Float" || lyricDisplayMode == "Full") }
    private val lyricShowMusicName by boolean("LyricShowMusicName", true) { showLyricOnIsland && lyricDisplayMode != "None" && lyricDisplayMode != "Full" }
    private val lyricShowPrevious by boolean("LyricShowPrevious", true) { showLyricOnIsland && lyricDisplayMode == "Below" }
    private val lyricShowNext by boolean("LyricShowNext", true) { showLyricOnIsland && lyricDisplayMode == "Below" }
    private val lyricMusicNameFont by choices("LyricMusicNameFont", arrayOf("ExtraBold35", "ExtraBold40", "Semibold35", "Semibold40", "Regular30", "Regular35", "Regular40", "Regular45", "Bold180"), "Semibold35") { showLyricOnIsland && lyricDisplayMode != "None" }
    private val lyricTextFont by choices("LyricTextFont", arrayOf("ExtraBold35", "ExtraBold40", "Semibold35", "Semibold40", "Regular30", "Regular35", "Regular40", "Regular45", "Bold180"), "Regular35") { showLyricOnIsland && lyricDisplayMode != "None" }
    private val lyricColorMode by choices("LyricColorMode", arrayOf("Custom", "Theme"), "Theme") { showLyricOnIsland && lyricDisplayMode != "None" }
    private val lyricGradientMode by choices("LyricGradientMode", arrayOf("Sync", "LeftToRight", "RightToLeft"), "Sync") { showLyricOnIsland && lyricDisplayMode != "None" && lyricColorMode == "Theme" }
    private val lyricCustomColor by color("LyricCustomColor", Color(255, 255, 255)) { showLyricOnIsland && lyricDisplayMode != "None" && lyricColorMode == "Custom" }
    private val lyricBackgroundAlpha by int("LyricBackgroundAlpha", 160, 0..255) { showLyricOnIsland && (lyricDisplayMode == "Below" || lyricDisplayMode == "Float" || lyricDisplayMode == "Full") }
    private val lyricTextAlpha by int("LyricTextAlpha", 0, 0..255) { showLyricOnIsland && lyricDisplayMode != "None" }
    private val lyricBlur by boolean("LyricBlur", true) { showLyricOnIsland && (lyricDisplayMode == "Below" || lyricDisplayMode == "Float" || lyricDisplayMode == "Full") }
    private val lyricBounce by boolean("LyricBounce", true) { showLyricOnIsland && lyricDisplayMode != "None" }
    private val lyricScrollAnimation by boolean("LyricScrollAnimation", true) { showLyricOnIsland && (lyricDisplayMode == "Below" || lyricDisplayMode == "Float") }
    private val lyricScrollAnimTime by int("LyricScrollAnimTime", 300, 100..1000) { showLyricOnIsland && lyricScrollAnimation && (lyricDisplayMode == "Below" || lyricDisplayMode == "Float") }
    private val lyricShowProgress by boolean("LyricShowProgress", true) { showLyricOnIsland && (lyricDisplayMode == "Below" || lyricDisplayMode == "Float" || lyricDisplayMode == "Full") }
    private val lyricFullWidth by int("LyricFullWidth", 300, 100..500) { showLyricOnIsland && lyricDisplayMode == "Full" }
    private val lyricFullHeight by int("LyricFullHeight", 50, 30..150) { showLyricOnIsland && lyricDisplayMode == "Full" }
    private val lyricFullAnimation by choices("LyricFullAnimation", arrayOf("None", "Fade", "SlideLeft", "SlideRight", "SlideUp", "SlideDown", "Scale", "Typewriter"), "Fade") { showLyricOnIsland && lyricDisplayMode == "Full" }
    private val lyricFullAnimTime by int("LyricFullAnimTime", 300, 100..1000) { showLyricOnIsland && lyricDisplayMode == "Full" && lyricFullAnimation != "None" }

    private var breakProgressTarget = 0F
    private var animatedBreakProgress = 0F
    private var lastBreakProgressUpdateTime: Long = 0L

    private var gappleProgressTarget = 0F
    private var animatedGappleProgress = 0F
    private var lastGappleProgressUpdateTime: Long = 0L

    private var AnimGlobalX = 0F
    private var AnimGlobalY = 0F
    private var AnimGlobalWidth = 100F
    private var AnimGlobalHeight = 28F

    private var VelGlobalX = 0f
    private var VelGlobalY = 0f
    private var VelGlobalWidth = 0f
    private var VelGlobalHeight = 0f

    private var animBubbleY = 0F
    private var animBubbleAlpha = 0F
    private var animBubbleWidth = 0F
    private var animBubbleHeight = 0F
    private var velBubbleY = 0f
    private var velBubbleAlpha = 0f
    private var velBubbleWidth = 0f
    private var velBubbleHeight = 0f

    private var animInsideWidth = 0F
    private var velInsideWidth = 0f

    private var animScrollOffset = 0F
    private var velScrollOffset = 0f
    private var lastLyricChangeTime = 0L
    private var lastLyricText = ""
    private var scrollAnimProgress = 0F
    private var velScrollAnim = 0F

    private data class SlotRipple(val x: Float, val y: Float, val startTime: Long)
    private val slotRipples = CopyOnWriteArrayList<SlotRipple>()
    private val prevSlotItems = HashMap<Int, ItemStack?>()
    private var lastChestContainerHash: Int = 0

    private const val ITEM_NOTIFY_HEIGHT = 38F
    private const val NORMAL_WATERMARK_HEIGHT = 28F

    private var prevX: Double = 0.0
    private var prevZ: Double = 0.0
    private var AnimatedBps = 0.0
    private var lastBPSUpdateTime: Long = 0L
    private var displayedBPS: Double = 0.0
    private var ProgressBarAnimationWidth = 0F

    private val prevModuleStates = HashMap<Module, Boolean>()
    private val notifications = CopyOnWriteArrayList<ToggleNotification>()
    private var scaledScreen = ScaledResolution(mc)
    private var width = scaledScreen.scaledWidth
    private var height = scaledScreen.scaledHeight
    private var start_y = (height / 20).toFloat()

    private var headerFooterCacheTime = 0L
    private var cachedHeader: List<String>? = null
    private var cachedFooter: List<String>? = null

    private fun getSafePing(): Int {
        val player = mc.thePlayer ?: return 0
        return mc.netHandler?.getPlayerInfo(player.uniqueID)?.responseTime ?: 0
    }

    private fun getLogoResource(): ResourceLocation {
        return when (logoIcon) {
            "Default" -> ResourceLocation("edneko/watermark_images/logo_icon.png")
            "A" -> ResourceLocation("edneko/watermark_images/A.png")
            "Diamond" -> ResourceLocation("edneko/watermark_images/Diamond.png")
            "Radioactive" -> ResourceLocation("edneko/watermark_images/Radioactive.png")
            "Start" -> ResourceLocation("edneko/watermark_images/start.png")
            "Start2" -> ResourceLocation("edneko/watermark_images/start2.png")
            "Earth" -> ResourceLocation("edneko/watermark_images/earth.png")
            else -> ResourceLocation("edneko/watermark_images/logo_icon.png")
        }
    }

    private fun getPingResource(): ResourceLocation {
        return when (pingIcon) {
            "Default" -> ResourceLocation("edneko/watermark_images/ms.png")
            "Ping2" -> ResourceLocation("edneko/watermark_images/ping2.png")
            "Ping3" -> ResourceLocation("edneko/watermark_images/ping3.png")
            "Ping4" -> ResourceLocation("edneko/watermark_images/ping4.png")
            "Ping5" -> ResourceLocation("edneko/watermark_images/ping5.png")
            else -> ResourceLocation("edneko/watermark_images/ms.png")
        }
    }

    private fun getGappleResource(): ResourceLocation {
        return when (gappleIcon) {
            "Default" -> ResourceLocation("edneko/watermark_images/apple.png")
            "Heart" -> ResourceLocation("edneko/watermark_images/heart.png")
            "Heart2" -> ResourceLocation("edneko/watermark_images/heart2.png")
            else -> ResourceLocation("edneko/watermark_images/apple.png")
        }
    }

    private fun getUserResource(): ResourceLocation {
        return when (userIcon) {
            "Default" -> ResourceLocation("edneko/watermark_images/user.png")
            "User2" -> ResourceLocation("edneko/watermark_images/user2.png")
            "User3" -> ResourceLocation("edneko/watermark_images/user3.png")
            "User4" -> ResourceLocation("edneko/watermark_images/user4.png")
            else -> ResourceLocation("edneko/watermark_images/user.png")
        }
    }

    private fun applyBlur(x: Float, y: Float, w: Float, h: Float) {
        GlStateManager.pushMatrix()
        when (blurMode) {
            "Gaussian" -> BlurEffects.blurArea(x, y, w, h, blurRadius, BlurEffects.BlurMode.GAUSSIAN)
            "Dual" -> BlurEffects.blurArea(x, y, w, h, blurRadius, BlurEffects.BlurMode.DUAL)
            "Better" -> BlurEffects.blurArea(x, y, w, h, blurRadius, BlurEffects.BlurMode.BETTER)
        }
        GlStateManager.popMatrix()
    }

    private fun spring(current: Float, target: Float, velocity: Float): Pair<Float, Float> {
        val displacement = target - current
        val force = displacement * animTension
        val drag = velocity * animFriction
        val acceleration = force - drag
        val newVelocity = velocity + acceleration
        val newPosition = current + newVelocity
        return newPosition to newVelocity
    }

    private fun getTabListHeaderFooter(): Pair<IChatComponent?, IChatComponent?> {
        try {
            val tabOverlay = mc.ingameGUI.tabList
            val cls = GuiPlayerTabOverlay::class.java

            var headerField = try { cls.getDeclaredField("header") } catch (e: Exception) { cls.getDeclaredField("field_175256_a") }
            headerField.isAccessible = true
            val header = headerField.get(tabOverlay) as? IChatComponent

            var footerField = try { cls.getDeclaredField("footer") } catch (e: Exception) { cls.getDeclaredField("field_175255_b") }
            footerField.isAccessible = true
            val footer = footerField.get(tabOverlay) as? IChatComponent

            return Pair(header, footer)
        } catch (e: Exception) {
            return Pair(null, null)
        }
    }

    private fun shouldShowBreakProgress(): Boolean {
        if (!breakProgressOnlyFuckerNuker) return true
        val fucker = ModuleManager.getModule("Fucker") ?: return false
        val nuker = ModuleManager.getModule("Nuker") ?: return false
        return fucker.state || nuker.state
    }

    val onUpdate = handler<UpdateEvent> {
        if (mc.thePlayer == null || mc.theWorld == null) return@handler

        if (tabListCheck) {
            mc.gameSettings.keyBindPlayerList.pressed = false
        }

        val distanceX = mc.thePlayer.posX - prevX
        val distanceZ = mc.thePlayer.posZ - prevZ
        val currentCalculatedBPS = sqrt(distanceX.pow(2) + distanceZ.pow(2)) * 20.0
        if (System.currentTimeMillis() - lastBPSUpdateTime >= bpsUpdateInterval) {
            displayedBPS = currentCalculatedBPS
            lastBPSUpdateTime = System.currentTimeMillis()
        }
        prevX = mc.thePlayer.posX
        prevZ = mc.thePlayer.posZ

        if (mc.playerController != null && mc.thePlayer != null) {
            val currentBreakProgress = mc.playerController.curBlockDamageMP
            if (System.currentTimeMillis() - lastBreakProgressUpdateTime >= 50) {
                breakProgressTarget = currentBreakProgress
                lastBreakProgressUpdateTime = System.currentTimeMillis()
            }
        } else {
            breakProgressTarget = 0F
        }

        val gappleModule = ModuleManager.getModule("Gapple")
        if (gappleModule != null && gappleModule.state) {
            val currentProgress = getGappleEatingProgress()
            if (System.currentTimeMillis() - lastGappleProgressUpdateTime >= 50) {
                gappleProgressTarget = currentProgress
                lastGappleProgressUpdateTime = System.currentTimeMillis()
            }
        } else {
            gappleProgressTarget = 0F
        }

        if (ModuleNotify) {
            for (module in ModuleManager) {
                if (!prevModuleStates.containsKey(module)) {
                    prevModuleStates[module] = module.state
                    continue
                }
                val prevState = prevModuleStates[module]!!
                val currentState = module.state
                if (prevState != currentState) {
                    prevModuleStates[module] = currentState

                    val titleText = "Module Toggled"
                    val modName = "${module.name}"
                    val stateText = if (currentState) "§l§aEnabled" else "§l§cDisabled"
                    val message = "§l$modName§r §fhas been $stateText§r §f!"

                    showToggleNotification(titleText, message, currentState, module.name)
                }
            }
        }
    }

    private fun getGappleEatingProgress(): Float {
        val gappleModule = ModuleManager.getModule("Gapple") ?: return 0f
        if (!gappleModule.state) return 0f
        
        return try {
            val getProgressMethod = gappleModule::class.java.getDeclaredMethod("getEatingProgress")
            getProgressMethod.isAccessible = true
            (getProgressMethod.invoke(gappleModule) as? Float) ?: 0f
        } catch (e: Exception) {
            try {
                val isEatingField = gappleModule::class.java.getDeclaredField("isEating")
                isEatingField.isAccessible = true
                val isEating = isEatingField.getBoolean(gappleModule)
                
                if (!isEating) return 0f
                
                val ticksField = gappleModule::class.java.getDeclaredField("ticks")
                ticksField.isAccessible = true
                val ticks = ticksField.getInt(gappleModule)
                
                val cField = gappleModule::class.java.getDeclaredField("c")
                cField.isAccessible = true
                val c = cField.get(gappleModule) as Int
                
                (ticks.toFloat() / c.toFloat()).coerceIn(0f, 1f)
            } catch (e3: Exception) {
                0f
            }
        }
    }

    val onScreen = handler<ScreenEvent>(always = true) { event ->
        if (mc.theWorld == null || mc.thePlayer == null) return@handler
    }

    val onRender2D = handler<Render2DEvent> {
        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)

        updateNotifications()
        scaledScreen = ScaledResolution(mc)
        width = scaledScreen.scaledWidth
        height = scaledScreen.scaledHeight
        start_y = (height / 20).toFloat()

        val scaffoldModule = ModuleManager.getModule("Scaffold")
        val scaffoldModule2 = ModuleManager.getModule("Scaffold2")

        val isChestOpen = mc.currentScreen is GuiChest && ChestTheme
        val chestSlots = if (isChestOpen) {
            (mc.currentScreen as GuiChest).inventorySlots?.inventorySlots?.filter { it.inventory != mc.thePlayer?.inventory } ?: emptyList()
        } else emptyList()

        if (!isChestOpen) {
            prevSlotItems.clear()
            slotRipples.clear()
            lastChestContainerHash = 0
        } else {
            val currentContainerId = (mc.currentScreen as GuiChest).inventorySlots.windowId
            if (currentContainerId != lastChestContainerHash) {
                prevSlotItems.clear()
                slotRipples.clear()
                lastChestContainerHash = currentContainerId
            }
        }

        val tabKey = mc.gameSettings.keyBindPlayerList.keyCode
        val isTabKeyDown = if (tabKey > 0) Keyboard.isKeyDown(tabKey) else false
        val showTabList = tabListCheck && isTabKeyDown && mc.netHandler != null

        val playerList = if (showTabList) {
            mc.netHandler.playerInfoMap
                .filter { it.gameProfile.name != null }
                .sortedWith(compareBy({ it.gameProfile.name }))
        } else emptyList()

        val lerpSpeed = 0.2f * (Minecraft.getDebugFPS() / 60f).coerceIn(0.5f, 2f)
        animatedBreakProgress += (breakProgressTarget - animatedBreakProgress) * lerpSpeed
        animatedBreakProgress = animatedBreakProgress.coerceIn(0F, 1F)

        animatedGappleProgress += (gappleProgressTarget - animatedGappleProgress) * lerpSpeed
        animatedGappleProgress = animatedGappleProgress.coerceIn(0F, 1F)

        var targetWidth = 0F
        var targetHeight = 0F
        var targetX = 0F
        var targetY = start_y
        var renderMode = "NONE"

        var headerLines: List<String> = emptyList()
        var footerLines: List<String> = emptyList()

        if (showLyricOnIsland && lyricDisplayMode == "Full" && MusicPlayer.currentLyricDisplay.isNotEmpty()) {
            renderMode = "LYRIC_FULL"
            val textFont = getLyricTextFont()
            val currentLyric = MusicPlayer.currentLyricDisplay
            val textWidth = textFont.getStringWidth(currentLyric).toFloat()
            targetWidth = (textWidth + 40F).coerceIn(100F, lyricFullWidth.toFloat())
            targetHeight = if (lyricShowProgress) 50F else 35F
            targetX = (width - targetWidth) / 2
            targetY = lyricFloatOffsetY.toFloat()
        } else if (chestSlots.isNotEmpty()) {
            renderMode = "CHEST"
            val columns = 9
            val rows = (chestSlots.size + 8) / 9
            val padding = 8F
            val slotSize = 16F
            targetWidth = columns * slotSize + padding * 2
            targetHeight = rows * slotSize + padding * 2
            targetX = (width - targetWidth) / 2
            targetY = start_y.coerceIn(5f, height - targetHeight - 5f)

        } else if (showTabList && playerList.isNotEmpty()) {
            renderMode = "TABLIST"
            if (System.currentTimeMillis() - headerFooterCacheTime > 500) {
                val (h, f) = getTabListHeaderFooter()
                cachedHeader = h?.formattedText?.split("\n")
                cachedFooter = f?.formattedText?.split("\n")
                headerFooterCacheTime = System.currentTimeMillis()
            }
            headerLines = cachedHeader ?: emptyList()
            footerLines = cachedFooter ?: emptyList()

            val playerCount = playerList.size
            val maxRows = tabListMaxRows
            val columns = ceil(playerCount.toDouble() / maxRows.toDouble()).toInt()
            val headSize = 10F
            val outerPadding = 8F
            val padding = 6F
            val spacing = 4F
            var maxNameWidth = 50F

            playerList.forEach { it ->
                val fullName = mc.ingameGUI.tabList.getPlayerName(it)
                val w = Fonts.fontRegular35.getStringWidth(fullName).toFloat()
                if (w > maxNameWidth) maxNameWidth = w
            }
            val columnWidth = padding + headSize + spacing + maxNameWidth + spacing + 25F + padding
            val playersWidth = columns * columnWidth

            var maxHeaderW = 0f
            headerLines.forEach { maxHeaderW = max(maxHeaderW, Fonts.fontRegular35.getStringWidth(it).toFloat()) }
            var maxFooterW = 0f
            footerLines.forEach { maxFooterW = max(maxFooterW, Fonts.fontRegular35.getStringWidth(it).toFloat()) }

            targetWidth = max(playersWidth, max(maxHeaderW, maxFooterW) + padding * 2)

            val lineH = Fonts.fontRegular35.FONT_HEIGHT + 2
            val headerHeight = if(headerLines.isNotEmpty()) headerLines.size * lineH + 2 else 0
            val footerHeight = if(footerLines.isNotEmpty()) footerLines.size * lineH + 2 else 0

            val actualRows = if (columns == 1) playerCount else maxRows
            val playersBlockHeight = actualRows * (headSize + spacing) - spacing

            targetHeight = outerPadding +
                    headerHeight.toFloat() +
                    (if(headerHeight > 0) 2F else 0F) +
                    playersBlockHeight +
                    (if(footerHeight > 0) 2F else 0F) +
                    footerHeight.toFloat() +
                    outerPadding

            targetX = (width - targetWidth) / 2
            targetY = start_y

        } else if (scaffoldModule?.state == true ||  scaffoldModule2?.state == true && isScaffold) {
            renderMode = "SCAFFOLD"
            when (scaffoldStyle) {
                "Classic" -> { targetWidth = 190F; targetHeight = 58F }
                "Bar" -> { targetWidth = 280F; targetHeight = 28F }
                "Minimal" -> { targetWidth = 120F; targetHeight = 24F }
                "Compact" -> { targetWidth = 160F; targetHeight = 40F }
                "Standard" -> { targetWidth = 280F; targetHeight = 28F }
                "Circle" -> { targetWidth = 80F; targetHeight = 80F }
                "Split" -> { targetWidth = 200F; targetHeight = 50F }
                "Modern" -> { targetWidth = 220F; targetHeight = 65F }
                "Outline" -> { targetWidth = 180F; targetHeight = 35F }
                else -> { targetWidth = 190F; targetHeight = 58F }
            }
            targetX = (width - targetWidth) / 2
        } else if (showGappleProgress && animatedGappleProgress > 0.01f && ModuleManager.getModule("Gapple")?.state == true) {
            renderMode = "GAPPLE_PROGRESS"
            targetWidth = 190F
            targetHeight = 58F
            targetX = (width - targetWidth) / 2
            targetY = start_y
        } else if (notifications.isNotEmpty() && ModuleNotify && styles == "New Opai") {
            renderMode = "NOTIFY_STACK"
            val borderInfo = calcMaxNotificationWidth()
            targetWidth = borderInfo.coerceAtLeast(180F)
            targetHeight = (notifications.size * ITEM_NOTIFY_HEIGHT).toFloat()
            targetX = (width - targetWidth) / 2
        } else if (breakProgressCheck && animatedBreakProgress > 0.01f && shouldShowBreakProgress()) {
            renderMode = "BREAK_PROGRESS"
            targetWidth = 190F
            targetHeight = 58F
            targetX = (width - targetWidth) / 2
            targetY = start_y
        } else {
            when (styles) {
                "New Opai" -> {
                    renderMode = "NORMAL_OPAI"
                    val info = calcNormal3Info()
                    targetWidth = info.width
                    targetHeight = NORMAL_WATERMARK_HEIGHT
                    targetX = (width - targetWidth) / 2
                }
                "Normal" -> {
                    drawNormal()
                    if (notifications.isNotEmpty() && ModuleNotify) drawOldStyleNotifications()
                }
                "Opal" -> {
                    drawNormal2()
                    if (notifications.isNotEmpty() && ModuleNotify) drawOldStyleNotifications(startYOffset = 32F)
                }
            }
        }

        if (renderMode != "NONE") {
            val (nextW, vW) = spring(AnimGlobalWidth, targetWidth, VelGlobalWidth)
            AnimGlobalWidth = nextW.coerceAtLeast(0F)
            VelGlobalWidth = vW

            val (nextH, vH) = spring(AnimGlobalHeight, targetHeight, VelGlobalHeight)
            AnimGlobalHeight = nextH.coerceAtLeast(0F)
            VelGlobalHeight = vH

            val (nextX, vX) = spring(AnimGlobalX, targetX, VelGlobalX)
            AnimGlobalX = nextX
            VelGlobalX = vX

            val (nextY, vY) = spring(AnimGlobalY, targetY, VelGlobalY)
            AnimGlobalY = nextY
            VelGlobalY = vY

            val currentRadius = if (AnimGlobalHeight > 30F) {
                if (renderMode == "CHEST" || renderMode == "TABLIST") ChestRounded else 8F
            } else AnimGlobalHeight / 2F

            val drawX = AnimGlobalX
            val drawY = AnimGlobalY
            val drawW = AnimGlobalWidth
            val drawH = AnimGlobalHeight
            
            val isBelowMode = showLyricOnIsland && lyricDisplayMode == "Below"
            val islandCorners = if (isBelowMode) RenderUtils.RoundedCorners.TOP_ONLY else RenderUtils.RoundedCorners.ALL

            try {
                EmbeddedStencil.checkSetupFBO(mc.framebuffer)
                EmbeddedStencil.write(false)
                RenderUtils.drawRoundedRect(drawX, drawY, drawX + drawW, drawY + drawH, Color.WHITE.rgb, currentRadius, islandCorners)

                EmbeddedStencil.erase(false)
                if (ShadowCheck) {
                    val maxDist = shadowRadiusValue.toInt()
                    for (i in maxDist downTo 1) {
                        val alpha = (shadowColor.alpha * (1f - i.toFloat() / (maxDist + 1))).toInt().coerceIn(1, shadowColor.alpha)
                        RenderUtils.drawRoundedRect(
                            drawX - i, drawY - i,
                            drawX + drawW + i, drawY + drawH + i,
                            Color(shadowColor.red, shadowColor.green, shadowColor.blue, alpha).rgb,
                            currentRadius + i
                        )
                    }
                }

                EmbeddedStencil.erase(true)
                if (blurCheck) {
                    applyBlur(drawX, drawY, drawW, drawH)
                }

                RenderUtils.drawRoundedRect(
                    drawX, drawY,
                    drawX + drawW, drawY + drawH,
                    Color(0, 0, 0, BackgroundAlpha).rgb,
                    currentRadius,
                    islandCorners
                )
                
                glEnable(GL_BLEND)
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

                glDisable(GL_TEXTURE_2D)
                glEnable(GL_LINE_SMOOTH)
                glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
                glLineWidth(1.5f)
                RenderUtils.drawRoundedBorderRect(
                    drawX - 0.5f, drawY - 0.5f,
                    drawX + drawW + 0.5f, drawY + drawH + 0.5f,
                    0.5f,
                    Color(0, 0, 0, 0).rgb,
                    Color(255, 255, 255, 35).rgb,
                    currentRadius + 0.5f
                )
                glLineWidth(1.0f)
                glDisable(GL_LINE_SMOOTH)
                glEnable(GL_TEXTURE_2D)

                EmbeddedStencil.dispose()

            } catch (e: Exception) {
                if (ShadowCheck) {
                    val maxDist = shadowRadiusValue.toInt()
                    for (i in maxDist downTo 1) {
                        val alpha = (shadowColor.alpha * (1f - i.toFloat() / (maxDist + 1))).toInt().coerceIn(1, shadowColor.alpha)
                        RenderUtils.drawRoundedRect(
                            drawX - i, drawY - i,
                            drawX + drawW + i, drawY + drawH + i,
                            Color(shadowColor.red, shadowColor.green, shadowColor.blue, alpha).rgb,
                            currentRadius + i
                        )
                    }
                }
                RenderUtils.drawRoundedRect(drawX, drawY, drawX + drawW, drawY + drawH, Color(0,0,0,BackgroundAlpha).rgb, currentRadius, islandCorners)
                glEnable(GL_BLEND)
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            }

            when (renderMode) {
                "LYRIC_FULL" -> renderLyricFullContent(drawX, drawY, drawW, drawH)
                "SCAFFOLD" -> renderScaffoldContent(drawX, drawY, drawW, drawH)
                "NOTIFY_STACK" -> renderNotificationStack(drawX, drawY, drawW, drawH)
                "NORMAL_OPAI" -> renderNormal3Content(drawX, drawY, drawW, drawH)
                "CHEST" -> renderChestContent(drawX, drawY, drawW, drawH, chestSlots)
                "TABLIST" -> renderTabListContent(drawX, drawY, drawW, drawH, playerList, headerLines, footerLines)
                "BREAK_PROGRESS" -> renderBreakProgressContent(drawX, drawY, drawW, drawH)
                "GAPPLE_PROGRESS" -> renderGappleProgressContent(drawX, drawY, drawW, drawH)
            }
        }

        if (showLyricOnIsland && lyricDisplayMode != "None") {
            renderLyricDisplay()
        }

        glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE)
        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glPopMatrix()
    }

    private fun renderGappleProgressContent(x: Float, y: Float, w: Float, h: Float) {
        val percentage = animatedGappleProgress.coerceIn(0f, 1f)
        val padding = 8F
        val cornerRadius = 6F
        val iconSize = 32F
        val iconBgX = x + padding
        val iconBgY = y + padding
        val themeColor = Color(gappleProgressTheme.red, gappleProgressTheme.green, gappleProgressTheme.blue, 200)
        drawRoundedRect(iconBgX, iconBgY, iconBgX + iconSize, iconBgY + iconSize, themeColor.rgb, cornerRadius - 1)
        
        try {
            val appleImgSize = 24
            drawImage(getGappleResource(), 
                    (iconBgX + (iconSize - appleImgSize) / 2).toInt(), 
                    (iconBgY + (iconSize - appleImgSize) / 2 + 1).toInt(), 
                    appleImgSize, appleImgSize, Color.WHITE)
        } catch (e: Exception) {
            Fonts.fontSemibold40.drawCenteredString("EAT", iconBgX + iconSize / 2, iconBgY + iconSize / 2 - 8, Color.WHITE.rgb)
        }

        val textX = iconBgX + iconSize + 8F
        val titleY = y + padding + 2F
        Fonts.fontSemibold40.drawString("Eating Gapple", textX, titleY, Color.WHITE.rgb)
        val percentText = String.format("%.1f", percentage * 100) + "%"
        Fonts.fontRegular40.drawString(percentText, textX, titleY + Fonts.fontSemibold40.FONT_HEIGHT + 2F, Color(200, 200, 200).rgb)

        val barHeight = 8F
        val barY = y + h - barHeight - padding
        val maxBarWidth = w - (padding * 2)
        val currentBarWidth = maxBarWidth * percentage
        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight, Color(60, 60, 70, 180).rgb, 3F)
        val lighter = Color(gappleProgressTheme.red, gappleProgressTheme.green, gappleProgressTheme.blue, 255)
        drawRoundedRect(x + padding, barY, x + padding + currentBarWidth, barY + barHeight, lighter.rgb, 3F)
    }

    private fun renderBreakProgressContent(x: Float, y: Float, w: Float, h: Float) {
        val percentage = animatedBreakProgress.coerceIn(0f, 1f)
        val padding = 8F
        val cornerRadius = 6F
        val iconSize = 32F
        val iconBgX = x + padding
        val iconBgY = y + padding
        val themeColor = Color(breakProgressTheme.red, breakProgressTheme.green, breakProgressTheme.blue, 200)
        drawRoundedRect(iconBgX, iconBgY, iconBgX + iconSize, iconBgY + iconSize, themeColor.rgb, cornerRadius - 1)
        val bedImgSize = 24
        drawImage(ResourceLocation("edneko/watermark_images/bed.png"), (iconBgX + (iconSize - bedImgSize) / 2).toInt(), (iconBgY + (iconSize - bedImgSize) / 2 + 1).toInt(), bedImgSize, bedImgSize, Color.WHITE)

        val textX = iconBgX + iconSize + 8F
        val titleY = y + padding + 2F
        Fonts.fontSemibold40.drawString("Break Progress", textX, titleY, Color.WHITE.rgb)
        val percentText = String.format("%.1f", percentage * 100) + "%"
        Fonts.fontRegular40.drawString(percentText, textX, titleY + Fonts.fontSemibold40.FONT_HEIGHT + 2F, Color(200, 200, 200).rgb)

        val barHeight = 8F
        val barY = y + h - barHeight - padding
        val maxBarWidth = w - (padding * 2)
        val currentBarWidth = maxBarWidth * percentage
        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight, Color(60, 60, 70, 180).rgb, 3F)
        val lighter = Color(breakProgressTheme.red, breakProgressTheme.green, breakProgressTheme.blue, 255)
        drawRoundedRect(x + padding, barY, x + padding + currentBarWidth, barY + barHeight, lighter.rgb, 3F)
    }

    private fun drawCircle(x: Float, y: Float, radius: Float, color: Color) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        glBegin(GL_POLYGON)
        for (i in 0..360 step 10) {
            val theta = i * Math.PI / 180
            glVertex2d(x + radius * cos(theta), y + radius * sin(theta))
        }
        glEnd()

        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    private fun renderChestContent(x: Float, y: Float, w: Float, h: Float, slots: List<Slot>) {
        val padding = 8F
        val slotSize = 16

        val rippleDuration = 600L
        val maxRadius = 18F

        enableGUIStandardItemLighting()
        try {
            slots.forEachIndexed { index, slot ->
                val stack = slot.stack
                val col = index % 9
                val row = index / 9
                val itemX = (x + padding + col * slotSize).toInt()
                val itemY = (y + padding + row * slotSize).toInt()

                val prevStack = prevSlotItems[index]

                if (prevSlotItems.containsKey(index)) {
                    val isChanged = when {
                        stack == null && prevStack == null -> false
                        stack == null || prevStack == null -> true
                        else -> !ItemStack.areItemStacksEqual(stack, prevStack) || stack.stackSize != prevStack.stackSize
                    }

                    if (isChanged) {
                        slotRipples.add(SlotRipple((itemX + 8).toFloat(), (itemY + 8).toFloat(), System.currentTimeMillis()))
                    }
                }

                prevSlotItems[index] = stack?.copy()

                if (stack != null) {
                    if (mc.currentScreen is GuiHudDesigner) glDisable(GL_DEPTH_TEST)
                    mc.renderItem.renderItemAndEffectIntoGUI(stack, itemX, itemY)
                    mc.renderItem.renderItemOverlays(mc.fontRendererObj, stack, itemX, itemY)
                    if (mc.currentScreen is GuiHudDesigner) glEnable(GL_DEPTH_TEST)
                }
            }

            disableStandardItemLighting()
            GlStateManager.disableDepth()

            val currentTime = System.currentTimeMillis()
            val iterator = slotRipples.iterator()

            while (iterator.hasNext()) {
                val ripple = iterator.next()
                val timeAlive = currentTime - ripple.startTime

                if (timeAlive > rippleDuration) {
                    slotRipples.remove(ripple)
                } else {
                    val progress = timeAlive.toFloat() / rippleDuration.toFloat()
                    val ease = 1f - (1f - progress).pow(3)
                    val radius = maxRadius * ease
                    val alpha = (180 * (1f - progress)).toInt().coerceIn(0, 255)

                    if (alpha > 0) {
                        drawCircle(ripple.x, ripple.y, radius, Color(255, 255, 255, alpha))
                    }
                }
            }
            GlStateManager.enableDepth()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            disableStandardItemLighting()
            GlStateManager.enableAlpha()
            GlStateManager.disableBlend()
            GlStateManager.disableLighting()
        }
    }

    private fun renderTabListContent(x: Float, y: Float, w: Float, h: Float, players: List<NetworkPlayerInfo>, header: List<String>, footer: List<String>) {
        val maxRows = tabListMaxRows
        val outerPadding = 8F

        var currentY = y + outerPadding
        val centerX = x + w / 2F

        if (header.isNotEmpty()) {
            for (line in header) {
                Fonts.fontRegular35.drawCenteredString(line, centerX, currentY, Color.WHITE.rgb)
                currentY += Fonts.fontRegular35.FONT_HEIGHT + 2
            }
            currentY += 2
        }

        if (players.isNotEmpty()) {
            var maxNameWidth = 50F
            players.forEach { it ->
                val fullName = mc.ingameGUI.tabList.getPlayerName(it)
                val wName = Fonts.fontRegular35.getStringWidth(fullName).toFloat()
                if (wName > maxNameWidth) maxNameWidth = wName
            }
            val headSize = 10F
            val padding = 6F
            val spacing = 4F
            val itemHeight = headSize + spacing
            val colWidth = padding + headSize + spacing + maxNameWidth + spacing + 25F + padding
            val totalCols = ceil(players.size.toDouble() / maxRows.toDouble()).toInt()

            val playersTotalWidth = totalCols * colWidth
            var startX = centerX - playersTotalWidth / 2F

            val columnY = currentY

            for (i in players.indices) {
                val player = players[i]

                if (i > 0 && i % maxRows == 0) {
                    startX += colWidth
                    currentY = columnY
                }

                val rowX = startX + padding

                mc.textureManager.bindTexture(player.locationSkin)
                glColor4f(1f, 1f, 1f, 1f)
                Gui.drawScaledCustomSizeModalRect(rowX.toInt(), currentY.toInt(), 8f, 8f, 8, 8, headSize.toInt(), headSize.toInt(), 64f, 64f)

                val fullName = mc.ingameGUI.tabList.getPlayerName(player)
                Fonts.fontRegular35.drawString(fullName, rowX + headSize + spacing, currentY + 1.5F, Color.WHITE.rgb)

                val ping = player.responseTime
                val pingColor = when {
                    ping < 0 -> Color(50, 50, 50)
                    ping < 100 -> Color(100, 255, 100)
                    ping < 200 -> Color(255, 200, 50)
                    else -> Color(255, 80, 80)
                }
                val pingText = "${ping}ms"
                val pingW = Fonts.fontRegular35.getStringWidth(pingText).toFloat()
                Fonts.fontRegular35.drawString(pingText, startX + colWidth - padding - pingW, currentY + 1.5F, pingColor.rgb)

                currentY += itemHeight
            }

            val actualRows = if (totalCols == 1) players.size else maxRows
            currentY = columnY + actualRows * itemHeight + 2
        }

        if (footer.isNotEmpty()) {
            for (line in footer) {
                Fonts.fontRegular35.drawCenteredString(line, centerX, currentY, Color.WHITE.rgb)
                currentY += Fonts.fontRegular35.FONT_HEIGHT + 2
            }
        }
    }

    private fun renderScaffoldContent(x: Float, y: Float, w: Float, h: Float) {
        when (scaffoldStyle) {
            "Classic" -> renderScaffoldClassic(x, y, w, h)
            "Bar" -> renderScaffoldBar(x, y, w, h)
            "Minimal" -> renderScaffoldMinimal(x, y, w, h)
            "Compact" -> renderScaffoldCompact(x, y, w, h)
            "Standard" -> renderScaffoldStandard(x, y, w, h)
            "Circle" -> renderScaffoldCircle(x, y, w, h)
            "Split" -> renderScaffoldSplit(x, y, w, h)
            "Modern" -> renderScaffoldModern(x, y, w, h)
            "Outline" -> renderScaffoldOutline(x, y, w, h)
            else -> renderScaffoldClassic(x, y, w, h)
        }
    }

    private fun renderScaffoldClassic(x: Float, y: Float, w: Float, h: Float) {
        val hotbarBlockCount = (0..8).sumOf { slotIndex ->
            val stack = mc.thePlayer.inventory.getStackInSlot(slotIndex)
            if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        }
        val percentage = (hotbarBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)
        val targetBPS = displayedBPS
        AnimatedBps += (targetBPS - AnimatedBps) * 0.15 * (Minecraft.getDebugFPS() / 20.0).coerceIn(0.1, 2.0)

        val padding = 8F
        val cornerRadius = 6F
        val iconSize = 32F
        val iconBgX = x + padding
        val iconBgY = y + padding
        val themeColor = Color(ScaffoldTheme.red, ScaffoldTheme.green, ScaffoldTheme.blue, 200)
        drawRoundedRect(iconBgX, iconBgY, iconBgX + iconSize, iconBgY + iconSize, themeColor.rgb, cornerRadius - 1)
        val blockImgSize = 24
        drawImage(ResourceLocation("edneko/watermark_images/block.png"), (iconBgX + (iconSize - blockImgSize) / 2).toInt(), (iconBgY + (iconSize - blockImgSize) / 2 + 1).toInt(), blockImgSize, blockImgSize, Color.WHITE)

        val textX = iconBgX + iconSize + 8F
        val titleY = y + padding + 2F
        Fonts.fontSemibold40.drawString("Scaffold Toggled", textX, titleY, Color.WHITE.rgb)
        val bpsText = String.format("%.2f", if(AnimatedBps < 0.01) 0.0 else AnimatedBps)
        Fonts.fontRegular40.drawString("$hotbarBlockCount blocks - $bpsText block/s", textX, titleY + Fonts.fontSemibold40.FONT_HEIGHT + 2F, Color(200, 200, 200).rgb)

        val barHeight = 8F
        val barY = y + h - barHeight - padding
        val maxBarWidth = w - (padding * 2)
        val targetBarWidth = maxBarWidth * percentage
        val lerpSpeed = 0.15f * (Minecraft.getDebugFPS() / 60f).coerceIn(0.5f, 2f)
        ProgressBarAnimationWidth += (targetBarWidth - ProgressBarAnimationWidth) * lerpSpeed
        ProgressBarAnimationWidth = ProgressBarAnimationWidth.coerceIn(0F, maxBarWidth)
        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight, Color(60, 60, 70, 180).rgb, 3F)
        val lighter = Color((ScaffoldTheme.red + 50).coerceAtMost(255), (ScaffoldTheme.green + 50).coerceAtMost(255), (ScaffoldTheme.blue + 50).coerceAtMost(255), 255)
        drawRoundedRect(x + padding, barY, x + padding + ProgressBarAnimationWidth, barY + barHeight, lighter.rgb, 3F)
    }

    private fun renderScaffoldBar(x: Float, y: Float, w: Float, h: Float) {
        val hotbarBlockCount = (0..8).sumOf { slotIndex ->
            val stack = mc.thePlayer.inventory.getStackInSlot(slotIndex)
            if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        }
        val percentage = (hotbarBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)
        val targetBPS = displayedBPS
        AnimatedBps += (targetBPS - AnimatedBps) * 0.15 * (Minecraft.getDebugFPS() / 20.0).coerceIn(0.1, 2.0)

        val padding = 6F
        val barHeight = h - padding * 2
        val maxBarWidth = w - padding * 2
        val targetBarWidth = maxBarWidth * percentage
        val lerpSpeed = 0.15f * (Minecraft.getDebugFPS() / 60f).coerceIn(0.5f, 2f)
        ProgressBarAnimationWidth += (targetBarWidth - ProgressBarAnimationWidth) * lerpSpeed
        ProgressBarAnimationWidth = ProgressBarAnimationWidth.coerceIn(0F, maxBarWidth)

        drawRoundedRect(x + padding, y + padding, x + padding + maxBarWidth, y + padding + barHeight, Color(60, 60, 70, 180).rgb, barHeight / 2)
        val lighter = Color((ScaffoldTheme.red + 50).coerceAtMost(255), (ScaffoldTheme.green + 50).coerceAtMost(255), (ScaffoldTheme.blue + 50).coerceAtMost(255), 255)
        if (ProgressBarAnimationWidth > 1F) {
            drawRoundedRect(x + padding, y + padding, x + padding + ProgressBarAnimationWidth, y + padding + barHeight, lighter.rgb, barHeight / 2)
        }

        val bpsText = String.format("%.1f", if(AnimatedBps < 0.01) 0.0 else AnimatedBps)
        val blockText = "$hotbarBlockCount"
        val bpsFullText = "$bpsText b/s"
        val textWidth = Fonts.fontSemibold35.getStringWidth("$blockText  $bpsFullText")
        val centerX = x + w / 2

        if (ProgressBarAnimationWidth > textWidth + 10F) {
            Fonts.fontSemibold35.drawString(blockText, centerX - textWidth / 2, y + (h - Fonts.fontSemibold35.FONT_HEIGHT) / 2, Color.WHITE.rgb)
            Fonts.fontRegular30.drawString(bpsFullText, centerX - textWidth / 2 + Fonts.fontSemibold35.getStringWidth(blockText) + 6F, y + (h - Fonts.fontRegular30.FONT_HEIGHT) / 2 + 1F, Color(220, 220, 220, 200).rgb)
        } else {
            Fonts.fontSemibold35.drawCenteredString(blockText, centerX, y + (h - Fonts.fontSemibold35.FONT_HEIGHT) / 2, Color.WHITE.rgb)
        }
    }

    private fun renderScaffoldMinimal(x: Float, y: Float, w: Float, h: Float) {
        val hotbarBlockCount = (0..8).sumOf { slotIndex ->
            val stack = mc.thePlayer.inventory.getStackInSlot(slotIndex)
            if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        }
        val percentage = (hotbarBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)

        val padding = 4F
        val barHeight = h - padding * 2
        val maxBarWidth = w - padding * 2
        val targetBarWidth = maxBarWidth * percentage
        val lerpSpeed = 0.15f * (Minecraft.getDebugFPS() / 60f).coerceIn(0.5f, 2f)
        ProgressBarAnimationWidth += (targetBarWidth - ProgressBarAnimationWidth) * lerpSpeed
        ProgressBarAnimationWidth = ProgressBarAnimationWidth.coerceIn(0F, maxBarWidth)

        drawRoundedRect(x + padding, y + padding, x + padding + maxBarWidth, y + padding + barHeight, Color(60, 60, 70, 180).rgb, barHeight / 2)
        val lighter = Color((ScaffoldTheme.red + 50).coerceAtMost(255), (ScaffoldTheme.green + 50).coerceAtMost(255), (ScaffoldTheme.blue + 50).coerceAtMost(255), 255)
        if (ProgressBarAnimationWidth > 1F) {
            drawRoundedRect(x + padding, y + padding, x + padding + ProgressBarAnimationWidth, y + padding + barHeight, lighter.rgb, barHeight / 2)
        }

        val blockText = "$hotbarBlockCount"
        val centerX = x + w / 2
        Fonts.fontSemibold35.drawCenteredString(blockText, centerX, y + (h - Fonts.fontSemibold35.FONT_HEIGHT) / 2, Color.WHITE.rgb)
    }

    private fun renderScaffoldCompact(x: Float, y: Float, w: Float, h: Float) {
        val hotbarBlockCount = (0..8).sumOf { slotIndex ->
            val stack = mc.thePlayer.inventory.getStackInSlot(slotIndex)
            if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        }
        val percentage = (hotbarBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)
        val targetBPS = displayedBPS
        AnimatedBps += (targetBPS - AnimatedBps) * 0.15 * (Minecraft.getDebugFPS() / 20.0).coerceIn(0.1, 2.0)

        val padding = 6F
        val iconSize = 20F
        val iconBgX = x + padding
        val iconBgY = y + 6F
        val themeColor = Color(ScaffoldTheme.red, ScaffoldTheme.green, ScaffoldTheme.blue, 200)
        drawRoundedRect(iconBgX, iconBgY, iconBgX + iconSize, iconBgY + iconSize, themeColor.rgb, 4F)
        val blockImgSize = 14
        drawImage(ResourceLocation("edneko/watermark_images/block.png"), (iconBgX + (iconSize - blockImgSize) / 2).toInt(), (iconBgY + (iconSize - blockImgSize) / 2 + 1).toInt(), blockImgSize, blockImgSize, Color.WHITE)

        val textX = iconBgX + iconSize + 6F
        val bpsText = String.format("%.1f", if(AnimatedBps < 0.01) 0.0 else AnimatedBps)
        Fonts.fontSemibold35.drawString("$hotbarBlockCount blocks", textX, iconBgY + 2F, Color.WHITE.rgb)
        Fonts.fontRegular30.drawString("$bpsText b/s", textX + Fonts.fontSemibold35.getStringWidth("$hotbarBlockCount blocks") + 8F, iconBgY + 3F, Color(200, 200, 200, 200).rgb)

        val barHeight = 4F
        val barY = y + h - barHeight - padding + 4F
        val maxBarWidth = w - padding * 2
        val targetBarWidth = maxBarWidth * percentage
        val lerpSpeed = 0.15f * (Minecraft.getDebugFPS() / 60f).coerceIn(0.5f, 2f)
        ProgressBarAnimationWidth += (targetBarWidth - ProgressBarAnimationWidth) * lerpSpeed
        ProgressBarAnimationWidth = ProgressBarAnimationWidth.coerceIn(0F, maxBarWidth)
        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight, Color(60, 60, 70, 180).rgb, 2F)
        val lighter = Color((ScaffoldTheme.red + 50).coerceAtMost(255), (ScaffoldTheme.green + 50).coerceAtMost(255), (ScaffoldTheme.blue + 50).coerceAtMost(255), 255)
        drawRoundedRect(x + padding, barY, x + padding + ProgressBarAnimationWidth, barY + barHeight, lighter.rgb, 2F)
    }

    private fun renderScaffoldStandard(x: Float, y: Float, w: Float, h: Float) {
        val hotbarBlockCount = (0..8).sumOf { slotIndex ->
            val stack = mc.thePlayer.inventory.getStackInSlot(slotIndex)
            if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        }
        val percentage = (hotbarBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)

        val padding = 6F
        val barHeight = h - padding * 2
        val maxBarWidth = w - padding * 2
        val targetBarWidth = maxBarWidth * percentage
        val lerpSpeed = 0.15f * (Minecraft.getDebugFPS() / 60f).coerceIn(0.5f, 2f)
        ProgressBarAnimationWidth += (targetBarWidth - ProgressBarAnimationWidth) * lerpSpeed
        ProgressBarAnimationWidth = ProgressBarAnimationWidth.coerceIn(0F, maxBarWidth)

        drawRoundedRect(x + padding, y + padding, x + padding + maxBarWidth, y + padding + barHeight, Color(60, 60, 70, 180).rgb, 0F)
        val themeColor = Color(ScaffoldTheme.red, ScaffoldTheme.green, ScaffoldTheme.blue, 255)
        if (ProgressBarAnimationWidth > 1F) {
            drawRoundedRect(x + padding, y + padding, x + padding + ProgressBarAnimationWidth, y + padding + barHeight, themeColor.rgb, 0F)
        }

        if (standardShowBlockCount) {
            val blockText = "$hotbarBlockCount blocks"
            val blockWidth = Fonts.fontSemibold35.getStringWidth(blockText) + 12F
            val textY = y + (h - Fonts.fontSemibold35.FONT_HEIGHT) / 2

            if (ProgressBarAnimationWidth > blockWidth + 10F) {
                Fonts.fontSemibold35.drawString(blockText, x + padding + 8F, textY, Color.WHITE.rgb)
            } else {
                Fonts.fontSemibold35.drawCenteredString(blockText, x + w / 2, textY, Color.WHITE.rgb)
            }
        }
    }

    private fun renderScaffoldCircle(x: Float, y: Float, w: Float, h: Float) {
        val hotbarBlockCount = (0..8).sumOf { slotIndex ->
            val stack = mc.thePlayer.inventory.getStackInSlot(slotIndex)
            if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        }
        val percentage = (hotbarBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)

        val centerX = x + w / 2
        val centerY = y + h / 2
        val radius = min(w, h) / 2 - 12F
        val ringWidth = 5F

        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)

        glColor4f(0.24f, 0.24f, 0.27f, 0.8f)
        glLineWidth(ringWidth)
        glBegin(GL_LINE_LOOP)
        for (i in 0..360) {
            val theta = i * Math.PI / 180
            glVertex2d(centerX + radius * cos(theta), centerY + radius * sin(theta))
        }
        glEnd()

        val themeColor = Color(ScaffoldTheme.red, ScaffoldTheme.green, ScaffoldTheme.blue, 255)
        glColor4f(themeColor.red / 255f, themeColor.green / 255f, themeColor.blue / 255f, 1f)
        glBegin(GL_LINE_STRIP)
        val segments = 360
        val drawSegments = (segments * percentage).toInt()
        for (i in 0..drawSegments) {
            val theta = (i - 90) * Math.PI / 180
            glVertex2d(centerX + radius * cos(theta), centerY + radius * sin(theta))
        }
        glEnd()

        glDisable(GL_LINE_SMOOTH)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()

        val blockText = "$hotbarBlockCount"
        Fonts.fontSemibold40.drawCenteredString(blockText, centerX, centerY - Fonts.fontSemibold40.FONT_HEIGHT / 2, Color.WHITE.rgb)
    }

    private fun renderScaffoldSplit(x: Float, y: Float, w: Float, h: Float) {
        val hotbarBlockCount = (0..8).sumOf { slotIndex ->
            val stack = mc.thePlayer.inventory.getStackInSlot(slotIndex)
            if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        }
        val percentage = (hotbarBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)
        val targetBPS = displayedBPS
        AnimatedBps += (targetBPS - AnimatedBps) * 0.15 * (Minecraft.getDebugFPS() / 20.0).coerceIn(0.1, 2.0)

        val padding = 6F
        val barHeight = 4F
        val barY = y + h - barHeight - padding
        val contentHeight = barY - y - padding - 4F

        val dividerX = x + w / 2
        drawRoundedRect(dividerX - 1F, y + padding, dividerX + 1F, barY - 4F, Color(80, 80, 90, 150).rgb, 1F)

        val leftCenterX = x + w / 4
        val rightCenterX = x + w * 3 / 4

        Fonts.fontSemibold40.drawCenteredString("$hotbarBlockCount", leftCenterX, y + contentHeight / 2 - 2F, Color.WHITE.rgb)
        Fonts.fontRegular30.drawCenteredString("blocks", leftCenterX, y + contentHeight / 2 + Fonts.fontSemibold40.FONT_HEIGHT, Color(180, 180, 180).rgb)

        val bpsText = String.format("%.1f", if(AnimatedBps < 0.01) 0.0 else AnimatedBps)
        Fonts.fontSemibold40.drawCenteredString(bpsText, rightCenterX, y + contentHeight / 2 - 2F, Color.WHITE.rgb)
        Fonts.fontRegular30.drawCenteredString("b/s", rightCenterX, y + contentHeight / 2 + Fonts.fontSemibold40.FONT_HEIGHT, Color(180, 180, 180).rgb)

        val maxBarWidth = w - padding * 2
        val targetBarWidth = maxBarWidth * percentage
        val lerpSpeed = 0.15f * (Minecraft.getDebugFPS() / 60f).coerceIn(0.5f, 2f)
        ProgressBarAnimationWidth += (targetBarWidth - ProgressBarAnimationWidth) * lerpSpeed
        ProgressBarAnimationWidth = ProgressBarAnimationWidth.coerceIn(0F, maxBarWidth)
        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight, Color(60, 60, 70, 180).rgb, 2F)
        val lighter = Color((ScaffoldTheme.red + 50).coerceAtMost(255), (ScaffoldTheme.green + 50).coerceAtMost(255), (ScaffoldTheme.blue + 50).coerceAtMost(255), 255)
        drawRoundedRect(x + padding, barY, x + padding + ProgressBarAnimationWidth, barY + barHeight, lighter.rgb, 2F)
    }

    private fun renderScaffoldModern(x: Float, y: Float, w: Float, h: Float) {
        val hotbarBlockCount = (0..8).sumOf { slotIndex ->
            val stack = mc.thePlayer.inventory.getStackInSlot(slotIndex)
            if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        }
        val percentage = (hotbarBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)
        val targetBPS = displayedBPS
        AnimatedBps += (targetBPS - AnimatedBps) * 0.15 * (Minecraft.getDebugFPS() / 20.0).coerceIn(0.1, 2.0)

        val padding = 10F
        val themeColor = Color(ScaffoldTheme.red, ScaffoldTheme.green, ScaffoldTheme.blue, 40)
        drawRoundedRect(x + padding, y + padding, x + w - padding, y + h - padding, themeColor.rgb, 8F)

        val blockText = "$hotbarBlockCount"
        val centerX = x + w / 2
        val numberY = y + h / 2 - Fonts.font52.FONT_HEIGHT / 2 - 12F

        Fonts.font52.drawCenteredString(blockText, centerX, numberY, Color.WHITE.rgb)

        val labelY = numberY + Fonts.font52.FONT_HEIGHT + 2F
        Fonts.fontSemibold35.drawCenteredString("BLOCKS", centerX, labelY, Color(180, 180, 180).rgb)

        val bpsText = String.format("%.1f b/s", if(AnimatedBps < 0.01) 0.0 else AnimatedBps)
        Fonts.fontRegular30.drawCenteredString(bpsText, centerX, labelY + Fonts.fontSemibold35.FONT_HEIGHT + 2F, Color(150, 150, 150).rgb)

        val barHeight = 5F
        val barY = y + h - barHeight - padding - 2F
        val maxBarWidth = w - padding * 2
        val targetBarWidth = maxBarWidth * percentage
        val lerpSpeed = 0.15f * (Minecraft.getDebugFPS() / 60f).coerceIn(0.5f, 2f)
        ProgressBarAnimationWidth += (targetBarWidth - ProgressBarAnimationWidth) * lerpSpeed
        ProgressBarAnimationWidth = ProgressBarAnimationWidth.coerceIn(0F, maxBarWidth)
        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight, Color(40, 40, 50, 200).rgb, 3F)
        val lighter = Color(ScaffoldTheme.red, ScaffoldTheme.green, ScaffoldTheme.blue, 255)
        drawRoundedRect(x + padding, barY, x + padding + ProgressBarAnimationWidth, barY + barHeight, lighter.rgb, 3F)
    }

    private fun renderScaffoldOutline(x: Float, y: Float, w: Float, h: Float) {
        val hotbarBlockCount = (0..8).sumOf { slotIndex ->
            val stack = mc.thePlayer.inventory.getStackInSlot(slotIndex)
            if (stack != null && stack.item is ItemBlock) stack.stackSize else 0
        }
        val percentage = (hotbarBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)
        val targetBPS = displayedBPS
        AnimatedBps += (targetBPS - AnimatedBps) * 0.15 * (Minecraft.getDebugFPS() / 20.0).coerceIn(0.1, 2.0)

        val padding = 6F
        val innerPadding = 4F
        val themeColor = Color(ScaffoldTheme.red, ScaffoldTheme.green, ScaffoldTheme.blue, 255)

        RenderUtils.drawRoundedBorderRect(x + padding, y + padding, x + w - padding, y + h - padding, 
            2F, Color(0, 0, 0, 0).rgb, themeColor.rgb, 6F)

        val centerX = x + w / 2
        val textY = y + (h - Fonts.fontSemibold35.FONT_HEIGHT) / 2

        val blockText = "$hotbarBlockCount blocks"
        val bpsText = String.format("%.1f b/s", if(AnimatedBps < 0.01) 0.0 else AnimatedBps)
        val separator = "  |  "
        val fullText = "$blockText$separator$bpsText"

        Fonts.fontSemibold35.drawCenteredString(fullText, centerX, textY, Color.WHITE.rgb)

        val barHeight = 3F
        val barY = y + h - barHeight - padding - innerPadding
        val maxBarWidth = w - padding * 2 - innerPadding * 2
        val targetBarWidth = maxBarWidth * percentage
        val lerpSpeed = 0.15f * (Minecraft.getDebugFPS() / 60f).coerceIn(0.5f, 2f)
        ProgressBarAnimationWidth += (targetBarWidth - ProgressBarAnimationWidth) * lerpSpeed
        ProgressBarAnimationWidth = ProgressBarAnimationWidth.coerceIn(0F, maxBarWidth)

        drawRoundedRect(x + padding + innerPadding, barY, x + padding + innerPadding + maxBarWidth, barY + barHeight, Color(60, 60, 70, 150).rgb, 2F)
        if (ProgressBarAnimationWidth > 1F) {
            drawRoundedRect(x + padding + innerPadding, barY, x + padding + innerPadding + ProgressBarAnimationWidth, barY + barHeight, themeColor.rgb, 2F)
        }
    }

    private fun renderNotificationStack(x: Float, y: Float, w: Float, h: Float) {
        var currentYOffset = 0F
        val centerXOffset = 10F
        for (notify in notifications) {
            val rowY = y + currentYOffset
            notify.draw(x + centerXOffset, rowY)
            currentYOffset += ITEM_NOTIFY_HEIGHT
        }
    }

    private fun renderNormal3Content(x: Float, y: Float, w: Float, h: Float) {
        val info = calcNormal3Info()
        val textBaseY = y + (h - Fonts.fontSemibold40.FONT_HEIGHT) / 2 + Fonts.fontSemibold40.FONT_HEIGHT - 8
        val iconSize = 15F
        val iconY = y + (h - iconSize) / 2
        var cx = x + info.padding

        val colorRGB = Color(ColorA_, ColorB_, ColorC_, 255)
        drawImage(getLogoResource(), cx, iconY, 15, 15, colorRGB)
        cx += 15F + info.elementSpacing
        Fonts.fontSemibold40.drawString(ClientName, cx, textBaseY, colorRGB.rgb)
        cx += info.clientNameWidth + info.dotSpacing
        drawCenteredDot(cx, textBaseY)
        cx += 4F
        drawImage(getUserResource(), cx, iconY, 15, 15, Color.WHITE)
        cx += 15F + info.elementSpacing
        Fonts.fontSemibold40.drawString(info.username, cx - 1F, textBaseY, Color.WHITE.rgb)
        cx += info.usernameWidth + info.dotSpacing
        
        val showInsideLyric = showLyricOnIsland && lyricDisplayMode == "Inside" && 
            MusicPlayer.isCurrentlyPlaying && 
            (MusicPlayer.currentLyricDisplay.isNotEmpty() || MusicPlayer.currentMusicName != "None")
        
        if (showInsideLyric) {
            val currentLyric = MusicPlayer.currentLyricDisplay
            val currentMusicName = MusicPlayer.currentMusicName
            val displayText = if (lyricShowMusicName && currentMusicName != "None") {
                currentMusicName
            } else if (currentLyric.isNotEmpty()) {
                currentLyric.take(15)
            } else {
                ""
            }
            
            if (displayText.isNotEmpty()) {
                val font = if (lyricShowMusicName && currentMusicName != "None") getMusicNameFont() else getLyricTextFont()
                val lyricWidth = font.getStringWidth(displayText)
                
                if (lyricBounce) {
                    val (nextInside, vI) = spring(animInsideWidth, lyricWidth + 20F, velInsideWidth)
                    animInsideWidth = nextInside.coerceIn(0F, 300F)
                    velInsideWidth = vI
                    val (nextAlpha, vA) = spring(animBubbleAlpha, 1F, velBubbleAlpha)
                    animBubbleAlpha = nextAlpha.coerceIn(0F, 1F)
                    velBubbleAlpha = vA
                } else {
                    animInsideWidth = lyricWidth + 20F
                    animBubbleAlpha = 1F
                }
                
                if (animInsideWidth > 5F && animBubbleAlpha > 0.01F) {
                    val themeColor = ClientThemesUtils.getColor()
                    val lyricStartX = cx
                    val lyricEndX = cx + animInsideWidth
                    
                    drawRoundedRect(lyricStartX, y + 4F, lyricEndX, y + h - 4F, 
                        Color(30, 30, 35, (lyricBackgroundAlpha * animBubbleAlpha).toInt()).rgb, 5F)
                    
                    val useGradient = lyricColorMode == "Theme" && lyricGradientMode != "Sync"
                    val (gradientX, gradientY) = when {
                        !useGradient -> 0f to 0f
                        lyricGradientMode == "LeftToRight" -> 0.002f to 0f
                        else -> -0.002f to 0f
                    }
                    val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
                    val gradientSpeed = ClientThemesUtils.ThemeFadeSpeed / 5f
                    val gradientColors = if (useGradient) {
                        val startColor = ClientThemesUtils.setColor("start", 255)
                        val endColor = ClientThemesUtils.setColor("end", 255)
                        if (lyricGradientMode == "LeftToRight") {
                            listOf(
                                floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f),
                                floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f)
                            )
                        } else {
                            listOf(
                                floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f),
                                floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f)
                            )
                        }
                    } else null
                    
                    val textX = lyricStartX + (animInsideWidth - lyricWidth) / 2
                    val textColor = when {
                        lyricColorMode == "Custom" -> Color(lyricCustomColor.red, lyricCustomColor.green, lyricCustomColor.blue, (lyricTextAlpha * animBubbleAlpha).toInt()).rgb
                        else -> Color(themeColor.red, themeColor.green, themeColor.blue, (lyricTextAlpha * animBubbleAlpha).toInt()).rgb
                    }
                    
                    GradientFontShader.begin(useGradient, gradientX, gradientY, gradientColors ?: emptyList(), gradientSpeed, gradientOffset).use {
                        font.drawString(displayText, textX, textBaseY, if (useGradient) 0 else textColor)
                    }
                    
                    cx = lyricEndX + info.dotSpacing
                }
            }
        } else {
            if (lyricBounce) {
                val (nextInside, vI) = spring(animInsideWidth, 0F, velInsideWidth)
                animInsideWidth = nextInside.coerceIn(0F, 300F)
                velInsideWidth = vI
                val (nextAlpha, vA) = spring(animBubbleAlpha, 0F, velBubbleAlpha)
                animBubbleAlpha = nextAlpha.coerceIn(0F, 1F)
                velBubbleAlpha = vA
            } else {
                animInsideWidth = 0F
                animBubbleAlpha = 0F
            }
        }
        
        drawCenteredDot(cx, textBaseY)
        cx += 4F
        drawImage(getPingResource(), cx, iconY, 15, 15, Color.GREEN)
        cx += 15F + info.elementSpacing
        Fonts.fontSemibold40.drawString(info.pingStr, cx, textBaseY, Color.GREEN.rgb)
        cx += info.pingTextWidth
        Fonts.fontSemibold40.drawString("  to  ", cx, textBaseY, Color.WHITE.rgb)
        cx += info.toTextWidth
        Fonts.fontSemibold40.drawString(info.ipStr, cx, textBaseY, Color.WHITE.rgb)
        cx += info.serverIpWidth + info.dotSpacing
        drawCenteredDot(cx - 1, textBaseY)
        cx += 3F
        drawImage(ResourceLocation("edneko/watermark_images/fps.png"), cx, iconY, 15, 15, Color.WHITE)
        cx += 15F + info.elementSpacing
        Fonts.fontSemibold40.drawString(info.fpsStr, cx, textBaseY, Color.WHITE.rgb)
    }

    data class Normal3Info(
        val width: Float,
        val padding: Float = 13F,
        val elementSpacing: Float = 8F,
        val dotSpacing: Float = 13F,
        val username: String,
        val clientNameWidth: Float,
        val usernameWidth: Float,
        val pingStr: String,
        val pingTextWidth: Float,
        val toTextWidth: Float,
        val ipStr: String,
        val serverIpWidth: Float,
        val fpsStr: String,
        val fpsTextWidth: Float
    )

    private fun calcNormal3Info(): Normal3Info {
        val username = mc.session?.username ?: "Unknown"
        val fps = Minecraft.getDebugFPS()
        val pings = getSafePing()
        val ipStr = if (customip) ip else ServerUtils.remoteIp ?: "SinglePlayer"
        val clientNameWidth = Fonts.fontSemibold40.getStringWidth(ClientName).toFloat()
        val usernameWidth = Fonts.fontSemibold40.getStringWidth(username).toFloat() - 1f
        val pingStr = "${pings}ms"
        val pingTextWidth = Fonts.fontSemibold40.getStringWidth(pingStr).toFloat()
        val toTextWidth = Fonts.fontSemibold40.getStringWidth("  to  ").toFloat()
        val serverIpWidth = Fonts.fontSemibold40.getStringWidth(ipStr).toFloat()
        val fpsStr = "${fps}fps"
        val fpsTextWidth = Fonts.fontSemibold40.getStringWidth(fpsStr).toFloat()
        val padding = 13F
        val icon = 15F
        val space = 8F
        val dot = 13F
        val dotW = 4F
        
        var lyricWidthExtra = 0F
        val showInsideLyric = showLyricOnIsland && lyricDisplayMode == "Inside" && 
            MusicPlayer.isCurrentlyPlaying && 
            (MusicPlayer.currentLyricDisplay.isNotEmpty() || MusicPlayer.currentMusicName != "None")
        
        if (showInsideLyric) {
            val currentLyric = MusicPlayer.currentLyricDisplay
            val currentMusicName = MusicPlayer.currentMusicName
            val displayText = if (lyricShowMusicName && currentMusicName != "None") {
                currentMusicName
            } else if (currentLyric.isNotEmpty()) {
                currentLyric.take(15)
            } else {
                ""
            }
            if (displayText.isNotEmpty()) {
                val font = if (lyricShowMusicName && currentMusicName != "None") getMusicNameFont() else getLyricTextFont()
                lyricWidthExtra = font.getStringWidth(displayText) + 20F + dot
            }
        }
        
        val w = padding + icon + space + clientNameWidth + dot + dotW +
                icon + space + usernameWidth + dot + dotW +
                lyricWidthExtra +
                icon + space + pingTextWidth + toTextWidth + serverIpWidth + dot + 3F +
                icon + space + fpsTextWidth + padding
        return Normal3Info(w, username=username, clientNameWidth=clientNameWidth, usernameWidth=usernameWidth,
            pingStr=pingStr, pingTextWidth=pingTextWidth, toTextWidth=toTextWidth, ipStr=ipStr, serverIpWidth=serverIpWidth,
            fpsStr=fpsStr, fpsTextWidth=fpsTextWidth)
    }

    private fun calcMaxNotificationWidth(): Float {
        if (notifications.isEmpty()) return 0F
        var maxWidth = 0f
        val fixedElementWidth = 30F + 15F + 30F
        for (notif in notifications) {
            val titleWidth = Fonts.fontSemibold40.getStringWidth(notif.title).toFloat()
            val descWidth = Fonts.fontRegular35.getStringWidth(notif.message).toFloat()
            val textWidth = max(titleWidth, descWidth)
            val totalWidth = fixedElementWidth + textWidth
            maxWidth = max(maxWidth, totalWidth)
        }
        return maxWidth
    }

    private fun drawOldStyleNotifications(startYOffset: Float = 0F) {
        var currentY = start_y + startYOffset
        val padding = 3F
        for(notify in notifications) {
            val tW = Fonts.fontSemibold40.getStringWidth(notify.title)
            val dW = Fonts.fontRegular35.getStringWidth(notify.message)
            val w = 35F + max(tW, dW) + 20F
            val x = (width - w)/2
            drawRoundedBorderRect(x, currentY, x+w, currentY+ITEM_NOTIFY_HEIGHT, 0.2F, Color(0,0,0,BackgroundAlpha).rgb, Color(0,0,0,BackgroundAlpha).rgb, 10F)
            notify.draw(x + padding, currentY + 5F)
            currentY += ITEM_NOTIFY_HEIGHT + 2F
        }
    }

    private fun drawNormal() {
        val username = mc.session.username
        val fps = Minecraft.getDebugFPS()
        val pings = getSafePing()
        val colorRGB = Color(ColorA_, ColorB_, ColorC_, 255)
        val text = " | $username | ${fps}fps | ${pings}ms"
        val mainText = ClientName
        val h = 38F
        val wCalc = 20F + 18F + 5F + Fonts.fontSemibold40.getStringWidth(mainText + text) + 10F
        val x = (width - wCalc)/2
        val y = start_y
        val (nX, _) = spring(AnimGlobalX, x, VelGlobalX)
        AnimGlobalX = nX
        ShowShadow(x, y, wCalc, h)
        drawRoundedBorderRect(x, y, x+wCalc, y+h, 0.5F, Color(10,10,10,BackgroundAlpha).rgb, Color(30,30,30,BackgroundAlpha).rgb, h/2)
        drawImage(ResourceLocation("edneko/logo_icon.png"), (x+5).toInt(), (y + (h-18)/2).toInt(), 18, 18, colorRGB)
        Fonts.fontSemibold40.drawString(mainText, x + 28, y + (h-9)/2+1, colorRGB.rgb)
        Fonts.fontSemibold40.drawString(text, x + 28 + Fonts.fontSemibold40.getStringWidth(mainText), y + (h-9)/2+1, -1)
    }

    private fun drawNormal2() {}
    
    private fun drawCenteredDot(x: Float, textBaseY: Float) {
        val dotY = textBaseY - Fonts.fontSemibold40.FONT_HEIGHT / 2 + 3F
        Fonts.fontSemibold40.drawString("·", x - 3f, dotY, Color(180, 180, 180, 255).rgb)
    }

    private fun ShowShadow(x: Float, y: Float, w: Float, h: Float) {
        if (ShadowCheck) GlowUtils.drawGlow(x, y, w, h, shadowRadiusValue.toInt(), shadowColor)
    }

    fun drawToggleButton(StartX: Float, StartY: Float, ContainerH: Float, ModuleState: Boolean, animationState: SwitchAnimationState) {
        val btnH = 19F
        val btnW = 30F
        val margin = 3F
        val radius = btnH / 2
        val btnStartY = StartY + (ITEM_NOTIFY_HEIGHT - btnH) / 2
        animationState.updateState(ModuleState)
        val anim = animationState.getOutput()
        val trackColor = if (ModuleState) Color(ButtonColor.red, ButtonColor.green, ButtonColor.blue, 255) else Color(45, 45, 45, 255)
        drawRoundedBorderRect(StartX, btnStartY, StartX + btnW, btnStartY + btnH, 0.1f, trackColor.rgb, trackColor.rgb, radius)
        val knobSize = btnH - margin * 2
        val knobX = StartX + margin + (btnW - margin*2 - knobSize) * anim.toFloat()
        val knobColor = if (ModuleState) Color.WHITE.rgb else Color(100, 100, 100, 255).rgb
        drawRoundedBorderRect(knobX, btnStartY + margin, knobX + knobSize, btnStartY + margin + knobSize, 0.1f, knobColor, knobColor, knobSize / 2)
    }

    fun drawToggleText(StartX: Float, StartY: Float, TextBar: Pair<String, String>, ContainerH: Float) {
        val titleH = 9F
        val textStartX = StartX + 30F + 8F
        val center = StartY + ITEM_NOTIFY_HEIGHT / 2
        Fonts.fontSemibold40.drawString(TextBar.first, textStartX, center - titleH + 1F, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString(TextBar.second, textStartX, center + 3F, Color.WHITE.rgb)
    }

    enum class Direction { FORWARDS, BACKWARDS }
    class EaseOutExpo(private val duration: Long, private val end: Double) {
        private var start = 0.0
        private var startTime = System.currentTimeMillis()
        private var direction = Direction.FORWARDS
        fun setDirection(dir: Direction) { if (this.direction != dir) { this.direction = dir; startTime = System.currentTimeMillis(); start = getOutput() } }
        fun getOutput(): Double {
            val progress = (System.currentTimeMillis() - startTime).toDouble() / duration
            val result = when (direction) {
                Direction.FORWARDS -> if (progress >= 1.0) end else (-2.0.pow(-10 * progress) + 1) * end
                Direction.BACKWARDS -> if (progress >= 1.0) 0.0 else (2.0.pow(-10 * progress) * end)
            }
            return result.coerceIn(0.0, end)
        }
    }
    class SwitchAnimationState {
        private val animation = EaseOutExpo(300, 1.0)
        fun updateState(state: Boolean) = animation.setDirection(if (state) Direction.FORWARDS else Direction.BACKWARDS)
        fun getOutput() = animation.getOutput()
    }

    private abstract class Notification(val id: String = UUID.randomUUID().toString(), var title: String, var message: String, var createTime: Long = System.currentTimeMillis(), var duration: Long = 3000L) {
        var isMarkedForDelete = false
        abstract fun draw(x: Float, y: Float)
        open fun updateState(newMsg: String, newEnable: Boolean, newDuration: Long) { this.message = newMsg; this.createTime = System.currentTimeMillis(); this.duration = newDuration }
        fun getHeight() = ITEM_NOTIFY_HEIGHT
        fun update() { if (System.currentTimeMillis() > createTime + duration) isMarkedForDelete = true }
    }
    private class ToggleNotification(t: String, m: String, d: Long, var enabled: Boolean, val moduleName: String) : Notification(title=t, message=m, duration=d) {
        val anim = SwitchAnimationState()
        init { anim.updateState(enabled) }
        override fun updateState(newMsg: String, newEnable: Boolean, newDuration: Long) { super.updateState(newMsg, newEnable, newDuration); this.enabled = newEnable; anim.updateState(newEnable) }
        override fun draw(x: Float, y: Float) { drawToggleButton(x, y, 0F, enabled, anim); drawToggleText(x, y, Pair(title, message), 0F) }
    }

    fun showToggleNotification(title: String, message: String, enabled: Boolean, moduleName: String) {
        val existing = notifications.find { it.moduleName == moduleName }
        val duration = notifyDuration.toLong()
        if (existing != null) existing.updateState(message, enabled, duration)
        else notifications.add(ToggleNotification(title, message, duration, enabled, moduleName))
    }
    private fun updateNotifications() { notifications.forEach { it.update() }; notifications.removeAll { it.isMarkedForDelete } }

    object EmbeddedStencil {
        fun checkSetupFBO(framebuffer: Framebuffer?) {
            if (framebuffer != null && framebuffer.depthBuffer > -1) {
                setupFBO(framebuffer)
                framebuffer.depthBuffer = -1
            }
        }
        fun setupFBO(framebuffer: Framebuffer) {
            EXTFramebufferObject.glDeleteRenderbuffersEXT(framebuffer.depthBuffer)
            val stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT()
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
            EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight)
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
        }
        fun write(invert: Boolean) {
            checkSetupFBO(Minecraft.getMinecraft().framebuffer)
            glClearStencil(0)
            glClear(GL_STENCIL_BUFFER_BIT)
            glEnable(GL_STENCIL_TEST)
            glStencilFunc(GL_ALWAYS, 1, 65535)
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
            if (!invert) {
                glColorMask(false, false, false, false)
                glDepthMask(false)
                glStencilFunc(GL_ALWAYS, 1, 65535)
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
            }
        }
        fun erase(invert: Boolean) {
            glStencilFunc(if (invert) GL_EQUAL else GL_NOTEQUAL, 1, 65535)
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
            if (invert) {
                glColorMask(true, true, true, true)
                glDepthMask(true)
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
            } else {
                glColorMask(true, true, true, true)
                glDepthMask(true)
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
            }
        }
        fun dispose() {
            glDisable(GL_STENCIL_TEST)
        }
    }

    private fun getMusicNameFont() = when (lyricMusicNameFont) {
        "ExtraBold35" -> Fonts.fontExtraBold35
        "ExtraBold40" -> Fonts.fontExtraBold40
        "Semibold35" -> Fonts.fontSemibold35
        "Semibold40" -> Fonts.fontSemibold40
        "Regular30" -> Fonts.fontRegular30
        "Regular35" -> Fonts.fontRegular35
        "Regular40" -> Fonts.fontRegular40
        "Regular45" -> Fonts.fontRegular45
        "Bold180" -> Fonts.fontBold180
        else -> Fonts.fontSemibold35
    }

    private fun getLyricTextFont() = when (lyricTextFont) {
        "ExtraBold35" -> Fonts.fontExtraBold35
        "ExtraBold40" -> Fonts.fontExtraBold40
        "Semibold35" -> Fonts.fontSemibold35
        "Semibold40" -> Fonts.fontSemibold40
        "Regular30" -> Fonts.fontRegular30
        "Regular35" -> Fonts.fontRegular35
        "Regular40" -> Fonts.fontRegular40
        "Regular45" -> Fonts.fontRegular45
        "Bold180" -> Fonts.fontBold180
        else -> Fonts.fontRegular35
    }

    private fun renderLyricDisplay() {
        if (lyricDisplayMode == "Full") return
        
        val isPlaying = MusicPlayer.isCurrentlyPlaying
        val currentLyric = MusicPlayer.currentLyricDisplay
        val currentMusicName = MusicPlayer.currentMusicName

        if (!isPlaying || (currentLyric.isEmpty() && currentMusicName == "None")) {
            if (lyricBounce) {
                val (nextAlpha, vA) = spring(animBubbleAlpha, 0F, velBubbleAlpha)
                animBubbleAlpha = nextAlpha.coerceIn(0F, 1F)
                velBubbleAlpha = vA
                val (nextWidth, vW) = spring(animBubbleWidth, 0F, velBubbleWidth)
                animBubbleWidth = nextWidth.coerceIn(0F, 500F)
                velBubbleWidth = vW
                val (nextHeight, vH) = spring(animBubbleHeight, 0F, velBubbleHeight)
                animBubbleHeight = nextHeight.coerceIn(0F, 200F)
                velBubbleHeight = vH
            } else {
                animBubbleAlpha = 0F
                animBubbleWidth = 0F
                animBubbleHeight = 0F
            }
            return
        }

        when (lyricDisplayMode) {
            "Below" -> renderLyricBelow()
            "Float" -> renderLyricFloat()
            "Full" -> renderLyricFull()
        }
    }

    private fun renderLyricBelow() {
        val currentLyric = MusicPlayer.currentLyricDisplay
        val previousLyric = MusicPlayer.previousLyricDisplay
        val nextLyric = MusicPlayer.nextLyricDisplay
        val currentMusicName = MusicPlayer.currentMusicName
        val musicFont = getMusicNameFont()
        val textFont = getLyricTextFont()
        
        val displayLines = mutableListOf<Pair<String, Boolean>>()
        if (lyricShowMusicName && currentMusicName != "None") {
            displayLines.add(currentMusicName to true)
        }
        if (lyricShowPrevious && previousLyric.isNotEmpty()) {
            displayLines.add(previousLyric to false)
        }
        if (currentLyric.isNotEmpty()) {
            displayLines.add(currentLyric to false)
        }
        if (lyricShowNext && nextLyric.isNotEmpty()) {
            displayLines.add(nextLyric to false)
        }
        
        if (displayLines.isEmpty()) return

        val maxWidth = displayLines.maxOf { (line, isMusic) -> 
            (if (isMusic) musicFont else textFont).getStringWidth(line) + 40F 
        }.coerceIn(150F, 350F)
        val targetHeight = displayLines.size * 18F + 16F + if (lyricShowProgress) 10F else 0F

        if (lyricBounce) {
            val (nextW, vW) = spring(animBubbleWidth, maxWidth, velBubbleWidth)
            animBubbleWidth = nextW.coerceIn(0F, 500F)
            velBubbleWidth = vW
            val (nextH, vH) = spring(animBubbleHeight, targetHeight, velBubbleHeight)
            animBubbleHeight = nextH.coerceIn(0F, 200F)
            velBubbleHeight = vH
            val (nextAlpha, vA) = spring(animBubbleAlpha, 1F, velBubbleAlpha)
            animBubbleAlpha = nextAlpha.coerceIn(0F, 1F)
            velBubbleAlpha = vA
        } else {
            animBubbleWidth = maxWidth
            animBubbleHeight = targetHeight
            animBubbleAlpha = 1F
        }

        if (animBubbleAlpha < 0.01f || animBubbleWidth < 10f || animBubbleHeight < 10f) return

        val islandBottom = AnimGlobalY + AnimGlobalHeight
        val bubbleX = AnimGlobalX
        val bubbleY = islandBottom
        val alpha = (lyricBackgroundAlpha * animBubbleAlpha).toInt()

        if (lyricBlur && blurCheck) {
            try {
                EmbeddedStencil.checkSetupFBO(mc.framebuffer)
                EmbeddedStencil.write(false)
                RenderUtils.drawRoundedRect(bubbleX, bubbleY, bubbleX + AnimGlobalWidth, bubbleY + animBubbleHeight, Color.WHITE.rgb, 8F, RenderUtils.RoundedCorners.BOTTOM_ONLY)
                EmbeddedStencil.erase(true)
                applyBlur(bubbleX, bubbleY, AnimGlobalWidth, animBubbleHeight)
                EmbeddedStencil.dispose()
            } catch (e: Exception) {
            }
        }

        RenderUtils.drawRoundedRect(bubbleX, bubbleY, bubbleX + AnimGlobalWidth, bubbleY + animBubbleHeight, 
                       Color(0, 0, 0, alpha).rgb, 8F, RenderUtils.RoundedCorners.BOTTOM_ONLY)
        
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val themeColor = ClientThemesUtils.getColor()
        val useGradient = lyricColorMode == "Theme" && lyricGradientMode != "Sync"
        val (gradientX, gradientY) = when {
            !useGradient -> 0f to 0f
            lyricGradientMode == "LeftToRight" -> 0.002f to 0f
            else -> -0.002f to 0f
        }
        val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
        val gradientSpeed = ClientThemesUtils.ThemeFadeSpeed / 5f
        val gradientColors = if (useGradient) {
            val startColor = ClientThemesUtils.setColor("start", 255)
            val endColor = ClientThemesUtils.setColor("end", 255)
            if (lyricGradientMode == "LeftToRight") {
                listOf(
                    floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f),
                    floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f)
                )
            } else {
                listOf(
                    floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f),
                    floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f)
                )
            }
        } else null
        
        if (lyricScrollAnimation) {
            val currentFullText = displayLines.joinToString("|") { it.first }
            if (currentFullText != lastLyricText) {
                lastLyricText = currentFullText
                scrollAnimProgress = 18F
                velScrollAnim = 0F
            }
            if (scrollAnimProgress > 0.01F) {
                val animSpeed = 300F / lyricScrollAnimTime
                val (nextScroll, vS) = spring(scrollAnimProgress, 0F, velScrollAnim * animSpeed)
                scrollAnimProgress = nextScroll.coerceIn(0F, 50F)
                velScrollAnim = vS
            }
        }
        
        GradientFontShader.begin(useGradient, gradientX, gradientY, gradientColors ?: emptyList(), gradientSpeed, gradientOffset).use {
            var textY = bubbleY + 8F
            displayLines.forEachIndexed { index, (line, isMusic) ->
                val font = if (isMusic) musicFont else textFont
                val textWidth = font.getStringWidth(line)
                val textX = bubbleX + (AnimGlobalWidth - textWidth) / 2
                val currentLyricIndex = displayLines.indexOfFirst { it.first == currentLyric }
                val lineAlpha = if (index == currentLyricIndex || (currentLyricIndex == -1 && index == displayLines.size - 1)) {
                    (lyricTextAlpha * animBubbleAlpha).toInt()
                } else {
                    (lyricTextAlpha * 0.6 * animBubbleAlpha).toInt()
                }
                val colorToUse = when {
                    useGradient -> 0
                    lyricColorMode == "Theme" -> Color(themeColor.red, themeColor.green, themeColor.blue, lineAlpha).rgb
                    else -> Color(lyricCustomColor.red, lyricCustomColor.green, lyricCustomColor.blue, lineAlpha).rgb
                }
                val actualY = if (isMusic) textY else textY - scrollAnimProgress
                font.drawString(line, textX, actualY, colorToUse)
                textY += 18F
            }
        }
        
        if (lyricShowProgress) {
            val progress = MusicPlayer.progress.coerceIn(0F, 1F)
            val timeStr = MusicPlayer.timeDisplayString
            val barHeight = 4F
            val barY = bubbleY + animBubbleHeight - barHeight - 4F
            val barPadding = 8F
            val timeWidth = Fonts.fontRegular30.getStringWidth(timeStr)
            val timeX = bubbleX + barPadding
            val barStartX = timeX + timeWidth + 6F
            val maxBarWidth = AnimGlobalWidth - barPadding * 2 - timeWidth - 6F
            
            Fonts.fontRegular30.drawString(timeStr, timeX, barY - 1F, 
                Color(180, 180, 190, (lyricTextAlpha * animBubbleAlpha).toInt()).rgb)
            
            drawRoundedRect(barStartX, barY, barStartX + maxBarWidth, barY + barHeight, 
                Color(60, 60, 70, (180 * animBubbleAlpha).toInt()).rgb, 2F)
            
            val progressColor = when {
                lyricColorMode == "Custom" -> Color(lyricCustomColor.red, lyricCustomColor.green, lyricCustomColor.blue, (lyricTextAlpha * animBubbleAlpha).toInt())
                else -> Color(themeColor.red, themeColor.green, themeColor.blue, (lyricTextAlpha * animBubbleAlpha).toInt())
            }
            drawRoundedRect(barStartX, barY, barStartX + maxBarWidth * progress, barY + barHeight, 
                progressColor.rgb, 2F)
        }
    }

    private fun renderLyricFloat() {
        val currentLyric = MusicPlayer.currentLyricDisplay
        val previousLyric = MusicPlayer.previousLyricDisplay
        val nextLyric = MusicPlayer.nextLyricDisplay
        val currentMusicName = MusicPlayer.currentMusicName
        val musicFont = getMusicNameFont()
        val textFont = getLyricTextFont()
        
        val displayLines = mutableListOf<Pair<String, Boolean>>()
        if (lyricShowMusicName && currentMusicName != "None") {
            displayLines.add(currentMusicName to true)
        }
        if (lyricShowPrevious && previousLyric.isNotEmpty()) {
            displayLines.add(previousLyric to false)
        }
        if (currentLyric.isNotEmpty()) {
            displayLines.add(currentLyric to false)
        }
        if (lyricShowNext && nextLyric.isNotEmpty()) {
            displayLines.add(nextLyric to false)
        }
        
        if (displayLines.isEmpty()) return

        val maxWidth = displayLines.maxOf { (line, isMusic) -> 
            (if (isMusic) musicFont else textFont).getStringWidth(line) + 40F 
        }.coerceIn(150F, 350F)
        val targetHeight = displayLines.size * 18F + 16F + if (lyricShowProgress) 10F else 0F

        if (lyricBounce) {
            val (nextW, vW) = spring(animBubbleWidth, maxWidth, velBubbleWidth)
            animBubbleWidth = nextW.coerceIn(0F, 500F)
            velBubbleWidth = vW
            val (nextH, vH) = spring(animBubbleHeight, targetHeight, velBubbleHeight)
            animBubbleHeight = nextH.coerceIn(0F, 200F)
            velBubbleHeight = vH
            val (nextY, vY) = spring(animBubbleY, lyricFloatOffsetY.toFloat(), velBubbleY)
            animBubbleY = nextY
            velBubbleY = vY
            val (nextAlpha, vA) = spring(animBubbleAlpha, 1F, velBubbleAlpha)
            animBubbleAlpha = nextAlpha.coerceIn(0F, 1F)
            velBubbleAlpha = vA
        } else {
            animBubbleWidth = maxWidth
            animBubbleHeight = targetHeight
            animBubbleY = lyricFloatOffsetY.toFloat()
            animBubbleAlpha = 1F
        }

        if (animBubbleAlpha < 0.01f || animBubbleWidth < 10f || animBubbleHeight < 10f) return

        val islandBottom = AnimGlobalY + AnimGlobalHeight
        val bubbleX = (width - animBubbleWidth) / 2
        val bubbleY = islandBottom + animBubbleY
        val alpha = (lyricBackgroundAlpha * animBubbleAlpha).toInt()

        if (lyricBlur && blurCheck) {
            try {
                EmbeddedStencil.checkSetupFBO(mc.framebuffer)
                EmbeddedStencil.write(false)
                drawRoundedRect(bubbleX, bubbleY, bubbleX + animBubbleWidth, bubbleY + animBubbleHeight, Color.WHITE.rgb, 8F)
                EmbeddedStencil.erase(true)
                applyBlur(bubbleX, bubbleY, animBubbleWidth, animBubbleHeight)
                EmbeddedStencil.dispose()
            } catch (e: Exception) {
            }
        }

        drawRoundedRect(bubbleX, bubbleY, bubbleX + animBubbleWidth, bubbleY + animBubbleHeight, 
                       Color(0, 0, 0, alpha).rgb, 8F)
        
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val themeColor = ClientThemesUtils.getColor()
        val useGradient = lyricColorMode == "Theme" && lyricGradientMode != "Sync"
        val (gradientX, gradientY) = when {
            !useGradient -> 0f to 0f
            lyricGradientMode == "LeftToRight" -> 0.002f to 0f
            else -> -0.002f to 0f
        }
        val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
        val gradientSpeed = ClientThemesUtils.ThemeFadeSpeed / 5f
        val gradientColors = if (useGradient) {
            val startColor = ClientThemesUtils.setColor("start", 255)
            val endColor = ClientThemesUtils.setColor("end", 255)
            if (lyricGradientMode == "LeftToRight") {
                listOf(
                    floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f),
                    floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f)
                )
            } else {
                listOf(
                    floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f),
                    floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f)
                )
            }
        } else null
        
        if (lyricScrollAnimation) {
            val currentFullText = displayLines.joinToString("|") { it.first }
            if (currentFullText != lastLyricText) {
                lastLyricText = currentFullText
                scrollAnimProgress = 18F
                velScrollAnim = 0F
            }
            if (scrollAnimProgress > 0.01F) {
                val animSpeed = 300F / lyricScrollAnimTime
                val (nextScroll, vS) = spring(scrollAnimProgress, 0F, velScrollAnim * animSpeed)
                scrollAnimProgress = nextScroll.coerceIn(0F, 50F)
                velScrollAnim = vS
            }
        }
        
        GradientFontShader.begin(useGradient, gradientX, gradientY, gradientColors ?: emptyList(), gradientSpeed, gradientOffset).use {
            var textY = bubbleY + 8F
            displayLines.forEachIndexed { index, (line, isMusic) ->
                val font = if (isMusic) musicFont else textFont
                val textWidth = font.getStringWidth(line)
                val textX = bubbleX + (animBubbleWidth - textWidth) / 2
                val currentLyricIndex = displayLines.indexOfFirst { it.first == currentLyric }
                val lineAlpha = if (index == currentLyricIndex || (currentLyricIndex == -1 && index == displayLines.size - 1)) {
                    (lyricTextAlpha * animBubbleAlpha).toInt()
                } else {
                    (lyricTextAlpha * 0.6 * animBubbleAlpha).toInt()
                }
                val colorToUse = when {
                    useGradient -> 0
                    lyricColorMode == "Theme" -> Color(themeColor.red, themeColor.green, themeColor.blue, lineAlpha).rgb
                    else -> Color(lyricCustomColor.red, lyricCustomColor.green, lyricCustomColor.blue, lineAlpha).rgb
                }
                val actualY = if (isMusic) textY else textY - scrollAnimProgress
                font.drawString(line, textX, actualY, colorToUse)
                textY += 18F
            }
        }
        
        if (lyricShowProgress) {
            val progress = MusicPlayer.progress.coerceIn(0F, 1F)
            val timeStr = MusicPlayer.timeDisplayString
            val barHeight = 4F
            val barY = bubbleY + animBubbleHeight - barHeight - 4F
            val barPadding = 8F
            val timeWidth = Fonts.fontRegular30.getStringWidth(timeStr)
            val timeX = bubbleX + barPadding
            val barStartX = timeX + timeWidth + 6F
            val maxBarWidth = animBubbleWidth - barPadding * 2 - timeWidth - 6F
            
            Fonts.fontRegular30.drawString(timeStr, timeX, barY - 1F, 
                Color(180, 180, 190, (lyricTextAlpha * animBubbleAlpha).toInt()).rgb)
            
            drawRoundedRect(barStartX, barY, barStartX + maxBarWidth, barY + barHeight, 
                Color(60, 60, 70, (180 * animBubbleAlpha).toInt()).rgb, 2F)
            
            val progressColor = when {
                lyricColorMode == "Custom" -> Color(lyricCustomColor.red, lyricCustomColor.green, lyricCustomColor.blue, (lyricTextAlpha * animBubbleAlpha).toInt())
                else -> Color(themeColor.red, themeColor.green, themeColor.blue, (lyricTextAlpha * animBubbleAlpha).toInt())
            }
            drawRoundedRect(barStartX, barY, barStartX + maxBarWidth * progress, barY + barHeight, 
                progressColor.rgb, 2F)
        }
    }

    private var lastFullLyricText = ""
    private var fullLyricAnimStartTime = 0L
    private var fullLyricAnimProgress = 0F

    private fun renderLyricFullContent(x: Float, y: Float, w: Float, h: Float) {
        val currentLyric = MusicPlayer.currentLyricDisplay ?: ""
        val textFont = getLyricTextFont() ?: return
        val themeColor = ClientThemesUtils.getColor() ?: return
        
        if (currentLyric.isEmpty()) return
        
        if (currentLyric != lastFullLyricText) {
            lastFullLyricText = currentLyric
            fullLyricAnimStartTime = System.currentTimeMillis()
            fullLyricAnimProgress = 0F
        }
        
        val animDuration = lyricFullAnimTime.toFloat()
        val elapsed = (System.currentTimeMillis() - fullLyricAnimStartTime).toFloat()
        fullLyricAnimProgress = (elapsed / animDuration).coerceIn(0F, 1F)
        
        val textWidth = textFont.getStringWidth(currentLyric)
        val textX = x + (w - textWidth) / 2
        val textY = y + (h - textFont.height) / 2 - if (lyricShowProgress) 8F else 0F
        
        val colorToUse = when (lyricColorMode) {
            "Theme" -> Color(themeColor.red, themeColor.green, themeColor.blue, 255)
            else -> Color(lyricCustomColor.red, lyricCustomColor.green, lyricCustomColor.blue, 255)
        }
        
        glPushMatrix()
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        when (lyricFullAnimation) {
            "None" -> {
                textFont.drawString(currentLyric, textX, textY, colorToUse.rgb)
            }
            "Fade" -> {
                val alpha = (fullLyricAnimProgress * 255).toInt()
                val fadeColor = Color(colorToUse.red, colorToUse.green, colorToUse.blue, alpha)
                textFont.drawString(currentLyric, textX, textY, fadeColor.rgb)
            }
            "SlideLeft" -> {
                val offsetX = (1F - fullLyricAnimProgress) * w * 0.3F
                textFont.drawString(currentLyric, textX - offsetX, textY, colorToUse.rgb)
            }
            "SlideRight" -> {
                val offsetX = (1F - fullLyricAnimProgress) * w * 0.3F
                textFont.drawString(currentLyric, textX + offsetX, textY, colorToUse.rgb)
            }
            "SlideUp" -> {
                val offsetY = (1F - fullLyricAnimProgress) * h * 0.5F
                textFont.drawString(currentLyric, textX, textY + offsetY, colorToUse.rgb)
            }
            "SlideDown" -> {
                val offsetY = (1F - fullLyricAnimProgress) * h * 0.5F
                textFont.drawString(currentLyric, textX, textY - offsetY, colorToUse.rgb)
            }
            "Scale" -> {
                val scale = 0.5F + fullLyricAnimProgress * 0.5F
                glTranslatef(textX + textWidth / 2, textY + textFont.height / 2, 0F)
                glScalef(scale, scale, 1F)
                glTranslatef(-(textX + textWidth / 2), -(textY + textFont.height / 2), 0F)
                val alpha = (fullLyricAnimProgress * 255).toInt()
                val scaleColor = Color(colorToUse.red, colorToUse.green, colorToUse.blue, alpha)
                textFont.drawString(currentLyric, textX, textY, scaleColor.rgb)
            }
            "Typewriter" -> {
                val visibleChars = (fullLyricAnimProgress * currentLyric.length).toInt().coerceIn(0, currentLyric.length)
                val displayText = currentLyric.substring(0, visibleChars)
                textFont.drawString(displayText, textX, textY, colorToUse.rgb)
            }
            else -> {
                textFont.drawString(currentLyric, textX, textY, colorToUse.rgb)
            }
        }
        
        glPopMatrix()
        
        if (lyricShowProgress) {
            val progress = MusicPlayer.progress.coerceIn(0F, 1F)
            val timeStr = MusicPlayer.timeDisplayString ?: "0:00 / 0:00"
            val barHeight = 4F
            val barY = y + h - barHeight - 8F
            val barPadding = 8F
            val fontRegular30 = Fonts.fontRegular30 ?: return
            val timeWidth = fontRegular30.getStringWidth(timeStr)
            val timeX = x + barPadding
            val barStartX = timeX + timeWidth + 6F
            val maxBarWidth = w - barPadding * 2 - timeWidth - 6F
            
            fontRegular30.drawString(timeStr, timeX, barY - 1F, 
                Color(180, 180, 190, 200).rgb)
            
            drawRoundedRect(barStartX, barY, barStartX + maxBarWidth, barY + barHeight, 
                Color(60, 60, 70, 180).rgb, 2F)
            
            val progressColor = when (lyricColorMode) {
                "Custom" -> Color(lyricCustomColor.red, lyricCustomColor.green, lyricCustomColor.blue, 200)
                else -> Color(themeColor.red, themeColor.green, themeColor.blue, 200)
            }
            drawRoundedRect(barStartX, barY, barStartX + maxBarWidth * progress, barY + barHeight, 
                progressColor.rgb, 2F)
        }
    }

    private fun renderLyricFull() {
        val currentLyric = MusicPlayer.currentLyricDisplay ?: ""
        val previousLyric = MusicPlayer.previousLyricDisplay ?: ""
        val nextLyric = MusicPlayer.nextLyricDisplay ?: ""
        val currentMusicName = MusicPlayer.currentMusicName ?: "None"
        val musicFont = getMusicNameFont() ?: return
        val textFont = getLyricTextFont() ?: return
        
        val displayLines = mutableListOf<Pair<String, Boolean>>()
        if (lyricShowMusicName && currentMusicName != "None") {
            displayLines.add(currentMusicName to true)
        }
        if (lyricShowPrevious && previousLyric.isNotEmpty()) {
            displayLines.add(previousLyric to false)
        }
        if (currentLyric.isNotEmpty()) {
            displayLines.add(currentLyric to false)
        }
        if (lyricShowNext && nextLyric.isNotEmpty()) {
            displayLines.add(nextLyric to false)
        }
        
        if (displayLines.isEmpty()) return

        val maxWidth = displayLines.maxOf { (line, isMusic) -> 
            (if (isMusic) musicFont else textFont).getStringWidth(line) + 40F 
        }.coerceIn(100F, lyricFullWidth.toFloat())
        
        val lineCount = displayLines.size
        val targetHeight = (lineCount * 18F + 16F + if (lyricShowProgress) 10F else 0F).coerceIn(30F, lyricFullHeight.toFloat())

        if (lyricBounce) {
            val (nextW, vW) = spring(animBubbleWidth, maxWidth, velBubbleWidth)
            animBubbleWidth = nextW.coerceIn(0F, 500F)
            velBubbleWidth = vW
            val (nextH, vH) = spring(animBubbleHeight, targetHeight, velBubbleHeight)
            animBubbleHeight = nextH.coerceIn(0F, 200F)
            velBubbleHeight = vH
            val (nextY, vY) = spring(animBubbleY, lyricFloatOffsetY.toFloat(), velBubbleY)
            animBubbleY = nextY
            velBubbleY = vY
            val (nextAlpha, vA) = spring(animBubbleAlpha, 1F, velBubbleAlpha)
            animBubbleAlpha = nextAlpha.coerceIn(0F, 1F)
            velBubbleAlpha = vA
        } else {
            animBubbleWidth = maxWidth
            animBubbleHeight = targetHeight
            animBubbleY = lyricFloatOffsetY.toFloat()
            animBubbleAlpha = 1F
        }

        if (animBubbleAlpha < 0.01f || animBubbleWidth < 10f || animBubbleHeight < 10f) return

        val bubbleX = (width - animBubbleWidth) / 2
        val bubbleY = animBubbleY
        val alpha = (lyricBackgroundAlpha * animBubbleAlpha).toInt()

        if (lyricBlur && blurCheck) {
            try {
                EmbeddedStencil.checkSetupFBO(mc.framebuffer)
                EmbeddedStencil.write(false)
                drawRoundedRect(bubbleX, bubbleY, bubbleX + animBubbleWidth, bubbleY + animBubbleHeight, Color.WHITE.rgb, 14F)
                EmbeddedStencil.erase(true)
                applyBlur(bubbleX, bubbleY, animBubbleWidth, animBubbleHeight)
                EmbeddedStencil.dispose()
            } catch (e: Exception) {
            }
        }

        drawRoundedRect(bubbleX, bubbleY, bubbleX + animBubbleWidth, bubbleY + animBubbleHeight, 
                       Color(0, 0, 0, alpha).rgb, 14F)
        
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val themeColor = ClientThemesUtils.getColor()
        val useGradient = lyricColorMode == "Theme" && lyricGradientMode != "Sync"
        val (gradientX, gradientY) = when {
            !useGradient -> 0f to 0f
            lyricGradientMode == "LeftToRight" -> 0.002f to 0f
            else -> -0.002f to 0f
        }
        val gradientOffset = System.currentTimeMillis() % 10000 / 10000F
        val gradientSpeed = ClientThemesUtils.ThemeFadeSpeed / 5f
        val gradientColors = if (useGradient) {
            val startColor = ClientThemesUtils.setColor("start", 255)
            val endColor = ClientThemesUtils.setColor("end", 255)
            if (lyricGradientMode == "LeftToRight") {
                listOf(
                    floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f),
                    floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f)
                )
            } else {
                listOf(
                    floatArrayOf(endColor.red / 255f, endColor.green / 255f, endColor.blue / 255f, 1f),
                    floatArrayOf(startColor.red / 255f, startColor.green / 255f, startColor.blue / 255f, 1f)
                )
            }
        } else null
        
        if (lyricScrollAnimation) {
            val currentFullText = displayLines.joinToString("|") { it.first }
            if (currentFullText != lastLyricText) {
                lastLyricText = currentFullText
                scrollAnimProgress = 18F
                velScrollAnim = 0F
            }
            if (scrollAnimProgress > 0.01F) {
                val animSpeed = 300F / lyricScrollAnimTime
                val (nextScroll, vS) = spring(scrollAnimProgress, 0F, velScrollAnim * animSpeed)
                scrollAnimProgress = nextScroll.coerceIn(0F, 50F)
                velScrollAnim = vS
            }
        }
        
        GradientFontShader.begin(useGradient, gradientX, gradientY, gradientColors ?: emptyList(), gradientSpeed, gradientOffset).use {
            var textY = bubbleY + 8F
            displayLines.forEachIndexed { index, (line, isMusic) ->
                val font = if (isMusic) musicFont else textFont
                val textWidth = font.getStringWidth(line)
                val textX = bubbleX + (animBubbleWidth - textWidth) / 2
                val currentLyricIndex = displayLines.indexOfFirst { it.first == currentLyric }
                val lineAlpha = if (index == currentLyricIndex || (currentLyricIndex == -1 && index == displayLines.size - 1)) {
                    (lyricTextAlpha * animBubbleAlpha).toInt()
                } else {
                    (lyricTextAlpha * 0.6 * animBubbleAlpha).toInt()
                }
                val colorToUse = when {
                    useGradient -> 0
                    lyricColorMode == "Theme" -> Color(themeColor.red, themeColor.green, themeColor.blue, lineAlpha).rgb
                    else -> Color(lyricCustomColor.red, lyricCustomColor.green, lyricCustomColor.blue, lineAlpha).rgb
                }
                val actualY = if (isMusic) textY else textY - scrollAnimProgress
                font.drawString(line, textX, actualY, colorToUse)
                textY += 18F
            }
        }
        
        if (lyricShowProgress) {
            val progress = MusicPlayer.progress.coerceIn(0F, 1F)
            val timeStr = MusicPlayer.timeDisplayString
            val barHeight = 4F
            val barY = bubbleY + animBubbleHeight - barHeight - 4F
            val barPadding = 8F
            val timeWidth = Fonts.fontRegular30.getStringWidth(timeStr)
            val timeX = bubbleX + barPadding
            val barStartX = timeX + timeWidth + 6F
            val maxBarWidth = animBubbleWidth - barPadding * 2 - timeWidth - 6F
            
            Fonts.fontRegular30.drawString(timeStr, timeX, barY - 1F, 
                Color(180, 180, 190, (lyricTextAlpha * animBubbleAlpha).toInt()).rgb)
            
            drawRoundedRect(barStartX, barY, barStartX + maxBarWidth, barY + barHeight, 
                Color(60, 60, 70, (180 * animBubbleAlpha).toInt()).rgb, 2F)
            
            val progressColor = when {
                lyricColorMode == "Custom" -> Color(lyricCustomColor.red, lyricCustomColor.green, lyricCustomColor.blue, (lyricTextAlpha * animBubbleAlpha).toInt())
                else -> Color(themeColor.red, themeColor.green, themeColor.blue, (lyricTextAlpha * animBubbleAlpha).toInt())
            }
            drawRoundedRect(barStartX, barY, barStartX + maxBarWidth * progress, barY + barHeight, 
                progressColor.rgb, 2F)
        }
    }
}
