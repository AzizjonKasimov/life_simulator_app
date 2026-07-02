package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.engine.ActivityOption

@Composable
fun ActivitiesScreen(
    activities: List<ActivityOption>,
    onActivity: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "What do you want to do this year?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(activities) { option ->
            ActivityRow(option = option, onActivity = onActivity)
        }
    }
}

@Composable
private fun ActivityRow(
    option: ActivityOption,
    onActivity: (String) -> Unit,
) {
    val activity = option.activity
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = option.enabled) { onActivity(activity.id) },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadgeTile(
                icon = activityIcon(activity.id),
                container = if (option.enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                iconSize = 20.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = activity.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                option.reason != null -> LabelChip(text = option.reason, tone = ChipTone.NEUTRAL)
                activity.cost > 0 -> LabelChip(text = money(activity.cost), tone = ChipTone.SUCCESS)
            }
        }
    }
}

private fun activityIcon(id: String): ImageVector = when (id) {
    "gym" -> Icons.Filled.FitnessCenter
    "study" -> Icons.Filled.School
    "doctor" -> Icons.Filled.LocalHospital
    "meditate" -> Icons.Filled.SelfImprovement
    "volunteer" -> Icons.Filled.VolunteerActivism
    "night_out" -> Icons.Filled.Nightlife
    "vacation" -> Icons.Filled.FlightTakeoff
    "date" -> Icons.Filled.Favorite
    "adopt_pet" -> Icons.Filled.Pets
    "enroll_university", "enroll_grad" -> Icons.Filled.School
    "find_job", "quit_job" -> Icons.Filled.Work
    else -> Icons.Filled.Star
}
