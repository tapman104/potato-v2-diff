package com.tapman104.mpvplayer.player.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapman104.mpvplayer.util.TimeFormatter

@Composable
fun PlayerBottomControls(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableStateOf(0L) }

    val displayMs = if (isDragging) dragPositionMs else currentPositionMs
    val fraction = if (durationMs > 0L)
        (displayMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = TimeFormatter.formatMs(displayMs),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.width(42.dp)
            )
            Slider(
                value = fraction,
                onValueChange = { v ->
                    isDragging = true
                    dragPositionMs = (v * durationMs).toLong()
                },
                onValueChangeFinished = {
                    onSeek(dragPositionMs)
                    isDragging = false
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF8B5CF6),
                    activeTrackColor = Color(0xFF8B5CF6),
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(
                text = TimeFormatter.formatMs(durationMs),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.width(42.dp),
                textAlign = TextAlign.End
            )
        }

        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
