package com.tapman104.mpvplayer.player.playback

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.state.PlaylistState
import com.tapman104.mpvplayer.player.gesture.GestureOverlay
import com.tapman104.mpvplayer.player.playback.PlayerOverlay

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    playlistState: PlaylistState,
    surfaceView: SurfaceView,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit = {},
    onOpenFile: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSpeedOverride: (Float) -> Unit = {},
    onSpeedRestore: () -> Unit = {},
    onSelectAudioTrack: (Int) -> Unit,
    onSelectSubtitleTrack: (Int) -> Unit,
    onSubtitleAppearance: (size: Float, position: Float) -> Unit = { _, _ -> },
    onSubtitleReset: () -> Unit = {},
    // Auto-subtitle
    onAutoSelectSubtitle: () -> Unit = {},
    // Zoom / pan
    onZoomChange: (Float) -> Unit = {},
    onPanChange: (Float, Float) -> Unit = { _, _ -> },
    onDecodeModeChange: (com.tapman104.mpvplayer.player.model.DecodeMode) -> Unit = {},
    onAspectRatioChange: (com.tapman104.mpvplayer.player.model.AspectRatioMode) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    val toggleControls = { controlsVisible = !controlsVisible }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(modifier)
    ) {
        AndroidView(
            factory = { 
                surfaceView.apply {
                    setOnTouchListener { _, _ -> false } // don't consume — let Compose handle it
                }
            },
            // update is called on every recomposition — keeping the SurfaceView
            // reference alive prevents Compose from recycling/destroying the
            // SurfaceHolder that mpv renders into (which causes a black screen).
            update = { /* intentionally empty — factory already set the touch listener */ },
            modifier = Modifier.fillMaxSize()
        )

        GestureOverlay(
            onSeekForward = { ms -> onSeekRelative(ms) },
            onSeekBackward = { ms -> onSeekRelative(-ms) },
            onSpeedOverride = onSpeedOverride,
            onSpeedRestore = onSpeedRestore,
            onToggleControls = toggleControls,
            // Seek scrub
            currentPositionMs = playerState.currentPositionMs,
            durationMs = playerState.durationMs,
            onSeekTo = onSeek,
            onPauseForSeek = {},    // keep playing during scrub for responsiveness
            onResumeAfterSeek = {}, // no-op; seek is live
            // Zoom / Pan
            currentZoom = playerState.videoZoom,
            currentPanX = playerState.videoPanX,
            currentPanY = playerState.videoPanY,
            onZoomChange = onZoomChange,
            onPanChange = onPanChange,
            modifier = Modifier.fillMaxSize()
        )

        PlayerOverlay(
            playerState = playerState,
            playlistState = playlistState,
            controlsVisible = controlsVisible,
            onControlsVisibilityChange = { controlsVisible = it },
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onOpenFile = onOpenFile,
            onSpeedChange = onSpeedChange,
            onSelectAudioTrack = onSelectAudioTrack,
            onSelectSubtitleTrack = onSelectSubtitleTrack,
            onSubtitleAppearance = onSubtitleAppearance,
            onSubtitleReset = onSubtitleReset,
            onDecodeModeChange = onDecodeModeChange,
            onAspectRatioChange = onAspectRatioChange,
            onAutoSelectSubtitle = onAutoSelectSubtitle,
            onSettingsClick = onSettingsClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}
