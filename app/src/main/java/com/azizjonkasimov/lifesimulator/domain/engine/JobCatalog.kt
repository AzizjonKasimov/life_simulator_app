package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.Job
import com.azizjonkasimov.lifesimulator.domain.model.JobField

/** One rung on a career ladder: a title and the salary it pays. */
data class CareerRung(val title: String, val salaryPerYear: Int)

/**
 * A whole career: an entry requirement plus a ladder of [rungs] you climb through
 * promotions. [partTime] careers are open to teens; the rest need [minAge].
 */
data class Career(
    val id: String,
    val field: JobField,
    val rungs: List<CareerRung>,
    val minAge: Int = 18,
    val minSmarts: Int = 0,
    val requiresDegree: Boolean = false,
    val partTime: Boolean = false,
) {
    /** The job you hold when first hired (rung 1). */
    fun entryJob(): Job = jobAt(1)

    fun jobAt(level: Int): Job {
        val rung = rungs[(level - 1).coerceIn(0, rungs.lastIndex)]
        return Job(
            id = id,
            title = rung.title,
            field = field,
            salaryPerYear = rung.salaryPerYear,
            level = level.coerceIn(1, rungs.size),
            minAge = minAge,
            minSmarts = minSmarts,
            requiresDegree = requiresDegree,
        )
    }
}

