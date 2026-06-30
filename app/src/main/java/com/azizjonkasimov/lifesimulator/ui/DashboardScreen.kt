package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.azizjonkasimov.lifesimulator.domain.model.DailyFocus
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.TimedOpportunityState

@Composable
internal fun DashboardScreen(
    state: GameState,
    dashboard: DashboardSnapshot,
    actions: List<ActionAvailability>,
    onSelectDailyFocus: (DailyFocus) -> Unit,
    onPerformAction: (String) -> Unit,
    onAdvanceDay: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { MoneyLoopCard(state = state, dashboard = dashboard) }
        item { DailyFocusCard(state = state, onSelectDailyFocus = onSelectDailyFocus) }
        if (state.timedOpportunities.isNotEmpty()) {
            item { OpportunitiesCard(state = state) }
        }
        item {
            PriorityActionsCard(
                dashboard = dashboard,
                actions = actions,
                onPerformAction = onPerformAction,
            )
        }
        item { RecentLogCard(history = state.history) }
        item { EndDayButton(actionsTaken = state.dayPlan.actionsTaken, onAdvanceDay = onAdvanceDay) }
    }
}

@Composable
private fun MoneyLoopCard(
    state: GameState,
    dashboard: DashboardSnapshot,
) {
    SectionCard(
        title = "Money Loop",
        icon = UiIcons.money,
        trailing = {
            Text(
                text = "Net ${money(dashboard.netWorth)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        ProgressTrack(
            icon = if (state.career.employed) UiIcons.career else UiIcons.jobSearch,
            label = if (state.career.employed) "Career" else "Job Search",
            value = if (state.career.employed) {
                "${state.career.title} · ${money(state.career.salaryPerShift)}/shift · Rep ${state.career.reputation}%"
            } else {
                "${state.jobSearch.applicationsSent} apps · ${state.jobSearch.interviewReadiness}% interview-ready"
            },
            progress = if (state.career.employed) state.career.promotionReadiness else state.jobSearch.offerProgress,
        )
        ProgressTrack(
            icon = UiIcons.business,
            label = "Business · ${state.business.stage.label}",
            value = "${state.business.leads} leads · ${state.business.activeProjects} active · ${state.business.completedProjects} done",
            progress = (state.business.pipelineValue * 100 / 250).coerceIn(0, 100),
            accent = MaterialTheme.colorScheme.secondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile(
                icon = UiIcons.bill,
                label = "Next bill",
                value = dashboard.nextBillLabel,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                icon = UiIcons.runway,
                label = "Runway",
                value = "${runwayDays(state)}d",
                modifier = Modifier.weight(1f),
            )
        }
        StatBar(label = "Pressure", value = pressureMeterValue(state), reverseGood = true)
        Text(
            text = dashboard.pressureSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (dashboard.alerts.isNotEmpty()) {
            ChipFlowRow {
                dashboard.alerts.take(2).forEach { alert ->
                    LabelChip(text = alert, icon = UiIcons.pressure, tone = ChipTone.WARN)
                }
            }
        }
    }
}

@Composable
private fun DailyFocusCard(
    state: GameState,
    onSelectDailyFocus: (DailyFocus) -> Unit,
) {
    val plan = state.dayPlan
    SectionCard(
        title = "Daily Focus",
        icon = focusIcon(plan.activeFocus),
        trailing = {
            LabelChip(
                text = if (plan.locked) "Locked" else "Open",
                tone = if (plan.locked) ChipTone.NEUTRAL else ChipTone.SUCCESS,
            )
        },
    ) {
        Text(
            text = plan.activeFocus.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = plan.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!plan.locked) {
            ChipFlowRow {
                DailyFocus.entries.forEach { focus ->
                    FilterChip(
                        selected = focus == plan.activeFocus,
                        onClick = { onSelectDailyFocus(focus) },
                        label = { Text(text = focus.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = focusProgressText(state),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!plan.locked && plan.activeFocus != plan.recommendedFocus) {
                Text(
                    text = "Suggested: ${plan.recommendedFocus.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun OpportunitiesCard(state: GameState) {
    SectionCard(title = "Opportunities", icon = UiIcons.bill) {
        state.timedOpportunities.forEach { opportunity ->
            OpportunityRow(opportunity = opportunity, day = state.day)
        }
    }
}

@Composable
private fun OpportunityRow(
    opportunity: TimedOpportunityState,
    day: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = opportunityTitle(opportunity.id),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = opportunityDescription(opportunity.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LabelChip(
                text = "${(opportunity.expiresOnDay - day + 1).coerceAtLeast(0)}d left",
                tone = ChipTone.WARN,
            )
        }
        val target = opportunity.target.coerceAtLeast(1)
        ProgressTrack(
            icon = opportunityIcon(opportunity.id),
            label = "Progress",
            value = "${opportunity.progress.coerceAtMost(opportunity.target)} / ${opportunity.target}",
            progress = opportunity.progress.coerceAtMost(opportunity.target) * 100 / target,
            accent = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun PriorityActionsCard(
    dashboard: DashboardSnapshot,
    actions: List<ActionAvailability>,
    onPerformAction: (String) -> Unit,
) {
    val quickActions = dashboard.quickActionIds
        .mapNotNull { id -> actions.firstOrNull { it.action.id == id } }
        .take(3)
    SectionCard(title = "Do Next", icon = UiIcons.career) {
        if (quickActions.isEmpty()) {
            Text(
                text = "No suggested actions right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            quickActions.forEach { availability ->
                CompactActionRow(availability = availability, onPerformAction = onPerformAction)
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                if (availability.previewDeltas.isNotEmpty()) {
                    ChipFlowRow {
                        availability.previewDeltas.take(3).forEach { DeltaChip(delta = it) }
                    }
                }
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
        if (!availability.isAvailable && availability.reason != null) {
            Text(
                text = availability.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
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
            history.takeLast(2).asReversed().forEach { entry ->
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

private fun focusProgressText(state: GameState): String =
    if (state.dayPlan.activeFocus == DailyFocus.BALANCED) {
        "${state.dayPlan.categoriesCompleted.size.coerceAtMost(3)} / 3 categories for balanced reward"
    } else {
        "${state.dayPlan.focusActionsCompleted} matching actions today"
    }
