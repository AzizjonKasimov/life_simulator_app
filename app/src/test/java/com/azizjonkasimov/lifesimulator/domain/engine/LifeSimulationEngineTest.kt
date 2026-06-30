package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyFocus
import com.azizjonkasimov.lifesimulator.domain.model.FinanceState
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition
import com.azizjonkasimov.lifesimulator.domain.model.RelationshipState
import com.azizjonkasimov.lifesimulator.domain.model.TimedOpportunityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSimulationEngineTest {
    private val engine = LifeSimulationEngine()

    @Test
    fun newLifeInitializesEachArchetypeWithV4Systems() {
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
            assertEquals(1, state.dayPlan.day)
            assertEquals(state.dayPlan.recommendedFocus, state.dayPlan.activeFocus)
            assertFalse(state.dayPlan.locked)
            assertTrue(state.timedOpportunities.size <= 2)
            assertTrue(state.history.isNotEmpty())
            assertNotNull(engine.dashboardSnapshot(state).focusGoal)
        }
    }

    @Test
    fun actionAppliesCostsCrossSystemEffectsAndDeltas() {
        val initial = engine.startNewLife(LifeArchetype.JUNIOR_WORKER)
        val result = engine.performAction(initial, "work_shift")

        assertTrue(result.success)
        assertEquals(initial.timeRemaining - 5, result.state.timeRemaining)
        assertEquals(initial.stats.energy - 28, result.state.stats.energy)
        assertEquals(initial.finances.cash + initial.career.salaryPerShift, result.state.finances.cash)
        assertTrue(result.state.skills.career > initial.skills.career)
        assertTrue(result.state.career.reputation > initial.career.reputation)
        assertTrue(result.state.career.promotionReadiness > initial.career.promotionReadiness)
        assertTrue(result.actionDeltas.any { it.label == "Cash" && it.amount > 0 })
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
            timedOpportunities = emptyList(),
        )
        val result = engine.advanceDay(initial)

        assertTrue(result.success)
        assertEquals(8, result.state.day)
        assertTrue(result.state.finances.debt > initial.finances.debt)
        assertTrue(result.state.relationships.friends < initial.relationships.friends)
        assertTrue(result.messages.any { it.contains("Weekly bill") })
    }

    @Test
    fun focusRecommendationPriorityChoosesExpectedFocuses() {
        val focusEngine = LifeSimulationEngine(events = emptyList())

        assertEquals(
            DailyFocus.MONEY,
            focusEngine.advanceDay(stableState().copy(finances = stableState().finances.copy(cash = 40))).state.dayPlan.recommendedFocus,
        )
        assertEquals(
            DailyFocus.RECOVERY,
            focusEngine.advanceDay(stableState().copy(stats = stableState().stats.copy(stress = 90))).state.dayPlan.recommendedFocus,
        )
        assertEquals(
            DailyFocus.CAREER,
            focusEngine.advanceDay(stableState().copy(career = stableState().career.copy(promotionReadiness = 85))).state.dayPlan.recommendedFocus,
        )
        assertEquals(
            DailyFocus.SOCIAL,
            focusEngine.advanceDay(stableState().copy(relationships = RelationshipState(family = 30, friends = 40, network = 45))).state.dayPlan.recommendedFocus,
        )
        assertEquals(
            DailyFocus.BALANCED,
            focusEngine.advanceDay(stableState()).state.dayPlan.recommendedFocus,
        )
    }

    @Test
    fun focusOverrideWorksBeforeFirstActionAndLocksAfterAction() {
        val initial = engine.startNewLife(LifeArchetype.STUDENT)
        val selected = engine.selectDailyFocus(initial, DailyFocus.RECOVERY)

        assertTrue(selected.success)
        assertEquals(DailyFocus.RECOVERY, selected.state.dayPlan.activeFocus)

        val afterAction = engine.performAction(selected.state, "rest").state
        val locked = engine.selectDailyFocus(afterAction, DailyFocus.CAREER)

        assertFalse(locked.success)
        assertEquals(DailyFocus.RECOVERY, locked.state.dayPlan.activeFocus)
    }

    @Test
    fun focusBonusesRewardsAndMissPenaltiesApply() {
        val moneyState = engine.selectDailyFocus(engine.startNewLife(LifeArchetype.JUNIOR_WORKER), DailyFocus.MONEY).state
        val afterBudget = engine.performAction(moneyState, "budget_review").state

        assertEquals(moneyState.finances.debt - 60, afterBudget.finances.debt)

        val afterMoneyDay = engine.advanceDay(afterBudget)
        assertTrue(afterMoneyDay.messages.any { it.contains("Money focus completed") })
        assertTrue(afterMoneyDay.state.finances.cash >= afterBudget.finances.cash + 25)

        val careerState = engine.selectDailyFocus(engine.startNewLife(LifeArchetype.FREELANCER), DailyFocus.CAREER).state
        val missed = engine.advanceDay(careerState)

        assertTrue(missed.messages.any { it.contains("Career focus was missed") })
    }

    @Test
    fun timedOpportunitiesProgressCompleteFailAndCooldown() {
        val billState = stableState().copy(
            finances = stableState().finances.copy(cash = 240, weeklyLivingCost = 140, nextBillDueDay = 7),
            timedOpportunities = listOf(TimedOpportunityState("bill_buffer", progress = 100, target = 240, baseline = 100, expiresOnDay = 2)),
        )
        val billResult = engine.advanceDay(billState)
        assertTrue(billResult.messages.any { it.contains("Opportunity completed: Bill Buffer") })
        assertFalse(billResult.state.timedOpportunities.any { it.id == "bill_buffer" })
        assertTrue(billResult.state.opportunityCooldowns.containsKey("bill_buffer"))

        val reconnectState = stableState().copy(
            relationships = RelationshipState(family = 35, friends = 40, network = 35),
            timedOpportunities = listOf(TimedOpportunityState("reconnect", progress = 0, target = 2, baseline = 40, expiresOnDay = 4)),
        )
        val firstSocial = engine.performAction(reconnectState, "call_family").state
        assertEquals(1, firstSocial.timedOpportunities.first { it.id == "reconnect" }.progress)
        val secondSocial = engine.performAction(firstSocial, "call_family")
        assertTrue(secondSocial.messages.any { it.contains("Opportunity completed: Reconnect") })

        val failedState = stableState().copy(
            finances = stableState().finances.copy(cash = 20),
            timedOpportunities = listOf(TimedOpportunityState("bill_buffer", progress = 20, target = 240, baseline = 20, expiresOnDay = 1)),
        )
        val failed = engine.advanceDay(failedState)
        assertTrue(failed.messages.any { it.contains("Opportunity expired: Bill Buffer") })
        assertTrue(failed.state.opportunityCooldowns.containsKey("bill_buffer"))
    }

    @Test
    fun remainingTimedOpportunityTypesCanComplete() {
        val recovery = stableState().copy(
            stats = stableState().stats.copy(stress = 50),
            timedOpportunities = listOf(TimedOpportunityState("recovery_window", progress = 0, target = 20, baseline = 70, expiresOnDay = 3)),
        )
        assertTrue(engine.advanceDay(recovery).messages.any { it.contains("Recovery Window") })

        val promotion = stableState().copy(
            career = stableState().career.copy(promotionReadiness = 100),
            timedOpportunities = listOf(TimedOpportunityState("promotion_push", progress = 90, target = 100, baseline = 90, expiresOnDay = 3)),
        )
        assertTrue(engine.advanceDay(promotion).messages.any { it.contains("Promotion Push") })

        val debt = stableState().copy(
            finances = stableState().finances.copy(debt = 100),
            timedOpportunities = listOf(TimedOpportunityState("debt_brake", progress = 0, target = 90, baseline = 200, expiresOnDay = 5)),
        )
        assertTrue(engine.advanceDay(debt).messages.any { it.contains("Debt Brake") })
    }

    @Test
    fun archetypeSpecificActionsAppearOnlyForTheirArchetype() {
        val studentActions = engine.actionAvailability(engine.startNewLife(LifeArchetype.STUDENT)).map { it.action.id }
        val workerActions = engine.actionAvailability(engine.startNewLife(LifeArchetype.JUNIOR_WORKER)).map { it.action.id }
        val freelancerActions = engine.actionAvailability(engine.startNewLife(LifeArchetype.FREELANCER)).map { it.action.id }

        assertTrue("exam_prep" in studentActions)
        assertFalse("manager_check_in" in studentActions)
        assertTrue("manager_check_in" in workerActions)
        assertFalse("pitch_client" in workerActions)
        assertTrue("pitch_client" in freelancerActions)
        assertFalse("exam_prep" in freelancerActions)
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
        val initial = stableState().copy(
            calendar = CalendarState(day = 3, timeRemaining = 0),
            rngSeed = 12345L,
            history = emptyList(),
            timedOpportunities = emptyList(),
            opportunityCooldowns = emptyMap(),
        )

        val first = deterministicEngine.advanceDay(initial)
        val second = deterministicEngine.advanceDay(initial)

        assertEquals(first.state, second.state)
        assertEquals(first.messages, second.messages)
    }

    private fun stableState(): GameState = engine.startNewLife(LifeArchetype.FREELANCER).copy(
        calendar = CalendarState(day = 1, timeRemaining = LifeSimulationEngine.DAILY_TIME_BUDGET),
        stats = CoreStats(health = 70, mood = 65, energy = 80, stress = 35, social = 60),
        finances = FinanceState(cash = 500, debt = 0, weeklyLivingCost = 180, nextBillDueDay = 7, creditScore = 670),
        career = CareerState("Independent freelancer", level = 1, xp = 20, reputation = 25, promotionReadiness = 30, salaryPerShift = 95),
        relationships = RelationshipState(family = 60, friends = 60, network = 60),
        timedOpportunities = emptyList(),
        opportunityCooldowns = emptyMap(),
    )
}
