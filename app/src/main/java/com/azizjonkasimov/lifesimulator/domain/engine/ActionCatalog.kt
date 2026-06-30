package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition

object ActionCatalog {
    val actions: List<DailyActionDefinition> = listOf(
        DailyActionDefinition(
            id = "work_shift",
            title = "Work Shift",
            description = "Put in focused hours at your main job.",
            timeCost = 5,
            energyCost = 28,
            effect = ActionEffect(
                moneyDelta = 95,
                moodDelta = -3,
                stressDelta = 10,
                careerXpDelta = 12,
            ),
        ),
        DailyActionDefinition(
            id = "study",
            title = "Study",
            description = "Build knowledge that compounds into better options.",
            timeCost = 3,
            energyCost = 20,
            effect = ActionEffect(
                moodDelta = -1,
                stressDelta = 4,
                knowledgeDelta = 14,
                careerXpDelta = 5,
            ),
        ),
        DailyActionDefinition(
            id = "exercise",
            title = "Exercise",
            description = "Train your body and release pressure.",
            timeCost = 2,
            energyCost = 18,
            effect = ActionEffect(
                healthDelta = 7,
                moodDelta = 5,
                stressDelta = -9,
                fitnessDelta = 12,
            ),
        ),
        DailyActionDefinition(
            id = "rest",
            title = "Rest",
            description = "Take deliberate recovery time.",
            timeCost = 2,
            energyCost = 0,
            effect = ActionEffect(
                healthDelta = 2,
                moodDelta = 4,
                energyDelta = 24,
                stressDelta = -13,
            ),
        ),
        DailyActionDefinition(
            id = "socialize",
            title = "Socialize",
            description = "Spend time with people who keep life human.",
            timeCost = 3,
            energyCost = 16,
            moneyCost = 12,
            effect = ActionEffect(
                moodDelta = 9,
                stressDelta = -6,
                socialDelta = 13,
            ),
        ),
        DailyActionDefinition(
            id = "freelance_gig",
            title = "Freelance Gig",
            description = "Take a flexible contract for extra money and career practice.",
            timeCost = 4,
            energyCost = 30,
            effect = ActionEffect(
                moneyDelta = 125,
                moodDelta = -5,
                stressDelta = 14,
                careerXpDelta = 9,
                knowledgeDelta = 3,
            ),
        ),
    )
}
