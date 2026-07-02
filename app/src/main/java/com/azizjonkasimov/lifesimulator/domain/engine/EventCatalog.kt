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
private fun addPerson(type: RelationType, relationship: Int = 60) = Effect.AddPerson(type, relationship)
private fun leave(type: RelationType) = Effect.RemovePeople(type)
private val marry = Effect.PromoteRelation(RelationType.PARTNER, RelationType.SPOUSE)
private val promoteJob = Effect.PromoteJob
private val loseJob = Effect.LoseJob

private val CHILDHOOD = setOf(LifeStage.INFANT, LifeStage.CHILD)
private val CHILD = setOf(LifeStage.CHILD)
private val TEEN = setOf(LifeStage.TEEN)
private val YOUNG = setOf(LifeStage.YOUNG_ADULT)
private val ADULT = setOf(LifeStage.ADULT)
private val SENIOR = setOf(LifeStage.SENIOR)
private val WORKING = setOf(LifeStage.YOUNG_ADULT, LifeStage.ADULT)
private val GROWN = setOf(LifeStage.YOUNG_ADULT, LifeStage.ADULT, LifeStage.SENIOR)
private val employed: (GameState) -> Boolean = { it.job != null }

// Relationship-state conditions --------------------------------------------
private fun alive(p: com.azizjonkasimov.lifesimulator.domain.model.Person) = p.alive
private val single: (GameState) -> Boolean =
    { st -> st.relationships.none { alive(it) && (it.relation == RelationType.PARTNER || it.relation == RelationType.SPOUSE) } }
private val hasPartner: (GameState) -> Boolean =
    { st -> st.relationships.any { alive(it) && it.relation == RelationType.PARTNER } }
private val hasSpouse: (GameState) -> Boolean =
    { st -> st.relationships.any { alive(it) && it.relation == RelationType.SPOUSE } }
private val partnered: (GameState) -> Boolean =
    { st -> st.relationships.any { alive(it) && (it.relation == RelationType.PARTNER || it.relation == RelationType.SPOUSE) } }
private val hasChild: (GameState) -> Boolean =
    { st -> st.relationships.any { alive(it) && it.relation == RelationType.CHILD } }
private val hasFriend: (GameState) -> Boolean =
    { st -> st.relationships.any { alive(it) && it.relation == RelationType.FRIEND } }
