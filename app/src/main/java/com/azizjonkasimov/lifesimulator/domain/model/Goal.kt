package com.azizjonkasimov.lifesimulator.domain.model

/**
 * A named milestone the player can chase. Goals never act for the player — they
 * only track and celebrate progress, giving the open-ended economy a spine of
 * things to aim at, ending in Financial Independence.
 */
data class Goal(
    val id: String,
    val title: String,
    val description: String,
    val isComplete: (GoalMetrics) -> Boolean,
    val progress: (GoalMetrics) -> Float,
)

/** The derived numbers goals are judged against, computed once by the engine. */
data class GoalMetrics(
    val netWorth: Int,
    val savings: Int,
    val investedValue: Int,
    val debt: Int,
    val passiveIncome: Int,
    val weeklyCost: Int,
    val businessTier: BusinessTier,
    val ownsHome: Boolean,
    val ownsIncomeAsset: Boolean,
    val employed: Boolean,
    val careerLevel: Int,
)

/** A goal paired with whether it's done and how far along it is (0..1) for the UI. */
data class GoalStatus(
    val goal: Goal,
    val complete: Boolean,
    val progress: Float,
)

/**
 * Weekly money that arrives without spending a day on it, split by source so the
 * player can see where it comes from. Reaching [total] >= the weekly bill is
 * Financial Independence.
 */
data class PassiveIncomeBreakdown(
    val savingsInterest: Int,
    val business: Int,
    val properties: Int,
    val market: Int,
) {
    val total: Int
        get() = savingsInterest + business + properties + market

    companion object {
        val EMPTY = PassiveIncomeBreakdown(0, 0, 0, 0)
    }
}
