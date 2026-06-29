# Life Simulator App

A personal Android dashboard-style life simulator game. The first milestone is a local debug install on a phone, not a Play Store release.

## Status

Repository bootstrap is in place. The Android project has not been scaffolded yet.

## Product Direction

- Playable dashboard-first life simulation.
- Phone-friendly surfaces for stats, progression, activities, events, history, and controls.
- Local-first gameplay with no backend, accounts, telemetry, or Play Store release plumbing until explicitly needed.

## Planned Stack

- Kotlin
- Jetpack Compose
- Material 3
- Gradle version catalog
- Room for durable simulation state when persistence is introduced
- DataStore for settings when needed

## Development

After the Android project is scaffolded, use PowerShell from the repo root:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
.\gradlew.bat testDebugUnitTest
```

## Agent Instructions

Local agent loader files are intentionally gitignored. On the user's machine, `AGENTS.md` and `CLAUDE.md` point agents to the canonical instruction and context files maintained outside this app repo.
