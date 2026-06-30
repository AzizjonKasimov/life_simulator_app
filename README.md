# Life Simulator App

A personal Android dashboard-style life simulator game. The first milestone is a local debug install on a phone, not a Play Store release.

## Status

V0.4 is a native Android command-center life simulator with a stronger daily loop. It has grouped adult-life actions, goals, finances, career progression, relationships, deterministic events, auto-suggested daily focus, timed pressure opportunities, action result chips, and a dashboard-first phone UI.

## Product Direction

- Playable dashboard-first life simulation.
- Phone-friendly surfaces for stats, progression, activities, events, history, and controls.
- Local-first gameplay with no backend, accounts, telemetry, or Play Store release plumbing until explicitly needed.

## Stack

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

Signed release builds copy the install APK into the repo root:

```powershell
.\gradlew.bat assembleRelease
```

After a signed release build, use `LifeSimulator-latest.apk` from the root folder for phone installation. A versioned copy such as `LifeSimulator-0.2.0.apk` is kept there too.

The V0.4 rebuild uses Room database version `3` and JSON schema version `3`. Updating from the earlier V0.2 save resets the old local save so daily focus and timed opportunity state can start cleanly. New saves are stored as `schemaVersion + stateJson` so future simulator systems can evolve without frequent table rewrites.

## App Updates

Life Simulator uses a GitHub Releases-based updater for side-loaded installs. The app checks:

```text
https://raw.githubusercontent.com/AzizjonKasimov/life-simulator-app-releases/main/version.json
```

When `versionCode` is newer than the installed app, it downloads the APK from the release manifest and opens Android's package installer. The first install and every update must be signed with the same local release key.

For the first phone install, use `LifeSimulator-latest.apk` from the repo root after a signed release build, or download the APK from the latest release in `AzizjonKasimov/life-simulator-app-releases`. After that, the in-app updater can fetch newer release APKs from the same channel.

Release signing uses local, gitignored files:

- `release.keystore`
- `keystore.properties`

Create `keystore.properties` from `keystore.properties.example`, keep both files private, and back them up. Publish a release with:

```powershell
.\release.ps1 -VersionName 0.4.0 -VersionCode 4 -Notes "Daily focus, timed opportunities, and action feedback."
```

The script builds a signed APK, publishes `LifeSimulator-<version>.apk` to `AzizjonKasimov/life-simulator-app-releases`, and updates `version.json` for the in-app updater.

## Game Loop

Start a fictional young-adult life from one of three archetypes: Student, Junior Worker, or Freelancer. Each day starts with an auto-suggested focus: Money, Career, Recovery, Social, or Balanced. The player can override it before the first action, then the focus locks for the day.

Each day gives limited time and energy for work, growth, wellbeing, social, and money actions. Matching the day's focus gives small bonuses, and completing the focus at day end gives a modest stability reward. Ignoring a non-balanced focus adds light stress and mood pressure.

Timed opportunities add short pressure goals such as building a bill buffer, lowering stress, pushing promotion readiness, reconnecting socially, or reducing debt. Active opportunities appear on the dashboard and can complete or expire with small consequences.

Choices affect cash, debt, bills, credit, career XP, promotion readiness, health, mood, stress, relationships, goals, modifiers, action history, and events. V0.4 also adds archetype-specific actions: Exam Prep for Students, Manager Check-in for Junior Workers, and Pitch Client for Freelancers.

## License

MIT License. See [LICENSE](LICENSE).

## Agent Instructions

Local agent loader files are intentionally gitignored. On the user's machine, `AGENTS.md` and `CLAUDE.md` point agents to the canonical instruction and context files maintained outside this app repo.
