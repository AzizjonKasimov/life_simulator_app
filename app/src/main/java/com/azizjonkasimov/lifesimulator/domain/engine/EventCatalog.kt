package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Effect
import com.azizjonkasimov.lifesimulator.domain.model.EventCategory
import com.azizjonkasimov.lifesimulator.domain.model.EventChoice
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LifeEvent
import com.azizjonkasimov.lifesimulator.domain.model.LifeStage
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
import com.azizjonkasimov.lifesimulator.domain.model.Stat

// Concise authoring helpers ------------------------------------------------
private fun happy(a: Int) = Effect.StatDelta(Stat.HAPPINESS, a)
private fun health(a: Int) = Effect.StatDelta(Stat.HEALTH, a)
private fun smarts(a: Int) = Effect.StatDelta(Stat.SMARTS, a)
private fun looks(a: Int) = Effect.StatDelta(Stat.LOOKS, a)
private fun cash(a: Int) = Effect.MoneyDelta(a)
private fun rel(type: RelationType, a: Int) = Effect.RelationshipDelta(amount = a, relation = type)
private fun flag(f: String) = Effect.AddFlag(f)

private val CHILDHOOD = setOf(LifeStage.INFANT, LifeStage.CHILD)
private val CHILD = setOf(LifeStage.CHILD)
private val TEEN = setOf(LifeStage.TEEN)
private val YOUNG = setOf(LifeStage.YOUNG_ADULT)
private val ADULT = setOf(LifeStage.ADULT)
private val SENIOR = setOf(LifeStage.SENIOR)
private val WORKING = setOf(LifeStage.YOUNG_ADULT, LifeStage.ADULT)
private val employed: (GameState) -> Boolean = { it.job != null }

