package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.azizjonkasimov.lifesimulator.data.SaveRepository
import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.InvestmentType
import com.azizjonkasimov.lifesimulator.domain.model.PassiveIncomeBreakdown
import com.azizjonkasimov.lifesimulator.domain.model.SimulationResult
import kotlinx.coroutines.launch

class LifeSimulatorViewModel(
    private val repository: SaveRepository,
    private val engine: LifeSimulationEngine,
) : ViewModel() {
    var uiState by mutableStateOf(LifeSimulatorUiState())
        private set

    private var currentGameState: GameState? = null
    private var messages: List<String> = emptyList()
    private var lastActionDeltas: List<ActionDelta> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeGameState().collect { savedState ->
                currentGameState = savedState
                rebuildState(isLoading = false)
            }
        }
    }

    fun startNewLife() {
        viewModelScope.launch {
            val gameState = engine.startNewLife()
            messages = listOf("A fresh start.")
            lastActionDeltas = emptyList()
            repository.saveGameState(gameState)
        }
    }

    fun performAction(actionId: String) {
        dispatch(deltasFromResult = true) { engine.performAction(it, actionId) }
    }

    fun advanceDay() {
        dispatch { engine.advanceDay(it) }
    }

    fun deposit(amount: Int) = dispatch { engine.deposit(it, amount) }
    fun withdraw(amount: Int) = dispatch { engine.withdraw(it, amount) }
    fun payDebt(amount: Int) = dispatch { engine.payDebt(it, amount) }
    fun invest(type: InvestmentType, amount: Int) = dispatch { engine.invest(it, type, amount) }
    fun sellInvestment(type: InvestmentType) = dispatch { engine.sellInvestment(it, type) }
    fun buyAsset(assetId: String) = dispatch { engine.buyAsset(it, assetId) }
    fun sellAsset(assetId: String) = dispatch { engine.sellAsset(it, assetId) }
    fun setAutoSave(percent: Int) = dispatch { engine.setAutoSave(it, percent) }
    fun setAutoInvest(percent: Int, type: InvestmentType) = dispatch { engine.setAutoInvest(it, percent, type) }

    fun resetSave() {
        viewModelScope.launch {
            messages = emptyList()
            lastActionDeltas = emptyList()
            repository.resetSave()
        }
    }

    fun showMessage(message: String) {
        messages = listOf(message)
        lastActionDeltas = emptyList()
        rebuildState(isLoading = false)
    }

    fun selectTab(tab: GameTab) {
        rebuildState(isLoading = false, selectedTab = tab)
    }

    /** Runs an engine operation, surfaces its messages, and persists on success. */
    private fun dispatch(
        deltasFromResult: Boolean = false,
        operation: (GameState) -> SimulationResult,
    ) {
        val state = currentGameState ?: return
        val result = operation(state)
        viewModelScope.launch {
            messages = result.errorMessage?.let { listOf(it) } ?: result.messages
            lastActionDeltas = if (deltasFromResult) result.actionDeltas else emptyList()
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
            actions = state?.let(engine::actionAvailability).orEmpty(),
            dashboard = state?.let(engine::dashboardSnapshot),
            netWorth = state?.let(engine::netWorth) ?: 0,
            weeklyCost = state?.let(engine::weeklyLivingTotal) ?: 0,
            passiveIncome = state?.let(engine::passiveIncome) ?: PassiveIncomeBreakdown.EMPTY,
            goals = state?.let(engine::goalStatuses).orEmpty(),
            selectedTab = selectedTab,
            messages = messages,
            lastActionDeltas = lastActionDeltas,
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
