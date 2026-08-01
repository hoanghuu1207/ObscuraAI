package com.techvertex.obscura.core.video.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoFaceScanner(private val context: Context) {

    companion object {
        private const val TAG = "VideoFaceScanner"
        private const val SAMPLE_INTERVAL_MS = 250L // Sample frame every 250ms

        fun getInterpolatedFaceRect(timeMs: Long, timedRects: List<TimedFaceRect>): RectF? {
            if (timedRects.isEmpty()) return null
            if (timedRects.size == 1) return timedRects.first().rect

            if (timeMs <= timedRects.first().timeMs) return timedRects.first().rect
            if (timeMs >= timedRects.last().timeMs) return timedRects.last().rect

            // Find surrounding keyframes
            for (i in 0 until timedRects.size - 1) {
                val current = timedRects[i]
                val next = timedRects[i + 1]

                if (timeMs in current.timeMs..next.timeMs) {
                    val timeDiff = (next.timeMs - current.timeMs).toFloat().coerceAtLeast(1f)
                    val factor = (timeMs - current.timeMs) / timeDiff

                    val left = current.rect.left + factor * (next.rect.left - current.rect.left)
                    val top = current.rect.top + factor * (next.rect.top - current.rect.top)
                    val right = current.rect.right + factor * (next.rect.right - current.rect.right)
                    val bottom = current.rect.bottom + factor * (next.rect.bottom - current.rect.bottom)

                    return RectF(left, top, right, bottom)
                }
            }

            return timedRects.last().rect
        }
    }

    suspend fun scanVideoForFaces(
        videoUri: Uri,
        onProgress: (Int) -> Unit
    ): List<TimedFaceRect> = withContext(Dispatchers.IO) {
        val faceDetector = FaceDetectorEngine()
        val timedRects = mutableListOf<TimedFaceRect>()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            if (durationMs <= 0) {
                faceDetector.close()
                return@withContext emptyList()
            }

            var currentTimeMs = 0L
            val totalSteps = (durationMs / SAMPLE_INTERVAL_MS).toInt().coerceAtLeast(1)
            var currentStep = 0

            while (currentTimeMs <= durationMs) {
                val timeUs = currentTimeMs * 1000L
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                if (bitmap != null) {
                    val scaledBitmap = scaleBitmapIfNeeded(bitmap, 480)
                    val detectedFaces = faceDetector.detectFaces(scaledBitmap)

                    if (bitmap != scaledBitmap) {
                        scaledBitmap.recycle()
                    }
                    bitmap.recycle()

                    if (detectedFaces.isNotEmpty()) {
                        // Take the primary/first detected face
                        val primaryFace = detectedFaces.first()
                        timedRects.add(TimedFaceRect(currentTimeMs, primaryFace))
                    }
                }

                currentStep++
                val progress = ((currentStep * 100) / totalSteps).coerceIn(0, 100)
                onProgress(progress)

                currentTimeMs += SAMPLE_INTERVAL_MS
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning video for faces", e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
            faceDetector.close()
        }

        return@withContext timedRects
    }


    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxDim
            newHeight = (maxDim / aspectRatio).toInt()
        } else {
            newHeight = maxDim
            newWidth = (maxDim * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
