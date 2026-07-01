# Potato Player MPV — Comprehensive Codebase Flow & System Architecture Report

This report provides an in-depth, architectural breakdown of the **Potato Player MPV** codebase. It illustrates how every component interacts, who commands whom, how execution flows through the system, and identifies the exact leader files, their roles, and their key functions.

---

## 1. Executive Summary: The Leaders of the Codebase

In a modern reactive Android application built with **Jetpack Compose**, **MVVM**, and a native **JNI JNI engine (`libmpv`)**, clear separation of concerns is vital. The system is governed by four distinct **Leader Components**, each ruling its own domain:

```
+----------------------------------------------------------------------------------------------------+
|                                      THE CODEBASE LEADERS                                          |
+----------------------------------------------------------------------------------------------------+
|                                                                                                    |
|  1. STATE & BUSINESS ORCHESTRATION LEADER                                                          |
|     File: [PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt)                                             |
|     Role: The supreme brain of the player. Commands playback, manages UI state (`PlayerState`),    |
|           coordinates playlists, subtitles, and saves/restores watch progress.                     |
|                                                                                                    |
|  2. NATIVE MEDIA ENGINE LEADER                                                                     |
|     File: [MpvController.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt)                                               |
|     Role: The bridge between Kotlin and native C/C++ `libmpv`. Commands surface binding,           |
|           executes engine operations, and dispatches native events back to the Kotlin realm.       |
|                                                                                                    |
|  3. TOUCH & GESTURE LEADER                                                                         |
|     File: [GestureHandler.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt)                                              |
|     Role: The input governor. Captures multi-touch gestures, drives the state machine, modifies    |
|           system volume/brightness, and commands seek/zoom visual indicators.                      |
|                                                                                                    |
|  4. UI PRESENTATION LEADER                                                                         |
|     File: [PlayerScreen.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt) & [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt)                         |
|     Role: The Compose visual root. Renders the video output surface, overlays interactive controls,|
|           and routes user clicks and drags to the ViewModel and Gesture Handler.                   |
+----------------------------------------------------------------------------------------------------+
```

---

## 2. System Architecture & Layered Flow Map

The codebase is organized into five distinct layers. Data flows **downward** as command invocations and **upward** as reactive state updates and asynchronous event notifications.

```
======================================================================================================
                                     LAYER 1: APPLICATION ENTRY
======================================================================================================
  [MainActivity.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/MainActivity.kt)   ----(Launches Intent with Media URI)---->   [PlayerActivity.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/PlayerActivity.kt)
                                                                             |
                                                            (Creates ViewModel & SurfaceView)
                                                                             |
                                                                             v
======================================================================================================
                                  LAYER 2: UI & PRESENTATION LAYER (Compose)
======================================================================================================
                                                 [PlayerScreen.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt)
                                                        |
                 +--------------------------------------+--------------------------------------+
                 |                                      |                                      |
                 v                                      v                                      v
         [PlayerVideo.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerVideo.kt)                         [PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt)                        [GestureHandler.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt)
    (Wraps Android SurfaceView)                         |                               (Touch State Machine)
                 |                     +----------------+----------------+                     |
                 |                     |                |                |                     v
                 |                     v                v                v            [Gesture Indicators]
                 |            [PlayerTopBar]   [BottomControls]  [QuickActions]  (Volume, Seek, Brightness)
                 |                     |                |                |                     |
                 |                     +----------------+----------------+                     |
                 |                                      |                                      |
                 |                              (User Commands)                 (System Vol/Brightness)
                 |                                      |                                      |
                 v                                      v                                      v
======================================================================================================
                              LAYER 3: STATE & BUSINESS LOGIC LAYER (ViewModel)
======================================================================================================
                                              [PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt) <-----------------------------+
                                                        |
                 +--------------------------------------+--------------------------------------+
                 |                                      |                                      |
                 v                                      v                                      v
       [PlaylistManager.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlaylistManager.kt)                     [SubtitleController.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/SubtitleController.kt)             [ResumePositionManager.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/ResumePositionManager.kt)
  (Manages Queue & Next/Prev)            (Manages Subtitle Tracks & Styles)    (Saves/Restores Progress)
                 |                                      |                                      |
                 +--------------------------------------+--------------------------------------+
                                                        |
                                            (High-Level Engine Calls)
                                                        |
                                                        v
======================================================================================================
                                   LAYER 4: NATIVE ENGINE FACADE (MPV)
======================================================================================================
                                               [MpvController.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt)
                                                        |
                 +--------------------------------------+--------------------------------------+
                 |                                      |                                      |
                 v                                      v                                      v
      [MpvCommandExecutor.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvCommandExecutor.kt)                  [MpvSurface.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvSurface.kt)                       [MpvEventDispatcher.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvEventDispatcher.kt)
    (Sends C-strings to libmpv)           (Binds Android Surface to VO)          (Receives Native Events)
                 |                                      |                                      ^
                 |                                      |                                      |
                 v                                      v                              (Property Changes)
         +---------------------------------------------------------------+                     |
         |                         libmpv (Native JNI)                   |                     |
         +---------------------------------------------------------------+                     |
                                                                                               |
                                                                                     [MpvPropertyObserver]
                                                                                               |
======================================================================================================
                                  LAYER 5: STORAGE & PERSISTENCE LAYER
======================================================================================================
                 [AppDatabase.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/database/AppDatabase.kt)                                     [UserPreferencesRepository.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/preferences/UserPreferencesRepository.kt)
                 [ResumePositionDao.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/database/ResumePositionDao.kt)                               (Android DataStore / SharedPreferences)
              (Room SQLite Database for Playback Progress)                        (Subtitle Styling, Decoder Modes)
======================================================================================================
```

