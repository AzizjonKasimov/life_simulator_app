package com.azizjonkasimov.lifesimulator.ui

import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
import com.azizjonkasimov.lifesimulator.domain.model.GameState

data class LifeSimulatorUiState(
    val isLoading: Boolean = true,
    val gameState: GameState? = null,
    val actions: List<ActionAvailability> = emptyList(),
    val dashboard: DashboardSnapshot? = null,
    val selectedTab: GameTab = GameTab.DASHBOARD,
    val messages: List<String> = emptyList(),
    val lastActionDeltas: List<ActionDelta> = emptyList(),
)

enum class GameTab(
    val label: String,
    val iconText: String,
) {
    DASHBOARD(label = "Dashboard", iconText = "D"),
    ACTIONS(label = "Actions", iconText = "A"),
    PROGRESS(label = "Progress", iconText = "P"),
    HISTORY(label = "History", iconText = "H"),
}
