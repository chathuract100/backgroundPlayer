# backgroundPlayer

A simple personal Android media player for **local audio and video** that keeps
playing in the background while you use other apps.

- 🎵 **Background audio** — music keeps playing with the screen off, with
  controls on the notification shade and lock screen.
- 📺 **Floating video (Picture-in-Picture)** — videos shrink into a floating
  window over other apps, with play/pause/next controls.
- 📂 **Local media** — play files already on your phone (MediaStore scan) or
  import individual files (Storage Access Framework). Media is referenced by
  URI and **never copied**. No accounts, no streaming, no network.
- 📝 **Playlists** with drag-free reorder, shuffle & repeat, plus a searchable,
  filterable library (all / audio / video).

Android only (min Android 8.0 / API 26, target API 35). Built with Kotlin,
Jetpack Compose, and Media3 (ExoPlayer + MediaSession).

## Architecture

Single-activity Compose app. A Media3 `MediaSessionService` (foreground service)
owns the `ExoPlayer`; the UI connects through a `MediaController` and talks to
playback only through a `PlaybackController` interface, so ViewModels are tested
with a fake. Library and playlists persist in Room. Dependencies are wired by a
small hand-written `AppContainer` (no Hilt).

```
app/src/main/java/com/backgroundplayer/
├─ data/            Room entities + DAOs, LibraryRepository, media importer
├─ playback/        PlaybackController, ExoPlayer/MediaController impls, service, PiP policy
├─ ui/              Compose screens + ViewModels (library, playlists, player) + app shell
├─ permissions/     runtime-permission policy
├─ App.kt           Application + AppContainer (manual DI)
└─ MainActivity.kt  nav host + Picture-in-Picture wiring
```

## Building

Requires JDK 17 and the Android SDK (platform 35). From the repo root:

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew :app:testDebugUnitTest   # run the unit/Robolectric test suite
```

> The Robolectric tests download an `android-all` runtime jar from Maven on
> first run, so the initial test run needs network access; later runs are
> offline-safe.

## Status

Feature-complete against the design spec with a green unit/Robolectric suite,
built task-by-task (TDD). **Not yet verified on a physical device/emulator** —
background audio, lock-screen controls, headphone-unplug auto-pause, video
Picture-in-Picture, SAF import, and video thumbnail rendering are device-only
behaviors and must be checked on hardware before release.

- Design spec: [`docs/superpowers/specs/2026-10-14-background-player-design.md`](docs/superpowers/specs/2026-10-14-background-player-design.md)
- Implementation plan: [`docs/superpowers/plans/2026-10-14-background-player.md`](docs/superpowers/plans/2026-10-14-background-player.md)
