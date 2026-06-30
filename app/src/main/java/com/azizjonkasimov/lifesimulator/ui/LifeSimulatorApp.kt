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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.ActionCategory
import com.azizjonkasimov.lifesimulator.domain.model.BusinessStage
import com.azizjonkasimov.lifesimulator.domain.model.CoreStats
import com.azizjonkasimov.lifesimulator.domain.model.DailyFocus
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition
import com.azizjonkasimov.lifesimulator.domain.model.DashboardSnapshot
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryEntry
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind
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
    onStart: () -> Unit,
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
                text = "Start unemployed with a few bills due, then build steady work and a client pipeline.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Normal start",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Alex Rivers, 22. Unemployed, $180 cash, $350 debt, modest skills, and one recoverable pressure curve.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Start Life")
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        imageVector = statusIcon(state),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${state.career.title} - ${dashboard.headline}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StatusBadge(text = dashboard.status)
        }
        HudStrip(state = state, dashboard = dashboard)
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
private fun HudStrip(
    state: GameState,
    dashboard: DashboardSnapshot,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconMetricPill(
            icon = Icons.Filled.AttachMoney,
            label = "Cash",
            value = money(state.finances.cash),
            modifier = Modifier.weight(1f),
        )
        IconMetricPill(
            icon = Icons.Filled.MoneyOff,
            label = "Debt",
            value = money(state.finances.debt),
            modifier = Modifier.weight(1f),
        )
        IconMetricPill(
            icon = Icons.Filled.AccessTime,
            label = "Time",
            value = "${state.timeRemaining}h",
            modifier = Modifier.weight(1f),
        )
        IconMetricPill(
            icon = Icons.Filled.Favorite,
            label = "Energy",
            value = "${state.stats.energy}%",
            modifier = Modifier.weight(1f),
        )
    }
    Text(
        text = "${dashboard.nextBillLabel} - Net ${money(dashboard.netWorth)}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun IconMetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            MoneyLoopCard(state = state, dashboard = dashboard)
        }
        item {
            FocusHudRow(
                state = state,
                onSelectDailyFocus = onSelectDailyFocus,
            )
        }
        if (state.timedOpportunities.isNotEmpty()) {
            item {
                OpportunityPanel(state = state)
            }
        }
        item {
            PriorityActions(
                dashboard = dashboard,
                actions = actions,
                onPerformAction = onPerformAction,
            )
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
private fun FocusHudRow(
    state: GameState,
    onSelectDailyFocus: (DailyFocus) -> Unit,
) {
    SectionCard(title = "Daily Focus") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = focusIcon(state.dayPlan.activeFocus),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Pill(text = if (state.dayPlan.locked) "Locked" else "Open")
        }
        FocusPicker(
            activeFocus = state.dayPlan.activeFocus,
            enabled = !state.dayPlan.locked,
            onSelectDailyFocus = onSelectDailyFocus,
        )
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
            if (state.dayPlan.activeFocus != state.dayPlan.recommendedFocus) {
                Text(
                    text = "Suggested: ${state.dayPlan.recommendedFocus.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MoneyLoopCard(
    state: GameState,
    dashboard: DashboardSnapshot,
) {
    val totalWeeklyCost = totalWeeklyCost(state)
    val runwayDays = if (totalWeeklyCost <= 0) 0 else (state.finances.cash * 7 / totalWeeklyCost).coerceAtLeast(0)
    val careerProgress = if (state.career.employed) state.career.promotionReadiness else state.jobSearch.offerProgress
    val careerLabel = if (state.career.employed) {
        "${money(state.career.salaryPerShift)} shift pay"
    } else {
        "${state.jobSearch.applicationsSent} apps - ${state.jobSearch.interviewReadiness}% ready"
    }
    SectionCard(title = "Money Loop") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MeterTile(
                icon = if (state.career.employed) Icons.Filled.Work else Icons.Filled.Search,
                label = if (state.career.employed) "Career" else "Job offer",
                value = careerLabel,
                progress = careerProgress,
                modifier = Modifier.weight(1f),
            )
            MeterTile(
                icon = Icons.Filled.BusinessCenter,
                label = "Business",
                value = "${state.business.leads} leads - ${state.business.activeProjects} active",
                progress = (state.business.pipelineValue * 100 / 250).coerceIn(0, 100),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconMetricTile(
                icon = Icons.Filled.Event,
                label = "Next bill",
                value = dashboard.nextBillLabel,
                modifier = Modifier.weight(1f),
            )
            IconMetricTile(
                icon = Icons.Filled.CreditCard,
                label = "Runway",
                value = "${runwayDays}d",
                modifier = Modifier.weight(1f),
            )
        }
        MeterRow(
            icon = Icons.Filled.Warning,
            label = "Pressure",
            value = pressureMeterValue(state),
            reverseGood = true,
        )
        Text(
            text = dashboard.pressureSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (dashboard.alerts.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dashboard.alerts.take(3).forEach { alert ->
                    IconBadge(icon = Icons.Filled.Warning, text = alert)
                }
            }
        }
    }
}

@Composable
private fun MeterTile(
    icon: ImageVector,
    label: String,
    value: String,
    progress: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = meterColor(progress),
            )
        }
    }
}

