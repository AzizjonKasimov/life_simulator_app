package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.engine.AssetCatalog
import com.azizjonkasimov.lifesimulator.domain.model.AssetDefinition
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.InvestmentType
import com.azizjonkasimov.lifesimulator.domain.model.PassiveIncomeBreakdown

@Composable
internal fun MoneyScreen(
    state: GameState,
    netWorth: Int,
    passiveIncome: PassiveIncomeBreakdown,
    weeklyCost: Int,
    onDeposit: (Int) -> Unit,
    onWithdraw: (Int) -> Unit,
    onPayDebt: (Int) -> Unit,
    onInvest: (InvestmentType, Int) -> Unit,
    onSellInvestment: (InvestmentType) -> Unit,
    onBuyAsset: (String) -> Unit,
    onSellAsset: (String) -> Unit,
    onSetAutoSave: (Int) -> Unit,
    onSetAutoInvest: (Int, InvestmentType) -> Unit,
) {
    val cash = state.finances.cash
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { NetWorthCard(state = state, netWorth = netWorth) }
        item { PassiveIncomeCard(passive = passiveIncome, weeklyCost = weeklyCost) }
        item { SavingsCard(state = state, onDeposit = onDeposit, onWithdraw = onWithdraw) }
        item { AutoAllocationCard(state = state, cash = cash, onSetAutoSave = onSetAutoSave, onSetAutoInvest = onSetAutoInvest) }
        if (state.finances.debt > 0) {
            item { DebtCard(state = state, onPayDebt = onPayDebt) }
        }
        item { InvestmentsCard(state = state, cash = cash, onInvest = onInvest, onSellInvestment = onSellInvestment) }
        item { ShopCard(state = state, cash = cash, onBuyAsset = onBuyAsset, onSellAsset = onSellAsset) }
    }
}

