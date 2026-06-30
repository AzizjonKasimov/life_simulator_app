package com.azizjonkasimov.lifesimulator.domain.model

data class LifeEventDefinition(
    val id: String,
    val title: String,
    val description: String,
    val condition: (GameState) -> Boolean,
    val effect: ActionEffect,
)
