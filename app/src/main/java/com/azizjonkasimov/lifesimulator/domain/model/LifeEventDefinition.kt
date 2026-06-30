package com.azizjonkasimov.lifesimulator.domain.model

/**
 * A life event. If [choices] is empty it is a *passive* event: it fires at the
 * start of a day and applies [effect] automatically. If [choices] is non-empty
 * it is a *decision*: the game pauses and asks the player to pick, and the
 * chosen outcome (sometimes a gamble) is applied instead.
 */
data class LifeEventDefinition(
    val id: String,
    val title: String,
    val description: String,
    val condition: (GameState) -> Boolean,
    val effect: ActionEffect = ActionEffect(),
    val choices: List<EventChoice> = emptyList(),
) {
    val isDecision: Boolean
        get() = choices.isNotEmpty()
}

/**
 * One option in a decision. [cashCost] is paid up front (and gates the option if
 * the player cannot afford it). [outcomes] holds one result for a sure thing, or
 * several weighted results for a gamble resolved by a seeded roll.
 */
data class EventChoice(
    val id: String,
    val label: String,
    val description: String = "",
    val cashCost: Int = 0,
    val outcomes: List<EventOutcome>,
) {
    val isGamble: Boolean
        get() = outcomes.size > 1
}

/** A single possible result of an [EventChoice]. */
data class EventOutcome(
    val weight: Int = 1,
    val message: String,
    val good: Boolean = true,
    val cashDelta: Int = 0,
    val effect: ActionEffect = ActionEffect(),
)

/** Marks that a decision is waiting on the player; the dialog blocks until resolved. */
data class PendingDecision(
    val eventId: String,
)
