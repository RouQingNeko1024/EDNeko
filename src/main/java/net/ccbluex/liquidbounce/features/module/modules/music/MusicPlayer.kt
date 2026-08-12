/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.music

import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.music.core.LocalMusicSource
import net.ccbluex.liquidbounce.features.module.modules.music.core.MusicSource
import net.ccbluex.liquidbounce.features.module.modules.music.core.NeteaseMusicSource
import net.ccbluex.liquidbounce.features.module.modules.music.core.ParsedLyrics
import net.ccbluex.liquidbounce.features.module.modules.music.core.PlaybackEngine
import net.ccbluex.liquidbounce.features.module.modules.music.core.Track
import net.ccbluex.liquidbounce.features.module.modules.music.core.TrackSource
import net.ccbluex.liquidbounce.ui.client.music.GuiMusicPlayer
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.gui.GuiScreen
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes
import kotlinx.coroutines.launch

/**
 * Music player orchestration layer.
 *
 * This object no longer plays audio directly: it delegates stream opening and
 * lyric loading to a [MusicSource] (local files or Netease) and audio playback
 * to a [PlaybackEngine]. All public read-only properties used by the HUD/Island
 * UI keep their original signatures so no UI code needs to change.
 */
object MusicPlayer : Module("MusicPlayer", Category.CLIENT) {

    private var volumeValue by int("音量", 50, 0..100)
    private val autoPlay by boolean("自动播放", false)
    private val loopMode by choices("循环模式", arrayOf("关闭", "单曲循环", "列表循环"), "列表循环")
    private val showInfo by boolean("显示信息", true)
    val musicPlatform by choices("音乐平台", arrayOf("本地", "网易云"), "本地")
    val searchLimit by int("搜索数量", 10, 1..30)
    private val neteaseDomain by text("网易云域名", "music.163.com")
    private val openGuiOnEnable by boolean("打开界面", true)

    /** Prevents openGui ↔ onEnable recursion when enabling the module to show the GUI. */
    private var suppressOpenGuiOnEnable = false

    private var selectedMusicName = "无"

    private val engine = PlaybackEngine()

    /** Active playback queue (local or online tracks). */
    private val queue = mutableListOf<Track>()
    private var currentIndex = 0
    private var currentTrack: Track? = null

    /** Cached local scan, used for the dropdown and HUD list. */
    private var localTracks: List<Track> = emptyList()

    private var currentLyric: String = ""
    private var lyricLines = listOf<String>()
    private var currentLyricIndex = 0
    private var playStartTime: Long = 0
    private var lyricTimestamps = mutableListOf<Long>()
    private var musicDuration: Long = 0

    val currentMusicName: String
        get() = currentTrack?.displayName ?: "无"

    val currentLyricDisplay: String
        get() = currentLyric

    val previousLyricDisplay: String
        get() = if (currentLyricIndex > 0 && lyricLines.isNotEmpty()) lyricLines[currentLyricIndex - 1] else ""

    val nextLyricDisplay: String
        get() = if (currentLyricIndex < lyricLines.size - 1 && lyricLines.isNotEmpty()) lyricLines[currentLyricIndex + 1] else ""

    val isCurrentlyPlaying: Boolean
        get() = engine.isPlaying

    val musicListNames: List<String>
        get() = localTracks.map { it.displayName }

    val localTrackList: List<Track>
        get() = localTracks

    val playingTrack: Track?
        get() = currentTrack

    private lateinit var musicChoicesValue: ListValue

    private fun initMusicChoices() {
        musicChoicesValue = choices("音乐", arrayOf("无"), "无").onChanged {
            if (it != "无" && it != selectedMusicName) {
                selectedMusicName = it
                val index = musicListNames.indexOf(it)
                if (index >= 0) {
                    playLocalIndex(index)
                }
            }
        } as ListValue
    }

    init {
        initMusicChoices()
    }

    val progress: Float
        get() {
            if (!engine.isPlaying || musicDuration <= 0) return 0F
            val elapsed = System.currentTimeMillis() - playStartTime
            return (elapsed.toFloat() / musicDuration.toFloat()).coerceIn(0F, 1F)
        }

