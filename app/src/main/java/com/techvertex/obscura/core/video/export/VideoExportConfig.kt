package com.techvertex.obscura.core.video.export

import android.net.Uri
import com.techvertex.obscura.core.video.model.BlurVideoConfig

data class VideoExportConfig(
    val inputUri: Uri,
    val outputPath: String,
    val blurConfigs: List<BlurVideoConfig>,
    val keepOriginalResolution: Boolean = true,
    val faceTrackKeyframes: List<com.techvertex.obscura.core.video.face.TimedFaceRect>? = null
)
