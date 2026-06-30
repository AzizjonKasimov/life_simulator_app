package com.azizjonkasimov.lifesimulator.domain.model

data class SimulationResult(
    val state: GameState,
    val success: Boolean,
    val messages: List<String>,
    val errorMessage: String? = null,
    val actionDeltas: List<ActionDelta> = emptyList(),
)
