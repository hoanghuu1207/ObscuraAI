package com.techvertex.obscura.core.video.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.techvertex.obscura.core.video.model.FrameRatio
import kotlin.math.abs

class DraggableFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val BORDER_WIDTH = 3f
        private const val TOUCH_SLOP = 30f
        private const val MIN_FRAME_SIZE = 0.1f
    }

    private val frameRect = RectF(0.2f, 0.15f, 0.8f, 0.75f)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = BORDER_WIDTH
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private enum class DragMode {
        NONE, MOVE,
        RESIZE_TOP_LEFT, RESIZE_TOP_RIGHT,
        RESIZE_BOTTOM_LEFT, RESIZE_BOTTOM_RIGHT,
        RESIZE_TOP, RESIZE_BOTTOM,
        RESIZE_LEFT, RESIZE_RIGHT
    }

    private var dragMode = DragMode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var lockedRatio: FrameRatio = FrameRatio.FREE
    private val cropBounds = RectF(0f, 0f, 1f, 1f)

    var onFrameChanged: ((RectF) -> Unit)? = null
    var onFrameInteractionStarted: (() -> Unit)? = null
    var onFrameInteractionEnded: (() -> Unit)? = null

    fun setCropBounds(bounds: RectF) {
        cropBounds.set(bounds)
        clampFrameToExistingBounds()
        invalidate()
    }

    private fun clampFrameToExistingBounds() {
        frameRect.left = frameRect.left.coerceIn(cropBounds.left, cropBounds.right - MIN_FRAME_SIZE)
        frameRect.top = frameRect.top.coerceIn(cropBounds.top, cropBounds.bottom - MIN_FRAME_SIZE)
        frameRect.right = frameRect.right.coerceIn(cropBounds.left + MIN_FRAME_SIZE, cropBounds.right)
        frameRect.bottom = frameRect.bottom.coerceIn(cropBounds.top + MIN_FRAME_SIZE, cropBounds.bottom)
    }

    fun setFrameRect(rect: RectF) {
        frameRect.set(rect)
        invalidate()
    }

    fun getFrameRect(): RectF = RectF(frameRect)

    fun setFrameRatio(ratio: FrameRatio) {
        lockedRatio = ratio
        if (ratio != FrameRatio.FREE) {
            applyRatioLock()
        }
        invalidate()
    }

    private fun applyRatioLock() {
        if (lockedRatio == FrameRatio.FREE) return

        val targetAspect = lockedRatio.widthRatio / lockedRatio.heightRatio
        val centerX = (frameRect.left + frameRect.right) / 2f
        val centerY = (frameRect.top + frameRect.bottom) / 2f
        val currentWidth = frameRect.width()
        val currentHeight = frameRect.height()

        val viewAspect = width.toFloat() / height.toFloat()
        val adjustedAspect = targetAspect / viewAspect

        val newWidth: Float
        val newHeight: Float
        if (adjustedAspect > currentWidth / currentHeight) {
            newHeight = currentHeight
            newWidth = newHeight * adjustedAspect
        } else {
            newWidth = currentWidth
            newHeight = newWidth / adjustedAspect
        }

        val boundsW = cropBounds.width()
        val boundsH = cropBounds.height()
        val clampedW = newWidth.coerceAtMost(boundsW)
        val clampedH = newHeight.coerceAtMost(boundsH)

        frameRect.set(
            (centerX - clampedW / 2f).coerceIn(cropBounds.left, cropBounds.right - clampedW),
            (centerY - clampedH / 2f).coerceIn(cropBounds.top, cropBounds.bottom - clampedH),
            (centerX + clampedW / 2f).coerceIn(cropBounds.left + clampedW, cropBounds.right),
            (centerY + clampedH / 2f).coerceIn(cropBounds.top + clampedH, cropBounds.bottom)
        )
        onFrameChanged?.invoke(RectF(frameRect))
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val w = width.toFloat()
        val h = height.toFloat()

        val pixelRect = RectF(
            frameRect.left * w,
            frameRect.top * h,
            frameRect.right * w,
            frameRect.bottom * h
        )

        canvas.drawRect(pixelRect, borderPaint)

        drawCornerHandle(canvas, pixelRect.left, pixelRect.top)
        drawCornerHandle(canvas, pixelRect.right, pixelRect.top)
        drawCornerHandle(canvas, pixelRect.left, pixelRect.bottom)
        drawCornerHandle(canvas, pixelRect.right, pixelRect.bottom)
    }

    private fun drawCornerHandle(canvas: Canvas, x: Float, y: Float) {
        val length = 30f
        val strokeW = 10f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeJoin = Paint.Join.MITER
            strokeCap = Paint.Cap.SQUARE
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val pxRect = RectF(frameRect.left * w, frameRect.top * h, frameRect.right * w, frameRect.bottom * h)

        val dx = if (x <= pxRect.centerX()) length else -length
        val dy = if (y <= pxRect.centerY()) length else -length

        val path = Path().apply {
            moveTo(x + dx, y)
            lineTo(x, y)
            lineTo(x, y + dy)
        }
        canvas.drawPath(path, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x / width.toFloat()
        val y = event.y / height.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragMode = detectDragMode(x, y)
                lastTouchX = x
                lastTouchY = y
                if (dragMode != DragMode.NONE) {
                    onFrameInteractionStarted?.invoke()
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragMode == DragMode.NONE) return false

                val dx = x - lastTouchX
                val dy = y - lastTouchY

                when (dragMode) {
                    DragMode.MOVE -> {
                        val frameW = frameRect.width()
                        val frameH = frameRect.height()
                        val minLeft = cropBounds.left
                        val maxLeft = maxOf(minLeft, cropBounds.right - frameW)
                        val minTop = cropBounds.top
                        val maxTop = maxOf(minTop, cropBounds.bottom - frameH)
                        val newLeft = (frameRect.left + dx).coerceIn(minLeft, maxLeft)
                        val newTop = (frameRect.top + dy).coerceIn(minTop, maxTop)
                        frameRect.offsetTo(newLeft, newTop)
                    }

                    DragMode.RESIZE_TOP_LEFT -> {
                        frameRect.left = (frameRect.left + dx).coerceIn(cropBounds.left, frameRect.right - MIN_FRAME_SIZE)
                        frameRect.top = (frameRect.top + dy).coerceIn(cropBounds.top, frameRect.bottom - MIN_FRAME_SIZE)
                    }

                    DragMode.RESIZE_TOP_RIGHT -> {
                        frameRect.right = (frameRect.right + dx).coerceIn(frameRect.left + MIN_FRAME_SIZE, cropBounds.right)
                        frameRect.top = (frameRect.top + dy).coerceIn(cropBounds.top, frameRect.bottom - MIN_FRAME_SIZE)
                    }

                    DragMode.RESIZE_BOTTOM_LEFT -> {
                        frameRect.left = (frameRect.left + dx).coerceIn(cropBounds.left, frameRect.right - MIN_FRAME_SIZE)
                        frameRect.bottom = (frameRect.bottom + dy).coerceIn(frameRect.top + MIN_FRAME_SIZE, cropBounds.bottom)
                    }

                    DragMode.RESIZE_BOTTOM_RIGHT -> {
                        frameRect.right = (frameRect.right + dx).coerceIn(frameRect.left + MIN_FRAME_SIZE, cropBounds.right)
                        frameRect.bottom = (frameRect.bottom + dy).coerceIn(frameRect.top + MIN_FRAME_SIZE, cropBounds.bottom)
                    }

                    DragMode.RESIZE_TOP -> {
                        frameRect.top = (frameRect.top + dy).coerceIn(cropBounds.top, frameRect.bottom - MIN_FRAME_SIZE)
                    }

                    DragMode.RESIZE_BOTTOM -> {
                        frameRect.bottom = (frameRect.bottom + dy).coerceIn(frameRect.top + MIN_FRAME_SIZE, cropBounds.bottom)
                    }

                    DragMode.RESIZE_LEFT -> {
                        frameRect.left = (frameRect.left + dx).coerceIn(cropBounds.left, frameRect.right - MIN_FRAME_SIZE)
                    }

                    DragMode.RESIZE_RIGHT -> {
                        frameRect.right = (frameRect.right + dx).coerceIn(frameRect.left + MIN_FRAME_SIZE, cropBounds.right)
                    }

                    else -> {}
                }

                if (lockedRatio != FrameRatio.FREE && dragMode != DragMode.MOVE) {
                    applyRatioLock()
                }

                lastTouchX = x
                lastTouchY = y
                onFrameChanged?.invoke(RectF(frameRect))
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragMode != DragMode.NONE) {
                    onFrameInteractionEnded?.invoke()
                }
                dragMode = DragMode.NONE
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun detectDragMode(x: Float, y: Float): DragMode {
        val slop = TOUCH_SLOP / width.toFloat()

        if (isNear(x, y, frameRect.left, frameRect.top, slop)) return DragMode.RESIZE_TOP_LEFT
        if (isNear(x, y, frameRect.right, frameRect.top, slop)) return DragMode.RESIZE_TOP_RIGHT
        if (isNear(x, y, frameRect.left, frameRect.bottom, slop)) return DragMode.RESIZE_BOTTOM_LEFT
        if (isNear(x, y, frameRect.right, frameRect.bottom, slop)) return DragMode.RESIZE_BOTTOM_RIGHT

        val midX = (frameRect.left + frameRect.right) / 2f
        val midY = (frameRect.top + frameRect.bottom) / 2f

        if (isNear(x, y, midX, frameRect.top, slop)) return DragMode.RESIZE_TOP
        if (isNear(x, y, midX, frameRect.bottom, slop)) return DragMode.RESIZE_BOTTOM
        if (isNear(x, y, frameRect.left, midY, slop)) return DragMode.RESIZE_LEFT
        if (isNear(x, y, frameRect.right, midY, slop)) return DragMode.RESIZE_RIGHT

        if (frameRect.contains(x, y)) return DragMode.MOVE

        return DragMode.NONE
    }

    private fun isNear(x: Float, y: Float, targetX: Float, targetY: Float, slop: Float): Boolean {
        return abs(x - targetX) < slop && abs(y - targetY) < slop
    }
}
