package com.inktone.app.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object MusicPlayer {

    private var player: MediaPlayer? = null
    private var fadeJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var currentId: String? = null
        private set
    var volume: Float = 0.8f
        private set
    private var currentVolume: Float = 0f

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun position(): Int = player?.currentPosition ?: 0

    fun duration(): Int = player?.duration ?: 0

    fun seekTo(ms: Int) {
        runCatching { player?.seekTo(ms) }
    }

    fun play(path: String, id: String) {
        if (currentId == id && player != null) {
            if (player?.isPlaying != true) player?.start()
            fadeTo(volume)
            return
        }
        release()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(path)
                isLooping = true
                prepare()
            }
            player = mp
            currentId = id
            currentVolume = 0f
            mp.start()
            fadeTo(volume)
        } catch (e: Exception) {
            release()
        }
    }

    fun pause() {
        fadeTo(0f) {
            player?.pause()
            currentVolume = 0f
        }
    }

    fun stop() {
        release()
    }

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        if (isPlaying()) {
            fadeJob?.cancel()
            currentVolume = volume
            player?.setVolume(volume, volume)
        }
    }

    private fun fadeTo(target: Float, onEnd: (() -> Unit)? = null) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val p = player ?: return@launch
            val start = currentVolume
            val steps = 24
            for (i in 1..steps) {
                val v = start + (target - start) * (i.toFloat() / steps)
                currentVolume = v
                p.setVolume(v, v)
                delay(35)
            }
            onEnd?.invoke()
        }
    }

    private fun release() {
        fadeJob?.cancel()
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
        currentId = null
        currentVolume = 0f
    }
}
