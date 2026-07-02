package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Ailment
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
import com.azizjonkasimov.lifesimulator.domain.model.Prison
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
        val traits = TraitCatalog.roll(rng)
        val traitLabels = traits.mapNotNull { TraitCatalog.byId(it)?.label }
        val birthLog = buildList {
            add(LogEntry(0, "You were born $genderWord in $birthplace.", LogKind.MILESTONE))
            if (traitLabels.isNotEmpty()) {
                add(LogEntry(0, "You have a ${traitLabels.joinToString(", ")} streak.", LogKind.NEUTRAL))
            }
        }
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
            log = birthLog,
            alive = true,
            traits = traits,
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

        s = progressPrison(s, logs)
        s = applyAnnualDrift(s)
        s = progressHealth(s, rng, logs)
        if (!s.inPrison) {
            s = payAndPromote(s, newAge, logs)
            s = progressEducation(s, logs)
        }
        s = ageRelationships(s, rng, logs)

        rollDeath(s, rng)?.let { cause ->
            val dead = s.copy(
                alive = false,
                causeOfDeath = cause,
                pendingEventIds = emptyList(),
                log = s.log + logs + LogEntry(newAge, "You died at $newAge from $cause.", LogKind.MILESTONE),
            )
            return SimulationResult.success(applyAchievements(dead), messages = listOf("You died at age $newAge."))
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
        s = applyAchievements(s)
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
        s = applyAchievements(s)
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
        if (state.inPrison && activity.category != ActivityCategory.PRISON) return SimulationResult.failure(state, "You can't do that from prison.")
        if (!state.inPrison && activity.category == ActivityCategory.PRISON) return SimulationResult.failure(state, "You're not in prison.")
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
        s = applyAchievements(s)
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
            "take_trip" -> {
                effects += cashE(-TRIP_COST); effects += Effect.RelationshipDelta(14, personId = personId); effects += happy(5)
                text = "You and ${person.name} took a memorable trip together."; message = "A wonderful trip with ${person.name}."
            }
            "ask_advice" -> {
                effects += Effect.RelationshipDelta(4, personId = personId); effects += smartsE(1)
                text = "${person.name} gave you some thoughtful advice."
            }
            else -> return SimulationResult.failure(state, "You can't do that.")
        }

        var s = applyEffects(state, effects)
        s = s.copy(log = s.log + LogEntry(s.age, text, LogKind.RELATIONSHIP))
        s = checkHealthDeath(s)
        s = applyAchievements(s)

        val changes = statChanges(beforeStats, beforeMoney, s.character.stats, s.character.money).toMutableList()
        val relAfter = s.relationships.find { it.id == personId }?.relationship ?: relBefore
        if (relAfter != relBefore) changes.add(0, StatChange(person.name, relAfter - relBefore))
        return SimulationResult.success(s, messages = listOfNotNull(message), statChanges = changes)
    }

    // ---- Generations -------------------------------------------------------

    /** The living children who could carry on the bloodline once you've died. */
    fun eligibleHeirs(state: GameState): List<Person> =
        if (state.alive) emptyList()
        else state.relationships.filter { it.alive && it.relation == RelationType.CHILD }

    /** What each heir inherits: the after-tax estate split evenly among the children. */
    fun estateShareEach(state: GameState): Int {
        val heirs = eligibleHeirs(state).size
        if (heirs == 0) return 0
        return afterTaxEstate(state) / heirs
    }

    /**
     * Continue the story as one of your children after death. The family reshapes
     * around the heir — your spouse becomes their parent, your other children their
     * siblings, you their late parent — and they inherit a taxed share of the estate.
     */
    fun continueAsHeir(state: GameState, heirId: String): SimulationResult {
        if (state.alive) return SimulationResult.failure(state, "You're still alive.")
        val heir = state.relationships.find { it.id == heirId && it.alive && it.relation == RelationType.CHILD }
            ?: return SimulationResult.failure(state, "They can't carry on your line.")

        val deceased = state.character
        val newSeed = rngFor(state.rngSeed, state.age, "heir-$heirId").nextLong()
        val rng = Random(newSeed)

        val heirGender = heir.gender ?: if (rng.nextBoolean()) Gender.MALE else Gender.FEMALE
        val inheritance = estateShareEach(state)
        val nextGen = state.generation + 1

        val stats = Stats(
            happiness = 60 + rng.nextInt(-10, 11),
            health = 80 + rng.nextInt(-10, 16),
            smarts = 40 + rng.nextInt(0, 41),
            looks = 40 + rng.nextInt(0, 41),
        ).clamped()
        val traits = inheritTraits(state.traits, rng)
        val traitLabels = traits.mapNotNull { TraitCatalog.byId(it)?.label }

        val age = heir.age
        val log = buildList {
            add(LogEntry(age, "You carry on as ${heir.name}, child of ${deceased.name}. (Generation $nextGen)", LogKind.MILESTONE))
            if (inheritance > 0) add(LogEntry(age, "You inherited ${money(inheritance)} from the estate.", LogKind.MONEY))
            if (traitLabels.isNotEmpty()) add(LogEntry(age, "You take after your family — a ${traitLabels.joinToString(", ")} streak.", LogKind.NEUTRAL))
        }

        val heirState = GameState(
            character = Character(
                name = heir.name,
                gender = heirGender,
                birthplace = deceased.birthplace,
                age = age,
                stats = stats,
                money = inheritance,
            ),
            relationships = buildHeirFamily(state, heir),
            education = educationForAge(age),
            job = null,
            flags = if (age >= 18) setOf("hs_grad") else emptySet(),
            eventsSeen = emptySet(),
            pendingEventIds = emptyList(),
            activitiesUsed = emptySet(),
            rngSeed = newSeed,
            log = log,
            alive = true,
            traits = traits,
            generation = nextGen,
        )
        return SimulationResult.success(applyAchievements(heirState), messages = listOf("You live on through ${heir.name}."))
    }

    /** Reshape the family tree around [heir]: late parent, surviving parent, siblings. */
    private fun buildHeirFamily(state: GameState, heir: Person): List<Person> {
        val deceased = state.character
        val family = mutableListOf<Person>()
        // You become the heir's late parent.
        family += Person(
            id = "parent_late",
            name = deceased.name,
            relation = if (deceased.gender == Gender.MALE) RelationType.FATHER else RelationType.MOTHER,
            age = state.age,
            relationship = 75,
            alive = false,
            gender = deceased.gender,
        )
        // Your surviving spouse becomes the heir's living parent.
        state.relationships.firstOrNull { it.alive && it.relation == RelationType.SPOUSE }?.let { spouse ->
            val g = spouse.gender ?: if (deceased.gender == Gender.MALE) Gender.FEMALE else Gender.MALE
            family += spouse.copy(
                relation = if (g == Gender.MALE) RelationType.FATHER else RelationType.MOTHER,
                relationship = spouse.relationship.coerceIn(45, 90),
                gender = g,
            )
        }
        // Your other living children become the heir's siblings.
        state.relationships
            .filter { it.alive && it.relation == RelationType.CHILD && it.id != heir.id }
            .forEach { family += it.copy(relation = RelationType.SIBLING, relationship = it.relationship.coerceIn(40, 90)) }
        return family
    }

    /** Pass on roughly half of a parent's traits, plus the odd fresh one; capped at two. */
    private fun inheritTraits(parentTraits: Set<String>, rng: Random): Set<String> {
        val inherited = parentTraits.filterTo(mutableSetOf()) { rng.nextDouble() < 0.5 }
        if (inherited.isEmpty() || rng.nextInt(0, 3) == 0) inherited += TraitCatalog.roll(rng)
        return inherited.take(2).toSet()
    }

    private fun educationForAge(age: Int): Education = when {
        age < 5 -> Education(EducationLevel.NONE)
        age < 14 -> Education(EducationLevel.PRIMARY)
        else -> Education(EducationLevel.SECONDARY)
    }

    private fun afterTaxEstate(state: GameState): Int {
        val estate = state.netWorth.coerceAtLeast(0)
        val taxable = (estate - ESTATE_EXEMPTION).coerceAtLeast(0)
        return (estate - (taxable * ESTATE_TAX_RATE).toInt()).coerceAtLeast(0)
    }

    // ---- UI-facing queries -------------------------------------------------

    fun availableActivities(state: GameState): List<ActivityOption> {
        if (!state.alive) return emptyList()
        val inPrison = state.inPrison
        return ActivityCatalog.all
            .filter {
                (if (inPrison) it.category == ActivityCategory.PRISON else it.category != ActivityCategory.PRISON) &&
                    state.age >= it.minAge &&
                    !(it.requiresUnemployed && state.job != null) &&
                    it.requires(state)
            }
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
        val canTrip = cash >= TRIP_COST
        val tripReason = if (canTrip) null else "Costs ${money(TRIP_COST)}"
        when (person.relation) {
            RelationType.PARTNER -> {
                val ready = person.relationship >= 55 && state.age >= 18
                options += InteractionOption("propose", "Propose", ready, if (ready) null else "They're not ready")
                options += InteractionOption("take_trip", "Take a trip", canTrip, tripReason)
                options += InteractionOption("break_up", "Break up", true, null)
            }
            RelationType.SPOUSE -> {
                val kids = state.relationships.count { it.relation == RelationType.CHILD }
                options += InteractionOption("have_child", "Have a child", kids < 6, if (kids < 6) null else "Your hands are full")
                options += InteractionOption("take_trip", "Take a trip", canTrip, tripReason)
                options += InteractionOption("ask_money", "Ask for money", true, null)
                options += InteractionOption("divorce", "Divorce", true, null)
            }
            RelationType.MOTHER, RelationType.FATHER -> {
                options += InteractionOption("ask_advice", "Ask for advice", true, null)
                options += InteractionOption("ask_money", "Ask for money", true, null)
            }
            RelationType.FRIEND -> {
                options += InteractionOption("ask_advice", "Ask for advice", true, null)
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
        var ailments = state.ailments
        var assets = state.assets
        var prison = state.prison
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
                is Effect.AddAilment -> {
                    if (ailments.none { it.id == effect.id }) {
                        val rng = rngFor(state.rngSeed, character.age, "ailment-${effect.id}-${ailments.size}")
                        HealthCatalog.ailment(effect.id, rng)?.let { ailments = ailments + it }
                    }
                }
                is Effect.CureAilments -> ailments = emptyList()
                is Effect.Imprison -> {
                    prison = Prison(sentence = effect.years.coerceAtLeast(1))
                    job = null
                    jobYears = 0
                }
                is Effect.Release -> {
                    prison = null
                    flags = flags + "ex_convict"
                }
                is Effect.AddAsset -> AssetCatalog.spec(effect.id)?.let { spec ->
                    AssetCatalog.asset(effect.id, assets.size)?.let { asset ->
                        assets = assets + asset
                        character = character.copy(stats = character.stats.withDelta(Stat.HAPPINESS, spec.happiness))
                        spec.flag?.let { flags = flags + it }
                    }
                }
            }
        }
        return state.copy(
            character = character,
            relationships = relationships,
            flags = flags,
            job = job,
            jobYears = jobYears,
            ailments = ailments,
            assets = assets,
            prison = prison,
        )
    }

    private fun applyAnnualDrift(state: GameState): GameState {
        val age = state.age
        var healthDrift = when {
            age < 30 -> 0
            age < 50 -> -1
            age < 65 -> -2
            age < 80 -> -3
            else -> -4
        }
        var looksDrift = when {
            age in 1..17 -> 1
            age > 35 -> -1
            else -> 0
        }
        var smartsDrift = 0
        var happinessDrift = 0
        // Traits colour the drift: health traits run lifelong (offsetting age); the
        // rest shape you during your formative years, so effects stay bounded.
        val formative = age in 1..25
        for (id in state.traits) {
            val t = TraitCatalog.byId(id) ?: continue
            healthDrift += t.healthDrift
            if (formative) {
                smartsDrift += t.smartsDrift
                looksDrift += t.looksDrift
                happinessDrift += t.happinessDrift
            }
        }
        val base = state.character.stats
        val stats = base.copy(
            happiness = base.happiness + happinessDrift,
            health = base.health + healthDrift,
            smarts = base.smarts + smartsDrift,
            looks = base.looks + looksDrift,
        ).clamped()
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

    /** Serve another year inside; release when the sentence is up. */
    private fun progressPrison(state: GameState, logs: MutableList<LogEntry>): GameState {
        val prison = state.prison ?: return state
        val served = prison.served + 1
        return if (served >= prison.sentence) {
            logs += LogEntry(state.age, "You were released from prison.", LogKind.MILESTONE)
            state.copy(prison = null, flags = state.flags + "ex_convict")
        } else {
            val left = prison.sentence - served
            logs += LogEntry(state.age, "Another year behind bars — $left to go.", LogKind.NEUTRAL)
            state.copy(prison = prison.copy(served = served))
        }
    }

    /** Chronic conditions drain Health, acute ones tick toward recovery, and a new
     *  age-weighted condition may appear. */
    private fun progressHealth(state: GameState, rng: Random, logs: MutableList<LogEntry>): GameState {
        var stats = state.character.stats
        val surviving = mutableListOf<Ailment>()
        for (a in state.ailments) {
            stats = stats.withDelta(Stat.HEALTH, -a.annualDrain)
            when {
                a.chronic -> surviving += a
                a.yearsLeft - 1 <= 0 -> logs += LogEntry(state.age, "You recovered from ${a.name}.", LogKind.HEALTH)
                else -> surviving += a.copy(yearsLeft = a.yearsLeft - 1)
            }
        }
        var result = state.copy(character = state.character.copy(stats = stats), ailments = surviving)
        // No spontaneous onset while inside — prison health is handled by its events.
        if (!result.inPrison) {
            HealthCatalog.rollOnset(result, rng)?.let { onset ->
                result = result.copy(ailments = result.ailments + onset)
                logs += LogEntry(state.age, "You were diagnosed with ${onset.name}.", LogKind.HEALTH)
            }
        }
        return result
    }

    /** Unlock any achievements whose condition now holds, logging each. */
    private fun applyAchievements(state: GameState): GameState {
        val newly = AchievementCatalog.all.filter { it.id !in state.achievements && it.predicate(state) }
        if (newly.isEmpty()) return state
        return state.copy(
            achievements = state.achievements + newly.map { it.id },
            log = state.log + newly.map { LogEntry(state.age, "Achievement unlocked: ${it.name}.", LogKind.MILESTONE) },
        )
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
                health < 30 -> state.ailments.maxByOrNull { it.severity }?.let { "complications from ${it.name}" }
                    ?: if (age >= 70) "old age" else "a sudden illness"
                age >= 70 -> "old age"
                else -> "natural causes"
            }
            else -> null
        }
    }

    // ---- Activity specials -------------------------------------------------

    private fun handleSpecial(state: GameState, activity: Activity): Triple<GameState, List<LogEntry>, List<String>> {
        val special = activity.special
        return when {
            special == "find_job" -> tryFindJob(state).let { (s, log, msg) -> Triple(s, listOf(log), listOf(msg)) }
            special == "quit_job" -> {
                val title = state.job?.title ?: "your job"
                Triple(
                    state.copy(job = null, jobYears = 0),
                    listOf(LogEntry(state.age, "You quit your job as a $title.", LogKind.WORK)),
                    listOf("You quit your job."),
                )
            }
            special == "enroll_university" -> enroll(state, EducationLevel.UNIVERSITY, years = 4)
            special == "enroll_grad" -> enroll(state, EducationLevel.GRADUATE, years = 2)
            special == "date" -> goOnDate(state)
            special == "adopt_pet" -> {
                val s = applyEffects(state, listOf(Effect.AddPerson(RelationType.PET, 70), happy(4)))
                val pet = s.relationships.last()
                Triple(s, listOf(LogEntry(s.age, "You adopted ${pet.name}.", LogKind.RELATIONSHIP)), listOf("You adopted ${pet.name}!"))
            }
            special == "treat" -> getTreatment(state)
            special == "good_behavior" -> goodBehavior(state)
            special != null && special.startsWith("crime:") -> commitCrime(state, special.removePrefix("crime:"))
            special != null && special.startsWith("buy:") -> buyAsset(state, special.removePrefix("buy:"))
            else -> {
                val logs = if (activity.logText.isNotBlank()) listOf(LogEntry(state.age, activity.logText, activity.logKind)) else emptyList()
                Triple(state, logs, emptyList())
            }
        }
    }

    /** Attempt a crime: pull it off for a payoff, or get caught and sentenced. */
    private fun commitCrime(state: GameState, crimeId: String): Triple<GameState, List<LogEntry>, List<String>> {
        val crime = CrimeCatalog.byId(crimeId) ?: return Triple(state, emptyList(), emptyList())
        val rng = rngFor(state.rngSeed, state.age, "crime-$crimeId-${state.log.size}")
        val smarts = state.character.stats.smarts
        val lucky = "lucky" in state.traits
        val catch = (crime.catchChance - smarts / 400.0 - (if (lucky) 0.08 else 0.0)).coerceIn(0.05, 0.95)
        return if (rng.nextDouble() < catch) {
            val sentence = crime.sentence.first + rng.nextInt(0, crime.sentence.last - crime.sentence.first + 1)
            val years = if (sentence == 1) "1 year" else "$sentence years"
            val s = applyEffects(state, listOf(Effect.Imprison(sentence), happy(-6)))
            Triple(
                s,
                listOf(LogEntry(s.age, "${crime.label}: you were caught and sentenced to $years in prison.", LogKind.MILESTONE)),
                listOf("Busted — $years inside."),
            )
        } else {
            val loot = crime.payoff.first + rng.nextInt(0, crime.payoff.last - crime.payoff.first + 1)
            val s = applyEffects(state, listOf(cashE(loot), happy(2), Effect.AddFlag("criminal")))
            Triple(
                s,
                listOf(LogEntry(s.age, "${crime.label}: you got away with ${money(loot)}.", LogKind.MONEY)),
                listOf("You got away with ${money(loot)}!"),
            )
        }
    }

    /** Seek medical care: acute conditions usually clear, chronic ones with a chance. */
    private fun getTreatment(state: GameState): Triple<GameState, List<LogEntry>, List<String>> {
        if (state.ailments.isEmpty()) return Triple(state, emptyList(), emptyList())
        val rng = rngFor(state.rngSeed, state.age, "treat-${state.log.size}")
        val remaining = mutableListOf<Ailment>()
        val cured = mutableListOf<String>()
        for (a in state.ailments) {
            val chance = when {
                !a.chronic -> 0.85
                a.severity >= 3 -> 0.35
                a.severity == 2 -> 0.5
                else -> 0.65
            }
            if (rng.nextDouble() < chance) cured += a.name else remaining += a
        }
        val s = state.copy(
            ailments = remaining,
            character = state.character.copy(stats = state.character.stats.withDelta(Stat.HEALTH, 6)),
        )
        return if (cured.isNotEmpty()) {
            Triple(
                s,
                listOf(LogEntry(s.age, "Treatment cleared: ${cured.joinToString(", ")}.", LogKind.HEALTH)),
                listOf("Recovered from ${cured.size} condition${if (cured.size == 1) "" else "s"}."),
            )
        } else {
            Triple(
                s,
                listOf(LogEntry(s.age, "The treatment helped you cope, but didn't beat it this time.", LogKind.HEALTH)),
                listOf("No cure this round, but you're managing."),
            )
        }
    }

    /** Buy an asset (the price was already paid); records it and lifts your mood. */
    private fun buyAsset(state: GameState, specId: String): Triple<GameState, List<LogEntry>, List<String>> {
        val spec = AssetCatalog.spec(specId) ?: return Triple(state, emptyList(), emptyList())
        val s = applyEffects(state, listOf(Effect.AddAsset(specId)))
        return Triple(
            s,
            listOf(LogEntry(s.age, "You bought a ${spec.name.lowercase()}.", LogKind.MONEY)),
            listOf("You bought a ${spec.name}!"),
        )
    }

    /** Keep your nose clean inside for a chance at a shorter sentence. */
    private fun goodBehavior(state: GameState): Triple<GameState, List<LogEntry>, List<String>> {
        val prison = state.prison ?: return Triple(state, emptyList(), emptyList())
        val rng = rngFor(state.rngSeed, state.age, "parole-${state.log.size}")
        return if (prison.sentence > 1 && rng.nextDouble() < 0.35) {
            Triple(
                state.copy(prison = prison.copy(sentence = prison.sentence - 1)),
                listOf(LogEntry(state.age, "Good behaviour earned you a year off your sentence.", LogKind.NEUTRAL)),
                listOf("A year off your sentence!"),
            )
        } else {
            Triple(
                state,
                listOf(LogEntry(state.age, "You kept your head down this year.", LogKind.NEUTRAL)),
                listOf("You stayed out of trouble."),
            )
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
    private fun smartsE(amount: Int) = Effect.StatDelta(Stat.SMARTS, amount)
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
        const val TRIP_COST = 800
        const val UNI_TUITION = 4000
        const val GRAD_TUITION = 8000
        // The estate passes tax-free up to the exemption; the rest is taxed before
        // being split among heirs, so wealth erodes across generations rather than
        // compounding into a dynasty money-printer.
        const val ESTATE_EXEMPTION = 50_000
        const val ESTATE_TAX_RATE = 0.4
    }
}
