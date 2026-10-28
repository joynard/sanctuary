package com.sanctuary.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sanctuary.core.data.ProfileDraft
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val displayName: String = "",
    val dailyGoal: Int = UserProfile.DEFAULT_DAILY_GOAL,
    val intention: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class OnboardingViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        if (value.length > UserProfile.MAX_DISPLAY_NAME_LENGTH) return
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun onGoalChange(value: Int) {
        _uiState.update {
            it.copy(dailyGoal = value.coerceIn(UserProfile.MIN_DAILY_GOAL, UserProfile.MAX_DAILY_GOAL))
        }
    }

    fun onIntentionChange(value: String) {
        if (value.length > UserProfile.MAX_INTENTION_LENGTH) return
        _uiState.update { it.copy(intention = value, errorMessage = null) }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            profileRepository.save(
                draft = ProfileDraft(
                    displayName = current.displayName,
                    dailyGoal = current.dailyGoal,
                    intention = current.intention,
                ),
                markOnboardingComplete = true,
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Belum bisa menyimpan. Coba lagi sebentar.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(profileRepository: ProfileRepository) = viewModelFactory {
            initializer { OnboardingViewModel(profileRepository) }
        }
    }
}
