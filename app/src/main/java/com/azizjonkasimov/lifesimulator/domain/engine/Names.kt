package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.Person
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
import kotlin.random.Random

/** Real-world names and birthplaces for generating a character and their family. */
object Names {
    private val birthplaces = listOf(
        "New York, USA", "Los Angeles, USA", "London, England", "Manchester, England",
        "Toronto, Canada", "Sydney, Australia", "Seoul, South Korea", "Tokyo, Japan",
        "Berlin, Germany", "Paris, France", "Madrid, Spain", "Rome, Italy",
        "São Paulo, Brazil", "Mexico City, Mexico", "Mumbai, India", "Lagos, Nigeria",
        "Cairo, Egypt", "Istanbul, Turkey", "Stockholm, Sweden", "Amsterdam, Netherlands",
        "Dublin, Ireland", "Manila, Philippines", "Buenos Aires, Argentina", "Cape Town, South Africa",
    )

    private val maleFirst = listOf(
        "James", "Liam", "Noah", "Ethan", "Lucas", "Mason", "Daniel", "Alexander",
        "Ryan", "Adrian", "Marcus", "Leo", "Oscar", "Julian", "Nathan", "Samuel",
        "David", "Diego", "Hassan", "Kenji", "Mateo", "Omar",
    )

    private val femaleFirst = listOf(
        "Emma", "Olivia", "Sophia", "Ava", "Mia", "Isabella", "Amelia", "Chloe",
        "Grace", "Lily", "Nora", "Hazel", "Zoe", "Clara", "Elena", "Maya",
        "Aisha", "Yuki", "Sofia", "Priya", "Layla", "Ana",
    )

    private val lastNames = listOf(
        "Smith", "Johnson", "Brown", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        "Anderson", "Taylor", "Thomas", "Lee", "Walker", "Hall", "Young", "King",
        "Wright", "Lopez", "Hill", "Green", "Nakamura", "Okafor", "Kim", "Rossi",
    )

    fun birthplace(rng: Random): String = birthplaces.random(rng)

    fun firstName(gender: Gender, rng: Random): String =
        (if (gender == Gender.MALE) maleFirst else femaleFirst).random(rng)

    fun lastName(rng: Random): String = lastNames.random(rng)

    /** Generate parents and 0–2 siblings sharing the player's [familyName]. */
    fun startingFamily(rng: Random, familyName: String): List<Person> {
        val family = mutableListOf<Person>()
        val parentAge = 26 + rng.nextInt(0, 15)
        family += Person(
            id = "mother",
            name = "${femaleFirst.random(rng)} $familyName",
            relation = RelationType.MOTHER,
            age = parentAge + rng.nextInt(0, 4),
            relationship = 65 + rng.nextInt(0, 26),
        )
        family += Person(
            id = "father",
            name = "${maleFirst.random(rng)} $familyName",
            relation = RelationType.FATHER,
            age = parentAge + rng.nextInt(0, 6),
            relationship = 65 + rng.nextInt(0, 26),
        )
        val siblingCount = rng.nextInt(0, 3)
        for (i in 0 until siblingCount) {
            val gender = if (rng.nextBoolean()) Gender.MALE else Gender.FEMALE
            family += Person(
                id = "sibling_$i",
                name = "${firstName(gender, rng)} $familyName",
                relation = RelationType.SIBLING,
                age = 1 + rng.nextInt(0, 6),
                relationship = 50 + rng.nextInt(0, 31),
            )
        }
        return family
    }
}
