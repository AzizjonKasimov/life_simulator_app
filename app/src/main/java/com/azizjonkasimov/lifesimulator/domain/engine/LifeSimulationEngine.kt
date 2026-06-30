package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.CalendarState
import com.azizjonkasimov.lifesimulator.domain.model.CareerState
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
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

class LifeSimulationEngine(
    private val actions: List<DailyActionDefinition> = ActionCatalog.actions,
    private val events: List<LifeEventDefinition> = EventCatalog.events,
) {
    fun startNewLife(archetype: LifeArchetype): GameState {
        val preset = startingPreset(archetype)
        return GameState(
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

        val quickActions = buildList {
            if (state.finances.cash < state.finances.weeklyLivingCost) add("work_shift")
            if (state.finances.debt > 0) add("budget_review")
            if (state.stats.stress >= 65 || state.stats.energy <= 35) add("rest")
            if (state.relationships.average <= 45) add("call_family")
            add("study_course")
            add("exercise")
        }.distinct().take(4)

        return DashboardSnapshot(
            headline = "Week ${state.week}, Day ${state.day}",
            status = statusFor(state),
            alerts = alerts,
            quickActionIds = quickActions,
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
        actions.map { action ->
            val reason = unavailableReason(state, action)
            ActionAvailability(
                action = action,
                isAvailable = reason == null,
                reason = reason,
            )
        }

    fun performAction(state: GameState, actionId: String): SimulationResult {
        val action = actions.firstOrNull { it.id == actionId }
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
        val effect = dynamicEffectFor(state, action)
        val charged = state.copy(
            calendar = state.calendar.copy(
                timeRemaining = (state.timeRemaining - action.timeCost).coerceAtLeast(0),
            ),
            finances = state.finances.copy(cash = state.finances.cash - action.moneyCost).normalized(),
            stats = state.stats.copy(energy = state.stats.energy - action.energyCost).clamped(),
        )
        val applied = applyEffect(charged, effect)
            .let { if (action.id == "rest") it.copy(modifiers = it.modifiers.filterNot { modifier -> modifier.id == "burnout_risk" }) else it }
            .refreshDynamicGoals()
            .settlePromotion()

        val completedGoals = newlyCompleted(beforeGoals, applied.goals)
        val rewarded = applyGoalRewards(applied, completedGoals)
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
                completedGoals.forEach { add("Goal completed: ${it.title}.") }
            },
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
            withEvent.modifiers.none { it.id == "burnout_risk" }
        ) {
            withEvent.copy(
                modifiers = withEvent.modifiers + LifeModifier(
                    id = "burnout_risk",
                    title = "Burnout risk",
                    description = "Daily energy and mood suffer until you recover.",
                    daysRemaining = 3,
                    moodDelta = -2,
                    energyDelta = -6,
                    stressDelta = 3,
                ),
            )
        } else {
            withEvent
        }
        val completedGoals = newlyCompleted(beforeGoals, withBurnoutRisk.goals)
        val rewarded = applyGoalRewards(withBurnoutRisk, completedGoals)
        val nextDay = rewarded.copy(
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

        return SimulationResult(
            state = nextDay,
            success = true,
            messages = buildList {
                billResult.message?.let { add(it) }
                eventRoll.event?.let { add(it.title) }
                completedGoals.forEach { add("Goal completed: ${it.title}.") }
                add("Day ${state.day + 1} begins.")
            },
        )
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

    companion object {
        const val DAILY_TIME_BUDGET = 12
        private const val EVENT_CHANCE_PERCENT = 35
        private const val HISTORY_LIMIT = 100
    }
}
