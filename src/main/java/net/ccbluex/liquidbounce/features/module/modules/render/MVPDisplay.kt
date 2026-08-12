package net.ccbluex.liquidbounce.features.module.modules.render

import javazoom.jl.player.JavaSoundAudioDevice
import javazoom.jl.player.Player
import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.item.EntityFireworkRocket
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

object MVPDisplay : Module("MVPDisplay", Category.RENDER, gameDetecting = false) {

    private val victoryKeywords = listOf(
        "win", "victory", "胜", "赢", "winner", "champion", "冠军",
        "first place", "第一名", "you won", "胜利", "best",
         "获胜", "triumph", "恭喜", "well played"
    )

    private val checkFirework by boolean("检测烟花", true)
    private val fireworkRadius by int("烟花检测半径", 10, 5..50) { checkFirework }

    private val checkTitle by boolean("检测Title", true)
    private val checkSubtitle by boolean("检测Subtitle", true)
    private val checkChat by boolean("检测聊天栏", true)

    private val checkTabCount by boolean("检测Tab人数", false)
    private val targetTabCount by int("目标Tab人数", 1, 1..100) { checkTabCount }

    private val checkKillStreak by boolean("检测连杀", false)
    private val killStreakCount by int("连杀数量", 5, 2..50) { checkKillStreak }

    private val displayStyle by choices("界面样式", arrayOf("经典", "极简", "霓虹", "渐变", "玻璃", "游戏", "卡片", "暗黑", "彩虹", "星尘"), "游戏")

    private val displayX by int("显示位置X", 0, -500..500)
    private val displayY by int("显示位置Y", 130, -500..500)
    private val displayWidth by int("显示宽度", 200, 100..400)
    private val displayHeight by int("显示高度", 80, 50..200)

    private val avatarSize by int("头像大小", 48, 24..96)

    private val backgroundAlpha by int("背景透明度", 230, 0..255)
    private val blurBackground by boolean("背景模糊", true)
    private val blurStrength by float("模糊强度", 10F, 1F..30F) { blurBackground }

    private val bounceAnimation by boolean("弹跳动画", true)
    private val bounceTension by float("弹跳张力", 0.01f, 0.01f..0.5f) { bounceAnimation }
    private val bounceFriction by float("弹跳摩擦", 0.1f, 0.01f..0.5f) { bounceAnimation }

    private val firstLineText by text("第一行文字", "MVP")
    private val secondLineText by text("第二行文字", "为本场MVP!")
    private val thirdLineText by text("第三行文字", "♪正在高奏您的MVP凯歌:  ")

    private val displayDuration by int("显示时间(秒)", 5, 1..30)
    private val fadeAnimation by boolean("淡入淡出动画", true)
    private val scaleAnimation by boolean("缩放动画", true)
    private val animationDuration by int("动画时间(毫秒)", 300, 100..1000) { fadeAnimation || scaleAnimation }

    private val cooldownTime by int("冷却时间(秒)", 30, 0..120)

    private val mvpVolume by int("音乐音量", 100, 0..100)

