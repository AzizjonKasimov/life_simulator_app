package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Character
import com.azizjonkasimov.lifesimulator.domain.model.Education
import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.Effect
import com.azizjonkasimov.lifesimulator.domain.model.EventCategory
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.LifeEvent
import com.azizjonkasimov.lifesimulator.domain.model.LogEntry
import com.azizjonkasimov.lifesimulator.domain.model.LogKind
import com.azizjonkasimov.lifesimulator.domain.model.Person
import com.azizjonkasimov.lifesimulator.domain.model.SimulationResult
import com.azizjonkasimov.lifesimulator.domain.model.Stat
import com.azizjonkasimov.lifesimulator.domain.model.StatChange
import com.azizjonkasimov.lifesimulator.domain.model.Stats
import kotlin.random.Random

/** A UI-facing view of an activity plus whether it can be used right now. */
data class ActivityOption(
    val activity: Activity,
    val enabled: Boolean,
    val reason: String?,
)

data class Interaction(val id: String, val label: String)

/**
 * The whole simulation. Time advances one year per [ageUp]; everything the player
 * does is a pure `GameState -> SimulationResult`. All chance is seeded off
 * [GameState.rngSeed] so a given life replays identically.
 */
class LifeSimulationEngine {

    fun startNewLife(name: String, gender: Gender): GameState {
        val seed = Random.nextLong()
        val rng = Random(seed)
        val trimmed = name.trim().ifBlank { Names.firstName(gender, rng) }
        val familyName = trimmed.substringAfterLast(' ', "").ifBlank { Names.lastName(rng) }
        val fullName = if (trimmed.contains(' ')) trimmed else "$trimmed $familyName"
        val birthplace = Names.birthplace(rng)
        val stats = Stats(
            happiness = 55 + rng.nextInt(-10, 11),
            health = 75 + rng.nextInt(-10, 16),
            smarts = 35 + rng.nextInt(0, 41),
            looks = 35 + rng.nextInt(0, 41),
        ).clamped()
        val genderWord = if (gender == Gender.MALE) "a baby boy" else "a baby girl"
        return GameState(
            character = Character(fullName, gender, birthplace, age = 0, stats = stats, money = 0),
            relationships = Names.startingFamily(rng, familyName),
            education = Education(EducationLevel.NONE),
            job = null,
            flags = emptySet(),
            eventsSeen = emptySet(),
            pendingEventIds = emptyList(),
            activitiesUsed = emptySet(),
            rngSeed = seed,
            log = listOf(LogEntry(0, "You were born $genderWord in $birthplace.", LogKind.MILESTONE)),
            alive = true,
        )
    }

    fun ageUp(state: GameState): SimulationResult {
        if (!state.alive) return SimulationResult.failure(state, "This life has already ended.")
        if (state.pendingEventIds.isNotEmpty()) {
            return SimulationResult.failure(state, "Resolve this year's events first.")
        }

        val newAge = state.age + 1
        val rng = rngFor(state.rngSeed, newAge, "year")
        val logs = mutableListOf<LogEntry>()
        var s = state.copy(
            character = state.character.copy(age = newAge),
            activitiesUsed = emptySet(),
        )

        s = applyAnnualDrift(s)

        val job = s.job
        if (job != null) {
            s = s.copy(character = s.character.copy(money = s.character.money + job.salaryPerYear))
            logs += LogEntry(newAge, "Earned ${money(job.salaryPerYear)} working as a ${job.title}.", LogKind.MONEY)
        }

        s = progressEducation(s, logs)
        s = ageRelationships(s, rng, logs)

        rollDeath(s, rng)?.let { cause ->
            val dead = s.copy(
                alive = false,
                causeOfDeath = cause,
                pendingEventIds = emptyList(),
                log = s.log + logs + LogEntry(newAge, "You died at $newAge from $cause.", LogKind.MILESTONE),
            )
            return SimulationResult.success(dead, messages = listOf("You died at age $newAge."))
        }

        val eligible = EventCatalog.eligible(s)
        val count = if (eligible.isEmpty()) 0 else 1 + rng.nextInt(0, 3)
        val picked = weightedSample(eligible, count, rng)

        var seen = s.eventsSeen
        for (ev in picked) {
            if (ev.oneShot) seen = seen + ev.id
            if (!ev.interactive) {
                ev.choices.firstOrNull()?.let { choice ->
                    s = applyEffects(s, choice.effects)
                    logs += LogEntry(newAge, choice.resultText, logKindFor(ev.category))
                }
            }
        }
        s = s.copy(
            eventsSeen = seen,
            pendingEventIds = picked.filter { it.interactive }.map { it.id },
            log = s.log + logs,
        )
        s = checkHealthDeath(s)
        return SimulationResult.success(s, messages = listOf(if (s.alive) "You are now $newAge." else "You died at age $newAge."))
    }

