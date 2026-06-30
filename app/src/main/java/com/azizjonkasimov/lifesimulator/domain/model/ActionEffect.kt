package com.azizjonkasimov.lifesimulator.domain.model

data class ActionEffect(
    val cashDelta: Int = 0,
    val debtDelta: Int = 0,
    val creditScoreDelta: Int = 0,
    val healthDelta: Int = 0,
    val moodDelta: Int = 0,
    val energyDelta: Int = 0,
    val stressDelta: Int = 0,
    val socialDelta: Int = 0,
    val knowledgeDelta: Int = 0,
    val fitnessDelta: Int = 0,
    val careerXpDelta: Int = 0,
    val communicationDelta: Int = 0,
    val creativityDelta: Int = 0,
    val reputationDelta: Int = 0,
    val promotionReadinessDelta: Int = 0,
    val familyDelta: Int = 0,
    val friendsDelta: Int = 0,
    val networkDelta: Int = 0,
    val goalProgress: Map<String, Int> = emptyMap(),
    val modifier: LifeModifier? = null,
)
