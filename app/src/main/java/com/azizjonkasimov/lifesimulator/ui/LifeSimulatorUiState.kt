package com.azizjonkasimov.lifesimulator.ui

import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.GameState

data class LifeSimulatorUiState(
    val isLoading: Boolean = true,
    val gameState: GameState? = null,
    val actions: List<ActionAvailability> = emptyList(),
    val selectedTab: GameTab = GameTab.DASHBOARD,
    val messages: List<String> = emptyList(),
)

enum class GameTab(
    val label: String,
    val iconText: String,
) {
    DASHBOARD(label = "Dashboard", iconText = "D"),
    ACTIONS(label = "Actions", iconText = "A"),
    HISTORY(label = "History", iconText = "H"),
    PROGRESS(label = "Progress", iconText = "P"),
}
