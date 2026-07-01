package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.AssetTag
import com.azizjonkasimov.lifesimulator.domain.model.BusinessState
import com.azizjonkasimov.lifesimulator.domain.model.BusinessTier
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
import com.azizjonkasimov.lifesimulator.domain.model.EconomyState
import com.azizjonkasimov.lifesimulator.domain.model.EventOutcome
import com.azizjonkasimov.lifesimulator.domain.model.FinanceState
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.GoalMetrics
import com.azizjonkasimov.lifesimulator.domain.model.GoalStatus
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind
import com.azizjonkasimov.lifesimulator.domain.model.Investment
import com.azizjonkasimov.lifesimulator.domain.model.InvestmentType
import com.azizjonkasimov.lifesimulator.domain.model.JobSearchState
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition
import com.azizjonkasimov.lifesimulator.domain.model.LifeModifier
import com.azizjonkasimov.lifesimulator.domain.model.LifeProfile
import com.azizjonkasimov.lifesimulator.domain.model.PassiveIncomeBreakdown
import com.azizjonkasimov.lifesimulator.domain.model.PendingDecision
import com.azizjonkasimov.lifesimulator.domain.model.RelationshipState
import com.azizjonkasimov.lifesimulator.domain.model.SimulationResult
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet

/**
 * The whole game in one place. The day is the unit of play: spend time and energy
 * on actions, then end the day to settle bills, investments, business income, and
 * roll for events and decisions. Money is the throughline — earn it with work and
 * gigs, grow it with savings and investments, and spend it on assets that change
 * how everything else plays.
 *
 * All randomness flows from a single seed so a given save replays identically.
 */
