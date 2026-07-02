package com.azizjonkasimov.lifesimulator.data

import com.azizjonkasimov.lifesimulator.domain.model.Ailment
import com.azizjonkasimov.lifesimulator.domain.model.Asset
import com.azizjonkasimov.lifesimulator.domain.model.AssetKind
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
import com.azizjonkasimov.lifesimulator.domain.model.Prison
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
        .put("education", state.education.toJson())
        .put("job", state.job?.toJson() ?: JSONObject.NULL)
        .put("jobYears", state.jobYears)
        .put("flags", state.flags.toJsonArray())
        .put("eventsSeen", state.eventsSeen.toJsonArray())
        .put("pendingEventIds", state.pendingEventIds.toJsonArray())
        .put("activitiesUsed", state.activitiesUsed.toJsonArray())
        .put("rngSeed", state.rngSeed)
        .put("log", JSONArray().apply { state.log.forEach { put(it.toJson()) } })
        .put("alive", state.alive)
        .put("causeOfDeath", state.causeOfDeath ?: JSONObject.NULL)
        .put("ailments", JSONArray().apply { state.ailments.forEach { put(it.toJson()) } })
        .put("prison", state.prison?.toJson() ?: JSONObject.NULL)
        .put("assets", JSONArray().apply { state.assets.forEach { put(it.toJson()) } })
        .put("traits", state.traits.toJsonArray())
        .put("achievements", state.achievements.toJsonArray())
        .put("generation", state.generation)
        .toString()

    fun decode(raw: String): GameState {
        val root = JSONObject(raw)
        return GameState(
            character = root.getJSONObject("character").toCharacter(),
            relationships = root.getJSONArray("relationships").map { it.toPerson() },
            education = decodeEducation(root),
            job = if (root.isNull("job")) null else root.getJSONObject("job").toJob(),
            jobYears = root.optInt("jobYears", 0),
            flags = root.getJSONArray("flags").toStringSet(),
            eventsSeen = root.getJSONArray("eventsSeen").toStringSet(),
            pendingEventIds = root.getJSONArray("pendingEventIds").toStringList(),
            activitiesUsed = root.getJSONArray("activitiesUsed").toStringSet(),
            rngSeed = root.getLong("rngSeed"),
            log = root.getJSONArray("log").map { it.toLogEntry() },
            alive = root.optBoolean("alive", true),
            causeOfDeath = if (root.isNull("causeOfDeath")) null else root.optString("causeOfDeath", "").ifBlank { null },
            ailments = root.optJSONArray("ailments")?.map { it.toAilment() } ?: emptyList(),
            prison = root.optJSONObject("prison")?.toPrison(),
            assets = root.optJSONArray("assets")?.map { it.toAsset() } ?: emptyList(),
            traits = root.optJSONArray("traits")?.toStringSet() ?: emptySet(),
            achievements = root.optJSONArray("achievements")?.toStringSet() ?: emptySet(),
            generation = root.optInt("generation", 1),
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

    // ---- Education ---------------------------------------------------------

    private fun Education.toJson(): JSONObject = JSONObject()
        .put("level", level.name)
        .put("enrolledIn", enrolledIn?.name ?: JSONObject.NULL)
        .put("yearsLeft", yearsLeft)

    private fun decodeEducation(root: JSONObject): Education {
        val obj = root.optJSONObject("education")
        if (obj != null) {
            return Education(
                level = enumOrDefault(obj.optString("level", EducationLevel.NONE.name), EducationLevel.NONE),
                enrolledIn = if (obj.isNull("enrolledIn")) {
                    null
                } else {
                    runCatching { enumValueOf<EducationLevel>(obj.getString("enrolledIn")) }.getOrNull()
                },
                yearsLeft = obj.optInt("yearsLeft", 0),
            )
        }
        // Legacy (v0.10.0 schema-6): education was stored as a bare level string.
        return Education(enumOrDefault(root.optString("education", EducationLevel.NONE.name), EducationLevel.NONE))
    }

    private fun Person.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("relation", relation.name)
        .put("age", age)
        .put("relationship", relationship)
        .put("alive", alive)
        .put("gender", gender?.name ?: JSONObject.NULL)

    private fun JSONObject.toPerson(): Person = Person(
        id = getString("id"),
        name = getString("name"),
        relation = enumOrDefault(getString("relation"), RelationType.FRIEND),
        age = getInt("age"),
        relationship = getInt("relationship"),
        alive = optBoolean("alive", true),
        gender = if (isNull("gender")) null else runCatching { enumValueOf<Gender>(getString("gender")) }.getOrNull(),
    ).clamped()

    private fun Job.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("field", field.name)
        .put("salaryPerYear", salaryPerYear)
        .put("level", level)
        .put("minAge", minAge)
        .put("minSmarts", minSmarts)
        .put("requiresDegree", requiresDegree)

    private fun JSONObject.toJob(): Job = Job(
        id = getString("id"),
        title = getString("title"),
        field = enumOrDefault(getString("field"), JobField.SERVICE),
        salaryPerYear = getInt("salaryPerYear"),
        level = optInt("level", 1),
        minAge = optInt("minAge", 18),
        minSmarts = optInt("minSmarts", 0),
        requiresDegree = optBoolean("requiresDegree", false),
    )

    // ---- M3 entities: ailments, prison, assets -----------------------------

    private fun Ailment.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("severity", severity)
        .put("chronic", chronic)
        .put("yearsLeft", yearsLeft)

    private fun JSONObject.toAilment(): Ailment = Ailment(
        id = getString("id"),
        name = getString("name"),
        severity = optInt("severity", 1),
        chronic = optBoolean("chronic", false),
        yearsLeft = optInt("yearsLeft", 0),
    )

    private fun Prison.toJson(): JSONObject = JSONObject()
        .put("sentence", sentence)
        .put("served", served)

    private fun JSONObject.toPrison(): Prison = Prison(
        sentence = getInt("sentence"),
        served = optInt("served", 0),
    )

    private fun Asset.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("kind", kind.name)
        .put("value", value)

    private fun JSONObject.toAsset(): Asset = Asset(
        id = getString("id"),
        name = getString("name"),
        kind = enumOrDefault(getString("kind"), AssetKind.LUXURY),
        value = getInt("value"),
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
