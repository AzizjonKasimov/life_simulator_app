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
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
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

/** A UI-facing interaction offered for one person, with its availability. */
data class InteractionOption(
    val id: String,
    val label: String,
    val enabled: Boolean,
    val reason: String?,
)

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
        s = payAndPromote(s, newAge, logs)
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
        if (!activity.requires(state)) return SimulationResult.failure(state, "You can't do that right now.")
        if (activityId in state.activitiesUsed) return SimulationResult.failure(state, "You've already done that this year.")
        if (state.character.money < activity.cost) return SimulationResult.failure(state, "You can't afford that.")

        val beforeStats = state.character.stats
        val beforeMoney = state.character.money
        var s = state
        if (activity.cost != 0) s = s.copy(character = s.character.copy(money = s.character.money - activity.cost))
        s = applyEffects(s, activity.effects)
        s = s.copy(activitiesUsed = s.activitiesUsed + activityId)

        val (afterSpecial, logs, messages) = handleSpecial(s, activity)
        s = afterSpecial.copy(log = afterSpecial.log + logs)
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
        val option = interactionsFor(state, person).find { it.id == interactionId }
            ?: return SimulationResult.failure(state, "You can't do that.")
        if (!option.enabled) return SimulationResult.failure(state, option.reason ?: "You can't do that right now.")

        val beforeStats = state.character.stats
        val beforeMoney = state.character.money
        val relBefore = person.relationship
        val rng = rngFor(state.rngSeed, state.age, "interact-$personId-$interactionId-${state.log.size}")

        val effects = mutableListOf<Effect>()
        val text: String
        var message: String? = null
        when (interactionId) {
            "spend_time" -> {
                effects += Effect.RelationshipDelta(8, personId = personId); effects += happy(3)
                text = "You spent quality time with ${person.name}."
            }
            "converse" -> {
                val delta = rng.nextInt(-2, 7)
                effects += Effect.RelationshipDelta(delta, personId = personId)
                text = if (delta >= 0) "You had a nice chat with ${person.name}." else "Your talk with ${person.name} got a little tense."
            }
            "compliment" -> {
                effects += Effect.RelationshipDelta(5, personId = personId); effects += happy(1)
                text = "You gave ${person.name} a heartfelt compliment."
            }
            "insult" -> {
                effects += Effect.RelationshipDelta(-12, personId = personId); effects += happy(-1)
                text = "You insulted ${person.name}. It did not go over well."
            }
            "gift" -> {
                effects += cashE(-GIFT_COST); effects += Effect.RelationshipDelta(12, personId = personId); effects += happy(1)
                text = "You gave ${person.name} a thoughtful gift."
            }
            "propose" -> {
                effects += Effect.PromoteRelation(RelationType.PARTNER, RelationType.SPOUSE)
                effects += Effect.AddFlag("married"); effects += happy(12)
                text = "${person.name} said yes! You're married."; message = "You married ${person.name}!"
            }
            "break_up" -> {
                effects += Effect.RemovePeople(RelationType.PARTNER); effects += happy(-8)
                text = "You broke up with ${person.name}."; message = "You and ${person.name} broke up."
            }
            "have_child" -> {
                effects += Effect.AddPerson(RelationType.CHILD, 85); effects += happy(8)
                text = "You and ${person.name} welcomed a new child."; message = "A new arrival!"
            }
            "divorce" -> {
                val settlement = (state.character.money / 4).coerceAtLeast(0)
                effects += Effect.RemovePeople(RelationType.SPOUSE)
                if (settlement > 0) effects += cashE(-settlement)
                effects += happy(-10)
                text = "You divorced ${person.name}."; message = "You divorced ${person.name}."
            }
            "ask_money" -> {
                if (person.relationship >= 45) {
                    val gift = 200 + rng.nextInt(0, 800)
                    effects += cashE(gift); effects += Effect.RelationshipDelta(-3, personId = personId)
                    text = "${person.name} lent you ${money(gift)}."; message = "${person.name} gave you ${money(gift)}."
                } else {
                    effects += Effect.RelationshipDelta(-2, personId = personId)
                    text = "${person.name} turned down your request for money."; message = "They said no."
                }
            }
            else -> return SimulationResult.failure(state, "You can't do that.")
        }

        var s = applyEffects(state, effects)
        s = s.copy(log = s.log + LogEntry(s.age, text, LogKind.RELATIONSHIP))
        s = checkHealthDeath(s)

        val changes = statChanges(beforeStats, beforeMoney, s.character.stats, s.character.money).toMutableList()
        val relAfter = s.relationships.find { it.id == personId }?.relationship ?: relBefore
        if (relAfter != relBefore) changes.add(0, StatChange(person.name, relAfter - relBefore))
        return SimulationResult.success(s, messages = listOfNotNull(message), statChanges = changes)
    }

    // ---- UI-facing queries -------------------------------------------------

    fun availableActivities(state: GameState): List<ActivityOption> {
        if (!state.alive) return emptyList()
        return ActivityCatalog.all
            .filter { state.age >= it.minAge && !(it.requiresUnemployed && state.job != null) && it.requires(state) }
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

    /** The interactions offered for [person] right now, contextual to their relation and your money. */
    fun interactionsFor(state: GameState, person: Person): List<InteractionOption> {
        if (!state.alive || !person.alive) return emptyList()
        val cash = state.character.money
        val options = mutableListOf(
            InteractionOption("spend_time", "Spend time", true, null),
            InteractionOption("converse", "Converse", true, null),
            InteractionOption("compliment", "Compliment", true, null),
            InteractionOption("gift", "Give a gift", cash >= GIFT_COST, if (cash >= GIFT_COST) null else "Costs ${money(GIFT_COST)}"),
        )
        when (person.relation) {
            RelationType.PARTNER -> {
                val ready = person.relationship >= 55 && state.age >= 18
                options += InteractionOption("propose", "Propose", ready, if (ready) null else "They're not ready")
                options += InteractionOption("break_up", "Break up", true, null)
            }
            RelationType.SPOUSE -> {
                val kids = state.relationships.count { it.relation == RelationType.CHILD }
                options += InteractionOption("have_child", "Have a child", kids < 6, if (kids < 6) null else "Your hands are full")
                options += InteractionOption("ask_money", "Ask for money", true, null)
                options += InteractionOption("divorce", "Divorce", true, null)
            }
            RelationType.MOTHER, RelationType.FATHER -> {
                options += InteractionOption("ask_money", "Ask for money", true, null)
            }
            else -> Unit
        }
        options += InteractionOption("insult", "Insult", true, null)
        return options
    }

    // ---- Internals ---------------------------------------------------------

    private fun applyEffects(state: GameState, effects: List<Effect>): GameState {
        var character = state.character
        var relationships = state.relationships
        var flags = state.flags
        var job = state.job
        var jobYears = state.jobYears
        val familyName = character.name.substringAfterLast(' ', "")
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
                is Effect.StartJob -> JobCatalog.byId(effect.jobId)?.let { job = it; jobYears = 0 }
                is Effect.AddPerson -> {
                    val rng = rngFor(state.rngSeed, character.age, "person${relationships.size}")
                    relationships = relationships + Names.generatePerson(effect.relation, character.age, familyName, effect.relationship, rng)
                }
                is Effect.PromoteRelation -> relationships = relationships.map { p ->
                    if (p.relation == effect.from && p.alive) p.copy(relation = effect.to) else p
                }
                is Effect.RemovePeople -> relationships = relationships.filterNot { it.relation == effect.relation }
                is Effect.PromoteJob -> job?.let { held -> JobCatalog.promoted(held)?.let { job = it; jobYears = 0 } }
                is Effect.LoseJob -> { job = null; jobYears = 0 }
            }
        }
        return state.copy(character = character, relationships = relationships, flags = flags, job = job, jobYears = jobYears)
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

    /** Pay this year's salary, bank a year of tenure, and roll for a promotion. */
    private fun payAndPromote(state: GameState, age: Int, logs: MutableList<LogEntry>): GameState {
        val job = state.job ?: return state
        var s = state.copy(
            character = state.character.copy(money = state.character.money + job.salaryPerYear),
            jobYears = state.jobYears + 1,
        )
        logs += LogEntry(age, "Earned ${money(job.salaryPerYear)} working as a ${job.title}.", LogKind.MONEY)
        val next = JobCatalog.promoted(job) ?: return s
        val chance = promotionChance(s.character.stats.smarts, state.jobYears)
        if (rngFor(state.rngSeed, age, "promo").nextDouble() < chance) {
            s = s.copy(job = next, jobYears = 0)
            logs += LogEntry(age, "You were promoted to ${next.title} — now ${money(next.salaryPerYear)}/yr.", LogKind.WORK)
        }
        return s
    }

    private fun promotionChance(smarts: Int, jobYears: Int): Double =
        (0.04 + smarts / 500.0 + jobYears * 0.03).coerceAtMost(0.4)

    private fun progressEducation(state: GameState, logs: MutableList<LogEntry>): GameState {
        val age = state.age
        val edu = state.education
        if (edu.isEnrolled) {
            val tuition = if (edu.enrolledIn == EducationLevel.GRADUATE) GRAD_TUITION else UNI_TUITION
            val studied = state.copy(
                character = state.character.copy(
                    money = state.character.money - tuition,
                    stats = state.character.stats.withDelta(Stat.SMARTS, 3),
                ),
            )
            val yearsLeft = edu.yearsLeft - 1
            return if (yearsLeft <= 0) {
                val completed = edu.enrolledIn!!
                val flag = if (completed == EducationLevel.GRADUATE) "grad_degree" else "college_grad"
                logs += LogEntry(age, "You graduated with a ${completed.label} degree!", LogKind.MILESTONE)
                studied.copy(education = Education(level = completed), flags = studied.flags + flag)
            } else {
                studied.copy(education = edu.copy(yearsLeft = yearsLeft))
            }
        }
        return when {
            age == 5 && edu.level == EducationLevel.NONE -> {
                logs += LogEntry(age, "You started primary school.", LogKind.SCHOOL)
                state.copy(education = Education(EducationLevel.PRIMARY))
            }
            age == 14 && edu.level == EducationLevel.PRIMARY -> {
                logs += LogEntry(age, "You started high school.", LogKind.SCHOOL)
                state.copy(education = Education(EducationLevel.SECONDARY))
            }
            age == 18 && edu.level == EducationLevel.SECONDARY && "hs_grad" !in state.flags -> {
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

    // ---- Activity specials -------------------------------------------------

    private fun handleSpecial(state: GameState, activity: Activity): Triple<GameState, List<LogEntry>, List<String>> =
        when (activity.special) {
            "find_job" -> tryFindJob(state).let { (s, log, msg) -> Triple(s, listOf(log), listOf(msg)) }
            "quit_job" -> {
                val title = state.job?.title ?: "your job"
                Triple(
                    state.copy(job = null, jobYears = 0),
                    listOf(LogEntry(state.age, "You quit your job as a $title.", LogKind.WORK)),
                    listOf("You quit your job."),
                )
            }
            "enroll_university" -> enroll(state, EducationLevel.UNIVERSITY, years = 4)
            "enroll_grad" -> enroll(state, EducationLevel.GRADUATE, years = 2)
            "date" -> goOnDate(state)
            "adopt_pet" -> {
                val s = applyEffects(state, listOf(Effect.AddPerson(RelationType.PET, 70), happy(4)))
                val pet = s.relationships.last()
                Triple(s, listOf(LogEntry(s.age, "You adopted ${pet.name}.", LogKind.RELATIONSHIP)), listOf("You adopted ${pet.name}!"))
            }
            else -> {
                val logs = if (activity.logText.isNotBlank()) listOf(LogEntry(state.age, activity.logText, activity.logKind)) else emptyList()
                Triple(state, logs, emptyList())
            }
        }

    private fun enroll(state: GameState, target: EducationLevel, years: Int): Triple<GameState, List<LogEntry>, List<String>> {
        val s = state.copy(education = state.education.copy(enrolledIn = target, yearsLeft = years))
        return Triple(
            s,
            listOf(LogEntry(state.age, "You enrolled in ${target.label}.", LogKind.SCHOOL)),
            listOf("You enrolled in ${target.label}."),
        )
    }

    private fun goOnDate(state: GameState): Triple<GameState, List<LogEntry>, List<String>> {
        val existing = state.relationships.firstOrNull {
            it.alive && (it.relation == RelationType.PARTNER || it.relation == RelationType.SPOUSE)
        }
        if (existing != null) {
            val s = applyEffects(state, listOf(Effect.RelationshipDelta(8, personId = existing.id), happy(3)))
            return Triple(s, listOf(LogEntry(s.age, "You had a lovely date with ${existing.name}.", LogKind.RELATIONSHIP)), listOf("A lovely evening with ${existing.name}."))
        }
        val rng = rngFor(state.rngSeed, state.age, "date-${state.log.size}")
        val chance = (0.4 + state.character.stats.looks / 250.0).coerceAtMost(0.85)
        return if (rng.nextDouble() < chance) {
            val s = applyEffects(state, listOf(Effect.AddPerson(RelationType.PARTNER, 55), happy(5), Effect.AddFlag("dating")))
            val partner = s.relationships.last { it.relation == RelationType.PARTNER }
            Triple(s, listOf(LogEntry(s.age, "You hit it off with ${partner.name} — you're seeing each other now.", LogKind.RELATIONSHIP)), listOf("You met ${partner.name}!"))
        } else {
            Triple(state, listOf(LogEntry(state.age, "The date didn't lead anywhere this time.", LogKind.RELATIONSHIP)), listOf("No spark this time."))
        }
    }

    private fun tryFindJob(state: GameState): Triple<GameState, LogEntry, String> {
        val rng = rngFor(state.rngSeed, state.age, "jobhunt")
        val smarts = state.character.stats.smarts
        val eligible = JobCatalog.eligible(state.age, smarts, state.education.level)
        if (eligible.isEmpty()) {
            return Triple(state, LogEntry(state.age, "You looked for work but weren't qualified for anything yet.", LogKind.WORK), "No jobs you qualify for yet.")
        }
        val chance = (0.35 + smarts / 200.0).coerceAtMost(0.9)
        return if (rng.nextDouble() < chance) {
            val job = weightedJob(eligible, rng)
            Triple(state.copy(job = job, jobYears = 0), LogEntry(state.age, "You were hired as a ${job.title}!", LogKind.WORK), "Hired as a ${job.title}!")
        } else {
            Triple(state, LogEntry(state.age, "You applied around but got no offers this year.", LogKind.WORK), "No offers this year.")
        }
    }

    /** Pick a job weighted by salary, so the qualified tend to land the better roles. */
    private fun weightedJob(eligible: List<com.azizjonkasimov.lifesimulator.domain.model.Job>, rng: Random): com.azizjonkasimov.lifesimulator.domain.model.Job {
        val total = eligible.sumOf { it.salaryPerYear.toDouble() }
        if (total <= 0.0) return eligible.random(rng)
        var roll = rng.nextDouble() * total
        for (job in eligible) {
            roll -= job.salaryPerYear
            if (roll < 0) return job
        }
        return eligible.last()
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

    private fun happy(amount: Int) = Effect.StatDelta(Stat.HAPPINESS, amount)
    private fun cashE(amount: Int) = Effect.MoneyDelta(amount)

    private fun rngFor(seed: Long, age: Int, salt: String): Random {
        var hash = seed
        hash = hash * 1_000_003L + age
        hash = hash * 1_000_003L + salt.hashCode()
        return Random(hash)
    }

    private fun money(value: Int): String = if (value < 0) "-$${-value}" else "$$value"

    private companion object {
        const val ACCIDENT_CHANCE = 0.0008
        const val GIFT_COST = 100
        const val UNI_TUITION = 4000
        const val GRAD_TUITION = 8000
    }
}
