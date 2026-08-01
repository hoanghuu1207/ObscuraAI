package com.techvertex.obscura.core.video.model

enum class VideoBlurType(val displayName: String, val isNone: Boolean = false) {
    NONE("None", true),
    GAUSSIAN_BLUR("Gaussian"),
    LINE_BLUR("Line"),
    ZOOM_BLUR("Zoom"),
    PAINT_BLUR("Paint"),
    POLAR_BLUR("Polar"),
    MOTION_BLUR("Motion")
}
