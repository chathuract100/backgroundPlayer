# backgroundPlayer — Design Spec

**Date:** 2026-10-14
**Status:** Approved design, pending implementation plan
**Platform:** Android only (min SDK 26 / Android 8.0, target latest)

## 1. Purpose

A simple personal media player for Android that plays **local audio and video**
in the background while the user does other things on the phone. Music keeps
playing with the screen off and with lock-screen/notification controls; video
can shrink into a floating Picture-in-Picture (PiP) window over other apps.

All media is local — files already on the device or manually imported by the
user. No streaming services, no accounts, no network required.

### Success criteria
- Play a song, leave the app / turn off the screen → audio keeps playing with
  working controls on the notification and lock screen.
- Play a video, leave the app (or tap minimize) → video continues in a floating
  PiP window with play/pause/skip.
- Build and install to a personal phone with a one-time Android Studio setup.

### Out of scope (v1 — YAGNI)
Streaming/online sources, tag editing, folder auto-sync/watching,
ratings/favorites, iPhone support, cloud sync.

## 2. Architecture

Single-activity app. The player itself lives in a **foreground service**
(Media3 `MediaSessionService`) that owns the `ExoPlayer` instance. This is what
makes true background playback work: the service keeps running and playing when
the app is backgrounded or the screen is off. The UI connects to the service
through a `MediaController` and only reflects/commands player state — it never
owns the player.

### Stack
- **Kotlin** + **Jetpack Compose** — UI
- **Media3** — `ExoPlayer` (playback), `MediaSession` (lock-screen/notification
  controls), `MediaSessionService` (background foreground-service host)
- **Room (SQLite)** — library + playlist persistence
- **MediaStore + Storage Access Framework (SAF)** — device scan + file/folder
  import with persisted URI permissions
- **Coil** — album art / video thumbnails
- **Android PiP API** — floating video window

### Layers
- `data` — Room entities, DAOs, repository, media importer (MediaStore + SAF +
  `MediaMetadataRetriever`)
- `playback` — the `MediaSessionService`, player setup, media notification,
  audio-focus / becoming-noisy handling
- `ui` — Compose screens + ViewModels, `MediaController` connection

Each layer has one clear responsibility and a defined interface: the UI depends
on the repository (for library data) and the `MediaController` (for playback);
the service depends on the repository to resolve track URIs; the data layer
depends on nothing above it.

## 3. Data model & media import

### Room tables
- **Track**: `id`, `uri` (content URI with persisted read permission), `title`,
  `artist`, `album`, `durationMs`, `isVideo` (derived from MIME type),
  `thumbnailUri`, `dateAdded`.
- **Playlist**: `id`, `name`, `createdAt`.
- **PlaylistTrack** (join): `playlistId`, `trackId`, `position`. The `position`
  column defines and preserves play order and supports reordering.

Media files are stored **by reference (URI), never copied** — the app stays
small and does not duplicate the user's files. The live playback queue and
"now playing" state are owned by ExoPlayer, not persisted to their own table.

### Import paths
1. **Scan device** — `MediaStore` enumerates existing audio/video on the phone.
2. **Import files/folder** — SAF picker (`ACTION_OPEN_DOCUMENT` /
   `OPEN_DOCUMENT_TREE`); URI permissions are persisted so access survives
   reboots.

For both paths, metadata (title/artist/duration/art) is read with
`MediaMetadataRetriever` and one Track row is written per file.

## 4. Playback, background audio & PiP

### Background audio
On play, the `MediaSessionService` promotes to a **foreground service** with a
media notification. The notification and lock screen show title/art and expose
play/pause/skip/seek. Playback continues when the app is backgrounded or the
screen is off, and stops cleanly when dismissed or when playback ends.

### Picture-in-Picture (video)
When a **video** is playing and the user leaves the app (`onUserLeaveHint`) or
taps "minimize", the player Activity calls `enterPictureInPictureMode` with
play/pause/skip `RemoteAction`s. Tapping the window restores full screen.
Audio-only tracks never trigger PiP — they ride the background service.

### Good-citizen playback
- **Audio focus** — pause/duck on incoming call or another app's audio; resume
  after.
- **Becoming noisy** — auto-pause on headphone unplug / Bluetooth disconnect.

### Permissions
`READ_MEDIA_AUDIO` + `READ_MEDIA_VIDEO` (Android 13+, fallback to
`READ_EXTERNAL_STORAGE` on older), `FOREGROUND_SERVICE` +
`FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`. Each is requested in
context with a short rationale; on denial, show a one-tap link to app settings.

## 5. Screens & navigation

- **Library** — full track list with filter (All / Music / Video) and search
  (title/artist). Tap to play; long-press to add to playlist/queue. Import
  button (scan / pick files) lives here.
- **Playlists** — list of playlists; create / rename / delete.
- **Playlist detail** — tracks with drag-to-reorder, remove, "play all" with
  shuffle and repeat (one/all) toggles.
- **Now Playing (full player)** — video surface (or album art for audio),
  scrubber, play/pause/skip, shuffle/repeat, minimize-to-PiP button for video.
- **Mini-player bar** — thin persistent bar pinned to the bottom of every
  screen (play/pause/skip + what's playing); tap to expand to the full player.

Bottom navigation switches between **Library** and **Playlists**; the player
opens over the top. Shuffle/repeat operate on the active queue.

## 6. Error handling

- **Unplayable/corrupt file** → catch the ExoPlayer error, show a brief message,
  skip to the next track (no crash).
- **File moved/deleted or revoked import permission** → mark the track
  "unavailable" with a re-import prompt rather than leaving a dead entry.
- **Permission denied** → rationale screen with a one-tap link to app settings.
- **Playback interruptions** → handled by audio-focus and becoming-noisy logic.

## 7. Testing

- **Unit tests** for bug-prone logic: playlist ordering/reorder, shuffle &
  repeat behavior, importer metadata parsing, and player-state → UI-state
  mapping (via a fake player).
- **Instrumented tests** for the Room database (library + playlist persistence).
- **Manual on-device checklist** for what only a real phone proves: background
  audio with screen off, lock-screen controls, PiP floating window, and
  headphone-unplug auto-pause.

Exhaustive automated coverage of playback itself is explicitly not a goal (hard
to automate, low value); the logic *around* playback is what gets tested.
