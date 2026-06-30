package com.tapman104.mpvplayer.player.gesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapman104.mpvplayer.util.TimeFormatter
import kotlinx.coroutines.delay
import kotlin.math.abs

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
    currentPositionMs: Long,
    durationMs: Long,
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

    // ── Auto-hide timers ─────────────────────────────────────────────────────
    LaunchedEffect(labelTrigger) {
        if (labelTrigger > 0) {
            showSeekIndicator = true
            delay(700L)
            showSeekIndicator = false
            delay(300L)
            seekLabel     = ""
            seekDirection = SeekDirection.None
            tapCount      = 0
            lastTapSide   = ""
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
            .pinchZoomGesture(
                currentZoom = localZoom,
                onZoomUpdate = { newZoom ->
                    localZoom = newZoom
                    onZoomChange(newZoom)
                },
                onZoomStart = { isZooming = true },
                onZoomEnd = {
                    isZooming = false
                    zoomTrigger++
                }
            )
            .horizontalSwipeSeekGesture(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onSeekPreview = { position, delta ->
                    previewPositionMs = position
                    previewDeltaMs = delta
                    showHorizontalSeekIndicator = true
                    onSeekPreview(position, delta)
                },
                onSeekStart = {
                    wasPlayingBeforeScrub = isPlaying
                    if (isPlaying) onPauseForScrub()
                    showHorizontalSeekIndicator = true
                },
                onSeekCommit = { position ->
                    onSeekCommit(position)
                },
                onSeekEnd = {
                    if (wasPlayingBeforeScrub) onResumeAfterScrub()
                    horizontalSeekTrigger++
                }
            )
            .doubleTapSeekGesture(
                onSeekForward      = { handleForward() },
                onSeekBackward     = { handleBackward() },
                onContinueSeek     = { isRightHalf -> 
                    if (isRightHalf) handleForward() else handleBackward() 
                },
                onToggleControls   = onToggleControls,
                onLongPressStart   = { isLongPressing = true },
                onLongPressEnd     = { isLongPressing = false },
                onSpeedOverride    = onSpeedOverride,
                onSpeedRestore     = onSpeedRestore,
            )
    ) {
        // ── Backward seek indicator ──────────────────────────────────────────
        AnimatedVisibility(
            visible = showSeekIndicator && seekDirection == SeekDirection.Backward,
            enter   = fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.75f),
            exit    = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp),
        ) {
            SeekIndicator(label = seekLabel, isForward = false)
        }

        // ── Forward seek indicator ───────────────────────────────────────────
        AnimatedVisibility(
            visible = showSeekIndicator && seekDirection == SeekDirection.Forward,
            enter   = fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.75f),
            exit    = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 56.dp),
        ) {
            SeekIndicator(label = seekLabel, isForward = true)
        }

        // ── Pinch zoom indicator ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showZoomIndicator,
            enter   = fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.75f),
            exit    = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp), // Staggered below speed indicator
        ) {
            PinchZoomIndicator(zoom = localZoom)
        }

        // ── Horizontal seek indicator ─────────────────────────────────────────
        AnimatedVisibility(
            visible = showHorizontalSeekIndicator,
            enter   = fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.75f),
            exit    = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
        ) {
            val sign = if (previewDeltaMs >= 0) "+" else "-"
            val deltaLabel = "$sign${TimeFormatter.formatMs(abs(previewDeltaMs))}"
            HorizontalSeekIndicator(
                currentTimeLabel = TimeFormatter.formatMs(previewPositionMs),
                deltaLabel = deltaLabel
            )
        }

        // ── Long-press speed badge ────────────────────────────────────────────
        AnimatedVisibility(
            visible = isLongPressing,
            enter   = fadeIn(tween(150)) + scaleIn(tween(200), initialScale = 0.7f),
            exit    = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
        ) {
            SpeedIndicator()
        }
    }
}

// ---------------------------------------------------------------------------
// SeekIndicator
// ---------------------------------------------------------------------------

@Composable
private fun SeekIndicator(
    label: String,
    isForward: Boolean,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.05f),
                    )
                )
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = if (isForward) "Seek forward" else "Seek backward",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SpeedIndicator
// ---------------------------------------------------------------------------

@Composable
private fun SpeedIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "speed_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "speed_glow",
    )

    Box(
        modifier = Modifier
            .alpha(glowAlpha)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1E1E1E).copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = "Speed override",
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "2× Speed",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
