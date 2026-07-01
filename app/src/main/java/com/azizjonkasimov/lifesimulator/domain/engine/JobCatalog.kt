package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Job
import com.azizjonkasimov.lifesimulator.domain.model.JobField

object JobCatalog {
    val all = listOf(
        Job("cashier", "Cashier", JobField.SERVICE, salaryPerYear = 20000, minAge = 16),
        Job("barista", "Barista", JobField.SERVICE, salaryPerYear = 22000, minAge = 16),
        Job("waiter", "Waiter", JobField.SERVICE, salaryPerYear = 24000, minAge = 16),
        Job("laborer", "Construction Laborer", JobField.LABOR, salaryPerYear = 31000, minAge = 18),
        Job("salesperson", "Salesperson", JobField.SERVICE, salaryPerYear = 30000, minAge = 18, minSmarts = 35),
        Job("clerk", "Office Clerk", JobField.OFFICE, salaryPerYear = 34000, minAge = 18, minSmarts = 40),
        Job("mechanic", "Mechanic", JobField.LABOR, salaryPerYear = 38000, minAge = 18, minSmarts = 40),
        Job("designer", "Graphic Designer", JobField.CREATIVE, salaryPerYear = 41000, minAge = 18, minSmarts = 50),
        Job("nurse_aide", "Nursing Aide", JobField.HEALTH, salaryPerYear = 36000, minAge = 18, minSmarts = 45),
        Job("junior_dev", "Junior Developer", JobField.TECH, salaryPerYear = 54000, minAge = 18, minSmarts = 65),
    )

    fun byId(id: String): Job? = all.find { it.id == id }

    fun eligible(age: Int, smarts: Int): List<Job> =
        all.filter { age >= it.minAge && smarts >= it.minSmarts }
}
