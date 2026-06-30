package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.RelationshipState
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet

@Composable
internal fun ProgressScreen(
    state: GameState,
    currentVersionLabel: String,
    updateChecking: Boolean,
    onCheckForUpdates: () -> Unit,
    onReset: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { CareerSection(state = state) }
        item { BusinessSection(state = state) }
        item { FinanceSection(state = state) }
        item { WellbeingSection(state = state) }
        item {
            SectionCard(title = "Relationships", icon = UiIcons.relationships) {
                RelationshipBars(relationships = state.relationships)
            }
        }
        item { SkillSection(skills = state.skills) }
        if (state.modifiers.isNotEmpty()) {
            item { ModifiersSection(state = state) }
        }
        item {
            AppUpdateSection(
                currentVersionLabel = currentVersionLabel,
                updateChecking = updateChecking,
                onCheckForUpdates = onCheckForUpdates,
            )
        }
        item {
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
            text = { Text(text = "This clears the current single save and returns to the start screen.") },
        )
    }
}

@Composable
private fun CareerSection(state: GameState) {
    SectionCard(title = "Career", icon = UiIcons.career) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(label = "Title", value = state.career.title, modifier = Modifier.weight(1f))
            MiniStat(
                label = "Status",
                value = if (state.career.employed) "Employed" else "Searching",
                modifier = Modifier.weight(1f),
            )
        }
        if (state.career.employed) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(label = "Level", value = state.career.level.toString(), modifier = Modifier.weight(1f))
                MiniStat(label = "Shift pay", value = money(state.career.salaryPerShift), modifier = Modifier.weight(1f))
                MiniStat(label = "Reputation", value = "${state.career.reputation}%", modifier = Modifier.weight(1f))
            }
            StatBar(label = "Promotion", value = state.career.promotionReadiness)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(label = "Applications", value = state.jobSearch.applicationsSent.toString(), modifier = Modifier.weight(1f))
                MiniStat(label = "Interview", value = "${state.jobSearch.interviewReadiness}%", modifier = Modifier.weight(1f))
            }
            StatBar(label = "Offer progress", value = state.jobSearch.offerProgress)
        }
    }
}

@Composable
private fun BusinessSection(state: GameState) {
    SectionCard(title = "Business", icon = UiIcons.business) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(label = "Stage", value = state.business.stage.label, modifier = Modifier.weight(1f))
            MiniStat(label = "Overhead", value = money(businessOverheadFor(state.business.stage)), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(label = "Leads", value = state.business.leads.toString(), modifier = Modifier.weight(1f))
            MiniStat(label = "Active", value = state.business.activeProjects.toString(), modifier = Modifier.weight(1f))
            MiniStat(label = "Completed", value = state.business.completedProjects.toString(), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(label = "Trust", value = "${state.business.clientTrust}%", modifier = Modifier.weight(1f))
            MiniStat(label = "Reputation", value = "${state.business.reputation}%", modifier = Modifier.weight(1f))
        }
        StatBar(label = "Pipeline value", value = (state.business.pipelineValue * 100 / 250).coerceIn(0, 100))
    }
}

@Composable
private fun FinanceSection(state: GameState) {
    SectionCard(title = "Finances", icon = UiIcons.finances) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(label = "Cash", value = money(state.finances.cash), modifier = Modifier.weight(1f))
            MiniStat(label = "Debt", value = money(state.finances.debt), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(label = "Weekly bill", value = money(state.finances.weeklyLivingCost), modifier = Modifier.weight(1f))
            MiniStat(label = "Credit", value = state.finances.creditScore.toString(), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WellbeingSection(state: GameState) {
    SectionCard(title = "Wellbeing", icon = UiIcons.wellbeing) {
        StatBar(label = "Health", value = state.stats.health)
        StatBar(label = "Mood", value = state.stats.mood)
        StatBar(label = "Energy", value = state.stats.energy)
        StatBar(label = "Stress", value = state.stats.stress, reverseGood = true)
        StatBar(label = "Social", value = state.stats.social)
    }
}

@Composable
private fun SkillSection(skills: SkillSet) {
    SectionCard(title = "Skills", icon = UiIcons.skills) {
        StatBar(label = "Knowledge", value = skills.knowledge.coerceAtMost(100))
        StatBar(label = "Fitness", value = skills.fitness.coerceAtMost(100))
        StatBar(label = "Career XP", value = skills.career % 100)
        StatBar(label = "Communication", value = skills.communication.coerceAtMost(100))
        StatBar(label = "Creativity", value = skills.creativity.coerceAtMost(100))
    }
}

@Composable
private fun RelationshipBars(relationships: RelationshipState) {
    StatBar(label = "Family", value = relationships.family)
    StatBar(label = "Friends", value = relationships.friends)
    StatBar(label = "Network", value = relationships.network)
}

@Composable
private fun ModifiersSection(state: GameState) {
    SectionCard(title = "Active Modifiers", icon = UiIcons.pressure) {
        state.modifiers.forEach { modifier ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${modifier.title} · ${modifier.daysRemaining}d",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = modifier.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppUpdateSection(
    currentVersionLabel: String,
    updateChecking: Boolean,
    onCheckForUpdates: () -> Unit,
) {
    SectionCard(title = "App Updates", icon = UiIcons.updates) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Installed version",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = currentVersionLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            OutlinedButton(
                enabled = !updateChecking,
                onClick = onCheckForUpdates,
            ) {
                Text(text = if (updateChecking) "Checking" else "Check")
            }
        }
        if (updateChecking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