class LifeSimulationEngine(
    private val actions: List<DailyActionDefinition> = ActionCatalog.actions,
    private val events: List<LifeEventDefinition> = EventCatalog.events,
    private val decisions: List<LifeEventDefinition> = DecisionEventCatalog.events,
) {

    // -----------------------------------------------------------------------
    // New game
    // -----------------------------------------------------------------------

    fun startNewLife(): GameState = GameState(
        profile = LifeProfile(name = "Alex Rivers", age = 22),
        calendar = CalendarState(day = 1, timeRemaining = DAILY_TIME_BUDGET, actionsToday = 0),
        stats = CoreStats(health = 66, mood = 54, energy = 76, stress = 52, social = 45),
        skills = SkillSet(knowledge = 14, fitness = 8, career = 6, communication = 12, creativity = 12),
        finances = FinanceState(cash = 180, debt = 350, weeklyLivingCost = 190, nextBillDueDay = 7, creditScore = 635),
        economy = EconomyState.EMPTY,
        career = CareerState(title = "Unemployed", level = 0, reputation = 8, promotionReadiness = 0, salaryPerShift = 0, employed = false),
        jobSearch = JobSearchState(searchProgress = 0),
        business = BusinessState(tier = BusinessTier.NONE, clients = 0, reputation = 0),
        relationships = RelationshipState(family = 50, friends = 42, network = 32),
        modifiers = emptyList(),
        pendingDecision = null,
        rngSeed = STARTING_SEED,
        history = listOf(
            HistoryEntry(
                day = 1,
                title = "New life started",
                detail = "Unemployed, in debt, $180 to your name. Earn, invest, and build a life worth more than its bills.",
                kind = HistoryKind.SYSTEM,
            ),
        ),
    )

    // -----------------------------------------------------------------------
    // Read models for the UI
    // -----------------------------------------------------------------------

    fun netWorth(state: GameState): Int =
        state.finances.cash + state.economy.savings + state.economy.investedValue + assetResaleValue(state) - state.finances.debt

    fun weeklyLivingTotal(state: GameState): Int =
        (state.finances.weeklyLivingCost + assetWeeklyCost(state)).coerceAtLeast(0)

    /**
     * Money that lands each week without spending a day on it: savings interest,
     * business net, property income, and expected market drift. When [PassiveIncomeBreakdown.total]
     * reaches the weekly bill, the player is financially independent.
     */
    fun passiveIncome(state: GameState): PassiveIncomeBreakdown = PassiveIncomeBreakdown(
        savingsInterest = state.economy.savings * SAVINGS_WEEKLY_INTEREST_PERCENT / 100,
        business = businessWeeklyNet(state).coerceAtLeast(0),
        properties = assetWeeklyIncome(state),
        market = state.economy.investments.sumOf { it.currentValue * it.type.driftPercent / 100 },
    )

    fun goalMetrics(state: GameState): GoalMetrics = GoalMetrics(
        netWorth = netWorth(state),
        savings = state.economy.savings,
        investedValue = state.economy.investedValue,
        debt = state.finances.debt,
        passiveIncome = passiveIncome(state).total,
        weeklyCost = weeklyLivingTotal(state),
        businessTier = state.business.tier,
        ownsHome = hasTag(state, AssetTag.HOME),
        ownsIncomeAsset = ownedDefinitions(state).any { it.weeklyIncome > 0 },
        employed = state.career.employed,
        careerLevel = state.career.level,
    )

    /** Every goal with whether it's reached (persisted or live) and its progress, for the UI. */
    fun goalStatuses(state: GameState): List<GoalStatus> {
        val metrics = goalMetrics(state)
        return GoalCatalog.goals.map { goal ->
            GoalStatus(
                goal = goal,
                complete = goal.id in state.completedGoals || goal.isComplete(metrics),
                progress = goal.progress(metrics).coerceIn(0f, 1f),
            )
        }
    }

    fun businessWeeklyRevenue(state: GameState): Int {
        if (!state.business.started) return 0
        val rate = state.business.tier.revenuePerClient
        val repMultiplier = 60 + state.business.reputation
        return state.business.clients * rate * repMultiplier / 100
    }

    fun businessWeeklyNet(state: GameState): Int =
        businessWeeklyRevenue(state) - state.business.tier.weeklyOverhead

    fun interviewOdds(state: GameState): Int =
        (18 + state.jobSearch.searchProgress * 60 / 100 + state.skills.communication / 3 +
            if (hasTag(state, AssetTag.DEGREE)) 10 else 0).coerceIn(5, 95)

    fun clientOdds(state: GameState): Int =
        (42 + state.business.reputation / 2 + if (hasTag(state, AssetTag.BUSINESS_BOOST)) 12 else 0).coerceIn(10, 92)

    fun pendingDecisionEvent(state: GameState): LifeEventDefinition? =
        state.pendingDecision?.let { pending -> decisions.firstOrNull { it.id == pending.eventId } }

    fun dashboardSnapshot(state: GameState): DashboardSnapshot {
        val weeklyCost = weeklyLivingTotal(state)
        val daysUntilBill = (state.finances.nextBillDueDay - state.day).coerceAtLeast(0)
        val alerts = buildList {
            if (state.finances.cash < weeklyCost) add("Cash is below the next weekly bill.")
            if (state.stats.stress >= 78) add("Stress is near burnout range.")
            if (state.finances.debt >= 500) add("Debt is weighing on you.")
            if (state.career.employed && state.career.promotionReadiness >= 80) add("A promotion is within reach.")
            if (state.business.started && state.business.clients == 0) add("Your business has no clients yet.")
        }.take(3)
        return DashboardSnapshot(
            status = statusFor(state),
            netWorth = netWorth(state),
            weeklyCost = weeklyCost,
            businessWeeklyNet = businessWeeklyNet(state),
            nextBillLabel = if (daysUntilBill == 0) "Due today: ${money(weeklyCost)}" else "In $daysUntilBill days: ${money(weeklyCost)}",
            pressureSummary = pressureSummaryFor(state),
            suggestionIds = suggestionIdsFor(state),
            alerts = alerts,
        )
    }

    fun actionAvailability(state: GameState): List<ActionAvailability> {
        val suggestions = suggestionIdsFor(state)
        return actions.map { action ->
            val reason = unavailableReason(state, action)
            val effect = dynamicEffectFor(state, action)
            ActionAvailability(
                action = action,
                isAvailable = reason == null,
                reason = reason,
                previewDeltas = buildActionDeltas(state, action, effect),
                focusMatch = false,
                recommendationReason = if (action.id in suggestions) "Suggested" else null,
                oddsPercent = when (action.id) {
                    "attend_interview" -> interviewOdds(state)
                    "find_client" -> clientOdds(state)
                    else -> null
                },
            )
        }
    }

    // -----------------------------------------------------------------------
    // Daily actions
    // -----------------------------------------------------------------------

    fun performAction(state: GameState, actionId: String): SimulationResult {
        val action = actions.firstOrNull { it.id == actionId }
            ?: return failure(state, "Unknown action.")
        unavailableReason(state, action)?.let { return failure(state, it) }

        val effect = dynamicEffectFor(state, action)
        val actionDeltas = buildActionDeltas(state, action, effect)

        val charged = state.copy(
            calendar = state.calendar.copy(
                timeRemaining = (state.timeRemaining - action.timeCost).coerceAtLeast(0),
                actionsToday = state.calendar.actionsToday + 1,
            ),
            finances = state.finances.copy(cash = state.finances.cash - action.moneyCost).normalized(),
            stats = state.stats.copy(energy = state.stats.energy - action.energyCost).clamped(),
        )
        var working = applyEffect(charged, effect)

        val messages = mutableListOf("${action.title} completed.")
        val entries = mutableListOf(
            HistoryEntry(day = working.day, title = action.title, detail = summarizeAction(action, effect), kind = HistoryKind.ACTION),
        )

        when (action.id) {
            "gig_work" -> working = working.copy(finances = working.finances.copy(gigsThisWeek = working.finances.gigsThisWeek + 1).normalized())
            "attend_interview" -> resolveInterview(working).let { working = it.state; messages += it.message; entries += it.entry }
            "find_client" -> resolveFindClient(working).let { working = it.state; messages += it.message; entries += it.entry }
            "launch_business" -> {
                working = working.copy(business = working.business.copy(tier = BusinessTier.SIDE_HUSTLE).normalized())
                messages += "Your side hustle is live."
                entries += HistoryEntry(working.day, "Side hustle launched", "You turned an idea into a service you can sell.", HistoryKind.CAREER)
            }
            "upgrade_business" -> {
                val next = nextTier(working.business.tier)
                if (next != null) {
                    val cost = working.business.tier.upgradeCost
                    working = working.copy(
                        finances = working.finances.copy(cash = working.finances.cash - cost).normalized(),
                        business = working.business.copy(tier = next).normalized(),
                    )
                    messages += "Business upgraded to ${next.label}."
                    entries += HistoryEntry(working.day, "Business upgraded", "Reinvested ${money(cost)} to reach ${next.label}.", HistoryKind.CAREER)
                }
            }
            "rest" -> working = working.copy(modifiers = working.modifiers.filterNot { it.id == BURNOUT_RISK_ID })
        }

        working = settlePromotion(working) { entries += it }

        val updated = working.copy(history = (working.history + entries).trimHistory())
        return SimulationResult(state = updated, success = true, messages = messages, actionDeltas = actionDeltas)
    }

    // -----------------------------------------------------------------------
    // Money tab — savings, debt, investments, assets (all free of time/energy)
    // -----------------------------------------------------------------------

    fun deposit(state: GameState, amount: Int): SimulationResult {
        if (amount <= 0) return failure(state, "Enter an amount to deposit.")
        if (state.finances.cash < amount) return failure(state, "Not enough cash.")
        val updated = state.copy(
            finances = state.finances.copy(cash = state.finances.cash - amount).normalized(),
            economy = state.economy.copy(savings = state.economy.savings + amount).normalized(),
            history = (state.history + HistoryEntry(state.day, "Moved to savings", "Set aside ${money(amount)}.", HistoryKind.FINANCE)).trimHistory(),
        )
        return SimulationResult(updated, true, listOf("Saved ${money(amount)}."))
    }

    fun withdraw(state: GameState, amount: Int): SimulationResult {
        if (amount <= 0) return failure(state, "Enter an amount to withdraw.")
        if (state.economy.savings < amount) return failure(state, "Not that much in savings.")
        val updated = state.copy(
            finances = state.finances.copy(cash = state.finances.cash + amount).normalized(),
            economy = state.economy.copy(savings = state.economy.savings - amount).normalized(),
            history = (state.history + HistoryEntry(state.day, "Withdrew savings", "Pulled ${money(amount)} into cash.", HistoryKind.FINANCE)).trimHistory(),
        )
        return SimulationResult(updated, true, listOf("Withdrew ${money(amount)}."))
    }

    fun payDebt(state: GameState, amount: Int): SimulationResult {
        if (amount <= 0) return failure(state, "Enter an amount to pay.")
        if (state.finances.cash < amount) return failure(state, "Not enough cash.")
        val applied = amount.coerceAtMost(state.finances.debt)
        if (applied <= 0) return failure(state, "You have no debt to pay.")
        val updated = state.copy(
            finances = state.finances.copy(
                cash = state.finances.cash - applied,
                debt = state.finances.debt - applied,
                creditScore = state.finances.creditScore + (applied / 100).coerceIn(1, 5),
            ).normalized(),
            history = (state.history + HistoryEntry(state.day, "Paid down debt", "Knocked ${money(applied)} off what you owe.", HistoryKind.FINANCE)).trimHistory(),
        )
        return SimulationResult(updated, true, listOf("Paid ${money(applied)} of debt."))
    }

    fun invest(state: GameState, type: InvestmentType, amount: Int): SimulationResult {
        if (amount < type.minimumBuy) return failure(state, "Minimum ${type.label} buy is ${money(type.minimumBuy)}.")
        if (state.finances.cash < amount) return failure(state, "Not enough cash.")
        val existing = state.economy.investments.firstOrNull { it.type == type }
        val merged = if (existing != null) {
            existing.copy(principal = existing.principal + amount, currentValue = existing.currentValue + amount)
        } else {
            Investment(type = type, principal = amount, currentValue = amount)
        }
        val investments = state.economy.investments.filterNot { it.type == type } + merged
        val updated = state.copy(
            finances = state.finances.copy(cash = state.finances.cash - amount).normalized(),
            economy = state.economy.copy(investments = investments).normalized(),
            history = (state.history + HistoryEntry(state.day, "Invested in ${type.label}", "Put ${money(amount)} into the market.", HistoryKind.FINANCE)).trimHistory(),
        )
        return SimulationResult(updated, true, listOf("Invested ${money(amount)} in ${type.label}."))
    }

    fun sellInvestment(state: GameState, type: InvestmentType): SimulationResult {
        val holding = state.economy.investments.firstOrNull { it.type == type }
            ?: return failure(state, "You hold no ${type.label}.")
        val updated = state.copy(
            finances = state.finances.copy(cash = state.finances.cash + holding.currentValue).normalized(),
            economy = state.economy.copy(investments = state.economy.investments.filterNot { it.type == type }).normalized(),
            history = (state.history + HistoryEntry(state.day, "Sold ${type.label}", "Cashed out for ${money(holding.currentValue)} (${signed(holding.gain)}).", HistoryKind.FINANCE)).trimHistory(),
        )
        return SimulationResult(updated, true, listOf("Sold ${type.label} for ${money(holding.currentValue)}."))
    }

    fun buyAsset(state: GameState, assetId: String): SimulationResult {
        val def = AssetCatalog.byId(assetId) ?: return failure(state, "Unknown item.")
        if (!def.consumable && assetId in state.economy.ownedAssets) return failure(state, "You already own that.")
        if (state.finances.cash < def.price) return failure(state, "Not enough cash.")
        val charged = state.copy(finances = state.finances.copy(cash = state.finances.cash - def.price).normalized())
        val withEffect = applyEffect(charged, def.purchaseEffect)
        val owned = if (def.consumable) withEffect.economy.ownedAssets else withEffect.economy.ownedAssets + assetId
        val updated = withEffect.copy(
            economy = withEffect.economy.copy(ownedAssets = owned),
            history = (withEffect.history + HistoryEntry(state.day, "Bought ${def.name}", def.description, HistoryKind.FINANCE)).trimHistory(),
        )
        val message = if (def.consumable) "Enjoyed ${def.name}." else "Bought ${def.name}."
        return SimulationResult(updated, true, listOf(message))
    }

    fun sellAsset(state: GameState, assetId: String): SimulationResult {
        val def = AssetCatalog.byId(assetId) ?: return failure(state, "Unknown item.")
        if (assetId !in state.economy.ownedAssets) return failure(state, "You don't own that.")
        val resale = def.effectiveResale
        val updated = state.copy(
            finances = state.finances.copy(cash = state.finances.cash + resale).normalized(),
            economy = state.economy.copy(ownedAssets = state.economy.ownedAssets.filterNot { it == assetId }),
            history = (state.history + HistoryEntry(state.day, "Sold ${def.name}", "Recovered ${money(resale)}.", HistoryKind.FINANCE)).trimHistory(),
        )
        return SimulationResult(updated, true, listOf("Sold ${def.name} for ${money(resale)}."))
    }

    /** Set the share of post-bill cash that auto-moves to savings each payday. Capped so save + invest ≤ 100%. */
    fun setAutoSave(state: GameState, percent: Int): SimulationResult {
        val capped = percent.coerceIn(0, 100 - state.economy.autoInvestPercent)
        val updated = state.copy(economy = state.economy.copy(autoSavePercent = capped).normalized())
        val message = if (capped > 0) "Auto-save set to $capped% of spare cash each payday." else "Auto-save turned off."
        return SimulationResult(updated, true, listOf(message))
    }

    /** Set the share of post-bill cash that auto-invests each payday, and which holding it buys. */
    fun setAutoInvest(state: GameState, percent: Int, type: InvestmentType): SimulationResult {
        val capped = percent.coerceIn(0, 100 - state.economy.autoSavePercent)
        val updated = state.copy(economy = state.economy.copy(autoInvestPercent = capped, autoInvestType = type).normalized())
        val message = if (capped > 0) "Auto-invest set to $capped% into ${type.label} each payday." else "Auto-invest off (${type.label} selected)."
        return SimulationResult(updated, true, listOf(message))
    }

    // -----------------------------------------------------------------------
    // Decisions
    // -----------------------------------------------------------------------

    fun resolveDecision(state: GameState, choiceId: String): SimulationResult {
        val event = pendingDecisionEvent(state) ?: return failure(state.copy(pendingDecision = null), "No decision to resolve.")
        val choice = event.choices.firstOrNull { it.id == choiceId }
            ?: return failure(state, "Unknown choice.")
        if (choice.cashCost > state.finances.cash) return failure(state, "Not enough cash for that.")

        val picked = pickOutcome(choice.outcomes, state.rngSeed)
        val outcome = applyInsurance(state, event, picked.outcome)
        val afterCost = state.copy(finances = state.finances.copy(cash = state.finances.cash - choice.cashCost).normalized())
        val withCash = afterCost.copy(finances = afterCost.finances.copy(cash = afterCost.finances.cash + outcome.cashDelta).normalized())
        val applied = applyEffect(withCash, outcome.effect).copy(
            pendingDecision = null,
            rngSeed = picked.seed,
            history = (withCash.history + HistoryEntry(
                day = state.day,
                title = "${event.title}: ${choice.label}",
                detail = outcome.message,
                kind = HistoryKind.EVENT,
            )).trimHistory(),
        )
        return SimulationResult(applied, true, listOf(outcome.message))
    }

    // -----------------------------------------------------------------------
    // End of day
    // -----------------------------------------------------------------------

    fun advanceDay(state: GameState): SimulationResult {
        val messages = mutableListOf<String>()
        val entries = mutableListOf<HistoryEntry>()
        var working = state

        // Recurring modifiers (e.g. burnout) tick and apply.
        working = applyActiveModifiers(working)

        // Owned assets that change your daily baseline (apartment, gym, therapist).
        working = applyEffect(working, assetDailyEffect(working))

        // Weekly settlement: business income, savings interest, investment swings, bills.
        if (working.day >= working.finances.nextBillDueDay) {
            val settlement = settleWeek(working)
            working = settlement.state
            messages += settlement.messages
            entries += settlement.entries
        }

        // Debt pressure / relief and relationship drift overnight.
        working = applyEffect(working, overnightDebtEffect(working))
        working = applyEffect(working, RELATIONSHIP_DECAY)

        // Sleep restores energy (less so when stressed).
        working = applyEffect(working, overnightRecovery(working))

        // Passive event.
        val eventRoll = rollPassiveEvent(working)
        working = working.copy(rngSeed = eventRoll.seed)
        eventRoll.event?.let { event ->
            working = applyEffect(working, event.effect)
            messages += event.title
            entries += HistoryEntry(state.day, event.title, event.description, HistoryKind.EVENT)
        }

        // Burnout risk if pushed too hard.
        if (working.stats.stress >= 84 && working.modifiers.none { it.id == BURNOUT_RISK_ID }) {
            working = working.copy(modifiers = working.modifiers + burnoutRiskModifier())
        }

        // Roll a decision for tomorrow (only if none pending).
        if (working.pendingDecision == null) {
            val decisionRoll = rollDecision(working)
            working = working.copy(rngSeed = decisionRoll.seed)
            decisionRoll.event?.let { decision ->
                working = working.copy(pendingDecision = PendingDecision(decision.id))
                messages += "A decision is waiting: ${decision.title}."
            }
        }

        // A decision or event may have pushed promotion over the line.
        working = settlePromotion(working) { entry -> entries += entry; messages += entry.title }

        // Celebrate any goals the day pushed over the line.
        val (goalState, goalMessages, goalEntries) = reconcileGoals(working)
        working = goalState
        messages += goalMessages
        entries += goalEntries

        // Wake up.
        working = working.copy(
            calendar = CalendarState(day = working.day + 1, timeRemaining = DAILY_TIME_BUDGET, actionsToday = 0),
        )
        entries += HistoryEntry(state.day, "Day ${state.day} ended", "You wake up to day ${state.day + 1}.", HistoryKind.DAY)
        messages += "Day ${state.day + 1} begins."

        val updated = working.copy(history = (working.history + entries).trimHistory())
        return SimulationResult(updated, true, messages)
    }

    // -----------------------------------------------------------------------
    // Weekly settlement
    // -----------------------------------------------------------------------

    private data class SettlementResult(val state: GameState, val messages: List<String>, val entries: List<HistoryEntry>)

    private fun settleWeek(state: GameState): SettlementResult {
        val messages = mutableListOf<String>()
        val entries = mutableListOf<HistoryEntry>()
        var working = state

        // Business income.
        val businessNet = businessWeeklyNet(working)
        if (working.business.started && businessNet != 0) {
            working = working.copy(finances = working.finances.copy(cash = working.finances.cash + businessNet).normalized())
            val detail = "${working.business.clients} clients at ${working.business.tier.label}. Overhead ${money(working.business.tier.weeklyOverhead)}."
            messages += if (businessNet >= 0) "Business earned ${money(businessNet)}." else "Business ran ${money(-businessNet)} short."
            entries += HistoryEntry(working.day, "Business week", detail, HistoryKind.FINANCE)
        }

        // A business isn't build-and-forget: reputation slips and clients can churn each week,
        // so it has to be tended (marketing holds reputation, which holds clients).
        if (working.business.started) {
            var seed = working.rngSeed
            val rep = working.business.reputation
            val churnChance = (BUSINESS_CHURN_BASE - rep / 5).coerceIn(BUSINESS_CHURN_MIN, BUSINESS_CHURN_BASE)
            var lost = 0
            repeat(working.business.clients) {
                val (r, next) = roll(seed)
                seed = next
                if (r < churnChance) lost++
            }
            working = working.copy(
                rngSeed = seed,
                business = working.business.copy(
                    clients = working.business.clients - lost,
                    reputation = rep - BUSINESS_REP_DECAY,
                ).normalized(),
            )
            if (lost > 0) {
                messages += "Lost $lost client${if (lost == 1) "" else "s"} to churn."
                entries += HistoryEntry(working.day, "Client churn", "$lost client${if (lost == 1) "" else "s"} moved on. Keep reputation up to hold them.", HistoryKind.CAREER)
            }
        }

        // Property income (rentals, franchise) — the passive earners.
        val propertyIncome = assetWeeklyIncome(working)
        if (propertyIncome > 0) {
            working = working.copy(finances = working.finances.copy(cash = working.finances.cash + propertyIncome).normalized())
            messages += "Property income ${money(propertyIncome)}."
            entries += HistoryEntry(working.day, "Property income", "Your holdings paid ${money(propertyIncome)} this week.", HistoryKind.FINANCE)
        }

        // Savings interest (tracked as a running lifetime total so the player can see what they've made).
        if (working.economy.savings > 0) {
            val interest = (working.economy.savings * SAVINGS_WEEKLY_INTEREST_PERCENT / 100).coerceAtLeast(1)
            val lifetime = working.economy.lifetimeInterest + interest
            working = working.copy(economy = working.economy.copy(savings = working.economy.savings + interest, lifetimeInterest = lifetime))
            entries += HistoryEntry(working.day, "Savings interest", "Your savings earned ${money(interest)} (${money(lifetime)} all-time).", HistoryKind.FINANCE)
        }

        // Investment swings (seeded).
        if (working.economy.investments.isNotEmpty()) {
            var seed = working.rngSeed
            var net = 0
            val updatedHoldings = working.economy.investments.map { holding ->
                val swing = holding.type.swingPercent
                val (raw, nextSeed) = roll(seed)
                seed = nextSeed
                val changePercent = holding.type.driftPercent + (raw % (2 * swing + 1) - swing)
                val newValue = (holding.currentValue * (100 + changePercent) / 100).coerceAtLeast(0)
                net += newValue - holding.currentValue
                holding.copy(currentValue = newValue)
            }
            working = working.copy(
                rngSeed = seed,
                economy = working.economy.copy(investments = updatedHoldings).normalized(),
            )
            messages += if (net >= 0) "Investments gained ${money(net)}." else "Investments dropped ${money(-net)}."
            entries += HistoryEntry(working.day, "Market week", "Your portfolio moved ${signed(net)}.", HistoryKind.FINANCE)
        }

        // Living bill (rent + asset upkeep).
        val bill = weeklyLivingTotal(working)
        val nextDue = working.finances.nextBillDueDay + 7
        working = if (working.finances.cash >= bill) {
            working.copy(finances = working.finances.copy(cash = working.finances.cash - bill, nextBillDueDay = nextDue, creditScore = working.finances.creditScore + 1).normalized())
                .also { entries += HistoryEntry(working.day, "Weekly bill paid", "Covered ${money(bill)} of living costs.", HistoryKind.FINANCE) }
        } else {
            val shortfall = bill - working.finances.cash
            messages += "Bill created ${money(shortfall)} of debt."
            working.copy(finances = working.finances.copy(cash = 0, debt = working.finances.debt + shortfall, nextBillDueDay = nextDue, creditScore = working.finances.creditScore - 3).normalized())
                .also { entries += HistoryEntry(working.day, "Bill went unpaid", "Short ${money(shortfall)} — added to debt.", HistoryKind.FINANCE) }
        }
        messages.add(0, "Weekly bill: ${money(bill)}.")

        // Auto-allocation: sweep a chosen share of the cash left after the bill into savings/investments,
        // so the player never has to deposit by hand. Both shares draw from the same post-bill base.
        val eco = working.economy
        val sweepBase = working.finances.cash
        if (sweepBase > 0 && eco.autoAllocationPercent > 0) {
            val toSave = sweepBase * eco.autoSavePercent / 100
            val toInvest = sweepBase * eco.autoInvestPercent / 100
            if (toSave > 0) {
                working = working.copy(
                    finances = working.finances.copy(cash = working.finances.cash - toSave).normalized(),
                    economy = working.economy.copy(savings = working.economy.savings + toSave).normalized(),
                )
                entries += HistoryEntry(working.day, "Auto-saved", "Moved ${money(toSave)} to savings automatically.", HistoryKind.FINANCE)
            }
            if (toInvest > 0) {
                val type = eco.autoInvestType
                val existing = working.economy.investments.firstOrNull { it.type == type }
                val merged = existing?.copy(principal = existing.principal + toInvest, currentValue = existing.currentValue + toInvest)
                    ?: Investment(type = type, principal = toInvest, currentValue = toInvest)
                val investments = working.economy.investments.filterNot { it.type == type } + merged
                working = working.copy(
                    finances = working.finances.copy(cash = working.finances.cash - toInvest).normalized(),
                    economy = working.economy.copy(investments = investments).normalized(),
                )
                entries += HistoryEntry(working.day, "Auto-invested", "Put ${money(toInvest)} into ${type.label} automatically.", HistoryKind.FINANCE)
            }
            val moved = listOfNotNull(
                if (toSave > 0) "${money(toSave)} to savings" else null,
                if (toInvest > 0) "${money(toInvest)} to ${eco.autoInvestType.label}" else null,
            )
            if (moved.isNotEmpty()) messages += "Auto-moved ${moved.joinToString(" and ")}."
        }

        // Cost of living creeps up over time (roughly monthly), so a fixed income can't coast forever.
        val settlementIndex = state.day / 7
        if (settlementIndex > 0 && settlementIndex % COST_OF_LIVING_RISE_EVERY == 0) {
            val rise = (working.finances.weeklyLivingCost * COST_OF_LIVING_RISE_PERCENT / 100).coerceAtLeast(COST_OF_LIVING_RISE_MIN)
            working = working.copy(finances = working.finances.copy(weeklyLivingCost = working.finances.weeklyLivingCost + rise))
            messages += "Cost of living rose to ${money(working.finances.weeklyLivingCost)}/week."
            entries += HistoryEntry(working.day, "Cost of living rose", "Prices went up — the base weekly bill is now ${money(working.finances.weeklyLivingCost)}.", HistoryKind.FINANCE)
        }

        // New week, fresh gig energy: pay recovers to full.
        working = working.copy(finances = working.finances.copy(gigsThisWeek = 0))

        return SettlementResult(working, messages, entries)
    }

    /** Marks newly reached goals as complete and returns celebratory messages + history for them. */
    private fun reconcileGoals(state: GameState): Triple<GameState, List<String>, List<HistoryEntry>> {
        val metrics = goalMetrics(state)
        val newly = GoalCatalog.goals.filter { it.id !in state.completedGoals && it.isComplete(metrics) }
        if (newly.isEmpty()) return Triple(state, emptyList(), emptyList())
        val updated = state.copy(completedGoals = state.completedGoals + newly.map { it.id })
        val messages = newly.map { "Goal reached — ${it.title}!" }
        val entries = newly.map { HistoryEntry(state.day, "Goal: ${it.title}", it.description, HistoryKind.GOAL) }
        return Triple(updated, messages, entries)
    }

    // -----------------------------------------------------------------------
    // Risk rolls
    // -----------------------------------------------------------------------

    private data class RollUpdate(val state: GameState, val message: String, val entry: HistoryEntry)

    private fun resolveInterview(state: GameState): RollUpdate {
        val odds = interviewOdds(state)
        val (result, seed) = roll(state.rngSeed)
        return when {
            result < odds -> {
                val hired = state.copy(
                    rngSeed = seed,
                    career = state.career.copy(employed = true, title = jobTitleFor(1), level = 1, salaryPerShift = salaryFor(1), promotionReadiness = 20, reputation = state.career.reputation.coerceAtLeast(15)).normalized(),
                    jobSearch = JobSearchState(searchProgress = 0),
                )
                RollUpdate(hired, "You got the job! Entry Associate at ${money(salaryFor(1))}/shift.", HistoryEntry(state.day, "Hired!", "The interview turned into a real offer.", HistoryKind.CAREER))
            }
            result < odds + (100 - odds) / 2 -> {
                val callback = applyEffect(state.copy(rngSeed = seed), ActionEffect(searchProgressDelta = 6, moodDelta = -1))
                RollUpdate(callback, "Callback — they liked you, no offer yet.", HistoryEntry(state.day, "Interview callback", "You advanced a round but didn't close it.", HistoryKind.CAREER))
            }
            else -> {
                val rejected = applyEffect(state.copy(rngSeed = seed), ActionEffect(searchProgressDelta = -12, moodDelta = -3, stressDelta = 4))
                RollUpdate(rejected, "Rejected this time. Keep at it.", HistoryEntry(state.day, "Interview rejection", "Not this one. Your search took a small hit.", HistoryKind.CAREER))
            }
        }
    }

    private fun resolveFindClient(state: GameState): RollUpdate {
        val odds = clientOdds(state)
        val (result, seed) = roll(state.rngSeed)
        val atCapacity = state.business.clients >= state.business.tier.maxClients
        return if (result < odds && !atCapacity) {
            val won = state.copy(rngSeed = seed, business = state.business.copy(clients = state.business.clients + 1).normalized())
            RollUpdate(won, "Signed a new client!", HistoryEntry(state.day, "New client", "Your pitch landed — recurring revenue grows.", HistoryKind.CAREER))
        } else {
            val missed = applyEffect(state.copy(rngSeed = seed), ActionEffect(stressDelta = 2, businessReputationDelta = 1))
            RollUpdate(missed, "No bites this time.", HistoryEntry(state.day, "No new client", "The outreach didn't convert. Reputation helps next time.", HistoryKind.CAREER))
        }
    }

    // -----------------------------------------------------------------------
    // Effect application
    // -----------------------------------------------------------------------

    private fun applyEffect(state: GameState, effect: ActionEffect): GameState {
        val stats = state.stats.copy(
            health = state.stats.health + effect.healthDelta,
            mood = state.stats.mood + effect.moodDelta,
            energy = state.stats.energy + effect.energyDelta,
            stress = state.stats.stress + effect.stressDelta,
            social = state.stats.social + effect.socialDelta,
        ).clamped()
        val skills = state.skills.copy(
            knowledge = state.skills.knowledge + effect.knowledgeDelta,
            fitness = state.skills.fitness + effect.fitnessDelta,
            career = state.skills.career + effect.careerSkillDelta,
            communication = state.skills.communication + effect.communicationDelta,
            creativity = state.skills.creativity + effect.creativityDelta,
        ).clamped()
        val finances = state.finances.copy(
            cash = state.finances.cash + effect.cashDelta,
            debt = state.finances.debt + effect.debtDelta,
            creditScore = state.finances.creditScore + effect.creditScoreDelta,
        ).normalized()
        val career = state.career.copy(
            reputation = state.career.reputation + effect.reputationDelta,
            promotionReadiness = state.career.promotionReadiness + effect.promotionReadinessDelta,
        ).normalized()
        val jobSearch = state.jobSearch.copy(
            searchProgress = state.jobSearch.searchProgress + effect.searchProgressDelta,
        ).normalized()
        val business = state.business.copy(
            clients = state.business.clients + effect.businessClientsDelta,
            reputation = state.business.reputation + effect.businessReputationDelta,
        ).normalized()
        val relationships = state.relationships.copy(
            family = state.relationships.family + effect.familyDelta,
            friends = state.relationships.friends + effect.friendsDelta,
            network = state.relationships.network + effect.networkDelta,
        ).clamped()
        val modifiers = effect.modifier?.let { modifier ->
            state.modifiers.filterNot { it.id == modifier.id } + modifier
        } ?: state.modifiers
        return state.copy(
            stats = stats,
            skills = skills,
            finances = finances,
            career = career,
            jobSearch = jobSearch,
            business = business,
            relationships = relationships,
            modifiers = modifiers,
        )
    }

    private fun settlePromotion(state: GameState, onPromote: (HistoryEntry) -> Unit): GameState {
        if (!state.career.employed || state.career.promotionReadiness < 100) return state
        val newLevel = state.career.level + 1
        val title = jobTitleFor(newLevel)
        onPromote(HistoryEntry(state.day, "Promotion earned", "You advanced to $title. Shifts now pay ${money(salaryFor(newLevel))}.", HistoryKind.CAREER))
        return state.copy(
            career = state.career.copy(
                level = newLevel,
                title = title,
                salaryPerShift = salaryFor(newLevel),
                promotionReadiness = 25,
                reputation = (state.career.reputation + 6).coerceAtMost(100),
            ).normalized(),
        )
    }

    private fun applyActiveModifiers(state: GameState): GameState {
        val afterEffects = state.modifiers.fold(state) { current, modifier ->
            applyEffect(current, ActionEffect(healthDelta = modifier.healthDelta, moodDelta = modifier.moodDelta, energyDelta = modifier.energyDelta, stressDelta = modifier.stressDelta))
        }
        return afterEffects.copy(modifiers = afterEffects.modifiers.mapNotNull { it.tick().takeIf { ticked -> ticked.daysRemaining > 0 } })
    }

    private fun overnightDebtEffect(state: GameState): ActionEffect =
        if (state.finances.debt > 0) {
            ActionEffect(moodDelta = -2, stressDelta = (3 + state.finances.debt / 150).coerceAtMost(10), creditScoreDelta = -1)
        } else {
            ActionEffect(stressDelta = -1, creditScoreDelta = 1)
        }

    private fun overnightRecovery(state: GameState): ActionEffect = ActionEffect(
        healthDelta = if (state.stats.stress > 80) -3 else 2,
        moodDelta = if (state.stats.stress > 75) -3 else 2,
        energyDelta = 50 - (state.stats.stress / 5),
        stressDelta = -6,
    )

    private fun assetDailyEffect(state: GameState): ActionEffect =
        ownedDefinitions(state).fold(ActionEffect()) { acc, def -> acc + def.dailyEffect }

    // -----------------------------------------------------------------------
    // Event / decision rolls
    // -----------------------------------------------------------------------

    private data class EventRoll(val event: LifeEventDefinition?, val seed: Long)

    private fun rollPassiveEvent(state: GameState): EventRoll {
        val next = nextSeed(state.rngSeed)
        val eligible = events.filter { it.condition(state) }
        if (eligible.isEmpty() || positiveModulo(next, 100) >= PASSIVE_EVENT_CHANCE) return EventRoll(null, next)
        val event = eligible[positiveModulo(next / 97L, eligible.size)]
        return EventRoll(event, nextSeed(next))
    }

    private fun rollDecision(state: GameState): EventRoll {
        val next = nextSeed(state.rngSeed)
        val eligible = decisions.filter { it.condition(state) }
        if (eligible.isEmpty() || positiveModulo(next, 100) >= DECISION_CHANCE) return EventRoll(null, next)
        val event = eligible[positiveModulo(next / 131L, eligible.size)]
        return EventRoll(event, nextSeed(next))
    }

    private data class PickedOutcome(val outcome: EventOutcome, val seed: Long)

    private fun pickOutcome(outcomes: List<EventOutcome>, seed: Long): PickedOutcome {
        if (outcomes.size == 1) return PickedOutcome(outcomes.first(), seed)
        val total = outcomes.sumOf { it.weight }.coerceAtLeast(1)
        val next = nextSeed(seed)
        var pick = positiveModulo(next, total)
        for (outcome in outcomes) {
            if (pick < outcome.weight) return PickedOutcome(outcome, next)
            pick -= outcome.weight
        }
        return PickedOutcome(outcomes.last(), next)
    }

    private fun applyInsurance(state: GameState, event: LifeEventDefinition, outcome: EventOutcome): EventOutcome =
        if (event.id == "medical" && !outcome.good && outcome.cashDelta < 0 && hasTag(state, AssetTag.INSURANCE)) {
            outcome.copy(cashDelta = outcome.cashDelta / 2, message = outcome.message + " (insurance covered half)")
        } else {
            outcome
        }

    // -----------------------------------------------------------------------
    // Availability + dynamic effects
    // -----------------------------------------------------------------------

    private fun unavailableReason(state: GameState, action: DailyActionDefinition): String? = when {
        action.id in EMPLOYED_ONLY && !state.career.employed -> "You need a steady job first."
        action.id in UNEMPLOYED_ONLY && state.career.employed -> "You already have a steady job."
        action.id == "attend_interview" && state.jobSearch.searchProgress < INTERVIEW_GATE -> "Build your job search to $INTERVIEW_GATE% first."
        action.id == "launch_business" && state.business.started -> "Your business is already running."
        action.id == "find_client" && !state.business.started -> "Launch your business first."
        action.id == "find_client" && state.business.clients >= state.business.tier.maxClients -> "You're at full capacity for this tier."
        action.id == "marketing" && !state.business.started -> "Launch your business first."
        action.id == "upgrade_business" && !state.business.started -> "Launch your business first."
        action.id == "upgrade_business" && nextTier(state.business.tier) == null -> "You're at the top tier."
        action.id == "upgrade_business" && state.finances.cash < state.business.tier.upgradeCost -> "Need ${money(state.business.tier.upgradeCost)} to upgrade."
        state.timeRemaining < action.timeCost -> "Not enough time left today."
        state.stats.energy < action.energyCost -> "Not enough energy."
        state.finances.cash < action.moneyCost -> "Not enough cash."
        else -> null
    }

    private fun dynamicEffectFor(state: GameState, action: DailyActionDefinition): ActionEffect = when (action.id) {
        "gig_work" -> {
            // Gigs pay less the more you lean on them in a week: a survival tool, not a wealth engine.
            val base = (GIG_BASE_PAY - state.finances.gigsThisWeek * GIG_FATIGUE_STEP).coerceAtLeast(GIG_MIN_PAY)
            action.effect.copy(cashDelta = base + if (hasTag(state, AssetTag.GIG_BOOST)) GIG_BOOST_BONUS else 0)
        }
        "work_shift" -> action.effect.copy(cashDelta = withDegreeBonus(state, state.career.salaryPerShift))
        "overtime" -> action.effect.copy(cashDelta = withDegreeBonus(state, (state.career.salaryPerShift * 0.6f).toInt() + 25))
        "study" -> if (hasTag(state, AssetTag.STUDY_BOOST)) {
            action.effect.copy(knowledgeDelta = action.effect.knowledgeDelta + 8, careerSkillDelta = action.effect.careerSkillDelta + 3)
        } else {
            action.effect
        }
        else -> action.effect
    }

    private fun suggestionIdsFor(state: GameState): List<String> {
        val ids = buildList {
            if (!state.career.employed) {
                if (state.jobSearch.searchProgress >= INTERVIEW_GATE) add("attend_interview") else add("apply_jobs")
                add("gig_work")
            } else {
                add("work_shift")
                if (state.career.promotionReadiness >= 60) add("manager_check_in")
            }
            if (state.business.started && state.business.clients < state.business.tier.maxClients) add("find_client")
            if (state.stats.stress >= 72 || state.stats.energy <= 28) add("rest")
        }
        return ids.distinct().filter { id -> actions.firstOrNull { it.id == id }?.let { unavailableReason(state, it) == null } == true }.take(3)
    }

    // -----------------------------------------------------------------------
    // Presentation helpers
    // -----------------------------------------------------------------------

    private fun statusFor(state: GameState): String = when {
        !state.career.employed && state.finances.cash < weeklyLivingTotal(state) -> "Scraping by"
        state.finances.debt >= 700 -> "Debt pressure"
        state.stats.health <= 35 -> "Fragile"
        state.stats.stress >= 82 -> "Burnout risk"
        passiveIncome(state).total.let { it > 0 && it >= weeklyLivingTotal(state) } -> "Financially Free"
        netWorth(state) >= 10_000 -> "Wealthy"
        netWorth(state) >= 3_000 -> "Comfortable"
        state.career.employed && state.career.promotionReadiness >= 80 -> "Breakthrough close"
        state.business.tier >= BusinessTier.STUDIO -> "Business growing"
        netWorth(state) >= 0 -> "Stable"
        else -> "In the red"
    }

    private fun pressureSummaryFor(state: GameState): String {
        val weekly = weeklyLivingTotal(state)
        return when {
            !state.career.employed && state.finances.cash < weekly -> "Cash is tight and there's no steady job yet. Gigs buy time; applications and interviews land the job."
            state.finances.debt >= 500 -> "Debt is heavy. Pay it down from the Money tab before interest compounds the stress."
            state.career.employed && state.career.promotionReadiness >= 70 -> "A promotion is close. Shifts, check-ins, and study push it over the line."
            state.business.started && state.business.clients == 0 -> "Your business needs clients. Find a few and weekly revenue starts flowing."
            state.stats.stress >= 70 -> "Stress is high. Rest, exercise, or a getaway protects everything else."
            netWorth(state) >= 3_000 -> "You're building real wealth. Keep investing and let it compound."
            else -> "Steady. Earn, set money aside, and look for the next upgrade."
        }
    }

    private fun summarizeAction(action: DailyActionDefinition, effect: ActionEffect): String {
        val gains = buildList {
            if (effect.cashDelta > 0) add("+${money(effect.cashDelta)}")
            if (effect.searchProgressDelta > 0) add("+${effect.searchProgressDelta} search")
            if (effect.promotionReadinessDelta > 0) add("+${effect.promotionReadinessDelta} promotion")
            if (effect.businessReputationDelta > 0) add("+${effect.businessReputationDelta} biz rep")
            if (effect.moodDelta > 0) add("+${effect.moodDelta} mood")
            if (effect.stressDelta < 0) add("${effect.stressDelta} stress")
        }
        val cost = "Time -${action.timeCost}h, energy -${action.energyCost}" + if (action.moneyCost > 0) ", ${money(-action.moneyCost)}" else ""
        return if (gains.isEmpty()) cost else "$cost. ${gains.joinToString(", ")}."
    }

    private fun buildActionDeltas(state: GameState, action: DailyActionDefinition, effect: ActionEffect): List<ActionDelta> = buildList {
        add(ActionDelta("Time", -action.timeCost))
        val energy = effect.energyDelta - action.energyCost
        if (energy != 0) add(ActionDelta("Energy", energy))
        val cash = effect.cashDelta - action.moneyCost
        if (cash != 0) add(ActionDelta("Cash", cash))
        if (effect.stressDelta != 0) add(ActionDelta("Stress", effect.stressDelta, positiveIsGood = false))
        if (effect.moodDelta != 0) add(ActionDelta("Mood", effect.moodDelta))
        if (effect.healthDelta != 0) add(ActionDelta("Health", effect.healthDelta))
        if (effect.searchProgressDelta != 0) add(ActionDelta("Job", effect.searchProgressDelta))
        if (effect.promotionReadinessDelta != 0) add(ActionDelta("Promo", effect.promotionReadinessDelta))
        if (effect.businessReputationDelta != 0) add(ActionDelta("Biz rep", effect.businessReputationDelta))
        if (effect.knowledgeDelta != 0) add(ActionDelta("Knowledge", effect.knowledgeDelta))
        val relationship = effect.socialDelta + effect.familyDelta + effect.friendsDelta + effect.networkDelta
        if (relationship != 0) add(ActionDelta("Social", relationship))
    }.take(6)

    // -----------------------------------------------------------------------
    // Small helpers
    // -----------------------------------------------------------------------

    private fun ownedDefinitions(state: GameState) = AssetCatalog.ownedDefinitions(state.economy.ownedAssets)

    private fun hasTag(state: GameState, tag: AssetTag): Boolean =
        ownedDefinitions(state).any { tag in it.tags }

    private fun assetWeeklyCost(state: GameState): Int =
        ownedDefinitions(state).sumOf { it.weeklyUpkeep + it.rentDelta }

    private fun assetWeeklyIncome(state: GameState): Int =
        ownedDefinitions(state).sumOf { it.weeklyIncome }

    private fun assetResaleValue(state: GameState): Int =
        ownedDefinitions(state).sumOf { it.effectiveResale }

    private fun withDegreeBonus(state: GameState, base: Int): Int =
        base + if (hasTag(state, AssetTag.DEGREE)) base * DEGREE_SALARY_BONUS_PERCENT / 100 else 0

    private fun nextTier(tier: BusinessTier): BusinessTier? = when (tier) {
        BusinessTier.NONE -> null
        BusinessTier.SIDE_HUSTLE -> BusinessTier.STUDIO
        BusinessTier.STUDIO -> BusinessTier.AGENCY
        BusinessTier.AGENCY -> BusinessTier.FIRM
        BusinessTier.FIRM -> null
    }

    private fun jobTitleFor(level: Int): String = when {
        level >= 5 -> "Lead Operator"
        level >= 4 -> "Senior Associate"
        level >= 3 -> "Project Associate"
        level >= 2 -> "Associate"
        else -> "Entry Associate"
    }

    private fun salaryFor(level: Int): Int = 105 + ((level - 1).coerceAtLeast(0) * 40)

    private fun failure(state: GameState, message: String): SimulationResult =
        SimulationResult(state = state, success = false, messages = emptyList(), errorMessage = message)

    private fun roll(seed: Long): Pair<Int, Long> {
        val next = nextSeed(seed)
        return positiveModulo(next, 100) to next
    }

    private fun nextSeed(seed: Long): Long = seed * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L

    private fun positiveModulo(value: Long, modulus: Int): Int = Math.floorMod(value, modulus.toLong()).toInt()

    private fun List<HistoryEntry>.trimHistory(): List<HistoryEntry> = takeLast(HISTORY_LIMIT)

    private fun burnoutRiskModifier(): LifeModifier = LifeModifier(
        id = BURNOUT_RISK_ID,
        title = "Burnout risk",
        description = "Daily energy and mood suffer until you recover.",
        daysRemaining = 3,
        moodDelta = -2,
        energyDelta = -6,
        stressDelta = 3,
    )

    private operator fun ActionEffect.plus(other: ActionEffect): ActionEffect = ActionEffect(
        cashDelta = cashDelta + other.cashDelta,
        debtDelta = debtDelta + other.debtDelta,
        creditScoreDelta = creditScoreDelta + other.creditScoreDelta,
        healthDelta = healthDelta + other.healthDelta,
        moodDelta = moodDelta + other.moodDelta,
        energyDelta = energyDelta + other.energyDelta,
        stressDelta = stressDelta + other.stressDelta,
        socialDelta = socialDelta + other.socialDelta,
        knowledgeDelta = knowledgeDelta + other.knowledgeDelta,
        fitnessDelta = fitnessDelta + other.fitnessDelta,
        careerSkillDelta = careerSkillDelta + other.careerSkillDelta,
        communicationDelta = communicationDelta + other.communicationDelta,
        creativityDelta = creativityDelta + other.creativityDelta,
        reputationDelta = reputationDelta + other.reputationDelta,
        promotionReadinessDelta = promotionReadinessDelta + other.promotionReadinessDelta,
        searchProgressDelta = searchProgressDelta + other.searchProgressDelta,
        businessClientsDelta = businessClientsDelta + other.businessClientsDelta,
        businessReputationDelta = businessReputationDelta + other.businessReputationDelta,
        familyDelta = familyDelta + other.familyDelta,
        friendsDelta = friendsDelta + other.friendsDelta,
        networkDelta = networkDelta + other.networkDelta,
        modifier = other.modifier ?: modifier,
    )

    private fun money(value: Int): String = if (value < 0) "-\$${-value}" else "\$$value"

    private fun signed(value: Int): String = if (value >= 0) "+${money(value)}" else money(value)

    companion object {
        const val DAILY_TIME_BUDGET = 12
        const val INTERVIEW_GATE = 40
        private const val STARTING_SEED = 50_001L
        private const val PASSIVE_EVENT_CHANCE = 28
        private const val DECISION_CHANCE = 38
        private const val SAVINGS_WEEKLY_INTEREST_PERCENT = 1
        // Gig pay diminishes with weekly use, so it can't replace real income.
        private const val GIG_BASE_PAY = 60
        private const val GIG_FATIGUE_STEP = 12
        private const val GIG_MIN_PAY = 15
        private const val GIG_BOOST_BONUS = 30
        private const val DEGREE_SALARY_BONUS_PERCENT = 25
        // A business has to be tended: reputation decays and clients churn each week.
        private const val BUSINESS_CHURN_BASE = 20
        private const val BUSINESS_CHURN_MIN = 3
        private const val BUSINESS_REP_DECAY = 4
        // Cost of living rises ~monthly so a fixed income can't coast.
        private const val COST_OF_LIVING_RISE_EVERY = 4
        private const val COST_OF_LIVING_RISE_PERCENT = 8
        private const val COST_OF_LIVING_RISE_MIN = 5
        private const val HISTORY_LIMIT = 100
        private const val BURNOUT_RISK_ID = "burnout_risk"
        private val RELATIONSHIP_DECAY = ActionEffect(socialDelta = -2, familyDelta = -1, friendsDelta = -2, networkDelta = -1)
        private val EMPLOYED_ONLY = setOf("work_shift", "overtime", "manager_check_in")
        private val UNEMPLOYED_ONLY = setOf("apply_jobs", "interview_prep", "attend_interview")
    }
}
