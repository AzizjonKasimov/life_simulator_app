package com.azizjonkasimov.lifesimulator.domain.model

/**
 * An active health condition. Chronic conditions persist until treated; acute ones
 * clear on their own after [yearsLeft] years. While active, a condition drains
 * [annualDrain] Health each year.
 */
data class Ailment(
    val id: String,
    val name: String,
    val severity: Int,        // 1 mild .. 3 severe
    val chronic: Boolean,
    val yearsLeft: Int = 0,   // acute only: years until it clears by itself
) {
    /** Health lost each year while this condition is active. */
    val annualDrain: Int get() = severity * 2
}
