package com.azizjonkasimov.lifesimulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.azizjonkasimov.lifesimulator.ui.LifeSimulatorApp
import com.azizjonkasimov.lifesimulator.ui.LifeSimulatorViewModel
import com.azizjonkasimov.lifesimulator.ui.LifeSimulatorViewModelFactory
import com.azizjonkasimov.lifesimulator.ui.theme.LifeSimulatorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LifeSimulatorViewModel by viewModels {
        val container = (application as LifeSimulatorApplication).appContainer
        LifeSimulatorViewModelFactory(
            repository = container.repository,
            engine = container.engine,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeSimulatorTheme {
                LifeSimulatorApp(viewModel = viewModel)
            }
        }
    }
}
