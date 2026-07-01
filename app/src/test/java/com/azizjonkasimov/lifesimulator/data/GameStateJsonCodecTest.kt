package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.Job
import com.azizjonkasimov.lifesimulator.domain.model.JobField
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateJsonCodecTest {
    private val engine = LifeSimulationEngine()

    @Test
    fun roundTrip_preservesAFreshLife() {
        val state = engine.ageUp(engine.startNewLife("Round Trip", Gender.FEMALE)).state
        val decoded = GameStateJsonCodec.decode(GameStateJsonCodec.encode(state))
        assertEquals(state, decoded)
    }

    @Test
    fun roundTrip_preservesJobFlagsAndDeath() {
        val base = engine.startNewLife("Worker", Gender.MALE)
        val state = base.copy(
            job = Job("clerk", "Office Clerk", JobField.OFFICE, salaryPerYear = 34000),
            flags = setOf("hs_grad", "homeowner"),
            eventsSeen = setOf("buy_house"),
            pendingEventIds = listOf("free_time"),
            activitiesUsed = setOf("gym"),
            alive = false,
            causeOfDeath = "old age",
        )
        val decoded = GameStateJsonCodec.decode(GameStateJsonCodec.encode(state))
        assertEquals(state, decoded)
    }
}
