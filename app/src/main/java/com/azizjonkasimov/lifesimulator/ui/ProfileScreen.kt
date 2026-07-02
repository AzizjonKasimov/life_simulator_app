package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.RelationType
import com.azizjonkasimov.lifesimulator.domain.model.Stat

@Composable
fun ProfileScreen(
    state: GameState,
    currentVersionLabel: String,
    updateChecking: Boolean,
    onCheckForUpdates: () -> Unit,
    onReset: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val spouse = state.relationships.firstOrNull { it.alive && it.relation == RelationType.SPOUSE }
            val partner = state.relationships.firstOrNull { it.alive && it.relation == RelationType.PARTNER }
            val children = state.relationships.count { it.alive && it.relation == RelationType.CHILD }
            val family = when {
                spouse != null -> "Married to ${spouse.name}"
                partner != null -> "Dating ${partner.name}"
                else -> "Single"
            }
            SectionCard(title = "Character", icon = UiIcons.person) {
                InfoRow("Name", state.character.name)
                InfoRow("Gender", state.character.gender.label)
                InfoRow("Born", state.character.birthplace)
                InfoRow("Age", "${state.age} · ${state.stage.label}")
                InfoRow("Education", state.education.summary)
                InfoRow("Job", state.job?.let { "${it.title} · ${money(it.salaryPerYear)}/yr" } ?: "Unemployed")
                InfoRow("Relationship", family)
                if (children > 0) InfoRow("Children", "$children")
                InfoRow("Money", money(state.character.money))
            }
        }
        item {
            SectionCard(title = "Stats", icon = UiIcons.health) {
                Stat.entries.forEach { stat ->
                    StatBar(stat = stat, value = state.character.stats.get(stat))
                }
            }
        }
        item {
            SectionCard(title = "App") {
                Text(
                    text = "Version $currentVersionLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onCheckForUpdates,
                    enabled = !updateChecking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = if (updateChecking) "Checking…" else "Check for Updates")
                }
                OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Start a New Life")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
