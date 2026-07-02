package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Effect
import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LogKind
import com.azizjonkasimov.lifesimulator.domain.model.Stat

/** Groups the Activities menu so the growing list of things to do stays navigable. */
enum class ActivityCategory(val label: String) {
    WELLBEING("Health & Wellbeing"),
    SOCIAL("Social"),
    SCHOOL_WORK("School & Work"),
    LIFESTYLE("Lifestyle & Assets"),
    CRIME("Crime"),
    PRISON("Prison"),
}

/**
 * A between-years action the player can choose. Each may be done once per year
 * (tracked by [GameState.activitiesUsed]) and costs money up front. [requires] is a
 * structural gate — when false the activity is hidden entirely (e.g. university once
 * you already hold a degree). [category] only groups the menu.
 */
data class Activity(
    val id: String,
    val label: String,
    val description: String,
    val logText: String,
    val cost: Int = 0,
    val minAge: Int = 0,
    val category: ActivityCategory = ActivityCategory.WELLBEING,
    val effects: List<Effect> = emptyList(),
    val requiresUnemployed: Boolean = false,
    val logKind: LogKind = LogKind.NEUTRAL,
    val requires: (GameState) -> Boolean = { true },
    /** Engine-handled special behaviour, e.g. "find_job" or "crime:burglary". */
    val special: String? = null,
)

object ActivityCatalog {
    private val base = listOf(
        Activity(
            id = "gym",
            label = "Go to the Gym",
            description = "Work out to build health and looks.",
            logText = "Spent the year getting in shape at the gym.",
            cost = 15,
            minAge = 8,
            category = ActivityCategory.WELLBEING,
            effects = listOf(
                Effect.StatDelta(Stat.HEALTH, 4),
                Effect.StatDelta(Stat.LOOKS, 2),
            ),
            logKind = LogKind.HEALTH,
        ),
        Activity(
            id = "doctor",
            label = "See a Doctor",
            description = "Get a check-up and a general tune-up.",
            logText = "Went to the doctor for a check-up.",
            cost = 60,
            minAge = 0,
            category = ActivityCategory.WELLBEING,
            effects = listOf(Effect.StatDelta(Stat.HEALTH, 8)),
            logKind = LogKind.HEALTH,
        ),
        Activity(
            id = "treatment",
            label = "Get Treatment",
            description = "Seek real medical care for your conditions.",
            logText = "",
            cost = 1200,
            minAge = 0,
            category = ActivityCategory.WELLBEING,
            logKind = LogKind.HEALTH,
            special = "treat",
            requires = { it.ailments.isNotEmpty() },
        ),
        Activity(
            id = "meditate",
            label = "Meditate",
            description = "Calm your mind and lift your mood.",
            logText = "Practiced meditation to clear your head.",
            cost = 0,
            minAge = 8,
            category = ActivityCategory.WELLBEING,
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
            category = ActivityCategory.SOCIAL,
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
            category = ActivityCategory.SOCIAL,
            effects = listOf(
                Effect.StatDelta(Stat.HAPPINESS, 6),
                Effect.StatDelta(Stat.HEALTH, -1),
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
            category = ActivityCategory.SOCIAL,
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
            category = ActivityCategory.SOCIAL,
            logKind = LogKind.RELATIONSHIP,
            special = "adopt_pet",
        ),
        Activity(
            id = "study",
            label = "Study Hard",
            description = "Hit the books to raise your smarts.",
            logText = "Buried yourself in books this year.",
            cost = 0,
            minAge = 6,
            category = ActivityCategory.SCHOOL_WORK,
            effects = listOf(
                Effect.StatDelta(Stat.SMARTS, 4),
                Effect.StatDelta(Stat.HAPPINESS, -1),
            ),
            logKind = LogKind.SCHOOL,
        ),
        Activity(
            id = "enroll_university",
            label = "Enrol in University",
            description = "A degree opens up better careers. ~\$4,000/yr for four years.",
            logText = "",
            cost = 0,
            minAge = 18,
            category = ActivityCategory.SCHOOL_WORK,
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
            category = ActivityCategory.SCHOOL_WORK,
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
            category = ActivityCategory.SCHOOL_WORK,
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
            category = ActivityCategory.SCHOOL_WORK,
            logKind = LogKind.WORK,
            special = "quit_job",
            requires = { it.job != null },
        ),
        Activity(
            id = "vacation",
            label = "Take a Vacation",
            description = "Get away from it all and recharge.",
            logText = "Took a proper vacation and came back refreshed.",
            cost = 1500,
            minAge = 18,
            category = ActivityCategory.LIFESTYLE,
            effects = listOf(
                Effect.StatDelta(Stat.HAPPINESS, 8),
                Effect.StatDelta(Stat.HEALTH, 3),
            ),
            logKind = LogKind.NEUTRAL,
        ),
        // ---- Prison-only -------------------------------------------------
        Activity(
            id = "prison_workout",
            label = "Work Out in the Yard",
            description = "Stay strong while you serve your time.",
            logText = "Spent the year lifting in the prison yard.",
            cost = 0,
            category = ActivityCategory.PRISON,
            effects = listOf(
                Effect.StatDelta(Stat.HEALTH, 3),
                Effect.StatDelta(Stat.HAPPINESS, 1),
            ),
            logKind = LogKind.HEALTH,
            requires = { it.inPrison },
        ),
        Activity(
            id = "good_behavior",
            label = "Keep Your Head Down",
            description = "Behave, and you might shave time off your sentence.",
            logText = "",
            cost = 0,
            category = ActivityCategory.PRISON,
            logKind = LogKind.NEUTRAL,
            special = "good_behavior",
            requires = { it.inPrison },
        ),
    )

    private val crimes = CrimeCatalog.crimes.map { crime ->
        Activity(
            id = "crime_${crime.id}",
            label = crime.label,
            description = crime.description,
            logText = "",
            cost = 0,
            minAge = crime.minAge,
            category = ActivityCategory.CRIME,
            logKind = LogKind.EVENT,
            special = "crime:${crime.id}",
        )
    }

    private val purchases = AssetCatalog.specs.map { spec ->
        Activity(
            id = "buy_${spec.id}",
            label = "Buy a ${spec.name}",
            description = "Add a ${spec.kind.label.lowercase()} to your name.",
            logText = "",
            cost = spec.price,
            minAge = spec.minAge,
            category = ActivityCategory.LIFESTYLE,
            logKind = LogKind.MONEY,
            special = "buy:${spec.id}",
        )
    }

    val all: List<Activity> = base + crimes + purchases

    fun byId(id: String): Activity? = all.find { it.id == id }
}
