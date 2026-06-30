package com.azizjonkasimov.lifesimulator.domain.model

/**
 * Something the player can buy that changes their life rather than just paying a
 * bill. Most assets are kept (their [dailyEffect] and [rentDelta] apply for as
 * long as they are owned and [weeklyUpkeep] is charged with the weekly bill).
 * A [consumable] asset (a vacation) applies its [purchaseEffect] once and is not
 * retained.
 */
data class AssetDefinition(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val weeklyUpkeep: Int = 0,
    val rentDelta: Int = 0,
    val purchaseEffect: ActionEffect = ActionEffect(),
    val dailyEffect: ActionEffect = ActionEffect(),
    val consumable: Boolean = false,
    val tags: Set<AssetTag> = emptySet(),
)

/** Gameplay hooks an owned asset grants, beyond its raw stat effects. */
enum class AssetTag {
    GIG_BOOST,
    BUSINESS_BOOST,
    STUDY_BOOST,
    INSURANCE,
}
