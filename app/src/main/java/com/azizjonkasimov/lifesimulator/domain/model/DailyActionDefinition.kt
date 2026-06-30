package com.azizjonkasimov.lifesimulator.domain.model

data class DailyActionDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: ActionCategory,
    val timeCost: Int,
    val energyCost: Int,
    val moneyCost: Int = 0,
    val effect: ActionEffect,
    val tags: List<String> = emptyList(),
)

data class ActionAvailability(
    val action: DailyActionDefinition,
    val isAvailable: Boolean,
    val reason: String?,
    val previewDeltas: List<ActionDelta> = emptyList(),
    val focusMatch: Boolean = false,
    val recommendationReason: String? = null,
    /** For risk actions (interview, find client): the success chance to surface, else null. */
    val oddsPercent: Int? = null,
)

enum class ActionCategory(val label: String) {
    WORK("Work"),
    GROWTH("Growth"),
    WELLBEING("Wellbeing"),
    SOCIAL("Social"),
    MONEY("Money"),
    BUSINESS("Business"),
}

data class ActionDelta(
    val label: String,
    val amount: Int,
    val positiveIsGood: Boolean = true,
)
