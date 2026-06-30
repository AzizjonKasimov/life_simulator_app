package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GameStateJsonCodecTest {
    @Test
    fun gameStateRoundTripsThroughJson() {
        val state = LifeSimulationEngine().startNewLife()
        val encoded = GameStateJsonCodec.encode(state)
        val decoded = GameStateJsonCodec.decode(encoded)

        assertFalse(encoded.contains("\"goals\""))
        assertEquals(state, decoded)
    }

    @Test
    fun entityUsesV4SchemaVersion() {
        val state = LifeSimulationEngine().startNewLife()
        val entity = state.toEntity()

        assertEquals(4, entity.schemaVersion)
        assertEquals(state, entity.toDomain())
    }
}
