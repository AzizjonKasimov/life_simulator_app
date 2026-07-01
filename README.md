# Life Simulator App

A personal Android dashboard-style life simulator game. It side-loads onto a phone as a signed APK and updates itself from GitHub Releases — not a Play Store app.

## Status

v0.9.2. You start broke and in debt, and the goal is to grow your **net worth** — the always-visible score — toward **Financial Independence**, where passive income covers your weekly bill. Earn money from gigs, a steady job, and a side business, then save it, invest it, pay down debt, and buy life-changing assets. Interviews, client wins, market swings, and client churn are seeded gambles, so outcomes are uncertain but a given save replays identically.

Recent passes:

- **v0.7.0** reinvented the core loop around a real economy (savings, investments, assets, net worth as the score) and visible risk/luck — and retired the old daily-focus autopilot and timed-opportunity systems that made it play itself.
- **v0.8.0** is a legibility + UX pass: savings interest is now shown (lifetime earned + next-payday projection), an opt-in **Auto-Save & Invest** sweeps a chosen share of spare cash into savings/investments each payday, and the Actions tab gained category filters over compact, scannable rows.
- **v0.9.0** is a depth + goals pass: gig pay now diminishes with weekly use and the cost of living rises over time (real early pressure); a business must be *run*, not just built (clients churn and reputation decays each week), with a new top **Firm** tier; big-ticket money sinks arrive (buy your home, income-producing rentals and a franchise, a salary-boosting degree); and an eleven-rung **goal ladder** — ending in Financial Independence — plus a passive-income tracker give the open economy a spine of things to chase.
- **v0.9.1–v0.9.2** trimmed to taste: removed the header status label and the random decision pop-ups. Passive flavor events (a good night's sleep, a stress spike) stay.

## Product Direction

- Playable dashboard-first life simulation; net worth is the throughline.
- Phone-friendly surfaces for money, career, business, wellbeing, relationships, daily actions, and history.
- Local-first gameplay with no backend, accounts, telemetry, or Play Store release plumbing until explicitly needed.
- Keep it legible and player-driven: few meters at a time, money always has something to do, and the game never auto-plays the optimal move.

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

Saves use Room database version `5` and JSON schema version `5`, stored as `schemaVersion + stateJson` so simulator systems can evolve without frequent table rewrites. The v0.7.0 reinvention reset pre-0.7 saves (schema `4` → `5`); since then, new fields are added backward-compatibly, so existing saves carry across updates. A save whose schema doesn't match — or fails to decode — is treated as "no save" and the app starts a fresh life instead of crashing.

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
.\release.ps1 -VersionName 0.9.0 -VersionCode 9 -Notes "Depth and goals: diminishing gigs, rising costs, a business you must run, big-ticket money sinks, and a goal ladder to Financial Independence."
```

The script bumps the version, builds a signed APK, publishes `LifeSimulator-<version>.apk` to `AzizjonKasimov/life-simulator-app-releases`, and updates `version.json` for the in-app updater. The version bump in this repo is left uncommitted for review.

## Game Loop

Start as Alex Rivers, a 22-year-old who's unemployed, with $180 in cash, $350 in debt, and a weekly living bill due every seven days. Net worth — cash + savings + investments + asset resale − debt — starts negative, and growing it is the game.

Each day gives a limited time and energy budget to spend on actions across making money, career, business, wellbeing, and relationships. Gig work is a survival tool that pays less the more you lean on it in a week, so it can't replace real income. Ending the day settles the week whenever a bill is due: business income lands (and clients may churn), property income pays out, savings earn interest, investments swing, the living bill is paid (or rolls into debt), and any opt-in auto-allocation sweeps spare cash into savings or investments. Roughly monthly the cost of living creeps up, so a fixed income can't coast. The night may then roll a small passive event that nudges your stats.

**Money (the Money tab).** Park cash in savings for steady weekly interest (with the lifetime total and next payday shown), invest in Index funds, Stocks, or Crypto — each with its own seeded weekly swing — pay down debt, and buy assets that change how the rest of the game plays: a car (better gig pay), a nicer apartment, a gym, a laptop, a therapist, insurance, or a getaway. Bigger buys anchor the late game: a **home** that ends rent, income-producing **rentals** and a **franchise**, and a **degree** that permanently lifts your salary. A **Passive Income** tracker shows what arrives each week without spending a day on it, measured against your bill — the yardstick for Financial Independence. **Auto-Save & Invest** moves a set percentage of each payday's leftover cash automatically, so you don't have to allocate by hand.

**Career.** While unemployed, applying and prepping build a single job-search bar; once it's high enough, attending an interview is a seeded, skill-weighted gamble with visible odds — a hire, a callback, or a rejection. Once employed, shifts, check-ins, and study fill a promotion bar that delivers titled, better-paid level-ups.

**Business.** Launch a side hustle and grow it as a legible income engine, but keep tending it: sign clients (a seeded chance improved by reputation), run marketing, and reinvest to upgrade tiers (Side Hustle → Studio → Agency → Firm) for more clients and higher rates. Each week pays clients × rate scaled by reputation, minus overhead — and each week reputation slips and clients can churn, so a business you stop working slowly shrinks.

**Risk & luck.** The gambles that matter are seeded: interviews, client wins, client churn, and investment weeks all flow from one save seed, so risk is fair and a given save replays identically.

**Goals & Financial Independence.** A named goal ladder runs from breaking even and going debt-free through becoming a landlord and a homeowner to six figures, ending in Financial Independence — the week your passive income covers your bill. Goals only track and celebrate; they never play for you.

**Surfaces.** Five tabs: **Home** (status, cash flow with passive income, your next goal, career and business at a glance, what to do next, and the recent log), **Actions** (category filters over compact action rows), **Money**, **Stats** (the full goal checklist plus career, business, money, wellbeing, relationships, skills, and app updates), and **History**.

## License

MIT License. See [LICENSE](LICENSE).

## Agent Instructions

Local agent loader files are intentionally gitignored. On the user's machine, `AGENTS.md` and `CLAUDE.md` point agents to the canonical instruction and context files maintained outside this app repo.
