package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition

object EventCatalog {
    val events: List<LifeEventDefinition> = listOf(
        LifeEventDefinition(
            id = "good_sleep",
            title = "Good sleep",
            description = "A quiet night gives you a better baseline tomorrow.",
            condition = { it.stats.stress <= 45 },
            effect = ActionEffect(healthDelta = 3, moodDelta = 4, energyDelta = 8),
        ),
        LifeEventDefinition(
            id = "stress_spike",
            title = "Stress spike",
            description = "Pressure catches up and makes the day feel heavier.",
            condition = { it.stats.stress >= 70 },
            effect = ActionEffect(healthDelta = -4, moodDelta = -6, stressDelta = 8),
        ),
        LifeEventDefinition(
            id = "small_opportunity",
            title = "Small opportunity",
            description = "Someone notices your consistency and a small opening appears.",
            condition = { it.skills.career >= 25 || it.skills.knowledge >= 30 },
            effect = ActionEffect(moneyDelta = 35, moodDelta = 3, careerXpDelta = 6),
        ),
        LifeEventDefinition(
            id = "lonely_evening",
            title = "Lonely evening",
            description = "Neglected relationships make the evening feel smaller.",
            condition = { it.stats.social <= 30 },
            effect = ActionEffect(moodDelta = -5, stressDelta = 3),
        ),
    )
}
