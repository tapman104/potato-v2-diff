package com.tapman104.mpvplayer.player.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tapman104.mpvplayer.core.database.ResumePositionDao
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.core.engine.MpvController
import com.tapman104.mpvplayer.core.engine.MpvConstants
import com.tapman104.mpvplayer.core.engine.MpvEventListener
import com.tapman104.mpvplayer.core.engine.TrackListParser
import com.tapman104.mpvplayer.player.model.*
import com.tapman104.mpvplayer.player.state.*
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode

class PlayerViewModel(
    private val context: Context,
    private val resumePositionDao: ResumePositionDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel(), MpvEventListener {
    private val TAG = "PlayerViewModel"

    private var lastTimePosUpdate = 0L
    private var lastSeekTime = 0L

    val controller = MpvController(context)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    val playlistManager = PlaylistManager(
        context = context,
        onLoadFile = { path -> controller.executor.loadFile(path) },
        hasSurface = { controller.surface.hasSurface() }
    )

    private val subtitleController = SubtitleController(
        executor = controller.executor,
        userPreferencesRepository = userPreferencesRepository,
        coroutineScope = viewModelScope,
        onPlayerStateUpdate = { size, position ->
            _playerState.update { it.copy(subtitleSize = size, subtitlePosition = position) }
        }
    )

    private val resumePositionManager = ResumePositionManager(
        resumePositionDao = resumePositionDao,
        coroutineScope = viewModelScope,
        getPlayerDurationMs = { _playerState.value.durationMs }
    )

    val playlistState: StateFlow<PlaylistState> = playlistManager.playlistState
    val subtitleAppearance: StateFlow<SubtitleAppearanceState> = subtitleController.subtitleAppearance
    val preferredSubtitleLang: StateFlow<String> = subtitleController.preferredSubtitleLang

    val subtitleSize = userPreferencesRepository.subtitleSize
    val subtitlePosition = userPreferencesRepository.subtitlePosition
    val resumePlayback = userPreferencesRepository.resumePlayback
    val decodeModePreference = userPreferencesRepository.decodeMode

    init {
        controller.dispatcher.addListener(this)
        controller.init()
        controller.surface.onSurfaceReady = {
            onSurfaceReady()
        }
    }

    // ---------------------------------------------------------------------------
    // Playback controls
    // ---------------------------------------------------------------------------

    fun play() = controller.executor.play()
    fun pause() = controller.executor.pause()
    fun togglePlay() = controller.executor.togglePlay()
    
    fun seekTo(positionMs: Long) {
        lastSeekTime = System.currentTimeMillis()
        controller.executor.seek(positionMs / 1000.0)
    }
    
    fun seekRelative(offsetMs: Long) {
        lastSeekTime = System.currentTimeMillis()
        controller.executor.seekRelative(offsetMs / 1000.0)
    }
    
    fun setSpeed(speed: Float) = controller.executor.setSpeed(speed.toDouble())
    fun setVolume(volume: Int) = controller.executor.setVolume(volume)
    fun setAudioTrack(id: Int) {
        controller.executor.setAudioTrack(id)
        _playerState.update { it.copy(selectedAudioTrackId = id) }
    }
    fun setSubtitleTrack(id: Int) {
        controller.executor.setSubtitleTrack(id)
        _playerState.update { it.copy(selectedSubtitleTrackId = id) }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _playerState.update { it.copy(aspectRatio = mode) }
        when (mode) {
            AspectRatioMode.DEFAULT -> controller.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "container")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
            AspectRatioMode.FIT -> controller.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "container")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
            AspectRatioMode.CROP -> controller.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "container")
                MPVLib.setPropertyDouble("panscan", 1.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
            AspectRatioMode.STRETCH -> controller.executor.execute {
                MPVLib.setPropertyString("video-aspect-override", "no")
                MPVLib.setPropertyString("video-aspect-mode", "none")
                MPVLib.setPropertyDouble("panscan", 0.0)
                MPVLib.setPropertyString("video-unscaled", "no")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Load / URI
    // ---------------------------------------------------------------------------

    fun loadAndPlay(uri: Uri) {
        _playerState.update { it.copy(isLoading = true, error = null) }
        playlistManager.loadAndPlay(uri)
    }

    fun onSurfaceReady() {
        playlistManager.onSurfaceReady()
    }

    /** Pauses playback immediately — used by screen-off receiver. */
    fun pausePlayback() {
        controller.executor.pause()
        _playerState.update { it.copy(isPlaying = false) }
    }

    // ---------------------------------------------------------------------------
    // Playlist
    // ---------------------------------------------------------------------------

    fun setPlaylist(uris: List<Uri>) = playlistManager.setPlaylist(uris)
    fun addToPlaylist(uri: Uri) = playlistManager.addToPlaylist(uri)
    fun playNext() = playlistManager.playNext()
    fun playPrevious() = playlistManager.playPrevious()
    fun playAt(index: Int) = playlistManager.playAt(index)

    // ---------------------------------------------------------------------------
    // Decode mode
    // ---------------------------------------------------------------------------

    fun setDecodeMode(mode: DecodeMode) {
        controller.executor.setHwdec(mode.mpvValue)
        _playerState.update { it.copy(decodeMode = mode) }
    }

    // ---------------------------------------------------------------------------
    // Zoom / Pan
    // ---------------------------------------------------------------------------

    fun setVideoZoom(zoom: Float) {
        controller.executor.setVideoZoom(zoom)
        _playerState.update { it.copy(videoZoom = zoom) }
    }

    fun setVideoPan(panX: Float, panY: Float) {
        controller.executor.setVideoPan(panX, panY)
        _playerState.update { it.copy(videoPanX = panX, videoPanY = panY) }
    }

    // ---------------------------------------------------------------------------
    // Subtitle appearance
    // ---------------------------------------------------------------------------

    fun setSubtitleFontSize(size: Int) = subtitleController.setSubtitleFontSize(size)
    fun setSubtitleFontColor(color: String) = subtitleController.setSubtitleFontColor(color)
    fun setSubtitleBold(bold: Boolean) = subtitleController.setSubtitleBold(bold)
    fun setSubtitleBorderStyle(style: String) = subtitleController.setSubtitleBorderStyle(style)
    fun setSubtitleBorderSize(size: Float) = subtitleController.setSubtitleBorderSize(size)
    fun setSubtitleShadow(shadow: Float) = subtitleController.setSubtitleShadow(shadow)
    fun setSubtitleBackgroundAlpha(alpha: Float) = subtitleController.setSubtitleBackgroundAlpha(alpha)
    fun setSubtitleAppearance(size: Float, position: Float) = subtitleController.setSubtitleAppearance(size, position)
    fun resetSubtitleAppearance() = subtitleController.resetSubtitleAppearance()

    // ---------------------------------------------------------------------------
    // Resume position
    // ---------------------------------------------------------------------------

    fun saveCurrentPosition(filePath: String, positionMs: Long) = resumePositionManager.saveCurrentPosition(filePath, positionMs)
    fun loadResumePosition(filePath: String, onResult: (Long?) -> Unit) = resumePositionManager.loadResumePosition(filePath, onResult)
    fun clearResumePosition(filePath: String) = resumePositionManager.clearResumePosition(filePath)

    // ---------------------------------------------------------------------------
    // Auto-subtitle selection
    // ---------------------------------------------------------------------------

    fun autoSelectSubtitle(tracks: List<SubtitleTrack>) = subtitleController.autoSelectSubtitle(tracks)
    fun setPreferredSubtitleLanguage(lang: String) = subtitleController.setPreferredSubtitleLanguage(lang)
    fun setSubtitleSize(size: Float) = subtitleController.setSubtitleSize(size)
    fun setSubtitlePosition(position: Float) = subtitleController.setSubtitlePosition(position)

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setResumePlayback(enabled)
        }
    }

    fun setDecodeModeStringPreference(mpvValue: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDecodeMode(mpvValue)
        }
    }

    // ---------------------------------------------------------------------------
    // MpvEventListener
    // ---------------------------------------------------------------------------

    override fun onFileLoaded() {
        _playerState.update { it.copy(isLoading = false, error = null) }
    }

    override fun onPlaybackStarted() {
        _playerState.update { it.copy(isPlaying = true, isLoading = false) }
    }

    override fun onPlaybackStopped(endReason: Int) {
        _playerState.update { it.copy(isPlaying = false) }
        // endReason 0 = natural EOF. Advance playlist only on clean end
        // AND only when the surface is still alive (not a VO teardown on lock).
        if (endReason == 0 && controller.surface.hasSurface()) {
            playlistManager.onPlaybackEnded()
        }
    }

    override fun onPropertyChange(name: String, value: Any?) {
        when (name) {
            MpvConstants.PROP_PAUSE -> {
                val paused = value as? Boolean ?: return
                _playerState.update { it.copy(isPlaying = !paused) }
            }
            MpvConstants.PROP_TIME_POS -> {
                val seconds = value as? Double ?: return
                val newPosMs = (seconds * 1000).toLong()
                val now = System.currentTimeMillis()
                
                // Throttle updates to 250ms during normal playback to save battery.
                // Allow fast updates during seeking/scrubbing (if a seek occurred in the last 500ms).
                if (now - lastSeekTime < 500 || now - lastTimePosUpdate >= 250) {
                    _playerState.update { it.copy(currentPositionMs = newPosMs) }
                    lastTimePosUpdate = now
                }
            }
            MpvConstants.PROP_DURATION -> {
                val seconds = value as? Double ?: return
                _playerState.update { it.copy(durationMs = (seconds * 1000).toLong()) }
            }
            MpvConstants.PROP_DEMUXER_CACHE_TIME -> {
                val seconds = value as? Double ?: return
                _playerState.update { it.copy(demuxerCacheTimeMs = (seconds * 1000).toLong()) }
            }
            MpvConstants.PROP_TRACK_LIST -> {
                val node = value as? MPVNode ?: return
                val audioTracks = TrackListParser.parseAudioTracks(node)
                val subtitleTracks = TrackListParser.parseSubtitleTracks(node)
                _playerState.update { it.copy(audioTracks = audioTracks, subtitleTracks = subtitleTracks) }
            }
            MpvConstants.PROP_AUDIO_ID -> {
                val id = (value as? Long)?.toInt() ?: -1
                _playerState.update { it.copy(selectedAudioTrackId = id) }
            }
            MpvConstants.PROP_SUBTITLE_ID -> {
                val id = (value as? Long)?.toInt() ?: -1
                _playerState.update { it.copy(selectedSubtitleTrackId = id) }
            }
            MpvConstants.PROP_SPEED -> {
                val speed = value as? Double ?: return
                _playerState.update { it.copy(speed = speed.toFloat()) }
            }
            MpvConstants.PROP_VOLUME -> {
                val volume = value as? Double ?: return
                _playerState.update { it.copy(volume = volume.toInt()) }
            }
        }
    }

    override fun onError(message: String) {
        _playerState.update { it.copy(error = message, isLoading = false) }
    }

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onCleared() {
        controller.dispatcher.removeListener(this)
        controller.destroy()
        super.onCleared()
    }
}
