package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionCategory
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition

@Composable
internal fun ActionsScreen(
    actions: List<ActionAvailability>,
    onPerformAction: (String) -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(ALL_FILTER) }
    val grouped = actions.groupBy { actionSectionKey(it.action) }
    val sections = actionSectionSpecs().filter { selectedFilter == ALL_FILTER || it.key == selectedFilter }

    Column(modifier = Modifier.fillMaxSize()) {
        ActionFilterBar(selected = selectedFilter, onSelect = { selectedFilter = it })
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            sections.forEach { section ->
                val sectionActions = grouped[section.key]
                    .orEmpty()
                    .sortedWith(
                        compareByDescending<ActionAvailability> { it.isAvailable }
                            .thenByDescending { it.isAvailable && it.recommendationReason != null },
                    )
                if (sectionActions.isNotEmpty()) {
                    if (selectedFilter == ALL_FILTER) {
                        item(key = "header_${section.key}") {
                            SectionHeader(icon = section.icon, title = section.label)
                        }
                    }
                    items(sectionActions, key = { it.action.id }) { availability ->
                        CompactActionItem(availability = availability, onPerformAction = onPerformAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionFilterBar(
    selected: String,
    onSelect: (String) -> Unit,
) {
    ChipFlowRow {
        actionFilters.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelect(key) },
                label = { Text(text = label) },
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * One tight row per action: glyph, title, a single info line (cost · reward · odds),
 * and a Do button. Far more scannable than full cards when there are many actions.
 */
@Composable
private fun CompactActionItem(
    availability: ActionAvailability,
    onPerformAction: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val action = availability.action
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadgeTile(
                icon = actionIcon(action),
                contentDescription = action.category.label,
                iconSize = 20.dp,
                padding = 8.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (availability.isAvailable) compactSubline(availability) else (availability.reason ?: "Unavailable"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (availability.isAvailable) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPerformAction(action.id)
                },
                enabled = availability.isAvailable,
            ) {
                Text(text = "Do")
            }
        }
    }
}

/** Cost, the single most salient reward, and odds when the action is a gamble. */
private fun compactSubline(availability: ActionAvailability): String {
    val parts = buildList {
        add(compactCostText(availability.action))
        rewardHint(availability)?.let { add(it) }
        availability.oddsPercent?.let { add("~$it%") }
    }
    return parts.joinToString(" · ")
}

private fun rewardHint(availability: ActionAvailability): String? {
    val deltas = availability.previewDeltas
    deltas.firstOrNull { it.label == "Cash" && it.amount > 0 }?.let { return "+\$${it.amount}" }
    deltas.firstOrNull { it.amount > 0 && it.positiveIsGood && it.label != "Time" }
        ?.let { return "+${it.amount} ${it.label}" }
    return null
}

private data class ActionSectionSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

private const val ALL_FILTER = "all"

/** Short chip labels for the filter bar; keys line up with [actionSectionKey]. */
private val actionFilters: List<Pair<String, String>> = listOf(
    ALL_FILTER to "All",
    "money" to "Money",
    "career" to "Career",
    "business" to "Business",
    "recover" to "Health",
    "connect" to "People",
)

private fun actionSectionSpecs(): List<ActionSectionSpec> = listOf(
    ActionSectionSpec("money", "Make Money", UiIcons.money),
    ActionSectionSpec("career", "Career & Jobs", UiIcons.career),
    ActionSectionSpec("business", "Build Business", UiIcons.business),
    ActionSectionSpec("recover", "Recover", UiIcons.recover),
    ActionSectionSpec("connect", "Connect", UiIcons.connect),
)

private fun actionSectionKey(action: DailyActionDefinition): String = when {
    action.id in setOf("gig_work", "work_shift", "overtime") -> "money"
    action.id in setOf("apply_jobs", "interview_prep", "attend_interview", "manager_check_in", "study", "network") -> "career"
    action.category == ActionCategory.BUSINESS -> "business"
    action.category == ActionCategory.WELLBEING -> "recover"
    action.category == ActionCategory.SOCIAL -> "connect"
    action.category == ActionCategory.WORK || action.category == ActionCategory.MONEY -> "money"
    else -> "career"
}
