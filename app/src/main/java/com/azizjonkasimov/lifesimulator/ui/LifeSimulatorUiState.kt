package com.azizjonkasimov.lifesimulator.ui

import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.GoalStatus
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition
import com.azizjonkasimov.lifesimulator.domain.model.PassiveIncomeBreakdown

data class LifeSimulatorUiState(
    val isLoading: Boolean = true,
    val gameState: GameState? = null,
    val actions: List<ActionAvailability> = emptyList(),
    val dashboard: DashboardSnapshot? = null,
    val pendingDecision: LifeEventDefinition? = null,
    val netWorth: Int = 0,
    val weeklyCost: Int = 0,
    val passiveIncome: PassiveIncomeBreakdown = PassiveIncomeBreakdown.EMPTY,
    val goals: List<GoalStatus> = emptyList(),
    val selectedTab: GameTab = GameTab.DASHBOARD,
    val messages: List<String> = emptyList(),
    val lastActionDeltas: List<ActionDelta> = emptyList(),
)

enum class GameTab(
    val label: String,
) {
    DASHBOARD(label = "Home"),
    ACTIONS(label = "Actions"),
    MONEY(label = "Money"),
    PROGRESS(label = "Stats"),
    HISTORY(label = "History"),
}
