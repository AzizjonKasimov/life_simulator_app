package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.engine.JobCatalog
import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine
import com.azizjonkasimov.lifesimulator.domain.model.Ailment
import com.azizjonkasimov.lifesimulator.domain.model.Asset
import com.azizjonkasimov.lifesimulator.domain.model.AssetKind
import com.azizjonkasimov.lifesimulator.domain.model.Education
import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.Job
import com.azizjonkasimov.lifesimulator.domain.model.JobField
import com.azizjonkasimov.lifesimulator.domain.model.Person
import com.azizjonkasimov.lifesimulator.domain.model.Prison
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun roundTrip_preservesM3State() {
        val base = engine.startNewLife("Deep Life", Gender.MALE)
        val state = base.copy(
            ailments = listOf(
                Ailment("diabetes", "Type 2 Diabetes", severity = 2, chronic = true),
                Ailment("bad_cold", "Bad Cold", severity = 1, chronic = false, yearsLeft = 1),
            ),
            prison = Prison(sentence = 3, served = 1),
            assets = listOf(Asset("house_0", "Family House", AssetKind.PROPERTY, value = 270_000)),
            traits = setOf("genius", "lucky"),
            achievements = setOf("scholar", "homeowner"),
        )
        val decoded = GameStateJsonCodec.decode(GameStateJsonCodec.encode(state))
        assertEquals(state, decoded)
    }

    @Test
    fun decode_toleratesMissingM3Fields() {
        // A v0.11.0 (schema 6) blob predates the health/crime/asset/trait/achievement fields.
        val json = JSONObject(GameStateJsonCodec.encode(engine.startNewLife("Old Save", Gender.FEMALE)))
        listOf("ailments", "prison", "assets", "traits", "achievements").forEach { json.remove(it) }
        val decoded = GameStateJsonCodec.decode(json.toString())
        assertTrue(decoded.ailments.isEmpty())
        assertNull(decoded.prison)
        assertTrue(decoded.assets.isEmpty())
        assertTrue(decoded.traits.isEmpty())
        assertTrue(decoded.achievements.isEmpty())
    }
}
