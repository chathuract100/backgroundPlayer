# backgroundPlayer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app that plays local audio/video in the background with lock-screen controls and a floating Picture-in-Picture video window.

**Architecture:** Single-activity Compose app. A Media3 `MediaSessionService` (foreground service) owns the `ExoPlayer`; the UI connects through a `MediaController` and talks to playback only through a `PlaybackController` interface (so ViewModels are testable with a fake). Library and playlists persist in Room. Media is stored by URI reference, never copied. Dependencies are wired by a small manual app container (no Hilt).

**Tech Stack:** Kotlin, Jetpack Compose, Media3 (ExoPlayer + MediaSession + MediaSessionService), Room, MediaStore + Storage Access Framework, Coil, Android PiP API. Tests: JUnit4, Robolectric (Room + Android-dependent unit tests), coroutines-test, Turbine.

**Spec:** `docs/superpowers/specs/2026-10-14-background-player-design.md`

## Global Constraints

- Platform: Android only. `minSdk 26`, `compileSdk 35`, `targetSdk 35`.
- Use the latest stable version of each library that satisfies these floors: Kotlin `2.0+`, Android Gradle Plugin `8.5+`, Compose BOM `2024.09+`, Media3 `1.4+`, Room `2.6+`, Coil `2.7+` (coil-compose), Robolectric `4.13+`, JUnit `4.13.2`, kotlinx-coroutines-test, Turbine `1.1+`.
- Media files are referenced by content `uri` with persisted read permission — **never copied** into app storage.
- No network, no accounts, no streaming. The app must function fully offline.
- Manifest permissions: `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO` (API 33+), `READ_EXTERNAL_STORAGE` (maxSdkVersion 32), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`.
- Dependency injection is a hand-written app container; do not add Hilt/Dagger.
- Out of scope (do not build): streaming, tag editing, folder auto-sync, ratings/favorites, iPhone support, cloud sync.

## Review Focus

Inputs the spec implies but that need explicit test coverage; each has its test added to the owning task:

1. **Corrupt/unplayable file** — playback skips to the next track instead of crashing (Task 5: `PlaybackErrorPolicy`).
2. **File moved/deleted or revoked URI permission** — the track is marked unavailable, not left as a dead entry (Task 3: repository `markUnavailable` / availability query).
3. **Empty library and empty playlist** — screens show an empty state and "play all" on an empty playlist is a no-op (Task 8 and Task 9 ViewModel tests).
4. **Duplicate import of the same file** — importing a URI already in the library does not create a second row (Task 3: dedupe-by-URI on insert).
5. **Headphone unplug / audio-focus loss** — playback auto-pauses rather than blasting the speaker (Task 6: ExoPlayer `handleAudioBecomingNoisy` + audio attributes; verified on device).

---

### Task 1: Project scaffold & build configuration

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `gradlew`/`gradlew.bat` + wrapper
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml` (permissions from Global Constraints; `MainActivity` as launcher)
- Create: `app/src/main/java/com/backgroundplayer/MainActivity.kt` (empty Compose scaffold)
- Test: `app/src/test/java/com/backgroundplayer/SmokeTest.kt`

**Interfaces:**
- Produces: a buildable `app` module with package `com.backgroundplayer`, Compose enabled, unit-test + Robolectric test source sets configured.

- [ ] **Step 1: Write the failing test** — `SmokeTest.kt` with `fun appPackageIsConfigured() { assertEquals("com.backgroundplayer", BuildConfig.APPLICATION_ID) }`
- [ ] **Step 2: Run `./gradlew test`** — Expected: FAIL (project does not build / BuildConfig missing)
- [ ] **Step 3: Create the Gradle files, version catalog, manifest, and empty `MainActivity`** per Files above. Enable `buildFeatures { compose = true; buildConfig = true }` and add the test dependencies from Global Constraints.
- [ ] **Step 4: Run `./gradlew assembleDebug` then `./gradlew test`** — Expected: both PASS
- [ ] **Step 5: Commit** — `chore: scaffold Android project and build config`

---

