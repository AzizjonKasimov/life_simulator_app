# Life Simulator App

A personal Android dashboard-style life simulator game. The first milestone is a local debug install on a phone, not a Play Store release.

## Status

V1 is a native Android systems-first prototype with a playable daily decision loop.

## Product Direction

- Playable dashboard-first life simulation.
- Phone-friendly surfaces for stats, progression, activities, events, history, and controls.
- Local-first gameplay with no backend, accounts, telemetry, or Play Store release plumbing until explicitly needed.

## Planned Stack

- Kotlin
- Jetpack Compose
- Material 3
- Gradle version catalog
- Room for durable simulation state

## Development

Use PowerShell from the repo root:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
.\gradlew.bat testDebugUnitTest
```

## Game Loop

Start a fictional young-adult life from one of three archetypes: Student, Junior Worker, or Freelancer. Each day gives limited time and energy for actions such as work, study, exercise, rest, socializing, and freelancing. Actions affect money, health, mood, stress, social life, knowledge, fitness, and career progress.

## License

MIT License. See [LICENSE](LICENSE).

## Agent Instructions

Local agent loader files are intentionally gitignored. On the user's machine, `AGENTS.md` and `CLAUDE.md` point agents to the canonical instruction and context files maintained outside this app repo.
