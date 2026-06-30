package com.azizjonkasimov.lifesimulator.domain.model

enum class LifeArchetype(
    val displayName: String,
    val summary: String,
) {
    STUDENT(
        displayName = "Student",
        summary = "Low income, high learning momentum, and a flexible schedule.",
    ),
    JUNIOR_WORKER(
        displayName = "Junior Worker",
        summary = "Stable paycheck, career traction, and moderate daily pressure.",
    ),
    FREELANCER(
        displayName = "Freelancer",
        summary = "Flexible work, variable income, and higher stress risk.",
    ),
}
