package com.tapman104.mpvplayer.player.playback

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.tapman104.mpvplayer.player.state.PlayerState
import com.tapman104.mpvplayer.player.model.DecodeMode

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    surfaceView: SurfaceView,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenFile: () -> Unit,
    fileName: String = "Unknown",
    onSelectAudioTrack: () -> Unit = {},
    onSelectSubtitleTrack: () -> Unit = {},
    onCycleDecodeMode: (DecodeMode) -> Unit = {},
    onMoreOptions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(modifier)
    ) {
        AndroidView(
            factory = {
                surfaceView.apply {
                    setOnTouchListener { _, _ -> false }
                }
            },
            update = { /* intentionally empty */ },
            modifier = Modifier.fillMaxSize()
        )

        PlayerOverlay(
            fileName = fileName,
            playerState = playerState,
            onOpenFile = onOpenFile,
            onTogglePlay = onTogglePlay,
            onSeek = onSeek,
            onSelectAudioTrack = onSelectAudioTrack,
            onSelectSubtitleTrack = onSelectSubtitleTrack,
            onCycleDecodeMode = onCycleDecodeMode,
            onMoreOptions = onMoreOptions,
            modifier = Modifier.fillMaxSize()
        )
    }
}
