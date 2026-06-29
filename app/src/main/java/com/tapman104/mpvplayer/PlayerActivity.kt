package com.tapman104.mpvplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tapman104.mpvplayer.player.playback.PlayerScreen
import com.tapman104.mpvplayer.settings.SettingsScreen
import com.tapman104.mpvplayer.ui.theme.MpvPlayerTheme
import com.tapman104.mpvplayer.player.viewmodel.PlayerViewModel
import com.tapman104.mpvplayer.player.viewmodel.PlayerViewModelFactory
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository

class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModelFactory(this)
    }

    private lateinit var surfaceView: SurfaceView

    /**
     * Pauses playback when the screen turns off (power button).
     * Using a BroadcastReceiver rather than onPause so that dialogs,
     * notifications, and picture-in-picture transitions do NOT pause playback.
     */
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                viewModel.pausePlayback()
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.loadAndPlay(it)
        }
    }

    /** The URI string of the currently loaded file — used as the resume key. */
    private var currentFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during playback (power button still fires ACTION_SCREEN_OFF)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Immersive sticky — hide system bars, show transiently on swipe
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        // Register screen-off receiver to pause audio on lock
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        // Create the SurfaceView that mpv will render into
        surfaceView = SurfaceView(this)

        // Register MpvSurface as the holder callback BEFORE setContent
        surfaceView.holder.addCallback(viewModel.controller.surface)

        setContent {
            MpvPlayerTheme {
                val playerState by viewModel.playerState.collectAsStateWithLifecycle()
                val playlistState by viewModel.playlistState.collectAsStateWithLifecycle()
                val preferredSubtitleLang by viewModel.preferredSubtitleLang.collectAsStateWithLifecycle()

                val resumePlaybackPref by viewModel.resumePlayback.collectAsStateWithLifecycle(
                    initialValue = UserPreferencesRepository.DEFAULT_RESUME_PLAYBACK
                )

                var pendingResumeMs by remember { mutableStateOf(0L) }
                var preOverrideSpeed by remember { mutableFloatStateOf(1f) }
                var showSettings by remember { mutableStateOf(false) }

                // Save position whenever playback pauses (isPlaying flips to false)
                val isPlaying = playerState.isPlaying
                LaunchedEffect(isPlaying) {
                    if (!isPlaying) {
                        val path = currentFilePath ?: return@LaunchedEffect
                        val posMs = playerState.currentPositionMs
                        viewModel.saveCurrentPosition(path, posMs)
                    }
                }

                // Detect new file loads via playlist change, load resume position once per file.
                LaunchedEffect(playlistState.currentUri) {
                    val uriStr = playlistState.currentUri ?: return@LaunchedEffect
                    currentFilePath = uriStr
                    viewModel.loadResumePosition(uriStr) { savedMs ->
                        if (savedMs != null && savedMs > 5000L && resumePlaybackPref) {
                            pendingResumeMs = savedMs
                        } else {
                            pendingResumeMs = 0L
                        }
                    }
                }

                LaunchedEffect(playerState.isLoading, pendingResumeMs) {
                    if (!playerState.isLoading && pendingResumeMs > 0L) {
                        viewModel.seekTo(pendingResumeMs)
                        pendingResumeMs = 0L
                    }
                }

                PlayerScreen(
                    fileName = playlistState.currentUri
                        ?.substringAfterLast('/')
                        ?.substringBeforeLast('.')
                        ?: "Unknown",
                    playerState = playerState,
                    surfaceView = surfaceView,
                    onTogglePlay = { viewModel.togglePlay() },
                    onSeek = { viewModel.seekTo(it) },
                    onOpenFile = { filePickerLauncher.launch(arrayOf("video/*")) },
                    onSelectAudioTrack = { /* wired in later prompt */ },
                    onSelectSubtitleTrack = { /* wired in later prompt */ },
                    onCycleDecodeMode = { /* wired in later prompt */ },
                    onMoreOptions = { showSettings = true },
                )

                if (showSettings) {
                    val subtitleSize by viewModel.subtitleSize.collectAsStateWithLifecycle(
                        initialValue = UserPreferencesRepository.DEFAULT_SUBTITLE_SIZE
                    )
                    val subtitlePosition by viewModel.subtitlePosition.collectAsStateWithLifecycle(
                        initialValue = UserPreferencesRepository.DEFAULT_SUBTITLE_POSITION
                    )
                    val resumePlayback by viewModel.resumePlayback.collectAsStateWithLifecycle(
                        initialValue = UserPreferencesRepository.DEFAULT_RESUME_PLAYBACK
                    )
                    val decodeMode by viewModel.decodeModePreference.collectAsStateWithLifecycle(
                        initialValue = UserPreferencesRepository.DEFAULT_DECODE_MODE
                    )
                    SettingsScreen(
                        preferredSubtitleLang = preferredSubtitleLang,
                        onSubtitleLangChange = { viewModel.setPreferredSubtitleLanguage(it) },
                        subtitleSize = subtitleSize,
                        subtitlePosition = subtitlePosition,
                        onSubtitleSizeChange = { viewModel.setSubtitleSize(it) },
                        onSubtitlePositionChange = { viewModel.setSubtitlePosition(it) },
                        resumePlayback = resumePlayback,
                        onResumePlaybackChange = { viewModel.setResumePlayback(it) },
                        decodeMode = decodeMode,
                        onDecodeModeChange = { viewModel.setDecodeModeStringPreference(it) },
                        onBack = { showSettings = false }
                    )
                }
            }
        }

        // Handle direct launch from a file manager or intent
        intent.data?.let { viewModel.loadAndPlay(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { viewModel.loadAndPlay(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenOffReceiver)
    }
}
