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
