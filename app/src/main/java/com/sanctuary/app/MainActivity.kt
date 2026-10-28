package com.sanctuary.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanctuary.core.designsystem.SanctuaryTheme
import com.sanctuary.core.model.ThemeMode
import com.sanctuary.feature.onboarding.OnboardingRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as SanctuaryApp).container
        setContent {
            val themeMode by container.appearancePreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SanctuaryTheme(darkTheme = darkTheme) {
                val view = LocalView.current
                SideEffect {
                    val window = view.context.findActivity()?.window ?: return@SideEffect
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                SanctuaryRoot(container = container)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
