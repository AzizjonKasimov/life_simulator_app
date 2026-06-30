package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.LifeModifier
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition

/**
 * Passive events: small, automatic colour at the start of a day. Decisions that
 * ask the player to choose live in [DecisionEventCatalog].
 */
object EventCatalog {
    val events: List<LifeEventDefinition> = listOf(
        LifeEventDefinition(
            id = "good_sleep",
            title = "Good sleep",
            description = "A quiet night gives you a better baseline today.",
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
            condition = { it.career.reputation >= 35 || it.skills.knowledge >= 30 },
            effect = ActionEffect(cashDelta = 40, moodDelta = 3, careerSkillDelta = 6, promotionReadinessDelta = 5),
        ),
        LifeEventDefinition(
            id = "lonely_evening",
            title = "Lonely evening",
            description = "Neglected relationships make the evening feel smaller.",
            condition = { it.relationships.average <= 35 || it.stats.social <= 30 },
            effect = ActionEffect(moodDelta = -5, stressDelta = 3, friendsDelta = -2),
        ),
        LifeEventDefinition(
            id = "debt_notice",
            title = "Debt notice",
            description = "A reminder from the bank makes the numbers feel very real.",
            condition = { it.finances.debt >= 300 },
            effect = ActionEffect(moodDelta = -4, stressDelta = 9, creditScoreDelta = -4),
        ),
        LifeEventDefinition(
            id = "mentor_tip",
            title = "Mentor tip",
            description = "A contact gives you advice that speeds up your next step.",
            condition = { it.relationships.network >= 55 && it.career.promotionReadiness >= 45 },
            effect = ActionEffect(careerSkillDelta = 10, reputationDelta = 4, promotionReadinessDelta = 8),
        ),
        LifeEventDefinition(
            id = "burnout_warning",
            title = "Burnout warning",
            description = "Your body starts pushing back against the pace.",
            condition = { it.stats.stress >= 82 || it.stats.energy <= 22 },
            effect = ActionEffect(
                healthDelta = -5,
                moodDelta = -5,
                stressDelta = 5,
                modifier = LifeModifier(
                    id = "burnout_risk",
                    title = "Burnout risk",
                    description = "Daily energy and mood suffer until you recover.",
                    daysRemaining = 3,
                    moodDelta = -2,
                    energyDelta = -6,
                    stressDelta = 3,
                ),
            ),
        ),
    )
}
