package com.azizjonkasimov.lifesimulator

import android.app.Application

class LifeSimulatorApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
