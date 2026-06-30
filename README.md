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

## App Updates

Life Simulator uses a GitHub Releases-based updater for side-loaded installs. The app checks:

```text
https://raw.githubusercontent.com/AzizjonKasimov/life-simulator-app-releases/main/version.json
```

When `versionCode` is newer than the installed app, it downloads the APK from the release manifest and opens Android's package installer. The first install and every update must be signed with the same local release key.

For the first phone install, download the APK from the latest release in `AzizjonKasimov/life-simulator-app-releases`. After that, the in-app updater can fetch newer release APKs from the same channel.

Release signing uses local, gitignored files:

- `release.keystore`
- `keystore.properties`

Create `keystore.properties` from `keystore.properties.example`, keep both files private, and back them up. Publish a release with:

```powershell
.\release.ps1 -VersionName 0.2.0 -VersionCode 2 -Notes "New actions and balance changes."
```

The script builds a signed APK, publishes `LifeSimulator-<version>.apk` to `AzizjonKasimov/life-simulator-app-releases`, and updates `version.json` for the in-app updater.

## Game Loop

Start a fictional young-adult life from one of three archetypes: Student, Junior Worker, or Freelancer. Each day gives limited time and energy for actions such as work, study, exercise, rest, socializing, and freelancing. Actions affect money, health, mood, stress, social life, knowledge, fitness, and career progress.

## License

MIT License. See [LICENSE](LICENSE).

## Agent Instructions

Local agent loader files are intentionally gitignored. On the user's machine, `AGENTS.md` and `CLAUDE.md` point agents to the canonical instruction and context files maintained outside this app repo.
