package com.techvertex.obscura.core.video.model

enum class FrameRatio(val widthRatio: Float, val heightRatio: Float, val label: String) {
    FREE(0f, 0f, "Free"),
    RATIO_1_1(1f, 1f, "1:1"),
    RATIO_4_3(4f, 3f, "4:3"),
    RATIO_16_9(16f, 9f, "16:9"),
    RATIO_9_16(9f, 16f, "9:16"),
    RATIO_3_4(3f, 4f, "3:4"),
}
