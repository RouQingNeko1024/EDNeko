/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.music.core

import javazoom.jl.player.JavaSoundAudioDevice
import javazoom.jl.player.Player
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import java.io.BufferedInputStream
import java.io.InputStream
import javax.sound.sampled.FloatControl
import kotlin.concurrent.thread

/**
 * JLayer based MP3 playback engine. Plays any [InputStream] (local file or network
 * stream) on a dedicated thread and reports completion through a callback.
 *
 * This is intentionally source-agnostic: [net.ccbluex.liquidbounce.features.module.modules.music.MusicPlayer]
 * obtains a stream from a [MusicSource] and hands it here.
 */
class PlaybackEngine {

    private var player: Player? = null
    private var playThread: Thread? = null
    private var audioDevice: VolumeControlledAudioDevice? = null

    @Volatile
    private var playing = false

    @Volatile
    private var volume = 0.5F

    /**
     * Bumped on every [stop]. A playback thread only fires its callbacks while
     * its captured generation is still current, so a thread that was stopped or
     * superseded (e.g. when switching tracks) stays silent instead of reporting
     * a spurious error/completion.
     */
    @Volatile
    private var generation = 0

    val isPlaying: Boolean
        get() = playing && player != null

    /**
     * Start playing [stream] on a background thread. [onComplete] fires only when
     * playback finished naturally (not when [stop] was called). [onError] fires
     * with the throwable message when the stream could not be played.
     *
     * Any previous playback is stopped first.
     */
    fun play(
        stream: InputStream,
        onComplete: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        stop()

        val myGeneration = generation
        playing = true
        playThread = thread(start = true, name = "MusicPlayer-Thread") {
            try {
                val buffered = if (stream is BufferedInputStream) stream else BufferedInputStream(stream)
                val device = VolumeControlledAudioDevice()
                device.setVolume(volume)
                audioDevice = device

                val currentPlayer = Player(buffered, device)
                player = currentPlayer

                currentPlayer.play()

                if (myGeneration == generation && playing && currentPlayer.isComplete) {
                    onComplete()
                }
            } catch (e: InterruptedException) {
                // stop() interrupted us, nothing to report
            } catch (e: Exception) {
                // Only report if this thread is still the active playback (not a
                // deliberate stop / track switch).
                if (myGeneration == generation) {
                    ClientUtils.LOGGER.error("[MusicPlayer] 播放失败: ${e.message}")
                    playing = false
                    onError(e.message ?: "unknown error")
                }
            } finally {
                try {
                    stream.close()
                } catch (ignored: Exception) {
                }
            }
        }
    }

    fun stop() {
        playing = false
        generation++
        try {
            player?.close()
        } catch (ignored: Exception) {
        }
        player = null
        audioDevice = null
        playThread?.interrupt()
        playThread = null
    }

    fun setVolume(value: Float) {
        volume = value.coerceIn(0F, 1F)
        audioDevice?.setVolume(volume)
    }

    /**
     * Volume-aware [JavaSoundAudioDevice] that reaches into JLayer's private
     * source line to apply a master gain control.
     */
    private class VolumeControlledAudioDevice : JavaSoundAudioDevice() {
        private var volumeControl: FloatControl? = null

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
            } catch (ignored: Exception) {
            }
        }

        private fun findVolumeControl() {
            try {
                val field = JavaSoundAudioDevice::class.java.getDeclaredField("source")
                field.isAccessible = true
                val source = field.get(this) as? javax.sound.sampled.SourceDataLine
                if (source != null && source.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    volumeControl = source.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                }
            } catch (ignored: Exception) {
            }
        }
    }
}
