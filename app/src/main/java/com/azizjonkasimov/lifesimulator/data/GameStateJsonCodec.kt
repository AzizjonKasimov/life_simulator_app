package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.model.BusinessState
import com.azizjonkasimov.lifesimulator.domain.model.BusinessTier
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.EconomyState
import com.azizjonkasimov.lifesimulator.domain.model.FinanceState
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind
import com.azizjonkasimov.lifesimulator.domain.model.Investment
import com.azizjonkasimov.lifesimulator.domain.model.InvestmentType
import com.azizjonkasimov.lifesimulator.domain.model.JobSearchState
import com.azizjonkasimov.lifesimulator.domain.model.LifeModifier
import com.azizjonkasimov.lifesimulator.domain.model.LifeProfile
import com.azizjonkasimov.lifesimulator.domain.model.PendingDecision
import com.azizjonkasimov.lifesimulator.domain.model.RelationshipState
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet
import org.json.JSONArray
import org.json.JSONObject

object GameStateJsonCodec {
    const val SCHEMA_VERSION = 5

    fun encode(state: GameState): String = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("profile", state.profile.toJson())
        .put("calendar", state.calendar.toJson())
        .put("stats", state.stats.toJson())
        .put("skills", state.skills.toJson())
        .put("finances", state.finances.toJson())
        .put("economy", state.economy.toJson())
        .put("career", state.career.toJson())
        .put("jobSearch", state.jobSearch.toJson())
        .put("business", state.business.toJson())
        .put("relationships", state.relationships.toJson())
        .put("modifiers", state.modifiers.modifiersToJsonArray())
        .put("pendingDecision", state.pendingDecision?.let { JSONObject().put("eventId", it.eventId) } ?: JSONObject.NULL)
        .put("rngSeed", state.rngSeed)
        .put("history", state.history.historyToJsonArray())
        .put("completedGoals", JSONArray().also { array -> state.completedGoals.forEach { array.put(it) } })
        .toString()

    fun decode(raw: String): GameState {
        val root = JSONObject(raw)
        return GameState(
            profile = root.getJSONObject("profile").toProfile(),
            calendar = root.getJSONObject("calendar").toCalendar(),
            stats = root.getJSONObject("stats").toStats(),
            skills = root.getJSONObject("skills").toSkills(),
            finances = root.getJSONObject("finances").toFinances(),
            economy = root.getJSONObject("economy").toEconomy(),
            career = root.getJSONObject("career").toCareer(),
            jobSearch = root.getJSONObject("jobSearch").toJobSearch(),
            business = root.getJSONObject("business").toBusiness(),
            relationships = root.getJSONObject("relationships").toRelationships(),
            modifiers = root.getJSONArray("modifiers").toModifierList(),
            pendingDecision = root.optJSONObject("pendingDecision")?.let { PendingDecision(it.getString("eventId")) },
            rngSeed = root.getLong("rngSeed"),
            history = root.getJSONArray("history").toHistoryList(),
            // Read with a default so saves written before v0.9.0 still load (no wipe).
            completedGoals = root.optJSONArray("completedGoals")?.let { array -> (0 until array.length()).map { array.getString(it) } } ?: emptyList(),
        )
    }
}

private fun LifeProfile.toJson(): JSONObject = JSONObject().put("name", name).put("age", age)

private fun JSONObject.toProfile(): LifeProfile = LifeProfile(name = getString("name"), age = getInt("age"))

private fun CalendarState.toJson(): JSONObject = JSONObject()
    .put("day", day)
    .put("timeRemaining", timeRemaining)
    .put("actionsToday", actionsToday)

private fun JSONObject.toCalendar(): CalendarState = CalendarState(
    day = getInt("day"),
    timeRemaining = getInt("timeRemaining"),
    actionsToday = optInt("actionsToday", 0),
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
    .put("gigsThisWeek", gigsThisWeek)

// gigsThisWeek is read with a default so saves written before v0.9.0 still load (no wipe).
private fun JSONObject.toFinances(): FinanceState = FinanceState(
    cash = getInt("cash"),
    debt = getInt("debt"),
    weeklyLivingCost = getInt("weeklyLivingCost"),
    nextBillDueDay = getInt("nextBillDueDay"),
    creditScore = getInt("creditScore"),
    gigsThisWeek = optInt("gigsThisWeek", 0),
).normalized()

private fun EconomyState.toJson(): JSONObject = JSONObject()
    .put("savings", savings)
    .put("investments", JSONArray().also { array -> investments.forEach { array.put(it.toJson()) } })
    .put("ownedAssets", JSONArray().also { array -> ownedAssets.forEach { array.put(it) } })
    .put("autoSavePercent", autoSavePercent)
    .put("autoInvestPercent", autoInvestPercent)
    .put("autoInvestType", autoInvestType.name)
    .put("lifetimeInterest", lifetimeInterest)

// New economy fields are read with defaults so saves written before v0.8.0 still load (no wipe).
private fun JSONObject.toEconomy(): EconomyState = EconomyState(
    savings = getInt("savings"),
    investments = getJSONArray("investments").let { array -> (0 until array.length()).map { array.getJSONObject(it).toInvestment() } },
    ownedAssets = getJSONArray("ownedAssets").let { array -> (0 until array.length()).map { array.getString(it) } },
    autoSavePercent = optInt("autoSavePercent", 0),
    autoInvestPercent = optInt("autoInvestPercent", 0),
    autoInvestType = runCatching { InvestmentType.valueOf(optString("autoInvestType", InvestmentType.INDEX.name)) }.getOrDefault(InvestmentType.INDEX),
    lifetimeInterest = optInt("lifetimeInterest", 0),
).normalized()

private fun Investment.toJson(): JSONObject = JSONObject()
    .put("type", type.name)
    .put("principal", principal)
    .put("currentValue", currentValue)

private fun JSONObject.toInvestment(): Investment = Investment(
    type = InvestmentType.valueOf(getString("type")),
    principal = getInt("principal"),
    currentValue = getInt("currentValue"),
)

private fun CareerState.toJson(): JSONObject = JSONObject()
    .put("title", title)
    .put("level", level)
    .put("reputation", reputation)
    .put("promotionReadiness", promotionReadiness)
    .put("salaryPerShift", salaryPerShift)
    .put("employed", employed)

private fun JSONObject.toCareer(): CareerState = CareerState(
    title = getString("title"),
    level = getInt("level"),
    reputation = getInt("reputation"),
    promotionReadiness = getInt("promotionReadiness"),
    salaryPerShift = getInt("salaryPerShift"),
    employed = getBoolean("employed"),
).normalized()

private fun JobSearchState.toJson(): JSONObject = JSONObject().put("searchProgress", searchProgress)

private fun JSONObject.toJobSearch(): JobSearchState = JobSearchState(searchProgress = getInt("searchProgress")).normalized()

private fun BusinessState.toJson(): JSONObject = JSONObject()
    .put("tier", tier.name)
    .put("clients", clients)
    .put("reputation", reputation)

private fun JSONObject.toBusiness(): BusinessState = BusinessState(
    tier = BusinessTier.valueOf(getString("tier")),
    clients = getInt("clients"),
    reputation = getInt("reputation"),
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

private fun List<LifeModifier>.modifiersToJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}

private fun JSONArray.toModifierList(): List<LifeModifier> =
    (0 until length()).map { getJSONObject(it).toModifier() }

private fun List<HistoryEntry>.historyToJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}

private fun JSONArray.toHistoryList(): List<HistoryEntry> =
    (0 until length()).map { getJSONObject(it).toHistory() }
