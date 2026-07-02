package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.azizjonkasimov.lifesimulator.domain.engine.InteractionOption
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.Person
import com.azizjonkasimov.lifesimulator.domain.model.RelationType

// Living people are grouped so the ones you act on most sit at the top.
private val relationOrder = listOf(
    RelationType.SPOUSE, RelationType.PARTNER, RelationType.CHILD,
    RelationType.MOTHER, RelationType.FATHER, RelationType.SIBLING,
    RelationType.FRIEND, RelationType.COWORKER, RelationType.PET,
)

@Composable
fun PeopleScreen(
    state: GameState,
    interactionsFor: (Person) -> List<InteractionOption>,
    onInteract: (String, String) -> Unit,
) {
    val living = state.relationships.filter { it.alive }
        .sortedBy { relationOrder.indexOf(it.relation).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    val gone = state.relationships.filter { !it.alive }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (living.isEmpty() && gone.isEmpty()) {
            item {
                Text(
                    text = "There's no one in your life right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(living) { person ->
            PersonCard(person = person, interactions = interactionsFor(person), onInteract = onInteract)
        }
        if (gone.isNotEmpty()) {
            item {
                Text(
                    text = "In memory",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(gone) { person -> MemoryRow(person = person) }
        }
    }
}

@Composable
private fun PersonCard(
    person: Person,
    interactions: List<InteractionOption>,
    onInteract: (String, String) -> Unit,
) {
    SectionCard(title = person.name, icon = UiIcons.person) {
        Text(
            text = "${person.relation.label} · Age ${person.age}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Relationship", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${person.relationship}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MeterLine(progress = person.relationship / 100f, color = meterColor(person.relationship))
        }
        ChipFlowRow {
            interactions.forEach { option ->
                OutlinedButton(
                    onClick = { onInteract(person.id, option.id) },
                    enabled = option.enabled,
                ) {
                    Text(text = option.label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MemoryRow(person: Person) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = person.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "your ${person.relation.label.lowercase()}, passed at ${person.age}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
