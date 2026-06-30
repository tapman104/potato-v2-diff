// videoZoom is log2-scale per MpvCommandExecutor.setVideoZoom; 0f = 1x, log2(3) approx 1.585f = 3x
package com.tapman104.mpvplayer.player.gesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun PinchZoomGestureHandler(
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isZooming by remember { mutableStateOf(false) }
    var showIndicator by remember { mutableStateOf(false) }
    var hideTrigger by remember { mutableIntStateOf(0) }
    
    // Track local zoom state starting from currentZoom
    var localZoom by remember { mutableFloatStateOf(currentZoom) }

    LaunchedEffect(currentZoom) {
        if (!isZooming) {
            localZoom = currentZoom
        }
    }

    LaunchedEffect(hideTrigger, isZooming) {
        if (isZooming) {
            showIndicator = true
        } else if (hideTrigger > 0) {
            showIndicator = true
            delay(700L)
            showIndicator = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pinchZoomGesture(
                currentZoom = localZoom,
                onZoomUpdate = { newZoom ->
                    localZoom = newZoom
                    onZoomChange(newZoom)
                },
                onZoomStart = { isZooming = true },
                onZoomEnd = {
                    isZooming = false
                    hideTrigger++
                }
            )
    ) {
        AnimatedVisibility(
            visible = showIndicator,
            enter = fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.75f),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            PinchZoomIndicator(zoom = localZoom)
        }
    }
}

fun Modifier.pinchZoomGesture(
    currentZoom: Float,
    onZoomUpdate: (Float) -> Unit,
    onZoomStart: () -> Unit,
    onZoomEnd: () -> Unit
): Modifier = composed {
    val currentOnZoomUpdate by rememberUpdatedState(onZoomUpdate)
    val currentOnZoomStart by rememberUpdatedState(onZoomStart)
    val currentOnZoomEnd by rememberUpdatedState(onZoomEnd)
    
    val currentInitialZoom by rememberUpdatedState(currentZoom)

    pointerInput(Unit) {
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            if (firstDown.isConsumed) return@awaitEachGesture
            
            var zoomConfirmed = false
            var zoomAccumulator = 0f
            var previousDist = 0f
            var accumulatedDistanceChange = 0f
            val distanceThreshold = 5f
            
            var p1Id: androidx.compose.ui.input.pointer.PointerId? = null
            var p2Id: androidx.compose.ui.input.pointer.PointerId? = null

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                
                if (event.changes.any { it.isConsumed }) {
                    if (zoomConfirmed) {
                        currentOnZoomEnd()
                    }
                    break
                }

                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount != 2) {
                    if (zoomConfirmed) {
                        currentOnZoomEnd()
                    }
                    break
                }

                val pressedChanges = event.changes.filter { it.pressed }
                val p1 = pressedChanges[0]
                val p2 = pressedChanges[1]
                
                val dist = (p1.position - p2.position).getDistance()
                
                if (p1Id == null || p2Id == null) {
                    p1Id = p1.id
                    p2Id = p2.id
                    previousDist = dist
                    zoomAccumulator = currentInitialZoom
                } else {
                    val distChange = abs(dist - previousDist)
                    
                    if (!zoomConfirmed) {
                        accumulatedDistanceChange += distChange
                        if (accumulatedDistanceChange > distanceThreshold) {
                            zoomConfirmed = true
                            currentOnZoomStart()
                        }
                    }
                    
                    if (zoomConfirmed) {
                        event.changes.forEach { 
                            if (it.id == p1Id || it.id == p2Id) {
                                it.consume() 
                            }
                        }
                        
                        if (previousDist > 0f && dist > 0f) {
                            val zoomDelta = ln(dist / previousDist)
                            zoomAccumulator += zoomDelta
                            val clampedZoom = zoomAccumulator.coerceIn(0f, 1.585f)
                            currentOnZoomUpdate(clampedZoom)
                        }
                    }
                    previousDist = dist
                }
            }
        }
    }
}

@Composable
fun PinchZoomIndicator(zoom: Float) {
    val percentage = ((2.0.pow(zoom.toDouble())) * 100).roundToInt()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1E1E1E).copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ZoomIn,
                contentDescription = "Zoom",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$percentage%",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
