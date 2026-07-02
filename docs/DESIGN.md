# Life Simulator — Game Design (BitLife-style rebuild)

> **Status:** M1–M4 shipped. The roadmap is complete: depth (M3), generations, and the
> M4 polish/balancing/event pass are all in. This document is the single target every
> build session works toward. When something here changes, change it *here first*.

## 1. Vision

A text-driven **life** simulator. You are *born* as a person somewhere in the world.
Each tap of **Age Up** lives one year: your stats shift, life events *happen to you*
and ask you to choose, and over a lifetime you build (or wreck) an education, a
career, relationships, health, and wealth — until you die and receive a legacy
summary. Seeded, replayable, and full of small human stories.

This is deliberately a **different game** from the v0.1–v0.9 finance simulator, which
lives on in git history and the released APKs. We keep that app's Android *chassis*
and replace its *engine*.

### Why this is a rebuild, not an extension

The two games have incompatible spines:

| | Finance sim (v0.9) | Life sim (this design) |
|---|---|---|
| Unit of time | a **day** with a time/energy budget | a **year** of your life |
| Core verb | spend actions to optimise net worth | **Age Up** and react to what happens |
| Point of the game | one number (net worth) goes up | accumulate a *life* of events & relationships |
| Content shape | few deep systems | **broad** pool of authored events |

You cannot stack the second on the first, which is why iterative edits to the finance
app kept missing the target.

## 2. Design pillars

1. **The year is the heartbeat.** Everything happens on Age Up. Between years you may
   *choose* activities, but time only moves when you age.
2. **Breadth over depth.** The feeling of "a life" comes from the *volume and variety*
   of authored events, not from any one deep system. Content is the product.
3. **Choices with consequences that compound.** Events offer 2–4 choices; effects ripple
   across the rest of the life (a flag set at 16 pays off at 40).
4. **People are real.** Relationships are *entities* with names, ages, and their own
   arcs — not a meter.
5. **Seeded and fair.** One seed per life; all rolls derive from it, so a life replays
   identically. (Carried over from what the finance game got right.)
6. **Legible, player-driven.** The game never plays the optimal move for you. Kept from
   the prior game's hard-won lessons — no autopilot.

## 3. Core loop

```
Character is born (age 0)
  └─> [ Age Up ] ──────────────────────────────────────────────┐
        1. age += 1, recompute life stage                       │
        2. annual drift: stats age, salary paid, people age     │
        3. death check (age- and health-weighted)               │
        4. build eligible event pool → weighted-pick 1–3        │
        5. present events; player picks a choice per event      │
        6. apply effects; append to the life log (the feed)     │
        7. resolve timed transitions (graduations, etc.)        │
        8. persist                                              │
      (between Age Ups: choose Activities, interact with People)─┘
  └─> death ──> Legacy summary ──> New Life
```

## 4. Character & stats

```kotlin
enum class Gender { MALE, FEMALE }

data class Stats(          // each 0..100
    val happiness: Int,
    val health: Int,
    val smarts: Int,
    val looks: Int,
)

data class Character(
    val name: String,
    val gender: Gender,
    val birthplace: String,   // e.g. "Seoul, South Korea"
    val age: Int,
    val stats: Stats,
    val money: Int,           // can go negative (debt) later; starts 0 as a child
)
```

The **four core stats** are the BitLife-classic set. They drift each year (health
declines with age; looks peak then fade) and are pushed by events, activities, and
assets.

## 5. Life stages

Stages gate agency and which events can fire.

| Stage | Ages | Agency |
|---|---|---|
| `INFANT` | 0–4 | none — events happen *to* you (family, health, personality seeds) |
| `CHILD` | 5–12 | school starts, friendships, limited activities |
| `TEEN` | 13–17 | grades, part-time jobs, dating begins, more choices |
| `YOUNG_ADULT` | 18–25 | university or work, moving out, serious relationships |
| `ADULT` | 26–64 | full career, marriage, children, assets |
| `SENIOR` | 65+ | retirement, declining health, grandchildren, legacy |

