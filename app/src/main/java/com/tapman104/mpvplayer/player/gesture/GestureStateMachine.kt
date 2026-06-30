package com.tapman104.mpvplayer.player.gesture

import android.media.AudioManager
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerId
import kotlin.math.abs
import kotlin.math.ln

internal class GestureStateMachine(
    private val audioManager: AudioManager,
    private val currentPositionMs: () -> Long,
    private val durationMs: () -> Long,
    private val initialZoomProvider: () -> Float,
    private val initialBrightnessProvider: () -> Float,
    private val listenerProvider: () -> PlayerGestureListener,
    private val screenWidthProvider: () -> Int,
    private val screenHeightProvider: () -> Int
) {
    private var continuationActive = false
    private var continuationSide = false
    private var lastContinuationTime = 0L

    private val listener get() = listenerProvider()

    suspend fun AwaitPointerEventScope.processGesture() {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        if (firstDown.isConsumed) return
        
        val startX = firstDown.position.x
        val isRightHalf = startX >= screenWidthProvider() / 2f
        val pointerId = firstDown.id
        val startTime = android.os.SystemClock.uptimeMillis()

        if (checkContinuationTimeout(isRightHalf)) {
            listener.onContinueSeek(isRightHalf)
            lastContinuationTime = android.os.SystemClock.uptimeMillis()
            // Return to let awaitEachGesture wait for pointer up
            return
        }

        var activeGesture = ActiveGesture.NONE
        var aborted = false

        var deltaX = 0f
        var deltaY = 0f
        
        // Horizontal state
        var startPositionMs = currentPositionMs()
        var lastTargetPositionMs = startPositionMs

        // Volume state
        var maxVolume = 0
        var startVolume = 0
        var lastHandledVolume = -1
        var dragAccumulator = 0f

        // Brightness state
        var startBrightness = 0f

        // Pinch state
        var p1Id: PointerId? = null
        var p2Id: PointerId? = null
        var zoomConfirmed = false
        var accumulatedDistanceChange = 0f
        var previousDist = 0f
        var zoomAccumulator = initialZoomProvider()

        while (true) {
            val elapsed = android.os.SystemClock.uptimeMillis() - startTime
            val timeRemaining = if (activeGesture == ActiveGesture.NONE) {
                (LONG_PRESS_TIMEOUT - elapsed).coerceAtLeast(1L)
            } else {
                Long.MAX_VALUE
            }

            val event = try {
                if (timeRemaining < Long.MAX_VALUE) {
                    withTimeout(timeRemaining) { awaitPointerEvent(PointerEventPass.Main) }
                } else {
                    awaitPointerEvent(PointerEventPass.Main)
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                if (activeGesture == ActiveGesture.NONE) {
                    activeGesture = ActiveGesture.LONG_PRESS
                    continuationActive = false
                    listener.onLongPressStart()
                    listener.onSpeedOverride(2.0f)
                }
                continue
            }

            if (event.changes.any { it.isConsumed }) {
                aborted = true
                break
            }

            val pressedCount = event.changes.count { it.pressed }

            if (pressedCount >= 2) {
                if (activeGesture != ActiveGesture.PINCH_ZOOM) {
                    cancelActiveGesture(activeGesture, lastTargetPositionMs)
                    activeGesture = ActiveGesture.PINCH_ZOOM
                    continuationActive = false
                    
                    val pressedChanges = event.changes.filter { it.pressed }
                    p1Id = pressedChanges[0].id
                    p2Id = pressedChanges[1].id
                    previousDist = (pressedChanges[0].position - pressedChanges[1].position).getDistance()
                    zoomAccumulator = initialZoomProvider()
                    accumulatedDistanceChange = 0f
                    zoomConfirmed = false
                }

                val p1 = event.changes.firstOrNull { it.id == p1Id }
                val p2 = event.changes.firstOrNull { it.id == p2Id }

                if (p1 != null && p2 != null && p1.pressed && p2.pressed) {
                    val dist = (p1.position - p2.position).getDistance()
                    val distChange = abs(dist - previousDist)

                    if (!zoomConfirmed) {
                        accumulatedDistanceChange += distChange
                        if (accumulatedDistanceChange > DISTANCE_THRESHOLD) {
                            zoomConfirmed = true
                            listener.onZoomStart()
                        }
                    }

                    if (zoomConfirmed) {
                        event.changes.forEach { if (it.id == p1Id || it.id == p2Id) it.consume() }
                        if (previousDist > 0f && dist > 0f) {
                            val clampedZoom = GestureUtils.calculateZoom(previousDist, dist, zoomAccumulator)
                            val zoomDelta = ln(dist / previousDist)
                            zoomAccumulator += zoomDelta
                            listener.onZoomUpdate(clampedZoom)
                        }
                    }
                    previousDist = dist
                }
            } else if (activeGesture != ActiveGesture.PINCH_ZOOM) {
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change != null && change.pressed) {
                    val moveX = change.position.x - change.previousPosition.x
                    val moveY = change.position.y - change.previousPosition.y

                    if (activeGesture == ActiveGesture.NONE) {
                        deltaX += moveX
                        deltaY += moveY

                        if (abs(deltaX) > DRAG_THRESHOLD || abs(deltaY) > DRAG_THRESHOLD) {
                            if (abs(deltaX) > DRAG_THRESHOLD && abs(deltaX) > abs(deltaY) * 2f) {
                                activeGesture = ActiveGesture.HORIZONTAL_SEEK
                                startPositionMs = currentPositionMs()
                                lastTargetPositionMs = startPositionMs
                                listener.onSeekStart()
                            } else if (abs(deltaY) > DRAG_THRESHOLD) {
                                if (isRightHalf) {
                                    activeGesture = ActiveGesture.VOLUME_DRAG
                                    maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    lastHandledVolume = startVolume
                                    dragAccumulator = deltaY
                                    val percentage = if (maxVolume > 0) ((startVolume.toFloat() / maxVolume) * 100).toInt() else 0
                                    listener.onVolumeChange(percentage)
                                    listener.onVolumeDragStart()
                                } else {
                                    activeGesture = ActiveGesture.BRIGHTNESS_DRAG
                                    startBrightness = initialBrightnessProvider()
                                    dragAccumulator = deltaY
                                    listener.onBrightnessUpdate(startBrightness)
                                    listener.onBrightnessDragStart()
                                }
                            }
                            continuationActive = false
                        }
                    } else if (activeGesture == ActiveGesture.HORIZONTAL_SEEK) {
                        deltaX += moveX
                    } else if (activeGesture == ActiveGesture.VOLUME_DRAG || activeGesture == ActiveGesture.BRIGHTNESS_DRAG) {
                        dragAccumulator += moveY
                    }

                    if (activeGesture != ActiveGesture.NONE) {
                        change.consume()
                    }

                    when (activeGesture) {
                        ActiveGesture.HORIZONTAL_SEEK -> {
                            val dur = durationMs()
                            val seekMs = GestureUtils.calculateHorizontalSeekMs(deltaX, dur, screenWidthProvider())
                            val targetPositionMs = (startPositionMs + seekMs).coerceIn(0L, dur)
                            if (targetPositionMs != lastTargetPositionMs) {
                                lastTargetPositionMs = targetPositionMs
                                listener.onSeekPreview(targetPositionMs, targetPositionMs - startPositionMs)
                            }
                        }
                        ActiveGesture.VOLUME_DRAG -> {
                            val newVolume = GestureUtils.calculateVolume(startVolume, dragAccumulator, screenHeightProvider(), maxVolume)
                            if (newVolume != lastHandledVolume) {
                                lastHandledVolume = newVolume
                                val newPercentage = if (maxVolume > 0) ((newVolume.toFloat() / maxVolume) * 100).toInt() else 0
                                listener.onVolumeChange(newPercentage)
                            }
                        }
                        ActiveGesture.BRIGHTNESS_DRAG -> {
                            val newBrightness = GestureUtils.calculateBrightness(startBrightness, dragAccumulator, screenHeightProvider())
                            listener.onBrightnessUpdate(newBrightness)
                        }
                        else -> {}
                    }
                }
            }

            if (event.changes.none { it.pressed }) {
                break
            }
        }

        handleGestureEnd(aborted, activeGesture, isRightHalf, zoomConfirmed, lastTargetPositionMs)
    }

    private fun checkContinuationTimeout(isRightHalf: Boolean): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        if (continuationActive && (now - lastContinuationTime) > CONTINUATION_TIMEOUT) {
            continuationActive = false
        }
        if (continuationActive && continuationSide != isRightHalf) {
            continuationActive = false
        }
        return continuationActive
    }

    private fun cancelActiveGesture(activeGesture: ActiveGesture, lastTargetPositionMs: Long) {
        when (activeGesture) {
            ActiveGesture.HORIZONTAL_SEEK -> {
                listener.onSeekCommit(lastTargetPositionMs)
                listener.onSeekEnd()
            }
            ActiveGesture.VOLUME_DRAG -> listener.onVolumeDragEnd()
            ActiveGesture.BRIGHTNESS_DRAG -> listener.onBrightnessDragEnd()
            ActiveGesture.LONG_PRESS -> {
                listener.onLongPressEnd()
                listener.onSpeedRestore()
            }
            else -> {}
        }
    }

    private suspend fun AwaitPointerEventScope.handleGestureEnd(
        aborted: Boolean,
        activeGesture: ActiveGesture,
        isRightHalf: Boolean,
        zoomConfirmed: Boolean,
        lastTargetPositionMs: Long
    ) {
        if (aborted) {
            continuationActive = false
            return
        }
        when (activeGesture) {
            ActiveGesture.HORIZONTAL_SEEK -> {
                listener.onSeekCommit(lastTargetPositionMs)
                listener.onSeekEnd()
            }
            ActiveGesture.VOLUME_DRAG -> listener.onVolumeDragEnd()
            ActiveGesture.BRIGHTNESS_DRAG -> listener.onBrightnessDragEnd()
            ActiveGesture.PINCH_ZOOM -> {
                if (zoomConfirmed) listener.onZoomEnd()
            }
            ActiveGesture.LONG_PRESS -> {
                listener.onLongPressEnd()
                listener.onSpeedRestore()
            }
            ActiveGesture.NONE -> {
                handleTapCandidate(isRightHalf)
            }
        }
    }

    private suspend fun AwaitPointerEventScope.handleTapCandidate(isRightHalf: Boolean) {
        var isDoubleTap = false
        try {
            withTimeout(DOUBLE_TAP_TIMEOUT) {
                val secondDown = awaitFirstDown(requireUnconsumed = false)
                val isSecondTapRightHalf = secondDown.position.x >= screenWidthProvider() / 2f
                if (isSecondTapRightHalf == isRightHalf) {
                    isDoubleTap = true
                }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            // Single tap
        }

        if (isDoubleTap) {
            if (isRightHalf) listener.onSeekForward() else listener.onSeekBackward()
            continuationActive = true
            continuationSide = isRightHalf
            lastContinuationTime = android.os.SystemClock.uptimeMillis()
        } else {
            listener.onToggleControls()
        }
    }
}
