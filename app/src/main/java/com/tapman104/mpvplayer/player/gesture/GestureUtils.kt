package com.tapman104.mpvplayer.player.gesture

import kotlin.math.log2
import kotlin.math.roundToInt

internal object GestureUtils {
    fun calculateHorizontalSeekMs(deltaX: Float, durationMs: Long, screenWidth: Int): Long {
        return (deltaX * (durationMs * HORIZONTAL_SEEK_SENSITIVITY_FRACTION / screenWidth)).toLong()
    }

    fun calculateVolume(startVolume: Int, dragAccumulator: Float, screenHeight: Int, maxVolume: Int): Int {
        val volumeDelta = (-dragAccumulator / screenHeight) * maxVolume
        return (startVolume + volumeDelta).roundToInt().coerceIn(0, maxVolume)
    }

    fun calculateBrightness(startBrightness: Float, dragAccumulator: Float, screenHeight: Int): Float {
        val brightnessDelta = -dragAccumulator / screenHeight
        return (startBrightness + brightnessDelta).coerceIn(0f, 1f)
    }

    fun calculateZoom(previousDist: Float, dist: Float, zoomAccumulator: Float): Float {
        val zoomDelta = log2(dist / previousDist)
        return (zoomAccumulator + zoomDelta).coerceIn(0f, MAX_ZOOM)
    }
}