---

## 3. Detailed Chained Execution Pipelines

To understand how files work with each other, we trace the exact **chain of execution** across four primary runtime scenarios.

### Pipeline A: Media Initialization & Playback Start Flow
When a user selects a video file or launches the player from an intent, the following chain reaction occurs:

```
[PlayerActivity.onCreate]
       │
       ├─► 1. Creates `SurfaceView` and instantiates `PlayerViewModel` (via Factory).
       │
       ▼
[PlayerViewModel.init]
       │
       ├─► 2. Registers itself as `MpvEventListener` on `controller.dispatcher`.
       ├─► 3. Calls `controller.init()`.
       │
       ▼
[MpvController.init]
       │
       ├─► 4. Copies `Roboto-Regular.ttf` font asset to internal app storage for OSD subtitles.
       ├─► 5. Invokes native JNI `MPVLib.create()`, sets config options (`vo=gpu`, `hwdec=mediacodec-copy`).
       ├─► 6. Calls `MPVLib.init()` and registers `MpvPropertyObserver`.
       │
       ▼
[PlayerActivity.onStart]
       │
       ├─► 7. Passes `SurfaceView` into `PlayerScreen` -> `PlayerVideo`.
       ├─► 8. Calls `viewModel.controller.surface.attachSurface(surfaceView.holder.surface)`.
       │
       ▼
[MpvSurface.attachSurface]
       │
       ├─► 9. Commands `MpvCommandExecutor.attachSurface(surface)` -> `MPVLib.attachSurface()`.
       ├─► 10. Triggers callback `onSurfaceReady()`.
       │
       ▼
[PlayerViewModel.onSurfaceReady]
       │
       └─► 11. Calls `playlistManager.loadAndPlay(uri)` -> `controller.executor.loadFile(path)`.
               Video playback begins natively on the bound GPU surface!
```

---

### Pipeline B: User Playback Control & State Synchronization Loop
When a user interacts with on-screen controls (e.g., clicking **Play/Pause** or scrubbing the seekbar in [PlayerBottomControls.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerBottomControls.kt)):

```
[User Taps Play/Pause Button in PlayerBottomControls]
       │
       ├─► 1. Lambda `onTogglePlay()` invoked, routed up through `PlayerOverlay` -> `PlayerScreen`.
       │
       ▼
[PlayerActivity] (Observing UI events)
       │
       ├─► 2. Calls `viewModel.togglePlay()`.
       │
       ▼
[PlayerViewModel.togglePlay]
       │
       ├─► 3. Delegates command to `controller.executor.togglePlay()`.
       │
       ▼
[MpvCommandExecutor.togglePlay]
       │
       ├─► 4. Executes native command: `MPVLib.command(arrayOf("cycle", "pause"))`.
       │
       ▼
[Native libmpv Engine]
       │
       ├─► 5. Engine toggles pause state and fires property change event for `"pause"`.
       │
       ▼
[MpvPropertyObserver / MpvEventDispatcher]
       │
       ├─► 6. Receives JNI callback, identifies `"pause"` property change, notifies listeners.
       │
       ▼
[PlayerViewModel.onPropertyChange(property = "pause", value = true/false)]
       │
       ├─► 7. Updates immutable state: `_playerState.update { it.copy(isPlaying = !value) }`.
       │
       ▼
[Jetpack Compose UI (`PlayerScreen`)]
       │
       └─► 8. Collects new `playerState` via `collectAsStateWithLifecycle()`, recomposes icon from Pause to Play!
```

