package com.sanctuary.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.designsystem.SanctuaryBackground
import com.sanctuary.core.designsystem.SanctuaryMark
import com.sanctuary.core.designsystem.SanctuaryPill
import com.sanctuary.core.designsystem.SectionLabel
import com.sanctuary.core.designsystem.SoftCard
import com.sanctuary.core.tracking.AccessibilityStatusChecker
import kotlin.math.roundToInt

@Composable
fun OnboardingRoute(
    profileRepository: ProfileRepository,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val statusChecker = remember(context) {
        AccessibilityStatusChecker(context.applicationContext)
    }
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(profileRepository),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var trackingEnabled by remember { mutableStateOf(statusChecker.isServiceEnabled()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        trackingEnabled = statusChecker.isServiceEnabled()
    }

    OnboardingScreen(
        state = state,
        trackingEnabled = trackingEnabled,
        onNameChange = viewModel::onNameChange,
        onGoalChange = viewModel::onGoalChange,
        onIntentionChange = viewModel::onIntentionChange,
        onEnableTracking = statusChecker::openAccessibilitySettings,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@Composable
private fun OnboardingScreen(
    state: OnboardingUiState,
    trackingEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onGoalChange: (Int) -> Unit,
    onIntentionChange: (String) -> Unit,
    onEnableTracking: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SanctuaryBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SanctuaryMark(size = 44.dp)
                    Text(
                        text = "SANCTUARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(36.dp))
                SectionLabel(stringResource(R.string.onboarding_eyebrow))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(32.dp))
                SoftCard {
                    Text(
                        text = stringResource(R.string.name_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.name_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        supportingText = {
                            Text(stringResource(R.string.name_helper))
                        },
                    )
                }

                Spacer(Modifier.height(16.dp))
                SoftCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.goal_label),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.goal_helper),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SanctuaryPill(
                            text = stringResource(R.string.goal_value, state.dailyGoal),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = state.dailyGoal.toFloat(),
                        onValueChange = { raw ->
                            onGoalChange((raw / 10f).roundToInt() * 10)
                        },
                        valueRange = 20f..500f,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.goal_min),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.goal_max),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                SoftCard {
                    Text(
                        text = stringResource(R.string.intention_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.intention_optional),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.intention,
                        onValueChange = onIntentionChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.intention_hint)) },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }

                Spacer(Modifier.height(16.dp))
                SoftCard {
                    SectionLabel(stringResource(R.string.permission_eyebrow))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.permission_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.permission_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    if (trackingEnabled) {
                        SanctuaryPill(text = stringResource(R.string.permission_enabled))
                    } else {
                        OutlinedButton(
                            onClick = onEnableTracking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(stringResource(R.string.permission_cta))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.start_button))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.offline_note),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                state.errorMessage?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = error,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
