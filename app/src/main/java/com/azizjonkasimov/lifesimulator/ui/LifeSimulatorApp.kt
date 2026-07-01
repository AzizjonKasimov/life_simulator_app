package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.GameState
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
            viewModel = viewModel,
            currentVersionLabel = updatePromptState.currentVersionLabel,
            updateChecking = updatePromptState.checking,
            onCheckForUpdates = { updatePromptState.checkForUpdates(showResult = true) },
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadgeTile(
            icon = UiIcons.netWorth,
            iconSize = 30.dp,
            padding = 14.dp,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Life Simulator",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start broke and in debt. Earn it, save it, invest it, and build a life worth more than its bills.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        SectionCard(title = "Your start", icon = UiIcons.career) {
            Text(
                text = "Alex Rivers, 22. Unemployed, $180 in cash, $350 in debt. Land work, launch a hustle, and grow your net worth — your call.",
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

@Composable
private fun ActiveGameScreen(
    uiState: LifeSimulatorUiState,
    viewModel: LifeSimulatorViewModel,
    currentVersionLabel: String,
    updateChecking: Boolean,
    onCheckForUpdates: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                GameTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(imageVector = tab.icon(), contentDescription = tab.label) },
                        label = { Text(text = tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
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
            AppHeader(
                state = state,
                netWorth = uiState.netWorth,
                messages = uiState.messages,
                lastActionDeltas = uiState.lastActionDeltas,
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (uiState.selectedTab) {
                GameTab.DASHBOARD -> DashboardScreen(
                    state = state,
                    dashboard = dashboard,
                    actions = uiState.actions,
                    passiveIncome = uiState.passiveIncome,
                    goals = uiState.goals,
                    onPerformAction = viewModel::performAction,
                    onAdvanceDay = viewModel::advanceDay,
                )
                GameTab.ACTIONS -> ActionsScreen(actions = uiState.actions, onPerformAction = viewModel::performAction)
                GameTab.MONEY -> MoneyScreen(
                    state = state,
                    netWorth = uiState.netWorth,
                    passiveIncome = uiState.passiveIncome,
                    weeklyCost = uiState.weeklyCost,
                    onDeposit = viewModel::deposit,
                    onWithdraw = viewModel::withdraw,
                    onPayDebt = viewModel::payDebt,
                    onInvest = viewModel::invest,
                    onSellInvestment = viewModel::sellInvestment,
                    onBuyAsset = viewModel::buyAsset,
                    onSellAsset = viewModel::sellAsset,
                    onSetAutoSave = viewModel::setAutoSave,
                    onSetAutoInvest = viewModel::setAutoInvest,
                )
                GameTab.PROGRESS -> ProgressScreen(
                    state = state,
                    goals = uiState.goals,
                    currentVersionLabel = currentVersionLabel,
                    updateChecking = updateChecking,
                    onCheckForUpdates = onCheckForUpdates,
                    onReset = viewModel::resetSave,
                )
                GameTab.HISTORY -> HistoryScreen(history = state.history)
            }
        }
    }

    uiState.pendingDecision?.let { decision ->
        DecisionDialog(
            event = decision,
            cash = uiState.gameState?.finances?.cash ?: 0,
            onChoose = viewModel::resolveDecision,
        )
    }
}

@Composable
private fun AppHeader(
    state: GameState,
    netWorth: Int,
    messages: List<String>,
    lastActionDeltas: List<ActionDelta>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadgeTile(
                icon = statusIcon(state),
                contentDescription = null,
                iconSize = 24.dp,
                padding = 10.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${state.career.title} · Day ${state.day}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "Net ${money(netWorth)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (netWorth >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile(
                icon = UiIcons.cash,
                label = "Cash",
                value = money(state.finances.cash),
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                icon = UiIcons.time,
                label = "Time",
                value = "${state.timeRemaining}h",
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                icon = UiIcons.energy,
                label = "Energy",
                value = "${state.stats.energy}%",
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
        }
        AnimatedVisibility(visible = messages.isNotEmpty() || lastActionDeltas.isNotEmpty()) {
            FeedbackBanner(messages = messages, deltas = lastActionDeltas)
        }
    }
}

@Composable
private fun FeedbackBanner(
    messages: List<String>,
    deltas: List<ActionDelta>,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            messages.take(3).forEach { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (deltas.isNotEmpty()) {
                ChipFlowRow {
                    deltas.forEach { DeltaChip(delta = it) }
                }
            }
        }
    }
}
