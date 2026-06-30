package com.azizjonkasimov.lifesimulator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.azizjonkasimov.lifesimulator.domain.model.GameState

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = SINGLE_SAVE_ID,
    val schemaVersion: Int = GameStateJsonCodec.SCHEMA_VERSION,
    val stateJson: String,
) {
    fun toDomain(): GameState = GameStateJsonCodec.decode(stateJson)

    companion object {
        const val SINGLE_SAVE_ID = 1
    }
}

fun GameState.toEntity(): GameStateEntity = GameStateEntity(
    stateJson = GameStateJsonCodec.encode(this),
)
