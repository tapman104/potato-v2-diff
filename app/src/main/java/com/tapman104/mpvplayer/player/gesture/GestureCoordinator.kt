package com.tapman104.mpvplayer.player.gesture

import android.media.AudioManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.gestureCoordinator(
    audioManager: AudioManager,
    currentPositionMs: () -> Long,
    durationMs: () -> Long,
    currentZoom: Float,
    initialBrightness: Float,
    listener: PlayerGestureListener
): Modifier = composed {
    val currentListener by rememberUpdatedState(listener)
    val currentInitialZoom by rememberUpdatedState(currentZoom)
    val currentInitialBrightness by rememberUpdatedState(initialBrightness)

    pointerInput(Unit) {
        val stateMachine = GestureStateMachine(
            audioManager = audioManager,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            initialZoomProvider = { currentInitialZoom },
            initialBrightnessProvider = { currentInitialBrightness },
            listenerProvider = { currentListener },
            screenWidthProvider = { size.width },
            screenHeightProvider = { size.height }
        )

        awaitEachGesture {
            with(stateMachine) {
                processGesture()
            }
        }
    }
}
