package com.sanctuary.feature.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.data.ScrollRepository
import com.sanctuary.core.data.TrackingPreferences
import com.sanctuary.core.designsystem.LocalSanctuaryColors
import com.sanctuary.core.designsystem.PlatformMark
import com.sanctuary.core.designsystem.QuietProgressRing
import com.sanctuary.core.designsystem.QuietStat
import com.sanctuary.core.designsystem.RhythmBarChart
import com.sanctuary.core.designsystem.SanctuaryBackground
import com.sanctuary.core.designsystem.SanctuaryMark
import com.sanctuary.core.designsystem.SanctuaryPill
import com.sanctuary.core.designsystem.SectionLabel
import com.sanctuary.core.designsystem.SoftCard
import com.sanctuary.core.designsystem.platformColor
import com.sanctuary.core.model.DailyScrollPoint
import com.sanctuary.core.model.ScrollPlatform
import com.sanctuary.core.model.UserProfile
import java.text.NumberFormat
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardRoute(
    profileRepository: ProfileRepository,
    scrollRepository: ScrollRepository,
    trackingPreferences: TrackingPreferences,
    isTrackingEnabled: () -> Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(
            profileRepository = profileRepository,
            scrollRepository = scrollRepository,
            trackingPreferences = trackingPreferences,
            isTrackingEnabled = isTrackingEnabled,
            clock = clock,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }

    DashboardScreen(
        state = state,
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        onTogglePaused = viewModel::togglePaused,
        modifier = modifier,
    )
}

@Composable
private fun DashboardScreen(
    state: DashboardUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onTogglePaused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    val today = state.today
    val total = today?.total ?: 0
    val goal = profile?.dailyGoal ?: UserProfile.DEFAULT_DAILY_GOAL
    val remaining = (goal - total).coerceAtLeast(0)
    val progress = if (goal == 0) 0f else total.toFloat() / goal
    val extras = LocalSanctuaryColors.current

    SanctuaryBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DashboardHeader(
                    profile = profile,
                    greeting = state.greeting,
                    today = state.today?.date ?: LocalDate.now(),
                )
            }

            item {
                SoftCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel(stringResource(R.string.dashboard_today))
                        if (state.trackingEnabled && !state.paused) {
                            SanctuaryPill(
                                text = stringResource(R.string.dashboard_tracking_on),
                                containerColor = extras.success.copy(alpha = 0.16f),
                                contentColor = extras.success,
                            )
                        } else if (state.paused) {
                            SanctuaryPill(text = stringResource(R.string.dashboard_paused_pill))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QuietProgressRing(
                            progress = progress,
                            modifier = Modifier.size(218.dp),
                            progressColor = if (total > goal) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            contentDescription = stringResource(
                                R.string.dashboard_ring_description,
                                formatNumber(total),
                                formatNumber(goal),
                            ),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatNumber(total),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.dashboard_scroll_unit),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.dashboard_of_goal, formatNumber(goal)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when {
                            total == 0 -> stringResource(R.string.dashboard_status_empty)
                            total < goal -> stringResource(
                                R.string.dashboard_status_below,
                                formatNumber(remaining),
                            )
                            else -> stringResource(R.string.dashboard_status_over)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        QuietStat(
                            value = formatNumber(remaining),
                            label = stringResource(R.string.dashboard_remaining),
                        )
                        QuietStat(
                            value = formatNumber(today?.sessionCount ?: 0),
                            label = stringResource(R.string.dashboard_sessions),
                        )
                    }
                }
            }

            if (!state.trackingEnabled || state.paused) {
                item {
                    TrackingNotice(
                        trackingEnabled = state.trackingEnabled,
                        paused = state.paused,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onTogglePaused = onTogglePaused,
                    )
                }
            }

            profile?.intention?.takeIf { it.isNotBlank() }?.let { intention ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_intention, intention),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                WeekCard(
                    points = state.week?.points.orEmpty(),
                    total = state.week?.total ?: 0,
                    average = state.week?.averagePerDay ?: 0.0,
                    goal = goal,
                )
            }

            item {
                PlatformsCard(today = today)
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    profile: UserProfile?,
    greeting: String,
    today: LocalDate,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sanctuary",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.dashboard_greeting,
                    greeting,
                    profile?.displayName ?: "Sahabat",
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatDate(today),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SanctuaryMark(size = 48.dp)
    }
}

@Composable
private fun TrackingNotice(
    trackingEnabled: Boolean,
    paused: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onTogglePaused: () -> Unit,
) {
    SoftCard(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
        Text(
            text = if (paused) {
                stringResource(R.string.dashboard_paused_title)
            } else {
                stringResource(R.string.dashboard_permission_title)
            },
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (paused) {
                stringResource(R.string.dashboard_paused_body)
            } else {
                stringResource(R.string.dashboard_permission_body)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        if (!trackingEnabled) {
            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.dashboard_permission_cta))
            }
        } else if (paused) {
            OutlinedButton(
                onClick = onTogglePaused,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.dashboard_resume))
            }
        }
    }
}

@Composable
private fun WeekCard(
    points: List<DailyScrollPoint>,
    total: Int,
    average: Double,
    goal: Int,
) {
    SoftCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(stringResource(R.string.dashboard_week_label))
            SanctuaryPill(text = stringResource(R.string.dashboard_week_total, formatNumber(total)))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.dashboard_week_average,
                formatNumber(average.roundToInt()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        if (total == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_week_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            RhythmBarChart(
                labels = points.map { shortDayLabel(it.date) },
                values = points.map { it.total },
                goal = goal,
                contentDescription = stringResource(
                    R.string.dashboard_week_description,
                    formatNumber(total),
                    formatNumber(goal),
                ),
            )
        }
    }
}

@Composable
private fun PlatformsCard(today: com.sanctuary.core.model.DailyScrollSummary?) {
    SoftCard {
        SectionLabel(stringResource(R.string.dashboard_platform_label))
        Spacer(Modifier.height(14.dp))
        val totals = today?.platformTotals.orEmpty()
        val total = totals.sumOf { it.total }
        val colors = LocalSanctuaryColors.current

        ScrollPlatform.entries.forEachIndexed { index, platform ->
            val count = totals.firstOrNull { it.platform == platform }?.total ?: 0
            val fraction = if (total == 0) 0f else count.toFloat() / total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlatformMark(platform = platform, size = 38.dp)
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = platform.displayName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = formatNumber(count),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = platformColor(platform),
                        trackColor = colors.chartFill,
                    )
                }
            }
            if (index != ScrollPlatform.entries.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                )
            }
        }
    }
}

private fun formatNumber(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(value)

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("id-ID")))

private fun shortDayLabel(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "Sen"
    2 -> "Sel"
    3 -> "Rab"
    4 -> "Kam"
    5 -> "Jum"
    6 -> "Sab"
    else -> "Min"
}
