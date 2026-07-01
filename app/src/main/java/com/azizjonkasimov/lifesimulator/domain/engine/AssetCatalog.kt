package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.AssetDefinition
import com.azizjonkasimov.lifesimulator.domain.model.AssetTag

/**
 * The shop. Things to do with money beyond bills: lasting upgrades that change
 * how the rest of the game plays, plus one consumable splurge. Resale value used
 * for net worth is half the price (see [LifeSimulationEngine.netWorth]).
 */
object AssetCatalog {
    val assets: List<AssetDefinition> = listOf(
        AssetDefinition(
            id = "car",
            name = "Reliable Used Car",
            description = "Gig work pays more and takes less out of you. A real earner.",
            price = 650,
            weeklyUpkeep = 8,
            purchaseEffect = ActionEffect(moodDelta = 4),
            tags = setOf(AssetTag.GIG_BOOST),
        ),
        AssetDefinition(
            id = "apartment",
            name = "Nicer Apartment",
            description = "Calmer mornings and better mood — but rent goes up every week.",
            price = 300,
            rentDelta = 110,
            dailyEffect = ActionEffect(moodDelta = 2, stressDelta = -2),
        ),
        AssetDefinition(
            id = "gym",
            name = "Gym Membership",
            description = "Passive health every day, and workouts hit harder.",
            price = 60,
            weeklyUpkeep = 12,
            dailyEffect = ActionEffect(healthDelta = 1),
            purchaseEffect = ActionEffect(moodDelta = 2),
        ),
        AssetDefinition(
            id = "laptop",
            name = "Quality Laptop",
            description = "Faster client work and sharper study. One-time buy.",
            price = 400,
            tags = setOf(AssetTag.BUSINESS_BOOST, AssetTag.STUDY_BOOST),
        ),
        AssetDefinition(
            id = "therapist",
            name = "Therapist",
            description = "Weekly sessions keep stress in check. Worth it when life is loud.",
            price = 0,
            weeklyUpkeep = 30,
            dailyEffect = ActionEffect(stressDelta = -3, moodDelta = 1),
        ),
        AssetDefinition(
            id = "insurance",
            name = "Health Insurance",
            description = "Softens the blow when a medical surprise hits.",
            price = 0,
            weeklyUpkeep = 25,
            tags = setOf(AssetTag.INSURANCE),
        ),
        AssetDefinition(
            id = "vacation",
            name = "Weekend Getaway",
            description = "A real reset: big mood lift and stress melts away. Consumable.",
            price = 280,
            consumable = true,
            purchaseEffect = ActionEffect(stressDelta = -30, moodDelta = 18, healthDelta = 5),
        ),
        // --- Big-ticket goals: a degree, a home, and income-producing property ---
        AssetDefinition(
            id = "degree",
            name = "University Degree",
            description = "A real credential. Your job pays 25% more and interviews go better. Permanent.",
            price = 3_500,
            resaleValue = 0,
            purchaseEffect = ActionEffect(moodDelta = 6, knowledgeDelta = 12, careerSkillDelta = 8, reputationDelta = 6),
            tags = setOf(AssetTag.DEGREE),
        ),
        AssetDefinition(
            id = "home",
            name = "Buy Your Home",
            description = "Own where you live: no more rent, just upkeep. Calmer, and it holds its value.",
            price = 7_000,
            weeklyUpkeep = 35,
            rentDelta = -140,
            resaleValue = 7_000,
            purchaseEffect = ActionEffect(moodDelta = 10, stressDelta = -6),
            dailyEffect = ActionEffect(moodDelta = 2, stressDelta = -1),
            tags = setOf(AssetTag.HOME),
        ),
        AssetDefinition(
            id = "rental_room",
            name = "Rent Out a Room",
            description = "A spare room on the market. Modest weekly income to get you started as a landlord.",
            price = 2_500,
            weeklyUpkeep = 10,
            weeklyIncome = 55,
            resaleValue = 2_500,
        ),
        AssetDefinition(
            id = "rental_property",
            name = "Rental Property",
            description = "A whole unit working for you. Real weekly income that counts toward being free.",
            price = 12_000,
            weeklyUpkeep = 40,
            weeklyIncome = 230,
            resaleValue = 12_000,
        ),
        AssetDefinition(
            id = "franchise",
            name = "Franchise Stake",
            description = "Buy into a running franchise. The biggest passive earner money can buy.",
            price = 25_000,
            weeklyUpkeep = 90,
            weeklyIncome = 520,
            resaleValue = 25_000,
        ),
    )

    private val byId: Map<String, AssetDefinition> = assets.associateBy { it.id }

    fun byId(id: String): AssetDefinition? = byId[id]

    /** Assets the player keeps (owned), in catalog order. */
    fun ownedDefinitions(ownedIds: List<String>): List<AssetDefinition> =
        assets.filter { it.id in ownedIds && !it.consumable }
}
