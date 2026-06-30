package com.azizjonkasimov.lifesimulator.domain.model

data class SkillSet(
    val knowledge: Int,
    val fitness: Int,
    val career: Int,
) {
    fun clamped(): SkillSet = copy(
        knowledge = knowledge.coerceAtLeast(0),
        fitness = fitness.coerceAtLeast(0),
        career = career.coerceAtLeast(0),
    )
}
