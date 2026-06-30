package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionCategory
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.BusinessStage
import com.azizjonkasimov.lifesimulator.domain.model.BusinessState
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition
import com.azizjonkasimov.lifesimulator.domain.model.DailyFocus
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
import com.azizjonkasimov.lifesimulator.domain.model.DayPlanState
import com.azizjonkasimov.lifesimulator.domain.model.FinanceState
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind
import com.azizjonkasimov.lifesimulator.domain.model.JobSearchState
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
    fun startNewLife(): GameState {
        val finances = FinanceState(
            cash = 180,
            debt = 350,
            weeklyLivingCost = 190,
            nextBillDueDay = 7,
            creditScore = 635,
        )
        val base = GameState(
            profile = LifeProfile(name = "Alex Rivers", age = 22),
            calendar = CalendarState(day = 1, timeRemaining = DAILY_TIME_BUDGET),
            stats = CoreStats(health = 66, mood = 54, energy = 76, stress = 52, social = 45),
            skills = SkillSet(knowledge = 14, fitness = 8, career = 6, communication = 12, creativity = 12),
            finances = finances,
            career = CareerState(
                title = "Unemployed",
                level = 0,
                xp = 0,
                reputation = 8,
                promotionReadiness = 0,
                salaryPerShift = 0,
                employed = false,
            ),
            jobSearch = JobSearchState(applicationsSent = 0, interviewReadiness = 10, offerProgress = 0),
            business = BusinessState(
                stage = BusinessStage.IDEA,
                leads = 0,
                activeProjects = 0,
                completedProjects = 0,
                clientTrust = 10,
                reputation = 0,
                pipelineValue = 25,
            ),
            relationships = RelationshipState(family = 50, friends = 42, network = 32),
            modifiers = emptyList(),
            dayPlan = emptyDayPlan(day = 1),
            timedOpportunities = emptyList(),
            opportunityCooldowns = emptyMap(),
            rngSeed = STARTING_SEED,
            history = listOf(
                HistoryEntry(
                    day = 1,
                    title = "New life started",
                    detail = "You began unemployed with bills coming due. Build income through job search, steady work, and a client pipeline.",
                    kind = HistoryKind.SYSTEM,
                ),
            ),
        )
        return prepareNewDay(base).state
    }

    fun dashboardSnapshot(state: GameState): DashboardSnapshot {
        val totalBill = totalWeeklyCost(state)
        val daysUntilBill = (state.finances.nextBillDueDay - state.day).coerceAtLeast(0)
        val alerts = buildList {
            if (!state.career.employed) {
                add("Steady work is not locked in yet.")
            }
            if (state.business.activeProjects > 0) {
                add("Client work is waiting to be completed.")
            }
            if (state.finances.cash < totalBill) {
                add("Cash is below the next weekly bill.")
            }
            if (state.finances.debt > 0) {
                add("Debt is adding pressure each night.")
            }
            if (state.stats.stress >= 75) {
                add("Stress is near burnout range.")
            }
            if (state.career.employed && state.career.promotionReadiness >= 80) {
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
                "Due today: ${totalBill} USD"
            } else {
                "In $daysUntilBill days: ${totalBill} USD"
            },
            netWorth = state.finances.netWorth,
        )
    }

    fun actionAvailability(state: GameState): List<ActionAvailability> =
        availableActionsFor(state).map { action ->
            val reason = unavailableReason(state, action)
            val focusMatch = focusMatches(state.dayPlan.activeFocus, action, state)
            val effect = dynamicEffectFor(state, action) + focusBonusFor(state, action)
            ActionAvailability(
                action = action,
                isAvailable = reason == null,
                reason = reason,
                previewDeltas = buildActionDeltas(
                    action = action,
                    effect = effect,
                    opportunityProgressDelta = opportunityProgressPreview(state, action, effect),
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

        val baseEffect = dynamicEffectFor(state, action)
        val focusBonus = focusBonusFor(state, action)
        val effect = baseEffect + focusBonus
        val actionDeltas = buildActionDeltas(
            action = action,
            effect = effect,
            opportunityProgressDelta = opportunityProgressPreview(state, action, effect),
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
        val transitions = applyActionTransitions(before = state, state = applied, action = action)
        val opportunityProgressed = refreshOpportunityProgress(transitions.state, action)
        val opportunityUpdate = resolveOpportunities(opportunityProgressed, failExpired = false)
        val afterOpportunity = opportunityUpdate.state
            .settlePromotion()
        val updated = afterOpportunity.copy(
            history = buildList {
                addAll(afterOpportunity.history)
                add(
                    HistoryEntry(
                        day = afterOpportunity.day,
                        title = action.title,
                        detail = summarizeAction(action, effect),
                        kind = HistoryKind.ACTION,
                    ),
                )
                addAll(transitions.entries)
                addAll(opportunityUpdate.entries)
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
                addAll(transitions.messages)
                addAll(opportunityUpdate.messages)
            },
            actionDeltas = actionDeltas,
        )
    }

    fun advanceDay(state: GameState): SimulationResult {
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
        )

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
        val opportunityProgressed = refreshOpportunityProgress(focusOutcome.state, action = null)
        val opportunityUpdate = resolveOpportunities(opportunityProgressed, failExpired = true)
        val afterOpportunity = opportunityUpdate.state
            .settlePromotion()
        val nextDayBase = afterOpportunity.copy(
            calendar = CalendarState(day = afterOpportunity.day + 1, timeRemaining = DAILY_TIME_BUDGET),
            rngSeed = eventRoll.nextSeed,
            history = buildList {
                addAll(afterOpportunity.history)
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
                    detail = "A timed pressure opportunity is now active.",
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
                newDay.newOpportunityTitle?.let { add("New opportunity: $it.") }
                add("Day ${state.day + 1} begins.")
            },
        )
    }

    private fun availableActionsFor(state: GameState): List<DailyActionDefinition> = actions

    private fun unavailableReason(state: GameState, action: DailyActionDefinition): String? = when {
        action.id in EMPLOYED_ACTION_IDS && !state.career.employed -> "You need a steady job first."
        action.id == "attend_interview" && state.career.employed -> "You already have a steady job."
        action.id == "attend_interview" && state.jobSearch.applicationsSent <= 0 -> "Send applications first."
        action.id == "attend_interview" && state.jobSearch.interviewReadiness < 35 -> "Interview readiness needs 35%."
        action.id == "attend_interview" && state.jobSearch.offerProgress < 55 -> "Build more employer interest first."
        action.id == "pitch_client" && state.business.leads <= 0 -> "Find leads first."
        action.id == "client_project" && state.business.activeProjects <= 0 -> "Pitch a client first."
        state.timeRemaining < action.timeCost -> "Not enough time left today."
        state.stats.energy < action.energyCost -> "Not enough energy."
        state.finances.cash < action.moneyCost -> "Not enough cash."
        else -> null
    }

    private fun dynamicEffectFor(state: GameState, action: DailyActionDefinition): ActionEffect = when (action.id) {
        "work_shift" -> action.effect.copy(cashDelta = state.career.salaryPerShift)
        "overtime" -> action.effect.copy(cashDelta = (state.career.salaryPerShift * 0.70f).toInt() + 30)
        "attend_interview" -> action.effect.copy(
            offerProgressDelta = 30 +
                (state.jobSearch.interviewReadiness / 5) +
                (state.skills.communication / 6) +
                state.jobSearch.applicationsSent.coerceAtMost(6),
        )
        "client_project" -> action.effect.copy(
            cashDelta = 85 +
                state.business.pipelineValue +
                (state.business.reputation * 2) +
                (state.skills.creativity / 2) +
                (state.skills.communication / 2),
        )
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
        val jobSearch = state.jobSearch.copy(
            applicationsSent = state.jobSearch.applicationsSent + effect.applicationsDelta,
            interviewReadiness = state.jobSearch.interviewReadiness + effect.interviewReadinessDelta,
            offerProgress = state.jobSearch.offerProgress + effect.offerProgressDelta,
        ).normalized()
        val business = state.business.copy(
            leads = state.business.leads + effect.leadsDelta,
            activeProjects = state.business.activeProjects + effect.activeProjectsDelta,
            completedProjects = state.business.completedProjects + effect.completedProjectsDelta,
            clientTrust = state.business.clientTrust + effect.clientTrustDelta,
            reputation = state.business.reputation + effect.businessReputationDelta,
            pipelineValue = state.business.pipelineValue + effect.pipelineValueDelta,
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

    private fun applyActionTransitions(
        before: GameState,
        state: GameState,
        action: DailyActionDefinition,
    ): TransitionUpdate {
        var updated = state
        val messages = mutableListOf<String>()
        val entries = mutableListOf<HistoryEntry>()

        if (action.id == "attend_interview" && !before.career.employed && updated.jobSearch.offerProgress >= 100) {
            updated = updated.copy(
                career = updated.career.copy(
                    employed = true,
                    title = jobTitleFor(1),
                    level = 1,
                    salaryPerShift = salaryFor(1),
                    promotionReadiness = 20,
                    reputation = updated.career.reputation.coerceAtLeast(15),
                ).normalized(),
                jobSearch = updated.jobSearch.copy(offerProgress = 100).normalized(),
            )
            messages += "You landed an entry job."
            entries += HistoryEntry(
                day = updated.day,
                title = "First job landed",
                detail = "The interview turned into an Entry Associate offer at ${salaryFor(1)} USD per shift.",
                kind = HistoryKind.CAREER,
            )
        }

        val nextStage = businessStageFor(updated.business.completedProjects)
        if (nextStage != updated.business.stage) {
            updated = updated.copy(business = updated.business.copy(stage = nextStage))
            messages += "Business upgraded to ${nextStage.label}."
            entries += HistoryEntry(
                day = updated.day,
                title = "Business upgraded",
                detail = "Completed client work moved the business to ${nextStage.label}.",
                kind = HistoryKind.CAREER,
            )
        }

        return TransitionUpdate(
            state = updated,
            messages = messages,
            entries = entries,
        )
    }

    private fun GameState.settlePromotion(): GameState {
        if (!career.employed || career.promotionReadiness < 100) return this
        val newLevel = career.level + 1
        val newTitle = jobTitleFor(newLevel)
        return copy(
            career = career.copy(
                level = newLevel,
                title = newTitle,
                salaryPerShift = salaryFor(newLevel),
                promotionReadiness = 25,
                reputation = (career.reputation + 6).coerceAtMost(100),
            ).normalized(),
            history = (history + HistoryEntry(
                day = day,
                title = "Promotion earned",
                detail = "You advanced to $newTitle. Shifts now pay ${salaryFor(newLevel)} USD.",
                kind = HistoryKind.CAREER,
            )).trimHistory(),
        )
    }

    private fun applyActiveModifiers(state: GameState): GameState {
        val afterEffects = state.modifiers.fold(state) { current, modifier ->
            applyEffect(
                current,
                ActionEffect(
                    healthDelta = modifier.healthDelta,
                    moodDelta = modifier.moodDelta,
                    energyDelta = modifier.energyDelta,
                    stressDelta = modifier.stressDelta,
                ),
            )
        }
        return afterEffects.copy(
            modifiers = afterEffects.modifiers.mapNotNull { modifier ->
                modifier.tick().takeIf { it.daysRemaining > 0 }
            },
        )
    }

    private fun applyBills(state: GameState): BillResult {
        if (state.day != state.finances.nextBillDueDay) return BillResult(state = state)

        val overhead = businessOverheadFor(state.business.stage)
        val totalDue = state.finances.weeklyLivingCost + overhead
        val label = if (overhead > 0) {
            "Weekly bill and business overhead"
        } else {
            "Weekly bill"
        }
        val nextDue = state.finances.nextBillDueDay + 7

        val updatedFinances = if (state.finances.cash >= totalDue) {
            state.finances.copy(
                cash = state.finances.cash - totalDue,
                nextBillDueDay = nextDue,
                creditScore = state.finances.creditScore + 1,
            ).normalized()
        } else {
            val shortfall = totalDue - state.finances.cash
            state.finances.copy(
                cash = 0,
                debt = state.finances.debt + shortfall,
                nextBillDueDay = nextDue,
                creditScore = state.finances.creditScore - 3,
            ).normalized()
        }
        val message = if (state.finances.cash >= totalDue) {
            "$label paid."
        } else {
            "$label created ${totalDue - state.finances.cash} USD debt."
        }
        return BillResult(
            state = state.copy(finances = updatedFinances),
            message = message,
            history = HistoryEntry(
                day = state.day,
                title = label,
                detail = if (overhead > 0) {
                    "Paid ${state.finances.weeklyLivingCost} USD living cost plus $overhead USD business overhead."
                } else {
                    "Handled the ${state.finances.weeklyLivingCost} USD weekly living cost."
                },
                kind = HistoryKind.FINANCE,
            ),
        )
    }

    private fun rollEvent(state: GameState): EventRoll {
        val next = nextSeed(state.rngSeed)
        val eligible = events.filter { it.condition(state) }
        if (eligible.isEmpty() || positiveModulo(next, 100) >= EVENT_CHANCE_PERCENT) {
            return EventRoll(event = null, nextSeed = next)
        }
        val event = eligible[positiveModulo(next / 97L, eligible.size)]
        return EventRoll(event = event, nextSeed = nextSeed(next))
    }

    private fun prepareNewDay(state: GameState): NewDayResult {
        val recommendation = focusRecommendationFor(state)
        val withPlan = state.copy(
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
            opportunityCooldowns = state.opportunityCooldowns.filterValues { cooldownUntil -> cooldownUntil > state.day },
        )
        if (withPlan.timedOpportunities.size >= MAX_ACTIVE_OPPORTUNITIES) {
            return NewDayResult(state = withPlan)
        }
        val newOpportunityId = OPPORTUNITY_PRIORITY.firstOrNull { id ->
            id !in withPlan.timedOpportunities.map { it.id } &&
                (withPlan.opportunityCooldowns[id] ?: 0) <= withPlan.day &&
                shouldOfferOpportunity(withPlan, id)
        } ?: return NewDayResult(state = withPlan)

        val opportunity = createOpportunity(withPlan, newOpportunityId)
        return NewDayResult(
            state = withPlan.copy(timedOpportunities = withPlan.timedOpportunities + opportunity),
            newOpportunityTitle = opportunityTitle(newOpportunityId),
        )
    }

    private fun emptyDayPlan(day: Int): DayPlanState = DayPlanState(
        day = day,
        recommendedFocus = DailyFocus.BALANCED,
        activeFocus = DailyFocus.BALANCED,
        reason = "Start the day and choose what matters most.",
        locked = false,
        actionsTaken = 0,
        focusActionsCompleted = 0,
        categoriesCompleted = emptySet(),
    )

    private fun focusRecommendationFor(state: GameState): FocusRecommendation {
        val totalBill = totalWeeklyCost(state)
        val daysUntilBill = state.finances.nextBillDueDay - state.day
        return when {
            state.finances.cash < totalBill ||
                (daysUntilBill <= 2 && state.finances.cash < totalBill + 75) -> FocusRecommendation(
                focus = DailyFocus.MONEY,
                reason = "Bills are close enough that cash needs priority.",
            )
            state.business.activeProjects > 0 && state.finances.cash < totalBill + 100 -> FocusRecommendation(
                focus = DailyFocus.MONEY,
                reason = "A client project can turn effort into cash quickly.",
            )
            state.stats.stress >= 70 || state.stats.energy <= 30 || state.stats.health <= 45 -> FocusRecommendation(
                focus = DailyFocus.RECOVERY,
                reason = "Your body is close to a stress spiral.",
            )
            !state.career.employed -> FocusRecommendation(
                focus = DailyFocus.CAREER,
                reason = "Landing steady work is the cleanest income upgrade.",
            )
            state.career.promotionReadiness >= 70 -> FocusRecommendation(
                focus = DailyFocus.CAREER,
                reason = "Promotion progress is close enough to push.",
            )
            state.relationships.average <= 42 -> FocusRecommendation(
                focus = DailyFocus.SOCIAL,
                reason = "Your support network is getting thin.",
            )
            else -> FocusRecommendation(
                focus = DailyFocus.BALANCED,
                reason = "No single pressure dominates today.",
            )
        }
    }

    private fun focusMatches(
        focus: DailyFocus,
        action: DailyActionDefinition,
        state: GameState,
    ): Boolean = when (focus) {
        DailyFocus.MONEY -> action.category == ActionCategory.MONEY ||
            action.category == ActionCategory.BUSINESS ||
            "income" in action.tags ||
            action.id in setOf("temp_shift", "work_shift", "overtime", "client_project")
        DailyFocus.CAREER -> if (state.career.employed) {
            action.id in setOf("work_shift", "overtime", "manager_check_in", "study_course", "networking")
        } else {
            action.id in setOf("send_applications", "interview_prep", "attend_interview", "networking", "study_course")
        }
        DailyFocus.RECOVERY -> action.category == ActionCategory.WELLBEING || "recovery" in action.tags || "health" in action.tags
        DailyFocus.SOCIAL -> action.category == ActionCategory.SOCIAL
        DailyFocus.BALANCED -> false
    }

    private fun focusBonusFor(state: GameState, action: DailyActionDefinition): ActionEffect {
        if (!focusMatches(state.dayPlan.activeFocus, action, state)) return ActionEffect()
        return when (state.dayPlan.activeFocus) {
            DailyFocus.MONEY -> if (action.effect.debtDelta < 0 || action.id == "budget_review") {
                ActionEffect(debtDelta = -15, stressDelta = if (action.category == ActionCategory.WORK || action.category == ActionCategory.BUSINESS) 2 else 0)
            } else {
                ActionEffect(cashDelta = 15, stressDelta = if (action.category == ActionCategory.WORK || action.category == ActionCategory.BUSINESS) 2 else 0)
            }
            DailyFocus.CAREER -> if (state.career.employed) {
                ActionEffect(careerXpDelta = 4, promotionReadinessDelta = 4, stressDelta = 2)
            } else {
                ActionEffect(careerXpDelta = 4, offerProgressDelta = 5, stressDelta = 2)
            }
            DailyFocus.RECOVERY -> ActionEffect(healthDelta = 3, moodDelta = 4, stressDelta = -5)
            DailyFocus.SOCIAL -> ActionEffect(
                moodDelta = 4,
                socialDelta = 2,
                communicationDelta = 3,
                familyDelta = 1,
                friendsDelta = 2,
                networkDelta = 2,
            )
            DailyFocus.BALANCED -> ActionEffect()
        }
    }

    private fun GameState.trackActionInPlan(action: DailyActionDefinition): GameState {
        val matchedFocus = focusMatches(dayPlan.activeFocus, action, this)
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
        val focus = state.dayPlan.activeFocus
        val completed = if (focus == DailyFocus.BALANCED) {
            state.dayPlan.categoriesCompleted.size >= 3
        } else {
            state.dayPlan.focusActionsCompleted > 0
        }
        val effect = when {
            completed && focus == DailyFocus.MONEY -> ActionEffect(cashDelta = 25, stressDelta = -3, creditScoreDelta = 2)
            completed && focus == DailyFocus.CAREER -> ActionEffect(promotionReadinessDelta = if (state.career.employed) 5 else 0, offerProgressDelta = if (state.career.employed) 0 else 5, reputationDelta = 2, moodDelta = 2)
            completed && focus == DailyFocus.RECOVERY -> ActionEffect(healthDelta = 4, moodDelta = 3, stressDelta = -7)
            completed && focus == DailyFocus.SOCIAL -> ActionEffect(moodDelta = 4, stressDelta = -3, communicationDelta = 2)
            completed && focus == DailyFocus.BALANCED -> ActionEffect(moodDelta = 4, stressDelta = -4, healthDelta = 2)
            !completed && focus != DailyFocus.BALANCED -> ActionEffect(moodDelta = -1, stressDelta = 3)
            else -> ActionEffect()
        }
        if (!effect.hasAnyImpact()) return FocusOutcome(state = state)

        val updated = applyEffect(state, effect)
        val title = if (completed) "${focus.label} focus completed" else "${focus.label} focus missed"
        val detail = if (completed) {
            "The daily focus paid off."
        } else {
            "Skipping the planned focus added light pressure."
        }
        return FocusOutcome(
            state = updated,
            messages = listOf(title),
            entries = listOf(
                HistoryEntry(
                    day = state.day,
                    title = title,
                    detail = detail,
                    kind = HistoryKind.GOAL,
                ),
            ),
        )
    }

    private fun quickActionIdsFor(state: GameState): List<String> {
        val candidates = buildList {
            state.timedOpportunities.forEach { addAll(recommendedActionIdsForOpportunity(it.id)) }
            if (state.business.activeProjects > 0) add("client_project")
            if (!state.career.employed) {
                if (
                    state.jobSearch.offerProgress >= 55 &&
                    state.jobSearch.interviewReadiness >= 35 &&
                    state.jobSearch.applicationsSent > 0
                ) {
                    add("attend_interview")
                } else {
                    add("send_applications")
                    add("interview_prep")
                }
                add("temp_shift")
            } else {
                add("work_shift")
                if (state.career.promotionReadiness >= 70) add("manager_check_in")
            }
            if (state.business.leads > 0) {
                add("pitch_client")
            } else {
                add("find_leads")
                if (state.business.stage == BusinessStage.IDEA) add("research_offer")
            }
            when (state.dayPlan.activeFocus) {
                DailyFocus.MONEY -> addAll(listOf("client_project", "temp_shift", "work_shift", "budget_review"))
                DailyFocus.CAREER -> if (state.career.employed) {
                    addAll(listOf("manager_check_in", "study_course", "networking"))
                } else {
                    addAll(listOf("send_applications", "interview_prep", "attend_interview"))
                }
                DailyFocus.RECOVERY -> addAll(listOf("rest", "exercise", "cook_at_home"))
                DailyFocus.SOCIAL -> addAll(listOf("call_family", "socialize", "networking"))
                DailyFocus.BALANCED -> addAll(listOf("budget_review", "exercise", "call_family"))
            }
        }.distinct()

        return candidates
            .mapNotNull { id -> actions.firstOrNull { it.id == id } }
            .filter { unavailableReason(state, it) == null }
            .map { it.id }
            .take(4)
    }

    private fun recommendationReasonFor(
        state: GameState,
        action: DailyActionDefinition,
        focusMatch: Boolean,
    ): String? {
        val opportunityRecommended = state.timedOpportunities.any { action.id in recommendedActionIdsForOpportunity(it.id) }
        return when {
            opportunityRecommended -> "Opportunity"
            state.business.activeProjects > 0 && action.id == "client_project" -> "Project"
            !state.career.employed && action.id in JOB_SEARCH_ACTION_IDS -> "Job"
            focusMatch -> "Focus"
            action.id in quickActionIdsFor(state) -> "Recommended"
            else -> null
        }
    }

    private fun shouldOfferOpportunity(state: GameState, id: String): Boolean {
        val daysUntilBill = state.finances.nextBillDueDay - state.day
        return when (id) {
            BILL_BUFFER_ID -> state.finances.cash < state.finances.weeklyLivingCost + 100 && daysUntilBill <= 4
            RECOVERY_WINDOW_ID -> state.stats.stress >= 65 || state.stats.energy <= 35 || state.stats.health <= 45
            PROMOTION_PUSH_ID -> state.career.employed && state.career.promotionReadiness in 70..99
            RECONNECT_ID -> state.relationships.average <= 45
            DEBT_BRAKE_ID -> state.finances.debt >= 400 || state.finances.creditScore < 620
            else -> false
        }
    }

    private fun createOpportunity(state: GameState, id: String): TimedOpportunityState = when (id) {
        BILL_BUFFER_ID -> TimedOpportunityState(
            id = id,
            progress = state.finances.cash.coerceAtMost(state.finances.weeklyLivingCost + 100),
            target = state.finances.weeklyLivingCost + 100,
            baseline = state.finances.cash,
            expiresOnDay = minOf(state.finances.nextBillDueDay, state.day + 3),
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
            progress = state.career.promotionReadiness.coerceIn(0, 100),
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
        PROMOTION_PUSH_ID -> state.career.employed && state.career.promotionReadiness >= 100
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

    private fun opportunityProgressPreview(
        state: GameState,
        action: DailyActionDefinition,
        effect: ActionEffect,
    ): Int = state.timedOpportunities.sumOf { opportunity ->
        when (opportunity.id) {
            BILL_BUFFER_ID -> {
                val resultingCash = state.finances.cash + effect.cashDelta - action.moneyCost
                (resultingCash.coerceIn(0, opportunity.target) - opportunity.progress).coerceAtLeast(0)
            }
            RECOVERY_WINDOW_ID -> (-effect.stressDelta).coerceAtLeast(0)
            PROMOTION_PUSH_ID -> effect.promotionReadinessDelta.coerceAtLeast(0)
            RECONNECT_ID -> if (action.category == ActionCategory.SOCIAL && opportunity.progress < opportunity.target) 1 else 0
            DEBT_BRAKE_ID -> (-effect.debtDelta).coerceAtLeast(0)
            else -> 0
        }
    }

    private fun recommendedActionIdsForOpportunity(id: String): List<String> = when (id) {
        BILL_BUFFER_ID -> listOf("budget_review", "temp_shift", "work_shift", "client_project", "overtime")
        RECOVERY_WINDOW_ID -> listOf("rest", "exercise", "cook_at_home")
        PROMOTION_PUSH_ID -> listOf("manager_check_in", "work_shift", "study_course", "networking")
        RECONNECT_ID -> listOf("socialize", "call_family", "networking")
        DEBT_BRAKE_ID -> listOf("budget_review", "temp_shift", "client_project", "work_shift", "overtime")
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
        if (effect.offerProgressDelta != 0) add(ActionDelta("Offer", effect.offerProgressDelta))
        if (effect.interviewReadinessDelta != 0) add(ActionDelta("Readiness", effect.interviewReadinessDelta))
        if (effect.applicationsDelta != 0) add(ActionDelta("Apps", effect.applicationsDelta))
        if (effect.promotionReadinessDelta != 0) add(ActionDelta("Promotion", effect.promotionReadinessDelta))
        if (effect.reputationDelta != 0) add(ActionDelta("Reputation", effect.reputationDelta))
        if (effect.leadsDelta != 0) add(ActionDelta("Leads", effect.leadsDelta))
        if (effect.activeProjectsDelta != 0) add(ActionDelta("Projects", effect.activeProjectsDelta))
        if (effect.completedProjectsDelta != 0) add(ActionDelta("Clients", effect.completedProjectsDelta))
        if (effect.businessReputationDelta != 0) add(ActionDelta("Biz rep", effect.businessReputationDelta))
        if (effect.pipelineValueDelta != 0) add(ActionDelta("Pipeline", effect.pipelineValueDelta))
        if (effect.clientTrustDelta != 0) add(ActionDelta("Trust", effect.clientTrustDelta))
        val relationshipDelta = effect.socialDelta + effect.familyDelta + effect.friendsDelta + effect.networkDelta
        if (relationshipDelta != 0) add(ActionDelta("Social", relationshipDelta))
        if (opportunityProgressDelta != 0) add(ActionDelta("Opp", opportunityProgressDelta))
    }.take(10)

    private fun summarizeAction(action: DailyActionDefinition, effect: ActionEffect): String {
        val gains = buildList {
            if (effect.cashDelta > 0) add("+${effect.cashDelta} cash")
            if (effect.debtDelta < 0) add("${effect.debtDelta} debt")
            if (effect.offerProgressDelta > 0) add("+${effect.offerProgressDelta} offer")
            if (effect.applicationsDelta > 0) add("+${effect.applicationsDelta} applications")
            if (effect.careerXpDelta > 0) add("+${effect.careerXpDelta} career XP")
            if (effect.promotionReadinessDelta > 0) add("+${effect.promotionReadinessDelta} promotion")
            if (effect.leadsDelta > 0) add("+${effect.leadsDelta} leads")
            if (effect.completedProjectsDelta > 0) add("+${effect.completedProjectsDelta} client project")
            if (effect.healthDelta > 0) add("+${effect.healthDelta} health")
            if (effect.moodDelta > 0) add("+${effect.moodDelta} mood")
            if (effect.stressDelta < 0) add("${effect.stressDelta} stress")
        }
        val cost = "Time -${action.timeCost}, energy -${action.energyCost}" +
            if (action.moneyCost > 0) ", cash -${action.moneyCost}" else ""
        return if (gains.isEmpty()) cost else "$cost. ${gains.joinToString(", ")}."
    }

    private fun statusFor(state: GameState): String = when {
        !state.career.employed && state.finances.cash < totalWeeklyCost(state) -> "Job hunt pressure"
        state.business.activeProjects > 0 -> "Client work ready"
        state.finances.debt >= 700 -> "Debt pressure"
        state.stats.health <= 35 -> "Fragile"
        state.stats.stress >= 82 -> "Burnout risk"
        state.finances.cash < totalWeeklyCost(state) -> "Cash tight"
        state.career.employed && state.career.promotionReadiness >= 80 -> "Breakthrough close"
        state.business.stage >= BusinessStage.RELIABLE_PIPELINE -> "Business growing"
        state.stats.mood >= 75 && state.stats.health >= 65 -> "Thriving"
        else -> "Stable"
    }

    private fun pressureSummaryFor(state: GameState): String {
        val totalBill = totalWeeklyCost(state)
        val daysUntilBill = (state.finances.nextBillDueDay - state.day).coerceAtLeast(0)
        return when {
            !state.career.employed && state.finances.cash < totalBill ->
                "Cash is tight and steady work is not secured. Temp shifts buy time; applications move you toward a job."
            state.business.activeProjects > 0 ->
                "There is paid client work ready. Finishing it improves cash and the business stage."
            state.business.leads > 0 ->
                "You have warm leads. Pitching can turn them into active paid projects."
            !state.career.employed ->
                "The first job is the biggest income upgrade. Build applications, readiness, and offer progress."
            state.career.promotionReadiness >= 70 ->
                "Promotion readiness is close. Focused work, study, and manager visibility can push it over."
            daysUntilBill <= 2 && state.finances.cash < totalBill + 75 ->
                "Bills are close. Cash, debt reduction, or a quick client payout matter most."
            state.stats.stress >= 70 ->
                "Stress is high enough to threaten the money loop. Recovery protects future options."
            state.relationships.average <= 42 ->
                "Relationships are thin. A small social action can stop the slide."
            else ->
                "You have room to balance money, career, recovery, and relationships."
        }
    }

    private fun businessStageFor(completedProjects: Int): BusinessStage = when {
        completedProjects >= 12 -> BusinessStage.SMALL_BUSINESS
        completedProjects >= 6 -> BusinessStage.RELIABLE_PIPELINE
        completedProjects >= 2 -> BusinessStage.SIDE_HUSTLE
        else -> BusinessStage.IDEA
    }

    private fun businessOverheadFor(stage: BusinessStage): Int = when (stage) {
        BusinessStage.IDEA,
        BusinessStage.SIDE_HUSTLE -> 0
        BusinessStage.RELIABLE_PIPELINE -> 45
        BusinessStage.SMALL_BUSINESS -> 90
    }

    private fun totalWeeklyCost(state: GameState): Int =
        state.finances.weeklyLivingCost + businessOverheadFor(state.business.stage)

    private fun jobTitleFor(careerLevel: Int): String = when {
        careerLevel >= 5 -> "Lead Operator"
        careerLevel >= 4 -> "Senior Associate"
        careerLevel >= 3 -> "Project Associate"
        careerLevel >= 2 -> "Associate"
        else -> "Entry Associate"
    }

    private fun salaryFor(careerLevel: Int): Int =
        105 + ((careerLevel - 1).coerceAtLeast(0) * 40)

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
        applicationsDelta = applicationsDelta + other.applicationsDelta,
        interviewReadinessDelta = interviewReadinessDelta + other.interviewReadinessDelta,
        offerProgressDelta = offerProgressDelta + other.offerProgressDelta,
        leadsDelta = leadsDelta + other.leadsDelta,
        activeProjectsDelta = activeProjectsDelta + other.activeProjectsDelta,
        completedProjectsDelta = completedProjectsDelta + other.completedProjectsDelta,
        clientTrustDelta = clientTrustDelta + other.clientTrustDelta,
        businessReputationDelta = businessReputationDelta + other.businessReputationDelta,
        pipelineValueDelta = pipelineValueDelta + other.pipelineValueDelta,
        familyDelta = familyDelta + other.familyDelta,
        friendsDelta = friendsDelta + other.friendsDelta,
        networkDelta = networkDelta + other.networkDelta,
        modifier = other.modifier ?: modifier,
    )

    private fun ActionEffect.hasAnyImpact(): Boolean =
        this != ActionEffect()

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

    private data class TransitionUpdate(
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
        private const val STARTING_SEED = 50_001L
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
        private val EMPLOYED_ACTION_IDS = setOf("work_shift", "overtime", "manager_check_in")
        private val JOB_SEARCH_ACTION_IDS = setOf("send_applications", "interview_prep", "attend_interview")
    }
}
