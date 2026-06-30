package com.azizjonkasimov.lifesimulator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = SINGLE_SAVE_ID,
    val day: Int,
    val archetype: String,
    val health: Int,
    val mood: Int,
    val energy: Int,
    val stress: Int,
    val social: Int,
    val knowledge: Int,
    val fitness: Int,
    val career: Int,
    val money: Int,
    val careerLevel: Int,
    val jobTitle: String,
    val timeRemaining: Int,
    val rngSeed: Long,
    val historyLog: String,
) {
    fun toDomain(): GameState = GameState(
        day = day,
        archetype = LifeArchetype.valueOf(archetype),
        stats = CoreStats(
            health = health,
            mood = mood,
            energy = energy,
            stress = stress,
            social = social,
        ),
        skills = SkillSet(
            knowledge = knowledge,
            fitness = fitness,
            career = career,
        ),
        money = money,
        careerLevel = careerLevel,
        jobTitle = jobTitle,
        timeRemaining = timeRemaining,
        rngSeed = rngSeed,
        history = decodeHistory(historyLog),
    )

    companion object {
        const val SINGLE_SAVE_ID = 1
    }
}

fun GameState.toEntity(): GameStateEntity = GameStateEntity(
    day = day,
    archetype = archetype.name,
    health = stats.health,
    mood = stats.mood,
    energy = stats.energy,
    stress = stats.stress,
    social = stats.social,
    knowledge = skills.knowledge,
    fitness = skills.fitness,
    career = skills.career,
    money = money,
    careerLevel = careerLevel,
    jobTitle = jobTitle,
    timeRemaining = timeRemaining,
    rngSeed = rngSeed,
    historyLog = encodeHistory(history),
)

private fun encodeHistory(history: List<HistoryEntry>): String =
    history.joinToString("\n") { entry ->
        listOf(
            entry.day.toString(),
            entry.kind.name,
            escape(entry.title),
            escape(entry.detail),
        ).joinToString("\t")
    }

private fun decodeHistory(value: String): List<HistoryEntry> {
    if (value.isBlank()) return emptyList()
    return value.lines().mapNotNull { line ->
        val parts = line.split("\t")
        if (parts.size != 4) return@mapNotNull null
        HistoryEntry(
            day = parts[0].toIntOrNull() ?: return@mapNotNull null,
            kind = runCatching { HistoryKind.valueOf(parts[1]) }.getOrDefault(HistoryKind.SYSTEM),
            title = unescape(parts[2]),
            detail = unescape(parts[3]),
        )
    }
}

private fun escape(value: String): String =
    value
        .replace("%", "%25")
        .replace("\t", "%09")
        .replace("\n", "%0A")

private fun unescape(value: String): String =
    value
        .replace("%0A", "\n")
        .replace("%09", "\t")
        .replace("%25", "%")
