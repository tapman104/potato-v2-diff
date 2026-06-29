package com.tapman104.mpvplayer.player.playback

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.tapman104.mpvplayer.player.model.*
import com.tapman104.mpvplayer.player.state.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOverlay(
    fileName: String,
    playerState: PlayerState,
    playlistState: PlaylistState,
    onOpenFile: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectAudioTrack: () -> Unit,
    onSelectSubtitleTrack: () -> Unit,
    onCycleDecodeMode: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000L)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = true }
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onOpenFile) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenFile) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Open file", tint = Color.White)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier.height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onSelectAudioTrack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Audiotrack, contentDescription = "Audio track")
                    }

                    FilledTonalIconButton(
                        onClick = onSelectSubtitleTrack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.ClosedCaption, contentDescription = "Subtitle track")
                    }

                    FilledTonalIconButton(
                        onClick = onCycleDecodeMode,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            text = when (playerState.decodeMode) {
                                DecodeMode.HW     -> "HW"
                                DecodeMode.HWPlus -> "HW+"
                                DecodeMode.SW     -> "SW"
                                else              -> "HW"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onMoreOptions,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var isDragging by remember { mutableStateOf(false) }
                var dragPositionMs by remember { mutableStateOf(0L) }
                val displayMs = if (isDragging) dragPositionMs else playerState.currentPositionMs
                val fraction = if (playerState.durationMs > 0L)
                    (displayMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                else 0f

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = formatMs(displayMs),
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.width(42.dp)
                    )
                    Slider(
                        value = fraction,
                        onValueChange = { v ->
                            isDragging = true
                            dragPositionMs = (v * playerState.durationMs).toLong()
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
                        text = formatMs(playerState.durationMs),
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
                        imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        if (playerState.isLoading) {
            CircularProgressIndicator(
                color = Color(0xFF8B5CF6),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
