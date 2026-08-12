/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.client.music

import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.features.module.modules.music.MusicPlayer
import net.ccbluex.liquidbounce.features.module.modules.music.core.LocalMusicSource
import net.ccbluex.liquidbounce.features.module.modules.music.core.Track
import net.ccbluex.liquidbounce.features.module.modules.music.core.TrackSource
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSlot
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.awt.Desktop

class GuiMusicPlayer(private val prevGui: GuiScreen?) : AbstractScreen() {

    private enum class Tab(val label: String) {
        LOCAL("本地"),
        NETEASE("网易云"),
        QUEUE("队列")
    }

    private lateinit var trackList: TrackList
    private lateinit var searchField: GuiTextField
    private lateinit var statusText: String

    private var currentTab = Tab.LOCAL
    private var searchResults = emptyList<Track>()
    private var selectedIndex = -1

    private val bottomPanelHeight = 118
    private val topBarHeight = 78
    private val tabRowY = 58

    override fun initGui() {
        statusText = "就绪"
        searchField = textField(100, Fonts.fontSemibold35, 20, 38, width - 110, 18) {
            maxStringLength = 128
        }

        trackList = TrackList(this).apply {
            registerScrollButtons(15, 16)
        }

        val sidebarX = width - 82
        var y = topBarHeight + 8
        +GuiButton(0, sidebarX, height - 26, 72, 20, "返回")
        +GuiButton(1, sidebarX, y, 72, 20, "播放"); y += 24
        +GuiButton(2, sidebarX, y, 72, 20, "加入队列"); y += 24
        +GuiButton(3, sidebarX, y, 72, 20, "上一首"); y += 24
        +GuiButton(4, sidebarX, y, 72, 20, "下一首"); y += 24
        +GuiButton(5, sidebarX, y, 72, 20, "停止"); y += 24
        +GuiButton(6, sidebarX, y, 72, 20, "刷新本地"); y += 24
        +GuiButton(7, sidebarX, y, 72, 20, "音乐目录"); y += 24
        +GuiButton(8, sidebarX, y, 34, 20, "-")
        +GuiButton(9, sidebarX + 38, y, 34, 20, "+")
        +GuiButton(11, 20, tabRowY, 52, 18, "本地")
        +GuiButton(12, 76, tabRowY, 52, 18, "网易")
        +GuiButton(13, 132, tabRowY, 52, 18, "队列")
        +GuiButton(14, 188, tabRowY, 52, 18, "搜索")

        refreshListSelection()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        assumeNonVolatile {
            drawDefaultBackground()
            drawRect(0f, 0f, width.toFloat(), topBarHeight.toFloat(), Color(18, 22, 32, 220).rgb)
            drawRect(0f, (height - bottomPanelHeight).toFloat(), width.toFloat(), height.toFloat(), Color(18, 22, 32, 235).rgb)

            Fonts.fontBold180.drawString("§b§lMusic Player", 20f, 8f, Color.WHITE.rgb, true)
            Fonts.fontSemibold35.drawString("§7$statusText", 20f, 30f, Color.LIGHT_GRAY.rgb)

            searchField.drawTextBox()
            if (searchField.text.isEmpty() && !searchField.isFocused) {
                Fonts.fontSemibold35.drawString("§8搜索关键词或歌曲 ID", searchField.xPosition + 4f, searchField.yPosition + 5f, Color.GRAY.rgb)
            }

            trackList.drawScreen(mouseX, mouseY, partialTicks)
            drawNowPlayingPanel()
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawNowPlayingPanel() {
        val panelY = height - bottomPanelHeight + 8
        val track = MusicPlayer.playingTrack
        val title = track?.displayName ?: "未播放"
        val source = when (track?.source) {
            TrackSource.LOCAL -> "本地"
            TrackSource.NETEASE -> "网易云"
            null -> "-"
        }

        Fonts.fontSemibold40.drawString("§f$title", 16f, panelY.toFloat(), Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString(
            "§7来源: $source  §7${MusicPlayer.timeDisplayString}  §7音量: ${MusicPlayer.getVolume()}",
            16f,
            (panelY + 14).toFloat(),
            Color.LIGHT_GRAY.rgb
        )

        val lyric = MusicPlayer.currentLyricDisplay
        if (lyric.isNotBlank()) {
            Fonts.fontSemibold35.drawString("§3$lyric", 16f, (panelY + 28).toFloat(), Color(120, 200, 255).rgb)
        }

        val barX = 16f
        val barY = (height - 28).toFloat()
        val barW = (width - 110).toFloat()
        val barH = 6f
        drawRect(barX, barY, barX + barW, barY + barH, Color(40, 48, 64).rgb)
        val progress = MusicPlayer.progress
        if (progress > 0f) {
            drawRect(barX, barY, barX + barW * progress, barY + barH, Color(0, 160, 255).rgb)
        }

        val state = if (MusicPlayer.isCurrentlyPlaying) "§a播放中" else "§c已停止"
        Fonts.fontSemibold35.drawString(state, barX + barW + 8f, barY - 2f, Color.WHITE.rgb)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> playSelected()
            2 -> addSelectedToQueue()
            3 -> MusicPlayer.playPrevious()
            4 -> MusicPlayer.playNext()
            5 -> MusicPlayer.stopMusic()
            6 -> {
                MusicPlayer.refreshLocalTracks()
                if (currentTab == Tab.LOCAL) {
                    statusText = "已刷新本地列表 (${MusicPlayer.localTrackList.size} 首)"
                    trackList.refresh()
                }
            }
            7 -> openMusicFolder()
            8 -> MusicPlayer.setVolume(MusicPlayer.getVolume() - 5)
            9 -> MusicPlayer.setVolume(MusicPlayer.getVolume() + 5)
            11 -> switchTab(Tab.LOCAL)
            12 -> switchTab(Tab.NETEASE)
            13 -> switchTab(Tab.QUEUE)
            14 -> searchNetease()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (searchField.isFocused) {
            if (keyCode == Keyboard.KEY_RETURN) {
                searchNetease()
                return
            }
            searchField.textboxKeyTyped(typedChar, keyCode)
            return
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(prevGui)
            return
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        trackList.handleMouseInput()
    }

    override fun updateScreen() {
        searchField.updateCursorCounter()
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        selectedIndex = -1
        statusText = when (tab) {
            Tab.LOCAL -> "本地音乐 ${MusicPlayer.localTrackList.size} 首"
            Tab.NETEASE -> "网易云搜索 (${searchResults.size} 条结果)"
            Tab.QUEUE -> "播放队列 ${MusicPlayer.queueTracks.size} 首"
        }
        trackList.refresh()
    }

    private fun currentTracks(): List<Track> = when (currentTab) {
        Tab.LOCAL -> MusicPlayer.localTrackList
        Tab.NETEASE -> searchResults
        Tab.QUEUE -> MusicPlayer.queueTracks
    }

    private fun refreshListSelection() {
        trackList.refresh()
        if (selectedIndex >= currentTracks().size) {
            selectedIndex = -1
        }
    }

    private fun playSelected() {
        val tracks = currentTracks()
        if (selectedIndex !in tracks.indices) {
            statusText = "请先选择一首歌曲"
            return
        }
        val track = tracks[selectedIndex]
        if (currentTab == Tab.LOCAL) {
            val localIndex = MusicPlayer.localTrackList.indexOf(track)
            if (localIndex >= 0) {
                MusicPlayer.playLocalIndex(localIndex)
            }
        } else {
            MusicPlayer.playTrack(track)
        }
        statusText = "正在播放: ${track.displayName}"
    }

    private fun addSelectedToQueue() {
        val tracks = currentTracks()
        if (selectedIndex !in tracks.indices) {
            statusText = "请先选择一首歌曲"
            return
        }
        val track = tracks[selectedIndex]
        val pos = MusicPlayer.enqueue(track)
        statusText = "已加入队列 (#$pos): ${track.displayName}"
        if (currentTab == Tab.QUEUE) {
            trackList.refresh()
        }
    }

    private fun searchNetease() {
        val keyword = searchField.text.trim()
        if (keyword.isEmpty()) {
            statusText = "请输入搜索关键词或歌曲 ID"
            return
        }

        val id = keyword.toLongOrNull()
        if (id != null && id > 0L) {
            statusText = "正在加载 ID: $id ..."
            SharedScopes.IO.launch {
                val track = MusicPlayer.fetchNeteaseTrack(id)
                mc.addScheduledTask {
                    if (track == null) {
                        statusText = "无法获取歌曲 (ID: $id)"
                        return@addScheduledTask
                    }
                    searchResults = listOf(track)
                    switchTab(Tab.NETEASE)
                    selectedIndex = 0
                    MusicPlayer.playTrack(track)
                    statusText = "正在播放: ${track.displayName}"
                }
            }
            return
        }

        statusText = "正在搜索: $keyword ..."
        SharedScopes.IO.launch {
            val results = MusicPlayer.searchNetease(keyword)
            mc.addScheduledTask {
                searchResults = results
                switchTab(Tab.NETEASE)
                selectedIndex = if (results.isNotEmpty()) 0 else -1
                statusText = if (results.isEmpty()) {
                    "未找到结果"
                } else {
                    "找到 ${results.size} 首: $keyword"
                }
            }
        }
    }

    private fun openMusicFolder() {
        try {
            Desktop.getDesktop().open(LocalMusicSource.musicDir)
            statusText = "已打开: ${LocalMusicSource.musicDir.absolutePath}"
        } catch (e: Exception) {
            statusText = "无法打开音乐目录"
        }
    }

    private inner class TrackList(gui: GuiScreen) : GuiSlot(
        mc,
        gui.width - 90,
        gui.height,
        topBarHeight + 6,
        gui.height - bottomPanelHeight,
        22
    ) {
        private var entries = currentTracks()

        fun refresh() {
            entries = currentTracks()
        }

        override fun getSize(): Int = entries.size

        override fun elementClicked(id: Int, doubleClick: Boolean, mouseX: Int, mouseY: Int) {
            if (id !in entries.indices) return
            selectedIndex = id
            if (doubleClick) {
                playSelected()
            }
        }

        override fun isSelected(id: Int): Boolean = id == selectedIndex

        override fun drawSlot(id: Int, x: Int, y: Int, heightIn: Int, mouseXIn: Int, mouseYIn: Int) {
            if (id !in entries.indices) return
            val track = entries[id]
            val prefix = when {
                track == MusicPlayer.playingTrack -> "§a▶ "
                isSelected(id) -> "§b> "
                else -> "§7  "
            }
            val suffix = if (track.source == TrackSource.NETEASE && track.neteaseId != null) {
                " §8(ID:${track.neteaseId})"
            } else {
                ""
            }
            Fonts.fontSemibold35.drawString(
                "$prefix§f${track.displayName}$suffix",
                x + 4f,
                y + 6f,
                Color.WHITE.rgb
            )
        }

        override fun drawBackground() {
            drawRect(8f, top.toFloat(), (left + width - 8).toFloat(), bottom.toFloat(), Color(0, 0, 0, 60).rgb)
        }
    }
}
