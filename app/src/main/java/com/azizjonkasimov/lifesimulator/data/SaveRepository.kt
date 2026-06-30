package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SaveRepository(
    private val gameStateDao: GameStateDao,
) {
    fun observeGameState(): Flow<GameState?> =
        gameStateDao.observeGameState().map { it.safeToDomain() }

    suspend fun getGameState(): GameState? =
        gameStateDao.getGameState().safeToDomain()

    suspend fun saveGameState(gameState: GameState) {
        gameStateDao.saveGameState(gameState.toEntity())
    }

    suspend fun resetSave() {
        gameStateDao.clear()
    }

    /**
     * A save written by an older schema (or any corrupt row) must never crash the
     * app. If the stored version does not match, or decoding throws, we treat it as
     * no save and the player simply starts fresh.
     */
    private fun GameStateEntity?.safeToDomain(): GameState? {
        val entity = this ?: return null
        if (entity.schemaVersion != GameStateJsonCodec.SCHEMA_VERSION) return null
        return runCatching { entity.toDomain() }.getOrNull()
    }
}
