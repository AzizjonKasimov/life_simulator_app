package com.azizjonkasimov.lifesimulator.domain.model

data class GameState(
    val day: Int,
    val archetype: LifeArchetype,
    val stats: CoreStats,
    val skills: SkillSet,
    val money: Int,
    val careerLevel: Int,
    val jobTitle: String,
    val timeRemaining: Int,
    val rngSeed: Long,
    val history: List<HistoryEntry>,
) {
    val week: Int
        get() = ((day - 1) / 7) + 1
}
