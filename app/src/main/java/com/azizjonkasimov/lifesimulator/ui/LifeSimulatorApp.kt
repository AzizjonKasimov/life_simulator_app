package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.ActionCategory
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyFocus
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.GoalState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind
import com.azizjonkasimov.lifesimulator.domain.model.LifeArchetype
import com.azizjonkasimov.lifesimulator.domain.model.RelationshipState
import com.azizjonkasimov.lifesimulator.domain.model.SkillSet
import com.azizjonkasimov.lifesimulator.domain.model.TimedOpportunityState
import com.azizjonkasimov.lifesimulator.update.UpdatePrompt
import com.azizjonkasimov.lifesimulator.update.rememberUpdatePromptState

@Composable
fun LifeSimulatorApp(
    viewModel: LifeSimulatorViewModel,
) {
    val uiState = viewModel.uiState
    val updatePromptState = rememberUpdatePromptState(onMessage = viewModel::showMessage)
    when {
        uiState.isLoading -> LoadingScreen()
        uiState.gameState == null -> NewLifeScreen(onStart = viewModel::startNewLife)
        else -> ActiveGameScreen(
            uiState = uiState,
            currentVersionLabel = updatePromptState.currentVersionLabel,
            updateChecking = updatePromptState.checking,
            onSelectTab = viewModel::selectTab,
            onSelectDailyFocus = viewModel::selectDailyFocus,
            onPerformAction = viewModel::performAction,
            onAdvanceDay = viewModel::advanceDay,
            onCheckForUpdates = { updatePromptState.checkForUpdates(showResult = true) },
            onReset = viewModel::resetSave,
        )
    }
    UpdatePrompt(state = updatePromptState)
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NewLifeScreen(
    onStart: (LifeArchetype) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Life Simulator",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start a fictional young-adult life and manage the dashboard: cash, career, health, stress, relationships, goals, and time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(LifeArchetype.entries) { archetype ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = archetype.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = archetype.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onStart(archetype) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Start as ${archetype.displayName}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveGameScreen(
    uiState: LifeSimulatorUiState,
    currentVersionLabel: String,
    updateChecking: Boolean,
    onSelectTab: (GameTab) -> Unit,
    onSelectDailyFocus: (DailyFocus) -> Unit,
    onPerformAction: (String) -> Unit,
    onAdvanceDay: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                GameTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = uiState.selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        icon = { Icon(imageVector = tab.icon(), contentDescription = tab.label) },
                        label = { Text(text = tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
    ) { paddingValues ->
        val state = uiState.gameState ?: return@Scaffold
        val dashboard = uiState.dashboard ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Header(
                state = state,
                dashboard = dashboard,
                messages = uiState.messages,
                lastActionDeltas = uiState.lastActionDeltas,
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (uiState.selectedTab) {
                GameTab.DASHBOARD -> DashboardTab(
                    state = state,
                    dashboard = dashboard,
                    actions = uiState.actions,
                    onSelectDailyFocus = onSelectDailyFocus,
                    onPerformAction = onPerformAction,
                    onAdvanceDay = onAdvanceDay,
                )
                GameTab.ACTIONS -> ActionsTab(actions = uiState.actions, onPerformAction = onPerformAction)
                GameTab.PROGRESS -> ProgressTab(
                    state = state,
                    currentVersionLabel = currentVersionLabel,
                    updateChecking = updateChecking,
                    onCheckForUpdates = onCheckForUpdates,
                    onReset = onReset,
                )
                GameTab.HISTORY -> HistoryTab(history = state.history)
            }
        }
    }
}

@Composable
private fun Header(
    state: GameState,
    dashboard: DashboardSnapshot,
    messages: List<String>,
    lastActionDeltas: List<ActionDelta>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dashboard.headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${state.profile.name}, ${state.profile.age} - ${state.career.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusBadge(text = dashboard.status)
        }
        AnimatedVisibility(visible = messages.isNotEmpty() || lastActionDeltas.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    messages.take(3).forEach { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (lastActionDeltas.isNotEmpty()) {
                        DeltaChipRow(deltas = lastActionDeltas)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DashboardTab(
    state: GameState,
    dashboard: DashboardSnapshot,
    actions: List<ActionAvailability>,
    onSelectDailyFocus: (DailyFocus) -> Unit,
    onPerformAction: (String) -> Unit,
    onAdvanceDay: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 72.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            TodayPlanCard(
                state = state,
                dashboard = dashboard,
                onSelectDailyFocus = onSelectDailyFocus,
            )
        }
        if (state.timedOpportunities.isNotEmpty()) {
            item {
                OpportunityPanel(state = state)
            }
        }
        item {
            DashboardMetricGrid(state = state, dashboard = dashboard)
        }
        if (dashboard.alerts.isNotEmpty()) {
            item {
                AlertPanel(alerts = dashboard.alerts)
            }
        }
        item {
            dashboard.focusGoal?.let { FocusGoalCard(goal = it) }
        }
        item {
            QuickActions(
                dashboard = dashboard,
                actions = actions,
                onPerformAction = onPerformAction,
            )
        }
        item {
            WellbeingPanel(stats = state.stats)
        }
        item {
            CareerRelationshipPanel(state = state)
        }
        item {
            RecentLog(history = state.history)
        }
        item {
            EndDayButton(onAdvanceDay = onAdvanceDay)
        }
    }
}

@Composable
private fun TodayPlanCard(
    state: GameState,
    dashboard: DashboardSnapshot,
    onSelectDailyFocus: (DailyFocus) -> Unit,
) {
    SectionCard(title = "Today's Plan") {
        Text(
            text = dashboard.pressureSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.dayPlan.activeFocus.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = state.dayPlan.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill(text = if (state.dayPlan.locked) "Locked" else "Open")
        }
        FocusPicker(
            activeFocus = state.dayPlan.activeFocus,
            enabled = !state.dayPlan.locked,
            onSelectDailyFocus = onSelectDailyFocus,
        )
        Text(
            text = focusProgressText(state),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FocusPicker(
    activeFocus: DailyFocus,
    enabled: Boolean,
    onSelectDailyFocus: (DailyFocus) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DailyFocus.entries.forEach { focus ->
            FilterChip(
                selected = focus == activeFocus,
                enabled = enabled,
                onClick = { onSelectDailyFocus(focus) },
                label = { Text(text = focus.label) },
            )
        }
    }
}

@Composable
private fun OpportunityPanel(state: GameState) {
    SectionCard(title = "Timed Opportunities") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.timedOpportunities.forEach { opportunity ->
                OpportunityRow(opportunity = opportunity, day = state.day)
            }
        }
    }
}

@Composable
private fun OpportunityRow(
    opportunity: TimedOpportunityState,
    day: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = opportunityTitle(opportunity.id),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Pill(text = "${(opportunity.expiresOnDay - day + 1).coerceAtLeast(0)}d")
        }
        Text(
            text = opportunityDescription(opportunity.id),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { opportunity.progress.coerceAtMost(opportunity.target) / opportunity.target.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${opportunity.progress.coerceAtMost(opportunity.target)} / ${opportunity.target}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EndDayButton(onAdvanceDay: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onAdvanceDay()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "End Day")
    }
}

@Composable
private fun DashboardMetricGrid(
    state: GameState,
    dashboard: DashboardSnapshot,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(label = "Cash", value = money(state.finances.cash), modifier = Modifier.weight(1f))
            MetricCard(label = "Debt", value = money(state.finances.debt), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(label = "Time", value = "${state.timeRemaining}h", modifier = Modifier.weight(1f))
            MetricCard(label = "Energy", value = "${state.stats.energy}%", modifier = Modifier.weight(1f))
            MetricCard(label = "Net", value = money(dashboard.netWorth), modifier = Modifier.weight(1f))
        }
        MetricCard(
            label = "Next bill",
            value = dashboard.nextBillLabel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AlertPanel(alerts: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Alerts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            alerts.forEach {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FocusGoalCard(goal: GoalState) {
    SectionCard(title = "Focus Goal") {
        GoalRow(goal = goal)
    }
}

@Composable
private fun QuickActions(
    dashboard: DashboardSnapshot,
    actions: List<ActionAvailability>,
    onPerformAction: (String) -> Unit,
) {
    val quickActions = dashboard.quickActionIds.mapNotNull { id -> actions.firstOrNull { it.action.id == id } }
    SectionCard(title = "Quick Actions") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quickActions.forEach { availability ->
                CompactActionRow(
                    availability = availability,
                    onPerformAction = onPerformAction,
                )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = availability.recommendationReason ?: costText(action),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DeltaChipRow(deltas = availability.previewDeltas.take(4))
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
    availability.reason?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun WellbeingPanel(stats: CoreStats) {
    SectionCard(title = "Wellbeing") {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            StatBar(label = "Health", value = stats.health)
            StatBar(label = "Mood", value = stats.mood)
            StatBar(label = "Energy", value = stats.energy)
            StatBar(label = "Stress", value = stats.stress, reverseGood = true)
            StatBar(label = "Social", value = stats.social)
        }
    }
}

@Composable
private fun CareerRelationshipPanel(state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard(title = "Career") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = state.career.title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Level ${state.career.level} - ${state.career.reputation}% reputation - ${money(state.career.salaryPerShift)} per shift",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatBar(label = "Promotion", value = state.career.promotionReadiness)
            }
        }
        SectionCard(title = "Relationships") {
            RelationshipBars(relationships = state.relationships)
        }
    }
}

@Composable
private fun RecentLog(history: List<HistoryEntry>) {
    SectionCard(title = "Recent Log") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            history.takeLast(3).asReversed().forEach { entry ->
                Text(
                    text = "Day ${entry.day}: ${entry.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = entry.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionsTab(
    actions: List<ActionAvailability>,
    onPerformAction: (String) -> Unit,
) {
    val sortedActions = actions.sortedWith(
        compareByDescending<ActionAvailability> { it.isAvailable && it.recommendationReason != null }
            .thenByDescending { it.isAvailable && it.focusMatch }
            .thenBy { it.action.category.ordinal },
    )
    val recommended = sortedActions.filter { it.isAvailable && it.recommendationReason != null }.take(5)
    val recommendedIds = recommended.map { it.action.id }.toSet()
    val grouped = sortedActions.filterNot { it.action.id in recommendedIds }.groupBy { it.action.category }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 72.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (recommended.isNotEmpty()) {
            item {
                Text(
                    text = "Recommended",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(recommended, key = { it.action.id }) { availability ->
                ActionCard(
                    availability = availability,
                    onPerformAction = onPerformAction,
                )
            }
        }
        ActionCategory.entries.forEach { category ->
            val categoryActions = grouped[category].orEmpty()
            if (categoryActions.isNotEmpty()) {
                item {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(categoryActions, key = { it.action.id }) { availability ->
                    ActionCard(
                        availability = availability,
                        onPerformAction = onPerformAction,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ActionCard(
    availability: ActionAvailability,
    onPerformAction: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val action = availability.action
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Pill(text = availability.recommendationReason ?: action.category.label)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Pill(text = costText(action))
                action.tags.forEach { Pill(text = it) }
                action.effectSummary().forEach { Pill(text = it) }
            }
            DeltaChipRow(deltas = availability.previewDeltas)
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

@Composable
private fun ProgressTab(
    state: GameState,
    currentVersionLabel: String,
    updateChecking: Boolean,
    onCheckForUpdates: () -> Unit,
    onReset: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 72.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            SectionCard(title = "Goals") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.goals.forEach { GoalRow(goal = it) }
                }
            }
        }
        item {
            SkillSection(skills = state.skills)
        }
        item {
            FinanceSection(state = state)
        }
        item {
            SectionCard(title = "Relationships") {
                RelationshipBars(relationships = state.relationships)
            }
        }
        if (state.modifiers.isNotEmpty()) {
            item {
                SectionCard(title = "Active Modifiers") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.modifiers.forEach { modifier ->
                            Text(text = "${modifier.title}: ${modifier.daysRemaining} days", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = modifier.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            AppUpdateSection(
                currentVersionLabel = currentVersionLabel,
                updateChecking = updateChecking,
                onCheckForUpdates = onCheckForUpdates,
            )
        }
        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
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
            text = { Text(text = "This clears the current single save and returns to the archetype picker.") },
        )
    }
}

@Composable
private fun HistoryTab(history: List<HistoryEntry>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 72.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(history.asReversed()) { entry ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Day ${entry.day}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Pill(text = entry.kind.label())
                    }
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = entry.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillSection(skills: SkillSet) {
    SectionCard(title = "Skills") {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            StatBar(label = "Knowledge", value = skills.knowledge.coerceAtMost(100))
            StatBar(label = "Fitness", value = skills.fitness.coerceAtMost(100))
            StatBar(label = "Career XP", value = skills.career % 100)
            StatBar(label = "Communication", value = skills.communication.coerceAtMost(100))
            StatBar(label = "Creativity", value = skills.creativity.coerceAtMost(100))
        }
    }
}

@Composable
private fun FinanceSection(state: GameState) {
    SectionCard(title = "Finances") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InlineMetric(label = "Cash", value = money(state.finances.cash), modifier = Modifier.weight(1f))
                InlineMetric(label = "Debt", value = money(state.finances.debt), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InlineMetric(label = "Weekly bill", value = money(state.finances.weeklyLivingCost), modifier = Modifier.weight(1f))
                InlineMetric(label = "Credit", value = state.finances.creditScore.toString(), modifier = Modifier.weight(1f))
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
    SectionCard(title = "App Updates") {
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
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RelationshipBars(relationships: RelationshipState) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        StatBar(label = "Family", value = relationships.family)
        StatBar(label = "Friends", value = relationships.friends)
        StatBar(label = "Network", value = relationships.network)
    }
}

@Composable
private fun GoalRow(goal: GoalState) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Pill(text = if (goal.isComplete) "Done" else goal.category.label)
        }
        Text(
            text = goal.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { goal.percent / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${goal.progress.coerceAtMost(goal.target)} / ${goal.target}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InlineMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun StatBar(
    label: String,
    value: Int,
    reverseGood: Boolean = false,
) {
    val normalized = value.coerceIn(0, 100) / 100f
    val displayValue = value.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "$displayValue%", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { if (reverseGood) 1f - normalized else normalized },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Pill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DeltaChipRow(deltas: List<ActionDelta>) {
    if (deltas.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        deltas.forEach { delta ->
            DeltaChip(delta = delta)
        }
    }
}

@Composable
private fun DeltaChip(delta: ActionDelta) {
    val isGood = if (delta.positiveIsGood) delta.amount > 0 else delta.amount < 0
    val container = when {
        delta.amount == 0 -> MaterialTheme.colorScheme.surfaceVariant
        isGood -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    }
    val content = when {
        delta.amount == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        isGood -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = container,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = "${delta.label} ${signed(delta.amount)}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun focusProgressText(state: GameState): String =
    if (state.dayPlan.activeFocus == DailyFocus.BALANCED) {
        "${state.dayPlan.categoriesCompleted.size.coerceAtMost(3)} / 3 categories for balanced reward"
    } else {
        "${state.dayPlan.focusActionsCompleted} matching actions today"
    }

private fun opportunityTitle(id: String): String = when (id) {
    "bill_buffer" -> "Bill Buffer"
    "recovery_window" -> "Recovery Window"
    "promotion_push" -> "Promotion Push"
    "reconnect" -> "Reconnect"
    "debt_brake" -> "Debt Brake"
    else -> id
}

private fun opportunityDescription(id: String): String = when (id) {
    "bill_buffer" -> "Reach a cash buffer before bills hit."
    "recovery_window" -> "Bring stress down before burnout compounds."
    "promotion_push" -> "Finish the current promotion push in time."
    "reconnect" -> "Complete social actions or rebuild relationship average."
    "debt_brake" -> "Reduce debt enough to relieve credit pressure."
    else -> "Complete this timed pressure goal."
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

private fun GameTab.icon(): ImageVector = when (this) {
    GameTab.DASHBOARD -> Icons.Filled.Dashboard
    GameTab.ACTIONS -> Icons.AutoMirrored.Filled.FormatListBulleted
    GameTab.PROGRESS -> Icons.Filled.BarChart
    GameTab.HISTORY -> Icons.Filled.History
}

private fun HistoryKind.label(): String = when (this) {
    HistoryKind.ACTION -> "Action"
    HistoryKind.CAREER -> "Career"
    HistoryKind.DAY -> "Day"
    HistoryKind.EVENT -> "Event"
    HistoryKind.FINANCE -> "Finance"
    HistoryKind.GOAL -> "Goal"
    HistoryKind.RELATIONSHIP -> "Social"
    HistoryKind.SYSTEM -> "System"
}

private fun costText(action: DailyActionDefinition): String =
    "${action.timeCost}h, ${action.energyCost} energy" +
        if (action.moneyCost > 0) ", ${money(action.moneyCost)}" else ""

private fun DailyActionDefinition.effectSummary(): List<String> = buildList {
    if (effect.cashDelta > 0) add("+cash")
    if (id == "work_shift" || id == "freelance_gig") add("+income")
    if (effect.debtDelta < 0) add("-debt")
    if (effect.careerXpDelta > 0 || effect.promotionReadinessDelta > 0) add("+career")
    if (effect.healthDelta > 0 || effect.fitnessDelta > 0) add("+health")
    if (effect.moodDelta > 0) add("+mood")
    if (effect.stressDelta < 0) add("-stress")
    if (effect.familyDelta > 0 || effect.friendsDelta > 0 || effect.networkDelta > 0) add("+social")
}.take(4)

private fun money(value: Int): String =
    if (value < 0) "-${money(-value)}" else "\$$value"
