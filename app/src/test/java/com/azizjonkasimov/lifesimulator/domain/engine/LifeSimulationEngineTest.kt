package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.FinanceState
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition
import com.azizjonkasimov.lifesimulator.domain.model.LifeProfile
import com.azizjonkasimov.lifesimulator.domain.model.RelationshipState
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSimulationEngineTest {
    private val engine = LifeSimulationEngine()

    @Test
    fun newLifeInitializesEachArchetypeWithV2Systems() {
        LifeArchetype.entries.forEach { archetype ->
            val state = engine.startNewLife(archetype)

            assertEquals(1, state.day)
            assertEquals(archetype, state.archetype)
            assertEquals(LifeSimulationEngine.DAILY_TIME_BUDGET, state.timeRemaining)
            assertTrue(state.finances.cash > 0)
            assertTrue(state.finances.weeklyLivingCost > 0)
            assertTrue(state.career.salaryPerShift > 0)
            assertTrue(state.relationships.average > 0)
            assertEquals(4, state.goals.size)
            assertTrue(state.history.isNotEmpty())
            assertNotNull(engine.dashboardSnapshot(state).focusGoal)
        }
    }

    @Test
    fun actionAppliesCostsAndCrossSystemEffects() {
        val initial = engine.startNewLife(LifeArchetype.JUNIOR_WORKER)
        val result = engine.performAction(initial, "work_shift")

        assertTrue(result.success)
        assertEquals(initial.timeRemaining - 5, result.state.timeRemaining)
        assertEquals(initial.stats.energy - 28, result.state.stats.energy)
        assertEquals(initial.finances.cash + initial.career.salaryPerShift, result.state.finances.cash)
        assertTrue(result.state.skills.career > initial.skills.career)
        assertTrue(result.state.career.reputation > initial.career.reputation)
        assertTrue(result.state.career.promotionReadiness > initial.career.promotionReadiness)
    }

    @Test
    fun invalidActionFailsWithoutChangingState() {
        val initial = engine.startNewLife(LifeArchetype.STUDENT).copy(
            calendar = CalendarState(day = 1, timeRemaining = 1),
            stats = engine.startNewLife(LifeArchetype.STUDENT).stats.copy(energy = 4),
        )
        val result = engine.performAction(initial, "work_shift")

        assertFalse(result.success)
        assertEquals(initial, result.state)
        assertEquals("Not enough time left today.", result.errorMessage)
    }

    @Test
    fun endDayAppliesBillsDebtPressureRecoveryAndRelationshipDecay() {
        val initial = engine.startNewLife(LifeArchetype.STUDENT).copy(
            calendar = CalendarState(day = 7, timeRemaining = 0),
            finances = engine.startNewLife(LifeArchetype.STUDENT).finances.copy(
                cash = 40,
                debt = 100,
                nextBillDueDay = 7,
            ),
        )
        val result = engine.advanceDay(initial)

        assertTrue(result.success)
        assertEquals(8, result.state.day)
        assertTrue(result.state.finances.debt > initial.finances.debt)
        assertTrue(result.state.relationships.friends < initial.relationships.friends)
        assertTrue(result.messages.any { it.contains("Weekly bill") })
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
            profile = LifeProfile("Test", 22, LifeArchetype.FREELANCER),
            calendar = CalendarState(day = 3, timeRemaining = 0),
            stats = CoreStats(health = 60, mood = 60, energy = 70, stress = 50, social = 50),
            skills = SkillSet(knowledge = 20, fitness = 10, career = 25, communication = 15, creativity = 18),
            finances = FinanceState(cash = 400, debt = 0, weeklyLivingCost = 180, nextBillDueDay = 7, creditScore = 660),
            career = CareerState("Independent freelancer", level = 1, xp = 25, reputation = 25, promotionReadiness = 25, salaryPerShift = 95),
            relationships = RelationshipState(family = 50, friends = 50, network = 50),
            goals = engine.startNewLife(LifeArchetype.FREELANCER).goals,
            modifiers = emptyList(),
            rngSeed = 12345L,
            history = emptyList(),
        )

        val first = deterministicEngine.advanceDay(initial)
        val second = deterministicEngine.advanceDay(initial)

        assertEquals(first.state, second.state)
        assertEquals(first.messages, second.messages)
    }
}
