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
) {
    val investedValue: Int
        get() = investments.sumOf { it.currentValue }

    fun normalized(): EconomyState = copy(
        savings = savings.coerceAtLeast(0),
        investments = investments
            .map { it.normalized() }
            .filter { it.currentValue > 0 || it.principal > 0 },
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
