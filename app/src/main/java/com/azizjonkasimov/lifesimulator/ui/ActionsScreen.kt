package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val sortedActions = actions.sortedWith(
        compareByDescending<ActionAvailability> { it.isAvailable && it.recommendationReason != null }
            .thenByDescending { it.isAvailable && it.focusMatch }
            .thenBy { it.action.category.ordinal },
    )
    val grouped = sortedActions.groupBy { actionSectionKey(it.action) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        actionSectionSpecs().forEach { section ->
            val sectionActions = grouped[section.key].orEmpty()
            if (sectionActions.isNotEmpty()) {
                item(key = "header_${section.key}") {
                    SectionHeader(icon = section.icon, title = section.label)
                }
                items(sectionActions, key = { it.action.id }) { availability ->
                    ActionCard(availability = availability, onPerformAction = onPerformAction)
                }
            }
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

@Composable
private fun ActionCard(
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                IconBadgeTile(
                    icon = actionIcon(action),
                    contentDescription = action.category.label,
                    iconSize = 22.dp,
                    padding = 9.dp,
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
                        text = action.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (availability.recommendationReason != null) {
                    LabelChip(
                        text = availability.recommendationReason,
                        icon = recommendationIcon(availability),
                        tone = recommendationTone(availability),
                    )
                }
            }
            ChipFlowRow {
                LabelChip(text = costText(action), icon = UiIcons.time)
                if (availability.focusMatch && availability.recommendationReason == null) {
                    LabelChip(text = "Focus match", icon = Icons.AutoMirrored.Filled.TrendingUp, tone = ChipTone.ACCENT)
                }
            }
            if (availability.previewDeltas.isNotEmpty()) {
                ChipFlowRow {
                    availability.previewDeltas.forEach { DeltaChip(delta = it) }
                }
            }
            if (!availability.isAvailable && availability.reason != null) {
                Text(
                    text = availability.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPerformAction(action.id)
                },
                enabled = availability.isAvailable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Do Action")
            }
        }
    }
}

private data class ActionSectionSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

private fun actionSectionSpecs(): List<ActionSectionSpec> = listOf(
    ActionSectionSpec("money", "Make Money", UiIcons.money),
    ActionSectionSpec("career", "Get Hired / Career", UiIcons.career),
    ActionSectionSpec("business", "Build Business", UiIcons.business),
    ActionSectionSpec("recover", "Recover", UiIcons.recover),
    ActionSectionSpec("connect", "Connect", UiIcons.connect),
)

private fun actionSectionKey(action: DailyActionDefinition): String = when {
    action.id in setOf("temp_shift", "budget_review", "work_shift", "overtime", "client_project") -> "money"
    action.category == ActionCategory.BUSINESS -> "business"
    action.category == ActionCategory.WELLBEING -> "recover"
    action.category == ActionCategory.SOCIAL -> "connect"
    action.id in setOf("send_applications", "interview_prep", "attend_interview", "manager_check_in", "study_course", "networking") -> "career"
    action.category == ActionCategory.MONEY || action.category == ActionCategory.WORK -> "money"
    else -> "career"
}
