package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.model.Character
import com.azizjonkasimov.lifesimulator.domain.model.Education
import com.azizjonkasimov.lifesimulator.domain.model.EducationLevel
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.Job
import com.azizjonkasimov.lifesimulator.domain.model.JobField
import com.azizjonkasimov.lifesimulator.domain.model.LogEntry
import com.azizjonkasimov.lifesimulator.domain.model.LogKind
import com.azizjonkasimov.lifesimulator.domain.model.Person
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
import com.azizjonkasimov.lifesimulator.domain.model.Stats
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON blob (de)serialization for [GameState]. Schema 6 is the BitLife-style life
 * sim; any older-schema save (the finance game, schema 5) is treated as "no save"
 * by [SaveRepository], so a fresh life starts cleanly.
 */
object GameStateJsonCodec {
    const val SCHEMA_VERSION = 6

    fun encode(state: GameState): String = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("character", state.character.toJson())
        .put("relationships", JSONArray().apply { state.relationships.forEach { put(it.toJson()) } })
        .put("education", state.education.level.name)
        .put("job", state.job?.toJson() ?: JSONObject.NULL)
        .put("flags", state.flags.toJsonArray())
        .put("eventsSeen", state.eventsSeen.toJsonArray())
        .put("pendingEventIds", state.pendingEventIds.toJsonArray())
        .put("activitiesUsed", state.activitiesUsed.toJsonArray())
        .put("rngSeed", state.rngSeed)
        .put("log", JSONArray().apply { state.log.forEach { put(it.toJson()) } })
        .put("alive", state.alive)
        .put("causeOfDeath", state.causeOfDeath ?: JSONObject.NULL)
        .toString()

    fun decode(raw: String): GameState {
        val root = JSONObject(raw)
        return GameState(
            character = root.getJSONObject("character").toCharacter(),
            relationships = root.getJSONArray("relationships").map { it.toPerson() },
            education = Education(enumOrDefault(root.getString("education"), EducationLevel.NONE)),
            job = if (root.isNull("job")) null else root.getJSONObject("job").toJob(),
            flags = root.getJSONArray("flags").toStringSet(),
            eventsSeen = root.getJSONArray("eventsSeen").toStringSet(),
            pendingEventIds = root.getJSONArray("pendingEventIds").toStringList(),
            activitiesUsed = root.getJSONArray("activitiesUsed").toStringSet(),
            rngSeed = root.getLong("rngSeed"),
            log = root.getJSONArray("log").map { it.toLogEntry() },
            alive = root.optBoolean("alive", true),
            causeOfDeath = if (root.isNull("causeOfDeath")) null else root.optString("causeOfDeath", "").ifBlank { null },
        )
    }

    // ---- Character & stats -------------------------------------------------

    private fun Character.toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("gender", gender.name)
        .put("birthplace", birthplace)
        .put("age", age)
        .put("money", money)
        .put("stats", JSONObject().put("happiness", stats.happiness).put("health", stats.health).put("smarts", stats.smarts).put("looks", stats.looks))

    private fun JSONObject.toCharacter(): Character {
        val statsJson = getJSONObject("stats")
        return Character(
            name = getString("name"),
            gender = enumOrDefault(getString("gender"), Gender.MALE),
            birthplace = getString("birthplace"),
            age = getInt("age"),
            money = getInt("money"),
            stats = Stats(
                happiness = statsJson.getInt("happiness"),
                health = statsJson.getInt("health"),
                smarts = statsJson.getInt("smarts"),
                looks = statsJson.getInt("looks"),
            ).clamped(),
        )
    }

    private fun Person.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("relation", relation.name)
        .put("age", age)
        .put("relationship", relationship)
        .put("alive", alive)

    private fun JSONObject.toPerson(): Person = Person(
        id = getString("id"),
        name = getString("name"),
        relation = enumOrDefault(getString("relation"), RelationType.FRIEND),
        age = getInt("age"),
        relationship = getInt("relationship"),
        alive = optBoolean("alive", true),
    ).clamped()

    private fun Job.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("field", field.name)
        .put("salaryPerYear", salaryPerYear)
        .put("minAge", minAge)
        .put("minSmarts", minSmarts)

    private fun JSONObject.toJob(): Job = Job(
        id = getString("id"),
        title = getString("title"),
        field = enumOrDefault(getString("field"), JobField.SERVICE),
        salaryPerYear = getInt("salaryPerYear"),
        minAge = optInt("minAge", 18),
        minSmarts = optInt("minSmarts", 0),
    )

    private fun LogEntry.toJson(): JSONObject = JSONObject()
        .put("age", age)
        .put("text", text)
        .put("kind", kind.name)

    private fun JSONObject.toLogEntry(): LogEntry = LogEntry(
        age = getInt("age"),
        text = getString("text"),
        kind = enumOrDefault(getString("kind"), LogKind.NEUTRAL),
    )

    // ---- Small helpers -----------------------------------------------------

    private fun Collection<String>.toJsonArray(): JSONArray {
        val array = JSONArray()
        for (item in this) array.put(item)
        return array
    }

    private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }

    private fun JSONArray.toStringSet(): Set<String> = toStringList().toSet()

    private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
        runCatching { enumValueOf<T>(name) }.getOrDefault(default)
}
