package com.sanctuary.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sanctuary.core.data.ProfileRepository
import com.sanctuary.core.data.ScrollRepository
import com.sanctuary.core.model.ScrollSession
import com.sanctuary.core.model.ScrollStats
import com.sanctuary.core.model.ScrollStatsCalculator
import com.sanctuary.core.model.UserProfile
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class HistoryDay(
    val date: LocalDate,
    val total: Int,
    val sessions: List<ScrollSession>,
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val rangeDays: Int = 7,
    val stats: ScrollStats? = null,
    val days: List<HistoryDay> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    profileRepository: ProfileRepository,
    scrollRepository: ScrollRepository,
    private val clock: Clock,
) : ViewModel() {

    private val currentDay = MutableStateFlow(LocalDate.now(clock))
    private val rangeDays = MutableStateFlow(7)

    val uiState: StateFlow<HistoryUiState> = combine(currentDay, rangeDays) { day, range ->
        day to range
    }
        .flatMapLatest { (day, range) ->
            val start = day.minusDays(range.toLong() - 1)
            combine(
                profileRepository.profile.catch { emit(null) },
                scrollRepository.observeRange(start, day).catch { emit(emptyList()) },
            ) { profile, sessions ->
                HistoryUiState(
                    isLoading = false,
                    profile = profile,
                    rangeDays = range,
                    stats = ScrollStatsCalculator.summarize(sessions, start, day),
                    days = sessions
                        .groupBy { it.localDay }
                        .map { (date, daySessions) ->
                            HistoryDay(
                                date = date,
                                total = daySessions.sumOf { it.scrollCount },
                                sessions = daySessions,
                            )
                        }
                        .sortedByDescending { it.date },
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    fun onResume() {
        currentDay.value = LocalDate.now(clock)
    }

    fun setRange(days: Int) {
        require(days == 7 || days == 30) { "Unsupported history range" }
        rangeDays.value = days
    }

    companion object {
        fun factory(
            profileRepository: ProfileRepository,
            scrollRepository: ScrollRepository,
            clock: Clock,
        ) = viewModelFactory {
            initializer {
                HistoryViewModel(
                    profileRepository = profileRepository,
                    scrollRepository = scrollRepository,
                    clock = clock,
                )
            }
        }
    }
}
