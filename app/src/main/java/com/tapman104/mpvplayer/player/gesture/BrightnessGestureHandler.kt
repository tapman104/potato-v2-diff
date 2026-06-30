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
import androidx.compose.ui.input.pointer.PointerEventPass
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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun BrightnessGestureHandler(
    initialBrightness: Float = 0.5f,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var showIndicator by remember { mutableStateOf(false) }
    var hideTrigger by remember { mutableIntStateOf(0) }
    
    // Track local brightness state starting from initialBrightness
    var currentBrightness by remember { mutableFloatStateOf(initialBrightness) }

    LaunchedEffect(initialBrightness) {
        if (!isDragging) {
            currentBrightness = initialBrightness
        }
    }

    LaunchedEffect(hideTrigger, isDragging) {
        if (isDragging) {
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
            .brightnessGesture(
                initialBrightness = currentBrightness,
                onBrightnessUpdate = { newBrightness ->
                    currentBrightness = newBrightness
                    onBrightnessChange(newBrightness)
                },
                onDragStart = { isDragging = true },
                onDragEnd = {
                    isDragging = false
                    hideTrigger++
                }
            )
    ) {
        AnimatedVisibility(
            visible = showIndicator,
            enter = fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.75f),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 56.dp)
        ) {
            BrightnessIndicator(brightness = currentBrightness)
        }
    }
}

fun Modifier.brightnessGesture(
    initialBrightness: Float,
    onBrightnessUpdate: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
): Modifier = composed {
    val currentOnBrightnessUpdate by rememberUpdatedState(onBrightnessUpdate)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    
    val currentInitialBrightness by rememberUpdatedState(initialBrightness)

    pointerInput(Unit) {
        val topMargin = 48.dp.toPx()
        val bottomMargin = 48.dp.toPx()
        val leftMargin = 24.dp.toPx()

        var startBrightness = 0f
        var dragAccumulator = 0f

        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            if (firstDown.isConsumed) return@awaitEachGesture
            
            val offset = firstDown.position
            val leftHalf = size.width / 2f
            
            if (offset.x < leftHalf && 
                offset.y > topMargin && 
                offset.y < (size.height - bottomMargin) &&
                offset.x > leftMargin
            ) {
                firstDown.consume()
                startBrightness = currentInitialBrightness
                dragAccumulator = 0f
                
                currentOnBrightnessUpdate(startBrightness)
                currentOnDragStart()
                
                var lastPosition = offset.y
                
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    event.changes.forEach { it.consume() }
                    
                    val change = event.changes.firstOrNull()
                    if (change != null) {
                        val currentPosition = change.position.y
                        val dragAmount = currentPosition - lastPosition
                        lastPosition = currentPosition
                        
                        dragAccumulator += dragAmount
                        
                        val brightnessDelta = -dragAccumulator / size.height
                        val newBrightness = (startBrightness + brightnessDelta).coerceIn(0f, 1f)
                        
                        currentOnBrightnessUpdate(newBrightness)
                    }
                    
                    if (event.changes.any { !it.pressed }) {
                        break
                    }
                }
                currentOnDragEnd()
            }
        }
    }
}

@Composable
private fun BrightnessIndicator(brightness: Float) {
    val percentage = (brightness * 100).roundToInt()
    val icon = when {
        percentage < 33 -> Icons.Filled.BrightnessLow
        percentage <= 66 -> Icons.Filled.BrightnessMedium
        else -> Icons.Filled.BrightnessHigh
    }

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
                imageVector = icon,
                contentDescription = "Brightness",
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
