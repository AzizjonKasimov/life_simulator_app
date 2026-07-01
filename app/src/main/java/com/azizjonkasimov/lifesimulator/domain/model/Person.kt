package com.azizjonkasimov.lifesimulator.domain.model

enum class RelationType(val label: String) {
    MOTHER("Mother"),
    FATHER("Father"),
    SIBLING("Sibling"),
    FRIEND("Friend"),
    PARTNER("Partner"),
    SPOUSE("Spouse"),
    CHILD("Child"),
    COWORKER("Coworker"),
    PET("Pet"),
}

/** A person in your life: family, friends, partners — each with their own arc. */
data class Person(
    val id: String,
    val name: String,
    val relation: RelationType,
    val age: Int,
    val relationship: Int,
    val alive: Boolean = true,
) {
    fun clamped(): Person = copy(relationship = relationship.coerceIn(0, 100))
}