object EventCatalog {
    val all: List<LifeEvent> = listOf(
        // ---- Infancy & childhood ---------------------------------------
        flavour("first_steps", EventCategory.FAMILY, "You took your first wobbly steps.", listOf(happy(2), health(1)), minAge = 1, maxAge = 2, stages = CHILDHOOD),
        flavour("first_word", EventCategory.FAMILY, "You said your first word — your parents were thrilled.", listOf(smarts(2), happy(1)), minAge = 1, maxAge = 3, stages = CHILDHOOD),
        flavour("aced_spelling", EventCategory.SCHOOL, "You aced a spelling test.", listOf(smarts(3), happy(2)), minAge = 6, maxAge = 12, stages = CHILD, oneShot = false, weight = 6),
        flavour("family_trip", EventCategory.FAMILY, "Your family took a road trip to the coast.", listOf(happy(4)), stages = CHILD, oneShot = false, weight = 6),
        LifeEvent(
            id = "playground_bully",
            category = EventCategory.FAMILY,
            prompt = "A bigger kid shoves you off the swings at the playground.",
            stages = CHILD,
            choices = listOf(
                EventChoice("Shove them back", "You stood your ground — and scraped a knee doing it.", listOf(happy(3), health(-2))),
                EventChoice("Tell a teacher", "The teacher sorted it out. Smart move.", listOf(smarts(1))),
                EventChoice("Cry and walk away", "It stung more than your knee would have.", listOf(happy(-3))),
            ),
        ),
        LifeEvent(
            id = "found_five",
            category = EventCategory.MONEY,
            prompt = "You spot a $5 bill on the sidewalk.",
            stages = CHILD,
            choices = listOf(
                EventChoice("Pocket it", "Finders keepers. You bought candy.", listOf(cash(5), happy(2))),
                EventChoice("Turn it in", "Your honesty impressed your mom.", listOf(smarts(1), rel(RelationType.MOTHER, 2))),
            ),
        ),
        LifeEvent(
            id = "stray_puppy",
            category = EventCategory.FAMILY,
            prompt = "A stray puppy follows you all the way home.",
            stages = CHILD,
            choices = listOf(
                EventChoice("Beg to keep it", "Your parents caved. Best day ever.", listOf(happy(6), rel(RelationType.MOTHER, 1))),
                EventChoice("Leave it outside", "You felt awful about it for weeks.", listOf(happy(-2))),
            ),
        ),
        LifeEvent(
            id = "dream_job_kid",
            category = EventCategory.FAMILY,
            prompt = "A relative asks what you want to be when you grow up.",
            stages = CHILD,
            weight = 6,
            choices = listOf(
                EventChoice("An astronaut!", "You spent the year staring at the stars.", listOf(happy(3), smarts(1))),
                EventChoice("A doctor!", "You started reading everything you could.", listOf(smarts(2))),
                EventChoice("A millionaire!", "Ambitious. You started a lemonade stand.", listOf(cash(15), happy(1))),
            ),
        ),

        // ---- Teen ------------------------------------------------------
        flavour("growth_spurt", EventCategory.HEALTH, "You hit a growth spurt.", listOf(looks(3), health(1)), stages = TEEN),
        flavour("varsity_team", EventCategory.SCHOOL, "You made the varsity team.", listOf(happy(5), health(3), looks(1)), stages = TEEN, condition = { it.character.stats.health > 45 }),
        LifeEvent(
            id = "first_party",
            category = EventCategory.ROMANCE,
            prompt = "You're invited to a party where there's alcohol.",
            stages = TEEN,
            minAge = 15,
            choices = listOf(
                EventChoice("Go and drink", "Fun night, rough morning.", listOf(happy(4), health(-3), flag("partier"))),
                EventChoice("Go but stay sober", "You had fun and remembered all of it.", listOf(happy(2))),
                EventChoice("Stay home and study", "Dull, but your grades thanked you.", listOf(happy(-1), smarts(2))),
            ),
        ),
        LifeEvent(
            id = "exam_answers",
            category = EventCategory.CRIME,
            prompt = "A classmate offers to sell you the answers to the final exam.",
            stages = TEEN,
            choices = listOf(
                EventChoice("Buy them", "You passed, but you learned nothing.", listOf(cash(-20), smarts(-2), flag("cheated"))),
                EventChoice("Refuse and study", "You earned your grade the hard way.", listOf(smarts(3))),
            ),
        ),
        LifeEvent(
            id = "school_dance",
            category = EventCategory.ROMANCE,
            prompt = "Your crush asks you to the school dance.",
            stages = TEEN,
            choices = listOf(
                EventChoice("Say yes", "Best night of your teenage life.", listOf(happy(8), looks(1))),
                EventChoice("Panic and decline", "You regretted it instantly.", listOf(happy(-4))),
            ),
        ),
        LifeEvent(
            id = "weekend_job",
            category = EventCategory.MONEY,
            prompt = "There's a weekend job going at the local shop.",
            stages = TEEN,
            minAge = 15,
            choices = listOf(
                EventChoice("Take it", "Your first taste of a paycheck.", listOf(cash(900), smarts(1), happy(-1))),
                EventChoice("Enjoy being young", "You spent the summer with friends instead.", listOf(happy(3))),
            ),
        ),
        LifeEvent(
            id = "shoplift_dare",
            category = EventCategory.CRIME,
            prompt = "A classmate dares you to shoplift a candy bar.",
            stages = TEEN,
            choices = listOf(
                EventChoice("Do it", "You got away with it — this time.", listOf(cash(2), flag("shoplifted"), happy(1))),
                EventChoice("Refuse", "Not worth the risk.", listOf(smarts(1))),
            ),
        ),
        LifeEvent(
            id = "cyberbully",
            category = EventCategory.FAMILY,
            prompt = "You're getting piled on in a group chat.",
            stages = TEEN,
            choices = listOf(
                EventChoice("Report it", "The adults stepped in and it stopped.", listOf(smarts(1), happy(1))),
                EventChoice("Clap back", "It escalated before it fizzled out.", listOf(happy(-1))),
                EventChoice("Log off for a while", "The break did you good.", listOf(happy(2), health(1))),
            ),
        ),
        LifeEvent(
            id = "parents_fighting",
            category = EventCategory.FAMILY,
            prompt = "Your parents have been fighting a lot lately.",
            stages = TEEN,
            choices = listOf(
                EventChoice("Try to mediate", "It was heavy, but they appreciated it.", listOf(smarts(1), rel(RelationType.MOTHER, 2), rel(RelationType.FATHER, 2), happy(-2))),
                EventChoice("Stay out of it", "You kept your head down.", listOf(happy(-1))),
                EventChoice("Talk to a counselor", "Getting it off your chest helped.", listOf(happy(2))),
            ),
        ),

        // ---- Young adult ----------------------------------------------
        LifeEvent(
            id = "university_choice",
            category = EventCategory.SCHOOL,
            prompt = "You got into a good university, but tuition is steep.",
            stages = YOUNG,
            minAge = 18,
            weight = 16,
            choices = listOf(
                EventChoice("Take a loan and go", "You signed the loan and dived into student life.", listOf(cash(-8000), smarts(7), flag("university"))),
                EventChoice("Start working instead", "You chose a paycheck over a campus.", listOf(smarts(1), cash(200))),
            ),
        ),
        LifeEvent(
            id = "coffee_shop_crush",
            category = EventCategory.ROMANCE,
            prompt = "You keep running into someone lovely at your coffee shop.",
            stages = YOUNG,
            choices = listOf(
                EventChoice("Ask them out", "They said yes. You're seeing each other now.", listOf(happy(7), flag("dating"))),
                EventChoice("Play it cool", "You told yourself 'next time'.", listOf(happy(1))),
            ),
        ),
        LifeEvent(
            id = "rent_hike",
            category = EventCategory.MONEY,
            prompt = "Your landlord is raising the rent.",
            stages = WORKING,
            weight = 12,
            choices = listOf(
                EventChoice("Just pay it", "It stung the wallet but you stayed put.", listOf(cash(-1200), happy(-1))),
                EventChoice("Find a cheaper place", "Moving was a hassle, but you saved money.", listOf(cash(-200), happy(-2), health(-1))),
                EventChoice("Move back with family", "Not glamorous, but your savings recovered.", listOf(rel(RelationType.MOTHER, 3), happy(-3))),
            ),
        ),
        LifeEvent(
            id = "friend_loan_small",
            category = EventCategory.FAMILY,
            prompt = "A close friend is short on rent and asks for help.",
            stages = WORKING,
            condition = { it.relationships.any { p -> p.relation == RelationType.FRIEND && p.alive } },
            choices = listOf(
                EventChoice("Chip in $500", "They paid you back and never forgot it.", listOf(cash(-500), rel(RelationType.FRIEND, 8))),
                EventChoice("Politely decline", "Things were a little awkward after.", listOf(rel(RelationType.FRIEND, -4))),
            ),
        ),
        LifeEvent(
            id = "burnout",
            category = EventCategory.WORK,
            prompt = "You've been running on empty at work.",
            stages = WORKING,
            condition = employed,
            choices = listOf(
                EventChoice("Take a real vacation", "You came back a new person.", listOf(cash(-800), happy(6), health(2))),
                EventChoice("Push through it", "You hit your targets and hit a wall.", listOf(happy(-3), health(-2))),
            ),
        ),
        LifeEvent(
            id = "fender_bender",
            category = EventCategory.RANDOM,
            prompt = "Someone rear-ends your car at a red light.",
            stages = WORKING,
            minAge = 18,
            choices = listOf(
                EventChoice("File an insurance claim", "The paperwork paid off.", listOf(cash(400), smarts(1))),
                EventChoice("Let it go", "Not worth the hassle, you decided.", listOf(happy(-1))),
            ),
        ),
        flavour("rescue_dog", EventCategory.FAMILY, "You adopted a rescue dog.", listOf(happy(5)), stages = WORKING, weight = 6),
        flavour("charity_5k", EventCategory.HEALTH, "You ran a charity 5K.", listOf(happy(3), health(2)), stages = WORKING, oneShot = false, weight = 5),

        // ---- Adult -----------------------------------------------------
        LifeEvent(
            id = "buy_house",
            category = EventCategory.MONEY,
            prompt = "You could buy a house, but it would drain your savings.",
            stages = ADULT,
            minAge = 28,
            weight = 14,
            oneShot = true,
            condition = { "homeowner" !in it.flags },
            choices = listOf(
                EventChoice("Buy the house", "Keys in hand — you're a homeowner.", listOf(cash(-20000), happy(6), flag("homeowner"))),
                EventChoice("Keep renting", "Flexibility over a mortgage, for now.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "cholesterol",
            category = EventCategory.HEALTH,
            prompt = "Your doctor warns that your cholesterol is high.",
            stages = ADULT,
            choices = listOf(
                EventChoice("Overhaul your diet", "Hard at first, but you felt great.", listOf(health(6), happy(-1))),
                EventChoice("Ignore the warning", "You'll deal with it later, you figured.", listOf(health(-5))),
            ),
        ),
        LifeEvent(
            id = "friend_big_loan",
            category = EventCategory.MONEY,
            prompt = "An old friend asks to borrow a large sum for a venture.",
            stages = ADULT,
            condition = { it.relationships.any { p -> p.relation == RelationType.FRIEND && p.alive } },
            choices = listOf(
                EventChoice("Lend it to them", "They were grateful beyond words.", listOf(cash(-3000), rel(RelationType.FRIEND, 8))),
                EventChoice("Turn them down", "Business and friendship — you kept them apart.", listOf(rel(RelationType.FRIEND, -6), smarts(1))),
            ),
        ),
        LifeEvent(
            id = "passed_over",
            category = EventCategory.WORK,
            prompt = "You were passed over for a big promotion.",
            stages = ADULT,
            condition = employed,
            choices = listOf(
                EventChoice("Confront your boss", "You spoke your mind. It was noted.", listOf(happy(-1), flag("outspoken"))),
                EventChoice("Quietly job-hunt", "You started keeping your options open.", listOf(smarts(1))),
                EventChoice("Accept it", "You swallowed the disappointment.", listOf(happy(-3))),
            ),
        ),
        LifeEvent(
            id = "inheritance",
            category = EventCategory.MONEY,
            prompt = "A relative passed and left you an inheritance.",
            stages = ADULT,
            oneShot = true,
            weight = 8,
            choices = listOf(
                EventChoice("Invest it wisely", "You put it to work for the future.", listOf(cash(10000), smarts(1))),
                EventChoice("Splurge a little", "You treated yourself and your family.", listOf(cash(4000), happy(6))),
            ),
        ),
        flavour("gardening", EventCategory.RANDOM, "You took up gardening on weekends.", listOf(happy(3), health(1)), stages = ADULT, oneShot = false, weight = 5),
        LifeEvent(
            id = "aching_knees",
            category = EventCategory.HEALTH,
            prompt = "Years of work have left your knees aching.",
            stages = setOf(LifeStage.ADULT, LifeStage.SENIOR),
            choices = listOf(
                EventChoice("See a specialist", "Worth every penny — you moved freely again.", listOf(cash(-300), health(4))),
                EventChoice("Tough it out", "You gritted your teeth through it.", listOf(health(-3))),
            ),
        ),

        // ---- Senior ----------------------------------------------------
        flavour("retirement", EventCategory.WORK, "You retired after decades of work.", listOf(happy(4), flag("retired")), stages = SENIOR),
        flavour("memoirs", EventCategory.RANDOM, "You wrote your memoirs.", listOf(happy(4), smarts(1)), stages = SENIOR),
        LifeEvent(
            id = "grandkids_visit",
            category = EventCategory.FAMILY,
            prompt = "Your grandchildren come to visit.",
            stages = SENIOR,
            oneShot = false,
            weight = 8,
            choices = listOf(
                EventChoice("Spoil them rotten", "Their laughter was worth every cent.", listOf(cash(-200), happy(8))),
                EventChoice("Tell them your stories", "They hung on your every word.", listOf(happy(5))),
            ),
        ),
        LifeEvent(
            id = "risky_surgery",
            category = EventCategory.HEALTH,
            prompt = "Doctors recommend a serious operation.",
            stages = SENIOR,
            choices = listOf(
                EventChoice("Have the surgery", "It was a long recovery, but it worked.", listOf(cash(-2000), health(10))),
                EventChoice("Decline it", "You chose comfort over the operating table.", listOf(health(-4), happy(1))),
            ),
        ),
        LifeEvent(
            id = "legacy_gift",
            category = EventCategory.MONEY,
            prompt = "A charity asks you to leave a legacy gift.",
            stages = SENIOR,
            choices = listOf(
                EventChoice("Donate generously", "You'll be remembered for your kindness.", listOf(cash(-5000), happy(7), flag("philanthropist"))),
                EventChoice("Keep it for family", "You wanted it to go to your own.", listOf(happy(1))),
            ),
        ),

        // ---- Cross-stage / random -------------------------------------
        flavour("good_year", EventCategory.RANDOM, "It was, all things considered, a good year.", listOf(happy(3)), minAge = 6, oneShot = false, weight = 4),
        flavour("the_flu", EventCategory.HEALTH, "You caught a bad case of the flu.", listOf(health(-5)), minAge = 3, oneShot = false, weight = 5),
        flavour("burst_pipe", EventCategory.MONEY, "A pipe burst and flooded part of your home.", listOf(cash(-600), happy(-2)), stages = WORKING, oneShot = false, weight = 4),
        flavour("speeding_ticket", EventCategory.CRIME, "You got a speeding ticket.", listOf(cash(-150), happy(-1)), stages = setOf(LifeStage.ADULT, LifeStage.YOUNG_ADULT), minAge = 18, oneShot = false, weight = 4),
        LifeEvent(
            id = "found_wallet",
            category = EventCategory.CRIME,
            prompt = "You find a wallet with $200 cash and an ID inside.",
            minAge = 10,
            choices = listOf(
                EventChoice("Return it to the owner", "They were overjoyed — and so were you.", listOf(happy(4), smarts(1))),
                EventChoice("Keep the cash", "You were richer, and a little guiltier.", listOf(cash(200), happy(-2), flag("kept_wallet"))),
            ),
        ),
        LifeEvent(
            id = "free_time",
            category = EventCategory.RANDOM,
            prompt = "You find yourself with real free time this year.",
            minAge = 13,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Pick up a new hobby", "You surprised yourself with a hidden talent.", listOf(happy(3), smarts(1))),
                EventChoice("Just relax", "Rest was exactly what you needed.", listOf(happy(2), health(1))),
            ),
        ),
    )

    private val byId: Map<String, LifeEvent> = all.associateBy { it.id }

    fun byId(id: String): LifeEvent? = byId[id]

    /** Every event that may fire for [state] this year. */
    fun eligible(state: GameState): List<LifeEvent> = all.filter { e ->
        state.age in e.minAge..e.maxAge &&
            state.stage in e.stages &&
            (!e.oneShot || e.id !in state.eventsSeen) &&
            e.condition(state)
    }
}

/** A single-choice flavour event that applies automatically on Age Up. */
private fun flavour(
    id: String,
    category: EventCategory,
    prompt: String,
    effects: List<Effect>,
    minAge: Int = 0,
    maxAge: Int = 120,
    stages: Set<LifeStage> = LifeStage.entries.toSet(),
    weight: Int = 10,
    oneShot: Boolean = true,
    condition: (GameState) -> Boolean = { true },
): LifeEvent = LifeEvent(
    id = id,
    category = category,
    prompt = prompt,
    choices = listOf(EventChoice("OK", prompt, effects)),
    minAge = minAge,
    maxAge = maxAge,
    stages = stages,
    weight = weight,
    oneShot = oneShot,
    condition = condition,
)