    val currentTimeString: String
        get() {
            if (!engine.isPlaying) return "0:00"
            val elapsed = (System.currentTimeMillis() - playStartTime) / 1000
            val minutes = elapsed / 60
            val seconds = elapsed % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }

    val totalTimeString: String
        get() {
            if (musicDuration <= 0) return "0:00"
            val totalSeconds = musicDuration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }

    val timeDisplayString: String
        get() = "$currentTimeString / $totalTimeString"

    override fun onEnable() {
        super.onEnable()
        scanMusicFiles()

        if (localTracks.isEmpty()) {
            chat("§c[音乐播放器] 未找到本地音乐文件！请将音乐文件放入: ${LocalMusicSource.musicDir.absolutePath}")
            chat("§7[音乐播放器] 或使用 §f.music search <关键词>§7 在线播放网易云音乐")
        } else if (showInfo) {
            chat("§a[音乐播放器] 已加载 ${localTracks.size} 首本地音乐，可在HUD编辑界面中加入歌词显示")
            chat("§7[音乐播放器] 音乐目录: ${LocalMusicSource.musicDir.absolutePath}")
            chat("§7[音乐播放器] 在线播放: §f.music search <关键词>")
        }

        // Default to the local queue so list-loop / next / prev work as before.
        queue.clear()
        queue.addAll(localTracks)
        currentIndex = 0

        if (selectedMusicName != "无" && musicListNames.contains(selectedMusicName)) {
            val index = musicListNames.indexOf(selectedMusicName)
            if (index >= 0) {
                playLocalIndex(index)
                return
            }
        }

        if (autoPlay && localTracks.isNotEmpty()) {
            playLocalIndex(0)
        }

        if (openGuiOnEnable && !suppressOpenGuiOnEnable) {
            displayMusicGui(mc.currentScreen)
        }
    }

    override fun onDisable() {
        super.onDisable()
        stopMusic()
        if (showInfo) {
            chat("§c[音乐播放器] 已停止播放")
        }
    }

    val onTick = handler<GameTickEvent> {
        if (!engine.isPlaying && autoPlay && loopMode == "列表循环" && queue.isNotEmpty()) {
            playNext()
        }

        if (engine.isPlaying && lyricTimestamps.isNotEmpty()) {
            val elapsed = System.currentTimeMillis() - playStartTime
            var newIndex = 0
            for (i in lyricTimestamps.indices) {
                if (elapsed >= lyricTimestamps[i]) {
                    newIndex = i
                } else {
                    break
                }
            }
            if (newIndex != currentLyricIndex && newIndex < lyricLines.size) {
                currentLyricIndex = newIndex
                currentLyric = lyricLines[currentLyricIndex]
            }
        }

        updateVolume()
    }

    fun scanMusicFiles() {
        localTracks = LocalMusicSource.scan()
        updateMusicChoices()
    }

    private fun updateMusicChoices() {
        val names = mutableListOf("无")
        names.addAll(musicListNames)

        musicChoicesValue.updateValues(names.toTypedArray())
        if (selectedMusicName !in names) {
            selectedMusicName = "无"
        }
    }

    private fun sourceFor(track: Track): MusicSource = when (track.source) {
        TrackSource.LOCAL -> LocalMusicSource
        TrackSource.NETEASE -> {
            NeteaseMusicSource.domain = neteaseDomain
            NeteaseMusicSource
        }
    }

    /**
     * Play [track] immediately. Adds it to the queue if not already present.
     * The actual stream opening / lyric loading runs on a background IO
     * coroutine, so this is safe to call from any thread.
     */
    fun playTrack(track: Track) {
        val idx = queue.indexOf(track)
        if (idx >= 0) {
            currentIndex = idx
        } else {
            queue.add(track)
            currentIndex = queue.size - 1
        }
        dispatchPlayback(track)
    }

    /**
     * Start playback of [track] on the shared IO pool.
     *
     * Crucially this must run off both the render thread (network I/O would
     * freeze the game) and off the previous [PlaybackEngine] thread: the first
     * thing [startPlayback] does is [PlaybackEngine.stop], which interrupts the
     * previous playback thread. If the next track's network requests ran on that
     * same interrupted thread they would fail immediately with "interrupted".
     */
    private fun dispatchPlayback(track: Track) {
        SharedScopes.IO.launch { startPlayback(track) }
    }

