package com.azizjonkasimov.lifesimulator.domain.model

data class ActionEffect(
    val moneyDelta: Int = 0,
    val healthDelta: Int = 0,
    val moodDelta: Int = 0,
    val energyDelta: Int = 0,
    val stressDelta: Int = 0,
    val socialDelta: Int = 0,
    val knowledgeDelta: Int = 0,
    val fitnessDelta: Int = 0,
    val careerXpDelta: Int = 0,
)
