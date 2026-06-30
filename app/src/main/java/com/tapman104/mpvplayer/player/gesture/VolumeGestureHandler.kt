package com.tapman104.mpvplayer.player.gesture

import android.content.Context
import android.media.AudioManager
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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun VolumeGestureHandler(
    volumePercentage: Int,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    
    var isDragging by remember { mutableStateOf(false) }
    var showIndicator by remember { mutableStateOf(false) }
    var hideTrigger by remember { mutableIntStateOf(0) }
    
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
            .volumeGesture(
                audioManager = audioManager,
                onVolumeChange = onVolumeChange,
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
            VolumeIndicator(percentage = volumePercentage)
        }
    }
}

fun Modifier.volumeGesture(
    audioManager: AudioManager,
    onVolumeChange: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
): Modifier = composed {
    val currentOnVolumeChange by rememberUpdatedState(onVolumeChange)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    
    pointerInput(Unit) {
        val topMargin = 48.dp.toPx()
        val bottomMargin = 48.dp.toPx()
        val rightMargin = 24.dp.toPx()

        var startVolume = 0
        var maxVolume = 0
        var dragAccumulator = 0f
        var lastHandledVolume = -1

        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            if (firstDown.isConsumed) return@awaitEachGesture
            
            val offset = firstDown.position
            val rightHalf = size.width / 2f
            
            if (offset.x > rightHalf && 
                offset.y > topMargin && 
                offset.y < (size.height - bottomMargin) &&
                offset.x < (size.width - rightMargin)
            ) {
                firstDown.consume()
                val pointerId = firstDown.id
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                lastHandledVolume = startVolume
                dragAccumulator = 0f
                
                val percentage = if (maxVolume > 0) {
                    (startVolume.toFloat() / maxVolume * 100).toInt()
                } else {
                    0
                }
                currentOnVolumeChange(percentage)
                currentOnDragStart()
                
                var lastPosition = offset.y
                
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    event.changes.forEach { it.consume() }
                    
                    val change = event.changes.firstOrNull { it.id == pointerId }
                    if (change != null) {
                        val currentPosition = change.position.y
                        val dragAmount = currentPosition - lastPosition
                        lastPosition = currentPosition
                        
                        dragAccumulator += dragAmount
                        
                        val volumeDelta = (-dragAccumulator / size.height) * maxVolume
                        val newVolume = (startVolume + volumeDelta).roundToInt().coerceIn(0, maxVolume)
                        
                        if (newVolume != lastHandledVolume) {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                            lastHandledVolume = newVolume
                            
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val newPercentage = if (maxVolume > 0) {
                                (currentVol.toFloat() / maxVolume * 100).toInt()
                            } else {
                                0
                            }
                            currentOnVolumeChange(newPercentage)
                        }
                    }
                    if (event.changes.any { it.id == pointerId && !it.pressed }) {
                        break
                    }
                }
                currentOnDragEnd()
            }
        }
    }
}

@Composable
private fun VolumeIndicator(percentage: Int) {
    val icon = when {
        percentage == 0 -> Icons.AutoMirrored.Filled.VolumeOff
        percentage < 50 -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
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
                contentDescription = "Volume",
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
