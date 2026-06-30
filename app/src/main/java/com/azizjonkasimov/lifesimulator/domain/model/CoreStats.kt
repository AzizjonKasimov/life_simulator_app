package com.azizjonkasimov.lifesimulator.domain.model

data class CoreStats(
    val health: Int,
    val mood: Int,
    val energy: Int,
    val stress: Int,
    val social: Int,
) {
    fun clamped(): CoreStats = copy(
        health = health.coerceIn(0, 100),
        mood = mood.coerceIn(0, 100),
        energy = energy.coerceIn(0, 100),
        stress = stress.coerceIn(0, 100),
        social = social.coerceIn(0, 100),
    )
}