@Composable
private fun IconMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MeterRow(
    icon: ImageVector,
    label: String,
    value: Int,
    reverseGood: Boolean = false,
) {
    val progress = value.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = meterColor(progress, reverseGood),
                    modifier = Modifier.size(18.dp),
                )
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = "$progress%", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = meterColor(progress, reverseGood),
        )
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
    SectionCard(title = "Opportunities") {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(
                    imageVector = opportunityIcon(opportunity.id),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = opportunityTitle(opportunity.id),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
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
private fun PriorityActions(
    dashboard: DashboardSnapshot,
    actions: List<ActionAvailability>,
    onPerformAction: (String) -> Unit,
) {
    val quickActions = dashboard.quickActionIds
        .mapNotNull { id -> actions.firstOrNull { it.action.id == id } }
        .take(3)
    SectionCard(title = "Priority Actions") {
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
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Icon(
                imageVector = actionIcon(action),
                contentDescription = action.category.label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconBadge(
                    icon = recommendationIcon(availability),
                    text = availability.recommendationReason ?: action.category.label,
                )
            }
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
private fun SectionHeader(
    icon: ImageVector,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
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
    val sections = actionSectionSpecs()
    val grouped = sortedActions.groupBy { actionSectionKey(it.action) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 72.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        sections.forEach { section ->
            val categoryActions = grouped[section.key].orEmpty()
            if (categoryActions.isNotEmpty()) {
                item {
                    SectionHeader(icon = section.icon, title = section.label)
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        imageVector = actionIcon(action),
                        contentDescription = action.category.label,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
                IconBadge(
                    icon = recommendationIcon(availability),
                    text = availability.recommendationReason ?: action.category.label,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconBadge(icon = Icons.Filled.AccessTime, text = costText(action))
                if (
                    availability.focusMatch &&
                    availability.recommendationReason?.contains("focus", ignoreCase = true) != true
                ) {
                    IconBadge(icon = Icons.AutoMirrored.Filled.TrendingUp, text = "Focus")
                }
                action.tags.take(2).forEach { Pill(text = it) }
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
            CareerSection(state = state)
        }
        item {
            BusinessSection(state = state)
        }
        item {
            FinanceSection(state = state)
        }
        item {
            WellbeingPanel(stats = state.stats)
        }
        item {
            SectionCard(title = "Relationships") {
                RelationshipBars(relationships = state.relationships)
            }
        }
        item {
            SkillSection(skills = state.skills)
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
            text = { Text(text = "This clears the current single save and returns to the start screen.") },
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
private fun CareerSection(state: GameState) {
    SectionCard(title = "Career") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InlineMetric(label = "Title", value = state.career.title, modifier = Modifier.weight(1f))
            InlineMetric(
                label = "Status",
                value = if (state.career.employed) "Employed" else "Searching",
                modifier = Modifier.weight(1f),
            )
        }
        if (state.career.employed) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InlineMetric(label = "Level", value = state.career.level.toString(), modifier = Modifier.weight(1f))
                InlineMetric(label = "Shift pay", value = money(state.career.salaryPerShift), modifier = Modifier.weight(1f))
                InlineMetric(label = "Reputation", value = "${state.career.reputation}%", modifier = Modifier.weight(1f))
            }
            StatBar(label = "Promotion", value = state.career.promotionReadiness)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InlineMetric(label = "Applications", value = state.jobSearch.applicationsSent.toString(), modifier = Modifier.weight(1f))
                InlineMetric(label = "Interview", value = "${state.jobSearch.interviewReadiness}%", modifier = Modifier.weight(1f))
            }
            StatBar(label = "Offer progress", value = state.jobSearch.offerProgress)
        }
    }
}

@Composable
private fun BusinessSection(state: GameState) {
    SectionCard(title = "Business") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InlineMetric(label = "Stage", value = state.business.stage.label, modifier = Modifier.weight(1f))
            InlineMetric(label = "Overhead", value = money(businessOverheadFor(state.business.stage)), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InlineMetric(label = "Leads", value = state.business.leads.toString(), modifier = Modifier.weight(1f))
            InlineMetric(label = "Active", value = state.business.activeProjects.toString(), modifier = Modifier.weight(1f))
            InlineMetric(label = "Completed", value = state.business.completedProjects.toString(), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InlineMetric(label = "Trust", value = "${state.business.clientTrust}%", modifier = Modifier.weight(1f))
            InlineMetric(label = "Reputation", value = "${state.business.reputation}%", modifier = Modifier.weight(1f))
        }
        StatBar(label = "Pipeline value", value = (state.business.pipelineValue * 100 / 250).coerceIn(0, 100))
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = metricIcon(label),
                    contentDescription = null,
                    tint = meterColor(displayValue, reverseGood),
                    modifier = Modifier.size(16.dp),
                )
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = "$displayValue%", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { normalized },
            modifier = Modifier.fillMaxWidth(),
            color = meterColor(displayValue, reverseGood),
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
    else -> "Complete this timed pressure opportunity."
}

private fun businessOverheadFor(stage: BusinessStage): Int = when (stage) {
    BusinessStage.IDEA,
    BusinessStage.SIDE_HUSTLE -> 0
    BusinessStage.RELIABLE_PIPELINE -> 45
    BusinessStage.SMALL_BUSINESS -> 90
}

private fun totalWeeklyCost(state: GameState): Int =
    state.finances.weeklyLivingCost + businessOverheadFor(state.business.stage)

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

private fun GameTab.icon(): ImageVector = when (this) {
    GameTab.DASHBOARD -> Icons.Filled.Dashboard
    GameTab.ACTIONS -> Icons.AutoMirrored.Filled.FormatListBulleted
    GameTab.PROGRESS -> Icons.Filled.BarChart
    GameTab.HISTORY -> Icons.Filled.History
}

private fun statusIcon(state: GameState): ImageVector = when {
    !state.career.employed -> Icons.Filled.Search
    state.business.activeProjects > 0 -> Icons.Filled.BusinessCenter
    state.stats.stress >= 75 -> Icons.Filled.Warning
    state.career.promotionReadiness >= 75 -> Icons.AutoMirrored.Filled.TrendingUp
    else -> Icons.Filled.Person
}

private fun focusIcon(focus: DailyFocus): ImageVector = when (focus) {
    DailyFocus.MONEY -> Icons.Filled.AttachMoney
    DailyFocus.CAREER -> Icons.Filled.Work
    DailyFocus.RECOVERY -> Icons.Filled.Favorite
    DailyFocus.SOCIAL -> Icons.Filled.Groups
    DailyFocus.BALANCED -> Icons.Filled.Dashboard
}

private fun actionIcon(action: DailyActionDefinition): ImageVector = when (action.id) {
    "temp_shift",
    "work_shift",
    "overtime",
    "budget_review" -> Icons.Filled.AttachMoney
    "send_applications" -> Icons.AutoMirrored.Filled.Assignment
    "interview_prep" -> Icons.Filled.School
    "attend_interview" -> Icons.Filled.Work
    "manager_check_in",
    "study_course",
    "networking" -> Icons.AutoMirrored.Filled.TrendingUp
    "research_offer",
    "find_leads",
    "pitch_client",
    "client_project",
    "improve_offer",
    "invest_tools" -> Icons.Filled.BusinessCenter
    "exercise" -> Icons.Filled.FitnessCenter
    "rest" -> Icons.Filled.Favorite
    "cook_at_home" -> Icons.Filled.Restaurant
    "socialize" -> Icons.Filled.Groups
    "call_family" -> Icons.Filled.Call
    else -> when (action.category) {
        ActionCategory.WORK -> Icons.Filled.Work
        ActionCategory.GROWTH -> Icons.Filled.School
        ActionCategory.WELLBEING -> Icons.Filled.Favorite
        ActionCategory.SOCIAL -> Icons.Filled.Groups
        ActionCategory.MONEY -> Icons.Filled.AttachMoney
        ActionCategory.BUSINESS -> Icons.Filled.BusinessCenter
    }
}

private fun metricIcon(label: String): ImageVector = when {
    label.contains("cash", ignoreCase = true) -> Icons.Filled.AttachMoney
    label.contains("debt", ignoreCase = true) -> Icons.Filled.MoneyOff
    label.contains("credit", ignoreCase = true) -> Icons.Filled.CreditCard
    label.contains("offer", ignoreCase = true) -> Icons.AutoMirrored.Filled.Assignment
    label.contains("promotion", ignoreCase = true) -> Icons.AutoMirrored.Filled.TrendingUp
    label.contains("pipeline", ignoreCase = true) -> Icons.Filled.BusinessCenter
    label.contains("trust", ignoreCase = true) -> Icons.Filled.Groups
    label.contains("health", ignoreCase = true) -> Icons.Filled.Favorite
    label.contains("mood", ignoreCase = true) -> Icons.Filled.Favorite
    label.contains("energy", ignoreCase = true) -> Icons.Filled.AccessTime
    label.contains("stress", ignoreCase = true) -> Icons.Filled.Warning
    label.contains("social", ignoreCase = true) -> Icons.Filled.Groups
    label.contains("family", ignoreCase = true) -> Icons.Filled.Call
    label.contains("friends", ignoreCase = true) -> Icons.Filled.Groups
    label.contains("network", ignoreCase = true) -> Icons.Filled.Work
    label.contains("knowledge", ignoreCase = true) -> Icons.Filled.School
    label.contains("fitness", ignoreCase = true) -> Icons.Filled.FitnessCenter
    label.contains("communication", ignoreCase = true) -> Icons.Filled.Call
    label.contains("creativity", ignoreCase = true) -> Icons.Filled.BusinessCenter
    else -> Icons.AutoMirrored.Filled.TrendingUp
}

private fun opportunityIcon(id: String): ImageVector = when (id) {
    "bill_buffer" -> Icons.Filled.CreditCard
    "recovery_window" -> Icons.Filled.Favorite
    "promotion_push" -> Icons.AutoMirrored.Filled.TrendingUp
    "reconnect" -> Icons.Filled.Groups
    "debt_brake" -> Icons.Filled.MoneyOff
    else -> Icons.Filled.Event
}

private fun recommendationIcon(availability: ActionAvailability): ImageVector = when {
    availability.recommendationReason?.contains("opportunity", ignoreCase = true) == true -> Icons.Filled.Event
    availability.focusMatch -> Icons.AutoMirrored.Filled.TrendingUp
    availability.recommendationReason != null -> Icons.Filled.Warning
    else -> actionIcon(availability.action)
}

private fun actionSectionSpecs(): List<ActionSectionSpec> = listOf(
    ActionSectionSpec("money", "Make Money", Icons.Filled.AttachMoney),
    ActionSectionSpec("career", "Get Hired / Career", Icons.Filled.Work),
    ActionSectionSpec("business", "Build Business", Icons.Filled.BusinessCenter),
    ActionSectionSpec("recover", "Recover", Icons.Filled.Favorite),
    ActionSectionSpec("connect", "Connect", Icons.Filled.Groups),
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

private fun pressureMeterValue(state: GameState): Int =
    maxOf(
        state.stats.stress,
        if (state.finances.cash < totalWeeklyCost(state)) 78 else 35,
        if (!state.career.employed) 62 else 25,
        (state.finances.debt / 10).coerceIn(0, 100),
    ).coerceIn(0, 100)

@Composable
private fun meterColor(
    value: Int,
    reverseGood: Boolean = false,
): Color {
    val stressValue = if (reverseGood) value else 100 - value
    return when {
        stressValue >= 72 -> MaterialTheme.colorScheme.error
        stressValue >= 45 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
}

private data class ActionSectionSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

private fun HistoryKind.label(): String = when (this) {
    HistoryKind.ACTION -> "Action"
    HistoryKind.CAREER -> "Career"
    HistoryKind.DAY -> "Day"
    HistoryKind.EVENT -> "Event"
    HistoryKind.FINANCE -> "Finance"
    HistoryKind.GOAL -> "Opportunity"
    HistoryKind.RELATIONSHIP -> "Social"
    HistoryKind.SYSTEM -> "System"
}

private fun costText(action: DailyActionDefinition): String =
    "${action.timeCost}h, ${action.energyCost} energy" +
        if (action.moneyCost > 0) ", ${money(action.moneyCost)}" else ""

private fun DailyActionDefinition.effectSummary(): List<String> = buildList {
    if (effect.cashDelta > 0) add("+cash")
    if (id in setOf("temp_shift", "work_shift", "overtime", "client_project")) add("+income")
    if (effect.debtDelta < 0) add("-debt")
    if (effect.applicationsDelta > 0 || effect.offerProgressDelta > 0 || effect.interviewReadinessDelta > 0) add("+job")
    if (effect.leadsDelta > 0 || effect.activeProjectsDelta > 0 || effect.completedProjectsDelta > 0) add("+business")
    if (effect.careerXpDelta > 0 || effect.promotionReadinessDelta > 0) add("+career")
    if (effect.healthDelta > 0 || effect.fitnessDelta > 0) add("+health")
    if (effect.moodDelta > 0) add("+mood")
    if (effect.stressDelta < 0) add("-stress")
    if (effect.familyDelta > 0 || effect.friendsDelta > 0 || effect.networkDelta > 0) add("+social")
}.take(4)

private fun money(value: Int): String =
    if (value < 0) "-${money(-value)}" else "\$$value"
