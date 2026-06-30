package com.azizjonkasimov.lifesimulator.domain.model

data class SkillSet(
    val knowledge: Int,
    val fitness: Int,
    val career: Int,
    val communication: Int,
    val creativity: Int,
) {
    fun clamped(): SkillSet = copy(
        knowledge = knowledge.coerceAtLeast(0),
        fitness = fitness.coerceAtLeast(0),
        career = career.coerceAtLeast(0),
        communication = communication.coerceAtLeast(0),
        creativity = creativity.coerceAtLeast(0),
    )
}