private val inUniversity: (GameState) -> Boolean = { it.education.isEnrolled }
private fun both(a: (GameState) -> Boolean, b: (GameState) -> Boolean): (GameState) -> Boolean = { a(it) && b(it) }

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

        // ==== Romance & dating =========================================
        LifeEvent(
            id = "meet_someone",
            category = EventCategory.ROMANCE,
            prompt = "You've met someone who makes your heart race.",
            stages = GROWN,
            minAge = 16,
            oneShot = false,
            weight = 13,
            condition = single,
            choices = listOf(
                EventChoice("Ask them out", "They said yes. You're seeing each other now.", listOf(addPerson(RelationType.PARTNER, 55), flag("dating"), happy(6))),
                EventChoice("Lose your nerve", "Maybe next time, you told yourself.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "blind_date",
            category = EventCategory.ROMANCE,
            prompt = "A friend sets you up on a blind date.",
            stages = WORKING,
            minAge = 18,
            oneShot = false,
            weight = 7,
            condition = single,
            choices = listOf(
                EventChoice("Give them a chance", "Against the odds, it clicked.", listOf(addPerson(RelationType.PARTNER, 52), flag("dating"), happy(5))),
                EventChoice("Not your type", "You parted as friends.", listOf(happy(1))),
            ),
        ),
        LifeEvent(
            id = "getting_serious",
            category = EventCategory.ROMANCE,
            prompt = "Things with your partner are getting serious. They suggest moving in together.",
            stages = GROWN,
            oneShot = false,
            weight = 9,
            condition = both(hasPartner) { "moved_in" !in it.flags },
            choices = listOf(
                EventChoice("Move in together", "A big step — and a happy one.", listOf(flag("moved_in"), rel(RelationType.PARTNER, 8), happy(5))),
                EventChoice("Take it slow", "No rush, you agreed.", listOf(happy(1))),
            ),
        ),
        LifeEvent(
            id = "partner_proposes",
            category = EventCategory.ROMANCE,
            prompt = "Your partner gets down on one knee and proposes.",
            stages = GROWN,
            minAge = 20,
            oneShot = false,
            weight = 10,
            condition = both(hasPartner) { "married" !in it.flags && it.relationships.any { p -> p.relation == RelationType.PARTNER && p.relationship >= 50 } },
            choices = listOf(
                EventChoice("Say yes!", "You're getting married!", listOf(marry, flag("married"), happy(14))),
                EventChoice("You're not ready", "It was an awkward, painful moment.", listOf(rel(RelationType.PARTNER, -15), happy(-4))),
            ),
        ),
        LifeEvent(
            id = "meet_the_parents",
            category = EventCategory.ROMANCE,
            prompt = "Your partner wants you to meet their parents.",
            stages = GROWN,
            oneShot = false,
            weight = 6,
            condition = hasPartner,
            choices = listOf(
                EventChoice("Charm them", "They adored you.", listOf(rel(RelationType.PARTNER, 6), happy(3))),
                EventChoice("Fumble it", "It was painfully awkward.", listOf(rel(RelationType.PARTNER, -3), happy(-2))),
            ),
        ),
        LifeEvent(
            id = "partner_argument",
            category = EventCategory.ROMANCE,
            prompt = "You and your partner have a nasty argument.",
            stages = GROWN,
            oneShot = false,
            weight = 8,
            condition = partnered,
            choices = listOf(
                EventChoice("Apologize first", "Swallowing your pride paid off.", listOf(rel(RelationType.PARTNER, 5), rel(RelationType.SPOUSE, 5), happy(-1))),
                EventChoice("Talk it through", "You both came out closer.", listOf(rel(RelationType.PARTNER, 7), rel(RelationType.SPOUSE, 7), smarts(1))),
                EventChoice("Give the silent treatment", "It festered for weeks.", listOf(rel(RelationType.PARTNER, -8), rel(RelationType.SPOUSE, -8), happy(-3))),
            ),
        ),
        LifeEvent(
            id = "long_distance",
            category = EventCategory.ROMANCE,
            prompt = "Your partner has to move away for a while.",
            stages = GROWN,
            oneShot = false,
            weight = 5,
            condition = hasPartner,
            choices = listOf(
                EventChoice("Make it work", "Distance was hard, but love held.", listOf(rel(RelationType.PARTNER, 4), happy(-2))),
                EventChoice("End it", "You parted ways.", listOf(leave(RelationType.PARTNER), happy(-6))),
            ),
        ),
        LifeEvent(
            id = "ex_returns",
            category = EventCategory.ROMANCE,
            prompt = "An old flame reappears and wants to rekindle things.",
            stages = GROWN,
            oneShot = false,
            weight = 4,
            condition = single,
            choices = listOf(
                EventChoice("Give it another go", "You're seeing each other again.", listOf(addPerson(RelationType.PARTNER, 50), flag("dating"), happy(4))),
                EventChoice("That ship has sailed", "You've grown past it.", listOf(smarts(1), happy(1))),
            ),
        ),

        // ==== Marriage & children ======================================
        flavour("wedding_day", EventCategory.ROMANCE, "You had a beautiful wedding surrounded by loved ones.", listOf(happy(8), cash(-6000), rel(RelationType.SPOUSE, 5)), stages = GROWN, condition = hasSpouse),
        LifeEvent(
            id = "want_kids",
            category = EventCategory.FAMILY,
            prompt = "You and your spouse talk about starting a family.",
            stages = GROWN,
            oneShot = false,
            weight = 8,
            condition = both(hasSpouse) { it.relationships.count { p -> p.relation == RelationType.CHILD } < 4 },
            choices = listOf(
                EventChoice("Have a child", "Your family is growing.", listOf(addPerson(RelationType.CHILD, 85), happy(8))),
                EventChoice("Not yet", "You decided to wait.", listOf(happy(1))),
            ),
        ),
        LifeEvent(
            id = "surprise_pregnancy",
            category = EventCategory.FAMILY,
            prompt = "A surprise: a baby is on the way.",
            stages = GROWN,
            minAge = 20,
            oneShot = false,
            weight = 5,
            condition = both(partnered) { it.relationships.count { p -> p.relation == RelationType.CHILD } < 4 },
            choices = listOf(
                EventChoice("Overjoyed", "You welcomed a new child with open arms.", listOf(addPerson(RelationType.CHILD, 85), happy(7), cash(-1500))),
                EventChoice("Overwhelmed", "Ready or not, your child arrived.", listOf(addPerson(RelationType.CHILD, 80), happy(1), cash(-1500))),
            ),
        ),
        LifeEvent(
            id = "child_first_word",
            category = EventCategory.FAMILY,
            prompt = "Your child said their first word.",
            stages = GROWN,
            oneShot = false,
            weight = 6,
            condition = hasChild,
            choices = listOf(EventChoice("Treasure it", "A moment you'll never forget.", listOf(happy(5), rel(RelationType.CHILD, 3)))),
        ),
        LifeEvent(
            id = "child_school_trouble",
            category = EventCategory.FAMILY,
            prompt = "Your child is struggling at school.",
            stages = ADULT,
            oneShot = false,
            weight = 6,
            condition = hasChild,
            choices = listOf(
                EventChoice("Help every night", "Slow going, but they improved.", listOf(rel(RelationType.CHILD, 6), happy(-1))),
                EventChoice("Hire a tutor", "Money well spent.", listOf(cash(-800), rel(RelationType.CHILD, 5))),
                EventChoice("Let them sort it out", "They felt unsupported.", listOf(rel(RelationType.CHILD, -5))),
            ),
        ),
        LifeEvent(
            id = "child_rebellion",
            category = EventCategory.FAMILY,
            prompt = "Your teenager is acting out.",
            stages = ADULT,
            oneShot = false,
            weight = 5,
            condition = hasChild,
            choices = listOf(
                EventChoice("Come down hard", "They resented the rules.", listOf(rel(RelationType.CHILD, -6), happy(-2))),
                EventChoice("Hear them out", "Patience won them back.", listOf(rel(RelationType.CHILD, 7), happy(1))),
            ),
        ),
        flavour("child_graduation", EventCategory.FAMILY, "Your child graduated — you couldn't be prouder.", listOf(happy(7), rel(RelationType.CHILD, 5)), stages = setOf(LifeStage.ADULT, LifeStage.SENIOR), condition = hasChild),
        LifeEvent(
            id = "child_college_tuition",
            category = EventCategory.MONEY,
            prompt = "Your child got into university, but it's expensive.",
            stages = setOf(LifeStage.ADULT, LifeStage.SENIOR),
            oneShot = true,
            weight = 6,
            condition = hasChild,
            choices = listOf(
                EventChoice("Pay their way", "A gift they'll never forget.", listOf(cash(-16000), rel(RelationType.CHILD, 10), happy(2))),
                EventChoice("They'll take loans", "They understood, mostly.", listOf(rel(RelationType.CHILD, -4))),
            ),
        ),
        LifeEvent(
            id = "anniversary",
            category = EventCategory.ROMANCE,
            prompt = "It's your wedding anniversary.",
            stages = GROWN,
            oneShot = false,
            weight = 6,
            condition = hasSpouse,
            choices = listOf(
                EventChoice("Plan something special", "A night to remember.", listOf(cash(-500), rel(RelationType.SPOUSE, 6), happy(4))),
                EventChoice("Let it slip your mind", "That went over badly.", listOf(rel(RelationType.SPOUSE, -8), happy(-2))),
            ),
        ),
        LifeEvent(
            id = "marriage_rough_patch",
            category = EventCategory.ROMANCE,
            prompt = "Your marriage has hit a rough patch.",
            stages = GROWN,
            oneShot = false,
            weight = 5,
            condition = hasSpouse,
            choices = listOf(
                EventChoice("Try counseling", "It helped you find each other again.", listOf(cash(-600), rel(RelationType.SPOUSE, 8))),
                EventChoice("Let it drift", "The distance grew.", listOf(rel(RelationType.SPOUSE, -10), happy(-3))),
            ),
        ),
        LifeEvent(
            id = "divorce_papers",
            category = EventCategory.ROMANCE,
            prompt = "The marriage has become unbearable.",
            stages = GROWN,
            oneShot = false,
            weight = 3,
            condition = both(hasSpouse) { it.relationships.any { p -> p.relation == RelationType.SPOUSE && p.relationship < 35 } },
            choices = listOf(
                EventChoice("Work to save it", "You chose to fight for it.", listOf(rel(RelationType.SPOUSE, 6), happy(-2))),
                EventChoice("File for divorce", "It was painful, but final.", listOf(leave(RelationType.SPOUSE), cash(-4000), happy(-6))),
            ),
        ),

        // ==== University ===============================================
        flavour("uni_roommate", EventCategory.SCHOOL, "You and your college roommate became close friends.", listOf(happy(4), addPerson(RelationType.FRIEND, 55)), condition = inUniversity, oneShot = true),
        LifeEvent(
            id = "uni_exam_week",
            category = EventCategory.SCHOOL,
            prompt = "Finals week is here and you're behind.",
            oneShot = false,
            weight = 8,
            condition = inUniversity,
            choices = listOf(
                EventChoice("Pull all-nighters", "You aced them, but you're exhausted.", listOf(smarts(4), health(-3))),
                EventChoice("Cram and hope", "You scraped by with energy to spare.", listOf(smarts(1), happy(2))),
            ),
        ),
        LifeEvent(
            id = "uni_internship",
            category = EventCategory.SCHOOL,
            prompt = "You're offered a summer internship in your field.",
            oneShot = true,
            weight = 9,
            condition = inUniversity,
            choices = listOf(
                EventChoice("Take the internship", "Real experience — and a paycheck.", listOf(cash(4000), smarts(3), flag("internship"))),
                EventChoice("Enjoy your summer", "You recharged instead.", listOf(happy(5))),
            ),
        ),
        LifeEvent(
            id = "uni_study_abroad",
            category = EventCategory.SCHOOL,
            prompt = "There's a chance to study abroad for a semester.",
            oneShot = true,
            weight = 6,
            condition = inUniversity,
            choices = listOf(
                EventChoice("Go for it", "A life-changing experience.", listOf(cash(-3000), happy(7), smarts(3), looks(1))),
                EventChoice("Stay put", "You kept your head down.", listOf(smarts(1))),
            ),
        ),
        flavour("uni_club", EventCategory.SCHOOL, "You joined a campus club and found your people.", listOf(happy(4), smarts(1)), condition = inUniversity, oneShot = false, weight = 5),

        // ==== Career & work ============================================
        LifeEvent(
            id = "impress_boss",
            category = EventCategory.WORK,
            prompt = "There's a chance to lead a high-stakes project.",
            stages = WORKING,
            oneShot = false,
            weight = 7,
            condition = employed,
            choices = listOf(
                EventChoice("Go above and beyond", "It paid off — you climbed the ladder.", listOf(promoteJob, happy(-1))),
                EventChoice("Keep a balance", "You protected your evenings.", listOf(happy(3))),
            ),
        ),
        LifeEvent(
            id = "raise_request",
            category = EventCategory.WORK,
            prompt = "You feel you're due for a raise.",
            stages = WORKING,
            oneShot = false,
            weight = 6,
            condition = employed,
            choices = listOf(
                EventChoice("Make your case", "They agreed to a bonus.", listOf(cash(3000), smarts(1))),
                EventChoice("Don't rock the boat", "You let it be.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "headhunted",
            category = EventCategory.WORK,
            prompt = "A rival company offers you a better position.",
            stages = GROWN,
            oneShot = false,
            weight = 5,
            condition = employed,
            choices = listOf(
                EventChoice("Jump ship", "A fresh start and a bigger title.", listOf(promoteJob, happy(3))),
                EventChoice("Stay loyal", "Your team respected the loyalty.", listOf(happy(2), smarts(1))),
            ),
        ),
        LifeEvent(
            id = "layoffs",
            category = EventCategory.WORK,
            prompt = "Layoffs are sweeping through your company.",
            stages = WORKING,
            oneShot = false,
            weight = 5,
            condition = employed,
            choices = listOf(
                EventChoice("Take the severance", "A clean break, with a cushion.", listOf(loseJob, cash(6000), happy(-3))),
                EventChoice("Keep your head down", "You survived the cut — barely.", listOf(happy(-2))),
            ),
        ),
        LifeEvent(
            id = "costly_mistake",
            category = EventCategory.WORK,
            prompt = "You've made a costly mistake at work.",
            stages = WORKING,
            oneShot = false,
            weight = 4,
            condition = employed,
            choices = listOf(
                EventChoice("Own up to it", "Honesty saved your job.", listOf(smarts(1), happy(-2))),
                EventChoice("Try to hide it", "It came out — and cost you the job.", listOf(loseJob, happy(-5))),
            ),
        ),
        LifeEvent(
            id = "office_romance",
            category = EventCategory.ROMANCE,
            prompt = "A coworker has been flirting with you.",
            stages = GROWN,
            oneShot = false,
            weight = 5,
            condition = both(employed, single),
            choices = listOf(
                EventChoice("Pursue it", "Office gossip aside, you're together.", listOf(addPerson(RelationType.PARTNER, 55), flag("dating"), happy(4))),
                EventChoice("Keep it professional", "Better to keep work and love apart.", listOf(smarts(1))),
            ),
        ),
        flavour("work_award", EventCategory.WORK, "You won an award for your work.", listOf(happy(5), smarts(1)), stages = WORKING, condition = employed, oneShot = false, weight = 4),
        LifeEvent(
            id = "overtime_crunch",
            category = EventCategory.WORK,
            prompt = "Your team is drowning in a deadline.",
            stages = WORKING,
            oneShot = false,
            weight = 6,
            condition = employed,
            choices = listOf(
                EventChoice("Grind it out", "The bonus was nice; the toll was real.", listOf(cash(2000), health(-2), happy(-2))),
                EventChoice("Protect your health", "You set boundaries.", listOf(happy(2))),
            ),
        ),
        LifeEvent(
            id = "career_change",
            category = EventCategory.WORK,
            prompt = "You're burned out on your whole career.",
            stages = ADULT,
            oneShot = false,
            weight = 3,
            condition = employed,
            choices = listOf(
                EventChoice("Quit and reinvent", "Terrifying — and freeing.", listOf(loseJob, happy(5), cash(-1000))),
                EventChoice("Stick with it", "The devil you know.", listOf(happy(-2), cash(1000))),
            ),
        ),

        // ==== Health ===================================================
        LifeEvent(
            id = "sports_injury",
            category = EventCategory.HEALTH,
            prompt = "You take a hard hit playing sports.",
            stages = setOf(LifeStage.TEEN, LifeStage.YOUNG_ADULT),
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Rest and recover", "Boring, but you healed right.", listOf(health(3), happy(-1))),
                EventChoice("Play through it", "Tough — and a little reckless.", listOf(health(-4), happy(2))),
            ),
        ),
        flavour("annual_checkup", EventCategory.HEALTH, "A clean bill of health at your check-up.", listOf(health(2)), stages = WORKING, oneShot = false, weight = 5),
        flavour("food_poisoning", EventCategory.HEALTH, "A dodgy meal left you sick for days.", listOf(health(-4), happy(-1)), minAge = 5, oneShot = false, weight = 4),
        LifeEvent(
            id = "back_pain",
            category = EventCategory.HEALTH,
            prompt = "Chronic back pain is wearing you down.",
            stages = setOf(LifeStage.ADULT, LifeStage.SENIOR),
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Start physiotherapy", "Slow, steady relief.", listOf(cash(-400), health(4))),
                EventChoice("Grit your teeth", "It only got worse.", listOf(health(-3), happy(-1))),
            ),
        ),
        LifeEvent(
            id = "mental_health",
            category = EventCategory.HEALTH,
            prompt = "You've been feeling low for a long time.",
            stages = WORKING,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("See a therapist", "Talking it out helped more than you expected.", listOf(cash(-500), happy(7))),
                EventChoice("Bottle it up", "It quietly ate at you.", listOf(happy(-5))),
            ),
        ),
        LifeEvent(
            id = "health_scare",
            category = EventCategory.HEALTH,
            prompt = "A scary test result comes back.",
            stages = ADULT,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Overhaul your lifestyle", "You took it as a wake-up call.", listOf(health(6), happy(-1))),
                EventChoice("Live in denial", "You buried your head in the sand.", listOf(health(-5))),
            ),
        ),

        // ==== Money ====================================================
        flavour("bonus_windfall", EventCategory.MONEY, "A surprise year-end bonus landed in your account.", listOf(cash(2500), happy(2)), stages = WORKING, condition = employed, oneShot = false, weight = 5),
        flavour("tax_return", EventCategory.MONEY, "Your tax return came back bigger than expected.", listOf(cash(1200)), stages = WORKING, oneShot = false, weight = 5),
        flavour("inheritance_small", EventCategory.MONEY, "A distant relative left you a small sum.", listOf(cash(3000)), stages = GROWN, oneShot = false, weight = 3),
        LifeEvent(
            id = "car_purchase",
            category = EventCategory.MONEY,
            prompt = "Your old car is dying. Time for a new one?",
            stages = WORKING,
            minAge = 18,
            oneShot = true,
            weight = 6,
            condition = { "car" !in it.flags },
            choices = listOf(
                EventChoice("Buy new", "That new-car smell is priceless.", listOf(cash(-18000), happy(6), flag("car"))),
                EventChoice("Buy a used one", "Reliable and easy on the wallet.", listOf(cash(-6000), happy(3), flag("car"))),
                EventChoice("Keep repairing the old one", "You babied it along another year.", listOf(cash(-400), happy(-1))),
            ),
        ),
        LifeEvent(
            id = "side_hustle",
            category = EventCategory.MONEY,
            prompt = "You have an idea for a side hustle.",
            stages = WORKING,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Grind on it", "Extra cash, less sleep.", listOf(cash(3500), health(-2), happy(-1))),
                EventChoice("Keep your free time", "You valued the rest more.", listOf(happy(3))),
            ),
        ),
        LifeEvent(
            id = "phone_upgrade",
            category = EventCategory.MONEY,
            prompt = "The latest phone just dropped.",
            stages = setOf(LifeStage.TEEN, LifeStage.YOUNG_ADULT),
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Splurge", "Shiny and new.", listOf(cash(-1000), happy(3))),
                EventChoice("Keep your old one", "It still works fine.", listOf(happy(-1))),
            ),
        ),

        // ==== Crime ====================================================
        LifeEvent(
            id = "tempted_to_steal",
            category = EventCategory.CRIME,
            prompt = "You could pocket something expensive when no one's looking.",
            stages = setOf(LifeStage.TEEN, LifeStage.YOUNG_ADULT),
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Take it", "You got away — this time.", listOf(cash(120), flag("thief"), happy(-1))),
                EventChoice("Walk away", "Your conscience stayed clean.", listOf(smarts(1))),
            ),
        ),
        LifeEvent(
            id = "bar_fight",
            category = EventCategory.CRIME,
            prompt = "A stranger squares up to you at a bar.",
            stages = YOUNG,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Walk it off", "No sense in a brawl.", listOf(smarts(1))),
                EventChoice("Throw a punch", "You won, but bruised.", listOf(health(-3), flag("brawler"), happy(1))),
            ),
        ),
        flavour("speeding_again", EventCategory.CRIME, "Another speeding ticket for the collection.", listOf(cash(-150), happy(-1)), stages = WORKING, minAge = 18, oneShot = false, weight = 3),
        LifeEvent(
            id = "shady_offer",
            category = EventCategory.CRIME,
            prompt = "Someone offers you easy money to look the other way.",
            stages = WORKING,
            oneShot = false,
            weight = 3,
            condition = employed,
            choices = listOf(
                EventChoice("Take the money", "Easy cash, uneasy conscience.", listOf(cash(4000), flag("corrupt"), happy(-2))),
                EventChoice("Refuse", "You kept your integrity.", listOf(smarts(2), happy(1))),
            ),
        ),
        flavour("jury_duty", EventCategory.RANDOM, "You were called for jury duty.", listOf(smarts(1), happy(-1)), stages = WORKING, oneShot = false, weight = 3),

        // ==== Friends & life ===========================================
        LifeEvent(
            id = "made_a_friend",
            category = EventCategory.FAMILY,
            prompt = "You really hit it off with someone new.",
            minAge = 6,
            oneShot = false,
            weight = 8,
            choices = listOf(
                EventChoice("Become close friends", "A friendship for the ages.", listOf(addPerson(RelationType.FRIEND, 55), happy(4))),
                EventChoice("Stay acquaintances", "Friendly, but distant.", listOf(happy(1))),
            ),
        ),
        LifeEvent(
            id = "friend_drifts",
            category = EventCategory.FAMILY,
            prompt = "You and an old friend have been drifting apart.",
            oneShot = false,
            weight = 5,
            condition = hasFriend,
            choices = listOf(
                EventChoice("Reach out", "You picked up right where you left off.", listOf(rel(RelationType.FRIEND, 8), happy(2))),
                EventChoice("Let it fade", "Some friendships just end.", listOf(rel(RelationType.FRIEND, -15), happy(-2))),
            ),
        ),
        LifeEvent(
            id = "midlife_crisis",
            category = EventCategory.RANDOM,
            prompt = "You wake up questioning everything.",
            stages = ADULT,
            minAge = 40,
            oneShot = true,
            weight = 5,
            choices = listOf(
                EventChoice("Buy the motorcycle", "Reckless, thrilling, alive.", listOf(cash(-9000), happy(6), health(-1))),
                EventChoice("Pick up a new passion", "You found meaning in something new.", listOf(happy(4), smarts(2))),
            ),
        ),
        LifeEvent(
            id = "class_reunion",
            category = EventCategory.RANDOM,
            prompt = "Your school reunion invitation arrives.",
            stages = setOf(LifeStage.ADULT, LifeStage.SENIOR),
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Go and reconnect", "Old faces, warm memories.", listOf(happy(4), rel(RelationType.FRIEND, 4))),
                EventChoice("Skip it", "You weren't in the mood.", listOf(happy(-1))),
            ),
        ),
        flavour("learned_language", EventCategory.RANDOM, "You picked up a new language.", listOf(smarts(2), happy(2)), minAge = 10, oneShot = false, weight = 4),
        LifeEvent(
            id = "ran_marathon",
            category = EventCategory.HEALTH,
            prompt = "You signed up to run a marathon.",
            stages = WORKING,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Train and finish", "You crossed that line on top of the world.", listOf(health(5), happy(5))),
                EventChoice("Give up halfway", "The training fizzled out.", listOf(happy(-2))),
            ),
        ),
        LifeEvent(
            id = "natural_disaster",
            category = EventCategory.RANDOM,
            prompt = "A storm batters your area.",
            oneShot = false,
            weight = 3,
            minAge = 8,
            choices = listOf(
                EventChoice("Help your neighbors", "The community pulled together.", listOf(happy(4), addPerson(RelationType.FRIEND, 45))),
                EventChoice("Hunker down", "You rode it out safely.", listOf(happy(-1))),
            ),
        ),
        flavour("spiritual_awakening", EventCategory.RANDOM, "A quiet moment gave you real clarity.", listOf(happy(4), smarts(1)), stages = GROWN, oneShot = false, weight = 3),

        // ==== Senior ===================================================
        LifeEvent(
            id = "senior_travel",
            category = EventCategory.RANDOM,
            prompt = "You've always wanted to see the world.",
            stages = SENIOR,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Take the trip", "The adventure of your golden years.", listOf(cash(-4000), happy(8), health(2))),
                EventChoice("Stay comfortable", "Home has its comforts.", listOf(happy(-1))),
            ),
        ),
        flavour("old_friend_passes", EventCategory.FAMILY, "An old friend passed away. You felt the years.", listOf(happy(-4), health(-1)), stages = SENIOR, oneShot = false, weight = 4),
        flavour("share_wisdom", EventCategory.RANDOM, "A young person sought out your advice.", listOf(happy(4), smarts(1)), stages = SENIOR, oneShot = false, weight = 5),
        LifeEvent(
            id = "health_decline",
            category = EventCategory.HEALTH,
            prompt = "Your body isn't what it used to be.",
            stages = SENIOR,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Fight to stay strong", "You clawed back some vitality.", listOf(cash(-1000), health(5))),
                EventChoice("Make peace with it", "You focused on what you had.", listOf(health(-3), happy(3))),
            ),
        ),
        flavour("reflect_on_life", EventCategory.RANDOM, "You spent the year reflecting on a life well lived.", listOf(happy(5)), stages = SENIOR, oneShot = false, weight = 4),
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