---

### Pipeline C: Multi-Touch Gesture Interaction Pipeline
When a user swipes or pinches on the screen during playback, input is intercepted before reaching visual buttons:

```
[User Swipes Vertically on Right Side of Screen]
       │
       ├─► 1. `AndroidView` inside `PlayerScreen` returns `false` on touch, letting `PlayerOverlay` receive events.
       │
       ▼
[GestureOverlay.kt / GestureHandler.kt]
       │
       ├─► 2. Captures raw `PointerInputChange` events via `pointerInput(Unit)`.
       ├─► 3. Feeds coordinates into `GestureStateMachine` and `GestureCoordinator`.
       │
       ▼
[GestureCoordinator.onSwipe]
       │
       ├─► 4. Determines swipe zone (Right = Volume, Left = Brightness, Horizontal = Seek).
       ├─► 5. Calculates percentage delta and updates local transient state.
       │
       ▼
[GestureHandler Actions]
       │
       ├─► 6. For Volume: Calls Android `AudioManager.setStreamVolume()`.
       ├─► 7. For Brightness: Invokes `onBrightnessChange(val)`, updating `Activity.window.attributes.screenBrightness`.
       ├─► 8. For Seeking: Triggers `onSeekPreview(timeMs, delta)` -> displays `HorizontalSeekIndicator`.
       │
       ▼
[Visual Feedback Components]
       │
       └─► 9. `VolumeIndicator`, `BrightnessIndicator`, or `PinchZoomIndicator` animate on screen with real-time values!
```

---

### Pipeline D: Subtitle & Audio Track Management Flow
When a user switches subtitle tracks or customizes subtitle appearance:

```
[User Taps Subtitle Icon in PlayerTopBar]
       │
       ├─► 1. Sets `showSubtitleDialog = true` in `PlayerOverlay`, rendering `SubtitleTrackDialog`.
       │
       ▼
[SubtitleTrackDialog]
       │
       ├─► 2. User selects track ID -> invokes callback `onSubtitleTrackSelected(trackId)`.
       │
       ▼
[PlayerViewModel.setSubtitleTrack(id)]
       │
       ├─► 3. Calls `subtitleController.selectTrack(id)`.
       │
       ▼
[SubtitleController.selectTrack]
       │
       ├─► 4. Commands `executor.setSubtitleTrack(id)` -> sends `"sid"` property to libmpv.
       ├─► 5. Saves user's language preference to `UserPreferencesRepository` (DataStore).
       ├─► 6. When styling is updated via `SubtitleAppearanceDialog`, updates `sub-color`, `sub-scale`, `sub-pos` on the fly!
```

---

## 4. Complete Component Catalog & Chain of Responsibility Matrix

The following matrix documents every critical file in the project, defining who controls it, whom it commands, and its core functions.