    /**
     * Append [track] to the queue. Returns its 1-based position.
     */
    fun enqueue(track: Track): Int {
        queue.add(track)
        return queue.size
    }

    val queueTracks: List<Track>
        get() = queue.toList()

    private fun startPlayback(track: Track) {
        engine.stop()

        val source = sourceFor(track)
        currentTrack = track
        selectedMusicName = track.displayName
        playStartTime = System.currentTimeMillis()

        val lyrics = try {
            source.loadLyrics(track)
        } catch (e: Exception) {
            ParsedLyrics.EMPTY
        }
        applyLyrics(lyrics)

        musicDuration = when {
            track.durationMs > 0 -> track.durationMs
            lyrics.durationHintMs > 0 -> lyrics.durationHintMs
            track.source == TrackSource.LOCAL && track.localFile != null ->
                LocalMusicSource.getDuration(track.localFile)
            else -> 0L
        }

        val stream = try {
            source.openStream(track)
        } catch (e: Exception) {
            chat("§c[音乐播放器] 播放失败: ${e.message}")
            currentLyric = ""
            return
        }

        if (showInfo) {
            chat("§a[音乐播放器] 正在播放: §f${track.displayName}")
        }

        engine.play(
            stream = stream,
            onComplete = { onMusicComplete() },
            onError = { msg -> chat("§c[音乐播放器] 播放失败: $msg") }
        )

        engine.setVolume(volumeValue / 100F)
    }

    private fun applyLyrics(lyrics: ParsedLyrics) {
        lyricLines = lyrics.lines
        lyricTimestamps = lyrics.timestamps.toMutableList()
        currentLyricIndex = 0
        currentLyric = if (lyricLines.isNotEmpty()) lyricLines[0] else ""
    }

    private fun onMusicComplete() {
        when (loopMode) {
            "单曲循环" -> currentTrack?.let { dispatchPlayback(it) }
            "列表循环" -> playNext()
            "关闭" -> { /* stop, nothing else */ }
        }
    }

    fun stopMusic() {
        engine.stop()
        currentLyric = ""
        lyricLines = emptyList()
        lyricTimestamps.clear()
        playStartTime = 0
    }

    fun playNext() {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex + 1) % queue.size
        dispatchPlayback(queue[currentIndex])
    }

    fun playPrevious() {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex - 1 + queue.size) % queue.size
        dispatchPlayback(queue[currentIndex])
    }

    /**
     * Play a local track by its index in the (dropdown) local list. Resets the
     * active queue to the local list so list-loop works across local files.
     */
    fun playLocalIndex(index: Int) {
        if (index !in localTracks.indices) return
        queue.clear()
        queue.addAll(localTracks)
        currentIndex = index
        dispatchPlayback(queue[index])
    }

    /**
     * (Re)scan and return the local track list.
     */
    fun refreshLocalTracks(): List<Track> {
        scanMusicFiles()
        return localTracks
    }

    /**
     * Search Netease Cloud Music. Network call; must run off the render thread.
     */
    fun searchNetease(keyword: String): List<Track> {
        NeteaseMusicSource.domain = neteaseDomain
        return NeteaseMusicSource.search(keyword, searchLimit)
    }

    /**
     * Resolve a Netease track by song id. Network call; run off the render thread.
     */
    fun fetchNeteaseTrack(id: Long): Track? {
        NeteaseMusicSource.domain = neteaseDomain
        return NeteaseMusicSource.fetchTrack(id)
    }

    private fun updateVolume() {
        engine.setVolume(volumeValue / 100F)
    }

    fun setVolume(vol: Int) {
        volumeValue = vol.coerceIn(0, 100)
        updateVolume()
    }

    fun getVolume(): Int = volumeValue

    fun openGui(prev: GuiScreen? = null) {
        if (!state) {
            suppressOpenGuiOnEnable = true
            try {
                state = true
            } finally {
                suppressOpenGuiOnEnable = false
            }
        }
        displayMusicGui(prev)
    }

    private fun displayMusicGui(prev: GuiScreen?) {
        mc.displayGuiScreen(GuiMusicPlayer(prev))
    }
}
