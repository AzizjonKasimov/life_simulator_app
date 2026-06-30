package com.azizjonkasimov.lifesimulator.domain.model

data class GameState(
    val profile: LifeProfile,
    val calendar: CalendarState,
    val stats: CoreStats,
    val skills: SkillSet,
    val finances: FinanceState,
    val career: CareerState,
    val jobSearch: JobSearchState,
    val business: BusinessState,
    val relationships: RelationshipState,
    val modifiers: List<LifeModifier>,
    val dayPlan: DayPlanState,
    val timedOpportunities: List<TimedOpportunityState>,
    val opportunityCooldowns: Map<String, Int>,
    val rngSeed: Long,
    val history: List<HistoryEntry>,
) {
    val day: Int
        get() = calendar.day

    val week: Int
        get() = calendar.week

    val timeRemaining: Int
        get() = calendar.timeRemaining

}

data class LifeProfile(
    val name: String,
    val age: Int,
)

data class CalendarState(
    val day: Int,
    val timeRemaining: Int,
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
) {
    val netWorth: Int
        get() = cash - debt

    fun normalized(): FinanceState = copy(
        cash = cash.coerceAtLeast(0),
        debt = debt.coerceAtLeast(0),
        weeklyLivingCost = weeklyLivingCost.coerceAtLeast(0),
        nextBillDueDay = nextBillDueDay.coerceAtLeast(1),
        creditScore = creditScore.coerceIn(300, 850),
    )
}

data class CareerState(
    val title: String,
    val level: Int,
    val xp: Int,
    val reputation: Int,
    val promotionReadiness: Int,
    val salaryPerShift: Int,
    val employed: Boolean,
) {
    fun normalized(): CareerState = copy(
        level = level.coerceAtLeast(if (employed) 1 else 0),
        xp = xp.coerceAtLeast(0),
        reputation = reputation.coerceIn(0, 100),
        promotionReadiness = promotionReadiness.coerceIn(0, 100),
        salaryPerShift = salaryPerShift.coerceAtLeast(0),
    )
}

data class JobSearchState(
    val applicationsSent: Int,
    val interviewReadiness: Int,
    val offerProgress: Int,
) {
    fun normalized(): JobSearchState = copy(
        applicationsSent = applicationsSent.coerceAtLeast(0),
        interviewReadiness = interviewReadiness.coerceIn(0, 100),
        offerProgress = offerProgress.coerceIn(0, 100),
    )
}

enum class BusinessStage(val label: String) {
    IDEA("Idea"),
    SIDE_HUSTLE("Side Hustle"),
    RELIABLE_PIPELINE("Reliable Pipeline"),
    SMALL_BUSINESS("Small Business"),
}

data class BusinessState(
    val stage: BusinessStage,
    val leads: Int,
    val activeProjects: Int,
    val completedProjects: Int,
    val clientTrust: Int,
    val reputation: Int,
    val pipelineValue: Int,
) {
    fun normalized(): BusinessState = copy(
        leads = leads.coerceAtLeast(0),
        activeProjects = activeProjects.coerceAtLeast(0),
        completedProjects = completedProjects.coerceAtLeast(0),
        clientTrust = clientTrust.coerceIn(0, 100),
        reputation = reputation.coerceIn(0, 100),
        pipelineValue = pipelineValue.coerceIn(0, 250),
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

enum class DailyFocus(val label: String) {
    MONEY("Money"),
    CAREER("Career"),
    RECOVERY("Recovery"),
    SOCIAL("Social"),
    BALANCED("Balanced"),
}

data class DayPlanState(
    val day: Int,
    val recommendedFocus: DailyFocus,
    val activeFocus: DailyFocus,
    val reason: String,
    val locked: Boolean,
    val actionsTaken: Int,
    val focusActionsCompleted: Int,
    val categoriesCompleted: Set<ActionCategory>,
)

data class TimedOpportunityState(
    val id: String,
    val progress: Int,
    val target: Int,
    val baseline: Int,
    val expiresOnDay: Int,
)

data class DashboardSnapshot(
    val headline: String,
    val status: String,
    val alerts: List<String>,
    val pressureSummary: String,
    val quickActionIds: List<String>,
    val nextBillLabel: String,
    val netWorth: Int,
)