| File & Link | Role & Responsibility | Primary Functions / Methods | Controlled By (Leader) | Commands / Dependencies (Subordinates) |
| :--- | :--- | :--- | :--- | :--- |
| **[PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt)** | **Overall State Leader.** Central ViewModel governing player UI state, media operations, and lifecycle. | `play()`, `pause()`, `seekTo()`, `onSurfaceReady()`, `onPropertyChange()`, `loadAndPlay()` | `PlayerActivity` | `MpvController`, `PlaylistManager`, `SubtitleController`, `ResumePositionManager` |
| **[MpvController.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt)** | **Engine Facade Leader.** Initializes libmpv, sets GPU context, fonts, and connects observers. | `init()`, `destroy()`, `copyFontAsset()` | `PlayerViewModel` | `MpvCommandExecutor`, `MpvEventDispatcher`, `MpvSurface`, `MPVLib` (JNI) |
| **[MpvCommandExecutor.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvCommandExecutor.kt)** | **Engine Command Translator.** Safe execution of string-based commands and property setters to native libmpv. | `loadFile()`, `seek()`, `setSpeed()`, `setAudioTrack()`, `setSubtitleTrack()`, `setPropertyString()` | `MpvController`, `PlayerViewModel`, Sub-controllers | `MPVLib.command()`, `MPVLib.setProperty()` |
| **[MpvEventDispatcher.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvEventDispatcher.kt)** | **Native Event Router.** Receives raw JNI event signals from libmpv and broadcasts them to Kotlin listeners. | `eventProperty()`, `event()`, `addListener()`, `removeListener()` | `MpvController`, `MPVLib` (Native Callback) | `MpvEventListener` implementations (`PlayerViewModel`) |
| **[MpvSurface.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvSurface.kt)** | **Video Render Target.** Manages binding and unbinding of Android `Surface` to mpv's GPU video output (`vo`). | `attachSurface()`, `detachSurface()`, `hasSurface()`, `setVo()` | `MpvController`, `PlayerActivity` | `MpvCommandExecutor.attachSurface()` |
| **[PlaylistManager.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlaylistManager.kt)** | **Queue Orchestrator.** Manages playlist items, current index, file loading, and next/previous track switching. | `loadAndPlay()`, `playNext()`, `playPrevious()`, `addToPlaylist()` | `PlayerViewModel` | `MpvCommandExecutor.loadFile()` |
| **[SubtitleController.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/SubtitleController.kt)** | **Subtitle Governor.** Handles subtitle track selection, external file loading, and ASS/OSD styling properties. | `selectTrack()`, `addSubtitleFile()`, `setSubtitleSize()`, `setSubtitlePosition()` | `PlayerViewModel` | `MpvCommandExecutor`, `UserPreferencesRepository` |
| **[ResumePositionManager.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/ResumePositionManager.kt)** | **Progress Guardian.** Automatically saves playback timestamp on pause/stop and restores position on file reopen. | `savePosition()`, `getResumePosition()`, `clearPosition()` | `PlayerViewModel` | `ResumePositionDao` (Room Database) |
| **[PlayerScreen.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerScreen.kt)** | **UI Root Leader.** Root Compose container for playback. Stacks video surface, gestures, and control overlays. | `PlayerScreen(...)` | `PlayerActivity` | `PlayerVideo`, `PlayerOverlay` |
| **[PlayerOverlay.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/playback/PlayerOverlay.kt)** | **Overlay Coordinator.** Stacks top bar, bottom controls, gestures, dialogs, and manages auto-hide timers. | `PlayerOverlay(...)` | `PlayerScreen` | `GestureHandler`, `PlayerTopBar`, `PlayerBottomControls`, `PlayerQuickActions`, Dialogs |
| **[GestureHandler.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt)** | **Touch Input Leader.** Captures multi-touch events, coordinates seek/volume/brightness scrubbing and zooming. | `GestureHandler(...)` | `PlayerOverlay` | `GestureCoordinator`, `GestureStateMachine`, `VolumeIndicator`, `BrightnessIndicator`, etc. |
| **[PlayerBottomControls.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerBottomControls.kt)** | **Primary Control UI.** Renders Play/Pause, seekbar, time text, next/prev, and track selection triggers. | `PlayerBottomControls(...)` | `PlayerOverlay` | Lambda callbacks -> `PlayerViewModel` |
| **[PlayerQuickActions.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerQuickActions.kt)** | **Auxiliary Control UI.** Renders aspect ratio toggle, decode mode switcher (HW/HW+/SW), and control lock button. | `PlayerQuickActions(...)` | `PlayerOverlay` | Lambda callbacks -> `PlayerViewModel` |
| **[PlayerTopBar.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/controls/PlayerTopBar.kt)** | **Top Header UI.** Renders back arrow, current filename, audio/subtitle selectors, and settings overflow menu. | `PlayerTopBar(...)` | `PlayerOverlay` | Lambda callbacks -> `PlayerActivity` / Dialog triggers |
| **[AppDatabase.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/database/AppDatabase.kt)** | **Database Leader.** Room SQLite database definition and DAO provider for local persistence. | `resumePositionDao()` | `PlayerViewModelFactory` | `ResumePositionDao`, `ResumePositionEntity` |
| **[UserPreferencesRepository.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/preferences/UserPreferencesRepository.kt)** | **Preferences Leader.** DataStore repository managing persistent user choices (decode mode, subtitle styling). | `updateSubtitleSize()`, `updateDecodeMode()`, flows for preferences | `PlayerViewModel`, Sub-controllers | Android DataStore / SharedPreferences |
| **[TrackListParser.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/TrackListParser.kt)** | **Metadata Translator.** Parses raw mpv track property strings into structured Kotlin `AudioTrack` and `SubtitleTrack` lists. | `parseTrackList()` | `PlayerViewModel`, `SubtitleController` | `AudioTrack`, `SubtitleTrack` models |

