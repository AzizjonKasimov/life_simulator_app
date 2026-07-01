package com.azizjonkasimov.lifesimulator.domain.model

data class GameState(
    val profile: LifeProfile,
    val calendar: CalendarState,
    val stats: CoreStats,
    val skills: SkillSet,
    val finances: FinanceState,
    val economy: EconomyState,
    val career: CareerState,
    val jobSearch: JobSearchState,
    val business: BusinessState,
    val relationships: RelationshipState,
    val modifiers: List<LifeModifier>,
    val rngSeed: Long,
    val history: List<HistoryEntry>,
    /** Ids of goals already reached and celebrated. Monotonic — a goal stays earned. */
    val completedGoals: List<String> = emptyList(),
) {
    val day: Int
        get() = calendar.day

    val week: Int
        get() = calendar.week

    val timeRemaining: Int
        get() = calendar.timeRemaining

    val actionsToday: Int
        get() = calendar.actionsToday
}

data class LifeProfile(
    val name: String,
    val age: Int,
)

data class CalendarState(
    val day: Int,
    val timeRemaining: Int,
    val actionsToday: Int = 0,
) {
    val week: Int
        get() = ((day - 1) / 7) + 1
}

data class FinanceState(
    val cash: Int,
    val debt: Int,
    val weeklyLivingCost: Int,
    val nextBillDueDay: Int,
    val creditScore: Int,
    /** Gigs worked since the last payday. Gig pay diminishes as this climbs, then resets weekly. */
    val gigsThisWeek: Int = 0,
) {
    fun normalized(): FinanceState = copy(
        cash = cash.coerceAtLeast(0),
        debt = debt.coerceAtLeast(0),
        weeklyLivingCost = weeklyLivingCost.coerceAtLeast(0),
        nextBillDueDay = nextBillDueDay.coerceAtLeast(1),
        creditScore = creditScore.coerceIn(300, 850),
        gigsThisWeek = gigsThisWeek.coerceAtLeast(0),
    )
}

data class CareerState(
    val title: String,
    val level: Int,
    val reputation: Int,
    val promotionReadiness: Int,
    val salaryPerShift: Int,
    val employed: Boolean,
) {
    fun normalized(): CareerState = copy(
        level = level.coerceAtLeast(if (employed) 1 else 0),
        reputation = reputation.coerceIn(0, 100),
        promotionReadiness = promotionReadiness.coerceIn(0, 100),
        salaryPerShift = salaryPerShift.coerceAtLeast(0),
    )
}

/** A single legible "how close am I to a job" meter, built by applying and prepping. */
data class JobSearchState(
    val searchProgress: Int,
) {
    fun normalized(): JobSearchState = copy(
        searchProgress = searchProgress.coerceIn(0, 100),
    )
}

enum class BusinessTier(
    val label: String,
    val revenuePerClient: Int,
    val maxClients: Int,
    val weeklyOverhead: Int,
    val upgradeCost: Int,
) {
    NONE("Not started", revenuePerClient = 0, maxClients = 0, weeklyOverhead = 0, upgradeCost = 0),
    SIDE_HUSTLE("Side Hustle", revenuePerClient = 45, maxClients = 3, weeklyOverhead = 0, upgradeCost = 500),
    STUDIO("Studio", revenuePerClient = 80, maxClients = 6, weeklyOverhead = 60, upgradeCost = 1800),
    AGENCY("Agency", revenuePerClient = 130, maxClients = 12, weeklyOverhead = 150, upgradeCost = 6000),
    FIRM("Firm", revenuePerClient = 200, maxClients = 20, weeklyOverhead = 400, upgradeCost = 0),
}

/**
 * A small business as a legible income engine: paying [clients] times the tier's
 * rate, scaled by [reputation]. Grow clients, raise reputation, upgrade the tier.
 */
data class BusinessState(
    val tier: BusinessTier,
    val clients: Int,
    val reputation: Int,
) {
    val started: Boolean
        get() = tier != BusinessTier.NONE

    fun normalized(): BusinessState = copy(
        clients = clients.coerceIn(0, tier.maxClients),
        reputation = reputation.coerceIn(0, 100),
    )
}

data class RelationshipState(
    val family: Int,
    val friends: Int,
    val network: Int,
) {
    val average: Int
        get() = ((family + friends + network) / 3).coerceIn(0, 100)

    fun clamped(): RelationshipState = copy(
        family = family.coerceIn(0, 100),
        friends = friends.coerceIn(0, 100),
        network = network.coerceIn(0, 100),
    )
}

data class LifeModifier(
    val id: String,
    val title: String,
    val description: String,
    val daysRemaining: Int,
    val healthDelta: Int = 0,
    val moodDelta: Int = 0,
    val energyDelta: Int = 0,
    val stressDelta: Int = 0,
) {
    fun tick(): LifeModifier = copy(daysRemaining = daysRemaining - 1)
}

/** Light, computed overview for the dashboard. Presentation specifics are read from state. */
data class DashboardSnapshot(
    val netWorth: Int,
    val weeklyCost: Int,
    val businessWeeklyNet: Int,
    val nextBillLabel: String,
    val pressureSummary: String,
    val suggestionIds: List<String>,
    val alerts: List<String>,
)
