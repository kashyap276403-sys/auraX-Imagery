# AuraX Imagery

**EVERY MEMORY, BEAUTIFULLY.**

A premium, offline-first Android gallery with a liquid-glass interface on a true-black AMOLED base.

- Kotlin, Jetpack Compose, Material 3 with a custom glass design system
- MVVM + Clean Architecture (`data` / `domain` / `ui`)
- Min SDK 35 (Android 15), compile/target SDK 36
- No network access: photos and videos never leave the device

## Status

Phases 1-4 of 7 are written. Phases 1-3 compile and pass their unit tests in CI. **Nothing has been run on a device yet.**
The environment used for development has no Android SDK, so builds and unit tests run in GitHub Actions.

| Phase | Scope | State |
|---|---|---|
| 1 | Project setup, architecture, theme, floating glass navigation | Builds in CI |
| 2 | MediaStore integration, permissions, background indexing | Builds in CI; untested on a device |
| 3 | Home timeline, albums, favorites | Builds in CI; untested on a device. Custom (user-created) albums not done yet |
| 4 | Full-screen viewer, video player | Written; awaiting first CI build. Edit is an interim hand-off to another app until the Phase 5 editor; delete moves to the system trash (system confirmation) |
| 5 | Photo editor, search | Not started |
| 6 | Liquid Glass renderer, Glass Studio | Not started |
| 7 | Settings, privacy, optimization, testing | Not started |

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
