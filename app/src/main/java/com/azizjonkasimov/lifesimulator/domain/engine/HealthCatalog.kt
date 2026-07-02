package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Ailment
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import kotlin.random.Random

/**
 * The catalog of health conditions the illness system can inflict. Chronic conditions
 * stay until treated; acute ones clear after a rolled number of years. Spontaneous
 * onset is age-weighted, so the aches of middle and old age arrive on schedule.
 */
object HealthCatalog {
    data class Condition(
        val id: String,
        val name: String,
        val severity: Int,          // 1 mild .. 3 severe
        val chronic: Boolean,
        val minAge: Int = 0,
        val duration: IntRange = 1..1, // acute only
        val onsetWeight: Int = 10,     // relative spontaneous-onset likelihood
    )

    val conditions: List<Condition> = listOf(
        Condition("bad_cold", "Bad Cold", severity = 1, chronic = false, duration = 1..1, onsetWeight = 14),
        Condition("broken_bone", "Broken Bone", severity = 1, chronic = false, minAge = 5, duration = 1..1, onsetWeight = 10),
        Condition("mono", "Mononucleosis", severity = 1, chronic = false, minAge = 12, duration = 1..2, onsetWeight = 6),
        Condition("asthma", "Asthma", severity = 1, chronic = true, minAge = 5, onsetWeight = 8),
        Condition("anxiety", "Anxiety", severity = 1, chronic = true, minAge = 12, onsetWeight = 8),
        Condition("depression", "Depression", severity = 2, chronic = true, minAge = 13, onsetWeight = 8),
        Condition("migraines", "Chronic Migraines", severity = 1, chronic = true, minAge = 18, onsetWeight = 6),
        Condition("hypertension", "High Blood Pressure", severity = 1, chronic = true, minAge = 35, onsetWeight = 10),
        Condition("diabetes", "Type 2 Diabetes", severity = 2, chronic = true, minAge = 35, onsetWeight = 8),
        Condition("arthritis", "Arthritis", severity = 1, chronic = true, minAge = 50, onsetWeight = 10),
        Condition("copd", "COPD", severity = 2, chronic = true, minAge = 55, onsetWeight = 6),
        Condition("heart_disease", "Heart Disease", severity = 3, chronic = true, minAge = 45, onsetWeight = 5),
        Condition("cancer", "Cancer", severity = 3, chronic = false, minAge = 30, duration = 2..4, onsetWeight = 4),
        Condition("stroke_recovery", "Stroke Recovery", severity = 2, chronic = false, minAge = 55, duration = 2..3, onsetWeight = 4),
    )

    private val byId: Map<String, Condition> = conditions.associateBy { it.id }

    fun spec(id: String): Condition? = byId[id]

    /** Build a fresh [Ailment] for [id], rolling an acute duration off [rng]. */
    fun ailment(id: String, rng: Random): Ailment? {
        val c = byId[id] ?: return null
        val years = if (c.chronic) 0 else c.duration.first + rng.nextInt(0, c.duration.last - c.duration.first + 1)
        return Ailment(c.id, c.name, c.severity, c.chronic, yearsLeft = years)
    }

    /** How likely a new condition is to appear this year, given age and current health. */
    fun onsetChance(age: Int, health: Int): Double {
        val base = when {
            age < 25 -> 0.008
            age < 40 -> 0.02
            age < 55 -> 0.05
            age < 70 -> 0.09
            else -> 0.14
        }
        return (base * (1.0 + (100 - health) / 200.0)).coerceAtMost(0.4)
    }

    /** Roll a spontaneous new condition the player doesn't already have, or null. */
    fun rollOnset(state: GameState, rng: Random): Ailment? {
        if (rng.nextDouble() >= onsetChance(state.age, state.character.stats.health)) return null
        val held = state.ailments.map { it.id }.toSet()
        val pool = conditions.filter { it.minAge <= state.age && it.id !in held }
        if (pool.isEmpty()) return null
        val total = pool.sumOf { it.onsetWeight }
        var roll = rng.nextInt(total)
        val chosen = pool.firstOrNull { roll -= it.onsetWeight; roll < 0 } ?: pool.last()
        return ailment(chosen.id, rng)
    }
}
