package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.azizjonkasimov.lifesimulator.data.SaveRepository
import com.azizjonkasimov.lifesimulator.domain.engine.InteractionOption
import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.Person
import com.azizjonkasimov.lifesimulator.domain.model.SimulationResult
import com.azizjonkasimov.lifesimulator.domain.model.StatChange
import kotlinx.coroutines.launch

class LifeSimulatorViewModel(
    private val repository: SaveRepository,
    private val engine: LifeSimulationEngine,
) : ViewModel() {
    var uiState by mutableStateOf(LifeSimulatorUiState())
        private set

    private var currentGameState: GameState? = null
    private var messages: List<String> = emptyList()
    private var statChanges: List<StatChange> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeGameState().collect { savedState ->
                currentGameState = savedState
                rebuildState(isLoading = false)
            }
        }
    }

    fun startNewLife(name: String, gender: Gender) {
        viewModelScope.launch {
            val gameState = engine.startNewLife(name, gender)
            messages = listOf("A new life begins.")
            statChanges = emptyList()
            repository.saveGameState(gameState)
        }
    }

    fun ageUp() = dispatch { engine.ageUp(it) }

    fun resolveEvent(eventId: String, choiceIndex: Int) =
        dispatch { engine.resolveEvent(it, eventId, choiceIndex) }

    fun doActivity(activityId: String) = dispatch { engine.doActivity(it, activityId) }

    fun interact(personId: String, interactionId: String) =
        dispatch { engine.interact(it, personId, interactionId) }

    fun interactionsFor(person: Person): List<InteractionOption> =
        currentGameState?.let { engine.interactionsFor(it, person) }.orEmpty()

    fun resetSave() {
        viewModelScope.launch {
            messages = emptyList()
            statChanges = emptyList()
            repository.resetSave()
        }
    }

    /** Living children who can carry on the bloodline, each with what they'd inherit. */
    fun heirOptions(): List<HeirOption> {
        val state = currentGameState ?: return emptyList()
        val share = engine.estateShareEach(state)
        return engine.eligibleHeirs(state).map { HeirOption(it, share) }
    }

    fun continueAsHeir(heirId: String) {
        val state = currentGameState ?: return
        val result = engine.continueAsHeir(state, heirId)
        viewModelScope.launch {
            messages = result.errorMessage?.let { listOf(it) } ?: result.messages
            statChanges = emptyList()
            if (result.success) {
                repository.saveGameState(result.state)
            } else {
                rebuildState(isLoading = false)
            }
        }
    }

    fun selectTab(tab: GameTab) = rebuildState(isLoading = false, selectedTab = tab)

    fun showMessage(message: String) {
        messages = listOf(message)
        statChanges = emptyList()
        rebuildState(isLoading = false)
    }

    private fun dispatch(operation: (GameState) -> SimulationResult) {
        val state = currentGameState ?: return
        val result = operation(state)
        viewModelScope.launch {
            messages = result.errorMessage?.let { listOf(it) } ?: result.messages
            statChanges = result.statChanges
            if (result.success) {
                repository.saveGameState(result.state)
            } else {
                rebuildState(isLoading = false)
            }
        }
    }

    private fun rebuildState(
        isLoading: Boolean,
        selectedTab: GameTab = uiState.selectedTab,
    ) {
        val state = currentGameState
        uiState = LifeSimulatorUiState(
            isLoading = isLoading,
            gameState = state,
            selectedTab = selectedTab,
            activities = state?.let(engine::availableActivities).orEmpty(),
            pendingEvent = state?.let(engine::pendingEvent),
            messages = messages,
            statChanges = statChanges,
        )
    }
}

class LifeSimulatorViewModelFactory(
    private val repository: SaveRepository,
    private val engine: LifeSimulationEngine,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LifeSimulatorViewModel::class.java)) {
            return LifeSimulatorViewModel(repository, engine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
