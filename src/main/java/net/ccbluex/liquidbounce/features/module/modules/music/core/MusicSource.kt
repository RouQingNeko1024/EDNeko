/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.music.core

import java.io.InputStream

/**
 * Parsed LRC lyrics: [timestamps] (in milliseconds) is index-aligned with [lines].
 */
data class ParsedLyrics(
    val timestamps: List<Long>,
    val lines: List<String>
) {
    val isEmpty: Boolean
        get() = lines.isEmpty()

    /**
     * Total duration hint derived from the last timestamp, used when no other
     * duration is available. Returns 0 when there are no timed lines.
     */
    val durationHintMs: Long
        get() = if (timestamps.isEmpty()) 0L else timestamps.last() + 10_000L

    companion object {
        val EMPTY = ParsedLyrics(emptyList(), emptyList())
    }
}

/**
 * A playable music source. Concrete implementations decide how songs are
 * discovered (local folder scan vs. online search), but every source must be
 * able to open a playable stream and (optionally) provide lyrics for a [Track].
 */
interface MusicSource {

    /**
     * Stable identifier, e.g. "local" or "netease".
     */
    val id: String

    /**
     * Open a decodable audio stream for [track]. The caller owns the returned
     * stream and is responsible for closing it.
     *
     * May perform network I/O, so it must never be called from the render thread.
     */
    fun openStream(track: Track): InputStream

    /**
     * Load lyrics for [track], or [ParsedLyrics.EMPTY] when none are available.
     */
    fun loadLyrics(track: Track): ParsedLyrics
}

/**
 * Shared LRC parser used by both local `.lrc` files and online lyric APIs.
 *
 * Supports the standard `[mm:ss.xx]` / `[mm:ss.xxx]` timestamp format and
 * multiple timestamps on a single line.
 */
object LrcParser {

    private val timeRegex = Regex("\\[(\\d{1,2}):(\\d{2})[.:](\\d{2,3})]")

    fun parse(content: String): ParsedLyrics {
        if (content.isBlank()) {
            return ParsedLyrics.EMPTY
        }

        val timestamps = mutableListOf<Long>()
        val lines = mutableListOf<String>()

        content.lines().forEach { rawLine ->
            if (rawLine.isBlank() || !rawLine.contains("[")) {
                return@forEach
            }

            val matches = timeRegex.findAll(rawLine).toList()
            if (matches.isEmpty()) {
                return@forEach
            }

            val text = rawLine.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) {
                return@forEach
            }

            matches.forEach { match ->
                val (minutes, seconds, millis) = match.destructured
                val ms = minutes.toLong() * 60_000L + seconds.toLong() * 1_000L +
                    if (millis.length == 2) millis.toLong() * 10 else millis.toLong()
                timestamps.add(ms)
                lines.add(text)
            }
        }

        if (lines.isEmpty()) {
            return ParsedLyrics.EMPTY
        }

        // Sort by timestamp so multi-timestamp lines stay in chronological order.
        val sorted = timestamps.indices.sortedBy { timestamps[it] }
        return ParsedLyrics(
            timestamps = sorted.map { timestamps[it] },
            lines = sorted.map { lines[it] }
        )
    }
}
