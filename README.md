# AuraX Imagery

**EVERY MEMORY, BEAUTIFULLY.**

A premium, offline-first Android gallery with a liquid-glass interface on a true-black AMOLED base.

- Kotlin, Jetpack Compose, Material 3 with a custom glass design system
- MVVM + Clean Architecture (`data` / `domain` / `ui`)
- Min SDK 35 (Android 15), compile/target SDK 36
- No network access: photos and videos never leave the device

## Status

Phases 1-2 of 7 are written. Phase 1 compiles in CI; Phase 2 also compiles and passes its unit tests in CI. **Nothing has been run on a device yet.**
The environment used for development has no Android SDK, so builds and unit tests run in GitHub Actions.

| Phase | Scope | State |
|---|---|---|
| 1 | Project setup, architecture, theme, floating glass navigation | Builds in CI |
| 2 | MediaStore integration, permissions, background indexing | Builds in CI; untested on a device |
| 3 | Home timeline, albums, favorites | Not started |
| 4 | Full-screen viewer, video player | Not started |
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
```
