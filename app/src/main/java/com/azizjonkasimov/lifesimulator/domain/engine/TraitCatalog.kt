package com.azizjonkasimov.lifesimulator.domain.engine

import kotlin.random.Random

/**
 * Personality traits rolled at birth. Each nudges the yearly stat drift a little and
 * gives authored events something to gate on. Deliberately small effects — a trait
 * colours a life, it doesn't decide it.
 */
object TraitCatalog {
    data class Trait(
        val id: String,
        val label: String,
        val description: String,
        val healthDrift: Int = 0,
        val smartsDrift: Int = 0,
        val looksDrift: Int = 0,
        val happinessDrift: Int = 0,
    )

    val traits: List<Trait> = listOf(
        Trait("genius", "Genius", "You pick things up fast.", smartsDrift = 1),
        Trait("athletic", "Athletic", "Your body holds up well.", healthDrift = 1),
        Trait("charismatic", "Charismatic", "People are drawn to you.", looksDrift = 1),
        Trait("kind", "Kind-hearted", "You bring out the good in others.", happinessDrift = 1),
        Trait("ambitious", "Ambitious", "You push hard for what you want.", smartsDrift = 1),
        Trait("hot_headed", "Hot-headed", "Your temper gets the better of you.", happinessDrift = -1),
        Trait("frail", "Frail", "Your health needs looking after.", healthDrift = -1),
        Trait("lucky", "Lucky", "Fortune tends to favour you."),
    )

    private val byId: Map<String, Trait> = traits.associateBy { it.id }

    fun byId(id: String): Trait? = byId[id]

    /** Roll one or two distinct starting traits. */
    fun roll(rng: Random): Set<String> {
        val shuffled = traits.shuffled(rng)
        val count = if (rng.nextInt(0, 3) == 0) 2 else 1
        return shuffled.take(count).map { it.id }.toSet()
    }
}
