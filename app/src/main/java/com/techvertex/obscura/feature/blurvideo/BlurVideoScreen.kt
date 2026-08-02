package com.techvertex.obscura.feature.blurvideo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techvertex.obscura.core.video.model.FrameRatio
import com.techvertex.obscura.core.video.model.VideoBlurType
import com.techvertex.obscura.core.video.player.BlurVideoPlayer
import com.techvertex.obscura.core.video.view.BlurVideoGLSurfaceView
import com.techvertex.obscura.core.video.view.DraggableFrameView
import com.techvertex.obscura.ui.theme.Blue0F172A
import com.techvertex.obscura.ui.theme.Blue1E293B
import com.techvertex.obscura.ui.theme.Gray334155
import com.techvertex.obscura.ui.theme.Gray94A3B8
import com.techvertex.obscura.ui.theme.Purple6366F1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurVideoScreen(
    videoUriStr: String,
    viewModel: BlurVideoViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videoUri = remember(videoUriStr) { videoUriStr.toUri() }

    val videoPlayer = remember { BlurVideoPlayer(context) }
    var glSurfaceView by remember { mutableStateOf<BlurVideoGLSurfaceView?>(null) }
    var draggableFrameView by remember { mutableStateOf<DraggableFrameView?>(null) }

    // Storage permission launcher for API < 29
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startExport(context, videoUri)
        } else {
            Toast.makeText(
                context,
                "Storage permission is required to save video",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val requestSaveVideo = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startExport(context, videoUri)
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    videoPlayer.pause()
                    glSurfaceView?.onPause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    glSurfaceView?.onResume()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            videoPlayer.release()
            glSurfaceView?.releaseRenderer()
        }
    }

    // Keep GL Surface View & Player in sync with State
    LaunchedEffect(uiState.blurConfig) {
        glSurfaceView?.updateBlurConfig(uiState.blurConfig)
        draggableFrameView?.setFrameRect(uiState.blurConfig.frameRect)
        draggableFrameView?.setFrameRatio(uiState.blurConfig.frameRatio)
    }

    LaunchedEffect(uiState.isShowOriginal) {
        glSurfaceView?.setShowOriginal(uiState.isShowOriginal)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Blur Video",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { requestSaveVideo() }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Video",
                            tint = Purple6366F1
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Blue0F172A)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Blue0F172A)
        ) {
            // 1. Video Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            val surfaceView = BlurVideoGLSurfaceView(ctx).apply {
                                initialize { surfaceTexture ->
                                    videoPlayer.initialize(videoUri, surfaceTexture)

                                    videoPlayer.onDurationReady = { duration ->
                                        viewModel.setVideoDuration(duration)
                                    }
                                    videoPlayer.onPositionChanged = { position ->
                                        viewModel.setCurrentPosition(position)
                                        updateCurrentTime(position)
                                    }
                                    videoPlayer.onVideoSizeChanged = { w, h ->
                                        viewModel.setVideoSize(w, h)
                                        setVideoSize(w, h)
                                    }
                                    videoPlayer.onPlaybackStateChanged = { isPlaying ->
                                        viewModel.setIsPlaying(isPlaying)
                                    }
                                }
                                addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                                    val w = right - left
                                    val h = bottom - top
                                    if (w > 0 && h > 0) {
                                        viewModel.setSurfaceSize(w, h)
                                    }
                                }
                            }
                            glSurfaceView = surfaceView
                            addView(surfaceView)

                            val frameView = DraggableFrameView(ctx).apply {
                                onFrameChanged = { rect ->
                                    viewModel.updateFramePosition(rect)
                                }
                                onFrameInteractionStarted = {
                                    viewModel.saveCurrentStateForUndo()
                                }
                            }
                            draggableFrameView = frameView
                            addView(frameView)
                        }
                    }
                )
            }

            // 2. Action Controls (Undo, Redo, Touch to view Original)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) Color.White else Gray94A3B8.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) Color.White else Gray94A3B8.copy(alpha = 0.4f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Auto Face Detect Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (uiState.isAutoFaceTrackEnabled) Purple6366F1 else Gray334155)
                            .clickable {
                                if (uiState.timedFaceRects.isEmpty()) {
                                    viewModel.scanFacesForAutoBlur(context, videoUri)
                                } else {
                                    viewModel.toggleAutoFaceTrack()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Auto Face Blur",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isAutoFaceTrackEnabled) "Auto Face: ON" else "Auto Face",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Hold to Compare Original Video
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Gray334155)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.setShowOriginal(true)
                                        tryAwaitRelease()
                                        viewModel.setShowOriginal(false)
                                    }
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Original",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hold Original",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 3. Playback Controls (Time / Seekbar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Blue1E293B)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { videoPlayer.togglePlayPause() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = viewModel.formatTime(uiState.currentPosition),
                        color = Gray94A3B8,
                        fontSize = 12.sp
                    )

                    Slider(
                        value = uiState.currentPosition.toFloat(),
                        onValueChange = { pos ->
                            viewModel.setCurrentPosition(pos.toLong())
                            videoPlayer.seekTo(pos.toLong())
                        },
                        valueRange = 0f..uiState.videoDuration.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Purple6366F1,
                            activeTrackColor = Purple6366F1,
                            inactiveTrackColor = Gray334155
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = viewModel.formatTime(uiState.videoDuration),
                        color = Gray94A3B8,
                        fontSize = 12.sp
                    )
                }
            }

            // 4. Intensity Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Blue1E293B)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Intensity",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = uiState.blurConfig.blurIntensity.toFloat(),
                    onValueChange = { value ->
                        viewModel.updateBlurIntensity(value.toInt())
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Purple6366F1,
                        activeTrackColor = Purple6366F1,
                        inactiveTrackColor = Gray334155
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Text(
                    text = "${uiState.blurConfig.blurIntensity}%",
                    color = Gray94A3B8,
                    fontSize = 12.sp,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )

                IconButton(
                    onClick = { viewModel.updateBlurIntensity(50) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Intensity",
                        tint = Gray94A3B8,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 5. Tab Content (Horizontal Scroll List)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(Blue0F172A)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (uiState.activeTab) {
                    BlurVideoTab.FRAME -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            items(FrameRatio.entries) { ratio ->
                                val isSelected = uiState.blurConfig.frameRatio == ratio
                                Card(
                                    onClick = { viewModel.updateFrameRatio(ratio) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Purple6366F1 else Gray334155
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ratio.label,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    BlurVideoTab.BLUR_STYLE -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            items(VideoBlurType.entries) { type ->
                                val isSelected = uiState.blurConfig.blurType == type
                                Card(
                                    onClick = { viewModel.updateBlurType(type) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Purple6366F1 else Gray334155
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type.displayName,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Tab Bar (Frame & Blur Style Tabs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Blue1E293B)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItemButton(
                    label = "Frame",
                    isSelected = uiState.activeTab == BlurVideoTab.FRAME,
                    onClick = { viewModel.setActiveTab(BlurVideoTab.FRAME) }
                )

                TabItemButton(
                    label = "Blur Style",
                    isSelected = uiState.activeTab == BlurVideoTab.BLUR_STYLE,
                    onClick = { viewModel.setActiveTab(BlurVideoTab.BLUR_STYLE) }
                )
            }
        }
    }

    // 6.5. Face Scanning Dialog
    if (uiState.isScanningFaces) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Blue1E293B,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Purple6366F1,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI Face Scanning... ${uiState.scanProgress}%",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detecting & tracking face keyframes",
                        color = Gray94A3B8,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // 7. Exporting Overlay
    if (uiState.exportProgress >= 0) {
        Dialog(
            onDismissRequest = { viewModel.cancelExport() },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Blue1E293B,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Purple6366F1,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Exporting Video... ${uiState.exportProgress}%",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cancel",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.cancelExport() }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // 8. Export Completed Result Dialog
    uiState.exportResult?.let { path ->
        Dialog(onDismissRequest = { viewModel.clearExportResult() }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Blue1E293B,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Purple6366F1),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Export Completed!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Saved to:\n$path",
                        color = Gray94A3B8,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        onClick = { viewModel.clearExportResult() },
                        colors = CardDefaults.cardColors(containerColor = Purple6366F1),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "OK",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabItemButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Purple6366F1 else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Gray94A3B8,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
