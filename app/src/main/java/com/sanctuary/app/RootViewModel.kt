package com.sanctuary.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.model.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RootViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = profileRepository.profile
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    init {
        viewModelScope.launch {
            runCatching { profileRepository.ensureDefault() }
        }
    }

    companion object {
        fun factory(profileRepository: ProfileRepository) = viewModelFactory {
            initializer { RootViewModel(profileRepository) }
        }
    }
}
