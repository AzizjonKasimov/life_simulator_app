package com.azizjonkasimov.lifesimulator.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameStateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LifeSimulatorDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
}