    fun resolveEvent(state: GameState, eventId: String, choiceIndex: Int): SimulationResult {
        if (eventId !in state.pendingEventIds) return SimulationResult.failure(state, "That decision has passed.")
        val event = EventCatalog.byId(eventId) ?: return SimulationResult.failure(state, "Unknown event.")
        val choice = event.choices.getOrNull(choiceIndex) ?: return SimulationResult.failure(state, "Invalid choice.")

        val beforeStats = state.character.stats
        val beforeMoney = state.character.money
        var s = applyEffects(state, choice.effects)
        s = s.copy(
            pendingEventIds = state.pendingEventIds - eventId,
            log = s.log + LogEntry(s.age, choice.resultText, logKindFor(event.category)),
        )
        s = checkHealthDeath(s)
        return SimulationResult.success(
            s,
            statChanges = statChanges(beforeStats, beforeMoney, s.character.stats, s.character.money),
        )
    }

    fun doActivity(state: GameState, activityId: String): SimulationResult {
        if (!state.alive) return SimulationResult.failure(state, "This life has ended.")
        if (state.pendingEventIds.isNotEmpty()) return SimulationResult.failure(state, "Resolve this year's events first.")
        val activity = ActivityCatalog.byId(activityId) ?: return SimulationResult.failure(state, "Unknown activity.")
        if (state.age < activity.minAge) return SimulationResult.failure(state, "You're too young for that.")
        if (activity.requiresUnemployed && state.job != null) return SimulationResult.failure(state, "You already have a job.")
        if (activityId in state.activitiesUsed) return SimulationResult.failure(state, "You've already done that this year.")
        if (state.character.money < activity.cost) return SimulationResult.failure(state, "You can't afford that.")

        val beforeStats = state.character.stats
        val beforeMoney = state.character.money
        var s = state
        if (activity.cost != 0) s = s.copy(character = s.character.copy(money = s.character.money - activity.cost))
        s = applyEffects(s, activity.effects)
        s = s.copy(activitiesUsed = s.activitiesUsed + activityId)

        val logs = mutableListOf<LogEntry>()
        val messages = mutableListOf<String>()
        if (activity.special == "find_job") {
            val (next, log, message) = tryFindJob(s)
            s = next
            logs += log
            messages += message
        } else if (activity.logText.isNotBlank()) {
            logs += LogEntry(s.age, activity.logText, activity.logKind)
        }
        s = s.copy(log = s.log + logs)
        return SimulationResult.success(
            s,
            messages = messages,
            statChanges = statChanges(beforeStats, beforeMoney, s.character.stats, s.character.money),
        )
    }

    fun interact(state: GameState, personId: String, interactionId: String): SimulationResult {
        if (!state.alive) return SimulationResult.failure(state, "This life has ended.")
        val person = state.relationships.find { it.id == personId }
            ?: return SimulationResult.failure(state, "They're not in your life.")
        if (!person.alive) return SimulationResult.failure(state, "They've passed away.")

        val (relDelta, happyDelta, text) = when (interactionId) {
            "spend_time" -> Triple(8, 3, "You spent quality time with ${person.name}.")
            "compliment" -> Triple(5, 1, "You gave ${person.name} a heartfelt compliment.")
            "insult" -> Triple(-12, -1, "You insulted ${person.name}. It did not go over well.")
            else -> return SimulationResult.failure(state, "You can't do that.")
        }
        val updated = state.relationships.map {
            if (it.id == personId) it.copy(relationship = it.relationship + relDelta).clamped() else it
        }
        val s = state.copy(
            relationships = updated,
            character = state.character.copy(stats = state.character.stats.withDelta(Stat.HAPPINESS, happyDelta)),
        ).let { it.copy(log = it.log + LogEntry(it.age, text, LogKind.RELATIONSHIP)) }
        return SimulationResult.success(s, statChanges = listOf(StatChange("${person.name} (${person.relation.label})", relDelta)))
    }

