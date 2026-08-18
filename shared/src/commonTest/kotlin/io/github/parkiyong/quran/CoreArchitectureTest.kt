package io.github.parkiyong.quran

import app.cash.turbine.test
import io.github.parkiyong.quran.core.viewmodel.BaseViewModel
import io.github.parkiyong.quran.di.appModules
import io.github.parkiyong.quran.navigation.NavigationItem
import io.github.parkiyong.quran.navigation.Screen
import io.github.parkiyong.quran.ui.adaptive.WindowWidthSizeClass
import io.github.parkiyong.quran.ui.audio.AudioDownloadsEffect
import io.github.parkiyong.quran.ui.audio.AudioDownloadsIntent
import io.github.parkiyong.quran.ui.audio.AudioDownloadsViewModel
import io.github.parkiyong.quran.ui.bookmarks.BookmarksEffect
import io.github.parkiyong.quran.ui.bookmarks.BookmarksIntent
import io.github.parkiyong.quran.ui.bookmarks.BookmarksViewModel
import io.github.parkiyong.quran.ui.settings.SettingsIntent
import io.github.parkiyong.quran.ui.settings.SettingsViewModel
import io.github.parkiyong.quran.ui.surah.SurahIndexEffect
import io.github.parkiyong.quran.ui.surah.SurahIndexIntent
import io.github.parkiyong.quran.ui.surah.SurahIndexViewModel
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CoreArchitectureTest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun verifyKoinConfiguration() {
        startKoin {
            modules(appModules())
        }.checkModules()
    }

    @Test
    fun testWindowWidthSizeClassBreakpoints() {
        assertEquals(WindowWidthSizeClass.COMPACT, WindowWidthSizeClass.fromWidth(0.dp))
        assertEquals(WindowWidthSizeClass.COMPACT, WindowWidthSizeClass.fromWidth(599.dp))
        assertEquals(WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.fromWidth(600.dp))
        assertEquals(WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.fromWidth(839.dp))
        assertEquals(WindowWidthSizeClass.EXPANDED, WindowWidthSizeClass.fromWidth(840.dp))
        assertEquals(WindowWidthSizeClass.EXPANDED, WindowWidthSizeClass.fromWidth(1200.dp))
    }

    @Test
    fun testSurahIndexViewModelUDF() = runTest {
        val viewModel = SurahIndexViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals("", initial.searchQuery)
            assertFalse(initial.isLoading)

            viewModel.onIntent(SurahIndexIntent.SearchQueryChanged("Baqarah"))
            val updated = awaitItem()
            assertEquals("Baqarah", updated.searchQuery)
        }

        viewModel.effect.test {
            viewModel.onIntent(SurahIndexIntent.SurahClicked(2))
            val effect = awaitItem()
            assertTrue(effect is SurahIndexEffect.NavigateToReader)
            assertEquals(2, effect.surahNumber)
        }
    }

    @Test
    fun testSettingsViewModelUDF() = runTest {
        val viewModel = SettingsViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.nightMode)
            assertTrue(initial.keepScreenOn)

            viewModel.onIntent(SettingsIntent.SetNightMode(true))
            val updated = awaitItem()
            assertTrue(updated.nightMode)

            viewModel.onIntent(SettingsIntent.SetKeepScreenOn(false))
            val updated2 = awaitItem()
            assertFalse(updated2.keepScreenOn)
        }
    }

    @Test
    fun testBookmarksViewModelUDF() = runTest {
        val viewModel = BookmarksViewModel()

        viewModel.effect.test {
            viewModel.onIntent(BookmarksIntent.BookmarkClicked(42L))
            val effect = awaitItem()
            assertTrue(effect is BookmarksEffect.NavigateToAyah)
            assertEquals(42L, effect.id)
        }
    }

    @Test
    fun testAudioDownloadsViewModelUDF() = runTest {
        val viewModel = AudioDownloadsViewModel()

        viewModel.effect.test {
            viewModel.onIntent(AudioDownloadsIntent.CancelDownload("surah_114"))
            val effect = awaitItem()
            assertTrue(effect is AudioDownloadsEffect.ShowMessage)
            assertEquals("Cancelled download surah_114", effect.message)
        }
    }
}
