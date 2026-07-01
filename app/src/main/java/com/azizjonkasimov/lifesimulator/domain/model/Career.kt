package com.azizjonkasimov.lifesimulator.domain.model

enum class EducationLevel(val label: String) {
    NONE("Not in school"),
    PRIMARY("Primary School"),
    SECONDARY("High School"),
    UNIVERSITY("University"),
    GRADUATE("Graduate"),
}

data class Education(
    val level: EducationLevel = EducationLevel.NONE,
)

enum class JobField(val label: String) {
    LABOR("Labor"),
    SERVICE("Service"),
    OFFICE("Office"),
    CREATIVE("Creative"),
    TECH("Tech"),
    HEALTH("Health"),
}

/** A held job. Pays [salaryPerYear] each Age Up. */
data class Job(
    val id: String,
    val title: String,
    val field: JobField,
    val salaryPerYear: Int,
    val minAge: Int = 18,
    val minSmarts: Int = 0,
)
