package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionCategory
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition
import com.azizjonkasimov.lifesimulator.domain.model.DailyFocus
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
import com.azizjonkasimov.lifesimulator.domain.model.DayPlanState
import com.azizjonkasimov.lifesimulator.domain.model.FinanceState
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.GoalCategory
import com.azizjonkasimov.lifesimulator.domain.model.GoalState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition
import com.azizjonkasimov.lifesimulator.domain.model.LifeModifier
import com.azizjonkasimov.lifesimulator.domain.model.LifeProfile
import com.azizjonkasimov.lifesimulator.domain.model.RelationshipState
import com.azizjonkasimov.lifesimulator.domain.model.SimulationResult
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet
import com.azizjonkasimov.lifesimulator.domain.model.TimedOpportunityState

class LifeSimulationEngine(
    private val actions: List<DailyActionDefinition> = ActionCatalog.actions,
    private val events: List<LifeEventDefinition> = EventCatalog.events,
) {
    fun startNewLife(archetype: LifeArchetype): GameState {
        val preset = startingPreset(archetype)
        val base = GameState(
            profile = LifeProfile(
                name = "Alex Rivers",
                age = 22,
                archetype = archetype,
            ),
            calendar = CalendarState(day = 1, timeRemaining = DAILY_TIME_BUDGET),
            stats = preset.stats,
            skills = preset.skills,
            finances = preset.finances,
            career = preset.career,
            relationships = preset.relationships,
            goals = defaultGoals(preset.finances.cash, preset.career.promotionReadiness, preset.relationships.average),
            modifiers = emptyList(),
            dayPlan = emptyDayPlan(day = 1),
            timedOpportunities = emptyList(),
            opportunityCooldowns = emptyMap(),
            rngSeed = seedFor(archetype),
            history = listOf(
                HistoryEntry(
                    day = 1,
                    title = "New life started",
                    detail = "You began as a ${archetype.displayName}. Build cash, career, wellbeing, and relationships.",
                    kind = HistoryKind.SYSTEM,
                ),
            ),
        )
        return prepareNewDay(base).state
    }

    fun dashboardSnapshot(state: GameState): DashboardSnapshot {
        val daysUntilBill = (state.finances.nextBillDueDay - state.day).coerceAtLeast(0)
        val alerts = buildList {
            if (state.finances.cash < state.finances.weeklyLivingCost) {
                add("Cash is below next weekly bill.")
            }
            if (state.finances.debt > 0) {
                add("Debt is adding pressure each night.")
            }
            if (state.stats.stress >= 75) {
                add("Stress is near burnout range.")
            }
            if (state.relationships.average <= 40) {
                add("Relationships need attention.")
            }
            if (state.career.promotionReadiness >= 80) {
                add("Promotion is close.")
            }
            state.modifiers.forEach { add("${it.title}: ${it.daysRemaining} days left.") }
        }.take(4)

        return DashboardSnapshot(
            headline = "Week ${state.week}, Day ${state.day}",
            status = statusFor(state),
            alerts = alerts,
            pressureSummary = pressureSummaryFor(state),
            quickActionIds = quickActionIdsFor(state),
            nextBillLabel = if (daysUntilBill == 0) {
                "Due today: ${state.finances.weeklyLivingCost} USD"
            } else {
                "In $daysUntilBill days: ${state.finances.weeklyLivingCost} USD"
            },
            netWorth = state.finances.netWorth,
            focusGoal = state.goals.firstOrNull { !it.isComplete } ?: state.goals.firstOrNull(),
        )
    }

    fun actionAvailability(state: GameState): List<ActionAvailability> =
        availableActionsFor(state).map { action ->
            val reason = unavailableReason(state, action)
            val focusMatch = focusMatches(state.dayPlan.activeFocus, action, state.dayPlan)
            val effect = dynamicEffectFor(state, action) + focusBonusFor(state, action)
            ActionAvailability(
                action = action,
                isAvailable = reason == null,
                reason = reason,
                previewDeltas = buildActionDeltas(
                    action = action,
                    effect = effect,
                    opportunityProgressDelta = opportunityProgressPreview(state, action),
                ),
                focusMatch = focusMatch,
                recommendationReason = recommendationReasonFor(state, action, focusMatch),
            )
        }

    fun selectDailyFocus(state: GameState, focus: DailyFocus): SimulationResult {
        if (state.dayPlan.locked || state.dayPlan.actionsTaken > 0) {
            return SimulationResult(
                state = state,
                success = false,
                messages = emptyList(),
                errorMessage = "Today's focus is locked after your first action.",
            )
        }

        val updated = state.copy(dayPlan = state.dayPlan.copy(activeFocus = focus))
        val message = if (focus == state.dayPlan.recommendedFocus) {
            "Following the suggested ${focus.label} focus."
        } else {
            "Switched today's focus to ${focus.label}."
        }
        return SimulationResult(state = updated, success = true, messages = listOf(message))
    }

    fun performAction(state: GameState, actionId: String): SimulationResult {
        val action = availableActionsFor(state).firstOrNull { it.id == actionId }
            ?: return SimulationResult(
                state = state,
                success = false,
                messages = emptyList(),
                errorMessage = "Unknown action.",
            )

        val reason = unavailableReason(state, action)
        if (reason != null) {
            return SimulationResult(
                state = state,
                success = false,
                messages = emptyList(),
                errorMessage = reason,
            )
        }

        val beforeGoals = state.goals
        val baseEffect = dynamicEffectFor(state, action)
        val focusBonus = focusBonusFor(state, action)
        val effect = baseEffect + focusBonus
        val actionDeltas = buildActionDeltas(
            action = action,
            effect = effect,
            opportunityProgressDelta = opportunityProgressPreview(state, action),
        )
        val charged = state.copy(
            calendar = state.calendar.copy(
                timeRemaining = (state.timeRemaining - action.timeCost).coerceAtLeast(0),
            ),
            finances = state.finances.copy(cash = state.finances.cash - action.moneyCost).normalized(),
            stats = state.stats.copy(energy = state.stats.energy - action.energyCost).clamped(),
        )
        val applied = applyEffect(charged, effect)
            .let { if (action.id == "rest") it.copy(modifiers = it.modifiers.filterNot { modifier -> modifier.id == BURNOUT_RISK_ID }) else it }
            .trackActionInPlan(action)
            .refreshDynamicGoals()
        val opportunityProgressed = refreshOpportunityProgress(applied, action)
        val opportunityUpdate = resolveOpportunities(opportunityProgressed, failExpired = false)
        val afterOpportunity = opportunityUpdate.state
            .refreshDynamicGoals()
            .settlePromotion()

        val completedGoals = newlyCompleted(beforeGoals, afterOpportunity.goals)
        val rewarded = applyGoalRewards(afterOpportunity, completedGoals)
        val updated = rewarded.copy(
            history = buildList {
                addAll(rewarded.history)
                add(
                    HistoryEntry(
                        day = rewarded.day,
                        title = action.title,
                        detail = summarizeAction(action, effect),
                        kind = HistoryKind.ACTION,
                    ),
                )
                addAll(opportunityUpdate.entries)
                completedGoals.forEach { goal ->
                    add(
                        HistoryEntry(
                            day = rewarded.day,
                            title = "Goal completed: ${goal.title}",
                            detail = goal.rewardText,
                            kind = HistoryKind.GOAL,
                        ),
                    )
                }
            }.trimHistory(),
        )

        return SimulationResult(
            state = updated,
            success = true,
            messages = buildList {
                add("${action.title} completed.")
                if (focusBonus.hasAnyImpact()) {
                    add("${state.dayPlan.activeFocus.label} focus gave a bonus.")
                }
                addAll(opportunityUpdate.messages)
                completedGoals.forEach { add("Goal completed: ${it.title}.") }
            },
            actionDeltas = actionDeltas,
        )
    }

    fun advanceDay(state: GameState): SimulationResult {
        val beforeGoals = state.goals
        val afterModifiers = applyActiveModifiers(state)
        val billResult = applyBills(afterModifiers)
        val debtPenalty = if (billResult.state.finances.debt > 0) {
            ActionEffect(
                moodDelta = -2,
                stressDelta = (4 + billResult.state.finances.debt / 120).coerceAtMost(12),
                creditScoreDelta = -1,
            )
        } else {
            ActionEffect(stressDelta = -1, creditScoreDelta = 1)
        }
        val relationshipDecay = ActionEffect(
            socialDelta = -2,
            familyDelta = -1,
            friendsDelta = -2,
            networkDelta = -1,
        )
        val recovery = ActionEffect(
            healthDelta = if (billResult.state.stats.stress > 80) -3 else 2,
            moodDelta = if (billResult.state.stats.stress > 75) -4 else 2,
            energyDelta = 50 - (billResult.state.stats.stress / 5),
            stressDelta = -6,
        )
        val recovered = applyEffect(
            state = applyEffect(
                state = applyEffect(billResult.state, debtPenalty),
                effect = relationshipDecay,
            ),
            effect = recovery,
        ).refreshDynamicGoals()

        val eventRoll = rollEvent(recovered)
        val withEvent = eventRoll.event?.let { applyEffect(recovered, it.effect) } ?: recovered
        val withBurnoutRisk = if (
            withEvent.stats.stress >= 84 &&
            withEvent.modifiers.none { it.id == BURNOUT_RISK_ID }
        ) {
            withEvent.copy(modifiers = withEvent.modifiers + burnoutRiskModifier())
        } else {
            withEvent
        }
        val focusOutcome = applyFocusEndOfDay(withBurnoutRisk)
        val opportunityProgressed = refreshOpportunityProgress(focusOutcome.state.refreshDynamicGoals(), action = null)
        val opportunityUpdate = resolveOpportunities(opportunityProgressed, failExpired = true)
        val afterOpportunity = opportunityUpdate.state
            .refreshDynamicGoals()
            .settlePromotion()
        val completedGoals = newlyCompleted(beforeGoals, afterOpportunity.goals)
        val rewarded = applyGoalRewards(afterOpportunity, completedGoals)
        val nextDayBase = rewarded.copy(
            calendar = CalendarState(day = rewarded.day + 1, timeRemaining = DAILY_TIME_BUDGET),
            rngSeed = eventRoll.nextSeed,
            history = buildList {
                addAll(rewarded.history)
                billResult.history?.let { add(it) }
                eventRoll.event?.let { event ->
                    add(
                        HistoryEntry(
                            day = state.day,
                            title = event.title,
                            detail = event.description,
                            kind = HistoryKind.EVENT,
                        ),
                    )
                }
                addAll(focusOutcome.entries)
                addAll(opportunityUpdate.entries)
                completedGoals.forEach { goal ->
                    add(
                        HistoryEntry(
                            day = state.day,
                            title = "Goal completed: ${goal.title}",
                            detail = goal.rewardText,
                            kind = HistoryKind.GOAL,
                        ),
                    )
                }
                add(
                    HistoryEntry(
                        day = state.day,
                        title = "Day ${state.day} ended",
                        detail = "You wake up to day ${state.day + 1}.",
                        kind = HistoryKind.DAY,
                    ),
                )
            }.trimHistory(),
        )
        val newDay = prepareNewDay(nextDayBase)
        val nextDay = newDay.newOpportunityTitle?.let { title ->
            newDay.state.copy(
                history = (newDay.state.history + HistoryEntry(
                    day = newDay.state.day,
                    title = "Opportunity opened: $title",
                    detail = "A timed pressure goal is now active.",
                    kind = HistoryKind.GOAL,
                )).trimHistory(),
            )
        } ?: newDay.state

        return SimulationResult(
            state = nextDay,
            success = true,
            messages = buildList {
                billResult.message?.let { add(it) }
                eventRoll.event?.let { add(it.title) }
                addAll(focusOutcome.messages)
                addAll(opportunityUpdate.messages)
                completedGoals.forEach { add("Goal completed: ${it.title}.") }
                newDay.newOpportunityTitle?.let { add("New opportunity: $it.") }
                add("Day ${state.day + 1} begins.")
            },
        )
    }

    private fun availableActionsFor(state: GameState): List<DailyActionDefinition> =
        actions.filter { action ->
            action.allowedArchetypes.isEmpty() || state.archetype in action.allowedArchetypes
        }

    private fun unavailableReason(state: GameState, action: DailyActionDefinition): String? = when {
        state.timeRemaining < action.timeCost -> "Not enough time left today."
        state.stats.energy < action.energyCost -> "Not enough energy."
        state.finances.cash < action.moneyCost -> "Not enough cash."
        else -> null
    }

    private fun dynamicEffectFor(state: GameState, action: DailyActionDefinition): ActionEffect = when (action.id) {
        "work_shift" -> action.effect.copy(cashDelta = state.career.salaryPerShift)
        "overtime" -> action.effect.copy(cashDelta = (state.career.salaryPerShift * 0.65f).toInt() + 25)
        "freelance_gig" -> action.effect.copy(cashDelta = 110 + (state.skills.creativity / 3) + (state.career.reputation / 4))
        "pitch_client" -> action.effect.copy(cashDelta = 65 + (state.skills.communication / 3) + (state.career.reputation / 3))
        "budget_review" -> if (state.finances.debt > 0) {
            action.effect.copy(cashDelta = 0, debtDelta = -45, creditScoreDelta = 4)
        } else {
            action.effect.copy(cashDelta = 15, debtDelta = 0, creditScoreDelta = 1)
        }
        else -> action.effect
    }

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
            career = state.skills.career + effect.careerXpDelta,
            communication = state.skills.communication + effect.communicationDelta,
            creativity = state.skills.creativity + effect.creativityDelta,
        ).clamped()
        val finances = state.finances.copy(
            cash = state.finances.cash + effect.cashDelta,
            debt = state.finances.debt + effect.debtDelta,
            creditScore = state.finances.creditScore + effect.creditScoreDelta,
        ).normalized()
        val career = state.career.copy(
            xp = state.career.xp + effect.careerXpDelta,
            reputation = state.career.reputation + effect.reputationDelta,
            promotionReadiness = state.career.promotionReadiness + effect.promotionReadinessDelta,
        ).normalized()
        val relationships = state.relationships.copy(
            family = state.relationships.family + effect.familyDelta,
            friends = state.relationships.friends + effect.friendsDelta,
            network = state.relationships.network + effect.networkDelta,
        ).clamped()
        val goals = state.goals.map { goal ->
            effect.goalProgress[goal.id]?.let(goal::advanced) ?: goal
        }
        val modifiers = effect.modifier?.let { modifier ->
            state.modifiers.filterNot { it.id == modifier.id } + modifier
        } ?: state.modifiers

        return state.copy(
            stats = stats,
            skills = skills,
            finances = finances,
            career = career,
            relationships = relationships,
            goals = goals,
            modifiers = modifiers,
        )
    }

    private fun GameState.refreshDynamicGoals(): GameState = copy(
        goals = goals.map { goal ->
            when (goal.id) {
                "emergency_buffer" -> goal.copy(progress = goal.progress.coerceAtLeast(finances.cash.coerceAtMost(goal.target)))
                "first_promotion" -> goal.copy(progress = goal.progress.coerceAtLeast(career.promotionReadiness.coerceAtMost(goal.target)))
                "social_circle" -> goal.copy(progress = goal.progress.coerceAtLeast(relationships.average.coerceAtMost(goal.target)))
                else -> goal
            }
        },
    )

    private fun GameState.settlePromotion(): GameState {
        if (career.promotionReadiness < 100) return this
        val newLevel = career.level + 1
        return copy(
            career = career.copy(
                level = newLevel,
                title = jobTitleFor(archetype, newLevel),
                salaryPerShift = salaryFor(archetype, newLevel),
                promotionReadiness = 25,
                reputation = (career.reputation + 6).coerceAtMost(100),
            ),
            history = (history + HistoryEntry(
                day = day,
                title = "Promotion earned",
                detail = "You advanced to ${jobTitleFor(archetype, newLevel)}.",
                kind = HistoryKind.CAREER,
            )).trimHistory(),
        )
    }

    private fun applyGoalRewards(state: GameState, completedGoals: List<GoalState>): GameState =
        completedGoals.fold(state) { current, goal ->
            when (goal.id) {
                "emergency_buffer" -> applyEffect(current, ActionEffect(moodDelta = 4, stressDelta = -4, creditScoreDelta = 8))
                "first_promotion" -> applyEffect(current, ActionEffect(cashDelta = 75, moodDelta = 6, reputationDelta = 4))
                "stable_week" -> applyEffect(current, ActionEffect(healthDelta = 5, moodDelta = 5, stressDelta = -5))
                "social_circle" -> applyEffect(current, ActionEffect(moodDelta = 7, stressDelta = -4, communicationDelta = 4))
                else -> current
            }
        }

    private fun newlyCompleted(before: List<GoalState>, after: List<GoalState>): List<GoalState> =
        after.filter { goal ->
            goal.isComplete && before.firstOrNull { it.id == goal.id }?.isComplete != true
        }

    private fun applyActiveModifiers(state: GameState): GameState {
        var updated = state
        state.modifiers.forEach { modifier ->
            updated = applyEffect(
                updated,
                ActionEffect(
                    healthDelta = modifier.healthDelta,
                    moodDelta = modifier.moodDelta,
                    energyDelta = modifier.energyDelta,
                    stressDelta = modifier.stressDelta,
                ),
            )
        }
        return updated.copy(modifiers = state.modifiers.mapNotNull { it.tick().takeIf { ticked -> ticked.daysRemaining > 0 } })
    }

    private fun applyBills(state: GameState): BillResult {
        if (state.day < state.finances.nextBillDueDay) return BillResult(state = state)
        val remainingCash = state.finances.cash - state.finances.weeklyLivingCost
        val shortfall = (-remainingCash).coerceAtLeast(0)
        val finances = state.finances.copy(
            cash = remainingCash.coerceAtLeast(0),
            debt = state.finances.debt + shortfall,
            nextBillDueDay = state.finances.nextBillDueDay + 7,
            creditScore = state.finances.creditScore - if (shortfall > 0) 8 else 0,
        ).normalized()
        val updated = state.copy(finances = finances)
        val detail = if (shortfall > 0) {
            "Weekly costs were ${state.finances.weeklyLivingCost} USD; $shortfall went onto debt."
        } else {
            "You paid ${state.finances.weeklyLivingCost} USD for rent, food, transport, and basics."
        }
        return BillResult(
            state = updated,
            message = if (shortfall > 0) "Weekly bill created $shortfall debt." else "Weekly bill paid.",
            history = HistoryEntry(
                day = state.day,
                title = "Weekly living costs",
                detail = detail,
                kind = HistoryKind.FINANCE,
            ),
        )
    }

    private fun rollEvent(state: GameState): EventRoll {
        val nextSeed = nextSeed(state.rngSeed)
        val candidates = events.filter { it.condition(state) }
        val shouldTrigger = candidates.isNotEmpty() && positiveModulo(nextSeed, 100) < EVENT_CHANCE_PERCENT
        val event = if (shouldTrigger) {
            candidates[positiveModulo(nextSeed / 100, candidates.size)]
        } else {
            null
        }
        return EventRoll(event = event, nextSeed = nextSeed)
    }

    private fun prepareNewDay(state: GameState): NewDayResult {
        val recommendation = focusRecommendationFor(state)
        val planned = state.copy(
            opportunityCooldowns = state.opportunityCooldowns.filterValues { state.day <= it },
            dayPlan = DayPlanState(
                day = state.day,
                recommendedFocus = recommendation.focus,
                activeFocus = recommendation.focus,
                reason = recommendation.reason,
                locked = false,
                actionsTaken = 0,
                focusActionsCompleted = 0,
                categoriesCompleted = emptySet(),
            ),
        )
        return maybeGenerateOpportunity(planned)
    }

    private fun emptyDayPlan(day: Int): DayPlanState = DayPlanState(
        day = day,
        recommendedFocus = DailyFocus.BALANCED,
        activeFocus = DailyFocus.BALANCED,
        reason = "Start with a balanced baseline.",
        locked = false,
        actionsTaken = 0,
        focusActionsCompleted = 0,
        categoriesCompleted = emptySet(),
    )

    private fun focusRecommendationFor(state: GameState): FocusRecommendation {
        val daysUntilBill = (state.finances.nextBillDueDay - state.day).coerceAtLeast(0)
        return when {
            state.finances.cash < state.finances.weeklyLivingCost ||
                (daysUntilBill <= 2 && state.finances.cash < state.finances.weeklyLivingCost + 100) ->
                FocusRecommendation(DailyFocus.MONEY, "Bills and cash runway are the sharpest pressure today.")
            state.stats.stress >= 70 || state.stats.energy <= 30 || state.stats.health <= 45 ->
                FocusRecommendation(DailyFocus.RECOVERY, "Your body is carrying too much pressure.")
            state.career.promotionReadiness >= 75 ->
                FocusRecommendation(DailyFocus.CAREER, "Promotion is close enough to justify a focused push.")
            state.relationships.average <= 45 || state.stats.social <= 35 ->
                FocusRecommendation(DailyFocus.SOCIAL, "Relationships are fading into a risk.")
            else -> FocusRecommendation(DailyFocus.BALANCED, "No single pressure dominates, so a balanced day builds momentum.")
        }
    }

    private fun focusMatches(
        focus: DailyFocus,
        action: DailyActionDefinition,
        plan: DayPlanState,
    ): Boolean = when (focus) {
        DailyFocus.MONEY -> action.category == ActionCategory.MONEY || "income" in action.tags
        DailyFocus.CAREER -> action.effect.careerXpDelta > 0 ||
            action.effect.promotionReadinessDelta > 0 ||
            action.effect.reputationDelta > 0 ||
            action.id in setOf("apply_jobs", "study_course", "networking")
        DailyFocus.RECOVERY -> action.category == ActionCategory.WELLBEING
        DailyFocus.SOCIAL -> action.category == ActionCategory.SOCIAL
        DailyFocus.BALANCED -> action.category !in plan.categoriesCompleted
    }

    private fun focusBonusFor(state: GameState, action: DailyActionDefinition): ActionEffect {
        if (!focusMatches(state.dayPlan.activeFocus, action, state.dayPlan)) return ActionEffect()
        return when (state.dayPlan.activeFocus) {
            DailyFocus.MONEY -> ActionEffect(
                cashDelta = if (state.finances.debt <= 0 || action.category != ActionCategory.MONEY) 15 else 0,
                debtDelta = if (state.finances.debt > 0 && action.category == ActionCategory.MONEY) -15 else 0,
                stressDelta = if (action.category == ActionCategory.WORK) 2 else 0,
            )
            DailyFocus.CAREER -> ActionEffect(
                careerXpDelta = 4,
                promotionReadinessDelta = 4,
                stressDelta = 2,
            )
            DailyFocus.RECOVERY -> ActionEffect(
                healthDelta = 3,
                moodDelta = 4,
                stressDelta = -5,
            )
            DailyFocus.SOCIAL -> ActionEffect(
                moodDelta = 4,
                socialDelta = 3,
                communicationDelta = 3,
                familyDelta = if (action.effect.familyDelta > 0) 3 else 0,
                friendsDelta = if (action.effect.friendsDelta > 0) 3 else 0,
                networkDelta = if (action.effect.networkDelta > 0) 3 else 0,
            )
            DailyFocus.BALANCED -> ActionEffect()
        }
    }

    private fun GameState.trackActionInPlan(action: DailyActionDefinition): GameState {
        val matchedFocus = focusMatches(dayPlan.activeFocus, action, dayPlan)
        return copy(
            dayPlan = dayPlan.copy(
                locked = true,
                actionsTaken = dayPlan.actionsTaken + 1,
                focusActionsCompleted = dayPlan.focusActionsCompleted + if (matchedFocus) 1 else 0,
                categoriesCompleted = dayPlan.categoriesCompleted + action.category,
            ),
        )
    }

    private fun applyFocusEndOfDay(state: GameState): FocusOutcome {
        val plan = state.dayPlan
        val completed = when (plan.activeFocus) {
            DailyFocus.BALANCED -> plan.categoriesCompleted.size >= 3
            else -> plan.focusActionsCompleted > 0
        }

        return when {
            completed -> {
                val effect = focusRewardFor(plan.activeFocus)
                FocusOutcome(
                    state = applyEffect(state, effect),
                    messages = listOf("${plan.activeFocus.label} focus completed."),
                    entries = listOf(
                        HistoryEntry(
                            day = state.day,
                            title = "Daily focus completed: ${plan.activeFocus.label}",
                            detail = "The day plan paid off with a small stability reward.",
                            kind = HistoryKind.GOAL,
                        ),
                    ),
                )
            }
            plan.activeFocus != DailyFocus.BALANCED -> FocusOutcome(
                state = applyEffect(state, ActionEffect(moodDelta = -1, stressDelta = 3)),
                messages = listOf("${plan.activeFocus.label} focus was missed."),
                entries = listOf(
                    HistoryEntry(
                        day = state.day,
                        title = "Daily focus missed: ${plan.activeFocus.label}",
                        detail = "A little pressure carried into the next morning.",
                        kind = HistoryKind.EVENT,
                    ),
                ),
            )
            else -> FocusOutcome(state = state)
        }
    }

    private fun focusRewardFor(focus: DailyFocus): ActionEffect = when (focus) {
        DailyFocus.MONEY -> ActionEffect(cashDelta = 25, stressDelta = -3, creditScoreDelta = 2)
        DailyFocus.CAREER -> ActionEffect(promotionReadinessDelta = 5, reputationDelta = 2, moodDelta = 2)
        DailyFocus.RECOVERY -> ActionEffect(healthDelta = 4, moodDelta = 3, stressDelta = -7)
        DailyFocus.SOCIAL -> ActionEffect(moodDelta = 4, stressDelta = -3, communicationDelta = 2)
        DailyFocus.BALANCED -> ActionEffect(moodDelta = 4, stressDelta = -4, healthDelta = 2)
    }

    private fun quickActionIdsFor(state: GameState): List<String> = buildList {
        state.timedOpportunities.forEach { addAll(recommendedActionIdsForOpportunity(it.id)) }
        when (state.dayPlan.activeFocus) {
            DailyFocus.MONEY -> addAll(listOf("budget_review", "work_shift", "freelance_gig"))
            DailyFocus.CAREER -> addAll(listOf("apply_jobs", "study_course", "networking", "manager_check_in"))
            DailyFocus.RECOVERY -> addAll(listOf("rest", "exercise", "cook_at_home"))
            DailyFocus.SOCIAL -> addAll(listOf("call_family", "socialize", "networking"))
            DailyFocus.BALANCED -> addAll(listOf("work_shift", "study_course", "exercise", "call_family"))
        }
        if (state.finances.cash < state.finances.weeklyLivingCost) add("work_shift")
        if (state.finances.debt > 0) add("budget_review")
        if (state.stats.stress >= 65 || state.stats.energy <= 35) add("rest")
        if (state.relationships.average <= 45) add("call_family")
    }
        .distinct()
        .filter { id -> availableActionsFor(state).any { it.id == id && unavailableReason(state, it) == null } }
        .take(4)

    private fun recommendationReasonFor(
        state: GameState,
        action: DailyActionDefinition,
        focusMatch: Boolean,
    ): String? {
        val opportunity = state.timedOpportunities.firstOrNull { action.id in recommendedActionIdsForOpportunity(it.id) }
        if (opportunity != null) return "Helps ${opportunityTitle(opportunity.id)}"
        if (focusMatch) {
            return if (state.dayPlan.activeFocus == DailyFocus.BALANCED) {
                "Adds balance"
            } else {
                "${state.dayPlan.activeFocus.label} focus"
            }
        }
        return when {
            action.id == "work_shift" && state.finances.cash < state.finances.weeklyLivingCost -> "Covers bills"
            action.id == "rest" && state.stats.stress >= 65 -> "Reduces burnout risk"
            action.id == "call_family" && state.relationships.average <= 45 -> "Repairs social pressure"
            else -> null
        }
    }

    private fun pressureSummaryFor(state: GameState): String = when {
        state.finances.cash < state.finances.weeklyLivingCost -> "Cash is below the next bill; money actions matter today."
        state.stats.stress >= 75 -> "Stress is in burnout range; recovery is urgent."
        state.finances.debt > 0 && (state.finances.nextBillDueDay - state.day).coerceAtLeast(0) <= 2 ->
            "Bills and debt are stacking pressure."
        state.career.promotionReadiness >= 80 -> "Promotion is close; career actions can finish the push."
        state.relationships.average <= 40 -> "Relationships are fading; social actions can stabilize mood."
        else -> "No single crisis dominates; follow the plan to build momentum."
    }

    private fun maybeGenerateOpportunity(state: GameState): NewDayResult {
        if (state.timedOpportunities.size >= MAX_ACTIVE_OPPORTUNITIES) return NewDayResult(state = state)
        val activeIds = state.timedOpportunities.map { it.id }.toSet()
        val candidateId = OPPORTUNITY_PRIORITY.firstOrNull { id ->
            id !in activeIds &&
                state.day > (state.opportunityCooldowns[id] ?: 0) &&
                shouldOfferOpportunity(id, state)
        } ?: return NewDayResult(state = state)

        val opportunity = createOpportunity(candidateId, state)
        return NewDayResult(
            state = state.copy(timedOpportunities = state.timedOpportunities + opportunity),
            newOpportunityTitle = opportunityTitle(candidateId),
        )
    }

    private fun shouldOfferOpportunity(id: String, state: GameState): Boolean {
        val daysUntilBill = (state.finances.nextBillDueDay - state.day).coerceAtLeast(0)
        return when (id) {
            BILL_BUFFER_ID -> state.finances.cash < state.finances.weeklyLivingCost + 100 && daysUntilBill <= 4
            RECOVERY_WINDOW_ID -> state.stats.stress >= 65 || state.stats.energy <= 35 || state.stats.health <= 50
            PROMOTION_PUSH_ID -> state.career.promotionReadiness in 70..99
            RECONNECT_ID -> state.relationships.average <= 50 || state.stats.social <= 40
            DEBT_BRAKE_ID -> state.finances.debt >= 90
            else -> false
        }
    }

    private fun createOpportunity(id: String, state: GameState): TimedOpportunityState = when (id) {
        BILL_BUFFER_ID -> TimedOpportunityState(
            id = id,
            progress = state.finances.cash.coerceAtMost(state.finances.weeklyLivingCost + 100),
            target = state.finances.weeklyLivingCost + 100,
            baseline = state.finances.cash,
            expiresOnDay = state.day + 3,
        )
        RECOVERY_WINDOW_ID -> TimedOpportunityState(
            id = id,
            progress = 0,
            target = (state.stats.stress - 50).coerceAtLeast(1),
            baseline = state.stats.stress,
            expiresOnDay = state.day + 2,
        )
        PROMOTION_PUSH_ID -> TimedOpportunityState(
            id = id,
            progress = state.career.promotionReadiness.coerceAtMost(100),
            target = 100,
            baseline = state.career.promotionReadiness,
            expiresOnDay = state.day + 3,
        )
        RECONNECT_ID -> TimedOpportunityState(
            id = id,
            progress = 0,
            target = 2,
            baseline = state.relationships.average,
            expiresOnDay = state.day + 3,
        )
        DEBT_BRAKE_ID -> TimedOpportunityState(
            id = id,
            progress = 0,
            target = 90,
            baseline = state.finances.debt,
            expiresOnDay = state.day + 4,
        )
        else -> error("Unknown opportunity: $id")
    }

    private fun refreshOpportunityProgress(
        state: GameState,
        action: DailyActionDefinition?,
    ): GameState = state.copy(
        timedOpportunities = state.timedOpportunities.map { opportunity ->
            when (opportunity.id) {
                BILL_BUFFER_ID -> opportunity.copy(progress = state.finances.cash.coerceIn(0, opportunity.target))
                RECOVERY_WINDOW_ID -> opportunity.copy(progress = (opportunity.baseline - state.stats.stress).coerceIn(0, opportunity.target))
                PROMOTION_PUSH_ID -> opportunity.copy(progress = state.career.promotionReadiness.coerceIn(0, opportunity.target))
                RECONNECT_ID -> {
                    val actionProgress = if (action?.category == ActionCategory.SOCIAL) 1 else 0
                    val progress = if (state.relationships.average >= 60) {
                        opportunity.target
                    } else {
                        (opportunity.progress + actionProgress).coerceIn(0, opportunity.target)
                    }
                    opportunity.copy(progress = progress)
                }
                DEBT_BRAKE_ID -> opportunity.copy(progress = (opportunity.baseline - state.finances.debt).coerceIn(0, opportunity.target))
                else -> opportunity
            }
        },
    )

    private fun resolveOpportunities(
        state: GameState,
        failExpired: Boolean,
    ): OpportunityUpdate {
        var updated = state
        val remaining = mutableListOf<TimedOpportunityState>()
        val messages = mutableListOf<String>()
        val entries = mutableListOf<HistoryEntry>()
        val cooldowns = updated.opportunityCooldowns.toMutableMap()

        updated.timedOpportunities.forEach { opportunity ->
            when {
                opportunityIsComplete(opportunity, updated) -> {
                    updated = applyOpportunityReward(updated, opportunity.id)
                    cooldowns[opportunity.id] = updated.day + OPPORTUNITY_COOLDOWN_DAYS
                    val title = opportunityTitle(opportunity.id)
                    messages += "Opportunity completed: $title."
                    entries += HistoryEntry(
                        day = updated.day,
                        title = "Opportunity completed: $title",
                        detail = opportunityRewardText(opportunity.id),
                        kind = HistoryKind.GOAL,
                    )
                }
                failExpired && updated.day >= opportunity.expiresOnDay -> {
                    updated = applyOpportunityFailure(updated, opportunity.id)
                    cooldowns[opportunity.id] = updated.day + OPPORTUNITY_COOLDOWN_DAYS
                    val title = opportunityTitle(opportunity.id)
                    messages += "Opportunity expired: $title."
                    entries += HistoryEntry(
                        day = updated.day,
                        title = "Opportunity expired: $title",
                        detail = opportunityFailureText(opportunity.id),
                        kind = HistoryKind.EVENT,
                    )
                }
                else -> remaining += opportunity
            }
        }

        return OpportunityUpdate(
            state = updated.copy(
                timedOpportunities = remaining,
                opportunityCooldowns = cooldowns,
            ),
            messages = messages,
            entries = entries,
        )
    }

    private fun opportunityIsComplete(opportunity: TimedOpportunityState, state: GameState): Boolean = when (opportunity.id) {
        BILL_BUFFER_ID -> state.finances.cash >= opportunity.target
        RECOVERY_WINDOW_ID -> state.stats.stress <= 50
        PROMOTION_PUSH_ID -> state.career.promotionReadiness >= 100
        RECONNECT_ID -> opportunity.progress >= opportunity.target || state.relationships.average >= 60
        DEBT_BRAKE_ID -> opportunity.progress >= opportunity.target
        else -> opportunity.progress >= opportunity.target
    }

    private fun applyOpportunityReward(state: GameState, id: String): GameState = when (id) {
        BILL_BUFFER_ID -> applyEffect(state, ActionEffect(stressDelta = -5, creditScoreDelta = 3, moodDelta = 3))
        RECOVERY_WINDOW_ID -> applyEffect(state, ActionEffect(healthDelta = 4, moodDelta = 4))
            .copy(modifiers = state.modifiers.filterNot { it.id == BURNOUT_RISK_ID })
        PROMOTION_PUSH_ID -> applyEffect(state, ActionEffect(reputationDelta = 3, cashDelta = 50, moodDelta = 4))
        RECONNECT_ID -> applyEffect(state, ActionEffect(moodDelta = 5, communicationDelta = 3, stressDelta = -3))
        DEBT_BRAKE_ID -> applyEffect(state, ActionEffect(creditScoreDelta = 6, stressDelta = -5))
        else -> state
    }

    private fun applyOpportunityFailure(state: GameState, id: String): GameState = when (id) {
        BILL_BUFFER_ID -> applyEffect(state, ActionEffect(stressDelta = 4))
        RECOVERY_WINDOW_ID -> if (state.stats.stress > 50 && state.modifiers.none { it.id == BURNOUT_RISK_ID }) {
            state.copy(modifiers = state.modifiers + burnoutRiskModifier())
        } else {
            state
        }
        PROMOTION_PUSH_ID -> applyEffect(state, ActionEffect(stressDelta = 2, moodDelta = -1))
        RECONNECT_ID -> applyEffect(state, ActionEffect(friendsDelta = -2, moodDelta = -2))
        DEBT_BRAKE_ID -> applyEffect(state, ActionEffect(creditScoreDelta = -2, stressDelta = 3))
        else -> state
    }

    private fun opportunityProgressPreview(state: GameState, action: DailyActionDefinition): Int =
        state.timedOpportunities.map { opportunity ->
            when (opportunity.id) {
                RECONNECT_ID -> if (action.category == ActionCategory.SOCIAL && opportunity.progress < opportunity.target) 1 else 0
                else -> 0
            }
        }.sum()

    private fun recommendedActionIdsForOpportunity(id: String): List<String> = when (id) {
        BILL_BUFFER_ID -> listOf("budget_review", "work_shift", "overtime", "freelance_gig", "pitch_client")
        RECOVERY_WINDOW_ID -> listOf("rest", "exercise", "cook_at_home")
        PROMOTION_PUSH_ID -> listOf("apply_jobs", "study_course", "networking", "manager_check_in", "exam_prep", "work_shift")
        RECONNECT_ID -> listOf("socialize", "call_family", "networking")
        DEBT_BRAKE_ID -> listOf("budget_review", "work_shift", "overtime", "freelance_gig")
        else -> emptyList()
    }

    private fun opportunityTitle(id: String): String = when (id) {
        BILL_BUFFER_ID -> "Bill Buffer"
        RECOVERY_WINDOW_ID -> "Recovery Window"
        PROMOTION_PUSH_ID -> "Promotion Push"
        RECONNECT_ID -> "Reconnect"
        DEBT_BRAKE_ID -> "Debt Brake"
        else -> id
    }

    private fun opportunityRewardText(id: String): String = when (id) {
        BILL_BUFFER_ID -> "The bill buffer lowers stress and improves credit confidence."
        RECOVERY_WINDOW_ID -> "Recovery stabilizes your health and clears burnout pressure."
        PROMOTION_PUSH_ID -> "The career push turns into reputation, cash, and mood."
        RECONNECT_ID -> "Reconnection improves mood, communication, and stress."
        DEBT_BRAKE_ID -> "Debt progress improves credit and relieves pressure."
        else -> "The timed opportunity paid off."
    }

    private fun opportunityFailureText(id: String): String = when (id) {
        BILL_BUFFER_ID -> "The bill still feels too close for comfort."
        RECOVERY_WINDOW_ID -> "Unresolved stress keeps burnout risk alive."
        PROMOTION_PUSH_ID -> "The missed career window adds a little pressure."
        RECONNECT_ID -> "Letting the silence continue costs a little closeness."
        DEBT_BRAKE_ID -> "Debt pressure keeps weighing on credit."
        else -> "The timed opportunity slipped away."
    }

    private fun buildActionDeltas(
        action: DailyActionDefinition,
        effect: ActionEffect,
        opportunityProgressDelta: Int,
    ): List<ActionDelta> = buildList {
        add(ActionDelta("Time", -action.timeCost))
        val energy = effect.energyDelta - action.energyCost
        if (energy != 0) add(ActionDelta("Energy", energy))
        val cash = effect.cashDelta - action.moneyCost
        if (cash != 0) add(ActionDelta("Cash", cash))
        if (effect.debtDelta != 0) add(ActionDelta("Debt", effect.debtDelta, positiveIsGood = false))
        if (effect.stressDelta != 0) add(ActionDelta("Stress", effect.stressDelta, positiveIsGood = false))
        if (effect.moodDelta != 0) add(ActionDelta("Mood", effect.moodDelta))
        if (effect.healthDelta != 0) add(ActionDelta("Health", effect.healthDelta))
        if (effect.promotionReadinessDelta != 0) add(ActionDelta("Promotion", effect.promotionReadinessDelta))
        if (effect.reputationDelta != 0) add(ActionDelta("Reputation", effect.reputationDelta))
        val relationshipDelta = effect.socialDelta + effect.familyDelta + effect.friendsDelta + effect.networkDelta
        if (relationshipDelta != 0) add(ActionDelta("Social", relationshipDelta))
        if (opportunityProgressDelta != 0) add(ActionDelta("Quest", opportunityProgressDelta))
    }.take(8)

    private fun summarizeAction(action: DailyActionDefinition, effect: ActionEffect): String {
        val gains = buildList {
            if (effect.cashDelta > 0) add("+${effect.cashDelta} cash")
            if (effect.debtDelta < 0) add("${effect.debtDelta} debt")
            if (effect.careerXpDelta > 0) add("+${effect.careerXpDelta} career XP")
            if (effect.promotionReadinessDelta > 0) add("+${effect.promotionReadinessDelta} promotion")
            if (effect.healthDelta > 0) add("+${effect.healthDelta} health")
            if (effect.moodDelta > 0) add("+${effect.moodDelta} mood")
            if (effect.stressDelta < 0) add("${effect.stressDelta} stress")
        }
        val cost = "Time -${action.timeCost}, energy -${action.energyCost}" +
            if (action.moneyCost > 0) ", cash -${action.moneyCost}" else ""
        return if (gains.isEmpty()) cost else "$cost. ${gains.joinToString(", ")}."
    }

    private fun startingPreset(archetype: LifeArchetype): StartingPreset = when (archetype) {
        LifeArchetype.STUDENT -> StartingPreset(
            stats = CoreStats(health = 72, mood = 64, energy = 86, stress = 32, social = 58),
            skills = SkillSet(knowledge = 22, fitness = 8, career = 4, communication = 12, creativity = 10),
            finances = FinanceState(cash = 240, debt = 120, weeklyLivingCost = 140, nextBillDueDay = 7, creditScore = 650),
            career = CareerState("Part-time student", level = 1, xp = 4, reputation = 12, promotionReadiness = 12, salaryPerShift = 85),
            relationships = RelationshipState(family = 64, friends = 58, network = 34),
        )
        LifeArchetype.JUNIOR_WORKER -> StartingPreset(
            stats = CoreStats(health = 68, mood = 60, energy = 78, stress = 42, social = 48),
            skills = SkillSet(knowledge = 14, fitness = 9, career = 24, communication = 16, creativity = 8),
            finances = FinanceState(cash = 540, debt = 260, weeklyLivingCost = 215, nextBillDueDay = 7, creditScore = 665),
            career = CareerState("Junior associate", level = 1, xp = 24, reputation = 26, promotionReadiness = 28, salaryPerShift = 115),
            relationships = RelationshipState(family = 54, friends = 48, network = 44),
        )
        LifeArchetype.FREELANCER -> StartingPreset(
            stats = CoreStats(health = 66, mood = 62, energy = 82, stress = 48, social = 42),
            skills = SkillSet(knowledge = 16, fitness = 7, career = 18, communication = 18, creativity = 22),
            finances = FinanceState(cash = 380, debt = 180, weeklyLivingCost = 185, nextBillDueDay = 7, creditScore = 640),
            career = CareerState("Independent freelancer", level = 1, xp = 18, reputation = 22, promotionReadiness = 20, salaryPerShift = 95),
            relationships = RelationshipState(family = 48, friends = 44, network = 50),
        )
    }

    private fun defaultGoals(cash: Int, promotion: Int, relationships: Int): List<GoalState> = listOf(
        GoalState(
            id = "emergency_buffer",
            title = "Build a 1k buffer",
            description = "Keep enough cash to survive surprises without spiraling into debt.",
            category = GoalCategory.FINANCE,
            progress = cash.coerceAtMost(1_000),
            target = 1_000,
            rewardText = "Financial confidence improves your mood and credit trajectory.",
        ),
        GoalState(
            id = "first_promotion",
            title = "Earn your next step",
            description = "Build promotion readiness through work, learning, and networking.",
            category = GoalCategory.CAREER,
            progress = promotion.coerceAtMost(100),
            target = 100,
            rewardText = "A promotion unlocks higher income and career confidence.",
        ),
        GoalState(
            id = "stable_week",
            title = "Stabilize the week",
            description = "Stack healthy choices that keep stress from taking over.",
            category = GoalCategory.WELLBEING,
            progress = 0,
            target = 7,
            rewardText = "A stable routine improves health, mood, and stress.",
        ),
        GoalState(
            id = "social_circle",
            title = "Repair your circle",
            description = "Invest in family, friends, and network until life feels less isolated.",
            category = GoalCategory.SOCIAL,
            progress = relationships.coerceAtMost(100),
            target = 100,
            rewardText = "A stronger circle makes pressure easier to carry.",
        ),
    )

    private fun statusFor(state: GameState): String = when {
        state.finances.debt >= 700 -> "Debt pressure"
        state.stats.health <= 35 -> "Fragile"
        state.stats.stress >= 82 -> "Burnout risk"
        state.finances.cash < state.finances.weeklyLivingCost -> "Cash tight"
        state.career.promotionReadiness >= 80 -> "Breakthrough close"
        state.stats.mood >= 75 && state.stats.health >= 65 -> "Thriving"
        else -> "Stable"
    }

    private fun jobTitleFor(archetype: LifeArchetype, careerLevel: Int): String = when (archetype) {
        LifeArchetype.STUDENT -> when {
            careerLevel >= 4 -> "Junior analyst"
            careerLevel >= 3 -> "Student coordinator"
            careerLevel >= 2 -> "Student intern"
            else -> "Part-time student"
        }
        LifeArchetype.JUNIOR_WORKER -> when {
            careerLevel >= 4 -> "Senior associate"
            careerLevel >= 3 -> "Project associate"
            careerLevel >= 2 -> "Associate"
            else -> "Junior associate"
        }
        LifeArchetype.FREELANCER -> when {
            careerLevel >= 4 -> "Specialist freelancer"
            careerLevel >= 3 -> "Trusted contractor"
            careerLevel >= 2 -> "Reliable freelancer"
            else -> "Independent freelancer"
        }
    }

    private fun salaryFor(archetype: LifeArchetype, careerLevel: Int): Int {
        val base = when (archetype) {
            LifeArchetype.STUDENT -> 85
            LifeArchetype.JUNIOR_WORKER -> 115
            LifeArchetype.FREELANCER -> 95
        }
        return base + ((careerLevel - 1).coerceAtLeast(0) * 35)
    }

    private fun seedFor(archetype: LifeArchetype): Long = when (archetype) {
        LifeArchetype.STUDENT -> 10_001L
        LifeArchetype.JUNIOR_WORKER -> 20_001L
        LifeArchetype.FREELANCER -> 30_001L
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
        careerXpDelta = careerXpDelta + other.careerXpDelta,
        communicationDelta = communicationDelta + other.communicationDelta,
        creativityDelta = creativityDelta + other.creativityDelta,
        reputationDelta = reputationDelta + other.reputationDelta,
        promotionReadinessDelta = promotionReadinessDelta + other.promotionReadinessDelta,
        familyDelta = familyDelta + other.familyDelta,
        friendsDelta = friendsDelta + other.friendsDelta,
        networkDelta = networkDelta + other.networkDelta,
        goalProgress = (goalProgress.keys + other.goalProgress.keys)
            .associateWith { key -> (goalProgress[key] ?: 0) + (other.goalProgress[key] ?: 0) }
            .filterValues { it != 0 },
        modifier = other.modifier ?: modifier,
    )

    private fun ActionEffect.hasAnyImpact(): Boolean =
        this != ActionEffect()

    private data class StartingPreset(
        val stats: CoreStats,
        val skills: SkillSet,
        val finances: FinanceState,
        val career: CareerState,
        val relationships: RelationshipState,
    )

    private data class BillResult(
        val state: GameState,
        val message: String? = null,
        val history: HistoryEntry? = null,
    )

    private data class EventRoll(
        val event: LifeEventDefinition?,
        val nextSeed: Long,
    )

    private data class FocusRecommendation(
        val focus: DailyFocus,
        val reason: String,
    )

    private data class FocusOutcome(
        val state: GameState,
        val messages: List<String> = emptyList(),
        val entries: List<HistoryEntry> = emptyList(),
    )

    private data class OpportunityUpdate(
        val state: GameState,
        val messages: List<String>,
        val entries: List<HistoryEntry>,
    )

    private data class NewDayResult(
        val state: GameState,
        val newOpportunityTitle: String? = null,
    )

    companion object {
        const val DAILY_TIME_BUDGET = 12
        private const val EVENT_CHANCE_PERCENT = 35
        private const val HISTORY_LIMIT = 100
        private const val MAX_ACTIVE_OPPORTUNITIES = 2
        private const val OPPORTUNITY_COOLDOWN_DAYS = 7
        private const val BILL_BUFFER_ID = "bill_buffer"
        private const val RECOVERY_WINDOW_ID = "recovery_window"
        private const val PROMOTION_PUSH_ID = "promotion_push"
        private const val RECONNECT_ID = "reconnect"
        private const val DEBT_BRAKE_ID = "debt_brake"
        private const val BURNOUT_RISK_ID = "burnout_risk"
        private val OPPORTUNITY_PRIORITY = listOf(
            BILL_BUFFER_ID,
            RECOVERY_WINDOW_ID,
            PROMOTION_PUSH_ID,
            RECONNECT_ID,
            DEBT_BRAKE_ID,
        )
    }
}
