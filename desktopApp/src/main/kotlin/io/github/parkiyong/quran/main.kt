package io.github.parkiyong.quran

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.parkiyong.quran.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Quran",
        ) {
            App()
        }
    }
}
