package io.github.parkiyong.quran

import androidx.compose.ui.window.ComposeUIViewController
import io.github.parkiyong.quran.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) { App() }
