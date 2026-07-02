package com.azizjonkasimov.lifesimulator.domain.model

/**
 * Current incarceration: a [sentence] in years, of which [served] have passed.
 * Each Age Up in prison increments [served]; release happens when it reaches the
 * sentence. Work, school, and most activities are on hold while inside.
 */
data class Prison(
    val sentence: Int,
    val served: Int = 0,
) {
    val yearsLeft: Int get() = (sentence - served).coerceAtLeast(0)
}
