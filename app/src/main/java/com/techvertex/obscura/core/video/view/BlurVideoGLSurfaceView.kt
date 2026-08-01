package com.techvertex.obscura.core.video.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.techvertex.obscura.core.video.gl.VideoTextureRenderer
import com.techvertex.obscura.core.video.model.BlurVideoConfig

class BlurVideoGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var renderer: VideoTextureRenderer? = null
    private var onSurfaceTextureReady: ((SurfaceTexture) -> Unit)? = null

    fun initialize(onSurfaceTextureAvailable: (SurfaceTexture) -> Unit) {
        this.onSurfaceTextureReady = onSurfaceTextureAvailable

        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true

        renderer = VideoTextureRenderer { surfaceTexture ->
            post { onSurfaceTextureAvailable(surfaceTexture) }
        }

        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updateBlurConfig(config: BlurVideoConfig) {
        renderer?.updateBlurConfig(config)
    }

    fun updateCurrentTime(timeMs: Long) {
        renderer?.updateCurrentTime(timeMs)
    }

    fun setVideoSize(width: Int, height: Int) {
        renderer?.setVideoSize(width, height)
    }

    fun setShowOriginal(show: Boolean) {
        renderer?.showOriginal = show
    }

    fun releaseRenderer() {
        renderer?.release()
        renderer = null
    }
}
