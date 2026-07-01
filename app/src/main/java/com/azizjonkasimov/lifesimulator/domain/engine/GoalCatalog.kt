package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.BusinessTier
import com.azizjonkasimov.lifesimulator.domain.model.Goal

/**
 * The ladder of named goals, early to endgame. It answers "what am I working
 * toward?" without ever playing for the player — each goal only checks state.
 * The final rung, Financial Independence, is the game's real summit: passive
 * income covering the weekly bill.
 */
object GoalCatalog {
    val goals: List<Goal> = listOf(
        Goal(
            id = "break_even",
            title = "Break Even",
            description = "Climb out of the red — net worth reaches \$0.",
            isComplete = { it.netWorth >= 0 },
            progress = { frac(it.netWorth + 400, 400) },
        ),
        Goal(
            id = "first_job",
            title = "First Paycheck",
            description = "Land a steady job with a real shift rate.",
            isComplete = { it.employed },
            progress = { if (it.employed) 1f else 0f },
        ),
        Goal(
            id = "debt_free",
            title = "Debt-Free",
            description = "Owe nothing to anyone.",
            isComplete = { it.debt <= 0 },
            progress = { 1f - frac(it.debt, 400) },
        ),
        Goal(
            id = "rainy_day",
            title = "Rainy-Day Fund",
            description = "Bank \$1,000 in savings for the bad weeks.",
            isComplete = { it.savings >= 1_000 },
            progress = { frac(it.savings, 1_000) },
        ),
        Goal(
            id = "entrepreneur",
            title = "Entrepreneur",
            description = "Grow a business past the side-hustle stage.",
            isComplete = { it.businessTier >= BusinessTier.STUDIO },
            progress = { frac(it.businessTier.ordinal, BusinessTier.STUDIO.ordinal) },
        ),
        Goal(
            id = "investor",
            title = "Investor",
            description = "Hold \$2,000 in the market.",
            isComplete = { it.investedValue >= 2_000 },
            progress = { frac(it.investedValue, 2_000) },
        ),
        Goal(
            id = "landlord",
            title = "Landlord",
            description = "Own something that pays you every week.",
            isComplete = { it.ownsIncomeAsset },
            progress = { if (it.ownsIncomeAsset) 1f else 0f },
        ),
        Goal(
            id = "homeowner",
            title = "Homeowner",
            description = "Buy the place you live and stop paying rent.",
            isComplete = { it.ownsHome },
            progress = { if (it.ownsHome) 1f else 0f },
        ),
        Goal(
            id = "five_figures",
            title = "Five Figures",
            description = "Reach \$25,000 net worth.",
            isComplete = { it.netWorth >= 25_000 },
            progress = { frac(it.netWorth, 25_000) },
        ),
        Goal(
            id = "six_figures",
            title = "Six Figures",
            description = "Reach \$100,000 net worth.",
            isComplete = { it.netWorth >= 100_000 },
            progress = { frac(it.netWorth, 100_000) },
        ),
        Goal(
            id = "financial_independence",
            title = "Financial Independence",
            description = "Your passive income covers your weekly bill. You're free.",
            isComplete = { it.passiveIncome > 0 && it.passiveIncome >= it.weeklyCost },
            progress = { if (it.weeklyCost <= 0) 0f else frac(it.passiveIncome, it.weeklyCost) },
        ),
    )
}

private fun frac(value: Int, target: Int): Float =
    if (target <= 0) 0f else (value.toFloat() / target).coerceIn(0f, 1f)
