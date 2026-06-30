package com.azizjonkasimov.lifesimulator.domain.model

data class DailyActionDefinition(
    val id: String,
    val title: String,
    val description: String,
    val timeCost: Int,
    val energyCost: Int,
    val moneyCost: Int = 0,
    val effect: ActionEffect,
)

data class ActionAvailability(
    val action: DailyActionDefinition,
    val isAvailable: Boolean,
    val reason: String?,
)
