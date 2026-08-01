package com.techvertex.obscura.core.video.model

enum class VideoCropRatio(val label: String) {
    ORIGINAL("Original"),
    CUSTOM("Custom"),
    SQUARE("Square"),
    RATIO_3_4("3 : 4"),
    RATIO_4_3("4 : 3"),
    RATIO_16_9("16 : 9"),
    RATIO_9_16("9 : 16");

    fun getAspectRatio(): Float? = when (this) {
        ORIGINAL -> null
        CUSTOM -> null
        SQUARE -> 1f
        RATIO_3_4 -> 3f / 4f
        RATIO_4_3 -> 4f / 3f
        RATIO_16_9 -> 16f / 9f
        RATIO_9_16 -> 9f / 16f
    }

    val isFreeForm: Boolean get() = this == CUSTOM
}
