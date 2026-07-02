package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.LogEntry
import com.azizjonkasimov.lifesimulator.domain.model.LogKind
import com.azizjonkasimov.lifesimulator.domain.model.Stat

@Composable
fun LifeScreen(
    state: GameState,
    onAgeUp: () -> Unit,
) {
    val blocked = state.pendingEventIds.isNotEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.prison?.let { prison ->
            item {
                val left = prison.yearsLeft
                StatusBanner(
                    icon = Icons.Filled.Gavel,
                    text = "You're in prison — $left year${if (left == 1) "" else "s"} left on your sentence.",
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        if (state.ailments.isNotEmpty()) {
            item {
                StatusBanner(
                    icon = Icons.Filled.Favorite,
                    text = "Living with ${state.ailments.joinToString(", ") { it.name }}.",
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        item {
            SectionCard(title = "Stats", icon = UiIcons.person) {
                Stat.entries.forEach { stat ->
                    StatBar(stat = stat, value = state.character.stats.get(stat))
                }
            }
        }
        item {
            Button(
                onClick = onAgeUp,
                enabled = !blocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Icon(imageVector = UiIcons.age, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (blocked) "Make your choice first" else "Age Up",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        item {
            Text(
                text = "Life Story",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        items(state.log.reversed()) { entry ->
            LogRow(entry = entry)
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = logIcon(entry.kind),
            contentDescription = null,
            tint = logTint(entry.kind),
            modifier = Modifier.size(18.dp),
        )
        Column {
            Text(
                text = "Age ${entry.age}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun StatusBanner(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = container,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun logTint(kind: LogKind): Color = when (kind) {
    LogKind.MILESTONE -> MaterialTheme.colorScheme.primary
    LogKind.MONEY -> MaterialTheme.colorScheme.secondary
    LogKind.HEALTH -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
