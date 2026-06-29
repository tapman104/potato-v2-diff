package com.tapman104.mpvplayer.player.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A [Modifier] that detects single tap, double tap (left/right half), and long-press gestures
 * for a video player seek interface.
 *
 * - **Single tap** → [onToggleControls]
 * - **Double tap on right half** → [onSeekForward]
 * - **Double tap on left half** → [onSeekBackward]
 * - **Long press (500 ms)** → [onLongPressStart] + [onSpeedOverride](2f);
 *   on lift → [onLongPressEnd] + [onSpeedRestore]
 *
 * Ignores events where the first down is already consumed (lets vertical swipe gestures
 * take priority).
 *
 * Uses [awaitEachGesture] so that we stay in [androidx.compose.ui.input.pointer.AwaitPointerEventScope],
 * where [withTimeout] and [awaitPointerEvent] are members.
 */
fun Modifier.doubleTapSeekGesture(
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onToggleControls: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onSpeedOverride: (Float) -> Unit,
    onSpeedRestore: () -> Unit,
): Modifier = composed {
    val currentOnSeekForward    by rememberUpdatedState(onSeekForward)
    val currentOnSeekBackward   by rememberUpdatedState(onSeekBackward)
    val currentOnToggleControls by rememberUpdatedState(onToggleControls)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressEnd   by rememberUpdatedState(onLongPressEnd)
    val currentOnSpeedOverride  by rememberUpdatedState(onSpeedOverride)
    val currentOnSpeedRestore   by rememberUpdatedState(onSpeedRestore)

    pointerInput(Unit) {
        // awaitEachGesture keeps us in AwaitPointerEventScope for the whole gesture cycle.
        // withTimeout / awaitPointerEvent are members of AwaitPointerEventScope.
        awaitEachGesture {

            // ── 1. Wait for the first down ───────────────────────────────────
            val firstDown = awaitFirstDown(requireUnconsumed = false)

            // Let vertical swipe gestures (volume/brightness) take priority
            if (firstDown.isConsumed) return@awaitEachGesture

            val tapPosition = firstDown.position

            // ── 2. Try long press (500 ms hold) ──────────────────────────────
            var isLongPress = false
            try {
                withTimeout(500L) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes.any { change -> !change.pressed }) break
                    }
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                isLongPress = true
            }

            if (isLongPress) {
                currentOnLongPressStart()
                currentOnSpeedOverride(2.0f)
                // Consume all events until the finger lifts
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    event.changes.forEach { it.consume() }
                    if (event.changes.any { change -> !change.pressed }) break
                }
                currentOnLongPressEnd()
                currentOnSpeedRestore()
                return@awaitEachGesture
            }

            // ── 3. First tap lifted — open a 300 ms window for a second tap ──
            val isRightHalf = tapPosition.x >= size.width / 2f
            var isDoubleTap = false

            try {
                withTimeout(300L) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val secondDown = event.changes.firstOrNull { change ->
                            change.pressed && !change.previousPressed
                        }
                        if (secondDown != null) break
                    }
                }
                isDoubleTap = true   // Only reached when second down arrives before timeout
            } catch (_: PointerEventTimeoutCancellationException) {
                // Timeout expired → single tap; isDoubleTap stays false
            }

            if (isDoubleTap) {
                if (isRightHalf) currentOnSeekForward() else currentOnSeekBackward()
            } else {
                currentOnToggleControls()
            }
        }
    }
}