---

## 5. Leader & Subsystem Deep-Dive

### A. The Brain: `PlayerViewModel` & Its Triad of Managers
Why is [PlayerViewModel.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/viewmodel/PlayerViewModel.kt) not just a single monolithic file? Because video playback involves three distinct, complex sub-domains:
1. **Playlist Management (`PlaylistManager`)**: Handles queueing multiple videos, resolving file paths, and managing end-of-file (EOF) transitions. When mpv reports `eof-reached`, `PlayerViewModel` notifies `PlaylistManager`, which automatically loads the next URI.
2. **Subtitle & Styling (`SubtitleController`)**: mpv requires specific property formatting for ASS (Advanced SubStation Alpha) overrides. This controller insulates the ViewModel from string-parsing logic, managing subtitle delays, external file sideloading (`sub-add`), and font scaling.
3. **Resume Progress (`ResumePositionManager`)**: Intercepts `time-pos` updates from mpv. To prevent flooding SQLite with writes on every video frame, it debounces time updates and commits the current playback timestamp to `ResumePositionDao` when playback pauses, stops, or changes files.

### B. The Engine Gateway: `MpvController` & The Native Triad
[MpvController.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/core/engine/MpvController.kt) encapsulates the C-JNI boundary into three clean helpers:
* **`MpvCommandExecutor`**: Prevents concurrent modification exceptions and JNI threading crashes by dispatching all commands (`seek`, `pause`, `loadfile`) through an internal executor queue.
* **`MpvEventDispatcher`**: Implements the Observer pattern over native JNI callbacks. When C++ `libmpv` fires an event (e.g., `MPV_EVENT_PROPERTY_CHANGE`), the dispatcher converts raw C-pointers into safe Kotlin strings and types (`Boolean`, `Long`, `Float`), notifying `PlayerViewModel`.
* **`MpvSurface`**: Manages the Android `SurfaceView` lifecycle. If the user puts the app in background or picture-in-picture mode, `MpvSurface` safely detaches the surface from mpv (`vo=null`) to prevent GPU crashes and re-attaches it when foregrounded.

### C. The Input Engine: `GestureHandler` & Touch State Machine
Instead of cluttering the UI code with complex math, [GestureHandler.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureHandler.kt) uses a dedicated **State Machine** ([GestureStateMachine.kt](file:///c:/Users/tapman/Desktop/mpv%20modern/mpvplayer/app/src/main/java/com/tapman104/mpvplayer/player/gesture/GestureStateMachine.kt)) to classify finger movements:
* **Idle State**: Waiting for initial touch threshold.
* **Volume / Brightness State**: Activated on vertical drag delta $> 10\text{px}$ on left/right screen edges. Computes percentage changes and emits immediate audio/window brightness updates.
* **Horizontal Seek State**: Activated on horizontal drag. Calculates time offset based on video duration and shows a scrubbing preview without committing the seek until finger release.
* **Pinch-to-Zoom State**: Activated when multi-touch centroid expands or contracts, dynamically scaling video pan/zoom properties in `libmpv`.

---

## 6. Summary of Architecture Harmony

The codebase achieves robust performance and maintainability through this clean chain of responsibility:
* **UI Components** never call mpv JNI functions directly; they only emit user intents to `PlayerViewModel`.
* **ViewModel** never manages Android Views or drawing canvases; it only updates `StateFlow<PlayerState>`.
* **Engine Components** never know about UI layouts or databases; they only execute commands and emit raw playback events.

This structured chaining ensures that **Potato Player MPV** remains responsive, modular, and highly extensible.
