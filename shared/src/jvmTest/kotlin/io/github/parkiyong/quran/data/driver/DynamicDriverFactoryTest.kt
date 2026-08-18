package io.github.parkiyong.quran.data.driver

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.parkiyong.quran.data.translation.TranslationsDatabase
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DynamicDriverFactoryTest {

    @Test
    fun testDynamicDriverCreationAndExecution() {
        val factory = JdbcQuranDatabaseDriverFactory()
        val inMemoryDriver = factory.createDriver(TranslationsDatabase.Schema, JdbcSqliteDriver.IN_MEMORY)
        val db = TranslationsDatabase(inMemoryDriver)

        db.translationsQueries.update(
            id = 1,
            name = "English Translation",
            translator = "Saheeh International",
            translatorForeign = null,
            filename = "en.saheeh.db",
            url = "https://example.com/en.saheeh.db",
            languageCode = "en",
            version = 1,
            minimumRequiredVersion = 1,
            userDisplayOrder = 1
        )

        val translations = db.translationsQueries.all().executeAsList()
        assertEquals(1, translations.size)
        assertEquals("English Translation", translations[0].name)
        assertEquals("en.saheeh.db", translations[0].filename)

        val dynamicDriver = factory.createDynamicDriver(":memory:".toPath())
        assertNotNull(dynamicDriver)
    }
}