Death can occur at any age via the health/age check or a fatal event; probability
climbs steeply in `SENIOR`.

## 6. Relationships (entities)

```kotlin
enum class RelationType { MOTHER, FATHER, SIBLING, FRIEND, PARTNER, SPOUSE, CHILD, COWORKER, PET }

data class Person(
    val id: String,
    val name: String,
    val relation: RelationType,
    val age: Int,
    val alive: Boolean = true,
    val relationship: Int,     // 0..100
    val stats: Stats? = null,  // partners/children carry their own
)
```

- **At birth:** generate a mother, a father, and 0–2 siblings.
- **Over a life:** make friends, meet partners, marry, have children; people age and
  can die (their own events fire — a parent's illness, a friend drifting away).
- **Interactions** (from the People tab): *Spend time* (+relationship), *Have a
  conversation* (rolls a small relationship event), later *Compliment / Insult / Ask
  for money / Propose*. M1 ships *Spend time* + *Conversation*.

## 7. Education & career

**Education** runs mostly automatically by stage, nudged by Smarts and study activities.

```kotlin
enum class EducationLevel { NONE, PRIMARY, SECONDARY, UNIVERSITY, GRADUATE }
```

- School during `CHILD`/`TEEN`; grades track Smarts.
- At 18: optionally enrol in **university** (costs money + years, raises Smarts, unlocks
  gated careers), yielding a degree.

**Career**

```kotlin
data class Job(
    val title: String,
    val field: JobField,
    val salaryPerYear: Int,
    val level: Int,          // promotion rung
    val requiresDegree: Boolean,
)
```

- **Apply** to eligible jobs (seeded, weighted by Smarts / education / Looks per field).
- Employment pays `salaryPerYear` each Age Up.
- Promotions raise level, title, and salary; work-related events and choices feed them.
- Teens can take part-time jobs; real careers open at `YOUNG_ADULT`.

## 8. Health & death

- **Health** stat drifts down with age, up with Gym / Doctor / good choices.
- **Illness events** are age-gated with rising probability; a Doctor visit or treatment
  choice mitigates.
- **Death** when `health <= 0`, via a fatal event, or the age-weighted annual roll.
  Produces a **cause of death** string for the legacy screen.

## 9. Money & assets (kept deliberately light)

> We have deep finance code from the old game. **Resist porting it wholesale** — a
> BitLife money loop is light. Money is one system among many, not the scoreboard.

- Money accrues from salary (yearly) and shrinks via chosen purchases and event costs.
- **Assets** (later milestones): a car, a house, possessions — mostly flavour + small
  stat effects and a resale value. A house can end "rent" style drains if we add them.
- A richer economy (investments, passive income) can be *reintroduced later* as a
  simplified **yearly** system if playtesting wants it — never as the daily engine.

## 10. Activities (the between-years menu)

Chosen from the Activities tab; each may cost money and moves stats. M1 ships ~5.

| Activity | Effect (illustrative) | Notes |
|---|---|---|
| Gym | Health +, Looks + | small cost |
| Library / Study | Smarts + | boosts school/uni |
| Doctor | Health + | costs money |
| Meditate | Happiness +, — Stress | free |
| Go Out / Party | Happiness + | small risk event |
| _(later)_ Date, Crime, Vacation, Adopt a pet, … | — | M2+ |

## 11. The event engine (the heart)

This is the system that makes it feel alive. Everything else is scaffolding for it.

```kotlin
enum class EventCategory { FAMILY, SCHOOL, WORK, ROMANCE, HEALTH, MONEY, CRIME, RANDOM }

data class LifeEvent(
    val id: String,
    val category: EventCategory,
    val minAge: Int = 0,
    val maxAge: Int = 120,
    val stages: Set<LifeStage> = LifeStage.entries.toSet(),
    val condition: (GameState) -> Boolean = { true }, // e.g. { it.job != null }
    val weight: Int = 10,          // relative fire chance
    val oneShot: Boolean = false,  // fire at most once per life
    val prompt: String,            // "Your teacher accuses you of cheating."
    val choices: List<EventChoice> // empty => a single "OK" (flavour event)
)

data class EventChoice(
    val label: String,             // "Deny it"
    val effects: List<Effect>,
    val resultText: String,        // "The teacher lets it slide. Happiness +5."
)

sealed interface Effect {
    data class StatDelta(val stat: Stat, val amount: Int) : Effect
    data class MoneyDelta(val amount: Int) : Effect
    data class RelationshipDelta(val personId: String?, val relation: RelationType?, val amount: Int) : Effect
    data class AddPerson(val relation: RelationType) : Effect
    data class SetFlag(val flag: String) : Effect
    data class StartJob(val jobId: String) : Effect
    // extend as systems grow
}
```

**Firing each Age Up:** filter the catalog by age/stage/condition and (for `oneShot`)
by `eventsSeen`; weighted-random pick 1–3; present; apply the chosen `EventChoice`.
Flavour events (no choices) render as a single log line with an OK.

**Flags** carry story state cheaply (`"expelled"`, `"married"`, `"felon"`), letting
later events gate on earlier choices — this is where "consequences compound."

## 12. Determinism & seeding

One `rngSeed` per life. A per-year, per-purpose derived RNG (`hash(seed, year, salt)`)
drives event selection and outcomes, so a life is reproducible and fair — the property
the finance game already valued.

## 13. New `GameState`

```kotlin
data class GameState(
    val character: Character,
    val stage: LifeStage,
    val relationships: List<Person>,
    val education: Education,
    val job: Job?,
    val assets: List<String>,
    val flags: Set<String>,
    val eventsSeen: Set<String>,     // for oneShot events
    val rngSeed: Long,
    val log: List<LogEntry>,         // the life feed (year-stamped)
    val alive: Boolean = true,
    val causeOfDeath: String? = null,
)
```

## 14. Content manifest (the volume that makes it feel like a life)

| Content | M1 (thin slice) | Full-game target |
|---|---|---|
| Life events | 40–50 | **200+** |
| — Infant/Child | ~10 | ~25 |
| — Teen | ~12 | ~40 |
| — Young Adult | ~10 | ~40 |
| — Adult | ~8 | ~60 |
| — Senior | ~5 | ~25 |
| — Cross-stage / random | ~5 | ~30 |
| Jobs | ~6 entry | ~40 across fields |
| Activities | ~5 | ~15 |
| Relationship interactions | 2 | ~10 |

Authoring events in **bulk** is its own deliberate task — not a side effect of tuning.
This is the single biggest lever on how alive the game feels.

## 15. UI surfaces (reuse `UiKit`)

Bottom-nav tabs (Compose, reusing existing components/theme):

- **Life** — character header (name, age, stage, money) + four stat bars + the scrolling
  **life feed** of year-stamped event cards + a large **Age Up** button. Event choices
  render as a card/modal with a button per option. *This screen is the game.*
- **Activities** — the between-years menu (§10).
- **People** — list of `Person`s; tap to interact.
- **Profile** — full character detail, education/career, full life history. (Assets/Money
  surface folds in here until it earns its own tab.)
- **Character creation** — replaces the current `NewLifeScreen`: name, gender, and a
  rolled starting family/birthplace, then "Start Life."
- **Legacy screen** — on death: age, cause, final stats, wealth, number of
  relationships, and highlights, then "Start New Life."

## 16. Persistence

Reuse the existing blob store unchanged in shape:

- `GameStateEntity(id, schemaVersion, stateJson)` — keep.
- New `GameStateJsonCodec` for the new `GameState`; **bump `SCHEMA_VERSION` 5 → 6**.
- `SaveRepository.safeToDomain()` already returns `null` on a schema mismatch or decode
  failure, so any existing v0.9 finance save is transparently treated as "no save" and
  the player starts a fresh life. `AppContainer` already uses
  `fallbackToDestructiveMigration()`. **No migration code required.**
- This is the sanctioned *truly-incompatible* change (a different game), so retiring the
  old save is correct — not the additive-field rule we use within a game.

## 17. Keep vs. replace (file-level)

**Keep (the chassis — game-agnostic, light rewiring):**

- `MainActivity`, `LifeSimulatorApplication`, `AppContainer`
- `data/LifeSimulatorDatabase`, `GameStateDao`, `GameStateEntity`, `SaveRepository`
- `update/UpdateManager`, `update/UpdatePrompt` (the GitHub updater)
- `ui/theme/LifeSimulatorTheme`, `ui/UiKit` (extend with life-sim components)
- The shell pattern in `ui/LifeSimulatorApp` (loading → new-life → active + bottom nav)
- The `LifeSimulatorViewModel` *pattern* (observe save flow → dispatch engine op →
  rebuild UI state); rewrite the specific operations.

**Replace (finance engine, model, and screens):**

- `domain/model/*` — GameState, FinanceState, EconomyState, CareerState, BusinessState,
  JobSearchState, CoreStats, SkillSet, etc. → the new life-sim model (§4–§13).
- `domain/engine/LifeSimulationEngine` (the ~1.3k-line daily engine) → the new year-based
  engine (§3).
- `domain/engine/ActionCatalog`, `AssetCatalog`, `GoalCatalog`, `EventCatalog` → new
  `EventCatalog`, `JobCatalog`, `ActivityCatalog`, `NameCatalog`.
- `data/GameStateJsonCodec` → new codec (schema 6).
- `ui/DashboardScreen`, `MoneyScreen`, `ProgressScreen`, `ActionsScreen`, `HistoryScreen`
  → new **Life**, **Activities**, **People**, **Profile**, **Legacy** screens.

## 18. Build roadmap

> **Progress (2026-07-02):** M1 shipped as v0.10.0, M2 as v0.11.0, M3 depth as v0.12.0,
> and M4 (generations + polish) as v0.13.0. The roadmap is complete; further work is
> content and tuning on this same spine.

- **M1 — Playable skeleton (the thin slice).** Character creation, four stats, life
  stages, the Age Up loop, the event engine + ~40–50 events, parents + siblings, ~5
  activities, death + legacy, save/load on the new schema, new nav + Life feed screen.
  *Goal: a real BitLife-in-miniature you can play and react to.* ✅ v0.10.0
- **M2 — Breadth.** University + degrees, ~20 jobs with promotions, romance → marriage →
  children, more activities, People interactions, ~120 total events. *Goal: it starts to
  feel like BitLife.* ✅ v0.11.0
- **M3 — Depth & spice.** Health/illness system, crime + jail, assets (house/car),
  richer relationship interactions, traits, achievements, 200+ events. ✅ v0.12.0
- **M4 — Polish + generations.** **Generations** (continue as your child on death, with a
  taxed estate inheritance and the family tree reshaped around the heir), an estate-tax
  balance lever against a dynasty money-printer, a further event-authoring pass (~229
  events), and UI (bloodline choice on the legacy screen, generation shown on Profile).
  ✅ v0.13.0

## 19. Design decisions (owner: you)

**Decided (2026-07-01):**

1. **Tone / content rating → PG‑13.** Real life with teeth (breakups, firings, minor
   crime, illness, death) but no explicit drugs/sex/gore. The voice for all event writing.
2. **Setting → Real-world.** Born in real countries/cities with culturally-fitting names.

**Still open (defaults in bold are my recommendation; not blocking M1):**

3. **Aesthetic.** **Keep the current dark "command-center" theme** · lighter/more playful.
4. **Generations.** ✅ **Decided & built (v0.13.0):** you can continue as your child on
   death (optional — a fresh life is always offered too).
5. **Money depth.** **Keep it light early** · reintroduce a simplified yearly economy later.
```