    // ---- UI-facing queries -------------------------------------------------

    fun availableActivities(state: GameState): List<ActivityOption> {
        if (!state.alive) return emptyList()
        return ActivityCatalog.all
            .filter { state.age >= it.minAge && !(it.requiresUnemployed && state.job != null) }
            .map { activity ->
                val reason = when {
                    activity.id in state.activitiesUsed -> "Done this year"
                    state.character.money < activity.cost -> "Costs ${money(activity.cost)}"
                    else -> null
                }
                ActivityOption(activity, enabled = reason == null, reason = reason)
            }
    }

    fun pendingEvent(state: GameState): LifeEvent? =
        state.pendingEventIds.firstOrNull()?.let { EventCatalog.byId(it) }

    fun interactionsFor(person: Person): List<Interaction> = listOf(
        Interaction("spend_time", "Spend time"),
        Interaction("compliment", "Compliment"),
        Interaction("insult", "Insult"),
    )

    // ---- Internals ---------------------------------------------------------

    private fun applyEffects(state: GameState, effects: List<Effect>): GameState {
        var character = state.character
        var relationships = state.relationships
        var flags = state.flags
        var job = state.job
        for (effect in effects) {
            when (effect) {
                is Effect.StatDelta -> character = character.copy(stats = character.stats.withDelta(effect.stat, effect.amount))
                is Effect.MoneyDelta -> character = character.copy(money = character.money + effect.amount)
                is Effect.RelationshipDelta -> relationships = relationships.map { p ->
                    val match = (effect.personId == null || effect.personId == p.id) &&
                        (effect.relation == null || effect.relation == p.relation)
                    if (match && p.alive) p.copy(relationship = p.relationship + effect.amount).clamped() else p
                }
                is Effect.AddFlag -> flags = flags + effect.flag
                is Effect.StartJob -> JobCatalog.byId(effect.jobId)?.let { job = it }
            }
        }
        return state.copy(character = character, relationships = relationships, flags = flags, job = job)
    }

    private fun applyAnnualDrift(state: GameState): GameState {
        val age = state.age
        val healthDrift = when {
            age < 30 -> 0
            age < 50 -> -1
            age < 65 -> -2
            age < 80 -> -3
            else -> -4
        }
        val looksDrift = when {
            age in 1..17 -> 1
            age > 35 -> -1
            else -> 0
        }
        val stats = state.character.stats
            .copy(health = state.character.stats.health + healthDrift, looks = state.character.stats.looks + looksDrift)
            .clamped()
        return state.copy(character = state.character.copy(stats = stats))
    }

    private fun progressEducation(state: GameState, logs: MutableList<LogEntry>): GameState {
        val age = state.age
        return when {
            age == 5 && state.education.level == EducationLevel.NONE -> {
                logs += LogEntry(age, "You started primary school.", LogKind.SCHOOL)
                state.copy(education = Education(EducationLevel.PRIMARY))
            }
            age == 14 && state.education.level == EducationLevel.PRIMARY -> {
                logs += LogEntry(age, "You started high school.", LogKind.SCHOOL)
                state.copy(education = Education(EducationLevel.SECONDARY))
            }
            age == 18 && state.education.level == EducationLevel.SECONDARY && "hs_grad" !in state.flags -> {
                logs += LogEntry(age, "You graduated high school!", LogKind.MILESTONE)
                state.copy(flags = state.flags + "hs_grad")
            }
            else -> state
        }
    }

    private fun ageRelationships(state: GameState, rng: Random, logs: MutableList<LogEntry>): GameState {
        val updated = state.relationships.map { person ->
            if (!person.alive) return@map person
            val newAge = person.age + 1
            val deathChance = when {
                newAge < 60 -> 0.0
                newAge < 75 -> 0.02
                newAge < 85 -> 0.06
                newAge < 95 -> 0.15
                else -> 0.35
            }
            if (deathChance > 0.0 && rng.nextDouble() < deathChance) {
                logs += LogEntry(state.age, "${person.name}, your ${person.relation.label.lowercase()}, passed away at $newAge.", LogKind.MILESTONE)
                person.copy(age = newAge, alive = false)
            } else {
                person.copy(age = newAge)
            }
        }
        return state.copy(relationships = updated)
    }

