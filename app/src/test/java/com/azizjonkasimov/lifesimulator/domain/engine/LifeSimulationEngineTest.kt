package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSimulationEngineTest {
    private val engine = LifeSimulationEngine()

    @Test
    fun startNewLife_producesNewbornWithParents() {
        val state = engine.startNewLife("Test Person", Gender.MALE)
        assertEquals(0, state.age)
        assertTrue(state.alive)
        assertTrue(state.relationships.any { it.relation == RelationType.MOTHER })
        assertTrue(state.relationships.any { it.relation == RelationType.FATHER })
        assertTrue(state.log.isNotEmpty())
    }

    @Test
    fun ageUp_incrementsAgeAndIsDeterministic() {
        val start = engine.startNewLife("Test Person", Gender.FEMALE)
        val first = engine.ageUp(start)
        val second = engine.ageUp(start)
        assertTrue(first.success)
        assertEquals(start.age + 1, first.state.age)
        // Pure + seeded: aging the same state twice yields an identical result.
        assertEquals(first.state, second.state)
    }

    @Test
    fun aLife_eventuallyEnds() {
        var state = engine.startNewLife("Mortal One", Gender.MALE)
        var guard = 0
        while (state.alive && guard < 200) {
            state = if (state.pendingEventIds.isNotEmpty()) {
                engine.resolveEvent(state, state.pendingEventIds.first(), 0).state
            } else {
                engine.ageUp(state).state
            }
            guard++
        }
        assertFalse("A life should end well within 200 years", state.alive)
        assertNotNull(state.causeOfDeath)
    }

    @Test
    fun doActivity_meditateRaisesHappinessOncePerYear() {
        val newborn = engine.startNewLife("Test", Gender.MALE)
        val atEight = newborn.copy(
            character = newborn.character.copy(age = 8, stats = newborn.character.stats.copy(happiness = 50)),
        )
        val result = engine.doActivity(atEight, "meditate")
        assertTrue(result.success)
        assertEquals(55, result.state.character.stats.happiness)
        assertTrue("meditate" in result.state.activitiesUsed)

        // Second attempt in the same year is rejected.
        val again = engine.doActivity(result.state, "meditate")
        assertFalse(again.success)
    }
}
