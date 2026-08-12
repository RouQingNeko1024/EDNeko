/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.command.commands

import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.music.MusicPlayer
import net.ccbluex.liquidbounce.features.module.modules.music.core.Track
import net.ccbluex.liquidbounce.utils.kotlin.SharedScopes

/**
 * `.music` command (alias `.mu`) controlling the [MusicPlayer]: online search
 * (Netease), playback, queue and volume. Network operations run on
 * [SharedScopes.IO] so the render thread is never blocked.
 */
object MusicCommand : Command("music", "mu") {

    private var lastSearch: List<Track> = emptyList()

    override fun execute(args: Array<String>) {
        if (args.size < 2) {
            printUsage()
            return
        }

        when (args[1].lowercase()) {
            "search" -> handleSearch(args)
            "play" -> handlePlay(args)
            "add" -> handleAdd(args)
            "queue" -> handleQueue()
            "next" -> { MusicPlayer.playNext(); chat("§a已切换到下一首") }
            "prev", "previous" -> { MusicPlayer.playPrevious(); chat("§a已切换到上一首") }
            "stop" -> { MusicPlayer.stopMusic(); chat("§a已停止播放") }
            "local" -> handleLocal(args)
            "volume", "vol" -> handleVolume(args)
            "gui" -> {
                MusicPlayer.openGui()
                chat("§a正在打开音乐播放器...")
            }
            else -> printUsage()
        }
    }

    private fun ensureEnabled() {
        if (!MusicPlayer.state) {
            MusicPlayer.state = true
        }
    }

    private fun handleSearch(args: Array<String>) {
        if (args.size < 3) {
            chatSyntax("music search <关键词>")
            return
        }
        val keyword = args.copyOfRange(2, args.size).joinToString(" ")
        chat("§7正在搜索网易云: §f$keyword §7...")

        SharedScopes.IO.launch {
            val results = MusicPlayer.searchNetease(keyword)
            lastSearch = results
            if (results.isEmpty()) {
                chat("§c未找到结果（可能网络异常或关键词无匹配）")
                return@launch
            }
            chat("§a搜索结果（共 ${results.size} 首）:")
            results.forEachIndexed { index, track ->
                val id = track.neteaseId ?: 0L
                chat("§3${index + 1}. §f${track.artist} - ${track.title} §7(ID: $id)")
            }
            chat("§7使用 §f.music play <序号|歌曲ID> §7播放，§f.music add <序号|歌曲ID> §7加入队列")
        }
    }

    private fun handlePlay(args: Array<String>) {
        when (val target = resolveTarget(args, "play") ?: return) {
            is TrackTarget.Search -> playTrackAsync(target.track)
            is TrackTarget.NeteaseId -> playByIdAsync(target.id)
        }
    }

    private fun handleAdd(args: Array<String>) {
        when (val target = resolveTarget(args, "add") ?: return) {
            is TrackTarget.Search -> {
                val position = MusicPlayer.enqueue(target.track)
                chat("§a已加入队列 (#$position): §f${target.track.displayName}")
            }
            is TrackTarget.NeteaseId -> {
                SharedScopes.IO.launch {
                    val track = MusicPlayer.fetchNeteaseTrack(target.id) ?: run {
                        chat("§c无法获取歌曲信息 (ID: ${target.id})")
                        return@launch
                    }
                    val position = MusicPlayer.enqueue(track)
                    chat("§a已加入队列 (#$position): §f${track.displayName}")
                }
            }
        }
    }

    private fun playTrackAsync(track: Track) {
        ensureEnabled()
        chat("§7正在加载: §f${track.displayName} §7...")
        SharedScopes.IO.launch {
            MusicPlayer.playTrack(track)
        }
    }

    private fun playByIdAsync(id: Long) {
        ensureEnabled()
        chat("§7正在加载歌曲 ID: §f$id §7...")
        SharedScopes.IO.launch {
            val track = MusicPlayer.fetchNeteaseTrack(id) ?: run {
                chat("§c无法获取歌曲信息 (ID: $id)")
                return@launch
            }
            chat("§7正在播放: §f${track.displayName}")
            MusicPlayer.playTrack(track)
        }
    }

    private sealed class TrackTarget {
        data class Search(val track: Track) : TrackTarget()
        data class NeteaseId(val id: Long) : TrackTarget()
    }

    /**
     * [arg] is a search result index (1..lastSearch.size) when it falls in that
     * range; otherwise it is treated as a Netease song id.
     */
    private fun resolveTarget(args: Array<String>, subCommand: String): TrackTarget? {
        if (args.size < 3) {
            chatSyntax("music $subCommand <序号|歌曲ID>")
            return null
        }
        val number = args[2].toLongOrNull()
        if (number == null || number <= 0L) {
            chat("§c无效序号或歌曲 ID")
            return null
        }
        val index = number.toInt()
        if (lastSearch.isNotEmpty() && index in 1..lastSearch.size) {
            return TrackTarget.Search(lastSearch[index - 1])
        }
        return TrackTarget.NeteaseId(number)
    }

    private fun handleQueue() {
        val tracks = MusicPlayer.queueTracks
        if (tracks.isEmpty()) {
            chat("§7队列为空")
            return
        }
        chat("§a当前队列（${tracks.size} 首）:")
        tracks.forEachIndexed { index, track ->
            chat("§3${index + 1}. §f${track.displayName}")
        }
    }

    private fun handleLocal(args: Array<String>) {
        ensureEnabled()
        val tracks = MusicPlayer.refreshLocalTracks()
        if (tracks.isEmpty()) {
            chat("§c未找到本地音乐文件")
            return
        }

        if (args.size >= 3) {
            val index = args[2].toIntOrNull()?.minus(1)
            if (index == null || index !in tracks.indices) {
                chat("§c无效序号 (1-${tracks.size})")
                return
            }
            MusicPlayer.playLocalIndex(index)
            return
        }

        chat("§a本地音乐（${tracks.size} 首）:")
        tracks.forEachIndexed { index, track ->
            chat("§3${index + 1}. §f${track.displayName}")
        }
        chat("§7使用 §f.music local <序号> §7播放")
    }

    private fun handleVolume(args: Array<String>) {
        if (args.size < 3) {
            chat("§7当前音量: §f${MusicPlayer.getVolume()}")
            return
        }
        val vol = args[2].toIntOrNull()
        if (vol == null || vol !in 0..100) {
            chatSyntax("music volume <0-100>")
            return
        }
        MusicPlayer.setVolume(vol)
        chat("§a音量已设为: §f$vol")
    }

    private fun printUsage() {
        chat("§3MusicPlayer 命令:")
        chatSyntax(
            arrayOf(
                "search <关键词>",
                "play <序号|歌曲ID>",
                "add <序号|歌曲ID>",
                "queue",
                "next",
                "prev",
                "stop",
                "local [序号]",
                "volume <0-100>",
                "gui"
            )
        )
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.size == 1) {
            val subCommands = listOf("search", "play", "add", "queue", "next", "prev", "stop", "local", "volume", "gui")
            return subCommands.filter { it.startsWith(args[0], true) }
        }
        return emptyList()
    }
}