    private val mvpMusicDir: File by lazy {
        val dir = File(FileManager.dir, "MVPMusic")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val mvpMusicList = mutableListOf<File>()
    private val mvpMusicCache = ConcurrentHashMap<String, Long>()
    private var selectedMvpMusicName = "无"
    private var mvpPlayer: Player? = null
    private var mvpAudioDevice: VolumeControlledAudioDevice? = null
    private var mvpPlayThread: Thread? = null
    private var isMvpPlaying = false

    private lateinit var mvpMusicChoicesValue: ListValue

    private fun initMvpMusicChoices() {
        mvpMusicChoicesValue = choices("MVP音乐", arrayOf("无"), "无").onChanged {
            selectedMvpMusicName = it
        } as ListValue
    }

    init {
        initMvpMusicChoices()
    }

    private var isDisplaying = false
    private var displayStartTime = 0L
    private var lastTriggerTime = 0L
    private var currentAnimationProgress = 0F
    private var animationState = AnimationState.HIDDEN
    private var animationTick = 0L

    private var animX = 0F
    private var animY = 0F
    private var animWidth = 0F
    private var animHeight = 0F
    private var animScale = 0F
    private var animAlpha = 0F

    private var velX = 0F
    private var velY = 0F
    private var velWidth = 0F
    private var velHeight = 0F
    private var velScale = 0F
    private var velAlpha = 0F

    private var killCount = 0
    private var lastKillTime = 0L
    private val killStreakTimeout = 5000L

    private var lastTitleText = ""
    private var lastSubtitleText = ""
    private var lastChatText = ""

    private enum class AnimationState {
        HIDDEN, FADE_IN, SHOWING, FADE_OUT
    }

    override fun onEnable() {
        super.onEnable()
        scanMvpMusicFiles()
        resetState()
    }

    override fun onDisable() {
        super.onDisable()
        stopMvpMusic()
        resetState()
    }

    private fun resetState() {
        isDisplaying = false
        displayStartTime = 0L
        currentAnimationProgress = 0F
        animationState = AnimationState.HIDDEN
        animationTick = 0L
        killCount = 0
        lastKillTime = 0L
        lastTitleText = ""
        lastSubtitleText = ""
        lastChatText = ""
        
        animX = 0F
        animY = 0F
        animWidth = 0F
        animHeight = 0F
        animScale = 0F
        animAlpha = 0F
        velX = 0F
        velY = 0F
        velWidth = 0F
        velHeight = 0F
        velScale = 0F
        velAlpha = 0F
    }

    private fun spring(current: Float, target: Float, velocity: Float): Pair<Float, Float> {
        val displacement = target - current
        val force = displacement * bounceTension
        val drag = velocity * bounceFriction
        val acceleration = force - drag
        val newVelocity = velocity + acceleration
        val newPosition = current + newVelocity
        return newPosition to newVelocity
    }

    private fun scanMvpMusicFiles() {
        mvpMusicList.clear()
        mvpMusicCache.clear()

        if (!mvpMusicDir.exists() || !mvpMusicDir.isDirectory) {
            updateMvpMusicChoices()
            return
        }

        mvpMusicDir.walk()
            .filter { file ->
                file.isFile && (
                    file.extension.equals("mp3", true) ||
                    file.extension.equals("wav", true) ||
                    file.extension.equals("flac", true)
                )
            }
            .sortedBy { it.nameWithoutExtension.lowercase() }
            .forEach { file ->
                mvpMusicList.add(file)
                mvpMusicCache[file.name] = file.lastModified()
            }

        updateMvpMusicChoices()
    }

    private fun updateMvpMusicChoices() {
        val names = mutableListOf("无")
        names.addAll(mvpMusicList.map { it.nameWithoutExtension })
        mvpMusicChoicesValue.updateValues(names.toTypedArray())
        if (selectedMvpMusicName !in names) {
            selectedMvpMusicName = "无"
        }
    }

    private fun updateAnimation() {
        if (!isDisplaying) {
            animationState = AnimationState.HIDDEN
            currentAnimationProgress = 0F
            return
        }

        animationTick++

        val elapsed = System.currentTimeMillis() - displayStartTime
        val animDuration = if (fadeAnimation || scaleAnimation) animationDuration.toLong() else 0L
        val showDuration = displayDuration * 1000L

        when {
            elapsed < animDuration -> {
                animationState = AnimationState.FADE_IN
                currentAnimationProgress = elapsed.toFloat() / animDuration
            }
            elapsed < animDuration + showDuration -> {
                animationState = AnimationState.SHOWING
                currentAnimationProgress = 1F
            }
            elapsed < animDuration + showDuration + animDuration -> {
                animationState = AnimationState.FADE_OUT
                currentAnimationProgress = 1F - (elapsed - animDuration - showDuration).toFloat() / animDuration
            }
            else -> {
                isDisplaying = false
                animationState = AnimationState.HIDDEN
                currentAnimationProgress = 0F
            }
        }
    }

    private fun checkVictoryConditions() {
        if (isDisplaying) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTriggerTime < cooldownTime * 1000L) return

        var victoryDetected = false

        if (checkFirework && checkFireworkVictory()) {
            victoryDetected = true
        }

        if (!victoryDetected && checkTabCount && checkTabCountVictory()) {
            victoryDetected = true
        }

        if (!victoryDetected && checkKillStreak && checkKillStreakVictory()) {
            victoryDetected = true
        }

        if (victoryDetected) {
            triggerMVPDisplay()
        }
    }

    private fun checkFireworkVictory(): Boolean {
        val player = mc.thePlayer ?: return false
        val world = mc.theWorld ?: return false

        val fireworks = world.loadedEntityList.filterIsInstance<EntityFireworkRocket>()
        for (firework in fireworks) {
            val distance = player.getDistanceToEntity(firework)
            if (distance <= fireworkRadius) {
                return true
            }
        }
        return false
    }

