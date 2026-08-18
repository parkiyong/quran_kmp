package io.github.parkiyong.quran.di

import io.github.parkiyong.quran.ui.audio.AudioDownloadsViewModel
import io.github.parkiyong.quran.ui.bookmarks.BookmarksViewModel
import io.github.parkiyong.quran.ui.settings.SettingsViewModel
import io.github.parkiyong.quran.ui.surah.SurahIndexViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val viewModelsModule = module {
    viewModelOf(::SurahIndexViewModel)
    viewModelOf(::BookmarksViewModel)
    viewModelOf(::AudioDownloadsViewModel)
    viewModelOf(::SettingsViewModel)
}

val coreModule = module {
    // Core services/repositories to be registered here
}

fun appModules(): List<Module> = listOf(coreModule, viewModelsModule)

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModules())
    }
}
