package com.sanctuary.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanctuary.core.data.AppearancePreferences
import com.sanctuary.core.data.BackupRepository
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.data.TrackingPreferences
import com.sanctuary.core.designsystem.GentleDivider
import com.sanctuary.core.designsystem.LocalSanctuaryColors
import com.sanctuary.core.designsystem.SanctuaryBackground
import com.sanctuary.core.designsystem.SanctuaryPill
import com.sanctuary.core.designsystem.SectionLabel
import com.sanctuary.core.designsystem.SoftCard
import com.sanctuary.core.sync.CloudSyncFailure
import com.sanctuary.core.model.ThemeMode
import com.sanctuary.core.sync.CloudSyncManager
import com.sanctuary.core.sync.CloudSyncState
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProfileRoute(
    profileRepository: ProfileRepository,
    trackingPreferences: TrackingPreferences,
    backupRepository: BackupRepository,
    cloudSyncManager: CloudSyncManager,
    appearancePreferences: AppearancePreferences,
    isTrackingEnabled: () -> Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(
            profileRepository = profileRepository,
            trackingPreferences = trackingPreferences,
            backupRepository = backupRepository,
            cloudSyncManager = cloudSyncManager,
            appearancePreferences = appearancePreferences,
            isTrackingEnabled = isTrackingEnabled,
            clock = clock,
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }

    ProfileScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onGoalChange = viewModel::onGoalChange,
        onIntentionChange = viewModel::onIntentionChange,
        onSave = viewModel::save,
        onTogglePaused = viewModel::togglePaused,
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        onExport = { exportLauncher.launch(state.backupFileName) },
        onSyncCloud = viewModel::syncCloud,
        onThemeChange = viewModel::setThemeMode,
        modifier = modifier,
    )
}

@Composable
private fun ProfileScreen(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onGoalChange: (Int) -> Unit,
    onIntentionChange: (String) -> Unit,
    onSave: () -> Unit,
    onTogglePaused: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onExport: () -> Unit,
    onSyncCloud: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SanctuaryBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 22.dp, end = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.profile_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.profile_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SoftCard {
                    SectionLabel(stringResource(R.string.profile_identity_label))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.profile_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.intention,
                        onValueChange = onIntentionChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.profile_intention_label)) },
                        placeholder = { Text(stringResource(R.string.profile_intention_hint)) },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_goal_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        SanctuaryPill(
                            text = stringResource(R.string.profile_goal_value, state.dailyGoal),
                        )
                    }
                    Slider(
                        value = state.dailyGoal.toFloat(),
                        onValueChange = { raw -> onGoalChange((raw / 10f).roundToInt() * 10) },
                        valueRange = 20f..500f,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onSave,
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.profile_save))
                        }
                    }
                    NoticeText(notice = state.saveNotice)
                }
            }

            item {
                SoftCard {
                    SectionLabel(stringResource(R.string.profile_tracking_label))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    if (state.trackingEnabled) {
                                        R.string.profile_tracking_active
                                    } else {
                                        R.string.profile_tracking_inactive
                                    },
                                ),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    if (state.trackingEnabled) {
                                        R.string.profile_tracking_body_active
                                    } else {
                                        R.string.profile_tracking_body_inactive
                                    },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SanctuaryPill(
                            text = if (state.trackingEnabled) {
                                stringResource(R.string.profile_tracking_on_pill)
                            } else {
                                stringResource(R.string.profile_tracking_off_pill)
                            },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    GentleDivider()
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_pause_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.profile_pause_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.paused,
                            onCheckedChange = { onTogglePaused() },
                            enabled = state.trackingEnabled,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (state.trackingEnabled) {
                        TextButton(
                            onClick = onOpenAccessibilitySettings,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        ) {
                            Text(stringResource(R.string.profile_open_settings))
                        }
                    } else {
                        OutlinedButton(
                            onClick = onOpenAccessibilitySettings,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(stringResource(R.string.profile_enable_tracking))
                        }
                    }
                }
            }

            item {
                AppearanceCard(
                    mode = state.themeMode,
                    onModeChange = onThemeChange,
                )
            }

            item {
                SoftCard {
                    SectionLabel(stringResource(R.string.profile_backup_label))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.profile_backup_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.profile_backup_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = onExport,
                        enabled = !state.isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.profile_backup_button))
                        }
                    }
                    NoticeText(notice = state.exportNotice)
                }
            }

            item {
                CloudSyncCard(
                    state = state.cloudState,
                    onSync = onSyncCloud,
                )
            }

            item {
                SoftCard {
                    SectionLabel(stringResource(R.string.profile_privacy_label))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.profile_privacy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

        }
    }
}