### Task 2: Room entities, database & DAOs

**Files:**
- Create: `app/src/main/java/com/backgroundplayer/data/db/Track.kt`, `Playlist.kt`, `PlaylistTrack.kt`
- Create: `.../data/db/TrackDao.kt`, `PlaylistDao.kt`, `AppDatabase.kt`
- Test: `app/src/test/java/com/backgroundplayer/data/db/TrackDaoTest.kt`, `PlaylistDaoTest.kt` (Robolectric, in-memory DB)

**Interfaces:**
- Produces:
  - `Track(id: Long, uri: String, title: String, artist: String?, album: String?, durationMs: Long, isVideo: Boolean, thumbnailUri: String?, dateAdded: Long, isAvailable: Boolean = true)` — `uri` has a unique index.
  - `Playlist(id: Long, name: String, createdAt: Long)`
  - `PlaylistTrack(playlistId: Long, trackId: Long, position: Int)` — composite PK.
  - `TrackDao`: `suspend insert(track): Long` (IGNORE on URI conflict, returns -1 when ignored), `observeAll(): Flow<List<Track>>`, `observeByType(isVideo): Flow<List<Track>>`, `suspend findByUri(uri): Track?`, `suspend setAvailability(id, Boolean)`.
  - `PlaylistDao`: `suspend create(name): Long`, `suspend rename(id, name)`, `suspend delete(id)`, `observePlaylists(): Flow<List<Playlist>>`, `suspend addTrack(PlaylistTrack)`, `observeTracks(playlistId): Flow<List<Track>>` (ordered by position), `suspend maxPosition(playlistId): Int?`, `suspend reorder(playlistId, trackId, newPosition)`, `suspend removeTrack(playlistId, trackId)`.

- [ ] **Step 1: Write failing tests** — `TrackDaoTest`: `insertThenObserveAllReturnsTrack`, `insertDuplicateUriIsIgnored`, `observeByTypeFiltersVideo`, `setAvailabilityUpdatesFlag`. `PlaylistDaoTest`: `addTracksObservedInPositionOrder`, `removeTrackDropsIt`, `renameChangesName`.
- [ ] **Step 2: Run `./gradlew test`** — Expected: FAIL (classes undefined)
- [ ] **Step 3: Implement the entities, DAOs, and `AppDatabase`** (abstract RoomDatabase exposing both DAOs; `@Index(value=["uri"], unique=true)` on Track).
- [ ] **Step 4: Run `./gradlew test`** — Expected: PASS
- [ ] **Step 5: Commit** — `feat: add Room entities, DAOs, and database`

---

### Task 3: LibraryRepository

**Files:**
- Create: `app/src/main/java/com/backgroundplayer/data/LibraryRepository.kt`
- Test: `app/src/test/java/com/backgroundplayer/data/LibraryRepositoryTest.kt` (Robolectric, in-memory DB)

**Interfaces:**
- Consumes: `TrackDao`, `PlaylistDao` (Task 2).
- Produces: `LibraryRepository(trackDao, playlistDao)` with:
  - `fun library(filter: MediaFilter, query: String): Flow<List<Track>>` where `enum class MediaFilter { ALL, AUDIO, VIDEO }`; `query` matches title or artist case-insensitively (blank = no filter).
  - `suspend fun addTrack(track: Track): Long` — delegates to dao insert; dedupes by URI (ignored insert returns existing row, no duplicate).
  - `suspend fun markUnavailable(trackId: Long)` and availability reflected in `library(...)`.
  - Playlist passthroughs: `createPlaylist(name)`, `rename`, `delete`, `playlists()`, `playlistTracks(id)`, `addToPlaylist(playlistId, trackId)` (appends at `maxPosition+1`), `reorder(playlistId, trackId, newPosition)`, `removeFromPlaylist(playlistId, trackId)`.

