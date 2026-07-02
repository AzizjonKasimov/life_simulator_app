package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Ailment
import com.azizjonkasimov.lifesimulator.domain.model.AssetKind
import com.azizjonkasimov.lifesimulator.domain.model.Education
import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.LogKind
import com.azizjonkasimov.lifesimulator.domain.model.Person
import com.azizjonkasimov.lifesimulator.domain.model.Prison
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSimulationEngineTest {
    private val engine = LifeSimulationEngine()

    @Test
    fun startNewLife_producesNewbornWithParents() {
        val state = engine.startNewLife("Test Person", Gender.MALE)
        assertEquals(0, state.age)
        assertTrue(state.alive)
        assertTrue(state.relationships.any { it.relation == RelationType.MOTHER })
        assertTrue(state.relationships.any { it.relation == RelationType.FATHER })
        assertTrue(state.log.isNotEmpty())
    }

    @Test
    fun ageUp_incrementsAgeAndIsDeterministic() {
        val start = engine.startNewLife("Test Person", Gender.FEMALE)
        val first = engine.ageUp(start)
        val second = engine.ageUp(start)
        assertTrue(first.success)
        assertEquals(start.age + 1, first.state.age)
        // Pure + seeded: aging the same state twice yields an identical result.
        assertEquals(first.state, second.state)
    }

    @Test
    fun aLife_eventuallyEnds() {
        var state = engine.startNewLife("Mortal One", Gender.MALE)
        var guard = 0
        while (state.alive && guard < 200) {
            state = if (state.pendingEventIds.isNotEmpty()) {
                engine.resolveEvent(state, state.pendingEventIds.first(), 0).state
            } else {
                engine.ageUp(state).state
            }
            guard++
        }
        assertFalse("A life should end well within 200 years", state.alive)
        assertNotNull(state.causeOfDeath)
    }

    @Test
    fun doActivity_meditateRaisesHappinessOncePerYear() {
        val newborn = engine.startNewLife("Test", Gender.MALE)
        val atEight = newborn.copy(
            character = newborn.character.copy(age = 8, stats = newborn.character.stats.copy(happiness = 50)),
        )
        val result = engine.doActivity(atEight, "meditate")
        assertTrue(result.success)
        assertEquals(55, result.state.character.stats.happiness)
        assertTrue("meditate" in result.state.activitiesUsed)

        // Second attempt in the same year is rejected.
        val again = engine.doActivity(result.state, "meditate")
        assertFalse(again.success)
    }

    @Test
    fun university_enrollThroughToGraduation_raisesEducation() {
        val base = engine.startNewLife("Student", Gender.MALE)
        var state = base.copy(
            character = base.character.copy(age = 18, stats = base.character.stats.copy(smarts = 60)),
            education = Education(EducationLevel.SECONDARY),
            flags = setOf("hs_grad"),
        )
        val enrolled = engine.doActivity(state, "enroll_university")
        assertTrue(enrolled.success)
        state = enrolled.state
        assertTrue(state.education.isEnrolled)

        var guard = 0
        while (state.education.isEnrolled && state.alive && guard < 30) {
            state = if (state.pendingEventIds.isNotEmpty()) {
                engine.resolveEvent(state, state.pendingEventIds.first(), 0).state
            } else {
                engine.ageUp(state).state
            }
            guard++
        }
        assertEquals(EducationLevel.UNIVERSITY, state.education.level)
        assertTrue("college_grad" in state.flags)
    }

    @Test
    fun propose_marriesThePartner() {
        val base = engine.startNewLife("Romantic", Gender.FEMALE)
        val partner = Person("p1", "Alex Doe", RelationType.PARTNER, age = 26, relationship = 65)
        val state = base.copy(
            character = base.character.copy(age = 26),
            relationships = base.relationships + partner,
        )
        val result = engine.interact(state, "p1", "propose")
        assertTrue(result.success)
        val now = result.state.relationships.find { it.id == "p1" }
        assertEquals(RelationType.SPOUSE, now?.relation)
        assertTrue("married" in result.state.flags)
    }

    @Test
    fun haveChild_addsAChild() {
        val base = engine.startNewLife("Parent", Gender.MALE)
        val spouse = Person("s1", "Sam Doe", RelationType.SPOUSE, age = 29, relationship = 75)
        val state = base.copy(
            character = base.character.copy(age = 29),
            relationships = base.relationships + spouse,
        )
        val before = state.relationships.count { it.relation == RelationType.CHILD }
        val result = engine.interact(state, "s1", "have_child")
        assertTrue(result.success)
        assertEquals(before + 1, result.state.relationships.count { it.relation == RelationType.CHILD })
    }

    @Test
    fun careerLadder_promotesUntilTheTop() {
        val entry = JobCatalog.career("developer")!!.entryJob()
        assertEquals(1, entry.level)
        val next = JobCatalog.promoted(entry)!!
        assertEquals(2, next.level)
        assertTrue(next.salaryPerYear > entry.salaryPerYear)

        var job = entry
        var hops = 0
        while (hops < 20) {
            job = JobCatalog.promoted(job) ?: break
            hops++
        }
        assertNull(JobCatalog.promoted(job))
    }

    @Test
    fun eligibleJobs_gateOnDegree() {
        val withoutDegree = JobCatalog.eligible(age = 30, smarts = 95, education = EducationLevel.SECONDARY)
        assertTrue(withoutDegree.isNotEmpty())
        assertTrue(withoutDegree.none { it.requiresDegree })

        val withDegree = JobCatalog.eligible(age = 30, smarts = 95, education = EducationLevel.UNIVERSITY)
        assertTrue(withDegree.any { it.requiresDegree })
    }

    // ---- M3: health, crime, assets, traits, achievements ------------------

    @Test
    fun illness_acuteClears_chronicPersists() {
        // Run it inside prison: the event pool is prison-only and illness onset is
        // paused there, so ailment changes come solely from year-over-year progression.
        val base = engine.startNewLife("Patient", Gender.FEMALE)
        val state = base.copy(
            character = base.character.copy(age = 40, stats = base.character.stats.copy(health = 80)),
            prison = Prison(sentence = 5, served = 0),
            ailments = listOf(
                Ailment("bad_cold", "Bad Cold", severity = 1, chronic = false, yearsLeft = 1),
                Ailment("diabetes", "Type 2 Diabetes", severity = 2, chronic = true),
            ),
        )
        val after = engine.ageUp(state).state
        assertTrue("a chronic condition persists", after.ailments.any { it.id == "diabetes" })
        assertFalse("an acute condition clears", after.ailments.any { it.id == "bad_cold" })
    }

    @Test
    fun treatment_onlyOfferedWhenIll() {
        val well = engine.startNewLife("Well", Gender.MALE).let { it.copy(character = it.character.copy(age = 30)) }
        assertFalse(engine.availableActivities(well).any { it.activity.id == "treatment" })
        val sick = well.copy(ailments = listOf(Ailment("asthma", "Asthma", severity = 1, chronic = true)))
        assertTrue(engine.availableActivities(sick).any { it.activity.id == "treatment" })
    }

    @Test
    fun jailEvent_imprisonsLosesJobAndBlocksWork() {
        val base = engine.startNewLife("Con", Gender.MALE)
        val state = base.copy(
            character = base.character.copy(age = 30),
            job = JobCatalog.career("developer")!!.entryJob(),
            pendingEventIds = listOf("drunk_driving"),
        )
        val jailed = engine.resolveEvent(state, "drunk_driving", 0).state
        assertNotNull("a sentence was handed down", jailed.prison)
        assertNull("the job is lost on imprisonment", jailed.job)
        assertTrue(jailed.inPrison)
        assertFalse("normal activities are blocked inside", engine.doActivity(jailed, "gym").success)
        assertTrue(
            "only prison activities are offered",
            engine.availableActivities(jailed).all { it.activity.category == ActivityCategory.PRISON },
        )
    }

    @Test
    fun prison_releasedAfterServingSentence() {
        val base = engine.startNewLife("Free", Gender.FEMALE)
        val state = base.copy(
            character = base.character.copy(age = 30),
            prison = Prison(sentence = 1, served = 0),
        )
        val after = engine.ageUp(state).state
        assertNull("released after serving the sentence", after.prison)
        assertTrue("ex_convict" in after.flags)
    }

    @Test
    fun buyingAHouse_addsAssetLowersCashAndUnlocksAchievement() {
        val base = engine.startNewLife("Owner", Gender.MALE)
        val state = base.copy(character = base.character.copy(age = 30, money = 500_000))
        val result = engine.doActivity(state, "buy_house")
        assertTrue(result.success)
        val s = result.state
        assertTrue(s.assets.any { it.kind == AssetKind.PROPERTY })
        assertEquals(200_000, s.character.money)
        assertTrue("net worth counts the asset", s.netWorth > s.character.money)
        assertTrue("homeowner" in s.achievements)
    }

    @Test
    fun startNewLife_rollsValidTraits() {
        val state = engine.startNewLife("Trait", Gender.FEMALE)
        assertTrue(state.traits.isNotEmpty())
        assertTrue(state.traits.all { TraitCatalog.byId(it) != null })
    }

    @Test
    fun maxingSmarts_unlocksScholarAchievement() {
        val base = engine.startNewLife("Brainy", Gender.MALE)
        val state = base.copy(
            character = base.character.copy(age = 20, stats = base.character.stats.copy(smarts = 100, happiness = 50)),
        )
        val after = engine.doActivity(state, "meditate").state
        assertTrue("scholar" in after.achievements)
    }

    // ---- M4: generations --------------------------------------------------

    @Test
    fun startNewLife_isFirstGeneration() {
        assertEquals(1, engine.startNewLife("Founder", Gender.MALE).generation)
    }

    private fun deceasedWithFamily(): GameState {
        val base = engine.startNewLife("Elder", Gender.MALE)
        return base.copy(
            character = base.character.copy(age = 80, money = 250_000),
            relationships = base.relationships +
                Person("sp", "Jordan Elder", RelationType.SPOUSE, age = 78, relationship = 70, gender = Gender.FEMALE) +
                Person("kid1", "Riley Elder", RelationType.CHILD, age = 40, relationship = 80, gender = Gender.FEMALE) +
                Person("kid2", "Sam Elder", RelationType.CHILD, age = 36, relationship = 60, gender = Gender.MALE),
            traits = setOf("genius"),
            alive = false,
            causeOfDeath = "old age",
        )
    }

    @Test
    fun continueAsHeir_requiresALivingChild() {
        val dead = engine.startNewLife("Lonely", Gender.FEMALE).copy(alive = false, causeOfDeath = "old age")
        assertFalse(engine.continueAsHeir(dead, "nobody").success)
        // Still alive → can't inherit yet.
        assertFalse(engine.continueAsHeir(engine.startNewLife("Alive", Gender.MALE), "anyone").success)
    }

    @Test
    fun continueAsHeir_remapsFamilyAndInheritsTaxedShare() {
        val dead = deceasedWithFamily()
        val result = engine.continueAsHeir(dead, "kid1")
        assertTrue(result.success)
        val s = result.state

        assertTrue(s.alive)
        assertEquals("Riley Elder", s.character.name)
        assertEquals(Gender.FEMALE, s.character.gender)
        assertEquals(40, s.age)
        assertEquals(2, s.generation)

        // Estate: 250,000 net worth → 250,000 − (200,000 × 0.4) = 170,000, split over two children = 85,000.
        assertEquals(85_000, s.character.money)

        // Family reshaped around the heir: late father (you), living mother (spouse), a sibling (other child).
        assertEquals(3, s.relationships.size)
        assertTrue("you are their late parent", s.relationships.any { it.relation == RelationType.FATHER && !it.alive })
        assertTrue("your spouse is now their parent", s.relationships.any { it.id == "sp" && it.relation == RelationType.MOTHER && it.alive })
        assertTrue("your other child is now their sibling", s.relationships.any { it.id == "kid2" && it.relation == RelationType.SIBLING })
        assertTrue("the heir is no longer their own child", s.relationships.none { it.relation == RelationType.CHILD })

        // Fresh life: valid inherited traits, cleared history, a milestone opening the story.
        assertTrue(s.traits.isNotEmpty() && s.traits.all { TraitCatalog.byId(it) != null })
        assertTrue(s.eventsSeen.isEmpty())
        assertTrue(s.log.any { it.kind == LogKind.MILESTONE })
    }

    @Test
    fun continueAsHeir_isDeterministic() {
        val dead = deceasedWithFamily()
        assertEquals(engine.continueAsHeir(dead, "kid1").state, engine.continueAsHeir(dead, "kid1").state)
    }

    @Test
    fun estateShareEach_isZeroWhenBroke() {
        val base = engine.startNewLife("Pauper", Gender.MALE)
        val dead = base.copy(
            character = base.character.copy(money = -5_000),
            relationships = base.relationships + Person("c", "Kid", RelationType.CHILD, age = 20, relationship = 70),
            alive = false,
        )
        assertEquals(0, engine.estateShareEach(dead))
    }
}
