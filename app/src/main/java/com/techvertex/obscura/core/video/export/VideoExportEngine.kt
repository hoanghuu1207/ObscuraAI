package com.techvertex.obscura.core.video.export

import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import com.techvertex.obscura.core.video.gl.BlurShaderProgram
import com.techvertex.obscura.core.video.model.BlurVideoConfig
import com.techvertex.obscura.core.video.model.VideoBlurConstants
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VideoExportEngine(
    private val context: Context,
    private val config: VideoExportConfig,
    private val onProgress: (Int) -> Unit
) {

    companion object {
        private const val TAG = "VideoExportEngine"
        private const val TIMEOUT_US = 10000L
        private const val VIDEO_MIME_TYPE = "video/avc"
        private const val DEFAULT_BITRATE = 8_000_000
        private const val DEFAULT_FRAME_RATE = 30
        private const val DEFAULT_IFRAME_INTERVAL = 1
        private const val FLOAT_SIZE_BYTES = 4
        private const val VERTEX_STRIDE = 4 * FLOAT_SIZE_BYTES

        private val VERTEX_DATA = floatArrayOf(
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
        )
    }

    private var isCancelled = false

    fun cancel() {
        isCancelled = true
    }

    fun execute(): String? {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null

        var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        var decoderSurfaceTexture: SurfaceTexture? = null
        var decoderSurface: Surface? = null
        var oesTextureId = 0

        try {
            val fd = context.contentResolver.openFileDescriptor(config.inputUri, "r")
                ?: throw IllegalStateException("Cannot open input video")
            extractor.setDataSource(fd.fileDescriptor)

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && videoTrackIndex == -1) {
                    videoTrackIndex = i
                    videoFormat = format
                } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                    audioTrackIndex = i
                    audioFormat = format
                }
            }

            if (videoTrackIndex == -1 || videoFormat == null) {
                Log.e(TAG, "No video track found")
                return null
            }

            val origWidth = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
            val origHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
            val duration = if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) {
                videoFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            val blurConfig = config.blurConfigs.firstOrNull() ?: BlurVideoConfig()
            val rotationDeg = blurConfig.rotationDegrees
            val cropRect = blurConfig.cropRect

            val rotatedWidth = if (rotationDeg == 90 || rotationDeg == 270) origHeight else origWidth
            val rotatedHeight = if (rotationDeg == 90 || rotationDeg == 270) origWidth else origHeight

            val outputWidth = (rotatedWidth * cropRect.width()).toInt().let { if (it % 2 != 0) it + 1 else it }
            val outputHeight = (rotatedHeight * cropRect.height()).toInt().let { if (it % 2 != 0) it + 1 else it }

            Log.d(TAG, "Video: ${origWidth}x${origHeight}, rotation=$rotationDeg, crop=$cropRect, output=${outputWidth}x${outputHeight}, duration=${duration}us")

            val outputResult = createOutputViaMediaStore()
                ?: throw IllegalStateException("Cannot create output file")
            val outputPfd = outputResult.first
            val outputDisplayPath = outputResult.second

            muxer = MediaMuxer(outputPfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val encoderFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, outputWidth, outputHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, getBitRate(videoFormat))
                setInteger(MediaFormat.KEY_FRAME_RATE, getFrameRate(videoFormat))
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_IFRAME_INTERVAL)
            }

            encoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE)
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            val encoderInputSurface = encoder.createInputSurface()

            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) throw RuntimeException("eglGetDisplay failed")

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                throw RuntimeException("eglInitialize failed")
            }

            val configAttribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val eglConfig = configs[0] ?: throw RuntimeException("eglChooseConfig failed")

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) throw RuntimeException("eglCreateContext failed")

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, encoderInputSurface, surfaceAttribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) throw RuntimeException("eglCreateWindowSurface failed")

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            oesTextureId = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            decoderSurfaceTexture = SurfaceTexture(oesTextureId)
            decoderSurfaceTexture.setDefaultBufferSize(origWidth, origHeight)

            val frameSyncObject = Object()
            var frameAvailable = false
            decoderSurfaceTexture.setOnFrameAvailableListener {
                synchronized(frameSyncObject) {
                    frameAvailable = true
                    frameSyncObject.notifyAll()
                }
            }

            decoderSurface = Surface(decoderSurfaceTexture)

            val blurProgram = BlurShaderProgram.createProgramForType(blurConfig.blurType)
            val passthroughProgram = BlurShaderProgram.createProgram(
                BlurShaderProgram.VERTEX_SHADER,
                BlurShaderProgram.FRAGMENT_SHADER_PASSTHROUGH
            )

            val vertexBuffer: FloatBuffer = ByteBuffer
                .allocateDirect(VERTEX_DATA.size * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(VERTEX_DATA)
                    position(0)
                }

            val stMatrix = FloatArray(16)
            val combinedStMatrix = FloatArray(16)
            val mvpMatrix = FloatArray(16)
            Matrix.setIdentityM(mvpMatrix, 0)

            encoder.start()

            val decoderMime = videoFormat.getString(MediaFormat.KEY_MIME) ?: VIDEO_MIME_TYPE
            decoder = MediaCodec.createDecoderByType(decoderMime)
            decoder.configure(videoFormat, decoderSurface, null, 0)
            decoder.start()

            extractor.selectTrack(videoTrackIndex)

            GLES20.glViewport(0, 0, outputWidth, outputHeight)
            GLES20.glClearColor(0f, 0f, 0f, 1f)

            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var muxerStarted = false
            var allInputExtracted = false
            var allDecoderDone = false
            var allEncoderDone = false
            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()

            while (!allEncoderDone && !isCancelled) {
                if (!allInputExtracted) {
                    val inIdx = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val buffer = decoder.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            allInputExtracted = true
                        } else {
                            val pts = extractor.sampleTime
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()

                            if (duration > 0) {
                                val progress = ((pts * 100) / duration).toInt().coerceIn(0, 99)
                                onProgress(progress)
                            }
                        }
                    }
                }

                if (!allDecoderDone) {
                    val outIdx = decoder.dequeueOutputBuffer(decoderBufferInfo, TIMEOUT_US)
                    if (outIdx >= 0) {
                        val isEos = (decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                        if (decoderBufferInfo.size > 0) {
                            decoder.releaseOutputBuffer(outIdx, true)

                            synchronized(frameSyncObject) {
                                while (!frameAvailable) {
                                    frameSyncObject.wait(5000)
                                    if (!frameAvailable) break
                                }
                                frameAvailable = false
                            }

                            decoderSurfaceTexture.updateTexImage()
                            decoderSurfaceTexture.getTransformMatrix(stMatrix)

                            System.arraycopy(stMatrix, 0, combinedStMatrix, 0, 16)
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
                                val cW = cropRect.width()
                                val cH = cropRect.height()
                                val tcLeft = cropRect.left
                                val tcBottom = 1f - cropRect.bottom

                                val cropMatrix = FloatArray(16)
                                Matrix.setIdentityM(cropMatrix, 0)
                                Matrix.scaleM(cropMatrix, 0, cW, cH, 1f)
                                Matrix.translateM(cropMatrix, 0, tcLeft / cW, tcBottom / cH, 0f)

                                val temp = FloatArray(16)
                                Matrix.multiplyMM(temp, 0, combinedStMatrix, 0, cropMatrix, 0)
                                System.arraycopy(temp, 0, combinedStMatrix, 0, 16)
                            }

                            val currentTimeMs = decoderBufferInfo.presentationTimeUs / 1000
                            val isBlurActive = currentTimeMs >= blurConfig.startTimeMs &&
                                    (blurConfig.endTimeMs == Long.MAX_VALUE || currentTimeMs <= blurConfig.endTimeMs)

                            val programToUse = if (isBlurActive && blurConfig.blurIntensity > 0 && blurProgram != 0) {
                                blurProgram
                            } else {
                                passthroughProgram
                            }

                            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                            GLES20.glUseProgram(programToUse)

                            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)

                            val posHandle = GLES20.glGetAttribLocation(programToUse, "a_Position")
                            GLES20.glEnableVertexAttribArray(posHandle)
                            vertexBuffer.position(0)
                            GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer)

                            val texHandle = GLES20.glGetAttribLocation(programToUse, "a_TextureCoord")
                            GLES20.glEnableVertexAttribArray(texHandle)
                            vertexBuffer.position(2)
                            GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer)

                            val mvpHandle = GLES20.glGetUniformLocation(programToUse, "u_MVPMatrix")
                            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)

                            val stHandle = GLES20.glGetUniformLocation(programToUse, "u_STMatrix")
                            GLES20.glUniformMatrix4fv(stHandle, 1, false, combinedStMatrix, 0)

                            val intensityHandle = GLES20.glGetUniformLocation(programToUse, "u_BlurIntensity")
                            if (intensityHandle >= 0) {
                                GLES20.glUniform1f(intensityHandle, blurConfig.blurIntensity / 100f)
                            }

                            val resolutionHandle = GLES20.glGetUniformLocation(programToUse, "u_Resolution")
                            if (resolutionHandle >= 0) {
                                GLES20.glUniform2f(resolutionHandle, outputWidth.toFloat(), outputHeight.toFloat())
                            }

                            val frameRectHandle = GLES20.glGetUniformLocation(programToUse, "u_FrameRect")
                            if (frameRectHandle >= 0) {
                                val rect = if (!config.faceTrackKeyframes.isNullOrEmpty()) {
                                    com.techvertex.obscura.core.video.face.VideoFaceScanner.getInterpolatedFaceRect(currentTimeMs, config.faceTrackKeyframes) ?: blurConfig.frameRect
                                } else {
                                    blurConfig.frameRect
                                }
                                val vb = blurConfig.videoBounds
                                val vbW = vb.width().coerceAtLeast(0.001f)
                                val vbH = vb.height().coerceAtLeast(0.001f)

                                val videoLeft = (rect.left - vb.left) / vbW
                                val videoTop = (rect.top - vb.top) / vbH
                                val videoRight = (rect.right - vb.left) / vbW
                                val videoBottom = (rect.bottom - vb.top) / vbH

                                GLES20.glUniform4f(frameRectHandle, videoLeft, 1f - videoBottom, videoRight, 1f - videoTop)
                            }

                            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

                            GLES20.glDisableVertexAttribArray(posHandle)
                            GLES20.glDisableVertexAttribArray(texHandle)
                            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

                            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, decoderBufferInfo.presentationTimeUs * 1000L)
                            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                        } else {
                            decoder.releaseOutputBuffer(outIdx, false)
                        }

                        if (isEos) {
                            encoder.signalEndOfInputStream()
                            allDecoderDone = true
                        }
                    }
                }

                while (true) {
                    val encIdx = encoder.dequeueOutputBuffer(encoderBufferInfo, 0)
                    if (encIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            muxerVideoTrack = muxer.addTrack(encoder.outputFormat)
                            if (audioFormat != null) {
                                muxerAudioTrack = muxer.addTrack(audioFormat)
                            }
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (encIdx >= 0) {
                        val encodedData = encoder.getOutputBuffer(encIdx)!!
                        if (encoderBufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(encoderBufferInfo.offset)
                            encodedData.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                            muxer.writeSampleData(muxerVideoTrack, encodedData, encoderBufferInfo)
                        }
                        val isEncoderEos = (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        encoder.releaseOutputBuffer(encIdx, false)
                        if (isEncoderEos) {
                            allEncoderDone = true
                            break
                        }
                    } else {
                        break
                    }
                }
            }

            if (!isCancelled) {
                if (audioTrackIndex != -1 && muxerAudioTrack != -1 && muxerStarted) {
                    copyAudioTrack(extractor, audioTrackIndex, muxer, muxerAudioTrack)
                }

                if (!isCancelled) {
                    onProgress(100)
                    finishMediaStoreEntry()
                } else {
                    cleanupPendingFile()
                }
            } else {
                cleanupPendingFile()
            }

            fd.close()

            if (blurProgram != 0) GLES20.glDeleteProgram(blurProgram)
            if (passthroughProgram != 0) GLES20.glDeleteProgram(passthroughProgram)

            outputPfd.close()

            return if (isCancelled) null else outputDisplayPath

        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            return null
        } finally {
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { decoderSurface?.release() } catch (_: Exception) {}
            try { decoderSurfaceTexture?.release() } catch (_: Exception) {}
            if (oesTextureId != 0) {
                try { GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0) } catch (_: Exception) {}
            }
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglTerminate(eglDisplay)
            }
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
            extractor.release()
        }
    }

    private fun copyAudioTrack(
        extractor: MediaExtractor,
        audioTrackIndex: Int,
        muxer: MediaMuxer,
        muxerAudioTrack: Int
    ) {
        try {
            val audioExtractor = MediaExtractor()
            val fd = context.contentResolver.openFileDescriptor(config.inputUri, "r") ?: return
            audioExtractor.setDataSource(fd.fileDescriptor)
            audioExtractor.selectTrack(audioTrackIndex)

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            while (!isCancelled) {
                val sampleSize = audioExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                bufferInfo.flags = audioExtractor.sampleFlags

                muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                audioExtractor.advance()
            }

            audioExtractor.release()
            fd.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error copying audio track", e)
        }
    }

    private var pendingMediaUri: android.net.Uri? = null

    private fun createOutputViaMediaStore(): Pair<android.os.ParcelFileDescriptor, String>? {
        val fileName = "blur_video_${System.currentTimeMillis()}.mp4"
        val relativePath = "${Environment.DIRECTORY_DCIM}/${VideoBlurConstants.EXPORT_FOLDER_NAME}"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return null

            val pfd = context.contentResolver.openFileDescriptor(uri, "rw") ?: return null
            pendingMediaUri = uri
            val displayPath = "$relativePath/$fileName"
            Pair(pfd, displayPath)
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                VideoBlurConstants.EXPORT_FOLDER_NAME
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            val pfd = android.os.ParcelFileDescriptor.open(
                file,
                android.os.ParcelFileDescriptor.MODE_CREATE or android.os.ParcelFileDescriptor.MODE_READ_WRITE
            )
            Pair(pfd, file.absolutePath)
        }
    }

    private fun cleanupPendingFile() {
        val uri = pendingMediaUri ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting cancelled file", e)
            }
        }
    }

    private fun finishMediaStoreEntry() {
        val uri = pendingMediaUri ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing MediaStore entry", e)
            }
        }
    }

    private fun getBitRate(format: MediaFormat): Int {
        return try {
            format.getInteger(MediaFormat.KEY_BIT_RATE)
        } catch (_: Exception) {
            DEFAULT_BITRATE
        }
    }

    private fun getFrameRate(format: MediaFormat): Int {
        return try {
            format.getInteger(MediaFormat.KEY_FRAME_RATE)
        } catch (_: Exception) {
            DEFAULT_FRAME_RATE
        }
    }
}
