package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.BusinessStage
import com.azizjonkasimov.lifesimulator.domain.model.BusinessState
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyFocus
import com.azizjonkasimov.lifesimulator.domain.model.FinanceState
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.JobSearchState
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition
import com.azizjonkasimov.lifesimulator.domain.model.RelationshipState
import com.azizjonkasimov.lifesimulator.domain.model.TimedOpportunityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSimulationEngineTest {
    private val engine = LifeSimulationEngine(events = emptyList())

    @Test
    fun newLifeStartsUnemployedWithMoneySystems() {
        val state = engine.startNewLife()
        val actionIds = engine.actionAvailability(state).map { it.action.id }

        assertEquals(1, state.day)
        assertEquals("Alex Rivers", state.profile.name)
        assertEquals(22, state.profile.age)
        assertFalse(state.career.employed)
        assertEquals("Unemployed", state.career.title)
        assertEquals(0, state.career.salaryPerShift)
        assertEquals(180, state.finances.cash)
        assertEquals(350, state.finances.debt)
        assertEquals(190, state.finances.weeklyLivingCost)
        assertEquals(635, state.finances.creditScore)
        assertEquals(JobSearchState(0, 10, 0), state.jobSearch)
        assertEquals(BusinessStage.IDEA, state.business.stage)
        assertEquals(state.dayPlan.recommendedFocus, state.dayPlan.activeFocus)
        assertFalse(state.dayPlan.locked)
        assertTrue(state.timedOpportunities.size <= 2)
        assertFalse("exam_prep" in actionIds)
        assertFalse("freelance_gig" in actionIds)
        assertFalse("apply_jobs" in actionIds)
    }

    @Test
    fun unemployedCannotWorkShiftButTempShiftEarnsCash() {
        val initial = engine.startNewLife()

        val blocked = engine.performAction(initial, "work_shift")
        assertFalse(blocked.success)
        assertEquals(initial, blocked.state)
        assertEquals("You need a steady job first.", blocked.errorMessage)

        val temp = engine.performAction(initial, "temp_shift")
        assertTrue(temp.success)
        assertEquals(initial.finances.cash + 80, temp.state.finances.cash)
        assertTrue(temp.actionDeltas.any { it.label == "Cash" && it.amount == 80 })
    }

    @Test
    fun applicationsPrepAndInterviewUnlockFirstJobDeterministically() {
        var state = engine.startNewLife()

        listOf(
            "send_applications",
            "send_applications",
            "interview_prep",
            "interview_prep",
            "send_applications",
        ).forEach { actionId ->
            state = engine.performAction(refillDay(state), actionId).state
        }

        assertTrue(state.jobSearch.offerProgress >= 55)
        assertTrue(state.jobSearch.interviewReadiness >= 35)
        assertTrue(state.jobSearch.applicationsSent > 0)

        val interview = engine.performAction(refillDay(state), "attend_interview")

        assertTrue(interview.success)
        assertTrue(interview.messages.any { it.contains("landed an entry job") })
        assertTrue(interview.state.career.employed)
        assertEquals("Entry Associate", interview.state.career.title)
        assertEquals(1, interview.state.career.level)
        assertEquals(105, interview.state.career.salaryPerShift)
        assertEquals(20, interview.state.career.promotionReadiness)
        assertEquals(100, interview.state.jobSearch.offerProgress)
    }

    @Test
    fun workShiftPaysSalaryAndPromotionRaisesSalaryAfterEmployment() {
        val base = engine.selectDailyFocus(stableState(), DailyFocus.BALANCED).state
        val employed = base.copy(
            career = CareerState(
                title = "Entry Associate",
                level = 1,
                xp = 20,
                reputation = 20,
            promotionReadiness = 92,
            salaryPerShift = 105,
            employed = true,
            )
        )

        val result = engine.performAction(employed, "work_shift")

        assertTrue(result.success)
        assertEquals(employed.finances.cash + 105, result.state.finances.cash)
        assertEquals(2, result.state.career.level)
        assertEquals("Associate", result.state.career.title)
        assertEquals(145, result.state.career.salaryPerShift)
        assertEquals(25, result.state.career.promotionReadiness)
    }

    @Test
    fun businessLeadsProjectsPayoutsAndStageUpgradeWork() {
        var state = engine.selectDailyFocus(stableState(), DailyFocus.BALANCED).state

        state = engine.performAction(refillDay(state), "find_leads").state
        assertEquals(2, state.business.leads)

        state = engine.performAction(refillDay(state), "pitch_client").state
        assertEquals(1, state.business.leads)
        assertEquals(1, state.business.activeProjects)

        val beforeProjectCash = state.finances.cash
        state = engine.performAction(refillDay(state), "client_project").state
        assertEquals(0, state.business.activeProjects)
        assertEquals(1, state.business.completedProjects)
        assertTrue(state.finances.cash > beforeProjectCash)

        val readyForUpgrade = refillDay(
            state.copy(
                business = state.business.copy(activeProjects = 1, completedProjects = 1),
            ),
        )
        val upgraded = engine.performAction(readyForUpgrade, "client_project").state

        assertEquals(2, upgraded.business.completedProjects)
        assertEquals(BusinessStage.SIDE_HUSTLE, upgraded.business.stage)
    }

    @Test
    fun weeklyBusinessOverheadStartsAtReliablePipeline() {
        val initial = stableState().copy(
            calendar = CalendarState(day = 7, timeRemaining = 0),
            finances = FinanceState(cash = 200, debt = 100, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 650),
            business = stableState().business.copy(stage = BusinessStage.RELIABLE_PIPELINE),
            timedOpportunities = emptyList(),
        )

        val result = engine.advanceDay(initial)

        assertTrue(result.success)
        assertEquals(8, result.state.day)
        assertEquals(135, result.state.finances.debt)
        assertTrue(result.messages.any { it.contains("business overhead") })
    }

    @Test
    fun focusRecommendationPriorityChoosesExpectedFocuses() {
        assertEquals(
            DailyFocus.MONEY,
            engine.advanceDay(stableState().copy(finances = stableState().finances.copy(cash = 40))).state.dayPlan.recommendedFocus,
        )
        assertEquals(
            DailyFocus.RECOVERY,
            engine.advanceDay(stableState().copy(stats = stableState().stats.copy(stress = 90))).state.dayPlan.recommendedFocus,
        )
        assertEquals(
            DailyFocus.CAREER,
            engine.advanceDay(stableState().copy(career = stableState().career.copy(promotionReadiness = 85))).state.dayPlan.recommendedFocus,
        )
        assertEquals(
            DailyFocus.SOCIAL,
            engine.advanceDay(stableState().copy(relationships = RelationshipState(family = 30, friends = 40, network = 45))).state.dayPlan.recommendedFocus,
        )
        assertEquals(
            DailyFocus.BALANCED,
            engine.advanceDay(stableState()).state.dayPlan.recommendedFocus,
        )
    }

    @Test
    fun quickActionsPrioritizeJobSearchAndActiveProjects() {
        val jobSearchState = stableState().copy(
            career = stableState().career.copy(employed = false, title = "Unemployed", level = 0, salaryPerShift = 0),
            jobSearch = JobSearchState(applicationsSent = 2, interviewReadiness = 40, offerProgress = 60),
            timedOpportunities = emptyList(),
        )
        assertTrue("attend_interview" in engine.dashboardSnapshot(jobSearchState).quickActionIds)

        val projectState = stableState().copy(
            business = stableState().business.copy(activeProjects = 1),
            timedOpportunities = emptyList(),
        )
        assertEquals("client_project", engine.dashboardSnapshot(projectState).quickActionIds.first())
    }

    @Test
    fun focusOverrideWorksBeforeFirstActionAndLocksAfterAction() {
        val initial = engine.startNewLife()
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
        val moneyState = engine.selectDailyFocus(engine.startNewLife(), DailyFocus.MONEY).state
        val afterBudget = engine.performAction(moneyState, "budget_review").state

        assertEquals(moneyState.finances.debt - 60, afterBudget.finances.debt)

        val afterMoneyDay = engine.advanceDay(afterBudget)
        assertTrue(afterMoneyDay.messages.any { it.contains("Money focus completed") })
        assertTrue(afterMoneyDay.state.finances.cash >= afterBudget.finances.cash + 25)

        val careerState = engine.selectDailyFocus(stableState(), DailyFocus.CAREER).state
        val missed = engine.advanceDay(careerState)

        assertTrue(missed.messages.any { it.contains("Career focus missed") })
    }

    @Test
    fun timedOpportunitiesProgressCompleteFailAndCooldown() {
        val billState = stableState().copy(
            finances = stableState().finances.copy(cash = 290, weeklyLivingCost = 190, nextBillDueDay = 7),
            timedOpportunities = listOf(TimedOpportunityState("bill_buffer", progress = 190, target = 290, baseline = 100, expiresOnDay = 2)),
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
            timedOpportunities = listOf(TimedOpportunityState("bill_buffer", progress = 20, target = 290, baseline = 20, expiresOnDay = 1)),
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

    private fun stableState(): GameState = engine.startNewLife().copy(
        calendar = CalendarState(day = 1, timeRemaining = LifeSimulationEngine.DAILY_TIME_BUDGET),
        stats = CoreStats(health = 70, mood = 65, energy = 80, stress = 35, social = 60),
        finances = FinanceState(cash = 600, debt = 0, weeklyLivingCost = 180, nextBillDueDay = 7, creditScore = 670),
        career = CareerState(
            title = "Entry Associate",
            level = 1,
            xp = 20,
            reputation = 25,
            promotionReadiness = 30,
            salaryPerShift = 105,
            employed = true,
        ),
        jobSearch = JobSearchState(applicationsSent = 4, interviewReadiness = 60, offerProgress = 100),
        business = BusinessState(
            stage = BusinessStage.IDEA,
            leads = 0,
            activeProjects = 0,
            completedProjects = 0,
            clientTrust = 20,
            reputation = 10,
            pipelineValue = 40,
        ),
        relationships = RelationshipState(family = 60, friends = 60, network = 60),
        timedOpportunities = emptyList(),
        opportunityCooldowns = emptyMap(),
    )

    private fun refillDay(state: GameState): GameState = state.copy(
        calendar = state.calendar.copy(timeRemaining = LifeSimulationEngine.DAILY_TIME_BUDGET),
        stats = state.stats.copy(energy = 100),
    )
}
