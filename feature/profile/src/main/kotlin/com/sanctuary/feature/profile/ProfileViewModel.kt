package com.sanctuary.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sanctuary.core.data.BackupRepository
import com.sanctuary.core.data.ProfileDraft
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.data.TrackingPreferences
import com.sanctuary.core.model.UserProfile
import com.sanctuary.core.sync.CloudSyncManager
import com.sanctuary.core.sync.CloudSyncState
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileNotice {
    SAVED,
    SAVE_FAILED,
    EXPORTED,
    EXPORT_FAILED,
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val dailyGoal: Int = UserProfile.DEFAULT_DAILY_GOAL,
    val intention: String = "",
    val trackingEnabled: Boolean = false,
    val paused: Boolean = false,
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val saveNotice: ProfileNotice? = null,
    val exportNotice: ProfileNotice? = null,
    val backupFileName: String = "sanctuary-backup.json",
    val cloudState: CloudSyncState? = null,
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val trackingPreferences: TrackingPreferences,
    private val backupRepository: BackupRepository,
    private val cloudSyncManager: CloudSyncManager,
    private val isTrackingEnabled: () -> Boolean,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            trackingEnabled = isTrackingEnabled(),
            backupFileName = backupRepository.suggestedFileName(LocalDate.now(clock)),
        ),
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { profileRepository.ensureDefault() }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            displayName = profile.displayName,
                            dailyGoal = profile.dailyGoal,
                            intention = profile.intention.orEmpty(),
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
        viewModelScope.launch {
            trackingPreferences.paused
                .catch { emit(false) }
                .collect { paused ->
                    _uiState.update { it.copy(paused = paused) }
                }
        }
        viewModelScope.launch {
            cloudSyncManager.state.collect { cloudState ->
                _uiState.update { it.copy(cloudState = cloudState) }
            }
        }
    }

    fun onResume() {
        val enabled = runCatching(isTrackingEnabled).getOrDefault(false)
        _uiState.update {
            it.copy(
                trackingEnabled = enabled,
                backupFileName = backupRepository.suggestedFileName(LocalDate.now(clock)),
            )
        }
    }

    fun onNameChange(value: String) {
        if (value.length > UserProfile.MAX_DISPLAY_NAME_LENGTH) return
        _uiState.update { it.copy(displayName = value, saveNotice = null) }
    }

    fun onGoalChange(value: Int) {
        _uiState.update {
            it.copy(
                dailyGoal = value.coerceIn(UserProfile.MIN_DAILY_GOAL, UserProfile.MAX_DAILY_GOAL),
                saveNotice = null,
            )
        }
    }

    fun onIntentionChange(value: String) {
        if (value.length > UserProfile.MAX_INTENTION_LENGTH) return
        _uiState.update { it.copy(intention = value, saveNotice = null) }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveNotice = null) }
            profileRepository.save(
                draft = ProfileDraft(
                    displayName = current.displayName,
                    dailyGoal = current.dailyGoal,
                    intention = current.intention,
                ),
                markOnboardingComplete = false,
            ).onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        displayName = profile.displayName,
                        dailyGoal = profile.dailyGoal,
                        intention = profile.intention.orEmpty(),
                        saveNotice = ProfileNotice.SAVED,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(isSaving = false, saveNotice = ProfileNotice.SAVE_FAILED)
                }
            }
        }
    }

    fun togglePaused() {
        val target = !_uiState.value.paused
        viewModelScope.launch {
            trackingPreferences.setPaused(target)
        }
    }

    fun syncCloud() {
        viewModelScope.launch {
            cloudSyncManager.syncNow()
        }
    }

    fun exportBackup(uri: Uri) {
        if (_uiState.value.isExporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportNotice = null) }
            backupRepository.export(uri).onSuccess {
                _uiState.update {
                    it.copy(isExporting = false, exportNotice = ProfileNotice.EXPORTED)
                }
            }.onFailure {
                _uiState.update {
                    it.copy(isExporting = false, exportNotice = ProfileNotice.EXPORT_FAILED)
                }
            }
        }
    }

    companion object {
        fun factory(
            profileRepository: ProfileRepository,
            trackingPreferences: TrackingPreferences,
            backupRepository: BackupRepository,
            cloudSyncManager: CloudSyncManager,
            isTrackingEnabled: () -> Boolean,
            clock: Clock,
        ) = viewModelFactory {
            initializer {
                ProfileViewModel(
                    profileRepository = profileRepository,
                    trackingPreferences = trackingPreferences,
                    backupRepository = backupRepository,
                    cloudSyncManager = cloudSyncManager,
                    isTrackingEnabled = isTrackingEnabled,
                    clock = clock,
                )
            }
        }
    }
}
