package com.techvertex.obscura.feature.blurvideo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvertex.obscura.core.video.export.VideoExportConfig
import com.techvertex.obscura.core.video.export.VideoExportEngine
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
    val isShowOriginal: Boolean = false
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

    private var exportEngine: VideoExportEngine? = null

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
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
            state.copy(blurConfig = state.blurConfig.copy(frameRect = RectF(rect)))
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
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    fun setIsPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun setShowOriginal(show: Boolean) {
        _uiState.update { it.copy(isShowOriginal = show) }
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
                blurConfigs = listOf(_uiState.value.blurConfig)
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
