package com.techvertex.obscura.core.video.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.techvertex.obscura.core.video.model.BlurVideoConfig
import com.techvertex.obscura.core.video.model.VideoBlurType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoTextureRenderer(
    private val onSurfaceTextureAvailable: (SurfaceTexture) -> Unit
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "VideoTextureRenderer"

        private val VERTEX_DATA = floatArrayOf(
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
        )
        private const val FLOAT_SIZE_BYTES = 4
        private const val VERTEX_STRIDE = 4 * FLOAT_SIZE_BYTES
    }

    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var currentProgram = 0
    private val programCache = mutableMapOf<VideoBlurType, Int>()
    private var passthroughProgram = 0

    private var fboId = 0
    private var fboTextureId = 0
    private var fboWidth = 0
    private var fboHeight = 0

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(VERTEX_DATA.size * FLOAT_SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(VERTEX_DATA)
            position(0)
        }

    @Volatile
    private var blurConfig: BlurVideoConfig = BlurVideoConfig()

    @Volatile
    private var videoWidth = 1

    @Volatile
    private var videoHeight = 1

    @Volatile
    private var surfaceWidth = 1

    @Volatile
    private var surfaceHeight = 1

    @Volatile
    private var currentTimeMs: Long = 0

    @Volatile
    private var updateSurface = false

    @Volatile
    private var isReleased = false

    @Volatile
    var showOriginal = false

    fun updateBlurConfig(config: BlurVideoConfig) {
        blurConfig = config
    }

    fun updateCurrentTime(timeMs: Long) {
        currentTimeMs = timeMs
    }

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
    }

    fun getSurfaceTexture(): SurfaceTexture? = surfaceTexture

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        oesTextureId = createOESTexture()

        surfaceTexture = SurfaceTexture(oesTextureId).apply {
            setOnFrameAvailableListener {
                updateSurface = true
            }
        }
        onSurfaceTextureAvailable(surfaceTexture!!)

        Matrix.setIdentityM(stMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)

        passthroughProgram = BlurShaderProgram.createProgram(
            BlurShaderProgram.VERTEX_SHADER,
            BlurShaderProgram.FRAGMENT_SHADER_PASSTHROUGH
        )

        currentProgram = BlurShaderProgram.createProgramForType(VideoBlurType.GAUSSIAN_BLUR)
        programCache[VideoBlurType.GAUSSIAN_BLUR] = currentProgram

        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        GLES20.glViewport(0, 0, width, height)
        setupFBO(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (isReleased) return

        if (updateSurface) {
            try {
                surfaceTexture?.updateTexImage()
                surfaceTexture?.getTransformMatrix(stMatrix)
                updateSurface = false
            } catch (e: Exception) {
                Log.e(TAG, "Error updating texture", e)
                return
            }
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val config = blurConfig

        buildCombinedStMatrix(config)
        buildMvpMatrix(config)

        val isBlurActive = currentTimeMs >= config.startTimeMs &&
                (config.endTimeMs == Long.MAX_VALUE || currentTimeMs <= config.endTimeMs)

        if (showOriginal || !isBlurActive || config.blurIntensity == 0) {
            drawWithProgram(passthroughProgram, oesTextureId, true, config)
        } else {
            val blurProgram = getOrCreateProgram(config.blurType)
            drawWithProgram(blurProgram, oesTextureId, true, config)
        }
    }

    private val combinedStMatrix = FloatArray(16)

    private fun buildCombinedStMatrix(config: BlurVideoConfig) {
        System.arraycopy(stMatrix, 0, combinedStMatrix, 0, 16)

        val rotationDeg = config.rotationDegrees
        val cropRect = config.cropRect

        if (rotationDeg != 0) {
            val rotMatrix = FloatArray(16)
            Matrix.setIdentityM(rotMatrix, 0)
            Matrix.translateM(rotMatrix, 0, 0.5f, 0.5f, 0f)
            Matrix.rotateM(rotMatrix, 0, rotationDeg.toFloat(), 0f, 0f, 1f)
            Matrix.translateM(rotMatrix, 0, -0.5f, -0.5f, 0f)

            val temp = FloatArray(16)
            Matrix.multiplyMM(temp, 0, combinedStMatrix, 0, rotMatrix, 0)
            System.arraycopy(temp, 0, combinedStMatrix, 0, 16)
        }

        if (cropRect.left != 0f || cropRect.top != 0f || cropRect.right != 1f || cropRect.bottom != 1f) {
            val cropW = cropRect.width()
            val cropH = cropRect.height()
            val tcLeft = cropRect.left
            val tcBottom = 1f - cropRect.bottom

            val cropMatrix = FloatArray(16)
            Matrix.setIdentityM(cropMatrix, 0)
            Matrix.scaleM(cropMatrix, 0, cropW, cropH, 1f)
            Matrix.translateM(cropMatrix, 0, tcLeft / cropW, tcBottom / cropH, 0f)

            val temp = FloatArray(16)
            Matrix.multiplyMM(temp, 0, combinedStMatrix, 0, cropMatrix, 0)
            System.arraycopy(temp, 0, combinedStMatrix, 0, 16)
        }
    }

    private fun buildMvpMatrix(config: BlurVideoConfig) {
        Matrix.setIdentityM(mvpMatrix, 0)

        if (videoWidth <= 1 || videoHeight <= 1 || surfaceWidth <= 1 || surfaceHeight <= 1) return

        val rotationDeg = config.rotationDegrees
        val cropRect = config.cropRect

        val rotW = if (rotationDeg == 90 || rotationDeg == 270) videoHeight.toFloat() else videoWidth.toFloat()
        val rotH = if (rotationDeg == 90 || rotationDeg == 270) videoWidth.toFloat() else videoHeight.toFloat()

        val croppedW = rotW * cropRect.width()
        val croppedH = rotH * cropRect.height()

        val videoAspect = croppedW / croppedH
        val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()

        if (videoAspect > surfaceAspect) {
            Matrix.scaleM(mvpMatrix, 0, 1f, surfaceAspect / videoAspect, 1f)
        } else {
            Matrix.scaleM(mvpMatrix, 0, videoAspect / surfaceAspect, 1f, 1f)
        }
    }

    private fun transformFrameRectToShaderSpace(
        rect: android.graphics.RectF,
        config: BlurVideoConfig
    ): FloatArray {
        var vidLeft = 0f
        var vidTop = 0f
        var vidRight = 1f
        var vidBottom = 1f

        if (videoWidth > 1 && videoHeight > 1 && surfaceWidth > 1 && surfaceHeight > 1) {
            val rotDeg = config.rotationDegrees
            val cr = config.cropRect
            val rotW = if (rotDeg == 90 || rotDeg == 270) videoHeight.toFloat() else videoWidth.toFloat()
            val rotH = if (rotDeg == 90 || rotDeg == 270) videoWidth.toFloat() else videoHeight.toFloat()
            val croppedW = rotW * cr.width()
            val croppedH = rotH * cr.height()
            val videoAspect = croppedW / croppedH
            val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()

            if (videoAspect > surfaceAspect) {
                val displayH = surfaceAspect / videoAspect
                vidTop = (1f - displayH) / 2f
                vidBottom = (1f + displayH) / 2f
            } else {
                val displayW = videoAspect / surfaceAspect
                vidLeft = (1f - displayW) / 2f
                vidRight = (1f + displayW) / 2f
            }
        }

        val vidW = vidRight - vidLeft
        val vidH = vidBottom - vidTop

        val quadLeft = (rect.left - vidLeft) / vidW
        val quadTop = (rect.top - vidTop) / vidH
        val quadRight = (rect.right - vidLeft) / vidW
        val quadBottom = (rect.bottom - vidTop) / vidH

        return floatArrayOf(quadLeft, 1f - quadBottom, quadRight, 1f - quadTop)
    }

    private fun drawWithProgram(
        program: Int,
        textureId: Int,
        isOES: Boolean,
        config: BlurVideoConfig
    ) {
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        if (isOES) {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        }

        val positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        GLES20.glEnableVertexAttribArray(positionHandle)
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            vertexBuffer
        )

        val texCoordHandle = GLES20.glGetAttribLocation(program, "a_TextureCoord")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        vertexBuffer.position(2)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            vertexBuffer
        )

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        val stMatrixHandle = GLES20.glGetUniformLocation(program, "u_STMatrix")
        GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, combinedStMatrix, 0)

        val intensityHandle = GLES20.glGetUniformLocation(program, "u_BlurIntensity")
        if (intensityHandle >= 0) {
            GLES20.glUniform1f(intensityHandle, blurConfig.blurIntensity / 100f)
        }

        val resolutionHandle = GLES20.glGetUniformLocation(program, "u_Resolution")
        if (resolutionHandle >= 0) {
            GLES20.glUniform2f(resolutionHandle, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        }

        val frameRectHandle = GLES20.glGetUniformLocation(program, "u_FrameRect")
        if (frameRectHandle >= 0) {
            val fr = transformFrameRectToShaderSpace(config.frameRect, config)
            GLES20.glUniform4f(frameRectHandle, fr[0], fr[1], fr[2], fr[3])
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }

    private fun getOrCreateProgram(type: VideoBlurType): Int {
        return programCache.getOrPut(type) {
            BlurShaderProgram.createProgramForType(type)
        }
    }

    private fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        return textures[0]
    }

    private fun setupFBO(width: Int, height: Int) {
        if (fboId != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            GLES20.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
        }

        fboWidth = width
        fboHeight = height

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        fboTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        val fbos = IntArray(1)
        GLES20.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            fboTextureId,
            0
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun release() {
        isReleased = true
        surfaceTexture?.release()
        surfaceTexture = null
        if (oesTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
        }
        if (fboId != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            GLES20.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
        }
        programCache.values.forEach { GLES20.glDeleteProgram(it) }
        programCache.clear()
        if (passthroughProgram != 0) GLES20.glDeleteProgram(passthroughProgram)
    }
}
