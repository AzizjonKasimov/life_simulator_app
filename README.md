# Life Simulator App

A personal Android life simulator game in the BitLife tradition: you're born as a person, and each tap of **Age Up** lives one year of choices, events, and relationships. It side-loads onto a phone as a signed APK and updates itself from GitHub Releases — not a Play Store app.

## Status

**M3 (depth) of a BitLife-style rebuild.** You're born somewhere in the real world with four core stats — Happiness, Health, Smarts, Looks — and each **Age Up** lives one year: your stats drift, seeded life events happen and ask you to choose, and you can spend the year on activities or time with the people in your life. Grow up, go to university, build a career with promotions, fall in love, marry, raise children, weather illness and the odd brush with the law, and grow old — until you die and get a legacy summary. Every gamble is seeded, so a given life replays identically.

> This replaces the v0.1–v0.9 **finance** simulator (grow-your-net-worth), which lives on in git history and the released `0.9.x` APKs. The full design for the life sim — systems, content plan, and roadmap — is in [docs/DESIGN.md](docs/DESIGN.md).

M3 adds depth on top of M2's breadth: a **health/illness** system (chronic and acute conditions that drain your health, with treatment to fight back), **crime & prison** (seeded heists with real risk, getting caught, and serving time that puts your life on hold), **assets** (cars, homes, and luxuries that build your net worth), personality **traits** rolled at birth, **achievements**, richer relationship interactions, and **200+ authored events** (up from ~106). All new save fields are additive, so an existing life carries straight across the update. The one remaining, optional M3 piece — generations (playing on as your child when you die) — is laid out in the design doc.

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
.\release.ps1 -VersionName 0.12.0 -VersionCode 14 -Notes "M3 depth: health and illness, crime and prison, assets and net worth, personality traits, achievements, and 200+ life events."
```

The script bumps the version, builds a signed APK, publishes `LifeSimulator-<version>.apk` to `AzizjonKasimov/life-simulator-app-releases`, and updates `version.json` for the in-app updater. The version bump in this repo is left uncommitted for review.

## Game Loop

You're born as a baby — a random name (or one you choose), a gender, a birthplace, four core stats, and a starting family. There's no score to maximize; the *story* is the point.

Each **Age Up** advances one year: stats drift with age (health and looks fade later in life; the young grow into themselves), any salary is paid, education milestones land, the people in your life age (and can pass away), a death check runs, and one to three **seeded life events** fire. Flavour events simply happen; real decisions pop up and ask you to choose, with consequences that ripple through the rest of the life. Between years you can spend the year on **activities** or with **people**.

**Stats.** Happiness, Health, Smarts, and Looks (0–100) are pushed by events, activities, age, illness, and your inborn traits. If Health hits zero, the life ends.

**Life stages.** Infant → Child → Teen → Young Adult → Adult → Senior gate which events can fire and what you can do — from playground scrapes and school dances to careers, houses, and grandchildren.

**People.** Parents and siblings to start; over a life you make friends, meet partners, marry, and have children — each a real person with a name, an age, and a relationship meter. From the People tab you can spend time, converse, compliment, gift, take a trip, ask for advice, propose, start a family, ask for money, or fall out — the interactions offered depend on who they are to you.

**Work & school.** School and graduation happen on their own; from 16+ you can take a part-time job. After high school you can enrol in **university** (and grad school) for a degree that unlocks better careers. Jobs sit on a ladder of 21 careers — you're hired by a seeded, smarts-weighted roll, then climb through **promotions** driven by your Smarts and years of tenure, with work events (raises, layoffs, being headhunted or fired) along the way.

**Money & assets.** A light layer — salary in, event and activity costs out, plus **assets** you can buy (cars, homes, luxuries) that build your **net worth**. It's one system among many, not the scoreboard.

**Health & illness.** Beyond the Health stat, you can pick up **conditions** — acute ones that clear on their own and chronic ones that quietly drain you each year until you seek **treatment**. They arrive more often as you age.

**Crime & justice.** From the Activities tab you can attempt crimes for a seeded payoff — but get caught and you'll serve a **prison** sentence that pauses your career and schooling and leaves you with a record.

**Traits & achievements.** You're born with personality **traits** that colour your life, and you rack up **achievements** for milestones — both shown on your Profile and in your legacy.

**Risk & luck.** Event selection, job hunts, illness, crime, and mortality all flow from one save seed, so a given life replays identically.

**Death & legacy.** Every life ends — by age, health, or misfortune — with a legacy screen recapping your years, final stats, wealth, and notable moments, then the option to start a new life.

**Surfaces.** Four tabs: **Life** (your stats, the Age Up button, and the life-story feed), **Activities**, **People**, and **Profile** (character details, stats, and app updates).

## License

MIT License. See [LICENSE](LICENSE).

## Agent Instructions

Local agent loader files are intentionally gitignored. On the user's machine, `AGENTS.md` and `CLAUDE.md` point agents to the canonical instruction and context files maintained outside this app repo.
