package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.BusinessState
import com.azizjonkasimov.lifesimulator.domain.model.BusinessTier
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.EconomyState
import com.azizjonkasimov.lifesimulator.domain.model.FinanceState
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.Investment
import com.azizjonkasimov.lifesimulator.domain.model.InvestmentType
import com.azizjonkasimov.lifesimulator.domain.model.JobSearchState
import com.azizjonkasimov.lifesimulator.domain.model.PendingDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSimulationEngineTest {
    private val engine = LifeSimulationEngine(events = emptyList(), decisions = emptyList())

    @Test
    fun newLifeStartsBrokeUnemployedWithEconomyAndNoLegacyActions() {
        val state = engine.startNewLife()
        val actionIds = engine.actionAvailability(state).map { it.action.id }

        assertEquals(1, state.day)
        assertFalse(state.career.employed)
        assertEquals(180, state.finances.cash)
        assertEquals(350, state.finances.debt)
        assertEquals(0, state.jobSearch.searchProgress)
        assertEquals(BusinessTier.NONE, state.business.tier)
        assertEquals(EconomyState.EMPTY, state.economy)
        assertNull(state.pendingDecision)
        assertTrue("gig_work" in actionIds)
        assertTrue("apply_jobs" in actionIds)
        assertFalse("temp_shift" in actionIds)
        assertFalse("client_project" in actionIds)
    }

    @Test
    fun gigWorkEarnsCashAndUnemployedCannotWorkShift() {
        val initial = engine.startNewLife()

        val gig = engine.performAction(initial, "gig_work")
        assertTrue(gig.success)
        assertEquals(initial.finances.cash + 60, gig.state.finances.cash)
        assertTrue(gig.actionDeltas.any { it.label == "Cash" && it.amount == 60 })

        val blocked = engine.performAction(initial, "work_shift")
        assertFalse(blocked.success)
        assertEquals("You need a steady job first.", blocked.errorMessage)
        assertEquals(initial, blocked.state)
    }

    @Test
    fun interviewIsGatedUntilSearchProgressAndThenCanHire() {
        val early = engine.startNewLife()
        assertEquals("Build your job search to 40% first.", engine.performAction(early, "attend_interview").errorMessage)

        val ready = early.copy(jobSearch = JobSearchState(searchProgress = 100), rngSeed = 1L)
        val interview = engine.performAction(ready, "attend_interview")

        assertTrue(interview.success)
        assertTrue(interview.state.career.employed)
        assertEquals("Entry Associate", interview.state.career.title)
        assertEquals(105, interview.state.career.salaryPerShift)
        assertEquals(0, interview.state.jobSearch.searchProgress)
        // Same seed and state always resolve identically.
        assertEquals(interview.state, engine.performAction(ready, "attend_interview").state)
    }

    @Test
    fun depositAndWithdrawMoveCashAndSavings() {
        val initial = engine.startNewLife()

        val deposited = engine.deposit(initial, 100)
        assertTrue(deposited.success)
        assertEquals(initial.finances.cash - 100, deposited.state.finances.cash)
        assertEquals(100, deposited.state.economy.savings)

        val withdrawn = engine.withdraw(deposited.state, 50)
        assertEquals(deposited.state.finances.cash + 50, withdrawn.state.finances.cash)
        assertEquals(50, withdrawn.state.economy.savings)

        assertFalse(engine.withdraw(withdrawn.state, 9_999).success)
    }

    @Test
    fun buyingAnAssetDeductsCashAppliesEffectAndCannotRepeat() {
        val initial = engine.startNewLife()

        val bought = engine.buyAsset(initial, "gym")
        assertTrue(bought.success)
        assertEquals(initial.finances.cash - 60, bought.state.finances.cash)
        assertTrue("gym" in bought.state.economy.ownedAssets)
        assertEquals(initial.stats.mood + 2, bought.state.stats.mood)

        assertFalse(engine.buyAsset(bought.state, "gym").success)
    }

    @Test
    fun investingBuysAHoldingAndSellingReturnsItsValue() {
        val initial = engine.startNewLife()

        val invested = engine.invest(initial, InvestmentType.INDEX, 50)
        assertTrue(invested.success)
        assertEquals(initial.finances.cash - 50, invested.state.finances.cash)
        assertEquals(50, invested.state.economy.investments.first { it.type == InvestmentType.INDEX }.currentValue)

        val sold = engine.sellInvestment(invested.state, InvestmentType.INDEX)
        assertEquals(initial.finances.cash, sold.state.finances.cash)
        assertTrue(sold.state.economy.investments.isEmpty())

        assertFalse(engine.invest(initial, InvestmentType.INDEX, 10).success)
    }

    @Test
    fun weeklySettlementPaysBusinessGrowsSavingsAndChargesBill() {
        val billDay = engine.startNewLife().copy(
            calendar = CalendarState(day = 7, timeRemaining = 0, actionsToday = 0),
            finances = FinanceState(cash = 1000, debt = 0, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 700),
            economy = EconomyState(savings = 1000, investments = emptyList(), ownedAssets = emptyList()),
            business = BusinessState(tier = BusinessTier.SIDE_HUSTLE, clients = 2, reputation = 50),
        )

        val result = engine.advanceDay(billDay)

        // business net = 2 * 45 * (60+50)/100 = 99; savings interest = 10; bill = 190.
        assertEquals(8, result.state.day)
        assertEquals(1000 + 99 - 190, result.state.finances.cash)
        assertEquals(1010, result.state.economy.savings)
        assertEquals(14, result.state.finances.nextBillDueDay)
    }

    @Test
    fun investmentSwingsAreDeterministicFromSeed() {
        val withHolding = engine.startNewLife().copy(
            calendar = CalendarState(day = 7, timeRemaining = 0, actionsToday = 0),
            finances = FinanceState(cash = 2000, debt = 0, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 700),
            economy = EconomyState(savings = 0, investments = listOf(Investment(InvestmentType.STOCKS, principal = 1000, currentValue = 1000)), ownedAssets = emptyList()),
        )

        assertEquals(engine.advanceDay(withHolding).state, engine.advanceDay(withHolding).state)
    }

    @Test
    fun resolvingADecisionAppliesItsOutcomeAndClearsIt() {
        val withDecisions = LifeSimulationEngine(events = emptyList())
        val state = withDecisions.startNewLife().copy(
            finances = FinanceState(cash = 300, debt = 0, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 700),
            pendingDecision = PendingDecision("splurge"),
        )

        val resolved = withDecisions.resolveDecision(state, "buy")

        assertTrue(resolved.success)
        assertEquals(80, resolved.state.finances.cash)
        assertNull(resolved.state.pendingDecision)
        assertEquals((state.stats.mood + 14).coerceAtMost(100), resolved.state.stats.mood)
    }

    @Test
    fun netWorthCountsCashSavingsInvestmentsAssetsMinusDebt() {
        val state = engine.startNewLife().copy(
            finances = FinanceState(cash = 100, debt = 50, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 700),
            economy = EconomyState(
                savings = 200,
                investments = listOf(Investment(InvestmentType.INDEX, principal = 50, currentValue = 50)),
                ownedAssets = listOf("gym"),
            ),
        )

        // 100 cash + 200 savings + 50 invested + 30 gym resale - 50 debt
        assertEquals(330, engine.netWorth(state))
    }

    @Test
    fun advanceDayIsDeterministicFromSeed() {
        val state = engine.startNewLife().copy(
            calendar = CalendarState(day = 3, timeRemaining = 0, actionsToday = 0),
            rngSeed = 12_345L,
        )
        val first = engine.advanceDay(state)
        val second = engine.advanceDay(state)
        assertEquals(first.state, second.state)
        assertEquals(first.messages, second.messages)
    }
}
