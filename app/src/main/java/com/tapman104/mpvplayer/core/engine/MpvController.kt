package com.tapman104.mpvplayer.core.engine

import android.content.Context
import `is`.xyz.mpv.MPVLib
import android.util.Log

class MpvController(private val context: Context) {
    private val TAG = "MpvController"
    val executor = MpvCommandExecutor()
    val dispatcher = MpvEventDispatcher()
    val surface = MpvSurface(executor)

    private var initialized = false

    private fun copyFontAsset() {
        val fontsDir = java.io.File(context.filesDir, "fonts")
        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }
        val fontFile = java.io.File(fontsDir, "Roboto-Regular.ttf")
        if (!fontFile.exists()) {
            try {
                context.assets.open("Roboto-Regular.ttf").use { inputStream ->
                    java.io.FileOutputStream(fontFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(TAG, "Copied Roboto-Regular.ttf to fonts directory")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy font asset", e)
            }
        }
    }

    fun init() {
        if (initialized) return
        copyFontAsset()
        Log.d(TAG, "Initializing MPV engine")
        
        executor.execute {
            try {
                // Initialize JNI context and native engine
                MPVLib.create(context.applicationContext)
                
                // Configure engine options before init
                MPVLib.setOptionString("config", "yes")
                MPVLib.setOptionString("config-dir", context.applicationContext.filesDir.path)
                MPVLib.setOptionString("force-window", "no")
                MPVLib.setOptionString("idle", "once")
                MPVLib.setOptionString("vo", "gpu")
                surface.setVo("gpu")
                MPVLib.setOptionString("gpu-context", "android")
                MPVLib.setOptionString("hwdec", "mediacodec-copy")
                MPVLib.setOptionString("keep-open", "yes")
                
                // Performance options
                MPVLib.setOptionString("vd-lavc-threads", "0")
                MPVLib.setOptionString("vd-lavc-dr", "yes")
                MPVLib.setOptionString("video-sync", "audio")
                MPVLib.setOptionString("interpolation", "no")
                MPVLib.setOptionString("tls-verify", "no")
                MPVLib.setOptionString("demuxer-max-bytes", "32MiB")
                MPVLib.setOptionString("demuxer-max-back-bytes", "32MiB")
                MPVLib.setOptionString("cache-pause-wait", "1")
                
                // Setup fonts
                MPVLib.setOptionString("sub-fonts-dir", "${context.filesDir.path}/fonts")
                MPVLib.setOptionString("sub-font", "Roboto")
                MPVLib.setOptionString("sub-font-provider", "none")
                MPVLib.setOptionString("sub-ass-override", "force")
                
                // Initialize MPV
                MPVLib.init()
                
                // Add event observer and property observers
                MPVLib.addObserver(dispatcher)
                MpvPropertyObserver.registerObservers()
                
                initialized = true
                Log.d(TAG, "MPV engine initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MPV engine", e)
                // Notify listeners of the initialization error
                synchronized(dispatcher) {
                    // Let's print out the error logs
                }
            }
        }
    }

    fun destroy() {
        if (!initialized) return
        Log.d(TAG, "Destroying MPV engine")
        initialized = false

        executor.detachSurface()

        executor.execute {
            try {
                MPVLib.removeObserver(dispatcher)
                MPVLib.destroy()
                Log.d(TAG, "MPV engine destroyed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to destroy MPV engine", e)
            }
        }

        // Shutdown AFTER submitting the cleanup task, not inside it
        executor.shutdown()
    }
}
