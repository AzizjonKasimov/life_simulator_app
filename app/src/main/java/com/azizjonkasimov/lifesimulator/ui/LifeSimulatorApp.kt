package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.azizjonkasimov.lifesimulator.domain.model.EventCategory
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.Gender
import com.azizjonkasimov.lifesimulator.domain.model.LifeEvent
import com.azizjonkasimov.lifesimulator.domain.model.StatChange
import com.azizjonkasimov.lifesimulator.update.UpdatePrompt
import com.azizjonkasimov.lifesimulator.update.rememberUpdatePromptState

@Composable
fun LifeSimulatorApp(
    viewModel: LifeSimulatorViewModel,
) {
    val uiState = viewModel.uiState
    val updatePromptState = rememberUpdatePromptState(onMessage = viewModel::showMessage)
    val state = uiState.gameState
    when {
        uiState.isLoading -> LoadingScreen()
        state == null -> CharacterCreationScreen(onStart = viewModel::startNewLife)
        !state.alive -> LegacyScreen(state = state, onNewLife = viewModel::resetSave)
        else -> ActiveGameScreen(
            uiState = uiState,
            viewModel = viewModel,
            currentVersionLabel = updatePromptState.currentVersionLabel,
            updateChecking = updatePromptState.checking,
            onCheckForUpdates = { updatePromptState.checkForUpdates(showResult = true) },
        )
    }
    UpdatePrompt(state = updatePromptState)
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CharacterCreationScreen(
    onStart: (String, Gender) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.MALE) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadgeTile(icon = UiIcons.person, iconSize = 30.dp, padding = 14.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Life Simulator", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "A new life is about to begin. Age up year by year, make your choices, and see how the story unfolds.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        SectionCard(title = "Who are you?", icon = UiIcons.person) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                GenderToggle("Male", gender == Gender.MALE, Modifier.weight(1f)) { gender = Gender.MALE }
                GenderToggle("Female", gender == Gender.FEMALE, Modifier.weight(1f)) { gender = Gender.FEMALE }
            }
            Button(onClick = { onStart(name, gender) }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Start Life")
            }
        }
    }
}

@Composable
private fun GenderToggle(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
private fun ActiveGameScreen(
    uiState: LifeSimulatorUiState,
    viewModel: LifeSimulatorViewModel,
    currentVersionLabel: String,
    updateChecking: Boolean,
    onCheckForUpdates: () -> Unit,
) {
    val state = uiState.gameState ?: return
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                GameTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(imageVector = tab.icon(), contentDescription = tab.label) },
                        label = { Text(text = tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            CharacterHeaderBar(state = state)
            AnimatedVisibility(visible = uiState.messages.isNotEmpty() || uiState.statChanges.isNotEmpty()) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    FeedbackBanner(messages = uiState.messages, changes = uiState.statChanges)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (uiState.selectedTab) {
                    GameTab.LIFE -> LifeScreen(state = state, onAgeUp = viewModel::ageUp)
                    GameTab.ACTIVITIES -> ActivitiesScreen(
                        activities = uiState.activities,
                        onActivity = viewModel::doActivity,
                    )
                    GameTab.PEOPLE -> PeopleScreen(state = state, onInteract = viewModel::interact)
                    GameTab.PROFILE -> ProfileScreen(
                        state = state,
                        currentVersionLabel = currentVersionLabel,
                        updateChecking = updateChecking,
                        onCheckForUpdates = onCheckForUpdates,
                        onReset = viewModel::resetSave,
                    )
                }
            }
        }
    }

    uiState.pendingEvent?.let { event ->
        EventDialog(event = event, onChoose = { index -> viewModel.resolveEvent(event.id, index) })
    }
}

@Composable
private fun CharacterHeaderBar(state: GameState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadgeTile(icon = UiIcons.person, iconSize = 22.dp, padding = 9.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.character.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Age ${state.age} · ${state.stage.label} · ${state.character.birthplace}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = money(state.character.money),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (state.character.money >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun FeedbackBanner(
    messages: List<String>,
    changes: List<StatChange>,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            messages.take(3).forEach { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (changes.isNotEmpty()) {
                ChipFlowRow {
                    changes.forEach { StatChangeChip(change = it) }
                }
            }
        }
    }
}

@Composable
private fun EventDialog(
    event: LifeEvent,
    onChoose: (Int) -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LabelChip(text = event.category.label, tone = categoryTone(event.category))
                Text(
                    text = event.prompt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                event.choices.forEachIndexed { index, choice ->
                    Button(onClick = { onChoose(index) }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = choice.label)
                    }
                }
            }
        }
    }
}

private fun categoryTone(category: EventCategory): ChipTone = when (category) {
    EventCategory.HEALTH -> ChipTone.DANGER
    EventCategory.MONEY -> ChipTone.SUCCESS
    EventCategory.CRIME -> ChipTone.WARN
    EventCategory.ROMANCE, EventCategory.FAMILY -> ChipTone.ACCENT
    else -> ChipTone.NEUTRAL
}
