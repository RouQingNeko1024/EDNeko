/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.music.core

import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.sound.sampled.AudioSystem

/**
 * Music source backed by local audio files inside the `Music` directory of the
 * client data folder. Migrated from the original [net.ccbluex.liquidbounce.features.module.modules.music.MusicPlayer]
 * folder scanning logic.
 */
object LocalMusicSource : MusicSource {

    override val id = "local"

    val musicDir: File by lazy {
        val path = File(FileManager.dir, "Music")
        if (!path.exists()) {
            path.mkdirs()
        }
        path
    }

    /**
     * Scan the music directory for supported audio files, sorted by name.
     */
    fun scan(): List<Track> {
        if (!musicDir.exists() || !musicDir.isDirectory) {
            return emptyList()
        }

        return musicDir.walk()
            .filter { file ->
                file.isFile && (
                    file.extension.equals("mp3", true) ||
                        file.extension.equals("wav", true) ||
                        file.extension.equals("flac", true)
                    )
            }
            .sortedBy { it.nameWithoutExtension.lowercase() }
            .map { Track.fromLocalFile(it) }
            .toList()
    }

    override fun openStream(track: Track): InputStream {
        val file = track.localFile
            ?: throw IllegalArgumentException("Local track has no file: ${track.displayName}")
        if (!file.exists()) {
            throw IllegalStateException("文件不存在: ${file.name}")
        }
        return BufferedInputStream(FileInputStream(file))
    }

    override fun loadLyrics(track: Track): ParsedLyrics {
        val file = track.localFile ?: return ParsedLyrics.EMPTY
        val lrcFile = File(file.parentFile, file.nameWithoutExtension + ".lrc")
        if (!lrcFile.exists()) {
            return ParsedLyrics.EMPTY
        }
        return try {
            LrcParser.parse(lrcFile.readText())
        } catch (e: Exception) {
            ClientUtils.LOGGER.warn("[MusicPlayer] 加载歌词失败: ${e.message}")
            ParsedLyrics.EMPTY
        }
    }

    /**
     * Best-effort track duration in milliseconds using the audio frame count,
     * falling back to a bitrate estimate. Migrated from the original player.
     */
    fun getDuration(file: File): Long {
        return try {
            val audioInputStream = AudioSystem.getAudioInputStream(file)
            val format = audioInputStream.format
            val frames = audioInputStream.frameLength
            audioInputStream.close()
            if (frames > 0 && format.frameRate > 0) {
                (frames * 1000L / format.frameRate).toLong()
            } else {
                estimateByBitrate(file)
            }
        } catch (e: Exception) {
            estimateByBitrate(file)
        }
    }

    private fun estimateByBitrate(file: File): Long {
        return try {
            val fileSize = file.length()
            val bitRate = 128000
            fileSize * 8L / bitRate * 1000L
        } catch (e: Exception) {
            0L
        }
    }
}
