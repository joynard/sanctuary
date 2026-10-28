package com.sanctuary.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.data.ScrollRepository
import com.sanctuary.core.data.TrackingPreferences
import com.sanctuary.core.model.DailyScrollSummary
import com.sanctuary.core.model.ScrollStats
import com.sanctuary.core.model.ScrollStatsCalculator
import com.sanctuary.core.model.UserProfile
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val today: DailyScrollSummary? = null,
    val week: ScrollStats? = null,
    val trackingEnabled: Boolean = false,
    val paused: Boolean = false,
    val greeting: String = "",
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val profileRepository: ProfileRepository,
    private val scrollRepository: ScrollRepository,
    private val trackingPreferences: TrackingPreferences,
    private val isTrackingEnabled: () -> Boolean,
    private val clock: Clock,
) : ViewModel() {

    private val currentDay = MutableStateFlow(LocalDate.now(clock))
    private val trackingEnabled = MutableStateFlow(isTrackingEnabled())

    val uiState: StateFlow<DashboardUiState> = currentDay
        .flatMapLatest { day ->
            combine(
                profileRepository.profile.catch { emit(null) },
                scrollRepository.observeRange(day.minusDays(6), day).catch { emit(emptyList()) },
                trackingPreferences.paused,
                trackingEnabled,
            ) { profile, sessions, paused, enabled ->
                DashboardUiState(
                    isLoading = false,
                    profile = profile,
                    today = ScrollStatsCalculator.summarizeDay(sessions, day),
                    week = ScrollStatsCalculator.summarize(
                        sessions = sessions,
                        start = day.minusDays(6),
                        end = day,
                    ),
                    trackingEnabled = enabled,
                    paused = paused,
                    greeting = greeting(clock),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(greeting = greeting(clock)),
        )

    fun onResume() {
        currentDay.value = LocalDate.now(clock)
        trackingEnabled.value = isTrackingEnabled()
    }

    fun togglePaused() {
        val target = !uiState.value.paused
        viewModelScope.launch {
            trackingPreferences.setPaused(target)
        }
    }

    companion object {
        fun factory(
            profileRepository: ProfileRepository,
            scrollRepository: ScrollRepository,
            trackingPreferences: TrackingPreferences,
            isTrackingEnabled: () -> Boolean,
            clock: Clock,
        ) = viewModelFactory {
            initializer {
                DashboardViewModel(
                    profileRepository = profileRepository,
                    scrollRepository = scrollRepository,
                    trackingPreferences = trackingPreferences,
                    isTrackingEnabled = isTrackingEnabled,
                    clock = clock,
                )
            }
        }

        private fun greeting(clock: Clock): String {
            val hour = LocalTime.now(clock).hour
            return when {
                hour < 11 -> "Selamat pagi"
                hour < 15 -> "Selamat siang"
                hour < 19 -> "Selamat sore"
                else -> "Selamat malam"
            }
        }
    }
}
