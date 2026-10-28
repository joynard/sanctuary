package com.sanctuary.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.data.ScrollRepository
import com.sanctuary.core.designsystem.EmptyQuietState
import com.sanctuary.core.designsystem.GentleDivider
import com.sanctuary.core.designsystem.PlatformMark
import com.sanctuary.core.designsystem.QuietStat
import com.sanctuary.core.designsystem.SanctuaryBackground
import com.sanctuary.core.designsystem.SanctuaryPill
import com.sanctuary.core.designsystem.SectionLabel
import com.sanctuary.core.designsystem.SoftCard
import com.sanctuary.core.designsystem.platformColor
import com.sanctuary.core.model.ScrollPlatform
import com.sanctuary.core.model.ScrollSession
import java.text.NumberFormat
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryRoute(
    profileRepository: ProfileRepository,
    scrollRepository: ScrollRepository,
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(profileRepository, scrollRepository, clock),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }

    HistoryScreen(
        state = state,
        onRangeChange = viewModel::setRange,
        modifier = modifier,
    )
}

@Composable
private fun HistoryScreen(
    state: HistoryUiState,
    onRangeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedDay by remember { mutableStateOf<LocalDate?>(null) }
    val goal = state.profile?.dailyGoal ?: 100
    val zone = remember(state.profile?.timezoneId) {
        runCatching { ZoneId.of(state.profile?.timezoneId ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
    }

    SanctuaryBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 22.dp, end = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.history_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30).forEach { days ->
                        FilterChip(
                            selected = state.rangeDays == days,
                            onClick = { onRangeChange(days) },
                            label = {
                                Text(stringResource(R.string.history_range_days, days))
                            },
                            shape = CircleShape,
                        )
                    }
                }
            }

            item {
                SummaryCard(
                    rangeDays = state.rangeDays,
                    total = state.stats?.total ?: 0,
                    average = state.stats?.averagePerDay ?: 0.0,
                    sessionCount = state.stats?.sessionCount ?: 0,
                    peak = state.stats?.peak,
                )
            }

            item {
                SectionLabel(
                    text = stringResource(R.string.history_days_label),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            if (!state.isLoading && state.days.isEmpty()) {
                item {
                    SoftCard {
                        EmptyQuietState(
                            title = stringResource(R.string.history_empty_title),
                            body = stringResource(R.string.history_empty_body),
                        )
                    }
                }
            } else {
                items(
                    count = state.days.size,
                    key = { index -> state.days[index].date.toString() },
                ) { index ->
                    val day = state.days[index]
                    val expanded = expandedDay == day.date
                    DayCard(
                        day = day,
                        goal = goal,
                        zone = zone,
                        expanded = expanded,
                        onClick = {
                            expandedDay = if (expanded) null else day.date
                        },
                    )
                }
            }

        }
    }
}

@Composable
private fun SummaryCard(
    rangeDays: Int,
    total: Int,
    average: Double,
    sessionCount: Int,
    peak: com.sanctuary.core.model.DailyScrollPoint?,
) {
    SoftCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                SectionLabel(stringResource(R.string.history_total_label))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatNumber(total),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.history_total_unit, rangeDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SanctuaryPill(
                text = stringResource(R.string.history_average_pill, formatNumber(average.roundToInt())),
            )
        }
        Spacer(Modifier.height(20.dp))
        GentleDivider()
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuietStat(
                value = formatNumber(sessionCount),
                label = stringResource(R.string.history_sessions_label),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (total > 0) formatNumber(peak?.total ?: 0) else "0",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = if (total > 0 && peak != null) {
                        stringResource(R.string.history_peak_label, formatShortDate(peak.date))
                    } else {
                        stringResource(R.string.history_peak_none)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DayCard(
    day: HistoryDay,
    goal: Int,
    zone: ZoneId,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val goalFraction = if (goal <= 0) 0f else day.total.toFloat() / goal
    val overGoal = day.total > goal
    SoftCard(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDayTitle(day.date),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.history_session_count, day.sessions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatNumber(day.total),
                style = MaterialTheme.typography.headlineSmall,
                color = if (overGoal) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.history_collapse else R.string.history_expand,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { goalFraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (overGoal) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ScrollPlatform.entries.forEach { platform ->
                val count = day.sessions
                    .filter { it.platform == platform }
                    .sumOf { it.scrollCount }
                if (count > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(platformColor(platform)),
                        )
                        Text(
                            text = "${platform.displayName} $count",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (expanded) {
            Spacer(Modifier.height(14.dp))
            GentleDivider()
            Spacer(Modifier.height(12.dp))
            day.sessions.forEach { session ->
                SessionRow(session = session, zone = zone)
            }
        }
    }
}

@Composable
private fun SessionRow(session: ScrollSession, zone: ZoneId) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlatformMark(platform = session.platform, size = 28.dp)
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.platform.displayName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = formatTimeRange(session, zone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.history_session_scrolls, session.scrollCount),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun formatNumber(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(value)

private fun formatDayTitle(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("id-ID")))

private fun formatShortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID")))

private fun formatTimeRange(session: ScrollSession, zone: ZoneId): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID"))
    val start = session.startedAt.atZone(zone).format(formatter)
    val end = session.endedAt.atZone(zone).format(formatter)
    return if (start == end) start else "$start - $end"
}
