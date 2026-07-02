package com.azizjonkasimov.lifesimulator.domain.model

enum class EventCategory(val label: String) {
    FAMILY("Family"),
    SCHOOL("School"),
    WORK("Work"),
    ROMANCE("Romance"),
    HEALTH("Health"),
    MONEY("Money"),
    CRIME("Crime"),
    RANDOM("Life"),
}

/** A single consequence of a choice (or of a flavour event). */
sealed interface Effect {
    data class StatDelta(val stat: Stat, val amount: Int) : Effect
    data class MoneyDelta(val amount: Int) : Effect
    /** Nudge everyone matching [relation] (or a specific [personId]) by [amount]. */
    data class RelationshipDelta(
        val amount: Int,
        val relation: RelationType? = null,
        val personId: String? = null,
    ) : Effect
    data class AddFlag(val flag: String) : Effect
    data class StartJob(val jobId: String) : Effect
    /** Bring a new person into your life (partner, child, friend, pet); name is generated. */
    data class AddPerson(val relation: RelationType, val relationship: Int = 60) : Effect
    /** Change everyone of relation [from] into [to] — e.g. PARTNER → SPOUSE on marriage. */
    data class PromoteRelation(val from: RelationType, val to: RelationType) : Effect
    /** Remove everyone of a relation from your life — a breakup or divorce. */
    data class RemovePeople(val relation: RelationType) : Effect
    /** Bump your current job up one rung, if there is one. */
    data object PromoteJob : Effect
    /** Lose your job (fired or laid off). */
    data object LoseJob : Effect
    /** Contract a health condition by its [HealthCatalog] id. */
    data class AddAilment(val id: String) : Effect
    /** Clear every active health condition — a clean bill of health. */
    data object CureAilments : Effect
    /** Go to prison for [years]. Ends any job. */
    data class Imprison(val years: Int) : Effect
    /** Walk free. */
    data object Release : Effect
    /** Acquire an asset by its [AssetCatalog] spec id (value/happiness from the spec). */
    data class AddAsset(val id: String) : Effect
}

data class EventChoice(
    val label: String,
    val resultText: String,
    val effects: List<Effect> = emptyList(),
)

/**
 * An authored life event. A single-choice (or zero-choice) event is *flavour* and
 * applies automatically on Age Up; an event with two or more choices is a real
 * decision presented to the player.
 */
data class LifeEvent(
    val id: String,
    val category: EventCategory,
    val prompt: String,
    val choices: List<EventChoice>,
    val minAge: Int = 0,
    val maxAge: Int = 120,
    val stages: Set<LifeStage> = LifeStage.entries.toSet(),
    val weight: Int = 10,
    val oneShot: Boolean = true,
    val condition: (GameState) -> Boolean = { true },
    /** Prison events fire only while incarcerated; everything else only while free. */
    val prisonOnly: Boolean = false,
) {
    val interactive: Boolean get() = choices.size >= 2
}