object JobCatalog {
    val careers: List<Career> = listOf(
        // ---- Service (teen-friendly, part-time) ------------------------
        Career("cashier", JobField.SERVICE, partTime = true, minAge = 16, rungs = listOf(
            CareerRung("Cashier", 20000), CareerRung("Shift Lead", 26000), CareerRung("Store Supervisor", 34000),
        )),
        Career("barista", JobField.SERVICE, partTime = true, minAge = 16, rungs = listOf(
            CareerRung("Barista", 22000), CareerRung("Head Barista", 28000), CareerRung("Café Manager", 38000),
        )),
        Career("waiter", JobField.SERVICE, partTime = true, minAge = 16, rungs = listOf(
            CareerRung("Waiter", 24000), CareerRung("Head Waiter", 31000), CareerRung("Restaurant Manager", 44000),
        )),
        Career("sales", JobField.SERVICE, minAge = 18, minSmarts = 40, rungs = listOf(
            CareerRung("Salesperson", 30000), CareerRung("Account Manager", 55000), CareerRung("Sales Director", 95000),
        )),
        // ---- Labor -----------------------------------------------------
        Career("laborer", JobField.LABOR, minAge = 18, rungs = listOf(
            CareerRung("Construction Laborer", 31000), CareerRung("Foreman", 42000), CareerRung("Site Manager", 58000),
        )),
        Career("mechanic", JobField.LABOR, minAge = 18, minSmarts = 35, rungs = listOf(
            CareerRung("Apprentice Mechanic", 28000), CareerRung("Mechanic", 40000), CareerRung("Master Mechanic", 54000), CareerRung("Shop Owner", 72000),
        )),
        Career("electrician", JobField.LABOR, minAge = 18, minSmarts = 40, rungs = listOf(
            CareerRung("Apprentice Electrician", 30000), CareerRung("Electrician", 48000), CareerRung("Master Electrician", 64000),
        )),
        // ---- Office ----------------------------------------------------
        Career("clerk", JobField.OFFICE, minAge = 18, minSmarts = 40, rungs = listOf(
            CareerRung("Office Clerk", 34000), CareerRung("Office Manager", 46000), CareerRung("Operations Manager", 64000),
        )),
        Career("accountant", JobField.OFFICE, minAge = 21, minSmarts = 60, requiresDegree = true, rungs = listOf(
            CareerRung("Junior Accountant", 46000), CareerRung("Accountant", 62000), CareerRung("Senior Accountant", 84000), CareerRung("Finance Director", 125000),
        )),
        Career("analyst", JobField.OFFICE, minAge = 21, minSmarts = 62, requiresDegree = true, rungs = listOf(
            CareerRung("Business Analyst", 54000), CareerRung("Senior Analyst", 74000), CareerRung("Consultant", 100000),
        )),
        Career("teacher", JobField.OFFICE, minAge = 22, minSmarts = 55, requiresDegree = true, rungs = listOf(
            CareerRung("Teaching Assistant", 30000), CareerRung("Teacher", 48000), CareerRung("Head Teacher", 66000), CareerRung("Principal", 92000),
        )),
        Career("lawyer", JobField.OFFICE, minAge = 24, minSmarts = 78, requiresDegree = true, rungs = listOf(
            CareerRung("Paralegal", 48000), CareerRung("Associate", 90000), CareerRung("Attorney", 165000), CareerRung("Partner", 320000),
        )),
        // ---- Creative --------------------------------------------------
        Career("designer", JobField.CREATIVE, minAge = 18, minSmarts = 50, rungs = listOf(
            CareerRung("Junior Designer", 36000), CareerRung("Designer", 52000), CareerRung("Art Director", 80000),
        )),
        Career("writer", JobField.CREATIVE, minAge = 18, minSmarts = 55, rungs = listOf(
            CareerRung("Copywriter", 34000), CareerRung("Senior Writer", 52000), CareerRung("Editor-in-Chief", 86000),
        )),
        Career("musician", JobField.CREATIVE, minAge = 16, minSmarts = 30, rungs = listOf(
            CareerRung("Session Musician", 22000), CareerRung("Recording Artist", 50000), CareerRung("Headliner", 150000),
        )),
        // ---- Tech ------------------------------------------------------
        Career("it_support", JobField.TECH, minAge = 18, minSmarts = 45, rungs = listOf(
            CareerRung("IT Support", 40000), CareerRung("Systems Admin", 58000), CareerRung("IT Manager", 86000),
        )),
        Career("developer", JobField.TECH, minAge = 18, minSmarts = 65, rungs = listOf(
            CareerRung("Junior Developer", 60000), CareerRung("Software Engineer", 92000), CareerRung("Senior Engineer", 135000), CareerRung("Engineering Lead", 185000),
        )),
        Career("data_scientist", JobField.TECH, minAge = 22, minSmarts = 72, requiresDegree = true, rungs = listOf(
            CareerRung("Data Analyst", 58000), CareerRung("Data Scientist", 98000), CareerRung("Head of Data", 155000),
        )),
        // ---- Health ----------------------------------------------------
        Career("nurse", JobField.HEALTH, minAge = 18, minSmarts = 50, rungs = listOf(
            CareerRung("Nursing Aide", 34000), CareerRung("Registered Nurse", 62000), CareerRung("Head Nurse", 90000),
        )),
        Career("pharmacist", JobField.HEALTH, minAge = 24, minSmarts = 72, requiresDegree = true, rungs = listOf(
            CareerRung("Pharmacy Tech", 40000), CareerRung("Pharmacist", 115000), CareerRung("Chief Pharmacist", 155000),
        )),
        Career("doctor", JobField.HEALTH, minAge = 26, minSmarts = 82, requiresDegree = true, rungs = listOf(
            CareerRung("Medical Intern", 62000), CareerRung("Physician", 155000), CareerRung("Senior Consultant", 245000), CareerRung("Chief of Medicine", 360000),
        )),
    )

    private val byId: Map<String, Career> = careers.associateBy { it.id }

    fun career(id: String): Career? = byId[id]

    /** Reconstruct a held [Job] from a career id at a rung — used to load/promote. */
    fun byId(id: String): Job? = byId[id]?.entryJob()

    /** The next rung up from a held job, or null if already at the top. */
    fun promoted(job: Job): Job? {
        val career = byId[job.id] ?: return null
        if (job.level >= career.rungs.size) return null
        return career.jobAt(job.level + 1)
    }

    /** Entry-level jobs the player currently qualifies for. */
    fun eligible(age: Int, smarts: Int, education: EducationLevel): List<Job> =
        careers
            .filter { age >= it.minAge && smarts >= it.minSmarts && (!it.requiresDegree || education.hasDegree) }
            .map { it.entryJob() }
}

private val EducationLevel.hasDegree: Boolean
    get() = this == EducationLevel.UNIVERSITY || this == EducationLevel.GRADUATE
