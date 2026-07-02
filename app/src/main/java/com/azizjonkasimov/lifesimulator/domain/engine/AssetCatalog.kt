package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Asset
import com.azizjonkasimov.lifesimulator.domain.model.AssetKind

/**
 * The things money can buy. Purchases cost [price] up front, give a one-time
 * [happiness] lift, and hold a resale [value] (a fraction of price) that counts
 * toward net worth. Deliberately light — assets are flavour and a scoreboard, not
 * an economy.
 */
object AssetCatalog {
    data class Spec(
        val id: String,
        val name: String,
        val kind: AssetKind,
        val price: Int,
        val minAge: Int,
        val happiness: Int,
        val flag: String? = null,      // set when owned, e.g. "homeowner"
        val resaleFactor: Double = 0.6,
    )

    val specs: List<Spec> = listOf(
        Spec("used_car", "Used Car", AssetKind.VEHICLE, price = 6000, minAge = 16, happiness = 3, flag = "car", resaleFactor = 0.5),
        Spec("new_car", "New Car", AssetKind.VEHICLE, price = 26000, minAge = 18, happiness = 6, flag = "car", resaleFactor = 0.6),
        Spec("sports_car", "Sports Car", AssetKind.VEHICLE, price = 90000, minAge = 21, happiness = 10, flag = "car", resaleFactor = 0.65),
        Spec("condo", "Condo", AssetKind.PROPERTY, price = 130000, minAge = 21, happiness = 8, flag = "homeowner", resaleFactor = 0.9),
        Spec("house", "Family House", AssetKind.PROPERTY, price = 300000, minAge = 24, happiness = 12, flag = "homeowner", resaleFactor = 0.9),
        Spec("mansion", "Mansion", AssetKind.PROPERTY, price = 1_200_000, minAge = 30, happiness = 18, flag = "homeowner", resaleFactor = 0.9),
        Spec("watch", "Luxury Watch", AssetKind.LUXURY, price = 15000, minAge = 18, happiness = 5, resaleFactor = 0.5),
        Spec("yacht", "Yacht", AssetKind.LUXURY, price = 450000, minAge = 30, happiness = 15, resaleFactor = 0.5),
    )

    private val byId: Map<String, Spec> = specs.associateBy { it.id }

    fun spec(id: String): Spec? = byId[id]

    /** Materialise an owned [Asset] from a spec, with a unique instance id. */
    fun asset(id: String, instanceSuffix: Int): Asset? {
        val s = byId[id] ?: return null
        return Asset(
            id = "${s.id}_$instanceSuffix",
            name = s.name,
            kind = s.kind,
            value = (s.price * s.resaleFactor).toInt(),
        )
    }
}
