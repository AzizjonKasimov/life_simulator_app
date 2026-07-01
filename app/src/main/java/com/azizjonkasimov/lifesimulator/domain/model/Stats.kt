package com.azizjonkasimov.lifesimulator.domain.model

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
}

/** The four classic life-sim stats. Each runs 0..100. */
enum class Stat(val label: String) {
    HAPPINESS("Happiness"),
    HEALTH("Health"),
    SMARTS("Smarts"),
    LOOKS("Looks"),
}

data class Stats(
    val happiness: Int,
    val health: Int,
    val smarts: Int,
    val looks: Int,
) {
    fun clamped(): Stats = Stats(
        happiness = happiness.coerceIn(0, 100),
        health = health.coerceIn(0, 100),
        smarts = smarts.coerceIn(0, 100),
        looks = looks.coerceIn(0, 100),
    )

    fun get(stat: Stat): Int = when (stat) {
        Stat.HAPPINESS -> happiness
        Stat.HEALTH -> health
        Stat.SMARTS -> smarts
        Stat.LOOKS -> looks
    }

    fun withDelta(stat: Stat, amount: Int): Stats = when (stat) {
        Stat.HAPPINESS -> copy(happiness = happiness + amount)
        Stat.HEALTH -> copy(health = health + amount)
        Stat.SMARTS -> copy(smarts = smarts + amount)
        Stat.LOOKS -> copy(looks = looks + amount)
    }.clamped()
}
