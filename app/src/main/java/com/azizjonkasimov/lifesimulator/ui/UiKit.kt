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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
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
import com.azizjonkasimov.lifesimulator.domain.model.LogKind
import com.azizjonkasimov.lifesimulator.domain.model.Stat
import com.azizjonkasimov.lifesimulator.domain.model.StatChange

// ---------------------------------------------------------------------------
// Shared visual vocabulary. Every screen builds from this small set of pieces
// so the whole app reads with one consistent rhythm.
// ---------------------------------------------------------------------------

internal enum class ChipTone { NEUTRAL, ACCENT, SUCCESS, WARN, DANGER }

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

/** Icon + label + value + colored meter. The one stat row used everywhere. */
@Composable
internal fun StatBar(
    stat: Stat,
    value: Int,
) {
    val display = value.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = statIcon(stat),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Text(text = stat.label, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "$display%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MeterLine(progress = display / 100f, color = meterColor(display))
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
            Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
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
                Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
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
internal fun StatChangeChip(change: StatChange) {
    val isGood = if (change.positiveIsGood) change.amount > 0 else change.amount < 0
    val container = when {
        change.amount == 0 -> MaterialTheme.colorScheme.surfaceVariant
        isGood -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
    }
    val content = when {
        change.amount == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        isGood -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    val valueText = if (change.label == "Money") signedMoney(change.amount) else signed(change.amount)
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Text(
            text = "${change.label} $valueText",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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

internal fun money(value: Int): String = if (value < 0) "-\$${-value}" else "\$$value"

internal fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

internal fun signedMoney(value: Int): String = if (value > 0) "+${money(value)}" else money(value)

@Composable
internal fun meterColor(value: Int): Color {
    val deficit = 100 - value
    return when {
        deficit >= 72 -> MaterialTheme.colorScheme.error
        deficit >= 45 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
}

// ---------------------------------------------------------------------------
// Icon mapping
// ---------------------------------------------------------------------------

internal fun statIcon(stat: Stat): ImageVector = when (stat) {
    Stat.HAPPINESS -> Icons.Filled.SentimentSatisfiedAlt
    Stat.HEALTH -> Icons.Filled.Favorite
    Stat.SMARTS -> Icons.Filled.School
    Stat.LOOKS -> Icons.Filled.Face
}

internal fun logIcon(kind: LogKind): ImageVector = when (kind) {
    LogKind.MILESTONE -> Icons.Filled.Star
    LogKind.MONEY -> Icons.Filled.AttachMoney
    LogKind.HEALTH -> Icons.Filled.Favorite
    LogKind.RELATIONSHIP -> Icons.Filled.Groups
    LogKind.SCHOOL -> Icons.Filled.School
    LogKind.WORK -> Icons.Filled.Work
    LogKind.EVENT -> Icons.Filled.AutoAwesome
    LogKind.NEUTRAL -> Icons.Filled.ChevronRight
}

internal fun GameTab.icon(): ImageVector = when (this) {
    GameTab.LIFE -> Icons.Filled.Timeline
    GameTab.ACTIVITIES -> Icons.Filled.SelfImprovement
    GameTab.PEOPLE -> Icons.Filled.Groups
    GameTab.PROFILE -> Icons.Filled.AccountCircle
}

internal object UiIcons {
    val happiness = Icons.Filled.SentimentSatisfiedAlt
    val health = Icons.Filled.Favorite
    val smarts = Icons.Filled.School
    val looks = Icons.Filled.Face
    val money = Icons.Filled.AttachMoney
    val age = Icons.Filled.Cake
    val person = Icons.Filled.Person
    val people = Icons.Filled.Groups
    val work = Icons.Filled.Work
}
