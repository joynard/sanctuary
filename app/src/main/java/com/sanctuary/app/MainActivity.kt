package com.sanctuary.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.sanctuary.core.designsystem.SanctuaryTheme
import com.sanctuary.feature.onboarding.OnboardingRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SanctuaryTheme {
                SanctuaryRoot(container = (application as SanctuaryApp).container)
            }
        }
    }
}

@Composable
private fun SanctuaryRoot(container: AppContainer) {
    val rootViewModel: RootViewModel = viewModel(
        factory = RootViewModel.factory(container.profileRepository),
    )
    val profile by rootViewModel.profile.collectAsStateWithLifecycle()

    val currentProfile = profile
    when {
        currentProfile == null -> LoadingScreen()
        !currentProfile.onboardingComplete -> OnboardingRoute(
            profileRepository = container.profileRepository,
        )
        else -> MainScaffold(container = container)
    }
}
