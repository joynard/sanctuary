package com.sanctuary.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sanctuary.core.designsystem.SanctuaryBackground
import com.sanctuary.core.designsystem.SanctuaryMark
import com.sanctuary.feature.dashboard.DashboardRoute
import com.sanctuary.feature.history.HistoryRoute
import com.sanctuary.feature.profile.ProfileRoute

private enum class Destination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    TODAY("dashboard", R.string.nav_today, Icons.Rounded.Home),
    HISTORY("history", R.string.nav_history, Icons.Rounded.History),
    PROFILE("profile", R.string.nav_profile, Icons.Rounded.Person),
}

@Composable
fun MainScaffold(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isTrackingEnabled = remember(container) {
        { container.accessibilityStatusChecker.isServiceEnabled() }
    }
    val openAccessibilitySettings = remember(container) {
        { container.accessibilityStatusChecker.openAccessibilitySettings() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                tonalElevation = 0.dp,
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                ) {
                    Destination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.TODAY.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Destination.TODAY.route) {
                DashboardRoute(
                    profileRepository = container.profileRepository,
                    scrollRepository = container.scrollRepository,
                    trackingPreferences = container.trackingPreferences,
                    isTrackingEnabled = isTrackingEnabled,
                    onOpenAccessibilitySettings = openAccessibilitySettings,
                    clock = container.clock,
                )
            }
            composable(Destination.HISTORY.route) {
                HistoryRoute(
                    profileRepository = container.profileRepository,
                    scrollRepository = container.scrollRepository,
                    clock = container.clock,
                )
            }
            composable(Destination.PROFILE.route) {
                ProfileRoute(
                    profileRepository = container.profileRepository,
                    trackingPreferences = container.trackingPreferences,
                    backupRepository = container.backupRepository,
                    cloudSyncManager = container.cloudSyncManager,
                    appearancePreferences = container.appearancePreferences,
                    isTrackingEnabled = isTrackingEnabled,
                    onOpenAccessibilitySettings = openAccessibilitySettings,
                    clock = container.clock,
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    SanctuaryBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SanctuaryMark(size = 84.dp)
            Text(
                text = "Sanctuary",
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.loading),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
