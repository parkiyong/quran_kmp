package io.github.parkiyong.quran.data.storage

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopQuranFileManagerTest {

    @Test
    fun testDirectoryStructureAndPaths() {
        val customRoot = "/test_root".toPath()
        val fileManager = DesktopQuranFileManager(
            customBaseDirectory = customRoot
        )

        assertEquals(customRoot, fileManager.appDataDirectory)
        assertEquals(customRoot / "cache", fileManager.cacheDirectory)
        assertEquals(customRoot / "databases", fileManager.databaseDirectory)
        assertEquals(customRoot / "quran_images", fileManager.imagesDirectory)
        assertEquals(customRoot / "audio", fileManager.audioDirectory)

        val dbPath = fileManager.resolveDatabasePath("ayahinfo_1024.db")
        assertEquals(customRoot / "databases" / "ayahinfo_1024.db", dbPath)

        val imgPath = fileManager.resolveImagePath("page001.png")
        assertEquals(customRoot / "quran_images" / "page001.png", imgPath)

        val audioPath = fileManager.resolveAudioPath("001001.mp3")
        assertEquals(customRoot / "audio" / "001001.mp3", audioPath)
    }
}