- [ ] **Step 1: Write failing tests** — `searchFiltersByTitleAndArtist`, `videoFilterExcludesAudio`, `addDuplicateUriDoesNotCreateSecondRow` (Review Focus #4), `markUnavailableHidesOrFlagsTrack` (Review Focus #2), `addToPlaylistAppendsAtEnd`, `reorderMovesTrack`.
- [ ] **Step 2: Run `./gradlew test`** — Expected: FAIL
- [ ] **Step 3: Implement `LibraryRepository`.** Compose the DAO flows; apply the query filter in Kotlin over the type-filtered flow.
- [ ] **Step 4: Run `./gradlew test`** — Expected: PASS
- [ ] **Step 5: Commit** — `feat: add LibraryRepository with search, dedupe, and playlist ops`

---

### Task 4: Media importer (MediaStore scan + SAF import)

**Files:**
- Create: `app/src/main/java/com/backgroundplayer/data/import/MediaMetadataReader.kt` (interface + `AndroidMediaMetadataReader` using `MediaMetadataRetriever`)
- Create: `.../data/import/TrackMapper.kt` (pure mapping: metadata → `Track`)
- Create: `.../data/import/MediaStoreScanner.kt`, `SafImporter.kt` (persist URI permission, read metadata, call `repository.addTrack`)
- Test: `app/src/test/java/com/backgroundplayer/data/import/TrackMapperTest.kt`

**Interfaces:**
- Consumes: `LibraryRepository.addTrack` (Task 3).
- Produces:
  - `data class RawMedia(uri: String, mimeType: String?, displayName: String, title: String?, artist: String?, album: String?, durationMs: Long?, thumbnailUri: String?)`
  - `interface MediaMetadataReader { suspend fun read(uri: Uri): RawMedia }`
  - `object TrackMapper { fun toTrack(raw: RawMedia): Track }` — rules: `isVideo = mimeType?.startsWith("video/") == true`; `title` falls back to `displayName` when null/blank; missing `durationMs` → 0.
  - `MediaStoreScanner.scan()` and `SafImporter.import(uris)` returning the count added.

- [ ] **Step 1: Write failing tests** — `TrackMapperTest`: `videoMimeMarksIsVideo`, `missingTitleFallsBackToDisplayName`, `nullDurationBecomesZero`, `audioMimeIsNotVideo`.
- [ ] **Step 2: Run `./gradlew test`** — Expected: FAIL
- [ ] **Step 3: Implement `TrackMapper` (pure), the `MediaMetadataReader` interface + Android impl, and the scanner/importer** (importer calls `takePersistableUriPermission`).
- [ ] **Step 4: Run `./gradlew test`** — Expected: PASS
- [ ] **Step 5: Commit** — `feat: add media importer (MediaStore scan + SAF import)`

---

### Task 5: PlaybackController interface & ExoPlayer state mapping

**Files:**
- Create: `app/src/main/java/com/backgroundplayer/playback/PlaybackController.kt` (interface + `PlaybackState`, `RepeatMode`)
- Create: `.../playback/PlaybackErrorPolicy.kt` (pure)
- Create: `.../playback/TrackMediaItem.kt` (Track ↔ Media3 `MediaItem`)
- Create: `.../playback/ExoPlaybackController.kt` (wraps a Media3 `Player`)
- Test: `app/src/test/java/com/backgroundplayer/playback/PlaybackErrorPolicyTest.kt`, `TrackMediaItemTest.kt`, `ExoPlaybackControllerTest.kt` (Robolectric + Media3 test utils fake player)

**Interfaces:**
- Consumes: `Track` (Task 2).
- Produces:
  - `enum class RepeatMode { OFF, ONE, ALL }`
  - `data class PlaybackState(currentTrack: Track?, isPlaying: Boolean, positionMs: Long, durationMs: Long, isVideo: Boolean, shuffle: Boolean, repeat: RepeatMode)` with an `EMPTY` default.
  - `interface PlaybackController { val state: StateFlow<PlaybackState>; fun play(tracks: List<Track>, startIndex: Int); fun pause(); fun resume(); fun next(); fun previous(); fun seekTo(ms: Long); fun setShuffle(Boolean); fun setRepeat(RepeatMode); fun release() }`
  - `object PlaybackErrorPolicy { fun onError(hasNext: Boolean): ErrorAction }` → `SKIP_TO_NEXT` when `hasNext`, else `STOP`.
  - `object TrackMediaItem { fun from(track: Track): MediaItem; fun trackId(item: MediaItem): Long }` (store `Track.id` in `MediaItem.mediaId`).

- [ ] **Step 1: Write failing tests** — `PlaybackErrorPolicyTest`: `skipsWhenNextExists`, `stopsWhenNoNext` (Review Focus #1). `TrackMediaItemTest`: `roundTripsTrackIdAndUri`. `ExoPlaybackControllerTest`: `playEmitsCurrentTrackAndPlaying`, `setRepeatOneMapsToPlayerRepeatOne`, `setShuffleTogglesPlayerShuffle` (using a Media3 fake/`TestExoPlayerBuilder` player).
- [ ] **Step 2: Run `./gradlew test`** — Expected: FAIL
- [ ] **Step 3: Implement the interface, pure policy, mapper, and `ExoPlaybackController`** translating `Player.Listener` events into `PlaybackState`, and `RepeatMode`/shuffle into ExoPlayer modes.
- [ ] **Step 4: Run `./gradlew test`** — Expected: PASS
- [ ] **Step 5: Commit** — `feat: add PlaybackController abstraction over ExoPlayer`

---

### Task 6: PlaybackService (MediaSessionService, background + notification)

**Files:**
- Create: `app/src/main/java/com/backgroundplayer/playback/PlaybackService.kt`
- Modify: `app/src/main/AndroidManifest.xml` (register the service with `foregroundServiceType="mediaPlayback"` and the `MediaSessionService` intent filter)
- Test: covered by Task 5 unit tests for logic; this task's deliverable is verified by build + the on-device checklist (Task 12).

**Interfaces:**
- Consumes: `ExoPlaybackController` / Media3 `Player` (Task 5).
- Produces: a running `MediaSessionService` exposing a `MediaSession` other components bind to; media notification via Media3's default notification provider.

- [ ] **Step 1:** Implement `PlaybackService` extending `MediaSessionService`: build an `ExoPlayer` with `setAudioAttributes(music, handleAudioFocus = true)` and `setHandleAudioBecomingNoisy(true)` (Review Focus #5), a `Player.Listener` applying `PlaybackErrorPolicy.onError(...)` on `onPlayerError` (Review Focus #1), and a `MediaSession`. Return it from `onGetSession`.
- [ ] **Step 2:** Register the service in the manifest; ensure the foreground media notification shows on play.
- [ ] **Step 3: Run `./gradlew assembleDebug`** — Expected: PASS
- [ ] **Step 4: Manual device check** (fold into Task 12 checklist): play audio, background the app / lock screen → audio continues with working notification controls; unplug headphones → auto-pause.
- [ ] **Step 5: Commit** — `feat: add background playback MediaSessionService`

---

### Task 7: PlaybackConnection (UI ↔ service MediaController)

**Files:**
- Create: `app/src/main/java/com/backgroundplayer/playback/PlaybackConnection.kt`
- Test: construction smoke test only; behavior verified on device (requires a running service).

**Interfaces:**
- Consumes: `PlaybackService` (Task 6), `PlaybackController` (Task 5).
- Produces: `PlaybackConnection(context)` implementing `PlaybackController` by holding a Media3 `MediaController` bound to `PlaybackService`, exposing the same `state: StateFlow<PlaybackState>`. ViewModels depend on the `PlaybackController` interface, not this class.

- [ ] **Step 1:** Implement `PlaybackConnection`: build a `MediaController` via `MediaController.Builder` against the service `SessionToken`; forward commands; map controller listener events to `PlaybackState` (reuse the Task 5 reducer).
- [ ] **Step 2: Run `./gradlew assembleDebug`** — Expected: PASS
- [ ] **Step 3: Commit** — `feat: connect UI to playback service via MediaController`

---

### Task 8: Library screen & ViewModel

**Files:**
- Create: `app/src/main/java/com/backgroundplayer/ui/library/LibraryViewModel.kt`
- Create: `.../ui/library/LibraryScreen.kt` (Compose)
- Test: `app/src/test/java/com/backgroundplayer/ui/library/LibraryViewModelTest.kt`

**Interfaces:**
- Consumes: `LibraryRepository` (Task 3), `PlaybackController` (Task 5), importer (Task 4).
- Produces: `LibraryViewModel(repository, playback, importer)` exposing `uiState: StateFlow<LibraryUiState>` (`tracks`, `filter`, `query`, `isEmpty`), and actions `setFilter`, `setQuery`, `playTrack(track)` (plays the current filtered list starting at that track), `scanDevice`, `importFiles(uris)`.

- [ ] **Step 1: Write failing tests** — `setQueryFiltersList`, `setFilterToVideoShowsOnlyVideo`, `emptyLibraryReportsIsEmpty` (Review Focus #3), `playTrackStartsAtSelectedIndex` — with a fake repository and fake `PlaybackController`.
- [ ] **Step 2: Run `./gradlew test`** — Expected: FAIL
- [ ] **Step 3: Implement `LibraryViewModel`, then `LibraryScreen`** (list with Coil thumbnails, filter chips, search field, import button). UI is not unit-tested.
- [ ] **Step 4: Run `./gradlew test`** — Expected: PASS
- [ ] **Step 5: Commit** — `feat: add library screen with search and filter`

---

### Task 9: Playlists & playlist detail

**Files:**
- Create: `.../ui/playlists/PlaylistsViewModel.kt`, `PlaylistsScreen.kt`
- Create: `.../ui/playlists/PlaylistDetailViewModel.kt`, `PlaylistDetailScreen.kt`
- Test: `app/src/test/java/com/backgroundplayer/ui/playlists/PlaylistsViewModelTest.kt`, `PlaylistDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `LibraryRepository` (Task 3), `PlaybackController` (Task 5), `RepeatMode` (Task 5).
- Produces:
  - `PlaylistsViewModel`: `playlists: StateFlow<List<Playlist>>`, `create(name)`, `rename(id,name)`, `delete(id)`.
  - `PlaylistDetailViewModel(repository, playback, playlistId)`: `tracks: StateFlow<List<Track>>`, `reorder(from,to)`, `remove(trackId)`, `playAll(shuffle: Boolean, repeat: RepeatMode)` — a no-op when the playlist is empty.

- [ ] **Step 1: Write failing tests** — `createThenRenameThenDelete`, `reorderPersistsNewOrder`, `playAllBuildsQueueInOrder`, `playAllOnEmptyPlaylistIsNoOp` (Review Focus #3), `playAllWithShuffleEnablesShuffle`.
- [ ] **Step 2: Run `./gradlew test`** — Expected: FAIL
- [ ] **Step 3: Implement the two ViewModels, then the screens** (drag-to-reorder list, create/rename/delete dialogs, play-all with shuffle/repeat toggles).
- [ ] **Step 4: Run `./gradlew test`** — Expected: PASS
- [ ] **Step 5: Commit** — `feat: add playlists and playlist detail with shuffle/repeat`

---

### Task 10: Now Playing & mini-player UI

**Files:**
- Create: `.../ui/player/PlayerScreen.kt` (full player), `.../ui/player/MiniPlayerBar.kt`, `.../ui/player/VideoSurface.kt` (AndroidView wrapping Media3 `PlayerView`)
- Test: UI; no unit test. Control→command wiring reuses the Task 5 `PlaybackController`.

**Interfaces:**
- Consumes: `PlaybackController.state` + commands (Task 5/7).
- Produces: `PlayerScreen` (video surface or album art, scrubber bound to `positionMs`/`durationMs`, play/pause/skip, shuffle/repeat toggles, a minimize-to-PiP button shown only when `state.isVideo`), and `MiniPlayerBar` (pinned, shows current track + play/pause/skip, tap to expand).

- [ ] **Step 1:** Implement `VideoSurface`, `MiniPlayerBar`, `PlayerScreen`, driven by `PlaybackController.state`.
- [ ] **Step 2: Run `./gradlew assembleDebug`** — Expected: PASS
- [ ] **Step 3: Commit** — `feat: add now-playing and mini-player UI`

---

### Task 11: Picture-in-Picture for video

**Files:**
- Create: `app/src/main/java/com/backgroundplayer/playback/PipPolicy.kt` (pure)
- Modify: `MainActivity.kt` (enter PiP on `onUserLeaveHint`/minimize; `PictureInPictureParams` with play/pause/next `RemoteAction`s; handle `onPictureInPictureModeChanged`), `AndroidManifest.xml` (`supportsPictureInPicture=true`, `configChanges` for the activity)
- Test: `app/src/test/java/com/backgroundplayer/playback/PipPolicyTest.kt`

**Interfaces:**
- Consumes: `PlaybackState` (Task 5).
- Produces: `object PipPolicy { fun shouldEnterPip(state: PlaybackState): Boolean }` → true only when `isVideo && isPlaying`.

- [ ] **Step 1: Write failing tests** — `entersPipForPlayingVideo`, `noPipForAudio`, `noPipWhenPaused`.
- [ ] **Step 2: Run `./gradlew test`** — Expected: FAIL
- [ ] **Step 3: Implement `PipPolicy`; wire `MainActivity` to enter PiP via the policy** and render the video surface in PiP.
- [ ] **Step 4: Run `./gradlew test`** then **manual device check** (fold into Task 12): play a video, leave the app → floating window continues with controls; audio-only track → no PiP.
- [ ] **Step 5: Commit** — `feat: add picture-in-picture for video playback`

---

### Task 12: Permissions, navigation & app wiring

**Files:**
- Create: `.../permissions/MediaPermissions.kt` (pure `requiredMediaPermissions(sdkInt): List<String>`), `.../ui/permissions/PermissionGate.kt` (rationale + settings link)
- Create: `.../AppContainer.kt` (manual DI: builds DB, repository, importer, playback connection), `.../App.kt` (Application subclass holding the container)
- Modify: `MainActivity.kt` (bottom nav Library/Playlists, player overlay + mini-player, permission gate on first run)
- Test: `app/src/test/java/com/backgroundplayer/permissions/MediaPermissionsTest.kt`

**Interfaces:**
- Consumes: every prior component via `AppContainer`.
- Produces: `object MediaPermissions { fun requiredMediaPermissions(sdkInt: Int): List<String> }` → API ≥33 returns `READ_MEDIA_AUDIO`+`READ_MEDIA_VIDEO`; below returns `READ_EXTERNAL_STORAGE`; adds `POST_NOTIFICATIONS` when API ≥33. A fully wired, runnable app.

- [ ] **Step 1: Write failing tests** — `api33RequestsGranularMediaAndNotifications`, `api29RequestsReadExternalStorage`.
- [ ] **Step 2: Run `./gradlew test`** — Expected: FAIL
- [ ] **Step 3: Implement `MediaPermissions`, `PermissionGate`, `AppContainer`/`App`, and wire navigation + mini-player + permission gate into `MainActivity`.**
- [ ] **Step 4: Run `./gradlew test` and `./gradlew assembleDebug`** — Expected: PASS
- [ ] **Step 5: On-device manual checklist** (from the spec): install the debug APK, then verify (a) background audio with screen off + lock-screen controls, (b) headphone-unplug auto-pause, (c) video PiP floating window, (d) scan + SAF import, (e) playlist create/reorder/play-all with shuffle & repeat, (f) library search/filter, (g) empty-library and empty-playlist states.
- [ ] **Step 6: Commit** — `feat: add permissions, navigation, and app wiring`

---

## Notes for the implementer

- Keep ViewModels dependent on the `PlaybackController` **interface** (Task 5), never on `PlaybackConnection` or ExoPlayer directly — that is what keeps the ViewModel tests device-free.
- Run `./gradlew test` after every task; only Task 12's final checklist needs a real device or emulator.
- Follow the commit after each task so the branch stays reviewable task-by-task.
