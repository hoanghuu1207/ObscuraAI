package com.techvertex.obscura.core.video.model

sealed class FrameShape {
    data class Rectangle(val cornerRadius: Float = 0f) : FrameShape()
}
