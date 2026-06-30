package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateJsonCodecTest {
    @Test
    fun gameStateRoundTripsThroughJson() {
        val state = LifeSimulationEngine().startNewLife(LifeArchetype.JUNIOR_WORKER)
        val encoded = GameStateJsonCodec.encode(state)
        val decoded = GameStateJsonCodec.decode(encoded)

        assertEquals(state, decoded)
    }

    @Test
    fun entityUsesV2SchemaVersion() {
        val state = LifeSimulationEngine().startNewLife(LifeArchetype.STUDENT)
        val entity = state.toEntity()

        assertEquals(2, entity.schemaVersion)
        assertEquals(state, entity.toDomain())
    }
}
