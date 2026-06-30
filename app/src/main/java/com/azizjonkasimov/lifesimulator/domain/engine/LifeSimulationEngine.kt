package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition
import com.azizjonkasimov.lifesimulator.domain.model.SimulationResult
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet

class LifeSimulationEngine(
    private val actions: List<DailyActionDefinition> = ActionCatalog.actions,
    private val events: List<LifeEventDefinition> = EventCatalog.events,
) {
    fun startNewLife(archetype: LifeArchetype): GameState {
        val preset = when (archetype) {
            LifeArchetype.STUDENT -> StartingPreset(
                stats = CoreStats(health = 72, mood = 64, energy = 86, stress = 32, social = 58),
                skills = SkillSet(knowledge = 18, fitness = 8, career = 4),
                money = 220,
                jobTitle = "Part-time student",
            )
            LifeArchetype.JUNIOR_WORKER -> StartingPreset(
                stats = CoreStats(health = 68, mood = 60, energy = 78, stress = 42, social = 48),
                skills = SkillSet(knowledge = 12, fitness = 9, career = 20),
                money = 520,
                jobTitle = "Junior associate",
            )
            LifeArchetype.FREELANCER -> StartingPreset(
                stats = CoreStats(health = 66, mood = 62, energy = 82, stress = 48, social = 42),
                skills = SkillSet(knowledge = 16, fitness = 7, career = 14),
                money = 360,
                jobTitle = "Independent freelancer",
            )
        }

        return GameState(
            day = 1,
            archetype = archetype,
            stats = preset.stats,
            skills = preset.skills,
            money = preset.money,
            careerLevel = 1,
            jobTitle = preset.jobTitle,
            timeRemaining = DAILY_TIME_BUDGET,
            rngSeed = seedFor(archetype),
            history = listOf(
                HistoryEntry(
                    day = 1,
                    title = "New life started",
                    detail = "You began as a ${archetype.displayName}.",
                    kind = HistoryKind.SYSTEM,
                ),
            ),
        )
    }

    fun actionAvailability(state: GameState): List<ActionAvailability> =
        actions.map { action ->
            val reason = unavailableReason(state, action)
            ActionAvailability(
                action = action,
                isAvailable = reason == null,
                reason = reason,
            )
        }

    fun performAction(state: GameState, actionId: String): SimulationResult {
        val action = actions.firstOrNull { it.id == actionId }
            ?: return SimulationResult(
                state = state,
                success = false,
                messages = emptyList(),
                errorMessage = "Unknown action.",
            )

        val reason = unavailableReason(state, action)
        if (reason != null) {
            return SimulationResult(
                state = state,
                success = false,
                messages = emptyList(),
                errorMessage = reason,
            )
        }

        val applied = applyEffect(
            state = state.copy(
                timeRemaining = (state.timeRemaining - action.timeCost).coerceAtLeast(0),
                money = state.money - action.moneyCost,
                stats = state.stats.copy(energy = state.stats.energy - action.energyCost),
            ),
            effect = action.effect,
        )
        val updated = applied.copy(
            careerLevel = careerLevelFor(applied.skills.career),
            jobTitle = jobTitleFor(applied.archetype, careerLevelFor(applied.skills.career)),
            history = (applied.history + HistoryEntry(
                day = applied.day,
                title = action.title,
                detail = summarizeAction(action),
                kind = HistoryKind.ACTION,
            )).trimHistory(),
        )

        return SimulationResult(
            state = updated,
            success = true,
            messages = listOf("${action.title} completed."),
        )
    }

    fun advanceDay(state: GameState): SimulationResult {
        val weeklyCost = if (state.day % 7 == 0) weeklyCostFor(state.archetype) else 0
        val afterCosts = state.copy(money = state.money - weeklyCost)
        val debtPenalty = if (afterCosts.money < 0) 8 else 0
        val recovery = ActionEffect(
            healthDelta = if (afterCosts.stats.stress > 75) -4 else 2,
            moodDelta = if (afterCosts.stats.stress > 70) -5 else 2,
            energyDelta = 46 - (afterCosts.stats.stress / 6),
            stressDelta = -8 + debtPenalty,
            socialDelta = -2,
        )
        val recovered = applyEffect(afterCosts, recovery)
        val eventRoll = rollEvent(recovered)
        val withEvent = eventRoll.event?.let { applyEffect(recovered, it.effect) } ?: recovered
        val nextDay = withEvent.copy(
            day = withEvent.day + 1,
            timeRemaining = DAILY_TIME_BUDGET,
            rngSeed = eventRoll.nextSeed,
            history = buildList {
                addAll(withEvent.history)
                if (weeklyCost > 0) {
                    add(
                        HistoryEntry(
                            day = state.day,
                            title = "Weekly living costs",
                            detail = "You paid $weeklyCost for food, rent, transport, and basics.",
                            kind = HistoryKind.DAY,
                        ),
                    )
                }
                eventRoll.event?.let { event ->
                    add(
                        HistoryEntry(
                            day = state.day,
                            title = event.title,
                            detail = event.description,
                            kind = HistoryKind.EVENT,
                        ),
                    )
                }
                add(
                    HistoryEntry(
                        day = state.day,
                        title = "Day ${state.day} ended",
                        detail = "You wake up to day ${state.day + 1}.",
                        kind = HistoryKind.DAY,
                    ),
                )
            }.trimHistory(),
        )

        return SimulationResult(
            state = nextDay,
            success = true,
            messages = buildList {
                if (weeklyCost > 0) add("Weekly living costs: -$weeklyCost.")
                eventRoll.event?.let { add(it.title) }
                add("Day ${state.day + 1} begins.")
            },
        )
    }

    private fun unavailableReason(state: GameState, action: DailyActionDefinition): String? = when {
        state.timeRemaining < action.timeCost -> "Not enough time left today."
        state.stats.energy < action.energyCost -> "Not enough energy."
        state.money < action.moneyCost -> "Not enough money."
        else -> null
    }

    private fun applyEffect(state: GameState, effect: ActionEffect): GameState {
        val stats = state.stats.copy(
            health = state.stats.health + effect.healthDelta,
            mood = state.stats.mood + effect.moodDelta,
            energy = state.stats.energy + effect.energyDelta,
            stress = state.stats.stress + effect.stressDelta,
            social = state.stats.social + effect.socialDelta,
        ).clamped()
        val skills = state.skills.copy(
            knowledge = state.skills.knowledge + effect.knowledgeDelta,
            fitness = state.skills.fitness + effect.fitnessDelta,
            career = state.skills.career + effect.careerXpDelta,
        ).clamped()

        return state.copy(
            stats = stats,
            skills = skills,
            money = state.money + effect.moneyDelta,
        )
    }

    private fun rollEvent(state: GameState): EventRoll {
        val nextSeed = nextSeed(state.rngSeed)
        val candidates = events.filter { it.condition(state) }
        val shouldTrigger = candidates.isNotEmpty() && positiveModulo(nextSeed, 100) < EVENT_CHANCE_PERCENT
        val event = if (shouldTrigger) {
            candidates[positiveModulo(nextSeed / 100, candidates.size)]
        } else {
            null
        }
        return EventRoll(event = event, nextSeed = nextSeed)
    }

    private fun summarizeAction(action: DailyActionDefinition): String =
        "Time -${action.timeCost}, energy -${action.energyCost}" +
            if (action.moneyCost > 0) ", money -${action.moneyCost}" else ""

    private fun careerLevelFor(careerXp: Int): Int = (careerXp / 100) + 1

    private fun jobTitleFor(archetype: LifeArchetype, careerLevel: Int): String = when (archetype) {
        LifeArchetype.STUDENT -> if (careerLevel >= 2) "Student intern" else "Part-time student"
        LifeArchetype.JUNIOR_WORKER -> if (careerLevel >= 2) "Associate" else "Junior associate"
        LifeArchetype.FREELANCER -> if (careerLevel >= 2) "Reliable freelancer" else "Independent freelancer"
    }

    private fun weeklyCostFor(archetype: LifeArchetype): Int = when (archetype) {
        LifeArchetype.STUDENT -> 140
        LifeArchetype.JUNIOR_WORKER -> 210
        LifeArchetype.FREELANCER -> 180
    }

    private fun seedFor(archetype: LifeArchetype): Long = when (archetype) {
        LifeArchetype.STUDENT -> 10_001L
        LifeArchetype.JUNIOR_WORKER -> 20_001L
        LifeArchetype.FREELANCER -> 30_001L
    }

    private fun nextSeed(seed: Long): Long = seed * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L

    private fun positiveModulo(value: Long, modulus: Int): Int = Math.floorMod(value, modulus.toLong()).toInt()

    private fun List<HistoryEntry>.trimHistory(): List<HistoryEntry> = takeLast(HISTORY_LIMIT)

    private data class StartingPreset(
        val stats: CoreStats,
        val skills: SkillSet,
        val money: Int,
        val jobTitle: String,
    )

    private data class EventRoll(
        val event: LifeEventDefinition?,
        val nextSeed: Long,
    )

    companion object {
        const val DAILY_TIME_BUDGET = 12
        private const val EVENT_CHANCE_PERCENT = 35
        private const val HISTORY_LIMIT = 80
    }
}
