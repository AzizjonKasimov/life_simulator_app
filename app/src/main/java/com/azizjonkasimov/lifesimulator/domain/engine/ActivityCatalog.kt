package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Effect
import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LogKind
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
import com.azizjonkasimov.lifesimulator.domain.model.Stat

/**
 * A between-years action the player can choose. Each may be done once per year
 * (tracked by [GameState.activitiesUsed]) and costs money up front. [requires] is a
 * structural gate — when false the activity is hidden entirely (e.g. university once
 * you already hold a degree).
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
    val requires: (GameState) -> Boolean = { true },
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
            id = "volunteer",
            label = "Volunteer",
            description = "Give back to your community.",
            logText = "Spent the year volunteering.",
            cost = 0,
            minAge = 13,
            effects = listOf(
                Effect.StatDelta(Stat.HAPPINESS, 3),
                Effect.StatDelta(Stat.SMARTS, 1),
            ),
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
            id = "vacation",
            label = "Take a Vacation",
            description = "Get away from it all and recharge.",
            logText = "Took a proper vacation and came back refreshed.",
            cost = 1500,
            minAge = 18,
            effects = listOf(
                Effect.StatDelta(Stat.HAPPINESS, 8),
                Effect.StatDelta(Stat.HEALTH, 3),
            ),
            logKind = LogKind.NEUTRAL,
        ),
        Activity(
            id = "date",
            label = "Go on a Date",
            description = "Look for love — or make time for the one you have.",
            logText = "",
            cost = 40,
            minAge = 16,
            logKind = LogKind.RELATIONSHIP,
            special = "date",
        ),
        Activity(
            id = "adopt_pet",
            label = "Adopt a Pet",
            description = "Bring home a companion.",
            logText = "",
            cost = 200,
            minAge = 10,
            logKind = LogKind.RELATIONSHIP,
            special = "adopt_pet",
        ),
        Activity(
            id = "enroll_university",
            label = "Enrol in University",
            description = "A degree opens up better careers. ~\$4,000/yr for four years.",
            logText = "",
            cost = 0,
            minAge = 18,
            logKind = LogKind.SCHOOL,
            special = "enroll_university",
            requires = { !it.education.isEnrolled && it.education.level == EducationLevel.SECONDARY && "hs_grad" in it.flags },
        ),
        Activity(
            id = "enroll_grad",
            label = "Enrol in Grad School",
            description = "A graduate degree for the top of your field. ~\$8,000/yr for two years.",
            logText = "",
            cost = 0,
            minAge = 21,
            logKind = LogKind.SCHOOL,
            special = "enroll_grad",
            requires = { !it.education.isEnrolled && it.education.level == EducationLevel.UNIVERSITY },
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
        Activity(
            id = "quit_job",
            label = "Quit Your Job",
            description = "Walk away — then you can look for something new.",
            logText = "You quit your job.",
            cost = 0,
            minAge = 16,
            logKind = LogKind.WORK,
            special = "quit_job",
            requires = { it.job != null },
        ),
    )

    fun byId(id: String): Activity? = all.find { it.id == id }
}
