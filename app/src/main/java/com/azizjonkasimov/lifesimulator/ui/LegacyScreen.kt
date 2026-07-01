package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LogKind
import com.azizjonkasimov.lifesimulator.domain.model.Stat

@Composable
fun LegacyScreen(
    state: GameState,
    onNewLife: () -> Unit,
) {
    val milestones = state.log.filter { it.kind == LogKind.MILESTONE }.reversed().take(7)
    val knownPeople = state.relationships.size
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconBadgeTile(icon = Icons.Filled.Star, iconSize = 30.dp, padding = 14.dp)
        Text(
            text = state.character.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${state.age} years old · ${state.character.birthplace}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionCard(title = "A life remembered", icon = Icons.Filled.Star) {
            Text(
                text = "Cause of death: ${state.causeOfDeath ?: "unknown"}.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Final wealth: ${money(state.character.money)} · People known: $knownPeople",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Stat.entries.forEach { stat ->
                StatBar(stat = stat, value = state.character.stats.get(stat))
            }
        }

        if (milestones.isNotEmpty()) {
            SectionCard(title = "Notable moments") {
                milestones.forEach { entry ->
                    Text(
                        text = "Age ${entry.age}: ${entry.text}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onNewLife, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Start a New Life")
        }
    }
}
