# Life Simulator App

A personal Android life simulator game in the BitLife tradition: you're born as a person, and each tap of **Age Up** lives one year of choices, events, and relationships. It side-loads onto a phone as a signed APK and updates itself from GitHub Releases — not a Play Store app.

## Status

**M1 (thin playable slice) of a BitLife-style rebuild.** You're born somewhere in the real world with four core stats — Happiness, Health, Smarts, Looks — and each **Age Up** lives one year: your stats drift, seeded life events happen and ask you to choose, and you can spend the year on activities or time with the people in your life. Education milestones, a light job layer, money, and family all play out year by year until you die and get a legacy summary. Every gamble is seeded, so a given life replays identically.

> This replaces the v0.1–v0.9 **finance** simulator (grow-your-net-worth), which lives on in git history and the released `0.9.x` APKs. The full design for the life sim — systems, content plan, and roadmap — is in [docs/DESIGN.md](docs/DESIGN.md).

M1 includes: character creation, the year-based Age Up loop, ~50 authored events across life stages, parents and siblings you can interact with, a handful of activities, a light job/education layer, death, and a legacy screen. M2 (breadth: university, careers with promotions, romance → marriage → children, ~120 events) and M3 (depth: health/illness, crime, assets, 200+ events) are laid out in the design doc.

## Product Direction

- A text-driven, year-at-a-time life simulation; the life story is the throughline.
- Phone-friendly surfaces: your life feed, activities, the people in your life, and a profile.
- Local-first gameplay with no backend, accounts, telemetry, or Play Store release plumbing until explicitly needed.
- Keep it legible and player-driven: the game presents choices and never plays the optimal move for you.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Gradle version catalog
- Room (with KSP) for durable simulation state

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

After a signed release build, use `LifeSimulator-latest.apk` from the root folder for phone installation. A versioned copy such as `LifeSimulator-0.9.0.apk` is kept there too.

Saves use Room database version `5` and JSON schema version `6`, stored as `schemaVersion + stateJson` so simulator systems can evolve without frequent table rewrites. The BitLife-style rebuild bumped the schema (`5` → `6`); because a save whose schema doesn't match — or fails to decode — is treated as "no save," any old finance save is retired cleanly and the app starts a fresh life instead of crashing. Within the life sim, new fields are added backward-compatibly so existing saves carry across updates.

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
.\release.ps1 -VersionName 0.10.0 -VersionCode 10 -Notes "BitLife-style rebuild (M1): born, Age Up through a life of seeded events and choices, relationships, death, and a legacy screen."
```

The script bumps the version, builds a signed APK, publishes `LifeSimulator-<version>.apk` to `AzizjonKasimov/life-simulator-app-releases`, and updates `version.json` for the in-app updater. The version bump in this repo is left uncommitted for review.

## Game Loop

You're born as a baby — a random name (or one you choose), a gender, a birthplace, four core stats, and a starting family. There's no score to maximize; the *story* is the point.

Each **Age Up** advances one year: stats drift with age (health and looks fade later in life; the young grow into themselves), any salary is paid, education milestones land, the people in your life age (and can pass away), a death check runs, and one to three **seeded life events** fire. Flavour events simply happen; real decisions pop up and ask you to choose, with consequences that ripple through the rest of the life. Between years you can spend the year on **activities** or with **people**.

**Stats.** Happiness, Health, Smarts, and Looks (0–100) are pushed by events, activities, and age. If Health hits zero, the life ends.

**Life stages.** Infant → Child → Teen → Young Adult → Adult → Senior gate which events can fire and what you can do — from playground scrapes and school dances to careers, houses, and grandchildren.

**People.** Parents and siblings to start; each is a real person with a name, an age, and a relationship meter you can raise by spending time together. (More relationship types — friends, partners, children — arrive in M2.)

**Work & school.** School and graduation happen on their own; from 16+ you can look for a job (a seeded, smarts-weighted hire) that pays a yearly salary.

**Money.** A light layer for now — salary in, event and activity costs out. It's one system among many, not the scoreboard.

**Risk & luck.** Event selection, job hunts, and mortality all flow from one save seed, so a given life replays identically.

**Death & legacy.** Every life ends — by age, health, or misfortune — with a legacy screen recapping your years, final stats, wealth, and notable moments, then the option to start a new life.

**Surfaces.** Four tabs: **Life** (your stats, the Age Up button, and the life-story feed), **Activities**, **People**, and **Profile** (character details, stats, and app updates).

## License

MIT License. See [LICENSE](LICENSE).

## Agent Instructions

Local agent loader files are intentionally gitignored. On the user's machine, `AGENTS.md` and `CLAUDE.md` point agents to the canonical instruction and context files maintained outside this app repo.
