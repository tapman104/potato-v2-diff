package com.tapman104.mpvplayer.player.gesture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

const val HORIZONTAL_SEEK_SENSITIVITY_FRACTION = 0.1f

fun Modifier.horizontalSwipeSeekGesture(
    currentPositionMs: Long,
    durationMs: Long,
    onSeekPreview: (positionMs: Long, deltaMs: Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekCommit: (positionMs: Long) -> Unit,
    onSeekEnd: () -> Unit,
): Modifier = composed {
    val currentOnSeekPreview by rememberUpdatedState(onSeekPreview)
    val currentOnSeekStart by rememberUpdatedState(onSeekStart)
    val currentOnSeekCommit by rememberUpdatedState(onSeekCommit)
    val currentOnSeekEnd by rememberUpdatedState(onSeekEnd)
    val currentPosition by rememberUpdatedState(currentPositionMs)
    val currentDuration by rememberUpdatedState(durationMs)

    pointerInput(Unit) {
        val topMargin = 48.dp.toPx()
        val bottomMargin = 48.dp.toPx()
        val leftMargin = 24.dp.toPx()
        val rightMargin = 24.dp.toPx()

        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            if (firstDown.isConsumed) return@awaitEachGesture

            val offset = firstDown.position
            // Center zone only
            if (offset.y < topMargin || offset.y > size.height - bottomMargin ||
                offset.x < (size.width / 2f) - leftMargin || offset.x > (size.width / 2f) + rightMargin) {
                return@awaitEachGesture
            }

            val pointerId = firstDown.id
            var deltaX = 0f
            var deltaY = 0f
            var isCommitted = false
            val startTime = System.currentTimeMillis()
            var startPositionMs = currentPosition
            var lastTargetPositionMs = startPositionMs

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change != null) {
                    if (change.isConsumed) {
                        if (isCommitted) {
                            currentOnSeekEnd()
                            isCommitted = false
                        }
                        break
                    }

                    deltaX += change.position.x - change.previousPosition.x
                    deltaY += change.position.y - change.previousPosition.y

                    if (!isCommitted) {
                        val elapsed = System.currentTimeMillis() - startTime
                        if (abs(deltaX) > 30f && abs(deltaX) > abs(deltaY) * 2f && elapsed >= 80L) {
                            isCommitted = true
                            startPositionMs = currentPosition
                            currentOnSeekStart()
                        }
                    }

                    if (isCommitted) {
                        change.consume()
                        val seekMs = (deltaX * (currentDuration * HORIZONTAL_SEEK_SENSITIVITY_FRACTION / size.width)).toLong()
                        val targetPositionMs = (startPositionMs + seekMs).coerceIn(0L, currentDuration)
                        
                        if (targetPositionMs != lastTargetPositionMs) {
                            lastTargetPositionMs = targetPositionMs
                            currentOnSeekPreview(targetPositionMs, targetPositionMs - startPositionMs)
                        }
                    }
                }

                if (event.changes.any { it.id == pointerId && !it.pressed }) {
                    break
                }
            }

            if (isCommitted) {
                currentOnSeekCommit(lastTargetPositionMs)
                currentOnSeekEnd()
            }
        }
    }
}

@Composable
fun HorizontalSeekIndicator(currentTimeLabel: String, deltaLabel: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1E1E1E).copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currentTimeLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = deltaLabel,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
