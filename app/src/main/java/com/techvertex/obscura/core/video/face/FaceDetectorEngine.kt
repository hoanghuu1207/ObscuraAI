package com.techvertex.obscura.core.video.face

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FaceDetectorEngine {

    companion object {
        private const val TAG = "FaceDetectorEngine"
    }

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    suspend fun detectFaces(bitmap: Bitmap): List<RectF> = suspendCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    val imageWidth = bitmap.width.toFloat().coerceAtLeast(1f)
                    val imageHeight = bitmap.height.toFloat().coerceAtLeast(1f)

                    val normalizedRects = faces.map { face ->
                        val box = face.boundingBox
                        RectF(
                            (box.left / imageWidth).coerceIn(0f, 1f),
                            (box.top / imageHeight).coerceIn(0f, 1f),
                            (box.right / imageWidth).coerceIn(0f, 1f),
                            (box.bottom / imageHeight).coerceIn(0f, 1f)
                        )
                    }
                    continuation.resume(normalizedRects)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                    continuation.resume(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in detectFaces", e)
            continuation.resume(emptyList())
        }
    }

    fun close() {
        try {
            detector.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing detector", e)
        }
    }
}
