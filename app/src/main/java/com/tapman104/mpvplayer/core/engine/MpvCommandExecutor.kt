package com.tapman104.mpvplayer.core.engine

import `is`.xyz.mpv.MPVLib
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MpvCommandExecutor {
    private val TAG = "MpvCommandExecutor"
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mpv-engine-thread")
    }

    /**
     * Incremented on every surface attach. Detach tasks capture this value at queue
     * time and skip themselves if a newer attach has since been queued, preventing
     * a stale detach from tearing down a freshly attached surface.
     */
    private val surfaceGeneration = AtomicInteger(0)

    fun execute(action: () -> Unit) {
        if (!executor.isShutdown) {
            executor.submit(action)
        }
    }

    /**
     * Increments and returns the new surface generation. Call this immediately
     * before queuing an attachSurface task so the matching detachSurface can
     * recognise it is stale if a newer generation has already been registered.
     */
    fun nextSurfaceGeneration(): Int = surfaceGeneration.incrementAndGet()

    /**
     * Queues MPVLib.detachSurface(). If [nextSurfaceGeneration] has been called
     * again by the time this task executes (i.e., a new attach is already in
     * flight), the detach is silently dropped.
     */
    fun detachSurface() {
        val capturedGen = surfaceGeneration.get()
        execute {
            val current = surfaceGeneration.get()
            if (current == capturedGen) {
                Log.d(TAG, "detachSurface gen=$capturedGen")
                MPVLib.detachSurface()
            } else {
                Log.d(TAG, "detachSurface skipped — stale gen=$capturedGen, current=$current")
            }
        }
    }

    fun play() {
        execute {
            MPVLib.setPropertyBoolean(MpvConstants.PROP_PAUSE, false)
        }
    }

    fun pause() {
        execute {
            MPVLib.setPropertyBoolean(MpvConstants.PROP_PAUSE, true)
        }
    }

    fun togglePlay() {
        execute {
            val paused = MPVLib.getPropertyBoolean(MpvConstants.PROP_PAUSE) ?: false
            MPVLib.setPropertyBoolean(MpvConstants.PROP_PAUSE, !paused)
        }
    }

    fun seek(seconds: Double) {
        execute {
            MPVLib.command("seek", seconds.toString(), "absolute", "exact")
        }
    }

    fun seekAbsolute(seconds: Double) {
        execute {
            MPVLib.command("seek", seconds.toString(), "absolute")
        }
    }

    fun seekRelative(seconds: Double) {
        execute {
            MPVLib.command("seek", seconds.toString(), "relative")
        }
    }

    fun loadFile(path: String) {
        execute {
            MPVLib.command("loadfile", path)
        }
    }

    fun setAudioTrack(id: Int) {
        execute {
            val value = if (id < 0) "no" else id.toString()
            MPVLib.setPropertyString(MpvConstants.PROP_AUDIO_ID, value)
        }
    }

    fun setSubtitleTrack(id: Int) {
        execute {
            val value = if (id < 0) "no" else id.toString()
            MPVLib.setPropertyString(MpvConstants.PROP_SUBTITLE_ID, value)
        }
    }

    fun setSpeed(speed: Double) {
        execute {
            MPVLib.setPropertyDouble(MpvConstants.PROP_SPEED, speed)
        }
    }

    fun setHwdec(mode: String) {
        execute {
            MPVLib.setPropertyString(MpvConstants.PROP_HWDEC, mode)
        }
    }

    fun setVolume(volume: Int) {
        execute {
            MPVLib.setPropertyDouble(MpvConstants.PROP_VOLUME, volume.toDouble())
        }
    }

    fun setSubtitleAppearance(size: Float, position: Float) {
        execute {
            MPVLib.setPropertyDouble("sub-scale", size.toDouble())
            MPVLib.setPropertyDouble("sub-pos", (100.0 - (position * 100.0)))
        }
    }

    private var lastZoom = Float.NaN

    fun setVideoZoom(zoom: Float) {
        if (zoom == lastZoom) return
        lastZoom = zoom
        execute { MPVLib.setPropertyDouble("video-zoom", zoom.toDouble()) }
    }

    private var lastPanX = Float.NaN
    private var lastPanY = Float.NaN

    fun setVideoPan(panX: Float, panY: Float) {
        if (panX == lastPanX && panY == lastPanY) return
        lastPanX = panX
        lastPanY = panY
        execute {
            MPVLib.setPropertyDouble("video-pan-x", panX.toDouble())
            MPVLib.setPropertyDouble("video-pan-y", panY.toDouble())
        }
    }

    fun shutdown() {
        executor.shutdown()
    }
}
