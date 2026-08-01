package com.techvertex.obscura.core.video.face

import android.graphics.RectF

data class TimedFaceRect(
    val timeMs: Long,
    val rect: RectF
)
