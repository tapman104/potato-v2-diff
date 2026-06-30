package com.tapman104.mpvplayer.player.gesture

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Seek direction enum
// ---------------------------------------------------------------------------

enum class SeekDirection { Forward, Backward, None }

// ---------------------------------------------------------------------------
// GestureHandler
// ---------------------------------------------------------------------------

/**
 * Integration point that wires all gesture layers together and renders the seek-feedback UI.
 *
 * The seek amount is resolved inside this composable and propagated to [onSeekForward] /
 * [onSeekBackward] as absolute-offset lambdas of type `(Long) -> Unit`.
 */
@Composable
fun GestureHandler(
    currentPositionMs: () -> Long,
    durationMs: () -> Long,
    isPlaying: Boolean,
    onSeekPreview: (Long, Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    onPauseForScrub: () -> Unit,
    onResumeAfterScrub: () -> Unit,
    onSeekForward: (Long) -> Unit,
    onSeekBackward: (Long) -> Unit,
    onToggleControls: () -> Unit,
    onSpeedOverride: (Float) -> Unit,
    onSpeedRestore: () -> Unit,
    modifier: Modifier = Modifier,
    currentZoom: Float = 0f,
    onZoomChange: (Float) -> Unit = {},
    initialBrightness: Float = -1f,
    onBrightnessChange: (Float) -> Unit = {},
    volumePercentage: Int = 0,
    onVolumeChange: (Int) -> Unit = {},
) {
    // ── State ────────────────────────────────────────────────────────────────
    var seekDirection    by remember { mutableStateOf(SeekDirection.None) }
    var seekLabel        by remember { mutableStateOf("") }
    var tapCount         by remember { mutableIntStateOf(0) }
    var lastTapSide      by remember { mutableStateOf("") }
    var showSeekIndicator by remember { mutableStateOf(false) }
    var labelTrigger     by remember { mutableIntStateOf(0) }
    var isLongPressing   by remember { mutableStateOf(false) }

    // ── Local state for horizontal swipe ─────────────────────────────────────
    var wasPlayingBeforeScrub by remember { mutableStateOf(false) }
    var showHorizontalSeekIndicator by remember { mutableStateOf(false) }
    var horizontalSeekTrigger by remember { mutableIntStateOf(0) }
    var previewPositionMs by remember { mutableStateOf(0L) }
    var previewDeltaMs by remember { mutableStateOf(0L) }

    // ── Local state for pinch zoom ───────────────────────────────────────────
    var isZooming by remember { mutableStateOf(false) }
    var showZoomIndicator by remember { mutableStateOf(false) }
    var zoomTrigger by remember { mutableIntStateOf(0) }
    var localZoom by remember { mutableFloatStateOf(currentZoom) }

    // ── Local state for volume and brightness ────────────────────────────────
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    
    var isVolumeDragging by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var volumeHideTrigger by remember { mutableIntStateOf(0) }

    var isBrightnessDragging by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var brightnessHideTrigger by remember { mutableIntStateOf(0) }
    var currentBrightness by remember { mutableFloatStateOf(initialBrightness) }

    // ── Auto-hide timers ─────────────────────────────────────────────────────
    LaunchedEffect(labelTrigger) {
        if (labelTrigger > 0) {
            showSeekIndicator = true
            delay(650L)
            showSeekIndicator = false
            tapCount      = 0
            lastTapSide   = ""
            delay(250L)
            seekLabel     = ""
            seekDirection = SeekDirection.None
        }
    }

    LaunchedEffect(currentZoom) {
        if (!isZooming) {
            localZoom = currentZoom
        }
    }

    LaunchedEffect(zoomTrigger, isZooming) {
        if (isZooming) {
            showZoomIndicator = true
        } else if (zoomTrigger > 0) {
            showZoomIndicator = true
            delay(700L)
            showZoomIndicator = false
        }
    }

    LaunchedEffect(horizontalSeekTrigger) {
        if (horizontalSeekTrigger > 0) {
            showHorizontalSeekIndicator = true
            delay(700L)
            showHorizontalSeekIndicator = false
        }
    }

    LaunchedEffect(initialBrightness) {
        if (!isBrightnessDragging) {
            currentBrightness = initialBrightness
        }
    }

    LaunchedEffect(volumeHideTrigger, isVolumeDragging) {
        if (isVolumeDragging) {
            showVolumeIndicator = true
        } else if (volumeHideTrigger > 0) {
            showVolumeIndicator = true
            delay(700L)
            showVolumeIndicator = false
        }
    }

    LaunchedEffect(brightnessHideTrigger, isBrightnessDragging) {
        if (isBrightnessDragging) {
            showBrightnessIndicator = true
        } else if (brightnessHideTrigger > 0) {
            showBrightnessIndicator = true
            delay(700L)
            showBrightnessIndicator = false
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    fun resolveSeekMs(count: Int): Long = when {
        count >= 6 -> 60_000L
        count == 5 -> 50_000L
        count == 4 -> 40_000L
        count == 3 -> 30_000L
        count == 2 -> 20_000L
        else       -> 10_000L
    }

    fun handleForward() {
        if (lastTapSide != "forward") {
            tapCount    = 0
            lastTapSide = "forward"
        }
        tapCount++
        val seekMs = resolveSeekMs(tapCount)
        seekLabel     = "+${seekMs / 1000}s"
        seekDirection = SeekDirection.Forward
        labelTrigger++
        onSeekForward(seekMs)
    }

    fun handleBackward() {
        if (lastTapSide != "backward") {
            tapCount    = 0
            lastTapSide = "backward"
        }
        tapCount++
        val seekMs = resolveSeekMs(tapCount)
        seekLabel     = "-${seekMs / 1000}s"
        seekDirection = SeekDirection.Backward
        labelTrigger++
        onSeekBackward(seekMs)
    }

    // ── Layout ───────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .gestureCoordinator(
                audioManager = audioManager,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                currentZoom = localZoom,
                initialBrightness = currentBrightness,
                listener = object : PlayerGestureListener {
                    override fun onVolumeChange(percentage: Int) {
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val newVol = kotlin.math.round((percentage / 100f) * maxVol).toInt().coerceIn(0, maxVol)
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        onVolumeChange(percentage)
                    }
                    override fun onVolumeDragStart() { isVolumeDragging = true }
                    override fun onVolumeDragEnd() {
                        isVolumeDragging = false
                        volumeHideTrigger++
                    }

                    override fun onBrightnessUpdate(brightness: Float) {
                        currentBrightness = brightness
                        onBrightnessChange(brightness)
                    }
                    override fun onBrightnessDragStart() { isBrightnessDragging = true }
                    override fun onBrightnessDragEnd() {
                        isBrightnessDragging = false
                        brightnessHideTrigger++
                    }

                    override fun onZoomUpdate(zoom: Float) {
                        localZoom = zoom
                        onZoomChange(zoom)
                    }
                    override fun onZoomStart() { isZooming = true }
                    override fun onZoomEnd() {
                        isZooming = false
                        zoomTrigger++
                    }

                    override fun onSeekPreview(positionMs: Long, deltaMs: Long) {
                        previewPositionMs = positionMs
                        previewDeltaMs = deltaMs
                        showHorizontalSeekIndicator = true
                        onSeekPreview(positionMs, deltaMs)
                    }
                    override fun onSeekStart() {
                        wasPlayingBeforeScrub = isPlaying
                        if (isPlaying) onPauseForScrub()
                        showHorizontalSeekIndicator = true
                    }
                    override fun onSeekCommit(positionMs: Long) = onSeekCommit(positionMs)
                    override fun onSeekEnd() {
                        if (wasPlayingBeforeScrub) onResumeAfterScrub()
                        horizontalSeekTrigger++
                    }

                    override fun onSeekForward() = handleForward()
                    override fun onSeekBackward() = handleBackward()
                    override fun onContinueSeek(isRightHalf: Boolean) {
                        if (isRightHalf) handleForward() else handleBackward()
                    }

                    override fun onToggleControls() = onToggleControls()
                    override fun onLongPressStart() { isLongPressing = true }
                    override fun onLongPressEnd() { isLongPressing = false }
                    override fun onSpeedOverride(speed: Float) = onSpeedOverride(speed)
                    override fun onSpeedRestore() = onSpeedRestore()
                }
            )
    ) {
        GestureOverlay(
            showSeekIndicator = showSeekIndicator,
            seekDirection = seekDirection,
            seekLabel = seekLabel,
            showZoomIndicator = showZoomIndicator,
            localZoom = localZoom,
            showVolumeIndicator = showVolumeIndicator,
            volumePercentage = volumePercentage,
            showBrightnessIndicator = showBrightnessIndicator,
            currentBrightness = currentBrightness,
            showHorizontalSeekIndicator = showHorizontalSeekIndicator,
            previewPositionMs = previewPositionMs,
            previewDeltaMs = previewDeltaMs,
            isLongPressing = isLongPressing
        )
    }
}
