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
private fun ail(id: String) = Effect.AddAilment(id)
private val cured = Effect.CureAilments
private fun jail(years: Int) = Effect.Imprison(years)
private fun acquire(id: String) = Effect.AddAsset(id)

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
private fun hasTrait(id: String): (GameState) -> Boolean = { id in it.traits }
private fun without(ailmentId: String): (GameState) -> Boolean = { st -> st.ailments.none { it.id == ailmentId } }
private val healthy: (GameState) -> Boolean = { it.ailments.isEmpty() }
private val heir: (GameState) -> Boolean = { it.generation > 1 }
private val hasSibling: (GameState) -> Boolean =
    { st -> st.relationships.any { alive(it) && it.relation == RelationType.SIBLING } }

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

        // ==== M3: Prison (fire only while incarcerated) ================
        LifeEvent(
            id = "prison_fight",
            category = EventCategory.CRIME,
            prompt = "Another inmate squares up to you in the yard.",
            prisonOnly = true,
            oneShot = false,
            weight = 8,
            choices = listOf(
                EventChoice("Stand your ground", "You held your own — and earned some respect.", listOf(health(-4), happy(2), flag("hardened"))),
                EventChoice("Back down", "You swallowed your pride to stay safe.", listOf(happy(-3))),
            ),
        ),
        LifeEvent(
            id = "prison_parole",
            category = EventCategory.CRIME,
            prompt = "You're up for a parole hearing.",
            prisonOnly = true,
            oneShot = false,
            weight = 6,
            choices = listOf(
                EventChoice("Plead your case", "Parole granted — you walk free.", listOf(Effect.Release, happy(8))),
                EventChoice("Say nothing", "You'll serve out your time.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "prison_class",
            category = EventCategory.SCHOOL,
            prompt = "The prison offers education classes.",
            prisonOnly = true,
            oneShot = false,
            weight = 7,
            choices = listOf(
                EventChoice("Enroll", "You used the time to better yourself.", listOf(smarts(3), happy(2))),
                EventChoice("Skip them", "You let the hours slip by.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "prison_gang",
            category = EventCategory.CRIME,
            prompt = "A prison faction wants you to join for protection.",
            prisonOnly = true,
            oneShot = false,
            weight = 6,
            choices = listOf(
                EventChoice("Join them", "Safer, but you're in deeper now.", listOf(flag("gang"), happy(1), health(-1))),
                EventChoice("Stay independent", "You kept your own counsel.", listOf(smarts(1), happy(-1))),
            ),
        ),
        LifeEvent(
            id = "prison_mentor",
            category = EventCategory.FAMILY,
            prompt = "An older inmate takes you under his wing.",
            prisonOnly = true,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Listen and learn", "His hard-won wisdom stuck with you.", listOf(smarts(2), happy(2))),
                EventChoice("Keep to yourself", "You did your time alone.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "prison_riot",
            category = EventCategory.CRIME,
            prompt = "A riot erupts in the cell block.",
            prisonOnly = true,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Wait it out", "You stayed clear of the worst.", listOf(health(-1))),
                EventChoice("Get involved", "You came through it bruised.", listOf(health(-5), happy(1), flag("hardened"))),
            ),
        ),
        flavour("prison_visit", EventCategory.FAMILY, "A loved one made the long trip to visit you inside.", listOf(happy(4)), prisonOnly = true, oneShot = false, weight = 6),
        flavour("prison_lonely", EventCategory.HEALTH, "The isolation of prison wore on you.", listOf(happy(-3)), prisonOnly = true, oneShot = false, weight = 6),
        flavour("prison_gym", EventCategory.HEALTH, "You spent your days lifting weights in the yard.", listOf(health(2)), prisonOnly = true, oneShot = false, weight = 5),
        flavour("prison_reflect", EventCategory.RANDOM, "Behind bars, you did a lot of thinking about your choices.", listOf(smarts(1)), prisonOnly = true, oneShot = false, weight = 5),

        // ==== M3: Illness & health system ==============================
        LifeEvent(
            id = "early_diagnosis",
            category = EventCategory.HEALTH,
            prompt = "A check-up turns up the early warning signs of diabetes.",
            stages = GROWN,
            minAge = 35,
            oneShot = false,
            weight = 6,
            condition = without("diabetes"),
            choices = listOf(
                EventChoice("Change your habits now", "You got ahead of it.", listOf(health(3), happy(-1))),
                EventChoice("Brush it off", "You'll deal with it later — or so you thought.", listOf(ail("diabetes"))),
            ),
        ),
        LifeEvent(
            id = "persistent_cough",
            category = EventCategory.HEALTH,
            prompt = "A cough has lingered for months.",
            stages = setOf(LifeStage.ADULT, LifeStage.SENIOR),
            minAge = 45,
            oneShot = false,
            weight = 5,
            condition = without("copd"),
            choices = listOf(
                EventChoice("Get it checked", "Caught early, it was manageable.", listOf(health(1), cash(-200))),
                EventChoice("Ignore it", "It settled into something chronic.", listOf(ail("copd"))),
            ),
        ),
        LifeEvent(
            id = "feeling_hollow",
            category = EventCategory.HEALTH,
            prompt = "You've felt hollow and low for a long stretch.",
            stages = GROWN,
            oneShot = false,
            weight = 6,
            condition = without("depression"),
            choices = listOf(
                EventChoice("Reach out for help", "Talking to someone lifted the fog.", listOf(happy(4))),
                EventChoice("Keep it inside", "It quietly took root.", listOf(ail("depression"), happy(-3))),
            ),
        ),
        LifeEvent(
            id = "lump_scare",
            category = EventCategory.HEALTH,
            prompt = "You find a lump and fear the worst.",
            stages = GROWN,
            minAge = 30,
            oneShot = false,
            weight = 4,
            condition = without("cancer"),
            choices = listOf(
                EventChoice("See a specialist now", "A scare, but you caught it in time.", listOf(cash(-500), health(1))),
                EventChoice("Wait and see", "The delay let it grow.", listOf(ail("cancer"))),
            ),
        ),
        LifeEvent(
            id = "experimental_cure",
            category = EventCategory.HEALTH,
            prompt = "A new treatment could clear your condition — for a price.",
            oneShot = false,
            weight = 6,
            condition = { it.ailments.isNotEmpty() },
            choices = listOf(
                EventChoice("Pay for it", "The treatment worked — you're in the clear.", listOf(cash(-5000), cured)),
                EventChoice("Can't justify the cost", "You'll keep managing on your own.", listOf(happy(-2))),
            ),
        ),
        LifeEvent(
            id = "chronic_support",
            category = EventCategory.HEALTH,
            prompt = "Living with a chronic condition is wearing you down.",
            oneShot = false,
            weight = 5,
            condition = { st -> st.ailments.any { it.chronic } },
            choices = listOf(
                EventChoice("Join a support group", "Others who understood made it lighter.", listOf(happy(4))),
                EventChoice("Go it alone", "The weight of it grew heavier.", listOf(happy(-3))),
            ),
        ),
        LifeEvent(
            id = "quit_bad_habit",
            category = EventCategory.HEALTH,
            prompt = "You've been meaning to kick a bad habit.",
            stages = GROWN,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Quit for good", "Hard, but your body thanked you.", listOf(health(4), happy(-1), flag("clean_living"))),
                EventChoice("Not this year", "The habit kept its grip.", listOf(health(-2))),
            ),
        ),
        LifeEvent(
            id = "dental_work",
            category = EventCategory.HEALTH,
            prompt = "Your teeth need serious work.",
            stages = GROWN,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Get it fixed", "Pricey, but your smile came back.", listOf(cash(-1200), looks(2), health(1))),
                EventChoice("Live with it", "You winced through the year.", listOf(happy(-1), looks(-1))),
            ),
        ),
        flavour("flu_season", EventCategory.HEALTH, "A brutal flu swept through and knocked you down.", listOf(ail("bad_cold")), minAge = 3, oneShot = false, weight = 5),
        flavour("bad_fall", EventCategory.HEALTH, "You slipped badly and broke a bone.", listOf(ail("broken_bone")), minAge = 6, oneShot = false, weight = 4),
        flavour("clean_bill", EventCategory.HEALTH, "A year of clean living did your body real good.", listOf(health(4)), stages = GROWN, oneShot = false, weight = 5, condition = healthy),
        flavour("blood_donor", EventCategory.HEALTH, "You became a regular blood donor.", listOf(happy(2), smarts(1)), minAge = 18, oneShot = false, weight = 3),

        // ==== M3: Crime & legal (fixed-consequence choices) ============
        LifeEvent(
            id = "heist_offer",
            category = EventCategory.CRIME,
            prompt = "An old contact offers you a cut of a robbery.",
            stages = WORKING,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Get involved", "Easy money — but you're in the life now.", listOf(cash(5000), flag("criminal"), happy(1))),
                EventChoice("Walk away", "Not worth what it would cost you.", listOf(smarts(1))),
            ),
        ),
        LifeEvent(
            id = "drunk_driving",
            category = EventCategory.CRIME,
            prompt = "You've had a few drinks, but your car is right there.",
            stages = GROWN,
            minAge = 18,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Risk the drive", "You were pulled over and arrested for a DUI.", listOf(jail(1), happy(-4))),
                EventChoice("Call a cab", "The smart call cost you a little cash.", listOf(cash(-40))),
            ),
        ),
        LifeEvent(
            id = "bar_brawl2",
            category = EventCategory.CRIME,
            prompt = "A shoving match at a bar turns ugly.",
            stages = GROWN,
            minAge = 18,
            oneShot = false,
            weight = 3,
            choices = listOf(
                EventChoice("Throw the first punch", "You were arrested for assault.", listOf(jail(1), happy(-2))),
                EventChoice("Walk it off", "You kept your cool and your record.", listOf(smarts(1), happy(1))),
            ),
        ),
        LifeEvent(
            id = "tax_fraud",
            category = EventCategory.CRIME,
            prompt = "You could quietly fudge your taxes for a fat refund.",
            stages = WORKING,
            oneShot = false,
            weight = 4,
            condition = employed,
            choices = listOf(
                EventChoice("Cheat the system", "The refund landed — so did the risk.", listOf(cash(3000), flag("corrupt"))),
                EventChoice("File honestly", "You slept easy.", listOf(smarts(1))),
            ),
        ),
        LifeEvent(
            id = "witness_mugging",
            category = EventCategory.CRIME,
            prompt = "You witness a mugging on your street.",
            minAge = 12,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Step in", "Reckless, but you helped.", listOf(health(-2), happy(3), flag("brave"))),
                EventChoice("Call for help", "You did the sensible thing.", listOf(smarts(1))),
                EventChoice("Look away", "It gnawed at your conscience.", listOf(happy(-2))),
            ),
        ),
        LifeEvent(
            id = "counterfeit_flip",
            category = EventCategory.CRIME,
            prompt = "Someone's selling knock-off goods cheap to resell.",
            stages = WORKING,
            oneShot = false,
            weight = 3,
            choices = listOf(
                EventChoice("Buy in to flip", "A tidy, shady little profit.", listOf(cash(2000), flag("criminal"))),
                EventChoice("Not worth it", "You passed on the hustle.", listOf(smarts(1))),
            ),
        ),
        LifeEvent(
            id = "police_stop",
            category = EventCategory.CRIME,
            prompt = "Police stop and question you.",
            minAge = 16,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Stay calm and cooperate", "It was over in minutes.", listOf(smarts(1))),
                EventChoice("Get mouthy", "You made it worse than it needed to be.", listOf(happy(-2))),
            ),
        ),
        LifeEvent(
            id = "mall_shoplifting",
            category = EventCategory.CRIME,
            prompt = "Your friends are pocketing things at the mall.",
            stages = TEEN,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Join in", "A cheap thrill and a guilty conscience.", listOf(cash(30), flag("shoplifted"), happy(1))),
                EventChoice("Nope out", "You weren't about to risk it.", listOf(smarts(1))),
            ),
        ),
        LifeEvent(
            id = "underpass_tag",
            category = EventCategory.CRIME,
            prompt = "The crew wants to spray-paint the underpass.",
            stages = TEEN,
            oneShot = false,
            weight = 3,
            choices = listOf(
                EventChoice("Grab a can", "Your art hit the wall — and stayed up.", listOf(happy(2), flag("vandal"))),
                EventChoice("Head home", "You left them to it.", listOf(smarts(1))),
            ),
        ),
        LifeEvent(
            id = "insurance_scam",
            category = EventCategory.CRIME,
            prompt = "You could stage a claim for an easy payout.",
            stages = ADULT,
            oneShot = false,
            weight = 3,
            choices = listOf(
                EventChoice("Fake the claim", "The cheque cleared. So did your morals.", listOf(cash(4000), flag("corrupt"))),
                EventChoice("Don't risk it", "You kept it clean.", listOf(smarts(1), happy(1))),
            ),
        ),

        // ==== M3: Asset windfalls ======================================
        flavour("inherited_home", EventCategory.MONEY, "A relative passed and left you their condo.", listOf(acquire("condo")), stages = setOf(LifeStage.ADULT, LifeStage.SENIOR), oneShot = true, weight = 4),
        flavour("won_car", EventCategory.MONEY, "You won a car in a charity raffle!", listOf(acquire("used_car"), happy(3)), stages = WORKING, minAge = 18, oneShot = true, weight = 3),
        flavour("gifted_watch", EventCategory.MONEY, "A wealthy relative gifted you a luxury watch.", listOf(acquire("watch"), happy(3)), stages = GROWN, minAge = 21, oneShot = true, weight = 2),

        // ==== M3: Trait-flavoured ======================================
        flavour("trait_genius_scholar", EventCategory.SCHOOL, "Your sharp mind won you a scholarship.", listOf(smarts(3), cash(2000)), minAge = 14, oneShot = false, weight = 5, condition = hasTrait("genius")),
        flavour("trait_athletic_meet", EventCategory.HEALTH, "You dominated a local athletics meet.", listOf(health(3), happy(3)), minAge = 8, oneShot = false, weight = 5, condition = hasTrait("athletic")),
        flavour("trait_charismatic_room", EventCategory.RANDOM, "Your charm won over a tough room.", listOf(happy(3), looks(1)), minAge = 10, oneShot = false, weight = 5, condition = hasTrait("charismatic")),
        flavour("trait_kind_returned", EventCategory.RANDOM, "A stranger repaid a kindness you'd long forgotten.", listOf(happy(4)), minAge = 8, oneShot = false, weight = 5, condition = hasTrait("kind")),
        flavour("trait_hot_headed_cost", EventCategory.FAMILY, "Your temper flared and cost you a friend.", listOf(rel(RelationType.FRIEND, -8), happy(-2)), oneShot = false, weight = 5, condition = both(hasTrait("hot_headed"), hasFriend)),
        flavour("trait_frail_setback", EventCategory.HEALTH, "Your fragile health laid you up again.", listOf(health(-3)), minAge = 5, oneShot = false, weight = 5, condition = hasTrait("frail")),
        flavour("trait_lucky_raffle", EventCategory.MONEY, "Lady Luck smiled — you won a raffle.", listOf(cash(1500), happy(2)), minAge = 8, oneShot = false, weight = 5, condition = hasTrait("lucky")),
        LifeEvent(
            id = "trait_ambitious_push",
            category = EventCategory.WORK,
            prompt = "Your ambition points you at a bold play at work.",
            stages = WORKING,
            oneShot = false,
            weight = 5,
            condition = both(hasTrait("ambitious"), employed),
            choices = listOf(
                EventChoice("Gun for the top", "You clawed your way up a rung.", listOf(promoteJob, happy(-1))),
                EventChoice("Steady as she goes", "You protected your peace.", listOf(happy(2))),
            ),
        ),
        LifeEvent(
            id = "trait_genius_invent",
            category = EventCategory.MONEY,
            prompt = "A clever idea of yours could actually become something.",
            minAge = 25,
            oneShot = false,
            weight = 4,
            condition = hasTrait("genius"),
            choices = listOf(
                EventChoice("Develop it", "It paid off in more ways than one.", listOf(cash(6000), smarts(2), happy(2))),
                EventChoice("Let it go", "The idea faded away.", listOf(happy(-1))),
            ),
        ),

        // ==== M3: Money ================================================
        LifeEvent(
            id = "lottery_windfall",
            category = EventCategory.MONEY,
            prompt = "Your scratch card is a big winner!",
            minAge = 18,
            oneShot = true,
            weight = 3,
            choices = listOf(
                EventChoice("Take the cash", "A life-changing sum, just like that.", listOf(cash(50000), happy(5))),
                EventChoice("Give most away", "You shared your luck around.", listOf(cash(5000), happy(7), flag("philanthropist"))),
            ),
        ),
        LifeEvent(
            id = "freelance_gig",
            category = EventCategory.MONEY,
            prompt = "A freelance gig lands in your lap.",
            stages = WORKING,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Take it on", "Extra cash for extra hours.", listOf(cash(2500), health(-1))),
                EventChoice("Pass — you're busy", "You chose your sanity.", listOf(happy(2))),
            ),
        ),
        flavour("market_dip", EventCategory.MONEY, "A market downturn dented your savings.", listOf(cash(-2000)), stages = WORKING, oneShot = false, weight = 3),
        flavour("unexpected_bill", EventCategory.MONEY, "An unexpected bill blindsided you.", listOf(cash(-800), happy(-1)), minAge = 18, oneShot = false, weight = 4),

        // ==== M3: General breadth — childhood ==========================
        LifeEvent(
            id = "lemonade_stand",
            category = EventCategory.MONEY,
            prompt = "You set up a lemonade stand on the corner.",
            stages = CHILD,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Hustle all summer", "Your first taste of enterprise.", listOf(cash(30), smarts(1), happy(1))),
                EventChoice("Give it away free", "The neighborhood loved you for it.", listOf(happy(3), rel(RelationType.MOTHER, 1))),
            ),
        ),
        LifeEvent(
            id = "science_fair",
            category = EventCategory.SCHOOL,
            prompt = "It's the school science fair.",
            stages = CHILD,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Go all out", "Your volcano stole the show.", listOf(smarts(3), happy(1))),
                EventChoice("Phone it in", "You did the bare minimum.", listOf(happy(1))),
            ),
        ),
        LifeEvent(
            id = "broke_window",
            category = EventCategory.FAMILY,
            prompt = "You broke a neighbor's window with a stray ball.",
            stages = CHILD,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Own up to it", "Honesty earned you respect.", listOf(rel(RelationType.MOTHER, 2), smarts(1))),
                EventChoice("Run and hide", "The guilt followed you home.", listOf(happy(-2))),
            ),
        ),
        LifeEvent(
            id = "class_clown",
            category = EventCategory.SCHOOL,
            prompt = "You could be the class clown this year.",
            stages = CHILD,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Ham it up", "You had them rolling — and the teacher fuming.", listOf(happy(3), smarts(-1))),
                EventChoice("Focus in class", "You kept your head in your books.", listOf(smarts(2))),
            ),
        ),
        flavour("lost_tooth", EventCategory.FAMILY, "You lost a tooth, and the tooth fairy came through.", listOf(happy(2), cash(2)), stages = CHILD, oneShot = false, weight = 5),
        flavour("summer_camp", EventCategory.FAMILY, "You went to summer camp and made memories.", listOf(happy(4), health(1)), stages = CHILD, oneShot = false, weight = 5),
        flavour("learned_swim", EventCategory.HEALTH, "You finally learned to swim.", listOf(health(2), happy(2)), stages = CHILD, oneShot = false, weight = 4),
        flavour("childhood_pet_passed", EventCategory.FAMILY, "A beloved childhood pet passed away.", listOf(happy(-4)), stages = CHILD, oneShot = false, weight = 3),

        // ==== General breadth — teen ===================================
        LifeEvent(
            id = "driving_test",
            category = EventCategory.RANDOM,
            prompt = "It's time for your driving test.",
            stages = TEEN,
            minAge = 16,
            oneShot = true,
            weight = 6,
            choices = listOf(
                EventChoice("Nail it", "Licence in hand — freedom!", listOf(happy(4), flag("licensed"))),
                EventChoice("Choke under pressure", "You'll try again next year.", listOf(happy(-2))),
            ),
        ),
        LifeEvent(
            id = "prom_night",
            category = EventCategory.ROMANCE,
            prompt = "Prom is coming up.",
            stages = TEEN,
            minAge = 16,
            oneShot = true,
            weight = 5,
            choices = listOf(
                EventChoice("Go all out", "A night you'd remember for decades.", listOf(cash(-300), happy(6))),
                EventChoice("Skip it", "You told yourself you didn't care.", listOf(happy(-2), cash(50))),
            ),
        ),
        LifeEvent(
            id = "peer_pressure",
            category = EventCategory.FAMILY,
            prompt = "Your friends pressure you to try something risky.",
            stages = TEEN,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Give in", "It was a thrill you half-regretted.", listOf(happy(2), health(-3), flag("reckless"))),
                EventChoice("Stand firm", "You held your line.", listOf(smarts(1))),
            ),
        ),
        LifeEvent(
            id = "part_time_gig",
            category = EventCategory.MONEY,
            prompt = "A neighbor offers you odd jobs for cash.",
            stages = TEEN,
            minAge = 15,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Take the work", "You earned your own money.", listOf(cash(600), happy(-1))),
                EventChoice("Enjoy the summer", "You spent it with friends instead.", listOf(happy(2))),
            ),
        ),
        LifeEvent(
            id = "online_fame",
            category = EventCategory.RANDOM,
            prompt = "A post of yours starts going viral.",
            stages = TEEN,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Chase the clout", "Fifteen minutes of fame.", listOf(happy(3), looks(1))),
                EventChoice("Log off", "You kept your feet on the ground.", listOf(smarts(1), happy(1))),
            ),
        ),
        flavour("first_heartbreak", EventCategory.ROMANCE, "Your first real heartbreak hit hard.", listOf(happy(-5)), stages = TEEN, oneShot = false, weight = 5, condition = single),
        flavour("band_tryout", EventCategory.SCHOOL, "You joined the school band.", listOf(happy(3), smarts(1)), stages = TEEN, oneShot = false, weight = 4),
        flavour("teen_detention", EventCategory.SCHOOL, "You landed yourself in detention.", listOf(happy(-2), smarts(1)), stages = TEEN, oneShot = false, weight = 3),

        // ==== General breadth — young adult ============================
        LifeEvent(
            id = "first_apartment",
            category = EventCategory.MONEY,
            prompt = "You could finally move into your own place.",
            stages = YOUNG,
            minAge = 18,
            oneShot = true,
            weight = 6,
            choices = listOf(
                EventChoice("Get your own place", "Freedom — and rent.", listOf(cash(-2000), happy(5))),
                EventChoice("Stay home to save", "Not glamorous, but sensible.", listOf(rel(RelationType.MOTHER, 2), cash(500))),
            ),
        ),
        LifeEvent(
            id = "backpacking",
            category = EventCategory.RANDOM,
            prompt = "Friends invite you backpacking abroad.",
            stages = YOUNG,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("See the world", "You came back changed.", listOf(cash(-2500), happy(8), smarts(2))),
                EventChoice("Stay and work", "You banked the money instead.", listOf(cash(1500))),
            ),
        ),
        LifeEvent(
            id = "roommate_trouble",
            category = EventCategory.FAMILY,
            prompt = "Your roommate stopped paying their share of the rent.",
            stages = YOUNG,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Cover it, then talk", "You kept the peace, at a cost.", listOf(cash(-800), happy(-1))),
                EventChoice("Kick them out", "Awkward, but fair.", listOf(happy(-2), smarts(1))),
            ),
        ),
        LifeEvent(
            id = "startup_dream",
            category = EventCategory.MONEY,
            prompt = "You have an idea for a startup.",
            stages = YOUNG,
            minAge = 20,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Bet on yourself", "Terrifying, exhilarating, all-in.", listOf(cash(-3000), happy(2), smarts(2), flag("founder"))),
                EventChoice("Play it safe", "Maybe someday.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "quarter_life",
            category = EventCategory.RANDOM,
            prompt = "You're questioning your whole path in life.",
            stages = YOUNG,
            minAge = 24,
            oneShot = true,
            weight = 4,
            choices = listOf(
                EventChoice("Reinvent yourself", "You found a new direction.", listOf(happy(3), smarts(1))),
                EventChoice("Push through", "You put your head down.", listOf(happy(-1))),
            ),
        ),
        flavour("adopt_kitten", EventCategory.FAMILY, "You adopted a kitten from the shelter.", listOf(addPerson(RelationType.PET, 70), happy(4)), stages = YOUNG, oneShot = false, weight = 4),
        flavour("networking_win", EventCategory.WORK, "A networking event opened new doors.", listOf(smarts(1), happy(1), addPerson(RelationType.FRIEND, 45)), stages = YOUNG, oneShot = false, weight = 4),
        flavour("gym_habit", EventCategory.HEALTH, "You finally stuck to a gym routine.", listOf(health(3), looks(2)), stages = YOUNG, oneShot = false, weight = 4),

        // ==== General breadth — adult ==================================
        LifeEvent(
            id = "neighbor_feud",
            category = EventCategory.FAMILY,
            prompt = "A feud with a neighbor is escalating.",
            stages = ADULT,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Make peace", "You extended an olive branch.", listOf(happy(2), smarts(1))),
                EventChoice("Dig in", "The cold war dragged on.", listOf(happy(-3))),
            ),
        ),
        LifeEvent(
            id = "home_renovation",
            category = EventCategory.MONEY,
            prompt = "Your home could use a renovation.",
            stages = ADULT,
            oneShot = false,
            weight = 4,
            condition = { "homeowner" in it.flags },
            choices = listOf(
                EventChoice("Renovate", "The place feels new again.", listOf(cash(-8000), happy(4))),
                EventChoice("Leave it", "You learned to live with the creaks.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "invest_market",
            category = EventCategory.MONEY,
            prompt = "A friend pushes you toward a hot investment.",
            stages = ADULT,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Invest wisely", "A patient bet that paid off.", listOf(cash(2000), smarts(1))),
                EventChoice("Chase the hype", "The fad fizzled and took your money.", listOf(cash(-3000), happy(-2))),
            ),
        ),
        LifeEvent(
            id = "parent_needs_care",
            category = EventCategory.FAMILY,
            prompt = "An aging parent needs care.",
            stages = ADULT,
            oneShot = false,
            weight = 5,
            condition = { st -> st.relationships.any { (it.relation == RelationType.MOTHER || it.relation == RelationType.FATHER) && it.alive } },
            choices = listOf(
                EventChoice("Take them in", "Hard work, but the right thing.", listOf(cash(-2000), rel(RelationType.MOTHER, 8), rel(RelationType.FATHER, 8), happy(-1))),
                EventChoice("Arrange care", "You made sure they were looked after.", listOf(cash(-4000), rel(RelationType.MOTHER, 3), rel(RelationType.FATHER, 3))),
            ),
        ),
        LifeEvent(
            id = "family_vacation",
            category = EventCategory.FAMILY,
            prompt = "The family is itching for a big vacation.",
            stages = ADULT,
            oneShot = false,
            weight = 5,
            condition = hasChild,
            choices = listOf(
                EventChoice("Splurge on the trip", "Memories worth every penny.", listOf(cash(-4000), happy(6), rel(RelationType.CHILD, 4))),
                EventChoice("Keep it modest", "A quieter break, but a break.", listOf(cash(-800), happy(2))),
            ),
        ),
        LifeEvent(
            id = "debt_trouble",
            category = EventCategory.MONEY,
            prompt = "The debts are piling up.",
            stages = ADULT,
            oneShot = false,
            weight = 5,
            condition = { it.character.money < 0 },
            choices = listOf(
                EventChoice("Buckle down", "You clawed your way back toward the black.", listOf(cash(1000), happy(-2))),
                EventChoice("Ignore it", "The hole only got deeper.", listOf(happy(-4))),
            ),
        ),
        flavour("work_conference", EventCategory.WORK, "A work conference took you somewhere new.", listOf(smarts(2), happy(1)), stages = ADULT, oneShot = false, weight = 4, condition = employed),
        flavour("kids_recital", EventCategory.FAMILY, "You beamed through your child's recital.", listOf(happy(4), rel(RelationType.CHILD, 3)), stages = ADULT, oneShot = false, weight = 5, condition = hasChild),
        flavour("industry_award", EventCategory.WORK, "Your industry recognized your work.", listOf(happy(4), smarts(1)), stages = ADULT, oneShot = false, weight = 3, condition = employed),

        // ==== General breadth — senior =================================
        LifeEvent(
            id = "bucket_list",
            category = EventCategory.RANDOM,
            prompt = "There's still one thing left on your bucket list.",
            stages = SENIOR,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Chase the dream", "You did the thing — at last.", listOf(cash(-3000), happy(7))),
                EventChoice("Stay content", "You were at peace as you were.", listOf(happy(1))),
            ),
        ),
        LifeEvent(
            id = "estate_planning",
            category = EventCategory.MONEY,
            prompt = "It's time to sort out your estate.",
            stages = SENIOR,
            oneShot = true,
            weight = 4,
            choices = listOf(
                EventChoice("Plan carefully", "Your affairs are in order.", listOf(smarts(2), happy(2), flag("estate_planned"))),
                EventChoice("Put it off", "A worry you kept pushing away.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "old_flame_senior",
            category = EventCategory.ROMANCE,
            prompt = "An old flame from decades ago reaches out.",
            stages = SENIOR,
            oneShot = false,
            weight = 3,
            condition = single,
            choices = listOf(
                EventChoice("Rekindle it", "Love, the second time around.", listOf(addPerson(RelationType.PARTNER, 55), happy(6))),
                EventChoice("Cherish the memory", "Some things are best remembered.", listOf(happy(2))),
            ),
        ),
        LifeEvent(
            id = "fall_at_home",
            category = EventCategory.HEALTH,
            prompt = "You take a bad fall at home.",
            stages = SENIOR,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("See a doctor", "Better safe than sorry.", listOf(cash(-500), health(2))),
                EventChoice("Shake it off", "You toughed it out — unwisely.", listOf(health(-5))),
            ),
        ),
        flavour("grandchild_born", EventCategory.FAMILY, "You became a grandparent!", listOf(happy(8)), stages = SENIOR, oneShot = false, weight = 6, condition = hasChild),
        flavour("senior_mentor", EventCategory.RANDOM, "You mentored young people in your community.", listOf(happy(4), smarts(1)), stages = SENIOR, oneShot = false, weight = 4),
        flavour("garden_prize", EventCategory.RANDOM, "Your garden won a neighborhood prize.", listOf(happy(4)), stages = SENIOR, oneShot = false, weight = 3),
        flavour("memoir_published", EventCategory.RANDOM, "A small press published your memoir.", listOf(happy(5), cash(1000)), stages = SENIOR, oneShot = true, weight = 3),

        // ==== General breadth — cross-stage ============================
        LifeEvent(
            id = "new_neighbor",
            category = EventCategory.FAMILY,
            prompt = "New neighbors just moved in next door.",
            minAge = 12,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Introduce yourself", "The start of a good friendship.", listOf(addPerson(RelationType.FRIEND, 45), happy(2))),
                EventChoice("Keep to yourself", "You stayed a stranger.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "reconnect_sibling",
            category = EventCategory.FAMILY,
            prompt = "You've grown distant from a sibling.",
            oneShot = false,
            weight = 4,
            condition = { st -> st.relationships.any { it.relation == RelationType.SIBLING && it.alive } },
            choices = listOf(
                EventChoice("Reach out", "You picked up where you left off.", listOf(rel(RelationType.SIBLING, 8), happy(3))),
                EventChoice("Let it be", "The gap quietly widened.", listOf(rel(RelationType.SIBLING, -4))),
            ),
        ),
        LifeEvent(
            id = "act_of_kindness",
            category = EventCategory.RANDOM,
            prompt = "You come across someone who's clearly in need.",
            minAge = 10,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Help generously", "It cost you, but it was worth it.", listOf(cash(-200), happy(4))),
                EventChoice("Give what you can", "Every little bit helped.", listOf(cash(-20), happy(2))),
                EventChoice("Walk on by", "You told yourself it wasn't your problem.", listOf(happy(-1))),
            ),
        ),
        flavour("found_talent", EventCategory.RANDOM, "You discovered a hidden talent.", listOf(happy(3), smarts(1)), minAge = 8, oneShot = false, weight = 4),
        flavour("learned_instrument", EventCategory.RANDOM, "You picked up a musical instrument.", listOf(happy(3), smarts(1)), minAge = 8, oneShot = false, weight = 3),
        flavour("power_outage", EventCategory.FAMILY, "A long power outage turned into cozy family nights.", listOf(happy(2)), minAge = 6, oneShot = false, weight = 3),
        flavour("community_honor", EventCategory.RANDOM, "Your community honored your contributions.", listOf(happy(4)), minAge = 18, oneShot = false, weight = 3),
        flavour("hard_year", EventCategory.RANDOM, "A hard year tested you — and you made it through.", listOf(happy(2), smarts(1)), minAge = 10, oneShot = false, weight = 3),

        // ==== M4: Bloodline (fire only for an inherited generation) ====
        flavour("family_legacy", EventCategory.RANDOM, "You feel the quiet weight — and pride — of carrying the family name.", listOf(happy(2), smarts(1)), minAge = 10, oneShot = false, weight = 5, condition = heir),
        flavour("visit_grave", EventCategory.FAMILY, "You visited your late parent's grave and made your peace.", listOf(happy(3)), minAge = 8, oneShot = false, weight = 5, condition = heir),
        LifeEvent(
            id = "late_parent_friend",
            category = EventCategory.FAMILY,
            prompt = "An old friend of your late parent tracks you down.",
            minAge = 12,
            oneShot = false,
            weight = 5,
            condition = heir,
            choices = listOf(
                EventChoice("Hear their stories", "They shared a side of your parent you'd never known.", listOf(happy(4), addPerson(RelationType.FRIEND, 50))),
                EventChoice("Keep your distance", "Some memories you'd rather keep your own.", listOf(happy(-1))),
            ),
        ),
        LifeEvent(
            id = "family_heirloom",
            category = EventCategory.MONEY,
            prompt = "Among your late parent's things is a valuable heirloom.",
            minAge = 16,
            oneShot = true,
            weight = 6,
            condition = heir,
            choices = listOf(
                EventChoice("Treasure it", "It's priceless to you now.", listOf(acquire("watch"), happy(4))),
                EventChoice("Sell it", "You could use the money more than the memory.", listOf(cash(1200))),
            ),
        ),
        LifeEvent(
            id = "parents_footsteps",
            category = EventCategory.WORK,
            prompt = "People keep saying you're just like your late parent.",
            stages = WORKING,
            oneShot = false,
            weight = 5,
            condition = both(heir, employed),
            choices = listOf(
                EventChoice("Honor their legacy", "You wear the comparison with pride.", listOf(happy(3), smarts(1))),
                EventChoice("Forge your own path", "You're your own person, and you proved it.", listOf(happy(2), flag("own_path"))),
            ),
        ),

        // ==== M4: Siblings =============================================
        flavour("sibling_wedding", EventCategory.FAMILY, "You celebrated at your sibling's wedding.", listOf(happy(4), rel(RelationType.SIBLING, 4)), stages = GROWN, oneShot = false, weight = 5, condition = hasSibling),
        LifeEvent(
            id = "sibling_favor",
            category = EventCategory.FAMILY,
            prompt = "Your sibling is in a tight spot and asks for a real favor.",
            oneShot = false,
            weight = 5,
            condition = hasSibling,
            choices = listOf(
                EventChoice("Bail them out", "Family is family. They never forgot it.", listOf(cash(-1000), rel(RelationType.SIBLING, 10))),
                EventChoice("You can't this time", "It strained things between you.", listOf(rel(RelationType.SIBLING, -5))),
            ),
        ),
        LifeEvent(
            id = "sibling_rivalry",
            category = EventCategory.FAMILY,
            prompt = "An old rivalry with your sibling flares back up.",
            oneShot = false,
            weight = 4,
            condition = hasSibling,
            choices = listOf(
                EventChoice("Let it go", "You chose peace over winning.", listOf(rel(RelationType.SIBLING, 6), happy(-1))),
                EventChoice("Prove them wrong", "Satisfying — and a little petty.", listOf(rel(RelationType.SIBLING, -5), happy(2))),
            ),
        ),

        // ==== M4: General breadth ======================================
        LifeEvent(
            id = "volunteer_work",
            category = EventCategory.RANDOM,
            prompt = "A local cause is looking for volunteers.",
            minAge = 14,
            oneShot = false,
            weight = 5,
            choices = listOf(
                EventChoice("Give your time", "Giving back left you fuller than you started.", listOf(happy(4), smarts(1))),
                EventChoice("Maybe another year", "You had your own plate to clear.", listOf(happy(-1))),
            ),
        ),
        flavour("learn_to_cook", EventCategory.HEALTH, "You taught yourself to cook properly.", listOf(health(2), happy(2)), minAge = 12, oneShot = false, weight = 4),
        LifeEvent(
            id = "bad_landlord",
            category = EventCategory.MONEY,
            prompt = "Your landlord won't fix the broken heating.",
            stages = WORKING,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Stand your ground", "You pushed back and finally got results.", listOf(happy(1), smarts(1), flag("stubborn"))),
                EventChoice("Just endure it", "A cold, miserable winter.", listOf(happy(-2), health(-2))),
            ),
        ),
        LifeEvent(
            id = "pet_illness",
            category = EventCategory.FAMILY,
            prompt = "Your pet has fallen ill.",
            oneShot = false,
            weight = 5,
            condition = { st -> st.relationships.any { it.alive && it.relation == RelationType.PET } },
            choices = listOf(
                EventChoice("Pay for the vet", "Worth every cent to see them bounce back.", listOf(cash(-800), happy(3))),
                EventChoice("Hope it passes", "You agonized over the choice.", listOf(happy(-4))),
            ),
        ),
        LifeEvent(
            id = "relocate_promotion",
            category = EventCategory.WORK,
            prompt = "A promotion is yours — if you relocate across the country.",
            stages = WORKING,
            oneShot = false,
            weight = 5,
            condition = employed,
            choices = listOf(
                EventChoice("Take the leap", "A bigger role in a brand-new city.", listOf(promoteJob, happy(-2))),
                EventChoice("Stay rooted", "Home was worth more than the title.", listOf(happy(3))),
            ),
        ),
        LifeEvent(
            id = "identity_theft",
            category = EventCategory.CRIME,
            prompt = "Someone has been using your identity.",
            minAge = 20,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Fight it head-on", "A stressful slog, but you cleared your name.", listOf(cash(-500), smarts(1))),
                EventChoice("Ignore the letters", "It spiraled while you looked away.", listOf(cash(-2500), happy(-3))),
            ),
        ),
        LifeEvent(
            id = "insomnia",
            category = EventCategory.HEALTH,
            prompt = "You can't sleep, night after night.",
            stages = GROWN,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("See a doctor", "A little help, and rest returned.", listOf(cash(-200), health(3))),
                EventChoice("Power through", "The exhaustion wore you down.", listOf(health(-3), happy(-2))),
            ),
        ),
        flavour("won_bet", EventCategory.MONEY, "A friendly bet went your way.", listOf(cash(400), happy(2)), minAge = 18, stages = GROWN, oneShot = false, weight = 3),
        LifeEvent(
            id = "car_breakdown",
            category = EventCategory.MONEY,
            prompt = "Your car dies on the side of the highway.",
            stages = WORKING,
            minAge = 18,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Get it repaired", "Painful bill, but you're back on the road.", listOf(cash(-1500), happy(-1))),
                EventChoice("Go without a car", "You made do — slowly.", listOf(happy(-2), health(-1))),
            ),
        ),
        LifeEvent(
            id = "work_mentor",
            category = EventCategory.WORK,
            prompt = "A respected colleague offers to mentor you.",
            stages = WORKING,
            oneShot = false,
            weight = 5,
            condition = employed,
            choices = listOf(
                EventChoice("Soak it up", "Their guidance sharpened you.", listOf(smarts(2), happy(2))),
                EventChoice("Go it alone", "You learned your own way, slower.", listOf(smarts(1))),
            ),
        ),
        flavour("community_garden", EventCategory.RANDOM, "You planted a plot in the community garden.", listOf(happy(3), health(1)), stages = GROWN, oneShot = false, weight = 4),
        flavour("art_class", EventCategory.RANDOM, "You took an art class and surprised yourself.", listOf(happy(3), smarts(1)), minAge = 10, oneShot = false, weight = 4),
        LifeEvent(
            id = "stand_up_bully",
            category = EventCategory.RANDOM,
            prompt = "You see someone being bullied in public.",
            minAge = 12,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Step in", "You did the right thing, nerves and all.", listOf(happy(3), flag("brave"))),
                EventChoice("Look away", "It sat wrong with you all day.", listOf(happy(-2))),
            ),
        ),
        LifeEvent(
            id = "charity_gala",
            category = EventCategory.MONEY,
            prompt = "You're invited to a black-tie charity gala.",
            stages = ADULT,
            oneShot = false,
            weight = 3,
            choices = listOf(
                EventChoice("Donate generously", "Your name headed the donor list.", listOf(cash(-2000), happy(4), flag("philanthropist"))),
                EventChoice("Just enjoy the night", "A fine evening, a modest cheque.", listOf(cash(-200), happy(2))),
            ),
        ),
        flavour("family_reunion", EventCategory.FAMILY, "A big family reunion brought everyone together.", listOf(happy(4)), stages = GROWN, oneShot = false, weight = 4),
        flavour("budget_smart", EventCategory.MONEY, "You finally learned to budget properly.", listOf(smarts(1), cash(500)), stages = WORKING, oneShot = false, weight = 4),
        flavour("jog_habit", EventCategory.HEALTH, "You built a running habit that stuck.", listOf(health(3), looks(1)), minAge = 16, oneShot = false, weight = 4),
        LifeEvent(
            id = "coding_bootcamp",
            category = EventCategory.SCHOOL,
            prompt = "A coding bootcamp could open new doors — for a price.",
            stages = WORKING,
            minAge = 18,
            oneShot = false,
            weight = 4,
            choices = listOf(
                EventChoice("Enroll", "Intense weeks, but a whole new skill set.", listOf(cash(-2000), smarts(4), flag("self_taught"))),
                EventChoice("Not right now", "You filed it under 'someday'.", listOf(happy(-1))),
            ),
        ),
    )

    private val byId: Map<String, LifeEvent> = all.associateBy { it.id }

    fun byId(id: String): LifeEvent? = byId[id]

    /** Every event that may fire for [state] this year. Prison events fire only while
     *  incarcerated; every other event fires only while free. */
    fun eligible(state: GameState): List<LifeEvent> {
        val inPrison = state.prison != null
        return all.filter { e ->
            e.prisonOnly == inPrison &&
                state.age in e.minAge..e.maxAge &&
                state.stage in e.stages &&
                (!e.oneShot || e.id !in state.eventsSeen) &&
                e.condition(state)
        }
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
    prisonOnly: Boolean = false,
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
    prisonOnly = prisonOnly,
)
