package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.engine.JobCatalog
import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import com.azizjonkasimov.lifesimulator.domain.model.Education
import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.Job
import com.azizjonkasimov.lifesimulator.domain.model.JobField
import com.azizjonkasimov.lifesimulator.domain.model.Person
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateJsonCodecTest {
    private val engine = LifeSimulationEngine()

    @Test
    fun roundTrip_preservesAFreshLife() {
        val state = engine.ageUp(engine.startNewLife("Round Trip", Gender.FEMALE)).state
        val decoded = GameStateJsonCodec.decode(GameStateJsonCodec.encode(state))
        assertEquals(state, decoded)
    }

    @Test
    fun roundTrip_preservesJobFlagsAndDeath() {
        val base = engine.startNewLife("Worker", Gender.MALE)
        val state = base.copy(
            job = Job("clerk", "Office Clerk", JobField.OFFICE, salaryPerYear = 34000),
            flags = setOf("hs_grad", "homeowner"),
            eventsSeen = setOf("buy_house"),
            pendingEventIds = listOf("free_time"),
            activitiesUsed = setOf("gym"),
            alive = false,
            causeOfDeath = "old age",
        )
        val decoded = GameStateJsonCodec.decode(GameStateJsonCodec.encode(state))
        assertEquals(state, decoded)
    }

    @Test
    fun roundTrip_preservesEducationCareerAndFamily() {
        val base = engine.startNewLife("Full Life", Gender.FEMALE)
        val state = base.copy(
            education = Education(EducationLevel.UNIVERSITY, enrolledIn = EducationLevel.GRADUATE, yearsLeft = 1),
            job = JobCatalog.career("developer")!!.jobAt(3),
            jobYears = 2,
            relationships = base.relationships +
                Person("sp", "Sam Lee", RelationType.SPOUSE, age = 30, relationship = 80) +
                Person("ch", "Kid Lee", RelationType.CHILD, age = 2, relationship = 90),
            flags = setOf("hs_grad", "college_grad", "grad_degree", "married"),
        )
        val decoded = GameStateJsonCodec.decode(GameStateJsonCodec.encode(state))
        assertEquals(state, decoded)
    }

    @Test
    fun decode_toleratesLegacyEducationString() {
        // A v0.10.0 (schema 6) blob stored education as a bare level string and had no jobYears.
        val json = JSONObject(GameStateJsonCodec.encode(engine.startNewLife("Legacy", Gender.MALE)))
        json.put("education", "SECONDARY")
        json.remove("jobYears")
        val decoded = GameStateJsonCodec.decode(json.toString())
        assertEquals(EducationLevel.SECONDARY, decoded.education.level)
        assertEquals(0, decoded.jobYears)
    }
}
