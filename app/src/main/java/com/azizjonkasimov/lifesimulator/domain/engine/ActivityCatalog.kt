package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Effect
import com.azizjonkasimov.lifesimulator.domain.model.LogKind
import com.azizjonkasimov.lifesimulator.domain.model.Stat

/**
 * A between-years action the player can choose. Each may be done once per year
 * (tracked by [GameState.activitiesUsed]) and costs money up front.
 */
data class Activity(
    val id: String,
    val label: String,
    val description: String,
    val logText: String,
    val cost: Int = 0,
    val minAge: Int = 0,
    val effects: List<Effect> = emptyList(),
    val requiresUnemployed: Boolean = false,
    val logKind: LogKind = LogKind.NEUTRAL,
    /** Engine-handled special behaviour, e.g. "find_job". */
    val special: String? = null,
)

object ActivityCatalog {
    val all = listOf(
        Activity(
            id = "gym",
            label = "Go to the Gym",
            description = "Work out to build health and looks.",
            logText = "Spent the year getting in shape at the gym.",
            cost = 15,
            minAge = 8,
            effects = listOf(
                Effect.StatDelta(Stat.HEALTH, 4),
                Effect.StatDelta(Stat.LOOKS, 2),
            ),
            logKind = LogKind.HEALTH,
        ),
        Activity(
            id = "study",
            label = "Study Hard",
            description = "Hit the books to raise your smarts.",
            logText = "Buried yourself in books this year.",
            cost = 0,
            minAge = 6,
            effects = listOf(
                Effect.StatDelta(Stat.SMARTS, 4),
                Effect.StatDelta(Stat.HAPPINESS, -1),
            ),
            logKind = LogKind.SCHOOL,
        ),
        Activity(
            id = "doctor",
            label = "See a Doctor",
            description = "Get a check-up and treatment.",
            logText = "Went to the doctor for a check-up.",
            cost = 60,
            minAge = 0,
            effects = listOf(Effect.StatDelta(Stat.HEALTH, 8)),
            logKind = LogKind.HEALTH,
        ),
        Activity(
            id = "meditate",
            label = "Meditate",
            description = "Calm your mind and lift your mood.",
            logText = "Practiced meditation to clear your head.",
            cost = 0,
            minAge = 8,
            effects = listOf(Effect.StatDelta(Stat.HAPPINESS, 5)),
            logKind = LogKind.NEUTRAL,
        ),
        Activity(
            id = "night_out",
            label = "Night Out",
            description = "Let loose with friends for a mood boost.",
            logText = "Enjoyed a big night out.",
            cost = 45,
            minAge = 16,
            effects = listOf(
                Effect.StatDelta(Stat.HAPPINESS, 6),
                Effect.StatDelta(Stat.HEALTH, -1),
            ),
            logKind = LogKind.NEUTRAL,
        ),
        Activity(
            id = "find_job",
            label = "Look for a Job",
            description = "Search and apply for work.",
            logText = "",
            cost = 0,
            minAge = 16,
            requiresUnemployed = true,
            logKind = LogKind.WORK,
            special = "find_job",
        ),
    )

    fun byId(id: String): Activity? = all.find { it.id == id }
}
