package io.github.parkiyong.quran.data.bookmark

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookmarkDatabaseTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: BookmarksDatabase

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BookmarksDatabase.Schema.create(driver)
        database = BookmarksDatabase(
            driver = driver,
            bookmarksAdapter = Bookmarks.Adapter(
                suraAdapter = IntColumnAdapter,
                ayahAdapter = IntColumnAdapter,
                pageAdapter = IntColumnAdapter
            ),
            last_pagesAdapter = Last_pages.Adapter(
                pageAdapter = IntColumnAdapter
            )
        )
    }

    @Test
    fun testBookmarkQueries() {
        val bookmarkQueries = database.bookmarkQueries

        driver.execute(
            null,
            "INSERT INTO bookmarks (sura, ayah, page, added_date) VALUES (1, 1, 1, 1000)",
            0
        )

        driver.execute(
            null,
            "INSERT INTO bookmarks (sura, ayah, page, added_date) VALUES (2, 5, 2, 2000)",
            0
        )

        // Test getBookmarksByDateAdded
        val dateAddedBookmarks = bookmarkQueries.getBookmarksByDateAdded().executeAsList()
        assertEquals(2, dateAddedBookmarks.size)
        assertEquals(2, dateAddedBookmarks[0].sura)
        assertEquals(1, dateAddedBookmarks[1].sura)

        // Test getBookmarksByLocation
        val locationBookmarks = bookmarkQueries.getBookmarksByLocation().executeAsList()
        assertEquals(2, locationBookmarks.size)
        assertEquals(1, locationBookmarks[0].sura)
        assertEquals(2, locationBookmarks[1].sura)

        // Test getBookmarksByPage
        val pageBookmarks = bookmarkQueries.getBookmarksByPage(page = 1).executeAsList()
        assertEquals(1, pageBookmarks.size)
        assertEquals(1, pageBookmarks[0].sura)
        assertEquals(1, pageBookmarks[0].ayah)

        // Test getBookmarkIdForSuraAyah
        val bookmarkId = bookmarkQueries.getBookmarkIdForSuraAyah(sura = 1, ayah = 1).executeAsOneOrNull()
        assertTrue(bookmarkId != null && bookmarkId > 0)
    }

    @Test
    fun testTagAndBookmarkTagQueries() {
        val tagQueries = database.tagQueries
        val bookmarkTagQueries = database.bookmarkTagQueries

        driver.execute(
            null,
            "INSERT INTO tags (_ID, name, added_date) VALUES (1, 'Favorites', 1000)",
            0
        )

        driver.execute(
            null,
            "INSERT INTO tags (_ID, name, added_date) VALUES (2, 'Memorization', 2000)",
            0
        )

        val tags = tagQueries.getTags().executeAsList()
        assertEquals(2, tags.size)
        assertEquals("Favorites", tags[0].name)
        assertEquals("Memorization", tags[1].name)

        val foundTag = tagQueries.tagByName("Favorites").executeAsOneOrNull()
        assertEquals(1L, foundTag?._ID)

        // Insert bookmark_tag
        driver.execute(
            null,
            "INSERT INTO bookmark_tag (bookmark_id, tag_id, added_date) VALUES (10, 1, 1000)",
            0
        )
        driver.execute(
            null,
            "INSERT INTO bookmark_tag (bookmark_id, tag_id, added_date) VALUES (10, 2, 2000)",
            0
        )

        val tagIds = bookmarkTagQueries.getTagIdsForBookmark(bookmark_id = 10L).executeAsList()
        assertEquals(listOf(1L, 2L), tagIds)
    }

    @Test
    fun testLastPageQueries() {
        val lastPageQueries = database.lastPageQueries

        driver.execute(
            null,
            "INSERT INTO last_pages (page, added_date) VALUES (150, 5000)",
            0
        )
        driver.execute(
            null,
            "INSERT INTO last_pages (page, added_date) VALUES (300, 9000)",
            0
        )

        val lastPages = lastPageQueries.getLastPages().executeAsList()
        assertEquals(2, lastPages.size)
        assertEquals(300, lastPages[0].page)
        assertEquals(150, lastPages[1].page)
    }
}