    private fun rollDeath(state: GameState, rng: Random): String? {
        val age = state.age
        val health = state.character.stats.health
        if (health <= 0) return "poor health"
        val base = when {
            age < 40 -> 0.001
            age < 55 -> 0.005
            age < 65 -> 0.012
            age < 75 -> 0.03
            age < 85 -> 0.08
            age < 95 -> 0.18
            else -> 0.4
        }
        val chance = (base * (1.0 + (100 - health) / 100.0 * 1.5)).coerceAtMost(0.95)
        val roll = rng.nextDouble()
        return when {
            roll < ACCIDENT_CHANCE -> listOf("a car accident", "a freak accident", "a bad fall").random(rng)
            roll < ACCIDENT_CHANCE + chance -> when {
                age >= 70 -> "old age"
                health < 25 -> "a sudden illness"
                else -> "natural causes"
            }
            else -> null
        }
    }

    private fun tryFindJob(state: GameState): Triple<GameState, LogEntry, String> {
        val rng = rngFor(state.rngSeed, state.age, "jobhunt")
        val smarts = state.character.stats.smarts
        val eligible = JobCatalog.eligible(state.age, smarts)
        if (eligible.isEmpty()) {
            return Triple(state, LogEntry(state.age, "You looked for work but weren't qualified for anything yet.", LogKind.WORK), "No jobs you qualify for yet.")
        }
        val chance = (0.35 + smarts / 200.0).coerceAtMost(0.9)
        return if (rng.nextDouble() < chance) {
            val job = eligible.random(rng)
            Triple(state.copy(job = job), LogEntry(state.age, "You were hired as a ${job.title}!", LogKind.WORK), "Hired as a ${job.title}!")
        } else {
            Triple(state, LogEntry(state.age, "You applied around but got no offers this year.", LogKind.WORK), "No offers this year.")
        }
    }

    private fun checkHealthDeath(state: GameState): GameState {
        if (state.alive && state.character.stats.health <= 0) {
            return state.copy(
                alive = false,
                causeOfDeath = "poor health",
                pendingEventIds = emptyList(),
                log = state.log + LogEntry(state.age, "Your health gave out. You died at ${state.age}.", LogKind.MILESTONE),
            )
        }
        return state
    }

    private fun statChanges(before: Stats, beforeMoney: Int, after: Stats, afterMoney: Int): List<StatChange> {
        val changes = mutableListOf<StatChange>()
        for (stat in Stat.entries) {
            val delta = after.get(stat) - before.get(stat)
            if (delta != 0) changes += StatChange(stat.label, delta)
        }
        val moneyDelta = afterMoney - beforeMoney
        if (moneyDelta != 0) changes += StatChange("Money", moneyDelta)
        return changes
    }

    private fun weightedSample(pool: List<LifeEvent>, count: Int, rng: Random): List<LifeEvent> {
        if (count <= 0 || pool.isEmpty()) return emptyList()
        val remaining = pool.toMutableList()
        val chosen = mutableListOf<LifeEvent>()
        repeat(minOf(count, remaining.size)) {
            val total = remaining.sumOf { it.weight }
            if (total <= 0) return@repeat
            var roll = rng.nextInt(total)
            var index = 0
            while (index < remaining.lastIndex) {
                roll -= remaining[index].weight
                if (roll < 0) break
                index++
            }
            chosen += remaining.removeAt(index)
        }
        return chosen
    }

    private fun logKindFor(category: EventCategory): LogKind = when (category) {
        EventCategory.HEALTH -> LogKind.HEALTH
        EventCategory.MONEY -> LogKind.MONEY
        EventCategory.WORK -> LogKind.WORK
        EventCategory.SCHOOL -> LogKind.SCHOOL
        EventCategory.ROMANCE, EventCategory.FAMILY -> LogKind.RELATIONSHIP
        else -> LogKind.EVENT
    }

    private fun rngFor(seed: Long, age: Int, salt: String): Random {
        var hash = seed
        hash = hash * 1_000_003L + age
        hash = hash * 1_000_003L + salt.hashCode()
        return Random(hash)
    }

    private fun money(value: Int): String = if (value < 0) "-$${-value}" else "$$value"

    private companion object {
        const val ACCIDENT_CHANCE = 0.0008
    }
}
