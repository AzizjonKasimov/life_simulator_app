package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaveRepositoryTest {
    @Test
    fun saveLoadsAfterRepositoryRecreation() = runBlocking {
        val dao = FakeGameStateDao()
        val firstRepository = SaveRepository(dao)
        val savedState = LifeSimulationEngine().startNewLife()

        firstRepository.saveGameState(savedState)
        val secondRepository = SaveRepository(dao)

        assertEquals(savedState, secondRepository.observeGameState().first())
    }

    @Test
    fun staleSchemaVersionLoadsAsNoSave() = runBlocking {
        val dao = FakeGameStateDao()
        dao.saveGameState(
            GameStateEntity(
                schemaVersion = GameStateJsonCodec.SCHEMA_VERSION - 1,
                stateJson = "{}",
            ),
        )

        val repository = SaveRepository(dao)

        assertNull(repository.observeGameState().first())
        assertNull(repository.getGameState())
    }

    @Test
    fun resetClearsSingleSave() = runBlocking {
        val dao = FakeGameStateDao()
        val repository = SaveRepository(dao)
        val savedState = LifeSimulationEngine().startNewLife()

        repository.saveGameState(savedState)
        repository.resetSave()

        assertNull(repository.getGameState())
    }

    private class FakeGameStateDao : GameStateDao {
        private val state = MutableStateFlow<GameStateEntity?>(null)

        override fun observeGameState(): Flow<GameStateEntity?> = state

        override suspend fun getGameState(): GameStateEntity? = state.value

        override suspend fun saveGameState(gameState: GameStateEntity) {
            state.value = gameState
        }

        override suspend fun clear() {
            state.value = null
        }
    }
}
