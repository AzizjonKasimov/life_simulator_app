package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyFocus
import com.azizjonkasimov.lifesimulator.domain.model.DayPlanState
import com.azizjonkasimov.lifesimulator.domain.model.FinanceState
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.ActionCategory
import com.azizjonkasimov.lifesimulator.domain.model.GoalCategory
import com.azizjonkasimov.lifesimulator.domain.model.GoalState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import com.azizjonkasimov.lifesimulator.domain.model.LifeModifier
import com.azizjonkasimov.lifesimulator.domain.model.LifeProfile
import com.azizjonkasimov.lifesimulator.domain.model.RelationshipState
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet
import com.azizjonkasimov.lifesimulator.domain.model.TimedOpportunityState
import org.json.JSONArray
import org.json.JSONObject

object GameStateJsonCodec {
    const val SCHEMA_VERSION = 3

    fun encode(state: GameState): String = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("profile", state.profile.toJson())
        .put("calendar", state.calendar.toJson())
        .put("stats", state.stats.toJson())
        .put("skills", state.skills.toJson())
        .put("finances", state.finances.toJson())
        .put("career", state.career.toJson())
        .put("relationships", state.relationships.toJson())
        .put("goals", state.goals.goalsToJsonArray())
        .put("modifiers", state.modifiers.modifiersToJsonArray())
        .put("dayPlan", state.dayPlan.toJson())
        .put("timedOpportunities", state.timedOpportunities.timedOpportunitiesToJsonArray())
        .put("opportunityCooldowns", state.opportunityCooldowns.toJsonObject())
        .put("rngSeed", state.rngSeed)
        .put("history", state.history.historyToJsonArray())
        .toString()

    fun decode(raw: String): GameState {
        val root = JSONObject(raw)
        return GameState(
            profile = root.getJSONObject("profile").toProfile(),
            calendar = root.getJSONObject("calendar").toCalendar(),
            stats = root.getJSONObject("stats").toStats(),
            skills = root.getJSONObject("skills").toSkills(),
            finances = root.getJSONObject("finances").toFinances(),
            career = root.getJSONObject("career").toCareer(),
            relationships = root.getJSONObject("relationships").toRelationships(),
            goals = root.getJSONArray("goals").toGoalList(),
            modifiers = root.getJSONArray("modifiers").toModifierList(),
            dayPlan = root.getJSONObject("dayPlan").toDayPlan(),
            timedOpportunities = root.getJSONArray("timedOpportunities").toTimedOpportunityList(),
            opportunityCooldowns = root.getJSONObject("opportunityCooldowns").toStringIntMap(),
            rngSeed = root.getLong("rngSeed"),
            history = root.getJSONArray("history").toHistoryList(),
        )
    }
}

private fun LifeProfile.toJson(): JSONObject = JSONObject()
    .put("name", name)
    .put("age", age)
    .put("archetype", archetype.name)

private fun JSONObject.toProfile(): LifeProfile = LifeProfile(
    name = getString("name"),
    age = getInt("age"),
    archetype = LifeArchetype.valueOf(getString("archetype")),
)

private fun CalendarState.toJson(): JSONObject = JSONObject()
    .put("day", day)
    .put("timeRemaining", timeRemaining)

private fun JSONObject.toCalendar(): CalendarState = CalendarState(
    day = getInt("day"),
    timeRemaining = getInt("timeRemaining"),
)

private fun CoreStats.toJson(): JSONObject = JSONObject()
    .put("health", health)
    .put("mood", mood)
    .put("energy", energy)
    .put("stress", stress)
    .put("social", social)

private fun JSONObject.toStats(): CoreStats = CoreStats(
    health = getInt("health"),
    mood = getInt("mood"),
    energy = getInt("energy"),
    stress = getInt("stress"),
    social = getInt("social"),
).clamped()

private fun SkillSet.toJson(): JSONObject = JSONObject()
    .put("knowledge", knowledge)
    .put("fitness", fitness)
    .put("career", career)
    .put("communication", communication)
    .put("creativity", creativity)

private fun JSONObject.toSkills(): SkillSet = SkillSet(
    knowledge = getInt("knowledge"),
    fitness = getInt("fitness"),
    career = getInt("career"),
    communication = getInt("communication"),
    creativity = getInt("creativity"),
).clamped()

private fun FinanceState.toJson(): JSONObject = JSONObject()
    .put("cash", cash)
    .put("debt", debt)
    .put("weeklyLivingCost", weeklyLivingCost)
    .put("nextBillDueDay", nextBillDueDay)
    .put("creditScore", creditScore)

private fun JSONObject.toFinances(): FinanceState = FinanceState(
    cash = getInt("cash"),
    debt = getInt("debt"),
    weeklyLivingCost = getInt("weeklyLivingCost"),
    nextBillDueDay = getInt("nextBillDueDay"),
    creditScore = getInt("creditScore"),
).normalized()

private fun CareerState.toJson(): JSONObject = JSONObject()
    .put("title", title)
    .put("level", level)
    .put("xp", xp)
    .put("reputation", reputation)
    .put("promotionReadiness", promotionReadiness)
    .put("salaryPerShift", salaryPerShift)

