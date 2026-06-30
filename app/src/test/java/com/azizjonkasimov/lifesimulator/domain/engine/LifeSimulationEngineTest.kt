package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSimulationEngineTest {
    private val engine = LifeSimulationEngine()

    @Test
    fun newLifeInitializesEachArchetype() {
        LifeArchetype.entries.forEach { archetype ->
            val state = engine.startNewLife(archetype)

            assertEquals(1, state.day)
            assertEquals(archetype, state.archetype)
            assertEquals(LifeSimulationEngine.DAILY_TIME_BUDGET, state.timeRemaining)
            assertTrue(state.money > 0)
            assertTrue(state.history.isNotEmpty())
        }
    }

    @Test
    fun actionAppliesCostsAndEffects() {
        val initial = engine.startNewLife(LifeArchetype.JUNIOR_WORKER)
        val result = engine.performAction(initial, "work_shift")

        assertTrue(result.success)
        assertEquals(initial.timeRemaining - 5, result.state.timeRemaining)
        assertEquals(initial.stats.energy - 28, result.state.stats.energy)
        assertEquals(initial.money + 95, result.state.money)
        assertTrue(result.state.skills.career > initial.skills.career)
    }

    @Test
    fun invalidActionFailsWithoutChangingState() {
        val initial = engine.startNewLife(LifeArchetype.STUDENT).copy(
            timeRemaining = 1,
            stats = engine.startNewLife(LifeArchetype.STUDENT).stats.copy(energy = 4),
        )
        val result = engine.performAction(initial, "work_shift")

        assertFalse(result.success)
        assertEquals(initial, result.state)
        assertEquals("Not enough time left today.", result.errorMessage)
    }

    @Test
    fun endDayAppliesWeeklyCosts() {
        val initial = engine.startNewLife(LifeArchetype.STUDENT).copy(
            day = 7,
            money = 300,
        )
        val result = engine.advanceDay(initial)

        assertTrue(result.success)
        assertEquals(8, result.state.day)
        assertTrue(result.state.money <= 160)
        assertTrue(result.messages.any { it.contains("Weekly living costs") })
    }

    @Test
    fun eventSelectionIsDeterministicFromSeed() {
        val deterministicEngine = LifeSimulationEngine(
            events = listOf(
                LifeEventDefinition(
                    id = "always",
                    title = "Always event",
                    description = "A deterministic test event.",
                    condition = { true },
                    effect = ActionEffect(moodDelta = 1),
                ),
            ),
        )
        val initial = GameState(
            day = 3,
            archetype = LifeArchetype.FREELANCER,
            stats = CoreStats(health = 60, mood = 60, energy = 70, stress = 50, social = 50),
            skills = SkillSet(knowledge = 20, fitness = 10, career = 25),
            money = 400,
            careerLevel = 1,
            jobTitle = "Independent freelancer",
            timeRemaining = 0,
            rngSeed = 12345L,
            history = emptyList(),
        )

        val first = deterministicEngine.advanceDay(initial)
        val second = deterministicEngine.advanceDay(initial)

        assertEquals(first.state, second.state)
        assertEquals(first.messages, second.messages)
    }
}
