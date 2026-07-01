package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.BusinessState
import com.azizjonkasimov.lifesimulator.domain.model.BusinessTier
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
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

    @Test
    fun savingsInterestTracksLifetimeTotal() {
        val billDay = engine.startNewLife().copy(
            calendar = CalendarState(day = 7, timeRemaining = 0, actionsToday = 0),
            finances = FinanceState(cash = 1000, debt = 0, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 700),
            economy = EconomyState(savings = 1000, investments = emptyList(), ownedAssets = emptyList()),
        )

        val result = engine.advanceDay(billDay)

        assertEquals(1010, result.state.economy.savings)
        assertEquals(10, result.state.economy.lifetimeInterest)
    }

    @Test
    fun autoAllocationSweepsLeftoverCashOnPayday() {
        val billDay = engine.startNewLife().copy(
            calendar = CalendarState(day = 7, timeRemaining = 0, actionsToday = 0),
            finances = FinanceState(cash = 1000, debt = 0, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 700),
            economy = EconomyState(
                savings = 0,
                investments = emptyList(),
                ownedAssets = emptyList(),
                autoSavePercent = 20,
                autoInvestPercent = 10,
                autoInvestType = InvestmentType.INDEX,
            ),
        )

        val result = engine.advanceDay(billDay)

        // Post-bill cash = 1000 - 190 = 810. Sweep 20% to savings, 10% into Index.
        assertEquals(162, result.state.economy.savings)
        assertEquals(81, result.state.economy.investments.first { it.type == InvestmentType.INDEX }.currentValue)
        assertEquals(810 - 162 - 81, result.state.finances.cash)
    }

    @Test
    fun setAutoSaveAndInvestRespectCombinedCap() {
        val initial = engine.startNewLife()

        val saved = engine.setAutoSave(initial, 60)
        assertEquals(60, saved.state.economy.autoSavePercent)

        // 70% would push the combined sweep past 100%, so it is capped to the remaining 40%.
        val invested = engine.setAutoInvest(saved.state, 70, InvestmentType.STOCKS)
        assertEquals(60, invested.state.economy.autoSavePercent)
        assertEquals(40, invested.state.economy.autoInvestPercent)
        assertEquals(InvestmentType.STOCKS, invested.state.economy.autoInvestType)
    }

    @Test
    fun gigWorkPayDiminishesWithRepeatedUse() {
        val initial = engine.startNewLife()

        val first = engine.performAction(initial, "gig_work")
        assertEquals(initial.finances.cash + 60, first.state.finances.cash)
        assertEquals(1, first.state.finances.gigsThisWeek)

        // A second gig the same week pays 60 - 12 = 48: gigs are a survival tool, not a wealth engine.
        val second = engine.performAction(first.state, "gig_work")
        assertEquals(first.state.finances.cash + 48, second.state.finances.cash)
        assertEquals(2, second.state.finances.gigsThisWeek)
    }

    @Test
    fun costOfLivingRisesRoughlyMonthly() {
        val monthEnd = engine.startNewLife().copy(
            calendar = CalendarState(day = 28, timeRemaining = 0, actionsToday = 0),
            finances = FinanceState(cash = 5_000, debt = 0, weeklyLivingCost = 190, nextBillDueDay = 28, creditScore = 700),
        )

        val result = engine.advanceDay(monthEnd)

        // Settlement index 28/7 = 4 → monthly rise of max(5, 190*8/100 = 15) = 15.
        assertEquals(205, result.state.finances.weeklyLivingCost)
    }

    @Test
    fun businessReputationDecaysAndClientsCanChurnOnSettlement() {
        val billDay = engine.startNewLife().copy(
            calendar = CalendarState(day = 7, timeRemaining = 0, actionsToday = 0),
            finances = FinanceState(cash = 5_000, debt = 0, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 700),
            business = BusinessState(tier = BusinessTier.STUDIO, clients = 6, reputation = 40),
            rngSeed = 123L,
        )

        val result = engine.advanceDay(billDay)

        // Reputation always slips by 4; clients can only hold or shrink on settlement, never grow.
        assertEquals(36, result.state.business.reputation)
        assertTrue(result.state.business.clients in 0..6)
        // Still fully deterministic from the seed.
        assertEquals(result.state, engine.advanceDay(billDay).state)
    }

    @Test
    fun propertyAssetsPayWeeklyIncome() {
        val billDay = engine.startNewLife().copy(
            calendar = CalendarState(day = 7, timeRemaining = 0, actionsToday = 0),
            finances = FinanceState(cash = 1_000, debt = 0, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 700),
            economy = EconomyState(savings = 0, investments = emptyList(), ownedAssets = listOf("rental_room")),
        )

        val result = engine.advanceDay(billDay)

        // Income +55; bill = 190 base + 10 upkeep = 200. Cash = 1000 + 55 - 200 = 855.
        assertEquals(855, result.state.finances.cash)
    }

    @Test
    fun financialIndependenceGoalFiresWhenPassiveCoversBill() {
        val livingOffSavings = engine.startNewLife().copy(
            calendar = CalendarState(day = 7, timeRemaining = 0, actionsToday = 0),
            finances = FinanceState(cash = 500, debt = 0, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 700),
            economy = EconomyState(savings = 100_000, investments = emptyList(), ownedAssets = emptyList()),
        )

        val result = engine.advanceDay(livingOffSavings)

        // 1% of $100k = $1,000/week passive, far above the $190 bill.
        assertTrue("financial_independence" in result.state.completedGoals)
        assertTrue(result.messages.any { it.contains("Financial Independence") })
    }

    @Test
    fun degreeRaisesShiftPay() {
        val employed = engine.startNewLife().copy(
            career = CareerState(title = "Associate", level = 2, reputation = 30, promotionReadiness = 20, salaryPerShift = 200, employed = true),
        )

        val withoutDegree = engine.performAction(employed, "work_shift")
        assertEquals(employed.finances.cash + 200, withoutDegree.state.finances.cash)

        val graduate = employed.copy(economy = employed.economy.copy(ownedAssets = listOf("degree")))
        val withDegree = engine.performAction(graduate, "work_shift")
        // A degree lifts shift pay by 25%: 200 → 250.
        assertEquals(graduate.finances.cash + 250, withDegree.state.finances.cash)
    }
}
