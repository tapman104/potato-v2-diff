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
import androidx.compose.runtime.getValue
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
import kotlin.math.abs

@Composable
fun GestureOverlay(
    showSeekIndicator: Boolean,
    seekDirection: SeekDirection,
    seekLabel: String,
    showZoomIndicator: Boolean,
    localZoom: Float,
    showVolumeIndicator: Boolean,
    volumePercentage: Int,
    showBrightnessIndicator: Boolean,
    currentBrightness: Float,
    showHorizontalSeekIndicator: Boolean,
    previewPositionMs: Long,
    previewDeltaMs: Long,
    isLongPressing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
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

        // ── Volume indicator ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showVolumeIndicator,
            enter = fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.75f),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 56.dp)
        ) {
            VolumeIndicator(percentage = volumePercentage)
        }

        // ── Brightness indicator ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showBrightnessIndicator,
            enter = fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.75f),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 56.dp)
        ) {
            BrightnessIndicator(brightness = currentBrightness)
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
