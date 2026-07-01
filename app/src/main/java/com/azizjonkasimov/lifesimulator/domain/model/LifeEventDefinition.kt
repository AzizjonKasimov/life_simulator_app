package com.azizjonkasimov.lifesimulator.domain.model

/**
 * A passive life event: small, automatic colour that fires at the start of a day
 * when its [condition] holds and applies [effect] automatically.
 */
data class LifeEventDefinition(
    val id: String,
    val title: String,
    val description: String,
    val condition: (GameState) -> Boolean,
    val effect: ActionEffect = ActionEffect(),
)
