package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import com.azizjonkasimov.lifesimulator.domain.model.InvestmentType
import org.json.JSONObject
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
    fun legacyEconomyJsonDecodesWithAutoAllocationDefaults() {
        // A save written before v0.8.0 has no auto-allocation fields; it must still load (no wipe).
        val state = LifeSimulationEngine().startNewLife()
        val root = JSONObject(GameStateJsonCodec.encode(state))
        val economy = root.getJSONObject("economy")
        listOf("autoSavePercent", "autoInvestPercent", "autoInvestType", "lifetimeInterest").forEach { economy.remove(it) }

        val decoded = GameStateJsonCodec.decode(root.toString())

        assertEquals(0, decoded.economy.autoSavePercent)
        assertEquals(0, decoded.economy.autoInvestPercent)
        assertEquals(InvestmentType.INDEX, decoded.economy.autoInvestType)
        assertEquals(0, decoded.economy.lifetimeInterest)
    }

    @Test
    fun entityUsesCurrentSchemaVersion() {
        val state = LifeSimulationEngine().startNewLife()
        val entity = state.toEntity()

        assertEquals(GameStateJsonCodec.SCHEMA_VERSION, entity.schemaVersion)
        assertEquals(state, entity.toDomain())
    }
}