private fun JSONObject.toCareer(): CareerState = CareerState(
    title = getString("title"),
    level = getInt("level"),
    xp = getInt("xp"),
    reputation = getInt("reputation"),
    promotionReadiness = getInt("promotionReadiness"),
    salaryPerShift = getInt("salaryPerShift"),
).normalized()

private fun RelationshipState.toJson(): JSONObject = JSONObject()
    .put("family", family)
    .put("friends", friends)
    .put("network", network)

private fun JSONObject.toRelationships(): RelationshipState = RelationshipState(
    family = getInt("family"),
    friends = getInt("friends"),
    network = getInt("network"),
).clamped()

private fun GoalState.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("description", description)
    .put("category", category.name)
    .put("progress", progress)
    .put("target", target)
    .put("rewardText", rewardText)

private fun JSONObject.toGoal(): GoalState = GoalState(
    id = getString("id"),
    title = getString("title"),
    description = getString("description"),
    category = GoalCategory.valueOf(getString("category")),
    progress = getInt("progress"),
    target = getInt("target"),
    rewardText = getString("rewardText"),
)

private fun LifeModifier.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("description", description)
    .put("daysRemaining", daysRemaining)
    .put("healthDelta", healthDelta)
    .put("moodDelta", moodDelta)
    .put("energyDelta", energyDelta)
    .put("stressDelta", stressDelta)

private fun JSONObject.toModifier(): LifeModifier = LifeModifier(
    id = getString("id"),
    title = getString("title"),
    description = getString("description"),
    daysRemaining = getInt("daysRemaining"),
    healthDelta = getInt("healthDelta"),
    moodDelta = getInt("moodDelta"),
    energyDelta = getInt("energyDelta"),
    stressDelta = getInt("stressDelta"),
)

private fun DayPlanState.toJson(): JSONObject = JSONObject()
    .put("day", day)
    .put("recommendedFocus", recommendedFocus.name)
    .put("activeFocus", activeFocus.name)
    .put("reason", reason)
    .put("locked", locked)
    .put("actionsTaken", actionsTaken)
    .put("focusActionsCompleted", focusActionsCompleted)
    .put("categoriesCompleted", categoriesCompleted.toJsonArray())

private fun JSONObject.toDayPlan(): DayPlanState = DayPlanState(
    day = getInt("day"),
    recommendedFocus = DailyFocus.valueOf(getString("recommendedFocus")),
    activeFocus = DailyFocus.valueOf(getString("activeFocus")),
    reason = getString("reason"),
    locked = getBoolean("locked"),
    actionsTaken = getInt("actionsTaken"),
    focusActionsCompleted = getInt("focusActionsCompleted"),
    categoriesCompleted = getJSONArray("categoriesCompleted").toActionCategorySet(),
)

private fun TimedOpportunityState.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("progress", progress)
    .put("target", target)
    .put("baseline", baseline)
    .put("expiresOnDay", expiresOnDay)

private fun JSONObject.toTimedOpportunity(): TimedOpportunityState = TimedOpportunityState(
    id = getString("id"),
    progress = getInt("progress"),
    target = getInt("target"),
    baseline = getInt("baseline"),
    expiresOnDay = getInt("expiresOnDay"),
)

private fun HistoryEntry.toJson(): JSONObject = JSONObject()
    .put("day", day)
    .put("title", title)
    .put("detail", detail)
    .put("kind", kind.name)

private fun JSONObject.toHistory(): HistoryEntry = HistoryEntry(
    day = getInt("day"),
    title = getString("title"),
    detail = getString("detail"),
    kind = HistoryKind.valueOf(getString("kind")),
)

private fun List<GoalState>.goalsToJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}

private fun JSONArray.toGoalList(): List<GoalState> =
    (0 until length()).map { getJSONObject(it).toGoal() }

private fun List<LifeModifier>.modifiersToJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}

private fun JSONArray.toModifierList(): List<LifeModifier> =
    (0 until length()).map { getJSONObject(it).toModifier() }

private fun Set<ActionCategory>.toJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.name) }
}

private fun JSONArray.toActionCategorySet(): Set<ActionCategory> =
    (0 until length()).map { ActionCategory.valueOf(getString(it)) }.toSet()

private fun List<TimedOpportunityState>.timedOpportunitiesToJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}

private fun JSONArray.toTimedOpportunityList(): List<TimedOpportunityState> =
    (0 until length()).map { getJSONObject(it).toTimedOpportunity() }

private fun Map<String, Int>.toJsonObject(): JSONObject = JSONObject().also { json ->
    forEach { (key, value) -> json.put(key, value) }
}

private fun JSONObject.toStringIntMap(): Map<String, Int> =
    keys().asSequence().associateWith { key -> getInt(key) }

private fun List<HistoryEntry>.historyToJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}

private fun JSONArray.toHistoryList(): List<HistoryEntry> =
    (0 until length()).map { getJSONObject(it).toHistory() }
