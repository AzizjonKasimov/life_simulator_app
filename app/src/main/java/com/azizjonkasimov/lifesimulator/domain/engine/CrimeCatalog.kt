package com.azizjonkasimov.lifesimulator.domain.engine

/**
 * Crimes the player can attempt from the Activities tab. Each is a seeded gamble:
 * pull it off for a [payoff], or get caught and serve a [sentence]. Smarter crooks
 * (and the lucky) get caught less; bigger scores carry a bigger [catchChance] and a
 * longer stretch inside.
 */
object CrimeCatalog {
    data class Crime(
        val id: String,
        val label: String,
        val description: String,
        val minAge: Int,
        val payoff: IntRange,
        val catchChance: Double,
        val sentence: IntRange, // years if caught
    )

    val crimes: List<Crime> = listOf(
        Crime("shoplift", "Shoplift", "Slip something small past the register.", minAge = 12, payoff = 20..250, catchChance = 0.35, sentence = 1..1),
        Crime("pickpocket", "Pickpocket", "Lift a wallet in a crowd.", minAge = 14, payoff = 60..500, catchChance = 0.40, sentence = 1..2),
        Crime("burglary", "Burglary", "Break into a house after dark.", minAge = 18, payoff = 500..5000, catchChance = 0.50, sentence = 1..4),
        Crime("car_theft", "Steal a Car", "Hotwire one and sell it on.", minAge = 18, payoff = 2000..14000, catchChance = 0.55, sentence = 2..5),
        Crime("fraud", "Run a Scam", "Con people out of their savings.", minAge = 21, payoff = 5000..45000, catchChance = 0.45, sentence = 2..7),
        Crime("bank_robbery", "Rob a Bank", "The big score — if you make it out.", minAge = 21, payoff = 25000..160000, catchChance = 0.72, sentence = 5..15),
    )

    private val byId: Map<String, Crime> = crimes.associateBy { it.id }

    fun byId(id: String): Crime? = byId[id]
}
