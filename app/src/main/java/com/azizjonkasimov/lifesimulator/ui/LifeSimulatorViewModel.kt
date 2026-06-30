package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.azizjonkasimov.lifesimulator.data.SaveRepository
import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import kotlinx.coroutines.launch

class LifeSimulatorViewModel(
    private val repository: SaveRepository,
    private val engine: LifeSimulationEngine,
) : ViewModel() {
    var uiState by mutableStateOf(LifeSimulatorUiState())
        private set

    private var currentGameState: GameState? = null
    private var messages: List<String> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeGameState().collect { savedState ->
                currentGameState = savedState
                rebuildState(isLoading = false)
            }
        }
    }

    fun startNewLife(archetype: LifeArchetype) {
        viewModelScope.launch {
            val gameState = engine.startNewLife(archetype)
            messages = listOf("Started a new ${archetype.displayName} life.")
            repository.saveGameState(gameState)
        }
    }

    fun performAction(actionId: String) {
        val state = currentGameState ?: return
        val result = engine.performAction(state, actionId)
        viewModelScope.launch {
            messages = result.errorMessage?.let { listOf(it) } ?: result.messages
            if (result.success) {
                repository.saveGameState(result.state)
            } else {
                rebuildState(isLoading = false)
            }
        }
    }

    fun advanceDay() {
        val state = currentGameState ?: return
        val result = engine.advanceDay(state)
        viewModelScope.launch {
            messages = result.messages
            repository.saveGameState(result.state)
        }
    }

    fun resetSave() {
        viewModelScope.launch {
            messages = emptyList()
            repository.resetSave()
        }
    }

    fun selectTab(tab: GameTab) {
        rebuildState(isLoading = false, selectedTab = tab)
    }

    private fun rebuildState(
        isLoading: Boolean,
        selectedTab: GameTab = uiState.selectedTab,
    ) {
        val state = currentGameState
        uiState = LifeSimulatorUiState(
            isLoading = isLoading,
            gameState = state,
            actions = state?.let(engine::actionAvailability).orEmpty(),
            selectedTab = selectedTab,
            messages = messages,
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
