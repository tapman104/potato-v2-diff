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
    onContinueSeek: (isRightHalf: Boolean) -> Unit,
    onToggleControls: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onSpeedOverride: (Float) -> Unit,
    onSpeedRestore: () -> Unit,
): Modifier = composed {
    val currentOnSeekForward    by rememberUpdatedState(onSeekForward)
    val currentOnSeekBackward   by rememberUpdatedState(onSeekBackward)
    val currentOnContinueSeek   by rememberUpdatedState(onContinueSeek)
    val currentOnToggleControls by rememberUpdatedState(onToggleControls)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressEnd   by rememberUpdatedState(onLongPressEnd)
    val currentOnSpeedOverride  by rememberUpdatedState(onSpeedOverride)
    val currentOnSpeedRestore   by rememberUpdatedState(onSpeedRestore)

    pointerInput(Unit) {
        var continuationActive = false
        var continuationSide = false
        var lastContinuationTime = 0L

        // awaitEachGesture keeps us in AwaitPointerEventScope for the whole gesture cycle.
        // withTimeout / awaitPointerEvent are members of AwaitPointerEventScope.
        awaitEachGesture {

            // ── 1. Wait for the first down ───────────────────────────────────
            val firstDown = awaitFirstDown(requireUnconsumed = false)

            // Let vertical swipe gestures (volume/brightness) take priority
            if (firstDown.isConsumed) return@awaitEachGesture

            val tapPosition = firstDown.position
            val isRightHalf = tapPosition.x >= size.width / 2f
            val now = System.currentTimeMillis()

            if (continuationActive && (now - lastContinuationTime) > 650L) {
                continuationActive = false
            }
            if (continuationActive && continuationSide != isRightHalf) {
                continuationActive = false
            }

            // ── 2. Try long press (500 ms hold) ──────────────────────────────
            var isLongPress = false
            var aborted = false
            try {
                withTimeout(500L) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes.any { it.isConsumed }) {
                            aborted = true
                            break
                        }
                        if (event.changes.any { change -> !change.pressed }) break
                    }
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                // If it timed out, it was held for 500ms without being consumed
                isLongPress = true
            }

            if (aborted) {
                continuationActive = false
                return@awaitEachGesture
            }

            if (isLongPress) {
                continuationActive = false
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

            if (continuationActive) {
                currentOnContinueSeek(isRightHalf)
                lastContinuationTime = System.currentTimeMillis()
                return@awaitEachGesture
            }

            continuationActive = false
            // ── 3. First tap lifted — open a 300 ms window for a second tap ──
            var isDoubleTap = false

            try {
                withTimeout(300L) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes.any { it.isConsumed }) {
                            aborted = true
                            break
                        }
                        val secondDown = event.changes.firstOrNull { change ->
                            change.pressed && !change.previousPressed
                        }
                        if (secondDown != null) break
                    }
                }
                if (!aborted) {
                    isDoubleTap = true   // Only reached when second down arrives before timeout
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                // Timeout expired → single tap; isDoubleTap stays false
            }

            if (aborted) {
                continuationActive = false
                return@awaitEachGesture
            }

            if (isDoubleTap) {
                if (isRightHalf) currentOnSeekForward() else currentOnSeekBackward()
                continuationActive = true
                continuationSide = isRightHalf
                lastContinuationTime = System.currentTimeMillis()
            } else {
                currentOnToggleControls()
            }
        }
    }
}
