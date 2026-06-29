package com.tapman104.mpvplayer.core.engine

interface MpvEventListener {
    fun onFileLoaded()
    fun onPlaybackStarted()
    fun onPlaybackStopped(endReason: Int)
    fun onPropertyChange(name: String, value: Any?)
    fun onError(message: String)
    /** Called when MPV's VO crashes fatally (e.g. surface destroyed during lock). */
    fun onVoLost() {}
}
