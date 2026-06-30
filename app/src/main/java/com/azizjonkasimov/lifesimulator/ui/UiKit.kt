package com.azizjonkasimov.lifesimulator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.azizjonkasimov.lifesimulator.domain.model.ActionAvailability
import com.azizjonkasimov.lifesimulator.domain.model.ActionCategory
import com.azizjonkasimov.lifesimulator.domain.model.ActionDelta
import com.azizjonkasimov.lifesimulator.domain.model.DailyActionDefinition
import com.azizjonkasimov.lifesimulator.domain.model.GameState
import com.azizjonkasimov.lifesimulator.domain.model.HistoryKind

// ---------------------------------------------------------------------------
// Shared visual vocabulary. Every screen builds from this small set of pieces
// so the whole app reads with one consistent rhythm instead of many one-off
// widgets.
// ---------------------------------------------------------------------------

internal enum class ChipTone { NEUTRAL, ACCENT, SUCCESS, WARN, DANGER }

/**
 * Wrapping row with consistent gaps, used for any cluster of chips. The
 * experimental [FlowRow] is kept fully internal here so call sites never need
 * their own opt-in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipFlowRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

/** The single card surface used for every panel in the app. */
@Composable
internal fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke()
            }
            content()
        }
    }
}

/** Flat, rounded progress line with no extra ornamentation. */
@Composable
internal fun MeterLine(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 6.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Label + value + colored meter. The one stat row used everywhere. */
@Composable
internal fun StatBar(
    label: String,
    value: Int,
    reverseGood: Boolean = false,
) {
    val display = value.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$display%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MeterLine(progress = display / 100f, color = meterColor(display, reverseGood))
    }
}

/** Icon + label + value descriptor + meter. Used for the headline progress tracks. */
@Composable
internal fun ProgressTrack(
    icon: ImageVector,
    label: String,
    value: String,
    progress: Int,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MeterLine(progress = progress.coerceIn(0, 100) / 100f, color = accent)
    }
}

/** Compact resource tile: icon next to a muted label over a bold value. */
@Composable
internal fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(20.dp),
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Muted label over a bold value, with no surface. For dense grids inside cards. */
@Composable
internal fun MiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

