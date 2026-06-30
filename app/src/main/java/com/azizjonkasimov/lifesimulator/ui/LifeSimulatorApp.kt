package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet

@Composable
fun LifeSimulatorApp(
    viewModel: LifeSimulatorViewModel,
) {
    val uiState = viewModel.uiState
    when {
        uiState.isLoading -> LoadingScreen()
        uiState.gameState == null -> NewLifeScreen(onStart = viewModel::startNewLife)
        else -> ActiveGameScreen(
            uiState = uiState,
            onSelectTab = viewModel::selectTab,
            onPerformAction = viewModel::performAction,
            onAdvanceDay = viewModel::advanceDay,
            onReset = viewModel::resetSave,
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NewLifeScreen(
    onStart: (LifeArchetype) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Life Simulator",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose a starting point. Every day gives limited time, energy, and money to shape a fictional young-adult life.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(LifeArchetype.entries) { archetype ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = archetype.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = archetype.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onStart(archetype) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Start as ${archetype.displayName}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveGameScreen(
    uiState: LifeSimulatorUiState,
    onSelectTab: (GameTab) -> Unit,
    onPerformAction: (String) -> Unit,
    onAdvanceDay: () -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                GameTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = uiState.selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        icon = { Text(text = tab.iconText, fontWeight = FontWeight.Bold) },
                        label = { Text(text = tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
    ) { paddingValues ->
        val state = uiState.gameState ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Header(state = state, messages = uiState.messages)
            Spacer(modifier = Modifier.height(12.dp))
            when (uiState.selectedTab) {
                GameTab.DASHBOARD -> DashboardTab(state = state, onAdvanceDay = onAdvanceDay)
                GameTab.ACTIONS -> ActionsTab(actions = uiState.actions, onPerformAction = onPerformAction)
                GameTab.HISTORY -> HistoryTab(history = state.history)
                GameTab.PROGRESS -> ProgressTab(state = state, onReset = onReset)
            }
        }
    }
}

@Composable
private fun Header(
    state: GameState,
    messages: List<String>,
) {
    val status = statusFor(state)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Day ${state.day}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Week ${state.week} - ${state.jobTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(text = status)
        }
        if (messages.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    messages.take(3).forEach { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun DashboardTab(
    state: GameState,
    onAdvanceDay: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(label = "Money", value = "${state.money} USD", modifier = Modifier.weight(1f))
                MetricCard(label = "Time", value = "${state.timeRemaining} h", modifier = Modifier.weight(1f))
                MetricCard(label = "Energy", value = "${state.stats.energy}%", modifier = Modifier.weight(1f))
            }
        }
        item {
            StatSection(stats = state.stats)
        }
        item {
            Button(
                onClick = onAdvanceDay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "End Day")
            }
        }
    }
}

@Composable
private fun ActionsTab(
    actions: List<ActionAvailability>,
    onPerformAction: (String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(actions) { availability ->
            ActionCard(
                availability = availability,
                onPerformAction = onPerformAction,
            )
        }
    }
}

@Composable
private fun ActionCard(
    availability: ActionAvailability,
    onPerformAction: (String) -> Unit,
) {
    val action = availability.action
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Costs: ${action.timeCost} h, ${action.energyCost} energy" +
                    if (action.moneyCost > 0) ", ${action.moneyCost} USD" else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!availability.isAvailable && availability.reason != null) {
                Text(
                    text = availability.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { onPerformAction(action.id) },
                enabled = availability.isAvailable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Do Action")
            }
        }
    }
}

@Composable
private fun HistoryTab(history: List<HistoryEntry>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(history.asReversed()) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Day ${entry.day} - ${entry.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = entry.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressTab(
    state: GameState,
    onReset: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            SkillSection(skills = state.skills, careerLevel = state.careerLevel)
        }
        item {
            WeeklySummary(state = state)
        }
        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Reset Life")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onReset()
                    },
                ) {
                    Text(text = "Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = "Cancel")
                }
            },
            title = { Text(text = "Reset this life?") },
            text = { Text(text = "This clears the current single save and returns to the archetype picker.") },
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatSection(stats: CoreStats) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Core Stats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        StatBar(label = "Health", value = stats.health)
        StatBar(label = "Mood", value = stats.mood)
        StatBar(label = "Energy", value = stats.energy)
        StatBar(label = "Stress", value = stats.stress, reverseGood = true)
        StatBar(label = "Social", value = stats.social)
    }
}

@Composable
private fun SkillSection(
    skills: SkillSet,
    careerLevel: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(label = "Career level", value = careerLevel.toString(), modifier = Modifier.fillMaxWidth())
        StatBar(label = "Knowledge XP", value = skills.knowledge.coerceAtMost(100))
        StatBar(label = "Fitness XP", value = skills.fitness.coerceAtMost(100))
        StatBar(label = "Career XP", value = skills.career % 100)
    }
}

@Composable
private fun WeeklySummary(state: GameState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Week ${state.week} Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = "Archetype: ${state.archetype.displayName}")
            Text(text = "Job: ${state.jobTitle}")
            Text(text = "Money: ${state.money} USD")
            Text(text = "History entries: ${state.history.size}")
        }
    }
}

@Composable
private fun StatBar(
    label: String,
    value: Int,
    reverseGood: Boolean = false,
) {
    val normalized = value.coerceIn(0, 100) / 100f
    val displayValue = value.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "$displayValue%", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { if (reverseGood) 1f - normalized else normalized },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun statusFor(state: GameState): String = when {
    state.money < 0 -> "In debt"
    state.stats.health <= 35 -> "Fragile"
    state.stats.stress >= 75 -> "Stressed"
    state.stats.mood >= 75 && state.stats.health >= 65 -> "Thriving"
    else -> "Stable"
}
