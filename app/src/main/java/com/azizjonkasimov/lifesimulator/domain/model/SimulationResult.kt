package com.azizjonkasimov.lifesimulator.domain.model

/** The outcome of an engine operation: a new state plus what to tell the player. */
data class SimulationResult(
    val state: GameState,
    val success: Boolean,
    val messages: List<String> = emptyList(),
    val statChanges: List<StatChange> = emptyList(),
    val errorMessage: String? = null,
) {
    companion object {
        fun success(
            state: GameState,
            messages: List<String> = emptyList(),
            statChanges: List<StatChange> = emptyList(),
        ): SimulationResult = SimulationResult(state, true, messages, statChanges)

        fun failure(state: GameState, message: String): SimulationResult =
            SimulationResult(state, false, errorMessage = message)
    }
}
