/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.music.core

import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.io.defaultAgent
import net.ccbluex.liquidbounce.utils.io.newCall
import net.ccbluex.liquidbounce.utils.io.parseJson
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Online music source backed by Netease Cloud Music public web endpoints.
 *
 * Self-contained: no private server required. All requests carry a
 * `Referer: https://music.163.com` header so the public API accepts them.
 *
 * Limitations: `outer/url` only returns non-VIP standard quality MP3 streams.
 * JLayer decodes MP3, so that is compatible. VIP / removed songs return an empty
 * body and surface as an error so the caller can skip them.
 */
object NeteaseMusicSource : MusicSource {

    override val id = "netease"

    /**
     * Configurable domain so a mirror can be used if the official one fails.
     */
    @Volatile
    var domain: String = "music.163.com"

    private val referer: String
        get() = "https://$domain"

    /**
     * Cache directory for downloaded songs. Kept separate from the local Music
     * folder so cached files are not picked up by [LocalMusicSource.scan].
     */
    private val cacheDir: File by lazy {
        val dir = File(FileManager.dir, "MusicCache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private fun requestBody(url: String): String? {
        return try {
            HttpClient.newCall {
                url(url)
                    .defaultAgent()
                    .header("Referer", referer)
//                    .header("Cookie", "appver=1.5.2; os=pc")
                    .get()
            }.execute().use { response ->
                if (!response.isSuccessful) {
                    ClientUtils.LOGGER.warn("[MusicPlayer] 网易云请求失败: HTTP ${response.code} ($url)")
                    return null
                }
                response.body.string()
            }
        } catch (e: Exception) {
            ClientUtils.LOGGER.warn("[MusicPlayer] 网易云请求异常: ${e.message}")
            null
        }
    }

    /**
     * Search songs by keyword. Returns up to [limit] tracks.
     */
    fun search(keyword: String, limit: Int): List<Track> {
        val encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
        val url = "$referer/api/search/get/web?s=$encoded&type=1&offset=0&limit=$limit"
        val body = requestBody(url) ?: return emptyList()

        return try {
            val root = body.parseJson().asJsonObject
            val result = root.getAsJsonObject("result") ?: return emptyList()
            val songs = result.getAsJsonArray("songs") ?: return emptyList()

            songs.mapNotNull { element ->
                runCatching {
                    val song = element.asJsonObject
                    val id = song.get("id").asLong
                    val name = song.get("name").asString
                    val artist = song.getAsJsonArray("artists")
                        ?.joinToString("/") { it.asJsonObject.get("name").asString }
                        ?: ""
                    val duration = song.get("duration")?.asLong ?: 0L
                    Track(
                        title = name,
                        artist = artist,
                        durationMs = duration,
                        source = TrackSource.NETEASE,
                        neteaseId = id
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            ClientUtils.LOGGER.warn("[MusicPlayer] 网易云搜索解析失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetch a single track by Netease song id. Network call; run off the render thread.
     */
    fun fetchTrack(id: Long): Track? {
        val url = "$referer/api/song/detail/?ids=[$id]"
        val body = requestBody(url) ?: return null

        return try {
            val root = body.parseJson().asJsonObject
            val songs = root.getAsJsonArray("songs") ?: return null
            if (songs.size() == 0) return null

            val song = songs[0].asJsonObject
            val songId = song.get("id").asLong
            val name = song.get("name").asString
            val artist = song.getAsJsonArray("artists")
                ?.joinToString("/") { it.asJsonObject.get("name").asString }
                ?: ""
            val duration = song.get("duration")?.asLong ?: 0L
            Track(
                title = name,
                artist = artist,
                durationMs = duration,
                source = TrackSource.NETEASE,
                neteaseId = songId
            )
        } catch (e: Exception) {
            ClientUtils.LOGGER.warn("[MusicPlayer] 网易云歌曲详情解析失败: ${e.message}")
            null
        }
    }

    /**
     * Download the song to a local cache file and play from there, rather than
     * streaming directly. A local file is immune to network stalls/timeouts
     * during decoding, and lets us reuse the file on replay.
     */
    override fun openStream(track: Track): InputStream {
        val id = track.neteaseId
            ?: throw IllegalArgumentException("Netease track has no id: ${track.displayName}")

        val cacheFile = File(cacheDir, "netease_$id.mp3")
        if (!cacheFile.exists() || cacheFile.length() <= 0L) {
            downloadToCache(id, cacheFile)
        }

        return BufferedInputStream(FileInputStream(cacheFile))
    }

    private fun downloadToCache(id: Long, target: File) {
        val url = "$referer/song/media/outer/url?id=$id.mp3"
        HttpClient.newCall {
            url(url)
                .defaultAgent()
                .header("Referer", referer)
                .get()
        }.execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("无法获取播放流 (HTTP ${response.code})，可能为 VIP/下架歌曲")
            }

            if (response.body.contentLength() == 0L) {
                throw IllegalStateException("歌曲不可播放（VIP/下架/无版权）")
            }

            val tempFile = File(target.parentFile, "${target.name}.part")
            try {
                tempFile.outputStream().use { out ->
                    response.body.byteStream().use { input -> input.copyTo(out) }
                }
                if (tempFile.length() <= 0L) {
                    throw IllegalStateException("下载失败：空文件（VIP/下架/无版权）")
                }
                if (target.exists()) {
                    target.delete()
                }
                if (!tempFile.renameTo(target)) {
                    tempFile.copyTo(target, overwrite = true)
                    tempFile.delete()
                }
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }
    }

    override fun loadLyrics(track: Track): ParsedLyrics {
        val id = track.neteaseId ?: return ParsedLyrics.EMPTY
        val url = "$referer/api/song/lyric?id=$id&lv=1&kv=1&tv=-1"
        val body = requestBody(url) ?: return ParsedLyrics.EMPTY

        return try {
            val root = body.parseJson().asJsonObject
            val lrc = root.getAsJsonObject("lrc") ?: return ParsedLyrics.EMPTY
            val lyric = lrc.get("lyric")?.takeIf { !it.isJsonNull }?.asString ?: return ParsedLyrics.EMPTY
            LrcParser.parse(lyric)
        } catch (e: Exception) {
            ClientUtils.LOGGER.warn("[MusicPlayer] 网易云歌词解析失败: ${e.message}")
            ParsedLyrics.EMPTY
        }
    }
}
