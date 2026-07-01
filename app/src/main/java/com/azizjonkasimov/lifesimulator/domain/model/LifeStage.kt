package com.azizjonkasimov.lifesimulator.domain.model

/** Life stages gate agency and which events can fire. Derived from age. */
enum class LifeStage(val label: String) {
    INFANT("Infant"),
    CHILD("Child"),
    TEEN("Teen"),
    YOUNG_ADULT("Young Adult"),
    ADULT("Adult"),
    SENIOR("Senior"),
    ;

    companion object {
        fun forAge(age: Int): LifeStage = when {
            age <= 4 -> INFANT
            age <= 12 -> CHILD
            age <= 17 -> TEEN
            age <= 25 -> YOUNG_ADULT
            age <= 64 -> ADULT
            else -> SENIOR
        }
    }
}
