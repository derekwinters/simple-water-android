package com.derekwinters.water.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.derekwinters.water.WaterApp
import com.derekwinters.water.data.UserSettings
import com.derekwinters.water.designsystem.AppTheme
import com.derekwinters.water.ui.firstrun.FirstRunRoute
import com.derekwinters.water.ui.history.HistoryRoute
import com.derekwinters.water.ui.home.HomeRoute
import com.derekwinters.water.ui.reminders.RemindersRoute
import com.derekwinters.water.ui.settings.SettingsRoute

object Routes {
    const val FIRST_RUN = "first-run"
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val REMINDERS = "reminders"
}

/** UI-001: waits for the stored settings, then starts on first-run or home. */
@Composable
fun WaterNavHost(app: WaterApp, openHomeRequests: Int) {
    val settings: UserSettings? by app.settingsStore.settings.collectAsState(initial = null)
    val loaded = settings
    if (loaded == null) {
        Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.bg))
    } else {
        WaterNavGraph(app = app, settings = loaded, openHomeRequests = openHomeRequests)
    }
}

@Composable
private fun WaterNavGraph(app: WaterApp, settings: UserSettings, openHomeRequests: Int) {
    val nav = rememberNavController()
    // Decided once: later settings changes navigate explicitly rather than swapping the graph.
    val start = remember { if (settings.onboarded) Routes.HOME else Routes.FIRST_RUN }
    // Routes read the latest settings through this one State object, so the NavHost builder
    // below captures nothing that changes and the graph is never rebuilt.
    val current = rememberUpdatedState(settings)

    // REM-012: a tapped notification returns to home (unless first run is still showing).
    LaunchedEffect(openHomeRequests) {
        if (openHomeRequests > 0 && settings.onboarded) {
            nav.popBackStack(Routes.HOME, inclusive = false)
        }
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.FIRST_RUN) {
            FirstRunRoute(
                app = app,
                onDone = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.FIRST_RUN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeRoute(
                app = app,
                settings = current,
                onOpenSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                onOpenHistory = { nav.navigate(Routes.HISTORY) { launchSingleTop = true } },
            )
        }
        composable(Routes.HISTORY) {
            HistoryRoute(app = app, settings = current, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                app = app,
                settings = current,
                onBack = { nav.popBackStack() },
                onOpenReminders = { nav.navigate(Routes.REMINDERS) { launchSingleTop = true } },
            )
        }
        composable(Routes.REMINDERS) {
            RemindersRoute(app = app, settings = current, onBack = { nav.popBackStack() })
        }
    }
}