@Composable
private fun AppearanceCard(
    mode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
) {
    SoftCard {
        SectionLabel(stringResource(R.string.profile_appearance_label))
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { option ->
                FilterChip(
                    selected = mode == option,
                    onClick = { onModeChange(option) },
                    label = { Text(stringResource(option.labelRes())) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.profile_theme_system
    ThemeMode.LIGHT -> R.string.profile_theme_light
    ThemeMode.DARK -> R.string.profile_theme_dark
}

@Composable
private fun CloudSyncCard(
    state: CloudSyncState?,
    onSync: () -> Unit,
) {
    val syncing = state is CloudSyncState.Syncing
    val configured = state != null && state !is CloudSyncState.NotConfigured
    val notLinked = state is CloudSyncState.NotLinked
    val statusRes = when (state) {
        null -> R.string.profile_cloud_checking
        CloudSyncState.NotConfigured -> R.string.profile_cloud_not_configured_pill
        CloudSyncState.NotLinked -> R.string.profile_cloud_not_linked_pill
        is CloudSyncState.Linked -> R.string.profile_cloud_ready_pill
        CloudSyncState.Syncing -> R.string.profile_cloud_syncing_pill
        is CloudSyncState.Synced -> R.string.profile_cloud_synced_pill
        is CloudSyncState.Failed -> R.string.profile_cloud_failed_pill
    }

    SoftCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(stringResource(R.string.profile_cloud_label))
            SanctuaryPill(text = stringResource(statusRes))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = cloudBody(state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSync,
            enabled = configured && !syncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    stringResource(
                        when {
                            !configured -> R.string.profile_cloud_unavailable
                            notLinked -> R.string.profile_cloud_activate
                            else -> R.string.profile_cloud_sync_now
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun cloudBody(state: CloudSyncState?): String = when (state) {
    null -> stringResource(R.string.profile_cloud_checking_body)
    CloudSyncState.NotConfigured -> stringResource(R.string.profile_cloud_not_configured_body)
    CloudSyncState.NotLinked -> stringResource(R.string.profile_cloud_not_linked_body)
    is CloudSyncState.Linked -> stringResource(R.string.profile_cloud_ready_body)
    CloudSyncState.Syncing -> stringResource(R.string.profile_cloud_syncing_body)
    is CloudSyncState.Synced -> if (state.sessions == 0) {
        stringResource(R.string.profile_cloud_synced_empty, formatCloudTime(state.at))
    } else {
        stringResource(
            R.string.profile_cloud_synced_body,
            formatCloudTime(state.at),
            state.sessions,
        )
    }
    is CloudSyncState.Failed -> stringResource(state.reason.messageRes())
}

private fun CloudSyncFailure.messageRes(): Int = when (this) {
    CloudSyncFailure.OFFLINE -> R.string.profile_cloud_error_offline
    CloudSyncFailure.ANONYMOUS_DISABLED -> R.string.profile_cloud_error_anonymous
    CloudSyncFailure.RATE_LIMITED -> R.string.profile_cloud_error_rate_limit
    CloudSyncFailure.AUTH_REJECTED -> R.string.profile_cloud_error_auth
    CloudSyncFailure.SERVER -> R.string.profile_cloud_error_server
    CloudSyncFailure.UNKNOWN -> R.string.profile_cloud_error_unknown
}

private fun formatCloudTime(at: Instant): String =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
        .withZone(ZoneId.systemDefault())
        .format(at)

@Composable
private fun NoticeText(notice: ProfileNotice?) {
    if (notice == null) return
    val colors = LocalSanctuaryColors.current
    val isError = notice == ProfileNotice.SAVE_FAILED || notice == ProfileNotice.EXPORT_FAILED
    val text = when (notice) {
        ProfileNotice.SAVED -> stringResource(R.string.profile_notice_saved)
        ProfileNotice.SAVE_FAILED -> stringResource(R.string.profile_notice_save_failed)
        ProfileNotice.EXPORTED -> stringResource(R.string.profile_notice_exported)
        ProfileNotice.EXPORT_FAILED -> stringResource(R.string.profile_notice_export_failed)
    }
    Spacer(Modifier.height(10.dp))
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else colors.success,
        textAlign = TextAlign.Center,
    )
}
