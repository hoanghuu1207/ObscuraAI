package com.techvertex.obscura.feature.blurvideo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvertex.obscura.core.video.export.VideoExportConfig
import com.techvertex.obscura.core.video.export.VideoExportEngine
import com.techvertex.obscura.core.video.face.TimedFaceRect
import com.techvertex.obscura.core.video.face.VideoFaceScanner
import com.techvertex.obscura.core.video.model.BlurVideoConfig
import com.techvertex.obscura.core.video.model.FrameRatio
import com.techvertex.obscura.core.video.model.VideoBlurConstants
import com.techvertex.obscura.core.video.model.VideoBlurType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class BlurVideoTab {
    FRAME, BLUR_STYLE
}

data class BlurVideoUiState(
    val blurConfig: BlurVideoConfig = BlurVideoConfig(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val videoDuration: Long = 0L,
    val currentPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val activeTab: BlurVideoTab = BlurVideoTab.FRAME,
    val exportProgress: Int = -1,
    val exportResult: String? = null,
    val isShowOriginal: Boolean = false,
    val isScanningFaces: Boolean = false,
    val scanProgress: Int = -1,
    val isAutoFaceTrackEnabled: Boolean = false,
    val timedFaceRects: List<TimedFaceRect> = emptyList()
)

@HiltViewModel
class BlurVideoViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(BlurVideoUiState())
    val uiState: StateFlow<BlurVideoUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<BlurVideoConfig>()
    private val redoStack = mutableListOf<BlurVideoConfig>()

    var videoWidth: Int = 0
        private set
    var videoHeight: Int = 0
        private set

    var surfaceWidth: Int = 0
        private set
    var surfaceHeight: Int = 0
        private set

    private var exportEngine: VideoExportEngine? = null

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
    }

    fun setSurfaceSize(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
    }

    fun setActiveTab(tab: BlurVideoTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateBlurIntensity(intensity: Int) {
        _uiState.update { state ->
            state.copy(blurConfig = state.blurConfig.copy(blurIntensity = intensity.coerceIn(0, 100)))
        }
    }

    fun updateBlurType(type: VideoBlurType) {
        saveCurrentStateForUndo()
        _uiState.update { state ->
            state.copy(blurConfig = state.blurConfig.copy(blurType = type))
        }
    }

    fun updateFramePosition(rect: RectF) {
        _uiState.update { state ->
            state.copy(
                blurConfig = state.blurConfig.copy(frameRect = RectF(rect)),
                isAutoFaceTrackEnabled = false // Manual drag overrides auto face track
            )
        }
    }

    fun updateFrameRatio(ratio: FrameRatio) {
        saveCurrentStateForUndo()
        _uiState.update { state ->
            state.copy(blurConfig = state.blurConfig.copy(frameRatio = ratio))
        }
    }

    fun setVideoDuration(durationMs: Long) {
        _uiState.update { state ->
            val updatedConfig = if (state.blurConfig.endTimeMs == Long.MAX_VALUE) {
                state.blurConfig.copy(endTimeMs = durationMs)
            } else state.blurConfig
            state.copy(videoDuration = durationMs, blurConfig = updatedConfig)
        }
    }

    fun setCurrentPosition(positionMs: Long) {
        _uiState.update { state ->
            var updatedConfig = state.blurConfig
            if (state.isAutoFaceTrackEnabled && state.timedFaceRects.isNotEmpty()) {
                val interpolated = VideoFaceScanner.getInterpolatedFaceRect(positionMs, state.timedFaceRects)
                if (interpolated != null) {
                    val viewRect = VideoFaceScanner.convertVideoRectToViewRect(
                        interpolated,
                        videoWidth,
                        videoHeight,
                        surfaceWidth,
                        surfaceHeight,
                        state.blurConfig.rotationDegrees
                    )
                    updatedConfig = updatedConfig.copy(frameRect = viewRect)
                }
            }
            state.copy(currentPosition = positionMs, blurConfig = updatedConfig)
        }
    }

    fun setIsPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun setShowOriginal(show: Boolean) {
        _uiState.update { it.copy(isShowOriginal = show) }
    }

    fun scanFacesForAutoBlur(context: Context, videoUri: Uri) {
        if (_uiState.value.isScanningFaces) return

        _uiState.update { it.copy(isScanningFaces = true, scanProgress = 0) }

        viewModelScope.launch(Dispatchers.IO) {
            val scanner = VideoFaceScanner(context.applicationContext)
            val rects = scanner.scanVideoForFaces(videoUri) { progress ->
                _uiState.update { it.copy(scanProgress = progress) }
            }

            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    val updatedConfig = if (rects.isNotEmpty()) {
                        val firstFace = VideoFaceScanner.convertVideoRectToViewRect(
                            rects.first().rect,
                            videoWidth,
                            videoHeight,
                            surfaceWidth,
                            surfaceHeight,
                            state.blurConfig.rotationDegrees
                        )
                        state.blurConfig.copy(frameRect = firstFace)
                    } else state.blurConfig

                    state.copy(
                        isScanningFaces = false,
                        scanProgress = -1,
                        timedFaceRects = rects,
                        isAutoFaceTrackEnabled = rects.isNotEmpty()
                    )
                }
            }
        }
    }

    fun toggleAutoFaceTrack() {
        _uiState.update { state ->
            state.copy(isAutoFaceTrackEnabled = !state.isAutoFaceTrackEnabled)
        }
    }

    fun saveCurrentStateForUndo() {
        val current = _uiState.value.blurConfig
        undoStack.add(current.copy())
        if (undoStack.size > VideoBlurConstants.MAX_UNDO_STACK_SIZE) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoState()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.blurConfig
        redoStack.add(current.copy())
        val previous = undoStack.removeAt(undoStack.lastIndex)
        _uiState.update { it.copy(blurConfig = previous) }
        updateUndoRedoState()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.blurConfig
        undoStack.add(current.copy())
        val next = redoStack.removeAt(redoStack.lastIndex)
        _uiState.update { it.copy(blurConfig = next) }
        updateUndoRedoState()
    }

    private fun updateUndoRedoState() {
        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun startExport(context: Context, videoUri: Uri) {
        if (_uiState.value.exportProgress >= 0) return

        _uiState.update { it.copy(exportProgress = 0, exportResult = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val exportConfig = VideoExportConfig(
                inputUri = videoUri,
                outputPath = "",
                blurConfigs = listOf(_uiState.value.blurConfig),
                faceTrackKeyframes = if (_uiState.value.isAutoFaceTrackEnabled) _uiState.value.timedFaceRects else null
            )

            exportEngine = VideoExportEngine(
                context = context.applicationContext,
                config = exportConfig,
                onProgress = { progress ->
                    _uiState.update { currentState ->
                        if (currentState.exportProgress >= 0) {
                            currentState.copy(exportProgress = progress)
                        } else currentState
                    }
                }
            )

            val resultPath = exportEngine?.execute()

            withContext(Dispatchers.Main) {
                _uiState.update { currentState ->
                    if (currentState.exportProgress != -1 && resultPath != null) {
                        currentState.copy(
                            exportProgress = -1,
                            exportResult = resultPath
                        )
                    } else {
                        currentState.copy(exportProgress = -1)
                    }
                }
            }
        }
    }

    fun cancelExport() {
        exportEngine?.cancel()
        _uiState.update { it.copy(exportProgress = -1) }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    @SuppressLint("DefaultLocale")
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
