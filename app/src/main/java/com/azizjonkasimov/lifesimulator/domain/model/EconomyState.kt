package com.azizjonkasimov.lifesimulator.domain.model

/**
 * Everything the player can do with money beyond spending it on bills:
 * a safe savings balance, market investments that swing each week, and the
 * one-time assets they have bought. Net worth is built from these plus cash
 * minus debt.
 */
data class EconomyState(
    val savings: Int,
    val investments: List<Investment>,
    val ownedAssets: List<String>,
    /** Share of leftover cash automatically moved to savings each payday (0–100). */
    val autoSavePercent: Int = 0,
    /** Share of leftover cash automatically invested each payday (0–100). */
    val autoInvestPercent: Int = 0,
    /** Which holding auto-invested cash buys into. */
    val autoInvestType: InvestmentType = InvestmentType.INDEX,
    /** Running total of interest savings have ever earned — answers "how much did I make?". */
    val lifetimeInterest: Int = 0,
) {
    val investedValue: Int
        get() = investments.sumOf { it.currentValue }

    /** Total share of spare cash swept away automatically each payday. */
    val autoAllocationPercent: Int
        get() = autoSavePercent + autoInvestPercent

    fun normalized(): EconomyState = copy(
        savings = savings.coerceAtLeast(0),
        investments = investments
            .map { it.normalized() }
            .filter { it.currentValue > 0 || it.principal > 0 },
        autoSavePercent = autoSavePercent.coerceIn(0, 100),
        autoInvestPercent = autoInvestPercent.coerceIn(0, 100),
        lifetimeInterest = lifetimeInterest.coerceAtLeast(0),
    )

    companion object {
        val EMPTY = EconomyState(savings = 0, investments = emptyList(), ownedAssets = emptyList())
    }
}

/** One market holding, aggregated per [InvestmentType] (at most one of each). */
data class Investment(
    val type: InvestmentType,
    val principal: Int,
    val currentValue: Int,
) {
    /** Signed profit/loss against what was put in. */
    val gain: Int
        get() = currentValue - principal

    fun normalized(): Investment = copy(
        principal = principal.coerceAtLeast(0),
        currentValue = currentValue.coerceAtLeast(0),
    )
}

/**
 * Risk profile of an investment. [driftPercent] is the average weekly return,
 * [swingPercent] the maximum random move either side of it. Higher risk = higher
 * average but wilder swings (and real chances of a bad week).
 */
enum class InvestmentType(
    val label: String,
    val blurb: String,
    val driftPercent: Int,
    val swingPercent: Int,
    val minimumBuy: Int,
) {
    INDEX("Index Fund", "Slow and steady. Rarely loses much.", driftPercent = 2, swingPercent = 5, minimumBuy = 50),
    STOCKS("Stocks", "Solid growth, bumpy ride.", driftPercent = 3, swingPercent = 16, minimumBuy = 100),
    CRYPTO("Crypto", "Lottery energy. Moonshots and crashes.", driftPercent = 4, swingPercent = 40, minimumBuy = 100),
}
