package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SaveRepository(
    private val gameStateDao: GameStateDao,
) {
    fun observeGameState(): Flow<GameState?> =
        gameStateDao.observeGameState().map { it?.toDomain() }

    suspend fun getGameState(): GameState? =
        gameStateDao.getGameState()?.toDomain()

    suspend fun saveGameState(gameState: GameState) {
        gameStateDao.saveGameState(gameState.toEntity())
    }

    suspend fun resetSave() {
        gameStateDao.clear()
    }
}
