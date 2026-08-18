package io.github.parkiyong.quran

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.parkiyong.quran.navigation.NavigationItem
import io.github.parkiyong.quran.navigation.QuranNavHost
import io.github.parkiyong.quran.navigation.Screen
import io.github.parkiyong.quran.ui.adaptive.AdaptiveNavigationScaffold
import io.github.parkiyong.quran.ui.settings.SettingsViewModel
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    KoinContext {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val selectedItem = when {
            currentRoute?.contains("Bookmarks") == true -> NavigationItem.BOOKMARKS
            currentRoute?.contains("AudioDownloads") == true -> NavigationItem.AUDIO_DOWNLOADS
            currentRoute?.contains("Settings") == true -> NavigationItem.SETTINGS
            else -> NavigationItem.SURAH_INDEX
        }

        val settingsViewModel = koinViewModel<SettingsViewModel>()
        val settingsState by settingsViewModel.uiState.collectAsState()

        val colorScheme = if (settingsState.nightMode) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }

        MaterialTheme(colorScheme = colorScheme) {
            AdaptiveNavigationScaffold(
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                QuranNavHost(navController = navController)
            }
        }
    }
}
