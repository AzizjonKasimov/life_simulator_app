package com.azizjonkasimov.lifesimulator.ui

import com.azizjonkasimov.lifesimulator.domain.engine.ActivityOption
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LifeEvent
import com.azizjonkasimov.lifesimulator.domain.model.Person
import com.azizjonkasimov.lifesimulator.domain.model.StatChange

data class LifeSimulatorUiState(
    val isLoading: Boolean = true,
    val gameState: GameState? = null,
    val selectedTab: GameTab = GameTab.LIFE,
    val activities: List<ActivityOption> = emptyList(),
    val pendingEvent: LifeEvent? = null,
    val messages: List<String> = emptyList(),
    val statChanges: List<StatChange> = emptyList(),
)

/** A child you could continue as after death, and the estate share they'd inherit. */
data class HeirOption(val person: Person, val inheritance: Int)

enum class GameTab(val label: String) {
    LIFE("Life"),
    ACTIVITIES("Activities"),
    PEOPLE("People"),
    PROFILE("Profile"),
}
