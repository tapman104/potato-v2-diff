package com.tapman104.mpvplayer.player.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tapman104.mpvplayer.player.state.PlayerState
import kotlinx.coroutines.delay

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
        // ── TOP BAR ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PlayerTopBar(
                fileName = fileName,
                onBack = onOpenFile,
                onOpenFile = onOpenFile
            )
        }

        // ── QUICK ACTIONS ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 64.dp, start = 8.dp)
        ) {
            PlayerQuickActions(
                decodeMode = playerState.decodeMode,
                onSelectAudioTrack = onSelectAudioTrack,
                onSelectSubtitleTrack = onSelectSubtitleTrack,
                onCycleDecodeMode = onCycleDecodeMode,
                onMoreOptions = onMoreOptions
            )
        }

        // ── BOTTOM CONTROLS ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerBottomControls(
                isPlaying = playerState.isPlaying,
                currentPositionMs = playerState.currentPositionMs,
                durationMs = playerState.durationMs,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek
            )
        }

        // ── LOADING INDICATOR ─────────────────────────────────────────────────
        if (playerState.isLoading) {
            CircularProgressIndicator(
                color = Color(0xFF8B5CF6),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
