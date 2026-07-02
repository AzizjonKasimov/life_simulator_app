package com.azizjonkasimov.lifesimulator.domain.model

enum class AssetKind(val label: String) {
    VEHICLE("Vehicle"),
    PROPERTY("Property"),
    LUXURY("Luxury"),
}

/**
 * A possession you own. [value] is its current worth, counted toward net worth on
 * the legacy screen. [id] is unique per owned item (you can own two cars).
 */
data class Asset(
    val id: String,
    val name: String,
    val kind: AssetKind,
    val value: Int,
)
