package io.github.parkiyong.quran.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.parkiyong.quran.ui.audio.AudioDownloadsScreen
import io.github.parkiyong.quran.ui.bookmarks.BookmarksScreen
import io.github.parkiyong.quran.ui.settings.SettingsScreen
import io.github.parkiyong.quran.ui.surah.SurahIndexScreen

@Composable
fun QuranNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.SurahIndex,
        modifier = modifier
    ) {
        composable<Screen.SurahIndex> {
            SurahIndexScreen()
        }
        composable<Screen.Bookmarks> {
            BookmarksScreen()
        }
        composable<Screen.AudioDownloads> {
            AudioDownloadsScreen()
        }
        composable<Screen.Settings> {
            SettingsScreen()
        }
    }
}
