package com.tapman104.mpvplayer.player.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.tapman104.mpvplayer.player.state.PlayerState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOverlay(
    fileName: String,
    playerState: PlayerState,
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
        // ── SECTION 1: TOP APP BAR ──
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                title = {
                    Text(
                        fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenFile) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenFile) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Open", tint = Color.White)
                    }
                }
            )
        }

        // ── SECTION 2: QUICK ACTIONS BAR ──
        // State: whether the action buttons are expanded or collapsed
        var quickActionsExpanded by remember { mutableStateOf(true) }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 64.dp, start = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // The 4 action buttons — slide in/out horizontally
                AnimatedVisibility(
                    visible = quickActionsExpanded,
                    enter = fadeIn(tween(180)) + expandHorizontally(
                        animationSpec = tween(180),
                        expandFrom = Alignment.End
                    ),
                    exit = fadeOut(tween(180)) + shrinkHorizontally(
                        animationSpec = tween(180),
                        shrinkTowards = Alignment.End
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        // Audio track button
                        FilledTonalIconButton(
                            onClick = onSelectAudioTrack,
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Filled.Audiotrack,
                                contentDescription = "Audio track",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Subtitle track button
                        FilledTonalIconButton(
                            onClick = onSelectSubtitleTrack,
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Filled.ClosedCaption,
                                contentDescription = "Subtitle track",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Decode mode button (text label: HW / HW+ / SW)
                        FilledTonalIconButton(
                            onClick = onCycleDecodeMode,
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "HW",   // static placeholder — wire to playerState.decodeMode in later prompt
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }

                        // More/Settings button
                        FilledTonalIconButton(
                            onClick = onMoreOptions,
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "More options",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Collapse / expand arrow button — always visible
                FilledTonalIconButton(
                    onClick = { quickActionsExpanded = !quickActionsExpanded },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.55f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (quickActionsExpanded)
                            Icons.Filled.ChevronLeft
                        else
                            Icons.Filled.ChevronRight,
                        contentDescription = if (quickActionsExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── SECTION 3: BOTTOM CONTROLS ──
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // SEEK BAR
                var isDragging by remember { mutableStateOf(false) }
                var dragPositionMs by remember { mutableStateOf(0L) }
                val displayMs = if (isDragging) dragPositionMs else playerState.currentPositionMs
                val fraction = if (playerState.durationMs > 0L)
                    (displayMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                else 0f

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
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

                // PLAY/PAUSE
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // ── SECTION 4: LOADING INDICATOR ──
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
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