/** The one chip style, in a few tones. Replaces the old Pill + IconBadge pair. */
@Composable
internal fun LabelChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: ChipTone = ChipTone.NEUTRAL,
) {
    val container: Color
    val content: Color
    when (tone) {
        ChipTone.NEUTRAL -> {
            container = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
        ChipTone.ACCENT -> {
            container = MaterialTheme.colorScheme.primaryContainer
            content = MaterialTheme.colorScheme.onPrimaryContainer
        }
        ChipTone.SUCCESS -> {
            container = MaterialTheme.colorScheme.secondaryContainer
            content = MaterialTheme.colorScheme.onSecondaryContainer
        }
        ChipTone.WARN -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            content = MaterialTheme.colorScheme.onTertiaryContainer
        }
        ChipTone.DANGER -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
        }
    }
    Surface(modifier = modifier, color = container, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Signed effect chip, green for good and red for bad. */
@Composable
internal fun DeltaChip(delta: ActionDelta) {
    val isGood = if (delta.positiveIsGood) delta.amount > 0 else delta.amount < 0
    val container = when {
        delta.amount == 0 -> MaterialTheme.colorScheme.surfaceVariant
        isGood -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
    }
    val content = when {
        delta.amount == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        isGood -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(color = container, shape = MaterialTheme.shapes.small) {
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

/** Square accent tile that frames an icon. Used for identity + action glyphs. */
@Composable
internal fun IconBadgeTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    padding: Dp = 9.dp,
    iconSize: Dp = 22.dp,
) {
    Surface(modifier = modifier, color = container, shape = MaterialTheme.shapes.medium) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier
                .padding(padding)
                .size(iconSize),
        )
    }
}

// ---------------------------------------------------------------------------
// Formatting + color helpers
// ---------------------------------------------------------------------------

internal fun money(value: Int): String =
    if (value < 0) "-${money(-value)}" else "\$$value"

internal fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

@Composable
internal fun meterColor(
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

internal fun costText(action: DailyActionDefinition): String =
    "${action.timeCost}h · ${action.energyCost} energy" +
        if (action.moneyCost > 0) " · ${money(action.moneyCost)}" else ""

/** Tighter cost string for dense rows on narrow screens: "5h · 28e · $30". */
internal fun compactCostText(action: DailyActionDefinition): String =
    "${action.timeCost}h · ${action.energyCost}e" +
        if (action.moneyCost > 0) " · ${money(action.moneyCost)}" else ""

internal fun runwayDays(state: GameState, weeklyCost: Int): Int =
    if (weeklyCost <= 0) 0 else (state.finances.cash * 7 / weeklyCost).coerceAtLeast(0)

// ---------------------------------------------------------------------------
// Icon mapping
// ---------------------------------------------------------------------------

internal fun GameTab.icon(): ImageVector = when (this) {
    GameTab.DASHBOARD -> Icons.Filled.Dashboard
    GameTab.ACTIONS -> Icons.AutoMirrored.Filled.FormatListBulleted
    GameTab.MONEY -> Icons.Filled.AccountBalanceWallet
    GameTab.PROGRESS -> Icons.Filled.BarChart
    GameTab.HISTORY -> Icons.Filled.History
}

internal fun statusIcon(state: GameState): ImageVector = when {
    !state.career.employed -> Icons.Filled.Search
    state.business.started && state.business.clients > 0 -> Icons.Filled.BusinessCenter
    state.stats.stress >= 75 -> Icons.Filled.Warning
    state.career.promotionReadiness >= 75 -> Icons.AutoMirrored.Filled.TrendingUp
    else -> Icons.Filled.Person
}

internal fun actionIcon(action: DailyActionDefinition): ImageVector = when (action.id) {
    "gig_work",
    "work_shift",
    "overtime" -> Icons.Filled.AttachMoney
    "apply_jobs" -> Icons.AutoMirrored.Filled.Assignment
    "interview_prep",
    "study" -> Icons.Filled.School
    "attend_interview" -> Icons.Filled.Work
    "manager_check_in",
    "network" -> Icons.AutoMirrored.Filled.TrendingUp
    "launch_business",
    "find_client",
    "marketing",
    "upgrade_business" -> Icons.Filled.BusinessCenter
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

internal fun HistoryKind.label(): String = when (this) {
    HistoryKind.ACTION -> "Action"
    HistoryKind.CAREER -> "Career"
    HistoryKind.DAY -> "Day"
    HistoryKind.EVENT -> "Event"
    HistoryKind.FINANCE -> "Finance"
    HistoryKind.GOAL -> "Opportunity"
    HistoryKind.RELATIONSHIP -> "Social"
    HistoryKind.SYSTEM -> "System"
}

internal fun recommendationTone(availability: ActionAvailability): ChipTone = when {
    availability.recommendationReason?.contains("opportunity", ignoreCase = true) == true -> ChipTone.WARN
    availability.focusMatch -> ChipTone.ACCENT
    availability.recommendationReason != null -> ChipTone.WARN
    else -> ChipTone.NEUTRAL
}

internal fun recommendationIcon(availability: ActionAvailability): ImageVector = when {
    availability.recommendationReason?.contains("opportunity", ignoreCase = true) == true -> Icons.Filled.Event
    availability.focusMatch -> Icons.AutoMirrored.Filled.TrendingUp
    availability.recommendationReason != null -> Icons.Filled.Warning
    else -> actionIcon(availability.action)
}

// Re-exported icons referenced by name from the screen files.
internal object UiIcons {
    val cash = Icons.Filled.AttachMoney
    val debt = Icons.Filled.MoneyOff
    val time = Icons.Filled.AccessTime
    val energy = Icons.Filled.Bolt
    val bill = Icons.Filled.Event
    val runway = Icons.Filled.CreditCard
    val pressure = Icons.Filled.Warning
    val business = Icons.Filled.BusinessCenter
    val career = Icons.Filled.Work
    val jobSearch = Icons.Filled.Search
    val money = Icons.Filled.AttachMoney
    val netWorth = Icons.Filled.AccountBalanceWallet
    val savings = Icons.Filled.Savings
    val autoSave = Icons.Filled.Autorenew
    val invest = Icons.AutoMirrored.Filled.ShowChart
    val shop = Icons.Filled.ShoppingCart
    val decision = Icons.Filled.Casino
    val recover = Icons.Filled.Favorite
    val connect = Icons.Filled.Groups
    val skills = Icons.Filled.School
    val finances = Icons.Filled.CreditCard
    val wellbeing = Icons.Filled.Favorite
    val relationships = Icons.Filled.Groups
    val updates = Icons.AutoMirrored.Filled.TrendingUp
}
