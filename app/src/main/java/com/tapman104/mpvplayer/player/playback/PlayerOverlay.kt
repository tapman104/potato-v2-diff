package com.tapman104.mpvplayer.player.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.model.DecodeMode
import kotlinx.coroutines.delay
import com.tapman104.mpvplayer.player.dialogs.AudioTrackDialog
import com.tapman104.mpvplayer.player.dialogs.SubtitleTrackDialog
import com.tapman104.mpvplayer.player.dialogs.AspectRatioDialog
import com.tapman104.mpvplayer.player.controls.PlayerTopBar
import com.tapman104.mpvplayer.player.controls.PlayerBottomControls
import com.tapman104.mpvplayer.player.controls.PlayerQuickActions
import com.tapman104.mpvplayer.player.gesture.GestureHandler


@Composable
fun PlayerOverlay(
    fileName: String,
    playerState: PlayerState,
    onOpenFile: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekForward: (Long) -> Unit,
    onSeekBackward: (Long) -> Unit,
    onSpeedOverride: (Float) -> Unit,
    onSpeedRestore: () -> Unit,
    onSelectAudioTrack: () -> Unit,
    onSelectSubtitleTrack: () -> Unit,
    onCycleDecodeMode: (DecodeMode) -> Unit,
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

    Box(modifier = modifier.fillMaxSize()) {

        // ── GESTURE HANDLER (above surface, below controls) ───────────────────
        GestureHandler(
            onSeekForward    = onSeekForward,
            onSeekBackward   = onSeekBackward,
            onToggleControls = { controlsVisible = true },
            onSpeedOverride  = onSpeedOverride,
            onSpeedRestore   = onSpeedRestore,
            modifier         = Modifier.fillMaxSize(),
        )

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
                onCycleDecodeMode = {
                    onCycleDecodeMode(
                        when (playerState.decodeMode) {
                            DecodeMode.HW     -> DecodeMode.HWPlus
                            DecodeMode.HWPlus -> DecodeMode.SW
                            DecodeMode.SW     -> DecodeMode.HW
                        }
                    )
                },
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
