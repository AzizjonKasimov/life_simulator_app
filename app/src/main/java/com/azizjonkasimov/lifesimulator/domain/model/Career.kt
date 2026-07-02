package com.azizjonkasimov.lifesimulator.domain.model

enum class EducationLevel(val label: String) {
    NONE("Not in school"),
    PRIMARY("Primary School"),
    SECONDARY("High School"),
    UNIVERSITY("University"),
    GRADUATE("Graduate"),
}

/**
 * Where the player is in schooling. [level] is what they have *completed*;
 * [enrolledIn] (with [yearsLeft]) is a degree currently being studied for.
 */
data class Education(
    val level: EducationLevel = EducationLevel.NONE,
    val enrolledIn: EducationLevel? = null,
    val yearsLeft: Int = 0,
) {
    val isEnrolled: Boolean get() = enrolledIn != null

    /** A short human label, e.g. "High School" or "Studying University (2 yr left)". */
    val summary: String
        get() = when {
            isEnrolled -> "Studying ${enrolledIn!!.label} ($yearsLeft yr left)"
            else -> level.label
        }
}

enum class JobField(val label: String) {
    LABOR("Labor"),
    SERVICE("Service"),
    OFFICE("Office"),
    CREATIVE("Creative"),
    TECH("Tech"),
    HEALTH("Health"),
}

/**
 * A held job at a specific rung of a career. [id] is the career id; [level] is the
 * 1-based rung. Pays [salaryPerYear] each Age Up; promotions raise the rung.
 */
data class Job(
    val id: String,
    val title: String,
    val field: JobField,
    val salaryPerYear: Int,
    val level: Int = 1,
    val minAge: Int = 18,
    val minSmarts: Int = 0,
    val requiresDegree: Boolean = false,
)
