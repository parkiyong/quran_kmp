package io.github.parkiyong.quran.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object SurahIndex : Screen

    @Serializable
    data object Bookmarks : Screen

    @Serializable
    data object AudioDownloads : Screen

    @Serializable
    data object Settings : Screen
}

enum class NavigationItem(
    val route: Screen,
    val title: String
) {
    SURAH_INDEX(Screen.SurahIndex, "Surah"),
    BOOKMARKS(Screen.Bookmarks, "Bookmarks"),
    AUDIO_DOWNLOADS(Screen.AudioDownloads, "Audio"),
    SETTINGS(Screen.Settings, "Settings")
}
