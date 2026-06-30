package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry

@Composable
internal fun DashboardScreen(
    state: GameState,
    dashboard: DashboardSnapshot,
    actions: List<ActionAvailability>,
    onPerformAction: (String) -> Unit,
    onAdvanceDay: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { CashFlowCard(state = state, dashboard = dashboard) }
        item { CareerCard(state = state) }
        item { BusinessCard(state = state, dashboard = dashboard) }
        item {
            DoNextCard(
                suggestionIds = dashboard.suggestionIds,
                actions = actions,
                onPerformAction = onPerformAction,
            )
        }
        item { RecentLogCard(history = state.history) }
        item { EndDayButton(actionsTaken = state.actionsToday, onAdvanceDay = onAdvanceDay) }
    }
}

@Composable
private fun CashFlowCard(
    state: GameState,
    dashboard: DashboardSnapshot,
) {
    SectionCard(
        title = "This Week",
        icon = UiIcons.finances,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile(
                icon = UiIcons.bill,
                label = "Weekly bill",
                value = money(dashboard.weeklyCost),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                icon = UiIcons.runway,
                label = "Runway",
                value = "${runwayDays(state, dashboard.weeklyCost)} days",
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = dashboard.pressureSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (dashboard.alerts.isNotEmpty()) {
            ChipFlowRow {
                dashboard.alerts.forEach { alert ->
                    LabelChip(text = alert, icon = UiIcons.pressure, tone = ChipTone.WARN)
                }
            }
        }
    }
}

@Composable
private fun CareerCard(state: GameState) {
    SectionCard(title = "Career", icon = if (state.career.employed) UiIcons.career else UiIcons.jobSearch) {
        if (state.career.employed) {
            ProgressTrack(
                icon = UiIcons.career,
                label = "Promotion progress",
                value = "${state.career.title} · ${money(state.career.salaryPerShift)}/shift",
                progress = state.career.promotionReadiness,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(label = "Level", value = state.career.level.toString(), modifier = Modifier.weight(1f))
                MiniStat(label = "Reputation", value = "${state.career.reputation}%", modifier = Modifier.weight(1f))
            }
        } else {
            ProgressTrack(
                icon = UiIcons.jobSearch,
                label = "Job search",
                value = "Interview unlocks at 40%",
                progress = state.jobSearch.searchProgress,
            )
            Text(
                text = if (state.jobSearch.searchProgress >= 40) {
                    "You're ready to interview — prep more to raise your odds."
                } else {
                    "Apply and prep to build toward a real interview."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BusinessCard(
    state: GameState,
    dashboard: DashboardSnapshot,
) {
    SectionCard(title = "Business", icon = UiIcons.business) {
        if (!state.business.started) {
            Text(
                text = "Launch a side hustle to earn weekly income. Sign clients, build reputation, and upgrade as it grows.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val maxClients = state.business.tier.maxClients.coerceAtLeast(1)
            ProgressTrack(
                icon = UiIcons.business,
                label = state.business.tier.label,
                value = "${state.business.clients}/${state.business.tier.maxClients} clients · Rep ${state.business.reputation}%",
                progress = state.business.clients * 100 / maxClients,
                accent = MaterialTheme.colorScheme.secondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(
                    label = "Weekly income",
                    value = money(dashboard.businessWeeklyNet),
                    modifier = Modifier.weight(1f),
                )
                MiniStat(
                    label = "Per client",
                    value = money(state.business.tier.revenuePerClient),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DoNextCard(
    suggestionIds: List<String>,
    actions: List<ActionAvailability>,
    onPerformAction: (String) -> Unit,
) {
    val suggestions = suggestionIds.mapNotNull { id -> actions.firstOrNull { it.action.id == id } }
    if (suggestions.isEmpty()) return
    SectionCard(title = "Do Next", icon = UiIcons.updates) {
        suggestions.forEach { availability ->
            CompactActionRow(availability = availability, onPerformAction = onPerformAction)
        }
    }
}

@Composable
private fun CompactActionRow(
    availability: ActionAvailability,
    onPerformAction: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val action = availability.action
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = availability.oddsPercent?.let { "~$it% success · ${costText(action)}" } ?: costText(action),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            enabled = availability.isAvailable,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPerformAction(action.id)
            },
        ) {
            Text(text = "Do")
        }
    }
}

@Composable
private fun RecentLogCard(history: List<HistoryEntry>) {
    SectionCard(title = "Recent", icon = UiIcons.updates) {
        if (history.isEmpty()) {
            Text(
                text = "No history yet — take an action to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            history.takeLast(3).asReversed().forEach { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Day ${entry.day} · ${entry.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = entry.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EndDayButton(
    actionsTaken: Int,
    onAdvanceDay: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onAdvanceDay()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (actionsTaken > 0) "End Day · $actionsTaken done" else "End Day",
            fontWeight = FontWeight.SemiBold,
        )
    }
}
