package com.techvertex.obscura.core.video.player

import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

class BlurVideoPlayer(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var surface: Surface? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var positionUpdateRunnable: Runnable? = null

    var onPositionChanged: ((Long) -> Unit)? = null
    var onDurationReady: ((Long) -> Unit)? = null
    var onVideoSizeChanged: ((Int, Int) -> Unit)? = null
    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null

    fun setSurfaceTexture(surfaceTexture: SurfaceTexture) {
        surface?.release()
        val newSurface = Surface(surfaceTexture)
        surface = newSurface
        exoPlayer?.setVideoSurface(newSurface)
    }

    fun initialize(uri: Uri, surfaceTexture: SurfaceTexture) {
        if (exoPlayer != null) {
            setSurfaceTexture(surfaceTexture)
            return
        }

        surface = Surface(surfaceTexture)

        exoPlayer = ExoPlayer.Builder(context).build().apply {
            setVideoSurface(surface)
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0.5f

            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        onVideoSizeChanged?.invoke(videoSize.width, videoSize.height)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        onDurationReady?.invoke(duration)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onPlaybackStateChanged?.invoke(isPlaying)
                    if (isPlaying) {
                        startPositionTracking()
                    } else {
                        stopPositionTracking()
                    }
                }
            })

            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0

    fun getDuration(): Long = exoPlayer?.duration ?: 0

    fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume
    }

    fun release() {
        stopPositionTracking()
        exoPlayer?.release()
        exoPlayer = null
        surface?.release()
        surface = null
    }

    private fun startPositionTracking() {
        stopPositionTracking()
        positionUpdateRunnable = object : Runnable {
            override fun run() {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        onPositionChanged?.invoke(player.currentPosition)
                        mainHandler.postDelayed(this, 100)
                    }
                }
            }
        }
        mainHandler.post(positionUpdateRunnable!!)
    }

    private fun stopPositionTracking() {
        positionUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        positionUpdateRunnable = null
    }
}
