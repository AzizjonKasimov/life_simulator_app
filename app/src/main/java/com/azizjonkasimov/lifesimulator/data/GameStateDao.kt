package com.azizjonkasimov.lifesimulator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStateDao {
    @Query("SELECT * FROM game_state WHERE id = 1")
    fun observeGameState(): Flow<GameStateEntity?>

    @Query("SELECT * FROM game_state WHERE id = 1")
    suspend fun getGameState(): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameState(gameState: GameStateEntity)

    @Query("DELETE FROM game_state")
    suspend fun clear()
}
