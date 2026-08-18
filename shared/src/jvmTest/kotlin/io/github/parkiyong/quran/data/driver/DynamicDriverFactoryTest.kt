package io.github.parkiyong.quran.data.driver

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.parkiyong.quran.data.translation.TranslationsDatabase
import okio.FileSystem
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

    @Test
    fun testDynamicDriverWithActualFile() {
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "dynamic_db_test_${System.currentTimeMillis()}"
        FileSystem.SYSTEM.createDirectories(tempDir)
        val dbPath = tempDir / "test.db"

        try {
            val factory = JdbcQuranDatabaseDriverFactory()
            val fileDriver = factory.createDynamicDriver(dbPath)
            TranslationsDatabase.Schema.create(fileDriver)

            val db = TranslationsDatabase(fileDriver)
            db.translationsQueries.update(
                id = 2,
                name = "French Translation",
                translator = "Muhammad Hamidullah",
                translatorForeign = null,
                filename = "fr.hamidullah.db",
                url = "https://example.com/fr.hamidullah.db",
                languageCode = "fr",
                version = 1,
                minimumRequiredVersion = 1,
                userDisplayOrder = 2
            )

            val translations = db.translationsQueries.all().executeAsList()
            assertEquals(1, translations.size)
            assertEquals("French Translation", translations[0].name)
        } finally {
            FileSystem.SYSTEM.deleteRecursively(tempDir)
        }
    }
}
