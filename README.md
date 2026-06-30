# Life Simulator App

A personal Android dashboard-style life simulator game. The first milestone is a local debug install on a phone, not a Play Store release.

## Status

V0.5 is a native Android command-center life simulator with one normal unemployed-person start and a deeper money loop. It has job search into steady work, career promotion, a client-service side business pipeline, finances, relationships, deterministic events, auto-suggested daily focus, timed pressure opportunities, action result chips, and a cleaner game-like HUD with icons and meters.

## Product Direction

- Playable dashboard-first life simulation.
- Phone-friendly surfaces for money status, career/business progression, wellbeing, relationships, activities, events, history, and controls.
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

After a signed release build, use `LifeSimulator-latest.apk` from the root folder for phone installation. A versioned copy such as `LifeSimulator-0.5.0.apk` is kept there too.

The V0.5 rebuild uses Room database version `4` and JSON schema version `4`. Updating from V0.4 resets the old local save so the single-start job, career, and business state can start cleanly. New saves are stored as `schemaVersion + stateJson` so future simulator systems can evolve without frequent table rewrites.

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

Create `keystore.properties` from `keystore.properties.example`, keep both files private, and back them up. Publish the V0.5 release with:

```powershell
.\release.ps1 -VersionName 0.5.0 -VersionCode 5 -Notes "Career, business, and money-making upgrade with a single normal start."
```

The script builds a signed APK, publishes `LifeSimulator-<version>.apk` to `AzizjonKasimov/life-simulator-app-releases`, and updates `version.json` for the in-app updater.

## Game Loop

Start as Alex Rivers, a normal unemployed 22-year-old with $180 cash, $350 debt, bills coming due, modest skills, and one recoverable pressure curve. Each day starts with an auto-suggested focus: Money, Career, Recovery, Social, or Balanced. The player can override it before the first action, then the focus locks for the day.

Each day gives limited time and energy for work, growth, business, wellbeing, social, and money actions. Matching the day's focus gives small bonuses, and completing the focus at day end gives a modest stability reward. Ignoring a non-balanced focus adds light stress and mood pressure.

The money loop now has two connected tracks. Job search actions build applications, interview readiness, and offer progress until the first steady job unlocks. Employed actions pay salary, build reputation, and push promotion readiness; promotions increase title, level, and shift pay. Business actions build an offer, find leads, pitch clients, complete paid projects, improve the pipeline, and grow through business stages. Weekly business overhead starts only after the business reaches a reliable pipeline.

Timed opportunities add short pressure events such as building a bill buffer, lowering stress, pushing promotion readiness, reconnecting socially, or reducing debt. Active opportunities appear on the dashboard and can complete or expire with small consequences.

The dashboard now prioritizes the money loop first: identity/status, cash/runway/debt, job offer or salary progress, business pipeline, next bill pressure, daily focus, up to three priority actions, active opportunities, and the recent log. The Actions tab is grouped by Make Money, Get Hired / Career, Build Business, Recover, and Connect. The Progress tab shows status sections only: Career, Business, Finances, Wellbeing, Relationships, Skills, and App Updates.

Choices affect cash, debt, bills, credit, job-search progress, business leads, active projects, client trust, pipeline value, career XP, promotion readiness, health, mood, stress, relationships, modifiers, action history, and events. Long-term goals/checklists were removed in V0.5; progression now lives directly in the career, business, money, wellbeing, relationship, daily focus, and timed opportunity systems.

## License

MIT License. See [LICENSE](LICENSE).

## Agent Instructions

Local agent loader files are intentionally gitignored. On the user's machine, `AGENTS.md` and `CLAUDE.md` point agents to the canonical instruction and context files maintained outside this app repo.
