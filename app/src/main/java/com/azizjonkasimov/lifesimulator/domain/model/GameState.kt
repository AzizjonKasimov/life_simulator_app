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
) {
    val age: Int get() = character.age
    val stage: LifeStage get() = LifeStage.forAge(character.age)
}