@Composable
private fun NetWorthCard(
    state: GameState,
    netWorth: Int,
) {
    SectionCard(title = "Net Worth", icon = UiIcons.netWorth) {
        Text(
            text = money(netWorth),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (netWorth >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(label = "Cash", value = money(state.finances.cash), modifier = Modifier.weight(1f))
            MiniStat(label = "Savings", value = money(state.economy.savings), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(label = "Invested", value = money(state.economy.investedValue), modifier = Modifier.weight(1f))
            MiniStat(label = "Debt", value = money(state.finances.debt), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PassiveIncomeCard(
    passive: PassiveIncomeBreakdown,
    weeklyCost: Int,
) {
    val total = passive.total
    val coversPercent = if (weeklyCost > 0) (total * 100 / weeklyCost) else 0
    val free = total > 0 && total >= weeklyCost
    val accent = if (free) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    SectionCard(
        title = "Passive Income",
        icon = UiIcons.invest,
        trailing = {
            Text(
                text = "${money(total)}/wk",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (free) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        },
    ) {
        Text(
            text = "Money that arrives without spending a day on it. Cover your weekly bill and you're financially free.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MeterLine(
            progress = if (weeklyCost > 0) total.toFloat() / weeklyCost else 0f,
            color = accent,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(label = "Covers bill", value = "$coversPercent%", modifier = Modifier.weight(1f))
            MiniStat(label = "Weekly bill", value = money(weeklyCost), modifier = Modifier.weight(1f))
        }
        val parts = listOfNotNull(
            passive.savingsInterest.takeIf { it > 0 }?.let { "Savings ${money(it)}" },
            passive.business.takeIf { it > 0 }?.let { "Business ${money(it)}" },
            passive.properties.takeIf { it > 0 }?.let { "Property ${money(it)}" },
            passive.market.takeIf { it > 0 }?.let { "Market ${money(it)}" },
        )
        if (parts.isNotEmpty()) {
            ChipFlowRow {
                parts.forEach { LabelChip(text = it, tone = ChipTone.NEUTRAL) }
            }
        }
        if (free) {
            LabelChip(text = "Financially free", icon = UiIcons.netWorth, tone = ChipTone.SUCCESS)
        }
    }
}

@Composable
private fun SavingsCard(
    state: GameState,
    onDeposit: (Int) -> Unit,
    onWithdraw: (Int) -> Unit,
) {
    val cash = state.finances.cash
    val savings = state.economy.savings
    SectionCard(
        title = "Savings",
        icon = UiIcons.savings,
        trailing = {
            Text(
                text = money(savings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
    ) {
        Text(
            text = "A safe place to park cash. Earns 1% interest every payday.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val nextInterest = if (savings > 0) (savings / 100).coerceAtLeast(1) else 0
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat(
                label = "Interest earned",
                value = "+${money(state.economy.lifetimeInterest)}",
                modifier = Modifier.weight(1f),
            )
            MiniStat(
                label = "Next payday",
                value = "+${money(nextInterest)}",
                modifier = Modifier.weight(1f),
            )
        }
        ChipFlowRow {
            MoneyButton(text = "Deposit \$100", enabled = cash >= 100, primary = true) { onDeposit(100) }
            MoneyButton(text = "Deposit all", enabled = cash > 0, primary = true) { onDeposit(cash) }
            MoneyButton(text = "Withdraw \$100", enabled = savings >= 100) { onWithdraw(100) }
            MoneyButton(text = "Withdraw all", enabled = savings > 0) { onWithdraw(savings) }
        }
    }
}

@Composable
private fun DebtCard(
    state: GameState,
    onPayDebt: (Int) -> Unit,
) {
    val cash = state.finances.cash
    val debt = state.finances.debt
    SectionCard(
        title = "Debt",
        icon = UiIcons.debt,
        trailing = {
            Text(
                text = money(debt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        },
    ) {
        Text(
            text = "Debt quietly adds stress every night. Clearing it pays off in calm.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipFlowRow {
            MoneyButton(text = "Pay \$100", enabled = cash >= 100, primary = true) { onPayDebt(100) }
            MoneyButton(text = "Pay all I can", enabled = cash > 0) { onPayDebt(minOf(cash, debt)) }
        }
    }
}

@Composable
private fun InvestmentsCard(
    state: GameState,
    cash: Int,
    onInvest: (InvestmentType, Int) -> Unit,
    onSellInvestment: (InvestmentType) -> Unit,
) {
    SectionCard(title = "Investments", icon = UiIcons.invest) {
        Text(
            text = "Put money to work. Values swing every week — higher risk, wilder ride.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InvestmentType.entries.forEach { type ->
            val holding = state.economy.investments.firstOrNull { it.type == type }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = type.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = type.blurb,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (holding != null) {
                        Text(
                            text = "${money(holding.currentValue)}\n${gainLabel(holding.gain)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (holding.gain >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                ChipFlowRow {
                    MoneyButton(text = "Invest ${money(type.minimumBuy)}", enabled = cash >= type.minimumBuy, primary = true) {
                        onInvest(type, type.minimumBuy)
                    }
                    if (cash >= 300 && type.minimumBuy != 300) {
                        MoneyButton(text = "Invest \$300", enabled = true, primary = true) { onInvest(type, 300) }
                    }
                    if (holding != null) {
                        MoneyButton(text = "Sell all", enabled = true) { onSellInvestment(type) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopCard(
    state: GameState,
    cash: Int,
    onBuyAsset: (String) -> Unit,
    onSellAsset: (String) -> Unit,
) {
    SectionCard(title = "Lifestyle & Assets", icon = UiIcons.shop) {
        Text(
            text = "Spend on things that change how the rest of your life plays.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AssetCatalog.assets.forEach { asset ->
            AssetRow(
                asset = asset,
                owned = !asset.consumable && asset.id in state.economy.ownedAssets,
                cash = cash,
                onBuyAsset = onBuyAsset,
                onSellAsset = onSellAsset,
            )
        }
    }
}

@Composable
private fun AssetRow(
    asset: AssetDefinition,
    owned: Boolean,
    cash: Int,
    onBuyAsset: (String) -> Unit,
    onSellAsset: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = asset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = asset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Net weekly effect: income earned minus upkeep and any rent change.
                val weekly = asset.weeklyIncome - asset.weeklyUpkeep - asset.rentDelta
                if (weekly != 0) {
                    Text(
                        text = if (weekly > 0) "+${money(weekly)}/week" else "${money(weekly)}/week",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (weekly > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            if (owned) {
                LabelChip(text = "Owned", tone = ChipTone.SUCCESS)
            }
        }
        ChipFlowRow {
            if (owned) {
                if (asset.effectiveResale > 0) {
                    MoneyButton(text = "Sell (${money(asset.effectiveResale)})", enabled = true) { onSellAsset(asset.id) }
                }
            } else {
                MoneyButton(
                    text = "Buy ${money(asset.price)}",
                    enabled = cash >= asset.price,
                    primary = true,
                ) { onBuyAsset(asset.id) }
            }
        }
    }
}

@Composable
private fun AutoAllocationCard(
    state: GameState,
    cash: Int,
    onSetAutoSave: (Int) -> Unit,
    onSetAutoInvest: (Int, InvestmentType) -> Unit,
) {
    val eco = state.economy
    SectionCard(title = "Auto-Save & Invest", icon = UiIcons.autoSave) {
        Text(
            text = "Every payday, automatically move a share of your spare cash — no manual deposits.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PercentStepperRow(
            label = "To Savings",
            percent = eco.autoSavePercent,
            onChange = onSetAutoSave,
        )
        PercentStepperRow(
            label = "To Investing",
            percent = eco.autoInvestPercent,
            onChange = { onSetAutoInvest(it, eco.autoInvestType) },
        )
        ChipFlowRow {
            InvestmentType.entries.forEach { type ->
                FilterChip(
                    selected = eco.autoInvestType == type,
                    onClick = { onSetAutoInvest(eco.autoInvestPercent, type) },
                    label = { Text(text = type.label) },
                )
            }
        }
        val previewSave = cash * eco.autoSavePercent / 100
        val previewInvest = cash * eco.autoInvestPercent / 100
        Text(
            text = if (eco.autoAllocationPercent > 0) {
                "≈ ${money(previewSave)} to savings + ${money(previewInvest)} to ${eco.autoInvestType.label} from your current cash."
            } else {
                "Off — set a percentage above to start automating."
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PercentStepperRow(
    label: String,
    percent: Int,
    onChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoneyButton(text = "-5%", enabled = percent > 0) { onChange((percent - 5).coerceAtLeast(0)) }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            MoneyButton(text = "+5%", enabled = percent < 100) { onChange(percent + 5) }
        }
    }
}

@Composable
private fun MoneyButton(
    text: String,
    enabled: Boolean,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val padding = ButtonDefaults.ContentPadding
    if (primary) {
        Button(onClick = onClick, enabled = enabled, contentPadding = padding) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, contentPadding = padding) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun gainLabel(gain: Int): String = if (gain >= 0) "+${money(gain)}" else money(gain)
