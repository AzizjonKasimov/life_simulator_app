package com.azizjonkasimov.lifesimulator.domain.engine

import com.azizjonkasimov.lifesimulator.domain.model.Education
import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.Person
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
}