    private fun checkTabCountVictory(): Boolean {
        val playerInfoMap = mc.netHandler?.playerInfoMap ?: return false
        return playerInfoMap.size <= targetTabCount
    }

    private fun checkKillStreakVictory(): Boolean {
        return killCount >= killStreakCount
    }

    private fun updateKillStreak() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastKillTime > killStreakTimeout && killCount > 0) {
            killCount = 0
        }
    }

    fun onKill() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastKillTime <= killStreakTimeout) {
            killCount++
        } else {
            killCount = 1
        }
        lastKillTime = currentTime
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.eventType != EventState.RECEIVE) return@handler

        if (isDisplaying) return@handler

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTriggerTime < cooldownTime * 1000L) return@handler

        var victoryDetected = false

        when (val packet = event.packet) {
            is S45PacketTitle -> {
                val text = packet.message?.unformattedText?.lowercase() ?: ""
                
                if (packet.type == S45PacketTitle.Type.TITLE && checkTitle) {
                    if (text != lastTitleText && checkVictoryKeywords(text)) {
                        victoryDetected = true
                        lastTitleText = text
                    }
                }
                
                if (packet.type == S45PacketTitle.Type.SUBTITLE && checkSubtitle) {
                    if (text != lastSubtitleText && checkVictoryKeywords(text)) {
                        victoryDetected = true
                        lastSubtitleText = text
                    }
                }
            }
            is S02PacketChat -> {
                if (checkChat) {
                    val text = packet.chatComponent.unformattedText.lowercase()
                    if (text != lastChatText && checkVictoryKeywords(text)) {
                        victoryDetected = true
                        lastChatText = text
                    }
                }
            }
        }

        if (victoryDetected) {
            triggerMVPDisplay()
        }
    }

    private fun checkVictoryKeywords(text: String): Boolean {
        val lowerText = text.lowercase()
        return victoryKeywords.any { keyword -> lowerText.contains(keyword.lowercase()) }
    }

    private fun triggerMVPDisplay() {
        if (isDisplaying) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTriggerTime < cooldownTime * 1000L) return

        isDisplaying = true
        displayStartTime = currentTime
        lastTriggerTime = currentTime
        animationState = AnimationState.FADE_IN
        currentAnimationProgress = 0F
        animationTick = 0L

        killCount = 0

        playMvpMusic()
    }

    private fun playMvpMusic() {
        if (selectedMvpMusicName == "无") return

        val musicFile = mvpMusicList.find { it.nameWithoutExtension == selectedMvpMusicName } ?: return

        stopMvpMusic()

        mvpPlayThread = thread(start = true, name = "MVPMusic-Thread") {
            try {
                isMvpPlaying = true
                val inputStream = BufferedInputStream(FileInputStream(musicFile))
                mvpAudioDevice = VolumeControlledAudioDevice()
                mvpAudioDevice?.setVolume(mvpVolume / 100F)
                mvpPlayer = Player(inputStream, mvpAudioDevice)
                mvpPlayer?.play()
            } catch (e: Exception) {
                isMvpPlaying = false
            }
        }
    }

    private fun stopMvpMusic() {
        isMvpPlaying = false
        try {
            mvpPlayer?.close()
        } catch (e: Exception) {
        }
        mvpPlayer = null
        mvpAudioDevice = null
        try {
            mvpPlayThread?.interrupt()
        } catch (e: Exception) {
        }
        mvpPlayThread = null
    }

    val onRender2D = handler<Render2DEvent> {
        updateAnimation()
        checkVictoryConditions()
        updateKillStreak()

        if (!isDisplaying || animationState == AnimationState.HIDDEN) return@handler

        val scaledResolution = ScaledResolution(mc)
        val screenWidth = scaledResolution.scaledWidth.toFloat()
        val screenHeight = scaledResolution.scaledHeight.toFloat()

        val centerX = screenWidth / 2 + displayX
        val centerY = screenHeight / 2 + displayY

        val targetX = centerX - displayWidth / 2
        val targetY = centerY - displayHeight / 2
        val targetWidth = displayWidth.toFloat()
        val targetHeight = displayHeight.toFloat()
        val targetScale = if (scaleAnimation) 0.5F + currentAnimationProgress * 0.5F else 1F
        val targetAlpha = if (fadeAnimation) currentAnimationProgress else 1F

        if (bounceAnimation) {
            val (newX, vX) = spring(animX, targetX, velX)
            animX = newX
            velX = vX

            val (newY, vY) = spring(animY, targetY, velY)
            animY = newY
            velY = vY

            val (newW, vW) = spring(animWidth, targetWidth, velWidth)
            animWidth = newW.coerceAtLeast(0F)
            velWidth = vW

            val (newH, vH) = spring(animHeight, targetHeight, velHeight)
            animHeight = newH.coerceAtLeast(0F)
            velHeight = vH

            val (newS, vS) = spring(animScale, targetScale, velScale)
            animScale = newS.coerceIn(0F, 2F)
            velScale = vS

            val (newA, vA) = spring(animAlpha, targetAlpha, velAlpha)
            animAlpha = newA.coerceIn(0F, 1F)
            velAlpha = vA
        } else {
            animX = targetX
            animY = targetY
            animWidth = targetWidth
            animHeight = targetHeight
            animScale = targetScale
            animAlpha = targetAlpha
        }

        val x = animX
        val y = animY
        val w = animWidth
        val h = animHeight
        val scale = animScale
        val alpha = (animAlpha * 255).toInt()

        GL11.glPushMatrix()
        GL11.glTranslatef(centerX, centerY, 0F)
        GL11.glScalef(scale, scale, 1F)
        GL11.glTranslatef(-centerX, -centerY, 0F)

        val themeColor = ClientThemesUtils.getColor()

        if (blurBackground) {
            BlurUtils.blurAreaRounded(x, y, x + w, y + h, 8F, blurStrength)
        }

        when (displayStyle) {
            "经典" -> renderClassicStyle(x, y, w, h, themeColor, alpha)
            "极简" -> renderMinimalStyle(x, y, w, h, themeColor, alpha)
            "霓虹" -> renderNeonStyle(x, y, w, h, themeColor, alpha)
            "渐变" -> renderGradientStyle(x, y, w, h, themeColor, alpha)
            "玻璃" -> renderGlassStyle(x, y, w, h, themeColor, alpha)
            "游戏" -> renderGameStyle(x, y, w, h, themeColor, alpha)
            "卡片" -> renderCardStyle(x, y, w, h, themeColor, alpha)
            "暗黑" -> renderDarkStyle(x, y, w, h, themeColor, alpha)
            "彩虹" -> renderRainbowStyle(x, y, w, h, themeColor, alpha)
            "星尘" -> renderStardustStyle(x, y, w, h, themeColor, alpha)
            else -> renderClassicStyle(x, y, w, h, themeColor, alpha)
        }

        GL11.glPopMatrix()
    }

    private fun renderClassicStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val bgColor = Color(20, 20, 25, bgAlpha)
        val borderColor = Color(themeColor.red, themeColor.green, themeColor.blue, alpha)

        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 8F)
        RenderUtils.drawRoundedBorder(x, y, x + w, y + h, 2F, borderColor.rgb, 8F)

        drawContent(x, y, w, h, themeColor, alpha)
    }

    private fun renderMinimalStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val bgColor = Color(15, 15, 18, bgAlpha)
        val lineColor = Color(themeColor.red, themeColor.green, themeColor.blue, alpha)

        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 4F)
        RenderUtils.drawRect(x, y, x + 3, y + h, lineColor.rgb)

        drawContent(x + 5, y, w - 5, h, themeColor, alpha)
    }

    private fun renderNeonStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val glowIntensity = (Math.sin(animationTick * 0.1) * 0.3 + 0.7).toFloat()
        
        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val bgColor = Color(10, 10, 15, bgAlpha)
        val glowColor = Color(
            (themeColor.red * glowIntensity).toInt().coerceIn(0, 255),
            (themeColor.green * glowIntensity).toInt().coerceIn(0, 255),
            (themeColor.blue * glowIntensity).toInt().coerceIn(0, 255),
            alpha
        )

        for (i in 8 downTo 1) {
            val glowAlpha = (alpha * 0.1F * i / 8F).toInt()
            val expandedGlow = Color(glowColor.red, glowColor.green, glowColor.blue, glowAlpha)
            RenderUtils.drawRoundedBorder(x - i, y - i, x + w + i, y + h + i, 1F, expandedGlow.rgb, 8F + i)
        }

        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 8F)
        RenderUtils.drawRoundedBorder(x, y, x + w, y + h, 2F, glowColor.rgb, 8F)

        drawContent(x, y, w, h, themeColor, alpha)
    }

    private fun renderGradientStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val gradientOffset = (animationTick * 0.02F) % 1F
        
        val startColor = ClientThemesUtils.setColor("start", alpha)
        val endColor = ClientThemesUtils.setColor("end", alpha)
        
        val gradientWidth = w * 2
        val offset = (gradientOffset * gradientWidth).toInt()
        
        val dynamicStartColor = Color(
            startColor.red,
            startColor.green,
            startColor.blue,
            (alpha * 0.9F).toInt()
        )
        val dynamicEndColor = Color(
            endColor.red,
            endColor.green,
            endColor.blue,
            (alpha * 0.9F).toInt()
        )
        
        RenderUtils.drawGradientRect(x, y, x + w, y + h, dynamicStartColor.rgb, dynamicEndColor.rgb, 0F)

        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val bgColor = Color(20, 20, 25, bgAlpha)
        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 8F)

        drawContent(x, y, w, h, themeColor, alpha)
    }

    private fun renderGlassStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val bgColor = Color(255, 255, 255, (bgAlpha * 0.08F / 255F).toInt())
        val borderColor = Color(255, 255, 255, (alpha * 0.2F).toInt())
        val highlightColor = Color(255, 255, 255, (alpha * 0.1F).toInt())

        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 12F)
        RenderUtils.drawRoundedRect(x, y, x + w, y + h / 3, highlightColor.rgb, 12F)
        RenderUtils.drawRoundedBorder(x, y, x + w, y + h, 1F, borderColor.rgb, 12F)

        val accentColor = Color(themeColor.red, themeColor.green, themeColor.blue, (alpha * 0.5F).toInt())
        RenderUtils.drawRect(x + 10, y + h - 3, x + w - 10, y + h, accentColor.rgb)

        drawContent(x, y, w, h, themeColor, alpha)
    }

    private fun renderGameStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val bgColor = Color(25, 25, 30, bgAlpha)
        val topColor = Color(themeColor.red, themeColor.green, themeColor.blue, alpha)
        val bottomColor = Color(
            (themeColor.red * 0.3F).toInt(),
            (themeColor.green * 0.3F).toInt(),
            (themeColor.blue * 0.3F).toInt(),
            alpha
        )

        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 6F)

        RenderUtils.drawRect(x, y, x + w, y + 25, topColor.rgb)
        RenderUtils.drawRect(x, y + h - 5, x + w, y + h, bottomColor.rgb)

        val titleColor = Color(255, 255, 255, alpha)
        Fonts.fontSemibold35.drawString(firstLineText, x + w / 2 - Fonts.fontSemibold35.getStringWidth(firstLineText) / 2, y + 7, titleColor.rgb)

        val avatarX = x + 15
        val avatarY = y + 35F
        val smallAvatarSize = avatarSize * 2 / 3

        val skinLocation: ResourceLocation? = mc.thePlayer?.locationSkin
        if (skinLocation != null) {
            RenderUtils.drawHead(skinLocation, avatarX.toInt(), avatarY.toInt(), smallAvatarSize, smallAvatarSize, Color.WHITE)
        }

        val textStartX = avatarX + smallAvatarSize + 12F
        val playerName = mc.thePlayer?.name ?: "Player"
        val secondLine = "$playerName$secondLineText"
        
        Fonts.fontSemibold35.drawString(secondLine, textStartX, avatarY + 5, Color(255, 255, 255, alpha).rgb)

        if (selectedMvpMusicName != "无") {
            val thirdLine = "$thirdLineText$selectedMvpMusicName"
            Fonts.fontRegular30.drawString(thirdLine, textStartX, avatarY + 22F, Color(180, 180, 190, alpha).rgb)
        }
    }

    private fun renderCardStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val shadowColor = Color(0, 0, 0, (alpha * 0.3F).toInt())
        val bgColor = Color(30, 30, 35, bgAlpha)
        val accentColor = Color(themeColor.red, themeColor.green, themeColor.blue, alpha)

        RenderUtils.drawRoundedRect(x + 4, y + 4, x + w + 4, y + h + 4, shadowColor.rgb, 10F)
        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 10F)

        RenderUtils.drawRoundedRect(x, y, x + w, y + 4, accentColor.rgb, 10F)

        drawContent(x, y, w, h, themeColor, alpha)
    }

    private fun renderDarkStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val bgColor = Color(5, 5, 8, bgAlpha)
        val subtleBorder = Color(40, 40, 45, alpha)
        val accentGlow = Color(themeColor.red, themeColor.green, themeColor.blue, (alpha * 0.3F).toInt())

        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 6F)
        RenderUtils.drawRoundedBorder(x, y, x + w, y + h, 1F, subtleBorder.rgb, 6F)

        RenderUtils.drawRoundedRect(x + 5, y + h - 8, x + w - 5, y + h - 5, accentGlow.rgb, 2F)

        drawContent(x, y, w, h, themeColor, alpha)
    }

    private fun renderRainbowStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val rainbowColor = ClientThemesUtils.getColor(animationTick.toInt())
        val bgColor = Color(20, 20, 25, bgAlpha)
        val borderColor = Color(rainbowColor.red, rainbowColor.green, rainbowColor.blue, alpha)

        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 8F)
        RenderUtils.drawRoundedBorder(x, y, x + w, y + h, 2F, borderColor.rgb, 8F)

        drawContent(x, y, w, h, rainbowColor, alpha)
    }

    private fun renderStardustStyle(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val bgAlpha = (alpha * backgroundAlpha / 255F).toInt()
        val bgColor = Color(10, 10, 20, bgAlpha)
        val starColor = Color(255, 255, 255, (alpha * 0.6F).toInt())
        val accentColor = Color(themeColor.red, themeColor.green, themeColor.blue, alpha)

        RenderUtils.drawRoundedRect(x, y, x + w, y + h, bgColor.rgb, 10F)

        val seed = animationTick.toInt()
        for (i in 0 until 15) {
            val starX = x + ((seed * 7 + i * 47) % w.toInt())
            val starY = y + ((seed * 13 + i * 31) % h.toInt())
            val starSize = 1 + (seed + i) % 2
            val twinkle = (Math.sin((seed + i) * 0.2) * 0.5 + 0.5).toFloat()
            val twinkleAlpha = (alpha * 0.3F * twinkle).toInt()
            val twinkleColor = Color(255, 255, 255, twinkleAlpha)
            RenderUtils.drawRect(starX.toFloat(), starY.toFloat(), starX + starSize.toFloat(), starY + starSize.toFloat(), twinkleColor.rgb)
        }

        RenderUtils.drawRoundedBorder(x, y, x + w, y + h, 1F, accentColor.rgb, 10F)

        drawContent(x, y, w, h, themeColor, alpha)
    }

    private fun drawContent(x: Float, y: Float, w: Float, h: Float, themeColor: Color, alpha: Int) {
        val avatarX = x + 10
        val avatarY = y + (h - avatarSize) / 2

        val skinLocation: ResourceLocation? = mc.thePlayer?.locationSkin
        if (skinLocation != null) {
            RenderUtils.drawHead(skinLocation, avatarX.toInt(), avatarY.toInt(), avatarSize, avatarSize, Color.WHITE)
        }

        val textStartX = avatarX + avatarSize + 15F
        val textStartY = y + 12F

        val firstLineFont = Fonts.fontSemibold40
        val secondLineFont = Fonts.fontSemibold35
        val thirdLineFont = Fonts.fontRegular30

        val firstLineColor = Color(themeColor.red, themeColor.green, themeColor.blue, alpha)
        val secondLineColor = Color(255, 255, 255, alpha)
        val thirdLineColor = Color(180, 180, 190, alpha)

        firstLineFont.drawString(firstLineText, textStartX, textStartY, firstLineColor.rgb)

        val playerName = mc.thePlayer?.name ?: "Player"
        val secondLine = "$playerName$secondLineText"
        secondLineFont.drawString(secondLine, textStartX, textStartY + 20F, secondLineColor.rgb)

        if (selectedMvpMusicName != "无") {
            val thirdLine = "$thirdLineText$selectedMvpMusicName"
            thirdLineFont.drawString(thirdLine, textStartX, textStartY + 38F, thirdLineColor.rgb)
        }
    }

    private class VolumeControlledAudioDevice : JavaSoundAudioDevice() {
        private var volumeControl: javax.sound.sampled.FloatControl? = null

        fun setVolume(volume: Float) {
            try {
                if (volumeControl == null) {
                    findVolumeControl()
                }
                volumeControl?.let { ctrl ->
                    val min = ctrl.minimum
                    val max = ctrl.maximum
                    val range = max - min
                    val gain = range * volume.coerceIn(0F, 1F) + min
                    ctrl.value = gain
                }
            } catch (e: Exception) {
            }
        }

        private fun findVolumeControl() {
            try {
                val field = JavaSoundAudioDevice::class.java.getDeclaredField("source")
                field.isAccessible = true
                val source = field.get(this) as? javax.sound.sampled.SourceDataLine
                if (source != null && source.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                    volumeControl = source.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN) as javax.sound.sampled.FloatControl
                }
            } catch (e: Exception) {
            }
        }
    }
}
