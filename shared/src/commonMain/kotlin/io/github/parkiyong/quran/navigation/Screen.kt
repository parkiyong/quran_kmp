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
    val title: String,
    val iconName: String
) {
    SURAH_INDEX(Screen.SurahIndex, "Surah", "MenuBook"),
    BOOKMARKS(Screen.Bookmarks, "Bookmarks", "Bookmark"),
    AUDIO_DOWNLOADS(Screen.AudioDownloads, "Audio", "Headphones"),
    SETTINGS(Screen.Settings, "Settings", "Settings")
}
