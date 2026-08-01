package com.techvertex.obscura.core.video.model

import android.graphics.RectF
import java.util.UUID

data class BlurVideoConfig(
    val id: String = UUID.randomUUID().toString(),
    val blurType: VideoBlurType = VideoBlurType.GAUSSIAN_BLUR,
    val blurIntensity: Int = 50,
    val frameRect: RectF = RectF(0.2f, 0.15f, 0.8f, 0.75f),
    val frameShape: FrameShape = FrameShape.Rectangle(),
    val frameRatio: FrameRatio = FrameRatio.FREE,
    val startTimeMs: Long = 0,
    val endTimeMs: Long = Long.MAX_VALUE,
    val zoomLevel: Float = 1f,
    val cropRect: RectF = RectF(0f, 0f, 1f, 1f),
    val cropRatio: VideoCropRatio = VideoCropRatio.ORIGINAL,
    val rotationDegrees: Int = 0,
    val videoBounds: RectF = RectF(0f, 0f, 1f, 1f)
)
