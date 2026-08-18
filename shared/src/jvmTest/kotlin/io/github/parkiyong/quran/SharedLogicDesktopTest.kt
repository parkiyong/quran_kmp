package io.github.parkiyong.quran

import io.github.parkiyong.quran.di.appModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SharedLogicDesktopTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun platformSmokeTest() {
        val platform = getPlatform()
        assertTrue(platform.name.contains("Java") || platform.name.contains("Desktop") || platform.name.contains("JVM"))

        val koinApp = startKoin {
            modules(appModules())
        }
        assertTrue(koinApp.koin.isInitialized())
    }
}