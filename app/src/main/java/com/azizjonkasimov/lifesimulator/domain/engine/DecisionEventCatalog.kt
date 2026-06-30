package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.ActionEffect
import com.azizjonkasimov.lifesimulator.domain.model.EventChoice
import com.azizjonkasimov.lifesimulator.domain.model.EventOutcome
import com.azizjonkasimov.lifesimulator.domain.model.LifeEventDefinition

/**
 * Decisions: the moments that make a run a story. Each pauses the game and asks
 * the player to choose. Some choices are sure things; others are gambles with
 * weighted outcomes resolved by a seeded roll. Several revolve around money so
 * the economy stays the beating heart of the game.
 */
object DecisionEventCatalog {
    val events: List<LifeEventDefinition> = listOf(
        LifeEventDefinition(
            id = "risky_gig",
            title = "A friend's startup gig",
            description = "An old friend wants you to buy into a weekend launch. Could pop, could flop.",
            condition = { it.finances.cash >= 220 },
            choices = listOf(
                EventChoice(
                    id = "invest",
                    label = "Buy in ($200)",
                    description = "High risk, high reward.",
                    cashCost = 200,
                    outcomes = listOf(
                        EventOutcome(weight = 3, message = "The launch popped — you triple your money.", good = true, cashDelta = 600),
                        EventOutcome(weight = 4, message = "It fizzled. The money's gone.", good = false, effect = ActionEffect(stressDelta = 6, moodDelta = -4)),
                    ),
                ),
                EventChoice(
                    id = "pass",
                    label = "Pass",
                    description = "Keep your cash.",
                    outcomes = listOf(EventOutcome(message = "You keep your cash and your peace of mind.")),
                ),
            ),
        ),
        LifeEventDefinition(
            id = "car_trouble",
            title = "Your car breaks down",
            description = "The engine's making a noise it definitely shouldn't.",
            condition = { "car" in it.economy.ownedAssets },
            choices = listOf(
                EventChoice(
                    id = "repair",
                    label = "Repair it ($150)",
                    cashCost = 150,
                    outcomes = listOf(EventOutcome(message = "Fixed and reliable again.", good = true, effect = ActionEffect(moodDelta = -1))),
                ),
                EventChoice(
                    id = "risk_it",
                    label = "Risk driving it",
                    description = "Maybe it's nothing.",
                    outcomes = listOf(
                        EventOutcome(weight = 3, message = "It holds up. Lucky.", good = true),
                        EventOutcome(weight = 2, message = "It died on the highway — costly tow and repair.", good = false, cashDelta = -300, effect = ActionEffect(stressDelta = 10, moodDelta = -4)),
                    ),
                ),
            ),
        ),
        LifeEventDefinition(
            id = "friend_loan",
            title = "A friend asks for $100",
            description = "They're in a tight spot and swear they'll pay you back.",
            condition = { it.relationships.friends >= 40 && it.finances.cash >= 100 },
            choices = listOf(
                EventChoice(
                    id = "lend",
                    label = "Lend it ($100)",
                    cashCost = 100,
                    outcomes = listOf(
                        EventOutcome(weight = 3, message = "Repaid with interest and gratitude.", good = true, cashDelta = 120, effect = ActionEffect(friendsDelta = 10)),
                        EventOutcome(weight = 2, message = "They ghosted. Money and trust gone.", good = false, effect = ActionEffect(friendsDelta = -8, moodDelta = -3)),
                    ),
                ),
                EventChoice(
                    id = "decline",
                    label = "Decline",
                    outcomes = listOf(EventOutcome(message = "It's awkward, and they're hurt.", good = false, effect = ActionEffect(friendsDelta = -6, moodDelta = -2))),
                ),
            ),
        ),
        LifeEventDefinition(
            id = "wedding",
            title = "Cousin's wedding, out of town",
            description = "Family expects you there. Flights and a gift won't be cheap.",
            condition = { it.relationships.family >= 40 && it.finances.cash >= 120 },
            choices = listOf(
                EventChoice(
                    id = "go",
                    label = "Go ($120)",
                    cashCost = 120,
                    outcomes = listOf(EventOutcome(message = "Worth every mile — family ties deepen.", good = true, effect = ActionEffect(familyDelta = 12, friendsDelta = 6, moodDelta = 8, stressDelta = -4))),
                ),
                EventChoice(
                    id = "skip",
                    label = "Send regrets",
                    outcomes = listOf(EventOutcome(message = "You save the cash, but the guilt lingers.", good = false, effect = ActionEffect(familyDelta = -8, moodDelta = -3))),
                ),
            ),
        ),
        LifeEventDefinition(
            id = "splurge",
            title = "That gadget you keep eyeing",
            description = "It's not in the budget. It would feel amazing though.",
            condition = { it.finances.cash >= 250 },
            choices = listOf(
                EventChoice(
                    id = "buy",
                    label = "Treat yourself ($220)",
                    cashCost = 220,
                    outcomes = listOf(EventOutcome(message = "Pure dopamine. No regrets... today.", good = true, effect = ActionEffect(moodDelta = 14, stressDelta = -8))),
                ),
                EventChoice(
                    id = "resist",
                    label = "Stay disciplined",
                    outcomes = listOf(EventOutcome(message = "Responsible, if a little dull. Your credit thanks you.", good = true, effect = ActionEffect(moodDelta = -2, creditScoreDelta = 3))),
                ),
            ),
        ),
        LifeEventDefinition(
            id = "freelance_bounty",
            title = "A freelance bounty online",
            description = "A tempting gig with a tight deadline and vague scope.",
            condition = { it.skills.creativity >= 14 || it.business.started },
            choices = listOf(
                EventChoice(
                    id = "grind",
                    label = "Grind the weekend",
                    outcomes = listOf(
                        EventOutcome(weight = 3, message = "Delivered clean. Paid well.", good = true, cashDelta = 180, effect = ActionEffect(stressDelta = 8)),
                        EventOutcome(weight = 1, message = "Endless scope creep, no payout.", good = false, effect = ActionEffect(stressDelta = 12, moodDelta = -4)),
                    ),
                ),
                EventChoice(
                    id = "skip",
                    label = "Skip it",
                    outcomes = listOf(EventOutcome(message = "You rest instead. Probably wise.", good = true, effect = ActionEffect(stressDelta = -3))),
                ),
            ),
        ),
        LifeEventDefinition(
            id = "medical",
            title = "A nagging pain won't quit",
            description = "It's probably nothing. Probably.",
            condition = { it.stats.health <= 55 },
            choices = listOf(
                EventChoice(
                    id = "doctor",
                    label = "See a doctor ($140)",
                    description = "Insurance lowers the bill.",
                    cashCost = 140,
                    outcomes = listOf(EventOutcome(message = "Caught early and sorted.", good = true, effect = ActionEffect(healthDelta = 12, stressDelta = -4))),
                ),
                EventChoice(
                    id = "tough",
                    label = "Tough it out",
                    outcomes = listOf(
                        EventOutcome(weight = 2, message = "It passes on its own.", good = true),
                        EventOutcome(weight = 3, message = "It got worse — an ER visit and a big bill.", good = false, cashDelta = -220, effect = ActionEffect(healthDelta = -12, stressDelta = 8)),
                    ),
                ),
            ),
        ),
        LifeEventDefinition(
            id = "promotion_offer",
            title = "Your manager hints at more",
            description = "More responsibility, more visibility. Also more on your plate.",
            condition = { it.career.employed && it.career.promotionReadiness >= 50 },
            choices = listOf(
                EventChoice(
                    id = "step_up",
                    label = "Step up",
                    outcomes = listOf(EventOutcome(message = "You raise your hand. Eyes are on you now.", good = true, effect = ActionEffect(promotionReadinessDelta = 20, reputationDelta = 6, stressDelta = 8))),
                ),
                EventChoice(
                    id = "steady",
                    label = "Stay steady",
                    outcomes = listOf(EventOutcome(message = "You protect your peace for now.", good = true, effect = ActionEffect(stressDelta = -4, moodDelta = 2))),
                ),
            ),
        ),
        LifeEventDefinition(
            id = "market_tip",
            title = "A hot investment tip",
            description = "Someone swears this one's a sure thing. They always do.",
            condition = { it.finances.cash >= 220 },
            choices = listOf(
                EventChoice(
                    id = "all_in",
                    label = "Go all in ($200)",
                    cashCost = 200,
                    outcomes = listOf(
                        EventOutcome(weight = 2, message = "It mooned. You cash out grinning.", good = true, cashDelta = 560),
                        EventOutcome(weight = 5, message = "Rugged. It's gone.", good = false, effect = ActionEffect(stressDelta = 10, moodDelta = -5)),
                    ),
                ),
                EventChoice(
                    id = "ignore",
                    label = "Ignore the hype",
                    outcomes = listOf(EventOutcome(message = "Probably the smart move.", good = true)),
                ),
            ),
        ),
        LifeEventDefinition(
            id = "crunch_offer",
            title = "A brutal but paid crunch",
            description = "Your boss offers a punishing sprint with a real bonus attached.",
            condition = { it.career.employed && it.stats.energy >= 30 },
            choices = listOf(
                EventChoice(
                    id = "take",
                    label = "Take the crunch",
                    outcomes = listOf(EventOutcome(message = "Exhausting — but the bonus lands.", good = true, cashDelta = 220, effect = ActionEffect(stressDelta = 16, healthDelta = -4, promotionReadinessDelta = 8))),
                ),
                EventChoice(
                    id = "decline",
                    label = "Protect your health",
                    outcomes = listOf(EventOutcome(message = "Balance over burnout.", good = true, effect = ActionEffect(stressDelta = -4, moodDelta = 3))),
                ),
            ),
        ),
    )

    private val byId: Map<String, LifeEventDefinition> = events.associateBy { it.id }

    fun byId(id: String): LifeEventDefinition? = byId[id]
}
