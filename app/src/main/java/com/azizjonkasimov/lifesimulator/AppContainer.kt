package com.azizjonkasimov.lifesimulator

import android.content.Context
import androidx.room.Room
import com.azizjonkasimov.lifesimulator.data.LifeSimulatorDatabase
import com.azizjonkasimov.lifesimulator.data.SaveRepository
import com.azizjonkasimov.lifesimulator.domain.engine.LifeSimulationEngine

class AppContainer(context: Context) {
    private val database: LifeSimulatorDatabase = Room.databaseBuilder(
        context.applicationContext,
        LifeSimulatorDatabase::class.java,
        "life_simulator.db",
    ).build()

    val repository: SaveRepository = SaveRepository(database.gameStateDao())
    val engine: LifeSimulationEngine = LifeSimulationEngine()
}
