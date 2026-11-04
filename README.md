# backgroundPlayer

A personal, offline Android player for **local audio and video** that keeps
playing in the background while you use other apps. No accounts, no streaming,
no network — media is referenced by URI and **never copied or deleted**.

Android only (min Android 8.0 / API 26, target API 35). Built with Kotlin,
Jetpack Compose, and Media3 (ExoPlayer + MediaSession).

## Features

**Library**
- Scan on-device media (MediaStore) and import individual files (Storage Access
  Framework); files are referenced, never copied.
- Search by title/artist; filter by All / Audio / Video.
- Video thumbnails; tracks whose file is missing or unreadable are auto-hidden.
- Remove a track from the library (hides the entry, file stays on disk) with Undo.

**Playback**
- Background audio with the screen off (Media3 foreground service).
- Notification-shade and lock-screen controls.
- Auto-pause on headphone / Bluetooth disconnect.
- Shuffle, repeat (off / all / one), next / previous, seek.

**Playlists**
- Create, rename, and delete (with confirmation).
- Add tracks from the library or via an in-playlist multi-select picker.
- Reorder (up / down) and remove tracks — with Undo.

**Now-playing & video**
- Full now-playing screen with artwork, scrubber, and transport.
- **Floating overlay tile** for audio over other apps (draggable; requires the
  "display over other apps" permission, requested on demand).
- **Picture-in-Picture** floating window for video.
- **Full-screen video** — immersive, follows device rotation, tap-to-reveal
  auto-hiding controls.

**Design**
- "Sunset glass" UI: animated gradient backdrop with soft color blobs, frosted
  translucent surfaces, sliding screen transitions, and an artwork-tinted player.

## Architecture

Single-activity Compose app. A Media3 `MediaSessionService` (foreground service)
owns the `ExoPlayer`; the UI connects through a `MediaController` and talks to
playback only through a `PlaybackController` interface, so ViewModels are tested
with a fake. Library and playlists persist in Room. Dependencies are wired by a
small hand-written `AppContainer` (no Hilt).

```
app/src/main/java/com/backgroundplayer/
├─ data/            Room entities + DAOs, LibraryRepository, media importer
├─ playback/        PlaybackController, ExoPlayer/MediaController impls, service,
│                   PiP + overlay policies
├─ ui/              Compose screens + ViewModels (library, playlists, player),
│                   app shell, theme, floating overlay
├─ App.kt           Application + AppContainer (manual DI)
└─ MainActivity.kt  nav host + Picture-in-Picture + floating-tile wiring
```

## Building

Requires JDK 17 and the Android SDK (platform 35). From the repo root:

```bash
./gradlew assembleDebug             # build the debug APK
./gradlew :app:testDebugUnitTest    # run the unit/Robolectric test suite
./gradlew installDebug              # install debug on a connected device
```

> The Robolectric tests download an `android-all` runtime jar from Maven on
> first run, so the initial test run needs network access; later runs are
> offline-safe.

### Release build (signed APK for sideloading)

Release signing reads from `keystore.properties` at the repo root (gitignored,
not committed). Create a keystore once and point to it:

```bash
keytool -genkeypair -v \
  -keystore backgroundplayer-release.jks \
  -alias backgroundplayer -keyalg RSA -keysize 2048 -validity 10000
```

`keystore.properties`:

```properties
storeFile=backgroundplayer-release.jks
storePassword=<your store password>
keyAlias=backgroundplayer
keyPassword=<your key password>
```

Then build:

```bash
./gradlew assembleRelease           # -> app/build/outputs/apk/release/app-release.apk
```

Share that APK; recipients enable "install unknown apps" for their file source
and tap it to install (Android 8.0+). **Back up the keystore and
`keystore.properties`** — the same key is required to ship updates that install
over an existing version. If `keystore.properties` is absent, the release build
falls back to debug signing so the project still builds.

## Status

Feature-complete with a green unit/Robolectric suite, built task-by-task (TDD),
and verified on a physical device (background audio, lock-screen controls,
headphone auto-pause, Picture-in-Picture, floating audio tile, full-screen
video, SAF import, and video thumbnails).

- Design spec: [`docs/superpowers/specs/2026-10-14-background-player-design.md`](docs/superpowers/specs/2026-10-14-background-player-design.md)
- Implementation plan: [`docs/superpowers/plans/2026-10-14-background-player.md`](docs/superpowers/plans/2026-10-14-background-player.md)
