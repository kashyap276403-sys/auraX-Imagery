# AuraX Imagery

**EVERY MEMORY, BEAUTIFULLY.**

A premium, offline-first Android gallery with a liquid-glass interface on a true-black AMOLED base.

- Kotlin, Jetpack Compose, Material 3 with a custom glass design system
- MVVM + Clean Architecture (`data` / `domain` / `ui`)
- Min SDK 35 (Android 15), compile/target SDK 36
- No network access: photos and videos never leave the device

## Status

Phases 1-7 of 7 are written (6 is a prototype). Phases 1-3 compile and pass their unit tests in CI. **Nothing has been run on a device yet.**
The environment used for development has no Android SDK, so builds and unit tests run in GitHub Actions.

| Phase | Scope | State |
|---|---|---|
| 1 | Project setup, architecture, theme, floating glass navigation | Builds in CI |
| 2 | MediaStore integration, permissions, background indexing | Builds in CI; untested on a device |
| 3 | Home timeline, albums, favorites | Builds in CI; untested on a device. Custom (user-created) albums not done yet |
| 4 | Full-screen viewer, video player | Written; awaiting first CI build. Edit opens the in-app editor (Phase 5); delete moves to the system trash (system confirmation) |
| 5 | Photo editor, search | Written; awaiting first CI build. Editor always saves a new copy (Pictures/AuraX Imagery) and never overwrites the original; EXIF is not copied to the copy. Search matches file name, album, month/year, type on the local index |
| 6 | Liquid Glass renderer, Glass Studio | **Prototype only** (builds in CI, unverified on a device): Haze 1.6.10 backdrop blur + AGSL shader in `ui/glass`, `LiquidNavCapsule`, Glass Studio (Settings > Glass Studio). Not yet applied to the app's own bars and sheets until the prototype is checked on a phone |
| 7 | Settings, privacy, optimization, testing | Settings written (performance, storage, privacy, app info); builds in CI, untested on a device. Large-library paging and a device performance pass are still open |

### Video player upgrade (separate 6-phase plan)

| Phase | Scope | State |
|---|---|---|
| V1 | Player UI, auto-hide controls, Screen Ratio system (Fit / Fill / Original / Stretch, 8 aspect presets, zoom slider + drag-to-pan, last mode remembered) | Builds in CI, `VideoDisplayTest` passes. **Not run on a device.** Pinch zoom, double-tap seek, brightness/volume gestures are Phase V2 |
| V2 | Pinch zoom + drag pan, double-tap seek (±10s), brightness (left half) and volume (right half) swipes with indicators, playback speed 0.25x-2x, orientation lock (Auto/Portrait/Landscape), gesture toggles in Settings > Video player | Builds in CI, `VideoGesturesTest` passes. **Not run on a device** |
| V3 | Embedded + external .srt subtitles (size, position), audio track picker, A-B repeat, repeat off/one/queue, playback queue (next/previous/autoplay/remove), bookmarks (Room DB v2 migration), frame preview while scrubbing | Builds in CI, `PlaybackLogicTest` passes. **Not run on a device**; the Room 1->2 migration has never run on a real database. No subtitle delay (Media3 has no offset) |
| V4 | Resume watching (Room v3 `playback_progress`, also the history), Continue watching / Recently played rows in Albums, sleep timer, Picture-in-Picture (play/pause button, auto-enter), save current frame as PNG, full video details sheet, Settings: resume switch + clear history | Builds in CI, `PlaybackHistoryTest` passes. **Not run on a device**; Room 2->3 migration and PiP never exercised on a phone |
| V5 | Cinematic looks (Original/Cinema/Vivid/Warm/Cool via a GPU colour matrix on the video layer), full Settings > Video player (default speed/screen mode, remember mode, seek interval, auto-hide, orientation, autoplay, subtitles default, haptics, gestures, resume, reset), shared `PlayerPill`, sheet border/radius from Glass Studio, reduced-motion aware fades, 48dp touch targets, seek-bar semantics, slower position polling while controls are hidden | Builds in CI, `CinematicAndSettingsTest` passes. **Not run on a device.** Sheets are separate windows, so they are not blurred |
| V6 | Code audit and fixes (neighbouring pages could clear the "busy" state; stale track indices), schema-export check of the Room migration SQL (`MigrationSqlTest`), device checklist in `docs/VIDEO_PLAYER_CHECKLIST.md` | Builds in CI. **No device or UI test has been run**, so V1-V6 are verified by compilation and unit tests only |

| V-UI | Cinematic player redesign: glass close/more buttons, title + info, 5-button transport with a luminous play button, floating cluster (volume, subtitles, audio, one-tap ratio and zoom cycles), thin bottom panel with keyframe scrub preview that follows the thumb, Info/Queue/Speed chips, fade+slide chrome | Builds in CI, unit tests pass. **Not run on a device.** Glass is dark translucent with a rim light; there is no backdrop blur on the video controls |
| V-Movie | Movie mode (per video, via the ⋮ menu): gestures, ratio/zoom, subtitles, audio, speed, cinematic look, A-B repeat and the right-side cluster only work there. Other videos get the plain gallery player (back, title, ⋮, play/pause, seek bar, fullscreen). Transport is -10s / play / +10s; Info, Queue, Speed, Volume and Zoom moved into the ⋮ menu | Builds in CI (check Actions). **Not run on a device** |
| Glass-WD | Glass style selector (Classic / Water Drop) in Glass Studio and Settings, saved in DataStore and applied app-wide at once. Water Drop = own AGSL shader (lens refraction, dispersion, specular crescent, caustic, gloss). New controls: Water Drop intensity, Border opacity. Menus, dialogs, sheets and video controls follow the style | Builds in CI, codec/colour unit tests pass. **Not run on a device.** Menus, dialogs, sheets and video controls are separate windows or sit over video, so they get tint, border and gloss but **no backdrop blur** |

## Building

Open in Android Studio (latest stable) and run the `app` configuration, or from the command line:

```
./gradlew assembleDebug
```

GitHub Actions (`.github/workflows/build.yml`) builds a debug APK on every push and uploads it as an artifact.

## Structure

```
app/src/main/java/com/aurax/imagery/
  domain/   model, repository interfaces, use cases (plain Kotlin, no Android types)
  ui/       theme, components (glass design system), navigation, one package per screen
  data/     Room index (local/database), MediaStore scanner and WorkManager indexing (media), repository impl
  utils/    media permission helpers, formatters
  ui/gallery  shared timeline grid, selection mode and list view-model (Photos, Favorites, album detail)
```
