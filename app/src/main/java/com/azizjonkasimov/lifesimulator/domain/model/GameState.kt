package com.azizjonkasimov.lifesimulator.domain.model

data class Character(
    val name: String,
    val gender: Gender,
    val birthplace: String,
    val age: Int,
    val stats: Stats,
    val money: Int,
)

enum class LogKind {
    MILESTONE,
    EVENT,
    MONEY,
    HEALTH,
    RELATIONSHIP,
    SCHOOL,
    WORK,
    NEUTRAL,
}

/** One year-stamped line in the life feed. */
data class LogEntry(
    val age: Int,
    val text: String,
    val kind: LogKind = LogKind.NEUTRAL,
)

/** A signed change surfaced to the player after an action, for the feedback chips. */
data class StatChange(
    val label: String,
    val amount: Int,
    val positiveIsGood: Boolean = true,
)

/**
 * The entire simulation state for one life. Persisted as a JSON blob; the year is
 * derived from [Character.age], and the life stage from that.
 */
data class GameState(
    val character: Character,
    val relationships: List<Person>,
    val education: Education,
    val job: Job?,
    val flags: Set<String>,
    val eventsSeen: Set<String>,
    val pendingEventIds: List<String>,
    val activitiesUsed: Set<String>,
    val rngSeed: Long,
    val log: List<LogEntry>,
    val alive: Boolean = true,
    val causeOfDeath: String? = null,
    /** Years spent at the current job rung; feeds promotion cadence. Resets on any job change. */
    val jobYears: Int = 0,
    /** Active health conditions dragging on Health each year (M3). */
    val ailments: List<Ailment> = emptyList(),
    /** Current incarceration, or null when free (M3). */
    val prison: Prison? = null,
    /** Owned possessions — cars, property, luxuries — counted toward net worth (M3). */
    val assets: List<Asset> = emptyList(),
    /** Personality traits rolled at birth; nudge drift and gate events (M3). */
    val traits: Set<String> = emptySet(),
    /** Unlocked achievement ids (M3). */
    val achievements: Set<String> = emptySet(),
) {
    val age: Int get() = character.age
    val stage: LifeStage get() = LifeStage.forAge(character.age)

    /** Cash plus the worth of everything you own. */
    val netWorth: Int get() = character.money + assets.sumOf { it.value }

    val inPrison: Boolean get() = prison != null
}
